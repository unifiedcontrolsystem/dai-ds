package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
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
import java.util.Random;
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
        eventsConfigTemplate_ = new EventTypeTemplate(log_);
        filter_ = new ForeignFilter(jsonPath_, log_);
        foreignScenario_ = new ForeignScenario(log_);
        echoEvent_ = new ForeignEventEcho();
    }

    void initialize() {
        log_.info("SYSTEM HEIRARCHY");
        systemHierarchy();
    }

    /**
     * This method is used to generate boot events
     * @param parameters data needed to generate boot events
     * @throws SimulatorException unable to generate boot events
     */
    void generateBootEvents(Map<String, String> parameters) throws SimulatorException {
        DataValidation.isNull(parameters, NULL_OBJ_ERR_MSG);
        parameters.put("count", "1"); //boot-event of specified type is always 1

        try {
            PropertyArray events = new PropertyArray();
            String state = parameters.getOrDefault(EVENT_TYPE, BOOT_STATES.all.toString());
            if(BOOT_STATES.valueOf(state).equals(BOOT_STATES.all)) {
                parameters.put(EVENT_TYPE, BOOT_STATES.off.toString());
                events.addAll(generateEvents(parameters, parameters.get(EVENT_TYPE).toLowerCase()).getAsArray());
                parameters.put(EVENT_TYPE, BOOT_STATES.on.toString());
                events.addAll(generateEvents(parameters, parameters.get(EVENT_TYPE).toLowerCase()).getAsArray());
                parameters.put(EVENT_TYPE, BOOT_STATES.ready.toString());
            }
            events.addAll(generateEvents(parameters, parameters.get(EVENT_TYPE).toLowerCase()).getAsArray());
            PropertyArray xnameLocations_ = filter_.getFilteredLocations();
            for(int index = 0; index < events.size(); index++) {
                PropertyMap event = events.getMap(index).getMap("STREAM_MESSAGE");
                String message = event.getStringOrDefault("message", "");
                Random random = new Random();
                random.setSeed(randomiserSeed_);
                int randomIndex = (int) ((random.nextDouble() * (xnameLocations_.size() - 0)) + 0);
                String timestamp = ZonedDateTime.now(ZoneId.of(zone_)).toInstant().toString() + " ";
                String xnameLocation = xnameLocations_.getString(randomIndex);
                String updatedMessage =timestamp + "aus-admin1 twistd: clmgr-power:" +
                        dataLoaderEngine_.getHostname(CommonFunctions.convertForeignToLocation(xnameLocation)) + message;
                event.put("message", updatedMessage);
            }
            PropertyArray publishEvents = new PropertyArray();
            publishEvents.add(events);
            publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void generateRasEvents(Map<String, String> parameters) throws SimulatorException {
        try {
            DataValidation.validateKey(parameters, EVENT_TYPE, MISSING_KEY);
            PropertyArray events = generateEvents(parameters, parameters.get(EVENT_TYPE).toLowerCase()).getAsArray();
            PropertyArray publishEvents = new PropertyArray();
            publishEvents.add(events);
            publishEvents(publishEvents, burst_, timeDelay_, output_, zone_);
        } catch (Exception e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    void generateSensorEvents(Map<String, String> parameters) throws SimulatorException {
        try {
            PropertyArray events = generateEvents(parameters, parameters.get(EVENT_TYPE).toLowerCase()).getAsArray();
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

            String scenarioMode = parameters.getOrDefault(parameters.get(EVENT_TYPE).toLowerCase(), foreignScenario_.getScenarioMode());
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
                    String streamTopic = eventInfo.getString(STREAM_TOPIC);
                    String timestampJPath = eventInfo.getString(TIMESTAMP_PATH);
                    PropertyMap event = eventInfo.getMap(STREAM_MESSAGE);
                    publishedEvents_ += jsonPath_.setTime(timestampJPath, event.getAsMap(), zone);
                    network_.setProperty(STREAM_ID, streamId);
                    network_.send(streamTopic, jsonParser_.toString(event));
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
        DataValidation.isValueNullOrEmpty(parameters, MISSING_KEY);
        validateLocations(parameters.getOrDefault(LOCATIONS, DEFAULT_LOCATIONS));
        loadDefaults(parameters);

        String eventsConfigTempFile = defaults.get(EVENTS_CONFIG_TEMPLATE);
        eventsConfigTemplate_.setEventsConfigTemplateFile(eventsConfigTempFile, type);
        if(!updateJpathFieldFilters_.isEmpty())
            eventsConfigTemplate_.setJPathFieldFiltersConfig(updateJpathFieldFilters_);


        timeDelay_ = Long.parseLong(defaults.get("time-delay-mus"));
        burst_ = Boolean.parseBoolean(defaults.get("burst"));
        output_ = defaults.get("output");
        zone_ = defaults.get("timezone");

        long numOfEventsToGenerate = Long.parseLong(defaults.getOrDefault("count", DEFAULT_COUNT));
        randomiserSeed_ = Long.parseLong(defaults.get("seed"));

        PropertyArray events =  filter_.generateEvents(eventsConfigTemplate_, numOfEventsToGenerate, randomiserSeed_).getAsArray();

        String streamId = eventsConfigTemplate_.getEventTypeStreamId();
        String streamName = eventsConfigTemplate_.getEventTypeStreamName();
        String jpathToTimestamp = eventsConfigTemplate_.getPathToUpdateTimestamp();

        STREAM_DATA.put(STREAM_ID, streamId);
        STREAM_DATA.put(STREAM_TOPIC, streamName);
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
        defaults.put(EVENTS_CONFIG_TEMPLATE, parameters.getOrDefault(EVENTS_CONFIG_TEMPLATE,
                dataLoaderEngine_.getEventsConfiguration(EVENTS_CONFIG_TEMPLATE, null)));
        defaults.put(BURST, parameters.getOrDefault(BURST,
                dataLoaderEngine_.getEventsConfiguration(BURST, DEFAULT_BURST)));
        defaults.put(COUNT, parameters.getOrDefault(COUNT,
                dataLoaderEngine_.getEventsConfiguration(COUNT, DEFAULT_COUNT)));
        defaults.put(SEED, parameters.getOrDefault(SEED,
                dataLoaderEngine_.getEventsConfiguration(SEED, String.valueOf(System.nanoTime()))));
        defaults.put(TIME_DELAY_MUS, parameters.getOrDefault(TIME_DELAY_MUS,
                dataLoaderEngine_.getEventsConfiguration(TIME_DELAY_MUS, DEFAULT_TIME_DELAY_MUS)));
        defaults.put(BOOT_FAILURE_PROB, parameters.getOrDefault(BOOT_FAILURE_PROB,
                dataLoaderEngine_.getEventsConfiguration(BOOT_FAILURE_PROB, DEFAULT_BOOT_FAILURE_PROB)));
        defaults.put(EVENT_DATA_TEMP, parameters.getOrDefault(EVENT_DATA_TEMP, null));
        defaults.put(TIMEZONE, parameters.getOrDefault(TIMEZONE,
                dataLoaderEngine_.getEventsConfiguration(TIMEZONE, ZoneId.systemDefault().toString())));

        STREAM_DATA.put(STREAM_ID, "");
        STREAM_DATA.put(STREAM_TOPIC, "");
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

    private void validateLocations(String locationsRegex) throws ConversionException, SimulatorException {
        filter_.validateLocations(dataLoaderEngine_.getNodeLocations(), locationsRegex);
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
    private final EventTypeTemplate eventsConfigTemplate_;
    private final ForeignScenario foreignScenario_;

    private boolean burst_;

    private long publishedEvents_;
    private long timeDelay_;
    private long randomiserSeed_;

    private String output_;
    private String zone_;

    private final String TIMESTAMP_PATH = "TIMESTAMP_PATH";

    private static final String EVENTS_CONFIG_TEMPLATE = "events-config-template";
    private static final String EVENT_TYPE = "type";
    private static final String BURST = "burst";
    private static final String COUNT = "count";
    private static final String SEED = "seed";
    private static final String TIME_DELAY_MUS = "time-delay-mus";
    private static final String BOOT_FAILURE_PROB = "boot-failure-prob";
    private static final String EVENT_DATA_TEMP = "event-data-template";
    private static final String TIMEZONE = "timezone";
    private static final String LOCATIONS = "locations";

    private static final String DEFAULT_COUNT = "0";
    private static final String DEFAULT_BURST = "true";
    private static final String DEFAULT_BOOT_FAILURE_PROB = "0";
    private static final String DEFAULT_TIME_DELAY_MUS = "0";
    private static final String DEFAULT_LOCATIONS = ".*";

    private static final String STREAM_ID = "STREAM_ID";
    private static final String STREAM_TOPIC = "STREAM_TOPIC";
    private static final String STREAM_MESSAGE = "STREAM_MESSAGE";
    private static final String MISSING_KEY = "given key/data is null, key = ";
    private static final String NULL_OBJ_ERR_MSG = "given data is null";

    private final PropertyMap STREAM_DATA = new PropertyMap();

    private enum BOOT_STATES {
        off,
        on,
        ready,
        all
    }
}
