// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class SystemManifest {
    PropertyMap systemManifest_;
    PropertyMap definitionSensorMetadata_;
    List<String> rasMetadata_;
    String systemName_;
    Map<String, Component> floorComponentList_;
    ConcurrentLinkedQueue<PublishData> eventQueueCreate_;
    ArrayDeque<PublishData> eventQueue_;
    int eventRatio_ = 1;
    String randomizerSeed_;
    String rowCount_;
    Logger logger_;
    int RegexMatchedCount=0;
    ArrayList<String> regexMatchedLocationList;

    public SystemManifest(Logger logger) {
        logger_ = logger;
    }

    /**
     * @param systemManifest : JSON Object containing the SystemManifest
     * @param eventsOutput : Reference to the Events buffer queue for pushing generated events
     */
    public void generateSystem(PropertyMap systemManifest, int ratio, String randomizerSeed, String rowCount, PropertyMap definitionSensorMetadata, List<String> rasMetadata, ConcurrentLinkedQueue<PublishData> eventsOutput) throws PropertyNotExpectedType {
        eventQueueCreate_ = eventsOutput;
        eventQueue_ = new ArrayDeque<>();
        systemManifest_ = systemManifest;
        eventRatio_ = ratio;
        definitionSensorMetadata_ = definitionSensorMetadata;
        rasMetadata_ = rasMetadata;
        randomizerSeed_ = randomizerSeed;
        rowCount_ = rowCount;
        readSystemName();
        loadFloorComponents();
    }

    private void loadFloorComponents() throws PropertyNotExpectedType {
        floorComponentList_ = new HashMap<>();

        PropertyMap views = systemManifest_.getMap("views");
        if (views == null)
            throw new RuntimeException("Missing 'views' in SystemManifest");

        PropertyMap full = views.getMap("Full");
        if (full == null)
            throw new RuntimeException("Missing 'views-full' in SystemManifest");

        PropertyMap floor = full.getMap("floor");
        if (floor == null)
            throw new RuntimeException("Missing 'views-full-floor' in SystemManifest");

        PropertyArray floorComponents = floor.getArray("content");

        long curTimeEpoch = convertInstantToMicrosec(Instant.now());

        if (floorComponents != null) {
            for (int i = 0; i < floorComponents.size(); i++) {
                PropertyMap floorComponent = floorComponents.getMap(i);
                if (floorComponent != null) {
                    String name = floorComponent.getStringOrDefault("name", "UNKNOWN");
                    String definition = floorComponent.getStringOrDefault("definition", "UNKNOWN");
                    Map<String, String> details = new HashMap<String, String>();
                    details.put("name", name);
                    details.put("definition", definition);
                    details.put("parentLocation", "");
                    details.put("parentForeignLocation", "");
                    details.put("randomizerSeed", randomizerSeed_);
                    details.put("rowCount", rowCount_);
                    details.put("startTime", String.valueOf(curTimeEpoch));
                    try {
                        Component cmp = new Component(details, systemManifest_, eventQueue_, eventRatio_,
                                definitionSensorMetadata_, rasMetadata_, logger_);
                        floorComponentList_.put(name, cmp);
                    } catch (Exception e) {
                        logger_.debug("Error encountered while adding adding component: " + name + "-" + definition);
                    }
                }
            }
        }
    }

    private static long convertInstantToMicrosec(Instant timestamp) {
        long microsec = TimeUnit.SECONDS.toMicros(timestamp.getEpochSecond())+TimeUnit.NANOSECONDS.toMicros(timestamp.getNano());
        return microsec;
    }

    private void readSystemName() throws PropertyNotExpectedType {
        systemName_ = new String(systemManifest_.getString("sysname"));
    }

    public String getSystemName() {
        return systemName_;
    }

    public int getComponentCount() {
        return floorComponentList_.size();
    }

    public String getHierarchy() {
        StringBuilder str = new StringBuilder();
        for(Map.Entry<String, Component> entry : floorComponentList_.entrySet()) {
            str.append(entry.getValue().getHierarchy());
        }
        return str.toString();
    }

    public void publishEventsFromLocation(int count, String regex, String regexLabel) throws Exception{
        int eventsPerComponent = count / RegexMatchedCount;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            if(count == 0)
                break;
            count = count - item.getValue().publishRemEvents(count, regex, eventsPerComponent, regexLabel);
        }

        int remEvents = count%RegexMatchedCount;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            if(remEvents == 0)
                break;
            remEvents = remEvents - item.getValue().publishRemEvents(remEvents, regex, 1, regexLabel);
        }
        startSendCreatedEvents();
    }

    public void publishBootSequenceFromLocation(String regex, float probabiltyValue) {
        probabiltyValue = ( probabiltyValue / 100 ) * regexMatchedLocationList.size();
        int intProbabilityValue = Math.round(probabiltyValue);
        int count = 0;

        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            count += item.getValue().publishUnAvailableEventsForLocation(regex);
        }
        logger_.info("Unavailable = "+ count);
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            intProbabilityValue = intProbabilityValue - item.getValue().publishBootingEventsForLocationWithBF(regex, regexMatchedLocationList, intProbabilityValue);
        }
        count =0;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            count += item.getValue().publishAvailableEventsForLocation(regex);
        }
        logger_.info("Available = "+ count);
        startSendCreatedEvents();
    }

    public void publishRASEventsFromLocation(int count, String regex, String regexLabel) {
        int eventsPerComponent = count / RegexMatchedCount;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            if(count == 0 )
                break;
            count = count - item.getValue().publishRemRASEvents(count, regex, eventsPerComponent, regexLabel);
        }

        int remEvents = count%RegexMatchedCount;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            if (remEvents == 0)
                break;
            remEvents = remEvents - item.getValue().publishRemRASEvents(remEvents, regex,1, regexLabel);
        }
        startSendCreatedEvents();
    }

    public void publishSensorEventsFromLocation(int count, String regex, String regexLabel) throws Exception {
        int eventsPerComponent = count/RegexMatchedCount;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            if(count == 0)
                break;
            count = count - item.getValue().publishRemSensorEvents(count, regex, eventsPerComponent, regexLabel);
        }

        int remEvents = count%RegexMatchedCount;
        for (Map.Entry<String, Component> item : floorComponentList_.entrySet()) {
            if(remEvents == 0)
                break;
            remEvents = remEvents - item.getValue().publishRemSensorEvents(remEvents, regex, 1, regexLabel);
        }
        startSendCreatedEvents();
    }

    private void startSendCreatedEvents() {
        eventQueueCreate_.addAll(eventQueue_);
        eventQueue_.clear();
    }

    public int getRegexMatchedComponentsCount(String regex, String eventType) {
        regexMatchedLocationList = null;
        RegexMatchedCount = 0;
        for (Map.Entry<String, Component> entry : floorComponentList_.entrySet()) {
            if(regexMatchedLocationList == null)
                regexMatchedLocationList = new ArrayList<>();
            RegexMatchedCount = RegexMatchedCount + entry.getValue().getRegexMatchedComponentsCount(regex, regexMatchedLocationList);
        }
        if(eventType.equals("b"))
            return regexMatchedLocationList.size();
        return RegexMatchedCount;
    }

    // Unblock when we need to set the exact number to failure boot sequence.
    /* private void createRandomLocationsList(ArrayList<String> regexMatchedLocationList, int probability, ArrayList<String> regexMatchedRandomSelectedList) {
        assert regexMatchedRandomSelectedList != null : "Initialized by caller function in System Manifest File";
        Random rand = new Random();
        for(int i=0; i< probability; i++) {
            int randomIndex = rand.nextInt(regexMatchedLocationList.size());
            String randomElement = regexMatchedLocationList.get(randomIndex);
            regexMatchedRandomSelectedList.add(randomElement);
            regexMatchedLocationList.remove(randomIndex);
        }
    }*/
}
