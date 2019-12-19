package com.intel.dai.hwinventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0
 */
@ToString
class ForeignHWInvByLocChassis extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocComputeModule> ComputeModules;
}
