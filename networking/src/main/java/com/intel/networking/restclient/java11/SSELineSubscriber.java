// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient.java11;

import com.intel.logging.Logger;
import com.intel.networking.restclient.EventSource;
import com.intel.networking.restclient.SSEEvent;

import java.util.concurrent.Flow;

/**
 * A line subscriber that uses the EventSource class to parse an incoming SSE stream. Please
 * see the java documentation for class: java.util.concurrent.Flow.Subscriber.
 */
class SSELineSubscriber implements Flow.Subscriber<String> {
    SSELineSubscriber(SSEEvent callback, Logger log) {
        callback_ = callback;
        log_ = log;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        log_.debug("*** LineSubscriber subscribed.");
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(String s) {
        event.processLine(s, callback_);
    }

    @Override
    public void onError(Throwable throwable) {
        log_.exception(throwable);
    }

    @Override
    public void onComplete() {
        log_.debug("*** LineSubscriber completed.");
    }

    private final SSEEvent callback_;
    private EventSource event = new EventSource();
    private Logger log_;
}
