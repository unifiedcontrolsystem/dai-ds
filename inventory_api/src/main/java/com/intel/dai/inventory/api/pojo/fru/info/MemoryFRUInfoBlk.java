// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.fru.info;

import lombok.ToString;

@ToString
public class MemoryFRUInfoBlk {
    public String BaseModuleType;
    public int BusWidthBits;
    public int CapacityMiB;
    public int DataWidthBits;
    public String ErrorCorrection;
    public String Manufacturer;
    public String MemoryType;
    public String MemoryDeviceType;
    public int OperatingSpeedMhz;
    public String PartNumber;
    public int RankCount;
    public String SerialNumber;

    public MemoryFRUInfoBlk() {
        BusWidthBits = -1;
        CapacityMiB = -1;
        DataWidthBits = -1;
        OperatingSpeedMhz = -1;
        RankCount = -1;

        BaseModuleType ="";
        ErrorCorrection ="";
        Manufacturer = "";
        MemoryType ="";
        MemoryDeviceType ="";
        PartNumber = "";
        SerialNumber = "";
    }
}
