// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import com.intel.properties.PropertyDocument;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class JsonCliftonLabsProviderTest {
    @Before
    public void setUp() throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("  ").append('"').append("map").append('"').append(':').append("{\"a\":[9,8,7,6]},");
        json.append("  ").append('"').append("list").append('"').append(':').append("[{\"a\":true},[0, 1, 2, 3],null],");
        json.append("  ").append('"').append("int").append('"').append(':').append("42,");
        json.append("  ").append('"').append("decimal").append('"').append(':').append("42.9,");
        json.append("  ").append('"').append("bigNumber").append('"').append(':').append("4200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000424242.0,");
        json.append("  ").append('"').append("str").append('"').append(':').append('"').append("string").append('"').append(",");
        json.append("  ").append('"').append("null").append('"').append(':').append("null,");
        json.append("  ").append('"').append("boolean1").append('"').append(':').append("true,");
        json.append("  ").append('"').append("boolean2").append('"').append(':').append("false");
        json.append("}");
        json_ = json.toString();
        parser_ = new JsonCliftonLabsProvider();
    }

    @Test
    public void parsing() throws ConfigIOParseException {
        PropertyDocument doc = parser_.fromString(json_);
        parser_.setIndent(2);
        String result = parser_.toString(doc);
        PropertyDocument doc2 = parser_.fromString(result);
        assertEquals(doc.getAsMap().keySet().size(), doc2.getAsMap().keySet().size());
        for(String key: doc.getAsMap().keySet()) {
            assertTrue(doc2.getAsMap().containsKey(key));
            assertEquals(doc.getAsMap().get(key), doc2.getAsMap().get(key));
        }
        parser_.setIndent(0);
        result = parser_.toString(doc2);
    }

    @Test
    public void listDocument() throws ConfigIOParseException {
        String json = "[]";
        PropertyDocument doc = parser_.fromString(json);
        parser_.toString(doc);
        parser_.setIndent(2);
        parser_.toString(doc);
    }

    @Test
    public void negative() {
        String json = "[";
        try {
            PropertyDocument doc = parser_.fromString(json);
            fail();
        } catch(ConfigIOParseException e) { /* PASS */ }
    }

    @Test
    public void throwsWhenParsingEmptyString() {
        try {
            parser_.fromString("");
            fail();
        } catch (ConfigIOParseException ex) {
            // PASS
        }
    }

    String json_;
    JsonCliftonLabsProvider parser_;
}
