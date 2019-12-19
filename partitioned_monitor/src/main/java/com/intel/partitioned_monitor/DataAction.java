// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

/**
 * Description of interface DataAction.
 */
@FunctionalInterface
public interface DataAction {
    void actOnData(CommonDataFormat data, PartitionedMonitorConfig config, SystemActions systemActions);
}
