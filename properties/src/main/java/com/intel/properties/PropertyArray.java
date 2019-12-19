// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.properties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * ArrayList derived class representing configuration information from a file like JSON or YML. Supported value
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
public final class PropertyArray extends ArrayList<Object> implements PropertyDocument {
    /**
     * Create a PropertyArray recursively from a List derived class.
     *
     * @param baseArray The List<?> derived class to build the PropertyArray from. If null the PropertyArray is
     *                created empty.
     */
    @SuppressWarnings("unchecked")
    public PropertyArray(Collection<?> baseArray) {
        if(baseArray != null) {
            for (Object value : baseArray) {
                if (value == null) add(null);
                else if (value instanceof Map<?, ?>)
                    add(new PropertyMap((Map<String, ?>) value));
                else if (value instanceof Collection<?>)
                    add(new PropertyArray((Collection<?>) value));
                else
                    add(value);
            }
        }
    }

    /**
     * Default constructor which creates an empty PropertyArray.
     */
    public PropertyArray() {}

    /**
     * Test if the instance is an array.
     *
     * @return True if the instance is an PropertyArray, false otherwise.
     */
    @Override
    public boolean isArray() {
        return true;
    }

    /**
     * Test if the instance is an map.
     *
     * @return True if the instance is an PropertyMap, false otherwise.
     */
    @Override
    public boolean isMap() {
        return false;
    }

    /**
     * Return the PropertyDocument cast to a PropertyArray.
     *
     * @return The cast instance if its a PropertyArray or null if its not.
     */
    @Override
    public PropertyArray getAsArray() {
        return this;
    }

    /**
     * Return the PropertyDocument cast to a PropertyMap.
     *
     * @return The cast instance if its a PropertyMap or null if its not.
     */
    @Override
    public PropertyMap getAsMap() {
        return null;
    }

    /**
     * Return the value as a BigDecimal.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a BigDecimal or null of the key is missing (this.get(key) semantics.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a BigDecimal.
     */
    public BigDecimal getBigDecimal(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toBigDecimal(value);
    }

    /**
     * Return the value as a BigInteger.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a BigInteger.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a BigInteger.
     */
    public BigInteger getBigInteger(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toBigInteger(value);
    }

    /**
     * Return the value as a double.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a double.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a double.
     */
    public double getDouble(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toDouble(value);
    }

    /**
     * Return the value as a long.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a long.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a long.
     */
    public long getLong(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toLong(value);
    }

    /**
     * Return the value as an int.
     *
     * @param index The array index of the value to lookup.
     * @return The value as an int.
     * @throws PropertyNotExpectedType when the data type cannot be converted to an int.
     */
    public int getInt(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toInt(value);
    }

    /**
     * Return the value as a short.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a short.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a short.
     */
    public short getShort(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toShort(value);
    }

    /**
     * Return the value as a boolean.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a boolean.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a boolean.
     */
    public boolean getBoolean(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toBoolean(value);
    }

    /**
     * Return the value as a String.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a String.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a String.
     */
    public String getString(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toString(value);
    }

    /**
     * Return the value as a PropertyMap.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a PropertyMap.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a PropertyMap.
     */
    public PropertyMap getMap(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toMap(value);
    }

    /**
     * Return the value as a PropertyArray.
     *
     * @param index The array index of the value to lookup.
     * @return The value as a PropertyArray.
     * @throws PropertyNotExpectedType when the data type cannot be converted to a PropertyArray.
     */
    public PropertyArray getArray(int index) throws PropertyNotExpectedType {
        Object value = get(index);
        return PropertyConversions.toArray(value);
    }
}
