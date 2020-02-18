// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.Timestamp;


public class EventSimEngine implements Runnable {
    private boolean constantMode_;
    PropertyMap appConfiguration_;
    private String systemManifestLocation_;
    private String sensorMetadataLocation_;
    private String rasMetadataLocation_;
    private int eventCount_;
    private long timeDelayMus_;
    private SystemManifest system_;
    private WlmApi wlmApi_;
    private ConfigIO parser_;

    int eventRatio_ = 1;
    String randomizerSeed_;
    String rowCount_;
    Logger logger_;
    PropertyMap systemManifestJSON_;
    PropertyMap definitionSensorMetadata_;
    List<String> rasMetadata_;
    ConcurrentLinkedQueue<PublishData> events_;
    int outstandingEventCount_ = 0;
    AtomicBoolean running_ = new AtomicBoolean(false);
    boolean active = false;
    NetworkSource source_;
    ConnectionManager connMan_;
    private int publishedEvents;


    public EventSimEngine(NetworkSource source, ConnectionManager connMan, Logger log) throws PropertyNotExpectedType {
        source_ = source;
        connMan_ = connMan;
        events_ = new ConcurrentLinkedQueue<>();
        logger_ = log;
        system_ = new SystemManifest(logger_);
        wlmApi_ = new WlmApi(logger_);
        parser_ = ConfigIOFactory.getInstance("json");
        assert parser_ != null: "Failed to create a JSON parser!";
        loadAppConfiguration();
        validateAppConfiguration();
    }

    private void loadAppConfiguration() throws PropertyNotExpectedType {
        appConfiguration_ = source_.getAppConfiguration();
    }

    private enum EventDefinitionType {
        DENSE_RACK,
        DENSE_CHASSIS,
        DENSE_COMPUTE_BLADE,
        DENSE_COMPUTE_NODE,
        UNKNOWN
    }

    public void initialize() throws PropertyNotExpectedType, IOException, ConfigIOParseException {
        loadMetadata();
        systemManifestJSON_ = loadSystemManifestFromJSON();
        upperCaseAllNames(systemManifestJSON_);
        system_.generateSystem(systemManifestJSON_, eventRatio_, randomizerSeed_, rowCount_, definitionSensorMetadata_, rasMetadata_, events_);
        System.out.println("System Hierarchy");
        System.out.println(system_.getHierarchy());
    }

    // VoltDB stores all locations in upper case, do the same here.
    private void upperCaseAllNames(PropertyMap map) {
        for(String key: map.keySet()) {
            logger_.debug("*** Converting to upper case: key = %s", key);
            if(key.equals("name")) {
                logger_.debug("*** >>> Changing '%s' to '%s'", map.getStringOrDefault(key, null),
                        map.getStringOrDefault(key, null).toUpperCase());
                map.put(key, map.getStringOrDefault(key, null).toUpperCase());
            }
            else if(map.getMapOrDefault(key, null) != null)
                upperCaseAllNames(map.getMapOrDefault(key, null));
            else if(map.getArrayOrDefault(key, null) != null)
                upperCaseAllNames(map.getArrayOrDefault(key, null));
        }
    }

    private void upperCaseAllNames(PropertyArray list) {
        for(Object obj: list) {
            if(obj instanceof PropertyDocument) {
                if(obj instanceof PropertyMap)
                    upperCaseAllNames((PropertyMap)obj);
                else
                    upperCaseAllNames((PropertyArray)obj);
            }
        }
    }

    public void run() {
        boolean display = false;
        int publishedEventCount = 0;
        try {
            running_ .set(true);
            while(running_.get()) {
                while (events_.size() > 0) {
                    PublishData ev = events_.poll(); // NOTE: This design is causing the slowdown in eventsim,
                                                     //       need non-synchronized batch execution.
                    if (ev != null) {
                        active = true;
                        display = true;
                        if(ev.subject_.equals("stateChanges")) {
                            for (ConnectionObject conn : connMan_.getConnections()) {
                                conn.publish(conn.url_, ev.message_);
                            }
                        }
                        else
                            source_.sendMessage(ev.subject_, ev.message_);
                        outstandingEventCount_--;
                        publishedEventCount++;
                        publishedEvents = publishedEventCount;
                        if((outstandingEventCount_ % 1000) == 0)
                            System.out.format("Mode='%s'; Remaining Messages: %d\r",
                                    constantMode_?CONSTANT_MODE:BURST_MODE, outstandingEventCount_);
                        if(constantMode_)
                            delayMicroSecond(timeDelayMus_);
                    }
                }
                if(display) {
                    System.out.println("Empty queue: Throttle down for a second; Published: " + publishedEventCount);
                    publishedEventCount = 0;
                    display = false;
                    active = false;
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to publish events");
            running_.set(false);
            e.printStackTrace();
        }
    }

    public static void delayMicroSecond(long delayTimeMus) {
        long waitUntil = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(delayTimeMus);
        while( waitUntil > System.nanoTime());
    }

    public int getOutstandingEventCount() {
        return outstandingEventCount_;
    }

    public int getPublishedEventCount() {
        return publishedEvents;
    }

    public void publishBatchForLocation(String regex, String regexLabel, String count) throws Exception{
        loadDefaults();
        if(regexLabel == null)
            regexLabel = ".*";
        if(regex == null)
            regex = ".*";
        if(count != null)
            eventCount_ = Integer.parseInt(count);
        if(ExistsLocationsMatchedRegex(regex,"a")) {
            system_.publishEventsFromLocation(eventCount_,regex, regexLabel);
            outstandingEventCount_ += eventCount_;
        }
    }

    public void publishRasEvents(String regex, String regexLabel, String count, String mode) throws Exception {
        loadDefaults();
        if(mode != null)
            constantMode_ = Boolean.parseBoolean(mode);
        if(count != null)
            eventCount_ = Integer.parseInt(count);
        if(regexLabel == null)
            regexLabel = ".*";
        if(regex == null)
            regex = ".*";
        if(ExistsLocationsMatchedRegex(regex,"r")) {
            system_.publishRASEventsFromLocation(eventCount_, regex, regexLabel);
            outstandingEventCount_ += eventCount_;
        }
    }

    public void publishSensorEvents(String regex, String regexLabel, String count, String mode) throws Exception {
        loadDefaults();
        if(mode != null)
            constantMode_ = Boolean.parseBoolean(mode);
        if(count != null)
            eventCount_ = Integer.parseInt(count);
        if(regexLabel == null)
            regexLabel = ".*";
        if(regex == null)
            regex = ".*";
        if(ExistsLocationsMatchedRegex(regex,"s")) {
            system_.publishSensorEventsFromLocation(eventCount_, regex, regexLabel);
            outstandingEventCount_ += eventCount_;
        }
    }

    public void publishBootEvents(String regex, String probabiltyValue, String mode) {
        float bfValue = 0;
        if(mode != null)
            constantMode_ = Boolean.parseBoolean(mode);
        if(probabiltyValue != null)
            bfValue = Float.parseFloat(probabiltyValue);
        if(regex == null)
            regex = ".*";
        if(ExistsLocationsMatchedRegex(regex,"b"))
            system_.publishBootSequenceFromLocation(regex, bfValue);
    }

    public void createReservation(Map<String, String> parameters) throws Exception {

        String name = parameters.get("name");
        String users = parameters.get("users");
        String nodes = parameters.get("nodes");
        Timestamp starttime = Timestamp.valueOf(parameters.get("starttime"));
        String duration = parameters.get("duration");

        if(nodes.equals("random"))
            nodes = pickRandomNodes();

        wlmApi_.createReservation(name, users, nodes, starttime, duration);
    }

    public void modifyReservation(Map<String, String> parameters) throws Exception {

        String name = parameters.get("name");
        String users = parameters.get("users");
        String nodes = parameters.get("nodes");
        String starttime = parameters.get("starttime");
        wlmApi_.modifyReservation(name, users, nodes, starttime);
    }

    public void deleteReservation(Map<String, String> parameters) throws Exception {

        String name = parameters.get("name");
        wlmApi_.deleteReservation(name);
    }

    public void startJob(Map<String, String> parameters) throws Exception {

        String jobid = parameters.get("jobid");
        String name = parameters.get("name");
        String users = parameters.get("users");
        String nodes = parameters.get("nodes");
        Timestamp starttime = Timestamp.valueOf(parameters.get("starttime"));
        String workdir = parameters.get("workdir");

        if(nodes.equals("random"))
            nodes = pickRandomNodes();

        wlmApi_.startJob(jobid, name, users, nodes, starttime, workdir);
    }

    public void terminateJob(Map<String, String> parameters) throws Exception {

        String jobid = parameters.get("jobid");
        String name = parameters.get("name");
        String users = parameters.get("users");
        String nodes = parameters.get("nodes");
        Timestamp starttime = Timestamp.valueOf(parameters.get("starttime"));
        String workdir = parameters.get("workdir");
        String exitStatus = parameters.get("exitstatus");

        if(nodes.equals("random"))
            nodes = pickRandomNodes();

        wlmApi_.terminateJob(jobid, name, users, nodes, starttime, workdir, exitStatus);
    }

    public void simulateWlm(Map<String, String> parameters) throws Exception {

        String reservations = parameters.get("reservations");
        String[] nodes = new String[0];
        wlmApi_.simulateWlm(reservations, nodes);
    }

    public String pickRandomNodes() {

        return "";
    }

    private boolean ExistsLocationsMatchedRegex(String regex, String eventType) {
        if (system_.getRegexMatchedComponentsCount(regex, eventType) == 0) {
            running_.set(false);
            if(eventType.equals("b"))
                throw new RuntimeException("No Matched Regex Locations to start Boot Sequence.");
            else if(eventType.equals("r"))
                throw new RuntimeException("No Matched Regex Locations to generate RAS Events.");
            else if(eventType.equals("s"))
                throw new RuntimeException("No Matched Regex Locations to generate Sensor Events.");
            else if(eventType.equals("a"))
                throw new RuntimeException("No Matched Regex Locations to generate RAS+SEnsor Events.");
            return false;
        }
        return true;
    }

    private void loadDefaults() throws PropertyNotExpectedType {
        eventCount_ = appConfiguration_.getInt("eventCount");
        timeDelayMus_ = appConfiguration_.getLongOrDefault("timeDelayMus",1);
    }

    public void stopPublishing() {
        System.out.println("Stop Publishing");
        running_.set(false);
    }

    private void validateAppConfiguration() throws PropertyNotExpectedType {
        if (!appConfiguration_.containsKey("SystemManifest"))
            throw new RuntimeException("EventSim Configuration file doesn't contain SystemManifest location");

        if (!appConfiguration_.containsKey("SensorMetadata"))
            throw new RuntimeException("EventSim Configuration file doesn't contain SensorMetadata location");

        if (!appConfiguration_.containsKey("RASMetadata"))
            throw new RuntimeException("EventSim Configuration file doesn't contain RASMetadata location");

        if(!appConfiguration_.containsKey("eventCount"))
            throw new RuntimeException("EventSim Configuration file doesn't contain 'eventCount' entry");

        if(!appConfiguration_.containsKey("timeDelayMus"))
            throw new RuntimeException("EventSim Configuration file doesn't contain 'timeDelayMus' entry");

        if(!appConfiguration_.containsKey("eventRatioSensorToRas"))
            throw new RuntimeException("EventSim Configuration file doesn't contain 'eventRatioSensorToRas' entry");

        if(!appConfiguration_.containsKey("randomizerSeed"))
            throw new RuntimeException("EventSim Configuration file doesn't contain 'randomizerSeed' entry");

        systemManifestLocation_ = appConfiguration_.getString("SystemManifest");
        sensorMetadataLocation_ = appConfiguration_.getString("SensorMetadata");
        rasMetadataLocation_ = appConfiguration_.getString("RASMetadata");
        eventCount_ = appConfiguration_.getInt("eventCount");
        timeDelayMus_ = appConfiguration_.getLongOrDefault("timeDelayMus",1);
        randomizerSeed_ = appConfiguration_.getStringOrDefault("randomizerSeed","1");
        rowCount_ = appConfiguration_.getStringOrDefault("rowCount", "1");
    }


    private void loadMetadata() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        ProcessSensorMetadata();
        ProcessRASMetadata();
    }

    private void ProcessSensorMetadata() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        PropertyMap sensorMetadata = loadSensorMetadataFromJSON();
        if (sensorMetadata == null)
            throw new RuntimeException("Unable to process SensorMetadata");
        Set<String> eventIdList = sensorMetadata.keySet();
        PropertyArray denseRackEventList = new PropertyArray();
        PropertyArray denseChassisEventList = new PropertyArray();
        PropertyArray desnseComputeNodeEventList = new PropertyArray();

        definitionSensorMetadata_ = new PropertyMap();

        for(String id: eventIdList) {
            PropertyMap event = sensorMetadata.getMap(id);
            if(event != null) {
                String description = event.getStringOrDefault("description", "UNKNOWN");
                if(event != null) {
                    event.put("id", id);
                    if (sortEventByDefinition(description) == EventDefinitionType.DENSE_RACK)
                        denseRackEventList.add(event);
                    else if (sortEventByDefinition(description) == EventDefinitionType.DENSE_CHASSIS)
                        denseChassisEventList.add(event);
                    else if (sortEventByDefinition(description) == EventDefinitionType.DENSE_COMPUTE_NODE)
                        desnseComputeNodeEventList.add(event);
                }
            }
        }

        definitionSensorMetadata_.put("Rack", denseRackEventList);
        definitionSensorMetadata_.put("Chassis", denseChassisEventList);
        definitionSensorMetadata_.put("Blade", desnseComputeNodeEventList);
        definitionSensorMetadata_.put("ComputeNode", desnseComputeNodeEventList);
        definitionSensorMetadata_.put("ServiceNode", desnseComputeNodeEventList);
    }

    /**
     * This method is temporarily used to bucket-ize the sensor metadata based on the definitions defined in the
     * A21 System Manifest. Once more clarity is obtained from customer, we can format our ForeignSensorMetaData.json
     * to contain detailed definitions for each event's occurrence
     */
    private EventDefinitionType sortEventByDefinition(String eventDescription) {
        String denseRackPattern = new String("^CC_.*");
        String denseChassisPattern = new String("^BC_(T|V|P|F|I|L)_NODE[0-3]_(?!(CPU[0-3]|KNC)).*");
        String genericDenseChassisPattern = new String("^BC_.*");
        String denseComputeNodePattern = new String("^BC_(T|V|P|F|I|L)_NODE[0-3]_(CPU[0-3]|KNC).*");

        if (eventDescription.matches(denseRackPattern)) {
            return EventDefinitionType.DENSE_RACK;
        }
        if (eventDescription.matches(denseComputeNodePattern)) {
            return EventDefinitionType.DENSE_COMPUTE_NODE;
        }
        if (eventDescription.matches(denseChassisPattern) | eventDescription.matches(genericDenseChassisPattern)) {
            return EventDefinitionType.DENSE_CHASSIS;
        }

        System.out.println("Event '" + eventDescription + "' didn't match with a known definition");
        return EventDefinitionType.UNKNOWN;
    }

    private void ProcessRASMetadata() throws IOException, ConfigIOParseException {
        PropertyMap rasEvents = loadRASMetadataFromJSON();
        rasMetadata_ = new ArrayList<>(rasEvents.keySet());
    }

    public PropertyMap loadSystemManifestFromJSON() throws IOException, ConfigIOParseException {
        return LoadConfigFile.fromFileLocation(systemManifestLocation_).getAsMap();
    }

    public PropertyMap loadSensorMetadataFromJSON()  throws IOException, ConfigIOParseException {
        return LoadConfigFile.fromResource(sensorMetadataLocation_).getAsMap();
    }

    public PropertyMap loadRASMetadataFromJSON()  throws IOException, ConfigIOParseException {
        return LoadConfigFile.fromResource(rasMetadataLocation_).getAsMap();
    }

    private static final String CONSTANT_MODE = "constant";
    private static final String BURST_MODE = "burst";
}
