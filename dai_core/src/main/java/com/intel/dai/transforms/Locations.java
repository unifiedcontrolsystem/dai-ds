// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.transforms;

import com.intel.logging.Logger;

/**
 * Interface to convert different forms of locations back and forth.
 */
public interface Locations {
    /**
     * From a DAI known hostname, get the location string.
     *
     * @param hostname The hostname to lookup. Cannot be empty or null.
     * @return The location string.
     * @throws RuntimeException thrown if the hostname was empty or null, or the lookup failed.
     */
    String hostnameToLocation(String hostname) throws RuntimeException;

    /**
     * From a DAI location string, get the DAI known hostname.
     *
     * @param location The DAI location string. Cannot be empty or null.
     * @return The DAI known hostname.
     * @throws RuntimeException thrown if the location was empty or null, or the lookup failed.
     */
    String locationToHostName(String location) throws RuntimeException;

    /**
     * Return a DAI location string from a foreign location string.
     *
     * @param foreignLocation The foreign location string. Cannot be empty or null.
     * @param otherArgs For some implementations this is used to pass more string information to the implementation.
     * @return The DAI known location string.
     * @throws RuntimeException thrown if the foreignLocation was empty or null, or the lookup failed.
     */
    String foreignLocationToLocation(String foreignLocation, String... otherArgs) throws RuntimeException;

    /**
     * Return a foreign location string from a DAI location string.
     *
     * @param location The location string. Cannot be empty or null.
     * @return The foreign location string.
     * @throws RuntimeException thrown if the location was empty or null, or the lookup failed.
     */
    String locationToForeignLocation(String location) throws RuntimeException;

    /**
     * Add the logger after creation.
     *
     * @param logger The {@link Logger} instance to use in this class.
     */
    void setLogger(Logger logger);
}
