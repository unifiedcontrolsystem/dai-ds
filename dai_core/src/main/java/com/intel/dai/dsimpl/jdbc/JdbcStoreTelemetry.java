// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.jdbc;

import com.intel.dai.dsapi.StoreTelemetry;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Description of class JdbcStoreTelemetry.
 */
public class JdbcStoreTelemetry implements StoreTelemetry, Closeable, AutoCloseable {
    public JdbcStoreTelemetry(Logger log) {
        log_ = log;
    }

    @Override
    public long logEnvDataAggregated(String sTypeOfData, String sLctn, long lTsInMicroSecs, double dMaxValue,
                                     double dMinValue, double dAvgValue, String sReqAdapterType, long lReqWorkItemId)
            throws DataStoreException {
        createConnection();
        createlogEnvDataAggregatedPreparedCall();

        try {
            telemetryAggregatedData_.setString(1, sLctn);
            telemetryAggregatedData_.setTimestamp(2, new Timestamp(lTsInMicroSecs / 1000));
            telemetryAggregatedData_.setString(3, sTypeOfData);
            telemetryAggregatedData_.setDouble(4, dMaxValue);
            telemetryAggregatedData_.setDouble(5, dMinValue);
            telemetryAggregatedData_.setDouble(6, dAvgValue);
            telemetryAggregatedData_.setString(7, sReqAdapterType);
            telemetryAggregatedData_.setLong(8, lReqWorkItemId);
            telemetryAggregatedData_.execute();
            connection_.commit();
        } catch(SQLException ex) {
            try {
                connection_.rollback(); // Cancel current transaction.
            } catch(SQLException e) {
                log_.exception(e, "Rollback failed after telemetry store failed");
            }
            log_.exception(ex, "An error occurred while executing stored procedure: %s",
                    storeTelementryProcedureName_);
            throw new DataStoreException("Failed to store the aggregated telemetry", ex);
        }

        String sPertinentInfo = "Lctn=" + sLctn + ",TypeOfData=" + sTypeOfData + ",lTsInMicroSecs=" + lTsInMicroSecs;
        log_.info("called stored procedure %s - Lctn=%s, TypeOfData=%s, PertinentInfo='%s'",
                storeTelementryProcedureName_, sLctn, sTypeOfData, sPertinentInfo);

        return 0L;
    }

    protected void createlogEnvDataAggregatedPreparedCall() throws DataStoreException {
        try {
            if (telemetryAggregatedData_ == null)
                telemetryAggregatedData_ = connection_.prepareCall("{call " + storeTelementryProcedureName_ +
                        "(?,?,?,?,?,?,?,?)}");
        } catch(SQLException e) {
            throw new DataStoreException("Failed to create the data store statement for telemetry", e);
        }
    }

    protected void createConnection() throws DataStoreException {
        if(connection_ == null)
            connection_ = DbConnectionFactory.createDefaultConnection();
    }

    @Override
    public void close() throws IOException {
        try {
            telemetryAggregatedData_.close();
            connection_.close();
        } catch(SQLException e) {
            log_.exception(e, "Failed to properly close the tier 2 DB connection");
        }
    }

    private static final String storeTelementryProcedureName_ = "AggregatedEnvDataStore";
    protected Connection connection_ = null;
    protected PreparedStatement telemetryAggregatedData_ = null;
    private Logger log_;
}
