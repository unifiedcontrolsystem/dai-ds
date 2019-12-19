package com.intel.dai.dsapi;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.file.Path;

public interface HWInvUtil {
    HWInvTree toCanonicalPOJO(String inputFileName) throws IOException, JsonIOException, JsonSyntaxException;
    String toCanonicalJson(HWInvTree tree);
    void fromStringToFile(String str, String outputFileName) throws IOException;
    String fromFile(Path inputFilePath) throws IOException;
}
