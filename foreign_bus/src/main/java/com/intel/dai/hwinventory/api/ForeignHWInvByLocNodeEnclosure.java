package com.intel.dai.hwinventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0s0b0    (?)
 */
@ToString
class ForeignHWInvByLocNodeEnclosure extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocNode> Nodes;
}
