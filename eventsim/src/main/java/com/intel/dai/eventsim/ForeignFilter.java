package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class ForeignFilter {

    ForeignFilter(JsonPath jsonPath, Logger log) {
        jsonPath_ = jsonPath;
        log_ = log;
        generator_ = new ForeignDataGenerator(jsonPath_, log_);
    }

    /**
     * This method is used to load locations matchingregex data
     * @param locations node locations
     * @param locationsRegex location-regex-data
     * @throws SimulatorException no locations matching data
     * @throws ConversionException no locations matching data
     */
    void validateLocations(List<String> locations, String locationsRegex) throws SimulatorException, ConversionException {
        filteredLocations_.clear();
        for(String key : locations) {
            if(key.matches(locationsRegex))
                filteredLocations_.add(CommonFunctions.convertLocationToForeign(key));
        }
        if(filteredLocations_.isEmpty())
            throw new SimulatorException("No matched locations available in database," +
                    " location-regex = " + locationsRegex);
        log_.info("Available locations matching given regex is filtered, location-regex = ", locationsRegex);
    }

    /**
     * This method is used to generate events for event type template, count
     * @param eventTypeTemplate event type template info
     * @param count number of events to be generated
     * @param seed random seed to replicate data
     * @return events
     * @throws PropertyNotExpectedType unable to create events
     * @throws IOException unable to create events
     * @throws ConfigIOParseException unable to create events
     * @throws SimulatorException unable to create events
     */
    PropertyDocument generateEvents(EventTypeTemplate eventTypeTemplate, long count, long seed) throws PropertyNotExpectedType, IOException, ConfigIOParseException, SimulatorException {
        PropertyMap updateJPathField = eventTypeTemplate.getUpdateFieldsInfoWithMetada();
        PropertyMap updateJPathWithMetadata = new PropertyMap();
        loadMetadataToUpdateJPathFields(updateJPathField, updateJPathWithMetadata);

        PropertyMap templateData = eventTypeTemplate.getEventTypeSingleTemplateData();
        PropertyMap sampleTemplateData = new PropertyMap(templateData);

        PropertyMap filtersForSingleTemplate = eventTypeTemplate.getFiltersForSingleTemplate();
        filterEventTypeTemplateData(templateData, filtersForSingleTemplate);

        PropertyMap jPathFilterCounterInfo = eventTypeTemplate.getFiltersForSingleTemplateCount();
        applyCounterToEventTypeTemplateData(templateData, jPathFilterCounterInfo, seed);

        Map<String, Object> overFlowJPath = eventTypeTemplate.getPathToGenerateDataAndOverFlowInfo();

        String pathToGenerateData = overFlowJPath.keySet().toArray()[0].toString();
        generateEvents(pathToGenerateData,templateData, updateJPathWithMetadata, count, seed);

        Map<String, Object> jPathCounterInfo = eventTypeTemplate.getJPathCounterInfo();
        applyCounterToEventTypeTemplateData(sampleTemplateData, jPathCounterInfo, seed);

        return generateEventsFormatUsingTemplate(templateData, sampleTemplateData, overFlowJPath, seed);
    }

    private PropertyDocument generateEventsFormatUsingTemplate(final PropertyMap templateData,
                                                               final PropertyMap singleTemplateData,
                                                               final Map<String, Object> overFlowJPath,
                                                               final long seed) throws PropertyNotExpectedType {
        return generator_.generateEventsFormatUsingTemplate(templateData, singleTemplateData, overFlowJPath, seed);
    }

    /**
     * This method is used to generate data for a given count at generate data path
     * @param path generate data path
     * @param templateData data
     * @param updateJPathField update-field-values
     * @param count number of events to be generated
     * @param seed random seed to replicate data
     * @throws PropertyNotExpectedType unable to generate events
     */
    private void generateEvents(final String path, PropertyMap templateData, final PropertyMap updateJPathField,
                                long count, long seed) throws PropertyNotExpectedType {
        generator_.generateEventsUsingTemplate(path, templateData, updateJPathField, count, seed);
    }

    /**
     * This method is used to apply counter on jpaths
     * @param templateData data
     * @param jPathCounterInfo jpsth info
     * @param seed random seed
     * @throws PropertyNotExpectedType unabel to apply counters
     */
    private void applyCounterToEventTypeTemplateData(PropertyMap templateData, final Map<String, Object> jPathCounterInfo,
                                                     final long seed) throws PropertyNotExpectedType {
        generator_.applyCounterToEventTypeTemplateData(templateData, jPathCounterInfo, seed);
    }

    /**
     * This methdo is used to filter template by feild values
     * @param templateData data
     * @param filtersForSingleTemplate filter fields
     * @throws PropertyNotExpectedType unable to filter data
     */
    private void filterEventTypeTemplateData(PropertyMap templateData, final PropertyMap filtersForSingleTemplate) throws PropertyNotExpectedType {
        generator_.filterEventTypeTemplateData(templateData, filtersForSingleTemplate);
    }

    /**
     * This method is load field with possible values
     * @param updateJPathField field names jpath
     * @param updateJPathWithMetadata possible jpath field values
     * @throws PropertyNotExpectedType unable to load metadata possible values
     * @throws IOException unable to load metadata possible values
     * @throws ConfigIOParseException unable to load metadata possible values
     * @throws SimulatorException unable to load metadata possible values
     */
    private void loadMetadataToUpdateJPathFields(final PropertyMap updateJPathField,
                                                 final PropertyMap updateJPathWithMetadata)
            throws PropertyNotExpectedType, IOException, ConfigIOParseException, SimulatorException {
        for(Map.Entry<String, Object> jpath : updateJPathField.entrySet()) {
            String path = jpath.getKey();
            PropertyMap fieldsMetadataInfo = (PropertyMap) jpath.getValue();

            PropertyMap pathAndValue = new PropertyMap();
            for(Map.Entry<String, Object> field : fieldsMetadataInfo.entrySet()) {
                String fieldName = field.getKey();
                PropertyMap metadataInfo = (PropertyMap) field.getValue();

                String metadataFile = metadataInfo.getString("metadata");
                Object metadataFilter = metadataInfo.get("metadata-filter");
                PropertyArray metadata = filterDataWithValue(metadataFile, metadataFilter);

                pathAndValue.put(fieldName, metadata);
            }

            updateJPathWithMetadata.put(path, pathAndValue);
        }
    }

    /**
     * This method is used to load possible values matching filter
     * @param metadataFile file with possible values
     * @param metadataFilter filter metadat values
     * @return filters metadata values
     * @throws IOException unable to load metadata possible values
     * @throws ConfigIOParseException unable to load metadata possible values
     * @throws SimulatorException unable to load metadata possible values
     * @throws PropertyNotExpectedType unable to load metadata possible values
     */
    private PropertyArray filterDataWithValue(final String metadataFile, final Object metadataFilter)
            throws IOException, ConfigIOParseException, SimulatorException, PropertyNotExpectedType {
        if(metadataFile.equals("DB-Locations"))
            return filteredLocations_;

        PropertyArray data = new PropertyArray();
        if(metadataFile.equals("Integer")) {
            PropertyArray range = (PropertyArray) metadataFilter;
            if(range.size() != 2)
                throw new SimulatorException("Please provide range with 2 values only, lower to higher limit");
            int lowLimit = range.getInt(0);
            int highLimit = range.getInt(1);
            for(int i = lowLimit; i < highLimit; i++)
                data.add(String.valueOf(i));
            return data;
        }

        PropertyArray metadata = loadDataFromMetadata(metadataFile);
        String filter = metadataFilter.toString();
        for(Object key : metadata) {
            if(key.toString().matches(filter))
                data.add(key);
        }
        return data;
    }

    /**
     * Thsi method is used to load data from file
     * @param metadataFile file path
     * @return data in file
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to read/load data
     */
    private PropertyArray loadDataFromMetadata(final String metadataFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(metadataFile).getAsArray();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(metadataFile).getAsArray();
        }
    }

    private final JsonPath jsonPath_;
    private final Logger log_;
    private final ForeignDataGenerator generator_;
    private PropertyArray filteredLocations_ = new PropertyArray();
}
