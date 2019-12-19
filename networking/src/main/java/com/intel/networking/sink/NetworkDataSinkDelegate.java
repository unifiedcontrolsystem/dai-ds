// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.sink;

/**
 * The callback interface for receiving incoming messages from the provider.
 */
@FunctionalInterface
public interface NetworkDataSinkDelegate {
    /**
     * The method to call from the provider when a message is received.
     *
     * @param subject The subject the message was received with.
     * @param payload The message payload converted from bytes to a string using UTF-8 encoding.
     */
    void processIncomingData(String subject, String payload);
}
