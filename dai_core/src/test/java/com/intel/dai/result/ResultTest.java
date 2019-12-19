package com.intel.dai.result;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResultTest {
    private Result result;

    @Before
    public void setUp() {
        result = new Result(0, "success");
    }

    @Test
    public void getReturnCode() {
        assertEquals(0, result.getReturnCode());
    }

    @Test
    public void setReturnCode() {
        result.setReturnCode(-1);
        assertEquals(-1, result.getReturnCode());
    }

    @Test
    public void getMessage() {
        assertEquals("success", result.getMessage());
    }

    @Test
    public void setMessage() {
        result.setMessage("failed");
        assertEquals("failed", result.getMessage());
    }

}