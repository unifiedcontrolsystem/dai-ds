// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.eventsim.java11;

import com.intel.networking.restserver.Response;
import com.intel.networking.restserver.ResponseException;
import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;
import java.util.Map;

/**
 * Description of class HttpExchangeResponse. This is an internal class for HTTP responses.
 */
class HttpExchangeResponse implements Response {
    HttpExchangeResponse(HttpExchange exchange) {
        exchange_ = exchange;
    }

    @Override
    public void setCode(int code) {
        code_ = code;
    }

    @Override
    public void setBody(String body) {
        body_ = body;
    }

    @Override
    public void addHeader(String key, String value) {
        headers_.put(key, value);
    }

    @Override
    public void applyChanges() throws ResponseException {
        // No need to implement here!
    }

    final HttpExchange exchange_;
    String body_ = "";
    int code_;
    Map<String,String> headers_ = new HashMap<>();
}
