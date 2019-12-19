// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.source.rabbitmq;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkDataSourceRabbitMQ implements NetworkDataSource, Runnable {
    /**
     * Used by the {@link NetworkDataSourceFactory} to create an instance of the RabbitMQ provider.
     *
     * @param args A Map<String,String> where the following values are recognized:
     *
     * exchangeName - (required) The required exchangeName for RabbitMQ.
     * uri          - (optional) The URI to connect to the RabbitMQ broker (server).
     */
    public NetworkDataSourceRabbitMQ(Logger logger, Map<String, String> args) {
        log_ = logger;
        args_ = args;
    }

    /**
     * Optionally initialize the implementation.
     */
    @Override
    public void initialize() {
        if(args_ == null) {
            log_.error("No args were given!");
            throw new IllegalArgumentException("args");
        }
        if(!args_.containsKey("exchangeName") || args_.get("exchangeName") == null ||
                args_.get("exchangeName").trim().equals("")) {
            log_.error("No RabbitMQ 'exchangeName' was given in the argument map!");
            throw new IllegalArgumentException("args");
        }
        exchangeName_ = args_.get("exchangeName");
        connectionUri_ = args_.getOrDefault("uri", "amqp://127.0.0.1");
    }

    /**
     * This sets the connection info for the provider in the provider specific format.
     *
     * @param info The network connection string for the provider.  Can be null and passed in the constructor args.
     */
    @Override
    public void connect(String info) {
        if(publisherThread_ == null) {
            if(info != null && !info.trim().equals("")) connectionUri_ = info;
            publisherThread_ = new Thread(this);
            publisherThread_.start();
            try { // wait till the server is actually up....
                Thread.sleep(300); // milliseconds
            } catch(InterruptedException e) { /* Ignore */ }
        }
    }

    /**
     * Closes the open connection if connect was called previously.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if(publisherThread_ != null) {
            Thread localReference = publisherThread_;
            stopPublisher_.set(true);
            try {
                localReference.join();
            } catch(InterruptedException e) { /* Ignore */ }
        }
    }

    /**
     * Sets the logger for the network source provider.
     *
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    @Override
    public void setLogger(Logger logger) {
        log_ = logger;
    }

    /**
     * Get the factory name of the implemented provider.
     *
     * @return The name of the provider to be created by the {@link NetworkDataSourceFactory}.
     */
    @Override
    public String getProviderName() {
        return "rabbitmq";
    }

    /**
     * Sends a message on a particular subject to the network.
     *
     * @param subject The subject to send the message for.
     * @param message The actual message to send.
     * @return True if the message was queued for delivery, false otherwise.
     */
    @Override
    public boolean sendMessage(String subject, String message) {
        if(publisherThread_ == null) {
            log_.error("The connect() method must be called before you can start sending data!");
            return false;
        }
        return queue_.add(new PublishData(subject, message));
    }

    @Override
    public void run() {
        stopPublisher_.set(false);
        try {
            initializePublisher();
        } catch(Exception e) {
            log_.exception(e, "Failed to start publisher!");
            destroyPublisher();
            return;
        }
        try {
            processSendRequests();
        } catch(IOException e) {
            log_.exception(e, "Failed to send requests using the publisher!");
        } finally {
            destroyPublisher();
        }
    }

    void processSendRequests() throws IOException {
        while (!stopPublisher_.get()) {
            if (!queue_.isEmpty()) {
                PublishData data = queue_.poll();
                // data cannot be null, look at the source of items in the queue (sensMessage).
                channel_.basicPublish(exchangeName_, data.subject, null, data.message.getBytes());
            }
        }
    }

    private void destroyPublisher() {
        if(channel_ != null) {
            try {
                channel_.close();
            } catch(Exception e) { /* Ignore*/ }
            channel_ = null;
        }
        if(connection_ != null) {
            try {
                connection_.close();
            } catch(Exception e) { /* Ignore*/ }
            connection_ = null;
        }
        publisherThread_ = null;
    }

    void initializePublisher() throws Exception {
        ConnectionFactory factory = createFactory();
        factory.setUri(connectionUri_);
        connection_ = factory.newConnection();
        channel_ = connection_.createChannel();
        channel_.exchangeDeclare(exchangeName_, BuiltinExchangeType.TOPIC);
    }

    ConnectionFactory createFactory() {
        return new ConnectionFactory();
    }

    private Map<String,String> args_;
    private Thread publisherThread_ = null;
    private Logger log_;
    private String exchangeName_;
    private String connectionUri_;
    ConcurrentLinkedQueue<PublishData> queue_ = new ConcurrentLinkedQueue<>();
    AtomicBoolean stopPublisher_ = new AtomicBoolean(false);
    private Connection connection_;
    private Channel channel_;

    private static final class PublishData {
        PublishData(String key, String msg) {
            subject = key;
            message = msg;
        }
        String subject;
        String message;
    }
}
