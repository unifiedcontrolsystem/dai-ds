package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;

public class BootParamApi {

    //Fetch boot parameters from a bootparametrs config file
    PropertyMap getBootParametrs() {
        try {
            PropertyMap data = readConfigFile(BOOT_PARAM_CONFIG);
            return processData(data);
        } catch (PropertyNotExpectedType | ConfigIOParseException | IOException e) {
            throw new RuntimeException("Error in loading boot parameters data.");
        }
    }

    private PropertyMap processData(PropertyMap data) throws PropertyNotExpectedType {
        data = data.getMap("boot-images");
        if(data == null)
            throw new RuntimeException("No boot-images data.");
        return data;
    }

    private PropertyMap readConfigFile(String boot_param_config) throws IOException, ConfigIOParseException {
        return LoadConfigFile.fromFileLocation(boot_param_config).getAsMap();
    }

    protected String BOOT_PARAM_CONFIG = "/opt/ucs/etc/BootParameters.json";
}
