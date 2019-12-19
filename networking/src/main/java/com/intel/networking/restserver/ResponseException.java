// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

/**
 * Description of class ResponseException. Please the description of Exception in java.lang package.
 */
public class ResponseException extends RESTServerException {
    public ResponseException() { super(); }
    public ResponseException(Throwable cause) { super(cause); }
    public ResponseException(String message) { super(message); }
    public ResponseException(String message, Throwable cause) { super(message, cause); }

    static final long serialVersionUID = 16386L;
}
