package com.intel.dai.eventsim

import com.intel.logging.Logger
import com.intel.networking.restclient.BlockingResult
import com.intel.networking.restclient.RESTClient
import com.intel.networking.restclient.RESTClientException
import spock.lang.Specification

class ConnectionManagerSpec extends Specification {

    def "Prior no subscriptions, add subscriptions" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
            connectionManagerTest.getAllSubscriptions().size() == 1
    }

    def "Add subscription, insufficient details no url" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        when:
        !connectionManagerTest.addSubscription(null, "test", new HashMap<String, String>())
        then :
            def e = thrown(RESTClientException)
            e.getMessage() == "Could not add subscription: url or subscriber null value(s)"
    }

    def "Add subscription, insufficient details no subscriber" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        when:
        connectionManagerTest.addSubscription("http://test.com", null, new HashMap<String, String>())
        then :
        def e = thrown(RESTClientException)
        e.getMessage() == "Could not add subscription: url or subscriber null value(s)"
    }

    def "Add subscription, but already exists" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>()) == false
    }

    def "Remove subscription with a valid id" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test1", new HashMap<String, String>())
        connectionManagerTest.addSubscription("http://test.com", "test2", new HashMap<String, String>())
        connectionManagerTest.addSubscription("http://test1.com", "test1", new HashMap<String, String>())
        connectionManagerTest.removeSubscriptionId(1)
        expect :
            connectionManagerTest.getAllSubscriptions().size() == 1
    }

    def "Remove subscription with a invalid id" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        expect :
        connectionManagerTest.removeSubscriptionId(1) == false
    }

    def "Add subscription, get the added subscription details using url and subscriber" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
            connectionManagerTest.getSubscription("http://test.com", "test").size() == 1
    }

    def "Exists subscriptions, fetch subscription url details which does not exists" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
            connectionManagerTest.getSubscription("http://test1.com", "test").size() == 0
    }

    def "Exists subscriptions, fetch subscription subscriber details which does not exists" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
        connectionManagerTest.getSubscription("http://test.com", "test1").size() == 0
    }

    def "Exists subscriptions, fetch subscription with subscriber as null value" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        when :
        connectionManagerTest.getSubscription("http://test.com", null) == null
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"
    }

    def "Exists subscriptions, fetch subscription with url as null value" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        when :
        connectionManagerTest.getSubscription(null, "test") == null
        then:
        def e = thrown(RESTClientException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"
    }

    def "No subscriptions, fetch all subscription details" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        expect :
            connectionManagerTest.getAllSubscriptions().size() == 0
    }

    def "Exists subscriptions, fetch subscription for a given valid id" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        Map<String, String> parameters = new HashMap<>()
        parameters.put("url", "http://test.com")
        connectionManagerTest.addSubscription("http://test.com", "test", parameters)
        expect :
            connectionManagerTest.getSubscriptionForId(1).size() == 1
    }

    def "Exists subscriptions, fetch subscription for a given invalid id" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
        connectionManagerTest.getSubscriptionForId(2).size() == 0
    }

    def "Exists subscriptions, publish generated events in constant mode" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        client.postRESTRequestBlocking(any() as URI, any() as String) >> BlockingResult.class
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
            connectionManagerTest.publish("message")
    }

    def "Exists subscriptions, publish generated events in burst mode" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        client.postRESTRequestBlocking(any() as URI, any() as String) >> BlockingResult.class
        connectionManagerTest.removeAllSubscriptions()
        connectionManagerTest.addSubscription("http://test.com", "test", new HashMap<String, String>())
        expect :
        connectionManagerTest.publish("message")
    }

    def "Zero subscriptions, publish generated events" () {
        RESTClient client = Mock(RESTClient.class)
        Logger log = Mock(Logger.class)
        ConnectionManager connectionManagerTest = new ConnectionManager(client, log)
        client.postRESTRequestBlocking(any() as URI, any() as String) >> BlockingResult.class
        connectionManagerTest.removeAllSubscriptions()
        expect :
        connectionManagerTest.publish("message")
    }
}
