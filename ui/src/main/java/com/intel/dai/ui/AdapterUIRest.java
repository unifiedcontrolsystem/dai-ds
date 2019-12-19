// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import java.io.IOException;
import java.nio.charset.Charset;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import spark.Request;
import com.intel.logging.LoggerFactory;

import java.util.*;
import java.util.List;

import static spark.Spark.*;
import com.intel.properties.*;
import com.intel.config_io.*;
import com.intel.dai.exceptions.DataStoreException;

import com.intel.dai.exceptions.BadInputException;
import com.intel.dai.locations.Location;

import org.voltdb.client.ProcCallException;

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
    private QueryAPI mGUI_Updater;
    private CannedAPI mCLI_Updater;
    private Map<String,String> AdapterMap;
    private static ConfigIO jsonParser_ = null;

    AdapterUIRest(String[] args, Logger logger) {
        super(logger);
        AdapterMap = new HashMap<>();
        AdapterMap.put("ctrl", "CONTROL");
        AdapterMap.put("diagnostics", "DIAGNOSTICS");
        mGUI_Updater = new QueryAPI(logger);
        mCLI_Updater = new CannedAPI(logger);
        rabbitMQHost = (args.length >= 4) ? args[3] : "localhost";
    }

    public static void main(String[] cmd_args) {
        Logger logger = LoggerFactory.getInstance("UI", AdapterUIRest.class.getName(), "console");
        AdapterSingletonFactory.initializeFactory("UI", AdapterUIRest.class.getName(), logger);
        jsonParser_ = ConfigIOFactory.getInstance("json");
        assert jsonParser_ != null : "Failed to create a JSON parser!";
        staticFiles.location("/demo-v2/");

        final AdapterUIRest uiRest = new AdapterUIRest(cmd_args, logger);
        uiRest.initialize("UI", AdapterUIRest.class.getName(), cmd_args);
        execute_routes(uiRest);
    }

    void setParser(ConfigIO parser) {
        AdapterUIRest.jsonParser_ = parser;
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

        get("/power/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = convertHttpRequestToMap(req);
            String sub_cmd = req.params(":sub_cmd").toLowerCase();
            parameters.put("command", "power");
            return uiRest.power_commands(parameters, sub_cmd);
        });
        get("/diagnostics/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String nodes = req.queryParams("device");
            String image = req.queryParams("image");
            String test = req.queryParams("test");
            String result = req.queryParams("result");
            String sub_cmd = req.params(":sub_cmd");
            if (sub_cmd.compareToIgnoreCase("inband") == 0){
                return uiRest.diagnostics_inband(nodes, image, test, result);
            }
            else if (sub_cmd.compareToIgnoreCase("outofband") == 0){
                return createJsonResult(new String[]{"E", "Command Not implemented"});
            }
            return createJsonResult(new String[]{"E", "Command not found!"});
        });
        get("/bios/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            Map<String, String> parameters = convertHttpRequestToMap(req);
            if (req.params(":sub_cmd").compareToIgnoreCase("version") == 0){
                parameters.put("prettyPrint", "true");
                return uiRest.bios_version(parameters);
            }
            else if (req.params(":sub_cmd").compareToIgnoreCase("update") == 0){
                return uiRest.bios_update(parameters);
            }
            parameters.put("command", "bios");
            return uiRest.bios_toggles(parameters, sub_cmd);
        });
        get("/inventory/hw-info", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = convertHttpRequestToMap(req);
            parameters.put("prettyPrint", "true");
            parameters.put("command", "inventory");
            return uiRest.get_hardware_info(parameters);
        });
        get("/inventory/os-info", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = convertHttpRequestToMap(req);
            parameters.put("prettyPrint", "true");
            uiRest.log_.info("Parameters:");
            uiRest.log_.info(parameters.toString());
            parameters.put("command", "inventory");
            return uiRest.get_os_info(parameters);
        });
        get("/inventory/snapshot", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = convertHttpRequestToMap(req);
            parameters.put("command", "inventory");
            return uiRest.get_inventory_snapshot(parameters);
        });
        get("/resource/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = convertHttpRequestToMap(req);
            String sub_cmd = req.params(":sub_cmd");
            parameters.put("command", "resource");
            return uiRest.resource_commands(parameters, sub_cmd);
        });
        get("/service/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            Map<String, String> parameters = convertHttpRequestToMap(req);
            String username = System.getProperty("user.name");
            parameters.put("UserID", username);
            parameters.put("command", "service");
            return uiRest.service_commands(parameters, sub_cmd);
        });
        get("/job/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            Map<String, String> parameters = convertHttpRequestToMap(req);
            parameters.put("command", "job");
            return uiRest.job_launch_commands(parameters, sub_cmd);
        });
        get("/sensor/:sub_cmd/*", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            String sensor = req.uri().split(sub_cmd)[1].substring(1);
            uiRest.log_.info("Received sensor name " + sensor);
            Map<String, String> parameters = convertHttpRequestToMap(req);
            parameters.put("sensor_name", sensor);
            parameters.put("command", "sensor");
            return uiRest.sensor_commands(parameters, sub_cmd);
        });
        get("/command/:adapter/:item", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String item = req.params(":item");
            String adapter = req.params(":adapter");
            return uiRest.create_response(adapter, Long.parseLong(item));
        });
        get("/query/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            HashMap<String, String> param_map = new HashMap<>();
            Set<String> params = req.queryParams();
            for(String pm : params){
                param_map.put(pm, req.queryParams(pm));
            }
            return uiRest.query_cmds(sub_cmd, param_map);
        });
        get("/cli/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            Map<String, String> parameters = convertHttpRequestToMap(req);
            return uiRest.canned_cmds(sub_cmd, parameters);
        });
        get("/discover/:sub_cmd", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            String sub_cmd = req.params(":sub_cmd");
            Map<String, String> parameters = convertHttpRequestToMap(req);
            return uiRest.discover_commands(parameters, sub_cmd);
        });
        get("/groups", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.listGroups();
        });
        get("/groups/:group_name", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.getDevicesFromGroup(req.params(":group_name"));
        });
        delete("/groups/:group_name", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.deleteDevicesFromGroup(req.params(":group_name"), req.queryParams("devices"));
        });
        put("/groups/:group_name", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.addDevicesToGroup(req.params(":group_name"), req.queryParams("devices"));
        });
        post("/groups/:group_name", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.addDevicesToGroup(req.params(":group_name"), req.queryParams("devices"));
        });
        get("/system", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.retrieveSystemInformation(uiRest);
        });
        get("/provision/profiles/:profile", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.getProvisioningProfilesInfo(req.params(":profile").split(","));
        });
        get("/provision/profiles", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.listProvisioningProfiles();
        });
        put("/provision/profiles", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url() + " " + req.body());
            Map<String, String> parameters = convertHttpBodytoMap(URLEncodedUtils.parse(req.body(), Charset.defaultCharset()));
            return uiRest.addProvisioningProfile(parameters);
        });
        post("/provision/profiles", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = convertHttpBodytoMap(URLEncodedUtils.parse(req.body(), Charset.defaultCharset()));
            return uiRest.editProvisioningProfile(parameters);
        });
        delete("/provision/profiles/:profile", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.deleteProvisioningProfile(req.params(":profile"));
        });
        get("/provision/nodes/:nodes", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = new HashMap<>();
            parameters.put("device", req.params(":nodes"));
            parameters.put("command", "provision");
            return uiRest.workItemBlocking("PROVISIONER",  parameters,
                    "ListNodeProfile");
        });
        put("/provision/nodes", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url() + " " + req.body());
            List<NameValuePair> body = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
            Map<String, String> parameters = convertHttpBodytoMap(body);
            parameters.put("command", "provision");
            uiRest.log_.info(parameters.toString());
            return uiRest.workItemBlocking("PROVISIONER", parameters,
                    "RebootLocationsWithThisBootImage");
        });
        get("/provision/nodes/environment/:nodes", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url());
            Map<String, String> parameters = new HashMap<>();
            parameters.put("device", req.params(":nodes"));
            parameters.put("command", "provision");
            return uiRest.workItemBlocking("PROVISIONER",  parameters,
                    "ShowLocationsEnvironment");
        });
        put("/provision/nodes/environment", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url() + " " + req.body());
            List<NameValuePair> body = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
            Map<String, String> parameters = convertHttpBodytoMap(body);
            parameters.put("command", "provision");
            uiRest.log_.info(parameters.toString());
            return uiRest.workItemBlocking("PROVISIONER", parameters,
                    "SetLocationsWithThisEnvironment");
        });
        put("/fanspeed", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url() + " " + req.body());
            List<NameValuePair> body = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
            Map<String, String> parameters = convertHttpBodytoMap(body);
            parameters.put("command", "fanspeed");
            uiRest.log_.info(parameters.toString());
            return uiRest.fanSpeedControl(parameters);
        });
        put("/flashled", (req, res) -> {
            uiRest.log_.info("Received Request " + req.url() + " " + req.body());
            List<NameValuePair> body = URLEncodedUtils.parse(req.body(), Charset.defaultCharset());
            Map<String, String> parameters = convertHttpBodytoMap(body);
            parameters.put("command", "flashled");
            uiRest.log_.info(parameters.toString());
            return uiRest.flashLedControl(parameters);
        });
        get("/events/list-rasevent-type", (req, res) -> {
            uiRest.log_.info("Received Request by Authorized user " + req.url());
            Map<String, String> parameters = convertHttpRequestToMap(req);
            return uiRest.listRasEventTypes(parameters);
        });
        get("/locations", (req, rest) -> {
            uiRest.log_.info("Received Request " + req.url());
            return uiRest.getLocations();
        });
    }

    String getLocations() {
        return jsonParser_.toString(location.getSystemLocations());
    }

    private static Map<String, String> convertHttpBodytoMap(List<NameValuePair> pairs){
        /* Convert request body which is name value pair to a Map
        * */
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

    String listRasEventTypes(Map<String, String> parameters) {
        String[] result = new String[2];
        try {
            PropertyMap output = raseventapi.getRasEventTypes(parameters);
            result[0] = "F";
            result[1] = jsonParser_.toString(output);
        } catch (Exception e) {
            result[0] = "FE";
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            result[1] = jsonParser_.toString(errorObj.constructErrorResult());
        }
        return createJsonResult(result);
    }

    String generateRasEvents(Map<String, String> parameters) {
        String[] result = new String[2];
        try {
            String location = parameters.getOrDefault("location", null);
            if(location != null) {
                Set<String> nodeLocations = convertHostnamesToLocations(location);
                for(Iterator<String> nodeLocation = nodeLocations.iterator(); nodeLocation.hasNext();)
                    parameters.put("location", nodeLocation.next());
            }
            raseventapi.createRasEvent(parameters);
            result[0] = "F";
            result[1] = "Success";

        } catch (ProviderException|BadInputException e) {
            result[0] = "E";
            result[1] = "Error: " + e.getMessage();
        }
        return createJsonResult(result);
    }

    private static Set<String> convertToSet(String devices)
    {
        Set<String> deviceSet = new HashSet<>(Arrays.asList(devices.split(",")));
        if(deviceSet.isEmpty()) {
            return null;
        }
        return deviceSet;
    }

    static String flattenMapToString(Map<String, String> elements) {

        StringBuilder flattenedString = new StringBuilder();

        /*Convert a Map<key, value> into a string with "key1=value1\nkey2=value\n" */
        elements.forEach((key, value) -> {
            flattenedString.append(key);
            flattenedString.append("=");
            flattenedString.append(value);
            flattenedString.append("\n");
        });
        return flattenedString.toString();
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
        return createJsonResult(output);
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
        return createJsonResult(output);
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
        return createJsonResult(output);
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
        return createJsonResult(output);
    }

    @Override
    public String addProvisioningProfile(Map<String, String> parameters) {
        log_.info(parameters.toString());
        try {
            BootImageApi bootImageInfo = new BootImageApi(bootImage);
            return createJsonResult(new String[]{"F",
                    jsonParser_.toString(bootImageInfo.addBootImageProfile(parameters))});
        } catch (ProviderException e) {
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            return createJsonResult(new String[]{"FE", jsonParser_.toString(errorObj.constructErrorResult())});
        }
    }

    @Override
    public String editProvisioningProfile(Map<String, String> parameters) {
        log_.info(parameters.toString());
        try {
            BootImageApi bootImageInfo = new BootImageApi(bootImage);
            return createJsonResult(new String[]{"F",
                    jsonParser_.toString(bootImageInfo.editBootImageProfile(parameters))});
        } catch (ProviderException e) {
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            return createJsonResult(new String[]{"FE", jsonParser_.toString(errorObj.constructErrorResult())});
        }
    }

    @Override
    public String deleteProvisioningProfile(String profileIdToDelete)  {
        try {
            BootImageApi bootImageInfo = new BootImageApi(bootImage);
            return createJsonResult(new String[]{"F",
                    jsonParser_.toString(bootImageInfo.deleteBootImageProfile(profileIdToDelete))});
        } catch (ProviderException e) {
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            return createJsonResult(new String[]{"FE", jsonParser_.toString(errorObj.constructErrorResult())});
        }
    }

    @Override
    public String listProvisioningProfiles() {
        try {
            BootImageApi bootImageInfo = new BootImageApi(bootImage);
            return createJsonResult(new String[]{"F",
                    jsonParser_.toString(bootImageInfo.listBootImageProfiles())});
        } catch (ProviderException e) {
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            return createJsonResult(new String[]{"FE", jsonParser_.toString(errorObj.constructErrorResult())});
        }
    }

    @Override
    public String getProvisioningProfilesInfo(String[] profileIds) {
        try {
            BootImageApi bootImageInfo = new BootImageApi(bootImage);
            return createJsonResult(new String[]{"F",
                    jsonParser_.toString(bootImageInfo.retrieveBootImageProfile(profileIds))});
        } catch (ProviderException e) {
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            return createJsonResult(new String[]{"FE", jsonParser_.toString(errorObj.constructErrorResult())});
        }
    }

    @Override
    public String fanSpeedControl(Map<String, String> parameters) throws IOException, InterruptedException {
        return workItemCtrlBlocking(parameters, "setFanSpeed");
    }

    @Override
    public String flashLedControl(Map<String, String> parameters) throws IOException, InterruptedException {
        return workItemCtrlBlocking(parameters, "flashLed");
    }

    @Override
    public String power_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException{
        String workWantDone;
        switch (sub_cmd){
            case "on":
                workWantDone = "PowerOnComputeNode";
                break;
            case "off":
                workWantDone = "PowerOffComputeNode";
                break;
            case "cycle":
                workWantDone = "PowerCycleComputeNode";
                break;
            case "reset":
                workWantDone = "ResetNodes";
                break;
            case "shutdown":
                workWantDone = "ShutdownNode";
                break;
            default: return createJsonResult(new String[]{"E", "Command not found!"});
        }
        return workItemCtrlBlocking(parameters, workWantDone);
    }

    @Override
    public String diagnostics_inband(String nodes, String image, String test, String result) throws IOException,
            InterruptedException{
        /*
         * This is a REST endpoint URL: POST localhost:4567/diagnostics/inband?device=value&image=test.bin
         */
        Map<String, String> deviceParams = new HashMap<>();
        deviceParams.put("device",nodes);
        deviceParams.put("image",image);
        deviceParams.put("test",test);
        deviceParams.put("result",result);
        return workItemBlocking("DIAGNOSTICS", deviceParams, "RunInbandDiagnostics");
    }

    @Override
    public String bios_update(Map<String, String> parameters) throws IOException, InterruptedException{
        return workItemCtrlBlocking(parameters, "UpdateBios");
    }

    public String bios_toggles(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException{
        String workWantDone;
        switch (sub_cmd){
            case "get-option":
                workWantDone = "GetBiosSettings";
                break;
            case "set-option":
                workWantDone = "SetBiosSettings";
                break;
            case "list-options":
                workWantDone = "ListBiosSettings";
                break;
            default: return createJsonResult(new String[]{"E", "Command not found!"});
        }
        return workItemCtrlBlocking(parameters, workWantDone);
    }

    @Override
    public String query_cmds(String cmd, HashMap<String, String> params) {
        String[] results_array = new String[2];
        try {
            String return_result = mGUI_Updater.getData(cmd, params);
            results_array[0] = "F";
            results_array[1] = return_result;
        } catch (Exception e) {
            results_array[0] = "FE";
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            results_array[1] = jsonParser_.toString(errorObj.constructErrorResult());
        }
        return createJsonResult(results_array);
    }

    @Override
    public String canned_cmds(String cmd, Map<String, String> params) {
        String[] results_array = new String[2];
        try {
            String lctn_param = params.getOrDefault("Lctn", "");
            if (!lctn_param.equals("")){
                try {
                    Set<String> nodes = convertGroupsToNodes(lctn_param);
                    Location locations = createLocationObject(nodes);
                    Set<String> nodeLocations = new HashSet<>(locations.getLocation().values());
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
        } catch (Exception e) {
            results_array[0] = "FE";
            ErrorCreation errorObj = new ErrorCreation(e.getMessage());
            results_array[1] = jsonParser_.toString(errorObj.constructErrorResult());
        }
        return createJsonResult(results_array);
    }

    @Override
    public String bios_version(Map<String, String> parameters) throws IOException, InterruptedException {
        return workItemCtrlBlocking(parameters, "CheckBiosVersion");
    }

    @Override
    public String get_hardware_info(Map<String, String> parameters) throws IOException, InterruptedException {
        return workItemCtrlBlocking(parameters, "GetHardwareInfo");
    }

    @Override
    public String get_os_info(Map<String, String> parameters) throws IOException, InterruptedException {
        return workItemCtrlBlocking(parameters, "GetOSInfo");
    }

    @Override
    public String get_inventory_snapshot(Map<String, String> parameters) throws IOException, InterruptedException {
        return workItemServiceBlocking(parameters, "InventorySnapshot");
    }

    public String resource_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException{
        String workWantDone;
        switch (sub_cmd){
            case "add": workWantDone = "UseNode";
                break;
            case "remove": workWantDone = "DontUseNode";
                break;
            case "check": workWantDone = "CheckNode";
                break;
            default: return createJsonResult(new String[]{"E", "Command not found!"});
        }

        Set <String> nodes = convertGroupsToNodes(parameters.getOrDefault("device", ""));
        parameters.put("locations", String.join(",", nodes));

        return workItemWlmBlocking(parameters, workWantDone);
    }

    @Override
    public String service_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException {
        String workWantDone;
        String device_param = parameters.getOrDefault("device", "");
        if (!device_param.equals("")){
            try {
                Set<String> nodes = convertGroupsToNodes(device_param);
                Location locations = createLocationObject(nodes);
                Set<String> nodeLocations = new HashSet<>(locations.getLocation().values());
                String device = String.join(",", nodeLocations);
                parameters.put("device", device);
            } catch (BadInputException e){
                // If the list contains items that are not compute nodes, default to using a simple list
                // NOTE: This may be temporary until a better solution is available
                log_.info(e.getMessage() + " : Falling back to comma separated list.");
                parameters.put("device", device_param);
            }
        }

        switch (sub_cmd){
            case "repair-start": workWantDone = "StartRepair";
                break;
            case "repair-end": workWantDone = "EndRepair";
                break;
            case "pool": workWantDone = "PlaceInPool";
                break;
            case "engineering-start": workWantDone = "StartEngineering";
                break;
            case "engineering-end": workWantDone = "EndEngineering";
                break;
            default: return createJsonResult(new String[]{"E", "Command not found!"});
        }
        return workItemServiceBlocking(parameters, workWantDone);
    }

    @Override
    public String job_launch_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException{
        String workWantDone;
        switch (sub_cmd){
            case "launch": workWantDone = "AddJob";
                break;
            case "cancel": workWantDone = "RemoveJob";
                break;
            case "check": workWantDone = "CheckJobStatus";
                break;
            default: return createJsonResult(new String[]{"E", "Command not found!"});
        }
        return workItemCtrlBlocking(parameters, workWantDone);
    }

    @Override
    public String sensor_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException{
        String workWantDone;
        switch (sub_cmd){
            case "get":
                workWantDone = "GetSensor";
                break;
            case "get_over_time":
                workWantDone = "GetSensorOverTime";
                break;
            default: return createJsonResult(new String[]{"E", "Command not found!"});
        }

        return workItemCtrlBlocking(parameters, workWantDone);
    }

    @Override
    public String discover_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException{
        return workItemCtrlBlocking(parameters, "Discover");

    }

    private String workItemWlmBlocking(Map<String, String> sDeviceParams, String workWantDone) throws IOException, InterruptedException {
        return workItemBlocking("WLM",  sDeviceParams, workWantDone);
    }

    private String workItemServiceBlocking(Map<String, String> sDeviceParams, String workWantDone) throws IOException, InterruptedException {
        return workItemBlocking("SERVICE",  sDeviceParams, workWantDone);
    }

    private String workItemCtrlBlocking(Map<String, String> sDeviceParams, String workWantDone) throws IOException, InterruptedException {
        return workItemBlocking("CONTROL", sDeviceParams, workWantDone);
    }

    String workItemBlocking(String adapterType, Map<String, String> params, String workWantDone)
            throws IOException, InterruptedException {

        HashMap<String, Object> serviceopInfo;
        HashMap<String, String> reservations;
        String opType, allowedUser;
        String command = params.getOrDefault("command", "");
        String device = params.getOrDefault("device", "");
        String user = params.getOrDefault("user", "");

        try {
            Set<String> nodeLocations = convertHostnamesToLocations(device);
            String[] location = nodeLocations.toArray(new String[0]);
            for(int i=0; i < location.length; i++){
                serviceopInfo = serviceInfo.getServiceOperationInfo(location[i]);

                if (serviceopInfo.size() > 0) {
                    opType = (String) serviceopInfo.get("TypeOfServiceOperation");
                    allowedUser = (String) serviceopInfo.get("UserStartedService");

                    if (!commandAllowed(command, opType, allowedUser, user, location[i])) {
                        return createJsonResult(new String[]{"E", "Command not permitted for location " + location[i] + " for user: " + user});
                    }
                }

                reservations = wlmInfo.getUsersForActiveReservation(location[i]);
                if (reservations != null && reservations.size() > 0) {
                    String lctn, wlmUsers;
                    for(Map.Entry<String, String> entry : reservations.entrySet()) {
                        lctn = entry.getKey();
                        wlmUsers = entry.getValue();
                        if (!commandAllowedWLM(command, user, lctn, wlmUsers)) {
                            return createJsonResult(new String[]{"E", "Command not permitted for location: " + lctn + " for user: " + user});
                        }
                    }
                }
            }
        } catch (BadInputException ie) {
            ie.printStackTrace();
            return createJsonResult(new String[]{"E", "Invalid location, try again with a valid location"});
        } catch(ProcCallException | ProviderException ie2) {
            ie2.printStackTrace();
            return createJsonResult(new String[]{"E", ie2.getMessage()});
        } catch(Exception ie3) {
            ie3.printStackTrace();
            return createJsonResult(new String[]{"E", "Unknown error, please try again"});
        }


        if (adapterType.equals("CONTROL")
                || adapterType.equals("PROVISIONER") || adapterType.equals("DIAGNOSTICS")) {
            return workItemBlockingForRackAdapters(adapterType, params, workWantDone);
        }
        else {
            /*System level adapters will use this block */
            long WorkItemId = workQueue.queueWorkItem(adapterType, null, workWantDone, params,
                    true, adapter.adapterType(), workQueue.baseWorkItemId());
            String[] result = workQueue.waitForWorkItemToFinishAndMarkDone(workWantDone, adapterType,
                    WorkItemId, adapter.adapterType(), workQueue.baseWorkItemId());
            return createJsonResult(result);
        }
             
    }

    /* This function determines if a user can run a command */
    boolean commandAllowed(String command, String serviceOpType, String allowedUser, String user,
                                   String location) throws ProviderException {

        boolean result = true;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("instancedata", "User=" + user + " Command=" + command + " Lctn=" + location);
        if (serviceOpType.equals("Exclusive")){
            if(!user.equals(allowedUser)){
                result = false;
                parameters.put("eventtype", adapter.getRasEventType("RasCliSecurityUnauthEngrAccess"));
                raseventapi.createRasEvent(parameters);
            }
        }
        else {
            if (!command.equals("service")){
                result = false;
                parameters.put("eventtype", adapter.getRasEventType("RasCliSecurityUnauthServiceOper"));
                raseventapi.createRasEvent(parameters);
            }
        }

        return result;
    }

    boolean commandAllowedWLM(String command, String user, String location, String wlmUsers) throws ProviderException {

        boolean result = true;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("instancedata", "User=" + user + " Command=" + command + " Lctn=" + location);

        if(wlmUsers.indexOf(user) == -1 && !user.equals("root")){
            result = false;
            parameters.put("eventtype", adapter.getRasEventType("RasCliSecurityUnauthWlmAlloc"));
            raseventapi.createRasEvent(parameters);
        }

        return result;

    }

    /* This function should only be called for Rack level adapters. Make sure that params have a
     * key called "device" */
    private String workItemBlockingForRackAdapters(String adapterType, Map<String, String> params,
                                                     String workWantDone)
            throws IOException, InterruptedException{
        /*Map that connects aggregator to it's connected nodes */
        Map<String, Set<String>> aggregatorToNodes;
        Set <String> nodes = convertGroupsToNodes(params.getOrDefault("device", ""));
        Location nodeLocations = createLocationObject(nodes);
        try {
            /*Get the aggregators and its corresponding node set */
            aggregatorToNodes = nodeLocations.getAggregatorNodeLocations();

        } catch (BadInputException e) {
            log_.exception(e);
            return createJsonResult(new String[]{"E", "Bad input.\n" + e.getMessage()});
        }
        Set<Long>  workItemIds = new HashSet<>();
        /*If nodes are connected to different aggregators, multiple workitems have to be queued with
         * each aggregator getting the work for nodes that it is managing */
        for (Map.Entry<String, Set<String>> entry : aggregatorToNodes.entrySet()) {
            Set<String> nodesConnectedToAggregator = entry.getValue();
            params.put("device", String.join(",", nodesConnectedToAggregator));
            log_.info("workitem is being sent for nodes %s connected to aggregator %s",
                    String.join(",", nodesConnectedToAggregator), entry.getKey());
            /*Queue the workitem in the aggregator that the nodes are connected to */
            long WorkItemId = workQueue.queueWorkItem(adapterType, entry.getKey(), workWantDone, params,
                    true, adapter.adapterType(), workQueue.baseWorkItemId());
            log_.info("workitem that has to be worked on %d", WorkItemId);
            workItemIds.add(WorkItemId);
        }
        String[] results = new String[2];
        for(Long workItemId: workItemIds) {
            String[] result = workQueue.waitForWorkItemToFinishAndMarkDone(workWantDone, adapterType,
                    workItemId, adapter.adapterType(), workQueue.baseWorkItemId());
            if(results[0] == null || result[0].equals("E")) {
                results[0] = result[0];
            }
            if(results[1] == null) {
                results[1] = result[1];
            } else {
                if (adapterType.equals("CONTROL")) {
                    results[1] = concatJsonStrings(results[1], result[1]);
                }
                else {
                    results[1] = results[1] + "\n" + result[1];
                }
            }
        }
        return createJsonResult(results);
    }

    private Set<String> convertHostnamesToLocations(String device) throws BadInputException {
        Set<String> nodes = convertGroupsToNodes(device);
        Location locations = createLocationObject(nodes);
        return new HashSet<>(locations.getLocation().values());
    }

    Location createLocationObject(Set <String> nodes) {
        return new Location(adapter, nodes, log_);
    }

    String concatJsonStrings(String string1, String string2) {
        try {
            PropertyMap jsonMap1 = (PropertyMap) jsonParser_.fromString(string1);
            PropertyMap jsonMap2 = (PropertyMap) jsonParser_.fromString(string2);
            PropertyMap result = new PropertyMap();
            result.putAll(jsonMap1);
            result.putAll(jsonMap2);
            return jsonParser_.toString(result);
        } catch(ConfigIOParseException e) {
            return e.getMessage();
        }
    }

    private Set <String> convertGroupsToNodes(String devices) {
        /* Here the devices will be a comma seperated mix of hostname, location, group and mix of all
         * Convert all groups to hostnames or locations (However they are stored in the database) */
        String[] deviceList = devices.split(",");
        Set<String> nodes = new HashSet<>();
        for(String device: deviceList) {
            Set<String> nodesInGroup;
            try {
                nodesInGroup = groupsMgr.getDevicesFromGroup(device);
            } catch(DataStoreException e) {
                log_.exception(e);
                return new HashSet<>();
            }
            if(nodesInGroup.isEmpty()) {
                nodes.add(device);
            } else {
                /* It is a group */
                nodes.addAll(nodesInGroup);
            }
        }
        return nodes;
    }

    private String create_response(String sAdapterName, long workItemId) throws IOException {
        return createJsonResult(workQueue.getWorkItemStatus(AdapterMap.get(sAdapterName), workItemId));
    }

    private static String createJsonResult(String[] sa){
        PropertyMap result = new PropertyMap();
        result.put("Status", sa[0]);
        result.put("Result", sa[1]);
        return jsonParser_.toString(result);
    }

    String retrieveSystemInformation(AdapterUIRest uiRest) {
        try {
            SystemInfo systemInfo = new SystemInfo(uiRest.configMgr);
            return createJsonResult(new String[]{"F",
                    jsonParser_.toString(systemInfo.generateSystemInfo())});
        } catch (DataStoreException | PropertyNotExpectedType | ProviderException e) {
            return createJsonResult(new String[]{"E", e.getMessage()});
        }
    }
}
