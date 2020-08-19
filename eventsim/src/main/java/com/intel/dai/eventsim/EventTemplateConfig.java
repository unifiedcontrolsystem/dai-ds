package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.util.Arrays;

/**
 * Description of class EventTemplateConfig.
 * This class is used to read/load event template configuration file
 */
class EventTemplateConfig {

    EventTemplateConfig(final String templateConfigFile, final Logger log) {
        templateConfigFile_ = templateConfigFile;
        log_ = log;
    }

    void setEventTemplateConfigFile(String file) { templateConfigFile_ = file; }

    /**
     * This method is used to validate name and type exists in configuration file
     * @throws SimulatorException unable to read/load data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    void validateEventNameAndType(String eventName, String eventType) throws SimulatorException, PropertyNotExpectedType {
        if(!events_.keySet().contains(eventName) || !events_.getMap(eventName).keySet().containsAll(Arrays.asList(SINGLE_EVENT_INFO))
                || !events_.getMap(eventName).getArray(SINGLE_EVENT_INFO[1]).contains(eventType))
            throw new SimulatorException("given event-name/event-type is missing in event template, event =" + eventName);
        if(!eventTypesConfig_.keySet().contains(eventType))
            throw new SimulatorException("given event-types configuration is missing, event-type = " + eventType);
            log_.info("event-type-configurations data is loaded successfully for given event-name = " +
                    eventName + ", event-type = " + eventType);
    }

    /**
     * This method is used to get event type template configuration data
     * @return event type template configuration data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getEventTypeTemplateConfig(String eventType) throws PropertyNotExpectedType {
        return new PropertyMap(eventTypesConfig_.getMap(eventType));
    }

    /**
     * This method is used to load default event type for a given event name
     * @param eventName name of event
     * @return default event type data
     * @throws PropertyNotExpectedType no default event type for a event
     */
    String getDefaultEventType(final String eventName) throws PropertyNotExpectedType {
        return events_.getMap(eventName).getString(SINGLE_EVENT_INFO[0]);
    }

    /**
     * This method is used to load event details and events type details
     * @throws SimulatorException unable to read/load data
     */
    void loadTemplateData() throws SimulatorException {
        try {
            templateConfig_ = LoadFileLocation.fromFileLocation(templateConfigFile_).getAsMap();
            DataValidation.validateKeys(templateConfig_, TEMPLATE_CONFIG, MISSING_TEMP_CONFIG);
            events_ = templateConfig_.getMap(TEMPLATE_CONFIG[0]);
            eventTypesConfig_ = templateConfig_.getMap(TEMPLATE_CONFIG[1]);
        } catch (ConfigIOParseException | IOException | PropertyNotExpectedType e) {
            throw new SimulatorException(e.getMessage());
        }
        log_.info("events-template configurations data is loaded successfully");
    }

/*    void setEventName(String eventName) { eventName_ = eventName; }
    void setEventType(String eventType) { eventType_ = eventType; }

    String getEventName() { return eventName_; }
    String getEventType() { return eventType_; }*/

    private PropertyMap events_;
    private PropertyMap eventTypesConfig_;

    private String templateConfigFile_;
    private String eventName_;
    private String eventType_;

    private PropertyMap templateConfig_ = new PropertyMap();

    private final Logger log_;

    private String[] TEMPLATE_CONFIG = {"event", "event-types"};
    private String[] SINGLE_EVENT_INFO = {"default", "types"};
    private String MISSING_TEMP_CONFIG = "template configuration is missing required field, field = ";
}