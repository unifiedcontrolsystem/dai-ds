package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.util.*;

/**
 * Description of class DataLoaderEngine.
 * load/validate configuration details of EventSim configuration file.
 * loads nodes data from volt and store in a map.
 * special apis to get the loaded data.
 */
public class DataLoaderEngine {

    DataLoaderEngine(PropertyMap engineconfiguration, NodeInformation nodeInfo, Logger log) throws SimulatorException {
        engineconfiguration_ = engineconfiguration;
        nodeInfo_ = nodeInfo;
        log_ = log;
        validateDataLoaderDetails();
    }

    /**
     * This method is validate loaded EventSim configuration file data.
     */
    private void validateDataLoaderDetails() throws SimulatorException {
        try {

            for(String key : EVENTSIM_CONFIG_KEYS) {
                if(engineconfiguration_.get(key) == null)
                    throw new SimulatorException("EventSim Configuration file doesn't contain '" + key + "' entry");
            }

            sensorMetadataLocation_ = engineconfiguration_.getString("SensorMetadata");
            rasMetadataLocation_ = engineconfiguration_.getString("RASMetadata");
            jobsMetadataLocation_ = engineconfiguration_.getString("JobsMetadata");
            bootParamsLocation_ = engineconfiguration_.getString("BootParameters");
            hwInventoryLocation_ = engineconfiguration_.getString("HWInventory");
            hwInventoryLocationPath_ = engineconfiguration_.getString("HWInventoryPath");
            hwInventoryQueryLocationPath_ = engineconfiguration_.getString("HWInventoryQueryPath");
            hwInventoryDiscStatusUrl_ = engineconfiguration_.getString("HWInventoryDiscStatUrl");
            eventCount_ = engineconfiguration_.getLong("eventCount");
            timeDelayMus_ = engineconfiguration_.getLong("timeDelayMus");
            randomizerSeed_ = engineconfiguration_.getLongOrDefault("randomizerSeed", -1);
            sensorPerEvent_ = engineconfiguration_.getLong("sensor-rate");
            eventsTemplate_ = engineconfiguration_.getMap("eventsTemplateConfig");
        } catch (PropertyNotExpectedType e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to load meta data of ras and sensor.
     */
    void loadData() throws SimulatorException {
        try {
            processSensorMetadata();
            processRASMetadata();
            loadJobsMetadata();
            loadSystemManifestFromDB();
            loadForeignLocationData();
            validateForeignLocationsWithDB();
            validateEventTemplate();
            loadEventTemplate();
        } catch (final IOException | ConfigIOParseException | PropertyNotExpectedType e) {
           throw new SimulatorException("Error while loading data into data loader engine: " + e.getMessage());
        }
    }

    /**
     * This method is used to load jobs metadata from resources
     */
    private void loadJobsMetadata() throws IOException, ConfigIOParseException {
        definitionJobsMetadata_ = loadJobsMetadataFromJSON();
    }

    /**
     * This method is used to validate all locations in db has a mapping foreign name
     */
    private void validateForeignLocationsWithDB() throws SimulatorException {
        List<String> locations = getNodeLocationData();
        locations.addAll(getNonNodeLocationData());
        if(!allLocations.containsAll(locations)) {
            throw new SimulatorException("Not all locations in database has the mapping foreign location");
        }
    }

    /**
     * This method is used to fetch all foreign location.
     */
    private void loadForeignLocationData() {
       allLocations = CommonFunctions.getLocations();
    }

    /**
     * This method is used to process sensor data.
     */
    private void processSensorMetadata() throws IOException, ConfigIOParseException {
        PropertyMap sensorMetadata = loadSensorMetadataFromJSON();
        if (sensorMetadata == null)
            throw new RuntimeException("Unable to process SensorMetadata");
        Set<String> eventIdList = sensorMetadata.keySet();
        PropertyArray denseRackEventList = new PropertyArray();
        PropertyArray denseChassisEventList = new PropertyArray();
        PropertyArray desnseComputeNodeEventList = new PropertyArray();

        definitionSensorMetadata_ = new PropertyMap();

        for(String id: eventIdList) {
            PropertyMap event = sensorMetadata.getMapOrDefault(id, null);
            if(event != null) {
                String description = event.getStringOrDefault("description", "UNKNOWN");
                event.put("id", id);
                if (sortEventByDefinition(description) == EventDefinitionType.DENSE_RACK)
                    denseRackEventList.add(event);
                else if (sortEventByDefinition(description) == EventDefinitionType.DENSE_CHASSIS)
                    denseChassisEventList.add(event);
                else if (sortEventByDefinition(description) == EventDefinitionType.DENSE_COMPUTE_NODE)
                    desnseComputeNodeEventList.add(event);
            }
        }

        definitionSensorMetadata_.put("Rack", denseRackEventList);
        definitionSensorMetadata_.put("Chassis", denseChassisEventList);
        definitionSensorMetadata_.put("Blade", desnseComputeNodeEventList);
        definitionSensorMetadata_.put("ComputeNode", desnseComputeNodeEventList);
        definitionSensorMetadata_.put("ServiceNode", desnseComputeNodeEventList);
    }

    /**
     * This method is used to load sensor metadata from a file
     */
    private PropertyMap loadSensorMetadataFromJSON()  throws IOException, ConfigIOParseException {
        return LoadFileLocation.fromResources(sensorMetadataLocation_).getAsMap();
    }

    /**
     * This method is used to load jobs metadata from a file
     */
    private PropertyMap loadJobsMetadataFromJSON()  throws IOException, ConfigIOParseException {
        return LoadFileLocation.fromResources(jobsMetadataLocation_).getAsMap();
    }

    /**
     * This method is used to process the sensor data.
     */
    private EventDefinitionType sortEventByDefinition(String eventDescription) {
        if (eventDescription.matches(denseRackPattern)) {
            return EventDefinitionType.DENSE_RACK;
        }
        if (eventDescription.matches(denseComputeNodePattern)) {
            return EventDefinitionType.DENSE_COMPUTE_NODE;
        }
        if (eventDescription.matches(denseChassisPattern) || eventDescription.matches(genericDenseChassisPattern)) {
            return EventDefinitionType.DENSE_CHASSIS;
        }

        log_.info("Event '" + eventDescription + "' didn't match with a known definition");
        return EventDefinitionType.UNKNOWN;
    }

    /**
     * This method is used to process ras meta data.
     */
    private void processRASMetadata() throws IOException, ConfigIOParseException {
        PropertyMap rasEvents = loadRASMetadataFromJSON();
        rasMetadata_ = new ArrayList<>(rasEvents.keySet());
    }

    /**
     * This method is used to load ras metadata from a file
     */
    private PropertyMap loadRASMetadataFromJSON()  throws IOException, ConfigIOParseException {
        return LoadFileLocation.fromResources(rasMetadataLocation_).getAsMap();
    }

    /**
     * This method is used to load locations data from db.
     */
    private void loadSystemManifestFromDB() throws SimulatorException {
        try {
            loadNodeLocations();
            loadNonNodeLocations();
            loadHostnames();
        } catch (final DataStoreException e) {
            throw new SimulatorException("Error while fetching location info from database", e);
        }
    }

    private void loadNonNodeLocations() {    }

    /**
     * This method is used to load cpmpute/service node locations.
     */
    private void loadNodeLocations() throws DataStoreException {
        nodeLocationdata_ = nodeInfo_.getNodeLocations();
    }

    /**
     * This method is used to load hostnames for a respective locations in a list.
     */
    private void loadHostnames() throws DataStoreException {
        nodeHostnamedata_ = new ArrayList<>(nodeInfo_.getComputeHostnameFromLocationMap().values());
    }

    /**
     * This method is used to load events template files data
     * @throws PropertyNotExpectedType If attribute contains incorrect data format
     */
    private void loadEventTemplate() throws PropertyNotExpectedType {
        fabricTemplate_ = eventsTemplate_.getMap("fabric").getString("eventTemplate");
        fabricStreamId_ = eventsTemplate_.getMap("fabric").getString("streamId");
    }

    /**
     * This method is used to validate configuration data
     * @throws PropertyNotExpectedType If attribute contains incorrect data format
     */
    private void validateEventTemplate() throws PropertyNotExpectedType {
        for (String event : EVENTS) {
            if (eventsTemplate_.get(event) == null)
                log_.info("EventSim Configuration file doesn't contain '" + event + "' entry");
            for (String key : EVENTS_TEMPLATE_KEYS) {
                if (eventsTemplate_.getMap(event).getString(key) == null)
                    log_.info("EventSim Configuration file doesn't contain '" + event + "' entry");
            }
        }
    }

    private enum EventDefinitionType {
        DENSE_RACK,
        DENSE_CHASSIS,
        DENSE_COMPUTE_BLADE,
        DENSE_COMPUTE_NODE,
        UNKNOWN
    }

    List<String> getNodeHostnameData() { return nodeHostnamedata_; }
    PropertyDocument getSensorMetaData() { return definitionSensorMetadata_;}
    List<String> getRasMetaData() {return rasMetadata_;}
    PropertyDocument getJobsMetaData() {return definitionJobsMetadata_;}
    List<String> getNodeLocationData() {return nodeLocationdata_;}
    List<String> getNonNodeLocationData() {return nonNodeLocationdata_;}
    long getDefaultNumberOfEventsToBeGenerated() {return eventCount_;}
    long getDefaultTimeDelayMus() {return timeDelayMus_;}
    long getDefaultRandomiserSeed() {return randomizerSeed_;}
    long getDefaultSensorsPerEvent() {return sensorPerEvent_;}
    public String getBootParamsFileLocation() { return bootParamsLocation_;}
    public String getHwInventoryFileLocation() {return hwInventoryLocation_;}
    public String getHwInventoryDiscStatusUrl() {return hwInventoryDiscStatusUrl_;}
    public String getHwInventoryFileLocationPath() {return hwInventoryLocationPath_;}
    public String getHwInventoryQueryLocationPath() {return hwInventoryQueryLocationPath_;}
    public String getFabricEventFileTemplate() {return  fabricTemplate_;}
    public String getFabricEventStreamId() {return fabricStreamId_;}
    private Collection<Object> allLocations = null;
    private static final String denseRackPattern = "^CC_.*";
    private static final String denseChassisPattern ="^BC_(T|V|P|F|I|L)_NODE[0-3]_(?!(CPU[0-3]|KNC)).*";
    private static final String genericDenseChassisPattern = "^BC_.*";
    private static final String denseComputeNodePattern = "^BC_(T|V|P|F|I|L)_NODE[0-3]_(CPU[0-3]|KNC).*";
    private final PropertyMap engineconfiguration_;
    private final Logger log_;
    private final NodeInformation nodeInfo_;
    private String sensorMetadataLocation_;
    private String rasMetadataLocation_;
    private String jobsMetadataLocation_;
    private long eventCount_;
    private long timeDelayMus_;
    private long randomizerSeed_;
    private long sensorPerEvent_;
    private String[] EVENTSIM_CONFIG_KEYS = {"SensorMetadata", "RASMetadata", "JobsMetadata", "BootParameters",
            "HWInventory", "HWInventoryPath", "HWInventoryQueryPath",
            "HWInventoryDiscStatUrl", "eventsTemplateConfig"};
    private String[] EVENTS_TEMPLATE_KEYS = {"eventTemplate", "streamId"};
    private String[] EVENTS = {"fabric"};

    private PropertyMap definitionSensorMetadata_;
    private PropertyMap definitionJobsMetadata_;
    private ArrayList<String> rasMetadata_;
    private List<String> nodeLocationdata_;
    private List<String> nonNodeLocationdata_ = new ArrayList<>();
    private List<String> nodeHostnamedata_;
    private String bootParamsLocation_;
    private String hwInventoryLocation_;
    private String hwInventoryDiscStatusUrl_;
    private String hwInventoryLocationPath_;
    private String hwInventoryQueryLocationPath_;
    private PropertyMap eventsTemplate_;
    private String fabricTemplate_;
    private String fabricStreamId_;
}
