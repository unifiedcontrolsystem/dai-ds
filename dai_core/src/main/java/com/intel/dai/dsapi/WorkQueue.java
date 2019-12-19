// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import java.io.IOException;
import java.util.Map;

public interface WorkQueue {
    boolean grabNextAvailWorkItem() throws IOException;
    boolean grabNextAvailWorkItem(String sQueueName) throws IOException;
    long baseWorkItemId();
    boolean isThisNewWorkItem();
    void setupAdaptersBaseWorkItem();
    String workToBeDone();
    int amtTimeToWait();

    void handleProcessingWhenUnexpectedWorkItem() throws IOException;

    long queueWorkItem(String sWrkAdapType, String sQueue, String sWorkWantDone, String sWorkWantDoneParameters,
                       boolean bNotifyCallerWhenWorkItemFinishes,
                       String sReqAdapterType, long lReqWorkItemId)
            throws IOException;
    long queueWorkItem(String sWrkAdapType, String sQueue, String sWorkWantDone, Map<String, String> sWorkWantDoneParameters,
                              boolean bNotifyCallerWhenWorkItemFinishes,
                              String sReqAdapterType, long lReqWorkItemId)
            throws IOException;

    void finishedWorkItem(String sCmdForMsg, long lWorkItemId, String sWorkItemResults)
            throws IOException;
    void finishedWorkItemDueToError(String sCmdForMsg, long lWorkItemId, String sWorkItemResults)
            throws IOException;
    long workItemId();
    String[] waitForWorkItemToFinishAndMarkDone(String sCmdForMsg, String sWaitingForAdapterType,
                                                long lWaitingForWorkItemId, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, InterruptedException;
    String workingResults();
    long saveWorkItemsRestartData(long lWorkItemId, String sRestartData, boolean bInsertRowIntoHistory, long lTsInMicroSecs)
            throws IOException;
    long saveWorkItemsRestartData(long lWorkItemId, String sRestartData)
            throws IOException;
    long saveWorkItemsRestartData(long lWorkItemId, String sRestartData, boolean bInsertRowIntoHistory)
            throws IOException;
    boolean wasWorkDone();
    String[] getWorkItemStatus(String sWaitingForAdapterType, long lWaitingForWorkItemId)
            throws IOException;
    void markWorkItemDone(String sWorkItemAdapterType, long lWorkItemId)
            throws IOException;
    Map<String, String> getClientParameters();
    String[] getClientParameters(String seperator);

    //--------------------------------------------------------------------------
    // Get the timestamp string from a work item's working results field.
    //--------------------------------------------------------------------------
    String getTsFromWorkingResults(String sWrkResults);
}
