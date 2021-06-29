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

    @SerializedName("IB_BIOS") public String rawIbBios;
    @SerializedName("OOB_REV_INFO") public String rawOobRevInfo;
    @SerializedName("IB_BOARD") public String rawIbBoard;
    @SerializedName("OOB_FRU") public String rawOobFru;
    @SerializedName("IB_SYS") public String rawIbSys;
    @SerializedName("IB_CHASS") public String rawIbChass;

    public IbBiosPojo ib_bios;
    public OobRevInfoPojo oob_rev_info;
    public IbBoardPojo ib_board;
    public OobFruPojo oob_fru;
    public IbSysPojo ib_sys;
    public IbChassPojo ib_chass;

    public FruHost(FruHost fruHost) {
        super(fruHost);
        boardSerial = fruHost.boardSerial;

        hostname = fruHost.hostname;

        rawIbBios = fruHost.rawIbBios;
        ib_bios = fruHost.ib_bios;

        rawOobRevInfo = fruHost.rawOobRevInfo;
        oob_rev_info = fruHost.oob_rev_info;

        rawIbBoard = fruHost.rawIbBoard;
        ib_board = fruHost.ib_board;

        rawOobFru = fruHost.rawOobFru;
        oob_fru = fruHost.oob_fru;

        rawIbSys = fruHost.rawIbSys;
        ib_sys = fruHost.ib_sys;

        rawIbChass = fruHost.rawIbChass;
        ib_chass = fruHost.ib_chass;
    }
}

@ToString
@EqualsAndHashCode
class IbBoardPojo {
    @SerializedName("Features") public String features;
    @SerializedName("Asset Tag") public String assetTag;
    @SerializedName("Chassis Handle") public String chassisHandle;
    @SerializedName("Serial Number") public String serialNumber;
    @SerializedName("Version") public String version;
    @SerializedName("Contained Object Handles") public String containedObjectHandles;
    @SerializedName("Product Name") public String productName;
    @SerializedName("Type") public String type;
    @SerializedName("Location In Chassis") public String locationInChassis;
    @SerializedName("Manufacturer") public String manufacturer;
}

@ToString
@EqualsAndHashCode
class IbSysPojo {
    @SerializedName("SKU Number") public String skuNumber;
    @SerializedName("UUID") public String uuid;
    @SerializedName("Family") public String family;
    @SerializedName("Serial Number") public String serialNumber;
    @SerializedName("Version") public String Version;
    @SerializedName("Product Name") public String productName;
    @SerializedName("Wake-up Type") public String wakeUpType;
    @SerializedName("Manufacturer") public String manufacturer;
}

@ToString
@EqualsAndHashCode
class IbChassPojo {
    @SerializedName("OEM Information") public String oemInformation;
    @SerializedName("Number Of Power Cords") public String numberOfPowerCords;
    @SerializedName("Contained Elements") public String containedElements;
    @SerializedName("Power Supply State") public String powerSupplyState;
    @SerializedName("Thermal State") public String thermalState;
    @SerializedName("Lock") public String lock;
    @SerializedName("Height") public String height;
    @SerializedName("Serial Number") public String serialNumber;
    @SerializedName("Version") public String version;
    @SerializedName("Asset Tag") public String assetTag;
    @SerializedName("SKU Number") public String skuNumber;
    @SerializedName("Boot-up State") public String bootUpState;
    @SerializedName("Security Status") public String securityStatus;
    @SerializedName("Type") public String type;
    @SerializedName("Manufacturer") public String manufacturer;
}
