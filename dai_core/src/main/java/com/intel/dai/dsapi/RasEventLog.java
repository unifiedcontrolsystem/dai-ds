// Copyright (C) 2018-2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import org.voltdb.client.ProcCallException;

import java.io.IOException;

public interface RasEventLog {

    String ensureRasDescrNameIsValid(String sDescriptiveName, long workItemId);

    void logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                  String sReqAdapterType, long lReqWorkItemId);
    void logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                      String sReqAdapterType, long lReqWorkItemId);
    void logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId,
                                    long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId);
    void logRasEventCheckForEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                        String sReqAdapterType, long lReqWorkItemId);
    void setRasEventAssociatedJobID(String jobID, String rasEventType, long rasEventID)
            throws IOException;
    void loadRasMetadata();
}
