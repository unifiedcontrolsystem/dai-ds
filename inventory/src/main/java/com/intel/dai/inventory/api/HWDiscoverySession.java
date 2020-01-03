package com.intel.dai.inventory.api;

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
}

@ToString
public class HWDiscoverySession {
    ProviderClassMap providerClassMap;
    TokenAuthProvider tokenAuthProvider;
    ProviderConfigurations providerConfigurations;
}
