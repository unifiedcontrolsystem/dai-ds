package com.intel.dai.resource_managers.pbs


import com.intel.logging.Logger;
import com.intel.dai.dsapi.*;
import com.intel.dai.AdapterInformation;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIO;
import spock.lang.Specification;
import com.intel.properties.PropertyDocument;
import com.intel.xdg.XdgConfigFile;

class AdapterWlmPBSSpec extends Specification {

    def underTest_
    def setup() {

        def log_ = Mock(Logger)
        def jobs_ = Mock(Jobs)
        jobs_.startJobInternal(_,_) >> new HashMap<String,Object>() {{ put("WlmJobStarted","T"); put("WlmJobCompleted","T")}}
        jobs_.completeJobInternal(_,_) >> new HashMap<String,Object>() {{ put("WlmJobStarted","T"); put("WlmJobCompleted","T")}}
        def eventlog_ = Mock(EventsLog)
        def raseventlog_ = Mock(RasEventLog)
        def workQueue_ = Mock(WorkQueue)
        def nodeinfo_ = Mock(NodeInformation)
        nodeinfo_.getComputeNodeLocationFromHostnameMap() >> new HashMap<String,String>() {{ put("hostname", "location") }}
        nodeinfo_.getComputeNodeSequenceNumberFromLocationMap() >> new HashMap<String,Long>() {{ put("location", new Long(300l)) }}
        def factory = Mock(DataStoreFactory)
        factory.createJobApi() >> jobs_
        factory.createEventsLog(_,_) >> eventlog_
        factory.createRasEventLog(_) >> raseventlog_
        factory.createWorkQueue(_) >> workQueue_
        factory.createNodeInformation() >> nodeinfo_
        def adapter_ = new AdapterInformation("WLM", AdapterWlmPBS.class.getSimpleName(), "location", "localhost", -1L)

        underTest_ = new AdapterWlmPBS(log_, adapter_, factory)
    }

    def "Test handlePBSJobMessages JobStarted"() {
        def jobStarted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Running\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        def jsonParser = ConfigIOFactory.getInstance("json");
        def jsonMessage = jsonParser.fromString(jobStarted).getAsMap();
        underTest_.handlePBSJobMessages(jobStarted, jsonMessage)
        expect: true
    }

    def "Test handlePBSJobMessages JobCompletion"() {
        def jobCompleted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Completed\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        def jsonParser = ConfigIOFactory.getInstance("json");
        def jsonMessage = jsonParser.fromString(jobCompleted).getAsMap();
        underTest_.handlePBSJobMessages(jobCompleted, jsonMessage)
        expect: true
    }

    def "Test handlePBSJobMessages IllegalMessage"() {
        def jobStarted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Illegal\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        def jsonParser = ConfigIOFactory.getInstance("json");
        def jsonMessage = jsonParser.fromString(jobStarted).getAsMap();
        underTest_.handlePBSJobMessages(jobStarted, jsonMessage)
        expect: true
    }

    def "Test waitUntilFinishedProcessingMessages"() {
        underTest_.shutDown()
        underTest_.waitUntilFinishedProcessingMessages()
        expect: true
    }

    def "Test getBitSetOfPBSNodes"() {
        BitSet bsNodes = new BitSet(12800)
        bsNodes.set(300)

        expect: underTest_.getBitSetOfPBSNodes("hostname", "327220", 1610580758534L, true) == bsNodes;
    }

    def "Test getBitSetOfPBSNodes No Register"() {
        BitSet bsNodes = new BitSet(12800)
        bsNodes.set(300)

        expect: underTest_.getBitSetOfPBSNodes("hostname", "327220", 1610580758534L, false) == bsNodes;
    }

    def "Test getBitSetOfPBSNodes No Seq"() {
        BitSet bsNodes = new BitSet(12800)
        def nodeinfo_ = Mock(NodeInformation)
        nodeinfo_.getComputeNodeLocationFromHostnameMap() >> new HashMap<String,String>() {{ put("hostname", "location") }}
        nodeinfo_.getComputeNodeSequenceNumberFromLocationMap() >> new HashMap<String,Long>() {{ put("location2", new Long(300l)) }}
        underTest_.nodeinfo = nodeinfo_

        expect: underTest_.getBitSetOfPBSNodes("hostname", "327220", 1610580758534L, false) == bsNodes;
    }

    def "Test assocJobIdAndNodeInCachedJobsTable"() {
        String sNodeList = "location"
        ArrayList<String> alNodes = new ArrayList<String>(Arrays.asList(sNodeList.replaceAll("\"","").split(",")));
        ArrayList<String> alNodeLctns = new ArrayList<String>(alNodes.size());
        String[] aTempNodeLctns = new String[alNodeLctns.size()];
        aTempNodeLctns = alNodeLctns.toArray(aTempNodeLctns);
        underTest_.assocJobIdAndNodeInCachedJobsTable("327220", 1610580758534L, aTempNodeLctns)
        expect: true
    }

    def "Test handleJobStartedMsg"() {
        def jobStarted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Running\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        def jsonParser = ConfigIOFactory.getInstance("json");
        def jsonMessage = jsonParser.fromString(jobStarted).getAsMap();
        underTest_.handleJobStartedMsg(jsonMessage)
        expect: true
    }

    def "Test handleJobCompletionMsg"() {
        def jobCompleted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Completed\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        def jsonParser = ConfigIOFactory.getInstance("json");
        def jsonMessage = jsonParser.fromString(jobCompleted).getAsMap();
        underTest_.handleJobCompletionMsg(jsonMessage)
        expect: true
    }

    def "Test handleEndOfJobProcessing"() {
        long   startTime  = 1581103966000L;
        long   endTime    = 1581104966000L;
        HashMap<String, Object> jobInfo = new HashMap<String,Object>() {{ put("WlmJobStarted","T"); put("WlmJobCompleted","T"); put("WlmJobWorkDir","/tmp");
            put("WlmJobState","T"); put("WlmJobEndTime", startTime); put("WlmJobStartTime", endTime)}}
        underTest_.handleEndOfJobProcessing("327220", jobInfo, "0")
        expect: true
    }

    def "Test handleEndOfJobProcessing Null"() {
        underTest_.handleEndOfJobProcessing("327220", null, "0")
        expect: true
    }

    def "Test shutdown"() {
        underTest_.shutDown()
        expect: true
    }

    def "Test processSinkMessage JobStarted"() {
        def jobStarted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Running\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        underTest_.processSinkMessage("pbs_runjobs", jobStarted)
        expect: true
    }

    def "Test processSinkMessage JobCompletion"() {
        def jobCompleted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Completed\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        underTest_.processSinkMessage("pbs_runjobs", jobCompleted)
        expect: true
    }

    def "Test processSinkMessage error"() {
        def jobCompleted = "{  \"job_energy\": {    \"string\": \"n/a\"  },  \"job_minpwr\": {    \"string\": \"n/a\"  },  \"timestamp\": 1610580758534,  \"job_state\": \"Job Completed\",  \"host\": \"pbs\",  \"job_mem\": \"0kb\",  \"job_cput\": \"00:00:00\",  \"job_avepwr\": {    \"string\": \"n/a\"  },  \"job_cpupercent\": 0,  \"job_instpwr\": {    \"string\": \"n/a\"  },  \"job_vmem\": \"0kb\",  \"job_ncpus\": 1,  \"job_maxpwr\": {    \"string\": \"n/a\"  },  \"job_walltime\": \"00:00:00\",  \"job_name\": \"helloworld\",  \"job_id\": \"54.pbs\",  \"user\": \"root\",  \"exit_status\": \"0\",  \"work_directory\":\"/home\"}"
        underTest_.processSinkMessage("error", jobCompleted)
        expect: true
    }

    def "Test processSinkMessage exception"() {
        underTest_.processSinkMessage("subject", null)
        expect: true
    }

    def "Test handleInputFromExternalComponent"() {
        underTest_.shutDown()

        String[] args = new String[1]
        def jsonparser_ = Mock(ConfigIO)
        def configJson = Mock(PropertyDocument)
        configJson.getAsMap() >>  new HashMap<String,Object>() {{ put("bootstrap.servers","admin:9092"); put("group.id","pbs_runjobs");
            put("schema.registry.url","http://admin:8081"); put("auto.commit.enable","true");
            put("auto.offset.reset","earliest"); put("specific.avro.reader","true");
            put("topics","pbs_runjobs")}}
        jsonparser_.readConfig(_) >> configJson
        underTest_.jsonParser = jsonparser_
        def stream = Mock(InputStream)
        def xdg_ = Mock(XdgConfigFile)
        xdg_.Open(_) >> stream
        underTest_.xdg = xdg_

        expect: underTest_.handleInputFromExternalComponent(args) == 0
    }

    def "Test handleInputFromExternalComponent PropertyNotExpectedType"() {
        underTest_.shutDown()

        String[] args = new String[1]
        def jsonparser_ = Mock(ConfigIO)
        def configJson = Mock(PropertyDocument)
        configJson.getAsMap() >>  new HashMap<String,Object>() {{ put("bootstrap.servers","admin:9092")}}
        jsonparser_.readConfig(_) >> configJson
        underTest_.jsonParser = jsonparser_

        expect: underTest_.handleInputFromExternalComponent(args) == 1
    }

    def "Test main nullargs"() {

        when: underTest_.main(null)

        then: thrown RuntimeException
    }

    def "Test main 2args"() {

        String[] args = new String[2];

        when: underTest_.main(args)

        then: thrown RuntimeException
    }

}
