// Copyright (C) 2018-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.networking.source.zmq;

import com.intel.logging.Logger;
import com.intel.networking.source.NetworkDataSource;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.zeromq.*;

public class NetworkDataSourceZMQ implements NetworkDataSource, Runnable {
    public NetworkDataSourceZMQ(Logger logger, Map<String, String> args) {
        setLogger(logger);
        if (args == null) {
            log_.error("No args were given!");
            throw new IllegalArgumentException("args");
        }
        if (!args.containsKey("uri") || args.get("uri") == null ||
                args.get("uri").trim().equals("")) {
            log_.error("No jeroMQ 'uri' was given in the argument map!");
            throw new IllegalArgumentException("args");
        }
        connectionUri_ = args.getOrDefault("uri", "tcp://127.0.0.1:5401");
    }

    @Override
    public void initialize() { }

    @Override
    public void connect(String info) {
        if (publisherThread_ == null) {
            if (info != null && !info.trim().equals("")) connectionUri_ = info;
            publisherThread_ = new Thread(this);
            publisherThread_.start();
            try { // wait till the server is actually up....
                Thread.sleep(100); // milliseconds
            } catch (InterruptedException e) { /* Do Nothing */ }
        }
    }

    @Override
    public void close() throws IOException {
       destroyPublisher();
    }

    @Override
    public void setLogger(Logger logger) {
        log_ = logger;
    }

    @Override
    public String getProviderName() {
        return "zmq";
    }

    @Override
    public boolean sendMessage(String subject, String message) {
        if (publisherThread_ == null) {
            log_.error("The connect() method must be called before you can start sending data!");
            return false;
        }
        queue_.add(new PublishData(subject,message));
        return true;
    }

    @Override
    public void run() {
        stopPublisher_.set(false);
        try {
            initializePublisher();
        } catch (Exception e) {
            log_.exception(e, "Failed to start publisher!");
            destroyPublisher();
            return;
        }
        try {
            processSendRequests();
        } catch (IOException e) {
            log_.exception(e, "Failed to send requests using the publisher!");
        } finally {
            destroyPublisher();
        }
    }

    void processSendRequests() throws IOException {
        while (!stopPublisher_.get()) {
            while (!queue_.isEmpty() ) {
                NetworkDataSourceZMQ.PublishData data = queue_.poll();
                publisher.sendStringUtf8(data.subject,ZMQ.SNDMORE);
                publisher.sendStringUtf8(data.message);
            }
        }
    }

    void initializePublisher() throws Exception {
        publisher = createSocketZMQ();
        publisher.bind(connectionUri_);
    }

    ZSocket createSocketZMQ() {
        return new ZSocket(SocketType.PUB.type());
    }

    private void destroyPublisher() {
        if(publisher != null) {
            try {
                publisher.close();
            } catch(Exception e) { /* Ignore*/ }
            publisher = null;
        }
        publisherThread_ = null;
    }

    private Thread publisherThread_ = null;
    private Logger log_;
    private String connectionUri_;
    private ZSocket publisher;
    ConcurrentLinkedQueue<PublishData> queue_ = new ConcurrentLinkedQueue<>();
    AtomicBoolean stopPublisher_ = new AtomicBoolean(false);
    private static final class PublishData {
        PublishData(String key, String msg) {
            subject = key;
            message = msg;
        }
        String subject;
        String message;
    }
}
