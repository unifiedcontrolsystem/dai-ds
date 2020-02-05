// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;

import java.util.ArrayList;

/**
 * Note that only the components and the state need to be extracted from
 * the state change notification.
 */
@ToString
public class ForeignHWInvChangeNotification {
    public ArrayList<String> Components;
    public String State;

    public ForeignHWInvChangeNotification() {
        Components = new ArrayList<>();
        State = "";
    }
}
