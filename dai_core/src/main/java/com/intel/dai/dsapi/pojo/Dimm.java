// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.dsapi.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
public class Dimm extends Component {
    public String serial;
    public String oem_serial;
    public String handle;
    public String locator;

    @SerializedName("IB_DIMM") public String raw_IB_DIMM;
    public IB_DIMM_POJO ib_dimm;
}
