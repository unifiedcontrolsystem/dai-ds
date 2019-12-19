// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring_providers;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.BootState;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.partitioned_monitor.*;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description of class BootEventActions.
 */
public class BootEventActions implements DataAction, Initializer {
    public BootEventActions(Logger logger) {
        log_ = logger;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void actOnData(CommonDataFormat data, PartitionedMonitorConfig config, SystemActions systemActions) {
        if(config_ == null)
            getConfig(config, systemActions);
        String bootImageId = data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY);
        boolean matched = bootImageId == null || bootImageId.isBlank() || knownIds_.containsKey(bootImageId);
        if(!matched || Instant.now().toEpochMilli() >= targetRequestTimeMs_.get())
            new Thread(this::updateBootImageTable).start(); // Background updates of table...
        long dataTimestamp = data.getNanoSecondTimestamp();
        actions_.changeNodeStateTo(data.getStateEvent(), data.getLocation(),
                dataTimestamp, informWlm_);
        if(data.getStateEvent() == BootState.NODE_ONLINE)
            updateNodeBootImageId(data);
        if(data.getStateEvent() == BootState.NODE_OFFLINE)
            actions_.storeRasEvent("RasMntrForeignNodeFailed", "No reason given by foreign software",
                    data.getLocation(), dataTimestamp);
        if(publish_)
            actions_.publishBootEvent(topic_, data.getStateEvent(), data.getLocation(), dataTimestamp);
    }

    private void getConfig(PartitionedMonitorConfig config, SystemActions systemActions) {
        config_ = config;
        actions_ = systemActions;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", publish_);
            bootInfoUrlPath_ = map.getStringOrDefault("bootImageInfoUrlPath", bootInfoUrlPath_);
            informWlm_ = map.getBooleanOrDefault("doActions", informWlm_);
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
            // TODO: Replace with foreign API call if required to get bootImageId based on xnameLocation.
            log_.error("Failed to update node boot image ID for location '%s'", data.getLocation());
            actions_.logFailedToUpdateNodeBootImageId(data.getLocation(), makeInstanceDataForFailedNodeUpdate(data));
        } else {
            log_.debug("Changing node location '%s' to have Boot ID of '%s'", data.getLocation(), bootImageId);
            actions_.changeNodeBootImageId(data.getLocation(), bootImageId);
        }
    }

    private String makeInstanceDataForFailedNodeUpdate(CommonDataFormat data) {
        return String.format("ForeignLocation='%s'; UcsLocation='%s'; BootMessage='%s'",
                data.retrieveExtraData("xnameLocation"), data.getLocation(), data.getStateEvent().toString());
    }

    private void updateBootImageTable() {
        if(updating_.get())
            return;
        updating_.set(true);
        try {
            if(client_ == null)
                client_ = createClient();
            BlockingResult result = client_.getRESTRequestBlocking(makeUri());
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

    private URI makeUri() throws URISyntaxException {
        return new URI(String.format("%s%s", baseUrl_, bootInfoUrlPath_));
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
        for(String key: info.keySet()) {
            bootInfo.put(key.toLowerCase(), fixNull(info.get(key)));
            log_.debug("translateBootImageInfo: %s = '%s'", key.toLowerCase(), bootInfo.get(key));
        }
        return bootInfo;
    }

    private String fixNull(Object obj) {
        if(obj == null)
            return null;
        else
            return obj.toString();
    }

    private String bootInfoUrlPath_ = "/bootparameters";
    private boolean informWlm_ = false;
    private boolean publish_ = false;
    private String topic_ = "ucs_boot_event";
    private Logger log_;
    private PartitionedMonitorConfig config_ = null;
    private SystemActions actions_ = null;
    private AtomicLong targetRequestTimeMs_ = new AtomicLong(0L);
    private ConcurrentMap<String, Object> knownIds_ = new ConcurrentHashMap<>(); // Used as a ConcurrentHashSet...
    private RESTClient client_ = null;
    private String baseUrl_ = null;
    private AtomicBoolean updating_ = new AtomicBoolean(false);

    private final static String FOREIGN_IMAGE_ID_KEY = "bootImageId";
    private final long DELTA_UPDATE_MS = Long.valueOf(System.getProperty("daiBootImagePollingMs",
            "7200000")); // Default is 2 hours.
    private static final Object OBJECT = new Object();
}
