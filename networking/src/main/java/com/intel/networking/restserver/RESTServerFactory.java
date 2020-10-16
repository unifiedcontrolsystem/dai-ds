// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Static factory class to create instances of a RESTServer, add implementations, and remove implementations.
 */
public final class RESTServerFactory {
    /**
     * Get a new specific implementation of RESTServer by name adding a logger. Use if you want a guaranteed
     * new server instance.
     *
     * @param implName The name of the implementation. Must be pre-registered with addImplementation.
     * @param log The logger the implementation will use.
     * @return Either a new instance or a singleton instance depending on the specific implementation.
     * @throws RESTServerException If the implementation cannot be constructed.
     */
    public static RESTServer getInstance(String implName, com.intel.logging.Logger log) throws RESTServerException {
        if(implementations_.containsKey(implName)) {
            try {
                Class<? extends RESTServer> clazz = implementations_.get(implName);
                return clazz.getDeclaredConstructor(com.intel.logging.Logger.class).newInstance(log);
            } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException cause) {
                throw new RESTServerException(String.format("The RESTServer implementation class for '%s' is missing" +
                        " the public constructor taking a Logger object as its only parameter", implName), cause);
            }
        } else
            return null;
    }

    /**
     * Get a new singleton implementation of RESTServer by name adding a logger. Use this when you need to share
     * the REST server among multiple component of the application (Similar to the way the Spark REST server works).
     *
     * @param implName The name of the implementation. Must be pre-registered with addImplementation.
     * @param log The logger the implementation will use.
     * @return Either the previously created server or a newly created RESTServer implementation.
     * @throws RESTServerException
     */
    public static RESTServer getSingletonInstance(String implName, com.intel.logging.Logger log)
            throws RESTServerException {
        addShutdownTask();
        if(singletons_.containsKey(implName))
            return singletons_.get(implName);
        else {
            RESTServer server = getInstance(implName, log);
            singletons_.put(implName, server);
            return server;
        }
    }

    /**
     * Add a new implementation to the factory. The implementation class must have a declared ctor with a single
     * parameter of type com.intel.logging.Logger must be specified.
     *
     * @param name The name of the new implementation. If replacing an implementation then the old Class object is
     *            returned.
     * @param clazz The Class object to associate with the implementation name.
     * @return null on a new implementation name, the old Class object associated with the name on replacement.
     * @throws RESTServerException If the class does not contain a public ctor that takes one parameter that is the
     * Logger type specified in the description of this method.
     */
    public static Class<? extends RESTServer> addImplementation(String name, Class<? extends RESTServer> clazz)
            throws RESTServerException {
        try {
            clazz.getDeclaredConstructor(com.intel.logging.Logger.class);
        } catch(NoSuchMethodException cause) {
            throw new RESTServerException("", cause);
        }
        return implementations_.putIfAbsent(name, clazz);
    }

    /**
     * Remove a previously added implementation by name.
     *
     * @param name The implementation name to remove.
     * @return The Class object of the removed implementation name or null if no implementation was removed.
     */
    public static Class<? extends RESTServer> removeImplementation(String name) {
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

    synchronized private static void addShutdownTask() {
        if(!addedShutdownTask_) {
            addedShutdownTask_ = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (RESTServer server : singletons_.values()) {
                    try {
                        server.close();
                    } catch (Exception e) { e.printStackTrace(); }
                }
                singletons_.clear();
            }));
        }
    }
    private RESTServerFactory() {}

    private static final Map<String, Class<? extends RESTServer>> implementations_ =
            new HashMap<String, Class<? extends RESTServer>>() {{
    }};
    private static Map<String,RESTServer> singletons_ = new HashMap<>();
    private static boolean addedShutdownTask_ = false;
}
