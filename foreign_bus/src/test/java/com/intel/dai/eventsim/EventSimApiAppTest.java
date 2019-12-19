package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

public class EventSimApiAppTest {
    public class EventSimApiAppMock extends EventSimApiApp {
        public EventSimApiAppMock(String configFile) throws IOException, ConfigIOParseException, PropertyNotExpectedType {
            super(configFile, mock(Logger.class));
            initialize();
        }

        @Override
        public void loadEngine() {
            eventSimEngine = eventSimEngineMock;
        }

        @Override
        public void loadBootParamApi() {
            bootParamApi_ = bootapiMock;
        }

        @Override
        public void loadInventoryApi() { inventoryApi_ = inventoryMock;}

        @Override
        void loadNetworkSource() {
            source_ = sourceMock;
            jsonParser_ = jsonParser;
        }
    }

    @Before
    public void setup() {
        sourceMock = mock(NetworkSource.class);
        eventSimEngineMock = mock(EventSimEngine.class);
        bootapiMock = mock(BootParamApi.class);
        inventoryMock = mock(InventoryApi.class);
        jsonParser = ConfigIOFactory.getInstance("json");
        assert jsonParser != null : "Failed to create a JSON parser!";
    }

    @Test
    public void testEventsApi() throws PropertyNotExpectedType, IOException, ConfigIOParseException, RESTServerException {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        apiAppMock.executeRoutes(apiAppMock);
    }

    @Test
    public void testRasEvents() throws Exception {
        Mockito.doNothing().when(eventSimEngineMock).publishRasEvents(anyString(), anyString(), anyString(), anyString());
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        Map<String, String> param = loadPam();
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", apiAppMock.generatRasEvents(param));
    }

    @Test
    public void testSensorEvents() throws Exception {
        Mockito.doNothing().when(eventSimEngineMock).publishSensorEvents(anyString(), anyString(), anyString(), anyString());
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        Map<String, String> param = loadPam();
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", apiAppMock.generateEnvEvents(param));
    }

    @Test
    public void testBootEvents() throws Exception {
        Mockito.doNothing().when(eventSimEngineMock).publishBootEvents(anyString(), anyString(), anyString());
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        Map<String, String> param = loadPam();
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", apiAppMock.generateBootEvents(param));
    }

    @Test
    public void testBootParameters() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        PropertyMap contentData = new PropertyMap();
        contentData.put("id", "image1");
        PropertyArray content = new PropertyArray();
        content.add(contentData);
        PropertyMap bootParamData = new PropertyMap();
        bootParamData.put("content", content);
        Mockito.when(apiAppMock.bootParamApi_.getBootParametrs()).thenReturn(bootParamData);
        assertEquals("[{\"id\":\"image1\"}]", apiAppMock.getBootParameters(new HashMap<>()));
    }

    @Test
    public void initiateInventory() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("xnames", "test1");
        assertNotNull(apiAppMock.initiateInventoryDiscover(parameters));
    }

    @Test
    public void getDiscoveryStatus() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        assertTrue(apiAppMock.getAllInventoryDiscoverStatus(new HashMap<>()).contains("[{\"Status\":\"Complete\",\"Details\":null,\"LastUpdateTime\":"));
    }

    @Test
    public void getHWInventory() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        PropertyArray data = new PropertyArray();
        data.add("id");
        Mockito.when(apiAppMock.inventoryApi_.getHwInventory()).thenReturn(data);
        assertEquals("[\"id\"]", apiAppMock.getInventoryHardware(new HashMap<>()));
    }

    @Test
    public void getHWInventoryForLocation() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        PropertyArray data = new PropertyArray();
        data.add("id");
        Mockito.when(apiAppMock.inventoryApi_.getInventoryHardwareForLocation("x00n1")).thenReturn(data);
        HashMap<String, String> params = new HashMap<>();
        params.put("sub_cmd", "x00n1");
        assertEquals("[\"id\"]", apiAppMock.getInventoryHardwareForLocation(params));
    }

    @Test
    public void getHWInventoryQueryForLocation() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        PropertyArray data = new PropertyArray();
        data.add("id");
        Mockito.when(apiAppMock.inventoryApi_.getInventoryHardwareQueryForLocation("x00n1")).thenReturn(data);
        HashMap<String, String> params = new HashMap<>();
        params.put("sub_cmd", "x00n1");
        assertEquals("[\"id\"]", apiAppMock.getInventoryHardwareQueryForLocation(params));
    }

    @Test
    public void startForeignServer() throws Exception {
        EventSimApiAppMock apiAppMock = new EventSimApiAppMock("Eventsim.json");
        Mockito.doNothing().when(apiAppMock.source_).startServer();
        apiAppMock.startServer();
    }

    private Map<String,String> loadPam() {
        HashMap<String, String> param = new HashMap<>();
        param.put("location", null);
        param.put("label", null);
        param.put("probability", null);
        param.put("burst", null);
        param.put("count", null);
        result.put("result", null);
        return param;
    }

    private ConfigIO jsonParser;
    private NetworkSource sourceMock;
    private EventSimEngine eventSimEngineMock;
    private BootParamApi bootapiMock;
    private InventoryApi inventoryMock;
    private PropertyMap result = new PropertyMap();
}
