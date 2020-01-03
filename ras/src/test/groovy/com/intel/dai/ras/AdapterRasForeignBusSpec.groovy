// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ras

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.AdapterOperations
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.LegacyVoltDbDirectAccess
import com.intel.dai.dsapi.NodeInformation
import com.intel.dai.dsapi.RasEventLog
import com.intel.dai.dsapi.WorkQueue
import com.intel.dai.ras.AdapterRasForeignBus
import com.intel.logging.Logger
import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification

class AdapterRasForeignBusSpec extends Specification {
    def logger_ = Mock(Logger)
    def adapter_
    def factory_
    def legacy_
    def workQueue_
    def client_
    def nodeInfo_
    def rasEvents_
    def operations_
    String[] args_ = [ "localhost", "location", "hostname" ]

    def underTest_
    def setup() {
        adapter_ = Mock(AdapterInformation)
        adapter_.isShuttingDown() >>> [false, false, true]
        factory_ = Mock(DataStoreFactory)
        operations_ = Mock(AdapterOperations)
        factory_.createAdapterOperations(_ as AdapterInformation) >> operations_
        operations_.shutdownAdapter() >> { /* Do Nothing */ return 0 }
        operations_.shutdownAdapter(_ as Throwable) >> { /* Do Nothing */ return 2 }
        legacy_ = Mock(LegacyVoltDbDirectAccess)
        workQueue_ = Mock(WorkQueue)
        client_ = Mock(Client)
        factory_.createVoltDbLegacyAccess() >> legacy_
        factory_.createWorkQueue(adapter_) >> workQueue_
        legacy_.getVoltDbClient() >> client_
        nodeInfo_ = Mock(NodeInformation)
        factory_.createNodeInformation() >> nodeInfo_
        rasEvents_ = Mock(RasEventLog)
        factory_.createRasEventLog(adapter_) >> rasEvents_
        rasEvents_.getRasEventType(_ as String, _ as Long) >> "000000000"

        underTest_ = new AdapterRasForeignBus(adapter_, logger_, factory_)
    }

    def buildMachineDescriptionResponse(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(new VoltTable.ColumnInfo("Description", VoltType.STRING))
        table.addRow("test_machine")
        response.getResults() >> [ table ]
        return response
    }

    def buildRasEventListThatNeedJobIdResponse(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(new VoltTable.ColumnInfo("EventType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP))
        table.addRow("event", 999L, "location", 100000L)
        response.getResults() >> [ table ]
        return response
    }

    def buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestampResponse(byte status, int rows) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(new VoltTable.ColumnInfo("JobId", VoltType.STRING))
        if(rows >= 1)
            table.addRow("jobTestId")
        if(rows >= 2)
            table.addRow("jobTestId2")
        response.getResults() >> [ table ]
        return response
    }

    def buildRasEventProcessNewControlOperationsResponse(byte status, boolean location) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(new VoltTable.ColumnInfo("EventType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("ControlOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                new VoltTable.ColumnInfo("DescriptiveName", VoltType.STRING)
        )
        table.addRow("eventType", 99999L, "controlOperation", location?"location":null, "jobId", "descriptiveName")
        response.getResults() >> [ table ]
        return response
    }

    def buildRasEventUpdateControlOperationDoneResponse() {
        def response = Mock(ClientResponse)
        def table = new VoltTable(new VoltTable.ColumnInfo("Result", VoltType.BIGINT))
        table.addRow(0L)
        response.getResults() >> [ table ]
        return response
    }

    def buildGenericResponse() {
        buildGenericResponse(ClientResponse.SUCCESS)
    }

    def buildGenericResponse(byte status) {
        def response = Mock(ClientResponse)
        def table = new VoltTable(new VoltTable.ColumnInfo("Result", VoltType.BIGINT))
        table.addRow(0L)
        response.getResults() >> [ table ]
        response.getStatus() >> status
        return response
    }

    def "Test fetchSystemName"() {
        given:
        client_.callProcedure("RasEventUpdateControlOperationDone", _ as String, _ as String, _ as String) >>
                buildGenericResponse()
        client_.callProcedure("RasEventUpdateJobId", _ as String, _ as String, _ as Long) >> buildGenericResponse()
        client_.callProcedure("MachineDescription") >> buildMachineDescriptionResponse(status)
        underTest_.fetchSystemName()

        expect:
        underTest_.machineName_ == expected

        where:
        status                             || expected
        ClientResponse.SUCCESS             || "test_machine"
        ClientResponse.OPERATIONAL_FAILURE || "Undefined Machine Name"
    }

    def "Test fetchSystemName with exception"() {
        given:
        client_.callProcedure("RasEventUpdateControlOperationDone", _ as String, _ as String, _ as String) >>
                buildGenericResponse(ClientResponse.OPERATIONAL_FAILURE)
        client_.callProcedure("RasEventUpdateJobId", _ as String, _ as String, _ as Long) >> buildGenericResponse()
        client_.callProcedure("MachineDescription") >> {
            throw new IOException("TEST")
        }

        when:
        underTest_.fetchSystemName()

        then:
        underTest_.machineName_ == "Undefined Machine Name"
    }

    def "Test mainProcessingFlow"() {
        nodeInfo_.isComputeNodeLocation(_) >> true
        nodeInfo_.isServiceNodeLocation(_) >> false
        client_.callProcedure("InternalCachedJobsRemoveExpiredJobs", _ as Long) >>
                buildGenericResponse(ClientResponse.OPERATIONAL_FAILURE)
        client_.callProcedure("RasEventUpdateControlOperationDone", _ as String, _ as String, _ as String) >>
                buildGenericResponse()
        client_.callProcedure("RasEventUpdateJobId", _ as String, _ as String, _ as Long) >> buildGenericResponse()
        client_.callProcedure("MachineDescription") >> buildMachineDescriptionResponse(ClientResponse.SUCCESS)
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >>
                buildRasEventListThatNeedJobIdResponse(result1)
        client_.callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp",
                _ as String, _ as Long, _ as Long) >>
                buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestampResponse(result2, rows)
        if(args != null)
            underTest_.mainProcessingFlow(args.split(","))
        else
            underTest_.mainProcessingFlow(new String[0])
        expect: true

        where:
        args                          | rows | result1                            | result2
        null                          | 1    | ClientResponse.SUCCESS             | ClientResponse.SUCCESS
        "localhost"                   | 1    | ClientResponse.OPERATIONAL_FAILURE | ClientResponse.SUCCESS
        "localhost,location"          | 2    | ClientResponse.SUCCESS             | ClientResponse.SUCCESS
        "localhost,location,hostname" | 1    | ClientResponse.SUCCESS             | ClientResponse.OPERATIONAL_FAILURE
        "localhost,location,hostname" | 0    | ClientResponse.SUCCESS             | ClientResponse.SUCCESS
    }

    def "Test mainProcessingFlow part 2"() {
        nodeInfo_.isComputeNodeLocation(_) >> compute
        nodeInfo_.isServiceNodeLocation(_) >> service
        client_.callProcedure("RasEventUpdateControlOperationDone", _ as String, _ as String, _ as String) >>
                buildGenericResponse()
        client_.callProcedure("RasEventUpdateJobId", _ as String, _ as String, _ as Long) >> buildGenericResponse()
        client_.callProcedure("MachineDescription") >> buildMachineDescriptionResponse(ClientResponse.SUCCESS)
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >>
                buildRasEventListThatNeedJobIdResponse(ClientResponse.SUCCESS)
        client_.callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp",
                _ as String, _ as Long, _ as Long) >>
                buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestampResponse(ClientResponse.SUCCESS,
                        compute?1:0)
        underTest_.mainProcessingFlow(args_)

        expect: true

        where:
        compute | service
        true    | false
        false   | true
        false   | false
    }

    def "Test mainProcessingFlow part 3"() {
        nodeInfo_.isComputeNodeLocation(_) >> true
        nodeInfo_.isServiceNodeLocation(_) >> false
        workQueue_.wasWorkDone() >>> [false, true, true]
        operations_.logRasEventSyncNoEffectedJob(_, _, _, _, _, _) >> { return }
        client_.callProcedure("RasEventUpdateControlOperationDone", _ as String, _ as String, _ as String) >>
                buildGenericResponse()
        client_.callProcedure("RasEventUpdateJobId", _ as String, _ as String, _ as Long) >>
                buildGenericResponse()
        client_.callProcedure("MachineDescription") >> buildMachineDescriptionResponse(ClientResponse.SUCCESS)
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >>
                buildRasEventListThatNeedJobIdResponse(ClientResponse.SUCCESS)
        client_.callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp",
                _ as String, _ as Long, _ as Long) >>
                buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestampResponse(ClientResponse.SUCCESS, 1)
        client_.callProcedure("RasEventProcessNewControlOperations") >>
                buildRasEventProcessNewControlOperationsResponse(result1, true)
        client_.callProcedure("RasEventUpdateControlOperationDone", _ as String, _ as String, _ as String) >>
                buildRasEventUpdateControlOperationDoneResponse()
        operations_.shutdownAdapter() >> { -> /* Do Nothing */ throw new IOException("TEST") }

        underTest_.mainProcessingFlow(args_)

        expect: expected

        where:
        result1                            | expected
        ClientResponse.SUCCESS             | true
        ClientResponse.OPERATIONAL_FAILURE | true
        ClientResponse.SUCCESS             | true
    }

    def "Test mainProcessingFlow part 4"() {
        nodeInfo_.isComputeNodeLocation(_) >> true
        nodeInfo_.isServiceNodeLocation(_) >> false
        workQueue_.wasWorkDone() >>> [false, true, true]
        operations_.logRasEventSyncNoEffectedJob(_, _, _, _, _, _) >> { return }
        client_.callProcedure("MachineDescription") >> buildMachineDescriptionResponse(ClientResponse.SUCCESS)
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >>
                buildRasEventListThatNeedJobIdResponse(ClientResponse.SUCCESS)
        client_.callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp",
                _ as String, _ as Long, _ as Long) >>
                buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestampResponse(ClientResponse.SUCCESS, rows)
        client_.callProcedure("RasEventProcessNewControlOperations") >>
                buildRasEventProcessNewControlOperationsResponse(ClientResponse.SUCCESS, false)
        client_.callProcedure(_ as String, _ as String, _ as String, _ as Long) >>
                buildGenericResponse(ClientResponse.OPERATIONAL_FAILURE)
        client_.callProcedure(_ as String, _ as String, _ as String, _ as String) >>
                buildGenericResponse(ClientResponse.OPERATIONAL_FAILURE)
        operations_.shutdownAdapter() >> { -> /* Do Nothing */ throw new IOException("TEST") }

        underTest_.mainProcessingFlow(args_)

        expect: expected

        where:
        rows | expected
        1    | true
        0    | true
    }
}
