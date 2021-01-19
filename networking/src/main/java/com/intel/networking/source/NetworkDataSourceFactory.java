// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.networking.source;

import com.intel.logging.Logger;
import com.intel.networking.source.kafka.NetworkDataSourceKafka;
import com.intel.networking.source.rabbitmq.NetworkDataSourceRabbitMQ;
import com.intel.networking.source.zmq.NetworkDataSourceZMQ;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Static factory class to create a {@link NetworkDataSource} object for a specific provider.
 */
public final class NetworkDataSourceFactory {
    @SuppressWarnings("serial")
    public static class FactoryException extends Exception {
        FactoryException(String message, Throwable cause) { super(message, cause); }
    }
    private NetworkDataSourceFactory() {}

    /**
     * Static factory to create a {@link NetworkDataSource} object from its descriptive name. The name is returned by the
     * {@link NetworkDataSource}.getProviderName() method of the specific provider.
     *
     * @param logger The logger to pass to the implementation.
     * @param name The provider name to create.
     * @param args Provider specific arguments defined by the individual provider. See the
     *             {@link NetworkDataSource}.getProviderName() derived class documentation for the specific information
     *             on the meaning of the arguments.
     * @return The created object if successful or null if the object failed to create.
     * @throws NetworkDataSourceFactory.FactoryException Thrown when a registered implementation cannot be created.
     */
    public static NetworkDataSource createInstance(Logger logger, String name, Map<String, String> args)
            throws FactoryException {
        if(registeredImplementations_.containsKey(name)) {
            Class<?> type = registeredImplementations_.get(name);
            try {
                Constructor<?> ctor = type.getConstructor(Logger.class, Map.class);
                NetworkDataSource instance = (NetworkDataSource)ctor.newInstance(logger, args);
                instance.initialize();
                return instance;
            } catch(NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new FactoryException(String.format("Failed to create a registered instance for '%s'!", name), e);
            }
        } else
            return null;
    }

    /**
     * Register an new implementation of NetworkDataSource.
     *
     * @param name The name of the implementation. Name "zmq" and "rabbitmq" are reserved.
     * @param implClass The class object of the implementation of NetworkDataSource.
     * @return true only if the registration is successful; false otherwise. Will return false if the same name is
     * registered a second time.
     */
    public static boolean registerNewImplementation(String name, Class<? extends NetworkDataSource> implClass) {
        if(implClass == null || name == null || name.trim().isEmpty())
            return false;
        if(registeredImplementations_.containsKey(name))
            return false;
        return (registeredImplementations_.putIfAbsent(name, implClass) == null);
    }

    /**
     * Remove an implementation from the factory. Optional.
     *
     * @param name The name to remove.
     * @return true if the name is removed false in all other cases.
     */
    public static boolean unregisterImplementation(String name) {
        if(registeredImplementations_.containsKey(name))
            return registeredImplementations_.remove(name) != null;
        return false;
    }

    @SuppressWarnings("serial")
    static Map<String, Class<? extends NetworkDataSource>> registeredImplementations_ =
            new HashMap<String, Class<? extends NetworkDataSource>>() {{
        put("rabbitmq", NetworkDataSourceRabbitMQ.class);
        put("kafka", NetworkDataSourceKafka.class);
        put("zmq", NetworkDataSourceZMQ.class);
    }};
}
