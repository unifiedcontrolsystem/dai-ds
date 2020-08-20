// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.authentication;

/**
 * Exception thrown by the implementation of the TokenAuthentication interface.
 */
public class TokenAuthenticationException extends Exception {
    public TokenAuthenticationException(String msg) { super(msg); }
    private final static long serialVersionUID = 1345338L; // Compiler Required...
}
