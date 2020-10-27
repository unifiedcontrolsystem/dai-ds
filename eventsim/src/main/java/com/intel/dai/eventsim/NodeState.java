package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Description of class NodeStates.
 * set the node states configuration file
 * fetch node states.
 */
public class NodeState {

    /**
     * This method used to fetch node states data.
     * @return node states data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    PropertyDocument getNodeStates() throws SimulatorException {
        try {
            return processData(readConfigFile(nodeStatesConfigFile_));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given node states config file doesn't exists : " + nodeStatesConfigFile_);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error in loading node states data.");
        }
    }

    /**
     * This method used to fetch node states data for a location.
     * @return node states data for a location
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    PropertyDocument getNodeStateForLocation(String location) throws SimulatorException {
        try {
            return processDataForLocation(processData(readConfigFile(nodeStatesConfigFile_)), location);
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given node states config file doesn't exists : " + nodeStatesConfigFile_);
        } catch (final ConfigIOParseException | IOException | PropertyNotExpectedType e) {
            throw new SimulatorException("Error in loading node states data.");
        }
    }

    /**
     * This method used to set node states configuration file.
     * @param nodeStatesConfigFile node states configuration file.
     * @throws SimulatorException when unable to set the location of node states configuration file.
     */
    void setNodeStatesConfigFile(final String nodeStatesConfigFile) throws SimulatorException {
        if (nodeStatesConfigFile == null || nodeStatesConfigFile.isEmpty())
            throw new SimulatorException("Invalid or null node states config file.");
        nodeStatesConfigFile_ = nodeStatesConfigFile;
    }

    /**
     * This method process the node states configuration file data.
     * @param data node states configuration file data.
     * @return processed node states configuration file data.
     * @throws SimulatorException when unable to node states configuration file data.
     */
    private PropertyMap processData(PropertyDocument data) throws SimulatorException {
        if (data == null || !data.isMap() || data.getAsMap().isEmpty())
            throw new SimulatorException("No node states data.");
        return data.getAsMap();
    }

    /**
     * This method process the node states configuration file data.
     * @param data node states configuration file data.
     * @param location location for which data needs to be retrieved.
     * @return processed node states configuration file data.
     * @throws PropertyNotExpectedType when unable to fetch hosts information data.
     */
    private PropertyMap processDataForLocation(PropertyDocument data, String location) throws SimulatorException, PropertyNotExpectedType {
        PropertyMap nodeStatesData = processData(data);
        if(!nodeStatesData.containsKey("Components"))
            throw new SimulatorException("'Components' parameters is missing in " + nodeStatesConfigFile_);
        PropertyArray nodeStates = nodeStatesData.getArrayOrDefault("Components", new PropertyArray());
        for(int i = 0; i < nodeStates.size(); i++) {
            PropertyMap nodeStateData = nodeStates.getMap(i);
            if(nodeStateData.getStringOrDefault("ID", "").contains(location)) {
                return nodeStateData;
            }
        }
        return new PropertyMap();
    }

    /**
     * This method reads the node states configuration file.
     * @param nodeStatesConfigFile node states configuration file.
     * @return file data.
     * @throws IOException  unable to find file or parse data.
     * @throws ConfigIOParseException unable to find file or parse data.
     */
    private PropertyDocument readConfigFile(String nodeStatesConfigFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(nodeStatesConfigFile).getAsMap();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(nodeStatesConfigFile).getAsMap();
        }
    }

    private String nodeStatesConfigFile_;
}