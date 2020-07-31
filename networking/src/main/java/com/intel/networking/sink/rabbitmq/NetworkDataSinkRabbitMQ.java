// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.sink.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.intel.logging.Logger;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.NetworkDataSinkFactory;
import com.rabbitmq.client.*;

/**
 * A RabbitMQ client implementation of a {@link NetworkDataSink} providers. Subject refers to the RabbitMQ routing keys.
 */
public class NetworkDataSinkRabbitMQ implements NetworkDataSink {
    /**
     * Used by the {@link NetworkDataSinkFactory} to create an instance of the RabbitMQ provider.
     *
     * @param logger The passed logger.
     * @param args A Map<String,String> where the following values are recognized:
     *
     * exchangeName - (required) The required exchangeName for RabbitMQ.
     * queueName    - (DEPRECATED) This string is now ignored.
     * uri          - (optional) The URI to connect to the RabbitMQ broker (server).
     * subjects     - (required) The comma separated list (no spaces allowed around commas) of subjects (routing keys)
     *                to listen for.
     */
    public NetworkDataSinkRabbitMQ(Logger logger, Map<String, String> args) {
        assert logger != null;
        log_ = logger;
        if(args == null) {
            error("No args were given!");
            throw new IllegalArgumentException("args");
        }
        if(!args.containsKey("exchangeName") || args.get("exchangeName") == null ||
                args.get("exchangeName").trim().equals("")) {
            error("No RabbitMQ 'exchangeName' was given in the argument map!");
            throw new IllegalArgumentException("args");
        }
        exchangeName_ = args.get("exchangeName");
        connectionString_ = args.getOrDefault("uri", "amqp://127.0.0.1");
        if (args.containsKey("subjects") && args.get("subjects") != null &&
                !args.get("subjects").trim().equals("")) {
            String[] subjects = args.get("subjects").split(",");
            Collections.addAll(subjects_, subjects);
        } else {
            error("No RabbitMQ 'subjects' was given in the argument map!");
            throw new IllegalArgumentException("args");
        }
    }

    @Override
    public void initialize() { }

    /**
     * Clear all previously set subjects. Has no effect if startListening was already called.
     */
    @Override
    public void clearSubjects() {
        subjects_.clear();
    }

    /**
     * Add a subject to this object. Must be called prior to startListening.
     *
     * @param subject The string that is the subject to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubject(String subject) {
        subjects_.add(subject);
    }

    /**
     * A collection of subjects to this object. Must be called prior to startListening.
     *
     * @param subjects The collection of subjects to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubjects(Collection<String> subjects) {
        subjects_.addAll(subjects);
    }

    /**
     * This sets the connection URI for RabbitMQ.
     *
     * @param info The connection URI for the RabbitMQ client connection. The format is a string that begins with
     *             "amqp://" and is defined in the RabbitMQ documentation.
     */
    @Override
    public void setConnectionInfo(String info) {
        connectionString_ = info;
    }

    /**
     * Sets the callback delegate {@link NetworkDataSinkDelegate} to call back with the data received and translated by
     * this provider.
     *
     * @param delegate The object implementing the {@link NetworkDataSinkDelegate} interface.
     */
    @Override
    public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {
        delegate_ = delegate;
    }

    /**
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    @Override
    public void setLogger(Logger logger) {
        log_ = logger;
    }

    /**
     * Get the factory name of the this provider.
     *
     * @return The string "rabbitmq" to be used to create an instance of this provider by the
     * {@link NetworkDataSinkFactory}.
     *
     * For this provider the arguments have the following meaning:
     *      args[0]  = required; RabbitMQ exchangeName.
     *      args[1]  = optional; RabbitMQ connection URI
     *      args[2+] = optional; subject(s) to bind to.
     */
    @Override
    public String getProviderName() {
        return "rabbitmq";
    }

    /**
     * Called to start listening for incoming messages filtered by subject. This can only be called once per provider
     * instance. This uses a RabbitMQ Consumer pattern.
     */
    @Override
    public void startListening() {
        if(tag_ == null) {
            try {
                if(setUpChannel())
                    listen();
                else {
                    log_.error("RabbitMQ failed to initialize");
                    stopListening();
                }
            } catch(Exception e) {
                error(e);
                stopListening();
            }
        }
    }

    /**
     * Called to stop listening for incoming messages. This should only be called once and only after startListening
     * was called. This halts the RabbitMQ Consumer instance.
     */
    @Override
    public void stopListening() {
        tearDownChannel();
    }

    /**
     * A flag to determine if RabbitMQ is currently listening.
     *
     * @return true if the listener is running; false otherwise.
     */
    @Override
    public boolean isListening() {
        return (tag_ != null);
    }

    private void error(String msg) {
        if(log_ != null) log_.error(msg);
    }

    void error(Exception e) {
        error("*** " + e.getMessage());
        for(StackTraceElement trace: e.getStackTrace())
            error(trace.toString());
    }

    ConnectionFactory createConnectionFactory() {
        return new ConnectionFactory();
    }

    private boolean setUpChannel() {
        ConnectionFactory factory = createConnectionFactory();
        try {
            factory.setUri(connectionString_);
        } catch (Exception e) {
            error(e);
            return false;
        }
        if(createConnection(factory))
            if(createChannel())
                return setUpChannelObject();
        return false;
    }

    private boolean setUpChannelObject() {
        try {
            channel_.exchangeDeclare(exchangeName_, BuiltinExchangeType.TOPIC);
            queueName_ = channel_.queueDeclare().getQueue();
            for (String subject : subjects_)
                channel_.queueBind(queueName_, exchangeName_, subject);
            return true;
        } catch(Exception e) {
            error(e);
            closeChannelObject();
            closeConnection();
            return false;
        }
    }

    private void closeChannelObject() {
        try {
            channel_.close();
        } catch(Exception e2) { log_.exception(e2); }
        channel_ = null;
        queueName_ = null;
    }

    private boolean createChannel() {
        try {
            channel_ = connection_.createChannel();
        } catch(Exception e) {
            error(e);
            closeConnection();
            return false;
        }
        return (channel_ != null);
    }

    private void closeConnection() {
        try {
            connection_.close();
        } catch(Exception e2) { log_.exception(e2); }
        connection_ = null;
    }

    private boolean createConnection(ConnectionFactory factory) {
        try {
            connection_ = factory.newConnection();
        } catch(Exception e) {
            error(e);
            return false;
        }
        return (connection_ != null);
    }

    private void tearDownChannel() {
        if(tag_ != null) {
            try {
                channel_.basicCancel(tag_);
            } catch(Exception e) {
                log_.exception(e);
            }
            tag_ = null;
        }
        if(channel_ != null) {
            closeChannelObject();
        }
        if(connection_ != null) {
            closeConnection();
        }
    }

    private void listen() throws  IOException {
        String tag = UUID.randomUUID().toString();
        consumeData(tag);
        tag_ = tag;
    }

    void consumeData(String tag) throws IOException {
        channel_.basicConsume(queueName_, true, tag, new DefaultConsumer(channel_) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) {
                String routingKey = envelope.getRoutingKey();
                if (delegate_ != null)
                    delegate_.processIncomingData(routingKey, new String(body, StandardCharsets.UTF_8));
            }
        });
    }

    private HashSet<String> subjects_ = new HashSet<>();
    private String connectionString_;
    private NetworkDataSinkDelegate delegate_ = null;
    private Logger log_;
    private String exchangeName_;
    private String queueName_ = null;
    private Connection connection_ = null;
    private Channel channel_ = null;
    private String tag_ = null;
}
