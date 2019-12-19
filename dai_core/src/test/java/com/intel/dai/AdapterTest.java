// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.*;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;

import org.mockito.ArgumentMatchers;
import sun.misc.SignalHandler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdapterTest {
    class MockAdapter extends Adapter {
        public MockAdapter(String sThisAdaptersAdapterType, String sAdapterName) throws IOException {
            super(sThisAdaptersAdapterType, sAdapterName, mock(Logger.class));
        }

        @Override
        public WorkQueue setUpAdapter(String servers, String sSnLctn) throws AdapterException {
            WorkQueue workQueue = mock(WorkQueue.class);
            this.workQueue(workQueue);
            return workQueue;
        }

        @Override
        void initializeSignalHandler() {
            if (callBase_)
                super.initializeSignalHandler();
            else
                mSignalHandler = mock(SignalHandler.class);
        }

        @Override
        void initializeClient() {
            if (callBase_)
                super.initializeClient();
            else
                mVoltDbStatusListener = clientStatus_;
        }

        @Override
        public void enableSignalHandlers() {
            if (callBase_)
                super.enableSignalHandlers();
        }

        @Override
        public Client client() {
            return client_;
        }

        @Override
        ClientResponse response() {
            if (callBase_)
                return super.response();
            return response_;
        }

        @Override
        void adapterTerminating(String sBaseWorkItemResults)
                throws IOException, InterruptedException, AdapterException {
            super.adapterTerminating(sBaseWorkItemResults);
            if(throwRuntimeException_)
                throw new RuntimeException("*** Test only path...");
        }

        @Override
        public boolean isComputeNodeLctn(String lctn) throws IOException, ProcCallException {
            if(dontCallBase_)
                return true;
            else
                return super.isComputeNodeLctn(lctn);
        }

        boolean callBase_ = false;
        boolean throwRuntimeException_ = false;
        boolean dontCallBase_ = false;
    }

    class Pair<F, S> {
        Pair(F arg1, S arg2) {
            first = arg1;
            second = arg2;
        }

        F first;
        S second;
    }

    @Before
    public void setUp() throws Exception {
        adapter_ = new MockAdapter("TEST", "Testing");
        adapter_.setShutdownHandler(mock(AdapterShutdownHandler.class));
        workQueue_ = adapter_.setUpAdapter("localhost", null);
        clientStatus_ = mock(ClientStatusListenerExt.class);
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
    }

    @BeforeClass
    public static void setUpTestClass() throws Exception  {
        File src = new File("../config-files/RasEventMetaData.json");
        Files.copy(src.toPath(), rasEventMetaData_.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @AfterClass
    public static void tearDown() {
        rasEventMetaData_.delete();
    }

    private VoltTable makeTable(Collection<Pair<String, VoltType>> columns, Collection<? extends Collection<?>> data) {
        ArrayList<VoltTable.ColumnInfo> columnsList = new ArrayList<VoltTable.ColumnInfo>();
        for (Pair<String, VoltType> pair : columns) {
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
    public void isComputeNodeLctn() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Lctn", VoltType.STRING));           // Column 0
            add(new Pair<>("SequenceNumber", VoltType.BIGINT)); // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add("R0-CN43");
                add(42L);
            }}); // Row 0
            add(new ArrayList<Object>() {{
                add("R0-CN44");
                add(43L);
            }}); // Row 1
            add(new ArrayList<Object>() {{
                add("R0-CN45");
                add(44L);
            }}); // Row 2
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        adapter_.isComputeNodeLctn("");
        adapter_.isComputeNodeLctn("R0-CN44");
    }

    @Test
    public void isServiceNodeLctn() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Lctn", VoltType.STRING));           // Column 0
            add(new Pair<>("HostName", VoltType.STRING));       // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add("SN0");
                add("submn1");
            }}); // Row 0
            add(new ArrayList<Object>() {{
                add("SN1");
                add("submn2");
            }}); // Row 1
            add(new ArrayList<Object>() {{
                add("SN2");
                add("submn3");
            }}); // Row 2
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        adapter_.isServiceNodeLctn("");
        adapter_.isServiceNodeLctn("R0-CN44");
    }

    @Test
    public void mapCompNodeLctnToHostName() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Lctn", VoltType.STRING));           // Column 0
            add(new Pair<>("HostName", VoltType.STRING));       // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add("R0-CN43");
                add("node44");
            }}); // Row 0
            add(new ArrayList<Object>() {{
                add("R0-CN44");
                add("node45");
            }}); // Row 1
            add(new ArrayList<Object>() {{
                add("R0-CN45");
                add("node46");
            }}); // Row 2
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        adapter_.mapCompNodeLctnToHostName();
        adapter_.mapCompNodeLctnToHostName();
    }

    @Test
    public void mapCompNodeHostNameToLctn() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Lctn", VoltType.STRING));           // Column 0
            add(new Pair<>("HostName", VoltType.STRING));       // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add("R0-CN43");
                add("node44");
            }}); // Row 0
            add(new ArrayList<Object>() {{
                add("R0-CN44");
                add("node45");
            }}); // Row 1
            add(new ArrayList<Object>() {{
                add("R0-CN45");
                add("node46");
            }}); // Row 2
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        adapter_.mapCompNodeHostNameToLctn();
        adapter_.mapCompNodeHostNameToLctn();
    }

    @Test
    public void mapCompNodeLctnToIpAddrAndBmcIpAddr() throws Exception {
        setUp();
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Lctn", VoltType.STRING));         // Column 0
            add(new Pair<>("IpAddr", VoltType.STRING));       // Column 1
            add(new Pair<>("BmcIpAddr", VoltType.STRING));    // Column 2
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add("R0-CN43");
                add("10.128.0.43");
                add("10.129.0.43");
            }}); // Row 0
            add(new ArrayList<Object>() {{
                add("R0-CN44");
                add("10.128.0.44");
                add("10.129.0.44");
            }}); // Row 1
            add(new ArrayList<Object>() {{
                add("R0-CN45");
                add("10.128.0.45");
                add("10.129.0.45");
            }}); // Row 2
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        adapter_.mapCompNodeLctnToIpAddrAndBmcIpAddr();
        adapter_.mapCompNodeLctnToIpAddrAndBmcIpAddr();
    }

    @Test
    public void miscAccessors() {
        adapter_.adapterAbnormalShutdown();
        adapter_.adapterAbnormalShutdown(false);
        adapter_.adapterShuttingDown();
        adapter_.adapterId();
        adapter_.adapterName();
        adapter_.adapterName("NewName");
        adapter_.adapterShutdownStarted(false);
        adapter_.adapterType();
        adapter_.client();
        adapter_.client(mock(Client.class));
        adapter_.dataMoverResultTblIndxToTableNameMap();
        adapter_.callBase_ = true;
        adapter_.response();
        adapter_.callBase_ = false;
        adapter_.signalHandler();
    }

    @Test
    public void enableSignalHandlers() {
        adapter_.initializeSignalHandler();
        adapter_.callBase_ = true;
        adapter_.enableSignalHandlers();
        adapter_.callBase_ = false;
    }

    @Test
    public void initializeSignalHandler() {
        adapter_.callBase_ = true;
        adapter_.initializeSignalHandler();
        adapter_.callBase_ = false;
    }

    @Test
    public void initializeClient() {
        adapter_.callBase_ = true;
        adapter_.initializeClient();
        adapter_.callBase_ = false;
    }

    @Test
    public void createHouseKeepingCallbackNoRtrnValue() throws Exception {
        ProcedureCallback callback = adapter_.createHouseKeepingCallbackNoRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "MethodCalledBackFrom", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        callback.clientCallback(response_);
    }

    @Test
    public void createHouseKeepingCallbackLongRtrnValueNegative() throws Exception {
        ProcedureCallback callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ComputeNodeDiscovered", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);

        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ServiceNodeDiscovered", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response_);

        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ComputeNodeSaveIpAddr", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("not the same as the expected IP address");
        callback.clientCallback(response_);

        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ServiceNodeSaveIpAddr", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("not the same as the expected IP address");
        callback.clientCallback(response_);

        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ComputeNodeSetState", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);

        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ServiceNodeSetState", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response_);

        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ComputeNodeSaveBootImageInfo", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("");
        callback.clientCallback(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response_);
    }


    @Test
    public void createHouseKeepingCallbackLongRtrnValuePositive() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("ScalarLong", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(-1L);
            }}); // Row 0
        }});
        table.advanceRow();
        ProcedureCallback callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "RasEventStore", "Info",
                9999L);
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
        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "RasEventStore", "Info",
                9999L);
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
        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ComputeNodeDiscovered", "Info",
                9999L);
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
        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "ComputeNodeDiscovered", "Info",
                9999L);
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
        callback = adapter_.createHouseKeepingCallbackLongRtrnValue(adapter_.adapterType(),
                adapter_.adapterName(), "Unknown", "Info",
                9999L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        callback.clientCallback(response_);
    }

    @Test(expected = RuntimeException.class)
    public void setupAdapter() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        adapter_.registerAdapter("testhost");
    }

    @Test
    public void setupAdapter2() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Id", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(9999L);
            }}); // Row 0
        }});
        table.advanceRow();
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        adapter_.registerAdapter("testhost");
    }

    @Test(expected = RuntimeException.class)
    public void adapterTerminating() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE, ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("FAILED");
        adapter_.adapterTerminating("Results");
    }

    @Test(expected = RuntimeException.class)
    public void adapterTerminating2() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(response_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS,
                ClientResponse.OPERATIONAL_FAILURE, ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("FAILED");
        adapter_.adapterTerminating("Results");
    }

    @Test
    public void adapterTerminating3() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS, ClientResponse.SUCCESS);
        adapter_.adapterTerminating("Results");
    }

    @Test(expected = RuntimeException.class)
    public void abend() throws Exception {
        adapter_.throwRuntimeException_ = true;
        adapter_.abend("Reason");
    }

    @Test
    public void logRasEventWithEffectedJob() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.any(ProcedureCallback.class),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(true);
        adapter_.logRasEventWithEffectedJob("Type", "data", "Lctn", "JobId", 1000L,
                "RAS", 9999L);
    }

    @Test
    public void logRasEventCheckForEffectedJob() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.any(ProcedureCallback.class),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(true);
        adapter_.logRasEventCheckForEffectedJob("Type", "data", "Lctn", 1000L,
                "RAS", 9999L);
    }

    @Test
    public void addRasMetaDataIntoDataStore() throws Exception {
        VoltTable table = makeTable(new ArrayList<Pair<String, VoltType>>() {{
            add(new Pair<>("Id", VoltType.BIGINT));             // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<Object>() {{
                add(0L);
            }}); // Row 0
        }});
        table.advanceRow();
        when(client_.callProcedure(ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        adapter_.addRasMetaDataIntoDataStore();
    }

    @Test
    public void killJob() throws Exception {
        adapter_.killJob("Lctn", "RAS", 9998L, "Op", null, "RAS", 9999L);
    }

    @Test
    public void markNodeActive() throws Exception {
        adapter_.dontCallBase_ = true;
        adapter_.markNodeActive("Lctn", false, 1000L, "RAS", 9999L);
        adapter_.dontCallBase_ = false;
    }

    @Test
    public void markNodePoweredOff() throws Exception {
        adapter_.dontCallBase_ = true;
        adapter_.markNodePoweredOff("Lctn", 1000L, "kernel", 9999L);
        adapter_.dontCallBase_ = false;
    }

    MockAdapter adapter_;
    WorkQueue workQueue_;
    static File rasEventMetaData_ = new File("/tmp/RasEventMetaData.json");
    ClientStatusListenerExt clientStatus_;
    Client client_;
    ClientResponse response_;
}
