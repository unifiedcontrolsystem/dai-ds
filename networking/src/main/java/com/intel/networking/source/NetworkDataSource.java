// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.source;

import com.intel.logging.Logger;

import java.io.Closeable;

/**
 * Interface for all NetworkDataSource providers. This is usually a publisher role in a PUB/SUB networking pattern.
 *
 * NOTE: Subject is the term used in this interface but this could have different names for different providers. In
 * ZeroMQ this is the "topic", in RabbitMQ this is the "routing key".
 */
public interface NetworkDataSource extends Closeable {
    /**
     * Optionally initialize the implementation.
     */
    void initialize();

    /**
     * This sets up the connection for the provider in the provider specific format.
     *
     * @param info The network connection string for the provider.
     */
    void connect(String info);

    /**
     * Sets the logger for the network source provider.
     *
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    void setLogger(Logger logger);

    /**
     * Get the factory name of the implemented provider.
     *
     * @return The name of the provider to be created by the {@link NetworkDataSourceFactory}.
     */
    String getProviderName();

    /**
     * Sends a message on a particular subject to the network.
     *
     * @param subject The subject to send the message for.
     * @param message The actual message to send.
     *
     * @return True if the message was queued for delivery, false otherwise.
     */
    boolean sendMessage(String subject, String message);
}
