package com.intel.dai.monitoring;

import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RasEventProviderForeignBusTest {
    static class MockRasEventProviderForeignBus extends RasEventProviderForeignBus {
        MockRasEventProviderForeignBus(Logger logger) { super(logger); initialize(); }

        @Override
        protected InputStream getMetaDataStream() {
            if(!returnNull_)
                if(!loadBad_)
                    return new ByteArrayInputStream(jsonMetaData_.getBytes());
                else
                    return new ByteArrayInputStream(badJsonMetaData_.getBytes());
            else
                return null;
        }

        private static boolean returnNull_ = false;
        private static boolean loadBad_ = false;
    }

    @Before
    public void setUp() throws Exception {
        MockRasEventProviderForeignBus.returnNull_ = false;
        MockRasEventProviderForeignBus.loadBad_ = false;
        RasEventProviderForeignBus.Accumulator.suppressionCount_ = 3;
        RasEventProviderForeignBus.Accumulator.suppressionWindowsSeconds_ = 10;
        transformer_ = new MockRasEventProviderForeignBus(mock(Logger.class));
        config_ = mock(NetworkListenerConfig.class);
        classConfig_.clear();
        classConfig_.put("useRepeatSuppression", true);
        classConfig_.put("suppressionCount", 3);
        classConfig_.put("suppressionWindowSeconds", 10);
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(classConfig_);

        providerConfig_ = new PropertyMap();
        providerConfig_.put("publish", true);
        ActionConfig_ = mock(NetworkListenerConfig.class);
        when(ActionConfig_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(providerConfig_);
    }

    @Test
    public void processRawStringData() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(sample1, config_);
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample1, config_);
        assertEquals(0, dataList.size());
        dataList = transformer_.processRawStringData(sample1, config_);
        assertEquals(1, dataList.size());
        transformer_.doSuppression_ = false;
        dataList = transformer_.processRawStringData(sample2, config_);
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample3, config_);
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample4, config_);
        assertEquals(1, dataList.size());
    }

    @Test(expected = RuntimeException.class)
    public void getStreamFails() throws Exception {
        MockRasEventProviderForeignBus.returnNull_ = true;
        new MockRasEventProviderForeignBus(mock(Logger.class));
    }

    @Test(expected = RuntimeException.class)
    public void getBadStream() throws Exception {
        MockRasEventProviderForeignBus.loadBad_ = true;
        new MockRasEventProviderForeignBus(mock(Logger.class));
    }

    @Test
    public void processRawStringDataMissingEventType() throws Exception {
        List<CommonDataFormat> result = transformer_.processRawStringData(badSample1, config_);
        assertEquals(0, result.size());
    }

    @Test // Cannot retrieve value, no assert is possible.
    public void processRawStringDataBadEventType() throws Exception {
        transformer_.processRawStringData(badSample2, config_);
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void processRawStringDataBadJson() throws Exception {
        transformer_.processRawStringData(badSample3, config_);
    }

    @Test
    public void actOnData1() {
        MockRasEventProviderForeignBus actions = new MockRasEventProviderForeignBus(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.RasEvent);
        data.setRasEvent("RasUnknownEvent", "Instance Data");
        actions.actOnData(data, ActionConfig_, mock(SystemActions.class));
        data.setRasEvent("RasUnknownEvent", "Instance Data");
        actions.actOnData(data, ActionConfig_, mock(SystemActions.class));
    }

    @Test
    public void actOnData2() {
        when(ActionConfig_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(null);
        MockRasEventProviderForeignBus actions = new MockRasEventProviderForeignBus(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.RasEvent);
        data.setRasEvent("RasUnknownEvent", "Instance Data");
        actions.actOnData(data, ActionConfig_, mock(SystemActions.class));
    }

    private RasEventProviderForeignBus transformer_;
    private NetworkListenerConfig config_;
    private PropertyMap classConfig_ = new PropertyMap();
    private NetworkListenerConfig ActionConfig_;
    private PropertyMap providerConfig_;
    private static final String sample1 = "{\"event-type\":\"ec_node_halt_rsp\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String sample2 = "{\"event-type\":\"ec_node_halt_rsp\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String sample3 = "{\"event-type\":\"ec_node_halt_rsp\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String sample4 = "{\"event-type\":\"ec_node_halt_rsp\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String badSample1 = "{\"event-tipe\":\"ec_node_halt_rsp\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String badSample2 = "{\"event-type\":\"ec_node_halt_xxx\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String badSample3 = "\"event-type\":\"ec_node_halt_rsp\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"payload\":\"sample1\"}";
    private static final String jsonMetaData_ = "{\n" +
            "    \"ec_boot\": \"RasMntrForeignNodeBoot\",\n" +
            "    \"ec_node_standby\": \"RasMntrForeignNodeStandby\",\n" +
            "    \"ec_service_started\": \"RasMntrForeignNodeSrvStarted\",\n" +
            "    \"ec_node_available\": \"RasMntrForeignNodeAvailable\",\n" +
            "    \"ec_node_failed\": \"RasMntrForeignNodeFailed\",\n" +
            "    \"ec_service_failed\": \"RasMntrForeignNodeSrvFailed\",\n" +
            "    \"ec_node_unavailable\": \"RasMntrForeignNodeUnavailable\",\n" +
            "    \"ec_smw_resiliency_hb\": \"RasMntrForeignHeartbeat\",\n" +
            "    \"ec_heartbeat_stop\": \"RasMntrForeignHeartbeatStop\",\n" +
            "    \"ec_hw_error\": \"RasMntrForeignHWErr\",\n" +
            "    \"ec_l0_voltage\": \"RasMntrForeignVoltFault\",\n" +
            "    \"ec_agg_data\": \"RasMntrForeignAggData\",\n" +
            "    \"ec_alps_app_status\": \"RasMntrForeignAlpsAppStatus\",\n" +
            "    \"ec_attribute_req\": \"RasMntrForeignAttrReq\",\n" +
            "    \"ec_attribute_data\": \"RasMntrForeignAttrData\",\n" +
            "    \"ec_boot_rsp\": \"RasMntrForeignBootRsp\",\n" +
            "    \"ec_debug_level\": \"RasMntrForeignDbgLevel\",\n" +
            "    \"ec_debug_level_rsp\": \"RasMntrForeignDbgLevelRsp\",\n" +
            "    \"ec_dump_cabinet\": \"RasMntrForeignDumpCabinet\",\n" +
            "    \"ec_dump_cabinet_data\": \"RasMntrForeignDumpCabinetData\",\n" +
            "    \"ec_global_nid_list_req\": \"RasMntrForeignGlobalNidListReq\",\n" +
            "    \"ec_global_nid_list_rsp\": \"RasMntrForeignGlobalNidListRsp\",\n" +
            "    \"ec_hsn_handshake\": \"RasMntrForeignHSNHandshake\",\n" +
            "    \"ec_hsn_link_cfg\": \"RasMntrForeignHSNLinkCfg\",\n" +
            "    \"ec_hsn_link_cfg_rsp\": \"RasMntrForeignHSNLinkCfgRsp\",\n" +
            "    \"ec_hsn_link_data_req\": \"RasMntrForeignHSNLinkDataReq\",\n" +
            "    \"ec_hsn_load_req\": \"RasMntrForeignHSNLoadReq\",\n" +
            "    \"ec_hsn_load_rsp\": \"RasMntrForeignHSNLoadRsp\",\n" +
            "    \"ec_imps_image_map_req\": \"RasMntrForeignImpsImgMapReq\",\n" +
            "    \"ec_imps_image_map_rsp\": \"RasMntrForeignImpsImgMapRsp\",\n" +
            "    \"ec_l0_hss_op\": \"RasMntrForeignL0Op\",\n" +
            "    \"ec_l0_hss_op_rsp\": \"RasMntrForeignL0OpRsp\",\n" +
            "    \"ec_l0_mod_dwn\": \"RasMntrForeignL0ModDown\",\n" +
            "    \"ec_l0_mod_dwn_rsp\": \"RasMntrForeignL0ModDownRsp\",\n" +
            "    \"ec_l0_mod_up\": \"RasMntrForeignL0ModUp\",\n" +
            "    \"ec_l0_mod_up_rsp\": \"RasMntrForeignL0ModUpRsp\",\n" +
            "    \"ec_l0_node_dwn\": \"RasMntrForeignL0NodeDown\",\n" +
            "    \"ec_l0_node_dwn_rsp\": \"RasMntrForeignL0NodeDownRsp\",\n" +
            "    \"ec_l0_node_up\": \"RasMntrForeignL0NodeUp\",\n" +
            "    \"ec_l0_node_up_rsp\": \"RasMntrForeignL0NodeUpRsp\",\n" +
            "    \"ec_l0_reset\": \"RasMntrForeignL0Reset\",\n" +
            "    \"ec_l0_reset_rsp\": \"RasMntrForeignL0ResetRsp\",\n" +
            "    \"ec_l1_attribute_req\": \"RasMntrForeignL1AttrReq\",\n" +
            "    \"ec_l1_attribute_data\": \"RasMntrForeignL1AttrData\",\n" +
            "    \"ec_marker\": \"RasMntrForeignMarker\",\n" +
            "    \"ec_nid_list_req\": \"RasMntrForeignNidListReq\",\n" +
            "    \"ec_nid_list_rsp\": \"RasMntrForeignNidListRsp\",\n" +
            "    \"ec_node_cmd\": \"RasMntrForeignNodeCmd\",\n" +
            "    \"ec_node_cmd_rsp\": \"RasMntrForeignNodeCmdRsp\",\n" +
            "    \"ec_node_halt\": \"RasMntrForeignNodeHalt\",\n" +
            "    \"ec_node_halt_rsp\": \"RasMntrForeignNodeHaltRsp\",\n" +
            "    \"ec_node_info\": \"RasMntrForeignNodeInfo\",\n" +
            "    \"ec_orb_timeout_handled\": \"RasMntrForeignOrbTimeoutHandled\",\n" +
            "    \"ec_part_id_lookup_req\": \"RasMntrForeignPartIdLookupReq\",\n" +
            "    \"ec_part_id_lookup_response\": \"RasMntrForeignPartIdLookupRsp\",\n" +
            "    \"ec_part_trans_request\": \"RasMntrForeignPartTransReq\",\n" +
            "    \"ec_part_trans_response\": \"RasMntrForeignPartTransRsp\",\n" +
            "    \"ec_part_trans_done\": \"RasMntrForeignPartTransDone\",\n" +
            "    \"ec_pm_request_data\": \"RasMntrForeignPMDataReq\",\n" +
            "    \"ec_pm_request_data_rsp\": \"RasMntrForeignPMDataRsp\",\n" +
            "    \"ec_power_config\": \"RasMntrForeignPowerCfg\",\n" +
            "    \"ec_power_config_rsp\": \"RasMntrForeignPowerCfgRsp\",\n" +
            "    \"ec_rca_diag\": \"RasMntrForeignRcaDiag\",\n" +
            "    \"ec_route_phase1\": \"RasMntrForeignRoutePh1\",\n" +
            "    \"ec_route_phase3_rsp\": \"RasMntrForeignRoutePh3Rsp\",\n" +
            "    \"ec_sicproc_stop\": \"RasMntrForeignSicProcStop\",\n" +
            "    \"ec_sicproc_stop_rsp\": \"RasMntrForeignSicProcStopRsp\",\n" +
            "    \"ec_sm_clear_state_flags\": \"RasMntrForeignClearStateFlag\",\n" +
            "    \"ec_sm_moniker_data\": \"RasMntrForeignMonikerData\",\n" +
            "    \"ec_software_node_info\": \"RasMntrForeignSoftwareNodeInfo\",\n" +
            "    \"ec_state_trans_request\": \"RasMntrForeignStateTransReq\",\n" +
            "    \"ec_state_trans_response\": \"RasMntrForeignStateTransRsp\",\n" +
            "    \"ec_state_trans_done\": \"RasMntrForeignStateTransDone\",\n" +
            "    \"ec_ui_boot\": \"RasMntrForeignUIBoot\",\n" +
            "    \"ec_ui_boot_rsp\": \"RasMntrForeignUIBootRsp\",\n" +
            "    \"ec_ui_dynam_state_req\": \"RasMntrForeignUIStateReq\",\n" +
            "    \"ec_ui_dynam_state_rsp\": \"RasMntrForeignUIStateRsp\",\n" +
            "    \"ec_ui_flag_rsp\": \"RasMntrForeignUIFlagRsp\",\n" +
            "    \"ec_ui_halt\": \"RasMntrForeignUIHalt\",\n" +
            "    \"ec_ui_halt_rsp\": \"RasMntrForeignUIHaltRsp\",\n" +
            "    \"ec_ui_part_conf_req\": \"RasMntrForeignUIPartConfReq\",\n" +
            "    \"ec_ui_part_conf_rsp\": \"RasMntrForeignUIPartConfRsp\",\n" +
            "    \"ec_update_node_list_req\": \"RasMntrForeignUpdateNodeListReq\",\n" +
            "    \"ec_update_node_list_rsp\": \"RasMntrForeignUpdateNodeListRsp\"\n" +
            "}\n";
    private static final String badJsonMetaData_ = "\n" +
            "    \"ec_boot\": \"RasMntrForeignNodeBoot\",\n" +
            "    \"ec_node_standby\": \"RasMntrForeignNodeStandby\",\n" +
            "\n";
}
