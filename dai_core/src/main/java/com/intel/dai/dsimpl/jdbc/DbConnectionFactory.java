// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.jdbc;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.DbConfig;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.LoggerFactory;


/* Class to get a connection to a DB (by default to the Nearline DB) */
public class DbConnectionFactory {
    public interface ConnectionCreator {
        Connection createConnection(String url, String username, String password) throws SQLException;
        Connection createConnection(String url) throws SQLException;
    }

    // NOTE:  by default, this will connect to the Nearline tier database.  Use the other constructors to specify
    public static Connection createDefaultConnection() throws DataStoreException {
        try {
            DbConfig config = new DbConfig();
            config.loadFromFile(DEFAULT_CONFIG_FILE);
            Connection connObj = createConnFromConfig(config);
            return connObj;
        } catch (IOException | ConfigIOParseException ex) {
            throw new DataStoreException("Unable to load DB configuration parameters from file: " +
                    DEFAULT_CONFIG_FILE, ex);
        }
    }

    public static Connection createConnectionFromConfig(DbConfig config) throws DataStoreException {
        Connection connObj = createConnFromConfig(config);
        return connObj;
    }

    public static Connection createAnonymousConnection(String driver, String url, boolean autoCommit)
            throws DataStoreException {
        Connection connObj = createConn(driver, url, null, null, "jdbc", autoCommit);
        return connObj;
    }

    private static Connection createConnFromConfig(DbConfig config) throws DataStoreException {
        Map<String, String> dbParams = config.getDbConfig();
        String url = dbParams.get("url");
        String username = dbParams.get("username");
        String password = dbParams.get("password");
        String dbType = dbParams.get("type");
        Boolean autoCommit = Boolean.parseBoolean(dbParams.get("auto-commit"));
        if (url.contains("postgresql")) {
            autoCommit = Boolean.parseBoolean("false");
        }
        String driver = dbParams.get("driver");

        return createConn(driver, url, username, password, dbType, autoCommit);
    }

    private static Connection createConn(String driver, String url, String username, String password, String dbType,
                                         boolean autoCommit) throws DataStoreException {
        if (driver != null) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException ex) {
                throw new DataStoreException("Unable to load driver: " + driver, ex);
            }
        }

        if (url == null) {
            throw new IllegalArgumentException("A URL must be specified for a database connection");
        }

        // TODO: Adapter needs to be refactored.  This is not the right behavior here, as returning a null connection
        // object will cause null pointer exceptions in various places.
//        if (dbType == null) {
//            throw new IllegalArgumentException("DB type for a database connection");
//        }
//
//        if (dbType.equals("volt")) {
//            LoggerFactory.getInstance().info("createConn() - jdbc is not being initialized due to configuration parameter");
//            return null;
//        }

        try {
            Connection connObj = null;
            if (username != null) {
                if (password == null) {
                    throw new IllegalArgumentException("Fatal Security Error: null password!");
                }

                password = password.trim();
                if (password.isEmpty()) {
                    throw new IllegalArgumentException("Fatal Security Error: empty password!");
                }

                connObj = connCreator.createConnection(url, username, password);
            } else { // Anonymous connection
                connObj = connCreator.createConnection(url);
            }

            connObj.setAutoCommit(autoCommit);
            return connObj;
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while establishing a connection to database at: " +
                    url, ex);
        }
    }

    // Convenience method for unit tests
    static void setConnectionCreator(ConnectionCreator connCreator) {
        DbConnectionFactory.connCreator = connCreator;
    }

    private static final String DEFAULT_CONFIG_FILE = "NearlineConfig.json";
    private static ConnectionCreator connCreator = new ConnectionCreator() {
        @Override
        public Connection createConnection(String url, String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public Connection createConnection(String url) throws SQLException {
            return DriverManager.getConnection(url);
        }
    };
}
