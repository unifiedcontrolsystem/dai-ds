// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import org.apache.commons.collections4.CollectionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.intel.dai.dsapi.HWInvLoc;
import com.intel.dai.dsapi.HWInvTree;
import com.intel.dai.dsapi.HWInvUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

    public HWInvTree toCanonicalPOJO(String inputFileName) throws IOException, JsonIOException, JsonSyntaxException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName),
                StandardCharsets.UTF_8));
        return gson.fromJson(br, HWInvTree.class);
    }
    public String toCanonicalJson(HWInvTree tree) {
        Comparator<HWInvLoc> compareByID = Comparator.comparing((HWInvLoc o) -> o.ID);
        if (tree != null) {
            tree.locs.sort(compareByID);
        }
        return gson.toJson(tree);
    }
    public void fromStringToFile(String str, String outputFileName) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName),
                StandardCharsets.UTF_8))) {
            bw.write(str);
            bw.flush();
        }
    }
    public String fromFile(Path inputFilePath) throws IOException {
        return Files.readString(inputFilePath, StandardCharsets.UTF_8);
    }

    public List<HWInvLoc> subtract(List<HWInvLoc> list0, List<HWInvLoc> list1) {
        return (List<HWInvLoc>) CollectionUtils.subtract(list0, list1);
    }
}
