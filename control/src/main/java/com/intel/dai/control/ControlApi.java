// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.control;

import com.intel.logging.Logger;
import com.intel.dai.dsapi.WorkQueue;

import java.io.IOException;
import java.util.Map;

public class ControlApi {

    private Logger logger;
    private String adapterName;
    private WorkQueue workQueue;

    public ControlApi(String adapterName, String adapterType, WorkQueue workQueue, Logger log) {
    }

    //--------------------------------------------------------------------------
    // Power On a node.
    //--------------------------------------------------------------------------
    public void powerOnNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sJobId, String sWorkItemAdapterType, long lWorkItemId) throws IOException, InterruptedException
    {
    }   // End powerOnNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sJobId, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Power Off a node.
    //--------------------------------------------------------------------------
    public void powerOffNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, InterruptedException
    {
    }   // End powerOffNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)

    public void powerOffNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sJobId, String sWorkItemAdapterType, long lWorkItemId) throws IOException, InterruptedException
    {
    }   // End powerOffNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sJobId, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Shutdown a node.
    //--------------------------------------------------------------------------
    public void shutdownNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, InterruptedException
    {
    }   // End shutdownNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)

    public void shutdownNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sJobId, String sWorkItemAdapterType, long lWorkItemId) throws IOException, InterruptedException
    {
    }   // End shutdownNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sJobId, String sWorkItemAdapterType, long lWorkItemId)

    //--------------------------------------------------------------------------
    // Power cycle a node.
    //--------------------------------------------------------------------------
    public void powerCycleNode(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator,
                               String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp,
                               String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, InterruptedException
    {
    }   // End powerCycleNode(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)

    //--------------------------------------------------------------------------
    // Reset one or more nodes (similar to powering off and back on the nodes).
    //--------------------------------------------------------------------------
    public void resetNodes(String[] saNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, InterruptedException
    {
    }   // End resetNodes(String[] saNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Increase a node's fan speed.
    //--------------------------------------------------------------------------
    public void increaseNodeFanSpeed(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, InterruptedException
    {
    }   // End increaseNodeFanSpeed(String sNodeLctn, Map<String, String> mapNodeLctnsToAggregator, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)

}
