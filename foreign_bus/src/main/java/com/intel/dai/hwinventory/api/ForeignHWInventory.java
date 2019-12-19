package com.intel.dai.hwinventory.api;

import lombok.ToString;
import java.util.List;

@ToString
class ForeignHWInventory {
    String XName;
    String Format;

    List<ForeignHWInvByLocCabinet> Cabinets;
    List<ForeignHWInvByLocChassis> Chassis;
    List<ForeignHWInvByLocComputeModule> ComputeModules;
    List<ForeignHWInvByLocRouterModule> RouterModules;

    List<ForeignHWInvByLocNodeEnclosure> NodeEnclosures;
    List<ForeignHWInvByLocHSNBoard> HSNBoards;
    List<ForeignHWInvByLocNode> Nodes;
    List<ForeignHWInvByLocProcessor> Processors;

    List<ForeignHWInvByLocMemory> Memory;
    List<ForeignHWInvByLocCabinetPDU> CabinetPDU;
    List<ForeignHWInvByLocCabinetPDUOutlet> CabinetPDUOutlets;
}
