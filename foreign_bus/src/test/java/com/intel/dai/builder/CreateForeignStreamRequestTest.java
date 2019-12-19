package com.intel.dai.builder;

import com.intel.networking.restclient.SSERequestBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreateForeignStreamRequestTest extends CreateForeignStreamRequest{
    @Test
    public void buildGetRequestUri() {
        SSERequestBuilder getUri = new CreateForeignStreamRequestTest();
        ArrayList<String> subjects = new ArrayList<>();
        subjects.add("test");
        Map<String, String> builder = new HashMap<>();
        builder.put("param1", "val1");
        Assert.assertEquals("?subjects=test&param1=val1",  getUri.buildRequest(subjects,  builder));
    }
}
