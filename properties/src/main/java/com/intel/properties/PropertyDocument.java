// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.properties;

/**
 * Base interface for PropertyMap and PropertyArray. Used to generalize the 2 implementations.
 */
public interface PropertyDocument {
    /**
     * Test if the instance is an array.
     *
     * @return True if the instance is an PropertyArray, false otherwise.
     */
    boolean isArray();

    /**
     * Test if the instance is an map.
     *
     * @return True if the instance is an PropertyMap, false otherwise.
     */
    boolean isMap();

    /**
     * Return the PropertyDocument cast to a PropertyArray.
     *
     * @return The cast instance if its a PropertyArray or null if its not.
     */
    PropertyArray getAsArray();

    /**
     * Return the PropertyDocument cast to a PropertyMap.
     *
     * @return The cast instance if its a PropertyMap or null if its not.
     */
    PropertyMap getAsMap();
}
