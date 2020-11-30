// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.dsapi.WorkQueue;
import org.voltdb.client.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import com.intel.config_io.ConfigIOParseException;
import java.lang.management.ManagementFactory;

public interface IAdapter {
    // Adapter API
    void initialize();
    String adapterName();
    void adapterName(String newName);
    WorkQueue setUpAdapter(String servers, String sSnLctn, AdapterShutdownHandler handler) throws AdapterException;
    WorkQueue setUpAdapter(String servers, String sSnLctn) throws AdapterException;
    void enableSignalHandlers();
    void setShutdownHandler(AdapterShutdownHandler handler);
    void registerAdapter(String sSnLctn) throws IOException, ProcCallException;
    long adapterId();
    boolean adapterAbnormalShutdown();
    void adapterAbnormalShutdown(boolean newState);
    String adapterType();
    void handleMainlineAdapterCleanup(boolean abnormal) throws IOException, InterruptedException, AdapterException;
    void handleMainlineAdapterException(Exception e);
    void adapterShutdownStarted(boolean newState);
    boolean adapterShuttingDown();
    Map<String, String> mapCompNodeLctnToHostName() throws IOException, ProcCallException;
    // Check the specified linux PID to see if it is still active.
    boolean isPidActive(long lPid);

    String getNodesBmcIpAddr(String sNodeLctn);
    Map<String, String[]> mapNodeLctnToIpAddrAndBmcIpAddr() throws IOException, ProcCallException;
    Map<String, String[]> mapCompNodeLctnToIpAddrAndBmcIpAddr() throws IOException, ProcCallException;

    Map<String, String> mapBmcIpAddrToNodeLctn() throws IOException, ProcCallException;

    Map<String, String> mapNodeLctnToAggregator() throws IOException, ProcCallException;

    static String statusByteAsString(byte bStatus) {
        String sStatusByteAsString = null;
        if (bStatus == ClientResponse.USER_ABORT)               { sStatusByteAsString = "USER_ABORT"; }
        else if (bStatus == ClientResponse.CONNECTION_LOST)     { sStatusByteAsString = "CONNECTION_LOST"; }
        else if (bStatus == ClientResponse.CONNECTION_TIMEOUT)  { sStatusByteAsString = "CONNECTION_TIMEOUT"; }
        else if (bStatus == ClientResponse.GRACEFUL_FAILURE)    { sStatusByteAsString = "GRACEFUL_FAILURE"; }
        else if (bStatus == ClientResponse.RESPONSE_UNKNOWN)    { sStatusByteAsString = "RESPONSE_UNKNOWN"; }
        else if (bStatus == ClientResponse.UNEXPECTED_FAILURE)  { sStatusByteAsString = "UNEXPECTED_FAILURE"; }
        else if (bStatus == ClientResponse.SUCCESS)             { sStatusByteAsString = "SUCCESS"; }
        else    { sStatusByteAsString = Byte.toString( bStatus ); }
        return sStatusByteAsString;
    }   // End statusByteAsString(byte bStatus)

    long pid();
    WorkQueue workQueue();
    void workQueue(WorkQueue workQueue);
    long getAdapterInstancesAdapterId(String sAdapterType, String sAdapterLctn, long lAdapterPid) throws IOException, ProcCallException;
    long getAdapterInstancesBaseWorkItemId(String sAdapterType, long lAdapterId) throws IOException, ProcCallException;
    void teardownAdapter(String sTermAdapterType, long lTermAdapterId, String sReqAdapterType, long lReqWorkItemId) throws IOException, AdapterException;
    void teardownAdaptersBaseWorkItem(String sBaseWorkItemResults, String sTermAdapterType, long lTermAdapterBaseWorkItemId) throws IOException, AdapterException;
    String snLctn();
    Map<String, MetaData> cachedRasEventTypeToMetaDataMap();


    // RAS event's meta data (this has been copied into com.intel.dai.dsimpl.voltdb.VoltDbRasEventLog)
    public static class MetaData {
        // Member data
        private String  DescriptiveName = "<DescriptiveName>";
        private String  Severity = "<Severity>";
        private String  Category = "<Category>";
        private String  Component = "<Component>";
        private String  ControlOperation = "<ControlOperation>";
        private String  Msg = "<Msg>";
        private boolean GenerateAlert = false;
        // Methods
        public String  descriptiveName()          { return DescriptiveName; }
        public void    descriptiveName(String s)  { DescriptiveName = s; }
        public String  severity()                 { return Severity; }
        public void    severity(String s)         { Severity = s; }
        public String  category()                 { return Category; }
        public void    category(String s)         { Category = s; }
        public String  component()                { return Component; }
        public void    component(String s)        { Component = s; }
        public String  controlOperation()         { return ControlOperation; }
        public void    controlOperation(String s) { ControlOperation = s; }
        public String  msg()                      { return Msg; }
        public void    msg(String s)              { Msg = s; }
        public boolean generateAlert()            { return GenerateAlert; }
        public void    generateAlert(boolean b)   { GenerateAlert = b; }
    }   // End class MetaData


    //--------------------------------------------------------------------------
    // Get the linux PID for this process (the process this is executed within).
    //--------------------------------------------------------------------------
    public static long getProcessPid() {
        // The following call returns a value similar to "<pid>@<hostname>".
        String sName = ManagementFactory.getRuntimeMXBean().getName();
        int iIndex = sName.indexOf('@');
        // Ensure that we got a "valid" string back.
        if (iIndex < 1) {
            // will occur if there is nothing before the @ or if there is no @.
            return -3;
        }
        return Long.parseLong(sName.substring(0, iIndex));
    }   // End getProcessPid()

    void abend(String sReason) throws IOException, InterruptedException, AdapterException;
    Map<Integer, String> dataMoverResultTblIndxToTableNameMap();
    Map<String, String> mapServNodeLctnToHostName()throws IOException, ProcCallException;
    boolean isComputeNodeLctn(String sLctn) throws IOException, ProcCallException;
    boolean isServiceNodeLctn(String sLctn) throws IOException, ProcCallException;
    Map<String, Long> mapCompNodeLctnToSeqNum() throws IOException, ProcCallException;
    Map<String, String> mapCompNodeHostNameToLctn() throws IOException, ProcCallException;
    // Get the specified node's inventory info out of the database.
    String getNodesInvInfoFromDb(String sTempLctn) throws IOException, ProcCallException;

    boolean isThisHwBeingServiced(String sLctn);
    boolean isThisHwOwnedByService(String sLctn);
    boolean isThisHwOwnedByWlm(String sLctn);
    String getOwningSubsystem(String sLctn);

    // VoltDB API
    void connectToDataStore(String commaSepListOfServers) throws UnknownHostException, IOException;
    Client client();

    // RAS and Environmental Data API
    void loadRasMetadata();
    void addRasMetaDataIntoDataStore() throws IOException, ProcCallException;
    long logEnvDataAggregated(String sTypeOfData, String sLctn, long lTsInMicroSecs, double dMaxValue, double dMinValue,
                              double dAvgValue, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, SQLException, TimeoutException, ConfigIOParseException, ClassNotFoundException;
    void logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                  String sReqAdapterType, long lReqWorkItemId);
    void logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                      String sReqAdapterType, long lReqWorkItemId);
    void logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId,
                                    long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId);
    void logRasEventCheckForEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                        String sReqAdapterType, long lReqWorkItemId);
    String getRasEventType(String description);

    // Power API
    void markNodePoweredOff(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;

    // Diagnostics API
    long diagStarted(String sLctn, long lServiceOperationId, String sDiag, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, ProcCallException, InterruptedException;
    long diagEnded(long lDiagId, String sLctn, String sDiag, String sResults, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, ProcCallException, InterruptedException;

    // Callback Factories API
    ProcedureCallback createHouseKeepingCallbackNoRtrnValue(String sAdapterType,
                                                            String sAdapterName,
                                                            String sSpThisIsCallbackFor,
                                                            String sPertinentInfo,
                                                            long lWorkItemId);
    ProcedureCallback createHouseKeepingCallbackLongRtrnValue(String sAdapterType,
                                                              String sAdapterName,
                                                              String sSpThisIsCallbackFor,
                                                              String sPertinentInfo,
                                                              long lWorkItemId);

    // Node states API
    void markNodeActive(String sNodeLctn, boolean bUsingSynthData, long lTsInMicroSecs,
                        String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;

    void markNodeInErrorState(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp,
                              String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;
    void markNodeInErrorState(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;

    void markNodeBiosStarting(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;

    void updateNodeMacAddress(String sNodeLctn, String nodeMacAddr, String nodeBmcMacAddr, String sWorkItemAdapterType, long lWorkItemId) throws IOException;

    // Jobs API
    void killJob(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp,
                 String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;

    // Workload Manager
    void tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException;
    void tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReasonForDraining)
            throws IOException, ProcCallException, InterruptedException;
    ArrayList<String> extractListOfWlmNodes(String sNodeList, String sJobId, long lWorkItemId)
            throws InterruptedException, IOException, ProcCallException;
    ArrayList<String> listOfJobsOnNode(String sNodeLctn) throws IOException, ProcCallException;
}
