// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.logging.Logger;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.client.ProcedureCallback;

import java.io.IOException;

//-------------------------------------------------------------------------
// Async Stored Procedure callback handler - used when people want to make async calls to stored procedures that returns a
// long numeric value.
// Parms:
//      Adapter obj                 - adapter object
//      String sAdapterType         - Type of the adapter that is "calling" this callback
//      String sAdapterName         - The name of the adapter that is "calling" this callback
//      String sSpThisIsCallbackFor - The stored procedure that was executed "by" this callback
//      String sPertinentInfo       - Info that might be pertinent to this particular execution of this callback
//      long   lWorkItemId          - The work item id that the adapter is "processing" during which it ends up using this callback
//-------------------------------------------------------------------------
class VoltDbCallBackForHouseKeeping implements ProcedureCallback {
    public final String KeyForLctn = "Lctn=";
    public final String KeyForTimeInMicroSecs = "TimeInMicroSecs=";
    public final String KeyForUsingSynthData = "UsingSynthData";

    // Member data
    private VoltDbRasEventLog obj;
    private String adapterType;
    private String adapterName;
    private String spThisIsCallbackFor;    // stored procedure that this is a callback for (which stored procedure was
    // being done by this callback)
    private String pertinentInfo;          // info that might be pertinent (included in log messages)
    private long workItemId;
    private Logger logger;

    // Class constructor
    VoltDbCallBackForHouseKeeping(VoltDbRasEventLog obj, String sAdapterType, String sAdapterName, String sSpThisIsCallbackFor,
                                  String sPertinentInfo, long lWorkItemId, Logger logger) {
        this.obj = obj;
        adapterType = sAdapterType;
        adapterName = sAdapterName;
        spThisIsCallbackFor = sSpThisIsCallbackFor;
        pertinentInfo = sPertinentInfo;
        workItemId = lWorkItemId;
        this.logger = logger;
    }

    // Member methods
    @Override
    public void clientCallback(ClientResponse response) throws IOException, ProcCallException, InterruptedException {
        // Ensure that the stored procedure was successful.
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            boolean bLoggedRasEvent = false;
            logger.error("VoltDbCallBackForHouseKeeping - %s callback FAILED - Status=%s, StatusString='%s', PertinentInfo=%s, WorkItemId=%d!!!",
                         spThisIsCallbackFor, VoltDbClient.statusByteAsString(response.getStatus()),
                         response.getStatusString(), pertinentInfo, workItemId);

            switch(spThisIsCallbackFor) {
                case "ComputeNodeDiscovered":
                    if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvCompNodeDiscFailedInvalidMacAddr",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", "
                                        + "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                ,null                                  // Lctn associated with this ras event
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                        );
                        bLoggedRasEvent = true;
                    }
                    break;
                case "ServiceNodeDiscovered":
                    if (response.getStatusString().contains("no entry in the ServiceNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvServiceNodeDiscFailedInvalidMacAddr",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", "
                                        + "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                ,null                                  // Lctn associated with this ras event
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                        );
                        bLoggedRasEvent = true;
                    }
                    break;
                case "ComputeNodeSaveIpAddr":
                    if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvCompNodeSaveIpAddrFailedInvalidMacAddr",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                        "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                , pertinentInfo                        // Lctn
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // requesting adapter work item id
                        );
                        bLoggedRasEvent = true;
                    } else if (response.getStatusString().contains("not the same as the expected IP address")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvCompNodeSaveIpAddrFailedInvalidIpAddr",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                        "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                , pertinentInfo                        // Lctn
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // requesting adapter work item id
                        );
                        bLoggedRasEvent = true;
                    }
                    break;
                case "ServiceNodeSaveIpAddr":
                    if (response.getStatusString().contains("no entry in the ServiceNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvServiceNodeSaveIpAddrFailedInvalidMacAddr",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                        "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                , pertinentInfo                        // Lctn
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // requesting adapter work item id
                        );
                        bLoggedRasEvent = true;
                    } else if (response.getStatusString().contains("not the same as the expected IP address")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvServiceNodeSaveIpAddrFailedInvalidIpAddr",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                        "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                , pertinentInfo                        // Lctn
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // requesting adapter work item id
                        );
                        bLoggedRasEvent = true;
                    }
                    break;
                case "ComputeNodeSetState":
                    if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvCompNodeSetStateFailedInvalidNode", workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " + "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                ,pertinentInfo                        // Lctn associated with this ras event
                                ,System.currentTimeMillis() * 1000L   // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                ,adapterType                          // type of adapter that is generating this ras event
                                ,workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                        );
                        bLoggedRasEvent = true;
                    }
                    else {
                        // Check & see if attempting to change the node's state from error to active (that is an invalid transition).
                        if (response.getStatusString().contains("Invalid state change was attempted from ERROR to ACTIVE")) {
                            logger.error("VoltDbCallBackForHouseKeeping - ComputeNodeSetState would not change the node state from error to active, node state left unchanged!");
                            // Indicate that we do not want the default RAS event logged.
                            bLoggedRasEvent = true;
                        }
                    }
                    break;
                case "ServiceNodeSetState":
                    if (response.getStatusString().contains("no entry in the ServiceNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvServiceNodeSetStateFailedInvalidNode",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                        "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                , pertinentInfo                        // Lctn associated with this ras event
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                        );
                        bLoggedRasEvent = true;
                    }
                    break;
                case "ComputeNodeSaveBootImageInfo":
                    if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvCompNodeSaveBootImageFailedInvalidNode",
                                workItemId)
                                ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                        "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                                , pertinentInfo                        // Lctn associated with this ras event
                                ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                , adapterType                          // type of adapter that is generating this ras event
                                , workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                        );
                        bLoggedRasEvent = true;
                    }
                    break;
            }

            // IFF another RAS event has not been logged, log the generic callback failed RAS event.
            if (bLoggedRasEvent == false) {
                obj.logRasEventNoEffectedJob(obj.getRasEventType("RasGenAdapterVoltDbCallBackForHouseKeepingFailed" ,
                        workItemId)
                        ,("AdapterName=" + adapterName + ", SpThisIsCallbackFor=" + spThisIsCallbackFor + ", " +
                                "PertinentInfo=" + pertinentInfo + ", StatusString=" + response.getStatusString())
                        ,null                                  // Lctn
                        ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                        , adapterType                          // type of adapter that is generating this ras event
                        , workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                );
                bLoggedRasEvent = true;
            }
        }   // stored procedure failed.
        else
        {   // stored procedure was successful.
            logger.info("VoltDbCallBackForHouseKeeping - %s was successful, PertinentInfo=%s, WorkItemId=%d",
                        spThisIsCallbackFor, pertinentInfo, workItemId);
            long lRtrnValue = response.getResults()[0].asScalarLong();  // save the stored procedure's results (returned as a scalar long).
            switch (spThisIsCallbackFor) {
                case "ComputeNodeDiscovered": case "ComputeNodeSaveBootImageInfo": case "ComputeNodeSaveIpAddr":
                case "ServiceNodeDiscovered": case "ServiceNodeSaveIpAddr": case "ServiceNodeSetState":
                    if (lRtrnValue > 0) {
                        // everything completed fine but we detected that this item did occur out of timestamp order.
                        logger.info("VoltDbCallBackForHouseKeeping - %s - %s - fyi, this item occurred out of " +
                                    "timestamp order in regards to similar items, but it still has been handled properly",
                                    spThisIsCallbackFor, pertinentInfo);
                        String sTempLctn = pertinentInfo;
                        if (pertinentInfo.contains(KeyForLctn)) {
                            String[] sParms = pertinentInfo.split(",");
                            for (String parms: sParms) {
                                if (parms.startsWith(KeyForLctn)) {
                                    sTempLctn = parms.substring(KeyForLctn.length());
                                    break;
                                }
                            }
                        }
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasGenAdapterDetectedOutOfTsOrderItem", workItemId)
                                ,("AdapterName=" + adapterName + ", StoredProcedure=" + spThisIsCallbackFor + ", PertinentInfo=" + pertinentInfo)
                                ,sTempLctn                            // Lctn
                                ,System.currentTimeMillis() * 1000L   // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                ,adapterType                          // type of adapter that is generating this ras event
                                ,workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                );
                    }
                    break;
                case "ComputeNodeSetState":
                    String sTempLctn = null;
                    String sTempTimeInMicroSecs = null;
                    String[] sParms = null;
                    if (lRtrnValue > 0) {
                        // everything completed fine but we detected that this item did occur out of timestamp order.
                        logger.info("VoltDbCallBackForHouseKeeping - %s - %s - fyi, this item occurred out of timestamp order in regards to similar items, but it still has been handled properly",
                                    spThisIsCallbackFor, pertinentInfo);
                        sTempLctn = pertinentInfo;
                        if (pertinentInfo.contains(KeyForLctn)) {
                            if (sParms == null)
                                sParms = pertinentInfo.split(",");
                            for (String parms: sParms) {
                                if (parms.startsWith(KeyForLctn)) {
                                    sTempLctn = parms.substring(KeyForLctn.length());
                                    break;
                                }
                            }
                        }
                        obj.logRasEventNoEffectedJob(obj.getRasEventType("RasGenAdapterDetectedOutOfTsOrderItem", workItemId)
                             ,("AdapterName=" + adapterName + ", StoredProcedure=" + spThisIsCallbackFor + ", " + "PertinentInfo=" + pertinentInfo)
                             ,sTempLctn                           // Lctn
                             ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                             ,adapterType                         // type of adapter that is generating this ras event
                             ,workItemId                          // work item that is being worked on that resulted in the generation of this ras event
                             );
                    }
                    // Cut a ras event to indicate that this node is now active (if appropriate) - I know that there is no job effected by this occurrence.
                    if (pertinentInfo.contains("NewState=A")) {
                        // we just set the compute node state to active.
                        // Get the lctn string (if appropriate)
                        if (sTempLctn == null) {
                            if (pertinentInfo.contains(KeyForLctn)) {
                                if (sParms == null)
                                    sParms = pertinentInfo.split(",");
                                for (String parms: sParms) {
                                    if (parms.startsWith(KeyForLctn)) {
                                        sTempLctn = parms.substring(KeyForLctn.length());
                                        break;
                                    }
                                }
                            }
                        }
                        // Get the TimeInMicroSecs string (if appropriate)
                        if (sTempTimeInMicroSecs == null) {
                            if (pertinentInfo.contains(KeyForTimeInMicroSecs)) {
                                if (sParms == null)
                                    sParms = pertinentInfo.split(",");
                                for (String parms: sParms) {
                                    if (parms.startsWith(KeyForTimeInMicroSecs)) {
                                        sTempTimeInMicroSecs = parms.substring(KeyForTimeInMicroSecs.length());
                                        break;
                                    }
                                }
                            }
                            if (sTempTimeInMicroSecs == null)
                                sTempTimeInMicroSecs = Long.toString(System.currentTimeMillis() * 1000L);
                        }
                        if (!pertinentInfo.contains(KeyForUsingSynthData)) {
                            // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
                            obj.logRasEventNoEffectedJob(obj.getRasEventType("RasProvNodeActive", workItemId)
                                 ,("AdapterName=" + adapterName + ", Lctn=" + sTempLctn)
                                 ,sTempLctn                            // lctn
                                 ,Long.parseLong(sTempTimeInMicroSecs) // time this occurred, in micro-seconds since epoch
                                 ,adapterType                          // type of adapter that is generating this ras event
                                 ,workItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                 );
                        }   // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
                        else {
                            // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                            logger.warn("@@@ did not cut the node active ras event (since machine is being run with synthesized data) - %s is active @@@",
                                        sTempLctn);
                        }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                    }
                    break;
                default:
                    break;
            }
        }   // stored procedure was successful.
    }
}   // End class VoltDbCallBackForHouseKeeping
