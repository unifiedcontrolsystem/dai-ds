package com.intel.dai.eventsim;


import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Description of class DataLoader.
 * load/validate api simulation configuration details in a file.
 * load/validate events simulation configuration details in a file.
 * load/validate network configuration details in a file.
 * loads/validate node locations and its respective foreign name
 */
class DataLoader {

    DataLoader(final String serverConfigFile, final String voltdbServer, final Logger log) {
        serverConfigFile_ = serverConfigFile;
        voltdbServer_ = voltdbServer;
        log_ = log;
        createDataStoreFactory();
    }

    /**
     * This method is used to load simulation server configuration data.
     * @throws SimulatorException unable to read/load simulation server configuration data.
     */
    void initialize() throws SimulatorException {
        loadData();
        log_.info("Foreign simulation server configuration details are loaded successfully.");
    }

    /**
     * This method is used to fetch default values of events simulation configuration.
     * @param parameter name of default parameter
     * @param defaultValue default value
     * @return if parameters exists return default value, if not default value given by user
     */
    String getEventsConfigutaion(final String parameter, final String defaultValue) {
        return eventsConfiguration_.getStringOrDefault(parameter, defaultValue);
    }

    /**
     * This method is used to return foreign simulation server network configuration details.
     * @return network configuration data.
     */
    PropertyMap getNetworkConfigurationData() {
        return networkConfiguration_;
    }

    /**
     * This method is used to return events template configuration file
     * @return location of events template file absolute path
     */
    String getEventsTemplateConfigurationFile() {
        return getEventsConfigutaion(FOREIGN_EVENTS_CONFIG_KEYS[1], null);
    }

    /**
     * This method is used to return sensor metadata.
     * @return sensor metadata
     * @throws IOException unable to read/load sensor metadata
     * @throws ConfigIOParseException unable to read/load sensor metadata
     */
    PropertyMap getSensorMetaData() throws IOException, ConfigIOParseException {
        PropertyMap data = loadDataFromFile(sensorMetadataFileAbsolutePath_);
        return data;
    }

    /**
     * This method is used to return jobe metadata.
     * @return jobs metadata
     * @throws IOException unable to read/load jobs metadata
     * @throws ConfigIOParseException unable to read/load jobs metadata
     */
    PropertyMap getJobsMetaData() throws IOException, ConfigIOParseException {
        PropertyMap data = loadDataFromFile(jobsMetadataFileAbsolutePath_);
        return data;
    }

    /**
     * This method is used to return ras metadata.
     * @return ras metadata
     * @throws IOException unable to read/load ras metadata
     * @throws ConfigIOParseException unable to read/load ras metadata
     */
    List<String> getRasMetaData() throws IOException, ConfigIOParseException {
        PropertyMap data = loadDataFromFile(rasMetadataFileAbsolutePath_);
        return new ArrayList<>(data.keySet());
    }

    /**
     * This method is used to load/validate simulation-server, api-simulation, events-simulation
     * network-config and node location details.
     * @throws SimulatorException unable to read/load/validate above configuration details
     */
    private void loadData() throws SimulatorException {
        try {
            loadForeignSimulatorServerConfiguration();
            loadForeignApiSimulatorConfigution();
            loadForeignEventsSimulatorConfiguration();
            loadNetworkConfiguration();
            loadManifestLocations();
            loadForeignLocations();
            validateForeignWithManifestLocations();
        } catch (PropertyNotExpectedType | DataStoreException | IOException | ConfigIOParseException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to load foreign simulation server configuration details
     * @throws IOException unable to read/load simulation server configuration details
     * @throws ConfigIOParseException unable to read/load simulation server configuration details
     * @throws SimulatorException unable to read/load simulation server configuration details
     */
    private void loadForeignSimulatorServerConfiguration() throws IOException, ConfigIOParseException,
            SimulatorException {
        serverConfiguration_ = loadDataFromFile(serverConfigFile_);
        DataValidation.validateKeys(serverConfiguration_, FOREIGN_SERVER_CONFIG, MISSING_FOREIGN_SERVER_CONFIG);
    }

    /**
     * This method is used to load api simulation configuration details
     * @throws PropertyNotExpectedType unable to read/load api simulation configuration details
     * @throws SimulatorException unable to read/load api simulation configuration details
     */
    private void loadForeignApiSimulatorConfigution() throws PropertyNotExpectedType, SimulatorException {
        PropertyMap apiConfiguration = serverConfiguration_.getMap(FOREIGN_SERVER_CONFIG[0]);
        DataValidation.validateKeys(apiConfiguration, FOREIGN_API_CONFIG_KEYS, MISSING_FOREIGN_SERVER_CONFIG);
        bootParamsFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[0]);
        hwInventoryFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[1]);
        hwInventoryFilePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[2]);
        hwInventoryQueryFilePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[3]);
        hwInventoryDiscStatusUrl_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[4]);
        sensorMetadataFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[5]);
        rasMetadataFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[6]);
        jobsMetadataFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_CONFIG_KEYS[7]);
    }

    /**
     * This method is used to load events simulation configuration details
     * @throws SimulatorException unable to read/load events simulation configuration details
     * @throws PropertyNotExpectedType unable to read/load events simulation configuration details
     */
    private void loadForeignEventsSimulatorConfiguration() throws SimulatorException, PropertyNotExpectedType {
        eventsConfiguration_ = serverConfiguration_.getMap(FOREIGN_SERVER_CONFIG[1]);
        DataValidation.validateKeys(eventsConfiguration_, FOREIGN_EVENTS_CONFIG_KEYS, MISSING_FOREIGN_SERVER_CONFIG);
    }

    /**
     * This method is used to load foreign simulation server network configuration details
     * @throws SimulatorException unable to read/load network configuration details
     * @throws PropertyNotExpectedType unable to read/load network configuration details
     */
    private void loadNetworkConfiguration() throws SimulatorException, PropertyNotExpectedType {
        networkConfiguration_ = serverConfiguration_.getMap(FOREIGN_SERVER_CONFIG[2]);
        DataValidation.validateKeys(networkConfiguration_, NETWORK_CONFIG_KEYS, MISSING_FOREIGN_SERVER_CONFIG);
    }

    /**
     * This method is used to load node, non-node locations and their respective hostnames.
     * @throws DataStoreException unable to fetch data
     */
    private void loadManifestLocations() throws DataStoreException {
        nodeInfo_ = factory_.createNodeInformation();
        loadNodeLocations();
        loadNonNodeLocations();
        loadHostnames();
    }

    /**
     * This method is used to load all node locations.
     * @throws DataStoreException unable to fetch data
     */
    private void loadNodeLocations() throws DataStoreException {
        nodeLocations_ = nodeInfo_.getNodeLocations();
    }

    /**
     * This method is used to load all non-node locations.
     */
    private void loadNonNodeLocations() {
        //TODO: To be added
        nonNodeLocations_ = new ArrayList<>();
    }

    /**
     * This method is used to load hostnames of all node/non-node locations.
     * @throws DataStoreException unable to fetch data
     */
    private void loadHostnames() throws DataStoreException {
        nodeHostnames_ = new ArrayList<>(nodeInfo_.getComputeHostnameFromLocationMap().values());
    }

    /**
     * This method is used to validate all node location contains respective foreign name
     * @throws SimulatorException unable to validate foreign name
     */
    private void validateForeignWithManifestLocations() throws SimulatorException {
        allLocations_ = new ArrayList<>();
        allLocations_.addAll(nodeLocations_);
        allLocations_.addAll(nonNodeLocations_);
        if(!foreignLocations_.containsAll(allLocations_))
            throw new SimulatorException("Not all locations in database has the mapping foreign name");
    }

    /**
     * This method is used to load data from file location or resources
     * @param metadataFile file path
     * @return data in file
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to reda/load data
     */
    private PropertyMap loadDataFromFile(final String metadataFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(metadataFile).getAsMap();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(metadataFile).getAsMap();
        }
    }

    /**
     * This method is used to fetch all foreign names.
     */
    private void loadForeignLocations() { foreignLocations_ = CommonFunctions.getLocations(); }


    /**
     * This method is used to create factory method for database using input data
     */
    private void createDataStoreFactory() {
        factory_ = new DataStoreFactoryImpl(new String[]{voltdbServer_}, log_);
    }

    List<String> getNodeLocations() { return nodeLocations_; }
    List<String> getNonNodeLocations() { return nonNodeLocations_; }
    List<String> getNodeLocationsHostnames() { return nodeHostnames_; }
    List<String> getAllLocations() { return allLocations_ ; }

    public String getBootParamsFileLocation() { return bootParamsFileAbsolutePath_; }
    public String getHwInventoryFileLocation() { return hwInventoryFileAbsolutePath_; }
    public String getHwInventoryFileLocationPath() { return hwInventoryFilePath_; }
    public String getHwInventoryQueryLocationPath() { return hwInventoryQueryFilePath_; }
    public String getHwInventoryDiscStatusUrl() { return hwInventoryDiscStatusUrl_; }

    private Collection<Object> foreignLocations_;

    DataStoreFactory factory_;

    private List<String> nodeLocations_;
    private List<String> nonNodeLocations_;
    private List<String> nodeHostnames_;
    private List<String> allLocations_;

    private PropertyMap serverConfiguration_;
    private PropertyMap eventsConfiguration_;
    private PropertyMap networkConfiguration_;

    private String bootParamsFileAbsolutePath_;
    private String hwInventoryFileAbsolutePath_;
    private String hwInventoryFilePath_;
    private String hwInventoryQueryFilePath_;
    private String hwInventoryDiscStatusUrl_;
    private String sensorMetadataFileAbsolutePath_;
    private String rasMetadataFileAbsolutePath_;
    private String jobsMetadataFileAbsolutePath_;

    private final Logger log_;
    private NodeInformation nodeInfo_;

    private final String serverConfigFile_;
    private final String voltdbServer_;

    private final String[] FOREIGN_API_CONFIG_KEYS = {"boot-parameters", "hw-inventory", "hw-inventory-path", "hw-inventory-query-path",
            "hw-inv-discover-status-url", "sensor-metadata", "ras-metadata", "jobs-metadata"};
    private final String[] FOREIGN_EVENTS_CONFIG_KEYS = {"count", "events-template-config", "time-delay-mus"};
    private final String[] NETWORK_CONFIG_KEYS = {"network"};
    private final String[] FOREIGN_SERVER_CONFIG = {"api-simulator-config", "events-simulator-config", "network-config"};
    private final String MISSING_FOREIGN_SERVER_CONFIG = "Eventsim config file is missing required entry, entry = ";
}
