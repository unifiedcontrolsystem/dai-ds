package com.intel.dai.inventory.api.es

import com.intel.dai.inventory.utilities.Helper
import spock.lang.Specification

class ElasticsearchITSpec extends Specification {
    Elasticsearch ts

    def setup() {

        println Helper.testStartMessage(specificationContext)
        ts = new Elasticsearch()
    }

    def cleanup() {
        ts.close()
        println Helper.testEndMessage(specificationContext)
    }

    def "getRestHighLevelClient - positive"() {
        when: ts.getRestHighLevelClient(hostName, port, userName, password)
        then: ts.client != null

        where:
        hostName                            | port | userName   | password
        "cmcheung-centos-7.ra.intel.com"    | 9200 | "elkrest"  | "elkdefault"
    }

    def "getRestHighLevelClient - negative"() {
        when: ts.getRestHighLevelClient(hostName, port, userName, password)
        then: ts.client != null

        where:
        hostName                            | port | userName   | password
        "no-such-server.ra.intel.com"       | 9200 | "elkrest"  | "elkdefault"
        "cmcheung-centos-7.ra.intel.com"    | 9999 | "elkrest"  | "elkdefault"
        "cmcheung-centos-7.ra.intel.com"    | 9200 | "badUser"  | "elkdefault"
        "cmcheung-centos-7.ra.intel.com"    | 9200 | "elkrest"  | "badPassword"
    }
}
