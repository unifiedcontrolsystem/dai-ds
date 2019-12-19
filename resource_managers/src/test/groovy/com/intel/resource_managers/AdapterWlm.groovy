package com.intel.resource_managers

import com.intel.resource_managers.cobalt.AdapterWlmCobalt;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.logging.Logger;
import com.intel.dai.dsapi.*;
import com.intel.dai.AdapterInformation;
import spock.lang.Specification
import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.IAdapter;
import com.intel.dai.result.Result;

class AdapterWlmSpec extends Specification {

    def underTest_

    def setup() {


        def jobs_ = Mock(Jobs)
        jobs_.checkStaleDataInternal(_) >> new HashMap<Long, String>() {
            {
                put(1, "stale1"); put(2, "stale2")
            }
        }
        def eventlog_ = Mock(EventsLog)
        def raseventlog_ = Mock(RasEventLog)
        def workQueue_ = Mock(WorkQueue)
        workQueue_.grabNextAvailWorkItem() >> true
        workQueue_.getClientParameters() >> new HashMap<String, String>()
        workQueue_.workToBeDone() >> "HandleInputFromExternalComponent"
        workQueue_.amtTimeToWait() >> 0
        def baseadapter_ = Mock(AdapterInformation)
        baseadapter_.isShuttingDown() >>> [false, true]
        baseadapter_.getType() >>> "WLM"
        baseadapter_.getName() >>> "AdapterWlm"
        def provider_ = Mock(WlmProvider)
        provider_.handleInputFromExternalComponent(_) >> 0
        def factory = Mock(DataStoreFactory)
        factory.createEventsLog(_, _) >> eventlog_
        factory.createRasEventLog(_) >> raseventlog_
        factory.createWorkQueue(_) >> workQueue_
        factory.createJobApi() >> jobs_
        def log_ = Mock(Logger)
        def adapter_ = Mock(IAdapter)
        adapter_.adapterAbnormalShutdown() >> false
        AdapterSingletonFactory.getAdapter() >> adapter_

        underTest_ = new AdapterWlm(baseadapter_, provider_, factory, log_)
    }

    def "Test finishedWorkingOnWorkItem rc0"() {
        def result = new Result(0, "Success");

        expect:
        underTest_.finishedWorkingOnWorkItem(result)
    }

    def "Test finishedWorkingOnWorkItem rc1"() {
        def result = new Result(1, "Error");

        expect:
        underTest_.finishedWorkingOnWorkItem(result)
    }

    def "Test cleanupAnyStaleDataInTables"() {

        expect:
        underTest_.cleanupAnyStaleDataInTables() == 2
    }

    def "Test cleanupAnyStaleDataInTables None"() {

        def jobs_ = Mock(Jobs)
        underTest_.jobs = jobs_
        jobs_.checkStaleDataInternal(_) >> null

        expect:
        underTest_.cleanupAnyStaleDataInTables() == 0
    }

    def "Test mainProcessingFlow"() {

        expect:
        underTest_.mainProcessingFlow() == 0
    }

    def "Test run"() {

        underTest_.baseAdapter.signalToShutdown()

        expect:
        underTest_.run() == 0
    }

}
