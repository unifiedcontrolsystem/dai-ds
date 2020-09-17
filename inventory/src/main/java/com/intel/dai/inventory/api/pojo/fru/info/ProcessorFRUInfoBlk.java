// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.fru.info;

import lombok.ToString;

@ToString
public class ProcessorFRUInfoBlk {
    @ToString
    public static class ProcessorIdBlk {
        public String EffectiveFamily;
        public String EffectiveModel;
        public String IdentificationRegisters;
        public String MicrocodeInfo;
        public String Step;
        public String VendorID;

        public ProcessorIdBlk() {
            EffectiveFamily = "";
            EffectiveModel = "";
            IdentificationRegisters = "";
            MicrocodeInfo = "";
            Step = "";
            VendorID = "";
        }
    }
    public String InstructionSet;
    public String Manufacturer;
    public int MaxSpeedMHz;
    public String Model;
    public String ProcessorArchitecture;
    public ProcessorIdBlk ProcessorId;
    public String ProcessorType;
    public int TotalCores;
    public int TotalThreads;

    public ProcessorFRUInfoBlk() {
        MaxSpeedMHz = -1;
        TotalCores = -1;
        TotalThreads = -1;

        InstructionSet = "";
        Manufacturer = "";
        Model = "";
        ProcessorArchitecture = "";
        ProcessorType = "";

        ProcessorId = new ProcessorIdBlk();
    }
}
