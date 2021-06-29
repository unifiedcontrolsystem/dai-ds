package com.intel.dai.dsapi.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class OobFruPojo {
    @SerializedName("Chassis Type") public String Chassis_Type;
    @SerializedName("Chassis Part Number") public String Chassis_Part_Number;
    @SerializedName("Chassis Serial") public String Chassis_Serial;
    @SerializedName("Chassis Extra") public String Chassis_Extra;
    @SerializedName("Board Mfg Date") public String Board_Mfg_Date;
    @SerializedName("Board Mfg") public String Board_Mfg;
    @SerializedName("Board Product") public String Board_Product;
    @SerializedName("Board Serial") public String Board_Serial;
    @SerializedName("Board Part Number") public String Board_Part_Number;
    @SerializedName("Product Manufacturer") public String Product_Manufacturer;
    @SerializedName("Product Part Number") public String Product_Part_Number;
    @SerializedName("Product Version") public String Product_Version;
    @SerializedName("Product Serial") public String Product_Serial;
    @SerializedName("Product Asset Tag") public String Product_Asset_Tag;
    @SerializedName("Board Extra") public String Board_Extra;
}
