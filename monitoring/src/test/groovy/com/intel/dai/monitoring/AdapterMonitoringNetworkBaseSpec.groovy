// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.config_io.ConfigIOParseException
import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.network_listener.NetworkListenerCore
import com.intel.logging.Logger
import spock.lang.Specification

class AdapterMonitoringNetworkBaseSpec extends Specification {
    static class MockUnderTest extends AdapterMonitoringNetworkBase {
        MockUnderTest(Logger logger, DataStoreFactory factory, AdapterInformation info, String benchmarkingFile, long maxBurstSeconds) {
            super(logger, factory, info, benchmarkingFile, maxBurstSeconds)
        }
    }

    Logger logger_
    DataStoreFactory factory_
    AdapterInformation info_
    def underTest_
    void setup() {
        logger_ = Mock(Logger)
        factory_ = Mock(DataStoreFactory)
        info_ = new AdapterInformation("TEST", "TEST", "location", "hostname", -1L)
        underTest_ = new MockUnderTest(logger_, factory_, info_, "./build/tmp/bmf.txt", 100L)
    }

    def "Test getConfigStream"() {
        when: underTest_.getConfigStream("unknownFile.json")
        then: thrown(FileNotFoundException)
    }

    def "Test execute"() {
        def core = Mock(NetworkListenerCore)
        core.run() >>> [0, 1]
        expect: underTest_.execute(core)
        and: !underTest_.execute(core)
    }

    def "Test entryPoint"() {
        when: underTest_.entryPoint(new ByteArrayInputStream())
        then: thrown(ConfigIOParseException)
    }
}
