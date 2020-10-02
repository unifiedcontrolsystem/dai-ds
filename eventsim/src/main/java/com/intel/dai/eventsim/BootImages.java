package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.sun.istack.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Description of class BootImages.
 * set the boot images configuration file
 * fetch boot images.
 */
public class BootImages {

    /**
     * This method used to fetch boot images data.
     * @return boot images data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    @NotNull
    PropertyDocument getBootImages() throws SimulatorException {
        try {
            return processData(readConfigFile(bootImagesConfigFile_));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given boot images config file doesn't exists : " + bootImagesConfigFile_);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error in loading boot images data.");
        }
    }

    /**
     * This method used to fetch boot images data for a location.
     * @return boot images data for a bootImageId
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    @NotNull
    PropertyDocument getBootImageForId(String bootImageId) throws SimulatorException {
        try {
            return processDataForImageId(processData(readConfigFile(bootImagesConfigFile_)), bootImageId);
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given boot images config file doesn't exists : " + bootImagesConfigFile_);
        } catch (final ConfigIOParseException | IOException | PropertyNotExpectedType e) {
            throw new SimulatorException("Error in loading boot images data.");
        }
    }

    /**
     * This method used to set boot images configuration file.
     * @param bootImagesConfigFile location of boot images configuration file.
     * @throws SimulatorException when unable to set the location of boot images configuration file.
     */
    void setBootImagesConfigFile(@NotNull final String bootImagesConfigFile) throws SimulatorException {
        if (bootImagesConfigFile == null || bootImagesConfigFile.isEmpty())
            throw new SimulatorException("Invalid or null boot images config file.");
        bootImagesConfigFile_ = bootImagesConfigFile;
    }

    /**
     * This method process the boot images configuration file data.
     * @param data boot images configuration file data.
     * @return processed boot images configuration file data.
     */
    private PropertyArray processData(PropertyArray data) {
        if (data == null || !data.isArray() || data.getAsArray().isEmpty())
            return new PropertyArray();
        return data.getAsArray();
    }

    /**
     * This method process the boot images configuration file data.
     * @param data boot images configuration file data.
     * @param bootImageId imageId for which data needs to be retrieved.
     * @return processed boot images configuration file data.
     * @throws PropertyNotExpectedType when unable to fetch imageId information data.
     */
    private PropertyMap processDataForImageId(PropertyArray data, String bootImageId) throws PropertyNotExpectedType {
        for(int i = 0; i < data.size(); i++) {
            PropertyMap bootImageData = data.getMap(i);
            if(bootImageData.getStringOrDefault("id", "").equals(bootImageId))
                return bootImageData;
        }
        return new PropertyMap();
    }

    /**
     * This method reads the boot images configuration file.
     * @param bootImagesConfigFile boot images configuration file.
     * @return file data.
     * @throws IOException  unable to find file or parse data.
     * @throws ConfigIOParseException unable to find file or parse data.
     */
    private PropertyArray readConfigFile(String bootImagesConfigFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(bootImagesConfigFile).getAsArray();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(bootImagesConfigFile).getAsArray();
        }
    }

    private String bootImagesConfigFile_;
}