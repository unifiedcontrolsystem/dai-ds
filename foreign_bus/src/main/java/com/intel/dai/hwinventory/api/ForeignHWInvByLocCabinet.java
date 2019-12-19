package com.intel.dai.hwinventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0
 */
@ToString
class ForeignHWInvByLocCabinet extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocChassis> Chassis;
}
