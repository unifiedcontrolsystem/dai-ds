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
import java.util.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import com.intel.config_io.ConfigIOParseException;
import java.lang.management.ManagementFactory;

import com.intel.properties.*;
import com.intel.config_io.*;
import com.intel.logging.*;


public interface IAdapter {
    // Adapter API
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

    String getNodesBmcIpAddr(String sNodeLctn);
    String getNodeNameFromLctn(String sNodeLctn);

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

    ConfigIO jsonParser();
    long pid();
    WorkQueue workQueue();
    void workQueue(WorkQueue workQueue);
    long getAdapterInstancesAdapterId(String sAdapterType, String sAdapterLctn, long lAdapterPid) throws IOException, ProcCallException;
    long getAdapterInstancesBaseWorkItemId(String sAdapterType, long lAdapterId) throws IOException, ProcCallException;
    void teardownAdapter(String sTermAdapterType, long lTermAdapterId, String sReqAdapterType, long lReqWorkItemId) throws IOException, AdapterException;
    void teardownAdaptersBaseWorkItem(String sBaseWorkItemResults, String sTermAdapterType, long lTermAdapterBaseWorkItemId) throws IOException, AdapterException;
    String snLctn();
    Map<String, MetaData> cachedRasDescrNameToMetaDataMap();


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
    // Check the specified linux PID to see if it is still active.
    boolean isPidActive(long lPid);

    void abend(String sReason) throws IOException, InterruptedException, AdapterException;
    Map<Integer, String> dataMoverResultTblIndxToTableNameMap();


    // Get the lctn string that corresponds to the specified hostname (either a compute node or a service node hostname may be specified).
    String getLctnFromHostname(String sHostname);
    boolean isComputeNodeLctn(String sLctn);
    boolean isServiceNodeLctn(String sLctn);
    String  extractNodeLctnFromLctn(String sNodeLctn);
    Map<String, String>   mapBmcIpAddrToNodeLctn() throws IOException, ProcCallException;
    Map<String, String[]> mapNodeLctnToIpAddrAndBmcIpAddr() throws IOException, ProcCallException;
    Map<String, String>   mapNodeLctnToAggregator() throws IOException, ProcCallException;

    Map<String, Long>     mapCompNodeLctnToSeqNum();
    Map<String, String>   mapCompNodeLctnToHostName();
    Map<String, String>   mapCompNodeHostNameToLctn();

    Map<String, String>   mapServNodeLctnToHostName();
    Map<String, String>   mapServNodeHostNameToLctn();

    // Get the fully qualified lctn (e.g., R0-CH00-CN0-A3 or R1-CH01-CN1-H1) for a PCIE FRU by using the NodeLctn and PCIE BusAddr.
    String getPcieFrusLctnUsingBusAddr(String sNodeLctn, String sPcieBusAddr);
    // Return a Map that takes a Service or Compute NodeLctn + Slot (e.g., "R0-CH00-CN0+3") and gives you its fully qualified FruLctn (e.g., R0-CH00-CN0-A3)
    Map<String, String> mapNodelctnAndSlotToLctn();
    // Return a map that takes a Service or Compute NodeLctn + PCIE BusAddr (e.g., "R0-CH00-CN0+0000:44:03.0") and gives you its fully qualified FruLctn (e.g., R0-CH00-CN0-A3)
    Map<String, String> mapNodelctnAndBusaddrToLctn();
    // Return a map that takes a Service or Compute node's lctn AND a Dimm ModuleLocator (R0-CH00-CN0 and CPU2_DIMM_A2) and gives you its fully qualified Dimm lctn (e.g., R0-CH00-CN0-D3)
    Map<String, String> mapNodelctnAndModulelocatorToLctn();
    // Return a map that takes a Service or Compute node's lctn AND a Processor SocketDesignation (R0-CH00-CN0 and CPU1) and gives you its fully qualified Processor lctn (e.g., R0-CH00-CN0-P1)
    Map<String, String> mapNodelctnAndSocketdesignationToLctn();


    //--------------------------------------------------------------------------
    // Class containing the inventory information that was obtained during node boot.
    //--------------------------------------------------------------------------
    class BootMsgInvInfo {
        BootMsgInvInfo(PropertyMap pmMsgInvInfo, Logger log) {
            mNodeName = pmMsgInvInfo.getStringOrDefault("nodename", null);
            mBoardManufacturer = pmMsgInvInfo.getStringOrDefault("board_manufacturer", null);
            mBoardProductName = pmMsgInvInfo.getStringOrDefault("board_product_name", null);
            mBoardSerial = pmMsgInvInfo.getStringOrDefault("board_serial_number", null);
            mBoardPartNumber = pmMsgInvInfo.getStringOrDefault("board_part_number", null);
            mBiosDate = pmMsgInvInfo.getStringOrDefault("bios_date", null);
            mBiosVendor = pmMsgInvInfo.getStringOrDefault("bios_vendor", null);
            mBiosVersion = pmMsgInvInfo.getStringOrDefault("BiosVersion", null);
            mTotalSystemMemoryGiB = pmMsgInvInfo.getLongOrDefault("TotalSystemMemoryGiB", -99999);
            mTotalSystemCores = pmMsgInvInfo.getLongOrDefault("TotalCores", -99999);
            mPaProcessor = pmMsgInvInfo.getArrayOrDefault("processor", null);
            mPaMemory = pmMsgInvInfo.getArrayOrDefault("memory", null);
            for (int iSctnCntr=2; ; ++iSctnCntr) {
                PropertyArray paTemp = pmMsgInvInfo.getArrayOrDefault(("memory"+iSctnCntr), null);
                if (paTemp != null)
                    // add these dimm entries in with the rest of the dimms.
                    mPaMemory.addAll(paTemp);
                else
                    // no more memory sections in the boot msg.
                    break;
            }
            mPaGpu = pmMsgInvInfo.getArrayOrDefault("GPU", null);
            mPaHfi = pmMsgInvInfo.getArrayOrDefault("HFI", null);
        }

        String        mNodeName = null;
        String        mBoardManufacturer = null;
        String        mBoardProductName = null;
        String        mBoardSerial = null;
        String        mBoardPartNumber = null;
        String        mBiosDate = null;
        String        mBiosVendor = null;
        String        mBiosVersion = null;
        long          mTotalSystemMemoryGiB = -99999;
        long          mTotalSystemCores = -99999;
        PropertyArray mPaProcessor = null;
        PropertyArray mPaMemory = null;
        PropertyArray mPaGpu = null;
        PropertyArray mPaHfi = null;

        public String toString() {
            StringBuilder sbTemp =  new StringBuilder();
            sbTemp.append("mNodeName='" + mNodeName + "', ");
            sbTemp.append("mBoardManufacturer='" + mBoardManufacturer + "', ");
            sbTemp.append("mBoardProductName='" + mBoardProductName + "', ");
            sbTemp.append("mBoardSerial='" + mBoardSerial + "', ");
            sbTemp.append("mBoardPartNumber='" + mBoardPartNumber + "', ");
            sbTemp.append("mBiosDate='" + mBiosDate + "', ");
            sbTemp.append("mBiosVendor='" + mBiosVendor + "', ");
            sbTemp.append("mBiosVersion='" + mBiosVersion + "', ");
            sbTemp.append("mTotalSystemMemoryGiB='" + mTotalSystemMemoryGiB + "', ");
            sbTemp.append("mTotalSystemCores='" + mTotalSystemCores + "', ");
            if (mPaProcessor == null)
                sbTemp.append("mPaProcessor is empty, ");
            else
                sbTemp.append("mPaProcessor='" + mPaProcessor.toString() + "', ");
            if (mPaMemory == null)
                sbTemp.append("mPaMemory is empty, ");
            else
                sbTemp.append("mPaMemory='" + mPaMemory.toString() + "', ");
            if (mPaGpu == null)
                sbTemp.append("mPaGpu is empty, ");
            else
                sbTemp.append("mPaGpu='" + mPaGpu.toString() + "', ");
            if (mPaHfi == null)
                sbTemp.append("mPaHfi is empty");
            else
                sbTemp.append("mPaHfi='" + mPaHfi.toString() + "'");
            return sbTemp.toString();
        }
    }   // End class BootMsgInvInfo

    // Get the specified node's inventory info out of the database.
    String getNodesInvInfoFromDb(String sTempLctn) throws IOException, ProcCallException;
    // Check that the specified node's inventory info meets any specified constraints for this lctn.
    String checkConstraints(String sTempLctn, boolean bUsingSynthData);
    // Replace the specified node's inventory information in the DB, using the inventory information obtained from the node's boot message.
    void replaceNodesInvInfoWithInfoFromBootMsg(String sTempLctn, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData) throws IOException, PropertyNotExpectedType;
    // Replace the specified node's bios information in the DB, using the information obtained from the node's UcsBios message.
    void replaceNodesBiosInfoWithInfoFromBootMsg(String sTempLctn, String sNewBiosInfoAsJson, Date dLineTimestamp, boolean bUsingSynthData) throws IOException;
    // Perform the necessary inventory checks for the specified FRU, if there is a mismatch also update the db's inventory information.
    void performFrusInvChkAndUpdate(String sNodeLctn, PropertyMap pmDbInvInfo, PropertyMap pmDbLctnInfo, PropertyMap pmDbFruInfo, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)
         throws IOException, ProcCallException, PropertyNotExpectedType;
    String getNodesBiosInfoFromDb(String sTempLctn);

    // Compare these 2 PropertyMaps to see whether they have the same set of keys and values.
    // Returns:
    //      Null if strings match each other
    //      String containing the mismatching keys
    String comparePropertyMaps(PropertyMap pm1, PropertyMap pm2);


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
    void logRasEventNoEffectedJob(String sRasEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                  String sReqAdapterType, long lReqWorkItemId);
    void logRasEventSyncNoEffectedJob(String sRasEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                      String sReqAdapterType, long lReqWorkItemId);
    void logRasEventWithEffectedJob(String sRasEventDescrName, String sInstanceData, String sLctn, String sJobId,
                                    long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId);
    void logRasEventCheckForEffectedJob(String sRasEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                        String sReqAdapterType, long lReqWorkItemId);
    String ensureRasDescrNameIsValid(String sRasEventDescrName);

    // Power API
    void markNodePoweredOff(String sNodeLctn, Boolean bHonorTsOrder, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
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
         throws IOException;

    void markNodeInErrorState(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp,
                              String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
         throws IOException;
    void markNodeInErrorState(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)
         throws IOException;

    void markNodeBiosStarting(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException;

    void updateNodeMacAddress(String sNodeLctn, String nodeMacAddr, String nodeBmcMacAddr, String sWorkItemAdapterType, long lWorkItemId) throws IOException;

    void chgAllNodeComponentsToPoweredOff(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException, ProcCallException;

    // Jobs API
    void killJob(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp,
                 String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
         throws IOException;

    // Workload Manager
    void tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)
         throws IOException, ProcCallException, InterruptedException;
    void tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReasonForDraining)
         throws IOException;
    ArrayList<String> extractListOfWlmNodes(String sNodeList, String sJobId, long lWorkItemId)
                      throws InterruptedException, IOException, ProcCallException;
    ArrayList<String> listOfJobsOnNode(String sNodeLctn) throws IOException, ProcCallException;
    void chkAndFixupStaleWlmNodeState(String sNodeLctn, String sNodeName, String sCorrectWlmNodeState, String sActualWlmNodeInfo)
         throws IOException, ProcCallException;
}
