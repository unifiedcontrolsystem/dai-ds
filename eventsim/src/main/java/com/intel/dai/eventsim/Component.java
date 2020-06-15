package com.intel.dai.eventsim;

import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
     * @throws ConversionException unable to create ras event.
     */
    List<ForeignEvent> publishRASEvents(final long eventsPerLocation, final long seed,
                                  @NotNull final List<String> regexMatchedLocations,
                                  @ NotNull final List<PropertyDocument> regexMatchedLabelDescriptions)
            throws ConversionException {
        List<ForeignEvent> rasEvents = new ArrayList<>();
        randomNumber.setSeed(seed);
        for (String location : regexMatchedLocations) {
            for (int i = 0; i < eventsPerLocation; i++) {
                ForeignEvent event = createRandomRASEvent(seed, location, regexMatchedLabelDescriptions);
                rasEvents.add(event);
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
     * @throws ConversionException unable to create ras event.
     */
    List<ForeignEvent> publishRemRASEvents(final long remEvents, final long seed,
                                     @NotNull final List<String> regexMatchedLocations,
                                     @ NotNull final List<PropertyDocument> regexMatchedLabelDescriptions)
            throws ConversionException {
        long eventsTobeGenerated = remEvents;
        randomNumber.setSeed(seed);
        List<ForeignEvent> rasEvents = new ArrayList<>();
        for (String location : regexMatchedLocations) {
            if(eventsTobeGenerated == 0)
                return rasEvents;
            ForeignEvent event = createRandomRASEvent(seed, location, regexMatchedLabelDescriptions);
            rasEvents.add(event);
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
     * @throws ConversionException unable to create sensor event.
     */
    List<ForeignEvent> publishSensorEvents(final long eventsPerLocation, final long seed,
                                     @NotNull final List<String> regexMatchedLocations,
                                     @NotNull final List<PropertyDocument> regexMatchedLabelDescriptions)
            throws ConversionException, SimulatorException {
        List<ForeignEvent> rasEvents = new ArrayList<>();
        randomNumber.setSeed(seed);
        for (String location : regexMatchedLocations) {
            for (int i = 0; i < eventsPerLocation; i++) {
                ForeignEvent event = createRandomSensorEvent(i + seed, location, regexMatchedLabelDescriptions);
                rasEvents.add(event);
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
     * @throws ConversionException unable to create sensor event.
     */
    List<ForeignEvent> publishRemSensorEvents(final long remEvents, final long seed,
                                        @NotNull final List<String> regexMatchedLocations,
                                        @NotNull final List<PropertyDocument> regexMatchedLabelDescriptions)
            throws ConversionException, SimulatorException {
        long eventsTobeGenerated = remEvents;
        List<ForeignEvent> sensorEvents = new ArrayList<>();
        randomNumber.setSeed(seed);
        for (String location : regexMatchedLocations) {
            if(eventsTobeGenerated == 0)
                return sensorEvents;
            ForeignEvent event = createRandomSensorEvent(eventsTobeGenerated + seed, location, regexMatchedLabelDescriptions);
            sensorEvents.add(event);
            eventsTobeGenerated--;
        }
        return sensorEvents;
    }

    /**
     * This method is used to generate boot-ready events for a given count and location.
     * @param regexMatchedLocations locations matching location-regex input.
     * @param eventsCount number of boot-ready events to generate.
     * @return boot-ready events.
     * @throws ConversionException unable to create boot-ready events.
     */
    List<ForeignEvent> publishAvailableEventsForLocation(@NotNull final List<String> regexMatchedLocations, long eventsCount)
            throws ConversionException {
        if(eventsCount == -1)
            return publishAvailableEventsForLocation(regexMatchedLocations);
        List<ForeignEvent> bootAvailableEvents = new ArrayList<>();
        while(bootAvailableEvents.size() != eventsCount) {
            for (String location : regexMatchedLocations) {
                if (bootAvailableEvents.size() == eventsCount)
                    break;
                ForeignEvent event = createAvailableEvent(location);
                bootAvailableEvents.add(event);
            }
        }
        return bootAvailableEvents;
    }

    /**
     * This method is to create available boot events.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @return available boot events.
     * @throws ConversionException when unable to find foreign location
     */
    private List<ForeignEvent> publishAvailableEventsForLocation(@NotNull final List<String> regexMatchedLocations)
            throws ConversionException {
        List<ForeignEvent> bootAvailableEvents = new ArrayList<>();
        for(String location : regexMatchedLocations) {
            ForeignEvent event = createAvailableEvent(location);
            bootAvailableEvents.add(event);
        }
        return bootAvailableEvents;
    }

    /**
     * This method is used to generate boot-on events for a given location and count.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param totalFailureEventsToGenerate number of failure boot-on events to create.
     * @param eventsCount number of boot-on events to generate.
     * @return boot-on events.
     * @throws ConversionException
     */
    List<ForeignEvent> publishBootingEventsForLocation(final @NotNull List<String> regexMatchedLocations,
                                                       final long totalFailureEventsToGenerate, long eventsCount) throws ConversionException {
        if(eventsCount == -1)
            return publishBootingEventsForLocation(regexMatchedLocations, totalFailureEventsToGenerate);
        List<ForeignEvent> bootingEvents = new ArrayList<>();
        while(bootingEvents.size() != eventsCount) {
            for (String location : regexMatchedLocations) {
                if (bootingEvents.size() == eventsCount)
                    break;
                if (totalFailureEventsToGenerate == regexMatchedLocations.size()) {
                    ForeignEvent event = createNodeFailureEvent(location);
                    bootingEvents.add(event);
                } else if (totalFailureEventsToGenerate == 0) {
                    ForeignEvent event = createBootingEvent(location);
                    bootingEvents.add(event);
                } else {
                    int randomNumber = generateRandomNumberBetween(0, regexMatchedLocations.size());
                    if ((randomNumber % 2) == 0) {
                        ForeignEvent event = createNodeFailureEvent(location);
                        bootingEvents.add(event);
                    } else {
                        ForeignEvent event = createBootingEvent(location);
                        bootingEvents.add(event);
                    }
                }
            }
        }
        return bootingEvents;
    }

    /**
     * This method is to create available boot events.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param totalFailureEventsToGenerate number of failure boot events to create.
     * @return boot events for a given location
     * @throws ConversionException when unable to find foreign location
     */
    private List<ForeignEvent> publishBootingEventsForLocation(final @NotNull List<String> regexMatchedLocations,
                                                 final long totalFailureEventsToGenerate) throws ConversionException {
        List<ForeignEvent> bootingEvents = new ArrayList<>();
        for(String location : regexMatchedLocations) {
            if(totalFailureEventsToGenerate == regexMatchedLocations.size()) {
                ForeignEvent event = createNodeFailureEvent(location);
                bootingEvents.add(event);
            } else if(totalFailureEventsToGenerate == 0) {
                ForeignEvent event = createBootingEvent(location);
                bootingEvents.add(event);
            } else {
                int randomNumber = generateRandomNumberBetween(0, regexMatchedLocations.size());
                if((randomNumber % 2) == 0) {
                    ForeignEvent event = createNodeFailureEvent(location);
                    bootingEvents.add(event);
                } else {
                    ForeignEvent event = createBootingEvent(location);
                    bootingEvents.add(event);
                }
            }
        }
        return bootingEvents;
    }

    /**
     * This method is used to generate boot-off events for a given count and location.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @param eventsCount number of boot-off events to generate.
     * @return boot-off events.
     * @throws ConversionException unable to create boot-off event.
     */
    List<ForeignEvent> publishUnAvailableEventsForLocation(@NotNull final List<String> regexMatchedLocations, long eventsCount)
            throws ConversionException {
        if(eventsCount == -1)
            return publishUnAvailableEventsForLocation(regexMatchedLocations);
        List<ForeignEvent> bootUnAvailableEvents = new ArrayList<>();
        while(bootUnAvailableEvents.size() != eventsCount) {
            for (String location : regexMatchedLocations) {
                if (bootUnAvailableEvents.size() == eventsCount)
                    break;
                ForeignEvent event = createUnavailableEvent(location);
                bootUnAvailableEvents.add(event);
            }
        }
        return bootUnAvailableEvents;
    }

    /**
     * This method is to create unavailable boot events.
     * @param regexMatchedLocations locations matching the location-regex input.
     * @return unavailable boot events.
     * @throws ConversionException when unable to find foreign location
     */
    private List<ForeignEvent> publishUnAvailableEventsForLocation(@NotNull final List<String> regexMatchedLocations)
            throws ConversionException {
        List<ForeignEvent> bootUnAvailableEvents = new ArrayList<>();
        for(String location : regexMatchedLocations) {
            ForeignEvent event = createUnavailableEvent(location);
            bootUnAvailableEvents.add(event);
        }
        return bootUnAvailableEvents;
    }

    /**
     * This method is to create random ras events.
     * @param seed to repeat same type of data.
     * @param location where events should be created.
     * @param rasMetadaData metadata of ras events.
     * @return ras event.
     * @throws ConversionException unable to create ras event.
     */
    private ForeignEvent createRandomRASEvent(long seed, @NotNull final String location, @NotNull final List<PropertyDocument> rasMetadaData) throws ConversionException {
        ForeignEventRAS ev = new ForeignEventRAS();
        ev.setLocation(CommonFunctions.convertLocationToForeign(location));
        PropertyArray result = (PropertyArray) rasMetadaData.get(generateRandomNumberBetween(0, rasMetadaData.size()));
        ev.setEventType(result.get(0).toString());
        ev.getJSON();
        return ev;
    }

    /**
     * This method is to create random ras events.
     * @param seed to repeat same type of data.
     * @param location where events should be created.
     * @param definitionSensorMetadata_ metadata of sensor events.
     * @return sensor event.
     * @throws ConversionException unable to create ras event.
     */
    private ForeignEvent createRandomSensorEvent(long seed, String location, List<PropertyDocument> definitionSensorMetadata_) throws ConversionException, SimulatorException {
        // Ignore seed in the current implementation of randomization
        ForeignEventSensor ev = new ForeignEventSensor();
        //ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToForeign(location));

        PropertyMap sensorDetails = (PropertyMap) definitionSensorMetadata_.get(generateRandomNumberBetween(0, definitionSensorMetadata_.size()));
        if (sensorDetails == null) {
            throw new SimulatorException("Unable to find sensor details for a component of type: ");
        }
        ev.setSensorName(sensorDetails.getStringOrDefault("id", "UNKNOWN"));
        ev.setSensorUnits(sensorDetails.getStringOrDefault("unit", "UNKNOWN"));
        ev.setSensorID(sensorDetails.getStringOrDefault("id", "UNKNOWN"));
        if(!sensorDetails.getStringOrDefault("unit", "UNKNOWN").equals(""))
            ev.setSensorValue(String.valueOf(generateRandomNumberBetween(20, 40)));
        else
            ev.setSensorValue(String.valueOf(0));
        ev.getJSON();
        return ev;
    }

    /**
     * This method is to create node failure event.
     * @param location where node failure event is created.
     * @return node failure event.
     * @throws ConversionException when unable to find foreign location
     */
    private ForeignEvent createNodeFailureEvent(String location) throws ConversionException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setLocation(CommonFunctions.convertLocationToForeign(location));
        ev.setRole("Compute");
        ev.setState("Empty");
        ev.setStatus("AdminDown");
        ev.getJSON();
        return ev;
    }

    /**
     * This method is to create node unavailable event.
     * @param location where node unavailable event is created.
     * @return node unavailable event.
     * @throws ConversionException when unable to find foreign location
     */
    private ForeignEvent createUnavailableEvent(String location) throws ConversionException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setLocation(CommonFunctions.convertLocationToForeign(location));
        ev.setRole("Compute");
        ev.setState("Off");
        ev.setStatus("AdminDown");
        ev.getJSON();
        return ev;
    }

    /**
     * This method is to create node booting event.
     * @param location where node booting event is created.
     * @return node booting event.
     * @throws ConversionException when unable to find foreign location
     */
    private ForeignEvent createBootingEvent(String location) throws ConversionException {
        ForeignEventBoot ev = new ForeignEventBoot();
        ev.setLocation(CommonFunctions.convertLocationToForeign(location));
        ev.setRole("Compute");
        ev.setState("On");
        ev.setStatus("AdminDown");
        ev.getJSON();
        return ev;
    }

    /**
     * This method is to create node available event.
     * @param location where node available event is created.
     * @return node available event.
     * @throws ConversionException when unable to find foreign location
     */
    private ForeignEvent createAvailableEvent(String location) throws ConversionException {
        ForeignEventBoot ev = new ForeignEventBoot();
        //ev.setTimestamp(convertInstantToMicrosec(Instant.now()));
        ev.setLocation(CommonFunctions.convertLocationToForeign(location));
        ev.setRole("Compute");
        ev.setState("Ready");
        ev.setStatus("AdminDown");
        ev.getJSON();
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
    private int generateRandomNumberBetween( long from, long to) {
        return (int) ((randomNumber.nextDouble() * (to - from)) + from);
    }

    /**
     * This method is to convert time to micro seconds.
     */
/*    private static long convertInstantToMicrosec(Instant timestamp) {
        return TimeUnit.SECONDS.toMicros(timestamp.getEpochSecond())+TimeUnit.NANOSECONDS.toMicros(timestamp.getNano());
    }*/

    private final Logger log_;
    private Random randomNumber = new Random();
}