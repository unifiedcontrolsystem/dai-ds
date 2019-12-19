// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking;

/**
 * Description of class NetworkException.
 */
public class NetworkException extends RuntimeException {
    public NetworkException(String cause) { super(cause); }

    public static final long serialVersionUID = 16340L;
}
