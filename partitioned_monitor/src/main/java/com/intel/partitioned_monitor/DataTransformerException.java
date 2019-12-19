// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

/**
 * Exception only thrown by DataTransformer implementations.
 */
@SuppressWarnings("serial")
public class DataTransformerException  extends Exception {
    public DataTransformerException() { super(); }
    public DataTransformerException(Throwable cause) { super(cause); }
    public DataTransformerException(String message) { super(message); }
    public DataTransformerException(String message, Throwable cause) { super(message, cause); }
}
