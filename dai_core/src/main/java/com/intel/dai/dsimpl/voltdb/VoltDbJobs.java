// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.Jobs;
import com.intel.logging.Logger;
import com.intel.dai.exceptions.DataStoreException;
import org.voltdb.client.ProcCallException;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;


/**
 * Description of class VoltDbJobs.
 */
public class VoltDbJobs implements Jobs {

    public VoltDbJobs(Logger log, String[] servers) {
        log_ = log;
        servers_ = servers;
    }

    public void initialize() {
        if(servers_ != null)
            VoltDbClient.initializeVoltDbClient(servers_);
        voltDb_ = getClient();
    }

    protected Client getClient() {
        return VoltDbClient.getVoltClientInstance();
    }

    /**
     * Add nodes to internal cached jobs table
     *
     * @param aNodeLctns array of node locations
     * @param sJobId String with job id
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     *
     */
    @Override
    public void addNodeEntryToInternalCachedJobs(String[] aNodeLctns, String sJobId, long lStartTimeInMicrosecs) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("InternalCachedJobsAddNodeEntry", aNodeLctns, sJobId, lStartTimeInMicrosecs);
            log_.info("addNodeEntryToInternalCachedJobs - called stored procedure %s - JobId=%s, NumComputeNodes=%d",
                    "InternalCachedJobsAddNodeEntry", sJobId, aNodeLctns.length);
            
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

    /**
     * Start job in internal job info table
     *
     * @param sJobId String with job id
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     * @return HashMap with job information
     *
     */
    @Override
    public HashMap<String, Object> startJobinternal(String sJobId, long lStartTimeInMicrosecs) throws DataStoreException {

        HashMap<String, Object> jobInfo = new HashMap<String, Object>();
        try {
            ClientResponse response = voltDb_.callProcedure("InternalJobInfoJobStarted", sJobId, lStartTimeInMicrosecs);
            log_.info("called stored procedure %s, Job Id=%s", "InternalJobInfoJobStarted", sJobId);
            VoltTable vt = response.getResults()[0];

            if(vt.advanceRow()){
                jobInfo.put("JobId", vt.getString(0));
                jobInfo.put("WlmJobStarted", vt.getString(1));
                jobInfo.put("WlmJobStartTime", vt.getTimestampAsLong(2));
                jobInfo.put("WlmJobEndTime", vt.getTimestampAsLong(3));
                jobInfo.put("WlmJobWorkDir", vt.getString(4));
                jobInfo.put("WlmJobCompleted", vt.getString(5));
                jobInfo.put("WlmJobState", vt.getString(6));
            }

        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }

        return jobInfo;
    }

    /**
     * Start job in job table
     *
     * @param sJobId String with job id
     * @param sJobName String with job name
     * @param sServiceNode String with batch service node name
     * @param iNumberOfNodes int with number of nodes on job
     * @param aNodeBitset byte array containing the BitSet of compute nodes for this job
     * @param sUserName String with username of job starter
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     *
     */
    @Override
    public void startJob(String sJobId, String sJobName, String sServiceNode, int iNumberOfNodes, byte[] aNodeBitset, String sUserName, long lStartTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("JobStarted", sJobId, sJobName, sServiceNode, iNumberOfNodes, aNodeBitset, sUserName, lStartTimeInMicrosecs, sAdapterType, lWorkItem);
            log_.info("startJob - called stored procedure %s - JobId=%s, JobName=%s, UserName=%s", "JobStarted", sJobId, sJobName, sUserName);
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }

    }

    /**
     * Complete job in internal job info table
     *
     * @param sJobId String with job id
     * @param sWorkDir String with job working directory
     * @param sWlmJobState String with job state
     * @param lEndTimeInMicrosecs long with end time of job in microseconds
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     * @return HashMap with job information
     *
     */
    @Override
    public HashMap<String, Object> completeJobInternal(String sJobId, String sWorkDir, String sWlmJobState, long lEndTimeInMicrosecs, long lStartTimeInMicrosecs) throws DataStoreException {

        HashMap<String, Object> jobInfo = new HashMap<String, Object>();
        try {
            ClientResponse response = voltDb_.callProcedure("InternalJobInfoJobCompleted", sJobId, sWorkDir, sWlmJobState, lEndTimeInMicrosecs, lStartTimeInMicrosecs);
            log_.info("called stored procedure %s - JobId=%s, WorkDir=%s, WlmJobState=%s", "InternalJobInfoJobCompleted", sJobId, sWorkDir, sWlmJobState);
            VoltTable vt = response.getResults()[0];

            if(vt.advanceRow()){
                jobInfo.put("JobId", vt.getString(0));
                jobInfo.put("WlmJobStarted", vt.getString(1));
                jobInfo.put("WlmJobStartTime", vt.getTimestampAsLong(2));
                jobInfo.put("WlmJobEndTime", vt.getTimestampAsLong(3));
                jobInfo.put("WlmJobWorkDir", vt.getString(4));
                jobInfo.put("WlmJobCompleted", vt.getString(5));
                jobInfo.put("WlmJobState", vt.getString(6));
            }

        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }

        return jobInfo;
    }

    /**
     * Terminate job in job table
     *
     * @param sJobId String with job id
     * @param sAccountingInfo String with accounting information
     * @param iExitStatus int with exit status of job
     * @param sWorkDir String with job working directory
     * @param sWlmJobState String with job state
     * @param lEndTimeInMicrosecs long with end time of job in microseconds
     *
     */
    @Override
    public void terminateJob(String sJobId, String sAccountingInfo, int iExitStatus, String sWorkDir, String sWlmJobState, long lEndTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("JobTerminated", sJobId, sAccountingInfo, iExitStatus, sWorkDir, sWlmJobState, lEndTimeInMicrosecs, sAdapterType, lWorkItem);
            log_.info("terminateJob - called stored procedure %s - JobId=%s, ExitStatus=%d", "JobTerminated", sJobId, iExitStatus);

        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

    /**
     * Terminate job in internal job info table
     *
     * @param lEndTimeInMicrosecs long with end time of job in microseconds
     * @param lUpdateTimeInMicrosecs long with update time of job in microseconds
     * @param sJobId String with job id
     *
     */
    @Override
    public void terminateJobInternal(long lEndTimeInMicrosecs, long lUpdateTimeInMicrosecs, String sJobId) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("InternalCachedJobsTerminated", lEndTimeInMicrosecs, lUpdateTimeInMicrosecs, sJobId);
            log_.info("called stored procedure %s - JobId=%s", "InternalCachedJobsTerminated", sJobId);
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }

    }

    /**
     * Remove job from internal job info table
     *
     * @param sJobId String with job id
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     *
     */
    @Override
    public void removeJobInternal(String sJobId, long lStartTimeInMicrosecs) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("InternalJobInfoJobRemove", sJobId, lStartTimeInMicrosecs);
            log_.info("called stored procedure %s - JobId=%s", "InternalJobInfoJobRemove", sJobId);
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

    /**
     * Check for stale data in internal job table and delete it
     *
     * @param lStaleTimeInMicrosecs long with stale time of jobs to check in microseconds
     * @return HashMap with stale job information
     *
     */
    public HashMap<Long, String> checkStaleDataInternal(long lStaleTimeInMicrosecs) throws DataStoreException {

        HashMap<Long, String> jobInfo = new HashMap<Long, String>();
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
        sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            ClientResponse response = voltDb_.callProcedure("InternalJobInfoCheckForStaleData", lStaleTimeInMicrosecs);
            log_.info("called stored procedure %s", "InternalJobInfoCheckForStaleData");

            long lNumStaleDataEntries = 0L;  // number of "stale data entries" that were detected
            // Loop through each of the possibly stale data
            VoltTable vt = response.getResults()[0];
            for (int iWrkItmCntr = 0; iWrkItmCntr < vt.getRowCount(); ++iWrkItmCntr) {
                vt.advanceRow();
                Date dTempStartTime = new Date(vt.getTimestampAsLong("WlmJobStartTime") / 1000L);
                Date dTempEndTime   = new Date(vt.getTimestampAsLong("WlmJobEndTime") / 1000L);
                String sTempMsg = "stale data entries that were deleted - "
                        + "JobId="           + vt.getString("JobId")                + ", "
                        + "WlmJobStarted="   + vt.getString("WlmJobStarted")        + ", "
                        + "WlmJobStartTime=" + sqlDateFormat.format(dTempStartTime) + ", "
                        + "WlmJobEndTime="   + sqlDateFormat.format(dTempEndTime)   + ", "
                        + "WlmJobCompleted=" + vt.getString("WlmJobCompleted")
                        ;
                jobInfo.put(lNumStaleDataEntries,sTempMsg);
                ++lNumStaleDataEntries;
            }

        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }

        return jobInfo;
    }

    @Override
    public void close() throws IOException {
        try {
            voltDb_.close();
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }

    // Object state...
    private Logger log_;
    private Client voltDb_;
    private String[] servers_;

}
