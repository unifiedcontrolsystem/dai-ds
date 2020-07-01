// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0c0s0  (?)
 */
@ToString
public class ForeignHWInvByLocComputeModule extends ForeignHWInvByLoc {
    public List<ForeignHWInvByLocNodeEnclosure> NodeEnclosures;
}
