// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.intel.properties.PropertyMap;
import com.intel.runtime_utils.TimeUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of class PartitionedMonitorSystemActions. See parent interface for details on actions.
 */
class NetworkListenerSystemActions implements SystemActions, Initializer {
    NetworkListenerSystemActions(Logger logger, DataStoreFactory factory, AdapterInformation info,
                                 NetworkListenerConfig config) throws NetworkListenerCore.Exception {
        if(parser_ == null) throw new NetworkListenerCore.Exception("Parser was not created");
        log_ = logger;
        factory_ = factory;
        adapter_ = info;
        config_ = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        telemetryActions_ = factory_.createStoreTelemetry();
        eventActions_ = factory_.createRasEventLog(adapter_);
        bootImage_ = factory_.createBootImageApi(adapter_);
        operations_ = factory_.createAdapterOperations(adapter_);
        nodeInformation_ = factory_.createNodeInformation();
    }

    @Override
    public void initialize() {
        log_.debug("Initialized NetworkListenerSystemActions");
    }

    @Override
    public void storeNormalizedData(String dataType, String location, long nanoSecondsTimeStamp, double value) {
        // TODO: This method has no behavior currently, the target in the future will be Tier 3 DB.
    }

    @Override
    public void storeAggregatedData(String dataType, String location, long nanoSecondsTimeStamp, double min,
                                    double max, double average) {
        try {
            telemetryActions_.logEnvDataAggregated(dataType, location, nanoSecondsTimeStamp / 1_000L, max, min, average,
                    adapter_.getType(), adapter_.getBaseWorkItemId());
        } catch(DataStoreException e) {
            log_.exception(e, "Failed to store aggregated data");
        }
    }

    @Override
    public void storeRasEvent(String eventName, String instanceData, String location, long nsTimestamp) {
        String type;
        try {
            type = eventActions_.getRasEventType(eventName, adapter_.getBaseWorkItemId());
        } catch(IOException e) {
            log_.error("Failed to get event type from the event name: %s", eventName);
            log_.exception(e);
            return;
        }
        long usTimestamp = nsTimestamp / 1000L;
        try {
            if (location == null || location.isBlank() || nodeInformation_.isServiceNodeLocation(location))
                eventActions_.logRasEventNoEffectedJob(type, instanceData, location, usTimestamp,
                        adapter_.getType(), adapter_.getBaseWorkItemId());
            else
                eventActions_.logRasEventCheckForEffectedJob(type, instanceData, location, usTimestamp,
                        adapter_.getType(), adapter_.getBaseWorkItemId());
        } catch(DataStoreException e) {
            log_.exception(e);
            eventActions_.logRasEventCheckForEffectedJob(type, instanceData, location, usTimestamp,
                    adapter_.getType(), adapter_.getBaseWorkItemId());
        }
    }

    @Override
    public void publishNormalizedData(String topic, String dataType, String location, long nanoSecondsTimeStamp,
                                      double value) {
        try {
            setUpPublisher();
            if(publisher_ != null) {
                String message = formatRawMessage(dataType, location, nanoSecondsTimeStamp, value);
                publisher_.sendMessage(topic, message);
            }
        } catch(NetworkDataSourceFactory.FactoryException e) {
            log_.exception(e, "Failed to create the specified publishing data source from the configuration");
            log_.error("Publishing will be disabled in the class (" + getClass().getCanonicalName() + ") instance");
        }
    }

    @Override
    public void publishAggregatedData(String topic, String dataType, String location, long nanoSecondsTimeStamp,
                                      double min, double max, double average) {
        try {
            setUpPublisher();
            if(publisher_ != null) {
                String message = formatAggregateMessage(dataType, location, nanoSecondsTimeStamp, min, max, average);
                publisher_.sendMessage(topic, message);
            }
        } catch(NetworkDataSourceFactory.FactoryException e) {
            log_.exception(e, "Failed to create the specified publishing data source from the configuration");
            log_.error("Publishing will be disabled in the class (" + getClass().getCanonicalName() + ") instance");
        }
    }

    @Override
    public void publishRasEvent(String topic, String eventName, String instanceData, String location,
                                long nsTimestamp) {
        try {
            setUpPublisher();
            if(publisher_ != null) {
                String message = formatEventMessage(eventName, location, nsTimestamp, instanceData);
                publisher_.sendMessage(topic, message);
            }
        } catch(NetworkDataSourceFactory.FactoryException e) {
            log_.exception(e, "Failed to create the specified publishing data source from the configuration");
            log_.error("Publishing will be disabled in the class (" + getClass().getCanonicalName() + ") instance");
        }
    }

    @Override
    public void publishBootEvent(String topic, BootState event, String location, long nsTimestamp) {
        try {
            setUpPublisher();
            if(publisher_ != null) {
                String message = formatBootMessage(event, location, nsTimestamp);
                publisher_.sendMessage(topic, message);
            }
        } catch(NetworkDataSourceFactory.FactoryException e) {
            log_.exception(e, "Failed to create the specified publishing data source from the configuration");
            log_.error("Publishing will be disabled in the class (" + getClass().getCanonicalName() + ") instance");
        }
    }

    @Override
    public void changeNodeStateTo(BootState event, String location, long nsTimestamp, boolean informWlm) {
        operations_.markNodeState(event, location, nsTimestamp / 1000, informWlm);
    }

    @Override
    public void changeNodeBootImageId(String location, String id) {
        try {
            if(nodeInformation_.isComputeNodeLocation(location))
                bootImage_.updateComputeNodeBootImageId(location, id, adapter_.getType());
            else
                logFailedToUpdateNodeBootImageId(location,
                        "Service nodes are currently unsupported for this operation.");
        } catch(DataStoreException e) {
            log_.exception(e, "Failed to update the boot image ID for compute node location '%s'", location);
            logFailedToUpdateNodeBootImageId(location, "Compute node: " + e.getMessage());
        }
    }

    @Override
    public void upsertBootImages(List<Map<String,String>> bootImageInfoList) {
        for(Map<String,String> foreignEntry: bootImageInfoList) {
            Map<String,String> entry = translateForeignBootImageInfo(foreignEntry);
            try {
                bootImage_.editBootImageProfile(entry);
            } catch(DataStoreException e) {
                log_.exception(e, "Failed to update boot image info for %s", entry.get("id"));
            }
        }
    }

    @Override
    public void logFailedToUpdateNodeBootImageId(String location, String instanceData) {
        long ts = TimeUtils.nanosecondsToMicroseconds(TimeUtils.getNsTimestamp());
        eventActions_.logRasEventNoEffectedJob("1000000084", instanceData, location, ts, adapter_.getType(),
                adapter_.getBaseWorkItemId());
    }

    @Override
    public void logFailedToUpdateBootImageInfo(String instanceData) {
        long ts = TimeUtils.nanosecondsToMicroseconds(TimeUtils.getNsTimestamp());
        eventActions_.logRasEventNoEffectedJob("1000000085", instanceData, null, ts, adapter_.getType(),
                adapter_.getBaseWorkItemId());
    }

    @Override
    public void logFailedToUpdateWorkItemResults(String instanceData) {
        long ts = TimeUtils.nanosecondsToMicroseconds(TimeUtils.getNsTimestamp());
        eventActions_.logRasEventNoEffectedJob("0001000017", instanceData, null, ts, adapter_.getType(),
                adapter_.getBaseWorkItemId());
    }

    @Override
    public void close() throws IOException {
        if(publisher_ != null)
            publisher_.close();
    }

    private Map<String,String> translateForeignBootImageInfo(Map<String,String> entry) {
        return entry; // TODO: Write the REAL translation when foreign formats are known.
    }

    private String formatRawMessage(String dataType, String location, long nanoSecondsTimeStamp, double value) {
        PropertyMap map = new PropertyMap();
        map.put("type", dataType);
        map.put("location", location);
        map.put("timestamp", toISO(nanoSecondsTimeStamp));
        map.put("value", value);
        return parser_.toString(map);
    }

    private String formatAggregateMessage(String dataType, String location, long nanoSecondsTimeStamp,
                                          double min, double max, double average) {
        PropertyMap map = new PropertyMap();
        map.put("type", dataType);
        map.put("location", location);
        map.put("timestamp", toISO(nanoSecondsTimeStamp));
        map.put("minimum", min);
        map.put("maximum", max);
        map.put("average", average);
        return parser_.toString(map);
    }

    private String formatEventMessage(String eventName, String location, long nsTimestamp, String instanceData) {
        PropertyMap map = new PropertyMap();
        map.put("event", eventName);
        map.put("instanceData", instanceData);
        map.put("location", location);
        map.put("timestamp", toISO(nsTimestamp));
        return parser_.toString(map);
    }

    private String formatBootMessage(BootState event, String location, long nsTimestamp) {
        PropertyMap map = new PropertyMap();
        map.put("event", event.toString());
        map.put("location", location);
        map.put("timestamp", toISO(nsTimestamp));
        return parser_.toString(map);
    }

    private String toISO(long nanoSecondsTimeStamp) {
        return Instant.ofEpochSecond(nanoSecondsTimeStamp / 1000000000, nanoSecondsTimeStamp % 1000000000).
                toString().replace("T", " ");
    }

    private void setUpPublisher() throws NetworkDataSourceFactory.FactoryException {
        if(!publisherConfigured_) {
            publisherConfigured_ = true;
            Map<String, String> args = new HashMap<>();
            String publisherName = config_.getStringOrDefault("sourceType", null);
            if (publisherName == null || publisherName.isBlank())
                return;
            for (Map.Entry<String,Object> entry : config_.entrySet()) {
                Object value = config_.getOrDefault(entry, null);
                if (entry.getValue() != null)
                    args.put(entry.getKey(), entry.getValue().toString());
                else
                    args.put(entry.getKey(), null);
            }
            publisher_ = NetworkDataSourceFactory.createInstance(log_, publisherName, args);
            assert publisher_ != null : "Failed to create a NetworkDataSource object of type '" + publisherName + "'";
            publisher_.connect(null);
        }
    }

    private Logger log_;
    private ConfigIO parser_ = ConfigIOFactory.getInstance("json");
    private DataStoreFactory factory_;
    private StoreTelemetry telemetryActions_;
    private RasEventLog eventActions_;
    private BootImage bootImage_;
    private AdapterOperations operations_;
    private AdapterInformation adapter_;
    private final NodeInformation nodeInformation_;
    private PropertyMap config_;
    private NetworkDataSource publisher_ = null;
    private boolean publisherConfigured_ = false;
}
