package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyDocument;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Description of class HardwareInventory.
 * set the hardware inventory/discover/query configuration file
 * fetch respective hardware inventory/discover/query data.
 */
public class HardwareInventory {

    /**
     * This method used to set inventory hardware configuration file.
     * @param hwInventoryConfigFile location of hardware inventory configuration file.
     * @throws SimulatorException when unable to set the location of hardware inventory configuration file.
     */
    void setInventoryHardwareConfigLocation(final String hwInventoryConfigFile) throws SimulatorException {
        if (hwInventoryConfigFile == null || hwInventoryConfigFile.isEmpty())
            throw new SimulatorException("Invalid or null hardware inventory config file");
        hwInventoryConfigFile_ = hwInventoryConfigFile;
    }

    /**
     * This method used to fetch hardware inventory data.
     * @return inventory hardware data.
     * @throws SimulatorException when unable to get inventory hardware data.
     */
    PropertyDocument getHwInventory() throws SimulatorException {
        try {
            return processDataAsArray(readConfigFile(hwInventoryConfigFile_));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given hardware inventory data file doesn't exist: " + hwInventoryConfigFile_);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error while loading hardware inventory data");
        }
    }

    /**
     * This method used to set inventory hardware configuration file path.
     * @param hwInventoryConfigPath location of hardware inventory configuration file path.
     * @throws SimulatorException when unable to set the location of hardware inventory configuration file path.
     */
    void setInventoryHardwareConfigPath(final String hwInventoryConfigPath) throws SimulatorException {
        if (hwInventoryConfigPath == null || hwInventoryConfigPath.isEmpty())
            throw new SimulatorException("Invalid or null hardware inventory config path");
        hwInventoryConfigPath_ = hwInventoryConfigPath;
    }

    /**
     * This method used to fetch hardware inventory for a location data.
     * @return inventory hardware for a location data.
     * @throws SimulatorException when unable to get inventory hardware for a location data.
     */
    PropertyDocument getInventoryHardwareForLocation(final String location) throws SimulatorException {
        String hwInventoryForLocationFile = hwInventoryConfigPath_ + "/" + location + HW_INV_LOC_FILE_EXT;
        try {
            return processData(readConfigFile(hwInventoryForLocationFile));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given hardware inventory data file for a location doesn't exist: " + hwInventoryForLocationFile);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error while loading hardware inventory data for a location");
        }
    }

    /**
     * This method used to set hardware inventory query configuration file.
     * @param hwInventoryQuery location of hardware inventory query configuration file.
     * @throws SimulatorException when unable to set the location of hardware inventory query configuration file.
     */
    void setInventoryHardwareQueryPath(final String hwInventoryQuery) throws SimulatorException {
        if (hwInventoryQuery == null || hwInventoryQuery.isEmpty())
            throw new SimulatorException("Invalid or null hardware inventory query path");
        hwInventoryQueryPath_ = hwInventoryQuery;
    }

    /**
     * This method used to fetch inventory hardware query for a location data.
     * @param location location of hardware inventory query for a location configuration file.
     * @throws SimulatorException when unable to set the location of hardware inventory query for a location configuration file.
     */
    PropertyDocument getInventoryHardwareQueryForLocation(final String location) throws SimulatorException {
        String hwInventoryQueryFileLocation = hwInventoryQueryPath_ + "/" + location + HW_INV_QRY_FILE_EXT;
        try {
            return processData(readConfigFile(hwInventoryQueryFileLocation));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given hardware inventory query data file for a location doesn't exist: " + hwInventoryQueryFileLocation);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error while loading hardware inventory query data for a location");
        }
    }

    /**
     * This method process the hardware discovery configuration file data.
     * @param data hardware discovery configuration file data.
     * @return processed hardware discovery configuration file data.
     * @throws SimulatorException when unable to process hardware discovery configuration file data.
     */
    private PropertyDocument processDataAsArray(PropertyDocument data) throws SimulatorException {
        if (data == null || !data.isArray() || data.getAsArray().isEmpty())
            throw new SimulatorException("Error while loading hardware inventory data");
        return data.getAsArray();
    }

    /**
     * This method process the hardware inventory/query configuration file data.
     * @param data hardware inventory/query file data.
     * @return processed hardware inventory/query configuration file data.
     * @throws SimulatorException when unable to process hardware inventory/query configuration file data.
     */
    private PropertyDocument processData(PropertyDocument data) throws SimulatorException {
        if (data == null)
            throw new SimulatorException("Error while loading hardware inventory data for a location");
        return data;
    }

    /**
     * This method reads the hardware inventory/discover/query configuration file.
     * @param hwInventoryFile hardware inventory/discover/query configuration file.
     * @return file data.
     * @throws IOException  unable to find file or parse data.
     * @throws ConfigIOParseException unable to find file or parse data.
     */
    private PropertyDocument readConfigFile(String hwInventoryFile) throws IOException, ConfigIOParseException {
        return LoadFileLocation.fromFileLocation(hwInventoryFile);
    }

    private String hwInventoryConfigPath_;
    private String hwInventoryQueryPath_;
    private String hwInventoryConfigFile_;
    private final static String HW_INV_LOC_FILE_EXT = ".json";
    private final static String HW_INV_QRY_FILE_EXT = ".json";
}