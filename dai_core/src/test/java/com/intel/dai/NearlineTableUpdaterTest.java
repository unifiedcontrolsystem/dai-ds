// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

import java.sql.*;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NearlineTableUpdaterTest {
    @Test
    public void update1() throws Exception {
        Connection connection = mock(Connection.class);
        NearlineTableUpdater updater = new NearlineTableUpdater(connection, mock(Logger.class));
        when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(mock(PreparedStatement.class));
        updater.update("Adapter", makeTable());
        updater.update("Adapter", makeTable());
    }

    @Test
    public void update2() throws Exception {
        Connection connection = mock(Connection.class);
        NearlineTableUpdater updater = new NearlineTableUpdater(connection, mock(Logger.class));
        when(connection.prepareCall(ArgumentMatchers.anyString())).thenReturn(mock(CallableStatement.class));
        updater.update("RasMetaData", makeTable());
        updater.update("RasMetaData", makeTable());
    }

    @Test
    public void update_error() throws Exception {
        Connection connection = mock(Connection.class);
        NearlineTableUpdater updater = new NearlineTableUpdater(connection, mock(Logger.class));
        when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(mock(PreparedStatement.class));
        try {
            updater.update("Adapter_TEST", makeTable());
        }
        catch (DataStoreException e){}
    }

    private VoltTable makeTable() {
        VoltTable result = new VoltTable(new VoltTable.ColumnInfo[] {
                new VoltTable.ColumnInfo("column1", VoltType.STRING),
                new VoltTable.ColumnInfo("column2", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("column3", VoltType.STRING)
        });
        TimestampType ts = new TimestampType(Date.from(Instant.now()));
        result.addRow("red1", ts, "blue1");
        result.addRow("red2", ts, "blue2");
        result.addRow("red3", ts, null);
        return result;
    }
}
