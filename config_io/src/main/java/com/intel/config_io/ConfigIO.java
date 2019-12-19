// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import com.intel.properties.*;

import java.io.*;

/**
 * A configuration parser and emitter interface.
 */
public interface ConfigIO {
    /**
     * Create a PropertyDocument (either PropertyMap or PropertyArray) from a filename.
     *
     * @param filename The filename as a String of the file to read from.
     * @return The PropertyDocument object representing the configuration in the filename.
     * @throws IOException Thrown if there is a problem accessing the filename (missing, reading, permission, etc...)
     * @throws ConfigIOParseException Thrown if the parser instance fails to parse the contents of the filename.
     */
    PropertyDocument readConfig(String filename) throws IOException, ConfigIOParseException;

    /**
     * Create a PropertyDocument (either PropertyMap or PropertyArray) from a File object.
     *
     * @param file The File object of the file to read from.
     * @return The PropertyDocument object representing the configuration in the file.
     * @throws IOException Thrown if there is a problem accessing the file (missing, reading, permission, etc...)
     * @throws ConfigIOParseException Thrown if the parser instance fails to parse the contents of the file.
     */
    PropertyDocument readConfig(File file) throws IOException, ConfigIOParseException;

    /**
     * Create a PropertyDocument (either PropertyMap or PropertyArray) from a InputStream.
     *
     * @param stream The File object of the file to read from.
     * @return The PropertyDocument object representing the configuration in the stream.
     * @throws IOException Thrown if there is a problem accessing the stream (reading, etc...)
     * @throws ConfigIOParseException Thrown if the parser instance fails to parse the contents of the stream.
     */
    PropertyDocument readConfig(InputStream stream) throws IOException, ConfigIOParseException;

    /**
     * Create a PropertyDocument (either PropertyMap or PropertyArray) from a Reader.
     *
     * @param reader The File object of the file to read from.
     * @return The PropertyDocument object representing the configuration in the reader.
     * @throws IOException Thrown if there is a problem accessing the reader (reading, etc...)
     * @throws ConfigIOParseException Thrown if the parser instance fails to parse the contents of the reader.
     */
    PropertyDocument readConfig(Reader reader) throws IOException, ConfigIOParseException;

    /**
     * Write the configuration in PropertyDocument (either PropertyMap or PropertyArray) to a filename.
     *
     * @param document The document to write.
     * @param filename The filename as a String to emmit the document to.
     * @throws IOException Thrown if the file cannot be written to or created.
     */
    void writeConfig(PropertyDocument document, String filename) throws IOException;

    /**
     * Write the configuration in PropertyDocument (either PropertyMap or PropertyArray) to a File.
     *
     * @param document The document to write.
     * @param file The file as a File to emmit the document to.
     * @throws IOException Thrown if the file cannot be written to or created.
     */
    void writeConfig(PropertyDocument document, File file) throws IOException;

    /**
     * Write the configuration in PropertyDocument (either PropertyMap or PropertyArray) to a OutputStream.
     *
     * @param document The document to write.
     * @param stream The stream as a OutputStream to emmit the document to.
     * @throws IOException Thrown if the stream cannot be written.
     */
    void writeConfig(PropertyDocument document, OutputStream stream) throws IOException;

    /**
     * Write the configuration in PropertyDocument (either PropertyMap or PropertyArray) to a Writer.
     *
     * @param document The document to write.
     * @param writer The writer as a Writer to emmit the document to.
     * @throws IOException Thrown if the writer cannot be written.
     */
    void writeConfig(PropertyDocument document, Writer writer) throws IOException;

    /**
     * From a String containing a configuration document, create the PropertyDocument.
     *
     * @param strConfig The document encoded in a String.
     * @return The resultant PropertyDocument.
     * @throws ConfigIOParseException
     */
    PropertyDocument fromString(String strConfig) throws ConfigIOParseException;

    /**
     * From a PropertyDocument create the corresponding text document in a String.
     *
     * @param document The document to create as text.
     * @return The String of the document created from the PropertyDocument.
     */
    String toString(PropertyDocument document);

    /**
     * Set the indent for the String document emitted if the provider supports it.
     *
     * @param indent Either 0 for no "pretty print" or an indent value (usually 2-4 spaces).
     * @return this.
     */
    ConfigIO setIndent(int indent);
}
