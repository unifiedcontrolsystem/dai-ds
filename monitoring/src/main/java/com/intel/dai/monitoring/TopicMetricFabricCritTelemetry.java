package com.intel.dai.monitoring;

import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.runtime_utils.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicMetricFabricCritTelemetry extends TopicBaseProcessor {
    TopicMetricFabricCritTelemetry(final Logger log, final boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(final EnvelopeData data, final PropertyMap map, final List<CommonDataFormat> results) {
        rasEventName_ = "";
        final PropertyMap fields = map.getMapOrDefault("fields", new PropertyMap());
        if(!fields.isEmpty()) {
            for(String requiredKey : REQ_KEYS) {
                if(!fields.containsKey(requiredKey)) {
                    log_.error("Missing required argument in FabricCritTelemetry data: '" + requiredKey + "'");
                    return;
                }
            }

            try {
                final String name = fields.getStringOrDefault(NAME, "");
                final String foreignLocation = fields.getStringOrDefault(LOCATION, "");
                final String location = CommonFunctions.convertForeignToLocation(foreignLocation);
                final long value = fields.getLongOrDefault(VALUE, 0);
                final long timestamp = TimeUtils.nSFromIso8601(fields.getString(TIMESTAMP));
                final boolean isEvent = isEvent(name);
                if(isEvent)
                    addToResults(name, rasEventName_, data.originalJsonText, location, timestamp, results);
                else
                    addToResults(name, value, UNITS, TYPE, timestamp, location, results);
            } catch (Exception e) {
                log_.warn("cannot find xname location: %s", e.getMessage());
            }
        }
    }

    private boolean isEvent(final String name) {
        for(String key : nameToRasEvent_.keySet()) {
            if(name.contains(key)) {
                rasEventName_ = nameToRasEvent_.get(key);
                return true;
            }
        }
        return false;
    }

    private final static String NAME = "PhysicalContext";
    private final static String LOCATION = "Location";
    private final static String TIMESTAMP = "Timestamp";
    private final static String VALUE = "Value";
    private final static String UNITS = "";
    private final static String TYPE = "";
    private static String rasEventName_;

    private final static String[] REQ_KEYS = {NAME, LOCATION, TIMESTAMP};

    private final static Map<String,String> nameToRasEvent_ = new HashMap<>() {{
        put("RoutingErrors", "RasCrayFabricCritTelemetry");
    }};
}
