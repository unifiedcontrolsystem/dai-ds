package com.intel.dai.network_listener

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.logging.Logger
import spock.lang.Specification

class NetworkListenerSystemActionsSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new NetworkListenerSystemActions(Mock(Logger), Mock(DataStoreFactory), Mock(AdapterInformation),
                Mock(NetworkListenerConfig))
    }

    def "upsertHWInventory"() {
        when: underTest_.upsertHWInventory(null)
        then: notThrown Exception
    }

    def "ingestCanonicalHWInvJson"() {
        expect: underTest_.ingestCanonicalHWInvJson(null) == null
    }

    def "toCanonicalHWInvJson"() {
        expect: underTest_.toCanonicalHWInvJson(null) == null
    }

    def "getForeignHWInvJson"() {
        expect: underTest_.getForeignHWInvJson(null) == null
    }
}
