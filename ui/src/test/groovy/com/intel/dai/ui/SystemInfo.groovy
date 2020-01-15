package com.intel.dai.ui

import spock.lang.Specification
import com.intel.dai.dsapi.Configuration;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyArray;
import java.util.HashMap;


class SystemInfoSpec extends Specification {

    def underTest_

    def setup() {

        def map1 = new HashMap<String,String>()
        map1.put("LCTN", "testService")
        def serviceobj = new PropertyMap(map1)
        def servicenodes = new PropertyArray();
        servicenodes.add(serviceobj)
        def map2 = new HashMap<String,String>()
        map2.put("LCTN", "testCompute")
        def computeobj = new PropertyMap(map2)
        def computenodes = new PropertyArray();
        computenodes.add(computeobj)
        def configMgr_ = Mock(Configuration)
        configMgr_.getServiceNodeConfiguration() >> servicenodes
        configMgr_.getComputeNodeConfiguration() >> computenodes

        underTest_ = new SystemInfo(configMgr_)
    }

    def "Test extractSchemaFromNodeObject"() {
        def map = new HashMap<String,String>()
        map.put("LCTN", "test")
        def nodeObject = new PropertyMap(map)

        expect:
        underTest_.extractSchemaFromNodeObject(nodeObject)
    }

    def "Test extractValuesFromNodes"() {
        def map = new HashMap<String,String>()
        map.put("LCTN", "test")
        def nodeObject = new PropertyMap(map)
        def nodes = new PropertyArray();
        nodes.add(nodeObject)

        expect:
        underTest_.extractValuesFromNodes(nodes)
    }

    def "Test addNodesOfSpecificNodeType"() {
        def map = new HashMap<String,String>()
        map.put("LCTN", "test")
        def nodeObject = new PropertyMap(map)
        def nodes = new PropertyArray();
        nodes.add(nodeObject)

        expect:
        underTest_.addNodesOfSpecificNodeType(nodes, "Compute")
    }

    def "Test generateSystemInfo"() {

        expect:
        underTest_.generateSystemInfo()
    }

}
