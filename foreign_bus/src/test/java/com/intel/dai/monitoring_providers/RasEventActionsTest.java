// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring_providers;

import com.intel.logging.Logger;
import com.intel.partitioned_monitor.CommonDataFormat;
import com.intel.partitioned_monitor.DataType;
import com.intel.partitioned_monitor.PartitionedMonitorConfig;
import com.intel.partitioned_monitor.SystemActions;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RasEventActionsTest {

    @Before
    public void setUp() throws Exception {
        providerConfig_ = new PropertyMap();
        providerConfig_.put("publish", true);
        config_ = mock(PartitionedMonitorConfig.class);
        when(config_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(providerConfig_);
    }

    @Test
    public void actOnData1() {
        RasEventActions actions = new RasEventActions(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.RasEvent);
        data.setRasEvent("RasUnknownEvent", "Instance Data");
        actions.actOnData(data, config_, mock(SystemActions.class));
        data.setRasEvent("RasUnknownEvent", "Instance Data");
        actions.actOnData(data, config_, mock(SystemActions.class));
    }

    @Test
    public void actOnData2() {
        when(config_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(null);
        RasEventActions actions = new RasEventActions(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.RasEvent);
        data.setRasEvent("RasUnknownEvent", "Instance Data");
        actions.actOnData(data, config_, mock(SystemActions.class));
    }

    private PartitionedMonitorConfig config_;
    private PropertyMap providerConfig_;
}
