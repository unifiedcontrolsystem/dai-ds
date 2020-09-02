package com.intel.dai.provisioners;

import com.intel.dai.dsapi.BootState;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class NetworkListenerProviderForeignBusTest {
    private static class MockNetworkListenerProviderForeignBus extends NetworkListenerProviderForeignBus {
        public MockNetworkListenerProviderForeignBus(Logger logger) { super(logger); }
        @Override protected RESTClient createClient() throws RESTClientException {
            RESTClient client = mock(RESTClient.class);
            when(client.getRESTRequestBlocking(any())).thenReturn(new BlockingResult(code,
                    imageListStr_, null));
            return client;
        }
        int code = 200;
    }

    @Before
    public void setUp() throws Exception {
        transformer_ = new NetworkListenerProviderForeignBus(mock(Logger.class));
        config_ = mock(NetworkListenerConfig.class);
        system_ = mock(SystemActions.class);
        map_ = new PropertyMap();
        map_.put("publish", true);
        map_.put("publishTopic", "test_boot_topic");
        map_.put("bootImageInfoUrlPath", "/com/intel/dai/hwinventory/api/test/bootId");
        map_.put("doActions", false);
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(map_);
        when(config_.getFirstNetworkBaseUrl()).thenReturn("http://127.0.0.2:9999");
    }

    @Test
    public void processRawStringData() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(sample1,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample2, mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample3, mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    public void processRawStringDataNegative1() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(sample4,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    public void processRawStringDataNegative2() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(badSample1,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    public void processRawStringDataNegative3() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(badSample2,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test
    public void actOnData1() throws InterruptedException {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_OFFLINE);
        data.storeExtraData("bootImageId", "someImage");
        map_.put("publish", false);
        System.setProperty("daiBootImagePollingMs", "150");
        MockNetworkListenerProviderForeignBus actions = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions.initialize();
        actions.actOnData(data, config_, system_);
        Thread.sleep(100);
        actions.actOnData(data, config_, system_);
        Thread.sleep(250);
        actions.actOnData(data, config_, system_);
    }

    @Test
    public void actOnData2() {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_ONLINE);
        data.storeExtraData("bootImageId", "someImage");
        MockNetworkListenerProviderForeignBus actions = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions.initialize();
        actions.actOnData(data, config_, system_);
        data.storeExtraData("bootImageId", "");
        actions.actOnData(data, config_, system_);
    }

    @Test
    public void actOnData3() {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_BOOTING);
        data.storeExtraData("bootImageId", "someImage");
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(null);
        MockNetworkListenerProviderForeignBus actions = new MockNetworkListenerProviderForeignBus(mock(Logger.class));
        actions.initialize();
        actions.code = 404;
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


    private static final String imageListStr_ = "[{\"hosts\":[\"Default\"],\"kernel\":\"http:\\/\\/kernel-data.com\",\"BootImageFile\":\"boot-image-file\",\"BootStrapImageFile\":\"boot-strap-image-file\",\"KernelArgs\":null,\"description\":\"boot-image-description\",\"BootImageChecksum\":\"boot-image-chceksum\",\"id\":\"boot-image-id\",\"params\":\"console=tty0 console=ttyS0,115200n8 initrd=initrd-data root=rootfs nfsserver=10.2.0.1 nfspath=\\/boot_images imagename=\\/sles  htburl=https:\\/\\/htburl.com\",\"BootStrapImageChecksum\":\"123\",\"BootOptions\":null,\"initrd\":\"http:\\/\\/initrd.com\"}]";
}
