package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.runtime_utils.TimeUtils;

import java.util.List;

public class TopicEventSyslog extends TopicBaseProcessor {

    TopicEventSyslog(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        if(!map.isEmpty()) {
            for(String requiredKey : REQ_KEYS) {
                if(!map.containsKey(requiredKey)) {
                    log_.error("Missing required argument in SYSLOG data: '" + requiredKey + "'");
                    return;
                }
            }

            try {
                final long timestamp = TimeUtils.nSFromIso8601(map.getString(TIMESTAMP));
                final PropertyMap hostInfoMap = map.getMapOrDefault(HOST, new PropertyMap());
                final String location = hostInfoMap.getStringOrDefault(NAME, "");
                final PropertyMap syslogDataMap = map.getMapOrDefault(data.topic.toLowerCase(), new PropertyMap());
                final long priority = syslogDataMap.getLongOrDefault(PRIORITY, 3);
                final boolean triggerEvent = triggerEvent(priority);
                if(triggerEvent)
                    addToResults(data.topic, RAS_EVENT_NAME, data.originalJsonText, location, timestamp, results);
            } catch (Exception e) {
                log_.warn("Error: %s", e.getMessage());
            }
        }
    }

    private boolean triggerEvent(final long priority) {
        if(priority >=0 && priority <= 3)
            return true;
        return false;
    }

    private final static String TIMESTAMP = "@timestamp";
    private final static String PRIORITY = "priority";
    private final static String HOST = "host";
    private final static String NAME = "name";
    private final static String SYSLOG = "syslog";
    private final static String RAS_EVENT_NAME = "RasHPCMSYSLOG";

    private final static String[] REQ_KEYS = {TIMESTAMP, SYSLOG, HOST};
}
