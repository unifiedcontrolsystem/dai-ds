package com.intel.dai.eventsim;

import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.network_listener.NetworkListenerProviderException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Description of class Component.
 * creates requested events data format like ras, sensor and boot.
 */
public class Component {

    Component(Logger log, String bootImageFileLocation) {
        log_ = log;
       /* try {
            bootImageInfo_ = LoadFileLocation.fromFileLocation(bootImageFileLocation).getAsMap().
                    getMapOrDefault("boot-images", new PropertyMap()).getArrayOrDefault("content", new PropertyArray());
        } catch(IOException | ConfigIOParseException e) {
            bootImageInfo_ = new PropertyArray();
        }*/
    }

    /**
     * This method is to create ras events.
     * @param eventsPerLocation number of events to create at a particular location.
     * @param seed to repeat same type of data.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param regexMatchedLabelDescriptions descriptions matching the description-regex.
     * @return ras events.
     * @throws NetworkListenerProviderException unable to create ras event.
     */
    List<String> publishRASEvents(final long eventsPerLocation, final long seed, @NotNull final List<String> regexMatchedLocations, @ NotNull final List<PropertyDocument> regexMatchedLabelDescriptions) throws NetworkListenerProviderException {
        List<String> rasEvents = new ArrayList<>();
        for (String location : regexMatchedLocations) {
            for (int i = 0; i < eventsPerLocation; i++) {
                ForeignEvent ev = createRandomRASEvent(i + seed, location, regexMatchedLabelDescriptions);
                String data = ev.getJSON();
                rasEvents.add(data);
            }
        }
        return rasEvents;
    }

    /**
     * This method is to create ras events.
     * @param remEvents number of events to create at a particular location.
     * @param seed to repeat same type of data.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param regexMatchedLabelDescriptions descriptions matching the description-regex.
     * @return ras events.
     * @throws NetworkListenerProviderException unable to create ras event.
     */
    List<String> publishRemRASEvents(final long remEvents, final long seed, @NotNull final List<String> regexMatchedLocations, @ NotNull final List<PropertyDocument> regexMatchedLabelDescriptions) throws NetworkListenerProviderException {
        long eventsTobeGenerated = remEvents;
        List<String> rasEvents = new ArrayList<>();
        for (String location : regexMatchedLocations) {
            if(eventsTobeGenerated == 0)
                return rasEvents;
            ForeignEvent ev = createRandomRASEvent(eventsTobeGenerated + seed, location, regexMatchedLabelDescriptions);
            String data = ev.getJSON();
            rasEvents.add(data);
            eventsTobeGenerated--;
        }
        return rasEvents;
    }

    /**
     * This method is to create sensor events.
     * @param eventsPerLocation number of events to create at a particular location.
     * @param seed to repeat same type of data.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param regexMatchedLabelDescriptions descriptions matching the description-regex.
     * @return sensor events.
     * @throws NetworkListenerProviderException unable to create sensor event.
     */
    List<String> publishSensorEvents(final long eventsPerLocation, final long seed, @NotNull final List<String> regexMatchedLocations, @NotNull final List<PropertyDocument> regexMatchedLabelDescriptions) throws NetworkListenerProviderException, SimulatorException {
        List<String> rasEvents = new ArrayList<>();
        for (String location : regexMatchedLocations) {
            for (int i = 0; i < eventsPerLocation; i++) {
                ForeignEvent ev = createRandomSensorEvent(i + seed, location, regexMatchedLabelDescriptions);
                String data = ev.getJSON();
                rasEvents.add(data);
            }
        }
        return rasEvents;
    }

    /**
     * This method is to create sensor events.
     * @param remEvents number of events to create at a particular location.
     * @param seed to repeat same type of data.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param regexMatchedLabelDescriptions descriptions matching the description-regex.
     * @return sensor events.
     * @throws NetworkListenerProviderException unable to create sensor event.
     */
    List<String> publishRemSensorEvents(final long remEvents, final long seed, @NotNull final List<String> regexMatchedLocations, @NotNull final List<PropertyDocument> regexMatchedLabelDescriptions) throws NetworkListenerProviderException, SimulatorException {
        long eventsTobeGenerated = remEvents;
        List<String> sensorEvents = new ArrayList<>();
        for (String location : regexMatchedLocations) {
            if(eventsTobeGenerated == 0)
                return sensorEvents;
            ForeignEvent ev = createRandomSensorEvent(eventsTobeGenerated + seed, location, regexMatchedLabelDescriptions);
            String data = ev.getJSON();
            sensorEvents.add(data);
            eventsTobeGenerated--;
        }
        return sensorEvents;
    }

    /**
     * This method is to create available boot events.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @return available boot events.
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    List<String> publishAvailableEventsForLocation(@NotNull final List<String> regexMatchedLocations) throws NetworkListenerProviderException {
        List<String> bootAvailableEvents = new ArrayList<>();
        for(String location : regexMatchedLocations) {
            ForeignEvent ev = createAvailableEvent(location);
            String data = ev.getJSON();
            bootAvailableEvents.add(data);
        }
        return bootAvailableEvents;
    }

    /**
     * This method is to create available boot events.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param totalFailureEventsToGenerate number of failure boot events to create.
     * @return boot events for a given location
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    List<String> publishBootingEventsForLocation(final @NotNull List<String> regexMatchedLocations, final long totalFailureEventsToGenerate) throws NetworkListenerProviderException {
        List<String> bootingEvents = new ArrayList<>();
        for(String location : regexMatchedLocations) {
            if(totalFailureEventsToGenerate == regexMatchedLocations.size()) {
                ForeignEvent ev = createNodeFailureEvent(location);
                String data = ev.getJSON();
                bootingEvents.add(data);
            } else if(totalFailureEventsToGenerate == 0) {
                ForeignEvent ev = createBootingEvent(location);
                String data = ev.getJSON();
                bootingEvents.add(data);
            } else {
                int randomNumber = generateRandomNumberBetween(0, regexMatchedLocations.size(), null);
                if((randomNumber % 2) == 0) {
                    ForeignEvent ev = createNodeFailureEvent(location);
                    String data = ev.getJSON();
                    bootingEvents.add(data);
                } else {
                    ForeignEvent ev = createBootingEvent(location);
                    String data = ev.getJSON();
                    bootingEvents.add(data);
                }
            }
        }
        return bootingEvents;
    }

    /**
     * This method is to create unavailable boot events.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @return unavailable boot events.
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    List<String> publishUnAvailableEventsForLocation(@NotNull final List<String> regexMatchedLocations) throws NetworkListenerProviderException {
        List<String> bootUnAvailableEvents = new ArrayList<>();
        for(String location : regexMatchedLocations) {
            ForeignEvent ev = createUnavailableEvent(location);
            String data = ev.getJSON();
            bootUnAvailableEvents.add(data);
        }
        return bootUnAvailableEvents;
    }

    /**
     * This method is to create random ras events.
     * @param seed to repeat same type of data.
     * @param location where events should be created.
     * @param rasMetadaData metadata of ras events.
     * @return ras event.
     * @throws NetworkListenerProviderException unable to create ras event.
     */
    private ForeignEvent createRandomRASEvent(long seed, @NotNull final String location, @NotNull final List<PropertyDocument> rasMetadaData) throws NetworkListenerProviderException {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToXName(location));
        PropertyArray result = (PropertyArray) rasMetadaData.get(generateRandomNumberBetween(0, rasMetadaData.size(), seed));
        ev.setEventType(result.get(0).toString());
        return ev;
    }

    /**
     * This method is to create random ras events.
     * @param seed to repeat same type of data.
     * @param location where events should be created.
     * @param definitionSensorMetadata_ metadata of sensor events.
     * @return sensor event.
     * @throws NetworkListenerProviderException unable to create ras event.
     */
    private ForeignEvent createRandomSensorEvent(long seed, String location, List<PropertyDocument> definitionSensorMetadata_) throws NetworkListenerProviderException, SimulatorException {
        // Ignore seed in the current implementation of randomization
        ForeignEventSensor ev = new ForeignEventSensor();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToXName(location));

        PropertyMap sensorDetails = (PropertyMap) definitionSensorMetadata_.get(generateRandomNumberBetween(0, definitionSensorMetadata_.size(), seed));
        if (sensorDetails == null) {
            throw new SimulatorException("Unable to find sensor details for a component of type: ");
        }
        ev.setSensorName(sensorDetails.getStringOrDefault("id", "UNKNOWN"));
        ev.setSensorUnits(sensorDetails.getStringOrDefault("unit", "UNKNOWN"));
        ev.setSensorID(sensorDetails.getStringOrDefault("id", "UNKNOWN"));
        if(!sensorDetails.getStringOrDefault("unit", "UNKNOWN").equals(""))
            ev.setSensorValue(String.valueOf(generateRandomNumberBetween(20, 40, seed)));
        else
            ev.setSensorValue(String.valueOf(0));
        return ev;
    }

    /**
     * This method is to create node failure event.
     * @param location where node failure event is created.
     * @return node failure event.
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    private ForeignEvent createNodeFailureEvent(String location) throws NetworkListenerProviderException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToXName(location));
        ev.setRole("Compute");
        ev.setState("Empty");
        ev.setStatus("AdminDown");
        return ev;
    }

    /**
     * This method is to create node unavailable event.
     * @param location where node unavailable event is created.
     * @return node unavailable event.
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    private ForeignEvent createUnavailableEvent(String location) throws NetworkListenerProviderException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToXName(location));
        ev.setRole("Compute");
        ev.setState("Off");
        ev.setStatus("AdminDown");
        return ev;
    }

    /**
     * This method is to create node booting event.
     * @param location where node booting event is created.
     * @return node booting event.
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    private ForeignEvent createBootingEvent(String location) throws NetworkListenerProviderException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToXName(location));
        ev.setRole("Compute");
        ev.setState("On");
        ev.setStatus("AdminDown");
        return ev;
    }

    /**
     * This method is to create node available event.
     * @param location where node available event is created.
     * @return node available event.
     * @throws NetworkListenerProviderException when unable to find foreign location
     */
    private ForeignEvent createAvailableEvent(String location) throws NetworkListenerProviderException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToXName(location));
        ev.setRole("Compute");
        ev.setState("Ready");
        ev.setStatus("AdminDown");
        return ev;
    }

/*    private String getRandomBootImageId() { // This is currently VERY specific. Needs generalization.
        String id = "mOS";
        try {
            id = bootImageInfo_.getMap(0).getStringOrDefault("id", ""); // Not really random but the first id.
        } catch(IndexOutOfBoundsException | PropertyNotExpectedType e) { *//* Live with the first default choice! *//* }
        return id;
    }*/

    /**
     * This method is to pick index between ranges.
     * @param from start index.
     * @param to end index
     * @return random number.
     */
    private int generateRandomNumberBetween( long from, long to, Long randomizerSeed) {
        if(randomizerSeed != null) {
            if (randomNumberWithSeed == null)
                randomNumberWithSeed = new Random(randomizerSeed);
            return (int) ((randomNumberWithSeed.nextDouble() * (to - from)) + from);
        }
        else {
            if (randomNumber == null)
                randomNumber = new Random();
            return (int) ((randomNumber.nextDouble() * (to - from)) + from);
        }
    }

    /**
     * This method is to convert time to micro seconds.
     * @param timestamp current time.
     * @return time in microseconds.
     */
    private static long convertInstantToMicrosec(Instant timestamp) {
        return TimeUnit.SECONDS.toMicros(timestamp.getEpochSecond())+TimeUnit.NANOSECONDS.toMicros(timestamp.getNano());
    }

    private final Logger log_;
    //private PropertyArray bootImageInfo_;
    private Random randomNumberWithSeed;
    private Random randomNumber;
}