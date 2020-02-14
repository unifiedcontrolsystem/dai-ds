package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyDocument;
import com.sun.istack.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Description of class BootParameters.
 * set the boot parameters configuration file
 * fetch boot parameters.
 */
public class BootParameters {

    /**
     * This method used to fetch boot parameters data.
     * @return boot parameters data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    @NotNull
    PropertyDocument getBootParameters() throws SimulatorException {
        try {
            return processData(readConfigFile(bootParamsConfigFile_));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given boot parameters config file doesn't exists : " + bootParamsConfigFile_);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error in loading boot parameters data.");
        }
    }

    /**
     * This method used to set boot parameters configuration file.
     * @param bootParamsConfigFile location of boot parameters configuration file.
     * @throws SimulatorException when unable to set the location of boot parameters configuration file.
     */
    void setBootParamsConfigFile(@NotNull final String bootParamsConfigFile) throws SimulatorException {
        if (bootParamsConfigFile == null || bootParamsConfigFile.isEmpty())
            throw new SimulatorException("Invalid or null boot parameters config file.");
        bootParamsConfigFile_ = bootParamsConfigFile;
    }

    /**
     * This method process the boot parameters configuration file data.
     * @param data boot parameters configuration file data.
     * @return processed boot parameters configuration file data.
     * @throws SimulatorException when unable to boot parameters configuration file data.
     */
    private PropertyDocument processData(PropertyDocument data) throws SimulatorException {
        if (data == null || !data.isMap() || data.getAsMap().isEmpty() || !data.getAsMap().containsKey(BOOT_IMAGES_KEY))
            throw new SimulatorException("No boot-images data.");
        return data.getAsMap().getMapOrDefault(BOOT_IMAGES_KEY, null);
    }

    /**
     * This method reads the boot parameters configuration file.
     * @param bootParametersConfigFile boot parameters configuration file.
     * @return file data.
     * @throws IOException  unable to find file or parse data.
     * @throws ConfigIOParseException unable to find file or parse data.
     */
    private PropertyDocument readConfigFile(String bootParametersConfigFile) throws IOException, ConfigIOParseException {
        return LoadFileLocation.fromFileLocation(bootParametersConfigFile);
    }

    private final static String BOOT_IMAGES_KEY = "boot-images";
    private String bootParamsConfigFile_;
}