// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.dsapi.NodeIpAndBmcIp;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.client.ProcCallException;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

import java.io.IOException;
import java.util.*;


/**
 * Description of class VoltDbStatus.
 */
public class VoltDbNodeInformation implements NodeInformation {
    public VoltDbNodeInformation(Logger log, String[] servers) {
        log_ = log;
        servers_ = servers;
    }

    public synchronized void initialize() {
        if(servers_ != null)
            VoltDbClient.initializeVoltDbClient(servers_);
        voltDb_ = getClient();
    }

    protected Client getClient() {
        return VoltDbClient.getVoltClientInstance();
    }

    @Override
    public synchronized String getComputeNodeState(String location) throws DataStoreException {
        try {
            ClientResponse response = voltDb_.callProcedure("ComputeNodeState", location);
            log_.info("called stored procedure %s, Location=%s", "ComputeNodeState", location);
            VoltTable vt = response.getResults()[0];
            vt.advanceRow();
            return vt.getString("State");
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

    @Override
    public synchronized Map<String, String> getComputeHostnameFromLocationMap() throws DataStoreException {
        if (locationToComputeNodeHostname_ == null) {
            try {
                locationToComputeNodeHostname_ = new HashMap<>();
                hostnameToComputeNodeLocation_ = new HashMap<>();
                ClientResponse response = voltDb_.callProcedure("ComputeNodeListLctnAndHostname");
                fillMaps(response, locationToComputeNodeHostname_, hostnameToComputeNodeLocation_);
            } catch(IOException | ProcCallException e) {
                locationToComputeNodeHostname_ = null;
                hostnameToComputeNodeLocation_ = null;
                throw new DataStoreException("Failed to get compute node information", e);
            }
        }
        return locationToComputeNodeHostname_;
    }

    @Override
    public synchronized Map<String, String> getServiceHostnameFromLocationMap() throws DataStoreException {
        if (locationToServiceNodeHostname_ == null) {
            try {
                locationToServiceNodeHostname_ = new HashMap<>();
                hostnameToServiceNodeLocation_ = new HashMap<>();
                ClientResponse response = voltDb_.callProcedure("ServiceNodeListLctnAndHostname");
                fillMaps(response, locationToServiceNodeHostname_, hostnameToServiceNodeLocation_);
            } catch(IOException | ProcCallException e) {
                locationToServiceNodeHostname_ = null;
                hostnameToServiceNodeLocation_ = null;
                throw new DataStoreException("Failed to get compute node information", e);
            }
        }
        return locationToServiceNodeHostname_;
    }

    @Override
    public synchronized Map<String, String> getComputeNodeLocationFromHostnameMap() throws DataStoreException {
        getComputeHostnameFromLocationMap();
        return hostnameToComputeNodeLocation_;
    }

    @Override
    public synchronized Map<String, String> getServiceNodeLocationFromHostnameMap() throws DataStoreException {
        getServiceHostnameFromLocationMap();
        return hostnameToServiceNodeLocation_;
    }

    @Override
    public synchronized Map<String, Long> getComputeNodeSequenceNumberFromLocationMap() throws DataStoreException {
        if (locationToSequenceNumber_ == null) {
            try {
                locationToSequenceNumber_ = new HashMap<>();
                ClientResponse response = voltDb_.callProcedure("ComputeNodeListLctnAndSeqNum");
                VoltTable result = response.getResults()[0];
                for (int row = 0; row < result.getRowCount(); ++row) {
                    result.advanceRow();
                    locationToSequenceNumber_.put(result.getString("Lctn"), result.getLong("SequenceNumber"));
                }
            } catch(IOException | ProcCallException e) {
                locationToSequenceNumber_ = null;
                throw new DataStoreException("Failed to get the compute node sequence number data", e);
            }
        }
        return locationToSequenceNumber_;
    }

    @Override
    public synchronized Map<String, String> getAggregatorLocationFromNodeLocationMap() throws DataStoreException {
        if (locationToAggregator_ == null) {
            locationToAggregator_ = new HashMap<>();
            try {
                ClientResponse response = voltDb_.callProcedure("ComputeNodesList");
                VoltTable result = response.getResults()[0];
                for (int row = 0; row < result.getRowCount(); ++row) {
                    result.advanceRow();
                    locationToAggregator_.put(result.getString("Lctn"), result.getString("Aggregator"));
                }

                response = voltDb_.callProcedure("ServiceNodesList");
                result = response.getResults()[0];
                for (int row = 0; row < result.getRowCount(); ++row) {
                    result.advanceRow();
                    locationToAggregator_.put(result.getString("Lctn"), result.getString("Aggregator"));
                }
            } catch(IOException | ProcCallException e) {
                locationToAggregator_ = null;
                throw new DataStoreException("Failed to get the compute node aggregator data", e);
            }
        }
        return locationToAggregator_;
    }

    @Override
    public synchronized Map<String, NodeIpAndBmcIp> getNodeAndBmcIPsFromLocationMap() throws DataStoreException {
        if (locationToNodeIpAndBmcIP_ == null) {
            locationToNodeIpAndBmcIP_ = new HashMap<>();
            locationToComputeNodeIpAndBmcIP_ = new HashMap<>();
            try {
                ClientResponse response = voltDb_.callProcedure("ComputeNodesList");
                VoltTable result = response.getResults()[0];
                for (int row = 0; row < result.getRowCount(); ++row) {
                    result.advanceRow();
                    NodeIpAndBmcIp ips = new NodeIpAndBmcIp(result.getString("IpAddr"), result.getString("BmcIpAddr"));
                    String location = result.getString("Lctn");
                    locationToComputeNodeIpAndBmcIP_.put(location, ips);
                    locationToNodeIpAndBmcIP_.put(location, ips);
                }

                response = voltDb_.callProcedure("ServiceNodesList");
                result = response.getResults()[0];
                for (int row = 0; row < result.getRowCount(); ++row) {
                    result.advanceRow();
                    NodeIpAndBmcIp ips = new NodeIpAndBmcIp(result.getString("IpAddr"), result.getString("BmcIpAddr"));
                    locationToNodeIpAndBmcIP_.put(result.getString("Lctn"), ips);
                }
            } catch(IOException | ProcCallException e) {
                locationToNodeIpAndBmcIP_ = null;
                locationToComputeNodeIpAndBmcIP_ = null;
                throw new DataStoreException("Failed to get the node's IP and its' BMC IP data", e);
            }
        }
        return locationToNodeIpAndBmcIP_;
    }

    @Override
    public synchronized Map<String, NodeIpAndBmcIp> getComputeNodeAndBmcIPsFromLocationMap() throws DataStoreException {
        getNodeAndBmcIPsFromLocationMap();
        return locationToComputeNodeIpAndBmcIP_;
    }

    @Override
    public synchronized boolean isComputeNodeLocation(String location) throws DataStoreException {
        return getComputeHostnameFromLocationMap().containsKey(location);
    }

    @Override
    public synchronized boolean isServiceNodeLocation(String location) throws DataStoreException {
        return getServiceHostnameFromLocationMap().containsKey(location);
    }

    @Override
    public synchronized String getNodesBmcIpAddress(String location) throws DataStoreException {
        return getNodeAndBmcIPsFromLocationMap().get(location).bmcIpAddress;
    }

    @Override
    public synchronized String getNodesIpAddress(String location) throws DataStoreException {
        return getNodeAndBmcIPsFromLocationMap().get(location).nodeIpAddress;
    }

    @Override
    public synchronized List<String> getComputeNodeLocations() throws DataStoreException {
        if(computeNodeLocationsSorted_ == null) {
            Map<String, String> locationMap = getComputeHostnameFromLocationMap();
            computeNodeLocationsSorted_ = new ArrayList<>(locationMap.keySet());
            computeNodeLocationsSorted_.sort(this::compareStrings);
        }
        return computeNodeLocationsSorted_;
    }

    @Override
    public synchronized List<String> getServiceNodeLocations() throws DataStoreException {
        if(serviceNodeLocationsSorted_ == null) {
            Map<String, String> locationMap = getServiceHostnameFromLocationMap();
            serviceNodeLocationsSorted_ = new ArrayList<>(locationMap.keySet());
            serviceNodeLocationsSorted_.sort(this::compareStrings);
        }
        return serviceNodeLocationsSorted_;
    }

    @Override
    public synchronized List<String> getNodeLocations() throws DataStoreException {
        if(nodeLocationsSorted_ == null) {
            nodeLocationsSorted_ = new ArrayList<>(getComputeNodeLocations());
            nodeLocationsSorted_.addAll(getServiceNodeLocations());
            nodeLocationsSorted_.sort(this::compareStrings);
        }
        return nodeLocationsSorted_;
    }

    private int compareStrings(String s1, String s2) {
        return s1.compareTo(s2);
    }

    private void fillMaps(ClientResponse response, Map<String, String> forwardLookup,
                          Map<String, String> backwardLookup) {
        VoltTable result = response.getResults()[0];
        for (int row = 0; row < result.getRowCount(); ++row) {
            result.advanceRow();
            forwardLookup.put(result.getString("Lctn"), result.getString("HostName"));
            backwardLookup.put(result.getString("HostName"), result.getString("Lctn"));
        }
    }

    // Object state...
    private Logger log_;
    private Client voltDb_;
    private String[] servers_;
    Map<String, String> locationToComputeNodeHostname_ = null;
    Map<String, String> locationToServiceNodeHostname_ = null;
    Map<String, String> hostnameToComputeNodeLocation_ = null;
    Map<String, String> hostnameToServiceNodeLocation_ = null;
    Map<String, Long> locationToSequenceNumber_ = null;
    Map<String, String> locationToAggregator_ = null;
    Map<String, NodeIpAndBmcIp> locationToNodeIpAndBmcIP_ = null;
    Map<String, NodeIpAndBmcIp> locationToComputeNodeIpAndBmcIP_ = null;
    private List<String> computeNodeLocationsSorted_ = null;
    private List<String> serviceNodeLocationsSorted_ = null;
    private List<String> nodeLocationsSorted_ = null;
}
