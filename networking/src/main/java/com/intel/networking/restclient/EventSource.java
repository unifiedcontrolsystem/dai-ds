// Copyright (C) 2020 Paul Amonson
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.networking.restclient;

/**
 * Description for class EventSource
 */
public class EventSource {
    public void processLine(String line, SSEEvent eventsCallback) {
        if (line.startsWith(":"))
            return;
        if(line.isBlank()) { // do dispatch...
            if(data.isBlank()) { // empty event; reset and abort
                reset();
                return;
            }
            if(eventsCallback != null)
                eventsCallback.event(event, data, id);
            reset();
        }
        else if (line.startsWith("event:"))
            event = line.substring(6).trim();
        else if(line.startsWith("data:"))
            data += line.substring(5).trim();
        else if(line.startsWith("id:"))
            id = line.substring(3).trim();
    }

    private void reset() {
        event = "";
        data = "";
    }

    private String event = "";
    private String data = "";
    private String id = null;
}
