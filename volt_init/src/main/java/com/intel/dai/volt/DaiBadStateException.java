// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.volt;

/**
 * Exception class thrown when the VoltDB either is already in a bad state or entered a bad state during VoltDB
 * initialization.
 */
@SuppressWarnings("serial")
public class DaiBadStateException extends Exception {
    DaiBadStateException(String msg) { super(msg); }
}
