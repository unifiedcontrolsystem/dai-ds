package com.intel.dai.eventsim;

import java.util.Map;

@FunctionalInterface
public interface NetworkSimulator {
    String routeHandler(Map<String, String> params) throws Exception;
}
