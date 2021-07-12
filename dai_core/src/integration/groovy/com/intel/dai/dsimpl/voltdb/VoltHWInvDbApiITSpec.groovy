package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.HWInvUtil
import com.intel.dai.dsapi.pojo.Dimm
import com.intel.dai.dsapi.pojo.FruHost
import com.intel.dai.dsapi.pojo.NodeInventory
import com.intel.logging.Logger
import spock.lang.Specification

class VoltHWInvDbApiITSpec extends Specification {
    VoltHWInvDbApi ts

    Logger logger = Mock Logger
    HWInvUtil util = new HWInvUtilImpl(logger)
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        ts = new VoltHWInvDbApi(logger, util, voltDbServers)
        ts.initialize()
    }

    def "ingest - raw DIMM"() {
        setup:
        Dimm dimm = new Dimm()
        dimm.serial = 'serial'
        dimm.mac = 'mac'
        dimm.locator = 'locator'
        dimm.timestamp = 10000L
        when:
        int numIngested = ts.ingest( 'id', dimm)
        Map<String, String> dimmMap = ts.getDimmJsonsOnFruHost(dimm.mac)
        then:
        numIngested == 1
        dimmMap.isEmpty() == false
    }

    def "ingest - raw FRU Host"() {
        setup:
        FruHost fruHost = new FruHost()
        fruHost.mac = 'mac'
        fruHost.timestamp = 10000L
        when:
        int numIngested = ts.ingest( 'id', fruHost)
        List<FruHost> fruHostList = ts.enumerateFruHosts()
        then:
        numIngested == 1
        fruHostList.isEmpty() == false
    }

    def "ingest - raw Node Inventory History"() {
        setup:
        NodeInventory nodeInventory = new NodeInventory()
        nodeInventory.timestamp = 10000L
        expect: ts.ingest(nodeInventory) == 1
    }
}
