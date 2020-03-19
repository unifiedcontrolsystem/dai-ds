// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.perflogging;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to record event counts and duration in a general way for micro benchmarking.
 *
 * To enable benchmarking set the DAI_USE_BENCHMARKING environment variable to "true". It will be disabled by default.
 *
 * To change the minimum burst threshold from the default of 10 set the DAI_BENCHMARKING_THRESHOLD environment
 * variable to the desired minimum size. This is to filter out random bursts.
 */
public class BenchmarkHelper {
    /**
     * Create a micro benchmarking object for a process that has bursts of activity. Only one per process is needed.
     *
     * @param dataSetName The name of the dataset for this benchmarking object.
     * @param filename The file to write after a "burst" is finished.
     * @param maxBurstSeconds The delay in seconds after a burst that signifies the burst is over.
     */
    public BenchmarkHelper(String dataSetName, String filename, long maxBurstSeconds) {
        this(dataSetName, new File(filename), maxBurstSeconds);
    }

    /**
     * Create a micro benchmarking object for a process that has bursts of activity. Only one per process is needed.
     *
     * @param dataSetName The name of the dataset for this benchmarking object.
     * @param file The file to write after a "burst" is finished.
     * @param maxBurstSeconds The delay in seconds after a burst that signifies the burst is over.
     */
    public BenchmarkHelper(String dataSetName, File file, long maxBurstSeconds) {
        file_ = file;
        maxBurstMicroSeconds_ = maxBurstSeconds * 1_000_000;
        dataSetName_ = dataSetName;
        if(System.getenv("DAI_USE_BENCHMARKING") != null)
            doBenchmarking_ = Boolean.parseBoolean(System.getenv("DAI_USE_BENCHMARKING").trim().toLowerCase());
        if(System.getenv("DAI_BENCHMARKING_THRESHOLD") != null)
            threshold_ = Long.parseLong(System.getenv("DAI_BENCHMARKING_THRESHOLD").trim());
    }

    /**
     * Change the dataset name after construction.
     *
     * @param newName The new name.
     */
    public void changeDataSetName(String newName) {
        if(doBenchmarking_) {
            assert newName != null : "Benchmarking: changeDataSetName: new name cannot be null";
            assert !newName.isBlank():"Benchmarking: changeDataSetName: new name cannot be blank";
            dataSetName_ = newName;
        }
    }

    /**
     * Replace a variable in the filename with a value.
     *
     * @param variableName The name of the variable to replace.
     * @param value The replacement for the variable.
     */
    public void replaceFilenameVariable(String variableName, String value) {
        if(variableName == null || variableName.isBlank()) throw new NullPointerException("variableName cannot be null or empty!");
        if(value == null || value.isBlank()) throw new NullPointerException("value cannot be null or empty!");
        file_ = new File(file_.toString().replace("{{" + variableName + "}}", value));
    }

    /**
     * Add a count to the default data tracker.
     *
     * @param value The value to add to the total count for the default data tracker.
     */
    public void addDefaultValue(long value) {
        if(doBenchmarking_) {
            commonAdd();
            defaultValue_.addAndGet(value);
        }
    }

    /**
     * Add a named count to the named data tracker.
     *
     * @param name The name of the data tracker to accumulate the count for.
     * @param value The value to add to the total count for the named data tracker.
     */
    public void addNamedValue(String name, long value) {
        if(doBenchmarking_) {
            commonAdd();
            if(!values_.containsKey(name))
                values_.put(name, new AtomicLong(0L));
            values_.get(name).addAndGet(value);
        }
    }

    /**
     * Called when the loop in the process does not do any work (i.e. no add methods are called).
     */
    public void tick() {
        if(doBenchmarking_ && (defaultValue_.get() > 0L || values_.size() > 0)) {
            long target = lastTs_.get() + maxBurstMicroSeconds_;
            if (getMicroSecondTimestamp() > target)
                recordAndReset();
        }
    }

    private void commonAdd() {
        long ts = getMicroSecondTimestamp();
        if (firstTs_.get() == 0L)
            firstTs_.set(ts);
        lastTs_.set(ts);
    }

    private void recordAndReset() {
        if(aboveThreshold()) {
            try (Writer out = new FileWriter(file_, StandardCharsets.UTF_8, true)) {
                StringBuilder builder = new StringBuilder();
                builder.append("{\"name\":\"").append(dataSetName_).append("\",");
                builder.append("\"start\":\"").append(firstTs_.get()).append("\",");
                builder.append("\"finish\":\"").append(lastTs_.get()).append("\",");
                builder.append("\"duration\":\"").append(lastTs_.get() - firstTs_.get()).append("\",");
                builder.append("\"counts\":{");
                boolean first = true;
                if(defaultValue_.get() > 0) {
                    builder.append("\"DEFAULT\":").append(defaultValue_.get());
                    first = false;
                }
                for(Map.Entry<String, AtomicLong> entry : values_.entrySet()) {
                    if(first)
                        first = false;
                    else
                        builder.append(",");
                    builder.append("\"").append(entry.getKey()).append("\":").append(entry.getValue().get());
                }
                builder.append("}}\n");
                out.write(builder.toString());
            } catch (IOException e) {
                System.err.println("*** Benchmarking Error: Failed to write result of benchmarking to file: " +
                        file_.toString());
            }
        }
        firstTs_.set(0L);
        lastTs_.set(0L);
        defaultValue_.set(0L);
        values_.clear();
    }

    private boolean aboveThreshold() {
        long max = defaultValue_.get();
        for(Map.Entry<String, AtomicLong> entry : values_.entrySet())
            max = Math.max(max, entry.getValue().get());
        return max >= threshold_;
    }

    private Instant getMicroSecondTimeStampAsInstant() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private long getMicroSecondTimestamp() {
        Instant now = getMicroSecondTimeStampAsInstant();
        return now.getEpochSecond() * 1_000_000 + now.getNano() / 1_000;
    }

    private File file_;
    private final long maxBurstMicroSeconds_;
    private String dataSetName_;
    private final Map<String, AtomicLong> values_ = new ConcurrentHashMap<>(32);
    private AtomicLong defaultValue_ = new AtomicLong(0L);
    private AtomicLong firstTs_ = new AtomicLong(0L);
    private AtomicLong lastTs_ = new AtomicLong(0L);
    private boolean doBenchmarking_ = false;
    private long threshold_ = 10;
}
