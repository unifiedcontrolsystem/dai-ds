package com.intel.dai.eventsim;

import com.intel.properties.PropertyMap;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class BootParamApiTest extends BootParamApi {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void loadBootParam() throws Exception {
        final File bootConfigFile = tempFolder.newFile("BootParameters.json");
        loadDataIntoFile(bootConfigFile, bootConfigData);
        BootParamApiTest bootParamApiTest = new BootParamApiTest();
        bootParamApiTest.BOOT_PARAM_CONFIG = bootConfigFile.getAbsolutePath();
        PropertyMap result = bootParamApiTest.getBootParametrs();
        assertEquals("boot id", result.getArray("content").getMap(0).getString("id"));
    }

    @Test
    public void loadInvalidBootConfig() throws Exception {
        final File bootConfigFile = tempFolder.newFile("BootParameters.json");
        loadDataIntoFile(bootConfigFile, invalidBootConfigData);
        BootParamApiTest bootParamApiTest = new BootParamApiTest();
        bootParamApiTest.BOOT_PARAM_CONFIG = bootConfigFile.getAbsolutePath();
        try {
            bootParamApiTest.getBootParametrs();
        }catch (RuntimeException e) {
            assertEquals("Error in loading boot parameters data.", e.getMessage());
        }
    }

    @Test
    public void noBootImageData() throws Exception {
        final File bootConfigFile = tempFolder.newFile("BootParameters.json");
        loadDataIntoFile(bootConfigFile, badBootConfigData);
        BootParamApiTest bootParamApiTest = new BootParamApiTest();
        bootParamApiTest.BOOT_PARAM_CONFIG = bootConfigFile.getAbsolutePath();
        try {
            bootParamApiTest.getBootParametrs();
        }catch (RuntimeException e) {
            assertEquals("No boot-images data.", e.getMessage());
        }
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.write(file, data, (String)null);
    }

    private String bootConfigData = "{\n" +
            "  \"boot-images\": {\n" +
            "    \"content\": [\n" +
            "      {\n" +
            "        \"id\": \"boot id\",\n" +
            "        \"description\": \"boot image description\",\n" +
            "        \"BootImageFile\": \"boot image file\",\n" +
            "        \"BootImageChecksum\": \"boot image checksum\",\n" +
            "        \"BootOptions\": null,\n" +
            "        \"KernelArgs\": null,\n" +
            "        \"BootStrapImageFile\": \"boot strap image file\",\n" +
            "        \"BootStrapImageChecksum\": \"boot strap image checksum\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}\n";

    private String badBootConfigData = "{\n" +
            "}\n";

    private String invalidBootConfigData = "{\n" +
            "{}\n";
}
