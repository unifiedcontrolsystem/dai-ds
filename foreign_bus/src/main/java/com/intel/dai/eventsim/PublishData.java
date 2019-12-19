package com.intel.dai.eventsim;

public class PublishData {
    String subject_ = "";
    String message_ = "";

    PublishData(String subject, String message) {
        this.subject_ = subject;
        this.message_ = message;
    }
}