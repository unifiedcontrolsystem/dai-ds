// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

/**
 * Main exception class for exceptions occured between events simulator and result output component
 */
@SuppressWarnings("serial")
public class ResultOutputException extends Exception {
    ResultOutputException(String message) { super(message); }
}
