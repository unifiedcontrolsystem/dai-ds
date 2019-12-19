package com.intel.runtime_utils;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Properties;

public class PropertyLoaderTest {

    @Rule
    public TemporaryFolder outputFolder_ = new TemporaryFolder();

    private File propertyFile_;

    @Before
    public void setup() {

        propertyFile_ = new File(outputFolder_.getRoot(), "test.properties");

        try(BufferedWriter br = new BufferedWriter(new FileWriter(propertyFile_, true))) {

            br.write("file_test = testing");

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void loadPropertiesFromFileAndResource() {

        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), "prop_loader_test.properties", propertyFile_.getPath());

        Assert.assertEquals("testing", props.get("file_test"));

        Assert.assertEquals("testing", props.get("resource_test"));
    }

    @Test
    public void loadPropertiesFromResourceWithInvalidFile()  {
        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), "prop_loader_test.properties", "fake.txt");

        Assert.assertEquals("resource", props.get("file_test"));
        Assert.assertEquals("testing", props.get("resource_test"));

    }

    @Test
    public void loadPropertiesFromResourceWithNoFile()  {
        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), "prop_loader_test.properties",  null);

        Assert.assertEquals("resource", props.get("file_test"));
        Assert.assertEquals("testing", props.get("resource_test"));

    }

    @Test
    public void loadNoPropertiesInvalidPaths()  {
        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), "fake.properties",  "fake.txt");

        Assert.assertFalse(props.contains("file_test"));
        Assert.assertFalse(props.contains("resource_test"));
    }

    @Test
    public void loadNoPropertiesNoPaths()  {
        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), null,  null);

        Assert.assertFalse(props.contains("file_test"));
        Assert.assertFalse(props.contains("resource_test"));
    }

    @Test
    public void loadNoPropertiesBadCanonicalName() {
        Properties props = PropertyLoader.loadProperties("coconuts", "prop_loader_test.properties",  null);

        Assert.assertFalse(props.contains("file_test"));
        Assert.assertFalse(props.contains("resource_test"));
    }

    @Test
    public void storePropertiesInSystem() {
        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), "prop_loader_test.properties", null);
        PropertyLoader.systemPropertiesMerge(props);

        String resourceTest = System.getProperty("resource_test");
        String fileTest = System.getProperty("file_test");

        System.clearProperty("resource_test");
        System.clearProperty("file_test");


        Assert.assertEquals("testing", resourceTest);
        Assert.assertEquals("resource", fileTest);

    }

    @Test
    public void storeSystemPropertiesNoOverwrite() {
        Properties props = PropertyLoader.loadProperties(this.getClass().getCanonicalName(), "prop_loader_test.properties", null);

        System.setProperty("resource_test", "default");
        System.setProperty("file_test", "default");


        PropertyLoader.systemPropertiesMerge(props);

        String resourceTest = System.getProperty("resource_test");
        String fileTest = System.getProperty("file_test");

        System.clearProperty("resource_test");
        System.clearProperty("file_test");

        Assert.assertEquals("default", resourceTest);
        Assert.assertEquals("default", fileTest);


    }

}
