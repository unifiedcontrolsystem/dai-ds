// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

/**
 * Description of class SSEEvent.
 */
@FunctionalInterface
public interface SSEEvent {
    /**
     * Called for SSE events.
     *
     * @param eventType The event type for the event.
     * @param event The event document as a string.
     * @param id The SSE event ID or null.
     */
    void event(String eventType, String event, String id);
}
