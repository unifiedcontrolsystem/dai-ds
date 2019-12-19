package com.intel.dai.builder

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.properties.PropertyMap
import spock.lang.Specification

class ForeignSubscriptionRequestSpec extends Specification {
    def "Test BuildRequest"() {
        ForeignSubscriptionRequest builder = new ForeignSubscriptionRequest()
        def subjects = [ "stateChanges" ]
        String post = builder.buildRequest(subjects, "test_id", "http://127.0.0.1:54321/callback")
        ConfigIO parser = ConfigIOFactory.getInstance("json")
        PropertyMap map = parser.fromString(post).getAsMap()
        expect: map.getString("Url") == "http://127.0.0.1:54321/callback"
        and:    map.getString("Subscriber") == "test_id"
        and:    map.getArray("Roles").size() == 5
        and:    map.getArray("SoftwareStatus").size() == 3
        and:    map.getArray("States").size() == 10
    }
}
