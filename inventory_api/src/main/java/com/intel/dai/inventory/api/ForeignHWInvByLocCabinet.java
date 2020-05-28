// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0
 */
@ToString
class ForeignHWInvByLocCabinet extends ForeignHWInvByLoc {
    List<ForeignHWInvByLocChassis> Chassis;
}
