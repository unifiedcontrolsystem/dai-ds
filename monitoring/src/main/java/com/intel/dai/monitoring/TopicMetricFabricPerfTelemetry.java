package com.intel.dai.monitoring;

import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.runtime_utils.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicMetricFabricPerfTelemetry extends TopicBaseProcessor {
    TopicMetricFabricPerfTelemetry(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(final EnvelopeData data, final PropertyMap map, final List<CommonDataFormat> results) {
        final PropertyMap fields = map.getMapOrDefault("fields", new PropertyMap());
        if(!fields.isEmpty()) {
            for(String requiredKey : REQ_KEYS) {
                if(!fields.containsKey(requiredKey)) {
                    log_.error("Missing required argument in FabricPerfTelemetry data: '" + requiredKey + "'");
                    return;
                }
            }

            try {
                final String name = fields.getStringOrDefault(NAME, "");
                final String foreignLocation = fields.getStringOrDefault(LOCATION, "");
                final String location = CommonFunctions.convertForeignToLocation(foreignLocation);
                final long value = fields.getLongOrDefault(VALUE, 0);
                final long timestamp = TimeUtils.nSFromIso8601(fields.getString(TIMESTAMP));
                final String type = nameToType_.getOrDefault(name, BW_TYPE);
                final String units = typeToUnits_.getOrDefault(type, BW_UNITS);
                addToResults(name, value, units, type, timestamp, location, results);
            } catch (Exception e) {
                log_.warn("cannot find xname location: %s", e.getMessage());
            }
        }
    }

    private final static String NAME = "PhysicalContext";
    private final static String LOCATION = "Location";
    private final static String VALUE = "Value";
    private final static String TIMESTAMP = "Timestamp";
    private final static String BW_UNITS = "bps";
    private final static String BW_TYPE = "BANDWIDTH";

    private final static String[] REQ_KEYS = {NAME, LOCATION, VALUE};

    private final static Map<String,String> nameToType_ = new HashMap<>() {{
        put("Congestion.txBW", BW_TYPE);
        put("Congestion.rxBW", BW_TYPE);
    }};

    private final static Map<String,String> typeToUnits_ = new HashMap<>() {{
        put(BW_TYPE, BW_UNITS);
    }};
}
