package com.intel.dai.hwinventory.api;

import lombok.ToString;

@ToString
public class Requester {
    RestRequest initiateDiscovery;
    RestRequest getDiscoveryStatus;
    RestRequest getHwInventorySnapshot;
    RestRequest getHWInventoryUpdate;

    public Requester() {
        initiateDiscovery = new RestRequest();
        getDiscoveryStatus = new RestRequest();
        getHwInventorySnapshot = new RestRequest();
        getHWInventoryUpdate = new RestRequest();
    }
}
