// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.WLMInformation;
import com.intel.logging.Logger;
import org.voltdb.client.ProcCallException;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.types.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Date;
import java.time.Instant;


/**
 * Description of class VoltDbWLMInformation.
 */
public class VoltDbWLMInformation implements WLMInformation {

    public VoltDbWLMInformation(Logger log, String[] servers) {
        log_ = log;
        VoltDbClient.initializeVoltDbClient(servers);
    }

    public void initialize() {
        voltDb_ = getClient();
    }

    protected Client getClient() {
        return VoltDbClient.getVoltClientInstance();
    }

    @Override
    public HashMap<String, String> getUsersForActiveReservation(String lctn) throws IOException, ProcCallException {

        String wlmUsers, wlmNodes;
        TimestampType startTime, endTime, now;
        now = new TimestampType(Date.from(Instant.now()));

        ClientResponse response = voltDb_.callProcedure("ReservationListAtTime", null, null);
        log_.info("called stored procedure %s", "ReservationListAtTime");
        VoltTable vt = response.getResults()[0];

        HashMap<String, String> reservationInfo = new HashMap<String, String>();
        while(vt.advanceRow()){
            wlmUsers = vt.getString("Users");
            wlmNodes = vt.getString("Nodes");
            startTime = vt.getTimestampAsTimestamp("StartTimestamp");
            endTime = vt.getTimestampAsTimestamp("EndTimestamp");

            if(now.compareTo(startTime) >= 0 && now.compareTo(endTime) <= 0) {
                if(wlmNodes.indexOf(lctn) != -1) {
                    reservationInfo.put(lctn, wlmUsers);
                }
            }
        }

        return reservationInfo;
    }

    // Object state...
    private Logger log_;
    private Client voltDb_;

}
