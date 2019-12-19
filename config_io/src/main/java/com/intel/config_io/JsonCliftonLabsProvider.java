// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

class JsonCliftonLabsProvider extends ConfigIOBase {
    JsonCliftonLabsProvider() {}

    @Override
    public PropertyDocument fromString(String json) throws ConfigIOParseException {
        try {
            Object obj = Jsoner.deserialize(json);
            PropertyDocument doc;
            if(obj instanceof JsonObject) {
                doc = new PropertyMap();
                walkJsonObject((JsonObject) obj, (PropertyMap)doc);
            } else {
                doc = new PropertyArray();
                walkJsonArray((JsonArray) obj, (PropertyArray)doc);
            }
            return doc;
        } catch (com.github.cliftonlabs.json_simple.JsonException e) {
            throw new ConfigIOParseException(e.getPosition());
        }
    }

    @Override
    public String toString(PropertyDocument document) {
        if(document.isMap())
            return toJson(document.getAsMap());
        else
            return toJson(document.getAsArray());
    }

    private String toJson(PropertyMap map) {
        JsonObject jObj = new JsonObject();
        walkPropertyMap(map, jObj);
        if(indent_ > 0)
            return prettyPrint(Jsoner.serialize(jObj));
        return Jsoner.serialize(jObj);
    }

    private String prettyPrint(String serializedJson) {
        StringReader reader = new StringReader(serializedJson);
        StringWriter writer = new StringWriter();
        try {
            Jsoner.prettyPrint(reader, writer, SPACES.substring(0, Math.min(SPACES.length(), indent_)), "\n");
            return writer.toString();
        } catch(JsonException | IOException e) {
            return serializedJson;
        }
    }

    private String toJson(PropertyArray array) {
        JsonArray jArray = new JsonArray();
        walkPropertyArray(array, jArray);
        if(indent_ > 0)
            return prettyPrint(Jsoner.serialize(jArray));
        return Jsoner.serialize(jArray);
    }

    private void walkJsonArray(JsonArray jArray, PropertyArray array) {
        for(Object item: jArray) {
            if(item == null) array.add(null);
            else {
                String type = item.getClass().getSimpleName();
                if (type.equals("JsonArray")) {
                    PropertyArray subArray = new PropertyArray();
                    walkJsonArray((JsonArray) item, subArray);
                    array.add(subArray);
                } else if (type.equals("JsonObject")) {
                    PropertyMap subMap = new PropertyMap();
                    walkJsonObject((JsonObject) item, subMap);
                    array.add(subMap);
                } else
                    array.add(item);
            }
        }
    }

    private void walkJsonObject(JsonObject jObject, PropertyMap map) {
        for(String key: jObject.keySet()) {
            Object value = jObject.get(key);
            if(value == null) map.put(key, null);
            else {
                String type = value.getClass().getSimpleName();
                if(type.equals("JsonArray")) {
                    PropertyArray subArray = new PropertyArray();
                    walkJsonArray((JsonArray) value, subArray);
                    map.put(key, subArray);
                } else if(type.equals("JsonObject")) {
                    PropertyMap subMap = new PropertyMap();
                    walkJsonObject((JsonObject) value, subMap);
                    map.put(key, subMap);
                } else
                    map.put(key, value);
            }
        }
    }

    private void walkPropertyArray(PropertyArray array, JsonArray jArray) {
        for(Object item: array) {
            if(item == null) jArray.add(null);
            else {
                String type = item.getClass().getSimpleName();
                if(type.equals("PropertyArray")) {
                    JsonArray subArray = new JsonArray();
                    walkPropertyArray((PropertyArray) item, subArray);
                    jArray.add(subArray);
                } else if(type.equals("PropertyMap")) {
                    JsonObject subObject = new JsonObject();
                    walkPropertyMap((PropertyMap) item, subObject);
                    jArray.add(subObject);
                } else
                    jArray.add(item);
            }
        }
    }

    private void walkPropertyMap(PropertyMap map, JsonObject jObj) {
        for(String key: map.keySet()) {
            Object item = map.get(key);
            if(item == null) jObj.put(key, null);
            else {
                String type = item.getClass().getSimpleName();
                if(type.equals("PropertyArray")) {
                    JsonArray subArray = new JsonArray();
                    walkPropertyArray((PropertyArray) item, subArray);
                    jObj.put(key, subArray);
                } else if(type.equals("PropertyMap")) {
                    JsonObject subObject = new JsonObject();
                    walkPropertyMap((PropertyMap) item, subObject);
                    jObj.put(key, subObject);
                } else
                    jObj.put(key, item);
            }
        }
    }

    private static final String SPACES = "                ";
}
