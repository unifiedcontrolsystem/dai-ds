// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.runtime_utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Control a threaded or non-threaded application's shutdown process. This is used to guarantee and orderly shutdown
 * with proper cleanup.
 */
public final class ControlledShutdown {
    /**
     * Called near the beginning of main() to setup a shutdown hook.
     */
    public static void startApplication() { startApplication(null); }

    /**
     * Called near the beginning of main() to setup a shutdown hook.
     *
     * @param callback If not null this functional interface is called during the shutdown hook to help debug issues.
     */
    public static void startApplication(DebugMessageCallback callback) {
        if(state_ != InternalState.NotStarted)
            throw new IllegalApplicationState(String.format("The '%s.startApplication' method must be called first!",
                    ControlledShutdown.class.getSimpleName()));
        callback_ = callback;
        shutdownThread_ = new Thread(ControlledShutdown::shuttingDown);
        Runtime.getRuntime().addShutdownHook(shutdownThread_);
        state_ = InternalState.Started;
        callCallback("ControlShutdown.startApplication called!");
    }

    /**
     * Called near the end of main to signal that all application cleanup was done and the process can end.
     */
    public static void endApplication() {
        if(state_ != InternalState.ShuttingDown)
            throw new IllegalApplicationState(String.format("The '%s.endApplication' method must be called after " +
                            "the application shutdown hook is called!", ControlledShutdown.class.getSimpleName()));
        waitForExit_.set(true);
        state_ = InternalState.ShutDown;
        callCallback("ControlShutdown.endApplication called!");
    }

    /**
     * This returns true if the shutdown hook was called. This is used on worker threads to determine if the
     * application needs to be shutdown and therefore the worker thread needs to end.
     *
     * @return true when shutting down (shutdown hook is called), false otherwise.
     */
    public static boolean isShuttingDown() {
        return state_ == InternalState.ShuttingDown;
    }

    /**
     * This will block until the shutdown hook is called. This is used when the application is a long running service
     * and there is a need to block on the main thread while work is done on a separate worker thread.
     */
    public static void waitForShutdown() {
        while(!isShuttingDown()) {
            try { Thread.sleep(1_000L); } // One second
            catch (InterruptedException e) { /* Ignore this interrupt. */ }
        }
    }

    /**
     * Do both a wait for shutdown signal and then end the application.
     */
    public static void waitForShutdownAndEnd() {
        waitForShutdown();
        endApplication();
    }

    private static void callCallback(String msg) {
        if(callback_ != null)
            callback_.debugMessage(msg);
    }

    private static void shuttingDown() {
        signalShutdown_.set(true);
        state_ = InternalState.ShuttingDown;
        callCallback("Shutdown hook called for ControlShutdown!");
        while(!waitForExit_.get()) {
            try { Thread.sleep(1_000L); } // One second
            catch (InterruptedException e) { /* Ignore this interrupt. */ }
        }
        callCallback("Shutdown hook exiting!");
    }
    private ControlledShutdown() {} // static class

    @FunctionalInterface
    public interface DebugMessageCallback {
        void debugMessage(String message);
    }

    private enum InternalState {
        NotStarted,
        Started,
        ShuttingDown,
        ShutDown
    }

    /**
     * Unchecked exception for calling startApplication or endApplication in the wrong order with the wrong
     * prerequisite states.
     */
    @SuppressWarnings("serial")
    public static class IllegalApplicationState extends RuntimeException {
        public IllegalApplicationState(String msg) { super(msg); }
    }

    private       static Thread shutdownThread_;
    private       static InternalState state_ = InternalState.NotStarted;
    private       static DebugMessageCallback callback_;
    private final static AtomicBoolean signalShutdown_ = new AtomicBoolean(false);
    private final static AtomicBoolean waitForExit_ = new AtomicBoolean(false);
}
