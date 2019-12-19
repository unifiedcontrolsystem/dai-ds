package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.io.FileNotFoundException;
import java.io.IOException;

public class InventoryApi {

    //Fetch hardware inventory details from a config file
    PropertyArray getHwInventory() {
        PropertyArray data;
        try {
             data = readConfigFileAsArray(HW_INVENTORY_CONFIG);
        } catch (IOException | ConfigIOParseException e) {
            throw new RuntimeException("Error in loading hardware inventory data.");
        }

        if(data.isEmpty())
            throw new RuntimeException("Error in loading hardware inventory data.");
        return data;
    }

    public PropertyArray getInventoryHardwareForLocation(String location) {
        PropertyArray data;
        try {
            HW_INV_LOCATION_CONFIG = HW_INV_LOCATION_CONFIG + location + ".json";
            data = readConfigFile(HW_INV_LOCATION_CONFIG);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("No inventory data for this location.");
        } catch (IOException | ConfigIOParseException e) {
            throw new RuntimeException("Error in loading hardware inventory data.");
        }

        if(data.size() == 0)
            throw new RuntimeException("Error in loading hardware inventory data.");
        return data;
    }

    public PropertyArray getInventoryHardwareQueryForLocation(String location) {
        PropertyArray data;
        try {
            HW_INV_QUERY_LOCATION_CONFIG = HW_INV_QUERY_LOCATION_CONFIG + location + ".json";
            data = readConfigFile(HW_INV_QUERY_LOCATION_CONFIG);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("No inventory data for this location.");
        } catch (IOException | ConfigIOParseException e) {
            throw new RuntimeException("Error in loading hardware inventory data.");
        }

        if(data.size() == 0)
            throw new RuntimeException("Error in loading hardware inventory data.");
        return data;
    }

    private PropertyArray readConfigFileAsArray(String hwInvConfigFile) throws ConfigIOParseException, IOException {
        return LoadConfigFile.fromFileLocation(hwInvConfigFile).getAsArray();
    }

    private PropertyArray readConfigFile(String hwInvConfigFile) throws ConfigIOParseException, IOException {
        return (PropertyArray) LoadConfigFile.fromFileLocation(hwInvConfigFile);
    }

    protected String HW_INVENTORY_CONFIG = "/opt/ucs/etc/InventoryNodeProcessorMemory.json";
    protected String HW_INV_LOCATION_CONFIG = "/opt/ucs/etc/";
    protected String HW_INV_QUERY_LOCATION_CONFIG = "/opt/ucs/etc/HwInvQuery_";
}
