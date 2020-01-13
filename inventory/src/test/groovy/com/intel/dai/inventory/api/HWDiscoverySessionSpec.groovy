package com.intel.dai.inventory.api

import spock.lang.Specification

class HWDiscoverySessionSpec extends Specification {
    def "ToString" () {
        expect:
        new HWDiscoverySession().toString() == new HWDiscoverySession().toString()
        new ProviderClassMap().toString() == new ProviderClassMap().toString()
        new TokenAuthProvider().toString() == new TokenAuthProvider().toString()
        new ProviderConfigurations().toString() == new ProviderConfigurations().toString()
        new Requester().toString() == new Requester().toString()
        new RestRequest().toString() == new RestRequest().toString()
    }
}
