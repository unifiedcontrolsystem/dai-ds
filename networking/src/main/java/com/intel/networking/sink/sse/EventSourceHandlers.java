// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.networking.sink.sse;

interface EventSourceHandlers {
    /**
     * Called when the event source is closed due to error.
     */
    void onClosed();

    /**
     * Called when the event source has an error. The error is only fatal if onClosed is called after the error.
     *
     * @param cause The Exception that caused the error.
     */
    void onError(Exception cause);

    /**
     * Called when the event source starts listening to the SSE stream.
     */
    void onOpen();

    /**
     * Called when the event source is attempting to connect.
     */
    void onConnecting();

    /**
     * Called to report debug information.
     *
     * @param fmt  The string format for the following args or if no args then the string to report.
     * @param args The arguments for the format string.
     */
    void onDebug(String fmt, Object... args);

    /**
     * Called when a completed message is received.
     *
     * @param event   The "event:" tag for the message.
     * @param message The combined "data:" of the message.
     * @param id      The "id:" tag for the message. May be null or empty.
     */
    void onMessage(String event, String message, String id);
}
