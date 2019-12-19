package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.HttpMethod;
import com.intel.networking.NetworkException;
import com.intel.networking.restserver.*;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkSource {

    public NetworkSource(String configFile, Logger logger) {
        assert logger != null;
        log_ = logger;
        configFile_ = configFile;
    }

    public void initialize() throws IOException, ConfigIOParseException {
        initializeInstances();
        initializeNetwork();
    }

    public PropertyMap getAppConfiguration() throws PropertyNotExpectedType {
        if(!appConfiguration_.containsKey("eventsimConfig"))
            throw new RuntimeException("EventSim Configuration file doesn't contain eventsimConfig entry");
        return appConfiguration_.getMap("eventsimConfig");
    }

    public void registerPathCallBack(String path, HttpMethod method, NetworkSimulator callBack) throws RESTServerException {
        server_.addHandler(path, method, this::apiCallBack);
        PropertyMap urlMethodObj = new PropertyMap();
        urlMethodObj.put(path,method);
        dispatchMap.put(urlMethodObj, callBack);
    }

    public void startServer() throws RESTServerException {
        server_.start();
    }

    public void stopServer() throws RESTServerException {
        server_.stop();
    }

    public boolean getServerStatus() {
        return server_.isRunning();
    }

    public String getAddress() {
        return server_.getAddress();
    }

    public int getPort() {
       return server_.getPort();
    }

    public boolean sendMessage(String eventType, String message) {
        try {
            server_.ssePublish(eventType, message, null);
            return true;
        } catch (RESTServerException e) {
            log_.warn("Error while publishing message to network. " + e.getMessage());
            return false;
        }
    }

    protected void loadConfigFile(String fileName) throws IOException, ConfigIOParseException {
        appConfiguration_ = LoadConfigFile.fromFileLocation(fileName).getAsMap();
    }

    void validateConfig(PropertyMap sseconfig) {
        if (!sseconfig.containsKey("serverAddress"))
            throw new RuntimeException("EventSim Configuration file doesn't contain serverAddress entry");

        if (!sseconfig.containsKey("serverPort"))
            throw new RuntimeException("EventSim Configuration file doesn't contain serverPort entry");

        if (!sseconfig.containsKey("urls"))
            throw new RuntimeException("EventSim Configuration file doesn't contain urls entry");

    }

    private void initializeNetwork() {
        String networkName = appConfiguration_.getStringOrDefault("network", null);
        if (networkName == null) {
            log_.warn("EventSim Configuration file doesn't contain network entry");
        }
        else if (networkName.equals("sse")) {
            initializeSSE();
        }
        else if (networkName.equals("rabbitmq")) {
            //TODO rabbitmq
        }
    }

    private PropertyMap getSSEConfiguration() {
        return appConfiguration_.getMapOrDefault("sseConfig", null);
    }

    private void initializeSSE() {
        PropertyMap sseConfig = getSSEConfiguration();
        validateConfig(sseConfig);
        log_.info("Configuring SSE server.");
        try {
            server_ = RESTServerFactory.getInstance("jdk11", log_);
            if(server_ == null) throw new RuntimeException();
            server_.setAddress(sseConfig.getString("serverAddress"));
            server_.setPort(Integer.parseInt(sseConfig.getString("serverPort")));

            PropertyMap subscribeUrls = sseConfig.getMap("urls");
            log_.debug("*** Registering new SSE URLs...");
            for (String url : subscribeUrls.keySet()) {
                PropertyArray urls = subscribeUrls.getArrayOrDefault(url, new PropertyArray());
                List<String> subjects = new ArrayList<>();
                for (Object subject : urls)
                    subjects.add(subject.toString());
                log_.debug("*** Added route method GET/SSE to new URL %s", url);
                server_.addSSEHandler(url, subjects);
            }

        } catch (RESTServerException | RuntimeException |  PropertyNotExpectedType e) {
            log_.exception(e);
        }
    }

    private Map<String, String> convertHttpRequestToMap(Request req) throws RequestException {
        //Convert request query parameters to a Map
        Map<String, String> parameters = new HashMap<>();
        String query = req.getQuery();
        String params = null;
        if(query != null && !query.isBlank())
            params = URLDecoder.decode(query, StandardCharsets.UTF_8);
        if(params == null || params.isBlank())
            return parameters;
        String[] parts = params.split("&");
        for(String part: parts) {
            String[] keyValue = part.split("=");
            parameters.put(keyValue[0], keyValue[1]);
        }
        return parameters;
    }

    public void apiCallBack(Request request, Response response) throws RequestException, ConfigIOParseException {
        // Read the body and grab param
        Map<String, String> params = new HashMap<>();
        if (request.getMethod() == HttpMethod.POST) {
            params = convertHttpBodytoMap(request.getBody());
            String[] subcmds = request.getPath().split("/");
            params.put("sub_cmd", subcmds[subcmds.length - 1]);
        }
        else if (request.getMethod() == HttpMethod.GET || request.getMethod() == HttpMethod.DELETE ) {
            params = convertHttpRequestToMap(request);
            String[] subcmds = request.getPath().split("/");
            params.put("sub_cmd", subcmds[subcmds.length - 1]);
        }

        NetworkSimulator callBack = callBackUrl(dispatchMap, request.getPath(), request.getMethod());
        if (callBack == null) {
            log_.warn("callback is empty");
            response.setCode(500);
            response.setBody(String.format("{\"reason\":\"No internal mapped callback method for '%s'\"}",
                    request.getPath()));
        } else {
            try {
                response.setBody(callBack.routeHandler(params));
                response.setCode(200);
            }  catch(NetworkException e) {
                String result[] = e.getMessage().split("::");
                response.setCode(Integer.parseInt(result[0]));
                response.setBody(String.format("{\"reason\":\"%s\"}", result[1]));
            }  catch(Exception e) {
                response.setCode(500);
                response.setBody(String.format("{\"reason\":\"%s\"}", e.toString()));
            }
        }
    }

    private NetworkSimulator callBackUrl(Map<PropertyMap, NetworkSimulator> dispatchMap, String path, HttpMethod method) {
        if(path == null || method == null)
            return null;
        PropertyMap urlMethod = new PropertyMap();
        urlMethod.put(path, method);
        NetworkSimulator callBackUrl = dispatchMap.getOrDefault(urlMethod, null);
        if(callBackUrl == null) {
            int lastOcrdIndex = path.lastIndexOf('/');
            String newUrl = path.substring(0, lastOcrdIndex + 1) + '*';
            PropertyMap obj = new PropertyMap();
            obj.put(newUrl, method);
            return dispatchMap.getOrDefault(obj, null);
        }
        return callBackUrl;
    }

    private Map<String, String> convertHttpBodytoMap(String param) throws ConfigIOParseException {
        /* Convert request body which is name value pair to a Map */
        if(param.isEmpty())
            return null;
        PropertyMap params =  parser_.fromString(param).getAsMap();
        Map<String, String> map = new HashMap<>();
        for(String key : params.keySet()) {
            if(params.get(key) == null)
                map.put(key, null);
            else
                map.put(key, params.get(key).toString());
        }
        return map;
    }

    private void initializeInstances() throws IOException, ConfigIOParseException {
        loadConfigFile(configFile_);
        parser_ = ConfigIOFactory.getInstance("json");
        assert parser_ != null: "Failed to create a JSON parser!";
    }

    private String configFile_;
    private Logger log_;
    private ConfigIO parser_;
    public PropertyMap appConfiguration_;
    public static RESTServer server_ = null;
    private Map<PropertyMap, NetworkSimulator> dispatchMap = new HashMap<>();
}
