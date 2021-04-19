// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.runtime_utils

import spock.lang.Specification

class ControlledShutdownSpec extends Specification {
    static class DelayedShutdown implements Runnable {
        @Override
        void run() {
            Thread.sleep(700)
            ControlledShutdown.shuttingDown()
        }
    }
    void setup() {
        cleanup()
    }

    void cleanup() {
        if(ControlledShutdown.shutdownThread_ != null) {
            Runtime.getRuntime().removeShutdownHook(ControlledShutdown.shutdownThread_)
            ControlledShutdown.shutdownThread_ = null
        }
        ControlledShutdown.state_ = ControlledShutdown.InternalState.NotStarted
    }

    def "Test startApplication"() {
        ControlledShutdown.startApplication()
        expect: !ControlledShutdown.isShuttingDown()
    }

    def "Test waitForShutdown"() {
        ControlledShutdown.startApplication()
        new Thread(new DelayedShutdown()).start()
        ControlledShutdown.waitForShutdown()
        ControlledShutdown.InternalState before = ControlledShutdown.state_
        ControlledShutdown.endApplication()
        expect: before                    == ControlledShutdown.InternalState.ShuttingDown
        and:    ControlledShutdown.state_ == ControlledShutdown.InternalState.ShutDown
    }

    def "Test startApplication Negative"() {
        ControlledShutdown.startApplication()
        when: ControlledShutdown.startApplication()
        then: thrown(ControlledShutdown.IllegalApplicationState)
    }

    def "Test endApplication Negative"() {
        ControlledShutdown.startApplication()
        new Thread(new DelayedShutdown()).start()
        ControlledShutdown.waitForShutdown()
        ControlledShutdown.InternalState before = ControlledShutdown.state_
        ControlledShutdown.endApplication()
        when: ControlledShutdown.endApplication()
        then: thrown(ControlledShutdown.IllegalApplicationState)
    }
}
