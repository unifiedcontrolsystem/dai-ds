package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Description of class EventSim.
 * initialisation of all dependent components eventsim server to serve.
 */
public class EventSim {

    EventSim(String[] args, Logger log) throws SimulatorException, RESTClientException, RESTServerException, PropertyNotExpectedType {
        log_ = log;
        loadConfigFile(args);
        validateConfig();
        initialiseInstances(args);
    }

    //for unit test cases
    EventSim(Logger log) {
        log_ = log;
        wlmApi = new WlmApi(log_);
    }

    /**
     * This method is used to initialise instances
     * @throws SimulatorException when unable to create instances.
     */
    void initialise(String[] args) throws SimulatorException {
        DataStoreFactory factory = createDataStoreFactory(args);
        nodeinfo = factory.createNodeInformation();
        PropertyMap eventsimConfig = serverConfiguration_.getMapOrDefault("eventsimConfig", null);
        simEngineDataLoader = new DataLoaderEngine(eventsimConfig, nodeinfo, log_);
        eventSimEngine = new SimulatorEngine(simEngineDataLoader, source_, log_);
        eventSimEngine.initialize();
    }

    //factory method for db
    DataStoreFactory createDataStoreFactory(String[] args) {
        return new DataStoreFactoryImpl(args, log_);
    }

    /**
     * This method is used load configuration file data
     * @param args_ voltdb details, configuration file location
     * @throws SimulatorException when unable to load configuration file data.
     * expects non null values.
     */
    private void loadConfigFile(String[] args_) throws SimulatorException {
        try {
            eventSimConfigFile_ = args_[1];
            PropertyDocument data = LoadFileLocation.fromFileLocation(eventSimConfigFile_);
            if(!data.isMap() || ((PropertyMap) data).size() == 0)
                throw new SimulatorException("Invalid or null EventSim server configuration details.");
            serverConfiguration_ = data.getAsMap();
        } catch (FileNotFoundException e) {
            throw new SimulatorException("Error while reading Eventsim server config file: " + eventSimConfigFile_);
        } catch (IOException | ConfigIOParseException e) {
            throw new SimulatorException("Error while reading Eventsim server config file", e);
        }
    }

    /**
     * This method is used validate configuration file data
     */
    private void validateConfig() throws SimulatorException {
        if(!serverConfiguration_.containsKey("networkConfig"))
            throw new SimulatorException("Eventsim config file is missing 'networkConfig' entry.");
        if(!serverConfiguration_.containsKey("eventsimConfig"))
            throw new SimulatorException("Eventsim config file is missing 'eventsimConfig' entry.");
    }

    /**
     * This method is used to initialise instances
     * @throws SimulatorException when unable to create instances.
     */
    private void initialiseInstances(String[] args) throws SimulatorException, RESTClientException, RESTServerException {
        jsonParser_ = ConfigIOFactory.getInstance("json");
        bootParamsApi_ = new BootParameters();
        hwInvApi_ = new HardwareInventory();
        wlmApi = new WlmApi(log_);
        ApiReqData apiReq = new ApiReqData(log_);
        PropertyMap ntwkConfig = serverConfiguration_.getMapOrDefault("networkConfig", null);
        source_ = new NetworkObject(ntwkConfig, log_, apiReq);
        source_.initialise();
    }

    private String eventSimConfigFile_;
    private final Logger log_;
    private PropertyMap serverConfiguration_;
    protected ConfigIO jsonParser_;
    protected BootParameters bootParamsApi_;
    protected HardwareInventory hwInvApi_;
    protected NetworkObject source_;
    SimulatorEngine eventSimEngine;
    private NodeInformation nodeinfo;
    protected DataLoaderEngine simEngineDataLoader;
    protected WlmApi wlmApi;
}
