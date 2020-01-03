package com.intel.dai.inventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0m0p0
 */
@ToString
class ForeignHWInvByLocCabinetPDU extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocCabinetPDUOutlet> CabinetPDUOutlets;
}
