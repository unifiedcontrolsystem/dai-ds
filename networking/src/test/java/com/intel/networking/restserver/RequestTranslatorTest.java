package com.intel.networking.restserver;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestTranslatorTest {
    static class MockTranslator extends RequestTranslator {
        MockTranslator() {
            super();
            parser_ = mock(ConfigIO.class);
            try {
                when(parser_.fromString(anyString())).thenReturn(new PropertyMap());
            } catch(ConfigIOParseException e) { /* Ignore */ }
        }

        @Override public Set<String> getSSESubjects(PropertyMap bodyMap) {
            return new HashSet<>();
        }
    }

    @Before
    public void setUp() throws Exception {
        translator_ = new MockTranslator();
    }

    @Test
    public void getBodyMap() {
        PropertyMap map = translator_.getBodyMap("{}");
        assertEquals(0, map.size());
    }

    @Test(expected = RuntimeException.class)
    public void getBodyMapNegative() throws Exception {
        when(translator_.parser_.fromString(anyString())).thenThrow(ConfigIOParseException.class);
        translator_.getBodyMap("{}");
    }

    MockTranslator translator_;
}
