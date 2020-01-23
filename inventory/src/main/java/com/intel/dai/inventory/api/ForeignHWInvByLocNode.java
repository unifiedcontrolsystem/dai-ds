// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0s0b0n0
 */
@ToString
class ForeignHWInvByLocNode extends  ForeignHWInvByLoc {
    List<ForeignHWInvByLocProcessor> Processors;
    List<ForeignHWInvByLocMemory> Memory;
}
