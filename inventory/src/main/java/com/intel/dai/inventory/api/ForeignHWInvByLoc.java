package com.intel.dai.inventory.api;

import lombok.ToString;

/**
 * Examples are derived from the foreign documentation.  (?) denotes missing information from the documentation.
 */

@ToString
class ForeignHWInvByLoc {
    String ID;
    String Type;
    int Ordinal;
    String Status;
    String HWInventoryByLocationType;
    ForeignFRU PopulatedFRU;
}
