// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

/**
 * Description of class RESTClientException. Please the description of Exception in java.lang package.
 */
public class RESTClientException extends Exception {
    public RESTClientException() { super(); }
    public RESTClientException(String msg) { super(msg); }
    public RESTClientException(Throwable cause) { super(cause); }
    public RESTClientException(String msg, Throwable cause) { super(msg, cause); }

    public static final long serialVersionUID = 16387L;
}
