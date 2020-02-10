package com.intel.dai.eventsim;

import com.intel.dai.network_listener.NetworkListenerProviderException;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyNotExpectedType;

import java.util.List;

/**
 * Description of class SimulatorEngine.
 * process the input data request to generate respective events like ras sensor and boot.
 * creates respective events like ras sensor and boot.
 */
public class SimulatorEngine {

    private final SystemGenerator system_;
    private long eventsCount_;
    private long timeDelayMus_;
    private boolean constantMode_;
    private long randomiserSeed_;
    private long publishedEvents;

    SimulatorEngine(DataLoaderEngine dataLoaderEngine, NetworkObject source, Logger log) {
        dataLoaderEngine_ = dataLoaderEngine;
        source_ = source;
        system_ = new SystemGenerator(log);
    }

    /**
     * This method is to load locations and display.
     *
     * @throws SimulatorException when unable to generate system generator.
     */
    public void initialize() throws SimulatorException {
        loadData();
        system_.generateSystem(dataLoaderEngine_);
        System.out.println("SYSTEM HEIRARCHY");
        systemHierarchy();
    }

    /**
     * This method is used to find the number of events published to network.
     *
     * @return events sent to network.
     */
    long getNumberOfEventsPublished() {
        return publishedEvents;
    }

    /**
     * This method is used to create and send boot events to network.
     *
     * @param locationRegex    regex of locations.
     * @param probFailureValue probability that nodes can be failed.
     * @param mode             with or without delay.
     * @throws SimulatorException when unable to create or send boot events.
     */
    void publishBootEvents(String locationRegex, String probFailureValue, final String mode) throws SimulatorException {
        publishedEvents = 0;
        try {
            float bfValue = 0;
            if (mode != null)
                constantMode_ = Boolean.parseBoolean(mode);
            if (probFailureValue != null)
                bfValue = Float.parseFloat(probFailureValue);
            if (locationRegex == null)
                locationRegex = ".*";
            if (ExistsLocationsMatchedRegex(locationRegex, EVENT_TYPE.BOOT)) {
                List<String> bootEvents = system_.publishBootEventsForLocation(bfValue);
                source_.send(ForeignEvent.EVENT_SUB_TYPE.stateChanges.toString(), bootEvents, constantMode_, timeDelayMus_);
                publishedEvents = bootEvents.size();
            }
        } catch (final RESTClientException | NetworkListenerProviderException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send ras events to network.
     *
     * @param locationRegex regex of locations.
     * @param regexLabel    regex for label descriptions.
     * @param count         number of events to be created and sent.
     * @param mode          with or without delay.
     * @throws SimulatorException when unable to create or send ras events.
     */
    void publishRasEvents(String locationRegex, String regexLabel, final String count, final String mode) throws SimulatorException {
        publishedEvents = 0;
        try {
            loadDefaults();
            if (mode != null)
                constantMode_ = Boolean.parseBoolean(mode);
            if (count != null)
                eventsCount_ = Integer.parseInt(count);
            if (regexLabel == null)
                regexLabel = ".*";
            if (locationRegex == null)
                locationRegex = ".*";
            if (ExistsLocationsMatchedRegex(locationRegex, EVENT_TYPE.RAS) && ExistsMatchedRegexLabel(regexLabel, EVENT_TYPE.RAS)) {
                List<String> rasEvents = system_.publishRASEventsForLocation(eventsCount_, randomiserSeed_);
                source_.send(ForeignEvent.EVENT_SUB_TYPE.events.toString(), rasEvents, constantMode_, timeDelayMus_);
                publishedEvents = rasEvents.size();
            }
        } catch (final PropertyNotExpectedType | NetworkListenerProviderException | RESTClientException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send sensor events to network.
     *
     * @param locationRegex regex of locations.
     * @param regexLabel    regex for label descriptions.
     * @param count         number of events to be created and sent.
     * @param mode          with or without delay.
     * @throws SimulatorException when unable to create or send sensor events.
     */
    void publishSensorEvents(String locationRegex, String regexLabel, final String count, final String mode) throws SimulatorException {
        publishedEvents = 0;
        try {
            loadDefaults();
            if (mode != null)
                constantMode_ = Boolean.parseBoolean(mode);
            if (count != null)
                eventsCount_ = Integer.parseInt(count);
            if (regexLabel == null)
                regexLabel = ".*";
            if (locationRegex == null)
                locationRegex = ".*";
            if (ExistsLocationsMatchedRegex(locationRegex, EVENT_TYPE.SENSOR) && ExistsMatchedRegexLabel(regexLabel, EVENT_TYPE.SENSOR)) {
                List<String> sensorEvents = system_.publishSensorEventsForLocation(eventsCount_, randomiserSeed_);
                source_.send(ForeignEvent.EVENT_SUB_TYPE.telemetry.toString(), sensorEvents, constantMode_, timeDelayMus_);
                publishedEvents = sensorEvents.size();
            }
        } catch (final PropertyNotExpectedType | NetworkListenerProviderException | RESTClientException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is find whether regex matched locations exits or not.
     *
     * @param regexLocation regex of locations.
     * @param eventType     type of the event requested.
     * @return true when matched regex locations exists
     * @throws SimulatorException when no matched regex locations.
     */
    private boolean ExistsLocationsMatchedRegex(final String regexLocation, final EVENT_TYPE eventType) throws SimulatorException {
        if (system_.getMatchedRegexLocations(regexLocation) == 0) {
            if (eventType.equals(EVENT_TYPE.BOOT))
                throw new SimulatorException("No Matched Regex Locations to start Boot Sequence.");
            else if (eventType.equals(EVENT_TYPE.RAS))
                throw new SimulatorException("No Matched Regex Locations to generate RAS Events.");
            else if (eventType.equals(EVENT_TYPE.SENSOR))
                throw new SimulatorException("No Matched Regex Locations to generate Sensor Events.");
        }
        return true;
    }

    /**
     * This method is find whether regex matched decriptions exits or not.
     *
     * @param regexLabel regex of label description.
     * @param eventType  type of the event requested.
     * @return true when matched regex label descriptions exists
     * @throws SimulatorException when no matched regex label descriptions.
     */
    private boolean ExistsMatchedRegexLabel(final String regexLabel, final EVENT_TYPE eventType) throws PropertyNotExpectedType, SimulatorException {
        if (system_.getMatchedRegexLabels(regexLabel, eventType) == 0) {
            if (eventType.equals(EVENT_TYPE.RAS))
                throw new SimulatorException("No Matched Regex Locations to generate RAS Events.");
            else if (eventType.equals(EVENT_TYPE.SENSOR))
                throw new SimulatorException("No Matched Regex Locations to generate Sensor Events.");
        }
        return true;
    }

    /**
     * This method is used to load location data from db.
     */
    private void loadData() throws SimulatorException {
        dataLoaderEngine_.loadData();
    }

    /**
     * This method is used to load default values from configuration file when respective parameters
     * are not passed through request.
     */
    private void loadDefaults() {
        eventsCount_ = dataLoaderEngine_.getDefaultNumberOfEventsToBeGenerated();
        timeDelayMus_ = dataLoaderEngine_.getDefaultTimeDelayMus();
        constantMode_ = false;
        randomiserSeed_ = dataLoaderEngine_.getDefaultRandomiserSeed();
    }

    /**
     * This method is used to display system location details.
     */
    private void systemHierarchy() {
        for (String location : dataLoaderEngine_.getNodeLocationData())
            System.out.println(location.toUpperCase());
    }

    enum EVENT_TYPE {
        BOOT,
        RAS,
        SENSOR
    }

    private final DataLoaderEngine dataLoaderEngine_;
    private final NetworkObject source_;
}