package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyNotExpectedType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Description of class ForeignDataGenerator
 * This class handles update field values, filters template by count and field-values
 * generate single sample event
 * using single sample event generate events for a requested count
 */
class ForeignDataGenerator {

    ForeignDataGenerator(JsonPath jPath, Logger log) {
        jPath_ = jPath;
        log_ = log;
    }

    /**
     * This method is used to filter data by filed-values
     * @param templateData data
     * @param filtersForSingleTemplate field-filter info
     * @throws PropertyNotExpectedType unable to filter data
     */
    void filterEventTypeTemplateData(final PropertyMap templateData, final PropertyMap filtersForSingleTemplate) throws PropertyNotExpectedType {
        for(Map.Entry<String, Object> key : filtersForSingleTemplate.entrySet()) {
            String jpath = key.getKey();
            String filter = key.getValue().toString();
            jPath_.read(jpath, templateData, filter);
        }
    }

    /**
     * This method is used to apply counter on jpaths
     * @param templateData data
     * @param jPathCounterInfo jpaths data
     * @param seed random seed
     * @throws PropertyNotExpectedType unable to apply count filters
     */
    void applyCounterToEventTypeTemplateData(final PropertyMap templateData, final Map<String, Object> jPathCounterInfo,
                                             final long seed) throws PropertyNotExpectedType {
        for(Map.Entry<String, Object> key : jPathCounterInfo.entrySet()) {
            String jpath = key.getKey();
            BigDecimal count = (BigDecimal) key.getValue();
            jPath_.setCounter(jpath, templateData, count.longValue(), seed);
        }
    }

    /**
     * This method is used to generate given count events at generate path
     * @param path final path
     * @param templateData data
     * @param updateJPathWithMetadata update-values with field names
     * @param count number of events to be geenrated
     * @param seed random seed to replicate data
     * @throws PropertyNotExpectedType unable to create events
     */
    void generateEventsUsingTemplate(final String path, final PropertyMap templateData,
                                     final PropertyMap updateJPathWithMetadata, final long count, final long seed) throws PropertyNotExpectedType {
        PropertyMap newFieldValues = updateJPathWithMetadata.getMap(path);
        jPath_.update(path, templateData, newFieldValues, count, seed);
    }

    /**
     * This method is used to find items in a data
     * @param items data
     * @return number of items in data
     */
    private long countItemsInData(final PropertyDocument items) {
        if(items.isArray())
            return items.getAsArray().size();
        return items.getAsMap().size();
    }

    /**
     * This method splits total events based on rate and places extra data in overflow path or create new event
     * @param templateData finalData
     * @param singleTemplateData sampleData
     * @param overFlowJPath overflowPath
     * @return finalout matching rate
     * @throws PropertyNotExpectedType unable to create events
     */
    PropertyDocument generateEventsFormatUsingTemplate(PropertyDocument templateData, final PropertyMap singleTemplateData,
                                                       final Map<String, Object> overFlowJPath, long seed) throws PropertyNotExpectedType {
        long countJPathItems = 0;
        long countJPathTemplateItems = 0;

        for(Map.Entry<String, Object> key : overFlowJPath.entrySet()) {
            String generateDataPath = key.getKey();
            String overFlowPath = key.getValue().toString();

            PropertyDocument itemsInfo = jPath_.parse(generateDataPath, templateData.getAsMap());
            countJPathItems = countItemsInData(itemsInfo);

            PropertyDocument templateItems = jPath_.parse(generateDataPath, singleTemplateData);
            countJPathTemplateItems = countItemsInData(templateItems);

            if(countJPathItems > countJPathTemplateItems)
                templateData = jPath_.split(templateData.getAsMap(), generateDataPath, overFlowPath,
                        countJPathTemplateItems, seed);
        }

        if(templateData.isMap()) {
            PropertyArray data = new PropertyArray();
            data.add(templateData);
            return data;
        }
        return templateData;
    }

    private final JsonPath jPath_;
    private final Logger log_;
}
