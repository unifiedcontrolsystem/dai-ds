package com.intel.dai.hwinventory.api;

import lombok.ToString;
import java.util.List;

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
