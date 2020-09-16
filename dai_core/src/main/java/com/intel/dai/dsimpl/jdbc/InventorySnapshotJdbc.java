// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.jdbc;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.InventorySnapshot;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.sql.*;
import java.time.Instant;

public class InventorySnapshotJdbc implements InventorySnapshot {
    public InventorySnapshotJdbc(Logger logger) {
        log_ = logger;
    }

    @Override
    public String getLastHWInventoryHistoryUpdate() throws DataStoreException {
        PreparedStatement retrieveRefLastRawInventoryHistoryUpdate = null;
        try {
            establishConnectionToNearlineDb();
            retrieveRefLastRawInventoryHistoryUpdate = prepareStatement(GET_LAST_RAW_INVENTORY_HISTORY_UPDATE_SQL);
            return executeRetrieveRefLastRawInventoryUpdateStmt(retrieveRefLastRawInventoryHistoryUpdate);
        } catch (DataStoreException e) {
            log_.error("HWI:%n  %s", e.getMessage());
            throw e;    // rethrow
        }
        finally {
            tearDown(retrieveRefLastRawInventoryHistoryUpdate);
        }
    }

    private String executeRetrieveRefLastRawInventoryUpdateStmt(PreparedStatement retrieveRefLastRawInventoryUpdate)
            throws DataStoreException {
        try (ResultSet result = retrieveRefLastRawInventoryUpdate.executeQuery()) {

            if (!result.next()) {
                throw new DataStoreException("HWI:%n  Reference last raw inventory update:!result.next()");
            }
            return  result.getString(1); // first column is indexed at 1
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage());
        } catch (NullPointerException ex) {
            String msg = String.format("HWI:%n  %s", ex.getMessage());
            throw new DataStoreException(msg);
        }
        // Ignore (assume result set is already closed or no longer valid)
    }

    @Override
    public void storeInventorySnapshot(String location, Instant timestamp, String info)
            throws DataStoreException {
        PreparedStatement insertSnapshot = null;
        try {
            establishConnectionToNearlineDb();
            insertSnapshot = prepareStatement(INSERT_SNAPSHOT_SQL);
            executeInsertSnapshotStmt(insertSnapshot, location, timestamp, info);
        } finally {
            tearDown(insertSnapshot);
        }
    }

    @Override
    public PropertyMap retrieveRefSnapshot(String location) throws DataStoreException {
        PreparedStatement retrieveRefSnapshot = null;
        PropertyMap inventoryInfo = null;
        try {
            establishConnectionToNearlineDb();
            retrieveRefSnapshot = prepareStatement(SELECT_REF_SNAPSHOT_BY_LOCATION_SQL);
            String inventoryInfoStr = executeRetrieveRefSnapshotStmt(retrieveRefSnapshot, location);

            if (inventoryInfoStr == null || inventoryInfoStr.isEmpty()) {
                // If the inventory info is empty, simply return an empty map
                inventoryInfo = new PropertyMap();
            } else {
                ConfigIO parser = ConfigIOFactory.getInstance("json");
                inventoryInfo = parser.fromString(inventoryInfoStr).getAsMap();
            }
        } catch (ConfigIOParseException ex) {
            throw new DataStoreException("Unable to parse inventory snapshot information for location: " + location);
        } finally {
            tearDown(retrieveRefSnapshot);
        }

        return inventoryInfo;
    }

    @Override
    public PropertyMap retrieveSnapshot(long id) throws DataStoreException {
        PreparedStatement retrieveSnapshot = null;
        PropertyMap inventoryInfo = null;
        try {
            establishConnectionToNearlineDb();
            retrieveSnapshot = prepareStatement(SELECT_SNAPSHOT_BY_ID_SQL);
            String inventoryInfoStr = executeRetrieveSnapshotByIdStmt(retrieveSnapshot, id);

            if (inventoryInfoStr == null || inventoryInfoStr.isEmpty()) {
                // If the inventory info is empty, simply return an empty map
                inventoryInfo = new PropertyMap();
            } else {
                ConfigIO parser = ConfigIOFactory.getInstance("json");
                inventoryInfo = parser.fromString(inventoryInfoStr).getAsMap();
            }
        } catch (ConfigIOParseException ex) {
            throw new DataStoreException("Unable to parse inventory snapshot information.  ID: " + id);
        } finally {
            tearDown(retrieveSnapshot);
        }

        return inventoryInfo;
    }

    @Override
    public void setReferenceSnapshot(int id) throws DataStoreException {
        PreparedStatement setRefSnapshot = null;
        try {
            establishConnectionToNearlineDb();
            setRefSnapshot = prepareCall(SET_REF_SNAPSHOT_SQL);
            executeSetRefSnapshotProcedure(setRefSnapshot, id);
        } finally {
            tearDown(setRefSnapshot);
        }
    }

    // Convenience method for unit testing
    void setDbConn(Connection dbConn) {
        this.dbConn = dbConn;
    }


    private void establishConnectionToNearlineDb() throws DataStoreException {
        if (dbConn == null) {
            dbConn = DbConnectionFactory.createDefaultConnection();
            if (dbConn == null) {
                throw new DataStoreException("DbConnectionFactory.createDefaultConnection() => null");
            }
        }
    }

    private void executeInsertSnapshotStmt(PreparedStatement insertSnapshot, String location, Instant timestamp,
                                           String info)
            throws DataStoreException {
        try {
            insertSnapshot.setString(1, location);
            insertSnapshot.setTimestamp(2, Timestamp.from(timestamp));
            insertSnapshot.setString(3, info);
            insertSnapshot.execute();
            dbConn.commit();
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while storing the inventory snapshot", ex);
        }
    }

    private String executeRetrieveRefSnapshotStmt(PreparedStatement retrieveRefSnapshot, String location)
            throws DataStoreException {
        ResultSet result = null;
        try {
            retrieveRefSnapshot.setString(1, location);
            result = retrieveRefSnapshot.executeQuery();

            if (!result.next()) {
                throw new DataStoreException("Reference inventory snapshot not found for location: " + location);
            }

            String inventoryInfo = result.getString("InventoryInfo");
            return inventoryInfo;
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while retrieving the reference snapshot", ex);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                // Ignore (assume result set is already closed or no longer valid)
            }
        }
    }

    private String executeRetrieveSnapshotByIdStmt(PreparedStatement retrieveSnapshotById, long id)
            throws DataStoreException {
        ResultSet result = null;
        try {
            retrieveSnapshotById.setLong(1, id);
            result = retrieveSnapshotById.executeQuery();

            if (!result.next()) {
                throw new DataStoreException(String.format("Inventory snapshot with ID %d not found", id));
            }

            String inventoryInfo = result.getString("InventoryInfo");
            return inventoryInfo;
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while retrieving the inventory snapshot", ex);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (SQLException ex) {
                // Ignore (assume result set is already closed or no longer valid)
            }
        }
    }

    private PreparedStatement prepareCall(String call) throws DataStoreException {
        try {
            return dbConn.prepareCall(call);
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to prepare procedure call statement", ex);
        }
    }

    private PreparedStatement prepareStatement(String sql) throws DataStoreException {
        try {
            return dbConn.prepareStatement(sql);
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to prepare SQL statement", ex);
        }
    }

    private void executeSetRefSnapshotProcedure(PreparedStatement setRefSnapshot, int id) throws DataStoreException {
        try {
            setRefSnapshot.setInt(1, id);
            setRefSnapshot.execute();
            dbConn.commit();
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while setting the reference snapshot", ex);
        }
    }

    private void tearDown(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                // Ignore (assume resource is already closed or no longer valid)
            }
        }

        try {
            if (dbConn != null) {
                dbConn.close();
                dbConn = null;
            }
        } catch (SQLException ex) {
            // Ignore (assume resource is already closed or no longer valid)
        }
    }

    Connection dbConn = null;
    private static final String INSERT_SNAPSHOT_SQL =
            "insert into Tier2_InventorySnapshot(Lctn, SnapshotTimestamp, InventoryInfo) values(?,?,?)";
    private static final String SELECT_REF_SNAPSHOT_BY_LOCATION_SQL =
            "select InventoryInfo from Tier2_InventorySnapshot where Lctn = ? and Reference";
    private static final String SELECT_SNAPSHOT_BY_ID_SQL =
            "select InventoryInfo from Tier2_InventorySnapshot where Id = ?";
    private static final String SET_REF_SNAPSHOT_SQL = "{call SetRefSnapshotDataForLctn(?)}";
    private static final String GET_LAST_RAW_INVENTORY_HISTORY_UPDATE_SQL =
            "select LastRawReplacementHistoryUpdate()";

    private Logger log_;
}
