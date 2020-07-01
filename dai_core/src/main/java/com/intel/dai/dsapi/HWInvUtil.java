// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * This interface contains methods that convert canonical inventory information amongst different formats.  The
 * possible formats are POJO, json string and json file.
 */
public interface HWInvUtil {
    HWInvTree toCanonicalPOJO(Path canonicalHWInvPath);
    HWInvTree toCanonicalPOJO(String canonicalHWInvJson);
    HWInvHistory toCanonicalHistoryPOJO(String canonicalHWInvHistoryJson);
    String toCanonicalJson(HWInvTree tree);
    String toCanonicalHistoryJson(HWInvHistory history);
    void toFile(String str, String outputFileName) throws IOException;
    String fromFile(Path inputFilePath) throws IOException;
    List<HWInvLoc> subtract(List<HWInvLoc> list0, List<HWInvLoc> list1);
}
