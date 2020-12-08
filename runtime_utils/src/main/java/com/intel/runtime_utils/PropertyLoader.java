package com.intel.runtime_utils;

import java.io.*;
import java.util.Properties;

public final class PropertyLoader {

    public static Properties loadProperties(String canonicalName, String resource, String pathToFile ) {
        Properties result = new Properties();

        if ( resource != null ) {
            InputStream is = null;
            try {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                if (is != null)
                    result.load(is);
            } catch ( IOException ignore ) {
            } finally {
                if(is != null) {
                    try {
                        is.close();
                    } catch(IOException e) { /* Ignore close failure... */ }
                }
            }
        }


        if (pathToFile != null) {
            File fp = new File(pathToFile);
            try (InputStream stream = new FileInputStream(fp)) {
                try (Reader fr = new InputStreamReader(stream)) {
                    result.load(fr);
                }

            } catch ( IOException ignore ) { }

        }

        return result;
    }

    public static void systemPropertiesMerge( Properties props ) {
        for ( String prop : props.stringPropertyNames() ) {
            if ( System.getProperty(prop) == null) {
                System.setProperty(prop, props.getProperty(prop));
            }
        }
    }
}
