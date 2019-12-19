// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltDbBootImageTest {
    private class MockVoltDbBootImage extends VoltDbBootImage {
        MockVoltDbBootImage(String[] servers, String type) { super(servers, type, mock(Logger.class)); initialize(); }
        @Override protected Client getClient() { return client_; }
    }

    private Client client_;
    private ClientResponse response_;

    @Before
    public void setUp() {
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("ID", VoltType.STRING),
                new VoltTable.ColumnInfo("DESCRIPTION", VoltType.STRING),
                new VoltTable.ColumnInfo("BOOTIMAGEFILE", VoltType.STRING),
                new VoltTable.ColumnInfo("BOOTIMAGECHECKSUM", VoltType.STRING),
                new VoltTable.ColumnInfo("BOOTSTRAPIMAGE", VoltType.STRING),
                new VoltTable.ColumnInfo("BOOTSTRAPIMAGECHECKSUM", VoltType.STRING),
                new VoltTable.ColumnInfo("KERNELARGSS", VoltType.STRING));
        t.addRow("profile_1", "image1", "compute", "12312312342", "sles", "12123667654244", "");
        t.addRow("profile_2", "image2", "compute", "343663938332", "centos", "322588778766656", "");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void addBootImageProfile() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("description", "description");
        bootImg.put("vnfs", "vnfs");
        bootImg.put("vnfs_checksum", "vnfs_checksum");
        bootImg.put("bootoptions", "bootoptions");
        bootImg.put("bootstrapimage", "bootstrapimage");
        bootImg.put("bootstrapimage_checksum", "bootstrapimage_checksum");
        bootImg.put("kernelargs", "kernelargs");
        bootImg.put("filelists", "filelists");
        assertEquals (bootImage.addBootImageProfile(bootImg),
                "Succeeded in performing operation");
    }

    @Test
    public void editBootImageProfile() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetInfo"), anyString())).thenReturn(response_);
        when(client_.callProcedure(eq("BootImageUpdateInfo"), any())).thenReturn(response_);
        when(client_.callProcedure(eq("BOOTIMAGE_HISTORY.insert"), any())).thenReturn(response_);
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("description", "description");
        bootImg.put("vnfs", "vnfs");
        bootImg.put("vnfs_checksum", "vnfs_checksum");
        bootImg.put("bootoptions", "bootoptions");
        bootImg.put("bootstrapimage", "bootstrapimage");
        bootImg.put("bootstrapimage_checksum", "bootstrapimage_checksum");
        bootImg.put("kernelargs", "kernelargs");
        bootImg.put("filelists", "filelists");
        assertEquals (bootImage.editBootImageProfile(bootImg),
                "Succeeded in performing operation");
    }

    @Test
    public void copyBootImageProfile() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("target_id", "target_id");
        assertEquals (bootImage.addBootImageProfile(bootImg),
                "Succeeded in performing operation");
    }

    @Test
    public void deleteBootImageProfile() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageDeleteInfo"), anyString())).thenReturn(response_);
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        assertEquals(bootImage.deleteBootImageProfile("profile_1"),
        "Succeeded in deleting bootimage");
    }

    @Test
    public void printBootImageProfile() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetInfo"), anyString())).thenReturn(response_);
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> result = bootImage.retrieveBootImageProfile("profile_1");
        assertNotNull(result);
    }

    @Test
    public void listBootImageProfiles() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetIds"))).thenReturn(response_);
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        List<String> result = new ArrayList<>();
        result.add("profile_1");
        result.add("profile_2");
        assertEquals(bootImage.listBootImageProfiles(), result);
    }

    @Test(expected = DataStoreException.class)
    public void addBootImageProfileNegative() throws IOException, ProcCallException, DataStoreException{
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("failed");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("description", "description");
        bootImg.put("vnfs", "vnfs");
        bootImg.put("vnfs_checksum", "vnfs_checksum");
        bootImg.put("bootoptions", "bootoptions");
        bootImg.put("bootstrapimage", "bootstrapimage");
        bootImg.put("bootstrapimage_checksum", "bootstrapimage_checksum");
        bootImg.put("kernelargs", "kernelargs");
        bootImg.put("filelists", "filelists");
        bootImage.addBootImageProfile(bootImg);
    }


    @Test(expected = DataStoreException.class)
    public void editBootImageProfileNegative() throws IOException, ProcCallException, DataStoreException{
        when(client_.callProcedure(eq("BootImageGetInfo"), anyString())).thenReturn(response_);
        when(client_.callProcedure(eq("BootImageUpdateInfo"), any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS, ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("failed");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("description", "description");
        bootImg.put("vnfs", "vnfs");
        bootImg.put("vnfs_checksum", "vnfs_checksum");
        bootImg.put("bootoptions", "bootoptions");
        bootImg.put("bootstrapimage", "bootstrapimage");
        bootImg.put("bootstrapimage_checksum", "bootstrapimage_checksum");
        bootImg.put("kernelargs", "kernelargs");
        bootImg.put("filelists", "filelists");
        bootImage.editBootImageProfile(bootImg);
    }

    @Test(expected = DataStoreException.class)
    public void copyEditBootImageProfileNegative() throws IOException, ProcCallException, DataStoreException{
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("failed");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("target_id", "target_id");
        bootImage.addBootImageProfile(bootImg);
    }

    @Test(expected = DataStoreException.class)
    public void deleteBootImageProfileNegative() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageDeleteInfo"), anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("failed");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        bootImage.deleteBootImageProfile("profile_1");
    }

    @Test(expected = DataStoreException.class)
    public void printBootImageProfileNegative() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetInfo"), anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("failed");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> result = bootImage.retrieveBootImageProfile("profile_1");
    }

    @Test(expected = DataStoreException.class)
    public void listBootImageProfilesNegative() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetIds"))).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getStatusString()).thenReturn("failed");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        List<String> result = bootImage.listBootImageProfiles();
    }

    @Test(expected = DataStoreException.class)
    public void addBootImageProfileException() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenThrow(new IOException());
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("description", "description");
        bootImg.put("vnfs", "vnfs");
        bootImg.put("vnfs_checksum", "vnfs_checksum");
        bootImg.put("bootoptions", "bootoptions");
        bootImg.put("bootstrapimage", "bootstrapimage");
        bootImg.put("bootstrapimage_checksum", "bootstrapimage_checksum");
        bootImg.put("kernelargs", "kernelargs");
        bootImg.put("filelists", "filelists");
        bootImage.addBootImageProfile(bootImg);
    }

    @Test(expected = DataStoreException.class)
    public void editBootImageProfileException() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetInfo"), anyString())).thenReturn(response_);
        when(client_.callProcedure(eq("BootImageUpdateInfo"), any())).thenThrow(new IOException());
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("description", "description");
        bootImg.put("vnfs", "vnfs");
        bootImg.put("vnfs_checksum", "vnfs_checksum");
        bootImg.put("bootoptions", "bootoptions");
        bootImg.put("bootstrapimage", "bootstrapimage");
        bootImg.put("bootstrapimage_checksum", "bootstrapimage_checksum");
        bootImg.put("kernelargs", "kernelargs");
        bootImg.put("filelists", "filelists");
        bootImage.editBootImageProfile(bootImg);
    }

    @Test(expected = DataStoreException.class)
    public void copyBootImageProfileException() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenThrow(new IOException());
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "id");
        bootImg.put("target_id", "target_id");
        bootImage.addBootImageProfile(bootImg);
    }

    @Test(expected = DataStoreException.class)
    public void deleteBootImageProfileException() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageDeleteInfo"), anyString())).thenThrow(new IOException());
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        bootImage.deleteBootImageProfile("profile_1");
    }

    @Test(expected = DataStoreException.class)
    public void printBootImageProfileException() throws IOException, ProcCallException, DataStoreException{
        when(client_.callProcedure(eq("BootImageGetInfo"), anyString())).thenThrow(new IOException());
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        bootImage.retrieveBootImageProfile("profile_1").toString();
    }

    @Test(expected = DataStoreException.class)
    public void listBootImageProfilesException() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(eq("BootImageGetIds"))).thenThrow(new IOException());
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        bootImage.listBootImageProfiles().toString();
    }

    @Test
    public void getComputeNodesBootImageId() throws IOException, ProcCallException {

        when(client_.callProcedure(eq("ComputeNodeGetBootImageId"),
                anyString())).thenReturn(response_);

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("BOOTIMAGEID", VoltType.STRING));
        t.addRow("image_1");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        Set<String> computeNodes = new HashSet<>();
        computeNodes.add("test_node_1");
        computeNodes.add("test_node_2");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        assertEquals("{test_node_1=image_1, test_node_2=image_1}",
                bootImage.getComputeNodesBootImageId(computeNodes).toString());
    }

    @Test
    public void getComputeNodesBootImageIdDeviceNotInComputeNodeTable() throws IOException, ProcCallException {

        when(client_.callProcedure(eq("ComputeNodeGetBootImageId"),
                anyString())).thenReturn(response_);

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("BOOTIMAGEID", VoltType.STRING));
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        Set<String> computeNodes = new HashSet<>();
        computeNodes.add("test_node_1");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        assertEquals("{test_node_1=Device doesn't exist}",
                bootImage.getComputeNodesBootImageId(computeNodes).toString());
    }

    @Test
    public void getComputeNodesBootImageIdVoltDbException() throws IOException, ProcCallException {

        when(client_.callProcedure(eq("ComputeNodeGetBootImageId"),
                anyString())).thenThrow(new IOException());

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("BOOTIMAGEID", VoltType.STRING));
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        Set<String> computeNodes = new HashSet<>();
        computeNodes.add("test_node_1");
        VoltDbBootImage bootImage = new MockVoltDbBootImage(null, "test");
        assertEquals("{test_node_1=An exception occurred while retrieving bootimage Id}",
                bootImage.getComputeNodesBootImageId(computeNodes).toString());
    }
}
