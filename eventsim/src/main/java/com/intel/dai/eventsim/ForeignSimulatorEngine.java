package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of class ForeignSimulatorEngine.
 * process the input data request.
 * create requested type of events like ras sensor and boot.
 * send generated events to network with latest timestamp created.
 */
class ForeignSimulatorEngine {

    private final EventTypeTemplate eventTypeTemplate_;

    ForeignSimulatorEngine(DataLoader dataLoaderEngine, NetworkObject source, Logger log) throws SimulatorException {
        dataLoaderEngine_ = dataLoaderEngine;
        source_ = source;
        log_ = log;
        jsonParser_ = ConfigIOFactory.getInstance("json");
        jsonPath_ = new JsonPath();
        eventTypeTemplate_ = new EventTypeTemplate(dataLoaderEngine_.getEventsTemplateConfigurationFile(), log_);
        filter_ = new ForeignFilter(jsonPath_, log_);
    }

    void initialize() {
        log_.info("SYSTEM HEIRARCHY");
        systemHierarchy();
    }

    void generateRasEvents(Map<String, String> parameters) throws SimulatorException {
        try {
            DataValidation.validateData(parameters, MISSING_KEY);

            String locationsRegex = parameters.getOrDefault("locations", ".*");
            filter_.validateLocations(dataLoaderEngine_.getNodeLocations(), locationsRegex);

            loadDefaults(parameters);
            
            String eventName = defaults.getOrDefault("sub_component", "fabric");
            String eventType = defaults.getOrDefault("type", eventTypeTemplate_.getDefaultEventType(eventName));
            long numOfEventsToGenerate = Long.parseLong(defaults.getOrDefault("count", "0"));
            long timeDelay = Long.parseLong(defaults.get("time-delay-mus"));
            long seed = Long.parseLong(defaults.get("seed"));
            boolean burst = Boolean.parseBoolean(defaults.get("burst"));
            String output = defaults.get("output");
            String eventTypeTemplateFile = defaults.get("template");

            eventTypeTemplate_.validateEventNameAndType(eventName, eventType);
            eventTypeTemplate_.loadData(eventType);
            eventTypeTemplate_.setEventTypeTemplateFile(eventTypeTemplateFile);

            String streamName = eventTypeTemplate_.getEventTypeStreamName();
            String jpathToTimestamp = eventTypeTemplate_.getPathToUpdateTimestamp();

            PropertyDocument event = filter_.generateEvents(eventTypeTemplate_, numOfEventsToGenerate, seed);
            publishEvents(event, streamName, burst, timeDelay, jpathToTimestamp, output);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    private void publishEvents(PropertyDocument events, String streamId, boolean burstMode, long timeDelayMus, String timestampJPath, String output) throws PropertyNotExpectedType, SimulatorException {
        try {
            publishedEvents_ = 0;
            ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.info("Start Time : " + startTime.toString());
            PropertyArray eventsInfo = events.getAsArray();
            for (int i = 0; i < eventsInfo.size(); i++) {
                PropertyMap event = eventsInfo.getMap(i);
                publishedEvents_ += jsonPath_.setTime(timestampJPath, event.getAsMap(), "UTC");
                source_.send(streamId, jsonParser_.toString(event));
                if(!burstMode)
                    delayMicroSecond(timeDelayMus);
            }
            ZonedDateTime endTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.info("End Time : " + endTime.toString());
            log_.info("Published events =  " + publishedEvents_);
            log_.info("Published messages =  " + eventsInfo.size());
            log_.debug("Total Time to publish " + publishedEvents_ + " events :" + (Duration.between(startTime, endTime).toMillis()) + " milli-seconds");
            if(output != null) {
                LoadFileLocation.writeFile(eventsInfo, output, true);
            }
        } catch (RESTClientException | IOException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    private void loadDefaults(Map<String, String> parameters) {
        defaults.clear();
        defaults.putAll(parameters);
        defaults.put("events-template-config",
                dataLoaderEngine_.getEventsConfigutaion("events-template-config", null));
        defaults.put("burst", parameters.getOrDefault("burst",
                dataLoaderEngine_.getEventsConfigutaion("burst", "true")));
        defaults.put("count", parameters.getOrDefault("count",
                dataLoaderEngine_.getEventsConfigutaion("count", "0")));
        defaults.put("seed", parameters.getOrDefault("seed",
                dataLoaderEngine_.getEventsConfigutaion("seed", String.valueOf(System.nanoTime()))));
        defaults.put("time-delay-mus", parameters.getOrDefault("time-delay-mus",
                dataLoaderEngine_.getEventsConfigutaion("time-delay-mus", "0")));
        defaults.put("boot-failure-prob", parameters.getOrDefault("boot-failure-prob",
                dataLoaderEngine_.getEventsConfigutaion("boot-failure-prob", "0")));
        defaults.put("template", parameters.getOrDefault("template", null));
    }

    private void systemHierarchy() {
        for (String location : dataLoaderEngine_.getNodeLocations())
            log_.info(location.toUpperCase());
    }

    /**
     * This method is used to create a constant delay.
     * @param delayTimeMus time delay in microseconds.
     */
    private void delayMicroSecond(final long delayTimeMus) {
        long waitUntil = System.nanoTime() + (delayTimeMus * 1000);
        while(waitUntil > System.nanoTime()) {;}
    }

    Map<String, String> defaults = new HashMap<>();

    private final DataLoader dataLoaderEngine_;
    private final ForeignFilter filter_;
    private final JsonPath jsonPath_;
    private final NetworkObject source_;
    private final Logger log_;
    private final ConfigIO jsonParser_;

    protected long publishedEvents_;

    private static final String MISSING_KEY = "Given key/data is null, key = ";
}
