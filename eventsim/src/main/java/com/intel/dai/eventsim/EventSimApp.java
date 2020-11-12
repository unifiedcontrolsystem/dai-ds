package com.intel.dai.eventsim;

import com.intel.dai.eventsim.java11.Java11RESTServer;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.RESTServerException;
import com.intel.networking.restserver.RESTServerFactory;
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
public class EventSimApp  extends  EventSim {

    EventSimApp(String voltdbServer, String serverConfig, Logger log) {
        super(voltdbServer, serverConfig, log);
    }

    void executeRoutes(EventSimApp eventsimApi) throws SimulatorException {
        network_.register("/apis/events/boot", HttpMethod.POST.toString(), eventsimApi::generateBootEvents);
        network_.register("/apis/events/ras", HttpMethod.POST.toString(), eventsimApi::generateRasEvents);
        network_.register("/apis/events/scenario", HttpMethod.POST.toString(), eventsimApi::generateEventsForScenario);
        network_.register("/apis/events/seed", HttpMethod.GET.toString(), eventsimApi::getRandomizationSeed);
        network_.register("/apis/events/sensor", HttpMethod.POST.toString(), eventsimApi::generateSensorEvents);
        network_.register("/apis/events/echo", HttpMethod.POST.toString(),eventsimApi::generateEchoEvents);
        network_.register("/apis/events/locations", HttpMethod.GET.toString(), eventsimApi::getAllAvailableLocations);
        network_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE.toString(), eventsimApi::unsubscribeAllStateChangeNotifications);
        network_.register("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.POST.toString(), eventsimApi::subscribeStateChangeNotifications );
        network_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.GET.toString(), eventsimApi::getSubscriptionDetailForId);
        network_.register("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.GET.toString(), eventsimApi::getAllSubscriptionDetails );
        network_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE.toString(), eventsimApi::unsubscribeStateChangeNotifications);
        network_.register("/apis/bss/boot/v1/bootparameters", HttpMethod.GET.toString(), eventsimApi::getBootParameters);
        network_.register("/apis/smd/hsm/v1/State/Components", HttpMethod.GET.toString(), eventsimApi::getNodeStates);
        network_.register("/apis/smd/hsm/v1/State/Components/*", HttpMethod.GET.toString(), eventsimApi::getNodeStatesForLocation);
        network_.register("/apis/ims/images", HttpMethod.GET.toString(), eventsimApi::getBootImages);
        network_.register("/apis/ims/images/*", HttpMethod.GET.toString(), eventsimApi::getBootImageForImageId);
        network_.register("/Inventory/Discover", HttpMethod.POST.toString(), eventsimApi::initiateInventoryDiscovery);
        network_.register("/Inventory/DiscoveryStatus", HttpMethod.GET.toString(), eventsimApi::getAllInventoryDiscoveryStatus);
        network_.register("/Inventory/Hardware", HttpMethod.GET.toString(), eventsimApi::getInventoryHardware);
        network_.register("/Inventory/Hardware/*", HttpMethod.GET.toString(), eventsimApi::getInventoryHardwareForLocation);
        network_.register("/Inventory/Hardware/Query/*", HttpMethod.GET.toString(), eventsimApi::getInventoryHardwareQueryForLocation);
        network_.register("/wlm/createRes", HttpMethod.POST.toString(), eventsimApi::createReservation);
        network_.register("/wlm/modifyRes", HttpMethod.POST.toString(), eventsimApi::modifyReservation);
        network_.register("/wlm/deleteRes", HttpMethod.POST.toString(), eventsimApi::deleteReservation);
        network_.register("/wlm/startJob", HttpMethod.POST.toString(), eventsimApi::startJob);
        network_.register("/wlm/terminateJob", HttpMethod.POST.toString(), eventsimApi::terminateJob);
        network_.register("/wlm/simulate", HttpMethod.POST.toString(), eventsimApi::simulateWlm);
    }

    /**
     * This method is used to create and send boot off,on,ready events to network.
     * @param parameters input details of the request.
     * @return Status = F if boot events are generated, Status = E on failure
     */
    String generateBootEvents(Map<String, String> parameters) {
        try {
            log_.info("Received ras api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            removeEmptyValueParameters(parameters);
            foreignSimulatorEngine_.generateBootEvents(parameters);
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
    String generateRasEvents(Map<String, String> parameters) {
        try {
            log_.info("Received ras api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            removeEmptyValueParameters(parameters);
            foreignSimulatorEngine_.generateRasEvents(parameters);
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
            removeEmptyValueParameters(parameters);
            foreignSimulatorEngine_.generateEventsForScenario(parameters);
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
            removeEmptyValueParameters(parameters);
            foreignSimulatorEngine_.generateSensorEvents(parameters);
            return create_result_json("F", "Success");
        } catch (SimulatorException e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to echo a given event to network.
     * @param parameters input details of the request.
     * @return Status = F if events are generated, Status = E on failure
     */
    String generateEchoEvents(Map<String, String> parameters) {
        try {
            log_.info("Received echo request: " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
            foreignSimulatorEngine_.generateEchoEvents(parameters);
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
        String seed = foreignSimulatorEngine_.getRandomizationSeed();
        return create_result_json("F", seed);
    }

    /**
     * This method is used to get the avialble locations data.
     * @param parameters input details of the request.
     * @return Status = F if seed is found, Status = E on failure
     */
    String getAllAvailableLocations(Map<String, String> parameters) {
        log_.info("Received list-locations api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        PropertyDocument locations =  foreignSimulatorEngine_.getAllAvailableLocations();
        return create_result_json("F", parser_.toString(locations));
    }

    /**
     * This method is used to un-subscribe all available subscriptions.
     * @param parameters input details of the request.
     * @return true if all subscription is removed
     * false if all subscription is not removed or found.
     */
    String unsubscribeAllStateChangeNotifications(Map<String, String> parameters) {
        log_.info("Received unsubscribeAllStateChangeNotifications api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        network_.unRegisterAll();
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
        network_.register(subUrl, subscriber, parameters);
        PropertyMap subscription = network_.getSubscription(subUrl, subscriber);
        return parser_.toString(subscription);
    }

    /**
     * This method to fetch subscriptions for a given id.
     * @param parameters input details of the request.
     * @return subscription details.
     */
    String getSubscriptionDetailForId(Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getSubscriptionDetailForId api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String subId = parameters.getOrDefault("sub_component", null);
        PropertyDocument result = network_.getSubscriptionForId(Long.parseLong(subId));
        if(result == null)
            throw new SimulatorException("No subscription for given details exists.");
        return parser_.toString(result);
    }

    /**
     * This method to fetch all available subscriptions.
     * @param parameters input details of the request.
     * @return all subscription details.
     * @throws SimulatorException when unable to fetch all subscription details.
     */
    String getAllSubscriptionDetails(Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getAllSubscriptionDetails api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        PropertyDocument result = network_.getAllSubscriptions();
        if(result == null)
            throw new SimulatorException("No subscriptions exists.");
        return parser_.toString(result);
    }

    /**
     * This method is used to un-subscribe for a given subscription-id.
     * @param parameters input details of the request.
     * @return "" if subscription is removed
     */
    String unsubscribeStateChangeNotifications(Map<String, String> parameters) {
        log_.info("Received unsubscribeStateChangeNotifications api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String subid = parameters.getOrDefault("sub_component", null);
        network_.unRegisterId(Long.parseLong(subid));
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
        bootParamsApi_.setBootParamsConfigFile(dataLoader_.getBootParamsFileLocation());
        assert parameters != null;
        String location = parameters.getOrDefault("name",null);
        if(location == null || location.equals("null"))
            return parser_.toString(bootParamsApi_.getBootParameters());
        return parser_.toString(bootParamsApi_.getBootParametersForLocation(location));
    }

    /**
     * This method used to fetch node states data.
     * @param parameters input details of the request.
     * @return node states data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    public String getNodeStates(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getNodeStates api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        nodeStatesApi_.setNodeStatesConfigFile(dataLoader_.getNodeStateFileLocation());
        return parser_.toString(nodeStatesApi_.getNodeStates());
    }

    /**
     * This method used to fetch node states data for a given location.
     * @param parameters input details of the request.
     * @return node states data for given location
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    public String getNodeStatesForLocation(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getNodeStatesForLocation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        nodeStatesApi_.setNodeStatesConfigFile(dataLoader_.getNodeStateFileLocation());
        assert parameters != null;
        String location = parameters.getOrDefault("sub_component",null);
        if(location == null || location.isEmpty())
            return parser_.toString(new PropertyMap());
        return parser_.toString(nodeStatesApi_.getNodeStateForLocation(location));
    }

    /**
     * This method used to fetch boot images data.
     * @param parameters input details of the request.
     * @return boot images data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    public String getBootImages(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getBootImages api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        bootImagesApi_.setBootImagesConfigFile(dataLoader_.getBootImagesFileLocation());
        return parser_.toString(bootImagesApi_.getBootImages());
    }

    /**
     * This method used to fetch boot images data for a given image id.
     * @param parameters input details of the request.
     * @return boot images data for given image id
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    public String getBootImageForImageId(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getBootImages api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        bootImagesApi_.setBootImagesConfigFile(dataLoader_.getBootImagesFileLocation());
        assert parameters != null;
        String imageId = parameters.getOrDefault("sub_component",null);
        if(imageId == null || imageId.isEmpty())
            return parser_.toString(new PropertyArray());
        return parser_.toString(bootImagesApi_.getBootImageForId(imageId));
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
            foreignNameUri.put("URI", getForeignServerUrl() + dataLoader_.getHwInventoryDiscStatusUrl() + "/" + i);
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
        return parser_.toString(discoveryStatus);
    }

    /**
     * This method used to set inventory hardware configuration file.
     * @param parameters input details of the request.
     * @throws SimulatorException when unable to set the location of hardware inventory configuration file.
     */
    public String getInventoryHardware(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getInventoryHardware api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        hwInvApi_.setInventoryHardwareConfigLocation(dataLoader_.getHwInventoryFileLocation());
        PropertyArray hwInventory = (PropertyArray) hwInvApi_.getHwInventory();
        return parser_.toString(hwInventory);
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
        hwInvApi_.setInventoryHardwareConfigPath(dataLoader_.getHwInventoryFileLocationPath());
        PropertyDocument hwInventory =  hwInvApi_.getInventoryHardwareForLocation(location);
        return parser_.toString(hwInventory);
    }

    /**
     * This method used to fetch inventory hardware query for a location data.
     * @param parameters input details of the request.
     * @throws SimulatorException when unable to set the location of hardware inventory query for a location configuration file.
     */
    public String getInventoryHardwareQueryForLocation(final Map<String, String> parameters) throws SimulatorException {
        log_.info("Received getInventoryHardwareQueryForLocation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        String location = parameters.getOrDefault("sub_component", null);
        hwInvApi_.setInventoryHardwareQueryPath(dataLoader_.getHwInventoryQueryLocationPath());
        PropertyDocument hwInventory = hwInvApi_.getInventoryHardwareQueryForLocation(location);
        return parser_.toString(hwInventory);
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

            String result = wlmApi_.createReservation(name, users, nodes, starttime, duration);

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
            String result = wlmApi_.modifyReservation(name, users, nodes, starttime);

            return create_result_json("F", result);
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    public String deleteReservation(final Map<String, String> parameters) {
        log_.info("Received deleteReservation api request : " + ZonedDateTime.now(ZoneId.systemDefault()).toString());
        try {
            String name = parameters.get("name");
            String result = wlmApi_.deleteReservation(name);

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

            String result = wlmApi_.startJob(jobid, name, users, nodes, starttime, workdir);

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

            String result = wlmApi_.terminateJob(jobid, name, users, nodes, starttime, workdir, exitStatus);

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

            wlmApi_.simulateWlm(reservations, nodes);

            return create_result_json("F", "Success");
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create logger instance before calling main method
     */
    private static void setup() {
        log_ = LoggerFactory.getInstance("EventSimApp", "EventSimApp", "console");
    }

    /**
     * This method to start server.
     */
    void startServer() throws SimulatorException {
        network_.startServer();
        isServerUp_ = true;
    }

    /**
     * This class is to imitate like a server.
     */
    void stopServer() throws SimulatorException {
        network_.stopServer();
        isServerUp_ = false;
    }

    /**
     * This method to fetch server status.
     */
    boolean serverStatus() {
        network_.serverStatus();
        return isServerUp_;
    }

    /**
     * This method is used to pick random node hostnames
     * @return random nodes hostnames
     */
    private String pickRandomNodes() {
        List<String> locations = dataLoader_.getNodeLocationsHostnames();
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
     * This method to generate url of the server
     */
    private String getForeignServerUrl() {
        String foreignServerAddr = network_.getAddress();
        String foreignServerPort = String.valueOf(network_.getPort());
        return "http://" + foreignServerAddr + ":" + foreignServerPort;
    }

    /**
     * This method is used to return api requested data
     * @param status status of request
     * @param result result data
     * @return status and data in a format
     */
    private String create_result_json(Object status, Object result){
        PropertyMap output = new PropertyMap();
        output.put("Status", status);
        output.put("Result", result);
        return parser_.toString(output);
    }

    /**
     * This method is used to filter input parameters by removing null value inputs
     * @param parameters input data
     */
    private void removeEmptyValueParameters(Map<String, String> parameters) {
        for(Map.Entry<String, String> parameter : parameters.entrySet()) {
            String key = parameter.getKey();
            String value = parameter.getValue();
            if(value.isEmpty()) {
                log_.info("Removing key, value for key is empty, key = " + key);
                parameters.remove(key);
            }
        }
    }

    static {
        setup();
    }

    public static void main(String[] args) throws RESTServerException {
        RESTServerFactory.addImplementation("jdk11", Java11RESTServer.class);
        if(args.length != 2) {
            log_.error("Wrong number of arguments for EventSim server, use 2 arguments: voltdb_servers and configuration_file");
            System.exit(1);
        }

        load(args[0], args[1]);
    }

    private static void load(String voltdbServer, String serverConfigFile) {
        try {
            final EventSimApp eventSimApp = new EventSimApp(voltdbServer, serverConfigFile, log_);
            eventSimApp.initialiseInstances();
            eventSimApp.initialiseData();
            eventSimApp.run(eventSimApp);

        } catch (SimulatorException e) {
            log_.exception(e);
            System.exit(1);
        }
    }

    void run(EventSimApp eventSimApp) throws SimulatorException {
        executeRoutes(eventSimApp);
        startServer();
    }

    static Logger log_;

    private int numOfForeignNamesDiscovery;
    private boolean isServerUp_ = false;
}
