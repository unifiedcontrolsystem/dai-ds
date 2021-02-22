package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Description of class ForeignSimulatorEngine.
 * process the input data request.
 * create requested type of events like ras sensor and boot.
 * send generated events to network with latest timestamp created.
 */
class ForeignSimulatorEngine {

    ForeignSimulatorEngine(DataLoader dataLoaderEngine, NetworkObject networkSource, Logger log) {
        dataLoaderEngine_ = dataLoaderEngine;
        network_ = networkSource;
        log_ = log;
        jsonParser_ = ConfigIOFactory.getInstance("json");
        jsonPath_ = new JsonPath();
        eventTypeTemplate_ = new EventTypeTemplate(dataLoaderEngine_.getEventsTemplateConfigurationFile(), log_);
        filter_ = new ForeignFilter(jsonPath_, log_);
        foreignScenario_ = new ForeignScenario(log_);
        echoEvent_ = new ForeignEventEcho();
    }

    void initialize() {
        log_.info("SYSTEM HEIRARCHY");
        systemHierarchy();
    }

    void generateBootEvents(Map<String, String> parameters) throws SimulatorException {
        PropertyArray events = new PropertyArray();
        try {
            parameters.put("count", "1");
            String state = parameters.getOrDefault("type", BOOT_STATES.all.toString());
            if(BOOT_STATES.valueOf(state).equals(BOOT_STATES.all)) {
                parameters.put("type", BOOT_STATES.off.toString());
                events.addAll(generateEvents(parameters, EVENT_TYPE.BOOT.toString().toLowerCase()).getAsArray());
                parameters.put("type", BOOT_STATES.on.toString());
                events.addAll(generateEvents(parameters, EVENT_TYPE.BOOT.toString().toLowerCase()).getAsArray());
                parameters.put("type", BOOT_STATES.ready.toString());
            }
            events.addAll(generateEvents(parameters, EVENT_TYPE.BOOT.toString().toLowerCase()).getAsArray());

            PropertyArray publishEvents = new PropertyArray();
            publishEvents.add(events);
            publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void generateRasEvents(Map<String, String> parameters) throws SimulatorException {
        try {
            PropertyArray events = generateEvents(parameters, EVENT_TYPE.RAS.toString().toLowerCase()).getAsArray();
            PropertyArray publishEvents = new PropertyArray();
            publishEvents.add(events);
            publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void generateSensorEvents(Map<String, String> parameters) throws SimulatorException {
        try {
            PropertyArray events = generateEvents(parameters, EVENT_TYPE.SENSOR.toString().toLowerCase()).getAsArray();
            PropertyArray publishEvents = new PropertyArray();
            publishEvents.add(events);
            publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void generateEventsForScenario(Map<String, String> parameters) throws SimulatorException {
        try {
            String scenarioConfigFile = parameters.get("file");
            foreignScenario_.setScenarioConfigFile(scenarioConfigFile);
            foreignScenario_.loadScenarioConfigData();

            String scenarioMode = parameters.getOrDefault("type", foreignScenario_.getScenarioMode());
            PropertyMap scenarioModeConfig = foreignScenario_.getScenarioModeConfigData(scenarioMode);

            String paramDelay = parameters.get("time-delay-mus");
            String timedelay = foreignScenario_.getScenarioDelay();
            if(paramDelay == null && timedelay != null)
                parameters.put("time-delay-mus",timedelay);

            String paramSeed = parameters.get("seed");
            String seed = foreignScenario_.getScenarioSeed(scenarioMode);
            if(paramSeed == null && seed != null)
                parameters.put("seed", seed);

            PropertyArray events = foreignScenario_.getEventsInfo();

            PropertyMap generatedEvents = generateEvents(parameters, scenarioMode, events, scenarioModeConfig);
            publishEventsForScenario(parameters, scenarioMode, generatedEvents, scenarioModeConfig);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void generateEchoEvents(Map<String, String> parameters) throws SimulatorException {
        String echoFile = parameters.get("file");
        String streamID = parameters.get("connection");

        try {

            // Read Event
            echoEvent_.processMessage(echoFile);

            // Publish Event
            publishEchoEvent(echoEvent_.props_, streamID);

        } catch(IllegalArgumentException e) {
            throw new SimulatorException( streamID + " is not a valid stream");
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }

    }

    /**
     * This method is used to return prior randomization seed to replicate data.
     * @return seed value
     */
    String getRandomizationSeed() {
        return String.valueOf(randomiserSeed_);
    }

    /**
     * This method is used to return available locatiosn data.
     * @return locations data
     */
    PropertyArray getAllAvailableLocations() {
        return new PropertyArray(dataLoaderEngine_.getNodeLocations());
    }

    private void publishEventsForScenario(Map<String, String> parameters, String scenario,
                                          PropertyMap events, PropertyMap scenarioParameters)
            throws PropertyNotExpectedType, SimulatorException {
        PropertyArray publishEvents;
        String startTime =  parameters.getOrDefault("start-time", scenarioParameters.getStringOrDefault("start-time", ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime().toString()) + "Z");
        switch (scenario) {
            case "burst":
                publishEvents = foreignScenario_.generateEventsForBurstMode(events, scenario);
                waitForSceduleTime(startTime);
                publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
                break;
            case "group-burst":
                publishEvents = foreignScenario_.generateEventsForGroupBurstMode(events, scenario);
                waitForSceduleTime(startTime);
                publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
                break;
            default:
                generateEventsForRepeatMode(events, parameters, scenarioParameters);
                break;
        }
    }

    private PropertyMap generateEvents(Map<String, String> parameters, String scenario,
                                       PropertyArray events, PropertyMap scenarioParameters) throws SimulatorException, PropertyNotExpectedType {
        if(scenario.equals("repeat"))
            scenario = parameters.getOrDefault("repeat-mode", scenarioParameters.getString("mode"));
        return generateEventsForScenarioMode(events, parameters, scenario);
    }

    private void publishEvents(PropertyDocument batch, boolean burstMode, long timeDelayMus,
                               String output, String zone) throws PropertyNotExpectedType,
            SimulatorException {
        try {
            publishedEvents_ = 0;
            ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.info("Start Time : " + startTime.toString());
            PropertyArray batchInfo = batch.getAsArray();
            PropertyArray batchOutput = new PropertyArray();
            long publishedMessages = 0;
            for (int i = 0; i < batchInfo.size(); i++) {
                PropertyArray eventsInfo = batchInfo.getArray(i);
                for (int j = 0; j < eventsInfo.size(); j++) {
                    PropertyMap eventInfo = eventsInfo.getMap(j);
                    String streamId = eventInfo.getString(STREAM_ID);
                    String timestampJPath = eventInfo.getString(TIMESTAMP_PATH);
                    PropertyMap event = eventInfo.getMap(STREAM_MESSAGE);
                    publishedEvents_ += jsonPath_.setTime(timestampJPath, event.getAsMap(), zone);
                    network_.send(streamId, jsonParser_.toString(event));
                    batchOutput.add(event);
                    if (!burstMode)
                        delayMicroSecond(timeDelayMus);
                }

                publishedMessages = publishedMessages + eventsInfo.size();
            }
            ZonedDateTime endTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.info("End Time : " + endTime.toString());
            log_.info("Published events =  " + publishedEvents_);
            log_.info("Published messages =  " + publishedMessages);
            log_.debug("Total Time to publish " + publishedEvents_ + " events :" + (Duration.between(startTime, endTime).toMillis()) + " milli-seconds");
            if(output != null) {
                LoadFileLocation.writeFile(batchOutput, output, true);
            }
        } catch (RESTClientException | IOException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    private void publishEchoEvent ( PropertyMap event, String streamID ) throws SimulatorException {
        try {
            ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.info("Publishing echo");
            log_.info("Start Time : " + startTime.toString());


            if ( !network_.isStreamIDValid(streamID ) ) {
                throw new SimulatorException(streamID + " is not a valid streamID.");
            }

            network_.send(streamID, jsonParser_.toString(event));

            ZonedDateTime endTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.info("End Time : " + endTime.toString());
            log_.debug("Total Time to publish " + 1 + " events :" + (Duration.between(startTime, endTime).toMillis()) + " milli-seconds");
        } catch (Exception  e) {
            log_.debug(e.getMessage());
            throw new SimulatorException(e.getMessage());
        }
    }

    private PropertyDocument generateEvents(Map<String, String> parameters, String type) throws SimulatorException, PropertyNotExpectedType, IOException, ConfigIOParseException, ConversionException {
        DataValidation.isDataNullOrEmpty(parameters, MISSING_KEY);

        String locationsRegex = parameters.getOrDefault("locations", ".*");
        filter_.validateLocations(dataLoaderEngine_.getNodeLocations(), locationsRegex);

        loadDefaults(parameters);

        String eventName = defaults.getOrDefault("sub_component", type);
        String eventType = defaults.getOrDefault("type", eventTypeTemplate_.getDefaultEventType(eventName));
        long numOfEventsToGenerate = Long.parseLong(defaults.getOrDefault("count", DEFAULT_COUNT));
        randomiserSeed_ = Long.parseLong(defaults.get("seed"));
        String eventTypeTemplateFile = defaults.get("template");

        eventTypeTemplate_.validateEventNameAndType(eventName, eventType);
        eventTypeTemplate_.loadData(eventType);
        eventTypeTemplate_.setEventTypeTemplateFile(eventTypeTemplateFile);

        timeDelay_ = Long.parseLong(defaults.get("time-delay-mus"));
        burst_ = Boolean.parseBoolean(defaults.get("burst"));
        output_ = defaults.get("output");
        zone_ = defaults.get("timezone");

        PropertyArray events =  filter_.generateEvents(eventTypeTemplate_, updateJpathFieldFilters_,
                                templateFieldFilters_, numOfEventsToGenerate, randomiserSeed_).getAsArray();

        String streamName = eventTypeTemplate_.getEventTypeStreamName();
        String jpathToTimestamp = eventTypeTemplate_.getPathToUpdateTimestamp();

        STREAM_DATA.put(STREAM_ID, streamName);
        STREAM_DATA.put(TIMESTAMP_PATH, jpathToTimestamp);
        filter_.assignStreamDataToAllEvents(STREAM_DATA, STREAM_MESSAGE, events);
        return events;
    }

    private void generateEventsForRepeatMode(PropertyMap events, Map<String, String> parameters,
                                             PropertyMap scenarioParameters) throws PropertyNotExpectedType, SimulatorException {
        String repeatModeScenario = scenarioParameters.getString("mode");

        PropertyArray events_ = new PropertyArray();
        if(repeatModeScenario.equals("burst"))
            events_ = foreignScenario_.generateEventsForBurstMode(events, repeatModeScenario);

        else if (repeatModeScenario.equals("group-burst"))
            events_ = foreignScenario_.generateEventsForGroupBurstMode(events, repeatModeScenario);

        String startTime = parameters.getOrDefault("start-time", scenarioParameters.getString("start-time"));
        waitForSceduleTime(startTime);

        String clockMode = "";
        if (parameters.getOrDefault("counter", null) != null)
            clockMode = "counter";

        if(parameters.getOrDefault("duration", null) != null)
            clockMode = "duration";

        if(clockMode.isEmpty())
            clockMode = scenarioParameters.getString("clock-mode");

        if(clockMode.equals("start-time")) {
            while(true)
                publishEvents(events_, burst_, timeDelay_, output_, zone_);
        }

        if(clockMode.equals("counter")) {
            String clockModeCounter = parameters.getOrDefault("counter", scenarioParameters.getString("counter"));
            long counterL = Long.parseLong(clockModeCounter);
            publishEventsCounterMode(events_, counterL);
        }
        else if(clockMode.equals("duration")) {
            String clockModeDuration = parameters.getOrDefault("duration", scenarioParameters.getString("duration"));
            long durationL = Long.parseLong(clockModeDuration);
            publishEventsDurationMode(events_, durationL);
        }
    }

    private PropertyMap generateEventsForScenarioMode(PropertyArray events, Map<String, String> parameters,
                                                      String scenario) throws SimulatorException {
        PropertyMap eventAndGenEventsInfo = new PropertyMap();
        PropertyArray publishEvents;
        try {
            for (Object event_object : events) {
                String event = event_object.toString();
                PropertyArray eventTypes = foreignScenario_.getEventTypesInfo(event);
                long totalEvents = Long.parseLong(foreignScenario_.getTotalEvents(scenario, event));
                long foreachEvent = totalEvents / eventTypes.size();
                long remEvents = totalEvents % eventTypes.size();
                publishEvents = new PropertyArray();
                Collections.shuffle(eventTypes);
                if (foreachEvent > 0) {
                    for (Object eventTypeObject : eventTypes) {
                        String eventType = eventTypeObject.toString();
                        parameters.put("sub_component", event);
                        parameters.put("type", eventType);
                        parameters.put("count", String.valueOf(foreachEvent));
                        publishEvents.addAll(generateEvents(parameters, event).getAsArray());
                    }
                }

                while (remEvents > 0) {
                    for (Object eventTypeObject : eventTypes) {
                        String eventType = eventTypeObject.toString();
                        parameters.put("sub_component", event);
                        parameters.put("type", eventType);
                        parameters.put("count", "1");
                        if (remEvents > 0)
                            publishEvents.addAll(generateEvents(parameters, event).getAsArray());
                        remEvents--;
                    }
                }
                eventAndGenEventsInfo.put(event, publishEvents);
            }
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
        return eventAndGenEventsInfo;
    }

    /**
     * This method is used to load all default values
     * @param parameters input parameters from api request
     */
    private void loadDefaults(Map<String, String> parameters) {
        defaults.clear();
        defaults.putAll(parameters);
        defaults.put("events-template-config",
                dataLoaderEngine_.getEventsConfiguration("events-template-config", null));
        defaults.put("burst", parameters.getOrDefault("burst",
                dataLoaderEngine_.getEventsConfiguration("burst", "true")));
        defaults.put("count", parameters.getOrDefault("count",
                dataLoaderEngine_.getEventsConfiguration("count", "0")));
        defaults.put("seed", parameters.getOrDefault("seed",
                dataLoaderEngine_.getEventsConfiguration("seed", String.valueOf(System.nanoTime()))));
        defaults.put("time-delay-mus", parameters.getOrDefault("time-delay-mus",
                dataLoaderEngine_.getEventsConfiguration("time-delay-mus", "0")));
        defaults.put("boot-failure-prob", parameters.getOrDefault("boot-failure-prob",
                dataLoaderEngine_.getEventsConfiguration("boot-failure-prob", "0")));
        defaults.put("template", parameters.getOrDefault("template", null));
        defaults.put("timezone", parameters.getOrDefault("timezone",
                dataLoaderEngine_.getEventsConfiguration("timezone", ZoneId.systemDefault().toString())));

        STREAM_DATA.put(STREAM_ID, "");
        STREAM_DATA.put(STREAM_MESSAGE, "");
        STREAM_DATA.put(TIMESTAMP_PATH, "");

        updateJpathFieldFilters_.clear();
        String[] updateFieldJpaths = parameters.getOrDefault("update-field-jpath", "").split(",");
        String[] updateFieldMetadata = parameters.getOrDefault("update-field-metadata", "").split(",");
        String[] updateFieldMetadataFilters = parameters.getOrDefault("update-field-metadata-filter", "").split(",");

        for(int index = 0; index < updateFieldJpaths.length && index < updateFieldMetadataFilters.length; index++) {
            PropertyMap updateJpathFieldFilter = new PropertyMap();
            if(!updateFieldJpaths[index].equals("") && !updateFieldMetadataFilters[index].equals("")) {
                updateJpathFieldFilter.put("jpath-field", updateFieldJpaths[index]);
                updateJpathFieldFilter.put("metadata-filter", updateFieldMetadataFilters[index]);
                updateJpathFieldFilters_.add(updateJpathFieldFilter);
            }

            if(updateFieldMetadata.length > index && !updateFieldMetadata[0].equals(""))
                updateJpathFieldFilter.put("metadata", updateFieldMetadata[index]);
        }


        templateFieldFilters_.clear();
        String[] templateFieldJpaths = parameters.getOrDefault("template-field-jpath", "").split(",");
        String[] templateFieldFilters = parameters.getOrDefault("template-field-filter", "").split(",");

        for(int index = 0; index < templateFieldJpaths.length && index < templateFieldFilters.length; index++) {
            PropertyMap templateField = new PropertyMap();
            if(!templateFieldJpaths[index].equals("") && !templateFieldFilters[index].equals("")) {
                templateField.put("jpath-field", templateFieldJpaths[index]);
                templateField.put("metadata-filter", templateFieldFilters[index]);
                templateFieldFilters_.add(templateField);
            }
        }

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
        while(waitUntil > System.nanoTime());
    }

    /**
     * This method is used to wait till scheduled strat-time
     * @param startTime scheduled time to start
     */
    private void waitForSceduleTime(String startTime) {
        startTime = startTime.replace(" ","T");
        LocalDateTime startTimeZ = ZonedDateTime.parse(startTime).toLocalDateTime();
        log_.info("Scenario start-time = " + startTimeZ);
        LocalDateTime currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
        log_.info("Scenario current-time = " + currentTime);
        Duration diffTime =Duration.between(currentTime, startTimeZ);
        delayMicroSecond(diffTime.toSeconds() * 1000 * 1000);
    }

    /**
     * This method is used to send events based on counter given in scenario configuration.
     * @param events scenario generated events.
     * @param counter counter value to repeat the sequence.
     */
    private void publishEventsCounterMode(PropertyDocument events, long counter) throws SimulatorException,
            PropertyNotExpectedType {
        while (counter > 0) {
            publishEvents(events, burst_, timeDelay_, output_, zone_);
            counter--;
            log_.info("Counter : " + counter);
        }
    }

    /**
     * This method is used to send events for a duration of time to network.
     * @param events events to publish.
     * @param duration how long duration to be continued.
     */
    private void publishEventsDurationMode(PropertyDocument events,long duration) throws SimulatorException,
            PropertyNotExpectedType {
        long time = TimeUnit.MINUTES.toNanos(duration) + System.nanoTime();
        while (time > System.nanoTime())
            publishEvents(events, burst_, timeDelay_, output_, zone_);
    }

    private final Map<String, String> defaults = new HashMap<>();
    private final PropertyArray updateJpathFieldFilters_ = new PropertyArray();
    private final PropertyArray templateFieldFilters_ = new PropertyArray();

    private final DataLoader dataLoaderEngine_;
    private final ForeignFilter filter_;
    private final ForeignEventEcho echoEvent_;
    private final JsonPath jsonPath_;
    private final NetworkObject network_;
    private final Logger log_;
    private final ConfigIO jsonParser_;
    private final EventTypeTemplate eventTypeTemplate_;
    private final ForeignScenario foreignScenario_;

    private boolean burst_;

    private long publishedEvents_;
    private long timeDelay_;
    private long randomiserSeed_;

    private String output_;
    private String zone_;

    private final String TIMESTAMP_PATH = "TIMESTAMP_PATH";

    private static final String DEFAULT_COUNT = "0";
    private static final String STREAM_ID = "STREAM_ID";
    private static final String STREAM_MESSAGE = "STREAM_MESSAGE";
    private static final String MISSING_KEY = "Given key/data is null, key = ";

    private final PropertyMap STREAM_DATA = new PropertyMap();

    private enum EVENT_TYPE {
        RAS,
        SENSOR,
        BOOT
    }
    private enum BOOT_STATES {
        off,
        on,
        ready,
        all
    }
}
