// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import com.intel.properties.PropertyDocument;

import java.io.*;

abstract class ConfigIOBase implements ConfigIO {
    @Override
    public PropertyDocument readConfig(String filename) throws IOException, ConfigIOParseException {
        return readConfig(new File(filename));
    }

    @Override
    public PropertyDocument readConfig(File file) throws IOException, ConfigIOParseException {
        try (InputStream stream = new FileInputStream(file)) {
            return readConfig(stream);
        }
    }

    @Override
    public PropertyDocument readConfig(InputStream stream) throws IOException, ConfigIOParseException {
        try (Reader reader = new InputStreamReader(stream)) {
            return readConfig(reader);
        }
    }

    @Override
    public PropertyDocument readConfig(Reader reader) throws IOException, ConfigIOParseException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader buffered = new BufferedReader(reader)) {
            String line = buffered.readLine();
            while (line != null) {
                builder.append(line);
                line = buffered.readLine();
            }
        }
        return fromString(builder.toString());
    }

    @Override
    public void writeConfig(PropertyDocument document, String filename) throws IOException {
        writeConfig(document, new File(filename));
    }

    @Override
    public void writeConfig(PropertyDocument document, File file) throws IOException {
        try (OutputStream stream = new FileOutputStream(file)) {
            writeConfig(document, stream);
        }
    }

    @Override
    public void writeConfig(PropertyDocument document, OutputStream stream) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {
            writeConfig(document, writer);
        }
    }

    @Override
    public void writeConfig(PropertyDocument document, Writer writer) throws IOException {
        if(document.isMap())
            writer.write(toString(document.getAsMap()));
        else
            writer.write(toString(document.getAsArray()));
    }

    @Override
    public ConfigIO setIndent(int indent) {
        if(indent < 0 || indent == 1 || indent > 8) throw new IllegalArgumentException("indent must be 0 or 2-8!");
        indent_ = indent;
        return this;
    }

    int indent() { return indent_; }

    protected int indent_ = 0;
}
