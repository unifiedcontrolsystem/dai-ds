package com.intel.dai.dsimpl.jdbc;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class JdbcStoreTelemetryTest {
    class MockJdbcStoreTelemetry extends JdbcStoreTelemetry {
        public MockJdbcStoreTelemetry(Logger log) {
            super(log);
        }

        @Override
        protected void createlogEnvDataAggregatedPreparedCall() throws DataStoreException {
            telemetryAggregatedData_ = mock(PreparedStatement.class);
        }

        @Override
        protected void createConnection() throws DataStoreException {
            connection_ = mock(Connection.class);
            if(commitFail) {
                try {
                    doThrow(SQLException.class).when(connection_).commit();
                } catch(SQLException e) { /* Can't throw in this context */ }
                if(rollbackFail) {
                    try {
                        doThrow(SQLException.class).when(connection_).rollback();
                    } catch(SQLException e) { /* Can't throw in this context */ }
                }
            }
        }
    }

    @Before
    public void setUp() {
        commitFail = false;
        rollbackFail = false;
        store = new MockJdbcStoreTelemetry(mock(Logger.class));
    }

    @Test
    public void logEnvDataAggregated() throws Exception {
        store.logEnvDataAggregated("TEST_DATA", "LOCATION", 1L, 0.0, 0.0, 0.0, "TYPE", 999L);
    }

    @Test(expected = DataStoreException.class)
    public void logEnvDataAggregatedException() throws Exception {
        commitFail = true;
        store.logEnvDataAggregated("TEST_DATA", "LOCATION", 1L, 0.0, 0.0, 0.0, "TYPE", 999L);
    }

    @Test(expected = DataStoreException.class)
    public void logEnvDataAggregatedException2() throws Exception {
        commitFail = true;
        rollbackFail = true;
        store.logEnvDataAggregated("TEST_DATA", "LOCATION", 1L, 0.0, 0.0, 0.0, "TYPE", 999L);
    }

    @Test
    public void close() throws Exception {
        store.logEnvDataAggregated("TEST_DATA", "LOCATION", 1L, 0.0, 0.0, 0.0, "TYPE", 999L);
        store.close();
    }

    @Test
    public void closeException() throws Exception {
        store.logEnvDataAggregated("TEST_DATA", "LOCATION", 1L, 0.0, 0.0, 0.0, "TYPE", 999L);
        doThrow(SQLException.class).when(store.telemetryAggregatedData_).close();
        store.close();
    }

    boolean commitFail = false;
    boolean rollbackFail = false;
    JdbcStoreTelemetry store;
}