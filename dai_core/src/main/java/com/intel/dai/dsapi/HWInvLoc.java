// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import lombok.ToString;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.Map;

@ToString
public class HWInvLoc {
    public String ID;
    public String Type;
    public int Ordinal;
    public String Info;

    public String FRUID;
    public String FRUType;
    public String FRUSubType;
    public String FRUInfo;

    public HWInvLoc() {
        Ordinal = -1;

        ID = "";
        Type = "";
        Info = "";
        FRUID = "";
        FRUType = "";
        FRUSubType = "";
        FRUInfo = "";
    }

    /**
     * Generates json fields to be merged into the HWInfo json blob.
     * @return JSONObject object containing the fields
     */
    public JsonObject toHWInfoJsonFields() {
        Map<String, String> translation = Map.of(
                "Memory","DIMM",
                "Processor", "CPU",
                "Drive", "DRIVE",
                "Node", "NODE");

        JsonObject entries = new JsonObject();

        String slotName = Type.equals("Node") ? "fru/NODE/loc"
                : String.format("fru/%s%d", translation.get(Type), Ordinal);

        String locField = String.format("%s/loc", slotName);
        entries.put(locField, generateEntry(ID));
        String infoField = String.format("%s/loc_info", slotName);
        entries.put(infoField, generateEntry(Info));
        String fruIdField = String.format("%s/fru_id", slotName);
        entries.put(fruIdField, generateEntry(FRUID));
        String fruInfoField = String.format("%s/fru_info", slotName);
        entries.put(fruInfoField, generateEntry(FRUInfo));

        return entries;
    }

    private JsonObject generateEntry(String value) {
        JsonObject entries = new JsonObject();
        entries.put("provider", "shasta");
        entries.put("value", value);
        return entries;
    }
}
