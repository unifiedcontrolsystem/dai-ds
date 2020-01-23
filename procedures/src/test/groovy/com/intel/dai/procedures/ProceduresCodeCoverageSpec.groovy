// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures

import spock.lang.Specification
import org.voltdb.*
import org.voltdb.types.*

class ProceduresCodeCoverageSpec extends Specification {
    def "AdapterListOfActiveAndWorkItems - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("AdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("Pid", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Queue", VoltType.STRING),
                new VoltTable.ColumnInfo("workItemId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("WorkingAdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingAdapterId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("WorkToBeDone", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingResults", VoltType.STRING),
                new VoltTable.ColumnInfo("Parameters", VoltType.STRING)
        )
        voltdbRes[0].addRow("AdapterType", 40, "Lctn", 41, "Queue", 42,
                "WorkingAdapterType", 43, "WorkToBeDone", "State", "WorkingResults", "Parameters")

        given:
        def testSubject = Spy(AdapterListOfActiveAndWorkItems)
        testSubject.voltQueueSQL(_) >> {}
        testSubject.voltQueueSQL(_,_,_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes

        def sStartingTimestamp = new TimestampType(0)
        def sEndingTimestamp = new TimestampType(42)

        when:
        def res = testSubject.run(sEndingTimestamp, sStartingTimestamp)

        then:
        res.toFormattedString().contains(
                "40 Lctn                  41 Queue                   42 WorkingAdapterType")
    }

    def "ComputeNodeSetWlmNodeState - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        given:
        def testSubject = Spy(ComputeNodeSetWlmNodeState)
        testSubject.voltQueueSQL(_,_,_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes

        when:
        def res = testSubject.run("", "", 0, "", 42)

        then:
        thrown VoltProcedure.VoltAbortException
    }

    def "ComputeNodeSetOwner - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        given:
        def testSubject = Spy(ComputeNodeSetOwner)
        testSubject.voltQueueSQL(_,_,_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes

        when:
        def res = testSubject.run("", "", 0, "", 42)

        then:
        thrown VoltProcedure.VoltAbortException
    }

    def "ComputeNodeSetStates - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))
        String[] saNodeLctns = {""}

        given:
        def testSubject = Spy(ComputeNodeSetStates)
        testSubject.voltQueueSQL(_,_,_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes

        when:
        def res = testSubject.run(saNodeLctns, "", 0, "", 42)

        then:
        thrown VoltProcedure.VoltAbortException
    }

    def "ServiceNodeSetOwner  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        given:
        def testSubject = Spy(ServiceNodeSetOwner)
        testSubject.voltQueueSQL(_,_,_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes

        when:
        def res = testSubject.run("", "", 0, "", 42)

        then:
        thrown VoltProcedure.VoltAbortException
    }

    def "ServiceEndRepairError  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStoppedService", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(40, "Lctn", "TypeOfServiceOperation",
                "UserStartedService", "UserStoppedService", new TimestampType(41), "StartRemarks", "LogFile")

        given:
        def testSubject = Spy(ServiceEndRepairError)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes
        testSubject.getTransactionTime() >> new Date()

        when:
        def res = testSubject.run(0, "", "")

        then:
        res == 0
    }

    def "ServiceEndRepair  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(40, "Lctn", "TypeOfServiceOperation",
                "UserStartedService", new TimestampType(41), "StartRemarks", "LogFile")

        given:
        def testSubject = Spy(ServiceEndRepair)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes
        testSubject.getTransactionTime() >> new Date()

        when:
        def res = testSubject.run(40, "sLctn", "sUser", "sRemarks")

        then:
        res == 0
    }

    def "ServiceStartPrepared  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStoppedService", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(40, "Lctn", "TypeOfServiceOperation",
                "UserStartedService", "UserStoppedService", new TimestampType(41), "StartRemarks", "LogFile")

        given:
        def testSubject = Spy(ServiceStartPrepared)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes
        testSubject.getTransactionTime() >> new Date()

        when:
        def res = testSubject.run("sLctn", 42, "sRemarks")

        then:
        res == 0
    }

    def "ServiceStartFailed  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStoppedService", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(40, "Lctn", "TypeOfServiceOperation",
                "UserStartedService", "UserStoppedService", new TimestampType(41), "StartRemarks", "LogFile")

        given:
        def testSubject = Spy(ServiceStartFailed)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes
        testSubject.getTransactionTime() >> new Date()

        when:
        def res = testSubject.run("sLctn", 42, "sRemarks")

        then:
        res == 0
    }

    def "ServiceForceCloseOperation  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStoppedService", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StopTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("StopRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(40, "Lctn", "TypeOfServiceOperation",
                "UserStartedService", "UserStoppedService",
                new TimestampType(41), new TimestampType(42),
                "StartRemarks", "StopRemarks","LogFile")

        given:
        def testSubject = Spy(ServiceForceCloseOperation)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes
        testSubject.getTransactionTime() >> new Date()

        when:
        def res = testSubject.run(42, "sLctn")

        then:
        res == 0
    }

    def "ServiceCloseOperation  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStoppedService", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StopTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("StopRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(40, "Lctn", "TypeOfServiceOperation",
                "UserStartedService", "UserStoppedService",
                new TimestampType(41), new TimestampType(42),
                "StartRemarks", "StopRemarks","LogFile")

        given:
        def testSubject = Spy(ServiceCloseOperation)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes
        testSubject.getTransactionTime() >> new Date()

        when:
        def res = testSubject.run(42, "sLctn")

        then:
        res == 0
    }

    def "ComputeNodeStates  - run" () {
        String[] aNodeLctns = {""}

        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING)
        )
        voltdbRes[0].addRow("Lctn")

        given:
        def testSubject = Spy(ComputeNodeStates)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> voltdbRes

        when:
        def res = testSubject.run(aNodeLctns)

        then:
        res != null
    }

    def "RasEventListByLimit  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING)
        )
        voltdbRes[0].addRow("Lctn")

        given:
        def testSubject = Spy(RasEventListByLimit)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> voltdbRes

        when:
        def res = testSubject.run(40, 42)

        then:
        res != null
    }

    def "ServiceStarted  - run" () {
        given:
        def testSubject = Spy(ServiceStarted)
        testSubject.getTransactionTime() >> new Date()
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> {}

        when:
        def res = testSubject.run("sLctn", 40,
                "sUser", "sOperation",
                "sRemarks", "sLogFile")

        then:
        res == 0
    }

    def "MachineAdapterInstanceBumpNextInstanceNumAndReturn  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("NumStartedInstances", VoltType.BIGINT),
                new VoltTable.ColumnInfo("NumInitialInstances", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Invocation", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        voltdbRes[0].addRow(42, 43, "Invocation", "LogFile")

        given:
        def testSubject = Spy(MachineAdapterInstanceBumpNextInstanceNumAndReturn)
        testSubject.getTransactionTime() >> new Date()
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(_) >> voltdbRes

        when:
        def res = testSubject.run("sAdapterType", "sSnLctn")

        then:
        res == 43
    }

    def "NodePurgeInventory_History  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("InventoryTimestamp", VoltType.TIMESTAMP)
        )
        voltdbRes[0].addRow("Lctn", new TimestampType(42))

        given:
        def testSubject = Spy(NodePurgeInventory_History)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> voltdbRes

        when:
        def res = testSubject.run(new TimestampType(43))

        then:
        res == 0
    }

    def "DiagResultSavePerUnit  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        given:
        def testSubject = Spy(DiagResultSavePerUnit)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> voltdbRes

        when:
        def res = testSubject.run(42, "sLctn", "cState", "sResults")

        then:
        thrown VoltProcedure.VoltAbortException
    }

    def "AdapterInfoUsingTypeLctnPid  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        given:
        def testSubject = Spy(AdapterInfoUsingTypeLctnPid)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> voltdbRes

        when:
        def res = testSubject.run("sAdaptersType", "sAdaptersType", -1)

        then:
        res != null
    }

    def "MachineAdapterInvocationInformation  - run" () {
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        given:
        def testSubject = Spy(MachineAdapterInvocationInformation)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> voltdbRes

        when:
        def res = testSubject.run("sAdaptersType", null)

        then:
        res != null
    }
}
