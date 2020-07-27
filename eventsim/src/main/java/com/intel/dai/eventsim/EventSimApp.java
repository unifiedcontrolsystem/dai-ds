package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.HttpMethod;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import org.json_voltpatches.JSONArray;
import org.json_voltpatches.JSONObject;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Description of class EventSimApp.
 * This class handles simulation of various types of data through API's.
 */
public class EventSimApp extends EventSim {

    private EventSimApp(String[] args) throws SimulatorException {
        super(args, log_);
    }

    EventSimApp(Logger log) {
        super(log);
    }

    public static void main(String[] args) {
        log_ = LoggerFactory.getInstance("EventSimApp", "EventSimApp", "console");
        if(args.length != 2) {
            log_.error("Wrong number of arguments for EventSim server, use 2 arguments: voltdb_servers and configuration_file");
            System.exit(1);
        }
        run(args);
    }

    private static void run(String[] args) {
        try {
            final EventSimApp eventsimServer = new EventSimApp(args);
            eventsimServer.initialise(args);
            eventsimServer.executeRoutes(eventsimServer);
            eventsimServer.startServer();
        } catch (SimulatorException e) {
            log_.exception(e);
            System.exit(1);
        }
    }

    void executeRoutes(EventSimApp eventsimApi) throws SimulatorException {
        source_.register("/apis/events/boot/*", HttpMethod.POST.toString(), eventsimApi::generateBootEvents);
        source_.register("/apis/events/fabric", HttpMethod.POST.toString(), eventsimApi::generateFabricEvents);
        source_.register("/apis/events/ras", HttpMethod.POST.toString(), eventsimApi::generatRasEvents);
        source_.register("/apis/events/scenario", HttpMethod.POST.toString(), eventsimApi::generateEventsForScenario);
        source_.register("/apis/events/seed", HttpMethod.GET.toString(), eventsimApi::getRandomizationSeed);
        source_.register("/apis/events/sensor", HttpMethod.POST.toString(), eventsimApi::generateSensorEvents);
        source_.register("/apis/events/job", HttpMethod.POST.toString(), eventsimApi::generateJobEvents);
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE.toString(), eventsimApi::unsubscribeAllStateChangeNotifications);
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.POST.toString(), eventsimApi::subscribeStateChangeNotifications );
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.GET.toString(), eventsimApi::getSubscriptionDetailForId);
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.GET.toString(), eventsimApi::getAllSubscriptionDetails );
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE.toString(), eventsimApi::unsubscribeStateChangeNotifications);
        source_.register("/bootparameters", HttpMethod.GET.toString(), eventsimApi::getBootParameters);
        source_.register("/Inventory/Discover", HttpMethod.POST.toString(), eventsimApi::initiateInventoryDiscovery);
        source_.register("/Inventory/DiscoveryStatus", HttpMethod.GET.toString(), eventsimApi::getAllInventoryDiscoveryStatus);
        source_.register("/Inventory/Hardware", HttpMethod.GET.toString(), eventsimApi::getInventoryHardware);
        source_.register("/Inventory/Hardware/*", HttpMethod.GET.toString(), eventsimApi::getInventoryHardwareForLocation);
        source_.register("/Inventory/Hardware/Query/*", HttpMethod.GET.toString(), eventsimApi::getInventoryHardwareQueryForLocation);
        source_.register("/wlm/createRes", HttpMethod.POST.toString(), eventsimApi::createReservation);
        source_.register("/wlm/modifyRes", HttpMethod.POST.toString(), eventsimApi::modifyReservation);
        source_.register("/wlm/deleteRes", HttpMethod.POST.toString(), eventsimApi::deleteReservation);
        source_.register("/wlm/startJob", HttpMethod.POST.toString(), eventsimApi::startJob);
        source_.register("/wlm/terminateJob", HttpMethod.POST.toString(), eventsimApi::terminateJob);
        source_.register("/wlm/simulate", HttpMethod.POST.toString(), eventsimApi::simulateWlm);
    }

    /**
     * This method is used to create and send boot off,on,ready events to network.
     * @param parameters input details of the request.
     * @return Status = F if boot events are generated, Status = E on failure
     */
    String generateBootEvents(Map<String, String> parameters) {
        try {
            log_.info("Received boot api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            String burst = parameters.getOrDefault("burst", "false");
            String delay = parameters.getOrDefault("delay", null);
            String locations = parameters.getOrDefault("locations", ".*");
            String output = parameters.getOrDefault("output", null);
            String bfProbability = parameters.getOrDefault("probability", "0");
            String seed = parameters.getOrDefault("seed", null);

            String sub_cmd = parameters.get("sub_component");
            switch (sub_cmd) {
                case "off"   :   eventSimEngine.publishBootOffEvents(locations, burst, delay, seed, output);
                                 break;
                case "on"    :   eventSimEngine.publishBootOnEvents(locations, bfProbability, burst, delay, seed, output);
                                 break;
                case "ready" :   eventSimEngine.publishBootReadyEvents(locations, burst, delay, seed, output);
                                 break;
                default      :   eventSimEngine.publishBootEvents(locations, bfProbability, burst, delay, seed, output);
                                 break;
            }
            return create_result_json("F", "Success");
        } catch (SimulatorException e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send fabric events to network.
     * @param parameters input details of the request.
     * @return Status = F if ras events are generated, Status = E on failure
     */
    String generateFabricEvents(Map<String, String> parameters) {
        try {
            log_.info("Received fabric api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            String burst = parameters.getOrDefault("burst", "false");
            String eventsCount = parameters.getOrDefault("count", null);
            String delay = parameters.getOrDefault("delay", null);
            String label = parameters.getOrDefault("label", ".*");
            String locations = parameters.getOrDefault("locations", ".*");
            String output = parameters.getOrDefault("output", null);
            String seed = parameters.getOrDefault("seed", null);
            String sensorsRate = parameters.getOrDefault("sensor-rate", null);
            eventSimEngine.publishFabricEvents(locations, label, burst, delay, seed, eventsCount, sensorsRate, output);
            return create_result_json("F", "Success");
        } catch (SimulatorException e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send ras events to network.
     * @param parameters input details of the request.
     * @return Status = F if ras events are generated, Status = E on failure
     */
    String generatRasEvents(Map<String, String> parameters) {
        try {
            log_.info("Received ras api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            String burst = parameters.getOrDefault("burst", "false");
            String eventsCount = parameters.getOrDefault("count", null);
            String delay = parameters.getOrDefault("delay", null);
            String label = parameters.getOrDefault("label", ".*");
            String locations = parameters.getOrDefault("locations", ".*");
            String output = parameters.getOrDefault("output", null);
            String seed = parameters.getOrDefault("seed", null);
            eventSimEngine.publishRasEvents(locations, label, burst, delay, seed, eventsCount, output);
            return create_result_json("F", "Success");
        } catch (SimulatorException e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send events to network for a scenario given in a file.
     * @param parameters input details of the request.
     * @return Status = F if scenario is generated, Status = E on failure
     */
    String generateEventsForScenario(Map<String, String> parameters) {
        try {
            log_.info("Received scenario api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            String scenarioFile = parameters.getOrDefault("file", null);
            String burst = parameters.getOrDefault("burst", "false");
            String counter = parameters.getOrDefault("counter", null);
            String delay = parameters.getOrDefault("delay", null);
            String duration = parameters.getOrDefault("duration", null);
            String locations = parameters.getOrDefault("locations", ".*");
            String output = parameters.getOrDefault("output", null);
            String bfProbability = parameters.getOrDefault("probability", "0");
            String rasLabel = parameters.getOrDefault("ras-label", ".*");
            String sensorLabel = parameters.getOrDefault("sensor-label", ".*");
            String seed = parameters.getOrDefault("seed", null);
            String startTime = parameters.getOrDefault("start-time", null);
            String type = parameters.getOrDefault("type", null);
            eventSimEngine.publishEventsForScenario(scenarioFile, type, locations, rasLabel, sensorLabel, bfProbability, burst, delay, seed, counter, duration, startTime, output);
            return create_result_json("F", "Success");
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send sensor/environmental events to network.
     * @param parameters input details of the request.
     * @return Status = F if events are generated, Status = E on failure
     */
    String generateSensorEvents(Map<String, String> parameters) {
        try {
            log_.info("Received sensor api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            String burst = parameters.getOrDefault("burst", "false");
            String eventsCount = parameters.getOrDefault("count", null);
            String delay = parameters.getOrDefault("delay", null);
            String label = parameters.getOrDefault("label", ".*");
            String location = parameters.getOrDefault("locations", ".*");
            String output = parameters.getOrDefault("output", null);
            String seed = parameters.getOrDefault("seed", null);
            eventSimEngine.publishSensorEvents(location, label, burst, delay, seed, eventsCount, output);
            return create_result_json("F", "Success");

        } catch (SimulatorException e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send job events to network.
     * @param parameters input details of the request.
     * @return Status = F if events are generated, Status = E on failure
     */
    String generateJobEvents(Map<String, String> parameters) {
        try {
            log_.info("Received job api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            String burst = parameters.getOrDefault("burst", "false");
            String eventsCount = parameters.getOrDefault("count", null);
            String delay = parameters.getOrDefault("delay", null);
            String label = parameters.getOrDefault("label", ".*");
            String location = parameters.getOrDefault("locations", ".*");
            String output = parameters.getOrDefault("output", null);
            String seed = parameters.getOrDefault("seed", null);
            eventSimEngine.publishJobEvents(location, label, burst, delay, seed, eventsCount, output);
            return create_result_json("F", "Success");

        } catch (SimulatorException e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to get the randomization seed used.
     * @param parameters input details of the request.
     * @return Status = F if seed is found, Status = E on failure
     */
    String getRandomizationSeed(Map<String, String> parameters) {
        log_.info("Received get-seed api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String seed = eventSimEngine.getRandomizationSeed();
        return create_result_json("F", seed);
    }

    /**
     * This method is used to un-subscribe all available subscriptions.
     * @param parameters input details of the request.
     * @return true if all subscription is removed
     * false if all subscription is not removed or found.
     */
    String unsubscribeAllStateChangeNotifications(Map<String, String> parameters) {
        log_.info("Received unsubscribeAllStateChangeNotifications api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        source_.unRegisterAll();
        return "";
    }

    /**
     * This method to fetch all available subscriptions.
     * @param parameters input details of the request.
     * @return all subscription details.
     */
    String subscribeStateChangeNotifications(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received subscribeStateChangeNotifications api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String subscriber = parameters.getOrDefault("Subscriber", null);
        String subUrl = parameters.getOrDefault("Url", null);
        source_.register(subUrl, subscriber, parameters);
        PropertyMap subscription = source_.getSubscription(subUrl, subscriber);
        return jsonParser_.toString(subscription);
    }

    /**
     * This method to fetch subscriptions for a given id.
     * @param parameters input details of the request.
     * @return subscription details.
     */
    String getSubscriptionDetailForId(Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getSubscriptionDetailForId api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String subId = parameters.getOrDefault("sub_component", null);
        PropertyDocument result = source_.getSubscriptionForId(Long.parseLong(subId));
        if(result == null)
            throw new SimulatorException("No subscription for given details exists.");
        return jsonParser_.toString(result);
    }

    /**
     * This method to fetch all available subscriptions.
     * @param parameters input details of the request.
     * @return all subscription details.
     * @throws SimulatorException when unable to fetch all subscription details.
     */
    String getAllSubscriptionDetails(Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getAllSubscriptionDetails api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        PropertyDocument result = source_.getAllSubscriptions();
        if(result == null)
            throw new SimulatorException("No subscriptions exists.");
        return jsonParser_.toString(result);
    }

    /**
     * This method is used to un-subscribe for a given subscription-id.
     * @param parameters input details of the request.
     * @return "" if subscription is removed
     */
    String unsubscribeStateChangeNotifications(Map<String, String> parameters) {
        log_.info("Received unsubscribeStateChangeNotifications api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String subid = parameters.getOrDefault("sub_component", null);
        source_.unRegisterId(Long.parseLong(subid));
        return "";
    }

    /**
     * This method used to fetch boot parameters data.
     * @param parameters input details of the request.
     * @return boot parameters data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    public String getBootParameters(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getBootParameters api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        bootParamsApi_.setBootParamsConfigFile(simEngineDataLoader.getBootParamsFileLocation());
        assert parameters != null;
        String location = parameters.getOrDefault("hosts",null);
        if(location == null || location.equals("null"))
            return jsonParser_.toString(bootParamsApi_.getBootParameters());
        return jsonParser_.toString(bootParamsApi_.getBootParametersForLocation(location));
    }

    /**
     * This method used to fetch hw inventory discovery data for all location/location
     * @param parameters input details of the request.
     * @return hw inventory data
     * @throws ResultOutputException when unable to find location to initialte discovery
     */
    public String initiateInventoryDiscovery(final Map<String, String> parameters) throws ResultOutputException {
        log_.info("Received initiateInventoryDiscovery api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String locations = parameters.getOrDefault("xnames", null);
        if(locations == null)
            throw new ResultOutputException("404::One or more requested RedfishEndpoint foreign IDs was not found.");
        locations = locations.substring(1,locations.length() - 1);
        List<String> foreignLocations = Arrays.asList(locations.split(","));
        numOfForeignNamesDiscovery = foreignLocations.size();
        List<PropertyMap> foreignNameDiscovery = new ArrayList<>();
        for(int i = 0; i < numOfForeignNamesDiscovery; i++) {
            PropertyMap foreignNameUri = new PropertyMap();
            foreignNameUri.put("URI", getForeignServerUrl() + simEngineDataLoader.getHwInventoryDiscStatusUrl() + "/" + i);
            foreignNameDiscovery.add(foreignNameUri);
        }

        List<JSONObject> jsonObj = new ArrayList<>();

        for(PropertyMap data : foreignNameDiscovery) {
            JSONObject obj = new JSONObject(data);
            jsonObj.add(obj);
        }

        JSONArray test = new JSONArray(jsonObj);
        return (test.toString());
    }

    /**
     * This method used to fetch hw inventory discovery data for all locatios.
     * @param parameters input details of the request.
     * @return hw inventory data
     */
    public String getAllInventoryDiscoveryStatus(final Map<String, String> parameters) {
        log_.info("Received getAllInventoryDiscoveryStatus api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        PropertyArray discoveryStatus = new PropertyArray();
        for(int i = 0; i < numOfForeignNamesDiscovery; i++) {
            PropertyMap foreignNameDiscoveryStatus = new PropertyMap();
            foreignNameDiscoveryStatus.put("ID", String.valueOf(i));
            foreignNameDiscoveryStatus.put("Status","Complete");
            foreignNameDiscoveryStatus.put("LastUpdateTime", System.currentTimeMillis(

            ));
            foreignNameDiscoveryStatus.put("Details", null);
            discoveryStatus.add(foreignNameDiscoveryStatus);
        }
        return jsonParser_.toString(discoveryStatus);
    }

    /**
     * This method used to set inventory hardware configuration file.
     * @param parameters input details of the request.
     * @throws SimulatorException when unable to set the location of hardware inventory configuration file.
     */
    public String getInventoryHardware(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getInventoryHardware api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        hwInvApi_.setInventoryHardwareConfigLocation(simEngineDataLoader.getHwInventoryFileLocation());
        PropertyArray hwInventory = (PropertyArray) hwInvApi_.getHwInventory();
        return jsonParser_.toString(hwInventory);
    }

    /**
     * This method used to fetch hardware inventory for a location data.
     * @param parameters input details of the request.
     * @return inventory hardware for a location data.
     * @throws SimulatorException when unable to get inventory hardware for a location data.
     */
    public String getInventoryHardwareForLocation(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getInventoryHardwareForLocation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String location = parameters.getOrDefault("sub_component", null);
        hwInvApi_.setInventoryHardwareConfigPath(simEngineDataLoader.getHwInventoryFileLocationPath());
        PropertyDocument hwInventory =  hwInvApi_.getInventoryHardwareForLocation(location);
        return jsonParser_.toString(hwInventory);
    }

    /**
     * This method used to fetch inventory hardware query for a location data.
     * @param parameters input details of the request.
     * @throws SimulatorException when unable to set the location of hardware inventory query for a location configuration file.
     */
    public String getInventoryHardwareQueryForLocation(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getInventoryHardwareQueryForLocation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String location = parameters.getOrDefault("sub_component", null);
        hwInvApi_.setInventoryHardwareQueryPath(simEngineDataLoader.getHwInventoryQueryLocationPath());
        PropertyDocument hwInventory = hwInvApi_.getInventoryHardwareQueryForLocation(location);
        return jsonParser_.toString(hwInventory);
    }

    public String createReservation(final Map<String, String> parameters) {
        log_.info("Received createReservation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String name = parameters.get("name");
            String users = parameters.get("users");
            String nodes = parameters.get("nodes");
            Timestamp starttime = Timestamp.valueOf(parameters.get("starttime"));
            String duration = parameters.get("duration");

            if(nodes.equals("random"))
                nodes = pickRandomNodes();

            String result = wlmApi.createReservation(name, users, nodes, starttime, duration);

            return create_result_json("F", result);
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    public String modifyReservation(final Map<String, String> parameters) {
        log_.info("Received modifyReservation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String name = parameters.get("name");
            String users = parameters.get("users");
            String nodes = parameters.get("nodes");
            String starttime = parameters.get("starttime");
            String result = wlmApi.modifyReservation(name, users, nodes, starttime);

            return create_result_json("F", result);
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    public String deleteReservation(final Map<String, String> parameters) {
        log_.info("Received deleteReservation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String name = parameters.get("name");
            String result = wlmApi.deleteReservation(name);

            return create_result_json("F", result);
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    public String startJob(final Map<String, String> parameters) {
        log_.info("Received startJob api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String jobid = parameters.get("jobid");
            String name = parameters.get("name");
            String users = parameters.get("users");
            String nodes = parameters.get("nodes");
            Timestamp starttime = Timestamp.valueOf(parameters.get("starttime"));
            String workdir = parameters.get("workdir");

            if(nodes.equals("random"))
                nodes = pickRandomNodes();

            String result = wlmApi.startJob(jobid, name, users, nodes, starttime, workdir);

            return create_result_json("F", result);
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    public String terminateJob(final Map<String, String> parameters) {
        log_.info("Received terminateJob api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String jobid = parameters.get("jobid");
            String name = parameters.get("name");
            String users = parameters.get("users");
            String nodes = parameters.get("nodes");
            Timestamp starttime = Timestamp.valueOf(parameters.get("starttime"));
            String workdir = parameters.get("workdir");
            String exitStatus = parameters.get("exitstatus");

            if(nodes.equals("random"))
                nodes = pickRandomNodes();

            String result = wlmApi.terminateJob(jobid, name, users, nodes, starttime, workdir, exitStatus);

            return create_result_json("F", result);
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    public String simulateWlm(final Map<String, String> parameters) {
        log_.info("Received simulateWlm api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String reservations = parameters.get("reservations");
            String[] nodes = pickRandomNodes().split(" ");

            wlmApi.simulateWlm(reservations, nodes);

            return create_result_json("F", "Success");
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    private String pickRandomNodes() throws Exception {
        simEngineDataLoader.loadData();
        List<String> locations = simEngineDataLoader.getNodeHostnameData();
        Random rand = new Random();
        int size = rand.nextInt(locations.size());
        StringBuilder nodes = new StringBuilder();

        for(int i = 0; i < size; i++) {
            int index = rand.nextInt(size);
            nodes.append(" ").append(locations.get(index));
        }

        return nodes.toString();
    }

    /**
     * This method to start server.
     */
    void startServer() throws SimulatorException { source_.startServer(); }

    /**
     * This class is to imitate like a server.
     */
    void stopServer() throws SimulatorException { source_.stopServer(); }

    /**
     * This method to fetch server status.
     */
    void serverStatus() { source_.serverStatus(); }

    /**
     * This method to generate url of the server
     */
    private String getForeignServerUrl() {
        String foreignServerAddr = source_.getAddress();
        String foreignServerPort = String.valueOf(source_.getPort());
        return "http://" + foreignServerAddr + ":" + foreignServerPort;
    }

    private String create_result_json(Object status, Object result){
        PropertyMap output = new PropertyMap();
        output.put("Status", status);
        output.put("Result", result);
        return jsonParser_.toString(output);
    }

    static Logger log_;
    private int numOfForeignNamesDiscovery = 1;
}
