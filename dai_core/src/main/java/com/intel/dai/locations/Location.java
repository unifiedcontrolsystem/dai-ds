/* Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

*/
package com.intel.dai.locations;

import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.BadInputException;
import com.intel.logging.Logger;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Location {

    private IAdapter adapter;
    private Set<String> nodes;
    private Logger log;

    /* Nodes can be hostname or locations or combination of both*/
    public Location(IAdapter adapter, Set<String> nodes, Logger log) {
        this.adapter = adapter;
        this.nodes = nodes;
        this.log = log;
    }

    public Map<String, String> getLocation() throws BadInputException {
        Map<String, String> allNodeLocations;
        allNodeLocations = adapter.mapCompNodeHostNameToLctn();
        return commonCode(allNodeLocations);
    }

    public Map<String, String> getHostname() throws BadInputException {
        Map<String, String> allNodehostnames;
        allNodehostnames = adapter.mapCompNodeLctnToHostName();
        return commonCode(allNodehostnames);
    }

    private Map<String, String> commonCode(Map<String, String> allNodesHostnameLocation) throws BadInputException {

        /* Based on which function is calling Key and value will contain hostname or location interchangeably */
        HashMap<String, String> hostnameLocationMap = new HashMap<>();

        for(String node:nodes) {
            String device = allNodesHostnameLocation.get(node);
            if(device == null) {
                if (allNodesHostnameLocation.containsValue(node)) {
                    hostnameLocationMap.put(node, node);
                }
                else {
                    log.error(node+ " is not a valid location or hostname of a node");
                    throw new BadInputException(node + " is a Bad input. It isn't a location or hostname of a node");
                }
            } else {
                hostnameLocationMap.put(node, device);
            }
        }
        return hostnameLocationMap;
    }

    /*Returns a Map where key is aggregator and value is String array of node locations */
    public Map<String, Set<String>> getAggregatorNodeLocations() throws BadInputException {

        Map<String, Set<String>> aggregatorLocations = new HashMap<>();
        Map<String, String> nodeLocations = getLocation();
        Map<String, String> allAggregatorLocations;
        if(nodeLocations.isEmpty()) {
            log.error("Unable to get location info for the nodes");
            throw new BadInputException("Nodes specified doesn't have any valid locations in database");
        }
        try {
            allAggregatorLocations = adapter.mapNodeLctnToAggregator();
        } catch (IOException | ProcCallException e) {
            log.exception(e);
            throw new BadInputException(e.toString());
        }
        for (Map.Entry<String, String> entry : nodeLocations.entrySet()) {
            String aggregatorLocation = allAggregatorLocations.get(entry.getValue());
            if(aggregatorLocation == null) {
                /* Not a valid node location */
                log.error(entry.getValue() + " doesn't have an aggregator associated with it");
                throw new BadInputException(entry.getValue() + " doesn't have a aggregator associated with it");
            } else {
                if(aggregatorLocations.containsKey(aggregatorLocation)) {
                    Set<String> nodesConnectedToAggregator = aggregatorLocations.get(aggregatorLocation);
                    nodesConnectedToAggregator.add(entry.getKey());
                    aggregatorLocations.put(aggregatorLocation, nodesConnectedToAggregator);
                } else {
                    Set<String> nodesConnectedToAggregator = new HashSet<>();
                    nodesConnectedToAggregator.add(entry.getKey());
                    aggregatorLocations.put(aggregatorLocation, nodesConnectedToAggregator);

                }
            }
        }
        return aggregatorLocations;
    }


}
