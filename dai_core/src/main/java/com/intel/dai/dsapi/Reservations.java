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
public interface Reservations extends AutoCloseable, Closeable {

    /**
     * Create reservation in reservation table
     *
     * @param sReservationName String with reservation name
     * @param sUsers String with users on reservation
     * @param sNodes String with nodes on reservation
     * @param lStartTimeInMicrosecs long with start time of reservation in microseconds
     * @param lEndTimeInMicrosecs long with end time of reservation in microseconds
     * @param lCreatedTimeInMicrosecs long with creation time of reservation in microseconds
     *
     */
    public void createReservation(String sReservationName, String sUsers, String sNodes, long lStartTimeInMicrosecs, long lEndTimeInMicrosecs, long lCreatedTimeInMicrosecsString, String sAdapterType, long lWorkItem) throws DataStoreException;

    /**
     * Update reservation in reservation table
     *
     * @param sReservationName String with reservation name
     * @param sUsers String with users on reservation
     * @param sNodes String with nodes on reservation
     * @param lStartTimeInMicrosecs long with start time of reservation in microseconds
     * @param lUpdatedTimeInMicrosecs long with update time of reservation in microseconds
     *
     */
    public void updateReservation(String sReservationName, String sUsers, String sNodes, long lStartTimeInMicrosecs, long lUpdatedTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException;

    /**
     * Delete reservation from reservation table
     *
     * @param sReservationName String with reservation name
     * @param lDeletedTimeInMicrosecs long with delete time of reservation in microseconds
     *
     */
    public void deleteReservation(String sReservationName, long lDeletedTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException;

}