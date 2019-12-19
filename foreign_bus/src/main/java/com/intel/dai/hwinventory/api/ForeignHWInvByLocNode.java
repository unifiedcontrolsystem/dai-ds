package com.intel.dai.hwinventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0s0b0n0
 */
@ToString
class ForeignHWInvByLocNode extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocProcessor> Processors;
    List<ForeignHWInvByLocMemory> Memory;
}
