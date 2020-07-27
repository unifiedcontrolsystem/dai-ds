package com.intel.dai.eventsim;

import com.intel.properties.PropertyDocument;

public class Event {

    Event(String streamId, PropertyDocument event) {
        this.streamId = streamId;
        this.message = event;
    }

    PropertyDocument message;
    String streamId;
}
