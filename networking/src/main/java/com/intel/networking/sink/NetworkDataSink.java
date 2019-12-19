// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.sink;

import com.intel.logging.Logger;

import java.util.Collection;

/**
 * Interface for all NetworkDataSink providers. This is usually a subscriber role in a PUB/SUB networking pattern.
 *
 * NOTE: Subject is the term used in this interface but this could have different names for different providers. In
 * ZeroMQ this is the "topic", in RabbitMQ this is the "routing key".
 */
public interface NetworkDataSink {
    /**
     * Optionally used to initialize the implementation.
     */
    void initialize();

    /**
     * Clear all previously set subjects. Has no effect if startListening was already called.
     */
    void clearSubjects();

    /**
     * Add a subject to this object. Must be called prior to startListening.
     *
     * @param subject The string that is the subject to add for listening on the network bus.
     */
    void setMonitoringSubject(String subject);

    /**
     * A collection of subjects to this object. Must be called prior to startListening.
     *
     * @param subjects The collection of subjects to add for listening on the network bus.
     */
    void setMonitoringSubjects(Collection<String> subjects);

    /**
     * This sets the connection info for the provider in the provider specific format.
     *
     * @param info The connection string for the provider.
     */
    void setConnectionInfo(String info);

    /**
     * Sets the callback delegate {@link NetworkDataSinkDelegate} to call back with the data received and translated by
     * the provider.
     *
     * @param delegate The object implementing the {@link NetworkDataSinkDelegate} interface.
     */
    void setCallbackDelegate(NetworkDataSinkDelegate delegate);

    /**
     * Called to start listening for incoming messages filtered by subject. This can only be called once per provider
     * instance.
     */
    void startListening();

    /**
     * Called to stop listening for incoming messages. This should only be called once and only after startListening
     * was called.
     */
    void stopListening();

    /**
     * @return A flag to determine if the provider implemented is currently listening.
     */
    boolean isListening();

    /**
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    void setLogger(Logger logger);

    /**
     * Get the factory name of the implemented provider.
     *
     * @return The name of the provider to be created by the {@link NetworkDataSinkFactory}.
     */
    String getProviderName();
}
