// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import java.sql.SQLException;
import java.nio.charset.Charset;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import java.util.*;

import com.intel.properties.*;
import com.intel.config_io.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.BadInputException;
import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;

import spark.Request;
import static spark.Spark.*;

public class AdapterUIRest extends AdapterUI {
    /**
     * Invocation Parms:
     *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
     *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
     * Example invocation:
     *      java AdapterUIRest voltdbserver1,voltdbserver2,10.11.12.13  (or java AdapterUIRest - this will default to using localhost)
     *
     * Determine what REST Java framework this will use.
     */
    private String rabbitMQHost;
    private Map<String,String> AdapterMap;
    ResponseCreator responseCreator;

    AdapterUIRest(String[] args, Logger logger) {
        super(logger);
        AdapterMap = new HashMap<>();
        AdapterMap.put("ctrl", "CONTROL");
        AdapterMap.put("diagnostics", "DIAGNOSTICS");
        responseCreator = new ResponseCreator();
        rabbitMQHost = (args.length >= 4) ? args[3] : "localhost";
    }

    public static void main(String[] cmd_args) {
        Logger logger = LoggerFactory.getInstance("UI", AdapterUIRest.class.getName(), "console");
        AdapterSingletonFactory.initializeFactory("UI", AdapterUIRest.class.getName(), logger);
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        assert parser != null : "Failed to create a JSON parser!";
        staticFiles.location("/demo-v2/");
        final AdapterUIRest uiRest = new AdapterUIRest(cmd_args, logger);
        uiRest.setParser(parser);
        uiRest.initialize("UI", AdapterUIRest.class.getName(), cmd_args);
        execute_routes(uiRest);
    }

    void setParser(ConfigIO parser) {
        responseCreator.setParser(parser);
    }

    static void execute_routes(AdapterUIRest uiRest) {

        get("/", (req, res) -> {
            res.redirect("ucs-gui.html");
            return 0;
        });

        get("/v", (req, res) -> {
            res.redirect("ucs-guivolt.html");
            return 0;
        });
        get("/query/:sub_cmd", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            HashMap<String, String> param_map = new HashMap<>();
            Set<String> params = req.queryParams();
            for(String pm : params){
                param_map.put(pm, req.queryParams(pm));
            }
            return uiRest.query_cmds(sub_cmd, param_map);
        });
        get("/cli/:sub_cmd", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            Map<String, String> parameters = convertHttpRequestToMap(req);
            return uiRest.canned_cmds(sub_cmd, parameters);
        });
        get("/groups", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            return uiRest.listGroups();
        });
        get("/groups/:group_name", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            return uiRest.getDevicesFromGroup(req.params(":group_name"));
        });
        delete("/groups/:group_name", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            return uiRest.deleteDevicesFromGroup(req.params(":group_name"), req.queryParams("devices"));
        });
        put("/groups/:group_name", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            Map<String, String> parameters = convertHttpBodytoMap(req);
            return uiRest.addDevicesToGroup(req.params(":group_name"), parameters.get("devices"));
        });
        post("/groups/:group_name", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            return uiRest.addDevicesToGroup(req.params(":group_name"), req.queryParams("devices"));
        });
        get("/system", (req, res) -> {
            uiRest.log_.debug("Received Request " + req.url());
            return uiRest.retrieveSystemInformation(uiRest);
        });
        get("/locations", (req, rest) -> {
            uiRest.log_.debug("Received Request " + req.url());
            return uiRest.getLocations();
        });
    }

    String getLocations() {
        return responseCreator.toString(locationMgr.getSystemLocations());
    }

    private static Map<String, String> convertHttpBodytoMap(Request req){
        /* Convert request body which is name value pair to a Map
        * */
        List<NameValuePair> pairs = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
        Map<String, String> map = new HashMap<>();
        for(int index=0; index<pairs.size(); index++){
            NameValuePair pair = pairs.get(index);
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }
    
    private static Map<String, String> convertHttpRequestToMap(Request req) {
        /* Convert request header parameters to a Map
         * */
        Map<String, String> parameters = new HashMap<>();
        Set<String> params = req.queryParams();
        for(String pm : params){
            /* SPEC for HTTP get reserved characters : https://tools.ietf.org/html/rfc3986#page-12 */
            if((req.queryParams(pm) != null) && (!req.queryParams(pm).isEmpty())){
                parameters.put(pm, req.queryParams(pm));
            }
        }
        return parameters;
    }

    private static Set<String> convertToSet(String devices)
        {
        Set<String> deviceSet = new HashSet<>(Arrays.asList(devices.split(",")));
        if(deviceSet.isEmpty()) {
            return null;
        }
        return deviceSet;
    }

    @Override
    public String addDevicesToGroup(String groupName, String devices)
    {
        String[] output = new String[2];
        try {
            output[0] = "F";
            output[1] = groupsMgr.addDevicesToGroup(groupName, convertToSet(devices));
        } catch(DataStoreException e) {
            log_.exception(e);
            output[0] = "E";
            output[1] = e.getMessage();
        }
        return responseCreator.createJsonResult(output);
    }

    @Override
    public String deleteDevicesFromGroup(String groupName, String devices)
    {
        String[] output = new String[2];
        try {
            output[0] = "F";
            output[1] = groupsMgr.deleteDevicesFromGroup(groupName, convertToSet(devices));
        } catch(DataStoreException e) {
            log_.exception(e);
            output[0] = "E";
            output[1] = e.getMessage();
        }
        return responseCreator.createJsonResult(output);
    }

    @Override
    public String getDevicesFromGroup(String groupName) {
        String[] output = new String[2];
        try {
            String joined = String.join(",", groupsMgr.getDevicesFromGroup(groupName));
            if(joined.isEmpty()) {
                output[0] = "E";
                output[1] = "Group doesn't exist or doesn't have any devices";
            } else {
                output[0] = "F";
                output[1] = joined;
            }
        } catch(DataStoreException e) {
            log_.exception(e);
            output[0] = "E";
            output[1] = e.getMessage();
        }
        return responseCreator.createJsonResult(output);
    }

    @Override
    public String listGroups() {
        String[] output = new String[2];
        try {
            String joined = String.join(",", groupsMgr.listGroups());
            if(joined.isEmpty()) {
                output[0] = "E";
                output[1] = "Groups aren't available";
            } else {
                output[0] = "F";
                output[1] = joined;
            }
        } catch(DataStoreException e) {
            log_.exception(e);
            output[0] = "E";
            output[1] = e.getMessage();
        }
        return responseCreator.createJsonResult(output);
    }

    @Override
    public String query_cmds(String cmd, HashMap<String, String> params) {
        String[] results_array = new String[2];
        QueryAPI mGUI_Updater = new QueryAPI();
        try {
            String return_result = mGUI_Updater.getData(cmd, params);
            results_array[0] = "F";
            results_array[1] = return_result;
        } catch (Exception e) {
            log_.exception(e, "[QueryAPI]");
            results_array[0] = "FE";
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            results_array[1] = responseCreator.toString(errorObj.constructErrorResult());
        }
        return responseCreator.createJsonResult(results_array);
    }

    @Override
    public String canned_cmds(String cmd, Map<String, String> params) {
        CannedAPI mCLI_Updater = new CannedAPI(log_);
        String[] results_array = new String[2];
        try {
            String lctn_param = params.getOrDefault("Lctn", "");
            if (!lctn_param.equals("")){
                try {
                    Set<String> nodeLocations = locationApi.convertHostnamesToLocations(lctn_param);
                    String lctn = String.join(",", nodeLocations);
                    params.put("Lctn", lctn);
                } catch (BadInputException e){
                    // If the list contains items that are not compute nodes, default to using a simple list
                    // NOTE: This may be temporary until a better solution is available
                    log_.info(e.getMessage() + " : Falling back to comma separated list.");
                    params.put("Lctn", lctn_param);
                }
            }
            String return_result = mCLI_Updater.getData(cmd, params);
            results_array[0] = "F";
            results_array[1] = return_result;
        } catch (SQLException | DataStoreException | ProviderException e) {
            log_.exception(e, "[CannedAPI]");
            results_array[0] = "FE";
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            results_array[1] = responseCreator.toString(errorObj.constructErrorResult());
        }
        return responseCreator.createJsonResult(results_array);
    }

    String retrieveSystemInformation(AdapterUIRest uiRest) {
        try {
            SystemInfo systemInfo = new SystemInfo(uiRest.configMgr);
            return responseCreator.createJsonResult(new String[]{"F",
                    responseCreator.toString(systemInfo.generateSystemInfo())});
        } catch (DataStoreException | PropertyNotExpectedType | ProviderException e) {
            return responseCreator.createJsonResult(new String[]{"E", e.getMessage()});
        }
    }
}
