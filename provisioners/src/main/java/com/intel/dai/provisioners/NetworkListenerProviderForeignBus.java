// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.provisioners;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description of class BootEventTransformer.
 */
public class NetworkListenerProviderForeignBus implements NetworkListenerProvider, Initializer {
    public NetworkListenerProviderForeignBus(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null)
            throw new RuntimeException("Failed to create a JSON parser for class" + getClass().getCanonicalName());
    }

    @Override
    public void initialize() { /* Not used but is required */ }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        try {
            PropertyMap message = parser_.fromString(data).getAsMap();
            log_.debug("*** Message: %s", data);
            checkMessage(message);
            PropertyArray locationArray = message.getArrayOrDefault(FOREIGN_LOCATION_KEY, new PropertyArray());
            String[] foreignLocations = locationArray.toArray(new String[0]);
            String nodeState = message.getStringOrDefault(FOREIGN_NODE_STATE_KEY, null);
            if(!conversionMap_.containsKey(nodeState))
                throw new NetworkListenerProviderException("The '" + FOREIGN_NODE_STATE_KEY + "' boot state field in JSON is " +
                        "not a known name: " + nodeState);
            BootState curNodeState = conversionMap_.get(nodeState);
            List<CommonDataFormat> commonList = new ArrayList<>();
            for(String foreignLocation: foreignLocations) {
                String location;
                try {
                    location = CommonFunctions.convertForeignToLocation(foreignLocation);
                    CommonDataFormat common = new CommonDataFormat(
                            CommonFunctions.convertISOToLongTimestamp(message.getString(FOREIGN_TIMESTAMP_KEY)), location,
                            DataType.StateChangeEvent);
                    common.setStateChangeEvent(curNodeState);
                    commonList.add(common);
                    if (common.getStateEvent() == BootState.NODE_ONLINE) {
                        // This is a contract with the BootEventActions class.
                        common.storeExtraData(ORIG_FOREIGN_LOCATION_KEY, foreignLocation);
                    }
                } catch(ConversionException e) {
                    log_.warn("Failed to convert a foreign location to a DAI location, skipping data element for " +
                            "location %s", foreignLocation);
                    log_.debug("Skipped location for data: %s", data);
                }
            }
            return commonList;
        } catch(ConfigIOParseException | PropertyNotExpectedType | ParseException e) {
            throw new NetworkListenerProviderException("Failed to parse the event from the component");
        }
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        if(config_ == null)
            getConfig(config, systemActions);
        String bootImageId = data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY);
        boolean matched = bootImageId == null || bootImageId.isBlank() || knownIds_.containsKey(bootImageId);
        if(!matched || Instant.now().toEpochMilli() >= targetRequestTimeMs_.get())
            new Thread(this::updateBootImageTable).start(); // Background updates of table...
        long dataTimestamp = data.getNanoSecondTimestamp();
        actions_.changeNodeStateTo(data.getStateEvent(), data.getLocation(),
                dataTimestamp, informWlm_);
        if(data.getStateEvent() == BootState.NODE_ONLINE) {
            try {
                data.storeExtraData(FOREIGN_IMAGE_ID_KEY, extractBootImageId(data.retrieveExtraData(ORIG_FOREIGN_LOCATION_KEY)));
            } catch (Exception e) {
                log_.error(e.getMessage());
            }
            updateNodeBootImageId(data);
        }
        if(data.getStateEvent() == BootState.NODE_OFFLINE)
            actions_.storeRasEvent("RasMntrForeignNodeFailed", "No reason given by foreign software",
                    data.getLocation(), dataTimestamp);
        if(publish_)
            actions_.publishBootEvent(topic_, data.getStateEvent(), data.getLocation(), dataTimestamp);
    }

    private void getConfig(NetworkListenerConfig config, SystemActions systemActions) {
        config_ = config;
        actions_ = systemActions;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", publish_);
            bootInfoUrlPath_ = map.getStringOrDefault("bootImageInfoUrlPath", bootInfoUrlPath_);
            informWlm_ = map.getBooleanOrDefault("informWorkLoadManager", informWlm_);
            topic_ = map.getStringOrDefault("publishTopic", topic_);
            try {
                baseUrl_ = config.getFirstNetworkBaseUrl(false);
            } catch(ConfigIOParseException e) {
                log_.exception(e, "Failed to get the baseUrl");
                baseUrl_ = "http://127.0.0.1:65535"; // A bogus URL will be used causing other errors to be logged.
            }
        }
    }

    private void updateNodeBootImageId(CommonDataFormat data) {
        String bootImageId = data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY);
        if(bootImageId.equals("")) {
            // TODO: Replace with foreign API call if required to get bootImageId based on foreign FoLocation.
            log_.error("Failed to update node boot image ID for location '%s'", data.getLocation());
            actions_.logFailedToUpdateNodeBootImageId(data.getLocation(), makeInstanceDataForFailedNodeUpdate(data));
        } else {
            log_.debug("Changing node location '%s' to have Boot ID of '%s'", data.getLocation(), bootImageId);
            actions_.changeNodeBootImageId(data.getLocation(), bootImageId);
        }
    }

    private String makeInstanceDataForFailedNodeUpdate(CommonDataFormat data) {
        return String.format("ForeignLocation='%s'; UcsLocation='%s'; BootMessage='%s'",
                data.retrieveExtraData(ORIG_FOREIGN_LOCATION_KEY), data.getLocation(), data.getStateEvent().toString());
    }
    
    private void updateBootImageTable() {
        if(updating_.get())
            return;
        updating_.set(true);
        try {
            if(client_ == null)
                client_ = createClient();
            BlockingResult result = client_.getRESTRequestBlocking(makeUri(null));
            if(result.code == 200) // HTTP 200 OK
                updateImageInformation(result.responseDocument);
            else
                throw new RESTClientException("RESTClient resulted in HTTP code: " + result.code +
                        ":" + result.responseDocument);
        } catch(RESTClientException | URISyntaxException e) {
            log_.exception(e);
            actions_.logFailedToUpdateBootImageInfo(String.format("Full URL=%s%s", baseUrl_, bootInfoUrlPath_));
        }
        updating_.set(false);
    }

    protected RESTClient createClient() throws RESTClientException { // protected for test mocking.
        RESTClient client = RESTClientFactory.getInstance("jdk11", log_);
        if(client == null)
            throw new RESTClientException("Failed to get the REST client implementation");
        return client;
    }

    private URI makeUri(String location) throws URISyntaxException {
        return new URI(String.format("%s%s%s%s", baseUrl_, bootInfoUrlPath_, FOREIGN_QUERY_PARAM, location));
    }

    private void updateImageInformation(String result) {
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        assert parser != null:"Failed to get JSON parser";
        log_.debug(result);
        try {
            PropertyArray list = parser.fromString(result).getAsArray();
            List<Map<String,String>> infoList = new ArrayList<>();
            for(int i = 0; i < list.size(); i++) {
                PropertyMap info = list.getMap(i);
                Map<String,String> bootInfo = translateBootImageInfo(info);
                infoList.add(bootInfo);
                knownIds_.putIfAbsent(bootInfo.get("id"), OBJECT);
            }
            actions_.upsertBootImages(infoList);
            targetRequestTimeMs_.set(Instant.now().toEpochMilli() + DELTA_UPDATE_MS);
        } catch(ConfigIOParseException | PropertyNotExpectedType e) {
            log_.exception(e);
            actions_.logFailedToUpdateBootImageInfo("Bad JSON returned: '" + result + "'");
        }
    }

    private Map<String, String> translateBootImageInfo(PropertyMap info) {
        // TODO: Translate the REAL foreign result into ours when known....
        Map<String,String> bootInfo = new HashMap<>();
        for(Map.Entry<String,Object> entry: info.entrySet()) {
            bootInfo.put(entry.getKey().toLowerCase(), fixNull(entry.getValue()));
            log_.debug("translateBootImageInfo: %s = '%s'", entry.getKey().toLowerCase(), entry.getValue());
        }
        return bootInfo;
    }

    private String fixNull(Object obj) {
        if(obj == null)
            return null;
        else
            return obj.toString();
    }

    private String extractBootImageId(String foreignLocation) {
        // TODO: Coded assuming the FOREIGN_IMAGE_ID_KEY is in the NODE_ONLINE message.
        // NOTE: The foreign boot image ID will be the DAI boot image id.
        try {

            if(client_ == null)
                client_ = createClient();
            BlockingResult result = client_.getRESTRequestBlocking(makeUri(foreignLocation));
            if(result.code != 200)
                throw new RESTClientException("RESTClient resulted in HTTP code: " + result.code +
                        ":" + result.responseDocument);
            PropertyMap bootImageData = parser_.fromString(result.responseDocument).getAsArray().getMap(0);
            return bootImageData.getStringOrDefault("id", "");

        } catch(RESTClientException | URISyntaxException | ConfigIOParseException | PropertyNotExpectedType e) {
            log_.exception(e);
            actions_.logFailedToUpdateBootImageInfo(String.format("Full URL=%s%s", baseUrl_, bootInfoUrlPath_));
        }
        return "mOS";
    }

    private void checkMessage(PropertyMap message) throws NetworkListenerProviderException {
        for(String required: requiredInMessage_)
            if (!message.containsKey(required))
                throw new NetworkListenerProviderException(String.format("Incoming event was missing '%s' in the JSON",
                        required));
    }

    private String bootInfoUrlPath_ = "/bootparameters";
    private boolean informWlm_ = false;
    private boolean publish_ = false;
    private String topic_ = "ucs_boot_event";
    private Logger log_;
    private NetworkListenerConfig config_ = null;
    private SystemActions actions_ = null;
    private AtomicLong targetRequestTimeMs_ = new AtomicLong(0L);
    private ConcurrentMap<String, Object> knownIds_ = new ConcurrentHashMap<>(); // Used as a ConcurrentHashSet...
    private RESTClient client_ = null;
    private String baseUrl_ = null;
    private AtomicBoolean updating_ = new AtomicBoolean(false);

    private final static String FOREIGN_IMAGE_ID_KEY = "bootImageId";
    private final long DELTA_UPDATE_MS = Long.parseLong(System.getProperty("daiBootImagePollingMs",
            "7200000")); // Default is 2 hours.
    private static final Object OBJECT = new Object();

    private ConfigIO parser_;
    private static final String[] requiredInMessage_ = new String[] {"Components", "State", "timestamp"};
    private final static Map<String, BootState> conversionMap_ = new HashMap<>() {{
        put("Ready", BootState.NODE_ONLINE);
        put("Off", BootState.NODE_OFFLINE);
        put("Empty", BootState.EMPTY);
        put("On", BootState.NODE_BOOTING);
    }};

    private final static String FOREIGN_LOCATION_KEY = "Components";
    private final static String ORIG_FOREIGN_LOCATION_KEY = "foreignLocation";
    private final static String FOREIGN_NODE_STATE_KEY = "State";
    private final static String FOREIGN_TIMESTAMP_KEY = "timestamp";
    private final static String FOREIGN_QUERY_PARAM = "?hosts=";
}
