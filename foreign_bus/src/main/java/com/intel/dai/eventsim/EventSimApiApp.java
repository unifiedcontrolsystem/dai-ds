package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.HttpMethod;
import com.intel.networking.NetworkException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.json_voltpatches.JSONArray;
import org.json_voltpatches.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class EventSimApiApp implements EventSimApi {

    EventSimApiApp(String configFile, Logger logger) {
        log_ = logger;
        configFile_ = configFile;
    }

    public void initialize() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        loadNetworkSource();
        loadBootParamApi();
        loadInventoryApi();
        loadConnectionManager();
        loadEngine();
    }

    public void startEngine() throws IOException, ConfigIOParseException, PropertyNotExpectedType {
        eventSimEngine.initialize();
        new Thread(eventSimEngine).start();
    }

    public static void main(String[] args) throws IOException, ConfigIOParseException, PropertyNotExpectedType, RESTServerException {
        Logger log = LoggerFactory.getInstance("EventSimApiApp", "EventSimApiApp", "console");
        assert log != null;
        jsonParser_ = ConfigIOFactory.getInstance("json");
        assert jsonParser_ != null : "Failed to create a JSON parser!";
        log.info("Starting Foreign Api Simulator Server");
        final EventSimApiApp eventsimapi = new EventSimApiApp(configFile, log);
        eventsimapi.run(eventsimapi);
        eventsimapi.startServer();
    }

    private void run(EventSimApiApp eventsimapi) throws PropertyNotExpectedType, IOException, ConfigIOParseException, RESTServerException {
        initialize();
        startEngine();
        executeRoutes(eventsimapi);
    }

    public void executeRoutes(EventSimApiApp eventsimapi) throws RESTServerException {
        source_.registerPathCallBack("/api/ras", HttpMethod.POST, eventsimapi::generatRasEvents);
        source_.registerPathCallBack("/api/sensor", HttpMethod.POST, eventsimapi::generateEnvEvents);
        source_.registerPathCallBack("/api/boot", HttpMethod.POST, eventsimapi::generateBootEvents);
        source_.registerPathCallBack("/bootparameters", HttpMethod.GET, eventsimapi::getBootParameters);
        source_.registerPathCallBack("/Inventory/Discover", HttpMethod.POST, eventsimapi::initiateInventoryDiscover);
        source_.registerPathCallBack("/Inventory/DiscoveryStatus", HttpMethod.GET, eventsimapi::getAllInventoryDiscoverStatus);
        source_.registerPathCallBack("/Inventory/Hardware", HttpMethod.GET, eventsimapi::getInventoryHardware);
        source_.registerPathCallBack("/Inventory/Hardware/*", HttpMethod.GET, eventsimapi::getInventoryHardwareForLocation);
        source_.registerPathCallBack("/Inventory/Hardware/Query/*", HttpMethod.GET, eventsimapi::getInventoryHardwareQueryForLocation);
        source_.registerPathCallBack("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.GET, eventsimapi::getSubscriptionDetailForId);
        source_.registerPathCallBack("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.GET, eventsimapi::getAllSubscriptionDetails );
        source_.registerPathCallBack("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.POST, eventsimapi::subscribeStateChangeNotifications );
        source_.registerPathCallBack("/apis/smd/hsm/v1/Subscriptions/SCN", HttpMethod.DELETE, eventsimapi::unsubscribeAllStateChangeNotifications);
        source_.registerPathCallBack("/apis/smd/hsm/v1/Subscriptions/SCN/*", HttpMethod.DELETE, eventsimapi::unsubscribeStateChangeNotifications);
    }

    String getSubscriptionDetailForId(Map<String, String> parameters) throws Exception {
        String connId = parameters.getOrDefault("sub_cmd", null);
        if(connId == null)
            throw new Exception("Insufficient data to get subscription details.");
        return jsonParser_.toString(connManager_.getConnectionForId(Long.parseLong(connId)));
    }

    String getAllSubscriptionDetails(Map<String, String> parameters) throws Exception {
        return jsonParser_.toString(connManager_.getAllConnections());
    }

    String unsubscribeStateChangeNotifications(Map<String, String> parameters) {
        String connId = parameters.getOrDefault("sub_cmd", null);
        if(connId == null)
            throw new NetworkException("400::Insufficient data to unsubscribe a connection.");
        connManager_.remove(connId);
        return "";
    }

    String unsubscribeAllStateChangeNotifications(Map<String, String> parameters) {
        connManager_.removeAll();
        return "";
    }

    String subscribeStateChangeNotifications(Map<String, String> parameters) throws Exception {
        connManager_.add(parameters);
        String subscriber = parameters.get("Subscriber");
        String connUrl = parameters.get("Url");
        PropertyMap connection = connManager_.getConnection(connUrl, subscriber);
        return jsonParser_.toString(connection);
    }

    String getForeignServerUrl() {
        String foreignServerAddr = source_.getAddress();
        String foreignServerPort = String.valueOf(source_.getPort());
        return "http://" + foreignServerAddr + ":" + foreignServerPort;
    }

    @Override
    public String getInventoryHardware(Map<String, String> paramters) {
        PropertyArray hwInventory = inventoryApi_.getHwInventory();
        return jsonParser_.toString(hwInventory);
    }

    public String getInventoryHardwareForLocation(Map<String, String> paramters) {
        String location = paramters.getOrDefault("sub_cmd", null);
        PropertyArray hwInventory = inventoryApi_.getInventoryHardwareForLocation(location);
        return jsonParser_.toString(hwInventory);
    }

    public String getInventoryHardwareQueryForLocation(Map<String, String> paramters) {
        String location = paramters.getOrDefault("sub_cmd", null);
        PropertyArray hwInventory = inventoryApi_.getInventoryHardwareQueryForLocation(location);
        return jsonParser_.toString(hwInventory);
    }

    @Override
    public String getAllInventoryDiscoverStatus(Map<String, String> parameters) {
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

    @Override
    public String initiateInventoryDiscover(Map<String, String> parameters) throws RESTServerException {
        String locations = parameters.getOrDefault("xnames", null);
        if(locations == null)
            throw new RESTServerException("Error: xnames details is required.");
        locations = locations.substring(1,locations.length() - 1);
        List<String> xnames = Arrays.asList(locations.split(","));
        numOfXnamesDiscovery = xnames.size();
        boolean force = Boolean.valueOf(parameters.getOrDefault("force", "false"));
        List<PropertyMap> xnameDiscovery = new ArrayList<>();
        for(int i = 0; i < numOfXnamesDiscovery; i++) {
            PropertyMap xnameUri = new PropertyMap();
            xnameUri.put("URI", getForeignServerUrl() + inventoryDiscoveryStatusURL + "/" + i);
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

    @Override
    public String generatRasEvents(Map<String, String> parameters) {
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

    @Override
    public String generateEnvEvents(Map<String, String> parameters) {
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

    @Override
    public String generateBootEvents(Map<String, String> parameters) {
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

    @Override
    public String getBootParameters(Map<String, String> paramters) {
        PropertyMap data = bootParamApi_.getBootParametrs();
        return jsonParser_.toString(data.getArrayOrDefault("content", new PropertyArray()));
    }

    void loadEngine() throws PropertyNotExpectedType, IOException, ConfigIOParseException {
        eventSimEngine = new EventSimEngine(source_, connManager_, log_);
    }

    void loadBootParamApi() {
        bootParamApi_ = new BootParamApi();
    }

    void loadInventoryApi() {
        inventoryApi_ = new InventoryApi();
    }

    void loadNetworkSource() throws IOException, ConfigIOParseException {
        source_ = new NetworkSource(configFile_, log_);
        source_.initialize();
    }

    void loadConnectionManager() {
        connManager_ = new ConnectionManager(log_);
    }

    private String create_result_json(Object status, Object result){
        PropertyMap output = new PropertyMap();
        output.put("Status", status);
        output.put("Result", result);
        return jsonParser_.toString(output);
    }

    void stopServer() throws RESTServerException {
        source_.stopServer();
    }

    void startServer() throws RESTServerException {
        source_.startServer();
    }

    private final String inventoryDiscoveryStatusURL = "/Inventory/DiscoveryStatus";
    static ConfigIO jsonParser_ = null;
    NetworkSource source_;
    public static Logger log_;
    EventSimEngine eventSimEngine;
    private static String configFile = "/opt/ucs/etc/EventSim.json";
    BootParamApi bootParamApi_;
    private String configFile_;
    InventoryApi inventoryApi_;
    private int numOfXnamesDiscovery = 1;
    private ConnectionManager connManager_;
}
