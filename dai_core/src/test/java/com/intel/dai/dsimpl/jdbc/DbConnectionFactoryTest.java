package com.intel.dai.dsimpl.jdbc;

import com.intel.dai.DbConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.intel.dai.exceptions.DataStoreException;

public class DbConnectionFactoryTest {
    @Before
    public void setUp() throws Exception {
        mockConfig = Mockito.mock(DbConfig.class);
        mockConnCreator = Mockito.mock(DbConnectionFactory.ConnectionCreator.class);
        mockConn = Mockito.mock(Connection.class);
        Mockito.when(mockConnCreator.
                createConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).
                thenReturn(mockConn);
        Mockito.when(mockConnCreator.createConnection(Mockito.anyString())).thenReturn(mockConn);

        DbConnectionFactory.setConnectionCreator(mockConnCreator);
    }

    @Test
    public void connectsUsingProvidedDbConfig() throws Exception {
        HashMap<String, String> connParams = new HashMap<>();
        connParams.put(USERNAME_KEY, USERNAME);
        connParams.put(PASSWORD_KEY, PASSWORD);
        connParams.put(URL_KEY, URL);
        connParams.put(DB_TYPE_KEY, DB_TYPE);
        Mockito.when(mockConfig.getDbConfig()).thenReturn(connParams);

        Connection createdConn = DbConnectionFactory.createConnectionFromConfig(mockConfig);

        // Must've loaded configuration once
        Mockito.verify(mockConfig, Mockito.times(1)).getDbConfig();
        // Must've created the connection using url, user and password (once)
        Mockito.verify(mockConnCreator, Mockito.times(1)).createConnection(URL, USERNAME, PASSWORD);
        // Must've configured auto-commit to false by default
        Mockito.verify(mockConn, Mockito.times(1)).setAutoCommit(false);

        // Must return the connection object provided by the ConnectionCreator
        Assert.assertSame(mockConn, createdConn);
    }

    @Test
    public void canPerformAnonymousConnection() throws Exception {
        Connection createdConn = DbConnectionFactory.createAnonymousConnection(DRIVER, URL, true);

        // Must've created the connection using the URL only
        Mockito.verify(mockConnCreator, Mockito.times(1)).createConnection(URL);
        // Must've configured auto-commit as specified
        Mockito.verify(mockConn, Mockito.times(1)).setAutoCommit(true);
        // Must return the connection object provided by the ConnectionCreator
        Assert.assertSame(mockConn, createdConn);
    }

    private static final String DRIVER = "com.intel.dai.dsimpl.jdbc.DbConnectionFactoryTest";
    private static final String USERNAME_KEY = "username";
    private static final String USERNAME = "test_user";
    private static final String PASSWORD_KEY = "password";
    private static final String PASSWORD = "1234";
    private static final String URL_KEY = "url";
    private static final String URL = "jdbc:acmedb://localhost:1234/testdb";
    private static final String DB_TYPE_KEY = "type";
    private static final String DB_TYPE = "jdbc";

    DbConfig mockConfig;
    DbConnectionFactory.ConnectionCreator mockConnCreator;
    Connection mockConn;
}