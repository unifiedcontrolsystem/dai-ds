// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.fru.info;

import lombok.ToString;

@ToString
public class HSNBoardFRUInfoBlk {
    public String AssetTag;
    public String ChassisType;
    public String Model;
    public String Manufacturer;
    public String PartNumber;
    public String SerialNumber;
    public String SKU;

    public HSNBoardFRUInfoBlk() {
        AssetTag = "";
        ChassisType = "";
        Model = "";
        Manufacturer = "";
        PartNumber = "";
        SerialNumber = "";
        SKU = "";
    }
}
