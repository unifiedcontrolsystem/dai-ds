package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.sun.istack.NotNull;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Description of class SimulatorEngine.
 * process the input data request to generate respective events like ras sensor and boot.
 * creates respective events like ras sensor and boot.
 */
public class SimulatorEngine {

    SimulatorEngine(DataLoader dataLoaderEngine, NetworkObject source, Logger log) {
        dataLoaderEngine_ = dataLoaderEngine;
        source_ = source;
        log_ = log;
        system_ = new SystemGenerator(log);
        scenario_ = new Scenario();
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
     * This method is used to create and send boot off,on,ready events to network.
     * @param regexLocation regex of locations.
     * @param bootFailureProbability probability that booting nodes can be failed
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data
     * @param output store generated events in a file
     * @throws SimulatorException unable to create boot off/on/ready events
     */
    void publishBootEvents(@NotNull final String regexLocation, @NotNull final String bootFailureProbability, @NotNull final String burst, final String timeDelayMus, final String randomiserSeed, final String output) throws SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocation);
        parameters.put("boot-failure-prob", bootFailureProbability);
        parameters.put("burst", burst);

        try {
            validateParameters(parameters);
            loadDefaults();
            boolean burstMode = Boolean.parseBoolean(burst);
            float bfprobability = Float.parseFloat(bootFailureProbability);
            if(!(0 <= bfprobability && bfprobability <= 1))
                throw new SimulatorException("'bootFailureProbability' value ranges from 0-1");
            if (timeDelayMus != null)
                timeDelayMus_ = Long.parseLong(timeDelayMus);
            if (randomiserSeed != null)
                randomiserSeed_ = Long.parseLong(randomiserSeed);
            if (ExistsLocationsMatchedRegex(regexLocation, EVENT_TYPE.BOOT)) {
                List<ForeignEvent> bootEvents = system_.publishBootEventsForLocation(bfprobability, -1);
                //source_.send(ForeignEvent.EVENT_SUB_TYPE.stateChanges.toString(), bootEvents, constantMode_, timeDelayMus_);
                Map<Long, List<ForeignEvent>> events = new HashMap<>();
                events.put(1L, bootEvents);
                publishGeneratedEvents(events, burstMode, output);
            }
        } catch (final ConversionException e) {
            throw new SimulatorException(e.getMessage());
        }
    }


    /**
     * This method is used to create and send boot off,on,ready events to network.
     * @param regexLocation regex of locations.
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data
     * @param output store generated events in a file
     * @throws SimulatorException unable to create boot off/on/ready events
     */
    void publishBootOffEvents(@NotNull final String regexLocation, @NotNull final String burst, final String timeDelayMus, final String randomiserSeed, final String output) throws SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocation);
        parameters.put("burst", burst);

        try {
            validateParameters(parameters);
            loadDefaults();
            boolean burstMode = Boolean.parseBoolean(burst);
            if (timeDelayMus != null)
                timeDelayMus_ = Long.parseLong(timeDelayMus);
            if (randomiserSeed != null)
                randomiserSeed_ = Long.parseLong(randomiserSeed);
            if (ExistsLocationsMatchedRegex(regexLocation, EVENT_TYPE.BOOT)) {
                List<ForeignEvent> bootEvents = system_.publishBootOffEventsForLocation(-1);
                //source_.send(ForeignEvent.EVENT_SUB_TYPE.stateChanges.toString(), bootEvents, constantMode_, timeDelayMus_);
                Map<Long, List<ForeignEvent>> events = new HashMap<>();
                events.put(1L, bootEvents);
                publishGeneratedEvents(events, burstMode, output);
            }
        } catch (final ConversionException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send boot off,on,ready events to network.
     * @param regexLocation regex of locations.
     * @param bootFailureProbability probability that booting nodes can be failed
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data
     * @param output store generated events in a file
     * @throws SimulatorException unable to create boot off/on/ready events
     */
    void publishBootOnEvents(@NotNull final String regexLocation, final String bootFailureProbability, @NotNull final String burst, final String timeDelayMus, final String randomiserSeed, final String output) throws SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocation);
        parameters.put("boot-failure-prob", bootFailureProbability);
        parameters.put("burst", burst);

        try {
            validateParameters(parameters);
            loadDefaults();
            boolean burstMode = Boolean.parseBoolean(burst);
            float bfprobability = Float.parseFloat(bootFailureProbability);
            if(!(0 <= bfprobability && bfprobability <= 1))
                throw new SimulatorException("'bootFailureProbability' value ranges from 0-1");
            if (timeDelayMus != null)
                timeDelayMus_ = Long.parseLong(timeDelayMus);
            if (randomiserSeed != null)
                randomiserSeed_ = Long.parseLong(randomiserSeed);
            if (ExistsLocationsMatchedRegex(regexLocation, EVENT_TYPE.BOOT)) {
                List<ForeignEvent> bootEvents = system_.publishBootOnEventsForLocation(bfprobability, -1);
                //source_.send(ForeignEvent.EVENT_SUB_TYPE.stateChanges.toString(), bootEvents, constantMode_, timeDelayMus_);
                Map<Long, List<ForeignEvent>> events = new HashMap<>();
                events.put(1L, bootEvents);
                publishGeneratedEvents(events, burstMode, output);
            }
        } catch (final ConversionException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send boot off,on,ready events to network.
     * @param regexLocation regex of locations.
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data
     * @param output store generated events in a file
     * @throws SimulatorException unable to create boot off/on/ready events
     */
    void publishBootReadyEvents(@NotNull final String regexLocation, @NotNull final String burst, final String timeDelayMus, final String randomiserSeed, final String output) throws SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocation);
        parameters.put("burst", burst);

        try {
            validateParameters(parameters);
            loadDefaults();
            boolean burstMode = Boolean.parseBoolean(burst);
            if (timeDelayMus != null)
                timeDelayMus_ = Long.parseLong(timeDelayMus);
            if (randomiserSeed != null)
                randomiserSeed_ = Long.parseLong(randomiserSeed);
            if (ExistsLocationsMatchedRegex(regexLocation, EVENT_TYPE.BOOT)) {
                List<ForeignEvent> bootEvents = system_.publishBootReadyEventsForLocation(-1);
                //source_.send(ForeignEvent.EVENT_SUB_TYPE.stateChanges.toString(), bootEvents, constantMode_, timeDelayMus_);
                Map<Long, List<ForeignEvent>> events = new HashMap<>();
                events.put(1L, bootEvents);
                publishGeneratedEvents(events, burstMode, output);
            }
        } catch (final ConversionException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send sensor events to network
     * @param regexLocation regex of locations
     * @param regexLabel regex for event type/description
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data.
     * @param numOfEvents number of sensor events to generate
     * @param output store generated events in a file
     * @throws SimulatorException unable to create sensor event
     */
    void publishSensorEvents(@NotNull final String regexLocation, @NotNull final String regexLabel, @NotNull final String burst, final String timeDelayMus, final String randomiserSeed, final String numOfEvents, final String output) throws SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocation);
        parameters.put("label-regex", regexLabel);
        parameters.put("burst", burst);

        try {
            validateParameters(parameters);
            loadDefaults();
            boolean burstMode = Boolean.parseBoolean(burst);
            if (timeDelayMus != null)
                timeDelayMus_ = Long.parseLong(timeDelayMus);
            if (randomiserSeed != null)
                randomiserSeed_ = Long.parseLong(randomiserSeed);
            if (numOfEvents != null)
                numOfEvents_ = Long.parseLong(numOfEvents);
            if (ExistsLocationsMatchedRegex(regexLocation, EVENT_TYPE.SENSOR) && ExistsMatchedRegexLabel(regexLabel, EVENT_TYPE.SENSOR)) {
                List<ForeignEvent> sensorEvents = system_.publishSensorEventsForLocation(numOfEvents_, randomiserSeed_);
                Map<Long, List<ForeignEvent>> events = new HashMap<>();
                events.put(1L, sensorEvents);
                publishGeneratedEvents(events, burstMode, output);
            }
        } catch (final PropertyNotExpectedType | ConversionException | IOException | ConfigIOParseException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send job events to network
     * @param regexLocation regex of locations
     * @param regexLabel regex for event type/description
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data.
     * @param numOfEvents number of sensor events to generate
     * @param output store generated events in a file
     * @throws SimulatorException unable to create job event
     */
    void publishJobEvents(@NotNull final String regexLocation, @NotNull final String regexLabel, @NotNull final String burst, final String timeDelayMus, final String randomiserSeed, final String numOfEvents, final String output) throws SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocation);
        parameters.put("label-regex", regexLabel);
        parameters.put("burst", burst);

        try {
            validateParameters(parameters);
            loadDefaults();
            boolean burstMode = Boolean.parseBoolean(burst);
            if (timeDelayMus != null)
                timeDelayMus_ = Long.parseLong(timeDelayMus);
            if (randomiserSeed != null)
                randomiserSeed_ = Long.parseLong(randomiserSeed);
            if (numOfEvents != null)
                numOfEvents_ = Long.parseLong(numOfEvents);
            if (ExistsLocationsMatchedRegex(regexLocation, EVENT_TYPE.JOB) && ExistsMatchedRegexLabel(regexLabel, EVENT_TYPE.JOB)) {
                List<ForeignEvent> jobEvents = system_.publishJobEventsForLocation(numOfEvents_, randomiserSeed_);
                Map<Long, List<ForeignEvent>> events = new HashMap<>();
                events.put(1L, jobEvents);
                publishGeneratedEvents(events, burstMode, output);
            }
        } catch (final PropertyNotExpectedType | ConversionException | IOException | ConfigIOParseException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to create and send events to network for a scenario given in a file.
     * @param scenarioFile scenario configuration data.
     * @param type burst/group-burst/repeat
     * @param regexLocations regex of locations
     * @param regexRasLabel regex for ras event type/description
     * @param regexSensorLabel regex for sensor event type/description
     * @param bootFailureProbability probability that booting nodes can be failed
     * @param burst true for burst mode, false for constant mode
     * @param timeDelayMus time delay to induce while sending events to network
     * @param randomiserSeed randomization seed to replicate data.
     * @param counter type = repeat, re-run counter times
     * @param duration type = repeat, run till duration expires
     * @param startTime type = repeat, start time, end time can be counter/duration
     * @param output store generated events in a file
     * @throws SimulatorException unable to generate event for a given scenario
     */
    void publishEventsForScenario(@NotNull final String scenarioFile, String type, @NotNull final String regexLocations,
                                         @NotNull final String regexRasLabel, @NotNull final String regexSensorLabel, @NotNull final String bootFailureProbability,
                                         @NotNull String burst, final String timeDelayMus, final String randomiserSeed, String counter, String duration, String startTime, String output) throws  SimulatorException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location-regex", regexLocations);
        parameters.put("ras-label-regex", regexLocations);
        parameters.put("sensor-label-regex", regexLocations);
        parameters.put("burst", burst);
        parameters.put("scenario-file",scenarioFile);
        parameters.put("boot-failure-probability", bootFailureProbability);

        try {
            validateParameters(parameters);
            boolean burstMode = Boolean.parseBoolean(burst);
            float bfprobability = Float.parseFloat(bootFailureProbability);
            if(!(0 <= bfprobability && bfprobability <= 1))
                throw new SimulatorException("'bootFailureProbability' value ranges from 0-1");

            ExistsLocationsMatchedRegex(regexLocations, EVENT_TYPE.OTHER);
            ExistsMatchedRegexLabel(regexRasLabel, EVENT_TYPE.RAS);
            ExistsMatchedRegexLabel(regexSensorLabel, EVENT_TYPE.SENSOR);

            scenario_.setScenarioToGenerateEventsPath(scenarioFile);
            scenario_.processEventsScenarioFile();
            if(type == null)
                type = scenario_.getScenarioMode();
            timeDelayMus_ = ((timeDelayMus == null) ?
                    Long.parseLong(scenario_.getScenarioDelay()) : Long.parseLong(timeDelayMus));

            PropertyMap scenarioTypeData = scenario_.getScenarioModeData(type);
            String clockMode = scenarioTypeData.getString("clock-mode");
            String counter_ = scenario_.getScenarioRepeatModeCounter();
            String duration_ = scenario_.getScenarioRepeatModeDuration();
            String startTime_ = scenario_.getScenarioRepeatModeStartTime();

            if (counter != null) {
                counter_ = counter;
                clockMode = "counter";
            }

            if(duration != null) {
                duration_ = duration;
                clockMode = "duration";
            }
            if(startTime != null) {
                startTime_ = startTime;
                clockMode = "start-time";
            }

            Map<Long, List<ForeignEvent>> events;
            switch (type) {
                case "burst":
                    randomiserSeed_ = ((randomiserSeed == null) ?
                            Long.parseLong(scenario_.getScenarioSeed(type)) : Long.parseLong(randomiserSeed));
                    scenarioTypeData.put("boot-prob-failure", bfprobability);
                    events = system_.generateBurstScenario(scenarioTypeData);
                    publishGeneratedEvents(events, burstMode, output);
                    break;
                case "group-burst":
                    randomiserSeed_ = ((randomiserSeed == null) ?
                            Long.parseLong(scenario_.getScenarioSeed(type)) : Long.parseLong(randomiserSeed));
                    scenarioTypeData.put("boot-prob-failure", bfprobability);
                    events = system_.generateGroupBurstScenarioEvents(scenarioTypeData);
                    publishGeneratedEvents(events, burstMode, output);
                    break;
                default:
                    String repeatMode = scenarioTypeData.getString("mode");
                    randomiserSeed_ = ((randomiserSeed == null) ?
                            Long.parseLong(scenario_.getScenarioSeed(type)) : Long.parseLong(randomiserSeed));
                    PropertyMap data = scenario_.getScenarioModeData(repeatMode);
                    data.put("boot-prob-failure", bfprobability);
                    events = system_.generateRepeatScenarioEvents(repeatMode, data);
                    publishRepeatScenarioGeneratedEvents(events, burstMode, clockMode, counter_, duration_, startTime_, output);
                    break;
            }

        } catch (PropertyNotExpectedType | ConversionException | IOException | ConfigIOParseException e) {
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
     * This method is used to return number of events generated and sent to network.
     * @return generated events
     */
    long getPublishedEventsCount() { return publishedEvents_.size(); }

    /**
     * This method is used to send repeat scenario generated events.
     * @param events scenario generated events
     * @param burst true for burst mode, false for constant mode
     * @param clockMode to choose counter/duration/start-time
     * @param counter type = repeat, re-run counter times
     * @param duration type = repeat, run till duration expires
     * @param startTime type = repeat, start time, end time can be counter/duration
     * @param output store generated events in a file
     */
    private void publishRepeatScenarioGeneratedEvents(Map<Long, List<ForeignEvent>> events, boolean burst, String clockMode, String counter, String duration, String startTime, String output) throws SimulatorException, PropertyNotExpectedType {
        if(!startTime.isEmpty()) {
            startTime = startTime.replace(" ","T");
            LocalDateTime startTimeZ = ZonedDateTime.parse(startTime).toLocalDateTime();
            LocalDateTime currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDateTime();
            Duration diffTime =Duration.between(currentTime, startTimeZ);
            delayMicroSecond(diffTime.toSeconds() * 1000 * 1000);
        }

        if(clockMode.equals("start-time")) {
            PropertyMap scenarioTypeData = scenario_.getScenarioModeData("repeat");
            clockMode = scenarioTypeData.getString("clock-mode");
        }
        if(clockMode.equals("counter")) {
            long counterL = Long.parseLong(counter);
            counterModeGeneratedEvents(events, counterL, burst, output);
        }
        else if(clockMode.equals("duration")) {
            long durationL = Long.parseLong(duration);
            durationModeGeneratedEvents(events, durationL, burst, output);
        }
    }

    /**
     * This method is used to send events based on duration of time to network.
     * @param events scenario generated events.
     * @param duration how long sequence to be continued.
     * @param burst true for burst mode, false for constant mode
     */
    private void durationModeGeneratedEvents(Map<Long, List<ForeignEvent>> events,long duration, boolean burst, String output) {
        long time = TimeUnit.MINUTES.toNanos(duration) + System.nanoTime();
        while (time > System.nanoTime())
            publishGeneratedEvents(events, burst, output);
    }

    /**
     * This method is used to send events based on counter given in scenario configuration.
     * @param events scenario generated events.
     * @param counter counter value to repeat the sequence.
     * @param burst true for burst mode, false for constant mode
     */
    private void counterModeGeneratedEvents(Map<Long, List<ForeignEvent>> events, long counter, boolean burst, String ouput) {
        while (counter > 0) {
            publishGeneratedEvents(events, burst, ouput);
            counter--;
            System.out.println("Counter : " + counter);
        }
    }

    /**
     * This method is used to send generated events to network.
     * @param events send generated events.
     * @param burstMode true without delay, false with delay
     * @param output store generated events in a file
     */
    private void publishGeneratedEvents(Map<Long, List<ForeignEvent>> events, boolean burstMode, String output) {
        publishedEvents_ .clear();
        events_.clear();
        long droppedEvents = 0;
        ZonedDateTime startTime1 = ZonedDateTime.now(ZoneId.systemDefault());
        System.out.println("Start Time : " + startTime1.toString());
        for(List<ForeignEvent> event : events.values()) {
            ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.debug("Rate/Group Start Time : " + startTime.toString());
            events_.clear();
            loadDefaultsMap(events_);
            for(ForeignEvent item: event) {
                try {
                    String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toString().replace("T", " ");
                    timestamp = ",\"timestamp\":\"" + timestamp + "\"}";
                    String subject = item.subject.toString();
                    item.message = item.message.replace("}", timestamp);
                    source_.send(subject, item.message);
                    events_.put(subject, events_.get(subject) + 1);
                    publishedEvents_.add(item.message);
                    if(!burstMode)
                        delayMicroSecond(timeDelayMus_);
                } catch (RESTClientException e) {
                    e.printStackTrace();
                    droppedEvents++;
                }
            }
            ZonedDateTime endTime = ZonedDateTime.now(ZoneId.systemDefault());
            log_.debug("Rate/Group End Time : " + endTime.toString());
            long sum = 0;
            for(ForeignEvent.EVENT_SUB_TYPE eventType : ForeignEvent.EVENT_SUB_TYPE.values()) {
                String key = eventType.toString();
                long value = events_.get(key);
                log_.debug(key + " events = " + String.valueOf(value));
                sum = sum + value;
            }
            System.out.println("Rate/Group Published events : " + sum);
            delayMicroSecond(timeDelayMus_);
        }
        ZonedDateTime endTime1 = ZonedDateTime.now(ZoneId.systemDefault());
        log_.debug("End Time : " + endTime1.toString());
        log_.debug("Total Published events : " + publishedEvents_.size());
        log_.debug("Dropped events : " + droppedEvents);
        log_.debug("Total Time Main Start to Main End :" + (Duration.between(startTime1,endTime1).toSeconds()) + " seconds");
        if(output != null) {
            try {
                scenario_.writeEventToFile(publishedEvents_, output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method is used to create a constant delay.
     * @param delayTimeMus time delay in microseconds.
     */
    private void delayMicroSecond(final long delayTimeMus) {
        long waitUntil = System.nanoTime() + (delayTimeMus * 1000);
        while(waitUntil > System.nanoTime()) {;}
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
                throw new SimulatorException("No Matched Regex Locations to generate Boot Events.");
            else if (eventType.equals(EVENT_TYPE.RAS))
                throw new SimulatorException("No Matched Regex Locations to generate RAS Events.");
            else if (eventType.equals(EVENT_TYPE.SENSOR))
                throw new SimulatorException("No Matched Regex Locations to generate Sensor Events.");
            else if (eventType.equals(EVENT_TYPE.JOB))
                throw new SimulatorException("No Matched Regex Locations to generate Job Events.");
            throw new SimulatorException("No Matched Regex Locations to generate Scenario sequence.");
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
    private boolean ExistsMatchedRegexLabel(final String regexLabel, final EVENT_TYPE eventType) throws PropertyNotExpectedType, SimulatorException, IOException, ConfigIOParseException {
        if (system_.getMatchedRegexLabels(regexLabel, eventType) == 0) {
            if (eventType.equals(EVENT_TYPE.RAS))
                throw new SimulatorException("No Matched Regex labels to generate RAS Events.");
            else if (eventType.equals(EVENT_TYPE.SENSOR))
                throw new SimulatorException("No Matched Regex labels to generate Sensor Events.");
            else if (eventType.equals(EVENT_TYPE.JOB))
                throw new SimulatorException("No Matched Regex labels to generate Job Events.");
        }
        return true;
    }

    /**
     * This method is used to load location data from db.
     */
    private void loadData() throws SimulatorException {
       dataLoaderEngine_.initialize();
    }

    /**
     * This method is used to load default values from configuration file when respective parameters
     * are not passed through request.
     */
    private void loadDefaults() {
        numOfEvents_ = Long.parseLong(dataLoaderEngine_.getEventsConfigutaion("count", "0"));
        timeDelayMus_ = Long.parseLong(dataLoaderEngine_.getEventsConfigutaion("time-delay-mus", "0"));
        randomiserSeed_ = Long.parseLong(dataLoaderEngine_.getEventsConfigutaion("seed", "0"));
    }

    /**
     * This method is used to display system location details.
     */
    private void systemHierarchy() {
        for (String location : dataLoaderEngine_.getNodeLocations())
            System.out.println(location.toUpperCase());
    }

    /**
     * This method is used to validate null inputs
     * @throws SimulatorException if input data is null
     */
    private void validateParameters(Map<String, String> parameters) throws SimulatorException {
        for(Map.Entry<String,String> entry : parameters.entrySet()) {
            if(entry.getValue() == null)
                throw new SimulatorException("'" + entry.getKey() + "' cannot be null/empty values.");
        }
    }

    private void loadDefaultsMap(Map<String, Long> defaultMap) {
        for(ForeignEvent.EVENT_SUB_TYPE eventType : ForeignEvent.EVENT_SUB_TYPE.values())
            defaultMap.put(eventType.toString(), 0L);
    }

    enum EVENT_TYPE {
        BOOT,
        RAS,
        SENSOR,
        JOB,
        OTHER;
    }

    private final DataLoader dataLoaderEngine_;
    private final NetworkObject source_;
    private final SystemGenerator system_;
    private final Scenario scenario_;
    private long numOfEvents_;
    private long timeDelayMus_;
    private long randomiserSeed_;
    private List<String> publishedEvents_ = new ArrayList<>();
    private final Logger log_;
    private Map<String, Long> events_ = new HashMap<>();
/*    private List<String> publishedEvents1_ = new ArrayList<>();*/
}