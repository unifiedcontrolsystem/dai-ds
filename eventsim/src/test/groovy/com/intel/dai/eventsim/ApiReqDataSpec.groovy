package com.intel.dai.eventsim


import com.intel.logging.Logger
import com.intel.networking.HttpMethod
import com.intel.networking.restserver.Request
import com.intel.networking.restserver.Response
import com.intel.properties.PropertyMap
import spock.lang.Specification


class ApiReqDataSpec extends Specification {

    def "Register url with null value"() {
        ApiReqData apiReqDataTest = new ApiReqData(Mock(Logger))
        when:
        apiReqDataTest.registerPathCallBack(null, Mock(NetworkSimulator))
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not register API URL or call back method : NULL value(s)"
    }

    def "Register callback method with null value"() {
        ApiReqData apiReqDataTest = new ApiReqData(Mock(Logger))
        PropertyMap input = new PropertyMap();
        when:
        apiReqDataTest.registerPathCallBack(input, null)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not register API URL or call back method : NULL value(s)"
    }

    def "Register url and callback method"() {
        ApiReqData apiReqDataTest = new ApiReqData(Mock(Logger))
        PropertyMap input = new PropertyMap();
        apiReqDataTest.registerPathCallBack(input, Mock(NetworkSimulator))
        expect:
        apiReqDataTest.dispatchMap.size() == 1
    }

    def "Initialise callback method for post"() {
        ApiReqData apiReqDataTest = new ApiReqData(Mock(Logger))
        PropertyMap input = new PropertyMap()
        input.put("/api/test", HttpMethod.POST)
        apiReqDataTest.registerPathCallBack(input, Mock(NetworkSimulator))
        Request req = Mock(Request)
        req.getMethod() >> HttpMethod.POST
        req.getBody() >> "{\"count\" : \"3\", \"location\" : \"R0-00-CH00-CN0\"}"
        req.getPath() >> "/api/test"
        Response res = Mock(Response)
        expect:
        apiReqDataTest.apiCallBack(req, res)
    }

    def "Initialise callback method for get"() {
        ApiReqData apiReqDataTest = new ApiReqData(Mock(Logger))
        PropertyMap input = new PropertyMap()
        input.put("/api/test", HttpMethod.GET)
        apiReqDataTest.registerPathCallBack(input, Mock(NetworkSimulator))
        Request req = Mock(Request)
        req.getMethod() >> HttpMethod.GET
        req.getQuery() >> "locations=test&count=1"
        req.getPath() >> "/api/test"
        Response res = Mock(Response)
        expect:
        apiReqDataTest.apiCallBack(req, res)
    }

    def "Mismtach of url and callbackmethod" () {
        ApiReqData apiReqDataTest = new ApiReqData(Mock(Logger))
        PropertyMap input = new PropertyMap()
        input.put("/api/test", HttpMethod.GET)
        apiReqDataTest.registerPathCallBack(input, Mock(NetworkSimulator))
        Request req = Mock(Request)
        req.getMethod() >> HttpMethod.POST
        req.getBody() >> "{\"count\" : \"3\", \"location\" : \"R0-00-CH00-CN0\"}"
        req.getPath() >> "/api/test"
        Response res = Mock(Response)
        expect:
        apiReqDataTest.apiCallBack(req, res)
    }
}
