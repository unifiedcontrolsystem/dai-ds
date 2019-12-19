// Copyright (C) 2017-2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.perflogging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * Object for logging performance data
 */
public class PerfLogger {

    // Constructor
    private PerfLogger(int targetCount, String outputFile ) {
        this.targetCount_ = targetCount;
        this.outputFile_ = outputFile;
    }

    public static PerfLogger getPerfLogger() {

        PerfLogger out = null;

        if ( System.getProperty("perfLogging", "false").equalsIgnoreCase("true")) {
            out = new PerfLogger(
                    Integer.parseInt(System.getProperty("perfTestNum", "100")),
                    System.getProperty("perfTestOutput", "/tmp/perflog.log"));
        }

        return out;
    }


    public void setTargetCount(int targetCount){
        this.targetCount_ = targetCount;
    }


    public static void logEvent ( PerfLogger logger, String event ) {
        if (logger != null) {
            try { logger._logEvent(event); } catch ( IOException ignore ) { }
        }
    }

    public static void incrementCount ( PerfLogger logger, int inc ) {
        if (logger != null) {
             logger._incrementCount(inc);
        }
    }

    public static void startLogging ( PerfLogger logger, String event ) {
        if ( logger != null && !logger.logging_ ) {
            logger.logging_ = true;
            try { logger._logEvent(event); } catch (IOException ignore ) {}
        }
    }

    private void _incrementCount(int inc) {
        this.count_ += inc;
    }

    private void _logEvent(String event) throws IOException {
        if ( met_ || targetCount_ == 0 ) return;
        if ( count_ == 0 ) {
            firstEvent_ = event;
            firstTimestamp_ = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            count_++;
        } else if ( count_ >= targetCount_ - 1 ) {
            Calendar lastTimestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            met_ = true;

            File fp = new File(outputFile_);

            try (FileWriter fw = new FileWriter(fp, fp.exists())) {
                BufferedWriter writer = new BufferedWriter(fw);
                writer.write(firstTimestamp_.getTime().getTime() + " : " + firstEvent_);
                writer.newLine();
                writer.write(lastTimestamp.getTime().getTime() + " : " + event);
                writer.newLine();
                writer.close();
            }

        } else {
            count_++;
        }
    }

    private int count_ = 0;
    private int targetCount_;
    private Calendar firstTimestamp_;
    private String firstEvent_;
    private String outputFile_;
    private boolean met_ = false;
    private boolean logging_ = false;




}
