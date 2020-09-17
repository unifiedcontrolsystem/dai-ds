// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.loc;

import lombok.ToString;

import java.util.List;

/**
 * Example: x0m0p0
 */
@ToString
public class ForeignHWInvByLocCabinetPDU extends ForeignHWInvByLoc {
    public List<ForeignHWInvByLocCabinetPDUOutlet> CabinetPDUOutlets;
}
