// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ras;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.IAdapter;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;

public class AdapterRasTest {
    class MockAdapterRas extends AdapterRas {
        MockAdapterRas() throws IOException, TimeoutException {
            super(mock(IAdapter.class), mock(Logger.class));
        }
    }

    @Before
    public void setUp() throws Exception {
        String config = "{\n" +
                "  \"notification\": {\n" +
                "    \"enabled\": true,\n" +
                "    \"implementation\": \"smtp\",\n" +
                "    \"to\": [\"email1@somewhere.net\", \"email2@somewhere.net\"],\n" +
                "    \"from\": \"email3@somewhere.net\",\n" +
                "    \"subject\": \"{{CONTEXTREASON}}: {{NODE}} @ {{TIMESTAMP}}\",\n" +
                "    \"body\": [\n" +
                "      \"=============================================================\",\n" +
                "      \" Reason: {{EVENT}} => {{ACTION}}\",\n" +
                "      \"=============================================================\",\n" +
                "      \"{{DETAILS}}\",\n" +
                "      \"-------------------------------------------------------------\"\n" +
                "    ]\n" +
                "  }\n" +
                "}\n";
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        configMap_ = parser.fromString(config).getAsMap();
        LoggerFactory.getInstance("TEST", "Testing", "console");
    }

    @Test
    public void test() throws Exception {
        AdapterRas ras = new MockAdapterRas();
    }

    private PropertyMap configMap_;
}
