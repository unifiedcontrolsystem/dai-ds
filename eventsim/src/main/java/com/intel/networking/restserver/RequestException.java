// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

/**
 * Description of class RequestException. Please the description of Exception in java.lang package.
 */
public class RequestException extends RESTServerException {
    public RequestException() { super(); }
    public RequestException(Throwable cause) { super(cause); }
    public RequestException(String message) { super(message); }
    public RequestException(String message, Throwable cause) { super(message, cause); }

    static final long serialVersionUID = 16385L;
}
