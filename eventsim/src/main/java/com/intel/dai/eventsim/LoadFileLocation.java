package com.intel.dai.eventsim;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyDocument;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

/**
 * Description of class LoadFileLocation.
 * loads data from location or from resources.
 */
public class LoadFileLocation {

    /**
     * This method is used to load data from file location.
     * @param location file location.
     * @return file data
     * @throws IOException when unable to read file.
     */
    public static PropertyDocument fromFileLocation(final String location) throws IOException, ConfigIOParseException {
        loadParser();
        return parser_.readConfig(location);
    }

    /**
     * This method is used to load data from resouces.
     * @param location filename in resources.
     * @return file data
     * @throws IOException when unable to read file.
     */
    public static PropertyDocument fromResources(final String location) throws IOException, ConfigIOParseException {
        InputStream stream = LoadFileLocation.class.getResourceAsStream(location);
        if(stream == null) throw new FileNotFoundException(String.format("Resource not found: %s", location));
        try {
            loadParser();
            return parser_.readConfig(stream);
        } finally {
            stream.close();
        }
    }

    /**
     * This method is used to load parser.
     */
    private static void loadParser() {
        if(parser_ == null)
            parser_ = ConfigIOFactory.getInstance("json");
    }

    private static ConfigIO parser_;
}