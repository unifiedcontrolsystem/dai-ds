// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

import java.io.Closeable;

/**
 * Description of interface DbStatusApi.
 */
public interface DbStatusApi extends AutoCloseable, Closeable {
    /**
     * Wait for a DbStatusEnum.SCHEMA_LOADED or a timeout.
     *
     * @param beforeEpochTimeMs The timeout value in milliseconds.
     * @return <b>true</b> if the state was reached before the timeout, <b>false</b> otherwise.
     */
    boolean waitForSchemaLoaded(long beforeEpochTimeMs);

    /**
     * Wait for a DbStatusEnum.POPULATE_DATA_COMPLETED or a timeout.
     *
     * @param beforeEpochTimeMs  The timeout value in milliseconds.
     * @return <b>true</b> if the state was reached before the timeout, <b>false</b> otherwise.
     */
    boolean waitForDataPopulated(long beforeEpochTimeMs);

    /**
     * Get the DB status as an DbStatusEnum.
     *
     * @return The enum of the current state. If the DB is is not running or can't be read then
     * DbStatusEnum.SCHEMA_MISSING is returned.
     * @throws DataStoreException When the DB execution fails internally.
     */
    DbStatusEnum getStatus() throws DataStoreException;

    /**
     * Get the current error message. If no error then this is likely an empty string.
     *
     * @return The text of the error message.
     * @throws DataStoreException When the DB execution fails internally.
     */
    String getStatusDescription() throws DataStoreException;

    /**
     * Get a flag indicating if the status is an error state.
     *
     * @param statusValue The value returned from getStatus().
     * @return <b>true</b> when the state is an error state, <b>false</b> otherwise.
     */
    boolean isErrorState(DbStatusEnum statusValue);

    /**
     * Call to signal the start of the initial data population phase.
     *
     * @throws DataStoreException When the DB execution fails internally.
     */
    void setDataPopulationStarting() throws DataStoreException;

    /**
     * Call to signal that the initial data population phase completed normally.
     *
     * @param description A description to store or an empty string.
     * @throws DataStoreException When the DB execution fails internally.
     */
    void setDataPopulationComplete(String description) throws DataStoreException;

    /**
     * Call to signal that the initial data population phase failed to complete.
     *
     * @param description Recommended that the actual error text be supplied here!
     * @throws DataStoreException When the DB execution fails internally.
     */
    void setDataPopulationFailed(String description) throws DataStoreException;
}
