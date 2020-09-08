// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.pojo.loc;

import lombok.ToString;
import java.util.List;

@ToString
public class ForeignHWInventory {
    public String XName;    // field name MUST match incoming json from foreign server
    public String Format;

    public List<ForeignHWInvByLocCabinet> Cabinets;
    public List<ForeignHWInvByLocChassis> Chassis;
    public List<ForeignHWInvByLocComputeModule> ComputeModules;
    public List<ForeignHWInvByLocRouterModule> RouterModules;

    public List<ForeignHWInvByLocNodeEnclosure> NodeEnclosures;
    public List<ForeignHWInvByLocHSNBoard> HSNBoards;
    public List<ForeignHWInvByLocNode> Nodes;
    public List<ForeignHWInvByLocProcessor> Processors;

    public List<ForeignHWInvByLocDrive> Drives;
    public List<ForeignHWInvByLocMemory> Memory;
    public List<ForeignHWInvByLocCabinetPDU> CabinetPDU;
    public List<ForeignHWInvByLocCabinetPDUOutlet> CabinetPDUOutlets;
}
