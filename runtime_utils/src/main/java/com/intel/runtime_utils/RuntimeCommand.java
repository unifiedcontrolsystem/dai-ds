// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.runtime_utils;

import com.intel.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

public class RuntimeCommand {
    private RuntimeCommand() {
        runtime = Runtime.getRuntime();
        stdOut = "";
        stdErr = "";
    }

    public RuntimeCommand(String command, Logger log) {
        this();
        this.command = tokenizeQuotedCommandWithSpaces(command);
        this.log = log;
    }

    public RuntimeCommand(String[] command, Logger log) {
        this();
        this.command = Arrays.copyOf(command, command.length);
        this.log = log;
    }

    public void execute() throws IOException, InterruptedException {
        execute(true, false);  // run the command and log any stdout and also log even uninteresting msgs.
    }


    public void execute(boolean bLogStdOut, boolean bDontLogUninterestingMsgs) throws IOException, InterruptedException {
        Process commandProcess = runtime.exec(command);

        try (InputStream outStream = commandProcess.getInputStream()) {
            if (outStream != null) {
                stdOut = extractFromStream(outStream);
                if (!stdOut.isEmpty())
                    log.info("stdout - '%s'", stdOut.trim());
            } else {
                stdOut = "";
            }
        }

        try (InputStream errStream = commandProcess.getErrorStream()) {
            if (errStream != null) {
                stdErr = extractFromStream(errStream);
                if (!stdErr.isEmpty())
                    log.error("stderr - '%s'", stdErr.trim());
            } else {
                stdErr = "";
            }
        }
        // Wait for the command to finish.
        commandProcess.waitFor();
    }

    public String getStdOut() {
        return stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    private String extractFromStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder strBuilder = new StringBuilder();

        String line = reader.readLine();
        while (line != null) {
            strBuilder.append(line);
            strBuilder.append("\n");
            line = reader.readLine();
        }

        return strBuilder.toString();
    }

    final String[] tokenizeQuotedCommandWithSpaces(String commandLine) {
        StringTokenizer tokener = new StringTokenizer(commandLine);
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder item = new StringBuilder();
        boolean inQuotes = false;
        String quoteType = "'";
        while(tokener.hasMoreTokens()) {
            String token = tokener.nextToken();
            if(inQuotes) {
                if(token.endsWith(quoteType)) {
                    item.append(" ");
                    item.append(token, 0, token.length() - 1);
                    tokens.add(item.toString());
                    item = new StringBuilder();
                    inQuotes = false;
                } else {
                    item.append(" ");
                    item.append(token);
                }
            } else {
                if (token.startsWith("\"") || token.startsWith("'")) {
                    char quote = token.charAt(0);
                    if(token.charAt(token.length() - 1) == quote)
                        tokens.add(token.substring(1, token.length() - 1));
                    else {
                        inQuotes = true;
                        quoteType = token.substring(0, 1);
                        item.append(token.substring(1));
                    }
                } else
                    tokens.add(token);
            }
        }
        if(inQuotes)
            throw new IllegalArgumentException("The command passed was not legally quoted: " + commandLine);
        String[] result = new String[tokens.size()];
        return tokens.toArray(result);
    }

    Runtime runtime;
    private String[] command;
    private String stdOut;
    private String stdErr;
    private Logger log;
}
