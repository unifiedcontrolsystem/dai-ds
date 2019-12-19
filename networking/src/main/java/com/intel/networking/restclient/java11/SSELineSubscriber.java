// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient.java11;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RequestInfo;
import com.intel.networking.restclient.SSEEvent;

import java.util.concurrent.Flow;

class SSELineSubscriber implements Flow.Subscriber<String> {
    private static class EventSource {
        void reset() {
            event = "";
            data = "";
        }
        String event = "";
        String data = "";
        String id = null;
    }

    SSELineSubscriber(RequestInfo info, SSEEvent callback, Logger log) {
        info_ = info;
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
        if (s.startsWith(":"))
            return;
        if(s.isBlank()) { // do dispatch...
            if(event.data.isBlank()) { // empty event; reset and abort
                event.reset();
                return;
            }
            if(callback_ != null)
                callback_.event(event.event, event.data, event.id);
            event.reset();
        }
        else if (s.startsWith("event:"))
            event.event = s.substring(6).trim();
        else if(s.startsWith("data:"))
            event.data += s.substring(5).trim();
        else if(s.startsWith("id:"))
            event.id = s.substring(3).trim();
    }

    @Override
    public void onError(Throwable throwable) {
        log_.exception(throwable);
    }

    @Override
    public void onComplete() {
        log_.debug("*** LineSubscriber completed.");
    }

    private final RequestInfo info_;
    private final SSEEvent callback_;
    private EventSource event = new EventSource();
    private Logger log_;
}
