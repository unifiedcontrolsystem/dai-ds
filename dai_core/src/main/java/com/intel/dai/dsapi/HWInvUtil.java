// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface HWInvUtil {
    HWInvTree toCanonicalPOJO(Path canonicalHWInvPath);
    HWInvTree toCanonicalPOJO(String canonicalHWInvJson);
    String toCanonicalJson(HWInvTree tree);
    void fromStringToFile(String str, String outputFileName) throws IOException;
    String fromFile(Path inputFilePath) throws IOException;
    List<HWInvLoc> subtract(List<HWInvLoc> list0, List<HWInvLoc> list1);
}
