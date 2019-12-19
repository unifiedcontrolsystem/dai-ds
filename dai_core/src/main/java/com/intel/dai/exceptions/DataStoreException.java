// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.exceptions;

/**
 * Main exception class for exceptions from the data store layer.  Any exceptions thrown from the data store layer must
 * either be a DataStoreException or derive from it.
 */
@SuppressWarnings("serial")
public class DataStoreException extends Exception {
    public DataStoreException(String message) {
        super(message);
    }

    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}