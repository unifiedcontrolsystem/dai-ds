// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.provisioners;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.runtime_utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of class BootEventTransformer.
 */
public class NetworkListenerProviderForeignBus implements NetworkListenerProvider, Initializer {
    public NetworkListenerProviderForeignBus(final Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null)
            throw new RuntimeException("Failed to create a JSON parser for class" + getClass().getCanonicalName());
    }

    @Override
    public void initialize() { /* Not used but is required */ }

    @Override
    public List<CommonDataFormat> processRawStringData(final String subject, final String data, final NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        try {
            if(keywordToNodeStates_ == null)
                keywordToNodeStatesMap(config);

            final List<CommonDataFormat> results = new ArrayList<>();

            //To filter syslog messages
            final String receivedDataType = subscribedTopicMap_.getStringOrDefault(subject, "Unknown");
            final PROVISIONER_TOPICS provisionerTopic = PROVISIONER_TOPICS.valueOf(receivedDataType);
            if(provisionerTopic == PROVISIONER_TOPICS.DhcpLogData) {
                if(!data.contains(DHCP_DISCOVER) && !data.contains(DHCP_REQUEST))
                    return results;
            }

            log_.debug("*** Message: %s", data);
            final boolean isJson = CommonFunctions.isJson(data);
            if(isJson) {
                for(final String json: CommonFunctions.breakupStreamedJSONMessages(data)) {
                    final PropertyMap document = parser_.fromString(json).getAsMap();
                    if(provisionerTopic != PROVISIONER_TOPICS.Unknown) {
                        log_.info("Received %s type data", receivedDataType);
                        if(provisionerTopic == PROVISIONER_TOPICS.DhcpLogData)
                            results.add(processDhcpMessage(document));
                        if(provisionerTopic == PROVISIONER_TOPICS.PowerLogData)
                            results.add(processNodePowerMessage(document));
                        if(provisionerTopic == PROVISIONER_TOPICS.ConsoleLogData)
                            results.add(processConsoleMessage(document));
                        if(provisionerTopic == PROVISIONER_TOPICS.HeartbeatLogData)
                            results.add(processHeartbeatMessage(document));
                    }
                }
            } else {
               //TO-DO for topic non-json format
            }
            return results;
        } catch(Exception e) {
            log_.warn(e.getMessage());
            throw new NetworkListenerProviderException("Failed to parse the message from the component");
        }
    }

    @Override
    public void actOnData(final CommonDataFormat data, final NetworkListenerConfig config, final SystemActions systemActions) {
        if(config_ == null)
            getConfig(config, systemActions);

        final String bootImageId = data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY);
        final long dataTimestamp = data.getNanoSecondTimestamp();

        if(bootImageId != null && !bootImageId.isEmpty()) {
            updateBootTableWithBootImage(data);
            updateNodeBootImageId(data);
        }
        if(data.getStateEvent() != null) {
            actions_.changeNodeStateTo(data.getStateEvent(), data.getLocation(),
                    dataTimestamp, informWlm_);
        }
        if(publish_)
            actions_.publishBootEvent(topic_, data.getStateEvent(), data.getLocation(), dataTimestamp);
    }

    private CommonDataFormat processHeartbeatMessage(PropertyMap heartbeatLogData) throws Exception {
        final String timestamp = getHBTimeStamp(heartbeatLogData);
        final long nsTimestamp = TimeUtils.getNsTimestamp() - Long.parseLong(timestamp) * 1000000;
        final String hostname = getHBNode(heartbeatLogData);

        final BootState bootState = getHBNodeState(heartbeatLogData);

        final CommonDataFormat common = new CommonDataFormat(nsTimestamp, hostname, DataType.StateChangeEvent);
        common.setStateChangeEvent(bootState);
        return common;
    }

    private CommonDataFormat processConsoleMessage(PropertyMap consoleLogData) throws Exception {
        if(consoleLogData.containsKey("message")) {
            final String message = consoleLogData.getStringOrDefault("message", null);
            final boolean isBootImageInfo = message.contains("command line: BOOT_IMAGE");
            if(isBootImageInfo)
                return processBootImageInfo(consoleLogData);
            return processNodeStateInfoFromConsole(consoleLogData);
        }
        throw new Exception("Received data is missing 'message' field:" + consoleLogData);
    }

    private CommonDataFormat processNodeStateInfoFromConsole(PropertyMap nodeStateInfo) throws Exception {
        if(!nodeStateInfo.containsKey("source"))
            throw new Exception("Received data is missing 'source' " + nodeStateInfo);
        final String hostInfo = nodeStateInfo.getString("source");
        final String hostname = hostInfo.substring(hostInfo.lastIndexOf("/") + 1);

        final String message = getMessage(nodeStateInfo);
        final String timestamp = getTimeStamp(message);
        final long nsTimestamp = TimeUtils.nSFromIso8601(TimeUtils.stringToIso8601(timestamp));

        final BootState bootState = getNodeState(message);

        final CommonDataFormat common = new CommonDataFormat(nsTimestamp, hostname, DataType.StateChangeEvent);
        common.setStateChangeEvent(bootState);
        return common;
    }

    private CommonDataFormat processBootImageInfo(PropertyMap consoleLogData) throws Exception {
        final String bootImageInfo = getMessage(consoleLogData);
        final String timestamp = getTimeStamp(bootImageInfo);
        final long nsTimestamp = TimeUtils.nSFromIso8601(TimeUtils.stringToIso8601(timestamp));
        log_.warn("***Processing BOOT_IMAGE information: %s", bootImageInfo);
        if(bootImageInfo.contains("BOOT_IMAGE=") && bootImageInfo.contains("HOSTNAME=")) {
            final String hostname = bootImageInfo.split("HOSTNAME=")[1].split(" ")[0];
            final String bootImageData = bootImageInfo.split("BOOT_IMAGE=")[1].split(" ")[0];
            final String[] subBootImageInfoArray = bootImageData.split("/");

            CommonDataFormat common = new CommonDataFormat(nsTimestamp, hostname, DataType.StateChangeEvent);
            common.setStateChangeEvent(BootState.KERNEL_BOOT_STARTED);
            common.storeExtraData(FOREIGN_IMAGE_ID_KEY, subBootImageInfoArray[subBootImageInfoArray.length - 2]);
            common.storeExtraData(FOREIGN_IMAGE_KERNEL_ARGS_KEY, subBootImageInfoArray[subBootImageInfoArray.length - 1]);
            common.storeExtraData(FOREIGN_IMAGE_DESCRIPTION_KEY, bootImageData);
            common.storeExtraData(FOREIGN_IMAGE_FILE_KEY, bootImageData);
            common.storeExtraData(FOREIGN_BOOTSTRAP_IMAGE_FILE_KEY, bootImageData);
            common.storeExtraData(FOREIGN_NODE_STATE_KEY, "A");
            return common;
        }
        throw new Exception("missing hostname or boot_image info in message: " + bootImageInfo);
    }

    private CommonDataFormat processNodePowerMessage(PropertyMap powerLogData) throws Exception {
        final long nsTimestamp = Long.parseLong(getPowerServiceTimeStamp(powerLogData));
        final String hostname = getPowerServiceNode(powerLogData);

        final BootState bootState = getPowerServiceNodeState(powerLogData);

        final CommonDataFormat common = new CommonDataFormat(nsTimestamp, hostname, DataType.StateChangeEvent);
        common.setStateChangeEvent(bootState);
        return common;
    }

    private CommonDataFormat processDhcpMessage(PropertyMap syslogData) throws Exception {
        final long nsTimestamp = TimeUtils.nSFromIso8601(getTimeStamp(syslogData));
        final String hostname = getHostName(syslogData);
        final String message = getMessage(syslogData);
        final BootState bootState = getNodeState(message);

        final CommonDataFormat common = new CommonDataFormat(nsTimestamp, hostname, DataType.StateChangeEvent);
        common.setStateChangeEvent(bootState);
        return common;
    }

    private BootState getHBNodeState(PropertyMap data) throws Exception {
        if(!data.containsKey("booted"))
            throw new Exception("Received data is missing 'booted' " + data);
        final boolean active = Boolean.parseBoolean(data.getString("booted"));
        return active ? BootState.ACTIVE : BootState.NODE_OFFLINE;
    }

    private BootState getPowerServiceNodeState(PropertyMap data) throws Exception {
        if(!data.containsKey("value"))
            throw new Exception("Received data is missing 'value' " + data);
        final PropertyMap hostData = data.getMap("value");
        if(!hostData.containsKey("string"))
            throw new Exception("Received data is missing 'string' " + data);
        final String state = hostData.getString("string");
        return getNodeState(state);
    }

    private String getHBTimeStamp(PropertyMap data) throws Exception {
        if(!data.containsKey("uptime"))
            throw new Exception("Received data is missing 'uptime' " + data);
        final PropertyMap hostData = data.getMapOrDefault("uptime", new PropertyMap());
        if(!hostData.containsKey("long"))
            return "0";
        return hostData.getString("long");
    }

    private String getPowerServiceTimeStamp(PropertyMap data) throws Exception {
        if(!data.containsKey("timestamp"))
            throw new Exception("Received data is missing 'timestamp' " + data);
        return data.getString("timestamp");
    }

    private String getTimeStamp(PropertyMap data) throws Exception {
        if(!data.containsKey("@timestamp"))
            throw new Exception("Received data is missing '@timestamp' " + data);
        return data.getString("@timestamp");
    }

    private String getTimeStamp(String data) {
        String timestamp = data.substring(data.indexOf("[") + 1, data.indexOf("]"));
/*        final Pattern pattern = Pattern.compile("[A-Za-z]{3} [0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} [0-9]{4}");
        if(!pattern.matcher(timestamp).matches())
            throw new Exception("Received data contains incorrect time format,ex: 'EEE MMM dd HH:mm:ss yyyy'" + data);*/
        return timestamp;
    }

    private String getHBNode(PropertyMap data) throws Exception {
        if(!data.containsKey("node"))
            throw new Exception("Received data is missing 'node' " + data);
        return data.getString("node");
    }

    private String getPowerServiceNode(PropertyMap data) throws Exception {
        if(!data.containsKey("host"))
            throw new Exception("Received data is missing 'host' " + data);
        final PropertyMap hostData = data.getMap("host");
        if(!hostData.containsKey("string"))
            throw new Exception("Received data is missing 'string' " + data);
        return hostData.getString("string");
    }

    private String getHostName(PropertyMap data) throws Exception {
        if(!data.containsKey("host"))
            log_.warn("Received data is missing 'host' %s", data);
        final PropertyMap hostInfo = data.getMapOrDefault("host", new PropertyMap());
        if(!hostInfo.containsKey("name"))
            throw new Exception("Received data is missing 'name' " + data);
        return hostInfo.getString("name");
    }

    private String getMessage(PropertyMap data) throws Exception {
        if(!data.containsKey("message"))
            throw new Exception("Received data is missing 'message' " + data);
        return data.getString("message");
    }

    private BootState getNodeState(String message) {
        String nodeState = "Unknown";
        for(String nodeStateMsg : keywordToNodeStates_.keySet()) {
            if(message.contains(nodeStateMsg)) {
                nodeState = keywordToNodeStates_.getStringOrDefault(nodeStateMsg, "Unknown");
                break;
            }
        }
        return conversionMap_.get(nodeState);
    }

    private void keywordToNodeStatesMap(NetworkListenerConfig config) {
        keywordToNodeStates_ = new PropertyMap();
        final PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        final PropertyMap nodeStatesConfig = map.getMapOrDefault(NODE_STATES, new PropertyMap());
        for(String nodeState : nodeStatesConfig.keySet()) {
            final PropertyArray keywords = nodeStatesConfig.getArrayOrDefault(nodeState, new PropertyArray());
            for(Object keyword : keywords.toArray())
                keywordToNodeStates_.putIfAbsent(keyword.toString(), nodeState);
        }

        subscribedTopicMap_ = map.getMapOrDefault(SUBSCRIBED_TOPIC, new PropertyMap());
        subjectMap_ = config.getSubjectMap();
    }

    private void getConfig(NetworkListenerConfig config, SystemActions systemActions) {
        config_ = config;
        actions_ = systemActions;
        final PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", publish_);
            informWlm_ = map.getBooleanOrDefault("informWorkLoadManager", informWlm_);
            topic_ = map.getStringOrDefault("publishTopic", topic_);
        }
    }

    private void updateNodeBootImageId(CommonDataFormat data) {
        final String bootImageId = data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY);
        log_.debug("Changing node location '%s' to have Boot ID of '%s'", data.getLocation(), bootImageId);
        actions_.changeNodeBootImageId(data.getLocation(), bootImageId);
    }

    String makeInstanceDataForFailedNodeUpdate(CommonDataFormat data) {
        return String.format("ForeignLocation='%s'; UcsLocation='%s'; BootMessage='%s'",
                data.retrieveExtraData(ORIG_FOREIGN_LOCATION_KEY), data.getLocation(), data.getStateEvent().toString());
    }

    private void updateBootTableWithBootImage(CommonDataFormat data) {
        final List<Map<String,String>> bootInfoList = new ArrayList<>();

        final Map<String, String> updateBootImageTableWithBootImageInfo = new HashMap<>(bootImageInfo_);
        updateBootImageTableWithBootImageInfo.put(FOREIGN_IMAGE_ID_KEY, data.retrieveExtraData(FOREIGN_IMAGE_ID_KEY));
        updateBootImageTableWithBootImageInfo.put(FOREIGN_IMAGE_DESCRIPTION_KEY, data.retrieveExtraData(FOREIGN_IMAGE_DESCRIPTION_KEY));
        updateBootImageTableWithBootImageInfo.put(FOREIGN_IMAGE_FILE_KEY, data.retrieveExtraData(FOREIGN_IMAGE_FILE_KEY));
        updateBootImageTableWithBootImageInfo.put(FOREIGN_BOOTSTRAP_IMAGE_FILE_KEY, data.retrieveExtraData(FOREIGN_IMAGE_FILE_KEY));
        updateBootImageTableWithBootImageInfo.put(FOREIGN_IMAGE_KERNEL_ARGS_KEY, data.retrieveExtraData(FOREIGN_IMAGE_KERNEL_ARGS_KEY));
        updateBootImageTableWithBootImageInfo.put(FOREIGN_NODE_STATE_KEY, data.retrieveExtraData(FOREIGN_NODE_STATE_KEY));

        bootInfoList.add(updateBootImageTableWithBootImageInfo);

        actions_.upsertBootImages(bootInfoList);
    }

    private boolean informWlm_ = false;
    private boolean publish_ = false;
    private String topic_ = "ucs_boot_event";
    private final Logger log_;
    private NetworkListenerConfig config_ = null;
    private SystemActions actions_ = null;
    private PropertyMap keywordToNodeStates_;
    private PropertyMap subjectMap_;
    private PropertyMap subscribedTopicMap_;

    private final ConfigIO parser_;
    private final static String ORIG_FOREIGN_LOCATION_KEY = "foreignLocation";

    private final static String NODE_STATES = "nodeStates";
    private final static String SUBSCRIBED_TOPIC = "subscribedTopicMap";

    private final static String FOREIGN_IMAGE_ID_KEY = "id";
    private final static String FOREIGN_IMAGE_DESCRIPTION_KEY = "description";
    private final static String FOREIGN_IMAGE_FILE_KEY = "bootimagefile";
    private final static String FOREIGN_IMAGE_CHECKSUM_KEY = "bootimagechecksum";
    private final static String FOREIGN_IMAGE_OPTIONS_KEY = "bootoptions";
    private final static String FOREIGN_BOOTSTRAP_IMAGE_FILE_KEY = "bootstrapimagefile";
    private final static String FOREIGN_BOOTSTRAP_IMAGE_CHECKSUM_KEY = "bootstrapimagechecksum";
    private final static String FOREIGN_NODE_STATE_KEY = "state";
    private final static String FOREIGN_IMAGE_KERNEL_ARGS_KEY = "kernelargs";
    private final static String FOREIGN_IMAGE_FILES_KEY = "files";

    private final String DHCP_DISCOVER = "DHCPDISCOVER";
    private final String DHCP_REQUEST = "DHCPREQUEST";
    
    private final static Map<String, BootState> conversionMap_ = new HashMap<>() {{
        put("Ready", BootState.NODE_ONLINE);
        put("Off", BootState.NODE_OFFLINE);
        put("On", BootState.NODE_BOOTING);
        put("DHCPDiscovered", BootState.DHCP_DISCOVERED);
        put("IPAddressAssigned", BootState.IP_ADDRESS_ASSIGNED);
        put("BiosStartedDueToReset", BootState.BIOS_STARTED_DUE_TO_RESET);
        put("SelectBootDevice", BootState.SELECTING_BOOT_DEVICE);
        put("PxeDownloadingNbpFile", BootState.PXE_DOWNLOAD_NBP_FILE);
        put("StartingKernelBoot", BootState.KERNEL_BOOT_STARTED);
        put("Active", BootState.ACTIVE);
        put("Shutdown", BootState.SHUTDOWN);
        put("Unknown", BootState.UNKNOWN);
    }};

    private final Map<String, String> bootImageInfo_ = new HashMap<>() {{
                put(FOREIGN_IMAGE_ID_KEY, "");
                put(FOREIGN_IMAGE_DESCRIPTION_KEY, "");
                put(FOREIGN_IMAGE_FILE_KEY, "");
                put(FOREIGN_IMAGE_CHECKSUM_KEY, "");
                put(FOREIGN_IMAGE_OPTIONS_KEY, "");
                put(FOREIGN_BOOTSTRAP_IMAGE_FILE_KEY, "");
                put(FOREIGN_BOOTSTRAP_IMAGE_CHECKSUM_KEY, "");
                put(FOREIGN_NODE_STATE_KEY, "M");
                put(FOREIGN_IMAGE_KERNEL_ARGS_KEY, "");
                put(FOREIGN_IMAGE_FILES_KEY, "");
            }};

    private enum PROVISIONER_TOPICS {
        ConsoleLogData,
        DhcpLogData,
        HeartbeatLogData,
        PowerLogData,
        Unknown
    }
}
