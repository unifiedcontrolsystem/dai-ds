// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.cfg;

import lombok.ToString;

@ToString
public class TokenAuthProvider {
    public String tokenServer;
    public String realm;
    public String clientId;
    public String clientSecret;
}
