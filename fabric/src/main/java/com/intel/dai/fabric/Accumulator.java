// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric;

import com.intel.logging.Logger;

import java.util.ArrayList;
import java.util.List;

class Accumulator {
    public Accumulator(Logger logger) {
        log_ = logger;
    }

    public void addValue(FabricTelemetryItem data) {
        values_.add(data.getValue());
        log_.debug("Accumulator: value count now %d compared to %d", values_.size(), count_);
        timestamps_.add(data.getTimestamp());
        if(useTime_ && timeExceeded())
            doAggregation(data);
        else if(!useTime_ && values_.size() == count_)
            doAggregation(data);
    }

    private void doAggregation(FabricTelemetryItem data) {
        log_.debug("Generating aggregated data...");
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for(double value: values_) {
            sum += value;
            max = Double.max(value, max);
            min = Double.min(value, min);
        }
        double avg = sum / (double)values_.size();
        data.setStatistics(min, avg, max);
        if(moving_) { // moving average algorithm with window size = count_;
            values_.remove(0);
            timestamps_.remove(0);
        } else { // window algorithm
            values_.clear();
            timestamps_.clear();
        }
    }

    private boolean timeExceeded() {
        return (timestamps_.get(timestamps_.size()-1) - timestamps_.get(0)) >= us_;
    }

    private List<Double> values_ = new ArrayList<>();
    private List<Long> timestamps_ = new ArrayList<>();
    private Logger log_;

    static boolean useTime_ = false;   // time window if true; count window if false.
    static int     count_   = 25;      // 25 sample count window.
    static long    us_      = 600_000; // 10 minutes default in microseconds.
    static boolean moving_  = false;   // true will give a moving average with the count_ window;
                                       // false is a simple window.
}
