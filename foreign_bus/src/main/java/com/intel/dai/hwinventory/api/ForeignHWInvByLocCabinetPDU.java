package com.intel.dai.hwinventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0m0p0
 */
@ToString
class ForeignHWInvByLocCabinetPDU extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocCabinetPDUOutlet> CabinetPDUOutlets;
}
