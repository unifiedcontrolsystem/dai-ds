package com.intel.dai.dsimpl.voltdb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
        return new String(Files.readAllBytes(inputFilePath));
    }
}
