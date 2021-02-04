// Copyright (C) 2018-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.networking.sink;

import com.intel.logging.Logger;
import com.intel.networking.sink.for_benchmarking.NetworkDataSinkBenchmark;
import com.intel.networking.sink.kafka.NetworkDataSinkKafka;
import com.intel.networking.sink.rabbitmq.NetworkDataSinkRabbitMQ;
import com.intel.networking.sink.sse.NetworkDataSinkEventSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Static factory class to create a {@link NetworkDataSink} object for a specific provider.
 */
public final class NetworkDataSinkFactory {
    @SuppressWarnings("serial")
    public static class FactoryException extends Exception {
        FactoryException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * Static factory to create a {@link NetworkDataSink} object from its descriptive name. The name is returned by the
     * {@link NetworkDataSink}.getProviderName() method of the specific provider.
     *
     * @param logger The logger to pass to the created instance.
     * @param name The provider name to create.
     * @param args Provider specific arguments defined by the individual provider. See the
     *             {@link NetworkDataSink}.getProviderName() derived class documentation for the specific information
     *             on the meaning of the arguments.
     * @return The created object if successful or null if the object failed to create.
     * @throws FactoryException when the instance cannot be created.
     */
    public static NetworkDataSink createInstance(Logger logger, String name, Map<String, String> args)
            throws FactoryException {
        if(registeredImplementations_.containsKey(name)) {
            Class<?> type = registeredImplementations_.get(name);
            try {
                Constructor<?> ctor = type.getConstructor(Logger.class, Map.class);
                NetworkDataSink instance = (NetworkDataSink)ctor.newInstance(logger, args);
                instance.initialize();
                return instance;
            } catch(NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new FactoryException(String.format("Failed to create a registered instance for '%s'!", name), e);
            }
        } else
            return null;
    }

    /**
     * Alias for createInstance. See above.
     *
     * @param logger The logger to pass to the created instance.
     * @param name The provider name to create.
     * @param args Provider specific arguments defined by the individual provider. See the
     *             {@link NetworkDataSink}.getProviderName() derived class documentation for the specific information
     *             on the meaning of the arguments.
     * @return The created object if successful or null if the object failed to create.
     * @throws FactoryException when the instance cannot be created.
     */
    public static NetworkDataSink createInstanceWithLogger(Logger logger, String name, Map<String, String> args)
            throws FactoryException {
        return createInstance(logger, name, args);
    }
        /**
         * Register an new implementation of NetworkDataSink.
         *
         * @param name The name of the implementation. Name "zmq" and "rabbitmq" are reserved.
         * @param implClass The class object of the implementation of NetworkDataSink.
         * @return true only if the registration is successful; false otherwise. Will return false if the same name is
         * registered a second time.
         */
    public static boolean registerNewImplementation(String name, Class<? extends NetworkDataSink> implClass) {
        if(name == null || name.trim().isEmpty() || implClass == null)
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
        return registeredImplementations_.remove(name) != null;
    }

    @SuppressWarnings("serial")
    static Map<String, Class<? extends NetworkDataSink>> registeredImplementations_ =
            new HashMap<String, Class<? extends NetworkDataSink>>() {{
        put("rabbitmq", NetworkDataSinkRabbitMQ.class);
        put("benchmark", NetworkDataSinkBenchmark.class);
        put("eventSource", NetworkDataSinkEventSource.class);
        put("kafka", NetworkDataSinkKafka.class);
    }};
}
