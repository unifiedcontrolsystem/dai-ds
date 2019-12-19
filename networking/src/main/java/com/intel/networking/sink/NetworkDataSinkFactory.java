// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.sink;

import com.intel.logging.Logger;
import com.intel.networking.sink.for_benchmarking.NetworkDataSinkBenchmark;
import com.intel.networking.sink.http_callback.NetworkDataSinkHttpCallback;
import com.intel.networking.sink.rabbitmq.NetworkDataSinkRabbitMQ;
import com.intel.networking.sink.restsse.NetworkDataSinkSSE;

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
     */
    public static NetworkDataSink createInstance(Logger logger, String name, Map<String, String> args) throws FactoryException {
        if(name.equals("rabbitmq")) {
            NetworkDataSink instance = new NetworkDataSinkRabbitMQ(logger, args);
            instance.initialize();
            return instance;
        } else if(name.equals("sse")) {
            NetworkDataSink instance = new NetworkDataSinkSSE(logger, args);
            instance.initialize();
            return instance;
        } else if(name.equals("http_callback")) {
            NetworkDataSink instance = new NetworkDataSinkHttpCallback(logger, args);
            instance.initialize();
            return instance;
        } else if(name.equals("benchmark")) {
            NetworkDataSink instance = new NetworkDataSinkBenchmark(logger, args);
            instance.initialize();
            return instance;
        } else if(registeredImplementations_ != null && registeredImplementations_.containsKey(name)) {
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
     * Register an new implementation of NetworkDataSink.
     *
     * @param name The name of the implementation. Name "zmq" and "rabbitmq" are reserved.
     * @param implClass The class object of the implementation of NetworkDataSink.
     * @return true only if the registration is successful; false otherwise. Will return false if the same name is
     * registered a second time.
     */
    public static boolean registerNewImplementation(String name, Class<? extends NetworkDataSink> implClass) {
        if(name == null || name.equals("rabbitmq") || name.equals("zmq"))
            return false;
        if(implClass == null)
            return false;
        if(registeredImplementations_ == null)
            registeredImplementations_ = new HashMap<>();
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
        if(registeredImplementations_ != null && registeredImplementations_.containsKey(name))
            return registeredImplementations_.remove(name) != null;
        return false;
    }

    static Map<String, Class<? extends NetworkDataSink>> registeredImplementations_;
}
