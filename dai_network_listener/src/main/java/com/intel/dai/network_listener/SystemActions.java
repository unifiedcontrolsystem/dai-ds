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
 * SystemActions interface for acting on normalized data from streams..
 */
public interface SystemActions extends AutoCloseable, Closeable {
    /**
     * This method will store raw environmental or log data.
     *
     * @param dataType The type of data to store.
     * @param location The DAI-DS location of the data to store.
     * @param microSecondsTimeStamp The microsecond timestamp for the data.
     * @param value The value to store.
     */
    void storeNormalizedData(String dataType, String location, long microSecondsTimeStamp, double value);

    /**
     * This method will store aggregated or summarized environmental data.
     *
     * @param dataType The type of data to store.
     * @param location The DAI-DS location of the data to store.
     * @param microSecondsTimeStamp The microsecond timestamp for the data.
     * @param min The minimum value for the window of the data.
     * @param max The maximum value for the window of the data.
     * @param average The average value for the window of data.
     */
    void storeAggregatedData(String dataType, String location, long microSecondsTimeStamp, double min, double max,
                             double average);

    /**
     * This method will record the RAS event in the online tier database.
     *
     * @param eventName The DAI-DS name of the event to store.
     * @param instanceData The instance data for the RAS event (free form string).
     * @param location The DAI-DS location of the event to store.
     * @param nsTimestamp The nanosecond timestamp for the event.
     */
    void storeRasEvent(String eventName, String instanceData, String location, long nsTimestamp);

    /**
     * This method will publish raw environmental or log data.
     *
     * @param topic Topic or subject to publish the data on the pub-sub bus.
     * @param dataType The type of data to store.
     * @param location The DAI-DS location of the data to store.
     * @param microSecondsTimeStamp The microsecond timestamp for the data.
     * @param value the value to publish.
     */
    void publishNormalizedData(String topic, String dataType, String location, long microSecondsTimeStamp,
                               double value);

    /**
     * This method will publish aggregated environmental.
     *
     * @param topic Topic or subject to publish the data on the pub-sub bus.
     * @param dataType The type of data to store.
     * @param location The DAI-DS location of the data to store.
     * @param microSecondsTimeStamp The microsecond timestamp for the data.
     * @param min The minimum value for the window of the data.
     * @param max The maximum value for the window of the data.
     * @param average The average value for the window of data.
     */
    void publishAggregatedData(String topic, String dataType, String location, long microSecondsTimeStamp,
                               double min, double max, double average);

    /**
     * This method will publish the RAS event in the online tier database.
     *
     * @param topic Topic or subject to publish the data on the pub-sub bus.
     * @param eventName The DAI-DS name of the event to publish.
     * @param instanceData The instance data for the RAS event (free form string).
     * @param location The DAI-DS location of the event to publish.
     * @param nsTimestamp The nanosecond timestamp for the event.
     */
    void publishRasEvent(String topic, String eventName, String instanceData, String location, long nsTimestamp);

    /**
     * This method will publish the RAS event in the online tier database.
     *
     * @param topic Topic or subject to publish the data on the pub-sub bus.
     * @param event The DAI-DS name of the boot event to publish.
     * @param location The DAI-DS location of the event to publish.
     * @param nsTimestamp The nanosecond timestamp for the event.
     */
    void publishBootEvent(String topic, BootState event, String location, long nsTimestamp);

    /**
     * Change the specific node to a new boot state.
     *
     * @param event The new boot state.
     * @param location The DAI-DS location of the event to publish.
     * @param nsTimestamp The nanosecond timestamp for the event.
     * @param informWlm inform the resource manager, usually false.
     */
    void changeNodeStateTo(BootState event, String location, long nsTimestamp, boolean informWlm);

    /**
     * Change the specific nodes boot image id.
     *
     * @param location Node to set boot image ID for.
     * @param id New ID for node.
     */
    void changeNodeBootImageId(String location, String id);

    /**
     * Update the boot node image list.
     *
     * @param bootImageInfoList The new list.
     */
    void upsertBootImages(List<Map<String,String>> bootImageInfoList);

    /**
     * Log a RAS event denoting the failure to update the node's boot image id.
     *
     * @param location Node where the failure occurred for.
     * @param instanceData Extra free form data on the failure, usually a exception stack dump.
     */
    void logFailedToUpdateNodeBootImageId(String location, String instanceData);

    /**
     * Log a RAS event denoting the failure to update the boot images.
     *
     * @param instanceData Extra free form data on the failure, usually a exception stack dump.
     */
    void logFailedToUpdateBootImageInfo(String instanceData);

    /**
     * Checks to see if the inventory data has been populated yet.
     *
     * @return true if the inventory is missing, false if there is inventory stored.
     * @throws IOException If the check cannot be made.
     * @throws DataStoreException If the check cannot be made.
     */
    boolean isHWInventoryEmpty() throws IOException, DataStoreException;

    /**
     * Updates or inserts new inventory information.
     *
     * @param location The node for the new or changed inventory info.
     * @param canonicalJson contains HW inventory in canonical format.
     */
    void upsertHWInventory(String location, String canonicalJson);

    /**
     * Returns the last update time of the inventory database.
     * @return timestamp string of the last update.
     */
    String lastHWInventoryHistoryUpdate();

    /**
     * Updates or inserts new inventory history.
     *
     * @param canonicalJson contains HW inventory history in canonical format.
     */
    void upsertHWInventoryHistory(String canonicalJson);

    /**
     * deletes inventory information for a node.
     *
     * @param location The node for the deleted inventory info.
     */
    void deleteHWInventory(String location);
}
