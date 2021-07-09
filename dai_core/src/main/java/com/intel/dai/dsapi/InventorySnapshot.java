// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.properties.PropertyMap;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;

public interface InventorySnapshot {
    void storeInventorySnapshot(String location, Instant timestamp, String info) throws DataStoreException;
    PropertyMap retrieveRefSnapshot(String location) throws DataStoreException;
    PropertyMap retrieveSnapshot(long id) throws DataStoreException;
    void setReferenceSnapshot(int id) throws DataStoreException;

    /**
     * Retrieves the foreign timestamp string of the last raw inventory history update.  This is a string
     * because its only use case is as a string parameter in a request to the foreign server for more
     * inventory history.
     * @return timestamp string of the last raw inventory history update
     * @throws DataStoreException when the timestamp string cannot be retrieved from the near line adapter
     */
    String getLastHWInventoryHistoryUpdate() throws DataStoreException;

    /**
     * The last json document ingested is characterized by its timestamp and serial, where
     * timestamp is in epoch seconds, and serial is a string.
     * @return
     * @throws DataStoreException
     */
    ImmutablePair<Long, String> getCharacteristicsOfLastRawDimmIngested() throws DataStoreException;
    ImmutablePair<Long, String> getCharacteristicsOfLastRawFruHostIngested() throws DataStoreException;
    boolean isRawDimmDuplicated(String serial, Long timestamp) throws DataStoreException;
    boolean isRawFruHostDuplicated(String mac, Long timestamp) throws DataStoreException;
}
