// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.properties;

import java.math.BigDecimal;
import java.math.BigInteger;

class PropertyConversions {
    private PropertyConversions() {}

    static BigDecimal toBigDecimal(Object value) throws PropertyNotExpectedType {
        if(value == null) return null;
        if(value instanceof BigDecimal) return (BigDecimal)value;
        if(value instanceof BigInteger) return new BigDecimal((BigInteger)value);
        if(value instanceof Number) return new BigDecimal(((Number)value).doubleValue());
        try {
            if(value instanceof String) return new BigDecimal((String)value);
        } catch(NumberFormatException e) { /* Fall Through */}
        throw new PropertyNotExpectedType(BigDecimal.class, value.getClass());
    }

    static BigInteger toBigInteger(Object value) throws PropertyNotExpectedType {
        if(value == null) return null;
        if(value instanceof BigDecimal) return ((BigDecimal)value).toBigInteger();
        if(value instanceof BigInteger) return (BigInteger)value;
        if(value instanceof Number) return new BigInteger(((Number)value).toString());
        try {
            if(value instanceof String) return new BigInteger((String)value);
        } catch(NumberFormatException e) { /* Fall Through */}
        throw new PropertyNotExpectedType(BigInteger.class, value.getClass());
    }

    static double toDouble(Object value) throws PropertyNotExpectedType {
        if(value == null) throw new PropertyNotExpectedType(Double.class);
        if(value instanceof BigDecimal) return ((BigDecimal)value).doubleValue();
        if(value instanceof BigInteger) return ((BigInteger)value).doubleValue();
        if(value instanceof Number) return ((Number)value).doubleValue();
        try {
            if(value instanceof String) return Double.parseDouble((String)value);
        } catch(NumberFormatException e) { /* Fall Through */}
        throw new PropertyNotExpectedType(Double.class, value.getClass());
    }

    static long toLong(Object value) throws PropertyNotExpectedType {
        if(value == null) throw new PropertyNotExpectedType(Long.class);
        if(value instanceof BigDecimal) return ((BigDecimal)value).longValue();
        if(value instanceof BigInteger) return ((BigInteger)value).longValue();
        if(value instanceof Number) return ((Number)value).longValue();
        try {
            if(value instanceof String) return Long.parseLong((String)value);
        } catch(NumberFormatException e) { /* Fall Through */}
        throw new PropertyNotExpectedType(Long.class, value.getClass());
    }

    static int toInt(Object value) throws PropertyNotExpectedType {
        if(value == null) throw new PropertyNotExpectedType(Integer.class);
        if(value instanceof BigDecimal) return ((BigDecimal)value).intValue();
        if(value instanceof BigInteger) return ((BigInteger)value).intValue();
        if(value instanceof Number) return ((Number)value).intValue();
        try {
            if(value instanceof String) return Integer.parseInt((String)value);
        } catch(NumberFormatException e) { /* Fall Through */}
        throw new PropertyNotExpectedType(Integer.class, value.getClass());
    }

    static short toShort(Object value) throws PropertyNotExpectedType {
        if(value == null) throw new PropertyNotExpectedType(Short.class);
        if(value instanceof BigDecimal) return ((BigDecimal)value).shortValue();
        if(value instanceof BigInteger) return ((BigInteger)value).shortValue();
        if(value instanceof Number) return ((Number)value).shortValue();
        try {
            if(value instanceof String) return Short.parseShort((String)value);
        } catch(NumberFormatException e) { /* Fall Through */}
        throw new PropertyNotExpectedType(Short.class, value.getClass());
    }

    static boolean toBoolean(Object value) throws PropertyNotExpectedType {
        if(value == null) throw new PropertyNotExpectedType(Boolean.class);
        if(value instanceof Boolean) return (Boolean)value;
        throw new PropertyNotExpectedType(Boolean.class, value.getClass());
    }

    static String toString(Object value) throws PropertyNotExpectedType {
        if(value == null) return null;
        if(value instanceof String) return (String)value;
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        throw new PropertyNotExpectedType(String.class, value.getClass());
    }

    static PropertyMap toMap(Object value) throws PropertyNotExpectedType {
        if(value == null) return null;
        if(value instanceof PropertyMap) return (PropertyMap)value;
        throw new PropertyNotExpectedType(PropertyMap.class, value.getClass());
    }

    static PropertyArray toArray(Object value) throws PropertyNotExpectedType {
        if(value == null) return null;
        if(value instanceof PropertyArray) return (PropertyArray) value;
        throw new PropertyNotExpectedType(PropertyArray.class, value.getClass());
    }
}
