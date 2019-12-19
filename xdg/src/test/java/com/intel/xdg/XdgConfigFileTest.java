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

import org.junit.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import static org.junit.Assert.*;

public class XdgConfigFileTest implements ICustomStreamFactory {
    private static String TMP_FOLDER = "/tmp";

    private class XdgConfigFileMock extends XdgConfigFile {
        XdgConfigFileMock() { super(); }
        // XdgConfigFileMock(String name) { super(name); }
        XdgConfigFileMock(String name, boolean opt) { super(name, opt); }

        @Override
        protected InputStream CustomStream(String baseName) {
            try {
                return new FileInputStream("/etc/hosts");
            } catch(IOException e) {
                return null;
            }
        }

        @Override
        protected String getVar(String name, String defaultValue) {
            switch(name) {
                case "HOME":
                    return TMP_FOLDER;
                case "XDG_CONFIG_DIRS":
                    return TMP_FOLDER + "/.etc/xdg";
                case "XDG_CONFIG_HOME":
                    return TMP_FOLDER + "/.config";
                default:
                    return null;
            }
        }
    }

    private class XdgConfigFileMock2 extends XdgConfigFile {
        @Override
        protected String getVar(String name, String defaultValue) {
            return null;
        }
    }

    private static String home_;
    private static String xdgDirs_;
    private static String xdgConfigHome_;
    private static String testName_ = "testCase.conf";
    private static String componentName_ = "unitTest";

    @BeforeClass
    public static void setUpAll() throws Exception {
        Map<String,String> vars = System.getenv();
        home_ = vars.getOrDefault("HOME", null);
        xdgDirs_ = vars.getOrDefault("XDG_CONFIG_DIRS", TMP_FOLDER + "/.etc/xdg");
        xdgConfigHome_ = vars.getOrDefault("XDG_CONFIG_HOME", home_ + "/.config");
        File f = new File(TMP_FOLDER + "/.config/" + componentName_);
        assertTrue(f.mkdirs());
        f = new File(TMP_FOLDER + "/.etc/xdg/" + componentName_ + ".d");
        assertTrue(f.mkdirs());
    }

    @AfterClass
    public static void tearDownAll() {
        File f = new File(TMP_FOLDER + "/.etc/xdg/" + componentName_ + ".d");
        f.delete();
        f = new File(TMP_FOLDER + "/.etc/xdg");
        f.delete();
        f = new File(TMP_FOLDER + "/.etc");
        f.delete();
        f = new File(TMP_FOLDER + "/.config/" + componentName_);
        f.delete();
        f = new File(TMP_FOLDER + "/.config");
        f.delete();
    }

    @Override
    public InputStream CustomStream(String baseName) {
        try {
            return new FileInputStream("/etc/hosts");
        } catch(IOException e) {
            return null;
        }
    }

    private void addHome(ArrayList<String> dirs, String name) {
        dirs.add(xdgConfigHome_);
        if(name != null)
            dirs.add(xdgConfigHome_ + "/" + name);
    }

    private void addEtc(ArrayList<String> dirs, String name) {
        dirs.add("/etc");
        if(name != null) {
            dirs.add("/etc/" + name);
            dirs.add("/etc/" + name + ".d");
        }
    }

    private void addOpt(ArrayList<String> dirs, String name) {
        dirs.add("/opt/etc");
        if(name != null) {
            dirs.add("/opt/" + name + "/etc");
            dirs.add("/opt/etc/" + name);
            dirs.add("/opt/etc/" + name + ".d");
        }
    }

    private void addXdgConfigDirs(ArrayList<String> dirs, String name) {
        for(String value: xdgDirs_.split(":")) {
            dirs.add(value);
            if(name != null) {
                dirs.add(value + "/" + name);
                dirs.add(value + "/" + name + ".d");
            }
        }
    }

    @Test
    public void getVar() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile();
        assertEquals(home_, xdg.getVar("HOME", "red_value"));
        assertEquals("red_value", xdg.getVar("HOME99", "red_value"));
        assertEquals("red", xdg.getVar("EMPTY_TEST_VAR", "red"));
    }

    @Test
    public void ctor_no_component_no_opt() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile(null, false);
        ArrayList<String> dirs = new ArrayList<>();
        addHome(dirs, null);
        dirs.add("{CUSTOM}");
        addEtc(dirs, null);
        addXdgConfigDirs(dirs, null);
        assertEquals(String.join(":", dirs), xdg.getSearchPath());
        assertNull(xdg.getComponentName());
    }

    @Test
    public void ctor_component_and_opt() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile(componentName_, true);
        ArrayList<String> dirs = new ArrayList<>();
        addHome(dirs, componentName_);
        dirs.add("{CUSTOM}");
        addEtc(dirs, componentName_);
        addOpt(dirs, componentName_);
        addXdgConfigDirs(dirs, componentName_);
        assertEquals(String.join(":", dirs), xdg.getSearchPath());
        assertEquals(xdg.getComponentName(), componentName_);
    }

    @Test
    public void ctor_component_and_no_opt() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile(componentName_, false);
        ArrayList<String> dirs = new ArrayList<>();
        addHome(dirs, componentName_);
        dirs.add("{CUSTOM}");
        addEtc(dirs, componentName_);
        addXdgConfigDirs(dirs, componentName_);
        assertEquals(String.join(":", dirs), xdg.getSearchPath());
    }

    @Test
    public void ctor_no_component_and_opt() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile(null, true);
        ArrayList<String> dirs = new ArrayList<>();
        addHome(dirs, null);
        dirs.add("{CUSTOM}");
        addEtc(dirs, null);
        addOpt(dirs, null);
        addXdgConfigDirs(dirs, null);
        assertEquals(String.join(":", dirs), xdg.getSearchPath());
    }

    @Test
    public void open_hosts() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile();
        assertNotNull(xdg.Open("hosts"));
    }

    @Test
    public void open_ssh_config() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile("ssh");
        assertNotNull(xdg.Open("ssh_config"));
    }

    @Test
    public void open_in_home_0() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile();
        assertNull(xdg.Open("hosts99"));
    }

    @Test
    public void open_in_home_1() throws Exception {
        XdgConfigFile xdg = new XdgConfigFileMock();
        File file = new File(TMP_FOLDER + "/.config/" + testName_);
        file.createNewFile();
        InputStream stream = xdg.Open(testName_);
        assertTrue(file.delete());
        assertNotNull(stream);
    }

    @Test
    public void open_in_home_2() throws Exception {
        XdgConfigFile xdg = new XdgConfigFileMock();
        File file = new File(TMP_FOLDER + "/.etc/xdg/" + testName_);
        file.createNewFile();
        InputStream stream = xdg.Open(testName_);
        assertTrue(file.delete());
        assertNotNull(stream);
    }

    @Test
    public void open_in_home_3() throws Exception {
        XdgConfigFile xdg = new XdgConfigFileMock(componentName_, false);
        File file = new File(TMP_FOLDER + "/.etc/xdg/" + componentName_ + ".d/" + testName_);
        file.createNewFile();
        InputStream stream = xdg.Open(testName_);
        assertTrue(file.delete());
        assertNotNull(stream);
    }

    @Test
    public void open_in_home_4() throws Exception {
        XdgConfigFile xdg = new XdgConfigFileMock(componentName_, false);
        File file = new File(TMP_FOLDER + "/.etc/xdg/" + componentName_ + ".d/" + testName_);
        file.createNewFile();
        InputStream stream = xdg.Open(testName_, this);
        assertTrue(file.delete());
        assertNotNull(stream);
    }

    @Test
    public void open_negative_1() throws Exception {
        try {
            XdgConfigFile xdg = new XdgConfigFile();
            xdg.Open(null);
            fail("Open with null is not allowed!");
        } catch (IllegalArgumentException e) {
            // Pass...
        }
    }

    @Test
    public void open_negative_2() throws Exception {
        try {
            XdgConfigFile xdg = new XdgConfigFile();
            xdg.Open("");
            fail("Open with empty string is not allowed!");
        } catch (IllegalArgumentException e) {
            // Pass...
        }
    }

    @Test
    public void customStream() throws Exception {
        XdgConfigFile xdg = new XdgConfigFile();
        assertNull(xdg.CustomStream("any_file.conf"));
    }

    @Test
    public void test_constructor_negative_1() throws Exception {
        try {
            XdgConfigFile xdg = new XdgConfigFile("");
            fail("Empty string incorrectly accepted!");
        } catch(IllegalArgumentException e) {
            // Pass test.
        }
    }

    @Test
    public void home_is_empty() throws Exception {
        try {
            XdgConfigFile xdg = new XdgConfigFileMock2();
            fail("No exception was thrown and HOME was undefined or empty!");
        } catch (RuntimeException e) {
            // Pass...
        }
    }

    @Test
    public void find_file() throws Exception {
        XdgConfigFile xdg = new XdgConfigFileMock();
        File file = new File(TMP_FOLDER + "/.config/" + testName_);
        file.createNewFile();
        String foundFile = xdg.FindFile(testName_);
        assertTrue(file.delete());
        assertEquals("/tmp/.config/testCase.conf", foundFile);
    }

    @Test
    public void find_file_2() throws Exception {
        XdgConfigFile xdg = new XdgConfigFileMock();
        File file = new File(TMP_FOLDER + "/.config/unknown_file_txt");
        String foundFile = xdg.FindFile(testName_);
        assertEquals(null, foundFile);
    }

    @Test
    public void find_file_negative() throws Exception {
        try {
            XdgConfigFile xdg = new XdgConfigFile();
            xdg.FindFile("");
            fail("Open with empty string is not allowed!");
        } catch (IllegalArgumentException e) {
            // Pass...
        }
        try {
            XdgConfigFile xdg = new XdgConfigFile();
            xdg.FindFile(null);
            fail("Open with a null string is not allowed!");
        } catch (IllegalArgumentException e) {
            // Pass...
        }
    }
}
