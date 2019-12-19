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

public class PropertyArrayTest {
    @Before public void setUp() {
        array_ = new PropertyArray();
        array_.add(null);
        array_.add(new BigDecimal(3.14159));
        array_.add(new BigInteger("42"));
        array_.add(1000L);
        array_.add(100);
        array_.add((short)10);
        array_.add(true);
        array_.add("testing");
        array_.add(new PropertyMap());
        array_.add(new PropertyArray());
        array_.add(new File("/tmp/file.tmp"));
    }

    @Test public void isArray() {
        assertTrue(array_.isArray());
    }

    @Test public void isMap() {
        assertFalse(array_.isMap());
    }

    @Test public void getAsArray() {
        assertNotNull(array_.getAsArray());
    }

    @Test public void getAsMap() {
        assertNull(array_.getAsMap());
    }

    @Test public void getBigDecimal() throws PropertyNotExpectedType {
        assertNull(array_.getBigDecimal(0));
        assertEquals(new BigDecimal(3.14159), array_.getBigDecimal(1));
        assertEquals(new BigDecimal(42), array_.getBigDecimal(2));
        assertEquals(new BigDecimal(1000), array_.getBigDecimal(3));
        assertEquals(new BigDecimal(100), array_.getBigDecimal(4));
        assertEquals(new BigDecimal(10), array_.getBigDecimal(5));
        try {
            array_.getBigDecimal(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigDecimal(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigDecimal(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigDecimal(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigDecimal(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getBigInteger() throws PropertyNotExpectedType {
        assertNull(array_.getBigInteger(0));
        assertEquals(new BigInteger("3"), array_.getBigInteger(1));
        assertEquals(new BigInteger("42"), array_.getBigInteger(2));
        assertEquals(new BigInteger("1000"), array_.getBigInteger(3));
        assertEquals(new BigInteger("100"), array_.getBigInteger(4));
        assertEquals(new BigInteger("10"), array_.getBigInteger(5));
        try {
            array_.getBigInteger(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigInteger(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigInteger(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigInteger(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBigInteger(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getDouble() throws PropertyNotExpectedType {
        assertEquals(3.14159, array_.getDouble(1), 0.000001);
        assertEquals(42.0, array_.getDouble(2), 0.000001);
        assertEquals(1000.0, array_.getDouble(3), 0.000001);
        assertEquals(100.0, array_.getDouble(4), 0.000001);
        assertEquals(10.0, array_.getDouble(5), 0.000001);
        try {
            array_.getDouble(0);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getDouble(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getDouble(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getDouble(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getDouble(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getDouble(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getLong() throws PropertyNotExpectedType {
        assertEquals(3, array_.getLong(1));
        assertEquals(42, array_.getLong(2));
        assertEquals(1000, array_.getLong(3));
        assertEquals(100, array_.getLong(4));
        assertEquals(10, array_.getLong(5));
        try {
            array_.getLong(0);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getLong(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getLong(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getLong(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getLong(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getLong(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getInt() throws PropertyNotExpectedType {
        assertEquals(3, array_.getInt(1));
        assertEquals(42, array_.getInt(2));
        assertEquals(1000, array_.getInt(3));
        assertEquals(100, array_.getInt(4));
        assertEquals(10, array_.getInt(5));
        try {
            array_.getInt(0);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getInt(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getInt(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getInt(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getInt(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getInt(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getShort() throws PropertyNotExpectedType {
        assertEquals(3, array_.getShort(1));
        assertEquals(42, array_.getShort(2));
        assertEquals(1000, array_.getShort(3));
        assertEquals(100, array_.getShort(4));
        assertEquals(10, array_.getShort(5));
        try {
            array_.getShort(0);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getShort(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getShort(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getShort(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getShort(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getShort(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getBoolean() throws PropertyNotExpectedType {
        assertTrue(array_.getBoolean(6));
        try {
            array_.getBoolean(0);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(1);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(2);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(3);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(4);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(5);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getBoolean(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getString() throws PropertyNotExpectedType {
        assertNull(array_.getString(0));
        assertEquals("testing", array_.getString(7));
        assertTrue(array_.getString(1).startsWith("3.1415"));
        assertEquals("42", array_.getString(2));
        assertEquals("1000", array_.getString(3));
        assertEquals("100", array_.getString(4));
        assertEquals("10", array_.getString(5));
        assertEquals("true", array_.getString(6));
        try {
            array_.getString(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getString(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getString(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getMap() throws PropertyNotExpectedType {
        assertNull(array_.getMap(0));
        assertEquals("PropertyMap", array_.getMap(8).getClass().getSimpleName());
        try {
            array_.getMap(1);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(2);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(3);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(4);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(5);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(9);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getMap(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void getArray() throws PropertyNotExpectedType {
        assertNull(array_.getArray(0));
        assertEquals("PropertyArray", array_.getArray(9).getClass().getSimpleName());
        try {
            array_.getArray(1);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(2);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(3);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(4);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(5);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(6);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(7);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(8);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
        try {
            array_.getArray(10);
            fail();
        } catch(PropertyNotExpectedType e) { /* PASS */ }
    }

    @Test public void fromList() {
        ArrayList<Object> array = new ArrayList<>();
        array.add("string");
        array.add(new HashMap<String, String>());
        array.add(new ArrayList<Integer>());
        array.add(null);
        new PropertyArray(array);
        new PropertyArray(null);
    }

    PropertyArray array_;
}
