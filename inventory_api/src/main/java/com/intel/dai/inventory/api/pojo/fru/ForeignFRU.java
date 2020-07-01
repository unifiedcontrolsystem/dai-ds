// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.fru;

import com.google.gson.Gson;
import com.intel.dai.inventory.api.pojo.fru.info.*;
import lombok.ToString;

@ToString
public class ForeignFRU {
    public String FRUID;
    public String Type;
    public String Subtype;

    public MemoryFRUInfoBlk MemoryFRUInfo;
    public ProcessorFRUInfoBlk ProcessorFRUInfo;
    public NodeFRUInfoBlk NodeFRUInfo;
    public NodeEnclosureFRUInfoBlk NodeEnclosureFRUInfo;
    public HSNBoardFRUInfoBlk HSNBoardFRUInfo;
    public DriveFRUInfoBlk DriveFRUInfo;

    public String info() {
        Gson gson = new Gson();

        if (MemoryFRUInfo != null) {
            return gson.toJson(MemoryFRUInfo);
        }
        if (ProcessorFRUInfo != null) {
            return gson.toJson(ProcessorFRUInfo);
        }
        if (NodeFRUInfo != null) {
            return gson.toJson(NodeFRUInfo);
        }
        if (NodeEnclosureFRUInfo != null) {
            return gson.toJson(NodeEnclosureFRUInfo);
        }
        if (HSNBoardFRUInfo != null) {
            return gson.toJson(HSNBoardFRUInfo);
        }
        if (DriveFRUInfo != null) {
            return gson.toJson(DriveFRUInfo);
        }
        return "";
    }

    public ForeignFRU() {
        FRUID = "";
        Type = "";
        Subtype = "";

        MemoryFRUInfo = null;
        ProcessorFRUInfo = null;
        NodeFRUInfo = null;
        NodeEnclosureFRUInfo = null;
        HSNBoardFRUInfo = null;
        DriveFRUInfo = null;
    }
}

