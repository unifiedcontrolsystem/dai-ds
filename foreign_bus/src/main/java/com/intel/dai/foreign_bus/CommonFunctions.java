// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.foreign_bus;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.runtime_utils.TimeUtils;
import com.intel.xdg.XdgConfigFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description of class CommonFunctions.
 */
final public class CommonFunctions {
    /**
     * Convert a string timestamp to a nano second long from epoch.
     *
     * @param timestamp String timestamp in ISO format.
     * @return The long representing the timestamp in nano seconds.
     * @throws ParseException If the date is not of the form yyyy-MM-dd HH:mm:ss.SSSX
     */
    public static long convertISOToLongTimestamp(String timestamp) throws ParseException {
        return TimeUtils.nSFromIso8601(timestamp);
    }

    /**
     * Convert a foreign location with sub-node information to a DAI location.
     *
     * @param foreignLocation The foreign location string.
     * @param otherArgs Args to help determine sub-node location information (1st=sensor name; 2nd=extra info).
     * @return The complete DAI location string.
     * @throws ConversionException If the foreign location is not recognized.
     */
    public static String convertForeignToLocation(String foreignLocation, String... otherArgs)
            throws ConversionException {
        if(nodeMap_ == null) {
            try {
                loadCall_.loadMaps();
            } catch(RuntimeException e) { /* Failed, drop to finally */ }
            if(nodeMap_ == null)
                nodeMap_ = new PropertyMap();
        }
        if(foreignLocation.equals("all"))
            return foreignLocation;
        String sensorName = (otherArgs.length > 0) ? otherArgs[0] : null;
        String extraLocation = (otherArgs.length > 1) ? otherArgs[1] : null;
        StringBuilder builder = new StringBuilder();
        try {
            if(nodeMap_.containsKey(foreignLocation)) {
                    builder.append(nodeMap_.getString(foreignLocation));
                    processName(builder, sensorName, extraLocation);
            } else
                throw new ConversionException(String.format("The foreign '%s' was not in the conversion map!",
                        foreignLocation));
        } catch(PropertyNotExpectedType e) {
            throw new ConversionException("Its possible the resource file for foreign translation has been corrupted",
                    e);
        }
        return builder.toString();
    }

    /**
     * Convert a DAI location to foreign location.
     *
     * @param daiLocation The DAI location string. May contain sub-component information.
     * @return The basic foreign location.
     * @throws ConversionException If the foreign location is not recognized.
     */
    public static String convertLocationToForeign(String daiLocation) throws ConversionException {
        if(nodeMap_ == null)
            loadCall_.loadMaps();
        if(daiLocation.equals("all"))
            return daiLocation;
        daiLocation = reduceDaiLocation(daiLocation);
        try {
            if (reverseNodeMap_.containsKey(daiLocation))
                return reverseNodeMap_.getString(daiLocation);
            else {
                throw new ConversionException(String.format("The DAI location string '%s' was not found " +
                        "in the conversion map!", daiLocation));
            }
        } catch(PropertyNotExpectedType e) {
            throw new ConversionException("Its possible the resource file for foreign translation has been corrupted",
                    e);
        }
    }

    /**
     * Fetch all foreign locations mapped to DAI locations
     * @return all foreign location information
     */
    public static Set<String> getForeignLocations() {
        if(nodeMap_ == null)
            loadCall_.loadMaps();
        return (nodeMap_ != null ? nodeMap_.keySet() : null);
    }

    /**
     * Fetch all DAI locations in system
     * @return all dai locations information
     */
    public static Collection<Object> getLocations() {
        if(nodeMap_ == null)
            loadCall_.loadMaps();
        return (nodeMap_ != null ? nodeMap_.values() : null);
    }

    /**
     * Parse a foreign stream down the to leaf level then calling back to parse the leaf. Will not parse state changes.
     *
     * @param jsonStream The JSON object from the stream of objects...
     * @throws ConfigIOParseException if any unexpected syntax or context is detected.
     */
    public static PropertyArray parseForeignTelemetry(String jsonStream) throws ConfigIOParseException {
        PropertyArray allLeafs = new PropertyArray();
        List<String> jsonObjects = breakupStreamedJSONMessages(jsonStream);
        for(String singleMessage: jsonObjects) {
            PropertyMap streamObject = parser_.fromString(singleMessage).getAsMap();
            if(!streamObject.containsKey("metrics"))
                throw new ConfigIOParseException("Stream object is missing the 'metrics' key");
            PropertyMap metrics = streamObject.getMapOrDefault("metrics", null);
            if(metrics == null)
                throw new ConfigIOParseException("The key 'metrics' is set to 'null'");
            if(!metrics.containsKey("messages"))
                throw new ConfigIOParseException("The key 'messages' does not exist in the 'metrics' object");
            PropertyArray messages = metrics.getArrayOrDefault("messages", new PropertyArray());
            for(int i = 0; i < messages.size(); i++) {
                PropertyMap message;
                try {
                    message = messages.getMap(i);
                } catch(PropertyNotExpectedType e) {
                    throw new ConfigIOParseException("Expected only objects under 'messages'");
                }
                processMessage(message, allLeafs);
            }
        }
        return allLeafs;
    }

    private static void processMessage(PropertyMap message, PropertyArray allLeafs) throws ConfigIOParseException {
        if(message.containsKey("Events"))
            processEvents(message.getArrayOrDefault("Events", new PropertyArray()), allLeafs);
        else
            throw new ConfigIOParseException("Missing key 'Events' in the 'message' object");
    }

    private static void processEvents(PropertyArray events, PropertyArray allLeafs) throws ConfigIOParseException {
        for(int i = 0; i < events.size(); i++) {
            PropertyMap event;
            try {
                event = events.getMap(i);
            } catch(PropertyNotExpectedType e) {
                throw new ConfigIOParseException("The event object in the 'Events' array is not a object");
            }
            processEvent(event, allLeafs);
        }
    }

    private static void processEvent(PropertyMap event, PropertyArray allLeafs) throws ConfigIOParseException {
        String prefix = event.getStringOrDefault("MessageId", "Missing.Id");
        if(!event.containsKey("Oem"))
            throw new ConfigIOParseException("Missing the 'Oem' key in the event in 'Events'");
        PropertyMap oem = event.getMapOrDefault("Oem", null);
        if(oem == null)
            throw new ConfigIOParseException("The 'Oem' key is defined as 'null' in the event");
        if(!oem.containsKey("Sensors"))
            throw new ConfigIOParseException("Missing the 'Sensors' key in the 'Oem' key");
        PropertyArray sensors = oem.getArrayOrDefault("Sensors", new PropertyArray());
        for(int i = 0; i < sensors.size(); i++) {
            PropertyMap leaf;
            try {
                leaf = sensors.getMap(i);
            } catch(PropertyNotExpectedType e) {
                throw new ConfigIOParseException("The actual data under the 'Sensors' array was not an object");
            }
            String fullName = prefix + ".";
            fullName += leaf.getStringOrDefault("PhysicalContext", "PhysicalContextMissing");
            fullName += "." + leaf.getStringOrDefault("DeviceSpecificContext", "DeviceSpecificContextMissing");
            leaf.put("__FullName__", fullName);
            allLeafs.add(leaf);
        }
    }

    /**
     * Used to break up a stream of JSON objects into individual JSON objects.
     *
     * @param data THe String containing a possible stream of JSON Objects.
     * @return A list of Strings representing each of the JSON objects.
     */
    public static List<String> breakupStreamedJSONMessages(String data) {
        List<String> jsonMessages = new ArrayList<>();
        int braceDepth = 0;
        int startOfMessage = 0;
        boolean inQuote = false;
        char chr;
        for(int index = 0; index < data.length(); index++) {
            chr = data.charAt(index);
            if(chr == '"')
                inQuote = toggleQuote(data, index, inQuote);
            if(inQuote || Character.isWhitespace(chr))
                continue;
            if(chr == '{')
                braceDepth++;
            else if(chr == '}')
                braceDepth--;
            if(braceDepth == 0) { // new message found
                jsonMessages.add(data.substring(startOfMessage, index + 1).trim());
                startOfMessage = index + 1;
            }
        }
        return jsonMessages;
    }

    private static boolean toggleQuote(String data, int index, boolean inQuote) {
        if(inQuote && data.charAt(index - 1) == '\\')
            return inQuote;
        return !inQuote;
    }

    private static String reduceDaiLocation(String location) {
        if(Pattern.compile("^R[0-9]+-CH[0-9]+-CN[0-9]+-.*").matcher(location).matches()) {
            String[] parts = location.split("-");
            return String.join("-", Arrays.copyOf(parts, 3));
        }
        return location;
    }

    static void loadConversionsMaps() {
        XdgConfigFile locator = new XdgConfigFile("ucs", true);
        String filename = locator.FindFile("LocationTranslationMap.json");
        if (filename != null) {
            try (InputStream stream = new FileInputStream(filename)) {
                if(loadConversionsMapsfromStream(stream))
                    return;
            } catch (IOException e) { /* Fall back to loading from resources. */ }
        }
        InputStream stream = CommonFunctions.class.getResourceAsStream("/resources/LocationTranslationMap.json");
        if (stream == null)
            throw new RuntimeException("Failed to open the location name transformer resource " +
                    "'/resources/LocationTranslationMap.json'");
        if(!loadConversionsMapsfromStream(stream))
            throw new RuntimeException("Failed to load the location name transformer resource " +
                    "'/resources/LocationTranslationMap.json'");
    }

    private static boolean loadConversionsMapsfromStream(InputStream stream) {
        try {
            ConfigIO parser = ConfigIOFactory.getInstance("json");
            assert parser != null: "Failed to get the JSON parser!";
            PropertyMap root = parser.readConfig(stream).getAsMap();
            nodeMap_ = root.getMap("conversion_node_map");
            sensorCpuPattern_ = root.getString("sensor_embedded_cpu_pattern");
            sensorDimmPattern_ = root.getString("sensor_embedded_dimm_pattern");
            sensorChannelPattern_ = root.getString("sensor_embedded_channel_pattern");
            reverseNodeMap_ = new PropertyMap();
            for(String key: nodeMap_.keySet()) {
                nodeMap_.put(key, nodeMap_.getString(key).toUpperCase());
                reverseNodeMap_.put(nodeMap_.getString(key), key);
            }
            return true;
        } catch(IOException | ConfigIOParseException | PropertyNotExpectedType e) {
            return false;
        } finally {
            try { stream.close(); } catch(IOException ec) { /* Nothing to do if close fails. */ }
        }
    }

    private static void processName(StringBuilder builder, String sensorName, String extra) {
        if(sensorName == null) return;
        NodeOrCpuInfo cpuInfo = matchCpu(sensorName);
        if(cpuInfo != null) {
            builder.append(String.format("-CPU%d", cpuInfo.number));
        }
        DimmInfo dimmInfo = matchDimm(sensorName);
        if(dimmInfo != null) {
            builder.append(String.format("-CH%d-DIMM%d", dimmInfo.dimmChannel, dimmInfo.dimmNumber));
        }
        builder.append("-").append(sensorName);
        if(extra != null)
            builder.append("-").append(extra);
    }

    private static NodeOrCpuInfo matchCpu(String str) {
        Pattern regex = Pattern.compile(sensorCpuPattern_);
        Matcher matcher = regex.matcher(str);
        if(!matcher.find()) return null;
        return new NodeOrCpuInfo(Integer.parseInt(str.substring(matcher.start() + 4, matcher.end())),
                str.substring(0, matcher.start()) + str.substring(matcher.end()));
    }

    private static DimmInfo matchDimm(String str) {
        Pattern regex = Pattern.compile(sensorDimmPattern_);
        Matcher dimmMatcher = regex.matcher(str);
        if(!dimmMatcher.find()) return null;
        Pattern regex2 = Pattern.compile(sensorChannelPattern_);
        Matcher channelMatcher = regex2.matcher(str);
        if(!channelMatcher.find()) return null;
        return new DimmInfo(Integer.parseInt(str.substring(channelMatcher.start() + 3, channelMatcher.end())),
                Integer.parseInt(str.substring(dimmMatcher.start() + 5, dimmMatcher.end())),
                str.substring(0, channelMatcher.start()) + str.substring(dimmMatcher.end()));
    }

    // For testing only....
    static void clearMaps() {
        nodeMap_ = null;
    }

    private CommonFunctions() {} // Disable creation...

    static PropertyMap nodeMap_ = null;
    static PropertyMap reverseNodeMap_ = null;
    static String sensorCpuPattern_ = null;
    static String sensorDimmPattern_ = null;
    static String sensorChannelPattern_ = null;
    static ConfigIO parser_ = ConfigIOFactory.getInstance("json");

    @FunctionalInterface
    public interface IndirectCall_ {
        void loadMaps();
    }
    static IndirectCall_ loadCall_ = CommonFunctions::loadConversionsMaps;

    private static class NodeOrCpuInfo {
        String newString;
        int number;

        NodeOrCpuInfo(int node, String newStr) {
            number = node;
            newString = newStr;
        }
    }

    private static class DimmInfo {
        String newString;
        int dimmChannel;
        int dimmNumber;

        DimmInfo(int channel, int dimm, String newStr) {
            dimmChannel = channel;
            dimmNumber = dimm;
            newString = newStr;
        }
    }
}
