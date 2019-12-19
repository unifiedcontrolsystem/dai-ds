// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.runtime_utils;

import com.intel.logging.LoggerFactory;
import com.intel.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class RuntimeCommandTest {
    @Test
    public void executesProvidedCommand() throws IOException, InterruptedException {
        setUpRuntimeCommand(ECHO_HELLO, HELLO_OUT, "");

        command.execute();

        // Should call Runtime.exec() with provided command
        Mockito.verify(mockRuntime, Mockito.times(1)).exec(command.tokenizeQuotedCommandWithSpaces(ECHO_HELLO));
        Mockito.when(mockProcess.getErrorStream()).thenReturn(null);
        Mockito.when(mockProcess.getInputStream()).thenReturn(null);
        command.execute();
    }

    @Test
    public void executesSameCommandOnSuccessiveCalls() throws IOException, InterruptedException {
        setUpRuntimeCommand(ECHO_HELLO, HELLO_OUT, "");

        command.execute();
        command.execute();

        // Should call command twice
        Mockito.verify(mockRuntime, Mockito.times(2)).exec(command.tokenizeQuotedCommandWithSpaces(ECHO_HELLO));
    }

    @Test
    public void retrievesCommandStdOutput() throws IOException, InterruptedException {
        setUpRuntimeCommand(ECHO_HELLO, HELLO_OUT, "");

        command.execute();

        // Should wait for command to finish executing
        Mockito.verify(mockProcess, Mockito.times(1)).waitFor();
        // Verify output
        Assert.assertEquals(HELLO_OUT + "\n", command.getStdOut());
        Assert.assertEquals("", command.getStdErr());
    }

    @Test
    public void retrievesCommandStdError() throws IOException, InterruptedException {
        setUpRuntimeCommand(ECO_HELLO, "", HELLO_ERR);

        command.execute();

        // Should wait for command to finish executing
        Mockito.verify(mockProcess, Mockito.times(1)).waitFor();
        // Verify output
        Assert.assertEquals("", command.getStdOut());
        Assert.assertEquals(HELLO_ERR + "\n", command.getStdErr());
    }

    @Test
    public void checkDifferentQuotedStringsInTokenizer() {
        Logger log = Mockito.mock(Logger.class);
        RuntimeCommand cmd = new RuntimeCommand(new String[] {}, log);
        cmd.runtime = Mockito.mock(Runtime.class);
        Assert.assertEquals(0, cmd.tokenizeQuotedCommandWithSpaces("").length);
        Assert.assertArrayEquals(new String[] {"first", "second", "third one"},
                cmd.tokenizeQuotedCommandWithSpaces("first second 'third one'"));
        Assert.assertArrayEquals(new String[] {"first", "second", "third one again"},
                cmd.tokenizeQuotedCommandWithSpaces("first second \"third one again\""));
        Assert.assertArrayEquals(new String[] {"first", "second", "third one again"},
                cmd.tokenizeQuotedCommandWithSpaces("'first' \"second\" \"third one again\""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkDifferentNegativeQuotedStringsInTokenizer() {
        Logger log = Mockito.mock(Logger.class);
        RuntimeCommand cmd = new RuntimeCommand(new String[] {}, log);
        cmd.runtime = Mockito.mock(Runtime.class);
        Assert.assertArrayEquals(new String[] {"first", "second", "third one again"},
                cmd.tokenizeQuotedCommandWithSpaces("first second \"third one again'"));
    }

    private void setUpRuntimeCommand(String cmd, String stdOut, String stdErr) throws IOException {
        mockProcess = Mockito.mock(Process.class);
        ByteArrayInputStream out = new ByteArrayInputStream(stdOut.getBytes());
        ByteArrayInputStream err = new ByteArrayInputStream(stdErr.getBytes());
        Mockito.when(mockProcess.getInputStream()).thenReturn(out);
        Mockito.when(mockProcess.getErrorStream()).thenReturn(err);

        mockRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockRuntime.exec(Mockito.anyString())).thenReturn(mockProcess);
        Mockito.when(mockRuntime.exec(Mockito.any(String[].class))).thenReturn(mockProcess);

        Logger log = Mockito.mock(Logger.class);
        command = new RuntimeCommand(cmd, log);
        command.runtime = mockRuntime;
    }

    private static final String ECHO_HELLO = "echo hello";
    private static final String HELLO_OUT = "hello";
    private static final String ECO_HELLO = "eco hello";
    private static final String HELLO_ERR = "bash: eco: command not found...";

    private Process mockProcess;
    private Runtime mockRuntime;
    private RuntimeCommand command;
}