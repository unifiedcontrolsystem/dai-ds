package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
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

    EventTypeTemplate(final Logger log) {
        log_ = log;
    }

    /**
     * This method is used to update sample events configuration template file
     * @param eventsConfigTemplateFile file path
     */
    void setEventsConfigTemplateFile(final String eventsConfigTemplateFile, final String eventType) throws SimulatorException, PropertyNotExpectedType {
        if(eventsConfigTemplateFile_ != eventsConfigTemplateFile) {
            loadEventsConfigTemplateData(eventsConfigTemplateFile);
            validateEventName(eventType);
        }
        if(eventType_ != eventType)
            validateEventName(eventType);
    }

     private void loadEventsConfigTemplateData(final String eventsConfigTemplateFile) throws SimulatorException {
        try {
            eventsConfigTemplateFile_ = eventsConfigTemplateFile;
            eventsConfigTemplateData_ = loadDataFromFile(eventsConfigTemplateFile_);
            if(!eventsConfigTemplateData_.containsKey(EVENT_TYPES))
                throw new SimulatorException("");
            eventsConfigTemplateData_ = eventsConfigTemplateData_.getMap(EVENT_TYPES);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void validateEventName(final String eventName) throws SimulatorException, PropertyNotExpectedType {
        DataValidation.validateKey(eventsConfigTemplateData_, eventName, MISSING_KEY);
        eventTypeConfigTemplate_ = eventsConfigTemplateData_.getMap(eventName);
    }

    public void setJPathFieldFiltersConfig(PropertyArray updateJPathFieldFilters_) {

    }

    /**
     * Thsi method is used to get sample event type format data
     * @return return sample event data
     * @throws PropertyNotExpectedType unable to read/load data
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to read/load data
     */
    PropertyMap getEventTypeSingleTemplateData() throws PropertyNotExpectedType, IOException, ConfigIOParseException {
        final String eventTypeTemplateFile = eventTypeConfigTemplate_.getString(MESSAGE_TEMPLATE_FILE);
        return loadDataFromFile(eventTypeTemplateFile);
    }

    /**
     * This method is used to return stream name to send data to network
     * @return stream name
     * @throws PropertyNotExpectedType unable to read/load daata
     */
    String getEventTypeStreamName() throws PropertyNotExpectedType {
        return eventTypeConfigTemplate_.getString(STREAM_TYPE);
    }

    String getEventTypeStreamId() throws PropertyNotExpectedType, IOException, ConfigIOParseException {
        final Pattern fileExtnPtrn = Pattern.compile("([^\\s]+(\\.(?i)(avsc|txt))$)");
        String streamId =  eventTypeConfigTemplate_.getString(STREAM_ID);
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
    PropertyMap getFiltersForSingleTemplate() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, SINGLE_MESSAGE_TEMPLATE, MISSING_KEY);
        return eventTypeConfigTemplate_.getMap(SINGLE_MESSAGE_TEMPLATE);
    }

    /**
     * This method is used to load count-filters info for single sample event
     * @return count-filter data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getFiltersForSingleTemplateCount() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, SINGLE_MESSAGE_TEMP_COUNT, MISSING_KEY);
        return eventTypeConfigTemplate_.getMap(SINGLE_MESSAGE_TEMP_COUNT);
    }

    /**
     * This method is used to load field metadat info for single sample event
     * @return fields metadata info
     * @throws PropertyNotExpectedType unable to read/load data
     */
    PropertyMap getUpdateFieldsInfoWithMetada() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, UPDATE_FIELDS, MISSING_KEY);
        return eventTypeConfigTemplate_.getMap(UPDATE_FIELDS);
    }

    boolean isUpdateTemplateRequired() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, UPDATE_TEMPLATE, MISSING_KEY);
        return eventTypeConfigTemplate_.getBoolean(UPDATE_TEMPLATE);
    }

    /**
     * This method is used to load final path where to generate data and its respective overflow path
     * for single sample event
     * @return generate data path and overflow path
     * @throws PropertyNotExpectedType unable to read/load data
     */
    Map<String, Object> getPathToGenerateDataAndOverFlowInfo() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, GEN_DATA_OVERFLOW_PATH, MISSING_KEY);
        final PropertyMap data =  eventTypeConfigTemplate_.getMap(GEN_DATA_OVERFLOW_PATH);
        dataDscByKeyLen_.clear();
        dataDscByKeyLen_.putAll(data);
        return dataDscByKeyLen_;
    }

    /**
     * This method is used to load restrict rate-count info
     * @return rate-count data
     * @throws PropertyNotExpectedType unable to read/load data
     */
    Map<String, Object> getJPathCounterInfo() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, PATH_COUNT, MISSING_KEY);
        final PropertyMap data =  eventTypeConfigTemplate_.getMap(PATH_COUNT);
        dataAscByKeyLen_.clear();
        dataAscByKeyLen_.putAll(data);
        return dataAscByKeyLen_;
    }

    /**
     * This method is used to return path where we need to update timestamp
     * @return path to timestamp
     * @throws PropertyNotExpectedType unable to read/load data
     */
    String getPathToUpdateTimestamp() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, TIMESTAMP_PATH, MISSING_KEY);
        return eventTypeConfigTemplate_.getString(TIMESTAMP_PATH);
    }

    /**
     * This method is used to return how timestamp data should be like (long or date)
     * @return timestamp type
     * @throws PropertyNotExpectedType unable to read/load data
     */
    String getPathToUpdateTimestampType() throws PropertyNotExpectedType, SimulatorException {
        DataValidation.validateKey(eventTypeConfigTemplate_, TIMESTAMP_TYPE, MISSING_KEY);
        return eventTypeConfigTemplate_.getString(TIMESTAMP_TYPE);
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

    private PropertyMap eventsConfigTemplateData_;
    private PropertyMap eventTypeConfigTemplate_;
    private PropertyMap eventTy;
    private String eventsConfigTemplateFile_ = null;
    private String eventType_ = null;

    private final Logger log_;

    private static final String MISSING_KEY = "given key/data is null, key = ";
    private static final String EVENT_TYPES = "event-types";
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