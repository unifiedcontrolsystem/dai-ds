package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.EOFException;

public class HWInvNotificationTranslator {

    public HWInvNotificationTranslator() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    public ForeignHWInvChangeNotification toPOJO(String json) {
        try {
            return gson.fromJson(json, ForeignHWInvChangeNotification.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    private transient Gson gson;
}
