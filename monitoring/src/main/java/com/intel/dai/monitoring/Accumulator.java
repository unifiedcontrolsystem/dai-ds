// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;

import java.util.ArrayList;
import java.util.List;

class Accumulator {
    Accumulator(Logger logger) { log_ = logger; }
    void addValue(CommonDataFormat data) {
        values_.add(data.getValue());
        timestamps_.add(data.getNanoSecondTimestamp());
        if(useTime_ && timeExceeded())
            doAggregation(data);
        else if(!useTime_ && values_.size() == count_)
            doAggregation(data);
    }

    void doAggregation(CommonDataFormat data) {
        log_.debug("Generating aggregated data...");
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for(double value: values_) {
            sum += value;
            max = Double.max(value, max);
            min = Double.min(value, min);
        }
        double avg = sum / (double)values_.size();
        data.setMinMaxAvg(min, max, avg);
        if(moving_) { // moving average algorithm with window size = count_;
            values_.remove(0);
            timestamps_.remove(0);
        } else { // window algorithm
            values_.clear();
            timestamps_.clear();
        }
    }

    boolean timeExceeded() {
        return (timestamps_.get(timestamps_.size()-1) - timestamps_.get(0)) >= ns_;
    }

    List<Double> values_ = new ArrayList<>();
    List<Long> timestamps_ = new ArrayList<>();
    Logger log_;

    static boolean useTime_ = false;    // time window if true; count window if false.
    static int count_ = 25;             // 25 sample count window.
    static long ns_ = 600_000_000_000L; // 10 minutes default in nanoseconds.
    static boolean moving_ = false;     // true will give a moving average with the count_ window;
                                        // false is a simple window.
}
