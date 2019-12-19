// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import com.intel.dai.AdapterInformation;
import com.intel.logging.Logger;
import org.junit.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartitionedMonitorAdapterAppTest {
    static class MockPartitionedMonitorAdapterApp extends PartitionedMonitorAdapterApp {
        @Override
        int execute(PartitionedMonitorAdapter adapter) { return 0; }

        @Override
        InputStream getConfigStream() throws FileNotFoundException {
            if(streamException_)
                throw new FileNotFoundException("TEST");
            return new ByteArrayInputStream(json_.getBytes());
        }
        boolean streamException_ = false;
    }
    @BeforeClass
    public static void setUp() {
        originalFile_ = new File(System.getenv("HOME") + "/.config/ucs/PartitionedMonitorConfig.json");
        savedFile_ = new File(originalFile_.toString() + "." + UUID.randomUUID().toString());
        if(originalFile_.exists())
            originalFile_.renameTo(savedFile_);
    }

    @AfterClass
    public static void tearDown() {
        if(savedFile_.exists())
            savedFile_.renameTo(originalFile_);
    }

    @Test
    public void entryPoint() {
        PartitionedMonitorAdapterApp app = new MockPartitionedMonitorAdapterApp();
        AdapterInformation info = new AdapterInformation("TEST", "TEST", "location", "hostname");
        assertEquals(0, app.entryPoint(new String[] {"127.0.0.1", "location", "hostname"}, mock(Logger.class), info));
    }

    @Test(expected = RuntimeException.class)
    public void entryPointNegative1() {
        PartitionedMonitorAdapterApp app = new MockPartitionedMonitorAdapterApp();
        assertEquals(0, app.entryPoint(null, mock(Logger.class), mock(AdapterInformation.class)));
    }

    @Test(expected = RuntimeException.class)
    public void entryPointNegative3() {
        MockPartitionedMonitorAdapterApp app = new MockPartitionedMonitorAdapterApp();
        app.streamException_ = true;
        assertEquals(0, app.entryPoint(new String[] {"127.0.0.1", "location", "hostname", "telemetryPart1"},
                mock(Logger.class), mock(AdapterInformation.class)));
    }

    @Test
    public void execute() {
        PartitionedMonitorAdapterApp app = new PartitionedMonitorAdapterApp();
        PartitionedMonitorAdapter adapter = mock(PartitionedMonitorAdapter.class);
        when(adapter.run()).thenReturn(0);
        assertEquals(0, app.execute(adapter));
    }

    @Test(expected = FileNotFoundException.class)
    public void getConfigStream() throws Exception {
        PartitionedMonitorAdapterApp app = new PartitionedMonitorAdapterApp();
        app.getConfigStream();
    }

    PartitionedMonitorAdapterApp app_;
    static File originalFile_;
    static File savedFile_;
    private static String json_ = "{\n" +
            "  \"providerClassMap\": {\n" +
            "    \"foreignInventoryAction\": \"com.intel.dai.foreign.actions.ForeignApiInventoryAction\",\n" +
            "    \"foreignPostBuilder\": \"com.intel.dai.foreign.builders.ForeignApiPostBuilder\",\n" +
            "    \"foreignTelemetryAction\": \"com.intel.dai.foreign.actions.ForeignApiDataAction\",\n" +
            "    \"foreignEventsAction\": \"com.intel.dai.foreign.actions.ForeignApiEventAction\",\n" +
            "    \"foreignStateAction\": \"com.intel.dai.foreign.actions.ForeignApiStateAction\",\n" +
            "    \"foreignInventoryData\": \"com.intel.dai.foreign.transforms.ForeignApiInventoryTransformer\",\n" +
            "    \"foreignStateData\": \"com.intel.dai.foreign.transforms.ForeignApiStateTransformer\",\n" +
            "    \"foreignTelemetryData\": \"com.intel.dai.foreign.transforms.ForeignApiDataTransformer\",\n" +
            "    \"foreignEventsData\": \"com.intel.dai.foreign.transforms.ForeignApiEventTransformer\",\n" +
            "    \"foreignGetBuilder\": \"com.intel.dai.foreign.builders.ForeignApiGetBuilder\",\n" +
            "    \"foreignLogsData\": \"com.intel.dai.foreign.builders.ForeignLogsTransformer\",\n" +
            "    \"foreignLogsAction\": \"com.intel.dai.foreign.builders.ForeignLogsAction\"\n" +
            "  },\n" +
            "  \"networkStreams\": {\n" +
            "    \"nodeSyslog1\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            2,\n" +
            "            3\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"rackTelemetry\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/rackTelemetry\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"rackRasEvents\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry3\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/3\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry2\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/2\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeSyslog0\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            0,\n" +
            "            1\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeSyslog3\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            6,\n" +
            "            7\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry1\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/1\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeRasEvents\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry0\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/0\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"inventoryChanges\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"includeRack\": false\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/inventoryChanges\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"nodeSyslog2\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            4,\n" +
            "            5\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"stateChanges\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"adapterProfiles\": {\n" +
            "    \"logPart3\": {\n" +
            "      \"actionProvider\": \"foreignLogsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeSyslog2\",\n" +
            "        \"nodeSyslog3\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"logs\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignLogsData\"\n" +
            "    },\n" +
            "    \"allInventoryChanges\": {\n" +
            "      \"actionProvider\": \"foreignInventoryAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"inventoryChanges\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"inventoryChanges\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignInventoryData\"\n" +
            "    },\n" +
            "    \"allEvents\": {\n" +
            "      \"actionProvider\": \"foreignEventsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeRasEvents\",\n" +
            "        \"rackRasEvents\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"events\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignEventsData\"\n" +
            "    },\n" +
            "    \"logPart2\": {\n" +
            "      \"actionProvider\": \"foreignLogsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeSyslog1\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"logs\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignLogsData\"\n" +
            "    },\n" +
            "    \"telemetryPart1\": {\n" +
            "      \"actionProvider\": \"foreignTelemetryAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeTelemetry0\",\n" +
            "        \"nodeTelemetry1\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"telemetry\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignTelemetryData\"\n" +
            "    },\n" +
            "    \"logPart1\": {\n" +
            "      \"actionProvider\": \"foreignLogsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeSyslog0\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"logs\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignLogsData\"\n" +
            "    },\n" +
            "    \"telemetryPart2\": {\n" +
            "      \"actionProvider\": \"foreignTelemetryAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeTelemetry2\",\n" +
            "        \"nodeTelemetry3\",\n" +
            "        \"rackTelemetry\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"telemetry\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignTelemetryData\"\n" +
            "    },\n" +
            "    \"allStateChanges\": {\n" +
            "      \"actionProvider\": \"foreignStateAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"stateChanges\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"stateChanges\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignStateData\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"logProvider\": \"log4j2\",\n" +
            "  \"subjectMap\": {\n" +
            "    \"telemetry\": \"EnvironmentalData\",\n" +
            "    \"inventoryChanges\": \"InventoryChangeEvent\",\n" +
            "    \"logs\": \"LogData\",\n" +
            "    \"events\": \"RasEvent\",\n" +
            "    \"stateChanges\": \"StateChangeEvent\"\n" +
            "  }\n" +
            "}";

}