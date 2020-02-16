package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.HttpMethod;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.json_voltpatches.JSONArray;
import org.json_voltpatches.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Description of class CallBackNetwork.
 * server serves api request
 */
public class EventSimApp extends EventSim {

    private EventSimApp(String[] args) throws SimulatorException, RESTClientException, RESTServerException, PropertyNotExpectedType {
        super(args, log_);
    }

    EventSimApp(Logger log) {
        super(log);
    }

    public static void main(String args[]) {
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
        } catch (SimulatorException | RESTServerException | RESTClientException | PropertyNotExpectedType e) {
            log_.error(e.getMessage());
            System.exit(1);
        }
    }

    void executeRoutes(EventSimApp eventsimApi) throws SimulatorException {
        source_.register("/api/ras", HttpMethod.POST.toString(), eventsimApi::generatRasEvents);
        source_.register("/api/sensor", HttpMethod.POST.toString(), eventsimApi::generateEnvEvents);
        source_.register("/api/boot", HttpMethod.POST.toString(), eventsimApi::generateBootEvents);
        source_.register("/bootparameters", HttpMethod.GET.toString(), eventsimApi::getBootParameters);
        source_.register("/Inventory/Discover", HttpMethod.POST.toString(), eventsimApi::initiateInventoryDiscovery);
        source_.register("/Inventory/DiscoveryStatus", HttpMethod.GET.toString(), eventsimApi::getAllInventoryDiscoveryStatus);
        source_.register("/Inventory/Hardware", HttpMethod.GET.toString(), eventsimApi::getInventoryHardware);
        source_.register("/Inventory/Hardware/*", HttpMethod.GET.toString(), eventsimApi::getInventoryHardwareForLocation);
        source_.register("/Inventory/Hardware/Query/*", HttpMethod.GET.toString(), eventsimApi::getInventoryHardwareQueryForLocation);
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE.toString(), eventsimApi::unsubscribeAllStateChangeNotifications);
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.POST.toString(), eventsimApi::subscribeStateChangeNotifications );
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.GET.toString(), eventsimApi::getSubscriptionDetailForId);
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.GET.toString(), eventsimApi::getAllSubscriptionDetails );
        source_.register("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE.toString(), eventsimApi::unsubscribeStateChangeNotifications);
    }

    /**
     * This method is used to create and send sensor events to network.
     * @param parameters input details of the request.
     */
    String generateEnvEvents(Map<String, String> parameters) {
        try {
            String location = parameters.get("location");
            String label = parameters.get("label");
            String eventscount = parameters.get("count");
            String burst = parameters.get("burst");
            eventSimEngine.publishSensorEvents(location, label, eventscount, burst);
            return create_result_json("F", "Success");

        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send ras events to network.
     * @param parameters input details of the request.
     */
    String generatRasEvents(Map<String, String> parameters) {
        try {
            String location = parameters.get("location");
            String label = parameters.get("label");
            String eventscount = parameters.get("count");
            String burst = parameters.get("burst");
            eventSimEngine.publishRasEvents(location, label, eventscount, burst);
            return create_result_json("F", "Success");
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method is used to create and send boot events to network.
     * @param parameters input details of the request.
     */
    String generateBootEvents(Map<String, String> parameters) {
        try {
            String location = parameters.get("location");
            String bfValue = parameters.get("probability");
            String burst = parameters.get("burst");
            eventSimEngine.publishBootEvents(location, bfValue, burst);
            return create_result_json("F", "Success");
        } catch (Exception e) {
            return create_result_json("E", "Error: " + e.getMessage());
        }
    }

    /**
     * This method used to fetch boot parameters data.
     * @param parameters input details of the request.
     * @return boot parameters data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    public String getBootParameters(final Map<String, String> parameters) throws SimulatorException {
        bootParamsApi_.setBootParamsConfigFile(simEngineDataLoader.getBootParamsFileLocation());
        String location = parameters.getOrDefault("hosts",null);
        if(location == null || location.equals("null"))
            return jsonParser_.toString(bootParamsApi_.getBootParameters());
        return jsonParser_.toString(bootParamsApi_.getBootParametersForLocation(location));
    }

    public String initiateInventoryDiscovery(final Map<String, String> parameters) throws ResultOutputException {
        String locations = parameters.getOrDefault("xnames", null);
        if(locations == null)
            throw new ResultOutputException("404::One or more requested RedfishEndpoint xname IDs was not found.");
        locations = locations.substring(1,locations.length() - 1);
        List<String> xnames = Arrays.asList(locations.split(","));
        numOfXnamesDiscovery = xnames.size();
        List<PropertyMap> xnameDiscovery = new ArrayList<>();
        for(int i = 0; i < numOfXnamesDiscovery; i++) {
            PropertyMap xnameUri = new PropertyMap();
            xnameUri.put("URI", getForeignServerUrl() + simEngineDataLoader.getHwInventoryDiscStatusUrl() + "/" + i);
            xnameDiscovery.add(xnameUri);
        }

        List<JSONObject> jsonObj = new ArrayList<>();

        for(PropertyMap data : xnameDiscovery) {
            JSONObject obj = new JSONObject(data);
            jsonObj.add(obj);
        }

        JSONArray test = new JSONArray(jsonObj);
        return (test.toString());
    }

    public String getAllInventoryDiscoveryStatus(final Map<String, String> parameters) {
        PropertyArray discoveryStatus = new PropertyArray();
        for(int i = 0; i < numOfXnamesDiscovery; i++) {
            PropertyMap xnameDiscoveryStatus = new PropertyMap();
            xnameDiscoveryStatus.put("ID", String.valueOf(i));
            xnameDiscoveryStatus.put("Status","Complete");
            xnameDiscoveryStatus.put("LastUpdateTime", System.currentTimeMillis(

            ));
            xnameDiscoveryStatus.put("Details", null);
            discoveryStatus.add(xnameDiscoveryStatus);
        }
        return jsonParser_.toString(discoveryStatus);
    }

    /**
     * This method used to set inventory hardware configuration file.
     * @param parameters input details of the request.
     * @throws SimulatorException when unable to set the location of hardware inventory configuration file.
     */
    public String getInventoryHardware(final Map<String, String> parameters) throws SimulatorException {
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
        String location = parameters.getOrDefault("sub_component", null);
        hwInvApi_.setInventoryHardwareQueryPath(simEngineDataLoader.getHwInventoryQueryLocationPath());
        PropertyDocument hwInventory = hwInvApi_.getInventoryHardwareQueryForLocation(location);
        return jsonParser_.toString(hwInventory);
    }

    /**
     * This method to fetch all available subscriptions.
     * @param parameters input details of the request.
     * @return all subscription details.
     */
    String subscribeStateChangeNotifications(final Map<String, String> parameters) throws SimulatorException {
        String subscriber = parameters.getOrDefault("Subscriber", null);
        String subUrl = parameters.getOrDefault("Url", null);
        source_.register(subUrl, subscriber, parameters);
        PropertyMap subscription = source_.getSubscription(subUrl, subscriber);
        return jsonParser_.toString(subscription);
    }

    /**
     * This method is used to un-subscribe all available subscriptions.
     * @param parameters input details of the request.
     * @return true if all subscription is removed
     * false if all subscription is not removed or found.
     */
    String unsubscribeAllStateChangeNotifications(Map<String, String> parameters) {
        source_.unRegisterAll();
        return "";
    }

    /**
     * This method to fetch subscriptions for a given id.
     * @param parameters input details of the request.
     * @return subscription details.
     */
    String getSubscriptionDetailForId(Map<String, String> parameters) throws SimulatorException {
        String subId = parameters.getOrDefault("sub_component", null);
        PropertyDocument result = source_.getSubscriptionForId(Long.parseLong(subId));
        if(result == null)
            throw new SimulatorException("No subscription for given details exists.");
        return jsonParser_.toString(result);
    }

    /**
     * This method is used to un-subscribe for a given subscription-id.
     * @param parameters input details of the request.
     * @return "" if subscription is removed
     */
    String unsubscribeStateChangeNotifications(Map<String, String> parameters) {
        String subid = parameters.getOrDefault("sub_component", null);
        source_.unRegisterId(Long.parseLong(subid));
        return "";
    }

    /**
     * This method to fetch all available subscriptions.
     * @param parameters input details of the request.
     * @return all subscription details.
     * @throws SimulatorException when unable to fetch all subscription details.
     */
    String getAllSubscriptionDetails(Map<String, String> parameters) throws SimulatorException {
        PropertyDocument result = source_.getAllSubscriptions();
        if(result == null)
            throw new SimulatorException("No subscriptions exists.");
        return jsonParser_.toString(result);
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

    private static Logger log_;
    private int numOfXnamesDiscovery = 1;
}
