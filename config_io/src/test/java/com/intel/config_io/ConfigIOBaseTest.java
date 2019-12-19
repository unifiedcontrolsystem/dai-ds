// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class ConfigIOBaseTest {
    private static class TestParser extends ConfigIOBase {

        @Override
        public PropertyDocument fromString(String json) throws ConfigIOParseException {
            return new PropertyMap();
        }

        @Override
        public String toString(PropertyDocument document) {
            return "{}";
        }
    }

    @Before
    public void setUp() throws IOException {
        parser_ = new TestParser();
        try (FileWriter writer = new FileWriter("/tmp/test.config_io")) {
            writer.write("{}");
        }
    }

    @After
    public void tearDown() {
        File file = new File("/tmp/test.config_io");
        if(file.exists()) file.delete();
        file = new File("/tmp/test2.config_io");
        if(file.exists()) file.delete();
    }

    @Test
    public void readJson() throws ConfigIOParseException, IOException {
        parser_.readConfig("/tmp/test.config_io");
    }

    @Test
    public void writeJson() throws IOException {
        parser_.writeConfig(new PropertyMap(), "/tmp/test2.config_io");
        parser_.writeConfig(new PropertyArray(), "/tmp/test2.config_io");
    }

    @Test
    public void setIndent() {
        parser_.setIndent(2);
        assertEquals(2, parser_.indent());
        try {
            parser_.setIndent(-1);
            fail();
        } catch(IllegalArgumentException e) { /* PASS */ }
        try {
            parser_.setIndent(9);
            fail();
        } catch(IllegalArgumentException e) { /* PASS */ }
        try {
            parser_.setIndent(1);
            fail();
        } catch(IllegalArgumentException e) { /* PASS */ }
        parser_.setIndent(0);
    }

    ConfigIOBase parser_;
}
