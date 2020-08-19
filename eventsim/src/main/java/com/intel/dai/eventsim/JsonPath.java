package com.intel.dai.eventsim;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    PropertyDocument set(String jsonPath, PropertyMap data, String newValue) throws PropertyNotExpectedType {
        return setPathValue(jsonPath, data, newValue);
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

    PropertyDocument set(String jsonPath, PropertyMap data, PropertyArray newvalues, long seed) throws PropertyNotExpectedType {
        return setPathValue(jsonPath, data, newvalues, seed);
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
        if(searchPathAndField.length != 2)
            return searchPathAndField;
        jsonPath = searchPathAndField[0] + ARRAY_ALL;
        String field = searchPathAndField[1].replaceAll(PATH_SEPARATOR, "");
        return new String[] {jsonPath, field};

    }

    private PropertyDocument setPathValue(String jsonPath, PropertyMap data, String newValue) throws PropertyNotExpectedType {
        String[] pathAndField = getPathAndField(jsonPath);
        if(pathAndField.length != 2)
            return data;
        String jpath = pathAndField[0];
        String field = pathAndField[1];

        PropertyDocument fieldData = getPathData(jpath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();
            for(int i = 0; i < item.size(); i++) {
                if(item.containsKey(field))
                    item.put(field, newValue);
            }
        } else {
            PropertyArray items = fieldData.getAsArray();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap item = items.getMap(i);
                if(item.containsKey(field))
                    item.put(field, newValue);
            }
        }
        return data;
    }

    private void setPathValue(String jsonPath, PropertyMap data, PropertyMap newFieldValues, long count, long seed) throws PropertyNotExpectedType {
        String[] pathAndField = getPathAndField(jsonPath);

        String jpath = pathAndField[0] + ARRAY_SPECIFIC;

        PropertyDocument arrayData = getPathData(jpath, data);
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
        long count = 0;
        String[] pathAndField = getPathAndField(jsonPath);

        String jpath = pathAndField[0] + ARRAY_SPECIFIC;

        PropertyDocument arrayData = getPathData(jpath, data);
        PropertyArray pathData = arrayData.getAsArray();
        for(int i = 0; i < pathData.size(); i++) {
            PropertyMap d1 = pathData.getMap(i);
            String[] levels = (pathAndField[1]+ ARRAY_SPECIFIC).split(PATH_SEPARATOR);

            int numOfLevels = levels.length;
            for(int j = 1; j < numOfLevels - 1; j++) {
                String level = levels[j];
                boolean isMap = !level.endsWith(IS_ARRAY);
                level = level.replaceAll(ARRAY_ALL_SEARCH, "");
                if(isMap)
                    d1 = d1.getMap(level);
                else
                    d1 = d1.getArray(level).getMap(0);
            }

            String level = levels[numOfLevels - 1];
            level = level.replaceAll("[(?,*)]", "*").replaceAll(ARRAY_ALL_SEARCH, "");
            boolean isMap = level.endsWith(IS_ARRAY);
            PropertyDocument fdata = isMap ? d1.getMap(level) : d1.getArray(level);

            String field = pathAndField[pathAndField.length - 1];
            field = field.replaceAll(PATH_SEPARATOR, "");
            if(fdata.isMap()) {
                PropertyMap items = fdata.getAsMap();
                if(items.containsKey(field)) {
                    items.put(field, ZonedDateTime.now(ZoneId.of(zone)).toInstant().toString());
                    count++;
                }
            } else {
                PropertyArray items = fdata.getAsArray();
                for(int k = 0; k < items.size(); k++) {
                    PropertyMap item = items.getMap(k);
                    if(item.containsKey(field)) {
                        item.put(field, ZonedDateTime.now(ZoneId.of(zone)).toInstant().toString());
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void setPathCounter(String jsonPath, PropertyMap data, long counter, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        String[] pathAndField = getPathAndField(jsonPath);
        String jpath = pathAndField[0];

        PropertyDocument fieldData = getPathData(jpath, data);
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

    private PropertyDocument setPathValue(String jsonPath, PropertyMap data, PropertyArray newvalues, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);
        String[] pathAndField = getPathAndField(jsonPath);
        if(pathAndField.length != 2)
            return data;
        String jpath = pathAndField[0];
        String field = pathAndField[1];

        PropertyDocument fieldData = getPathData(jpath, data);
        if(fieldData.isMap()) {
            PropertyMap item = fieldData.getAsMap();
            for(int i = 0; i < item.size(); i++) {
                if(item.containsKey(field))
                    item.put(field, newvalues.get(generateRandomNumberBetween(0, newvalues.size())));
            }
        } else {
            PropertyArray items = fieldData.getAsArray();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap item = items.getMap(i);
                if(item.containsKey(field))
                    item.put(field, newvalues.get(generateRandomNumberBetween(0, newvalues.size())));
            }
        }
        return data;
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

    private PropertyDocument splitDataByPathAndCount(PropertyMap templateData, String generateDataPath, String overFlowJPath, long countJPathTemplateItems, long seed) throws PropertyNotExpectedType {
        if(overFlowJPath.equals("new")) {
            return splitDataByPathAndCount(templateData, generateDataPath, countJPathTemplateItems, seed);
        }
        randomNumber.setSeed(seed);
        PropertyMap copyData = new PropertyMap(templateData);

        PropertyDocument fieldData = getPathData(generateDataPath, templateData);
        PropertyDocument overFlowPathDataFinal = getPathData(overFlowJPath, copyData);

        PropertyArray items = fieldData.getAsArray();
        PropertyArray newItems = new PropertyArray();
        while(items.size() > 0) {
            PropertyDocument jpathData = getPathDataByCount(generateDataPath, templateData, countJPathTemplateItems, seed);
            PropertyDocument overFlowPathData = getPathData(overFlowJPath, jpathData.getAsMap());
            newItems.add(overFlowPathData.getAsArray().getMap(0));
        }

        overFlowPathDataFinal.getAsArray().clear();
        overFlowPathDataFinal.getAsArray().addAll(newItems);
        templateData = copyData;
        return copyData;
    }

    private PropertyDocument splitDataByPathAndCount(PropertyMap templateData, String generateDataPath, long countJPathTemplateItems, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);
        PropertyMap copyData = new PropertyMap();

        PropertyDocument fieldData = getPathData(generateDataPath, templateData);

        PropertyArray items = fieldData.getAsArray();
        PropertyArray newItems = new PropertyArray();
        while(items.size() > 0) {
            PropertyDocument jpathData = getPathDataByCount(generateDataPath, templateData, countJPathTemplateItems, seed);
            newItems.add(jpathData.getAsMap());
        }

        return newItems;
    }

    private PropertyDocument getPathDataByCount(String generateDataPath, PropertyMap templateData, long countJPathTemplateItems, long seed) throws PropertyNotExpectedType {
        randomNumber.setSeed(seed);

        PropertyMap copyTemplate = new PropertyMap(templateData);

        PropertyDocument fieldData = getPathData(generateDataPath, templateData);
        PropertyDocument finalData = getPathData(generateDataPath, copyTemplate);

        if(fieldData.isArray()) {
            PropertyArray items = fieldData.getAsArray();
            PropertyArray data = new PropertyArray(items);
            PropertyArray newItems = new PropertyArray();

            for(long i = 0; i < countJPathTemplateItems && data.size() > 0; i++) {
                PropertyMap item = new PropertyMap(data.getMap(generateRandomNumberBetween(0, data.size())));
                newItems.add(item);
                data.remove(item);
                items.remove(item);
            }
            finalData.getAsArray().clear();
            finalData.getAsArray().addAll(newItems);
        }

        return copyTemplate;
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
        level = level.replaceAll("[(?,*)]", "*").replaceAll(ARRAY_ALL_SEARCH, "");
        boolean isMap = level.endsWith(IS_ARRAY);
        PropertyDocument fdata;
        if(isMap)
            fdata = data.getMap(level);
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

    private final String PATH_SEPARATOR = "/";
    private final String ARRAY_ALL = "[*]";
    private final String ARRAY_ALL_SEARCH = "\\[\\*\\]";
    private final String ARRAY_SPECIFIC = "[?]";
    private final String ARRAY_SPECIFIC_SEARCH = "\\[\\?\\]";
    private final String SPECIFIC = "?";
    private final String SPECIFIC_SEARCH = "\\?";
    private final String IS_ARRAY = "]";
    private Random randomNumber = new Random();
}
