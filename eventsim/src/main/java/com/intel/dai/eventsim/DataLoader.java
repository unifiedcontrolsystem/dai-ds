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
import java.util.*;

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
    String getEventsConfiguration(final String parameter, final String defaultValue) {
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
        return getEventsConfiguration(FOREIGN_EVENTS_TEMPLATE_CONFIG, null);
    }

    /**
     * This method is used to load/validate simulation-server, api-simulation, events-simulation
     * network-config and node location details.
     * @throws SimulatorException unable to read/load/validate above configuration details
     */
    private void loadData() throws SimulatorException {
        try {
            loadForeignSimulatorServerConfiguration();
            loadForeignApiSimulatorConfiguration();
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
        DataValidation.validateKeysAndNullValues(serverConfiguration_, FOREIGN_SERVER_CONFIG_KEYS, MISSING_FOREIGN_SERVER_CONFIG);
    }

    /**
     * This method is used to load api simulation configuration details
     * @throws PropertyNotExpectedType unable to read/load api simulation configuration details
     * @throws SimulatorException unable to read/load api simulation configuration details
     */
    private void loadForeignApiSimulatorConfiguration() throws PropertyNotExpectedType, SimulatorException {
        PropertyMap apiConfiguration = serverConfiguration_.getMap(FS_API_SIMULATOR_CONFIG);
        DataValidation.validateKeysAndNullValues(apiConfiguration, FS_API_CONFIG_KEYS, MISSING_FOREIGN_SERVER_CONFIG);
        bootParamsFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_BOOT_PARAMETERS);
        bootImagesFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_BOOT_IMAGES);
        hwInventoryFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_HW_INVENTORY);
        hwInventoryFilePath_ = apiConfiguration.getString(FOREIGN_API_HW_INVENTORY_PATH);
        hwInventoryQueryFilePath_ = apiConfiguration.getString(FOREIGN_API_HW_INV_QUERY_PATH);
        hwInventoryDiscStatusUrl_ = apiConfiguration.getString(FOREIGN_API_HW_INV_DISC_STATUS_URL);
        nodeStateFileAbsolutePath_ = apiConfiguration.getString(FOREIGN_API_NODE_STATE);
    }

    /**
     * This method is used to load events simulation configuration details
     * @throws SimulatorException unable to read/load events simulation configuration details
     * @throws PropertyNotExpectedType unable to read/load events simulation configuration details
     */
    private void loadForeignEventsSimulatorConfiguration() throws SimulatorException, PropertyNotExpectedType {
        eventsConfiguration_ = serverConfiguration_.getMap(FS_EVENTS_SIMULATOR_CONFIG);
        DataValidation.validateKeysAndNullValues(eventsConfiguration_, FS_EVENTS_CONFIG_KEYS, MISSING_FOREIGN_SERVER_CONFIG);
    }

    /**
     * This method is used to load foreign simulation server network configuration details
     * @throws PropertyNotExpectedType unable to read/load network configuration details
     */
    private void loadNetworkConfiguration() throws PropertyNotExpectedType {
        networkConfiguration_ = serverConfiguration_.getMap(FS_NETWORK_CONFIG);
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
        loadHostnameFromLocation();
    }

    /**
     * This method is used to load all node locations.
     * @throws DataStoreException unable to fetch data
     */
    private void loadNodeLocations() throws DataStoreException {
        nodeLocations_ = nodeInfo_.getNodeLocations();
    }

    private void loadHostnameFromLocation() throws DataStoreException {
        hostnameLctnMap_ = nodeInfo_.getComputeHostnameFromLocationMap();
        hostnameLctnMap_.putAll(nodeInfo_.getServiceHostnameFromLocationMap());
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
    String getHostname(String location) throws SimulatorException {
        if(!hostnameLctnMap_.containsKey(location))
            throw new SimulatorException("Location is missing: location=" + location);
        return hostnameLctnMap_.get(location);
    }

    public String getBootParamsFileLocation() { return bootParamsFileAbsolutePath_; }
    public String getNodeStateFileLocation() { return nodeStateFileAbsolutePath_; }
    public String getBootImagesFileLocation() { return bootImagesFileAbsolutePath_; }
    public String getHwInventoryFileLocation() { return hwInventoryFileAbsolutePath_; }
    public String getHwInventoryFileLocationPath() { return hwInventoryFilePath_; }
    public String getHwInventoryQueryLocationPath() { return hwInventoryQueryFilePath_; }
    public String getHwInventoryDiscStatusUrl() { return hwInventoryDiscStatusUrl_; }

    private Collection<Object> foreignLocations_;

    DataStoreFactory factory_;

    private Map<String, String> hostnameLctnMap_ = new HashMap<>();

    private List<String> nodeLocations_;
    private List<String> nonNodeLocations_;
    private List<String> nodeHostnames_;
    private List<String> allLocations_;

    private PropertyMap serverConfiguration_;
    private PropertyMap eventsConfiguration_;
    private PropertyMap networkConfiguration_;

    private String bootParamsFileAbsolutePath_;
    private String nodeStateFileAbsolutePath_;
    private String bootImagesFileAbsolutePath_;
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

    private final static String FS_API_SIMULATOR_CONFIG = "api-simulator-config";
    private final static String FS_EVENTS_SIMULATOR_CONFIG = "events-simulator-config";
    private final static String FS_NETWORK_CONFIG = "server-network-config";

    private final static String FOREIGN_API_BOOT_PARAMETERS = "boot-parameters";
    private final static String FOREIGN_API_BOOT_IMAGES = "boot-images";
    private final static String FOREIGN_API_HW_INVENTORY = "hw-inventory";
    private final static String FOREIGN_API_HW_INVENTORY_PATH = "hw-inventory-path";
    private final static String FOREIGN_API_HW_INV_QUERY_PATH = "hw-inventory-query-path";
    private final static String FOREIGN_API_HW_INV_DISC_STATUS_URL = "hw-inv-discover-status-url";
    private final static String FOREIGN_API_NODE_STATE = "node-state";

    private final static String FOREIGN_EVENTS_COUNT = "count";
    private final static String FOREIGN_EVENTS_TEMPLATE_CONFIG = "events-config-template";
    private final static String FOREIGN_EVENTS_TIME_DELAY_MUS = "time-delay-mus";


    private final String[] FOREIGN_SERVER_CONFIG_KEYS = {FS_API_SIMULATOR_CONFIG, FS_EVENTS_SIMULATOR_CONFIG, FS_NETWORK_CONFIG};

    private final String[] FS_API_CONFIG_KEYS = {FOREIGN_API_BOOT_PARAMETERS, FOREIGN_API_BOOT_IMAGES, FOREIGN_API_HW_INVENTORY,
            FOREIGN_API_HW_INVENTORY_PATH, FOREIGN_API_HW_INV_QUERY_PATH, FOREIGN_API_HW_INV_DISC_STATUS_URL, FOREIGN_API_NODE_STATE};
    private final String[] FS_EVENTS_CONFIG_KEYS = {FOREIGN_EVENTS_COUNT, FOREIGN_EVENTS_TEMPLATE_CONFIG, FOREIGN_EVENTS_TIME_DELAY_MUS};

    private final String MISSING_FOREIGN_SERVER_CONFIG = "Eventsim config file is missing required entry, entry = ";
}
