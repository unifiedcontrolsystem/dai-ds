package com.intel.dai.hwinventory.api;

import lombok.ToString;

@ToString
class ForeignFRU {
    String FRUID;
    String Type;
    String Subtype;
    String HWInventoryByFRUType;
}
