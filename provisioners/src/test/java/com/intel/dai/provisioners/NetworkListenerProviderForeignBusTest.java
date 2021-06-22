package com.intel.dai.provisioners;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.dai.network_listener.DataType;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.dai.network_listener.SystemActions;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkListenerProviderForeignBusTest {

    private static class MockNetworkListenerProviderForeignBus extends NetworkListenerProviderForeignBus {
        public MockNetworkListenerProviderForeignBus(Logger logger) { super(logger); }
    }

    @Before
    public void setUp() throws IOException, ConfigIOParseException {
        actions_ = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions_.initialize();
        transformer_ = new NetworkListenerProviderForeignBus(mock(Logger.class));
        config_ = mock(NetworkListenerConfig.class);
        system_ = mock(SystemActions.class);

        configData_ = loadDataFromFile(sampleConfigFile);

        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(configData_.getMapOrDefault("data", new PropertyMap()));
        when(config_.getSubjectMap()).thenReturn(configData_.getMapOrDefault("subjectMap", new PropertyMap()));
    }

    @Test
    public void processRawStringData() throws Exception {
        String data = mapToString(loadDataFromFile(sample1));
        List<CommonDataFormat> dataList = transformer_.processRawStringData("power", data, config_);
        assertEquals(1, dataList.size());

        data = mapToString(loadDataFromFile(sample2));
        dataList = transformer_.processRawStringData("dhcp", data, config_);
        assertEquals(1, dataList.size());

        data = mapToString(loadDataFromFile(sample3));
        dataList = transformer_.processRawStringData("dhcp", data, config_);
        assertEquals(0, dataList.size());

        data = mapToString(loadDataFromFile(sample4));
        dataList = transformer_.processRawStringData("nodestate", data, config_);
        assertEquals(1, dataList.size());

        data = mapToString(loadDataFromFile(sample5));
        dataList = transformer_.processRawStringData("nodestate", data, config_);
        assertEquals(1, dataList.size());
    }

    @Test
    @Ignore
    public void processRawStringDataNegative1() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData("topic", sample4,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    @Ignore
    public void processRawStringDataNegative2() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData("topic", badSample1,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    @Ignore
    public void processRawStringDataNegative3() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData("topic", badSample2,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    public void actOnData1() throws Exception {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_OFFLINE);
        configData_.getMap("data").put("publish", false);

        data.storeExtraData("id", "someImage");
        actions_.actOnData(data, config_, system_);

        data.storeExtraData("id", "");
        actions_.actOnData(data, config_, system_);
    }

    @Test
    public void actOnData2() throws Exception {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_ONLINE);
        configData_.getMap("data").put("publish", true);

        data.storeExtraData("id", "someImage");
        actions_.actOnData(data, config_, system_);

        data.storeExtraData("id", "");
        actions_.actOnData(data, config_, system_);
    }

    @Test
    public void actOnData3() {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(null);
        data.storeExtraData("bootImageId", "someImage");

        actions_.actOnData(data, config_, system_);
    }

    @Test
    public void makeInstanceDataForFailedNodeUpdate() {
        CommonDataFormat data = new CommonDataFormat(0L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_ONLINE);
        data.storeExtraData("foreignLocation", "original");
        assertEquals("ForeignLocation='original'; UcsLocation='location'; BootMessage='NODE_ONLINE'",
                transformer_.makeInstanceDataForFailedNodeUpdate(data));
    }

    private PropertyMap loadDataFromFile(final String location) throws IOException, ConfigIOParseException {
        if(parser_ == null)
            parser_ = ConfigIOFactory.getInstance("json");
        InputStream stream = NetworkListenerProviderForeignBusTest.class.getResourceAsStream(location);
        return parser_.readConfig(stream).getAsMap();
    }

    private String mapToString(PropertyMap data) {
        if(parser_ == null)
            parser_ = ConfigIOFactory.getInstance("json");
        return parser_.toString(data);
    }

    private NetworkListenerProviderForeignBus transformer_;
    private NetworkListenerConfig config_;
    private SystemActions system_;
    private ConfigIO parser_;
    private PropertyMap configData_;
    private MockNetworkListenerProviderForeignBus actions_;

    private static final String sampleConfigFile = "/resources/data-samples/sample-config.json";
    private static final String sample1 = "/resources/data-samples/sample1.json";
    private static final String sample2 = "/resources/data-samples/sample2.json";
    private static final String sample3 = "/resources/data-samples/sample3.json";
    private static final String sample4 = "/resources/data-samples/sample4.json";
    private static final String sample5 = "/resources/data-samples/sample5.json";
    private static final String badSample1 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"unknown\"}]}}";
    private static final String badSample2 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"unknown\"}]}}";
}
