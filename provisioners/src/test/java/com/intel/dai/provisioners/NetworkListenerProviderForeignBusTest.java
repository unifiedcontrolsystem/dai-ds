package com.intel.dai.provisioners;

import com.intel.dai.dsapi.BootState;
import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.dai.network_listener.DataType;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.dai.network_listener.SystemActions;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkListenerProviderForeignBusTest {

    private static class MockNetworkListenerProviderForeignBus extends NetworkListenerProviderForeignBus {
        public MockNetworkListenerProviderForeignBus(Logger logger) { super(logger); }
        @Override protected RESTClient createClient() {
            return client_;
        }
    }

    @Before
    public void setUp() {
        transformer_ = new NetworkListenerProviderForeignBus(mock(Logger.class));
        config_ = mock(NetworkListenerConfig.class);
        system_ = mock(SystemActions.class);
        client_ = mock(RESTClient.class);

        map_ = new PropertyMap();
        map_.put("publish", true);
        map_.put("publishTopic", "test_boot_topic");
        map_.put("bootParametersInfoUrl", "http://localhost:1234/com/intel/dai/hwinventory/api/test/bootparams");
        map_.put("bootParameterForLocationInfoUrl", "http://localhost:1234/com/intel/dai/hwinventory/api/test/bootparams?host=x0");
        map_.put("bootImageInfoUrl", "http://localhost:1234/com/intel/dai/hwinventory/api/test/bootimages");
        map_.put("bootImageForImageIdInfoUrl", "http://localhost:1234/com/intel/dai/hwinventory/api/test/bootimages/id");
        map_.put("doActions", false);

        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(map_);

        List<String> streams =  new ArrayList<String>() {{add("testStream");}};
        when(config_.getProfileStreams()).thenReturn(streams);

        Map<String, String> testStreamHMap = new HashMap<String, String>() {{
            put("tokenAuthProvider", "com.intel.authentication.KeycloakTokenAuthentication");
        }};
        PropertyMap testStreamMap = new PropertyMap(testStreamHMap);
        when(config_.getNetworkArguments(anyString())).thenReturn(testStreamMap);
    }

    @Test
    @Ignore
    public void processRawStringData() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData("topic", sample1,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData("topic", sample2, mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData("topic", sample3, mock(NetworkListenerConfig.class));
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
    public void actOnData1() throws InterruptedException, RESTClientException {
        when(client_.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(200,
                bootImagesListStr_, null), new BlockingResult(200,
                bootParamsListStr_, null));

        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_OFFLINE);
        data.storeExtraData("bootImageId", "someImage");
        map_.put("publish", false);
        System.setProperty("daiBootImagePollingMs", "150");
        MockNetworkListenerProviderForeignBus actions = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions.initialize();
        Map<String, String> testStreamHMap = new HashMap<String, String>() {{put("tokenAuthProvider", "testAuth");}};
        PropertyMap testStreamMap = new PropertyMap(testStreamHMap);
        when(config_.getNetworkArguments(anyString())).thenReturn(testStreamMap);
        actions.actOnData(data, config_, system_);
        Thread.sleep(100);
        actions.actOnData(data, config_, system_);
        Thread.sleep(250);
        actions.actOnData(data, config_, system_);
    }

    @Test
    public void actOnData2() throws RESTClientException {
        when(client_.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(200,
                bootImagesListStr_, null), new BlockingResult(200,
                bootParamsListStr_, null), new BlockingResult(200,
                bootParamForIdMap_, null), new BlockingResult(200,
                bootImageForIdMap_, null));

        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_ONLINE);
        data.storeExtraData("bootImageId", "someImage");
        data.storeExtraData("foreignLocation", "location");
        MockNetworkListenerProviderForeignBus actions = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions.initialize();
        actions.actOnData(data, config_, system_);
        when(client_.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(200,
                bootParamForIdMap_, null), new BlockingResult(200,
                bootImageForIdMap_, null));
        data.storeExtraData("bootImageId", "");
        actions.actOnData(data, config_, system_);
    }

    @Test
    public void actOnData3() throws RESTClientException {
        when(client_.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(404,
                bootImagesListStr_, null), new BlockingResult(404,
                bootParamsListStr_, null));

        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_BOOTING);
        data.storeExtraData("bootImageId", "someImage");
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(map_);
        MockNetworkListenerProviderForeignBus actions = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions.initialize();
        actions.actOnData(data, config_, system_);
    }

    @Test
    public void makeInstanceDataForFailedNodeUpdate() {
        CommonDataFormat data = new CommonDataFormat(0L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_ONLINE);
        data.storeExtraData("foreignLocation", "original");
        assertEquals("ForeignLocation='original'; UcsLocation='location'; BootMessage='NODE_ONLINE'",
                transformer_.makeInstanceDataForFailedNodeUpdate(data));
    }

    private NetworkListenerProviderForeignBus transformer_;
    private NetworkListenerConfig config_;
    private PropertyMap map_;
    private SystemActions system_;


    private static final String sample1 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"Ready\"}]}}";
    private static final String sample2 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"Off\"}]}}";
    private static final String sample3 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"On\"}]}}";
    private static final String sample4 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"unknown\"}]}}";
    private static final String badSample1 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"unknown\"}]}}";
    private static final String badSample2 = "{\"metrics\":{\"messages\":[{\"Components\":[\"x3000c0s34b4n0\"],\"Flag\":\"Alert\",\"State\":\"unknown\"}]}}";
    private static final String bootImagesListStr_ = "[\n" +
            "  {\n" +
            "    \"created\": \"2020-07-14T23:42:14.144283+00:00\" ,\n" +
            "    \"id\": \"boot-image-id-0\" ,\n" +
            "    \"link\": {\n" +
            "      \"etag\": \"etag-0\" ,\n" +
            "      \"path\": \"s3://boot-images/boot-image-id-0/manifest.json\" ,\n" +
            "      \"type\": \"s3\"\n" +
            "    } ,\n" +
            "    \"name\": \"boot-image-name-0\"\n" +
            "  } ,\n" +
            "  {\n" +
            "    \"created\": \"2020-07-15T17:04:04.130810+00:00\" ,\n" +
            "    \"id\": \"boot-image-id-1\" ,\n" +
            "    \"link\": {\n" +
            "      \"etag\": \"etag-1\" ,\n" +
            "      \"path\": \"s3://boot-images/boot-image-id-1/manifest.json\" ,\n" +
            "      \"type\": \"s3\"\n" +
            "    } ,\n" +
            "    \"name\": \"boot-image-name-1\"\n" +
            "  } ,\n" +
            "  {\n" +
            "    \"created\": \"2020-07-15T00:55:22.030740+00:00\" ,\n" +
            "    \"id\": \"boot-image-id-2\" ,\n" +
            "    \"link\": {\n" +
            "      \"etag\": \"etag-2\" ,\n" +
            "      \"path\": \"s3://boot-images/boot-image-id-2/manifest.json\" ,\n" +
            "      \"type\": \"s3\"\n" +
            "    } ,\n" +
            "    \"name\": \"boot-image-name-2\"\n" +
            "  }\n" +
            "]";

    private static final String bootImageForIdMap_ = " {\n" +
            "    \"created\": \"2020-07-15T17:04:04.130810+00:00\" ,\n" +
            "    \"id\": \"boot-image-id-1\" ,\n" +
            "    \"link\": {\n" +
            "      \"etag\": \"etag-1\" ,\n" +
            "      \"path\": \"s3://boot-images/boot-image-id-1/manifest.json\" ,\n" +
            "      \"type\": \"s3\"\n" +
            "    } ,\n" +
            "    \"name\": \"boot-image-name-1\"\n" +
            "  }";

    private static final String bootParamsListStr_ = "[\n" +
            "  {\n" +
            "    \"kernel\": \"s3://boot-images/boot-image-id-0/kernel\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"kernel\": \"s3://boot-images/boot-image-id-1/kernel\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"initrd\": \"s3://boot-images/boot-image-id-0/initrd\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"initrd\": \"s3://boot-images/boot-image-id-1/initrd\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"host0\"\n" +
            "    ],\n" +
            "    \"params\": \"param0\",\n" +
            "    \"kernel\": \"s3://boot-images/boot-image-id-0/kernel\",\n" +
            "    \"initrd\": \"s3://boot-images/boot-image-id-0/initrd\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"host1\"\n" +
            "    ],\n" +
            "    \"params\": \"param1\",\n" +
            "    \"kernel\": \"s3://boot-images/boot-image-id-0/kernel\",\n" +
            "    \"initrd\": \"s3://boot-images/boot-image-id-0/initrd\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"host2\"\n" +
            "    ],\n" +
            "    \"params\": \"param2\",\n" +
            "    \"kernel\": \"s3://boot-images/boot-image-id-2/kernel\",\n" +
            "    \"initrd\": \"s3://boot-images/boot-image-id-2/initrd\"\n" +
            "  }\n" +
            "]";

    private static final String bootParamForIdMap_ = " [{\n" +
            "    \"hosts\": [\n" +
            "      \"host1\"\n" +
            "    ],\n" +
            "    \"params\": \"param1\",\n" +
            "    \"kernel\": \"s3://boot-images/boot-image-id-0/kernel\",\n" +
            "    \"initrd\": \"s3://boot-images/boot-image-id-0/initrd\"\n" +
            "  }]";

    private static RESTClient client_;
}
