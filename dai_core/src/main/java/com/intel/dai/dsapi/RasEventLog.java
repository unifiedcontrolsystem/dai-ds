// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import java.io.IOException;

public interface RasEventLog {

    String getRasEventType(String sDescriptiveName, long workItemId) throws IOException;

    void logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                  String sReqAdapterType, long lReqWorkItemId);
    void logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                      String sReqAdapterType, long lReqWorkItemId);
    void logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId,
                                    long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId);
    void logRasEventCheckForEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs,
                                        String sReqAdapterType, long lReqWorkItemId);
}
