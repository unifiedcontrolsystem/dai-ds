// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

/**
 * Description of class RESTServerException. Please the description of Exception in java.lang package.
 */
public class RESTServerException extends Exception {
    public RESTServerException() { super(); }
    public RESTServerException(String msg) { super(msg); }
    public RESTServerException(Throwable cause) { super(cause); }
    public RESTServerException(String msg, Throwable cause) { super(msg, cause); }

    public static final long serialVersionUID = 16384L;
}
