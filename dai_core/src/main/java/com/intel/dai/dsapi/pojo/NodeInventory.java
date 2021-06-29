// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.dsapi.pojo;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
public class NodeInventory extends FruHost {
    public Dimm CPU0_DIMM_A1;
    public Dimm CPU0_DIMM_B1;
    public Dimm CPU0_DIMM_C1;
    public Dimm CPU0_DIMM_D1;
    public Dimm CPU0_DIMM_E1;
    public Dimm CPU0_DIMM_F1;
    public Dimm CPU0_DIMM_G1;
    public Dimm CPU0_DIMM_H1;

    public Dimm CPU1_DIMM_A1;
    public Dimm CPU1_DIMM_B1;
    public Dimm CPU1_DIMM_C1;
    public Dimm CPU1_DIMM_D1;
    public Dimm CPU1_DIMM_E1;
    public Dimm CPU1_DIMM_F1;
    public Dimm CPU1_DIMM_G1;
    public Dimm CPU1_DIMM_H1;

    public NodeInventory(FruHost fruHost) {
        super(fruHost);
    }
}
