// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;

@ToString
class ForeignFRU {
    String FRUID;
    String Type;
    String Subtype;
    String HWInventoryByFRUType;
}
