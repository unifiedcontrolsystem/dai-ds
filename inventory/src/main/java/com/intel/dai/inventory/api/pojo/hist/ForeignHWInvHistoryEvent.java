// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.pojo.hist;

import lombok.ToString;


@ToString
public class ForeignHWInvHistoryEvent {
    public String ID;
    public String FRUID;
    public String Timestamp;
    public String EventType;

    public ForeignHWInvHistoryEvent() {
        ID = "";
        FRUID = "";
        Timestamp = "";
        EventType = "";
    }
}
