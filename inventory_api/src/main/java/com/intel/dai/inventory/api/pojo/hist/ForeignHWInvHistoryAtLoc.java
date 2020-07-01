// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.pojo.hist;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class ForeignHWInvHistoryAtLoc {
    public String ID;
    public List<ForeignHWInvHistoryEvent> History;

    public ForeignHWInvHistoryAtLoc() {
        ID = "";
        History = new ArrayList<>();
    }
}
