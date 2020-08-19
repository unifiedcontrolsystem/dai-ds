package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import javax.validation.constraints.NotEmpty;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class Scenario {

    /**
     * This method used to set scenario configuration file to generate events.
     * @param scenarioFile scenario to generate events configuration file.
     * @throws SimulatorException when unable to set the location of scenario configuration file.
     */
    void setScenarioToGenerateEventsPath(@NotNull final String scenarioFile) throws SimulatorException {
        if (scenarioFile == null)
            throw new SimulatorException("scenario file location data is null.");
        scenarioFile_ = scenarioFile;
    }

    void processEventsScenarioFile() throws SimulatorException {
        if(scenarioFile_ == null)
            throw new SimulatorException("set scenario file location before processing data.");
        try {
            validateScenarioFileData(readConfigFile(scenarioFile_));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("scenario to generate events configuration file doesn't exist: " + scenarioFile_);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error while loading scenario to generate events configuration file data");
        }
    }

    /**
     * This method is used to get counter value for repeat scenario
     * @return counter value
     */
    @NotEmpty(message = "'counter' parameter is empty")
    String getScenarioRepeatModeCounter() {
        PropertyMap repeatData = getScenarioModeData("repeat");
        return repeatData.getStringOrDefault("counter", "0");
    }

    /**
     * This method is used to get duration value for repeat scenario
     * @return duration value
     */
    @NotEmpty(message = "'duration' parameter is empty")
    String getScenarioRepeatModeDuration() {
        PropertyMap repeatData = getScenarioModeData("repeat");
        return repeatData.getStringOrDefault("duration", "0");
    }

    /**
     * This method is used to get start time value for repeat scenario
     * @return start time value
     */
    @NotNull
    String getScenarioRepeatModeStartTime() {
        PropertyMap repeatData = getScenarioModeData("repeat");
        return repeatData.getStringOrDefault("start-time", Instant.now().toString());
    }

    /**
     * This method is used to fetch scenario mode.
     * @return scenario mode data.
     */
    @NotNull
    String getScenarioMode() {
        return mode_;
    }

    /**
     * This method is used to return delay to be induced between events/groups
     * @return delay amount in nanoseconds
     */
    @NotNull
    String getScenarioDelay() { return timeDelayMus_; }

    /**
     * This method is used to get seed used to generate data for a given scenario.
     * @return seed value
     */
    @NotEmpty(message = "'seed' parameter is empty")
    String getScenarioSeed(String mode) {
        PropertyMap repeatData = getScenarioModeData(mode);
        return repeatData.getStringOrDefault("seed", String.valueOf(System.nanoTime()));
    }

    /**
     * This method is used to return scenario data for any given mode.
     * @param mode scenario mode
     * @return scenario mode data.
     */
    @NotEmpty(message = "given scenario mode data is empty")
    PropertyMap getScenarioModeData(String mode) {
        return scenarioConfiguration_.getMapOrDefault(mode, null);
    }

    void writeEventToFile(List<String> values, String file) throws IOException {
        PropertyArray data = new PropertyArray();
        data.addAll(values);
        LoadFileLocation.writeFile(data, file, true);
    }

    /**
     * This method is used to validate scenario to generate events configuration file data.
     * @param scenarioConfiguration scenario data to generate events.
     */
    private void validateScenarioFileData(PropertyDocument scenarioConfiguration) throws SimulatorException {
        if(scenarioConfiguration == null || !scenarioConfiguration.isMap() || scenarioConfiguration.getAsMap().size() == 0)
            throw new SimulatorException("Scenario configuration data is empty/null.");
        scenarioConfiguration_ = scenarioConfiguration.getAsMap();


        for(String key : SCENARIO_KEYS) {
            if(!scenarioConfiguration_.containsKey(key))
                throw new SimulatorException("'" + key + "' parameter is missing in scenario configuration file.");
            validateScenarioActions(scenarioConfiguration_.getMapOrDefault(key, null), key);
        }

        mode_ = scenarioConfiguration_.getStringOrDefault("mode", null);
        timeDelayMus_ = scenarioConfiguration_.getStringOrDefault("delay", null);
    }

    /**
     * This method is used to validate respective scenario actions data.
     * @param modeData scenario data to generate events.
     * @param action name of scenario.
     */
    private void validateScenarioActions(PropertyMap modeData, String action) throws SimulatorException {
        switch (action) {
            case "burst":
                validateScenarioData(action, BURST_KEYS, modeData);
                break;
            case "group-burst":
                validateScenarioData(action, GROUP_BURST_KEYS, modeData);
                break;
            case "repeat":
                validateScenarioData(action, REPEAT_KEYS, modeData);
                break;
            default:
                break;
        }
    }


    /**
     * This method is used to validate group burst scenario data.
     * @param actionData group burst scenario data.
     */
    private void validateScenarioData(String type, String[] keys, PropertyMap actionData) throws SimulatorException {
        for(String key : keys) {
            if (!actionData.containsKey(key))
                throw new SimulatorException("'" + key + "' parameter is missing in " + type + " action scenario data.");
        }
    }

    /**
     * This method reads the scenario configuration file to generate events.
     * @param scenarioFile scenario to generate events configuration file.
     * @return file data.
     * @throws IOException  unable to find file or parse data.
     * @throws ConfigIOParseException unable to find file or parse data.
     */
    private PropertyDocument readConfigFile(String scenarioFile) throws IOException, ConfigIOParseException {
        return LoadFileLocation.fromFileLocation(scenarioFile);
    }

    private String scenarioFile_;
    private String mode_;
    private PropertyMap scenarioConfiguration_;
    private String timeDelayMus_;
    private String[] BURST_KEYS = new String[] {"ras", "sensor", "boot-on", "boot-off", "boot-ready", "rate"};
    private String[] GROUP_BURST_KEYS = new String[] {"totalRas", "totalSensor", "totalBootOn", "totalBootOff", "totalBootReady",
            "ras", "sensor", "boot-on", "boot-off", "boot-ready"};
    private String[] REPEAT_KEYS = new String[] {"mode", "clock-mode", "counter", "duration", "start-time"};
    private String[] SCENARIO_KEYS = new String[] {"mode", "delay", "repeat", "burst", "group-burst"};
}
