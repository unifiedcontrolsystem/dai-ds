package com.intel.dai.eventsim

import com.intel.logging.Logger
import com.intel.networking.restclient.RESTClientException
import com.intel.properties.PropertyMap
import spock.lang.Specification

class CallBackNetworkSpec extends Specification {

    void setup() {
        callBackNetworkTest_ = new CallBackNetwork(Mock(Logger))
        output = new PropertyMap()
        output.put("url", "http://test.com")
        output.put("subscriber", "test")
        output.put("ID", "1")
    }

    def "Check client and manager are ready"() {
        callBackNetworkTest_.initialize()
        callBackNetworkTest_.createClient()
        expect:
        callBackNetworkTest_.restClient_ != null
        callBackNetworkTest_.connectionManager_ != null
    }

    def "Fetch sbscription with valid url and subscriber"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)
        callBackNetworkTest_.connectionManager_.getSubscription("http://test.com", "test") >> output
        expect:
        PropertyMap result = callBackNetworkTest_.getSubscription("http://test.com", "test")
        result.values().toString() == "[test, 1, http://test.com]"
    }

    def "Fetch subscription with url or subscriber as null value"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)

        callBackNetworkTest_.connectionManager_.getSubscription(null, "test") >>
                { throw new RESTClientException("Insufficient details to get subscription: url or subscriber null value(s)")}
        callBackNetworkTest_.connectionManager_.getSubscription("test", null) >>
                { throw new RESTClientException("Insufficient details to get subscription: url or subscriber null value(s)")}

        when:
        callBackNetworkTest_.getSubscription(null, "test")
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"

        when:
        callBackNetworkTest_.getSubscription("test", null)
        then:
        e = thrown(RESTClientException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"
    }

    def "Fetch subscription for a given id and all"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)

        callBackNetworkTest_.connectionManager_.getSubscriptionForId(1) >> output
        callBackNetworkTest_.connectionManager_.getAllSubscriptions() >> output

        when:
        PropertyMap result1 = callBackNetworkTest_.getSubscriptionForId(1)
        then:
        result1.values().toString() == "[test, 1, http://test.com]"

        when:
        PropertyMap result2 = callBackNetworkTest_.getAllSubscriptions()
        then:
        result2.values().toString() == "[test, 1, http://test.com]"
    }

    def "publish zero or more events"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)

        callBackNetworkTest_.connectionManager_.publish(any()) >> ""
        callBackNetworkTest_.connectionManager_.publish(null) >> {throw new RESTClientException("No events to publish to network")}

        expect:
        callBackNetworkTest_.publish("test", "message")
        when:
        callBackNetworkTest_.publish("test", null)
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "No events to publish to network"
    }

    def "Add subscription with valid url, subscriber and other parameters"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)

        callBackNetworkTest_.connectionManager_.addSubscription("http://test.com", "test", new HashMap<String, String>()) >> true
        expect:
        callBackNetworkTest_.register("http://test.com", "test", new HashMap<String, String>()) == true
    }

    def "Add subscription with url or subscriber as null value"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)

        callBackNetworkTest_.connectionManager_.addSubscription(null, "test", new HashMap<String, String>()) >> {
            throw new RESTClientException("Could not register url or httpmethod : NULL value(s)")}
        callBackNetworkTest_.connectionManager_.addSubscription("http://test.com", null, new HashMap<String, String>()) >> {
            throw new RESTClientException("Could not register url or httpmethod : NULL value(s)")}

        when:
        callBackNetworkTest_.register(null, "test", new HashMap<String, String>())
        then:
        def e = thrown(RESTClientException)
        e.getMessage().equals("Could not register url or httpmethod : NULL value(s)")

        when:
        callBackNetworkTest_.register("http://test.com", null, new HashMap<String, String>())
        then:
        e = thrown(RESTClientException)
        e.getMessage() == "Could not register url or httpmethod : NULL value(s)"
    }

    def "Remove all subscriptions and for given id"() {
        callBackNetworkTest_.connectionManager_ = Mock(ConnectionManager)

        callBackNetworkTest_.connectionManager_.removeAllSubscriptions() >> true
        callBackNetworkTest_.connectionManager_.removeSubscriptionId(1) >> true

        expect:
        callBackNetworkTest_.unSubscribeId(1) == true
        callBackNetworkTest_.unSubscribeAll() == true
    }

    def "Test NOP methods"() {
        expect:
        callBackNetworkTest_.startServer()
        callBackNetworkTest_.stopServer()
        callBackNetworkTest_.serverStatus() == true
        callBackNetworkTest_.getAddress() == null
        callBackNetworkTest_.getPort() == null
    }

    CallBackNetwork callBackNetworkTest_
    PropertyMap output
}
