// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.pojo.scraped;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Fru {
    public String Host;
    public String Serial;
    public String Board_Serial;
    public String Chassis_Extra;
    public String Product_Part_Number;
    public String Product_Serial;
    public String Chassis_Serial;
    public String Board_Part_Number;
    public String Product_Manufacturer;
    public String Chassis_Type;
    public String Product_Asset_Tag;
    public String Chassis_Part_Number;
    public String Board_Product;
    public String Board_Extra;
    public String Product_Version;
    public String Board_Mfg;
    public String Board_Mfg_Date;
    public String BMC_Firmware_Revision;
    public String BMC_Auxiliary_Firmware_Revision_Information;
}
