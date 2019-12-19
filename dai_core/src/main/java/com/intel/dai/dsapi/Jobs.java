// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import java.io.Closeable;
import java.io.IOException;
import com.intel.dai.exceptions.DataStoreException;
import java.util.HashMap;

/**
 * Description of interface ServiceInformation.
 */
public interface Jobs extends AutoCloseable, Closeable {

    /**
     * Add nodes to internal cached jobs table
     *
     * @param aNodeLctns array of node locations
     * @param sJobId String with job id
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     *
     */
    public void addNodeEntryToInternalCachedJobs(String[] aNodeLctns, String sJobId, long lStartTimeInMicrosecs) throws DataStoreException;

    /**
     * Start job in internal job info table
     *
     * @param sJobId String with job id
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     * @return HashMap with job information
     *
     */
    public HashMap<String, Object> startJobinternal(String sJobId, long lStartTimeInMicrosecs) throws DataStoreException;

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
    public void startJob(String sJobId, String sJobName, String sServiceNode, int iNumberOfNodes, byte[] aNodeBitset, String sUserName, long lStartTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException;

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
    public HashMap<String, Object> completeJobInternal(String sJobId, String sWorkDir, String sWlmJobState, long lEndTimeInMicrosecs, long lStartTimeInMicrosecs) throws DataStoreException;

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
    public void terminateJob(String sJobId, String sAccountingInfo, int iExitStatus, String sWorkDir, String sWlmJobState, long lEndTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException;

    /**
     * Terminate job in internal job info table
     *
     * @param lEndTimeInMicrosecs long with end time of job in microseconds
     * @param lUpdateTimeInMicrosecs long with update time of job in microseconds
     * @param sJobId String with job id
     *
     */
    public void terminateJobInternal(long lEndTimeInMicrosecs, long lUpdateTimeInMicrosecs, String sJobId) throws DataStoreException;

    /**
     * Remove job from internal job info table
     *
     * @param sJobId String with job id
     * @param lStartTimeInMicrosecs long with start time of job in microseconds
     *
     */
    public void removeJobInternal(String sJobId, long lStartTimeInMicrosecs) throws DataStoreException;

    /**
     * Check for stale data in internal job table and delete it
     *
     * @param lStaleTimeInMicrosecs long with stale time of jobs to check in microseconds
     * @return HashMap with stale job information
     *
     */
    public HashMap<Long, String> checkStaleDataInternal(long lStaleTimeInMicrosecs) throws DataStoreException;
}