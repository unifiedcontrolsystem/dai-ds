// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.dsapi;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class HWInvHistory {
    public List<HWInvHistoryEvent> events;
    public HWInvHistory() {
        events = new ArrayList<>();
    }
}
