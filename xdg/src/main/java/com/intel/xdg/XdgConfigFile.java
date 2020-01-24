// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2017-2018 Intel(r) Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.intel.xdg;

import java.io.*;
import java.util.*;


/**
 * Class to support opening a configuration file or a custom stream based on the XDG Base Directory Specification.
 *
 * https://developer.gnome.org/basedir-spec/
 */
public class XdgConfigFile {

    /**
     * Default Constructor with no component name and no /opt support.
     */
    public XdgConfigFile() { this(null, false); }

    /**
     * Constructor with component name and defaulting to support /opt.
     *
     * @param componentName The name of the component.
     * @throws IllegalArgumentException Thrown when componentName is an empty string rather than 'null'.
     */
    public XdgConfigFile(String componentName) { this(componentName, true); }

    /**
     * Constructor for manually supporting a component name and /opt.
     *
     * @param componentName The name of the component.
     * @param includeOpt Boolean to either include /opt support (true) or not (false).
     * @throws IllegalArgumentException Thrown when componentName is an empty string rather than 'null'.
     */
    public XdgConfigFile(String componentName, boolean includeOpt) {
        if(componentName != null && componentName.trim().equals(""))
            throw new IllegalArgumentException("The 'componentName' argument cannot be an empty string!");
        componentName_ = componentName;
        includeOpt_ = includeOpt;
        home_ = getVar("HOME", null);
        path_ = getConfigPath();
    }

    /**
     * Opens a stream from the baseName using the XDG specification where all other paths searched based on the XDG
     * Base Directory Specification.
     *
     * @param baseName The filename (with extension) to attempt to open as an InputStream.
     * @return An open InputStream in which to read the configuration information from or 'null'.
     * @throws IllegalArgumentException Only thrown if the baseName is not null and it trims down to an empty string.
     */
    final public InputStream Open(String baseName) throws IllegalArgumentException {
        return Open(baseName, null);
    }

    /**
     * Opens a stream from the baseName using the XDG specification where if factory is not 'null' then the order will
     * be a search where the users home is first, the custom stream is tried, then all other paths searched based on
     * the XDG Base Directory Specification.
     *
     * @param baseName The filename (with extension) or hint for the custom factory to attempt to open as an
     *                 InputStream.
     * @param factory A factory pointing to an implemented ICustomStreamFactory interface or if 'null' no factory is
     *                called.
     * @return An open InputStream in which to read the configuration information from or 'null'.
     * @throws IllegalArgumentException Only thrown if the baseName is not null and it trims down to an empty string.
     */
    final public InputStream Open(String baseName, ICustomStreamFactory factory)
            throws IllegalArgumentException {
        if (baseName == null || baseName.trim().equals(""))
            throw new IllegalArgumentException("The 'baseName' argument must not be null or an empty string!");
        return OpenFileInPath(baseName, getConfigPath(), factory);
    }

    /**
     * Instead of opening a file stream, just locate the file.
     * @param baseName The filename (with extension) or hint for the custom factory to attempt to open as an
     *                 InputStream.
     * @return The absolute path of the file as a String or null.
     * @throws IllegalArgumentException Only thrown if the baseName is not null and it trims down to an empty string.
     */
    final public String FindFile(String baseName)
            throws IllegalArgumentException {
        if (baseName == null || baseName.trim().equals(""))
            throw new IllegalArgumentException("The 'baseName' argument must not be null or an empty string!");
        AbstractCollection<String> path = getConfigPath();
        for(String dir: path) {
            if(!dir.equals("{CUSTOM}")) {
                File f = new File(String.format("%s/%s", dir, baseName));
                if (checkFile(f)) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     * Override in a derived class to implement a custome version of this class and return a custom InputStream for the
     * configuration. This class defaults to returning 'null' which means no custom stream is opened.
     *
     * The implementation must not throw any exceptions.
     *
     * @param baseName The base filename with extension or a hint for the custom InputStream creation.
     * @return A custom InputStream object or null if the custom stream fails or is not supported.
     */
    protected InputStream CustomStream(String baseName) { return null; }

    String getVar(String name, String defaultValue) {
        String value = System.getenv(name);
        if(value == null) return defaultValue;
        return value;
    }

    String getComponentName() { return componentName_;}

    String getSearchPath() { return String.join(":", path_); }

    private boolean checkFile(File file) { return file.canRead() && file.isFile(); }

    private AbstractCollection<String> getConfigPath() {
        AbstractCollection<String> dirs = new StringArrayListSet();
        String val;
        if(home_ != null) {
            val = getVar("XDG_CONFIG_HOME", home_ + "/.config");
            dirs.add(val);
            if(componentName_ != null) dirs.add(home_ + "/.config/" + componentName_);
        }
        dirs.add("{CUSTOM}");
        dirs.add("/etc");
        if(componentName_ != null) {
            dirs.add("/etc/" + componentName_);
            dirs.add("/etc/" + componentName_ + ".d");
        }
        if(includeOpt_) {
            dirs.add("/opt/etc");
            if(componentName_ != null) {
                dirs.add("/opt/" + componentName_ + "/etc");
                dirs.add("/opt/etc/" + componentName_);
                dirs.add("/opt/etc/" + componentName_ + ".d");
            }
        }
        val = getVar("XDG_CONFIG_DIRS", "/etc/xdg");
        ArrayList<String> parts = new ArrayList<>();
        for(String value: val.split(":")) {
            parts.add(value);
            if(componentName_ != null) {
                parts.add(value + "/" + componentName_);
                parts.add(value + "/" + componentName_ + ".d");
            }
        }
        dirs.addAll(parts);
        return dirs;
    }

    private InputStream OpenFileInPath(String baseName, AbstractCollection<String> path, ICustomStreamFactory factory) {
        for(String dir: path) {
            if(dir.equals("{CUSTOM}")) {
                InputStream stream;
                if (factory != null) stream = factory.CustomStream(baseName);
                else stream = CustomStream(baseName);
                if(stream != null) return stream;
            } else {
                File f = new File(String.format("%s/%s", dir, baseName));
                if (checkFile(f)) {
                    try {
                        return new FileInputStream(f.getAbsolutePath());
                    } catch (IOException e) {
                        // Ignore this case and continue...
                    }
                }
            }
        }
        return null;
    }

    // Fields
    private boolean includeOpt_;
    private String componentName_;
    private AbstractCollection<String> path_;
    private String home_;
}
