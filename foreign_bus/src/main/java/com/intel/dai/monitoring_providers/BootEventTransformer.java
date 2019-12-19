// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring_providers;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.dsapi.BootState;
import com.intel.logging.Logger;
import com.intel.partitioned_monitor.*;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of class BootEventTransformer.
 */
public class BootEventTransformer implements DataTransformer, Initializer {
    public BootEventTransformer(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null)
            throw new RuntimeException("Failed to create a JSON parser for class" + getClass().getCanonicalName());
    }

    @Override
    public void initialize() { /* Not used but is required */ }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, PartitionedMonitorConfig config)
            throws DataTransformerException {
        try {
            PropertyMap message = parser_.fromString(data).getAsMap();
            log_.debug("*** Message: %s", data);
            checkMessage(message);
            String[] xnameLocations = message.getStringOrDefault(FOREIGN_LOCATION_KEY, "").split(",");
            String eventType = message.getStringOrDefault(FOREIGN_EVENT_TYPE_KEY, null);
            if(!conversionMap_.containsKey(eventType))
                throw new DataTransformerException("The '" + FOREIGN_EVENT_TYPE_KEY + "' boot state field in JSON is " +
                        "not a known name: " + eventType);
            BootState ucsEvent = conversionMap_.get(eventType);
            List<CommonDataFormat> commonList = new ArrayList<>();
            for(String xnameLocation: xnameLocations) {
                String location;
                location = CommonFunctions.convertXNameToLocation(xnameLocation);
                CommonDataFormat common = new CommonDataFormat(
                        CommonFunctions.convertISOToLongTimestamp(message.getString(FOREIGN_TIMESTAMP_KEY)), location,
                        DataType.StateChangeEvent);
                common.setStateChangeEvent(ucsEvent);
                commonList.add(common);
                if(common.getStateEvent() == BootState.NODE_ONLINE) {
                    // This is a contract with the BootEventActions class.
                    common.storeExtraData(ORIG_FOREIGN_LOCATION_KEY, xnameLocation);
                    common.storeExtraData(FOREIGN_IMAGE_ID_KEY, extractBootImageId(message, xnameLocation));
                }
            }
            return commonList;
        } catch(ConfigIOParseException | PropertyNotExpectedType | ParseException e) {
            throw new DataTransformerException("Failed to parse the event from the component");
        }
    }

    private String extractBootImageId(PropertyMap message, String xnameLocation) throws DataTransformerException {
        // TODO: Coded assuming the FOREIGN_IMAGE_ID_KEY is in the NODE_ONLINE message.
        // NOTE: The foreign boot image ID will be the DAI boot image id.
        return message.getStringOrDefault(FOREIGN_IMAGE_ID_KEY, "");
    }

    private void checkMessage(PropertyMap message) throws DataTransformerException {
        for(String required: requiredInMessage_)
            if (!message.containsKey(required))
                throw new DataTransformerException(String.format("Incoming event was missing '%s' in the JSON",
                        required));
    }

    private Logger log_;
    private ConfigIO parser_;
    private static final String[] requiredInMessage_ = new String[] {"event-type", "timestamp", "location"};
    private final static Map<String, BootState> conversionMap_ = new HashMap<>() {{
        put("ec_node_available", BootState.NODE_ONLINE);
        put("ec_node_unavailable", BootState.NODE_OFFLINE);
        put("ec_node_halt", BootState.NODE_OFFLINE);
        put("ec_node_failed", BootState.NODE_OFFLINE);
        put("ec_boot", BootState.NODE_BOOTING);
    }};

    private final static String FOREIGN_LOCATION_KEY = "location";
    private final static String ORIG_FOREIGN_LOCATION_KEY = "xnameLocation";
    private final static String FOREIGN_EVENT_TYPE_KEY = "event-type";
    private final static String FOREIGN_IMAGE_ID_KEY = "bootImageId";
    private final static String FOREIGN_TIMESTAMP_KEY = "timestamp";
}
