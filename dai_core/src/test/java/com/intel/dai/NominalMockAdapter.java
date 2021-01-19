// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.config_io.*;
import com.intel.properties.*;
import org.voltdb.client.Client;
import org.voltdb.client.ProcCallException;
import org.voltdb.client.ProcedureCallback;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;

public class NominalMockAdapter implements IAdapter {
    String type;
    String name;
    AdapterShutdownHandler  mShutdownHandler = null;
    AtomicBoolean abnormalShutdownFlag = new AtomicBoolean(false);
    public boolean isNewWorkItem = false;
    int abnormalShutdownFlagCounter = -1;
    public boolean isNextWorkItem = false;
    int timeToWait = 0;
    String[] clientParameters = new String[] {"param1", "param2", "param3"};
    int workToBeDoneIndex = 0;
    public String[] workToBeDone = new String[] {"UnknownWork"};
    boolean throwException = false;
    long workItemHandlerCount = 0L;
    private WorkQueue  mWorkQueue = null;

    public NominalMockAdapter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    private void exception() throws IOException {
        if(throwException) {
            throwException = false;
            throw new IOException("Intentional Test Exception!");
        }
    }

    @Override
    public WorkQueue setUpAdapter(String servers, String sSnLctn, AdapterShutdownHandler handler) throws AdapterException {
        return setUpAdapter(servers, sSnLctn);
    }

    @Override
    public WorkQueue setUpAdapter(String servers, String sSnLctn) throws AdapterException {
        mWorkQueue = mock(WorkQueue.class);
        return mWorkQueue;
    }

    @Override
    public String adapterName() {
        return name;
    }

    @Override
    public void adapterName(String newName) {
        name = newName;
    }

    @Override
    public void setShutdownHandler(AdapterShutdownHandler handler)  { mShutdownHandler = handler; }

    @Override
    public void enableSignalHandlers() {

    }

    @Override
    public void connectToDataStore(String commaSepListOfServers) throws UnknownHostException, IOException {

    }

    @Override
    public void registerAdapter(String sSnLctn) throws IOException, ProcCallException {
        exception();
    }

    @Override
    public long adapterId() {
        return 0;
    }

    @Override
    public void addRasMetaDataIntoDataStore() throws IOException, ProcCallException {

    }

    @Override
    public void loadRasMetadata() {

    }

    @Override
    public boolean adapterAbnormalShutdown() {
        if(abnormalShutdownFlagCounter > 0) {
            abnormalShutdownFlagCounter--;
            return false;
        } else if (abnormalShutdownFlagCounter == 0) {
            return true;
        } else {
            return abnormalShutdownFlag.get();
        }
    }

    @Override
    public void adapterAbnormalShutdown(boolean newState) {
        abnormalShutdownFlag.set(newState);
    }


    @Override
    public String adapterType() {
        return type;
    }


    @Override
    public void handleMainlineAdapterCleanup(boolean abnormal) throws IOException, InterruptedException
    {
    }

    @Override
    public void handleMainlineAdapterException(Exception e) {

    }

    @Override
    public void adapterShutdownStarted(boolean newState) {

    }

    @Override
    public boolean adapterShuttingDown() {
        return false;
    }

    @Override
    public long logEnvDataAggregated(String sTypeOfData, String sLctn, long lTsInMicroSecs, double dMaxValue,
                                     double dMinValue, double dAvgValue, String sReqAdapterType,
                                     long lReqWorkItemId) throws IOException {
        return 0;
    }

    @Override
    public void logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                         String sReqAdapterType, long lReqWorkItemId) {

    }

    @Override
    public void logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId,
                                           long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) {

    }

    @Override
    public void logRasEventCheckForEffectedJob(String sEventType, String sInstanceData, String sLctn,
                                               long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) {

    }

    @Override
    public void logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn,
                                             long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) {

    }

    @Override
    public Client client() {
        return mock(Client.class);
    }


    @Override
    public void markNodePoweredOff(String sNodeLctn, Boolean bHonorTsOrder, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException, ProcCallException, InterruptedException
    {}

    @Override
    public void updateNodeMacAddress(String sNodeLctn, String nodeMacAddr, String nodeBmcMacAddr,
                                     String sWorkItemAdapterType, long lWorkItemId) {

    }

    @Override
    public Map<String, String> mapCompNodeLctnToHostName() {
        return null;
    }

    @Override
    public String getNodesInvInfoFromDb(String sTempLctn) throws IOException, ProcCallException {
        return "Inventory Info";
    }

    @Override
    public long diagStarted(String sLctn, long lServiceActionId, String sDiag, String sReqAdapterType, long lReqWorkItemId) throws IOException, ProcCallException, InterruptedException {
        return 0;
    }

    @Override
    public long diagEnded(long lDiagId, String sLctn, String sDiag, String sResults, String sReqAdapterType, long lReqWorkItemId) throws IOException, ProcCallException, InterruptedException {
        return 0;
    }

    @Override
    public void abend(String sReason) throws IOException, InterruptedException {

    }

    @Override
    public ProcedureCallback createHouseKeepingCallbackNoRtrnValue(String sAdapterType, String sAdapterName, String sSpThisIsCallbackFor, String sPertinentInfo, long lWorkItemId) {
        return null;
    }

    @Override
    public ProcedureCallback createHouseKeepingCallbackLongRtrnValue(String sAdapterType, String sAdapterName, String sSpThisIsCallbackFor, String sPertinentInfo, long lWorkItemId) {
        return null;
    }

    @Override
    public void markNodeActive(String sNodeLctn, boolean bUsingSynthData, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException {

    }

    @Override
    public Map<Integer, String> dataMoverResultTblIndxToTableNameMap() {
        return null;
    }

    @Override
    public Map<String, String> mapServNodeLctnToHostName() {
        return null;
    }

    @Override
    public Map<String, String> mapServNodeHostNameToLctn() {
        return null;
    }

    @Override
    public String getLctnFromHostname(String sHostname) {
        return null;
    }


    @Override
    public boolean isComputeNodeLctn(String sLctn) {
        return false;
    }

    @Override
    public void markNodeBiosStarting(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException {

    }

    @Override
    public void markNodeInErrorState(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId) throws IOException {

    }

    @Override
    public void markNodeInErrorState(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId) throws IOException {

    }

    @Override
    public void killJob(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId) throws IOException {

    }

    @Override
    public boolean isServiceNodeLctn(String sLctn) {
        return false;
    }

    @Override
    public Map<String, Long> mapCompNodeLctnToSeqNum() {
        return null;
    }

    @Override
    public Map<String, String> mapCompNodeHostNameToLctn() {
        Map<String, String>  locationMap= new HashMap<>();
        locationMap.put("node1", "location1");
        return locationMap;
    }

    @Override
    public void tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId) throws IOException, ProcCallException, InterruptedException {

    }

    @Override
    public void chkAndFixupStaleWlmNodeState(String sNodeLctn, String sNodeName, String sCorrectWlmNodeState, String sActualWlmNodeInfo)
                throws IOException, ProcCallException
    {}

    @Override
    public void tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReason) throws IOException {}

    @Override
    public ArrayList<String> extractListOfWlmNodes(String sNodeList, String sJobId, long lWorkItemId) throws InterruptedException,
            IOException, ProcCallException {
        return null;
    }

    @Override
    public String extractNodeLctnFromLctn(String sNodeLctn) { return null; }

    @Override
    public ArrayList<String> listOfJobsOnNode(String sNodeLctn) throws IOException, ProcCallException {
        return null;
    }

    @Override
    public Map<String, String> mapBmcIpAddrToNodeLctn() throws IOException, ProcCallException {
        return null;
    }

    @Override
    public String getNodesBmcIpAddr(String sNodeLctn){
       return "10.0.0.1";
    }

    @Override
    public Map<String, String[]> mapNodeLctnToIpAddrAndBmcIpAddr() throws IOException, ProcCallException {
        return null;
    }

    @Override
    public Map<String, String> mapNodeLctnToAggregator() throws IOException, ProcCallException {
        return null;

    }

    @Override
    public  long pid() {
        return 0L;
    }

    @Override
    public WorkQueue workQueue() {
        if(mWorkQueue == null) {
            try {
                setUpAdapter("localhost", null);
            } catch (AdapterException e) {

            }
        }
        return mWorkQueue;
    }

    @Override
    public void workQueue(WorkQueue workQueue) {
        mWorkQueue = workQueue;
    }

    @Override
    public long getAdapterInstancesAdapterId(String sAdapterType, String sAdapterLctn, long lAdapterPid)
            throws IOException, ProcCallException {
        return 0L;
    }

    @Override
    public long getAdapterInstancesBaseWorkItemId(String sAdapterType, long lAdapterId) throws IOException,
            ProcCallException {
        return 0L;

    }

    public void teardownAdapter(String sTermAdapterType, long lTermAdapterId, String sReqAdapterType,
                                long lReqWorkItemId) throws IOException {

    }

    @Override
    public void teardownAdaptersBaseWorkItem(String sBaseWorkItemResults, String sTermAdapterType,
                                             long lTermAdapterBaseWorkItemId) throws IOException {

    }

    @Override
    public String snLctn() {
        return null;
    }

    @Override
    public boolean isThisHwBeingServiced(String sLctn) {
        return false;
    }

    @Override
    public boolean isThisHwOwnedByService(String sLctn) {
        return false;
    }

    @Override
    public boolean isThisHwOwnedByWlm(String sLctn) {
        return false;
    }

    @Override
    public String getOwningSubsystem(String sLctn) {
        return null;
    }

    @Override
    public String ensureRasDescrNameIsValid(String sRasEventDescrName) {
        return null;
    }

    @Override
    public boolean isPidActive(long lPid) {
        return true;
    }

    @Override
    public Map<String, MetaData> cachedRasDescrNameToMetaDataMap() {
        return null;
    }

    @Override
    public void performFrusInvChkAndUpdate(String sNodeLctn, PropertyMap pmDbInvInfo, PropertyMap pmDbLctnInfo, PropertyMap pmDbFruInfo, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)
                throws IOException, ProcCallException {
    }

    @Override
    public void replaceNodesInvInfoWithInfoFromBootMsg(String sTempLctn, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)
                throws IOException {
    }

    public String checkConstraints(String sTempLctn, boolean bUsingSynthData) {
        return null;
    }

    public ConfigIO jsonParser() {
        return ConfigIOFactory.getInstance("json");
    }

    public String comparePropertyMaps(PropertyMap pm1, PropertyMap pm2)  { return null; }

    public String getNodesBiosInfoFromDb(String sTempLctn) { return null; }

    public void replaceNodesBiosInfoWithInfoFromBootMsg(String sTempLctn, String sNewBiosInfoAsJson, Date dLineTimestamp, boolean bUsingSynthData) {}

    public String getNodeNameFromLctn(String sNodeLctn) { return null; }

    public void chgAllNodeComponentsToPoweredOff(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException, ProcCallException {}
    public String getPcieFrusLctnUsingBusAddr(String sNodeLctn, String sPcieBusAddr) { return ""; }
    public Map<String, String> mapNodelctnAndSlotToLctn() { return new HashMap<>(); }
    public Map<String, String> mapNodelctnAndBusaddrToLctn() { return new HashMap<>(); }
    public Map<String, String> mapNodelctnAndModulelocatorToLctn() { return new HashMap<>(); }
    public Map<String, String> mapNodelctnAndSocketdesignationToLctn() { return new HashMap<>(); }

}
