// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.provisioners;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
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
            List<CommonDataFormat> results = new ArrayList<>();
            log_.debug("*** Message: %s", data);
            for(String json: CommonFunctions.breakupStreamedJSONMessages(data)) {
                PropertyMap document = parser_.fromString(json).getAsMap();
                if(!document.containsKey("metrics"))
                    log_.warn("Message is missing 'metrics': %s", json);
                PropertyMap metrics = document.getMapOrDefault("metrics", new PropertyMap());
                if(!metrics.containsKey("messages"))
                    log_.warn("Message is missing 'metrics.messages': %s", json);
                PropertyArray messages = metrics.getArrayOrDefault("messages", new PropertyArray());
                for (Object o : messages) {
                    if (o instanceof PropertyMap) {
                        PropertyMap message = (PropertyMap) o;
                        if(!message.containsKey("Components"))
                            log_.warn("Message is missing 'metrics.messages[Components]': %s", json);
                        PropertyArray locations = message.getArrayOrDefault("Components", new PropertyArray());
                        for (int i = 0; i < locations.size(); i++) {
                            try {
                                String location = CommonFunctions.convertForeignToLocation(locations.getString(i));
                                String eventState = message.getString("State");
                                if(!conversionMap_.containsKey(eventState))
                                    log_.warn("No conversion state, Ignoring sc notification for eventState = %s", eventState);
                                BootState state = conversionMap_.get(eventState);
                                Instant now = Instant.now();
                                long ts = (now.getEpochSecond() * 1_000_000_000L) + now.getNano();
                                CommonDataFormat common = new CommonDataFormat(ts, location, DataType.StateChangeEvent);
                                common.setStateChangeEvent(state);
                                common.storeExtraData("Flag", message.getStringOrDefault("Flag", "Unknown"));
                                common.storeExtraData(ORIG_FOREIGN_LOCATION_KEY, locations.getString(i));
                                results.add(common);
                            } catch (ConversionException | PropertyNotExpectedType e) {
                                log_.warn("Single location state message failed so skipping it: %s", e.getMessage());
                            }
                        }
                    } else {
                        log_.warn("A message is expected to be an map object but was not: %s",
                                o.getClass().getCanonicalName());
                    }
                }
            }
            return results;
        } catch(ConfigIOParseException e) {
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
        if(data.getStateEvent() != null) {
            actions_.changeNodeStateTo(data.getStateEvent(), data.getLocation(),
                    dataTimestamp, informWlm_);
        }
        if(data.getStateEvent() == BootState.NODE_ONLINE) {
            try {
                data.storeExtraData(FOREIGN_IMAGE_ID_KEY,
                        extractBootImageId(data.retrieveExtraData(ORIG_FOREIGN_LOCATION_KEY)));
            } catch (Exception e) {
                log_.error(e.getMessage());
            }
            updateNodeBootImageId(data);
        }

        if(publish_)
            actions_.publishBootEvent(topic_, data.getStateEvent(), data.getLocation(), dataTimestamp);
    }

    private void getConfig(NetworkListenerConfig config, SystemActions systemActions) {
        config_ = config;
        actions_ = systemActions;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", publish_);
            bootParametersInfoUrl_ = map.getStringOrDefault("bootParametersInfoUrl", bootParametersInfoUrl_);
            bootParameterForLocationInfoUrl_ = map.getStringOrDefault("bootParameterForLocationInfoUrl", bootParameterForLocationInfoUrl_);
            bootImageInfoUrl_ = map.getStringOrDefault("bootImageInfoUrl", bootImageInfoUrl_);
            bootImageForImageIdInfoUrl_ = map.getStringOrDefault("bootImageForImageIdInfoUrl", bootImageForImageIdInfoUrl_);
            informWlm_ = map.getBooleanOrDefault("informWorkLoadManager", informWlm_);
            topic_ = map.getStringOrDefault("publishTopic", topic_);
        }
    }

    private void updateNodeBootImageId(CommonDataFormat data) {
        String bootImageId = data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY);
        if(bootImageId.equals("")) {
            log_.error("Failed to update node boot image ID for location '%s'", data.getLocation());
            actions_.logFailedToUpdateNodeBootImageId(data.getLocation(), makeInstanceDataForFailedNodeUpdate(data));
        } else {
            log_.debug("Changing node location '%s' to have Boot ID of '%s'", data.getLocation(), bootImageId);
            actions_.changeNodeBootImageId(data.getLocation(), bootImageId);
        }
    }

    String makeInstanceDataForFailedNodeUpdate(CommonDataFormat data) {
        return String.format("ForeignLocation='%s'; UcsLocation='%s'; BootMessage='%s'",
                data.retrieveExtraData(ORIG_FOREIGN_LOCATION_KEY), data.getLocation(), data.getStateEvent().toString());
    }
    
    private void updateBootImageTable() {
        if(updating_.get())
            return;
        updating_.set(true);

        try {
            updateBootTableWithBootImage();
            updateBootTableWithBootParams();
            targetRequestTimeMs_.set(Instant.now().toEpochMilli() + DELTA_UPDATE_MS);
        } catch (PropertyNotExpectedType e) {
            log_.warn("unable to update boot image table");
        }
        updating_.set(false);
    }

    private void updateBootTableWithBootImage() throws PropertyNotExpectedType {
        List<Map<String,String>> bootInfoList = new ArrayList<>();

        //send request for boot images
        log_.debug("Sending Boot Images URL request");
        PropertyDocument bootImagesApiData = sendForeignApiRequest(bootImageInfoUrl_);
        if(bootImagesApiData == null) {
            return ;
        }

        bootImagesInfo_ = bootImagesApiData.getAsArray();
        log_.debug("BOOT IMAGES DATA = ", bootImagesInfo_);

        for(int i = 0; i < bootImagesInfo_.size(); i++) {
            PropertyMap bootImageInfo = bootImagesInfo_.getMap(i);

            Map<String, String> updateBootImageTableWithBootImageInfo = new HashMap<>(bootImageInfo_);
            updateBootImageTableWithBootImageInfo.put("bootimagefile", bootImageInfo.getStringOrDefault("kernel", ""));
            updateBootImageTableWithBootImageInfo.put("bootstrapimagefile", bootImageInfo.getStringOrDefault("initrd", ""));
            updateBootImageTableWithBootImageInfo.put("kernelargs", bootImageInfo.getStringOrDefault("params", ""));
            updateBootImageTableWithBootImageInfo.put("id", bootImageInfo.getStringOrDefault("id", ""));
            updateBootImageTableWithBootImageInfo.put("description", bootImageInfo.getStringOrDefault("name", ""));

            bootInfoList.add(updateBootImageTableWithBootImageInfo);
        }

        actions_.upsertBootImages(bootInfoList);
    }

    private void updateBootTableWithBootParams() throws PropertyNotExpectedType {
        List<Map<String,String>> updateBootTable = new ArrayList<>();

        //send request for boot parameters
        log_.debug("Sending Boot Parameters URL request");
        PropertyDocument bootParametersApiData = sendForeignApiRequest(bootParametersInfoUrl_);
        if(bootParametersApiData == null) {
            return ;
        }

        PropertyArray bootParametersInfo = bootParametersApiData.getAsArray();
        log_.debug("BOOT PARAMS DATA = ", bootParametersInfo);

        for(int i = 0; i < bootParametersInfo.size(); i++) {
            PropertyMap bootParameterInfo = bootParametersInfo.getMap(i);
            if(!bootParameterInfo.containsKey("hosts"))
                continue;

            Map<String, String> updateBootImageTableWithBootParamInfo = new HashMap<>(bootImageInfo_);
            updateBootImageTableWithBootParamInfo.put("bootimagefile", bootParameterInfo.getStringOrDefault("kernel", ""));
            updateBootImageTableWithBootParamInfo.put("bootstrapimagefile", bootParameterInfo.getStringOrDefault("initrd", ""));
            updateBootImageTableWithBootParamInfo.put("kernelargs", bootParameterInfo.getStringOrDefault("params", ""));

            String bootImageId = fetchBootImageId(bootParameterInfo);
            updateBootImageTableWithBootParamInfo.put("id", bootImageId);

            PropertyArray bootImageInfoApiData = bootImagesInfo_;
            if(bootImageInfoApiData.isEmpty()) {
                log_.warn("Unable to get data for Image Id = %s", bootImageId);
                return;
            }

            PropertyMap bootImageIdMap = fetchBootImageIdInfo(bootImageInfoApiData, bootImageId);
            String bootImageName = fetchBootImage(bootImageIdMap);
            updateBootImageTableWithBootParamInfo.put("description", bootImageName);

            if(!bootParameterInfo.getString("params").isEmpty())
                updateBootImageTableWithBootParamInfo.put("state", "A");

            updateBootTable.add(updateBootImageTableWithBootParamInfo);
            knownIds_.putIfAbsent(updateBootImageTableWithBootParamInfo.get("id"), OBJECT);
        }

        actions_.upsertBootImages(updateBootTable);
    }

    protected RESTClient createClient() throws RESTClientException { // protected for test mocking.
        RESTClient client = RESTClientFactory.getInstance("apache", log_);
        if (client == null)
            throw new RESTClientException("Failed to get the REST client implementation");
        return client;
    }

    private void createTokenProvider() {
        for(String networkStreamName : config_.getProfileStreams()) {
            PropertyMap args = config_.getNetworkArguments(networkStreamName);
            if (args.getStringOrDefault("tokenAuthProvider", null) != null) {
                Map<String, String> map = new HashMap<>();
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    log_.debug("REST Client args: %s = %s", entry.getKey(), entry.getValue().toString());
                    map.put(entry.getKey(), entry.getValue().toString());
                }
                createTokenProvider(map.get("tokenAuthProvider"), map);
                client_.setTokenOAuthRetriever(tokenProvider_);
            }
        }
    }

    private void createTokenProvider(String className, Map<String,String> config) {
        try {
            Class<?> classObj = Class.forName(className);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            tokenProvider_ = (TokenAuthentication) ctor.newInstance();
            tokenProvider_.initialize(log_, config);
        } catch(ClassNotFoundException e) {
            log_.exception(e, String.format("Missing TokenAuthentication implementation '%s'", className));
        } catch (NoSuchMethodException e) {
            log_.exception(e, String.format("Missing public constructor for TokenAuthentication implementation '%s'",
                    className));
        } catch (IllegalAccessException e) {
            log_.exception(e, String.format("Default constructor for TokenAuthentication implementation " +
                    "'%s' must be public", className));
        } catch (InstantiationException | InvocationTargetException | TokenAuthenticationException e) {
            log_.exception(e, String.format("Cannot construct TokenAuthentication implementation '%s'", className));
        }
    }

    private URI makeUri(String uri, String argument) throws URISyntaxException {
        return new URI(String.format("%s%s", uri, argument));
    }

    private URI makeUri(String uri) throws URISyntaxException {
        return new URI(String.format("%s", uri));
    }

    private String extractBootImageId(String foreignLocation) {
        // The foreign boot image ID will be the DAI boot image id.
        try {

            //send request for boot parameters and identify boot-image-id
            log_.info("Sending Boot Parameters URL request for foreignLocation = %s", foreignLocation);
            PropertyDocument bootParametersForLocationApiData = sendForeignApiRequest(bootParameterForLocationInfoUrl_, foreignLocation);
            if(bootParametersForLocationApiData == null) {
                return "";
            }

            PropertyMap bootParametersForLocation = bootParametersForLocationApiData.getAsArray().getMap(0);
            log_.info("BOOT PARAMS DATA = ", bootParametersForLocation);
            String bootImageId = fetchBootImageId(bootParametersForLocation);
            log_.info("boot-image-id for foreignLocation = %s, id = ", foreignLocation, bootImageId);

            //send boot-images request using the image-id
            log_.info("Sending Boot Image URL request for id = %s", bootImageId);
            PropertyDocument bootImageInfoForIdApiData = sendForeignApiRequest(bootImageForImageIdInfoUrl_, bootImageId);
            if(bootImageInfoForIdApiData == null) {
                return  "";
            }

            PropertyMap bootImageInfoForId = bootImageInfoForIdApiData.getAsMap();
            log_.info("BOOT IMAGE DATA = ", bootImageInfoForId);
            String bootImageName = fetchBootImage(bootImageInfoForId);
            log_.info("BOOT IMAGE NAME = ", bootImageName);

            return bootImageId;

        } catch(PropertyNotExpectedType e) {
            log_.exception(e);
            actions_.logFailedToUpdateBootImageInfo(String.format("Boot Parameter URL=%s, Boot Image URL=%s",
                    bootParameterForLocationInfoUrl_, bootImageForImageIdInfoUrl_));
        }
        return "";
    }

    private PropertyDocument sendForeignApiRequest(String url, String argument) {
        String requestedUrl = "";
        try {

            if(client_ == null) {
                client_ = createClient();
                createTokenProvider();
            }

            URI apiReqURI = makeUri(url, argument);
            requestedUrl = apiReqURI.toURL().toString();
            log_.info("URL= %s", requestedUrl);
            BlockingResult result = client_.getRESTRequestBlocking(apiReqURI);

            if(result.code != 200) {
                throw new RESTClientException("RESTClient resulted in HTTP code: " + result.code +
                        ":" + result.responseDocument);
            }
            return parser_.fromString(result.responseDocument);

        } catch(RESTClientException | ConfigIOParseException | MalformedURLException | URISyntaxException e) {
            log_.exception(e);
            actions_.logFailedToUpdateBootImageInfo(String.format("Full URL=%s", requestedUrl));
        }
        return null;
    }

    private PropertyDocument sendForeignApiRequest(String url) {
        String requestedUrl = "";
        try {

            if(client_ == null) {
                client_ = createClient();
                createTokenProvider();
            }

            URI apiReqURI = makeUri(url);
            requestedUrl = apiReqURI.toURL().toString();
            log_.info("URL= %s", requestedUrl);
            BlockingResult result = client_.getRESTRequestBlocking(apiReqURI);

            if(result.code != 200) {
                throw new RESTClientException("RESTClient resulted in HTTP code: " + result.code +
                        ":" + result.responseDocument);
            }
            return parser_.fromString(result.responseDocument);

        } catch(RESTClientException | ConfigIOParseException | MalformedURLException | URISyntaxException e) {
            log_.exception(e);
            actions_.logFailedToUpdateBootImageInfo(String.format("Full URL=%s", requestedUrl));
        }
        return null;
    }

    private String fetchBootImageId(PropertyMap bootParamsData) {
        if(!bootParamsData.containsKey("kernel")) {
            log_.warn("missing kernel information in data : ", bootParamsData);
            return "";
        }

        String kernelInfo = bootParamsData.getStringOrDefault("kernel", "");
        log_.info("kernel data = ", kernelInfo);
        String[] kernelDataArray = kernelInfo.split("/");
        List<String> kernelDataParts = new ArrayList<>(Arrays.asList(kernelDataArray));
        int bootImagesIndex = kernelDataParts.lastIndexOf("boot-images");
        if( (bootImagesIndex+1) > kernelDataParts.size()) {
            log_.warn("missing 'boot-image' in data : ", bootParamsData);
            return "";
        }
        String bootImageId = kernelDataParts.get(bootImagesIndex + 1);
        log_.info("boot-image-id = ", bootImageId);
        return bootImageId;
    }

    private String fetchBootImage(PropertyMap bootImageData) {
        if(!bootImageData.containsKey("name")) {
            log_.warn("missing 'name' information in data : ", bootImageData);
            return "";
        }

        String bootImage = bootImageData.getStringOrDefault("name", "");
        log_.info("boot-image-name = ", bootImage);
        return bootImage;
    }

    private PropertyMap fetchBootImageIdInfo(PropertyArray bootImagesInfo, String bootImageId) throws PropertyNotExpectedType {
        for(int index = 0; index < bootImagesInfo.size(); index++) {
            PropertyMap bootImageIdInfo = bootImagesInfo.getMap(index);
            if(bootImageIdInfo.containsKey("id") && bootImageIdInfo.getString("id").equals(bootImageId))
                return bootImageIdInfo;
        }
        return new PropertyMap();
    }

    private String bootParametersInfoUrl_ = "/apis/bss/boot/v1/bootparameters";
    private String bootParameterForLocationInfoUrl_ = "/apis/bss/boot/v1/bootparameters?name=";
    private String bootImageInfoUrl_ = "/apis/ims/images";
    private String bootImageForImageIdInfoUrl_ = "/apis/ims/images/*";
    private boolean informWlm_ = false;
    private boolean publish_ = false;
    private String topic_ = "ucs_boot_event";
    private final Logger log_;
    private NetworkListenerConfig config_ = null;
    private SystemActions actions_ = null;
    private final AtomicLong targetRequestTimeMs_ = new AtomicLong(0L);
    private final ConcurrentMap<String, Object> knownIds_ = new ConcurrentHashMap<>(); // Used as a ConcurrentHashSet...
    private RESTClient client_ = null;
    final AtomicBoolean updating_ = new AtomicBoolean(false);

    private final static String FOREIGN_IMAGE_ID_KEY = "bootImageId";
    private final long DELTA_UPDATE_MS = Long.parseLong(System.getProperty("daiBootImagePollingMs",
            "7200000")); // Default is 2 hours.
    private static final Object OBJECT = new Object();

    private final ConfigIO parser_;
    private final static Map<String, BootState> conversionMap_ = new HashMap<>() {{
        put("Ready", BootState.NODE_ONLINE);
        put("Off", BootState.NODE_OFFLINE);
        put("Empty", BootState.EMPTY);
        put("On", BootState.NODE_BOOTING);
    }};

    private Map<String, String> bootImageInfo_ = new HashMap<>() {{
                put("id", "");
                put("description", "");
                put("bootimagefile", "");
                put("bootimagechecksum", "");
                put("bootoptions", "");
                put("bootstrapimagefile", "");
                put("bootstrapimagechecksum", "");
                put("state", "M");
                put("kernelargs", "");
                put("files", "");
            }};


    private final static String ORIG_FOREIGN_LOCATION_KEY = "foreignLocation";
    private TokenAuthentication tokenProvider_ = null;
    private PropertyArray bootImagesInfo_ = new PropertyArray();
}
