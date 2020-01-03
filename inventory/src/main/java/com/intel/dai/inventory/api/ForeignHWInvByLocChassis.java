package com.intel.dai.inventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0
 */
@ToString
class ForeignHWInvByLocChassis extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocComputeModule> ComputeModules;
}
