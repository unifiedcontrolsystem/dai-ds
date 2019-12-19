package com.intel.dai.monitoring_providers;

import com.intel.logging.Logger;
import com.intel.partitioned_monitor.CommonDataFormat;
import com.intel.partitioned_monitor.DataTransformerException;
import com.intel.partitioned_monitor.PartitionedMonitorConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class BootEventTransformerTest {
    @Before
    public void setUp() throws Exception {
        transformer_ = new BootEventTransformer(mock(Logger.class));
    }

    @Test
    public void processRawStringData() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(sample1,
                mock(PartitionedMonitorConfig.class));
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample2, mock(PartitionedMonitorConfig.class));
        assertEquals(1, dataList.size());
        dataList = transformer_.processRawStringData(sample3, mock(PartitionedMonitorConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test(expected = DataTransformerException.class)
    public void processRawStringDataNegative1() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(sample4,
                mock(PartitionedMonitorConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test(expected = DataTransformerException.class)
    public void processRawStringDataNegative2() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(badSample1,
                mock(PartitionedMonitorConfig.class));
        assertEquals(1, dataList.size());
    }

    @Test(expected = DataTransformerException.class)
    public void processRawStringDataNegative3() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData(badSample2,
                mock(PartitionedMonitorConfig.class));
        assertEquals(1, dataList.size());
    }

    private BootEventTransformer transformer_;
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
}
