// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.fru.info;

import lombok.ToString;

@ToString
public class DriveFRUInfoBlk {
    public String Manufacturer;
    public String SerialNumber;
    public String PartNumber;
    public String Model;
    public String SKU;
    public String Protocol;
    public String MediaType;
    public String EncryptionAbility;
    public String EncryptionStatus;

    public int CapacityBytes;
    public int RotationSpeedRPM;
    public int BlockSizeBytes;
    public int CapableSpeedGbs;
    public int NegotiatedSpeedGbs;
    public int PredictedMediaLifeLeftPercent;

    public boolean FailurePredicted;

    public DriveFRUInfoBlk() {
        Manufacturer = "";
        SerialNumber = "";
        PartNumber = "";
        Model = "";
        SKU = "";
        Protocol = "";
        MediaType = "";
        EncryptionAbility = "";
        EncryptionStatus = "";

        CapacityBytes = -1;
        RotationSpeedRPM = -1;
        BlockSizeBytes = -1;
        CapableSpeedGbs = -1;
        NegotiatedSpeedGbs = -1;
        PredictedMediaLifeLeftPercent = -1;

        FailurePredicted = true;
    }

}
