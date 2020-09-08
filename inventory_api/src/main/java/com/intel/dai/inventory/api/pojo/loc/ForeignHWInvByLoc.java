// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc;

import com.google.gson.Gson;
import com.intel.dai.inventory.api.pojo.fru.ForeignFRU;
import com.intel.dai.inventory.api.pojo.loc.info.*;
import lombok.ToString;


/**
 * Pojo containing information extracted from a foreign location.  Note that null
 * handling can be tricky.
 */
@ToString
public class ForeignHWInvByLoc {
    public String ID;
    public String Type;
    public int Ordinal;
    public String Status;
    public ForeignFRU PopulatedFRU;

    public MemoryLocationInfoBlk MemoryLocationInfo;
    public ProcessorLocationInfoBlk ProcessorLocationInfo;
    public NodeLocationInfoBlk NodeLocationInfo;
    public NodeEnclosureLocationInfoBlk NodeEnclosureLocationInfo;
    public HSNBoardLocationInfoBlk HSNBoardLocationInfo;
    public DriveLocationInfoBlk DriveLocationInfo;

    public String info() {
        Gson gson = new Gson();

        if (MemoryLocationInfo != null) {
            return gson.toJson(MemoryLocationInfo);
        }
        if (ProcessorLocationInfo != null) {
            return gson.toJson(ProcessorLocationInfo);
        }
        if (NodeLocationInfo != null) {
            return gson.toJson(NodeLocationInfo);
        }
        if (NodeEnclosureLocationInfo != null) {
            return gson.toJson(NodeEnclosureLocationInfo);
        }
        if (HSNBoardLocationInfo != null) {
            return gson.toJson(HSNBoardLocationInfo);
        }
        if (DriveLocationInfo != null) {
            return gson.toJson(DriveLocationInfo);
        }
        return "";
    }

    /**
     * We explicitly initialize each field in order to make the meaning of
     * null clear.  The constructor determines the default value of a json field.
     */
    public ForeignHWInvByLoc() {
        Ordinal = -1;

//        ID = "";
//        Type = "";
//        Status = "";

        ID = null;
        Type = null;
        Status = null;
        PopulatedFRU = null;

        MemoryLocationInfo = null;
        ProcessorLocationInfo = null;
        NodeLocationInfo = null;
        NodeEnclosureLocationInfo = null;
        HSNBoardLocationInfo = null;
        DriveLocationInfo = null;
    }
}

