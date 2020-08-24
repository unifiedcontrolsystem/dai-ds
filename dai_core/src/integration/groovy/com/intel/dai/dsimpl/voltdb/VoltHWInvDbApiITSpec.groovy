package com.intel.dai.dsimpl.voltdb

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

    def setup() {
        String server = "192.168.2.22"
        String[] servers = [server]
        ts = new VoltHWInvDbApi(logger, new HWInvUtilImpl(logger), servers)
        ts.initialize()
        ts.delete''
        ts.deleteAllRawHistoricalRecords()
        ts.deleteAllCookedNodes()
    }

    def "delete"() {
        setup: ts.ingest rawInventoryDataFilePath
        when: ts.delete ''
        then: ts.numberOfRawInventoryRows() == 0
    }

    def "ingest -- file"() {
        when: ts.ingest rawInventoryDataFilePath
        then: ts.numberOfRawInventoryRows() == 5
    }

    def "allLocationsAt"() {
        setup: ts.ingest rawInventoryDataFilePath
        when: ts.allLocationsAt 'R0-CB0-CN0', inventoryResultFileName
        then: FileUtils.contentEquals new File(inventoryExpectedResultFileName), new File(inventoryResultFileName)
    }

    def "numberOfRawInventoryRows"() {
        given: ts.numberOfRawInventoryRows() == 0
        when: ts.ingest rawInventoryDataFilePath
        then: ts.numberOfRawInventoryRows() == 5
    }

    def "generateHWInfoJsonBlob"() {
        setup: ts.ingest rawInventoryDataFilePath
        when:
        def res = ts.generateHWInfoJsonBlob 'R0-CB0-CN0'
        then:
        res.left == 'Node.0'
        res.right.contains '{"HWInfo":{"fru/DIMM0/fru_id":{"provider":"shasta","value":"Memory.0"},'
    }

    def "numberOfCookedNodes"() {
        setup: ts.ingest rawInventoryDataFilePath
        when: ts.ingestCookedNode '2002-10-02T15:00:00.05Z', 'Added', 'R0-CB0-CN0'
        then: ts.numberOfCookedNodes() == 1
    }

    def "deleteAllCookedNodes"() {
        setup:
        ts.ingest rawInventoryDataFilePath
        ts.ingestCookedNode '2002-10-02T15:00:00.05Z', 'Added', 'R0-CB0-CN0'
        expect: ts.numberOfCookedNodes() == 1

        when: ts.deleteAllCookedNodes()
        then: ts.numberOfCookedNodes() == 0
    }

    def "ingestCookedNode"() {
        given: ts.numberOfCookedNodes() == 0
        when:
        ts.ingest rawInventoryDataFilePath
        ts.ingestCookedNode Timestamp, Action, NodeLocation
        then: ts.numberOfCookedNodes() == 1

        where:
        Timestamp                   | Action    | NodeLocation
        '2002-10-02T15:00:00.05Z'   | 'Added'   | 'R0-CB0-CN0'
        '2003-10-02T15:00:00.05Z'   | 'Removed' | 'R0-CB0-CN0'
    }

    def "insertHistoricalRecord"() {
        given: ts.numberOfRawInventoryRows() == 0
        when: def count = populateRawInvHistoryTable()
        then: ts.numberRawInventoryHistoryRows() == count
    }

    def "lastHwInvHistoryUpdate"() {
        setup: populateRawInvHistoryTable()
        expect: '2003-10-02T15:00:00.05Z' == ts.lastHwInvHistoryUpdate()
    }

    def "ingestCookedNodesChanged"() {
        given: ts.numberCookedNodeInventoryHistoryRows() == 0
        when:
        ts.ingest rawInventoryDataFilePath
        populateRawInvHistoryTable()

        then: ts.ingestCookedNodesChanged() == 2
        println ts.dumpCookedNodes()
    }

    def populateRawInvHistoryTable() {
        ts.insertRawHistoricalRecord 'Added', 'R0-CB0-CN0', 'node.0', '2002-10-02T15:00:00.05Z'
        ts.insertRawHistoricalRecord 'Removed', 'R0-CB0-CN0', 'node.0', '2003-10-02T15:00:00.05Z'
        ts.insertRawHistoricalRecord 'Removed', 'R0-CB0-CN0-DIMM0', 'memory.0', '2003-10-02T15:00:00.05Z'

        return 3
    }
}
