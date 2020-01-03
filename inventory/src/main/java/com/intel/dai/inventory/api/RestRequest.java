package com.intel.dai.inventory.api;

import lombok.ToString;

@ToString
public class RestRequest {
    String endpoint;
    String verb;
    String resource;

    public RestRequest() {
        endpoint = "";
        verb = "";
        resource = "";
    }
}
