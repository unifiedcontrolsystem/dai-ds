package com.intel.dai.eventsim

import com.intel.logging.Logger
import com.intel.networking.restclient.RESTClientException
import com.intel.properties.PropertyMap
import spock.lang.Specification

class CallBackNetworkSpec extends Specification {

    def "Check client and manager are ready"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.initialize()
        callBackNetworkTest.createClient()
        expect:
        callBackNetworkTest.restClient_ != null
        callBackNetworkTest.connMan_ != null
    }

    def "Fetch sbscription with valid url and subscriber"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        PropertyMap output = new PropertyMap()
        output.put("url", "http://test.com")
        output.put("subscriber", "test")
        output.put("ID", "1")
        callBackNetworkTest.connMan_.getSubscription("http://test.com", "test") >> output
        expect:
        PropertyMap result = callBackNetworkTest.getSubscription("http://test.com", "test")
        result.values().toString() == "[test, 1, http://test.com]"
    }

    def "Fetch sbscription with url as null value"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        PropertyMap output = new PropertyMap()
        output.put("url", "http://test.com")
        output.put("subscriber", "test")
        output.put("ID", "1")
        callBackNetworkTest.connMan_.getSubscription("http://test.com", "test") >> output
        when:
        callBackNetworkTest.getSubscription(null, "test")
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"
    }

    def "Fetch sbscription with subscriber as null value"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        PropertyMap output = new PropertyMap()
        output.put("url", "http://test.com")
        output.put("subscriber", "test")
        output.put("ID", "1")
        callBackNetworkTest.connMan_.getSubscription("http://test.com", "test") >> output
        when:
        callBackNetworkTest.getSubscription("http://test.com", null)
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"
    }

    def "Fetch sbscription for a given id"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        PropertyMap output = new PropertyMap()
        output.put("url", "http://test.com")
        output.put("subscriber", "test")
        output.put("ID", "1")
        callBackNetworkTest.connMan_.getSubscriptionForId(1) >> output
        expect:
        PropertyMap result = callBackNetworkTest.getSubscriptionForId(1)
        result.values().toString() == "[test, 1, http://test.com]"
    }

    def "Fetch all sbscription"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        PropertyMap output = new PropertyMap()
        output.put("url", "http://test.com")
        output.put("subscriber", "test")
        output.put("ID", "1")
        callBackNetworkTest.connMan_.getAllSubscriptions() >> output
        expect:
        PropertyMap result = callBackNetworkTest.getAllSubscriptions()
        result.values().toString() == "[test, 1, http://test.com]"
    }

    def "publish events"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        callBackNetworkTest.connMan_.publish(any()) >> ""
        expect:
        callBackNetworkTest.publish("test", "message")
    }

    def "publish zero events"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        when:
        callBackNetworkTest.publish("test", null)
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "No events to publish to network"
    }

    def "Add subscription with valid url, subscriber and other parameters"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        callBackNetworkTest.connMan_.addSubscription("http://test.com", "test", new HashMap<String, String>()) >> true
        expect:
        callBackNetworkTest.register("http://test.com", "test", new HashMap<String, String>()) == true
    }

    def "Add subscription with url as null value"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        when:
        callBackNetworkTest.register(null, "test", new HashMap<String, String>())
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Could not register url or httpmethod : NULL value(s)"
    }

    def "Add subscription with subscriber as null value"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        when:
        callBackNetworkTest.register("http://test.com", null, new HashMap<String, String>())
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Could not register url or httpmethod : NULL value(s)"
    }

    def "Remove all subscriptions"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        callBackNetworkTest.connMan_.removeAllSubscriptions() >> true
        expect:
        callBackNetworkTest.unSubscribeAll() == true
    }

    def "Remove subsctiption for a given id"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        callBackNetworkTest.connMan_ = Mock(ConnectionManager)
        callBackNetworkTest.connMan_.removeSubscriptionId(1) >> true
        expect:
        callBackNetworkTest.unSubscribeId(1) == true
    }

    def "Start server"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        expect:
        callBackNetworkTest.startServer()
    }

    def "Stop server"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        expect:
        callBackNetworkTest.stopServer()
    }

    def "Server status"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        expect:
        callBackNetworkTest.serverStatus() == true
    }

    def "Fetch server address"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        expect:
        callBackNetworkTest.getAddress() == null
    }

    def "Fetch server port"() {
        CallBackNetwork callBackNetworkTest = new CallBackNetwork(Mock(Logger))
        expect:
        callBackNetworkTest.getPort() == null
    }
}
