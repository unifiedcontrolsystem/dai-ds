package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Description of class EventTypeTemplate
 * read/load event type template configuration data
 */
class EventTypeTemplate {

    EventTypeTemplate(final String templateConfigFile, final Logger log) {
        eventTemplateConfig_ = new EventTemplateConfig(templateConfigFile, log);
    }

    void setEventTemplateConfigFile(String file) throws SimulatorException {
        eventTemplateConfig_.setEventTemplateConfigFile(file);
        eventTemplateConfig_.loadTemplateData();
    }

    void validateEventNameAndType(String eventName, String eventType) throws SimulatorException, PropertyNotExpectedType {
        eventTemplateConfig_.validateEventNameAndType(eventName, eventType);
    }

    String getDefaultEventType(final String eventName) throws PropertyNotExpectedType, SimulatorException {
        eventTemplateConfig_.loadTemplateData();
        return eventTemplateConfig_.getDefaultEventType(eventName);
    }

    void loadData(String eventType) throws PropertyNotExpectedType {
        eventTypeTemplate_ = eventTemplateConfig_.getEventTypeTemplateConfig(eventType);
    }

    /**
     * Thsi method is used to get sample event type format data
     * @return return sample event data
     * @throws PropertyNotExpectedType unable to read/load data
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to read/load data
     */
    PropertyMap getEventTypeSingleTemplateData() throws PropertyNotExpectedType, IOException, ConfigIOParseException {
        String eventTypeTemplateFile = eventTypeTemplate_.getString(EVENT_TYPE_TEMP_CONFIG[0]);
        return loadDataFromFile(eventTypeTemplateFile);
    }

    /**
     * This method is used to return stream name to send data to network
     * @return stream name
     * @throws PropertyNotExpectedType unable to read/load daata
     */
    String getEventTypeStreamName() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getString(EVENT_TYPE_TEMP_CONFIG[1]);
    }

    /**
     * This method is used to load field-filters info for single sample event
     * @return field-filters data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getFiltersForSingleTemplate() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getMap(EVENT_TYPE_TEMP_CONFIG[2]);
    }

    /**
     * This method is used to load count-filters info for single sample event
     * @return count-filter data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getFiltersForSingleTemplateCount() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getMap(EVENT_TYPE_TEMP_CONFIG[3]);
    }

    /**
     * This method is used to load field metadat info for single sample event
     * @return fields metadata info
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getUpdateFieldsInfoWithMetada() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getMap(EVENT_TYPE_TEMP_CONFIG[4]);
    }

    /**
     * This method is used to load final path where to generate data and its respective overflow path
     * for single sample event
     * @return generate data path and overflow path
     * @throws PropertyNotExpectedType unable to read/load data
     */
    Map<String, Object> getPathToGenerateDataAndOverFlowInfo() throws PropertyNotExpectedType {
        PropertyMap data =  eventTypeTemplate_.getMap(EVENT_TYPE_TEMP_CONFIG[6]);
        dataDscByKeyLen_.clear();
        dataDscByKeyLen_.putAll(data);
        return dataDscByKeyLen_;
    }

    /**
     * This method is used to load restrict rate-count info
     * @return rate-count data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    Map<String, Object> getJPathCounterInfo() throws PropertyNotExpectedType {
        PropertyMap data =  eventTypeTemplate_.getMap(EVENT_TYPE_TEMP_CONFIG[5]);
        dataAscByKeyLen_.clear();
        dataAscByKeyLen_.putAll(data);
        return dataAscByKeyLen_;
    }

    /**
     * This method is used to return path where we need to update timestamp
     * @return path to timestamp
     * @throws PropertyNotExpectedType unable to read/load data
     */
    String getPathToUpdateTimestamp() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getString(EVENT_TYPE_TEMP_CONFIG[7]);
    }

    /**
     * This method is used to update sample event format file
     * @param eventTypeTemplateFile file path
     */
    void setEventTypeTemplateFile(final String eventTypeTemplateFile) {
        if(eventTypeTemplateFile != null)
            eventTypeTemplate_.put(EVENT_TYPE_TEMP_CONFIG[0], eventTypeTemplateFile);
    }

    /**
     * This method is used read/load file data
     * @param metadataFile file path
     * @return return data in file
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to read/load data
     */
    private PropertyMap loadDataFromFile(String metadataFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(metadataFile).getAsMap();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(metadataFile).getAsMap();
        }
    }

    private EventTemplateConfig eventTemplateConfig_;

    private PropertyMap eventTypeTemplate_;

    private final String[] EVENT_TYPE_TEMP_CONFIG = {"template", "stream-type", "single-template", "single-template-count", "update-fields",
                                                    "path-count", "generate-data-and-overflow-path", "timestamp"};

    private final String MISSING_EVENT_TYPE_TEMP_CONFIG = "Event type template configuration is missing required " +
            "fields, fields =";

    private Map<String, Object> dataDscByKeyLen_ = new TreeMap<String, Object>(
            new Comparator<String>() {
                @Override
                public int compare(String a1, String a2) {
                    return Integer.compare(a2.length(), a1.length());
                }

            }
    );

    private Map<String, Object> dataAscByKeyLen_ = new TreeMap<String, Object>(
            new Comparator<String>() {
                @Override
                public int compare(String a1, String a2) {
                    return Integer.compare(a1.length(), a2.length());
                }

            }
    );
}