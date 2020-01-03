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

    private static final String sample1 = "{\"event-type\":\"ec_node_available\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String sample2 = "{\"event-type\":\"ec_node_unavailable\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String sample3 = "{\"event-type\":\"ec_boot\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String sample4 = "{\"event-type\":\"unknown\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String badSample1 = "\"event-type\":\"unknown\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\"}";
    private static final String badSample2 = "{\"event-type\":\"ec_node_failed\",\"location\":\"all\"}";

    private static final String imageListStr_ = "[\n" +
            "                    {\"id\": \"centos7.3-special\",\n" +
            "                     \"description\": \"Centos 7.3 w/ Special VNFS\",\n" +
            "                     \"BootImageFile\": \"centos7.3-special\",\n" +
            "                     \"BootImageChecksum\": \"ecaa78c6c36a3442a45f20852f99a7cf\",\n" +
            "                     \"BootOptions\": null,\n" +
            "                     \"KernelArgs\": null,\n" +
            "                     \"BootStrapImageFile\": \"3.10.0-514.16.1.el7.x86_64\",\n" +
            "                     \"BootStrapImageChecksum\": \"93a94d8985aa3b10e38122d2bd8bbba1\"\n" +
            "                    },\n" +
            "\n" +
            "                    {\"id\": \"centos7.3-slurm\",\n" +
            "                     \"description\": \"Centos 7.3 w/ Slurm VNFS\",\n" +
            "                     \"BootImageFile\": \"centos7.3-1611-slurm\",\n" +
            "                     \"BootImageChecksum\": \"7427dbf6ec4e028f22d595195fe72563\",\n" +
            "                     \"BootOptions\": null,\n" +
            "                     \"KernelArgs\": null,\n" +
            "                     \"BootStrapImageFile\": \"3.10.0-514.16.1.el7.x86_64\",\n" +
            "                     \"BootStrapImageChecksum\": \"93a94d8985aa3b10e38122d2bd8bbba1\"\n" +
            "                    },\n" +
            "\n" +
            "                    {\"id\": \"centos7.3-slurm-special\",\n" +
            "                     \"description\": \"Centos 7.3 w/ Slurm and Special VNFS\",\n" +
            "                     \"BootImageFile\": \"centos7.3-1611-slurm-special\",\n" +
            "                     \"BootImageChecksum\": \"d94a4a134d1ea146afc7a82a084ae1a4\",\n" +
            "                     \"BootOptions\": null,\n" +
            "                     \"KernelArgs\": null,\n" +
            "                     \"BootStrapImageFile\": \"3.10.0-514.16.1.el7.x86_64\",\n" +
            "                     \"BootStrapImageChecksum\": \"93a94d8985aa3b10e38122d2bd8bbba1\"\n" +
            "                    },\n" +
            "\n" +
            "                    {\"id\": \"mOS\",\n" +
            "                     \"description\": \"mOS\",\n" +
            "                     \"BootImageFile\": \"mOS\",\n" +
            "                     \"BootImageChecksum\": \"589a4eedbf582d5e96bd62d6ec0bf1c0\",\n" +
            "                     \"BootOptions\": null,\n" +
            "                     \"KernelArgs\": \"console=tty0 console=ttyS0,115200 selinux=0 intel_idle.max_cstate=1 intel_pstate=disable nmi_watchdog=0 tsc=reliable lwkcpus=40.1-19,41-59:60.21-39,61-79 lwkmem=108G kernelcore=16G\",\n" +
            "                     \"BootStrapImageFile\": \"4.9.61-attaufer-mos-8139\",\n" +
            "                     \"BootStrapImageChecksum\": \"4c09b8f5908802ce64fb898eda8964d6\"\n" +
            "                    },\n" +
            "\n" +
            "                    {\"id\": \"diagImage\",\n" +
            "                     \"description\": \"HPC Offline Diagnostics Image\",\n" +
            "                     \"BootImageFile\": \"centos7.3-1611-slurm-special\",\n" +
            "                     \"BootImageChecksum\": \"d94a4a134d1ea146afc7a82a084ae1a4\",\n" +
            "                     \"BootOptions\": null,\n" +
            "                     \"KernelArgs\": null,\n" +
            "                     \"BootStrapImageFile\": \"sklDiagImage\",\n" +
            "                     \"BootStrapImageChecksum\": \"addc1dce586cf6b96d28bc5eb099773f\"\n" +
            "                    }\n" +
            "                ]";
}
