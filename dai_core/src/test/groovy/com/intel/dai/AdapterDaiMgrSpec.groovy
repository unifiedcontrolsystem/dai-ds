package com.intel.dai

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.DbStatusApi
import com.intel.dai.dsapi.Location
import com.intel.dai.dsapi.WorkQueue
import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification

import java.util.concurrent.TimeoutException

class AdapterDaiMgrSpec extends Specification {
    def client_
    def baseAdapter_
    def logger_
    def factory_
    def location_ = "location"
    static def workQueue

    def underTest_
    def setup() {
        client_ = Mock(Client)
        baseAdapter_ = Mock(IAdapter)
        baseAdapter_.client() >> client_
        baseAdapter_.adapterType() >> "TYPE"
        baseAdapter_.snLctn() >> { return location_ }
        baseAdapter_.mapServNodeLctnToHostName() >> new HashMap<String,String>() {{ put("location","hostname") }}
        logger_ = Mock(Logger)
        factory_ = Mock(DataStoreFactory)

        workQueue = Mock(WorkQueue)
        workQueue.workItemId() >> 42L
        workQueue.getTsFromWorkingResults(_ as String) >> "2019-09-30 13:00:00.0000"
        underTest_ = new AdapterDaiMgr(baseAdapter_, logger_, factory_)
        underTest_.workQueue = workQueue
        underTest_.quickClient_ = client_
    }

    def buildUcsVarResult(String value) {
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("Value", VoltType.STRING))
        table.addRow(value)
        response.getResults() >> [ table ]
        return response
    }

    def buildResultScalar(long value) {
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("scalar", VoltType.BIGINT))
        table.addRow(value)
        response.getResults() >> [ table ]
        return response
    }
    def buildResultString(String value) {
        return buildResultString(value, true, ClientResponse.SUCCESS)
    }
    def buildResultString(String value, boolean row, byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("str", VoltType.STRING))
        if(row)
            table.addRow(value)
        response.getResults() >> [ table ]
        return response
    }

    def buildRequeueAnyZombieWorkItemsResults(boolean row, byte status) {
        def response = Mock(ClientResponse)
        response.getStatus() >> status
        VoltTable table = new VoltTable(
                new VoltTable.ColumnInfo("WorkitemId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("WorkitemWorkingAdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkitemWorkingAdapterId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("WorkitemWorkToBeDone", VoltType.STRING))
        if(row)
            table.addRow(20L, "TYPE", 999L, "ChildDaiMgr")
        response.getResults() >> [ table ]
        return response
    }

    def "Test sub-class AdapterInstanceInfo"() {
        def value = new AdapterDaiMgr.AdapterInstanceInfo("TEST", 0L, "command", "logfile", null, "lctn", 0L)
        expect: value != null
    }

    def "Test sub-class MonitorAdapterInstance"() {
        given:
        AdapterDaiMgr.AdapterInstanceInfo info = new AdapterDaiMgr.AdapterInstanceInfo("", 0L, "", "",
                Mock(Process), "", 0L)
        baseAdapter_.getAdapterInstancesAdapterId(_,_,_) >> instanceId
        baseAdapter_.getAdapterInstancesBaseWorkItemId(_,_) >> baseId
        AdapterDaiMgr.MonitorAdapterInstance instance = new AdapterDaiMgr.MonitorAdapterInstance(info, 0L,
                logger_, baseAdapter_)
        instance.run()

        expect: true

        where:
        instanceId | baseId
        0L         | 0L
        1L         | 0L
        1L         | 1L
    }

    def "Test sub-class MonitorAdapterInstance with exception"() {
        given:
        AdapterDaiMgr.AdapterInstanceInfo info = new AdapterDaiMgr.AdapterInstanceInfo("", 0L, "", "",
                Mock(Process), "", 0L)
        AdapterDaiMgr.MonitorAdapterInstance instance = new AdapterDaiMgr.MonitorAdapterInstance(info, 0L,
                logger_, baseAdapter_)
        baseAdapter_.getAdapterInstancesAdapterId(_,_,_) >> 1L
        baseAdapter_.getAdapterInstancesBaseWorkItemId(_ as String,_ as Long) >> { throw new IOException("TEST") }

        when:
        instance.run()

        then:
        instance.log_ == logger_
    }

    def "Test for keywordSubstitutions"() {
        def str = "\$HOSTNAME \$LCTN \$INSTANCE"
        def replaced = underTest_.keywordSubstitutions(str, "hostname", "location", 42L)

        expect: replaced == "hostname location 42"
    }

    def "Test requeueAnyZombieWorkItems"() {
        client_.callProcedure(_,_) >> buildRequeueAnyZombieWorkItemsResults(row, status)
        underTest_.workQueue = queue
        underTest_.mTimeLastCheckedZombies = System.currentTimeMillis() - time
        expect: underTest_.requeueAnyZombieWorkItems() == result

        where:
        status                 | row   | queue           | time    || result
        ClientResponse.SUCCESS | false | null            |      0L || -1L
        ClientResponse.SUCCESS | false | null            | 121000L ||  0L
        ClientResponse.SUCCESS | true  | workQueue       | 121000L ||  1L
    }

    def "Test requeueAnyZombieWorkItems bad db status"() {
        given:
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure(_,_) >> response

        when: underTest_.requeueAnyZombieWorkItems()

        then: thrown(RuntimeException)
    }

    def "Test of isDaiMgrRunningOnThisSn"() {
        client_.callProcedure(_,_,_,_) >> buildResultString("row", row, status)
        expect: underTest_.isDaiMgrRunningOnThisSn() == result

        where:
        status                 | row   || result
        ClientResponse.SUCCESS | false || false
        ClientResponse.SUCCESS | true  || true
    }

    def "Test isDaiMgrRunningOnThisSn bad db status"() {
        given:
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure(_,_,_,_) >> response

        when: underTest_.isDaiMgrRunningOnThisSn()

        then: thrown(RuntimeException)
    }

    def "Test of isThereFreeAdapterInstance"() {
        client_.callProcedure("AdapterInfoUsingTypeLctnPid",_,_,_ as Long) >>
                buildResultString("row", row1, ClientResponse.SUCCESS)
        client_.callProcedure("WorkItemInfoNonBaseworkUsingAdaptertypeQueueState",_,_,_ as String) >>
                buildResultString("row", row2, ClientResponse.SUCCESS)
        expect: underTest_.isThereFreeAdapterInstance("","") == result

        where:
        status                 | row1  | row2  || result
        ClientResponse.SUCCESS | false | false || false
        ClientResponse.SUCCESS | true  | true  || false
        ClientResponse.SUCCESS | true  | false || true
    }

    def "Test isThereFreeAdapterInstance bad db status"() {
        given:
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure(_,_,_,_) >> response

        when: underTest_.isThereFreeAdapterInstance("", "")

        then: thrown(RuntimeException)
    }

    def "Test isThereFreeAdapterInstance bad db status on second call"() {
        given:
        client_.callProcedure(_,_,_,_ as Long) >> buildResultString("row", true, ClientResponse.SUCCESS)
        client_.callProcedure(_,_,_,_ as String) >> buildResultString("row", false, ClientResponse.OPERATIONAL_FAILURE)

        when: underTest_.isThereFreeAdapterInstance("", "")

        then: thrown(RuntimeException)
    }

    def "Test of startAdditionalAdapterInstance"() {
        def response = Mock(ClientResponse)
        def response2 = Mock(ClientResponse)
        def response3 = Mock(ClientResponse)
        def response4 = Mock(ClientResponse)
        def response5 = Mock(ClientResponse)
        def response6 = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        response2.getStatus() >> ClientResponse.SUCCESS
        response3.getStatus() >> ClientResponse.SUCCESS
        response4.getStatus() >> ClientResponse.SUCCESS
        response5.getStatus() >> ClientResponse.SUCCESS
        response6.getStatus() >> ClientResponse.SUCCESS
        client_.callProcedure("AdapterInfoUsingTypeLctnPid",_,_,_ as Long) >> response
        client_.callProcedure("WorkItemInfoNonBaseworkUsingAdaptertypeQueueState",_,_,_ as String) >> response2
        client_.callProcedure("MachineAdapterInvocationInformation", _, _) >> response3
        client_.callProcedure("AdapterInfoUsingTypeLctnPid",_,_,_ as Long) >> response4
        client_.callProcedure("WorkItemInfoNonBaseworkUsingAdaptertypeQueueState",_,_,_ as String) >> response5
        client_.callProcedure("MachineAdapterInstanceBumpNextInstanceNumAndReturn",_ as String, _ as String) >>
                response6
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.STRING))
        if(row1)
            table.addRow("row")
        VoltTable table2 = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.STRING))
        if(row2)
            table2.addRow("row")
        VoltTable table3 = new VoltTable(
                new VoltTable.ColumnInfo("AdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("SnLctn", VoltType.STRING),
                new VoltTable.ColumnInfo("Invocation", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        table3.addRow("TYPE", "location", "invocation", "file")
        VoltTable table4 = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.STRING))
        if(row3)
            table4.addRow("row")
        VoltTable table5 = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.STRING))
        VoltTable table6 = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.BIGINT))
        table6.addRow(1042L)
        response.getResults() >> [ table ]
        response2.getResults() >> [ table2 ]
        response3.getResults() >> [ table3 ]
        response4.getResults() >> [ table4 ]
        response5.getResults() >> [ table5 ]
        response6.getResults() >> [ table6 ]
        location_ = "notMe"
        expect: underTest_.startAdditionalAdapterInstance("","") == result

        where:
        status                 | row1  | row2  | row3  || result
        ClientResponse.SUCCESS | false | false | false || -1
        ClientResponse.SUCCESS | true  | false | true  || -2
        ClientResponse.SUCCESS | false | true  | true  || -1
        ClientResponse.SUCCESS | true  | true  | false ||  0
    }

    def "Test startAdditionalAdapterInstance bad db status"() {
        given:
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure("MachineAdapterInvocationInformation" as String, _, _) >> response

        when: underTest_.startAdditionalAdapterInstance("", "")

        then: thrown(RuntimeException)
    }

    def "Test performAdapterInstanceLoadBalancing"() {
        def response = Mock(ClientResponse)
        def response2 = Mock(ClientResponse)
        def response3 = Mock(ClientResponse)
        def response4 = Mock(ClientResponse)
        def response5 = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        response2.getStatus() >> ClientResponse.SUCCESS
        response3.getStatus() >> ClientResponse.SUCCESS
        response4.getStatus() >> ClientResponse.SUCCESS
        response5.getStatus() >> ClientResponse.SUCCESS
        VoltTable table = new VoltTable(
                new VoltTable.ColumnInfo("WorkingAdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Queue", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkToBeDone", VoltType.STRING)
        )
        if(row)
            table.addRow("TYPE", 0L, "queue", "Q", "work")
        VoltTable table2 = new VoltTable(
                new VoltTable.ColumnInfo("AdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("SnLctn", VoltType.STRING),
                new VoltTable.ColumnInfo("Invocation", VoltType.STRING),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING)
        )
        table2.addRow("TYPE", "location", "invocation", "file")
        VoltTable table3 = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.STRING))
        if(row)
            table3.addRow("row")
        VoltTable tableSimple = new VoltTable(new VoltTable.ColumnInfo("one", VoltType.STRING))
        tableSimple.addRow("row")

        response.getResults() >> [ table ]
        response2.getResults() >> [ table2 ]
        response3.getResults() >> [ table3 ]
        response4.getResults() >> [ tableSimple ]
        response5.getResults() >> [ tableSimple ]
        client_.callProcedure("WorkItemBackLog", _ as Long) >> response
        client_.callProcedure("MachineAdapterInvocationInformation", _ as String, _ as String) >> response2
        client_.callProcedure("AdapterInfoUsingTypeLctnPid", _ as String, _ as String, _ as Long) >> response4
        client_.callProcedure("WorkItemInfoNonBaseworkUsingAdaptertypeQueueState", _ as String, _ as String, _ as String) >> response5
        underTest_.BacklogChkInterval = interval
        location_ = "notMe"
        expect: underTest_.performAdapterInstanceLoadBalancing() == result
        where:
        interval       | row   || result
        Long.MAX_VALUE | false || -1
        Long.MIN_VALUE | false ||  0
        Long.MIN_VALUE | true  ||  1
    }

    def "Test ensureChildWrkItemsMakingProgress bad db status"() {
        given:
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        underTest_.mProgChkIntrvlDataReceiver = 0L
        client_.callProcedure("WorkItemInfoWrkadaptrtypeQueueWorktobddone", _, _, _) >> response

        when: underTest_.ensureChildWrkItemsMakingProgress("location")

        then: thrown(RuntimeException)
    }

    def "Test ensureChildWrkItemsMakingProgress"() {
        def response = Mock(ClientResponse)
        underTest_.mProgChkIntrvlDataReceiver = check
        response.getStatus() >> ClientResponse.SUCCESS
        client_.callProcedure("WorkItemInfoWrkadaptrtypeQueueWorktobddone", _, _, _) >> response
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("WorkingResults", VoltType.STRING))
        table.addRow("")
        response.getResults() >> [ table ]

        expect: underTest_.ensureChildWrkItemsMakingProgress("location") == result

        where:
        check          | row   || result
        Long.MAX_VALUE | false || 0L
        0L             | true  || 0L
        0L             | false || 0L
    }

    def "Test ensureDaiMgrsStillActive"() {
        underTest_.mTimeLastCheckedDaiMgrsActive = time
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        VoltTable table = new VoltTable(
                new VoltTable.ColumnInfo("Queue", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingAdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkToBeDone", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingResults", VoltType.STRING)
        )
        if(row)
            table.addRow("queue", "TYPE", 1042L, "Q", "work", "results")
        response.getResults() >> [ table ]
        client_.callProcedure("WorkItemsForSmwAndSsnDaimgrs", _ as String) >> response
        expect: underTest_.ensureDaiMgrsStillActive() == result

        where:
        time           | row   || result
        Long.MAX_VALUE | false || -1L
        0L             | false ||  0L
        0L             | true  ||  0L
    }

    def "Test ensureDaiMgrsStillActive with exception"() {
        given:
        underTest_.mTimeLastCheckedDaiMgrsActive = 0L
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure("WorkItemsForSmwAndSsnDaimgrs", _ as String) >> response

        when: underTest_.ensureDaiMgrsStillActive()

        then: thrown(RuntimeException)
    }

    def "Test determineInitialNodeStates with exception"() {
        given:
        def response = Mock(ClientResponse)
        def response2 = Mock(ClientResponse)
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("count", VoltType.BIGINT))
        table.addRow(1000L)
        response.getResults() >> [ table ]
        client_.callProcedure(_ as String) >> response
        response2.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure(_ as String, _ as String) >> response2
        underTest_.mAlreadyChkdInitialNodeStates = false
        when: underTest_.determineInitialNodeStates("")

        then: thrown(RuntimeException)
    }

    def "Test cleanupStaleAdapterInstancesOnThisServiceNode with exception"() {
        given:
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure(_ as String, _ as String) >> response

        when: underTest_.cleanupStaleAdapterInstancesOnThisServiceNode("")

        then: thrown(RuntimeException)
    }

    def "Test startupAdapterInstancesForThisSn with exception"() {
        given:
        underTest_.mHaveStartedAdapterInstancesForThisSn = false
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.OPERATIONAL_FAILURE
        client_.callProcedure(_ as String, _ as String) >> response

        when: underTest_.startupAdapterInstancesForThisSn("", "", 0L)

        then: thrown(RuntimeException)
    }

    def "Test proofOfLife"() {
        given:
        underTest_.mTimeLastProvedAlive = 0L
        def response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        VoltTable table = new VoltTable(new VoltTable.ColumnInfo("State", VoltType.STRING))
        if(row)
            table.addRow(state)
        response.getResults() >> [ table ]
        client_.callProcedure(_ as String, _ as String, _ as Long) >> response
        when: underTest_.proofOfLife()

        then: thrown(RuntimeException)

        where:
        row   | state || result
        false | "A"   || true
        true  | "A"   || true
        true  | "Q"   || true
    }

    def "Test handleMotherSuperiorDaiMgr with exception"() {
        given:

        when: underTest_.handleMotherSuperiorDaiMgr("", "")

        then: thrown(RuntimeException)
    }

    def "Test handleChildDaiMgr with exception"() {
        given:

        when: underTest_.handleChildDaiMgr("", "")

        then: thrown(RuntimeException)
    }

    def "Test getProcessPid"() {
        def process = Mock(Process)

        expect: AdapterDaiMgr.getProcessPid(process) == -1
    }

    def "Test connectRetryPhase with exception"() {
        given:
        underTest_.quickClient_ = Mock(Client)
        when: underTest_.connectRetryPhase(new String[1], 0L, 0L)

        then: thrown(TimeoutException)
    }

    def "Test waitForVoltDB"() {
        underTest_.quickClient_ = client_
        def status = Mock(DbStatusApi)
        status.waitForDataPopulated(_) >> true
        factory_.createDbStatusApi(_ as Client) >> status
        underTest_.quickClient_ = client_
        underTest_.waitForVoltDB("localhost", 1000L, 100L)
        expect: true
    }

    def "Test waitForVoltDB with exception"() {
        given:
        def status = Mock(DbStatusApi)
        status.waitForDataPopulated(_) >> false
        factory_.createDbStatusApi(_) >> status

        when: underTest_.waitForVoltDB("localhost", 0L, 0L)

        then: thrown(TimeoutException)
    }

    def "Test waitForVoltDB with exception 2"() {
        given:
        underTest_.quickClient_ = client_
        def status = Mock(DbStatusApi)
        status.waitForDataPopulated(_) >> false
        factory_.createDbStatusApi(_) >> { throw new DataStoreException("TEST") }

        when: underTest_.waitForVoltDB("localhost", 0L, 0L)

        then: thrown(TimeoutException)
    }

    def "Test monitorAdapterInstance"() {
        underTest_.monitorAdapterInstance(null, 0)
        expect: true
    }

    def "Test mainProcessingFlow"() {
        def status = Mock(DbStatusApi)
        def locationApi = Mock(Location)
        def workQueue = Mock(WorkQueue)
        locationApi.getLocationFromHostname(_) >> location
        factory_.createLocation(_) >> locationApi
        status.waitForDataPopulated(_) >> true
        factory_.createDbStatusApi(_) >> status
        factory_.createWorkQueue(_) >> workQueue
        underTest_.quickClient_ = client_
        workQueue.grabNextAvailWorkItem(_) >>> [ true, true, true, true, false ]
        workQueue.workToBeDone() >> work
        def params = new String[3]
        params[0] = ""
        params[1] = ""
        params[2] = ""
        workQueue.getClientParameters(_) >> params
        def realArgs
        if(useArgs) {
            realArgs = new String[3]
            realArgs[0] = "localhost"
            realArgs[1] = "location"
            realArgs[2] = "hostname"
        } else
            realArgs = new String[0]
        client_.callProcedure("UCSCONFIGVALUE.select", _ as String) >>>
                [ buildUcsVarResult("/tmp"), buildUcsVarResult("test.log") ]
        client_.callProcedure("@AdHoc", _ as String) >>> [ buildResultScalar(System.currentTimeMillis() - time),
                                                           buildResultScalar(System.currentTimeMillis() + 999L) ]
        client_.callProcedure("MachineAreWeUsingSynthesizedData", "1") >> buildResultString(fake)

        client_.callProcedure("WorkItemRequeueZombies", "TYPE") >>
                buildRequeueAnyZombieWorkItemsResults(true, ClientResponse.SUCCESS)
        baseAdapter_.adapterShuttingDown() >>> [ false, false, true ]
        underTest_.mainProcessingFlow(realArgs)

        expect: result
        where:
        useArgs | location   | time   | fake | work                                  || result
        true    | "location" | 0L     | "N"  | "StartAdditionalChildAdapterInstance" || true
        true    | "location" | 0L     | "N"  | "MotherSuperiorDaiMgr"                || true
        true    | "location" | 0L     | "N"  | "ChildDaiMgr"                         || true
        true    | "location" | 0L     | "N"  | "other"                               || true
        true    | "location" | 5000L  | "Y"  | "other"                               || true
        false   | " "        | 0L     | "N"  | "other"                               || true
        false   | null       | 0L     | "N"  | "other"                               || true
    }

    def "Test ensureNotZombie"() {
        ClientResponse response1 = Mock(ClientResponse)
        response1.getStatus() >> ClientResponse.SUCCESS
        VoltTable vt = new VoltTable(new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingAdapterId", VoltType.BIGINT))
        vt.addRow("A", 0L)
        response1.getResults() >> [ vt ]
        client_.callProcedure(_ as String, _ as String, _ as Long) >> response1
        when: underTest_.ensureNotZombie()
        then: thrown(RuntimeException)
    }

    def "Test checkNodesStuckShuttingDown"() {
        ClientResponse response1 = Mock(ClientResponse)
        response1.getStatus() >> ClientResponse.SUCCESS
        VoltTable vt = new VoltTable(new VoltTable.ColumnInfo("WorkingAdapterId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("DbUpdatedTimestamp", VoltType.TIMESTAMP)
        )
        vt.addRow(0L, "location", "A", 0L)
        response1.getResults() >> [ vt ]
        client_.callProcedure(_ as String, _ as String) >> response1
        underTest_.mTimeLastCheckedNodesStuckShuttingDown = 0L
        expect: underTest_.checkNodesStuckShuttingDown() == 0L
    }

    def "Test cleanupAdapterInstancesWithoutActivePidOnThisServiceNode"() {
        ClientResponse response1 = Mock(ClientResponse)
        response1.getStatus() >> ClientResponse.SUCCESS
        VoltTable vt = new VoltTable(new VoltTable.ColumnInfo("AdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Pid", VoltType.BIGINT)
        )
        vt.addRow("location", 0L, 0L)
        response1.getResults() >> [ vt ]
        client_.callProcedure(_ as String, _ as String) >> response1
        expect: underTest_.cleanupAdapterInstancesWithoutActivePidOnThisServiceNode("location") == 1L
    }

    def "Test ensureNodeConsoleMsgsFlowingIntoDai"() {
        ClientResponse response1 = Mock(ClientResponse)
        response1.getStatus() >> ClientResponse.SUCCESS
        VoltTable vt = new VoltTable(new VoltTable.ColumnInfo("ProofOfLifeTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING)
        )
        vt.addRow(0L, "location", "A")
        response1.getResults() >> [ vt ]
        client_.callProcedure(_ as String, _ as String) >> response1
        underTest_.mTimeLastCheckedNodesMissingConsoleMsgs = 0L
        expect: underTest_.ensureNodeConsoleMsgsFlowingIntoDai() == 0L
    }

    def "Test cleanupStaleAdapterInstancesOnThisServiceNode"() {
        ClientResponse response1 = Mock(ClientResponse)
        response1.getStatus() >> ClientResponse.SUCCESS
        VoltTable vt = new VoltTable(new VoltTable.ColumnInfo("AdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Pid", VoltType.BIGINT)
        )
        vt.addRow("location", 0L, 0L)
        response1.getResults() >> [ vt ]
        client_.callProcedure(_ as String, _ as String) >> response1
        expect: underTest_.cleanupStaleAdapterInstancesOnThisServiceNode("location") == 1L
    }
}
