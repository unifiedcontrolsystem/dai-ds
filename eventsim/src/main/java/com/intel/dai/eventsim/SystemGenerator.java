package com.intel.dai.eventsim;

import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.util.*;

/**
 * Description of class SystemGenerator.
 * loads locations data from db.
 * creates respective events like ras sensor and boot.
 */
class SystemGenerator {

    SystemGenerator(Logger log) {
        log_ = log;
    }

    /**
     * This method used to load locations data.
     * @param dataLoaderEngine_ database object to fetch data from db and load locations data into map.
     * @throws SimulatorException when unable to fetch boot parameters configuration file.
     */
    void generateSystem(final DataLoaderEngine dataLoaderEngine_) throws SimulatorException {
        dataLoaderEngine = dataLoaderEngine_;
        component_ = new Component(log_, dataLoaderEngine_.getBootParamsFileLocation());
        loadComponents();
    }

    /**
     * This method used to create boot off,on,ready events.
     * @param bfValue probability of number of failure events can be generated.
     * @return generated boot off,on,ready events
     */
    List<ForeignEvent> publishBootEventsForLocation(final float bfValue, long numOfEvents) throws ConversionException {
        float totalFailureEvents = (bfValue) * regexMatchedLocations.size();
        long totalFailureEventsToGenerate = Math.round(totalFailureEvents);

        List<ForeignEvent> unavailableEvents = component_.publishUnAvailableEventsForLocation(regexMatchedLocations, numOfEvents);
        log_.info("Unavailable = "+ unavailableEvents.size());
        List<ForeignEvent> bootEvents = new ArrayList<>(unavailableEvents);

        List<ForeignEvent> bootingEvents = component_.publishBootingEventsForLocation(regexMatchedLocations, totalFailureEventsToGenerate, numOfEvents);
        log_.info("Booting = "+ bootingEvents.size());
        bootEvents.addAll(bootingEvents);

        List<ForeignEvent> availableEvents = component_.publishAvailableEventsForLocation(regexMatchedLocations, numOfEvents);
        log_.info("Available = "+ availableEvents.size());
        bootEvents.addAll(availableEvents);

        return bootEvents;
    }

    /**
     * This method used to create boot off events.
     * @return generated boot off events
     */
    List<ForeignEvent> publishBootOffEventsForLocation(long numOfEvents) throws ConversionException {
        return new ArrayList<>(component_.publishUnAvailableEventsForLocation(regexMatchedLocations, numOfEvents));
    }

    /**
     * This method used to create boot on events.
     * @param bfValue probability of number of failure events can be generated.
     * @return generated boot on events
     */
    List<ForeignEvent> publishBootOnEventsForLocation(final float bfValue, long numOfEvents) throws ConversionException {
        float totalFailureEvents = (bfValue) * regexMatchedLocations.size();
        long totalFailureEventsToGenerate = Math.round(totalFailureEvents);

        return new ArrayList<>(component_.publishBootingEventsForLocation(regexMatchedLocations, totalFailureEventsToGenerate, numOfEvents));
    }

    /**
     * This method used to create boot ready events.
     * @return generated boot ready events
     */
    List<ForeignEvent> publishBootReadyEventsForLocation(long numOfEvents) throws ConversionException {
        return new ArrayList<>(component_.publishAvailableEventsForLocation(regexMatchedLocations, numOfEvents));
    }

    /**
     * This method used to create ras events.
     * @param numOfEvents numbers of ras events to be generated.
     * @param seed to repeat same type of data.
     * @return generated sensor events
     * @throws SimulatorException when unable to create exact number of events required.
     * @throws ConversionException when unable to create ras event.
     */
    List<ForeignEvent> publishRASEventsForLocation(long numOfEvents, final long seed)
            throws SimulatorException, ConversionException {
        long eventsPerLocation = numOfEvents / regexMatchedLocations.size();
        List<ForeignEvent> rasEvents = new ArrayList<>();
        if(eventsPerLocation != 0) {
            rasEvents = component_.publishRASEvents(eventsPerLocation, seed, regexMatchedLocations,
                    rasRegexMatchedLabelDescriptions_);
        }

        long remRasEvents = numOfEvents % regexMatchedLocations.size();
        if (remRasEvents == 0)
            return rasEvents;
        rasEvents.addAll(component_.publishRemainingRASEvents(remRasEvents, seed, regexMatchedLocations, rasRegexMatchedLabelDescriptions_));
        remRasEvents = numOfEvents - rasEvents.size();
        if(remRasEvents != 0)
            throw new SimulatorException("Incorrect number of ras events generated");
        return rasEvents;
    }

    /**
     * This method used to create sensor events.
     * @param numOfEvents numbers of sensor events to be generated.
     * @param seed to repeat same type of data.
     * @return generated sensor events
     * @throws SimulatorException when unable to create exact number of events required.
     * @throws ConversionException when unable to create sensor event.
     */
    List<ForeignEvent> publishSensorEventsForLocation(long numOfEvents, final long seed) throws SimulatorException, ConversionException {
        long eventsPerLocation = numOfEvents / regexMatchedLocations.size();
        List<ForeignEvent> sensorEvents = new ArrayList<>();
        if(eventsPerLocation != 0) {
            sensorEvents = component_.publishSensorEvents(eventsPerLocation, seed, regexMatchedLocations, sensorRegexMatchedLabelDescriptions_);
        }

        long remEvents = numOfEvents % regexMatchedLocations.size();
        if (remEvents == 0)
            return sensorEvents;
        sensorEvents.addAll(component_.publishRemainingSensorEvents(remEvents, seed, regexMatchedLocations, sensorRegexMatchedLabelDescriptions_));
        remEvents = numOfEvents - sensorEvents.size();
        if(remEvents != 0)
            throw new SimulatorException("Incorrect number of sensor events generated");
        return sensorEvents;
    }

    /**
     * This method used to create job events.
     * @param numOfEvents numbers of job events to be generated.
     * @param seed to repeat same type of data.
     * @return generated job events
     * @throws SimulatorException when unable to create exact number of events required.
     * @throws ConversionException when unable to create job event.
     */
    List<ForeignEvent> publishJobEventsForLocation(long numOfEvents, final long seed) throws SimulatorException, ConversionException {
        long eventsPerLocation = numOfEvents / regexMatchedLocations.size();
        List<ForeignEvent> jobEvents = new ArrayList<>();
        if(eventsPerLocation != 0) {
            jobEvents = component_.publishJobEvents(eventsPerLocation, seed, regexMatchedLocations, jobsRegexMatchedLabelDescriptions_);
        }

        long remEvents = numOfEvents % regexMatchedLocations.size();
        if (remEvents == 0)
            return jobEvents;
        jobEvents.addAll(component_.publishRemJobEvents(remEvents, seed, regexMatchedLocations, jobsRegexMatchedLabelDescriptions_));
        remEvents = numOfEvents - jobEvents.size();
        if(remEvents != 0)
            throw new SimulatorException("Incorrect number of job events generated");
        return jobEvents;
    }

    /**
     * This method filters regex matched locations from all the locations available.
     * @param locationRegex regex for locations.
     * @return number of matching regex locations.
     */
    int getMatchedRegexLocations(final String locationRegex) {
        List<String> regexMatchedLocationList = new ArrayList<>();
        for (String location : locations) {
            if(location.matches(locationRegex))
                regexMatchedLocationList.add(location);
        }
        regexMatchedLocations = regexMatchedLocationList;
        return regexMatchedLocations.size();
    }

    /**
     * This method filters regex matched labels from respective events meta data.
     * @param regexLabelDesc regex for label descriptions.
     * @param eventType event types like ras sensor or boot.
     * @return number of matching regex label descriptions
     * @throws PropertyNotExpectedType when unable to fetch respective label description data.
     */
    int getMatchedRegexLabels(final String regexLabelDesc, final SimulatorEngine.EVENT_TYPE eventType) throws PropertyNotExpectedType {
        int value = 0;

        switch (eventType) {
            case RAS     :  rasRegexMatchedLabelDescriptions_.clear();
                            for (String labelDesc : dataLoaderEngine.getRasMetaData()) {
                                if(labelDesc.matches(regexLabelDesc)) {
                                    PropertyArray resultRas = new PropertyArray();
                                    resultRas.add(labelDesc);
                                    rasRegexMatchedLabelDescriptions_.add(resultRas);
                                }
                            }
                            value = rasRegexMatchedLabelDescriptions_.size();
                            break;

            case SENSOR  :  sensorRegexMatchedLabelDescriptions_.clear();
                            PropertyMap data = dataLoaderEngine.getSensorMetaData().getAsMap();
                            for(String itemKey : data.keySet()) {
                                PropertyArray compKey = data.getArrayOrDefault(itemKey, new PropertyArray());
                                for(int i = 0; i < compKey.size(); i++) {
                                    PropertyMap itemData = compKey.getMap(i);
                                    String itemDescription = itemData.getStringOrDefault("description", "");
                                    if(itemDescription.matches(regexLabelDesc))
                                        sensorRegexMatchedLabelDescriptions_.add(itemData);
                                }
                            }
                            value = sensorRegexMatchedLabelDescriptions_.size();
                            break;

            case JOB  :     jobsRegexMatchedLabelDescriptions_.clear();
                            PropertyMap jobData = dataLoaderEngine.getJobsMetaData().getAsMap();
                            for(String jobid : jobData.keySet()) {
                                PropertyMap jobDesc = jobData.getMapOrDefault(jobid, new PropertyMap());
                                String jobDescription = jobDesc.getStringOrDefault("type", "");
                                if(jobDescription.matches(regexLabelDesc)) {
                                    jobDesc.put("id", jobid);
                                    jobsRegexMatchedLabelDescriptions_.add(jobDesc);
                                }
                            }
                            value = jobsRegexMatchedLabelDescriptions_.size();
                            break;

            default      :  break;
        }
        return value;
    }

    /**
     * This method is used to generate and group events for burst scenario
     * @param scenarioData burst scenario data.
     * @return burst scenario events with scenario data groups.
     * @throws PropertyNotExpectedType unable to find rate or seed parameter.
     * @throws ConversionException unable to generate or group scenario events.
     * @throws SimulatorException unable to generate or group scenario events.
     */
    Map<Long, List<ForeignEvent>> generateBurstScenario(PropertyMap scenarioData) throws PropertyNotExpectedType, ConversionException, SimulatorException {
        long eventsRate = Long.parseLong(scenarioData.getString("rate"));
        long randomizerSeed = Long.parseLong(scenarioData.getString("seed"));
        List<ForeignEvent> events = generateBurstScenarioEvents(scenarioData);
        return groupEventsForGivenRate(events, eventsRate,randomizerSeed);
    }

    /**
     * This method is used to generate events for burst scenario.
     * @param scenarioData burst scenario data.
     */
    private List<ForeignEvent> generateBurstScenarioEvents(PropertyMap scenarioData) throws PropertyNotExpectedType, SimulatorException, ConversionException {
        long ras = Long.parseLong(scenarioData.getString("ras"));
        long sensor = Long.parseLong(scenarioData.getString("sensor"));
        long bootOn = Long.parseLong(scenarioData.getString("boot-on"));
        long bootOff = Long.parseLong(scenarioData.getString("boot-off"));
        long bootReady = Long.parseLong(scenarioData.getString("boot-ready"));
        long randomizerSeed = Long.parseLong(scenarioData.getString("seed"));
        float bfProbability = Float.parseFloat(scenarioData.getString("boot-prob-failure"));

        List<ForeignEvent> burstEvents = new ArrayList<>(publishRASEventsForLocation(ras, randomizerSeed));
        burstEvents.addAll(publishSensorEventsForLocation(sensor, randomizerSeed));
        burstEvents.addAll(publishBootOffEventsForLocation(bootOff));
        burstEvents.addAll(publishBootOnEventsForLocation(bfProbability, bootOn));
        burstEvents.addAll(publishBootReadyEventsForLocation(bootReady));
        return burstEvents;
    }

    /**
     * This method is used to form group of events based on events rate.
     * @param events All combined set of ras, sensor,boot events for a given scenario configuration.
     * @param eventsRate rate mentioned in scenario configuration file.
     * @param seed to select same data for a given seed.
     * @return Map with keys as number of groups and values number of events for a given rate.
     */
    private Map<Long, List<ForeignEvent>> groupEventsForGivenRate(List<ForeignEvent> events, long eventsRate, long seed) {
        Map<Long, List<ForeignEvent>> eventsMap = new HashMap<>();
        Collections.shuffle(events,new Random(seed));
        List<ForeignEvent> groupEvents = new ArrayList<>();
        for(ForeignEvent event : events) {
            if(groupEvents.size() == eventsRate) {
                eventsMap.put((long)(eventsMap.size() + 1), new ArrayList<>(groupEvents));
                groupEvents.clear();
            }
            groupEvents.add(event);
        }
        if(groupEvents.size() != 0)
            eventsMap.put((long)(eventsMap.size() + 1), new ArrayList<>(groupEvents));
        return eventsMap;
    }


    /**
     * This method is used to generate events for group burst scenario.
     * @param scenarioData group burst scenario data.
     * @return scenario based group events.
     */
    Map<Long, List<ForeignEvent>> generateGroupBurstScenarioEvents(PropertyMap scenarioData) throws PropertyNotExpectedType, ConversionException, SimulatorException {
        long totalRas = Long.parseLong(scenarioData.getString("totalRas"));
        long totalSensor = Long.parseLong(scenarioData.getString("totalSensor"));
        long totalBootOn = Long.parseLong(scenarioData.getString("totalBootOn"));
        long totalBootOff = Long.parseLong(scenarioData.getString("totalBootOff"));
        long totalBootReady = Long.parseLong(scenarioData.getString("totalBootReady"));
        long ras = Long.parseLong(scenarioData.getString("ras"));
        long sensor = Long.parseLong(scenarioData.getString("sensor"));
        long bootOn = Long.parseLong(scenarioData.getString("boot-on"));
        long bootOff = Long.parseLong(scenarioData.getString("boot-off"));
        long bootReady = Long.parseLong(scenarioData.getString("boot-ready"));
        long randomizerSeed = Long.parseLong(scenarioData.getString("seed"));
        float bfProbability = Float.parseFloat(scenarioData.getString("boot-prob-failure"));

        List<ForeignEvent> rasEvents = new ArrayList<>(publishRASEventsForLocation(totalRas, randomizerSeed));
        Map<Long, List<ForeignEvent>> rasMap = groupEventsForGivenRate(rasEvents, ras, randomizerSeed);
        List<ForeignEvent> sensorEvents = new ArrayList<>(publishSensorEventsForLocation(totalSensor, randomizerSeed));
        Map<Long, List<ForeignEvent>> sensorMap = groupEventsForGivenRate(sensorEvents, sensor, randomizerSeed);
        List<ForeignEvent> bootOffEvents = new ArrayList<>(publishBootOffEventsForLocation(totalBootOff));
        Map<Long, List<ForeignEvent>> bootOffMap = groupEventsForGivenRate(bootOffEvents, bootOff, randomizerSeed);
        List<ForeignEvent> bootOnEvents = new ArrayList<>(publishBootOnEventsForLocation(bfProbability, totalBootOn));
        Map<Long, List<ForeignEvent>> bootOnMap = groupEventsForGivenRate(bootOnEvents, bootOn, randomizerSeed);
        List<ForeignEvent> bootReadyEvents = new ArrayList<>(publishBootReadyEventsForLocation(totalBootReady));
        Map<Long, List<ForeignEvent>> bootReadyMap = groupEventsForGivenRate(bootReadyEvents, bootReady, randomizerSeed);

        mergeEventsWithGroups(rasMap, sensorMap);
        mergeEventsWithGroups(rasMap, bootOnMap);
        mergeEventsWithGroups(rasMap, bootOffMap);
        mergeEventsWithGroups(rasMap, bootReadyMap);
        return rasMap;
    }

    private void mergeEventsWithGroups(Map<Long, List<ForeignEvent>> map1, Map<Long, List<ForeignEvent>> map2) {
        Set<Long> keys = new HashSet<>(map1.keySet());
        keys.addAll(map2.keySet());
        for(long group : keys) {
            if(map1.containsKey(group) && map2.containsKey(group)) {
                List<ForeignEvent> data = map1.get(group);
                data.addAll(map2.get(group));
                map1.put(group, data);
            }

            else if(map2.containsKey(group))
                map1.put(group, map2.get(group));

        }
    }


    /**
     * This method is used to generate events for repeat scenario.
     * @param mode scenario mode.
     * @param modeData repeat scenario data.
     */
    Map<Long, List<ForeignEvent>> generateRepeatScenarioEvents(String mode, PropertyMap modeData) throws PropertyNotExpectedType, SimulatorException, ConversionException {
        if(modeData == null)
            throw new SimulatorException("repeat scenario mode configuration data is empty/null");
        if(mode.equals("burst"))
            return generateBurstScenario(modeData);
        else if(mode.equals("group-burst"))
            return generateGroupBurstScenarioEvents(modeData);
        return new HashMap<>();
    }

    /**
     * This method loads node and non-node locations data into map.
     */
    private void loadComponents() throws SimulatorException {
        loadNodeLocations();
        loadNonNodeLocations();
    }

    /**
     * This method loads node locations data into map.
     */
    private void loadNodeLocations() throws SimulatorException {
        nodeLocations_ = dataLoaderEngine.getNodeLocationData();
        if(nodeLocations_ == null)
            throw new SimulatorException("No node locations data");
        locations.addAll(nodeLocations_);
    }

    /**
     * This method loads non-node locations data into map.
     */
    private void loadNonNodeLocations() {
        nonNodeLocations_ = dataLoaderEngine.getNonNodeLocationData();
        if(nonNodeLocations_ != null)
            locations.addAll(nonNodeLocations_);
    }

    private final Logger log_;
    private DataLoaderEngine dataLoaderEngine;
    private List<String> locations = new ArrayList<>();
    private List<String> regexMatchedLocations = new ArrayList<>();
    private Component component_;
    private List<String> nodeLocations_;
    private List<String> nonNodeLocations_;
    private List<PropertyDocument> rasRegexMatchedLabelDescriptions_ = new ArrayList<>();
    private List<PropertyDocument> sensorRegexMatchedLabelDescriptions_ = new ArrayList<>();
    private List<PropertyDocument> jobsRegexMatchedLabelDescriptions_ = new ArrayList<>();
}