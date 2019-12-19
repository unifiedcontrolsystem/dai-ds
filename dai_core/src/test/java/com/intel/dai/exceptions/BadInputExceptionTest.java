package com.intel.dai.exceptions;

import org.junit.Test;

import static org.junit.Assert.*;

public class BadInputExceptionTest {
    @Test
    public void canThrowWithMessage() {
        String message = ":o!";
        try {
            throw new BadInputException(message);
        } catch (BadInputException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    @Test
    public void canThrowWithMessageAndCause() {
        String message = ":o!";
        Exception cause = new Exception(":O!!");
        try {
            throw new BadInputException(message, cause);
        } catch (BadInputException ex) {
            assertEquals(message, ex.getMessage());
            assertEquals(cause, ex.getCause());
        }
    }

}