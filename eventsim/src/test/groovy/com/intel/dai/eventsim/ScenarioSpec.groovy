package com.intel.dai.eventsim

import spock.lang.Specification

class ScenarioSpec extends Specification {

    def "set scenario config file with null data"() {
        Scenario scenarioTest = new Scenario()
        when:
        scenarioTest.setScenarioToGenerateEventsPath(null)
        then:
        def e = thrown(SimulatorException)
        e.message == "scenario file location data is null."
    }

    def "set scenario config file before process data"() {
        Scenario scenarioTest = new Scenario()
        when:
        scenarioTest.processEventsScenarioFile()
        then:
        def e = thrown(SimulatorException)
        e.message == "set scenario file location before processing data."
    }

    def "validate scenario config file data"() {
        Scenario scenarioTest = new Scenario()
        when:
        scenarioTest.validateScenarioFileData(null)
        then:
        def e = thrown(SimulatorException)
        e.message == "Scenario configuration data is empty/null."
    }

}
