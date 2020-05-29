package com.intel.perflogging

import spock.lang.Specification

class BenchmarkHelperSpec extends Specification {
    final static String FILENAME = "./build/tmp/benchmarking-{{TEST}}.json"
    def underTest_

    void setup() {
        underTest_ = new BenchmarkHelper("Testing", FILENAME, 1)
    }

    void cleanup() {
        File f = new File(FILENAME)
        f.delete()
    }

    def "Test changeDataSetName No Benchmarking"() {
        underTest_.changeDataSetName("changedName")
        expect:
        underTest_.dataSetName_ == "Testing"
    }

    def "Test changeDataSetName"() {
        underTest_.doBenchmarking_ = true
        underTest_.changeDataSetName("changedName")
        expect:
        underTest_.dataSetName_ == "changedName"
    }

    def "Test AddDefaultValue No Benchmarking"() {
        underTest_.addDefaultValue(10)
        expect:
        underTest_.defaultValue_.get() == 0L
    }

    def "Test AddNamedValue No Benchmarking"() {
        underTest_.addNamedValue("TestName", 10)
        expect:
        underTest_.values_.size() == 0
    }

    def "Test Tick No Benchmarking"() {
        underTest_.tick()
        expect: true
    }

    def "Test AddDefaultValue Benchmarking"() {
        underTest_.doBenchmarking_ = true
        underTest_.addDefaultValue(10)
        expect:
        underTest_.defaultValue_.get() == 10L
    }

    def "Test AddNamedValue Benchmarking"() {
        underTest_.doBenchmarking_ = true
        underTest_.addNamedValue("TestName", 10)
        underTest_.addNamedValue("TestName2", 20)
        expect:
        underTest_.values_.size() == 2
    }

    def "Test Tick Benchmarking"() {
        underTest_.doBenchmarking_ = true
        underTest_.addDefaultValue(10)
        underTest_.tick()
        expect:
        underTest_.values_.size() == 0
    }

    def "Test Recording Data"() {
        underTest_.doBenchmarking_ = true
        underTest_.addDefaultValue(10)
        underTest_.addNamedValue("Testing", 20)
        underTest_.lastTs_.set(0L)
        underTest_.recordAndReset()
        expect:
        underTest_.values_.size() == 0
    }

    def "Test replaceFilenameVariable"() {
        underTest_.replaceFilenameVariable("TEST", "Red")
        expect: underTest_.file_.toString() == "./build/tmp/benchmarking-Red.json"
    }

    def "Test replaceFilenameVariable Negative 1"() {
        when: underTest_.replaceFilenameVariable(VALUE1, VALUE2)
        then: thrown(NullPointerException)
        where:
        VALUE1  | VALUE2
        "TEST"  | null
        "TEST"  | "  \n"
        null    | null
        "  \t"  | null
    }
}
