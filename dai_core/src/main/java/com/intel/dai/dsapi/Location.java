// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.properties.PropertyMap;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.BadInputException;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

/**
 * Queries to retrieve location from a given node hostname and viceversa.
 */
public interface Location {
    /**
     * Get the location string of a given compute or non-compute node hostname
     *
     * @param host hostname to get location from
     * @return String with location
     */
    String getLocationFromHostname(String host);

    /**
     * Get hostname of a given compute or non-compute location string
     *
     * @param lctn location string to get hostname from
     * @return String with hostname
     */
    String getHostnameFromLocation(String lctn);

    /**
     * Get the system label string
     * @return String with the system label
     */
    String getSystemLabel();

    /**
     * Get locations strings of the system where is running.
     * @return PropertyMap with locations
     */
    PropertyMap getSystemLocations();

    /**
     * Reloads the cache of hosts and locations
     *
     */
    void reloadCache() throws DataStoreException;

    public Set<String> getLocationsFromNodes(Set<String> nodes) throws BadInputException;

    public Set<String> getNodesFromLocations(Set<String> locations) throws BadInputException;

}
