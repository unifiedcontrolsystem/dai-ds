// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc.info;

import lombok.ToString;

@ToString
public class NodeLocationInfoBlk {
    @ToString
    public static class ProcessorSummaryBlk {
        public int Count;
        public String Model;

        public ProcessorSummaryBlk() {
            Count = -1;
            Model = "";
        }
    }
    @ToString
    public static class MemorySummaryBlk {
        public int TotalSystemMemoryGiB;

        public MemorySummaryBlk() {
            TotalSystemMemoryGiB = -1;
        }
    }

    public String Id;
    public String Name;
    public String Description;
    public String HostName;

    public ProcessorSummaryBlk ProcessorSummary;
    public MemorySummaryBlk MemorySummary;

    public NodeLocationInfoBlk() {
        Id = "";
        Name = "";
        Description = "";
        HostName = "";

        ProcessorSummary = new ProcessorSummaryBlk();
        MemorySummary = new MemorySummaryBlk();
    }
}
