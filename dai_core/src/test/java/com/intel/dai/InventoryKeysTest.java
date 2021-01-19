// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class InventoryKeysTest {
    @Test
    public void testTotalCores() {
        assertEquals("cpu_info/CPU1/TotalCores",
                InventoryKeys.totalCores(1));
    }
}
