package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;

import java.io.FileNotFoundException;
import java.io.IOException;

class ForeignEventEcho {

    private PropertyMap loadDataFromFile(final String metadataFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(metadataFile).getAsMap();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(metadataFile).getAsMap();
        }
    }

    void processMessage(String messageFile) throws SimulatorException {
        if ( messageFile == null ) {
            throw new SimulatorException("message file location is null.");
        }

        try {
            props_ = loadDataFromFile(messageFile);

        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException(e.getMessage());
        }

    }

    PropertyMap props_ = new PropertyMap();
}
