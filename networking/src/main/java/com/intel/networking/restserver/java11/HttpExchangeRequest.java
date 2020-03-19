// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver.java11;

import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.Request;
import com.intel.networking.restserver.RequestException;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of class HttpExchangeRequest. This is an internal class for HTTP requests.
 */
class HttpExchangeRequest implements Request {
    HttpExchangeRequest(HttpExchange exchange) {
        exchange_ = exchange;
    }

    @Override
    public HttpMethod getMethod() throws RequestException {
        return HttpMethod.valueOf(exchange_.getRequestMethod());
    }

    @Override
    public String getPath() throws RequestException {
        return exchange_.getRequestURI().getPath();
    }

    @Override
    public Map<String, String> getHeaders() throws RequestException {
        Map<String,String> results = new HashMap<>();
        for(String key: exchange_.getRequestHeaders().keySet()) {
            String[] values = new String[exchange_.getRequestHeaders().get(key).size()];
            exchange_.getRequestHeaders().get(key).toArray(values);
            results.put(key, String.join(",", values));
        }
        return results;
    }

    @Override
    public String getBody() throws RequestException {
        try {
            return readBody(exchange_.getRequestBody());
        } catch(IOException e) {
            throw new RequestException("Failed to read the request body stream", e);
        }
    }

    @Override
    public String getQuery() throws RequestException {
        return exchange_.getRequestURI().getQuery();
    }

    private String readBody(InputStream bodyInputStream) throws IOException {
        byte[] bytes = bodyInputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    private final HttpExchange exchange_;
}
