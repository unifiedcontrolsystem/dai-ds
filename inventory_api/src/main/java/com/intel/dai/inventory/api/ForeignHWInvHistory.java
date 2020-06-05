// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
class ForeignHWInvHistory {
    List<ForeignHWInvHistoryAtLoc> Components;

    public ForeignHWInvHistory() {
        Components = new ArrayList<>();
    }
}
