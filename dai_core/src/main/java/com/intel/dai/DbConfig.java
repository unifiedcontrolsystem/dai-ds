// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.intel.config_io.*;
import com.intel.properties.*;

import com.intel.xdg.XdgConfigFile;


public class DbConfig {
    private final HashMap<String, String> mDbConfig;

    private static final String DB_KEY = "db";

    public DbConfig() {
        mDbConfig = new HashMap<>();
    }

    public void loadFromFile(String fileName) throws ConfigIOParseException, IOException {
        XdgConfigFile config = new XdgConfigFile(Adapter.XDG_COMPONENT);
        String fullFileName = config.FindFile(fileName);
        if(fullFileName == null)
            throw new FileNotFoundException(String.format("Filename '%s' was not found in the XDG_PATH!", fileName));
        PropertyMap obj;
        try (InputStream stream = new FileInputStream(fullFileName)) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                ConfigIO parser = ConfigIOFactory.getInstance("json");
                assert parser != null: "Failed to create JSON parser!";
                obj = parser.readConfig(reader).getAsMap();
                assert obj != null: "Config file read was not in the correct format!";
            }
        }

        PropertyMap dbConfig = obj.getMapOrDefault(DB_KEY, null);
        assert dbConfig != null: "Failed to load the DB configuration!";

        for (String k: dbConfig.keySet())
            mDbConfig.put(k, dbConfig.getStringOrDefault(k, ""));
    }

    public Map<String, String> getDbConfig() {
        return mDbConfig;
    }
}
