// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.foreign_bus;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.xdg.XdgConfigFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
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
     * @throws DateTimeParseException If the date is not of the form yyyy-MM-dd HH:mm:ss.SSSX
     */
    public static long convertISOToLongTimestamp(String timestamp) throws DateTimeParseException {
        timestamp = timestamp.replace(" ", "T");
        Instant ts = ZonedDateTime.parse(timestamp).toInstant();
        String[] parts = timestamp.split("\\.");
        String fraction = parts.length == 1 ? "0" : parts[1].replace("Z","");
        if(fraction.isEmpty() || fraction.length() > 9)
            throw new DateTimeParseException("Fraction of seconds is malformed, must be 1-9 digits", ": ", timestamp.indexOf('.'));
        return (ts.getEpochSecond() * 1_000_000_000L) +
                (Long.parseLong(fraction) * (long)Math.pow(10, 9-fraction.length()));
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
        if(nodeMap_ == null)
            loadCall_.loadMaps();
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
