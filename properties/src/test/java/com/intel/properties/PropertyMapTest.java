// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.properties;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;

public class PropertyMapTest {
    @Before public void setUp() {
        map_ = new PropertyMap();
        map_.put("0", null);
        map_.put("1", new BigDecimal(3.14159));
        map_.put("2", new BigInteger("42"));
        map_.put("3", 1000L);
        map_.put("4", 100);
        map_.put("5", (short)10);
        map_.put("6", true);
        map_.put("7", "testing");
        map_.put("8", new PropertyMap());
        map_.put("9", new PropertyArray());
        map_.put("10", new File("/tmp/file.tmp"));
    }

    @Test public void isArray() {
        assertFalse(map_.isArray());
    }

    @Test public void isMap() {
        assertTrue(map_.isMap());
    }

    @Test public void getAsArray() {
        assertNull(map_.getAsArray());
    }

    @Test public void getAsMap() {
        assertNotNull(map_.getAsMap());
    }

    @Test public void getBigDecimal() throws PropertyNotExpectedType {
        BigDecimal def = new BigDecimal("999.0");
        assertNull(map_.getBigDecimal("0"));
        assertEquals(new BigDecimal(3.14159), map_.getBigDecimal("1"));
        assertEquals(new BigDecimal(42), map_.getBigDecimal("2"));
        assertEquals(new BigDecimal(1000), map_.getBigDecimal("3"));
        assertEquals(new BigDecimal(100), map_.getBigDecimal("4"));
        assertEquals(new BigDecimal(10), map_.getBigDecimal("5"));
        try {
            map_.getBigDecimal("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigDecimal("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigDecimal("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigDecimal("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigDecimal("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(new BigDecimal(3.14159), map_.getBigDecimalOrDefault("1", def));
        assertEquals(def, map_.getBigDecimalOrDefault("99", def));
        assertEquals(def, map_.getBigDecimalOrDefault("10", def));
        assertNull(map_.getBigDecimalOrDefault("99", null));
    }

    @Test public void getBigInteger() throws PropertyNotExpectedType {
        BigInteger def = new BigInteger("999");
        assertNull(map_.getBigInteger("0"));
        assertEquals(new BigInteger("3"), map_.getBigInteger("1"));
        assertEquals(new BigInteger("42"), map_.getBigInteger("2"));
        assertEquals(new BigInteger("1000"), map_.getBigInteger("3"));
        assertEquals(new BigInteger("100"), map_.getBigInteger("4"));
        assertEquals(new BigInteger("10"), map_.getBigInteger("5"));
        try {
            map_.getBigInteger("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigInteger("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigInteger("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigInteger("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBigInteger("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(new BigInteger("42"), map_.getBigIntegerOrDefault("2", def));
        assertEquals(def, map_.getBigIntegerOrDefault("99", def));
        assertEquals(def, map_.getBigIntegerOrDefault("10", def));
        assertNull(map_.getBigIntegerOrDefault("99", null));
    }

    @Test public void getDouble() throws PropertyNotExpectedType {
        double def = 999.0;
        assertEquals(3.14159, map_.getDouble("1"), 0.000001);
        assertEquals(42.0, map_.getDouble("2"), 0.000001);
        assertEquals(1000.0, map_.getDouble("3"), 0.000001);
        assertEquals(100.0, map_.getDouble("4"), 0.000001);
        assertEquals(10.0, map_.getDouble("5"), 0.000001);
        try {
            map_.getDouble("0");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getDouble("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getDouble("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getDouble("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getDouble("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getDouble("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(3.14159, map_.getDoubleOrDefault("1", def), 0.000001);
        assertEquals(def, map_.getDoubleOrDefault("99", def), 0.000001);
    }

    @Test public void getLong() throws PropertyNotExpectedType {
        long def = 999;
        assertEquals(3, map_.getLong("1"));
        assertEquals(42, map_.getLong("2"));
        assertEquals(1000, map_.getLong("3"));
        assertEquals(100, map_.getLong("4"));
        assertEquals(10, map_.getLong("5"));
        try {
            map_.getLong("0");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getLong("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getLong("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getLong("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getLong("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getLong("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(1000, map_.getLongOrDefault("3", def), 0.000001);
        assertEquals(def, map_.getLongOrDefault("99", def), 0.000001);
    }

    @Test public void getInt() throws PropertyNotExpectedType {
        int def = 999;
        assertEquals(3, map_.getInt("1"));
        assertEquals(42, map_.getInt("2"));
        assertEquals(1000, map_.getInt("3"));
        assertEquals(100, map_.getInt("4"));
        assertEquals(10, map_.getInt("5"));
        try {
            map_.getInt("0");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getInt("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getInt("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getInt("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getInt("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getInt("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(100, map_.getIntOrDefault("4", def), 0.000001);
        assertEquals(def, map_.getIntOrDefault("99", def), 0.000001);
    }

    @Test public void getShort() throws PropertyNotExpectedType {
        short def = 999;
        assertEquals(3, map_.getShort("1"));
        assertEquals(42, map_.getShort("2"));
        assertEquals(1000, map_.getShort("3"));
        assertEquals(100, map_.getShort("4"));
        assertEquals(10, map_.getShort("5"));
        try {
            map_.getShort("0");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getShort("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getShort("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getShort("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getShort("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getShort("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(10, map_.getShortOrDefault("5", def), 0.000001);
        assertEquals(def, map_.getShortOrDefault("99", def), 0.000001);
    }

    @Test public void getBoolean() throws PropertyNotExpectedType {
        boolean def = false;
        assertTrue(map_.getBoolean("6"));
        try {
            map_.getBoolean("0");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("1");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("2");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("3");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("4");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("5");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getBoolean("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertTrue(map_.getBooleanOrDefault("6", def));
        assertEquals(def, map_.getBooleanOrDefault("99", def));
    }

    @Test public void getString() throws PropertyNotExpectedType {
        String def = "default";
        assertNull(map_.getString("0"));
        assertEquals("testing", map_.getString("7"));
        assertTrue(map_.getString("1").startsWith("3.1415"));
        assertEquals("42", map_.getString("2"));
        assertEquals("1000", map_.getString("3"));
        assertEquals("100", map_.getString("4"));
        assertEquals("10", map_.getString("5"));
        assertEquals("true", map_.getString("6"));
        try {
            map_.getString("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getString("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getString("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals("testing", map_.getStringOrDefault("7", def));
        assertEquals(def, map_.getStringOrDefault("99", def));
        assertEquals(def, map_.getStringOrDefault("10", def));
        assertNull(map_.getStringOrDefault("99", null));
    }

    @Test public void getMap() throws PropertyNotExpectedType {
        PropertyMap def = new PropertyMap();
        assertNull(map_.getMap("0"));
        PropertyMap golden = map_.getMap("8");
        assertEquals("PropertyMap", golden.getClass().getSimpleName());
        try {
            map_.getMap("1");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("2");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("3");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("4");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("5");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("9");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getMap("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(golden, map_.getMapOrDefault("8", def));
        assertEquals(def, map_.getMapOrDefault("99", def));
        assertEquals(def, map_.getMapOrDefault("10", def));
        assertNull(map_.getMapOrDefault("99", null));
    }

    @Test public void getArray() throws PropertyNotExpectedType {
        PropertyArray def = new PropertyArray();
        assertNull(map_.getArray("0"));
        PropertyArray golden =  map_.getArray("9");
        assertEquals("PropertyArray", golden.getClass().getSimpleName());
        try {
            map_.getArray("1");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("2");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("3");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("4");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("5");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("6");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("7");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("8");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            map_.getArray("10");
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        assertEquals(golden, map_.getArrayOrDefault("9", def));
        assertEquals(def, map_.getArrayOrDefault("99", def));
        assertEquals(def, map_.getArrayOrDefault("10", def));
        assertNull(map_.getArrayOrDefault("99", null));
    }

    @Test public void fromMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("string,", "string");
        map.put("map", new HashMap<String, String>());
        map.put("list", new ArrayList<Integer>());
        map.put("null", null);
        new PropertyMap(map);
        new PropertyMap(null);
    }

    PropertyMap map_;
}
