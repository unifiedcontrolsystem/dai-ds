package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public class NetworkSourceTest {

    public class SSENetworkTestMock extends NetworkSource {

        public SSENetworkTestMock(String configFile) throws IOException, ConfigIOParseException {
            super(configFile, mock(Logger.class));
            initialize();
        }

        @Override
        public void loadConfigFile(String configFile) {
            appConfiguration_ = contentConfigMap;
        }

        public void getConfigDetails() throws PropertyNotExpectedType {
            goodConfigJSON = contentConfigMap.getMap("sseconfig");
        }
    }

    @Before
    public void setup() throws ConfigIOParseException {
        jsonParser_ = ConfigIOFactory.getInstance("json");
        assert jsonParser_ != null : "Failed to create a JSON parser!";
        contentConfigMap = jsonParser_.fromString(json_).getAsMap();
    }

    @Test (expected = RuntimeException.class)
    public void testSSEServer() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        SSENetworkTestMock test = new SSENetworkTestMock("Eventsim.json");
        test.getConfigDetails();
        goodConfigJSON.remove("serverAddress");
        test.validateConfig(goodConfigJSON);
    }

    @Test (expected = RuntimeException.class)
    public void testSSEServerPort() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        SSENetworkTestMock test = new SSENetworkTestMock("Eventsim.json");
        test.getConfigDetails();
        goodConfigJSON.remove("serverPort");
        test.validateConfig(goodConfigJSON);
    }

    @Test (expected = RuntimeException.class)
    public void testSSESubscribeUrls() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        SSENetworkTestMock test = new SSENetworkTestMock("Eventsim.json");
        test.getConfigDetails();
        goodConfigJSON.remove("serverPort");
        test.validateConfig(goodConfigJSON);
    }

    @Test (expected = RuntimeException.class)
    public void testNetworkConfig() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
            contentConfigMap.remove("network");
            SSENetworkTestMock test = new SSENetworkTestMock("Eventsim.json");
            test.validateConfig(contentConfigMap.getMap("network"));
    }

    @Test (expected = RuntimeException.class)
    public void testEventsimConfig() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        contentConfigMap.remove("network");
        SSENetworkTestMock test = new SSENetworkTestMock("Eventsim.json");
        test.getAppConfiguration();
    }

    private ConfigIO jsonParser_;

    private PropertyMap contentConfigMap = new PropertyMap();
    private PropertyMap goodConfigJSON = new PropertyMap();

    private static String json_ = "{\n" +
            "  \"network\": \"sse\",\n" +
            "  \"sseConfig\": {\n" +
            "        \"serverPort\": 5678,\n" +
            "        \"serverAddress\": \"127.0.0.1\",\n" +
            "        \"urls\": \"\"\n" +
            "    }\n" +
            "}";
}
