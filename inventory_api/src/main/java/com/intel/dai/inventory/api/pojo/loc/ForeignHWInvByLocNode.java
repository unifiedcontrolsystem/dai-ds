// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0s0b0n0
 */
@ToString
public class ForeignHWInvByLocNode extends ForeignHWInvByLoc {
    public List<ForeignHWInvByLocProcessor> Processors;
    public List<ForeignHWInvByLocMemory> Memory;
    public List<ForeignHWInvByLocDrive> Drives;
}
