// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.for_benchmarking;

import com.intel.logging.Logger;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.NetworkDataSinkFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Description of class NetworkDataSinkBenchmark.
 */
public class NetworkDataSinkBenchmark implements NetworkDataSink {
    /**
     * Used by the {@link NetworkDataSinkFactory} to create an instance of the benchmark provider.
     *
     * @param args A Map<String,String> where the following values are recognized:
     *
     * initialDelaySeconds - (def: 30) The initial delay before sending the file contents the first time.
     * rawDataFileName     - (def: /opt/ucs/etc/RawBenchmarkData.txt) The file to process and send each line as a
     *                       message.
     * publishedSubject    - (optional) The subject to use when sending the messages.
     */
    public NetworkDataSinkBenchmark(Logger logger, Map<String, String> args) {
        log_ = logger;
        initialDelaySeconds_ = Integer.parseInt(args.getOrDefault("initialDelaySeconds", "30"));
        rawDataFileName_ = args.getOrDefault("rawDataFileName", "/opt/ucs/etc/RawBenchmarkData.txt");
        publishedSubject_ = args.getOrDefault("publishedSubject", "events");
        for(String key: args.keySet())
            log_.info("*** ARGUMENT: '%s' = '%s'", key, args.get(key));
    }

    @Override public void initialize() {}

    @Override public void clearSubjects() {}

    @Override public void setMonitoringSubject(String subject) {}

    @Override public void setMonitoringSubjects(Collection<String> subjects) {}

    @Override public void setConnectionInfo(String info) {}

    @Override public void setCallbackDelegate(NetworkDataSinkDelegate delegate) { callback_ = delegate; }

    @Override public void startListening() { startEngine(); }

    @Override public void stopListening() { stopEngine(); }

    @Override public boolean isListening() { return engineThread_ != null && engineThread_.isAlive(); }

    @Override public void setLogger(Logger logger) { log_ = logger; }

    @Override public String getProviderName() { return "benchmark"; }

    private void startEngine() {
        if(engineThread_ == null) {
            engineThread_ = new Thread(this::stageOne);
            engineThread_.start();
            while(!engineRunning_.get()) ; // Intentionally empty while loop, should be short.
        }
    }

    private void stopEngine() {
        if(isListening()) {
            log_.info("*** Shutting down the engine normally");
            engineRunning_.set(false);
            try {
                engineThread_.join();
            } catch(InterruptedException e) { /* Nothing to do here. */ }
        }
    }

    private void stageOne() {
        engineRunning_.set(true);
        log_.info("*** Starting stage 1 with file '%s'...", rawDataFileName_);
        try {
            Thread.sleep(initialDelaySeconds_ * 1000); // seconds to milliseconds.
            processFile();
            stageTwo();
        } catch(InterruptedException | IOException e) {
            log_.exception(e, "*** Failed to process the file '%s'", rawDataFileName_);
            engineRunning_.set(false);
        }
    }

    private void stageTwo() {
        File watch = new File(rawDataFileName_);
        long lastModified = watch.lastModified();
        while(engineRunning_.get()) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                log_.exception(e, "*** Stage 2 sleep was interrupted");
                engineRunning_.set(false);
                continue;
            }
            long newDate = watch.lastModified();
            if(newDate > lastModified) {
                try {
                    processFile();
                } catch(IOException e) {
                    log_.exception(e, "*** Failed to process the file '%s'", rawDataFileName_);
                    log_.info(e.getMessage());
                    engineRunning_.set(false);
                    continue;
                }
                lastModified = newDate;
            }
        }
        log_.info("*** Exiting the '%s' engine thread.", this.getClass().getCanonicalName());
    }

    private void processFile() throws IOException {
        log_.info("*** Processing the file '%s'...", rawDataFileName_);
        List<String> lines = new ArrayList<>();
        try (FileReader reader = new FileReader(rawDataFileName_)) {
            try (Scanner scanner = new Scanner(reader)) {
                while(scanner.hasNext())
                    lines.add(scanner.nextLine().trim());
            }
        }
        log_.info("*** Processed the file '%s'", rawDataFileName_);
        log_.info("*** Sending data to listener's owner...");
        for(String line: lines)
            callback_.processIncomingData(publishedSubject_, line);
        log_.info("*** Finished sending data to listener's owner");
    }

    private Logger log_;
    private final long initialDelaySeconds_;
    private final String rawDataFileName_;
    private final String publishedSubject_;
    private NetworkDataSinkDelegate callback_;
    private Thread engineThread_ = null;
    private final AtomicBoolean engineRunning_ = new AtomicBoolean(false);
}
