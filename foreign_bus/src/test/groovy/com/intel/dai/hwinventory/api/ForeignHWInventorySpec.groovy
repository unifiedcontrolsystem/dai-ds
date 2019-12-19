package com.intel.dai.hwinventory.api

// This test is necessary because ForeignHWInventory.toString() is exercised by
// existing test code.  The toString() annotation is added to ForeignHWInventory
// for consistency.
class ForeignHWInventorySpec extends spock.lang.Specification {
    def "ForeignHWInvByLocCabinet toString" () {
        def cab0 = new ForeignHWInvByLocCabinet()
        def cab1 = new ForeignHWInvByLocCabinet()
        expect: cab0.toString() == cab1.toString()
    }
    def "ForeignHWInvByLocChassis toString" () {
        def ts0 = new ForeignHWInvByLocChassis()
        def ts1 = new ForeignHWInvByLocChassis()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocComputeModule toString" () {
        def ts0 = new ForeignHWInvByLocComputeModule()
        def ts1 = new ForeignHWInvByLocComputeModule()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocRouterModule toString" () {
        def ts0 = new ForeignHWInvByLocRouterModule()
        def ts1 = new ForeignHWInvByLocRouterModule()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocNodeEnclosure toString" () {
        def ts0 = new ForeignHWInvByLocNodeEnclosure()
        def ts1 = new ForeignHWInvByLocNodeEnclosure()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocHSNBoard toString" () {
        def ts0 = new ForeignHWInvByLocHSNBoard()
        def ts1 = new ForeignHWInvByLocHSNBoard()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocNode toString" () {
        def ts0 = new ForeignHWInvByLocNode()
        def ts1 = new ForeignHWInvByLocNode()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocProcessor toString" () {
        def ts0 = new ForeignHWInvByLocProcessor()
        def ts1 = new ForeignHWInvByLocProcessor()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocMemory toString" () {
        def ts0 = new ForeignHWInvByLocMemory()
        def ts1 = new ForeignHWInvByLocMemory()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocCabinetPDU toString" () {
        def ts0 = new ForeignHWInvByLocCabinetPDU()
        def ts1 = new ForeignHWInvByLocCabinetPDU()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLocCabinetPDUOutlet toString" () {
        def ts0 = new ForeignHWInvByLocCabinetPDUOutlet()
        def ts1 = new ForeignHWInvByLocCabinetPDUOutlet()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignHWInvByLoc toString" () {
        def ts0 = new ForeignHWInvByLoc()
        def ts1 = new ForeignHWInvByLoc()
        expect: ts0.toString() == ts1.toString()
    }
    def "ForeignFRU toString" () {
        def ts0 = new ForeignFRU()
        def ts1 = new ForeignFRU()
        expect: ts0.toString() == ts1.toString()
    }
    def "Test ForeignHWInventory toString" () {
        def tree0 = new ForeignHWInventory()
        def tree1 = new ForeignHWInventory()

        tree0.XName = "c0n0"
        tree1.XName = "c0n0"
        tree0.Format = "NodeOnly"
        tree1.Format = "NodeOnly"

        tree0.Cabinets = new ArrayList<ForeignHWInvByLocCabinet>()
        tree1.Cabinets = new ArrayList<ForeignHWInvByLocCabinet>()
        tree0.Chassis = new ArrayList<ForeignHWInvByLocChassis>()
        tree1.Chassis = new ArrayList<ForeignHWInvByLocChassis>()

        tree0.ComputeModules = new ArrayList<ForeignHWInvByLocComputeModule>()
        tree1.ComputeModules = new ArrayList<ForeignHWInvByLocComputeModule>()
        tree0.Memory = new ArrayList<ForeignHWInvByLocMemory>()
        tree1.Memory = new ArrayList<ForeignHWInvByLocMemory>()

        tree0.NodeEnclosures = new ArrayList<ForeignHWInvByLocNodeEnclosure>()
        tree1.NodeEnclosures = new ArrayList<ForeignHWInvByLocNodeEnclosure>()
        tree0.Nodes = new ArrayList<ForeignHWInvByLocNode>()
        tree1.Nodes = new ArrayList<ForeignHWInvByLocNode>()

        tree0.Processors = new ArrayList<ForeignHWInvByLocProcessor>()
        tree1.Processors = new ArrayList<ForeignHWInvByLocProcessor>()
        tree0.RouterModules = new ArrayList<ForeignHWInvByLocRouterModule>()
        tree1.RouterModules = new ArrayList<ForeignHWInvByLocRouterModule>()

        tree0.HSNBoards = new ArrayList<ForeignHWInvByLocHSNBoard>()
        tree1.HSNBoards = new ArrayList<ForeignHWInvByLocHSNBoard>()
        tree0.CabinetPDU = new ArrayList<ForeignHWInvByLocCabinetPDU>()
        tree1.CabinetPDU = new ArrayList<ForeignHWInvByLocCabinetPDU>()
        tree0.CabinetPDUOutlets = new ArrayList<ForeignHWInvByLocCabinetPDUOutlet>()
        tree1.CabinetPDUOutlets = new ArrayList<ForeignHWInvByLocCabinetPDUOutlet>()

        expect: tree0.toString() == tree1.toString()
    }
}
