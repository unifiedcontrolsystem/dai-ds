// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.dai.dsapi.BootState;
import com.intel.dai.exceptions.DataStoreException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Description of interface StoreOrPublish.
 */
public interface SystemActions extends AutoCloseable, Closeable {
    void storeNormalizedData(String dataType, String location, long microSecondsTimeStamp, double value);
    void storeAggregatedData(String dataType, String location, long microSecondsTimeStamp, double min, double max,
                             double average);
    void storeRasEvent(String eventName, String instanceData, String location, long nsTimestamp);

    void publishNormalizedData(String topic, String dataType, String location, long microSecondsTimeStamp,
                               double value);
    void publishAggregatedData(String topic, String dataType, String location, long microSecondsTimeStamp,
                               double min, double max, double average);
    void publishRasEvent(String topic, String eventName, String instanceData, String location, long nsTimestamp);
    void publishBootEvent(String topic, BootState event, String location, long nsTimestamp);
    void changeNodeStateTo(BootState event, String location, long nsTimestamp, boolean informWlm);
    void changeNodeBootImageId(String location, String id);
    void upsertBootImages(List<Map<String,String>> bootImageInfoList);
    void logFailedToUpdateNodeBootImageId(String location, String instanceData);
    void logFailedToUpdateBootImageInfo(String instanceData);

//    boolean isHWInventoryEmpty() throws IOException, DataStoreException;
//    void upsertHWInventory(String location);
}
