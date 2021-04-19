// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.pojo.scraped;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Dimm {
    public String Location;
    public String Size;
    public int Rank;
    public String Serial;
    public String Part;
    public String Speed;
}
