// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

/**
 * Note that a constructor is necessary to ensure that locs is never null.  This means that
 * an empty json results in an empty loc array.
 */
@ToString
public class HWInvTree {
    public List<HWInvLoc> locs;
    public HWInvTree() {
        locs = new ArrayList<>();
    }
}
