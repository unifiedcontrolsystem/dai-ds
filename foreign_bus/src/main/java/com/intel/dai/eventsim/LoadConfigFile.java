package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.xdg.XdgConfigFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class LoadConfigFile {
    private static ConfigIO parser_;

    private static void loadParser() {
        parser_ = ConfigIOFactory.getInstance("json");
    }

    public static PropertyDocument fromFileLocation(String location) throws IOException, ConfigIOParseException {
        if(parser_ == null)
            loadParser();
        return parser_.readConfig(location);
    }

    public static PropertyDocument fromResource(String location) throws IOException, ConfigIOParseException {
        if(parser_ == null)
            loadParser();
        InputStream stream = LoadConfigFile.class.getResourceAsStream(location);
        if(stream == null) throw new FileNotFoundException(String.format("Resource not found: %s", location));
        try {
            return parser_.readConfig(stream);
        } finally {
            stream.close();
        }
    }
}
