package com.intel.networking.restserver;

import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class DefaultRequestTranslatorTest {

    @Before
    public void setUp() throws Exception {
        translator_ = new DefaultRequestTranslator();
    }

    @Test
    public void getSSESubjects() {
        PropertyMap map = translator_.getBodyMap("{\"subjects\":[\"sub1\",\"sub2\"]}");
        Set<String> subjects = translator_.getSSESubjects(map);
        assertEquals(2, subjects.size());
        assertTrue(subjects.contains("sub1"));
        assertTrue(subjects.contains("sub2"));
    }

    private DefaultRequestTranslator translator_;
}
