// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Description of class RasEventTransformer.
 */
public class DemoEventProviderForeignBus implements NetworkListenerProvider, Initializer {
    public DemoEventProviderForeignBus(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                DemoEventProviderForeignBus.class.getCanonicalName());
    }

    @Override
    public void initialize() {
    }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        List<CommonDataFormat> commonList = new ArrayList<>();
        PropertyMap document;
        try {
            document = parser_.fromString(data).getAsMap();
        } catch(ConfigIOParseException e) {
            log_.exception(e);
            return commonList;
        }
        if(!document.containsKey("metrics") || document.get("metrics") == null)
            throw new NetworkListenerProviderException("Missing the 'metric' top level key.");
        PropertyMap metrics = document.getMapOrDefault("metrics", null); // Cannot return null at this point.
        if(!metrics.containsKey("messages") || metrics.get("messages") == null)
            throw new NetworkListenerProviderException("Missing the second level 'messages' key.");
        PropertyArray messages = metrics.getArrayOrDefault("messages", null); // cannot return null at this point.
        for(Object obj: messages) {
            if(obj instanceof PropertyMap) {
                PropertyMap map = (PropertyMap)obj;
                if(checkMessage(map)) {
                    log_.debug("*** Processing data item...");
                    try {
                        // The 2 values from the map cannot be null after the call to checkMessage.
                        String timestamp = map.getString("timereported").replace("T", " ");
                        long nsTimestamp = CommonFunctions.convertISOToLongTimestamp(timestamp);
                        String logMessage = map.getString("message");
                        CommonDataFormat dataObj = new CommonDataFormat(nsTimestamp, null, DataType.RasEvent);
                        dataObj.setRasEvent("RasUnknownEvent", logMessage);
                        commonList.add(dataObj);
                    } catch(PropertyNotExpectedType | ParseException e) {
                        log_.exception(e);
                    }
                }
            }
        }
        return commonList;
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        if(!configured_) getConfig(config);
        systemActions.storeRasEvent(data.getRasEvent(), data.getRasEventPayload(), data.getLocation(),
                data.getNanoSecondTimestamp());
        if(publish_) {
            systemActions.publishRasEvent(topic_, data.getRasEvent(), data.getRasEventPayload(),
                    data.getLocation(), data.getNanoSecondTimestamp());
        }
    }

    private void getConfig(NetworkListenerConfig config) {
        configured_ = true;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", false);
            topic_ = map.getStringOrDefault("publishTopic", topic_);
        }
    }

    private boolean checkMessage(PropertyMap message) {
        boolean result = true;
        for(String required: requiredInMessage_)
            if (!message.containsKey(required) || message.get(required) == null) {
                result = false;
                break;
            }
        return result;
    }

    private boolean publish_ = false;
    private String topic_ = "ucs_ras_event";
    private boolean configured_ = false;
    private Logger log_;
    private ConfigIO parser_;
    private static final String[] requiredInMessage_ = new String[] {"message", "timereported"};
}
