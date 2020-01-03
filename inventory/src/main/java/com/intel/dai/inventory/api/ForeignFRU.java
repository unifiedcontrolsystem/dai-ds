package com.intel.dai.inventory.api;

import lombok.ToString;

@ToString
class ForeignFRU {
    String FRUID;
    String Type;
    String Subtype;
    String HWInventoryByFRUType;
}
