package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

class ForeignScenario {

    private PropertyArray events_;
    private String scenario_;

    ForeignScenario(final Logger log) {
        log_ = log;
    }

    /**
     * This method is used to set the scenario configuration file
     * @param file abosolute path to the scenario configuration file
     * @throws SimulatorException unable to load data
     */
    void setScenarioConfigFile(String file) throws SimulatorException {
        scenarioConfigFile_ = file;
        loadScenarioConfigData();
    }

    /**
     * Thsi method is used to load data from a file
     * @throws SimulatorException unable to load data
     */
    void loadScenarioConfigData() throws SimulatorException {
        try {
            scenarioConfig_ = loadDataFromFile(scenarioConfigFile_);
            DataValidation.validateKeys(scenarioConfig_, SCENARIO_CONFIG, MISSING_SCENARIO_CONFIG);
            validateScenarioConfiguration(scenarioConfig_);
        } catch (ConfigIOParseException | IOException | PropertyNotExpectedType e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to fetch number of events to be generated for mode scenario
     * @param mode name of mode
     * @param event event name
     * @return number of events to be generated
     * @throws PropertyNotExpectedType unable to fetch data
     */
    String getTotalEvents(String mode, String event) throws PropertyNotExpectedType {
        PropertyMap scenarioModeConfig = getScenarioModeConfigData(mode);
        return scenarioModeConfig.getString(event);
    }

    String getScenarioMode() {
        return scenario_;
    }

    /**
     * This method is used to provide available types for a given event
     * @param event name of event
     * @return types of available events
     * @throws PropertyNotExpectedType unable to fetch data
     */
    PropertyArray getEventTypesInfo(String event) throws PropertyNotExpectedType {
        return scenarioConfig_.getArray(event);
    }

    /**
     * This method is used to fetch seed info for a given mode
     * @param mode name of mode
     * @return seed info
     * @throws PropertyNotExpectedType unable to fetch data
     */
    String getScenarioSeed(String mode) throws PropertyNotExpectedType {
        PropertyMap modeConfigData = getScenarioModeConfigData(mode);
        return modeConfigData.getStringOrDefault("seed", null);
    }

    PropertyArray getEventsInfo() {
        return events_;
    }

    /**
     * This method is used to fetch delay info
     * @return delay info
     * @throws PropertyNotExpectedType unable to fetch data
     */
    String getScenarioDelay() throws PropertyNotExpectedType { return scenarioConfig_.getString(SCENARIO_CONFIG[2]); }

    /**
     * This method is used to fetch config info for a given mode
     * @param mode name of mode
     * @return config info respective to mode
     * @throws PropertyNotExpectedType unable to fetch data
     */
    PropertyMap getScenarioModeConfigData(String mode) throws PropertyNotExpectedType {
        return scenarioConfig_.getMap(mode);
    }

    /**
     * This method used to generate events for burst mode scenario
     * @param events generated events data
     * @param scenario name of scenario
     * @return events array of batches
     * @throws PropertyNotExpectedType unable to create batches for events
     */
    PropertyArray generateEventsForBurstMode(PropertyMap events, String scenario) throws PropertyNotExpectedType {
        PropertyMap batchInfo = getScenarioModeConfigData(scenario);
        long rate = Long.parseLong(batchInfo.getString("rate"));
        PropertyArray pubEvents = new PropertyArray();
        for(Map.Entry<String, Object> item : events.entrySet()) {
            PropertyArray event = (PropertyArray) item.getValue();
            pubEvents.addAll(event);
        }
        return createBatchesForBurst(rate, pubEvents);
    }

    /**
     * This method used to generate events for group-burst mode scenario
     * @param events generated events data
     * @param scenario name of scenario
     * @return events array of batches
     * @throws PropertyNotExpectedType unable to create batches for events
     */
    PropertyArray generateEventsForGroupBurstMode(PropertyMap events, String scenario) throws PropertyNotExpectedType {
        return createBatchesForGroupBurst(scenario, events);
    }

    /**
     * This method is used to validate scenario config data
     * @param scenarioConfig scenario data
     * @throws SimulatorException missing field
     * @throws PropertyNotExpectedType missing field
     */
    private void validateScenarioConfiguration(PropertyMap scenarioConfig) throws SimulatorException, PropertyNotExpectedType {
        scenario_ = scenarioConfig.getString(SCENARIO_CONFIG[0]);
        if(!scenarioConfig.containsKey(scenario_))
            throw new SimulatorException("scenario configuration is missing required mode, mode = " + scenario_);

        validateScenarioMode(scenario_);

        events_ = scenarioConfig.getArray(SCENARIO_CONFIG[1]);
        for(Object event : events_) {
            if(!scenarioConfig.containsKey(event.toString()))
                throw new SimulatorException("scenario configuration is missing required event type, event type = " + event.toString());
        }
    }

    /**
     * This method is used to validate mode config data
     * @param mode mode name
     * @throws SimulatorException missing field
     * @throws PropertyNotExpectedType missing field
     */
    private void validateScenarioMode(String mode) throws SimulatorException, PropertyNotExpectedType {
        if(!scenarioConfig_.containsKey(mode))
            throw new SimulatorException("scenario configuration is missing required mode, mode = " + mode);

        PropertyMap modeConfig = scenarioConfig_.getMap(mode);

        if(SCENARIO_MODES.valueOf(mode.toUpperCase()) == SCENARIO_MODES.BURST) {
            DataValidation.validateKeys(modeConfig, BURST_CONFIG, MISSING_MODE_CONFIG);
            return;
        }
        else if(SCENARIO_MODES.valueOf(mode.toUpperCase()) == SCENARIO_MODES.GROUP_BURST) {
            return;
        }
        else if(SCENARIO_MODES.valueOf(mode.toUpperCase()) == SCENARIO_MODES.REPEAT) {
            DataValidation.validateKeys(modeConfig, REPEAT_CONFIG, MISSING_MODE_CONFIG);
            return;
        }
        throw new SimulatorException("Unknown scenario mode is specified");
    }

    /**
     * This method is used to load data from file location or resources
     * @param metadataFile file path
     * @return data in file
     * @throws IOException unable to read/load data
     * @throws ConfigIOParseException unable to reda/load data
     */
    private PropertyMap loadDataFromFile(final String metadataFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(metadataFile).getAsMap();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(metadataFile).getAsMap();
        }
    }

    /**
     * This method is used to create batches for a given rate
     * @param rate name of scenario
     * @param events events data
     * @return array containing batches with rate-count events
     */
    private PropertyArray createBatchesForBurst(long rate, PropertyArray events) {

        PropertyArray batches = new PropertyArray();
        Collections.shuffle(events);

        PropertyArray batch = new PropertyArray();
        for (Object eventObj : events) {
            PropertyMap event = (PropertyMap) eventObj;
            batch.add(event);

            if (batch.size() == rate) {
                batches.add(batch);
                batch = new PropertyArray();
            }
        }
        batches.add(batch);
        return  batches;
    }

    /**
     * This method is used to create batches for a given rate
     * @param scenario name of scenario
     * @param eventsMap events data
     * @return array containing batches with rate-count events
     * @throws PropertyNotExpectedType unable to create batches
     */
    private PropertyArray createBatchesForGroupBurst(String scenario, PropertyMap eventsMap) throws PropertyNotExpectedType {
        PropertyMap groupBurstInfo = getScenarioModeConfigData(scenario);
        PropertyArray mainBatch = new PropertyArray();

        for(Map.Entry<String, Object> eventObj : eventsMap.entrySet()) {
            String eventName = eventObj.getKey();
            PropertyArray events = (PropertyArray) eventObj.getValue();
            long rate = Long.parseLong(groupBurstInfo.getString(GROUP_BURST_CONFIG + eventName));
            PropertyArray burstBatch = createBatchesForBurst(rate, events);
            mergeArrays(mainBatch, burstBatch);
        }
        return mainBatch;
    }

    /**
     * This method is used to merge two arrays
     * @param mainBatch array1
     * @param burstBatch array2
     * @throws PropertyNotExpectedType unable to merge arrays
     */
    private void mergeArrays(PropertyArray mainBatch, PropertyArray burstBatch) throws PropertyNotExpectedType {
        if(mainBatch.isEmpty()) {
            mainBatch.addAll(burstBatch);
            return;
        }

        int index = 0;
        for(; index < mainBatch.size() && index < burstBatch.size(); index++) {
            PropertyArray mainBatchPart = mainBatch.getArray(index);
            PropertyArray burstBatchPart = burstBatch.getArray(index);
            mainBatchPart.addAll(burstBatchPart);
        }

        for(; index < burstBatch.size(); index++) {
            PropertyArray burstBatchPart = burstBatch.getArray(index);
            mainBatch.add(burstBatchPart);
        }
    }

    private PropertyMap scenarioConfig_;

    private String scenarioConfigFile_;

    private final Logger log_;

    private final String[] SCENARIO_CONFIG = {"mode", "events", "time-delay-mus"};
    private final String[] BURST_CONFIG = {"batch-rate"};
    private final String[] REPEAT_CONFIG = {"mode", "clock-mode", "duration", "counter", "start-time"};

    private final String GROUP_BURST_CONFIG = "group-batch-";
    private final String MISSING_SCENARIO_CONFIG = "scenario configuration is missing required field, field = ";
    private final String MISSING_MODE_CONFIG = "scenario configuration is missing required mode-field configuration, field = ";

    private enum SCENARIO_MODES {
        BURST,
        GROUP_BURST,
        REPEAT
    }
}