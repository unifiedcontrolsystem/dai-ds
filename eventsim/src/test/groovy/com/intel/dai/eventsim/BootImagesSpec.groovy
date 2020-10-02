package com.intel.dai.eventsim


import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class BootImagesSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setup() {
        data.put("key", "value")
    }

    def "Read tmp file, fetch boot image details"() {
        final File testConfigFile = tempFolder.newFile("test.json")
        BootImages bootImagesTest = new BootImages()

        when:
        loadDataIntoFile(testConfigFile, file_data)
        bootImagesTest.setBootImagesConfigFile(testConfigFile.absolutePath)
        bootImagesTest.getBootImages()
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file_data             |   output
        "[{\"id\" ,: \"data]" |   "Error in loading boot images data."
    }

    def "Read tmp file, fetch boot image details for a given id"() {
        final File testConfigFile = tempFolder.newFile("test.json")
        BootImages bootImagesTest = new BootImages()

        when:
        loadDataIntoFile(testConfigFile, file_data)
        bootImagesTest.setBootImagesConfigFile(testConfigFile.absolutePath)
        bootImagesTest.getBootImageForId("boot-image-id-0")
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file_data             |   output
        "[{\"id\" ,: \"data]" |   "Error in loading boot images data."
    }

    def "Set boot image configuration file, fetch boot image details for a given id"() {
        BootImages bootImagesTest = new BootImages()
        when:
        bootImagesTest.setBootImagesConfigFile(file_name)
        bootImagesTest.getBootImageForId("boot-image-id-0")
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file_name    |   output
        null         |   "Invalid or null boot images config file."
        ""           |   "Invalid or null boot images config file."
        "/test.json" |   "Given boot images config file doesn't exists : /test.json"
    }

    def "Set boot image configuration file, fetch boot image details"() {
        BootImages bootImagesTest = new BootImages()
        when:
        bootImagesTest.setBootImagesConfigFile(file)
        bootImagesTest.getBootImages()
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file         |   output
        null         |   "Invalid or null boot images config file."
        ""           |   "Invalid or null boot images config file."
        "/test.json" |   "Given boot images config file doesn't exists : /test.json"
    }

    def "fetch boot image details for a given id throwing exceptions"() {
        BootImages bootImagesTest = new BootImages()
        when:
        bootImagesTest.setBootImagesConfigFile(file)
        bootImagesTest.getBootImageForId("boot-image-id-0")
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file         |   output
        null         |   "Invalid or null boot images config file."
        ""           |   "Invalid or null boot images config file."
        "/test.json" |   "Given boot images config file doesn't exists : /test.json"
    }

    private static void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }


    private static PropertyMap data = new PropertyMap()
    private static PropertyMap EMPTY_MAP = new PropertyMap()
    private static PropertyArray EMPTY_ARRAY = new PropertyArray()
}
