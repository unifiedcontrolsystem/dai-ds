// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

/**
 * Main exception class for exceptions occured between events simulator and its subcomponents
 */
@SuppressWarnings("serial")
public class SimulatorException extends Exception {
    public SimulatorException(String message) {
        super(message);
    }
    public SimulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
