// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.AdapterOperations;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.dsapi.RasEventLog;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.client.ProcedureCallback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of class VoltDbAdapterOperations.
 */
public class VoltDbAdapterOperations implements AdapterOperations {
    public VoltDbAdapterOperations(Logger logger, String[] servers, AdapterInformation adapter) {
        log_ = logger;
        servers_ = servers;
        adapter_ = adapter;
    }

    @Override
    public long getAdapterInstancesAdapterId() throws ProcCallException, IOException {
        try {
            createClient();
            ClientResponse response = client_.callProcedure("AdapterInfoUsingTypeLctnPid", adapter_.getType(),
                    adapter_.getLocation(), adapter_.getPid());
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.fatal("getAdapterInstancesAdapterId - Stored procedure AdapterInfoUsingTypeLctnPid FAILED - " +
                                "AdapterName=%s, AdapterLocation=%s, AdapterPID=%d, Status=%s, StatusString=%s, " +
                                "AdapterID=%d!",
                        adapter_.getName(), adapter_.getLocation(), adapter_.getPid(),
                        VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                        adapter_.getId());
                throw new RuntimeException(response.getStatusString());
            }
            // Ensure that the adapter entry still exists in the table (it may have already been cleaned up during
            // the shutdown of the adapter instance itself).
            VoltTable vt = response.getResults()[0];
            if (vt.getRowCount() > 0) {
                // the adapter entry still exists in the table.
                vt.advanceRow();
                return vt.getLong("Id");
            }
            return -1;  // could not find the adapter entry for the specified adapter instance.
        } catch(IOException e) {
            VoltDbClient.failedConnection();
            client_ = null;
            throw e;
        }
    }

    @Override
    public void registerAdapter() throws IOException, ProcCallException {
        createClient();
        // Add entry for this particular adapter in the adapter table.
        ClientResponse response = client_.callProcedure("AdapterStarted"
                ,adapter_.getType()     // this particular adapter's type
                ,-1L                    // work item id that requested this method, -1 being used since there is no
                                        //     work item yet associated with this new adapter
                ,adapter_.getLocation() // lctn of the service node that this adapter instance is running on.
                ,adapter_.getPid()      // pid of this adapter instance's process
        );
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("registerAdapter - AdapterStarted stored procedure FAILED for this adapter - " +
                            "Status=%s, StatusString=%s, AdapterType=%s, Lctn=%s, Pid=%d!",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                    adapter_.getType(), adapter_.getLocation(), adapter_.getPid());
            throw new RuntimeException(response.getStatusString());
        }
        // Set/save the value for this adapter's adapter id.
        long lThisAdaptersAdapterId = response.getResults()[0].asScalarLong();
        adapter_.setId(lThisAdaptersAdapterId);
        // Set/save the lctn string of the service node this adapter instance is running on.
        log_.info("registerAdapter - successfully started adapter, AdapterType=%s, AdapterId=%d, Lctn=%s, Pid=%d",
                adapter_.getType(), lThisAdaptersAdapterId, adapter_.getLocation(), adapter_.getPid());
    }

    @Override
    public int shutdownAdapter() throws AdapterException {
        return shutdownAdapter(null);
    }

    @Override
    public int shutdownAdapter(Throwable cause) throws AdapterException {
        createRas();
        int result = 0; // for the shell...
        String reason = "Adapter terminating normally.";
        if(cause != null) {
            try {
                ras_.logRasEventNoEffectedJob(
                        "RasGenAdapterException"
                        ,("Exception=" + cause)             // instance data
                        ,null                               // Lctn
                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event
                                                            // occurred, in micro-seconds since epoch
                        ,adapter_.getType()                 // type of adapter that is requesting this
                        ,adapter_.getBaseWorkItemId()       // requesting work item
                );
                ras_.logRasEventNoEffectedJob("RasGenAdapterAbend"
                        ,("AdapterName=" + adapter_.getName() + ", Reason=exception")
                        ,null                               // lctn
                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event
                                                            // occurred, in micro-seconds since epoch
                        ,adapter_.getType()                 // type of the adapter that is requesting/issuing this
                                                            // invocation
                        ,adapter_.getBaseWorkItemId()       // work item id for the work item that is being
                                                            // processed/executing, that is requesting/issuing this
                                                            // invocation
                );
            } catch (Exception e2) { log_.exception(e2); }
            result = 1;
            reason = "Adapter terminating by exception!";
        }
        tearDownAdapter();
        tearDownAdaptersBaseWorkItem(reason);
        adapter_.signalToShutdown();
        return result;
    }

    @Override
    public void markNodeState(BootState newState, String location, long timestamp, boolean informWlm) {
        createClient();
        createNodeInfoMaps();
        createRas();
        // Check & see if this is a compute node or a service node.
        String storedProcedureName;
        log_.debug("*** Updating node '%s' to new state '%s'", location, newState.toString());
        BasicNodeInfo node = nodes_.get(location);
        if(node == null) {
            log_.error("New boot state for a node that is not in the node database: %s", location);
            return;
        }
        if (nodes_.get(location).isComputeNode())
            storedProcedureName = "ComputeNodeSetState";
        else
            storedProcedureName = "ServiceNodeSetState";

        String sTempNewState = states_.get(newState);
        try {
            client_.callProcedure(new CallbackForNodeStateChange(adapter_,
                            new VoltDbRasEventLog(servers_, adapter_, log_), location, storedProcedureName)
                    , storedProcedureName   // stored procedure name
                    , location              // node's location string
                    , sTempNewState         // node's new state
                    , timestamp             // time that this occurred in microseconds since epoch
                    , adapter_.getType()    // type of the adapter that is requesting/issuing this stored procedure
                    , adapter_.getBaseWorkItemId()); // work item id for the work item that is being
                                                     // processed/executing, that is requesting/issuing this stored
                                                     // procedure
            log_.info("markNode(%s) - called stored procedure %s - Lctn=%s, NewState=%s", newState.toString(),
                    storedProcedureName, location, sTempNewState);
        } catch (IOException e) {
            log_.exception(e, "markNode(%s) - failed to update node state at location: %s", newState.toString(),
                    location);
            return;
        }

        // Tell WLM that it can start using this node (if appropriate).
        if(newState == BootState.NODE_ONLINE && informWlm) {
            // Ensure that this node is owned by the WLM - we cannot tell the WLM to use this node if it is not
            // owned by the WLM subsystem.
            informWlmAboutActiveNode(location);
        }
    }

    @Override
    public void markNodeInErrorState(String location, boolean informWlm) {
        createClient();
        createNodeInfoMaps();
        createRas();
        createWorkQueue();
        String storedProcedureName = (nodes_.get(location).isComputeNode())?"ErrorOnComputeNode":"ErrorOnServiceNode";
        try {
            client_.callProcedure(new CallbackForErrorNodeStateChange(this, location, storedProcedureName, log_)
            , storedProcedureName
            , location
            , adapter_.getType()
            , workQueue_.workItemId());
        } catch(IOException e) {
            log_.exception(e, "Failed to set the node state to error for location %s", location);
            failedToLogNodeStateChange(location, storedProcedureName, null);
        }

        if(informWlm)
            informWlmAboutErroredNode(location);
    }

    void failedToLogNodeStateChange(String location, String storedProcedureName, ClientResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("AdapterName=").append(adapter_.getName()).append(", SpThisIsCallbackFor=")
                .append(storedProcedureName).append(", PertinentInfo=").append(location);
        if(response != null)
            builder.append(", StatusString=").append(response.getStatusString());
            ras_.logRasEventNoEffectedJob(
                    "RasGenAdapterMyCallbackForHouseKeepingNoRtrnValueFailed"
                    , builder.toString()
                    , null
                    , System.currentTimeMillis() * 1000L
                    , adapter_.getType()
                    , workQueue_.workItemId()
            );
    }


    protected void createClient() {
        if (client_ == null) {
            VoltDbClient.initializeVoltDbClient(servers_);
            client_ = VoltDbClient.getVoltClientInstance();
        }
    }

    protected void createRas() {
        if(ras_ == null)
            ras_ = new VoltDbRasEventLog(servers_, adapter_, log_);
    }

    protected void createWorkQueue() {
        if(workQueue_ == null)
            workQueue_ = new VoltDbWorkQueue(servers_, adapter_, log_);
    }

    private void informWlmAboutNode(String location, boolean active) {
        BasicNodeInfo nodeInfo = nodes_.get(location);
        // Ensure that this is a compute node - can not tell WLM to start using a non-compute node.
        if (nodeInfo.isComputeNode()) {
            // Ensure that this node is owned by the WLM - cannot tell the WLM to use this node if it is not owned by
            // the WLM subsystem.
            if (!nodeInfo.isOwnedByWlm()) {
                createNonComputeNodeRasEvent(location);
                return;  // return without telling WLM to start using this node.
            }
            createWorkItemForWlmForNode(location, active);
        }
        else {
            createNonComputeNodeRasEvent(location);
        }
    }

    private void informWlmAboutActiveNode(String location) {
        informWlmAboutNode(location, true);
    }

    private void informWlmAboutErroredNode(String location) {
        informWlmAboutNode(location, false);
    }

    private void createNonComputeNodeRasEvent(String location) {
        log_.error("Cannot tell WLM to begin using this node because the specified node " +
                "is NOT a compute node - Node=%s!", location);
        // Cut a RAS event to capture this occurrence.
        ras_.logRasEventNoEffectedJob("RasWlmCantTellWlmToUseNonComputeNode"
                , null                               // instance data
                , location                           // Lctn
                , System.currentTimeMillis() * 1000L // time that the event that triggered this ras event
                // occurred, in micro-seconds since epoch
                , adapter_.getType()                 // the type of adapter that ran this diagnostic
                , adapter_.getBaseWorkItemId()       // the work item id that the adapter was doing when this
                // diagnostic ended
        );
    }

    private void createWorkItemForWlmForNode(String location, boolean active) {
        // Send a work item to the WLM adapter/provider so the WLM knows that it can start scheduling jobs on
        // this node.
        createWorkQueue();
        String sTempWork    = active?"UseNode":"DontUseNode";
        try {
            long lUnWorkItemId = workQueue_.queueWorkItem("WLM" // type of adapter that needs to handle this work
                    , null                 // queue this work should be put on
                    , sTempWork            // work that needs to be done
                    , location             // parameters for this work
                    , false                // false indicates that we do NOT want to know when this work item
                    // finishes
                    , adapter_.getType()   // type of adapter that requested this work to be done
                    , adapter_.getBaseWorkItemId() // work item that the requesting adapter was working on when it
                    // requested this work be done
            );
            log_.info("Successfully queued %s work item - Node=%s, NewWorkItemId=%d",
                    sTempWork, location, lUnWorkItemId);
        } catch(IOException e) {
            log_.exception(e, "Failed to queued %s work item - Node=%s",
                    sTempWork, location);
        }
    }

    private void createNodeInfoMaps() {
        if(nodes_ == null) {
            nodes_ = new HashMap<>();
            try {
                // Compute Nodes
                ClientResponse response = client_.callProcedure("ComputeNodeBasicInformation");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    BasicNodeInfo info = new BasicNodeInfo(vt.getString("HostName"), vt.getLong("SequenceNumber"),
                            false, vt.getString("Owner"));
                    nodes_.put(vt.getString("Lctn"), info);
                }
                // Service Nodes
                response = client_.callProcedure("ServiceNodeBasicInformation");
                // Loop through each of the nodes that were returned.
                vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    BasicNodeInfo info = new BasicNodeInfo(vt.getString("HostName"), vt.getLong("SequenceNumber"),
                            true, vt.getString("Owner"));
                    nodes_.put(vt.getString("Lctn"), info);
                }
            } catch(IOException | ProcCallException e) {
                log_.exception(e, "Failed to get compute node or service node basic information");
            }
        }
    }

    private void tearDownAdapter() throws AdapterException {
        ClientResponse response;

        try {
            response = client_.callProcedure("AdapterTerminated"
                    ,adapter_.getType()           // the type of adapter that is being terminated
                    ,adapter_.getId()             // the adapter id of the adapter that is being terminated
                    ,adapter_.getType()           // the type of adapter that requested the specified adapter be
                                                  // terminated
                    ,adapter_.getBaseWorkItemId() // work item Id that the requesting adapter was performing when it
                                                  // requested this adapter be terminated
            );
        } catch(ProcCallException | IOException e) {
            log_.exception(e, "tearDownAdapter - stored procedure AdapterTerminated FAILED");
            throw new AdapterException("Datastore procedure call failed", e);
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("tearDownAdapter - stored procedure AdapterTerminated FAILED - Status=%s, StatusString=%s, " +
                            "TermAdapterType=%s, TermAdapterId=%d, ReqAdapterType=%s, ReqWorkItemId=%d!",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                    adapter_.getType(), adapter_.getId(), adapter_.getType(), adapter_.getBaseWorkItemId());
            throw new RuntimeException(response.getStatusString());
        }
        log_.info("teardownAdapter - successfully terminated adapter, TermAdapterType=%s, TermAdapterId=%d, " +
                        "ReqAdapterType=%s, ReqWorkItemId=%d",
                adapter_.getType(), adapter_.getId(), adapter_.getType(), adapter_.getBaseWorkItemId());
    }

    private void tearDownAdaptersBaseWorkItem(String results) throws AdapterException {
        ClientResponse response;
        try {
            response = client_.callProcedure("WorkItemFinished"
                    ,adapter_.getType()           // the type of adapter that is being terminated
                    ,adapter_.getBaseWorkItemId() // the specific base work item for the adapter that is being
                                                  // terminated
                    ,results                      // results for the specific base work item for the adapter that is
                                                  // being terminated
            );
        } catch(ProcCallException | IOException e) {
            log_.exception(e, "teardownAdaptersBaseWorkItem - stored procedure WorkItemFinished FAILED");
            throw new AdapterException("Datastore procedure call failed", e);
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("teardownAdaptersBaseWorkItem - stored procedure WorkItemFinished FAILED - Status=%s, " +
                            "StatusString=%s, TermAdapterType=%s, TermAdapterBaseWorkItemId=%d, " +
                            "TermAdapterBaseWorkItemResults=%s!",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                    adapter_.getType(), adapter_.getBaseWorkItemId(), results);
            throw new RuntimeException(response.getStatusString());
        }
        log_.info("teardownAdaptersBaseWorkItem - successfully finished this adapters base work item - " +
                        "TermAdapterType=%s, TermAdapterBaseWorkItemId=%d, TermAdapterBaseWorkItemResults=%s",
                adapter_.getType(), adapter_.getBaseWorkItemId(), results);
    }

    private Logger log_;
            Client client_;
    private String[] servers_;
    protected RasEventLog ras_;
    protected WorkQueue workQueue_;
    private AdapterInformation adapter_;
    private Map<String,BasicNodeInfo> nodes_ = null;

    @SuppressWarnings("serial")
    private static Map<BootState,String> states_ = new HashMap<BootState,String>() {{
        put(BootState.NODE_BOOTING, "B");
        put(BootState.NODE_OFFLINE, "M");
        put(BootState.NODE_ONLINE, "A");
    }};

    static class CallbackForNodeStateChange implements ProcedureCallback {
        CallbackForNodeStateChange(AdapterInformation adapter, RasEventLog ras, String location, String procedureName) {
            adapter_ = adapter;
            location_ = location;
            procedureName_ = procedureName;
            ras_ = ras;
        }

        @Override
        public void clientCallback(ClientResponse response) throws Exception {
            if(response.getStatus() != ClientResponse.SUCCESS) {
                if (response.getStatusString().contains("no entry in the ComputeNode table") ||
                        response.getStatusString().contains("no entry in the ServiceNode table")) {
                    String instanceData = "AdapterName=" + adapter_.getName() + ", SpThisIsCallbackFor=" +
                            procedureName_ + ", " + "PertinentInfo=" + location_ + ", StatusString=" +
                            response.getStatusString();
                    ras_.logRasEventNoEffectedJob("RasProvCompNodeSetStateFailedInvalidNode",
                            instanceData, location_, System.currentTimeMillis() * 1000L, adapter_.getType(),
                            adapter_.getBaseWorkItemId());
                }
            }
        }

        private final AdapterInformation adapter_;
        private final String location_;
        private final String procedureName_;
        private final RasEventLog ras_;
    }

    static class CallbackForErrorNodeStateChange implements ProcedureCallback {
        CallbackForErrorNodeStateChange(VoltDbAdapterOperations parent, String location, String name, Logger logger) {
            this.location = location;
            this.logger = logger;
            this.name = name;
            this.parent = parent;
        }
        @Override
        public void clientCallback(ClientResponse response) throws Exception {
            if (response.getStatus() != ClientResponse.SUCCESS) {
                logger.error("MyCallbackForHouseKeepingNoRtrnValue - %s callback FAILED - Status=%s, " +
                                "StatusString='%s', PertinentInfo=%s!!!",
                        name, VoltDbClient.statusByteAsString(response.getStatus()),
                        response.getStatusString(), location);
                parent.failedToLogNodeStateChange(location, name, response);
            } else {
                logger.info("MyCallbackForHouseKeepingNoRtrnValue - %s was successful, PertinentInfo=%s",
                        name, location);
            }
        }
        private final String location;
        private final String name;
        private final Logger logger;
        private final VoltDbAdapterOperations parent;
    }
}
