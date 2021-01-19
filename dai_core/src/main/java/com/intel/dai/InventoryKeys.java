// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

public class InventoryKeys {
    public static final String SERIAL_NUMBER = "fru/baseboard/board/serial_number";
    public static final String PRODUCT_NAME  = "fru/baseboard/board/product_name";
    public static final String MANUFACTURER  = "fru/baseboard/board/manufacturer";
    public static final String PART_NUMBER   = "fru/baseboard/board/part_number";
    public static final String BIOS_VERSION  = "bios/bios_version";
    public static final String SYSTEM_MEMORY = "inventory/memory/TotalSystemMemoryGiB";

    private static final String TOTAL_CORES   = "cpu_info/CPU%s/TotalCores";

    public static String totalCores(Integer cpu) {
        return String.format(TOTAL_CORES, cpu);
    }
}
