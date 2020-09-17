// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0
 */
@ToString
public class ForeignHWInvByLocChassis extends ForeignHWInvByLoc {
    public List<ForeignHWInvByLocComputeModule> ComputeModules;
}
