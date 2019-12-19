// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.DbStatusApi;
import com.intel.dai.dsapi.DbStatusEnum;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;

/**
 * Description of class VoltDbStatus.
 */
public class DbStatusApiImpl implements DbStatusApi {
    public DbStatusApiImpl(Logger log, Client volt) {
        log_ = log;
        voltDb_ = volt;
    }

    @Override
    public boolean waitForSchemaLoaded(long beforeEpochTimeMs) {
        return commonWaitRoutine(beforeEpochTimeMs, DbStatusEnum.SCHEMA_LOADED,
                DbStatusEnum.SCHEMA_ERROR);
    }

    @Override
    public boolean waitForDataPopulated(long beforeEpochTimeMs) {
        return commonWaitRoutine(beforeEpochTimeMs, DbStatusEnum.POPULATE_DATA_COMPLETED,
                DbStatusEnum.POPULATE_DATA_ERROR);
    }

    @Override
    public DbStatusEnum getStatus() throws DataStoreException {
        String status = getStringColumnByName(STATUS_FIELD);
        return DbStatusEnum.fromDbString(status);
    }

    @Override
    public String getStatusDescription() throws DataStoreException {
        return getStringColumnByName(DESCRIPTION_FIELD);
    }

    @Override
    public boolean isErrorState(DbStatusEnum status) {
        return status == null || status.isError();
    }

    @Override
    public void setDataPopulationStarting() throws DataStoreException {
        setPopulateStatus(DbStatusEnum.POPULATE_DATA_STARTED, "");
    }

    @Override
    public void setDataPopulationComplete(String description) throws DataStoreException {
        setPopulateStatus(DbStatusEnum.POPULATE_DATA_COMPLETED, description);
    }

    @Override
    public void setDataPopulationFailed(String description) throws DataStoreException {
        setPopulateStatus(DbStatusEnum.POPULATE_DATA_ERROR, description);
    }

    @Override
    public void close() throws IOException {
        try {
            voltDb_.close();
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void setPopulateStatus(DbStatusEnum status, String description) throws DataStoreException {
        try {
            voltDb_.callProcedure(PROC_SET_POP_STATUS, status.getDbString(), description);
        } catch(ProcCallException | IOException e) {
            log_.exception(e);
            throw new DataStoreException(String.format("Failed to set the '%s' state!", status.toString()), e);
        }
    }

    private String getStringColumnByName(String name) throws DataStoreException {
        String result;
        try {
            ClientResponse response = voltDb_.callProcedure(PROC_GETSTATUS);
            try {
                response.getResults()[0].advanceRow();
                result = (String) response.getResults()[0].get(name, VoltType.STRING);
            } catch(Exception e) {
                return DbStatusEnum.SCHEMA_MISSING.getDbString();
            }
        } catch(ProcCallException | IOException e) {
            log_.exception(e);
            throw new DataStoreException(String.format("Failed to get status field '%s'!", name), e);
        }
        return result;
    }

    private boolean commonWaitRoutine(long beforeEpochTimeMs, DbStatusEnum target, DbStatusEnum error) {
        while(Instant.now().toEpochMilli() <= beforeEpochTimeMs) {
            if(loopDelayReturningInterrupted()) break; // Quick exit on interruption.
            try {
                DbStatusEnum status = getStatus();
                if (status.equals(target)) return true; // target reached.
                if (status.equals(error)) return false; // Quick exit on error.
            } catch(DataStoreException e) {
                log_.exception(e);
                break;
            }
        }
        return false;

    }
    private boolean loopDelayReturningInterrupted() {
        try { Thread.sleep(LOOP_DELAY_MS); } catch(InterruptedException e) { return true; }
        return false;
    }

    // Object state...
    private Logger log_;
    private Client voltDb_;

    // Internal specific constants....
    private static final long   LOOP_DELAY_MS         = 50L;

    private static final String STATUS_FIELD          = "Status";
    private static final String DESCRIPTION_FIELD     = "Description";

    private static final String PROC_GETSTATUS        = "GetDbStatus";
    private static final String PROC_SET_POP_STATUS   = "SetDbPopulationStatus";
}
