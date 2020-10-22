// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.logging.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.ProcedureCallback;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltDbCallBackForHouseKeepingTest {

    class Pair<F, S> {
        Pair(F arg1, S arg2) {
            first = arg1;
            second = arg2;
        }

        F first;
        S second;
    }

    
    private Logger log_ = mock(Logger.class);
    private static File rasEventMetaData_ = new File("/tmp/RasEventMetaData.json");
    private ClientStatusListenerExt clientStatus_;
    private Client client_;
    private ClientResponse response_;
    private String adapterType_;
    private String adapterName_;
    private VoltDbRasEventLog rasEventLog;

    @Before
    public void setUp() {
        adapterType_ = "test";
        adapterName_ = "testing";
        clientStatus_ = mock(ClientStatusListenerExt.class);
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
        rasEventLog = mock(VoltDbRasEventLog.class);

    }

    @BeforeClass
    public static void setUpTestClass() throws Exception  {
        File src = new File("../configurations/common/RasEventMetaData.json");
        Files.copy(src.toPath(), rasEventMetaData_.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterClass
    public static void tearDown() {
        rasEventMetaData_.delete();
    }

    private VoltTable makeTable(Collection<VoltDbCallBackForHouseKeepingTest.Pair<String, VoltType>> columns,
                                Collection<? extends Collection<?>> data) {
        ArrayList<VoltTable.ColumnInfo> columnsList = new ArrayList<VoltTable.ColumnInfo>();
        for (VoltDbCallBackForHouseKeepingTest.Pair<String, VoltType> pair : columns) {
            columnsList.add(new VoltTable.ColumnInfo(pair.first, pair.second));
        }
        VoltTable.ColumnInfo[] ciArray = new VoltTable.ColumnInfo[columnsList.size()];
        columnsList.toArray(ciArray);
        VoltTable result = new VoltTable(ciArray);
        for (Collection<?> row : data) {
            Object[] rowArray = new Object[row.size()];
            row.toArray(rowArray);
            result.addRow(rowArray);
        }
        return result;
    }

    @Test
    public void createHouseKeepingCallbackNegative() throws Exception {
        ProcedureCallback callback = new VoltDbCallBackForHouseKeeping( rasEventLog,adapterType_,
                adapterName_, "ComputeNodeDiscovered", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);

        callback = new VoltDbCallBackForHouseKeeping(rasEventLog,adapterType_,
                adapterName_, "ServiceNodeDiscovered", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response_);

        callback = new VoltDbCallBackForHouseKeeping(rasEventLog,adapterType_,
                adapterName_, "ComputeNodeSaveIpAddr", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("not the same as the expected IP address");
        callback.clientCallback(response_);

        callback = new VoltDbCallBackForHouseKeeping(rasEventLog,adapterType_,
                adapterName_, "ServiceNodeSaveIpAddr", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("not the same as the expected IP address");
        callback.clientCallback(response_);

        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "ComputeNodeSetState", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);

        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "ServiceNodeSetState", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response_);

        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "ComputeNodeSaveBootImageInfo", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);
    }


    @Test
    public void createHouseKeepingCallbackPositive() throws Exception {
        VoltTable table = makeTable(new ArrayList<VoltDbCallBackForHouseKeepingTest.Pair<String, VoltType>>() {{
            add(new VoltDbCallBackForHouseKeepingTest.Pair<>("ScalarLong", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(-1L);
            }}); // Row 0
        }});
        table.advanceRow();
        ProcedureCallback callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "RasEventStore", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        callback.clientCallback(response_);

        table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("ScalarLong", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(1L);
            }}); // Row 0
        }});
        table.advanceRow();
        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "RasEventStore", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        callback.clientCallback(response_);

        table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("ScalarLong", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(1L);
            }}); // Row 0
        }});
        table.advanceRow();
        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "ComputeNodeDiscovered", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        callback.clientCallback(response_);

        table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("ScalarLong", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(-1L);
            }}); // Row 0
        }});
        table.advanceRow();
        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "ComputeNodeDiscovered", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        callback.clientCallback(response_);

        table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("ScalarLong", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(-1L);
            }}); // Row 0
        }});
        table.advanceRow();
        callback = new VoltDbCallBackForHouseKeeping(rasEventLog, adapterType_,
                adapterName_, "Unknown", "Info",
                9999L, log_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        callback.clientCallback(response_);
    }
}