// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.OutputStreamWriter;

import static org.junit.Assert.*;

public class DbConfigTest {
    @Before
    public void setUp() throws Exception {
        try (OutputStreamWriter writer = new FileWriter(file_)) {
            writer.write("{\n" +
                    "    \"db\": {\n" +
                    "        \"type\": \"jdbc\",\n" +
                    "        \"url\": \"jdbc:postgresql://localhost:5432/exampledb\",\n" +
                    "        \"username\": \"exampleuser\",\n" +
                    "        \"password\": \"example@123\"\n" +
                    "    }\n" +
                    "}\n");
        }
    }

    @After
    public void tearDown() throws Exception {
        file_.delete();
    }

    @Test
    public void loadConfig() throws Exception {
        DbConfig config = new DbConfig();
        config.loadFromFile("NearlineConfig.json");
        config.getDbConfig();
    }

    @Test
    public void loadConfig2() throws Exception {
        DbConfig config = new DbConfig();
        try {
            config.loadFromFile("WrongName.json");
            fail();
        } catch(FileNotFoundException e) { /* PASS */ }
    }

    final private File file_ = new File("/tmp/NearlineConfig.json");
}
