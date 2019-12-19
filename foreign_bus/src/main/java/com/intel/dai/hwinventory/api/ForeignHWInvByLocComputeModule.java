package com.intel.dai.hwinventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0s0  (?)
 */
@ToString
class ForeignHWInvByLocComputeModule extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocNodeEnclosure> NodeEnclosures;
}
