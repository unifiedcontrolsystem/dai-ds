// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc.info;

import lombok.ToString;

@ToString
public class MemoryLocationInfoBlk {
    @ToString
    public static class MemoryLocationBlk {
        public int Socket;
        public int MemoryController;
        public int Channel;
        public int Slot;

        public MemoryLocationBlk() {
            Socket = -1;
            MemoryController = -1;
            Channel = -1;
            Slot = -1;
        }
    }
    public String Id;
    public String Name;
    public MemoryLocationBlk MemoryLocation;
    public MemoryLocationInfoBlk() {
        Id = "";
        Name = "";
        MemoryLocation = new MemoryLocationBlk();
    }
}
