package com.intel.dai.eventsim;

import com.intel.properties.PropertyArray;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class HardwareInventoryTest extends HardwareInventory {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void validHardwareInventory() throws Exception {
        final File hwInvConfigFile = tempFolder.newFile("InventoryNodeProcessorMemory.json");
        loadDataIntoFile(hwInvConfigFile, validHwInvData);
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        hardwareInventoryTest.setInventoryHardwareConfigLocation(hwInvConfigFile.getAbsolutePath());
        PropertyArray result = (PropertyArray) hardwareInventoryTest.getHwInventory();
        assertEquals("node-id", result.getMap(0).getArray("Nodes").getMap(0).get("ID"));
    }

    @Test
    public void emptyLocationHwInventoryConfigFile() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareConfigLocation("");
            hardwareInventoryTest.getHwInventory();
        } catch (SimulatorException e) {
            assertEquals("Invalid or null hardware inventory config file", e.getMessage());
        }
    }

    @Test
    public void nullLocationHwInventoryConfigFile() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareConfigLocation(null);
            hardwareInventoryTest.getHwInventory();
        } catch (SimulatorException e) {
            assertEquals("Invalid or null hardware inventory config file", e.getMessage());
        }
    }

    @Test
    public void hwInventoryConfigFileNotExists() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareConfigLocation("Test.json");
            hardwareInventoryTest.getHwInventory();
        } catch (SimulatorException e) {
            assertEquals("Given hardware inventory data file doesn't exist: Test.json", e.getMessage());
        }
    }

    @Test
    public void invalidJsonHwInventoryConfigData() throws Exception {
        final File hwInvConfigFile = tempFolder.newFile("InventoryNodeProcessorMemory.json");
        loadDataIntoFile(hwInvConfigFile, inValidJsonHwInvData);
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        hardwareInventoryTest.setInventoryHardwareConfigLocation(hwInvConfigFile.getAbsolutePath());
        try {
            hardwareInventoryTest.getHwInventory();
        } catch (SimulatorException e) {
            assertEquals("Error while loading hardware inventory data", e.getMessage());
        }
    }

    @Test
    public void validHardwareInventory_ForLocation() throws Exception {
        final File hwInvConfigFileForLocation = tempFolder.newFile("x0c0s0b0n0.json");
        loadDataIntoFile(hwInvConfigFileForLocation, validHwInvData);
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        hardwareInventoryTest.setInventoryHardwareConfigPath(hwInvConfigFileForLocation.getParent());
        PropertyArray result = (PropertyArray) hardwareInventoryTest.getInventoryHardwareForLocation("x0c0s0b0n0");
        assertEquals("node-id", result.getMap(0).getArray("Nodes").getMap(0).get("ID"));
    }

    @Test
    public void emptyLocationHwInventoryConfigFile_ForLocation() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareConfigPath("");
            hardwareInventoryTest.getInventoryHardwareForLocation("");
        } catch (SimulatorException e) {
            assertEquals("Invalid or null hardware inventory config path", e.getMessage());
        }
    }

    @Test
    public void nullLocationHwInventoryConfigFile_ForLocation() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareConfigPath(null);
            hardwareInventoryTest.getInventoryHardwareForLocation(null);
        } catch (SimulatorException e) {
            assertEquals("Invalid or null hardware inventory config path", e.getMessage());
        }
    }

    @Test
    public void hwInventoryConfigFileNotExists_ForLocation() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareConfigPath("/test");
            hardwareInventoryTest.getInventoryHardwareForLocation("x0c0s0b0n0");
        } catch (SimulatorException e) {
            assertEquals("Given hardware inventory data file for a location doesn't exist: /test/x0c0s0b0n0.json", e.getMessage());
        }
    }

    @Test
    public void invalidJsonHwInventoryConfigData_ForLocation() throws Exception {
        final File hwInvConfigFileForLocation = tempFolder.newFile("x0c0s0b0n0.json");
        loadDataIntoFile(hwInvConfigFileForLocation, inValidJsonHwInvData);
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        hardwareInventoryTest.setInventoryHardwareConfigPath(hwInvConfigFileForLocation.getParent());
        try {
            hardwareInventoryTest.getInventoryHardwareForLocation("x0c0s0b0n0");
        } catch (SimulatorException e) {
            assertEquals("Error while loading hardware inventory data for a location", e.getMessage());
        }
    }

    @Test
    public void validHardwareInventoryQuery() throws Exception {
        final File hwInvQueryConfigFile = tempFolder.newFile("x0c0s0b0n0.json");
        loadDataIntoFile(hwInvQueryConfigFile, validHwInvData);
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        hardwareInventoryTest.setInventoryHardwareQueryPath(hwInvQueryConfigFile.getParent());
        PropertyArray result = (PropertyArray) hardwareInventoryTest.getInventoryHardwareQueryForLocation("x0c0s0b0n0");
        assertEquals("node-id", result.getMap(0).getArray("Nodes").getMap(0).get("ID"));
    }

    @Test
    public void emptyLocationHwInventoryQueryConfigFile() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareQueryPath("");
            hardwareInventoryTest.getInventoryHardwareQueryForLocation("");
        } catch (SimulatorException e) {
            assertEquals("Invalid or null hardware inventory query path", e.getMessage());
        }
    }

    @Test
    public void nullLocationHwInventoryQueryConfigFile() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareQueryPath(null);
            hardwareInventoryTest.getInventoryHardwareQueryForLocation(null);
        } catch (SimulatorException e) {
            assertEquals("Invalid or null hardware inventory query path", e.getMessage());
        }
    }

    @Test
    public void hwInventoryQueryConfigFileNotExists() {
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        try {
            hardwareInventoryTest.setInventoryHardwareQueryPath("/test");
            hardwareInventoryTest.getInventoryHardwareQueryForLocation("x0c0s0b0n0");
        } catch (SimulatorException e) {
            assertEquals("Given hardware inventory query data file for a location doesn't exist: /test/x0c0s0b0n0.json", e.getMessage());
        }
    }

    @Test
    public void invalidJsonHwInventoryQueryConfigData() throws Exception {
        final File hwInvQueryConfigFile = tempFolder.newFile("x0c0s0b0n0.json");
        loadDataIntoFile(hwInvQueryConfigFile, inValidJsonHwInvData);
        HardwareInventoryTest hardwareInventoryTest = new HardwareInventoryTest();
        hardwareInventoryTest.setInventoryHardwareQueryPath(hwInvQueryConfigFile.getParent());
        try {
            hardwareInventoryTest.getInventoryHardwareQueryForLocation("x0c0s0b0n0");
        } catch (SimulatorException e) {
            assertEquals("Error while loading hardware inventory query data for a location", e.getMessage());
        }
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.write(file, data, (String)null);
    }

    private String validHwInvData = "[{\n" +
            "  \"Nodes\": [\n" +
            "    {\n" +
            "      \"ID\": \"node-id\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Processors\": [\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-2\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Memory\": [\n" +
            "    {\n" +
            "      \"ID\": \"memory-node-id\"\n" +
            "    }\n" +
            "  ]\n" +
            "}]";

    private String inValidJsonHwInvData = "[{\n" +
            "  \"Nodes\": [\n" +
            "    \n" + // add '{' before '\n' makes valid json
            "      \"ID\": \"node-id\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Processors\": [\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-2\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Memory\": [\n" +
            "    {\n" +
            "      \"ID\": \"memory-node-id\"\n" +
            "    }\n" +
            "  ]\n" +
            "}]";
}
