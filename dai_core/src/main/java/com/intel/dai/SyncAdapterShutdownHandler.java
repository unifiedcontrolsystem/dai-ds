// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

import com.intel.logging.Logger;

class SyncAdapterShutdownHandler implements AdapterShutdownHandler {
    public SyncAdapterShutdownHandler(Logger log) {
        this(log, 0);
    }

    public SyncAdapterShutdownHandler(Logger log, long maxWaitTimeSec) {
        shutdownLock = new ReentrantLock();
        shutdownComplete = shutdownLock.newCondition();
        this.log = log;
        this.maxWaitTimeSec = maxWaitTimeSec;
    }

    @Override
    public void handleShutdown() {
        log.info("Shutdown handler invoked");
        shutdownLock.lock();
        try {
            if (maxWaitTimeSec > 0) {
                boolean timeUp = !shutdownComplete.await(maxWaitTimeSec, TimeUnit.SECONDS);
                if (timeUp) {
                    log.warn("Adapter shutdown exceeded time limit.  Forcing shutdown...");
                }
            } else {
                if(!shutdownComplete.await(5, TimeUnit.MINUTES))
                    log.warn("Awaiting shutdown for 5 minutes failed");
            }
        } catch (InterruptedException ex) {
            log.exception(ex, "Shutdown process in Adapter interrupted");
        } finally {
            shutdownLock.unlock();
        }
        log.info("Shutdown handler exiting...");
    }

    public void signalShutdownComplete() {
        shutdownLock.lock();
        try {
            shutdownComplete.signalAll();
        } catch(Exception e) {
            log.exception(e);
        } finally {
            shutdownLock.unlock();
        }
    }

    private Lock shutdownLock;
    private Condition shutdownComplete;
    private Logger log;
    private long maxWaitTimeSec;
}