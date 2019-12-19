// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.ServiceInformation;
import com.intel.logging.Logger;
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
public class VoltDbServiceInformation implements ServiceInformation {

    public VoltDbServiceInformation(Logger log, String[] servers) {
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
    public HashMap<String, Object> getServiceOperationInfo(String lctn) throws IOException, ProcCallException {
        ClientResponse response = voltDb_.callProcedure("ServiceOperationGetInfo", lctn);
        log_.info("called stored procedure %s, Lctn=%s", "ServiceOperationGetInfo", lctn);
        HashMap<String, Object> serviceInfo = new HashMap<String, Object>();
        VoltTable vt = response.getResults()[0];

        if(vt.advanceRow()){
            serviceInfo.put("ServiceOperationId", vt.getLong(0));
            serviceInfo.put("Lctn", vt.getString(1));
            serviceInfo.put("TypeOfServiceOperation", vt.getString(2));
            serviceInfo.put("UserStartedService", vt.getString(3));
            serviceInfo.put("UserStoppedService", vt.getString(4));
            serviceInfo.put("State", vt.getString(5));
            serviceInfo.put("Status", vt.getString(6));
            serviceInfo.put("StartTimestamp", vt.getTimestampAsLong(7));
            serviceInfo.put("StopTimestamp", vt.getTimestampAsLong(8));
            serviceInfo.put("StartRemarks", vt.getString(9));
            serviceInfo.put("StopRemarks", vt.getString(10));
            serviceInfo.put("DbUpdatedTimestamp", vt.getTimestampAsLong(11));
            serviceInfo.put("LogFile", vt.getString(12));
        }

        return serviceInfo;
    }

    @Override
    public void close() throws IOException {
        try {
            voltDb_.close();
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }

    // Object state...
    private Logger log_;
    private Client voltDb_;

}
