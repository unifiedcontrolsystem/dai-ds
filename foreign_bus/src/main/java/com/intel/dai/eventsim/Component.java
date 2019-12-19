// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.logging.Logger;
import com.intel.partitioned_monitor.DataTransformerException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Component {
    private static final String BOOT_IMAGE_INFO_FILE = "/opt/ucs/etc/BootParameters.json";

    private String name_;
    private String foreignName_;
    private String definition_;
    private String type_;
    private String fullLocation_;
    private String foreignFullLocation_;
    private Map<String, Component> subcomponents_;
    private PropertyMap systemManifestJson_;
    private String rowCountStr_;
    private PropertyArray bootImageInfo_;

    private ArrayDeque<PublishData> eventQueue_;
    private int SensorEventToRASEventRatio = 1;
    private int randomizerSeed;
    private static int remToPublishCount;
    private static int intProbabilityValue;
    public int rowCount_ = 1;
    public long startTimeLong_ = 1661025364;

    PropertyMap definitionSensorMetadata_;
    List<String> rasMetadata_;
    String randomizerSeedStr;
    String startTime_;
    Random myRandomGenerator_;
    Random myRandomGeneratorSensor_;
    PropertyMap views_;
    PropertyMap full_;
    PropertyMap defintions_;
    PropertyMap definitionObject_;
    ArrayList<String> failedRegexMatchedNodes;
    int checkCount = 0;
    Logger logger_;

    public Component(Map<String, String> details, PropertyMap systemManifestJson,
                     ArrayDeque<PublishData> eventQueue, int ratio, PropertyMap definitionSensorMetadata,
                     List<String> rasMetadata, Logger logger) throws PropertyNotExpectedType {
        logger_ = logger;
        initComponent(details, systemManifestJson, eventQueue, ratio,
                definitionSensorMetadata, rasMetadata);
    }

    public String getName() {
        return name_;
    }

    private void initComponent(Map<String, String> details, PropertyMap systemManifestJson,
                               ArrayDeque<PublishData> eventQueue, int ratio, PropertyMap definitionSensorMetadata,
                               List<String> rasMetadata) throws PropertyNotExpectedType {
        name_ = details.get("name");
        if (name_ == null)
            throw new RuntimeException("Missing name of component while initializing it");

        definition_ = details.get("definition");
        if(definition_ == null)
            throw new RuntimeException("Missing definition of component while initializing it");

        String parentLocation = details.get("parentLocation");
        if(parentLocation == null) {
            throw new RuntimeException("Missing parentLocation of component while initializing it");
        } else if (parentLocation != "") {
            fullLocation_ = new String(parentLocation + "-" + name_);
        } else {
            fullLocation_ = new String(name_);
        }

        randomizerSeedStr = details.get("randomizerSeed");
        assert randomizerSeedStr != null : "Missing randomizerSeed";
        randomizerSeed = Integer.parseInt(randomizerSeedStr);

        myRandomGenerator_ =  new Random(randomizerSeed);
        myRandomGeneratorSensor_ = new Random();
        rowCountStr_ = details.get("rowCount");
        assert rowCountStr_ != null : "Missing Total row count";
        rowCount_ = Integer.parseInt(rowCountStr_);

        startTime_ = details.get("startTime");
        assert startTime_ != null : "Missing Start Time";
        startTimeLong_ = Long.valueOf(startTime_);

        fullLocation_.concat(name_);
        systemManifestJson_ = systemManifestJson;

        views_ = systemManifestJson_.getMap("views");
        if (views_ == null)
            throw new RuntimeException("Missing 'views' in SystemManifest");

        full_ = views_.getMap("Full");
        if (full_ == null)
            throw new RuntimeException("Missing 'views-full' in SystemManifest");

        defintions_ = full_.getMap("definitions");
        if (defintions_ == null)
            throw new RuntimeException("Missing 'views-full-defintions' in SystemManifest");

        definitionObject_ = defintions_.getMap(definition_);
        if (definitionObject_ == null)
            throw new RuntimeException("Missing " + definition_ + " under 'views-full-defintions' in SystemManifest");

        type_ = definitionObject_.getStringOrDefault("type", "UNKNOWN");

        foreignName_ = getForeignName();

        foreignFullLocation_ = details.get("parentForeignLocation") + foreignName_;
        definitionSensorMetadata_ = definitionSensorMetadata;
        rasMetadata_ = rasMetadata;
        eventQueue_ = eventQueue;
        SensorEventToRASEventRatio = ratio;
        subcomponents_ = initSubcomponents();
        try {
            bootImageInfo_ = LoadConfigFile.fromFileLocation(BOOT_IMAGE_INFO_FILE).getAsMap().
                    getMapOrDefault("boot-images", new PropertyMap()).getArrayOrDefault("content", new PropertyArray());
        } catch(IOException | ConfigIOParseException e) {
            bootImageInfo_ = new PropertyArray();
        }
    }

    private Map<String, Component> initSubcomponents() throws PropertyNotExpectedType {
        Map<String, Component> subcomponents = new HashMap<>();
        PropertyArray contentList = definitionObject_.getArray("content");
        if(contentList != null) {
            for (int i = 0; i < contentList.size(); i++) {
                PropertyMap item = contentList.getMap(i);
                if(item != null) {
                    String name = item.getString("name");
                    String definition = item.getStringOrDefault("definition", "UNKNOWN");
                    Map<String,String> details = new HashMap<String, String>();
                    details.put("name", name);
                    details.put("definition", definition);
                    details.put("parentLocation", fullLocation_);
                    details.put("parentForeignLocation", foreignFullLocation_);
                    details.put("randomizerSeed", randomizerSeedStr);
                    details.put("rowCount", rowCountStr_);
                    details.put("startTime", startTime_);
                    try {
                        Component sub = new Component(details, systemManifestJson_, eventQueue_, SensorEventToRASEventRatio,
                                definitionSensorMetadata_, rasMetadata_, logger_);
                        subcomponents.put(name, sub);
                    } catch (Exception e) {
                        logger_.exception(e);
                        logger_.debug("Error encountered while adding adding component: " + name + " - " + definition);
                    }
                }
            }
        }
        return subcomponents;
    }

    private String getForeignName() {
        String foreignName = new String();
        if(type_.equals("Rack")) {
            // Check if '-' is present in the rack
            if(name_.contains("-")) {
                // Contains '-' -> Create new name based on row, column
                String[] parts = name_.split("-");
                if(parts.length == 2) {
                    String rowID = parts[0].substring(1);
                    String colID = parts[1];
                    foreignName = "c"+rowID+"-"+colID;
                } else {
                    // Set a default value when Row names are in a different format
                    foreignName = "cX-Y";
                }
            } else {
                // Doesn't contain '-' Covert into a row & column based on the row count
                String rowIDStr = name_.replaceAll("\\D+", "");
                int rowID = Integer.parseInt(rowIDStr);
                int foreignRowID = rowID/rowCount_;
                int foreignColID = rowID%rowCount_;
                foreignName = "c"+String.valueOf(foreignRowID)+"-"+String.valueOf(foreignColID);
            }
        } else if (type_.equals("Chassis")){
            foreignName = "c"+name_.substring(2);
        } else if (type_.equals("Blade")) {
            foreignName = "s"+name_.substring(2);
        } else if ((type_.equals("ComputeNode")) || (type_.equals("ServiceNode"))) {
            foreignName = "n"+name_.substring(2);
        } else {
            throw new RuntimeException ("Invalid foreignName - will not add to hierarchy");
        }
        return foreignName;
    }

    public Map<String, Component> getSubcomponents() {
        return subcomponents_;
    }

    public int getTotalSubcomponentsCount() {
        int count = 0;
        for(Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            count += entry.getValue().getTotalSubcomponentsCount();
        }
        return count + subcomponents_.size();
    }

    public String getLocation() {
        return fullLocation_;
    }

    public String getHierarchy() {
        return getHierarchy(1,"");
    }

    public String getHierarchy(int level, String componentFullName) {
        StringBuilder str = new StringBuilder();
        if(!componentFullName.isEmpty()) {
            str.append(componentFullName);
            str.append("-");
        }
        str.append(getName());
        str.append("\n");
        for(Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            if(!componentFullName.isEmpty()) {
                str.append(entry.getValue().getHierarchy(level + 1, componentFullName + "-" + getName()));
            } else {
                str.append(entry.getValue().getHierarchy(level + 1, getName()));
            }
        }
        return str.toString();
    }

    public int publishRemEvents(int eventCount, String regex, int publishCount, String regexLabel) throws RuntimeException, PropertyNotExpectedType {
        remToPublishCount = eventCount;
        if(checkCount == 0 && !(regexLabel.equals(".*"))) {
            checkCount++;
            ArrayList<String> temp = new ArrayList<>();
            for (String object: rasMetadata_) {
                if(object.matches(regexLabel))
                    temp.add(object);
            }
            rasMetadata_ = temp;
            PropertyMap definitionSensorMetadata_new = new PropertyMap();
            PropertyArray denseRackEventList = new PropertyArray();
            PropertyArray denseChassisEventList = new PropertyArray();
            PropertyArray compute = new PropertyArray();
            PropertyArray blade = new PropertyArray();
            PropertyArray service = new PropertyArray();
            PropertyArray denseRackEventList11 = definitionSensorMetadata_.getArray("Rack");
            PropertyArray denseChassisEventList1 = definitionSensorMetadata_.getArray("Chassis");
            PropertyArray bladeold = definitionSensorMetadata_.getArray("Blade");
            PropertyArray computeold = definitionSensorMetadata_.getArray("ComputeNode");
            PropertyArray serviceold = definitionSensorMetadata_.getArray("ServiceNode");
            if(denseRackEventList11 != null) {
                for (int i = 0; i < denseRackEventList11.size(); i++) {
                    PropertyMap a = denseRackEventList11.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            denseRackEventList.add(a);
                    }
                }
            }
            if(denseChassisEventList1 != null ) {
                for (int i = 0; i < denseChassisEventList1.size(); i++) {
                    PropertyMap a = denseChassisEventList1.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            denseChassisEventList.add(a);
                    }
                }
            }
            if(bladeold != null) {
                for (int i = 0; i < bladeold.size(); i++) {
                    PropertyMap a = bladeold.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            blade.add(a);
                    }
                }
            }
            if(computeold != null) {
                for (int i = 0; i < computeold.size(); i++) {
                    PropertyMap a = computeold.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            compute.add(a);
                    }
                }
            }
            if(serviceold != null) {
                for (int i = 0; i < serviceold.size(); i++) {
                    PropertyMap a = serviceold.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            service.add(a);
                    }
                }
            }
            definitionSensorMetadata_new.put("Rack", denseRackEventList);
            definitionSensorMetadata_new.put("Chassis", denseChassisEventList);
            definitionSensorMetadata_new.put("Blade", blade);
            definitionSensorMetadata_new.put("ComputeNode", compute);
            definitionSensorMetadata_new.put("ServiceNode", service);
            definitionSensorMetadata_=definitionSensorMetadata_new;
            denseRackEventList11 = definitionSensorMetadata_.getArray("Rack");
            denseChassisEventList1 = definitionSensorMetadata_.getArray("Chassis");
            bladeold = definitionSensorMetadata_.getArray("Blade");
            computeold = definitionSensorMetadata_.getArray("ComputeNode");
            serviceold = definitionSensorMetadata_.getArray("ServiceNode");
            boolean checkEmpty = (denseRackEventList11 != null && denseRackEventList11.size() == 0) && (denseChassisEventList1 != null && denseChassisEventList1.size() == 0) && (bladeold != null && bladeold.size() == 0) && (computeold!= null && computeold.size() == 0) && (serviceold != null && serviceold.size() == 0);
            if(checkEmpty && rasMetadata_.size() == 0)
                throw new RuntimeException("No Matched RAS/Sensor Data to generate events.");
        }
        return publishEventsForLocation(publishCount, regex);
    }

    public int publishEventsForLocation(int eventCount, String regex) throws PropertyNotExpectedType, RuntimeException {
        // Equally divide amongst the current component and the subcomponents
        int count = 0;
        if(getLocation().matches(regex)) {
            int rasEventCount = eventCount / (SensorEventToRASEventRatio + 1);
            int sensorEventCount = eventCount - rasEventCount;

            for (int i = 0; i < rasEventCount; i++) {
                ForeignEvent ev = createRandomRASEvent(i + randomizerSeed);
                if (ev != null) {
                    PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                    eventQueue_.add(data);
                } else {
                    System.out.println("Unable to create RAS event");
                    i--;
                }
            }
            for (int i = 1; i <= sensorEventCount; i++) {
                ForeignEvent ev = createRandomSensorEvent(i + randomizerSeed);
                if (ev != null) {
                    PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                    eventQueue_.add(data);
                } else {
                    System.out.println("Unable to create Sensor event");
                    i--;
                }
            }
            remToPublishCount = remToPublishCount - rasEventCount - sensorEventCount;
            count += eventCount;
        }
        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            if(!(remToPublishCount > 0))
                break;
            count += entry.getValue().publishEventsForLocation(eventCount, regex);
        }
        return count;
    }

    public int publishRemRASEvents(int eventCount, String regex, int publishCount, String regexLabel) throws RuntimeException {
        remToPublishCount = eventCount;
        if(checkCount == 0 && !(regexLabel.equals(".*"))) {
            checkCount++;
            ArrayList<String> temp = new ArrayList<>();
            for (String object: rasMetadata_) {
                if(object.matches(regexLabel))
                    temp.add(object);
            }
            rasMetadata_ = temp;
        }
        if(rasMetadata_.size() > 0)
            return publishRASEventsForLocation(publishCount, regex);
        else
            throw new RuntimeException("No Matched RAS Data to generate events.");

    }

    public int publishRASEventsForLocation(int eventCount, String regex) {
        int count = 0;
        if(getLocation().matches(regex)) {
            for (int i = 0; i < eventCount; i++) {
                ForeignEvent ev = createRandomRASEvent(i + randomizerSeed);
                if (ev != null) {
                    PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                    eventQueue_.add(data);
                } else {
                    System.out.println("Unable to create Sensor event");
                    i--;
                }
            }
            remToPublishCount = remToPublishCount - eventCount;
            count += eventCount;
        }

        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            if(!(remToPublishCount > 0))
                break;
            count += entry.getValue().publishRASEventsForLocation(eventCount, regex);
        }
        return count;
    }

    public int publishRemSensorEvents(int eventCount, String regex, int publishCount, String regexLabel) throws RuntimeException, PropertyNotExpectedType {
        remToPublishCount = eventCount;
        if(checkCount == 0) {
            checkCount++;
            PropertyMap definitionSensorMetadata_new = new PropertyMap();
            PropertyArray denseRackEventList = new PropertyArray();
            PropertyArray denseChassisEventList = new PropertyArray();
            PropertyArray compute = new PropertyArray();
            PropertyArray blade = new PropertyArray();
            PropertyArray service = new PropertyArray();
            PropertyArray denseRackEventList11 = definitionSensorMetadata_.getArray("Rack");
            PropertyArray denseChassisEventList1 = definitionSensorMetadata_.getArray("Chassis");
            PropertyArray bladeold = definitionSensorMetadata_.getArray("Blade");
            PropertyArray computeold = definitionSensorMetadata_.getArray("ComputeNode");
            PropertyArray serviceold = definitionSensorMetadata_.getArray("ServiceNode");
            if(denseRackEventList11 != null) {
                for (int i = 0; i < denseRackEventList11.size(); i++) {
                    PropertyMap a = denseRackEventList11.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            denseRackEventList.add(a);
                    }
                }
            }
            if(denseChassisEventList1 != null) {
                for (int i = 0; i < denseChassisEventList1.size(); i++) {
                    PropertyMap a = denseChassisEventList1.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            denseChassisEventList.add(a);
                    }
                }
            }
            if(bladeold != null) {
                for (int i = 0; i < bladeold.size(); i++) {
                    PropertyMap a = bladeold.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            blade.add(a);
                    }
                }
            }
            if(computeold != null) {
                for (int i = 0; i < computeold.size(); i++) {
                    PropertyMap a = computeold.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            compute.add(a);
                    }
                }
            }
            if(serviceold != null) {
                for (int i = 0; i < serviceold.size(); i++) {
                    PropertyMap a = serviceold.getMap(i);
                    if(a != null) {
                        Object description = a.get("description");
                        if (description != null && description.toString().toLowerCase().matches(regexLabel))
                            service.add(a);
                    }
                }
            }
            definitionSensorMetadata_new.put("Rack", denseRackEventList);
            definitionSensorMetadata_new.put("Chassis", denseChassisEventList);
            definitionSensorMetadata_new.put("Blade", blade);
            definitionSensorMetadata_new.put("ComputeNode", compute);
            definitionSensorMetadata_new.put("ServiceNode", service);
            definitionSensorMetadata_=definitionSensorMetadata_new;
            denseRackEventList11 = definitionSensorMetadata_.getArray("Rack");
            denseChassisEventList1 = definitionSensorMetadata_.getArray("Chassis");
            bladeold = definitionSensorMetadata_.getArray("Blade");
            computeold = definitionSensorMetadata_.getArray("ComputeNode");
            serviceold = definitionSensorMetadata_.getArray("ServiceNode");
            boolean checkEmpty = (denseRackEventList11 != null && denseRackEventList11.size() == 0) && (denseChassisEventList1 != null && denseChassisEventList1.size() == 0) && (bladeold != null && bladeold.size() == 0) && (computeold != null && computeold.size() == 0) && (serviceold != null && serviceold.size() == 0);
            if(checkEmpty)
                throw new RuntimeException("No Matched Sensor Data to generate events.");

        }
        return publishSensorEventsForLocation(publishCount, regex);
    }

    public int publishSensorEventsForLocation(int eventCount, String regex) throws PropertyNotExpectedType {
        int count = 0;
        if(getLocation().matches(regex)) {
            for (int i = 0; i < eventCount; i++) {
                ForeignEvent ev = createRandomSensorEvent(i + randomizerSeed);
                if (ev != null) {
                    PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                    eventQueue_.add(data);
                } else {
                    System.out.println("Unable to create Sensor event");
                    i--;
                }
            }
            remToPublishCount = remToPublishCount - eventCount;
            count += eventCount;
        }

        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            if(!(remToPublishCount > 0))
                break;
            count += entry.getValue().publishSensorEventsForLocation(eventCount, regex);
        }
        return count;
    }

    public int publishUnAvailableEventsForLocation(String regex) {
        int count = 0;
        if( getLocation().matches(regex) && (type_.equals("ComputeNode") || type_.equals("ServiceNode")) ) {
            ForeignEvent ev = createUnavailableEvent();
            PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
            eventQueue_.add(data);
            count++;
        }
        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            count += entry.getValue().publishUnAvailableEventsForLocation(regex);
        }
        return count;
    }

    public int publishBootingEventsForLocationWithBF(String regex, ArrayList<String> regexMatchedLocationList, int probabilityValue) {
        intProbabilityValue = probabilityValue;
        return publishBootingEventsForLocation(regex, regexMatchedLocationList);
    }

    public int publishBootingEventsForLocation(String regex, ArrayList<String> regexMatchedLocationList) {
        int count = 0;
        if (failedRegexMatchedNodes == null)
            failedRegexMatchedNodes = new ArrayList<>();
        if( getLocation().matches(regex) && (type_.equals("ComputeNode") || type_.equals("ServiceNode")) ){
            if(intProbabilityValue == regexMatchedLocationList.size()) {
                ForeignEvent ev = createNodeFailureEvent();
                PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                eventQueue_.add(data);
            }
            else if(intProbabilityValue == 0) {
                ForeignEvent ev = createBootingEvent();
                PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                eventQueue_.add(data);
            }
            else {
                String randomSelectedNode = pickRandomRegexMatchedLocation(regexMatchedLocationList);
                if(randomSelectedNode.equals(getLocation())){
                    ForeignEvent ev = createNodeFailureEvent();
                    PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                    eventQueue_.add(data);
                    failedRegexMatchedNodes.add(getLocation());
                    count++;
                }
                else {
                    ForeignEvent ev = createBootingEvent();
                    PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                    eventQueue_.add(data);
                }
                regexMatchedLocationList.remove(getLocation());
            }
        }
        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            count += entry.getValue().publishBootingEventsForLocation(regex, regexMatchedLocationList);
        }
        return count;
    }

    public int publishAvailableEventsForLocation(String regex) {
        int count = 0;
        if( getLocation().matches(regex) && (type_.equals("ComputeNode") || type_.equals("ServiceNode")) ) {
            if(!failedRegexMatchedNodes.contains(getLocation())) {
                ForeignEvent ev = createAvailableEvent();
                PublishData data = new PublishData(ev.getEventCategory(), ev.getJSON());
                eventQueue_.add(data);
                count++;
            }
        }
        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            count += entry.getValue().publishAvailableEventsForLocation(regex);
        }
        return count;
    }

    private ForeignEvent createNodeFailureEvent() {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setTimestamp((startTimeLong_ += 500000));
        try {
            ev.setLocation(CommonFunctions.convertLocationToXName(getLocation()));
        } catch (DataTransformerException e) {
            logger_.error("error while converting location to xname");
        }
        ev.setEventType("ec_node_failed");
        ev.setEventCategory("stateChanges");
        return ev;
    }

    private ForeignEvent createUnavailableEvent() {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setTimestamp((startTimeLong_ += 500000));
        try {
            ev.setLocation(CommonFunctions.convertLocationToXName(getLocation()));
        } catch (DataTransformerException e) {
            logger_.error("error while converting location to xname");
        }
        ev.setEventType("ec_node_unavailable");
        ev.setEventCategory("stateChanges");
        return ev;
    }

    private ForeignEvent createBootingEvent() {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setTimestamp((startTimeLong_ += 500000));
        try {
            ev.setLocation(CommonFunctions.convertLocationToXName(getLocation()));
        } catch (DataTransformerException e) {
            logger_.error("error while converting location to xname");
        }
        ev.setEventType("ec_boot");
        ev.setEventCategory("stateChanges");
        return ev;
    }

    private ForeignEvent createAvailableEvent() {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setTimestamp((startTimeLong_ += 500000));
        try {
            ev.setLocation(CommonFunctions.convertLocationToXName(getLocation()));
        } catch (DataTransformerException e) {
            logger_.error("error while converting location to xname");
        }
        ev.setBootImageId(getRandomBootImageId());
        ev.setEventType("ec_node_available");
        ev.setEventCategory("stateChanges");
        return ev;
    }

    private String getRandomBootImageId() { // This is currently VERY specific. Needs generalization.
        String id = "mOS";
        try {
            id = bootImageInfo_.getMap(0).getStringOrDefault("id", ""); // Not really random but the first id.
        } catch(IndexOutOfBoundsException | PropertyNotExpectedType e) { /* Live with the first default choice! */ }
        return id;
    }

    public ForeignEvent createRandomRASEvent(int seed) {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        try {
            ev.setLocation(CommonFunctions.convertLocationToXName(getLocation()));
        } catch (DataTransformerException e) {
            logger_.error("error while converting location to xname");
        }
        ev.setEventCategory("events");
        ev.setEventType(rasMetadata_.get(generateRandomNumberBetween(0, rasMetadata_.size(), true)));
        return ev;
    }

    public ForeignEvent createRandomSensorEvent(int seed) throws PropertyNotExpectedType {
        // Ignore seed in the current implementation of randomization
        ForeignEventSensor ev = new ForeignEventSensor();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        try {
            ev.setLocation(CommonFunctions.convertLocationToXName(getLocation()));
        } catch (DataTransformerException e) {
            logger_.error("error while converting location to xname");
        }
        if (definitionSensorMetadata_ == null) {
            System.out.println("Definition sensor metadata is null");
            return null;
        }
        PropertyArray metadataDetails = definitionSensorMetadata_.getArray(type_);
        if (metadataDetails == null) {
            System.out.println("Metadata details is null: " + type_);
            return null;
        }
        PropertyMap sensorDetails = metadataDetails.getMap(generateRandomNumberBetween(0, metadataDetails.size(), true));
        if (sensorDetails == null)
            throw new RuntimeException("Unable to find sensor details for a component of type: " + type_);
        ev.setSensorName(sensorDetails.getStringOrDefault("id", "UNKNOWN"));
        ev.setSensorUnits(sensorDetails.getStringOrDefault("unit", "UNKNOWN"));
        ev.setSensorID(sensorDetails.getStringOrDefault("id", "UNKNOWN"));
        if(!sensorDetails.getStringOrDefault("unit", "UNKNOWN").equals(""))
            ev.setSensorValue(String.valueOf(generateRandomNumberBetween(20, 40, false)));
        else
            ev.setSensorValue(String.valueOf(0));
        return ev;
    }

    private  int generateRandomNumberBetween( int from, int to, boolean randomizerSeed) {
        if(randomizerSeed)
            return (int) ((myRandomGenerator_.nextDouble() * (to - from)) + from);
        else
            return (int) ((myRandomGeneratorSensor_.nextDouble() * (to - from)) + from);
    }

    public int getRegexMatchedComponentsCount(String regex, List<String> regexMatchedLocationList) {
        int count = 0;
        if(getLocation().matches(regex)) {
            count++;
            if(type_.equals("ComputeNode") || type_.equals("ServiceNode"))
                regexMatchedLocationList.add(getLocation());
        }
        for (Map.Entry<String, Component> entry : subcomponents_.entrySet()) {
            count += entry.getValue().getRegexMatchedComponentsCount(regex,regexMatchedLocationList);
        }
        return count;
    }

    private String pickRandomRegexMatchedLocation(ArrayList<String> regexMatchedLocationList) {
        Random rand = new Random(randomizerSeed);
        int randomIndex = (int)rand.nextDouble() * regexMatchedLocationList.size();
        String randomElement = regexMatchedLocationList.get(randomIndex);
        return randomElement;
    }

    private static long convertInstantToMicrosec(Instant timestamp) {
        long microsec = TimeUnit.SECONDS.toMicros(timestamp.getEpochSecond())+TimeUnit.NANOSECONDS.toMicros(timestamp.getNano());
        return microsec;
    }
}
