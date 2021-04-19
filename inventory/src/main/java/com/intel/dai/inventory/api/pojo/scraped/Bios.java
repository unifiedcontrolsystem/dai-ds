// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.pojo.scraped;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Bios {
    public String BIOS_Revision;
    public String Firmware_Revision;
    public String Version;
    public String ROM_Size;
    public String Address;
    public String Release_Date;
}
