// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.Reservations;
import com.intel.logging.Logger;
import com.intel.dai.exceptions.DataStoreException;
import org.voltdb.client.ProcCallException;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.HashMap;


/**
 * Description of class VoltDbServiceInformation.
 */
public class VoltDbReservations implements Reservations {

    public VoltDbReservations(Logger log, String[] servers) {
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
    @Override
    public void createReservation(String sReservationName, String sUsers, String sNodes, long lStartTimeInMicrosecs, long lEndTimeInMicrosecs, long lCreatedTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("ReservationCreated", sReservationName, sUsers, sNodes, lStartTimeInMicrosecs, lEndTimeInMicrosecs, lCreatedTimeInMicrosecs, sAdapterType, lWorkItem);
            log_.info("called stored procedure %s - Reservation Name=%s, Users=%s, Nodes=%s", "ReservationCreated", sReservationName, sUsers, sNodes);
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

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
    @Override
    public void updateReservation(String sReservationName, String sUsers, String sNodes, long lStartTimeInMicrosecs, long lUpdatedTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("ReservationUpdated", sReservationName, sUsers, sNodes, lStartTimeInMicrosecs, null, lUpdatedTimeInMicrosecs, sAdapterType, lWorkItem);
            log_.info("called stored procedure %s - Reservation Name=%s, Users=%s, Nodes=%s", "ReservationUpdated", sReservationName, sUsers, sNodes);
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

    /**
     * Delete reservation from reservation table
     *
     * @param sReservationName String with reservation name
     * @param lDeletedTimeInMicrosecs long with delete time of reservation in microseconds
     *
     */
    @Override
    public void deleteReservation(String sReservationName, long lDeletedTimeInMicrosecs, String sAdapterType, long lWorkItem) throws DataStoreException {

        try {
            ClientResponse response = voltDb_.callProcedure("ReservationDeleted", sReservationName, lDeletedTimeInMicrosecs, sAdapterType, lWorkItem);
            log_.info("called stored procedure %s - Reservation Name=%s", "ReservationDeleted", sReservationName);
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Retrieving node state failed", e);
        }
    }

    // Object state...
    private Logger log_;
    private Client voltDb_;
    private String[] servers_;

}
