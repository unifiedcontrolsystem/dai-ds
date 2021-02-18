package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

/**
 * Description of class EventSim
 * create instances of several api classes to be served
 */
class EventSim {

    EventSim(final String voltdbServer, final String serverConfigFile, final Logger log) throws SimulatorException {
        voltdbServer_ = voltdbServer;
        serverConfigFile_ = serverConfigFile;
        log_ = log;
        parser_ = ConfigIOFactory.getInstance("json");

        DataValidation.isNullOrEmpty(log_, MISSING_LOGGER);
        DataValidation.isNullOrEmpty(parser_, MISSING_PARSER);
        dataLoader_ = new DataLoader(serverConfigFile_, voltdbServer_, log_);
    }

    /**
     * This method is used to load foreign simulation configuration data
     * create and initialise several api classes instances
     * @throws SimulatorException unable to create/initialise instances
     */
    void initialiseInstances() throws SimulatorException {
        loadData();
        initialise();
    }

    /**
     * This method is used to initialise api classes instances
     * @throws SimulatorException unable to create/initialise instances
     */
    void initialiseData() throws SimulatorException {
        network_.initialise();
        foreignSimulatorEngine_.initialize();
    }

    /**
     * This method is used to initialise api classes instances
     * @throws SimulatorException unable to create/initialise instances
     */
    void initialise() throws SimulatorException {
        bootParamsApi_ = new BootParameters();
        bootImagesApi_ = new BootImages();
        nodeStatesApi_ = new NodeState();
        hwInvApi_ = new HardwareInventory();
        wlmApi_ = new WlmApi(log_);
        apiReq_ = new ApiReqData(log_);

        PropertyMap networkConfiguration = dataLoader_.getNetworkConfigurationData();
        network_ = new NetworkObject(networkConfiguration, log_, apiReq_);
        foreignSimulatorEngine_ = new ForeignSimulatorEngine(dataLoader_, network_, log_);
    }

    /**
     * This method is used to load foreignserver configuration data
     * @throws SimulatorException unable to read/load foreign server configuration data
     */
    private void loadData() throws SimulatorException {
        dataLoader_.initialize();
        log_.info("Foreign simulation server configuration details are loaded successfully.");
    }

    protected BootParameters bootParamsApi_;
    protected BootImages bootImagesApi_;
    protected NodeState nodeStatesApi_;
    protected HardwareInventory hwInvApi_;
    protected WlmApi wlmApi_;
    protected NetworkObject network_;
    protected DataLoader dataLoader_;
    protected ForeignSimulatorEngine foreignSimulatorEngine_;
    protected ApiReqData apiReq_;

    protected final ConfigIO parser_;

    private final String voltdbServer_;
    private final String serverConfigFile_;
    private final Logger log_;

    private final static String MISSING_LOGGER = "logger instance cannot be null.";
    private final static String MISSING_PARSER = "parser instance cannot be null.";
}
