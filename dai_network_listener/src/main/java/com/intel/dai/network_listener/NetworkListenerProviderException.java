// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

/**
 * Exception only thrown by DataTransformer implementations.
 */
@SuppressWarnings("serial")
public class NetworkListenerProviderException extends Exception {
    public NetworkListenerProviderException() { super(); }
    public NetworkListenerProviderException(Throwable cause) { super(cause); }
    public NetworkListenerProviderException(String message) { super(message); }
    public NetworkListenerProviderException(String message, Throwable cause) { super(message, cause); }
}
