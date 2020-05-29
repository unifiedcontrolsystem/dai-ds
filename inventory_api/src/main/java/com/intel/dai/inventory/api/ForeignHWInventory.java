// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;
import java.util.List;

@ToString
class ForeignHWInventory {
    String ForeignName;
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
