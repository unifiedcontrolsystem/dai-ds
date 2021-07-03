package com.intel.dai.dsimpl.voltdb


import com.intel.dai.dsapi.HWInvHistoryEvent
import com.intel.dai.dsapi.HWInvUtil
import com.intel.logging.Logger
import org.apache.commons.io.FileUtils
import spock.lang.Specification

import java.nio.file.Paths

class VoltHWInvDbApiITSpec extends Specification {
    static def tmpDir = "build/tmp/"
    static def dataDir = "src/integration/resources/data/"
    static def expectedDir = "src/integration/resources/data/expected/"

    static def inventoryFileName = 'oneNode.json'
    static def inventoryResultFileName = tmpDir+inventoryFileName
    static def inventoryExpectedResultFileName = expectedDir+inventoryFileName
    static def rawInventoryDataFilePath = Paths.get dataDir+inventoryFileName

    VoltHWInvDbApi ts

    Logger logger = Mock Logger
    HWInvUtil util = new HWInvUtilImpl(logger)
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        ts = new VoltHWInvDbApi(logger, util, voltDbServers)
        ts.initialize()
//        ts.deleteAllRawHistoricalRecords()
//        ts.deleteAllCookedNodes()
    }

//    def "ingest -- file"() {
//        when: ts.ingest rawInventoryDataFilePath
//        then: ts.numberOfRawInventoryRows() == 5
//    }
//
//    def "allLocationsAt"() {
//        setup: ts.ingest rawInventoryDataFilePath
//        when: ts.allLocationsAt 'R0-CB0-CN0', inventoryResultFileName
//        then: FileUtils.contentEquals new File(inventoryExpectedResultFileName), new File(inventoryResultFileName)
//    }
//
//    def "numberOfRawInventoryRows"() {
//        given: ts.numberOfRawInventoryRows() == 0
//        when: ts.ingest rawInventoryDataFilePath
//        then: ts.numberOfRawInventoryRows() == 5
//    }
//
//    def "generateHWInfoJsonBlob"() {
//        setup: ts.ingest rawInventoryDataFilePath
//        when:
//        def res = ts.generateHWInfoJsonBlob 'R0-CB0-CN0'
//        then:
//        res.left == 'Node.0'
//        res.right.contains '{"HWInfo":{"fru/DIMM0/fru_id":{"provider":"shasta","value":"Memory.0"},'
//    }
//
//    def "numberOfCookedNodes"() {
//        setup: ts.ingest rawInventoryDataFilePath
//        when: ts.ingestCookedNode 'R0-CB0-CN0', '2002-10-02T15:00:00.05Z'
//        then: ts.numberOfCookedNodes() == 1
//    }
//
//    def "deleteAllCookedNodes"() {
//        setup:
//        ts.ingest rawInventoryDataFilePath
//        ts.ingestCookedNode 'R0-CB0-CN0', '2002-10-02T15:00:00.05Z'
//        expect: ts.numberOfCookedNodes() == 1
//
//        when: ts.deleteAllCookedNodes()
//        then: ts.numberOfCookedNodes() == 0
//    }
//
//    def "ingestCookedNode"() {
//        given: ts.numberOfCookedNodes() == 0
//        when:
//        ts.ingest rawInventoryDataFilePath
//        ts.ingestCookedNode NodeLocation, Timestamp
//        then: ts.numberOfCookedNodes() == 1
//
//        where:
//        Timestamp                   | NodeLocation
//        '2002-10-02T15:00:00.05Z'   | 'R0-CB0-CN0'
//        '2003-10-02T15:00:00.05Z'   | 'R0-CB0-CN0'
//    }
//
//    def "insertHistoricalRecord"() {
//        given: ts.numberOfRawInventoryRows() == 0
//        when: def count = populateRawInvHistoryTable()
//        then: ts.numberRawInventoryHistoryRows() == count
//    }
//
//    def "ingestCookedNodesChanged"() {
//        Map<String, String> lastNodeChangeTimestamp = [:]
//        lastNodeChangeTimestamp['R0-CB0-CN0'] = '2002-10-02T15:00:00.05Z'
//        given: ts.numberOfCookedNodes() == 0
//        when:
//        ts.ingest rawInventoryDataFilePath
//
//        then: ts.ingestCookedNodesChanged(lastNodeChangeTimestamp) == 1
//        println ts.dumpCookedNodes()
//    }
//
//    def populateRawInvHistoryTable() {
//        def event = new HWInvHistoryEvent()
//        event.Action = 'Added'
//        event.ID = 'R0-CB0-CN0'
//        event.FRUID = 'node.0'
//        event.Timestamp = '2002-10-02T15:00:00.05Z'
//        ts.insertRawHistoricalRecord event
//
//        event.Action = 'Removed'
//        event.ID = 'R0-CB0-CN0'
//        event.FRUID = 'node.0'
//        event.Timestamp = '2003-10-02T15:00:00.05Z'
//        ts.insertRawHistoricalRecord event
//
//        event.ID = 'R0-CB0-CN0-DIMM0'
//        event.FRUID = 'memory.0'
//        ts.insertRawHistoricalRecord event
//
//        return 3
//    }
}
