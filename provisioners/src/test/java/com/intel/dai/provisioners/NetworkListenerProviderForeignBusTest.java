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
        when(config_.getFirstNetworkBaseUrl(anyBoolean())).thenReturn("http://127.0.0.2:9999");
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

    @Test(expected = NetworkListenerProviderException.class)
    public void processRawStringDataNegative1() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(sample4,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void processRawStringDataNegative2() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(badSample1,
                mock(NetworkListenerConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test(expected = NetworkListenerProviderException.class)
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

    private NetworkListenerProviderForeignBus transformer_;
    private NetworkListenerConfig config_;
    private PropertyMap map_;
    private SystemActions system_;


    private static final String sample1 = "{\"State\":\"Ready\",\"Components\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String sample2 = "{\"State\":\"Off\",\"Components\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String sample3 = "{\"State\":\"On\",\"Components\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String sample4 = "{\"State\":\"unknown\",\"Components\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String badSample1 = "\"State\":\"unknown\",\"Components\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String badSample2 = "{\"State\":\"unknown\",\"Components\":\"all\"}";


    private static final String imageListStr_ = "[{\"hosts\":[\"Default\"],\"kernel\":\"http:\\/\\/api-gw-service-nmn.local\\/apis\\/ars\\/assets\\/artifacts\\/generic\\/vmlinuz-4.12.14-15.5_8.1.96-cray_shasta_c\",\"BootImageFile\":\"centos7.3-vtune\",\"BootStrapImageFile\":\"3.10.0-514.16.1.el7.x86_64\",\"KernelArgs\":null,\"description\":\"Centos 7.3 w\\/ Vtune VNFS\",\"BootImageChecksum\":\"ecaa78c6c36a3442a45f20852f99a7cf\",\"id\":\"boot-image\",\"params\":\"console=tty0 console=ttyS0,115200n8 initrd=initrd-4.12.14-15.5_8.1.96-cray_shasta_c root=crayfs nfsserver=10.2.0.1 nfspath=\\/var\\/opt\\/cray\\/boot_images imagename=\\/SLES15 selinux=0 rd.shell rd.net.timeout.carrier=40 rd.retry=40 ip=dhcp rd.neednet=1 crashkernel=256M htburl=https:\\/\\/api-gw-service-nmn.local\\/a                            pis\\/hbtd\\/hmi\\/v1\\/heartbeat bad_page=panic hugepagelist=2m-2g intel_iommu=off iommu=pt numa_interleave_omit=headless numa_zonelist_order=node oops=panic pageblock_                            order=14 pcie_ports=native printk.synchronous=y quiet turbo_boost_limit=999\",\"BootStrapImageChecksum\":\"93a94d8985aa3b10e38122d2bd8bbba1\",\"BootOptions\":null,\"initrd\":\"http:\\/\\/api-gw-service-nmn.local\\/apis\\/ars\\/assets\\/artifacts\\/generic\\/initrd-4.12.14-15.5_8.1.96-cray_shasta_c\"}]";
}
