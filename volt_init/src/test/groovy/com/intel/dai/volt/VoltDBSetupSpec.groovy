// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.volt

import com.intel.logging.Logger
import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientConfig
import org.voltdb.client.ClientResponse
import org.voltdb.client.ProcCallException
import spock.lang.Specification

import java.util.concurrent.TimeoutException

class VoltDBSetupSpec extends Specification {
    def parentClient_
    def logger_
    DaiVoltState checkValue_ = DaiVoltState.EMPTY
    boolean useCalls_ = true
    List<DaiVoltState> stateList_ = null

    class TestVoltDBSetup extends VoltDBSetup {
        TestVoltDBSetup(String servers, long timeoutSeconds, String jar, String sqlFile, String manifest, String machine,
                            String rasData, Logger logger) throws IOException {
            super(servers, timeoutSeconds, jar, sqlFile, manifest, machine, rasData, logger)
        }

        @Override
        protected Client createClient(ClientConfig config) {
            return parentClient_
        }

        @Override
        DaiVoltState checkVoltState() {
            if(useCalls_)
                return super.checkVoltState()
            else {
                if(stateList_ == null || stateList_.size() == 0)
                    return checkValue_
                else {
                    DaiVoltState value = stateList_.get(0)
                    stateList_.remove(0)
                    if(stateList_.isEmpty())
                        stateList_ = null
                    return value
                }
            }
        }
    }

    def underTest_
    void setup() {
        stateList_ = null
        parentClient_ = Mock(Client)
        logger_ = Mock(Logger)
        def sql = "../build/tmp/VoltDB.sql"
        new File(sql).text = """;"""
        underTest_ = new TestVoltDBSetup("localhost", 3L, "../libs/procedures.jar", sql,
                "../configurations/eventsim/SystemManifest.json",
                "../configurations/eventsim/MachineConfig.json", "../configurations/common/RasEventMetaData.json",
                logger_)
    }

    VoltTable makeTable(List<List<?>> columns, List<List<?>> data) {
        VoltTable.ColumnInfo[] columnArray = new VoltTable.ColumnInfo[columns.size()]
        int counter = 0;
        for(List<?> pair: columns)
            columnArray[counter++] = new VoltTable.ColumnInfo((String)pair.get(0), (VoltType)pair.get(1))
        VoltTable table = new VoltTable(columnArray)
        if(data != null)
            for(List<?> row: data)
                table.addRow(row.toArray())
        return table
    }

    ClientResponse makeResponse(byte status, List<List<?>> columns, List<List<?>> data) {
        ClientResponse result = Mock(ClientResponse)
        result.getStatus() >> status
        if(status == ClientResponse.SUCCESS) {
            VoltTable[] tables = new VoltTable[1]
            tables[0] = makeTable(columns, data)
            result.getResults() >> tables
        }
        return result
    }

    ClientResponse makeStatusResponse(byte status, String data) {
        def columns = [["Status", VoltType.STRING]]
        def tableData = (data != null) ? [[data]] : null
        return makeResponse(status, columns, tableData)
    }

    void overrideAdHocProcedure(ClientResponse response) {
        parentClient_.callProcedure("@AdHoc", _ as String) >> response
    }

    void overrideAdHocProcedure(Throwable response) {
        parentClient_.callProcedure("@AdHoc", _ as String) >> { throw response }
    }

    def "Test no connection"() {
        given:
            parentClient_.createConnection(_ as String) >> { throw new ConnectException("TEST EXCEPTION") }
        when:
            underTest_.setupOrWaitForVoltDB(3L)
        then:
            thrown(TimeoutException)
    }

    def "Test has connection but unknown status table response"() {
        given:
            overrideAdHocProcedure(makeStatusResponse(ClientResponse.RESPONSE_UNKNOWN, null))
        when:
            underTest_.setupOrWaitForVoltDB(3L)
        then:
            thrown(RuntimeException)
    }

    def "Test has connection but no status table"() {
        given:
            overrideAdHocProcedure(new ProcCallException(makeStatusResponse(ClientResponse.SUCCESS, null),"lastCallInfo", null))
        expect:
            underTest_.checkVoltState() == DaiVoltState.EMPTY
    }

    def "Test has connection but no status table rows"() {
        given:
            overrideAdHocProcedure(makeStatusResponse(ClientResponse.SUCCESS, null))
        expect:
            underTest_.checkVoltState() == DaiVoltState.SCHEMA_LOADING
    }

    def "Test has connection and status"() {
        given:
            overrideAdHocProcedure(makeStatusResponse(ClientResponse.SUCCESS, "populate-completed"))
            underTest_.checkVoltState()
            def state = underTest_.checkVoltState()
            underTest_.closeClient()
        expect:
            state == DaiVoltState.READY
    }

    def "Test setup from empty db"() {
        given:
            useCalls_ = false
            underTest_.client_ = parentClient_
            parentClient_.callProcedure(_ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("@AdHoc", _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("UCSCONFIGVALUE.select", "UcsLogfileDirectory") >>
                    makeResponse(ClientResponse.SUCCESS, [["Value", VoltType.STRING]], [["/tmp"]])
            parentClient_.callProcedure(_ as String, _ as String, _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            stateList_ = [ DaiVoltState.EMPTY, DaiVoltState.SCHEMA_LOADED, DaiVoltState.READY, DaiVoltState.READY ]
            underTest_.setupOrWaitForVoltDB(3L)
        expect:
            underTest_.checkVoltState() == DaiVoltState.READY
    }

    def "Test setup from empty db negative 1"() {
        given:
            useCalls_ = false
            underTest_.client_ = parentClient_
            parentClient_.callProcedure(_ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("@AdHoc", _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure(_ as String, _ as String, _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            stateList_ = [ DaiVoltState.EMPTY, DaiVoltState.SCHEMA_LOADED, DaiVoltState.READY, DaiVoltState.READY ]
        when:
            underTest_.setupOrWaitForVoltDB(3L)
        then:
            thrown(DaiBadStateException)
    }

    def "Test setup from empty db negative 2"() {
        given:
            useCalls_ = false
            underTest_.client_ = parentClient_
            parentClient_.updateClasses(_ as String) >>
                    makeResponse(ClientResponse.OPERATIONAL_FAILURE, [["value", VoltType.BIGINT]], [[1L]])
            parentClient_.callProcedure(_ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("@AdHoc", _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("UCSCONFIGVALUE.select", "UcsLogfileDirectory") >>
                    makeResponse(ClientResponse.SUCCESS, [["Value", VoltType.STRING]], [["/tmp"]])
            parentClient_.callProcedure(_ as String, _ as String, _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            underTest_.setupOrWaitForVoltDB(3L)
        expect:
            underTest_.checkVoltState() == DaiVoltState.EMPTY
    }

    def "Test straggler methods"() {
        given:
            underTest_.connect()
            underTest_.setSchemaFailedState("TEST")
            underTest_.setPopulateFailed()
            VoltDBSetup.exceptToString(new Exception("TEST"))
        when:
            VoltDBSetup.setupVoltDBOrWait("localhost", 1L, logger_)
        then:
            thrown(Exception)
    }


    def "Test has connection and bad status"() {
        given:
            checkValue_ = DaiVoltState.SCHEMA_ERROR
            useCalls_ = false
            underTest_.client_ = parentClient_
            parentClient_.callProcedure(_ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("@AdHoc", _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
            parentClient_.callProcedure("UCSCONFIGVALUE.select", "UcsLogfileDirectory") >>
                    makeResponse(ClientResponse.SUCCESS, [["Value", VoltType.STRING]], [["/tmp"]])
            parentClient_.callProcedure(_ as String, _ as String, _ as String) >>
                    makeResponse(ClientResponse.SUCCESS, [["value", VoltType.BIGINT]], [[0L]])
        when:
            underTest_.setupOrWaitForVoltDB(3L)
        then:
            thrown(DaiBadStateException)
    }

}
