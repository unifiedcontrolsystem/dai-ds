package com.intel.dai.fabric

import com.intel.config_io.ConfigIOFactory
import com.intel.config_io.ConfigIOParseException
import com.intel.properties.PropertyMap
import spock.lang.Specification

class FabricItemBaseSpec extends Specification {
    static class TestClass extends FabricItemBase {
        TestClass(long timestamp, String location, String name) {
            super(timestamp, location, name)
        }
        TestClass(String json) {
            getPropertyMapFromJSON(json);
        }
        @Override protected void addOtherValuesToJsonMap(PropertyMap map) { }
    }

    def underTest_
    void setup() {
        underTest_ = new TestClass(99L, "location", "name")
        FabricItemBase.parser_ = ConfigIOFactory.getInstance("json")
    }

    def cleanup() {
        FabricItemBase.parser_ = ConfigIOFactory.getInstance("json")
    }

    def "Test GetTimestamp"() {
        expect: underTest_.getTimestamp() == 99L
    }

    def "Test GetLocation"() {
        expect: underTest_.getLocation() == "location"
    }

    def "Test GetName"() {
        expect: underTest_.getName() == "name"
    }

    def "Test ToString"() {
        expect: underTest_.toString() == """{"name":"name","location":"location","timestamp":99}"""
    }

    def "Test AddOtherValuesToJsonMap"() {
        underTest_.addOtherValuesToJsonMap()
        expect: true
    }

    def "Test GetPropertyMapFromJSON"() {
        def inst = new TestClass("""{"name":"name","location":"location","timestamp":99}""")
        expect: inst.getTimestamp() == 99L
        and:    inst.getName() == "name"
        and:    inst.getLocation() == "location"
    }

    def "Test GetPropertyMapFromJSON Negative"() {
        when:  new TestClass(JSON)
        then:  thrown(ConfigIOParseException)
        where:
        JSON                                                                || RESULT
        """{"name":null,"location":"location","timestamp":99,"a":"A"}"""    || true
        """{"name":"name","location":"location","a":"A"}"""                 || true
        """{"name":null,"location":"location","timestamp":"red","a":"A"}""" || true
    }

    def "Test ctor negative"() {
        when: new TestClass(99L, "location", null)
        then: thrown(IllegalArgumentException)
    }

    def "Test toString Negative"() {
        given: FabricItemBase.parser_ = null
        when: underTest_.toString()
        then: thrown(NullPointerException)
    }

    def "Test getPropertyMapFromJSON Negative"() {
        given: FabricItemBase.parser_ = null
        when: underTest_.getPropertyMapFromJSON("{}")
        then: thrown(NullPointerException)
    }
}
