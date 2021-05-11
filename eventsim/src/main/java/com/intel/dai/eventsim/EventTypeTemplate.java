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
import java.util.regex.Pattern;

/**
 * Description of class EventTypeTemplate
 * read/load event type template configuration data
 */
class EventTypeTemplate {

    EventTypeTemplate(final String templateConfigFile, final Logger log) {
        eventTemplateConfig_ = new EventTemplateConfig(templateConfigFile, log);
    }

    void validateEventNameAndType(final String eventName, final String eventType) throws SimulatorException,
            PropertyNotExpectedType {
        eventTemplateConfig_.validateEventNameAndType(eventName, eventType);
    }

    String getDefaultEventType(final String eventName) throws PropertyNotExpectedType, SimulatorException {
        eventTemplateConfig_.loadTemplateData();
        return eventTemplateConfig_.getDefaultEventType(eventName);
    }

    void loadData(final String eventType) throws PropertyNotExpectedType {
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
        final String eventTypeTemplateFile = eventTypeTemplate_.getString(MESSAGE_TEMPLATE_FILE);
        return loadDataFromFile(eventTypeTemplateFile);
    }

    /**
     * This method is used to return stream name to send data to network
     * @return stream name
     * @throws PropertyNotExpectedType unable to read/load daata
     */
    String getEventTypeStreamName() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getString(STREAM_TYPE);
    }

    String getEventTypeStreamId() throws PropertyNotExpectedType, IOException, ConfigIOParseException {
        final Pattern fileExtnPtrn = Pattern.compile("([^\\s]+(\\.(?i)(avsc|txt))$)");
        String streamId =  eventTypeTemplate_.getString(STREAM_ID);
        boolean isFile = fileExtnPtrn.matcher(streamId).matches();
        if(isFile)
            return loadDataFromFile(streamId).toString();
        return streamId;
    }

    /**
     * This method is used to load field-filters info for single sample event
     * @return field-filters data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getFiltersForSingleTemplate() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getMap(SINGLE_MESSAGE_TEMPLATE);
    }

    /**
     * This method is used to load count-filters info for single sample event
     * @return count-filter data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getFiltersForSingleTemplateCount() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getMap(SINGLE_MESSAGE_TEMP_COUNT);
    }

    /**
     * This method is used to load field metadat info for single sample event
     * @return fields metadata info
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getUpdateFieldsInfoWithMetada() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getMap(UPDATE_FIELDS);
    }

    boolean isUpdateTemplateRequired() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getBoolean(UPDATE_TEMPLATE);
    }

    /**
     * This method is used to load final path where to generate data and its respective overflow path
     * for single sample event
     * @return generate data path and overflow path
     * @throws PropertyNotExpectedType unable to read/load data
     */
    Map<String, Object> getPathToGenerateDataAndOverFlowInfo() throws PropertyNotExpectedType {
        final PropertyMap data =  eventTypeTemplate_.getMap(GEN_DATA_OVERFLOW_PATH);
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
        final PropertyMap data =  eventTypeTemplate_.getMap(PATH_COUNT);
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
        return eventTypeTemplate_.getString(TIMESTAMP_PATH);
    }

    /**
     * This method is used to return how timestamp data should be like (long or date)
     * @return timestamp type
     * @throws PropertyNotExpectedType unable to read/load data
     */
    String getPathToUpdateTimestampType() throws PropertyNotExpectedType {
        return eventTypeTemplate_.getString(TIMESTAMP_TYPE);
    }

    /**
     * This method is used to update sample event format file
     * @param eventTypeTemplateFile file path
     */
    void setEventTypeTemplateFile(final String eventTypeTemplateFile) {
        if(eventTypeTemplateFile != null)
            eventTypeTemplate_.put(MESSAGE_TEMPLATE_FILE, eventTypeTemplateFile);
    }

    /**
     * This method is used read/load file data
     * @param metadataFile file path
     * @return return data in file
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to read/load data
     */
    private PropertyMap loadDataFromFile(final String metadataFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(metadataFile).getAsMap();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(metadataFile).getAsMap();
        }
    }

    private EventTemplateConfig eventTemplateConfig_;

    private PropertyMap eventTypeTemplate_;

    private static final String MESSAGE_TEMPLATE_FILE = "template";
    private static final String STREAM_ID = "stream-id";
    private static final String STREAM_TYPE = "stream-type";
    private static final String SINGLE_MESSAGE_TEMPLATE = "single-template";
    private static final String SINGLE_MESSAGE_TEMP_COUNT = "single-template-count";
    private static final String UPDATE_FIELDS = "update-fields";
    private static final String UPDATE_TEMPLATE = "update-template";
    private static final String PATH_COUNT = "path-count";
    private static final String GEN_DATA_OVERFLOW_PATH = "generate-data-and-overflow-path";
    private static final String TIMESTAMP_PATH = "timestamp";
    private static final String TIMESTAMP_TYPE = "timestamp-type";

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