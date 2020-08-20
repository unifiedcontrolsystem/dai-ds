// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Description of class NetworkListenerConfig which stores the configuration for the dai_network_listener component.
 * The actual filename is specified by the application using this component. This class attempts to validate the
 * configuration for valid structure.
 */
public class NetworkListenerConfig {
    public NetworkListenerConfig(AdapterInformation adapter, Logger logger) {
        assert logger != null;
        log_ = logger;
        if(adapter == null)
            throw new RuntimeException("You MUST pass a valid AdapterInformation instance to the constructor.");
        adapter_ = adapter;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to get a JSON parser"); // Cannot happen, for compile
        useDebugPrint_ = Boolean.parseBoolean(System.getProperty(this.getClass().getCanonicalName() + ".DEBUG",
                "false"));
    }

    public void loadFromStream(InputStream stream) throws IOException, ConfigIOParseException {
        config_ = parser_.readConfig(stream).getAsMap();
        String[] allRequired = new String[] {
                "adapterProfiles",
                "subjectMap",
                "providerClassMap",
                "networkStreams"
        };
        for(String required: allRequired)
            if(!config_.containsKey(required))
                throw new ConfigIOParseException(String.format("The key '%s' is missing", required));
        useBenchmarking_ = config_.getBooleanOrDefault("useBenchmarkingActions", false);
        profiles_ = config_.getMapOrDefault("adapterProfiles", new PropertyMap());
        networkStreams_ = config_.getMapOrDefault("networkStreams", new PropertyMap());
        subjectMap_ = config_.getMapOrDefault("subjectMap", new PropertyMap());
        providers_ = config_.getMapOrDefault("providerClassMap", new PropertyMap());
        providerConfigs_ = config_.getMapOrDefault("providerConfigurations", new PropertyMap());
        for(String provider: providers_.keySet())
            log_.debug("Loaded Provider: %s = %s", provider, providers_.getStringOrDefault(provider, null));

        // Run self reference tests on file...
        for(String profileName: profiles_.keySet())
            if(!validateProfile(profileName))
                throw new ConfigIOParseException("JSON failed content reference validation!");
    }

    public boolean useBenchmarking() { return useBenchmarking_; }
    public String getLoggerProvider() { return config_.getStringOrDefault("logProvider", "console"); }
    public AdapterInformation getAdapterInformation() { return adapter_; }
    public String getCurrentProfile() { return currentProfile_; }
    public void setCurrentProfile(String currentProfile) {
        assert currentProfile != null && !currentProfile.isBlank() : "Illegal profile name!";
        if(!profiles_.containsKey(currentProfile))
            throw new IllegalArgumentException();
        currentProfile_ = currentProfile;
    }

    public List<String> getProfileSubjects() {
        checkProfile();
        PropertyMap profile = profiles_.getMapOrDefault(currentProfile_, null); // cannot return null.
        PropertyArray subjects = profile.getArrayOrDefault("subjects", null);
        List<String> result = new ArrayList<>();
        for(Object item: subjects) result.add(item.toString());
        return result;
    }

    public String getProviderName() {
        checkProfile();
        String name = profiles_.getMapOrDefault(currentProfile_, null).getStringOrDefault("adapterProvider", null);
        return providers_.getStringOrDefault(name, null);
    }

    public Collection<String> getProfileStreams() {
        checkProfile();
        PropertyMap profile = profiles_.getMapOrDefault(currentProfile_, new PropertyMap());
        PropertyArray array = profile.getArrayOrDefault("networkStreamsRef", new PropertyArray());
        List<String> result = new ArrayList<>();
        for (Object item : array)
            result.add(item.toString());
        return result;
    }

    public String getNetworkName(String networkStreamName) {
        checkProfile();
        // networkStreamName checked already, its value cannot be null or missing.
        PropertyMap network = networkStreams_.getMapOrDefault(networkStreamName, null);
        assert network != null: "Network stream arguments were unexpectedly null, check the configuration!";
        return network.getStringOrDefault("name", null);
    }

    public PropertyMap getNetworkArguments(String networkStreamName) {
        // networkStreamName checked already, its value cannot be null or missing.
        PropertyMap network = networkStreams_.getMapOrDefault(networkStreamName, null);
        return new PropertyMap(network.getMapOrDefault("arguments", null));
    }

    public String getProviderClassNameFromName(String name) {
        return providers_.getStringOrDefault(name, null);
    }

    public PropertyMap getProviderConfigurationFromClassName(String className) {
        return providerConfigs_.getMapOrDefault(className, null);
    }

    public String getFirstNetworkBaseUrl(boolean useSSL) throws ConfigIOParseException {
        checkProfile();
        String streamName = getProfileStreams().iterator().next();
        String scheme = useSSL?"https":"http";
        PropertyMap arguments = getNetworkArguments(streamName);
        try {
            return String.format("%s://%s:%d", scheme, arguments.getString("connectAddress"),
                    arguments.getInt("connectPort"));
        } catch(PropertyNotExpectedType e) {
            throw new ConfigIOParseException("Failed to get the base URL from the first network stream in the " +
                    "profile: " + currentProfile_, e);
        }
    }

    private void checkProfile() {
        assert currentProfile_ != null:"Current profile not currently set!";
    }

    //////////////////////////////////////////////////////////////////////////
    // Config File Validation section.
    //////////////////////////////////////////////////////////////////////////
    private boolean validateProfile(String profileName) {
        log_.debug("Validating profileName: %s", profileName);
        boolean result = profiles_.containsKey(profileName);
        PropertyMap profile = profiles_.getMapOrDefault(profileName, new PropertyMap());
        result = (result && profile.containsKey("adapterProvider"));
        result = (result && profile.containsKey("networkStreamsRef"));
        result = (result && profile.containsKey("subjects"));
        for (Map.Entry<String, Object> entry : profile.entrySet()) {
            if (!result) break;
            log_.debug("    Validating key: %s", entry.getKey());
            switch (entry.getKey()) {
                case "adapterProvider":
                    result = providers_.containsKey(entry.getValue().toString());
                    break;
                case "networkStreamsRef":
                    result = (entry.getValue() instanceof PropertyArray) && ((PropertyArray)entry.getValue()).size() > 0;
                    if (result)
                        result = validateNetworkStreams((PropertyArray) entry.getValue());
                    break;
                case "subjects":
                    result = (entry.getValue() instanceof PropertyArray) && ((PropertyArray)entry.getValue()).size() > 0;
                    if (result)
                        result = validateSubjects((PropertyArray) entry.getValue());
                    break;
                default:
                    result = false;
            }
        }
        log_.debug("Validating profileName %s results: %s", profileName, result?"OK":"FAILED");
        return result;
    }

    private boolean validateNetworkStreams(PropertyArray networkStreams) {
        boolean result = networkStreams != null && networkStreams.size() > 0;
        if(!result) return false;
        for(Object oSubject: networkStreams) {
            result = oSubject instanceof String && validateNetworkStream(oSubject.toString());
            if(!result) break;
        }
        return result;
    }

    private boolean validateNetworkStream(String networkStream) {
        log_.debug("Validating networkStream: %s", networkStream);
        boolean result = networkStreams_.containsKey(networkStream);
        PropertyMap stream = networkStreams_.getMapOrDefault(networkStream, new PropertyMap());
        result = (result && stream.containsKey("name"));
        result = (result && stream.containsKey("arguments"));
        if(result) {
            for (Map.Entry<String, Object> entry : stream.entrySet()) {
                log_.debug("    Validating key: %s", entry.getKey());
                switch (entry.getKey()) {
                    case "name":
                        result = entry.getValue() instanceof String;
                        break;
                    case "arguments":
                        result = entry.getValue() instanceof PropertyMap;
                        if (result)
                            result = validatePossibleRequestReferences((PropertyMap) entry.getValue());
                        break;
                    default:
                        result = false;
                }
                if (!result) break;
            }
        }
        return result;
    }

    private boolean validatePossibleRequestReferences(PropertyMap arguments) {
        boolean result = true;
        if(arguments.containsKey("requestType")) {
            log_.debug("Validating possible request references: %s", arguments.get("requestType"));
            String value = arguments.getStringOrDefault("requestType", null);
            result = value != null && (value.equals("POST") || value.equals("GET"));
        }
        if(arguments.containsKey("requestBuilder")) {
            log_.debug("Validating possible request references: %s", arguments.get("requestBuilder"));
            String value = arguments.getStringOrDefault("requestBuilder",
                    "com.intel.networking.sink.SSEStreamRequestBuilder");
            result = result || (value != null && providers_.containsKey(value));
        }
        return result;
    }

    private boolean validateSubjects(PropertyArray subjects) {
        log_.debug("Validating subjects...");
        boolean result = subjects != null && subjects.size() > 0;
        if(result)
            for(Object oSubject: subjects) {
                result = oSubject instanceof String && subjectMap_.containsKey(oSubject.toString());
                if(!result) break;
            }
        return result;
    }
    //////////////////////////////////////////////////////////////////////////
    // End of Config File Validation section.
    //////////////////////////////////////////////////////////////////////////

    private boolean useBenchmarking_;
    private Logger log_;
    private ConfigIO parser_;
    private PropertyMap config_;
    private PropertyMap networkStreams_;
    private PropertyMap subjectMap_;
    private PropertyMap providers_;
    private PropertyMap profiles_;
    private PropertyMap providerConfigs_;
    private String currentProfile_ = null;
    private AdapterInformation adapter_;

    private boolean useDebugPrint_; // set to "true" on adapter launch to debug a configuration file.
}
