// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.dsapi.pojo;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.google.gson.annotations.SerializedName;

@ToString
@EqualsAndHashCode(callSuper = true)
public class FruHost extends Component {
    public String boardSerial;
    public String hostname;

    @SerializedName("IB_BIOS") public String raw_IB_BIOS;
    @SerializedName("OOB_REV_INFO") public String raw_OOB_REV_INFO;
    @SerializedName("IB_BOARD") public String raw_IB_BOARD;
    @SerializedName("OOB_FRU") public String raw_OOB_FRU;
    @SerializedName("IB_SYS") public String raw_IB_SYS;
    @SerializedName("IB_CHASS") public String raw_IB_CHASS;

    public IB_BIOS_POJO ib_bios;
    public OOB_REV_INFO_POJO oob_rev_info;
    public IB_BOARD_POJO ib_board;
    public OOB_FRU_POJO oob_fru;
    public IB_SYS_POJO ib_sys;
    public IB_CHASS_POJO ib_chass;

    public FruHost(FruHost fruHost) {
        super(fruHost);
        boardSerial = fruHost.boardSerial;

        hostname = fruHost.hostname;

        raw_IB_BIOS = fruHost.raw_IB_BIOS;
        ib_bios = fruHost.ib_bios;

        raw_OOB_REV_INFO = fruHost.raw_OOB_REV_INFO;
        oob_rev_info = fruHost.oob_rev_info;

        raw_IB_BOARD = fruHost.raw_IB_BOARD;
        ib_board = fruHost.ib_board;

        raw_OOB_FRU = fruHost.raw_OOB_FRU;
        oob_fru = fruHost.oob_fru;

        raw_IB_SYS = fruHost.raw_IB_SYS;
        ib_sys = fruHost.ib_sys;

        raw_IB_CHASS = fruHost.raw_IB_CHASS;
        ib_chass = fruHost.ib_chass;
    }
}

@ToString
@EqualsAndHashCode
class IB_BOARD_POJO {
    @SerializedName("Features") public String Features;
    @SerializedName("Asset Tag") public String Asset_Tag;
    @SerializedName("Chassis Handle") public String Chassis_Handle;
    @SerializedName("Serial Number") public String Serial_Number;
    @SerializedName("Version") public String Version;
    @SerializedName("Contained Object Handles") public String Contained_Object_Handles;
    @SerializedName("Product Name") public String Product_Name;
    @SerializedName("Type") public String Type;
    @SerializedName("Location In Chassis") public String Location_In_Chassis;
    @SerializedName("Manufacturer") public String Manufacturer;
}

@ToString
@EqualsAndHashCode
class IB_SYS_POJO {
    @SerializedName("SKU Number") public String SKU_Number;
    @SerializedName("UUID") public String UUID;
    @SerializedName("Family") public String Family;
    @SerializedName("Serial Number") public String Serial_Number;
    @SerializedName("Version") public String Version;
    @SerializedName("Product Name") public String Product_Name;
    @SerializedName("Wake-up Type") public String Wake_up_Type;
    @SerializedName("Manufacturer") public String Manufacturer;
}

@ToString
@EqualsAndHashCode
class IB_CHASS_POJO {
    @SerializedName("OEM Information") public String OEM_Information;
    @SerializedName("Number Of Power Cords") public String Number_Of_Power_Cords;
    @SerializedName("Contained Elements") public String Contained_Elements;
    @SerializedName("Power Supply State") public String Power_Supply_State;
    @SerializedName("Thermal State") public String Thermal_State;
    @SerializedName("Lock") public String Lock;
    @SerializedName("Height") public String Height;
    @SerializedName("Serial Number") public String Serial_Number;
    @SerializedName("Version") public String Version;
    @SerializedName("Asset Tag") public String Asset_Tag;
    @SerializedName("SKU Number") public String SKU_Number;
    @SerializedName("Boot-up State") public String Boot_up_State;
    @SerializedName("Security Status") public String Security_Status;
    @SerializedName("Type") public String Type;
    @SerializedName("Manufacturer") public String Manufacturer;
}
