package com.intel.dai.eventsim;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class InventoryApiTest extends InventoryApi {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void loadHardwareInventory() throws Exception {
        final File invConfigFile = tempFolder.newFile("InventoryNodeProcessorMemory.json");
        loadDataIntoFile(invConfigFile, hwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INVENTORY_CONFIG = invConfigFile.getAbsolutePath();
        PropertyArray result = hwInvApiTest.getHwInventory();
        assertEquals("node id", result.getMap(0).getArray("Nodes").getMap(0).get("ID"));
    }

    @Test(expected = RuntimeException.class)
    public void errorOnLoadNoHardwareInventory() throws Exception {
        final File invConfigFile = tempFolder.newFile("InventoryNodeProcessorMemory.json");
        loadDataIntoFile(invConfigFile, hwInvInvalidData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INVENTORY_CONFIG = invConfigFile.getAbsolutePath();
        hwInvApiTest.getHwInventory();
    }

    @Test(expected = RuntimeException.class)
    public void emptyHardwareInventoryData() throws Exception {
        final File invConfigFile = tempFolder.newFile("InventoryNodeProcessorMemory.json");
        loadDataIntoFile(invConfigFile, emptyHwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INVENTORY_CONFIG = invConfigFile.getAbsolutePath();
        hwInvApiTest.getHwInventory();
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.write(file, data, (String)null);
    }

    @Test
    public void loadHardwareInventoryForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("test_x00n1.json");
        loadDataIntoFile(invConfigFile, hwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INV_LOCATION_CONFIG = invConfigFile.getParent() + "/";
        PropertyArray result = hwInvApiTest.getInventoryHardwareForLocation("test_x00n1");
        assertEquals("node id", result.getMap(0).getArray("Nodes").getMap(0).get("ID"));
    }

    @Test(expected = RuntimeException.class)
    public void errorOnLoadNoHardwareInventoryForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("test_x00n1.json");
        loadDataIntoFile(invConfigFile, hwInvInvalidData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INV_LOCATION_CONFIG = invConfigFile.getParent() + "/";
        hwInvApiTest.getInventoryHardwareForLocation("test_x00n1");
    }

    @Test(expected = RuntimeException.class)
    public void emptyHardwareInventoryDataForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("test_x00n1.json");
        loadDataIntoFile(invConfigFile, emptyHwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INV_LOCATION_CONFIG = invConfigFile.getParent() + "/";
        hwInvApiTest.getInventoryHardwareForLocation("test_x00n1");
    }

    @Test(expected = RuntimeException.class)
    public void NoFileHardwareInventoryDataForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("test_x00n1.json");
        loadDataIntoFile(invConfigFile, emptyHwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.getInventoryHardwareForLocation("test_x00n1");
    }

    @Test
    public void loadHardwareInventoryQueryForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("HwInvQuery_test_x00n1.json");
        loadDataIntoFile(invConfigFile, hwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INV_QUERY_LOCATION_CONFIG = invConfigFile.getParent() + "/HwInvQuery_";
        PropertyArray result = hwInvApiTest.getInventoryHardwareQueryForLocation("test_x00n1");
        assertEquals("node id", result.getMap(0).getArray("Nodes").getMap(0).get("ID"));
    }

    @Test(expected = RuntimeException.class)
    public void errorOnLoadNoHardwareInventoryQueryForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("HwInvQuery_test_x00n1.json");
        loadDataIntoFile(invConfigFile, hwInvInvalidData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INV_QUERY_LOCATION_CONFIG = invConfigFile.getParent() + "/HwInvQuery_";
        hwInvApiTest.getInventoryHardwareQueryForLocation("test_x00n1");
    }

    @Test(expected = RuntimeException.class)
    public void emptyHardwareInventoryDataQueryForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("HwInvQuery_test_x00n1.json");
        loadDataIntoFile(invConfigFile, emptyHwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.HW_INV_QUERY_LOCATION_CONFIG = invConfigFile.getParent() + "/HwInvQuery_";
        hwInvApiTest.getInventoryHardwareQueryForLocation("test_x00n1");
    }

    @Test(expected = RuntimeException.class)
    public void NoFileHardwareInventoryDataQueryForLocation() throws Exception {
        final File invConfigFile = tempFolder.newFile("HwInvQuery_test_x00n1.json");
        loadDataIntoFile(invConfigFile, emptyHwInvData);
        InventoryApiTest hwInvApiTest = new InventoryApiTest();
        hwInvApiTest.getInventoryHardwareQueryForLocation("test_x00n1");
    }

    private String hwInvData = "[{\n" +
            "  \"Nodes\": [\n" +
            "    {\n" +
            "      \"ID\": \"node id\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Processors\": [\n" +
            "    {\n" +
            "      \"ID\": \"processor node id\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"ID\": \"processor node id\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Memory\": [\n" +
            "    {\n" +
            "      \"ID\": \"memory node id\"\n" +
            "    }\n" +
            "  ]\n" +
            "}]";

    private String hwInvInvalidData = "[{\n" +
            "  \"Nodes\": [\n" +
            "    {\n" +
            "      \"ID\": \"node id\"\n" +
            "}]";

    private String emptyHwInvData = "[]";

    private String hwInvDataForLocation = "{\n" +
            "\"XName\": \"x0c0s0b0n0\",\n" +
            "\"Format\": \"NestNodesOnly\"\n" +
            "}";
}
