package com.intel.dai.monitoring_providers;

import com.intel.dai.dsapi.BootState;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.partitioned_monitor.CommonDataFormat;
import com.intel.partitioned_monitor.DataType;
import com.intel.partitioned_monitor.PartitionedMonitorConfig;
import com.intel.partitioned_monitor.SystemActions;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BootEventActionsTest {
    private static class MockBootEventActions extends BootEventActions {
        public MockBootEventActions(Logger logger) { super(logger); }
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
        config_ = mock(PartitionedMonitorConfig.class);
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
    public void actOnData1() throws InterruptedException {
        CommonDataFormat data = new CommonDataFormat(1000000L, "location", DataType.StateChangeEvent);
        data.setStateChangeEvent(BootState.NODE_OFFLINE);
        data.storeExtraData("bootImageId", "someImage");
        map_.put("publish", false);
        System.setProperty("daiBootImagePollingMs", "150");
        MockBootEventActions actions = new MockBootEventActions(mock(Logger.class));
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
        MockBootEventActions actions = new MockBootEventActions(mock(Logger.class));
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
        MockBootEventActions actions = new MockBootEventActions(mock(Logger.class));
        actions.initialize();
        actions.code = 404;
        actions.actOnData(data, config_, system_);
    }

    private PartitionedMonitorConfig config_;
    private PropertyMap map_;
    private SystemActions system_;

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
