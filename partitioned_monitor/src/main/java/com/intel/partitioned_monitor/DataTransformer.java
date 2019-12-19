// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import java.util.List;

/**
 * Interface to implement a raw data to CommonDataFormat transformation.
 */
@FunctionalInterface
public interface DataTransformer {
    List<CommonDataFormat> processRawStringData(String data, PartitionedMonitorConfig config)
            throws DataTransformerException;
}
