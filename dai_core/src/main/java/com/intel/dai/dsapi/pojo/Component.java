// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.dsapi.pojo;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class Component {
    public int id;
    public String mac;
    public int timestamp;

    public Component(FruHost component) {
        id = component.id;
        mac = component.mac;
        timestamp = component.timestamp;
    }
}
