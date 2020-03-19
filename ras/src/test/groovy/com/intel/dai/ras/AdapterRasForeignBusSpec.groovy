// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.ras

import com.intel.dai.IAdapter
import com.intel.dai.dsapi.WorkQueue
import com.intel.logging.Logger
import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import org.voltdb.types.TimestampType
import spock.lang.Specification

class AdapterRasForeignBusSpec extends Specification {
    def logger_ = Mock(Logger)
    def adapter_
    def workQueue_
    def client_
    String[] args_ = [ "localhost", "location", "hostname" ]

    def underTest_
    def setup() {
        adapter_ = Mock(IAdapter)
        adapter_.adapterShuttingDown() >>> [false, false, true]
        workQueue_ = Mock(WorkQueue)
        client_ = Mock(Client)

        workQueue_.grabNextAvailWorkItem() >>> [true, false, false]
        adapter_.setUpAdapter(_ as String, _ as String) >> workQueue_
        client_.callProcedure("ComputeNodeListLctnAndSeqNum") >>
                buildComputeNodeListLctnAndSeqNum()
        client_.callProcedure("InternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp",
                _ as TimestampType, _ as TimestampType) >>
                buildInternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp()
        client_.callProcedure("RasEventProcessNewControlOperations") >>
                buildRasEventProcessNewControlOperations()
        client_.callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp", _ as String,
                _ as Long, _ as Long) >> buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp()
        adapter_.client() >> client_
        adapter_.isComputeNodeLctn(_ as String) >> true

        underTest_ = new AdapterRasForeignBus(adapter_, logger_)
    }

    def buildComputeNodeListLctnAndSeqNum(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("SequenceNumber", VoltType.INTEGER))
        table.addRow("R0-CH0-CN0", 1)
        response.getResults() >> [ table ]
        return response
    }

    def buildComputeNodeListLctnAndSeqNum() {
        buildComputeNodeListLctnAndSeqNum(ClientResponse.SUCCESS)
    }

    def buildRasEventListThatNeedJobId(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table0 = new VoltTable(
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("EventType", VoltType.STRING),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP)
        )
        table0.addRow(1L, "0000000000", "R0-CH0-CN0", 99_999_999L)
        table0.addRow(1L, "0000000000", "R0-CH0-CN1", 99_999_999L)
        table0.addRow(1L, "0000000000", "R0-CH0-CN2", 99_999_999L)
        def table1 = new VoltTable(new VoltTable.ColumnInfo("_", VoltType.TIMESTAMP))
        table1.addRow(99_999_999L)
        response.getResults() >> [ table0, table1 ]
        return response
    }

    def buildRasEventListThatNeedJobId() {
        buildRasEventListThatNeedJobId(ClientResponse.SUCCESS)
    }

    def buildInternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp() {
        return buildInternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp(ClientResponse.SUCCESS)
    }

    def buildInternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(
                new VoltTable.ColumnInfo("NodeLctn", VoltType.STRING),
                new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("DbUpdatedTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("EndTimestamp", VoltType.TIMESTAMP),
        )
        table.addRow("R0-CH0-CN0", "JobID", 94_999_999, 94_999_999, 94_999_999)
        response.getResults() >> [ table ]
        return response
    }

    def buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp() {
        return buildInternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp(ClientResponse.SUCCESS)
    }

    def buildInternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(
                new VoltTable.ColumnInfo("JobId", VoltType.STRING)
        )
        table.addRow("97")
        table.addRow("98")
        table.addRow("99")
        response.getResults() >> [ table ]
        return response
    }

    def buildRasEventProcessNewControlOperations() {
        return buildRasEventProcessNewControlOperations(ClientResponse.SUCCESS)
    }

    def buildRasEventProcessNewControlOperations(byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        def table = new VoltTable(
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("EventType", VoltType.STRING),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("Sernum", VoltType.STRING),
                new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                new VoltTable.ColumnInfo("NumberRepeats", VoltType.INTEGER),
                new VoltTable.ColumnInfo("ControlOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("ControlOperationDone", VoltType.STRING),
                new VoltTable.ColumnInfo("InstanceData", VoltType.STRING),
                new VoltTable.ColumnInfo("DbUpdatedTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("LastChgAdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("LastChgWorkItemId", VoltType.BIGINT)
        )
        table.addRow(0L, "0004000034", "R0-CH0-CN3", "SerNum", "JobId", 0, "ErrorOnNode", "N", "", 99_999_999L,
                99_999_999L, "PROVISIONER", -99L)
        table.addRow(0L, "0004000034", "R0-CH0-CN0", "SerNum", "JobId", 0, "TestControlOperation", "N", "",
                99_999_999L, 99_999_999L, "RAS", -99L)
        table.addRow(0L, "0004000034", "R0-CH0-CN0", "SerNum", "JobId", 0, "IncreaseFanSpeed", "N", "",
                99_999_999L, 99_999_999L, "MONITOR", -99L)
        table.addRow(0L, "0004000034", "R0-CH0-CN0", "SerNum", "JobId", 0, "NotAControlOp", "N", "",
                99_999_999L, 99_999_999L, "RAS", -99L)
        response.getResults() >> [ table ]
        return response
    }

    def "Test mainProcessingFlow HandleFillingInJobIdsAndControlOps"() {
        ClientResponse response = buildRasEventListThatNeedJobId()
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >> response
        if(ADDROW)
            response.getResults()[0].addRow(1L, "0000000000", "R0-CH0-CN3", 99_999_999L)
        adapter_.client() >> client_
        workQueue_.workToBeDone() >> "HandleFillingInJobIdsAndControlOps"
        underTest_.mainProcessingFlow(args_)
        expect: RESULT
        where:
        ADDROW || RESULT
        false  || true
        true   || true
    }

    def "Test mainProcessingFlow HandleFillingInJobIdsAndControlOps Negative"() {
        ClientResponse response = buildRasEventListThatNeedJobId(ClientResponse.OPERATIONAL_FAILURE)
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >> response
        adapter_.client() >> client_
        workQueue_.workToBeDone() >> "HandleFillingInJobIdsAndControlOps"
        underTest_.mainProcessingFlow(args_)
        expect: true
    }

    def "Test mainProcessingFlow HandleFillingInJobIdOnly"() {
        ClientResponse response = buildRasEventListThatNeedJobId()
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >> response
        if(ADDROW)
            response.getResults()[0].addRow(1L, "0000000000", "R0-CH0-CN3", 99_999_999L)
        adapter_.client() >> client_
        workQueue_.workToBeDone() >> "HandleFillingInJobIdOnly"
        underTest_.mainProcessingFlow(args_)
        expect: true
        where:
        ADDROW || RESULT
        false  || true
        true   || true
    }

    def "Test mainProcessingFlow HandleControlOpsOnly"() {
        client_.callProcedure("RasEventListThatNeedJobId", _ as Long) >> buildRasEventListThatNeedJobId()
        adapter_.client() >> client_
        workQueue_.workToBeDone() >> "HandleControlOpsOnly"
        underTest_.mainProcessingFlow(args_)
        expect: true
    }
}
