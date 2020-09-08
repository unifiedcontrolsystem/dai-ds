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

    /**
     * We explicitly initialize each field in order to make the meaning of
     * null clear.  The constructor determines the default value of a json field.
     */
    public ForeignFRU() {
//        FRUID = "";
//        Type = "";
//        Subtype = "";

        FRUID = null;
        Type = null;
        Subtype = null;

        MemoryFRUInfo = null;
        ProcessorFRUInfo = null;
        NodeFRUInfo = null;
        NodeEnclosureFRUInfo = null;
        HSNBoardFRUInfo = null;
        DriveFRUInfo = null;
    }
}

