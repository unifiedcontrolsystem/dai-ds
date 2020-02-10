package com.intel.dai.eventsim;

import com.intel.dai.network_listener.NetworkListenerProviderException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.util.ArrayList;
import java.util.List;

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
     * This method used to create boot events.
     * @param bfValue probability of number of failure vents can be generated.
     * @return generated boot events
     */
    List<String> publishBootEventsForLocation(final float bfValue) throws NetworkListenerProviderException {
        float totalFailureEvents = ( bfValue / 100 ) * regexMatchedLocations.size();
        long totalFailureEventsToGenerate = Math.round(totalFailureEvents);
        List<String> bootEvents = new ArrayList<>();

        List<String> unavailableEvents = component_.publishUnAvailableEventsForLocation(regexMatchedLocations);
        log_.info("Unavailable = "+ unavailableEvents.size());
        bootEvents.addAll(unavailableEvents);

        List<String> bootingEvents = component_.publishBootingEventsForLocation(regexMatchedLocations, totalFailureEventsToGenerate);
        log_.info("Booting = "+ bootingEvents.size());
        bootEvents.addAll(bootingEvents);

        List<String> availableEvents = component_.publishAvailableEventsForLocation(regexMatchedLocations);
        log_.info("Available = "+ availableEvents.size());
        bootEvents.addAll(availableEvents);

        return bootEvents;
    }

    /**
     * This method used to create ras events.
     * @param eventsCount numbers of ras events to be generated.
     * @param seed to repeat same type of data.
     * @return generated sensor events
     * @throws SimulatorException when unable to create exact number of events required.
     * @throws NetworkListenerProviderException when unable to create ras event.
     */
    List<String> publishRASEventsForLocation(long eventsCount, final long seed) throws SimulatorException, NetworkListenerProviderException {
        long eventsPerLocation = eventsCount / regexMatchedLocations.size();
        List<String> rasEvents = new ArrayList<>();
        if(eventsPerLocation != 0) {
            rasEvents = component_.publishRASEvents(eventsPerLocation, seed, regexMatchedLocations, regexMatchedLabelDescriptions);
            eventsCount = eventsCount - rasEvents.size();
        }

        long remRasEvents = eventsCount % regexMatchedLocations.size();
        if (remRasEvents == 0)
            return rasEvents;
        rasEvents.addAll(component_.publishRemRASEvents(remRasEvents, seed, regexMatchedLocations, regexMatchedLabelDescriptions));
        remRasEvents = eventsCount - rasEvents.size();
        if(remRasEvents != 0)
            throw new SimulatorException("Incorrect number of ras events generated");
        return rasEvents;
    }

    /**
     * This method used to create sensor events.
     * @param eventsCount numbers of sensor events to be generated.
     * @param seed to repeat same type of data.
     * @return generated sensor events
     * @throws SimulatorException when unable to create exact number of events required.
     * @throws NetworkListenerProviderException when unable to create sensor event.
     */
    List<String> publishSensorEventsForLocation(long eventsCount, final long seed) throws SimulatorException, NetworkListenerProviderException {
        long eventsPerLocation = eventsCount / regexMatchedLocations.size();
        List<String> sensorEvents = new ArrayList<>();
        if(eventsPerLocation != 0) {
            sensorEvents = component_.publishSensorEvents(eventsPerLocation, seed, regexMatchedLocations, regexMatchedLabelDescriptions);
            eventsCount = eventsCount - sensorEvents.size();
        }

        long remEvents = eventsCount % regexMatchedLocations.size();
        if (remEvents == 0)
            return sensorEvents;
        sensorEvents.addAll(component_.publishRemSensorEvents(remEvents, seed, regexMatchedLocations, regexMatchedLabelDescriptions));
        remEvents = eventsCount - sensorEvents.size();
        if(remEvents != 0)
            throw new SimulatorException("Incorrect number of sensor events generated");
        return sensorEvents;
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
        List<PropertyDocument> regexMatchedLabelDescList = new ArrayList<>();
        switch (eventType) {
            case RAS     :
                            for (String labelDesc : dataLoaderEngine.getRasMetaData()) {
                                if(labelDesc.matches(regexLabelDesc)) {
                                    PropertyArray resultRas = new PropertyArray();
                                    resultRas.add(labelDesc);
                                    regexMatchedLabelDescList.add(resultRas);
                                }
                            }
                            regexMatchedLabelDescriptions = regexMatchedLabelDescList;
                            value = regexMatchedLabelDescriptions.size();
                            break;

            case SENSOR  :  PropertyMap data = dataLoaderEngine.getSensorMetaData().getAsMap();
                            for(String itemKey : data.keySet()) {
                                PropertyArray compKey = data.getArrayOrDefault(itemKey, null);
                                for(int i = 0; i < compKey.size(); i++) {
                                    PropertyMap itemData = compKey.getMap(i);
                                    String itemDescription = itemData.getStringOrDefault("description", null);
                                    if(itemDescription.matches(regexLabelDesc))
                                        regexMatchedLabelDescList.add(itemData);
                                }
                            }
                            regexMatchedLabelDescriptions = regexMatchedLabelDescList;
                            value = regexMatchedLabelDescriptions.size();
                            break;

            default      :  break;
        }
        return value;
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
    private List<PropertyDocument> regexMatchedLabelDescriptions = new ArrayList<>();
    private Component component_;
    private List<String> nodeLocations_;
    private List<String> nonNodeLocations_;
}