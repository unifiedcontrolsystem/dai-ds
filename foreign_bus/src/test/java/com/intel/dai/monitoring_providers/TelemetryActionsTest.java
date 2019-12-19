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

public class TelemetryActionsTest {
    @Before
    public void setUp() {
        providerConfig_ = new PropertyMap();
        providerConfig_.put("publish", true);
        config_ = mock(PartitionedMonitorConfig.class);
        when(config_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(providerConfig_);
    }

    @Test
    public void actOnData1() {
        TelemetryActions actions = new TelemetryActions(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.EnvironmentalData);
        data.setValueAndUnits(100.0, "degC", "Temp");
        actions.actOnData(data, config_, mock(SystemActions.class));
        data.setMinMaxAvg(95.0, 105.0, 100.0);
        actions.actOnData(data, config_, mock(SystemActions.class));
    }

    @Test
    public void actOnData2() {
        providerConfig_.put("publish", false);
        TelemetryActions actions = new TelemetryActions(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.EnvironmentalData);
        data.setValueAndUnits(100.0, "degC", "Temp");
        actions.actOnData(data, config_, mock(SystemActions.class));
        data.setMinMaxAvg(95.0, 105.0, 100.0);
        actions.actOnData(data, config_, mock(SystemActions.class));
    }

    @Test
    public void actOnData3() {
        when(config_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(null);
        TelemetryActions actions = new TelemetryActions(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.EnvironmentalData);
        data.setValueAndUnits(100.0, "degC", "Temp");
        actions.actOnData(data, config_, mock(SystemActions.class));
        data.setMinMaxAvg(95.0, 105.0, 100.0);
        actions.actOnData(data, config_, mock(SystemActions.class));
    }

    private PartitionedMonitorConfig config_;
    private PropertyMap providerConfig_;
}
