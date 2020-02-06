// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
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

    public ForeignHWInvByLoc() {
        ID = "";
        Type = "";
        Ordinal = -1;
        Status = "";
        HWInventoryByLocationType = "";
        PopulatedFRU = null;
    }
}
