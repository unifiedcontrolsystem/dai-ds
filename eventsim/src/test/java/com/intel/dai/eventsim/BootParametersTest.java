package com.intel.dai.eventsim;

import com.intel.properties.PropertyMap;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class BootParametersTest extends BootParameters {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void validBootParameters() throws Exception {
        final File bootConfigFile = tempFolder.newFile("BootParameters.json");
        loadDataIntoFile(bootConfigFile, validBootData);
        BootParametersTest bootParametersTest = new BootParametersTest();
        bootParametersTest.setBootParamsConfigFile(bootConfigFile.getAbsolutePath());
        PropertyMap result = (PropertyMap) bootParametersTest.getBootParameters();
        assertEquals("boot-image-id", result.getArray("content").getMap(0).getString("id"));
    }

    @Test
    public void emptyLocationBootParametersConfigFile() {
        BootParametersTest bootParametersTest = new BootParametersTest();
        try {
            bootParametersTest.setBootParamsConfigFile("");
            bootParametersTest.getBootParameters();
        } catch (SimulatorException e) {
            assertEquals("Invalid or null boot parameters config file.", e.getMessage());
        }
    }

    @Test
    public void nullLocationBootParametersConfigFile() {
        BootParametersTest bootParametersTest = new BootParametersTest();
        try {
            bootParametersTest.setBootParamsConfigFile(null);
            bootParametersTest.getBootParameters();
        } catch (SimulatorException e) {
            assertEquals("Invalid or null boot parameters config file.", e.getMessage());
        }
    }

    @Test
    public void bootParametersConfigFileNotExists() {
        BootParametersTest bootParametersTest = new BootParametersTest();
        try {
            bootParametersTest.setBootParamsConfigFile("Test.json");
            bootParametersTest.getBootParameters();
        } catch (SimulatorException e) {
            assertEquals("Given boot parameters config file doesn't exists : Test.json", e.getMessage());
        }
    }

    @Test
    public void invalidJsonBootParametersConfigData() throws Exception {
        final File bootConfigFile = tempFolder.newFile("BootParameters.json");
        loadDataIntoFile(bootConfigFile, inValidJsonBootData);
        BootParametersTest bootParametersTest = new BootParametersTest();
        bootParametersTest.setBootParamsConfigFile(bootConfigFile.getAbsolutePath());
        try {
            bootParametersTest.getBootParameters();
        } catch (SimulatorException e) {
            assertEquals("Error in loading boot parameters data.", e.getMessage());
        }
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.write(file, data, (String)null);
    }

    private String validBootData = "{\n" +
            "  \"boot-images\": {\n" +
            "    \"content\": [\n" +
            "      {\n" +
            "        \"id\": \"boot-image-id\",\n" +
            "        \"description\": \"boot-image-description\",\n" +
            "        \"BootImageFile\": \"boot-image-file\",\n" +
            "        \"BootImageChecksum\": \"boot-image-checksum\",\n" +
            "        \"BootOptions\": null,\n" +
            "        \"KernelArgs\": null,\n" +
            "        \"BootStrapImageFile\": \"boot-strap-image-file\",\n" +
            "        \"BootStrapImageChecksum\": \"boot-strap-image-checksum\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}\n";

    private String inValidJsonBootData = "{\n" +
            "  \"boot-images\": {\n" +
            "    \"content\": [\n" +
            "      \n" + // add '{' before '\n' makes valid json
            "        \"id\": \"boot-image-id\",\n" +
            "        \"description\": \"boot-image-description\",\n" +
            "        \"BootImageFile\": \"boot-image-file\",\n" +
            "        \"BootImageChecksum\": \"boot-image-checksum\",\n" +
            "        \"BootOptions\": null,\n" +
            "        \"KernelArgs\": null,\n" +
            "        \"BootStrapImageFile\": \"boot-strap-image-file\",\n" +
            "        \"BootStrapImageChecksum\": \"boot-strap-image-checksum\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}\n";
}
