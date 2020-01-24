// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.properties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HashMap derived class representing configuration information from a file like JSON or YML. Supported value
 * types are:
 *      BigDecimal
 *      BigInteger
 *      Double
 *      Long
 *      Integer
 *      Short
 *      Boolean
 *      String
 *      PropertyMap
 *      PropertyArray
 */
@SuppressWarnings("serial")
public final class PropertyMap extends HashMap<String, Object> implements PropertyDocument {
    /**
     * Create a PropertyMap recursively from a Map derived class.
     *
     * @param baseMap The Map<String,?> derived class to build the PropertyMap from. If null the PropertyMap is
     *                created empty.
     */
    @SuppressWarnings("unchecked")
    public PropertyMap(Map<String,?> baseMap) {
        if(baseMap != null) {
            for (Entry<String,?> entry : baseMap.entrySet()) {
                Object value = entry.getValue();
                if (value == null) put(entry.getKey(), null);
                else if (value instanceof Map<?, ?>)
                    put(entry.getKey(), new PropertyMap((Map<String, ?>) value));
                else if (value instanceof List<?>)
                    put(entry.getKey(), new PropertyArray((List<?>) value));
                else
                    put(entry.getKey(), value);
            }
        }
    }

    /**
     * Default constructor which creates an empty PropertyMap.
     */
    public PropertyMap() {}

    /**
     * Test if the instance is an array.
     *
     * @return True if the instance is an PropertyArray, false otherwise.
     */
    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * Test if the instance is an map.
     *
     * @return True if the instance is an PropertyMap, false otherwise.
     */
    @Override
    public boolean isMap() {
        return true;
    }

    /**
     * Return the PropertyDocument cast to a PropertyArray.
     *
     * @return The cast instance if its a PropertyArray or null if its not.
     */
    @Override
    public PropertyArray getAsArray() {
        return null;
    }

    /**
     * Return the PropertyDocument cast to a PropertyMap.
     *
     * @return The cast instance if its a PropertyMap or null if its not.
     */
    @Override
    public PropertyMap getAsMap() {
        return this;
    }

    /**
     * Return the value as a BigDecimal.
     *
     * @param key The key of the map to lookup.
     * @return The value as a BigDecimal or null of the key is missing (this.get(key) semantics.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a BigDecimal.
     */
    public BigDecimal getBigDecimal(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toBigDecimal(get(key));
    }

    /**
     * Return the value as a BigInteger.
     *
     * @param key The key of the map to lookup.
     * @return The value as a BigInteger or null of the key is missing (this.get(key) semantics.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a BigInteger.
     */
    public BigInteger getBigInteger(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toBigInteger(get(key));
    }

    /**
     * Return the value as a double.
     *
     * @param key The key of the map to lookup.
     * @return The value as a double.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a double.
     */
    public double getDouble(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toDouble(get(key));
    }

    /**
     * Return the value as a long.
     *
     * @param key The key of the map to lookup.
     * @return The value as a long.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a long.
     */
    public long getLong(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toLong(get(key));
    }

    /**
     * Return the value as an int.
     *
     * @param key The key of the map to lookup.
     * @return The value as an int.
     * @throws PropertyNotExpectedType when the data type cannot be converted to an int.
     */
    public int getInt(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toInt(get(key));
    }

    /**
     * Return the value as a short.
     *
     * @param key The key of the map to lookup.
     * @return The value as a short.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a short.
     */
    public short getShort(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toShort(get(key));
    }

    /**
     * Return the value as a boolean.
     *
     * @param key The key of the map to lookup.
     * @return The value as a boolean.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a boolean.
     */
    public boolean getBoolean(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toBoolean(get(key));
    }

    /**
     * Return the value as a String.
     *
     * @param key The key of the map to lookup.
     * @return The value as a String.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a String.
     */
    public String getString(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toString(get(key));
    }

    /**
     * Return the value as a PropertyMap.
     *
     * @param key The key of the map to lookup.
     * @return The value as a PropertyMap.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a PropertyMap.
     */
    public PropertyMap getMap(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toMap(get(key));
    }

    /**
     * Return the value as a PropertyArray.
     *
     * @param key The key of the map to lookup.
     * @return The value as a PropertyArray.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a PropertyArray.
     */
    public PropertyArray getArray(String key) throws PropertyNotExpectedType {
        return PropertyConversions.toArray(get(key));
    }

    /**
     * Return the value as a BigDecimal or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a BigDecimal or the default value def.
     */
    public BigDecimal getBigDecimalOrDefault(String key, BigDecimal def) {
        try {
            BigDecimal result = getBigDecimal(key);
            if(result == null && def != null) result = def;
            return result;
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a BigInteger or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a BigInteger or the default value def.
     */
    public BigInteger getBigIntegerOrDefault(String key, BigInteger def) {
        try {
            BigInteger result = getBigInteger(key);
            if(result == null && def != null) result = def;
            return result;
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a double or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a double or the default value def.
     */
    public double getDoubleOrDefault(String key, double def) {
        try {
            return getDouble(key);
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a long or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a long or the default value def.
     */
    public long getLongOrDefault(String key, long def) {
        try {
            return getLong(key);
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as an int or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as an int or the default value def.
     */
    public int getIntOrDefault(String key, int def) {
        try {
            return getInt(key);
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a short or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a short or the default value def.
     */
    public short getShortOrDefault(String key, short def) {
        try {
            return getShort(key);
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a boolean or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a boolean or the default value def.
     */
    public boolean getBooleanOrDefault(String key, boolean def) {
        try {
            return getBoolean(key);
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a String or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a String or the default value def.
     */
    public String getStringOrDefault(String key, String def) {
        try {
            String result = getString(key);
            if(result == null && def != null) result = def;
            return result;
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a PropertyMap or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a PropertyMap or the default value def.
     */
    public PropertyMap getMapOrDefault(String key, PropertyMap def) {
        try {
            PropertyMap result = getMap(key);
            if(result == null && def != null) result = def;
            return result;
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }

    /**
     * Return the value as a PropertyArray or return the default.
     *
     * @param key The key of the map to lookup.
     * @param def The default to return if the key doesn't exist or is of the wrong type.
     * @return The value as a PropertyArray or the default value def.
     */
    public PropertyArray getArrayOrDefault(String key, PropertyArray def) {
        try {
            PropertyArray result = getArray(key);
            if(result == null && def != null) result = def;
            return result;
        } catch(PropertyNotExpectedType e) {
            return def;
        }
    }
}
