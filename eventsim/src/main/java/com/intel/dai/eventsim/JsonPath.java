package com.intel.dai.eventsim;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

class JsonPath {


    PropertyDocument parse(String jsonPath, PropertyMap data) throws PropertyNotExpectedType {
        boolean isSpecificRead = jsonPath.contains(SPECIFIC);
        if(isSpecificRead)
            return data;
        return getPathData(jsonPath, data);
    }

    PropertyDocument read(String jsonPath, PropertyMap data, String value) throws PropertyNotExpectedType {
        return getPathData(jsonPath, data, value, false);
    }

    void update(String path, PropertyMap templateData, PropertyMap newFieldValues, long count, long seed) throws PropertyNotExpectedType {
        setPathValue(path, templateData, newFieldValues, count, seed);
    }

    long setTime(String jsonPath, PropertyMap data, String zone) throws PropertyNotExpectedType {
        return setPathTime(jsonPath, data, zone);
    }

    void setCounter(String jsonPath, PropertyMap data, long counter, long seed) throws PropertyNotExpectedType {
        setPathCounter(jsonPath,data, counter, seed);
    }

    PropertyDocument set(String jsonPath, PropertyMap data, Set<String> fields, PropertyMap newValues, long seed) throws PropertyNotExpectedType {
        return setPathValue(jsonPath, data, fields, newValues, seed);
    }

    PropertyDocument split(PropertyMap templateData, String generateDataPath, String overFlowJPath, long countJPathTemplateItems, long seed) throws PropertyNotExpectedType {
        return splitDataByPathAndCount(templateData, generateDataPath, overFlowJPath, countJPathTemplateItems, seed);
    }

    private String[] getPathAndField(String jsonPath) {
        boolean isSpecificRead = jsonPath.contains(SPECIFIC);
        if(!isSpecificRead)
            return new String[] {jsonPath};
        boolean isArraySpecificRead = jsonPath.contains(ARRAY_SPECIFIC);
        String[] searchPathAndField = isArraySpecificRead ? jsonPath.split(ARRAY_SPECIFIC_SEARCH) : jsonPath.split(SPECIFIC_SEARCH);
        for(int index=0; index < searchPathAndField.length - 1; index++)
            searchPathAndField[index] = searchPathAndField[index] + ARRAY_ALL;
        return searchPathAndField;
    }

    private void setPathValue(String jsonPath, PropertyMap data, PropertyMap newFieldValues, long count, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyDocument arrayData = getPathData(jsonPath, data);
        if(arrayData.isArray()) {
            PropertyArray items = arrayData.getAsArray();
            PropertyArray copyItems = new PropertyArray(items);
            PropertyArray newItems = new PropertyArray();
            for(long i = 0; i < count; i++) {
                PropertyMap item = new PropertyMap(copyItems.getMap(generateRandomNumberBetween(0, copyItems.size())));
                for(String field : newFieldValues.keySet()) {
                    if(item.containsKey(field)) {
                        PropertyArray values = newFieldValues.getArray(field);
                        item.put(field, values.get(generateRandomNumberBetween(0, values.size())));
                    }
                }
                newItems.add(item);
            }
            items.clear();
            items.addAll(newItems);
        }
    }

    private long setPathTime(String jsonPath, PropertyMap data, String zone) throws PropertyNotExpectedType {
        String[] pathAndField = getPathAndField(jsonPath);

        String level_0_jPath = pathAndField[0];
        PropertyDocument arrayData = getPathData(level_0_jPath, data);
        PropertyArray pathData = arrayData.getAsArray();

        timeModifiedItems_ = 0;
        getFieldAndUpdateTime(1, pathData, pathAndField, zone);
        return timeModifiedItems_;
    }

    private void getFieldAndUpdateTime(int level, PropertyArray data, String[] paths, String zone) throws PropertyNotExpectedType {
        if(level == paths.length - 1) {
            String field = paths[level];
            for(int index = 0; index < data.size(); index++) {
                PropertyMap item = data.getMap(index);
                if(item.containsKey(field)) {
                    item.put(field, ZonedDateTime.now(ZoneId.of(zone)).toInstant().toString());
                    timeModifiedItems_++;
                }
            }
            return;
        }

        for(int index = 0; index < data.size(); index++) {
            String key = paths[level].replaceAll(ARRAY_ALL_SEARCH, "");
            if(key.contains("/")) {
                String[] mapPath = key.split("/");
                PropertyMap mapData = data.getMap(index);
                for(int mapPathIndex = 0; mapPathIndex < mapPath.length - 1; mapPathIndex++) {
                    mapData = mapData.getMap(mapPath[mapPathIndex]);
                }
                PropertyArray arrayData = mapData.getArray(mapPath[mapPath.length - 1]);
                getFieldAndUpdateTime(level+1, arrayData, paths, zone);
            }
            else {
                PropertyArray indexData = data.getMap(index).getArray(key);
                getFieldAndUpdateTime(level + 1, indexData, paths, zone);
            }
        }
    }

    private void setPathCounter(String jsonPath, PropertyMap data, long counter, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyDocument fieldData = getPathData(jsonPath, data);
        if(fieldData.isArray()) {
            PropertyArray items = fieldData.getAsArray();
            PropertyArray newItems = new PropertyArray(items);

            if(counter > 0 && items.size() > 0)
                items.clear();

            for(int i = 0; i < counter && newItems.size() > 0; i++) {
                PropertyMap item = newItems.getMap(generateRandomNumberBetween(0, newItems.size()));
                items.add(item);
            }
        }
    }

    private PropertyDocument setPathValue(String jsonPath, PropertyMap data, Set<String> fields, PropertyMap newValues, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyDocument fieldData = getPathData(jsonPath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();
            for(int i = 0; i < item.size(); i++) {
                for(String field : fields) {
                    if(item.containsKey(field)) {
                        PropertyArray values = newValues.getArray(field);
                        item.put(field, values.get(generateRandomNumberBetween(0, values.size())));
                    }
                }
            }
        } else {
            PropertyArray items = fieldData.getAsArray();

            PropertyArray newItems = new PropertyArray();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap item = new PropertyMap(items.getMap(i));
                for(String field : fields) {
                    if(item.containsKey(field)) {
                        PropertyArray values = newValues.getArray(field);
                        item.put(field, values.get(generateRandomNumberBetween(0, values.size())));
                    }
                }
                newItems.add(item);
            }
            items.clear();
            items.addAll(newItems);
        }
        return data;
    }

    private PropertyDocument splitDataByPathAndCount(PropertyMap templateData, String generateDataPath, String overFlowDataJPath, long countGenerateDataPathHoldItems, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);
        if(overFlowDataJPath.equals("new")) {
            return splitDataByPathAndCount(templateData, generateDataPath, countGenerateDataPathHoldItems, seed);
        }

        PropertyArray finalData = new PropertyArray();

        //Split generate data and overflow path and find diffences path
        List<String> generatePathLevels = Arrays.asList(generateDataPath.split(PATH_SEPARATOR));
        List<String> overFlowPathLevels = Arrays.asList(overFlowDataJPath.split(PATH_SEPARATOR));
        List<String> traversePathFromOvFlwToGenPath = new ArrayList<>();
        for(String key : generatePathLevels) {
            if(!overFlowPathLevels.contains(key)) {
                traversePathFromOvFlwToGenPath.add(key);
            }
        }
        String[] traversePath = new String[traversePathFromOvFlwToGenPath.size()];
        traversePath = traversePathFromOvFlwToGenPath.toArray(traversePath);

        //Get child of overflowpath data
        PropertyArray overFlowPathData = getPathData(overFlowDataJPath, templateData).getAsArray();
        PropertyMap overFlowPathDataChild = overFlowPathData.getMap(0);

        //traverse through generate-data-path endpoint and get items to split and clear data at endpoint overflowpath
        PropertyArray overFlowPathDataEndPoint = traversePathAndFetchData(traversePath, overFlowPathDataChild);
        PropertyArray items = new PropertyArray(overFlowPathDataEndPoint);
        Collections.shuffle(items, randomNumber);
        overFlowPathDataEndPoint.clear();

        int itemsToSplit = items.size();

        //Collect data and add data to endpoint-overflowpath
        while(itemsToSplit != 0) {
            PropertyMap newOverFlowPathDataEndPoint = new PropertyMap(overFlowPathDataChild);
            PropertyArray childOverFlowPathEndPoint = traversePathAndFetchData(traversePath, newOverFlowPathDataEndPoint).getAsArray();
            PropertyArray itemsByCount = pickRandomItemsByCount(items, itemsToSplit, countGenerateDataPathHoldItems, seed);
            childOverFlowPathEndPoint.addAll(itemsByCount);
            finalData.add(newOverFlowPathDataEndPoint);
            itemsToSplit = itemsToSplit - itemsByCount.size();
        }
        overFlowPathData.clear();
        overFlowPathData.addAll(finalData);
        return templateData;
    }

    private PropertyArray traversePathAndFetchData(String[] paths, PropertyMap data) throws PropertyNotExpectedType {
        for(int i = 0; i < paths.length - 1; i++) {
            String path = paths[i];
            boolean isArray = path.contains("*");
            path = path.replaceAll(ARRAY_ALL_SEARCH, "");
            if(isArray)
                data = data.getArray(path).getMap(0);
            else
                data = data.getMap(path);
        }

        String level = paths[paths.length - 1];
        level = level.replaceAll("[(?,*)]", "*").replaceAll(ARRAY_ALL_SEARCH, "");
        return data.getArray(level);
    }

    private PropertyDocument splitDataByPathAndCount(PropertyMap templateData, String generateDataPath, long countGenerateDataPathHoldItems, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);
        PropertyArray finalData = new PropertyArray();

        PropertyArray fieldData = getPathData(generateDataPath, templateData).getAsArray();
        PropertyArray items = new PropertyArray(fieldData);
        Collections.shuffle(items, randomNumber);
        fieldData.clear();

        int itemsToSplit = items.size();

        while(itemsToSplit != 0) {
            PropertyMap data = new PropertyMap(templateData);
            PropertyArray generateDataPathEndPoint = getPathData(generateDataPath, data).getAsArray();
            PropertyArray jpathData = pickRandomItemsByCount(items, itemsToSplit, countGenerateDataPathHoldItems, seed);
            generateDataPathEndPoint.addAll(jpathData);
            finalData.add(data);
            itemsToSplit = itemsToSplit - jpathData.size();
        }
        return finalData;
    }

    private PropertyArray pickRandomItemsByCount(PropertyArray items, int index, long countJPathTemplateItems, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyArray randomItems = new PropertyArray();
        for(int counter = 0; counter < countJPathTemplateItems && (1 <= index && index <= items.size()); counter++) {
            PropertyMap item = items.getMap(--index);
            randomItems.add(item);
        }
        return randomItems;
    }

    private PropertyDocument getPathData(String jsonPath, PropertyMap data, String value, boolean parse) throws PropertyNotExpectedType {
        String[] pathAndField = getPathAndField(jsonPath);
        if(pathAndField.length != 2)
            return data;
        String jpath = pathAndField[0];
        String field = pathAndField[1];

        PropertyDocument fieldData = getPathData(jpath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();
            if(!(item.containsKey(field) && item.getString(field).equals(value)))
                item.clear();
        } else {
            PropertyArray items = fieldData.getAsArray();
            List<PropertyMap> toDelete = new ArrayList<>();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap item = items.getMap(i);
                if(item.containsKey(field) && !item.getString(field).matches(value))
                    toDelete.add(item);
            }
            items.removeAll(toDelete);
        }
        if(parse)
            return fieldData;
        return data;
    }

    private PropertyDocument getPathData(String jPath, PropertyMap data) throws PropertyNotExpectedType {
        String[] levels = jPath.split(PATH_SEPARATOR);
        int numOfLevels = levels.length;
        if(numOfLevels == 0)
            return data;
        for(int i = 0; i < numOfLevels - 1; i++) {
            String level = levels[i];
            boolean isMap = !level.endsWith(IS_ARRAY);
            long index = 0;
            if(!isMap) {
                String digit = level.substring(level.length() - 2, level.length() -1);

            }
            level = level.replaceAll(ARRAY_ALL_SEARCH, "");
            if(isMap)
                data = data.getMap(level);
            else
                data = data.getArray(level).getMap(0);
        }
        String level = levels[numOfLevels - 1];
        boolean isMap = !level.endsWith(IS_ARRAY);
        level = level.replaceAll("[(?,*)]", "*").replaceAll(ARRAY_ALL_SEARCH, "");

        PropertyDocument fdata;
        if(isMap)
            fdata = data.getMap(level);
        else
            fdata = data.getArray(level);
        return fdata;
    }

    private int generateRandomNumberBetween( long from, long to) {
        return (int) ((randomNumber.nextDouble() * (to - from)) + from);
    }

    /*PropertyDocument parse(String jsonPath, PropertyMap data, String value) throws PropertyNotExpectedType {
        return getPathData(jsonPath, data, value, true);
    }*/

    /*void filter(String path, PropertyMap sampleEventTemplate, long rate, long seed) throws PropertyNotExpectedType {
        filterPathWithRate(path, sampleEventTemplate, rate, seed);
    }*/

    /*PropertyMap clear(String overFlowPath, PropertyMap data, int index, long seed) throws PropertyNotExpectedType {
        return clearPath(overFlowPath, data, index, seed);
    }*/

    /*PropertyMap clear(String overFlowPath, PropertyMap data, long seed) throws PropertyNotExpectedType {
        return clearPath(overFlowPath, data, seed);
    }*/

    /*void merge(String path, PropertyMap finalData, PropertyDocument data, long seed) throws PropertyNotExpectedType {
        addDataToPath(path, finalData, data, seed);
    }

    void merge(String path, PropertyMap finalData, PropertyDocument data, long count, long seed) throws PropertyNotExpectedType {
        addDataToPath(path, finalData, data, count, seed);
    }*/

   /* PropertyDocument delete(String jsonPath, PropertyMap data) throws PropertyNotExpectedType {
        return deletePathData(jsonPath,data);
    }

    PropertyDocument delete(String jsonPath, PropertyMap data, String value) throws PropertyNotExpectedType {
        return deletePathValueData(jsonPath, data, value);
    }*/

    /*private void filterPathWithRate(String jsonPath, PropertyMap data, long rate, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyDocument fieldData = getPathData(jsonPath, data);
        if(fieldData.isArray()) {
            PropertyArray items = fieldData.getAsArray();
            PropertyArray newItems = new PropertyArray();
            for(int i = 0; i < rate && items.size() > 0; i++) {
                PropertyMap item = items.getMap(generateRandomNumberBetween(0, items.size()));
                newItems.add(item);
                items.remove(item);
            }
            fieldData = newItems;
        }
    }*/

    /*private PropertyMap clearPath(String overFlowPath, PropertyMap data, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyDocument fieldData = getPathData(overFlowPath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();

        } else {
            PropertyArray items = fieldData.getAsArray();
            items.clear();
        }
        return data;
    }*/

    /*private PropertyMap clearPath(String overFlowPath, PropertyMap data, int index, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyDocument fieldData = getPathData(overFlowPath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();

        } else {
            PropertyArray items = fieldData.getAsArray();
            items.remove(index);
        }
        return data;
    }*/

   /* private void addDataToPath(String path, PropertyMap finalData, PropertyDocument data, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);
        PropertyDocument fieldData = getPathData(path, finalData);
        if(fieldData.isArray()) {
            PropertyArray items  = fieldData.getAsArray();
            PropertyArray addItems = data.getAsArray();

            PropertyArray newItems = new PropertyArray(items);

            if(addItems.size() > 0)
                items.clear();

            items.addAll(newItems);
            items.addAll(addItems);
            items.size();
        }
    }*/

    /*private void addDataToPath(String path, PropertyMap finalData, PropertyDocument data, long count, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);
        PropertyDocument fieldData = getPathData(path, finalData);
        if(fieldData.isArray()) {
            PropertyArray items  = fieldData.getAsArray();
            PropertyArray addItems = data.getAsArray();

            PropertyArray newItems = new PropertyArray(items);

            if(count > 0)
                items.clear();

            for(int i = 0; i < count; i++)
                items.add(addItems.get(generateRandomNumberBetween(0, addItems.size())));
        }
    }*/

       /* private PropertyDocument deletePathData(String jsonPath, PropertyMap data) throws PropertyNotExpectedType {
        String[] pathAndField = getPathAndField(jsonPath);
        if(pathAndField.length != 2)
            return data;
        String jpath = pathAndField[0];

        PropertyDocument fieldData = getPathData(jpath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();
            item.clear();
        } else {
            PropertyArray items = fieldData.getAsArray();
            if (items.size() > 0) {
                items.subList(0, items.size()).clear();
            }
        }
        return data;
    }

    private PropertyDocument deletePathValueData(String jsonPath, PropertyMap data, String value) throws PropertyNotExpectedType {
        String[] pathAndField = getPathAndField(jsonPath);
        if(pathAndField.length != 2)
            return data;
        String jpath = pathAndField[0];
        String field = pathAndField[1];

        PropertyDocument fieldData = getPathData(jpath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();
            if(item.containsKey(field) && item.getString(field).equals(value))
                item.clear();
        } else {
            PropertyArray items = fieldData.getAsArray();
            List<PropertyMap> toDelete = new ArrayList<>();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap item = items.getMap(i);
                if(item.containsKey(field) && item.getString(field).equals(value))
                    toDelete.add(item);
            }
            items.removeAll(toDelete);
        }
        return data;
    }*/

    private long timeModifiedItems_ = 0;
    private final String PATH_SEPARATOR = "/";
    private final String ARRAY_ALL = "[*]";
    private final String ARRAY_ALL_SEARCH = "\\[\\*\\]";
    private final String ARRAY_SPECIFIC = "[?]";
    private final String ARRAY_SPECIFIC_SEARCH = "\\[\\?\\]/";
    private final String SPECIFIC = "?";
    private final String SPECIFIC_SEARCH = "\\?/";
    private final String IS_ARRAY = "]";
    private final Random randomNumber = new Random();
}
