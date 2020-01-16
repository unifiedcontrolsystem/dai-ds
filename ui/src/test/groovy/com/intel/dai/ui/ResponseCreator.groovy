package com.intel.dai.ui

import spock.lang.Specification
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.properties.PropertyMap;
import java.util.HashMap;
import java.util.ArrayList;


class responseCreatorSpec extends Specification {

    def underTest_

    def setup() {

        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        ConfigIOFactory.getInstance(_) >> jsonParser

        underTest_ = new ResponseCreator()
    }

    def "Test setParser"() {
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"

        expect:
        underTest_.setParser(jsonParser)
    }

    def "Test toString"() {
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.toString(result).equals("0")
    }

    def "Test fromString"() {
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.fromString("0")
    }

    def "Test createJsonResult"() {

        def sa = new String[2]
        sa[0] = "0"
        sa[1] = "SUCCESS"
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.createJsonResult(sa)
    }

    def "Test concatControlJsonResponses"() {

        def results = new ArrayList<String>()
        results.add("Resp1")
        results.add("Resp2")
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.concatControlJsonResponses(results)
    }

    def "Test convertToStandardFormat"() {
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.convertToStandardFormat(result)
    }

    def "Test createData"() {
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.createData(result)
    }

    def "Test createSchema"() {
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.createSchema()
    }

    def "Test concatProvisionerJsonResponses"() {

        def results = new ArrayList<String>()
        results.add("Resp1")
        results.add("Resp2")
        def map = new HashMap<String,String>()
        map.put("Status", "0")
        def result = new PropertyMap(map)
        def jsonParser = Mock(ConfigIO)
        jsonParser.fromString(_) >> result
        jsonParser.toString(_) >> "0"
        underTest_.setParser(jsonParser)

        expect:
        underTest_.concatProvisionerJsonResponses(results)
    }

}
