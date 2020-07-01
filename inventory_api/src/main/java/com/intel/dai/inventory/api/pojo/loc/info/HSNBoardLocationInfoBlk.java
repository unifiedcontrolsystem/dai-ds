// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc.info;

import lombok.ToString;

@ToString
public class HSNBoardLocationInfoBlk {
    public String Id;
    public String Name;
    public String Description;
    public String HostName;

    public HSNBoardLocationInfoBlk() {
        Id = "";
        Name = "";
        Description = "";
        HostName = "";
    }
}
