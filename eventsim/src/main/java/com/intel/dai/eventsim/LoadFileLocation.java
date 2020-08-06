package com.intel.dai.eventsim;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;

import javax.validation.constraints.NotNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
     * This method is used to write data to file
     * @param data data
     * @param file file path
     * @throws IOException null/empty data or file provided
     */
    static void writeFile(@NotNull final PropertyArray data, @NotNull final String file, boolean prettyPrint) throws IOException {
        if(data == null || data.isEmpty() || file == null || file.isEmpty())
            throw new IOException("data or file path is null or empty.");

        if(prettyPrint) {
            writeFile(data, file, 2);
            return;
        }

        writeFile(data, file, 0);
    }

    /**
     * This method is used to write data to file
     * @param data data
     * @param file file path
     * @throws IOException null/empty data or file provided
     */
    private static void writeFile(@NotNull final PropertyArray data, @NotNull final String file, int indent) throws IOException {
        loadParser();
        parser_.setIndent(indent);
        parser_.writeConfig(data, file);
        parser_.setIndent(0);
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