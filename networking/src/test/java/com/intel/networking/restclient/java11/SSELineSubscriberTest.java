package com.intel.networking.restclient.java11;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RequestInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Flow;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class SSELineSubscriberTest {
    @Before
    public void setUp() {
        eventType_ = null;
        eventId_ = null;
        eventData_ = null;
        info_ = mock(RequestInfo.class);
        subscriber_ = new SSELineSubscriber(info_, this::events, mock(Logger.class));
    }

    private void events(String eventType, String event, String id) {
        eventType_ = eventType;
        eventId_ = id;
        eventData_ = event;
    }

    private void commonMessageStream() {
        subscriber_.onNext(":Comment");
        subscriber_.onNext("id:1");
        subscriber_.onNext("event:red");
        subscriber_.onNext("\n");
        subscriber_.onNext("event:red");
        subscriber_.onNext("retry:0");
        subscriber_.onNext("unknown:stuff");
        subscriber_.onNext("data:[10,");
        subscriber_.onNext("data:11,");
        subscriber_.onNext("data:12]");
        subscriber_.onNext("\n");
    }

    @Test
    public void onSubscribe() {
        subscriber_.onSubscribe(mock(Flow.Subscription.class));
    }

    @Test
    public void onNext() {
        commonMessageStream();
        assertEquals("red", eventType_);
        assertEquals("1", eventId_);
        assertEquals("[10,11,12]", eventData_);
    }

    @Test
    public void onNextNoCallback() {
        subscriber_ = new SSELineSubscriber(info_, null, mock(Logger.class));
        commonMessageStream();
    }

    @Test
    public void onError() {
        subscriber_.onError(new Exception("TEST"));
    }

    @Test
    public void onComplete() {
        subscriber_.onComplete();
    }

    private SSELineSubscriber subscriber_;
    private RequestInfo info_;
    private String eventType_;
    private String eventId_;
    private String eventData_;
}
