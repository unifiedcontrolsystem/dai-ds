package com.intel.dai.eventsim

import com.intel.properties.PropertyMap
import spock.lang.Specification

class DataValidationSpec extends Specification {

    def "Validate keys and key-value data for a given input"() {
        PropertyMap data = new PropertyMap()
        when:
        if(key != null || value != null)
            data.put(key, value)

        DataValidation.validateKeys(data , keys, input_message)
        then:
        def e =  thrown(SimulatorException)
        e.message.equals(output_message)

        where:
        key     |        value      |   keys        |      input_message       |    output_message
        null    |        null       |   KEYS        |     "error message"      |   "data/keys/message is empty or null"
        "key-1" |       "value"     |   null        |     "error message"      |   "data/keys/message is empty or null"
        "key-1" |       "value"     |   EMPTY_KEYS  |     "error message"      |   "data/keys/message is empty or null"
        "key-1" |       "value"     |   KEYS        |         null             |   "data/keys/message is empty or null"
        "key-1" |       "value"     |   KEYS        |          ""              |   "data/keys/message is empty or null"
        null    |       "value"     |   KEYS        |   "missing key, key ="   |   "missing key, key = key-1"
        "key-1" |        null       |   KEYS        |   "missing value, key =" |   "missing value, key = key-1"
        "key-1" |        ""         |   KEYS        |   "missing value, key =" |   "missing value, key = key-1"
        "key-2" |        "value"    |   KEYS        |   "missing value, key =" |   "missing value, key = key-1"
      }

    def "Validate key-value data for a given input"() {
        Map<String, String> data = new HashMap<>()
        when:
        if (key != null || value != null)
            data.put(key, value)

        DataValidation.validateData(data, input_message)
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output_message)

        where:
        key     | value   | input_message     | output_message
        null    | null    | "error message"   | "data/message is empty or null"
        "key-1" | "value" | null              | "data/message is empty or null"
        "key-1" | "value" | ""                | "data/message is empty or null"
        "key-1" | null    | "missing value, " | "missing value,  key-1=null"
        null    | "value" | "missing key, "   | "missing key,  null=value"
    }

    private final static String[] KEYS = ['key-1']
    private final static String[] EMPTY_KEYS = []
}
