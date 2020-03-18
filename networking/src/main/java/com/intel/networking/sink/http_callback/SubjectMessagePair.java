// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.http_callback;

/**
 * Description of class SubjectMessagePair which is a specialize storage pair abstraction for subject and message pair.
 */
public class SubjectMessagePair {
    public SubjectMessagePair(String subject, String message) {
        this.subject = subject;
        this.message = message;
    }

    public final String subject;
    public final String message;
}
