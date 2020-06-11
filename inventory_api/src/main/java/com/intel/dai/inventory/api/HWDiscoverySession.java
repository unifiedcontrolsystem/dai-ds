// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.google.gson.internal.$Gson$Preconditions;
import lombok.ToString;

@ToString
class ProviderClassMap {
    String tokenAuthProvider;
    String requester;
}

@ToString
class TokenAuthProvider {
    String tokenServer;
    String realm;
    String clientId;
    String clientSecret;
}

@ToString
class ProviderConfigurations {
    TokenAuthProvider tokenAuthProvider;
    Requester requester;

    ProviderConfigurations() {
        tokenAuthProvider = new TokenAuthProvider();
        requester = new Requester();
    }
}

@ToString
public class HWDiscoverySession {
    ProviderClassMap providerClassMap;
    ProviderConfigurations providerConfigurations;

    public HWDiscoverySession() {
        providerClassMap = new ProviderClassMap();
        providerConfigurations = new ProviderConfigurations();
    }
}
