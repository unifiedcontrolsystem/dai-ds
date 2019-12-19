// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;


import com.intel.dai.AdapterInformation;
import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.*;
import org.mockito.ArgumentMatchers;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VoltDbWorkQueueTest {

    class MockVoltDbWorkQueue extends VoltDbWorkQueue {

        MockVoltDbWorkQueue(String[] servers, IAdapter adapter) throws DataStoreException {
            super(servers, adapter, mock(VoltDbRasEventLog.class), mock(Logger.class));
            voltClient = client_;
            logger = mock(Logger.class);
            initialize();
        }
    }

    class Pair<F, S> {
        Pair(F arg1, S arg2) {
            first = arg1;
            second = arg2;
        }

        F first;
        S second;
    }


    private String data_;
    private static File rasEventMetaData_ = new File("/tmp/RasEventMetaData.json");
    Client client_;
    ClientResponse response_;
    private VoltDbWorkQueue workQueue_;
    private IAdapter mockAdapter_;
    String[] servers;

    @Before
    public void setUp() throws Exception {
        data_ = "$ ls /tmp\n";
        data_ += "AdapterMonitor.json          +~JF3063571134902629166.tmp  kotlin-idea-1286715959362651557-is-running\n";
        data_ += "config-err-t8vgYo            +~JF3509249003006038163.tmp  qt_temp.TJ9987\n";
        data_ += "gnome-software-5S7ZKZ/       +~JF3568074949108489513.tmp  qt_temp.XM9987\n";
        data_ += "gnome-software-BV73KZ/       +~JF4124368963987919597.tmp  systemd-private-f8afce3018bc419896ae173af47b61fa-colord.service-Bn84fe/\n";
        data_ += "gnome-software-C272KZ/       +~JF4658857290627716297.tmp  systemd-private-f8afce3018bc419896ae173af47b61fa-fwupd.service-GeI7dV/\n";
        data_ += "gnome-software-WKA4KZ/       +~JF4832798642101703280.tmp  systemd-private-f8afce3018bc419896ae173af47b61fa-rtkit-daemon.service-n8XeA2/\n";
        data_ += "hsperfdata_pdamons/          +~JF5456389088828079819.tmp  systemd-private-f8afce3018bc419896ae173af47b61fa-systemd-timesyncd.service-0Y16Bk/\n";
        data_ += "ijinit1.gradle               +~JF6393155873922674790.tmp  unity_support_test.0\n";
        data_ += "ijinit.gradle                +~JF7146357706227951940.tmp  vboxdrv-Module.symvers\n";
        data_ += "ijtestinit.gradle            +~JF7330417402075521911.tmp\n";
        data_ += "+~JF2360217825858465920.tmp  +~JF9147760958600505132.tmp\n";
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Id", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add(9999L);
            }}); // Row 0
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(client_.callProcedure(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS, ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        servers = new String[] {"localhost"};
        mockAdapter_ = mock(IAdapter.class);
        when(mockAdapter_.adapterId()).thenReturn(0L);
        when(mockAdapter_.adapterName()).thenReturn("test1_" + 0);
        when(mockAdapter_.adapterType()).thenReturn("test1");
        when(mockAdapter_.pid()).thenReturn(1000L);
        when(mockAdapter_.snLctn()).thenReturn("myself");
        workQueue_ = new MockVoltDbWorkQueue(servers, mockAdapter_);
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

    private VoltTable makeTable(Collection<VoltDbWorkQueueTest.Pair<String, VoltType>> columns,
                                Collection<? extends Collection<?>> data) {
        ArrayList<VoltTable.ColumnInfo> columnsList = new ArrayList<>();
        for (VoltDbWorkQueueTest.Pair<String, VoltType> pair : columns) {
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

    @Test(expected = NullPointerException.class)
    public void ctor1() throws Exception {
        VoltDbClient.voltClient = mock(Client.class);
        new VoltDbWorkQueue(mock(Client.class), servers, mockAdapter_, mock(Logger.class)).initialize();
    }

    @Test(expected = NullPointerException.class)
    public void ctor2() throws Exception {
        VoltDbClient.voltClient = mock(Client.class);
        new VoltDbWorkQueue(servers, mockAdapter_, mock(Logger.class)).initialize();
    }

    @Test(expected = NullPointerException.class)
    public void ctor3() {
        AdapterInformation info = new AdapterInformation("test1", "test1", "myself", "myName", 0L);
        info.signalToShutdown();
        VoltDbClient.voltClient = mock(Client.class);
        new VoltDbWorkQueue(servers, info, mock(Logger.class)).initialize();
    }

    @Test
    public void compressAndDecompress() {
        String compressed = VoltDbWorkQueue.compressResult(data_);
        assertTrue(compressed.length() < data_.length());
        String output = VoltDbWorkQueue.decompressResult(compressed);
        assertEquals(data_, output);
    }

    @Test
    public void compressAndDecompressTruncated() {
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        data_ += data_;
        String compressed = VoltDbWorkQueue.compressResult(data_);
        assertTrue(compressed.length() < data_.length());
        String output = VoltDbWorkQueue.decompressResult(compressed);
        assertNotEquals(data_, output);
        assertTrue(output.endsWith("\n\n*** Results have been truncated!"));
    }


    @Test(expected = RuntimeException.class)
    public void setupAdaptersBaseWorkItem() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        workQueue_.setupAdaptersBaseWorkItem();
    }

    @Test(expected = RuntimeException.class)
    public void setupAdaptersBaseWorkItem2() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Id", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add(9999L);
            }}); // Row 0
        }});
        table.advanceRow();
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS, ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        workQueue_.setupAdaptersBaseWorkItem();
    }

    @Test
    public void setupAdaptersBaseWorkItem3() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Id", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add(9999L);
            }}); // Row 0
        }});
        table.advanceRow();
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS, ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        workQueue_.setupAdaptersBaseWorkItem();
    }

    @Test(expected = RuntimeException.class)
    public void grabNextAvailWorkItem() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        workQueue_.grabNextAvailWorkItem();
    }

    @Test
    public void grabNextAvailWorkItem2() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>()); // No rows
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        assertFalse(workQueue_.grabNextAvailWorkItem());
    }

    @Test
    public void grabNextAvailWorkItem3() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Id", VoltType.BIGINT));             // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));          // Column 1
            add(new VoltDbWorkQueueTest.Pair<>("WorkingResults", VoltType.STRING)); // Column 2
            add(new VoltDbWorkQueueTest.Pair<>("WorkToBeDone", VoltType.STRING));   // Column 3
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add(9999L);
                add("Q"); // State
                add("Results"); // Results
                add("SomeWork"); // Results
            }}); // Row 0
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        assertTrue(workQueue_.grabNextAvailWorkItem());
    }

    @Test
    public void handleProcessingWhenUnexpectedWorkItem() throws Exception {
        workQueue_.handleProcessingWhenUnexpectedWorkItem();
    }

    @Test
    public void baseWorkItemId() {
        workQueue_.baseWorkItemId();
    }

    @Test
    public void isThisNewWorkItem() {
        workQueue_.isThisNewWorkItem();
    }

    @Test
    public void getClientParameters() {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Parameters", VoltType.STRING));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add("A,B,C");
            }}); // Row 0
        }});
        workQueue_.workItemResponse = response_;
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        table.advanceRow();
        assertEquals(3, workQueue_.getClientParameters(",").length);
    }

    @Test
    public void getClientParameters1() {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Parameters", VoltType.STRING));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add("Key1#Value1$Key2#Value2$Key3#Value3");
            }}); // Row 0
        }});
        workQueue_.workItemResponse = response_;
        when(response_.getResults()).thenReturn(new VoltTable[]{table});
        table.advanceRow();
        workQueue_.getClientParameters();
    }

    @Test(expected = RuntimeException.class)
    public void queueWorkItem() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        workQueue_.queueWorkItem("RAS", "Queue", "WorkWantDone",
                "Params1",false, "RAS", 9999L);
    }

    @Test
    public void queueWorkItem2() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("Id", VoltType.BIGINT));         // Column 0
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add(9999L);
            }}); // Row 0
        }});
        table.advanceRow();
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        long id = workQueue_.queueWorkItem("RAS", "Queue", "WorkWantDone",
                "Params1",true, "RAS", 9999L);
        assertEquals(9999L, id);
    }

    @Test
    public void queueWorkItem1() {
    }

    @Test(expected = RuntimeException.class)
    public void finishedWorkItem() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("FAILED");
        workQueue_.finishedWorkItem("CMD", 9999L, "Results");
    }

    @Test
    public void finishedWorkItem2() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        workQueue_.finishedWorkItem("CMD", 9999L, "Results");
    }

    @Test(expected = RuntimeException.class)
    public void finishedWorkItemDueToError() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("FAILED");
        workQueue_.finishedWorkItemDueToError("CMD", 9999L, "Results");
    }

    @Test
    public void finishedWorkItemDueToError2() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        workQueue_.finishedWorkItemDueToError("CMD", 9999L, "Results");
    }

    @Test
    public void workItemId() {
        workQueue_.workItemId();
    }


    @Test(expected = RuntimeException.class)
    public void waitForWorkItemToFinishAndMarkDone() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        workQueue_.waitForWorkItemToFinishAndMarkDone("CMD", "RAS", 9999L,
                "RAS",9998L);
    }

    @Test
    public void waitForWorkItemToFinishAndMarkDone2() throws Exception {
        VoltTable table1 = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>()); // No rows
        VoltTable table2 = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add("F"); // State
                add(VoltDbWorkQueue.compressResult(data_)); // Results
            }}); // Row 0
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table1 }, new VoltTable[] { table2 });
        workQueue_.waitForWorkItemToFinishAndMarkDone("CMD", "RAS", 9999L,
                "RAS",9998L);
    }

    @Test
    public void waitForWorkItemToFinishAndMarkDone3() throws Exception {
        VoltTable table1 = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>()); // No rows
        VoltTable table2 = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add("E"); // State
                add(VoltDbWorkQueue.compressResult(data_)); // Results
            }}); // Row 0
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table1 }, new VoltTable[] { table2 });
        workQueue_.waitForWorkItemToFinishAndMarkDone("CMD", "RAS", 9999L,
                "RAS",9998L);
    }

    @Test
    public void workingResults() {
        workQueue_.workingResults();
    }

    @Test
    public void requeueAnyZombieWorkItems() {
    }

    @Test
    public void saveWorkItemsRestartData() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyByte(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        assertEquals(0, workQueue_.saveWorkItemsRestartData(9999L, "Restart Data"));
        assertEquals(0, workQueue_.saveWorkItemsRestartData(9999L, "Restart Data", true));
    }

    @Test
    public void saveWorkItemsRestartData1() {
    }

    @Test
    public void saveWorkItemsRestartData2() {
    }

    @Test
    public void wasWorkDone() {
        workQueue_.wasWorkDone();
    }

    @Test(expected = RuntimeException.class)
    public void getWorkItemStatus() throws Exception {
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        workQueue_.getWorkItemStatus("RAS", 9999L);
    }

    @Test
    public void getWorkItemStatus2() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>()); // No rows
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        workQueue_.getWorkItemStatus("RAS", 9999L);
    }

    @Test
    public void getWorkItemStatus3() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add("F"); // State
                add(VoltDbWorkQueue.compressResult(data_)); // Results
            }}); // Row 0
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        workQueue_.getWorkItemStatus("RAS", 9999L);
    }

    @Test
    public void getWorkItemStatus4() throws Exception {
        VoltTable table = makeTable(new ArrayList<>() {{
            add(new VoltDbWorkQueueTest.Pair<>("State", VoltType.STRING));         // Column 0
            add(new VoltDbWorkQueueTest.Pair<>("Results", VoltType.STRING));         // Column 1
        }}, new ArrayList<ArrayList<Object>>() {{
            add(new ArrayList<>() {{
                add("E"); // State
                add(VoltDbWorkQueue.compressResult(data_)); // Results
            }}); // Row 0
        }});
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        workQueue_.getWorkItemStatus("RAS", 9999L);
    }

    @Test
    public void markWorkItemDone() {
    }
}
