// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Static factory class to create instances of a RESTClient, add implementations, and remove implementations.
 */
public final class RESTClientFactory {
    /**
     * Get a new specific implementation of RESTClient by name adding a logger.
     *
     * @param implName The name of the implementation. Must be pre-registered with addImplementation.
     * @param log The logger the implementation will use.
     * @return Either a new instance or a singleton instance depending on the specific implementation.
     * @throws RESTClientException If the implementation cannot be constructed.
     */
    public static RESTClient getInstance(String implName, com.intel.logging.Logger log) throws RESTClientException {
        if(implementations_.containsKey(implName)) {
            try {
                Class<? extends RESTClient> clazz = implementations_.get(implName);
                return clazz.getDeclaredConstructor(com.intel.logging.Logger.class).newInstance(log);
            } catch(NoSuchMethodException | InstantiationException | IllegalAccessException |
                    InvocationTargetException cause) {
                throw new RESTClientException(String.format("The RESTServer implementation class for '%s' is missing" +
                        " the public constructor taking a Logger object as its only parameter", implName), cause);
            }
        } else
            return null;
    }

    /**
     * Add a new implementation to the factory. The implementation class must have a declared ctor with a single
     * parameter of type com.intel.logging.Logger must be specified.
     *
     * @param name The name of the new implementation. If replacing an implementation then the old Class object is
     *            returned.
     * @param clazz The Class object to associate with the implementation name.
     * @return null on a new implementation name, the old Class object associated with the name on replacement.
     * @throws RESTClientException If the class does not contain a public ctor that takes one parameter that is the
     * Logger type specified in the description of this method.
     */
    public static Class<? extends RESTClient> addImplementation(String name, Class<? extends RESTClient> clazz)
            throws RESTClientException {
        try {
            clazz.getDeclaredConstructor(com.intel.logging.Logger.class);
        } catch(NoSuchMethodException cause) {
            throw new RESTClientException("", cause);
        }
        return implementations_.putIfAbsent(name, clazz);
    }

    /**
     * Remove a previously added implementation by name.
     *
     * @param name The implementation name to remove.
     * @return The Class object of the removed implementation name or null if no implementation was removed.
     */
    public static Class<? extends RESTClient> removeImplementation(String name) {
        return implementations_.remove(name);
    }

    /**
     * Get list of implementation names from factory.
     *
     * @return The current set of names.
     */
    public static Collection<String> getImplementationNames() {
        return new HashSet<>(implementations_.keySet());
    }

    static void clearAllImplementations() { // For unit tests only.
        implementations_.clear();
    }

    private RESTClientFactory() {} // Make instantiation impossible.

    static final Map<String, Class<? extends RESTClient>> implementations_ = new HashMap<>() {{
        put("jdk11", com.intel.networking.restclient.java11.Java11RESTClient.class);
        put("apache", com.intel.networking.restclient.apache.ApacheRESTClient.class);
    }};
}
