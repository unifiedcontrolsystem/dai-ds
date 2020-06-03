// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intel.dai.dsapi.*;
import org.apache.commons.collections4.CollectionUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class HWInvUtilImpl implements HWInvUtil {
    private transient Gson gson;

    public HWInvUtilImpl() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    @Override
    public HWInvTree toCanonicalPOJO(Path canonicalHWInvPath) {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(canonicalHWInvPath.toString()),
                            StandardCharsets.UTF_8));
            return gson.fromJson(br, HWInvTree.class);
        } catch (RuntimeException e) {
            return null;
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    @Override
    public HWInvTree toCanonicalPOJO(String canonicalHWInvJson) {
        try {
            return gson.fromJson(canonicalHWInvJson, HWInvTree.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    @Override
    public String toCanonicalJson(HWInvTree tree) {
        Comparator<HWInvLoc> compareByID = Comparator.comparing((HWInvLoc o) -> o.ID);
        if (tree != null) {
            tree.locs.sort(compareByID);
        }
        return gson.toJson(tree);
    }

    @Override
    public String toCanonicalHistoryJson(HWInvHistory history) {
        Comparator<HWInvHistoryEvent> compareByID = Comparator.comparing((HWInvHistoryEvent o) -> o.ID);
        if (history != null) {
            history.events.sort(compareByID);
        }
        return gson.toJson(history);
    }

    @Override
    public HWInvHistory toCanonicalHistoryPOJO(String canonicalHWInvHistoryJson) {
        try {
            return gson.fromJson(canonicalHWInvHistoryJson, HWInvHistory.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            return null;
        }
    }

    @Override
    public void fromStringToFile(String str, String outputFileName) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName),
                StandardCharsets.UTF_8))) {
            bw.write(str);
            bw.flush();
        }
    }

    @Override
    public String fromFile(Path inputFilePath) throws IOException {
        return new String(Files.readAllBytes(inputFilePath), StandardCharsets.UTF_8);
    }

    @Override
    public List<HWInvLoc> subtract(List<HWInvLoc> list0, List<HWInvLoc> list1) {
        return (List<HWInvLoc>) CollectionUtils.subtract(list0, list1);
    }
}
