package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.DbStatusEnum;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.time.Instant;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DbStatusApiImplTest {
    @Before
    public void setUp() throws Exception {
        client_ = mock(Client.class);
        results_ = mock(ClientResponse.class);
        doAnswer((Answer<VoltTable[]>)invocation -> table_).when(results_).getResults();
        status_ = new DbStatusApiImpl(log_, client_);
        when(client_.callProcedure(anyString())).thenReturn(results_);
        table_ = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
    }

    private void setStatusTable(String status, String description) {
        table_[0].addRow(status, description);
    }

    @Test
    public void closeNormalTest() throws Exception {
        status_.close();
    }

    @Test(expected = IOException.class)
    public void closeExceptionTest() throws Exception {
        doThrow(InterruptedException.class).when(client_).close();
        status_.close();
    }

    @Test
    public void getStatusTest() throws DataStoreException {
        setStatusTable(DbStatusEnum.SCHEMA_LOADING.getDbString(), "");
        assertEquals(DbStatusEnum.SCHEMA_LOADING, status_.getStatus());
        assertFalse(status_.isErrorState(status_.getStatus()));
    }

    @Test
    public void getDescriptionTest() throws DataStoreException {
        setStatusTable(DbStatusEnum.SCHEMA_LOADING.getDbString(), "");
        assertEquals("", status_.getStatusDescription());
    }

    @Test(expected = DataStoreException.class)
    public void getStatusExceptionTest() throws Exception {
        when(client_.callProcedure(anyString())).thenThrow(ProcCallException.class);
        setStatusTable(DbStatusEnum.SCHEMA_LOADING.getDbString(), "");
        assertEquals(DbStatusEnum.SCHEMA_LOADING, status_.getStatus());
    }

    @Test
    public void getStatusEmptyTest() throws Exception {
        table_ = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        assertEquals(DbStatusEnum.SCHEMA_MISSING, status_.getStatus());
    }

    @Test
    public void isErrorSchemaTest() throws Exception {
        setStatusTable(DbStatusEnum.SCHEMA_ERROR.getDbString(), "");
        DbStatusEnum status = status_.getStatus();
        assertEquals(DbStatusEnum.SCHEMA_ERROR, status);
        assertTrue(status_.isErrorState(status));
        assertTrue(status_.isErrorState(null));
    }

    @Test
    public void isErrorPopulateTest() throws Exception {
        setStatusTable(DbStatusEnum.POPULATE_DATA_ERROR.getDbString(), "");
        DbStatusEnum status = status_.getStatus();
        assertEquals(DbStatusEnum.POPULATE_DATA_ERROR, status);
        assertTrue(status_.isErrorState(status));
    }

    @Test
    public void setDataPopulationStartingTest() throws DataStoreException {
        status_.setDataPopulationStarting();
    }

    @Test
    public void setDataPopulationCompleteTest() throws DataStoreException {
        status_.setDataPopulationComplete("");
    }

    @Test
    public void setDataPopulationFailedTest() throws DataStoreException {
        status_.setDataPopulationFailed("Some Error");
    }

    @Test(expected = DataStoreException.class)
    public void setDataPopulationStartingExceptionTest() throws Exception {
        doThrow(ProcCallException.class).when(client_).callProcedure(anyString(), anyString(), anyString());
        status_.setDataPopulationStarting();
    }

    @Test
    public void waitForSchemaLoadedTest() throws Exception {
        VoltTable[] table1 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table1[0].addRow(DbStatusEnum.SCHEMA_LOADING.getDbString(), "");
        VoltTable[] table2 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table2[0].addRow(DbStatusEnum.SCHEMA_LOADED.getDbString(), "");
        when(results_.getResults()).thenReturn(table1);
        new Thread(()-> {
            sleep(55);
            when(results_.getResults()).thenReturn(table2);
        }).start();
        assertTrue(status_.waitForSchemaLoaded(Instant.now().toEpochMilli() + 150L));
    }

    @Test
    public void waitForSchemaLoadedTimeoutTest() throws Exception {
        VoltTable[] table1 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table1[0].addRow(DbStatusEnum.SCHEMA_LOADING.getDbString(), "");
        VoltTable[] table2 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table2[0].addRow(DbStatusEnum.SCHEMA_LOADED.getDbString(), "");
        when(results_.getResults()).thenReturn(table1);
        new Thread(()-> {
            sleep(55);
        }).start();
        assertFalse(status_.waitForSchemaLoaded(Instant.now().toEpochMilli() + 60L));
    }

    @Test
    public void waitForDataPopulatedTest() throws Exception {
        VoltTable[] table1 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table1[0].addRow(DbStatusEnum.POPULATE_DATA_STARTED.getDbString(), "");
        VoltTable[] table2 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table2[0].addRow(DbStatusEnum.POPULATE_DATA_COMPLETED.getDbString(), "");
        when(results_.getResults()).thenReturn(table1);
        new Thread(()-> {
            sleep(55);
            when(results_.getResults()).thenReturn(table2);
        }).start();
        assertTrue(status_.waitForDataPopulated(Instant.now().toEpochMilli() + 150L));
    }

    @Test
    public void waitForDataPopulatedErrorTest() throws Exception {
        VoltTable[] table1 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table1[0].addRow(DbStatusEnum.POPULATE_DATA_STARTED.getDbString(), "");
        VoltTable[] table2 = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Status", VoltType.STRING),
                        new VoltTable.ColumnInfo("Description", VoltType.STRING)
                }, 2)
        };
        table2[0].addRow(DbStatusEnum.POPULATE_DATA_ERROR.getDbString(), "");
        when(results_.getResults()).thenReturn(table1);
        new Thread(()-> {
            sleep(55);
            when(results_.getResults()).thenReturn(table2);
        }).start();
        assertFalse(status_.waitForDataPopulated(Instant.now().toEpochMilli() + 60L));
    }

    @Test
    public void waitForDataPopulatedExceptionTest() throws Exception {
        when(client_.callProcedure(anyString())).thenThrow(ProcCallException.class);
        assertFalse(status_.waitForDataPopulated(Instant.now().toEpochMilli() + 60L));
    }

    private void sleep(long msDelay) {
        try { Thread.sleep(msDelay); } catch(InterruptedException e) { /* Ignore interruption */ }
    }

    private DbStatusApiImpl status_;
    private ClientResponse results_;
    private Client client_;
    private Logger log_ = mock(Logger.class);
    private VoltTable[] table_;
}
