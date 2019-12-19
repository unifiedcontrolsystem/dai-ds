package com.intel.dai

import com.intel.logging.Logger
import spock.lang.Specification

class AdapterSingletonFactorySpec extends Specification {
    def "setAdapter"() {
        def adapter = Mock(IAdapter)
        AdapterSingletonFactory.setAdapter(adapter)
        expect: AdapterSingletonFactory.adapter_ == adapter
    }
}
class AdapterInformationSpec extends Specification {
    def "AdapterInformation"() {
        def adapter = Mock(IAdapter)
        def logger = Mock(Logger)
        def ts = new AdapterInformation("type", "name", "location", "hostname")

        expect: ts != null
    }
}