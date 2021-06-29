package com.intel.dai.dsapi.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class IbDimm {
    @SerializedName("Configured Voltage") public String Configured_Voltage;
    public String Manufacturer;
    public String Set;
    public String Rank;
    @SerializedName("Data Width") public String Data_Width;
    @SerializedName("Maximum Voltage") public String Maximum_Voltage;
    public String Type;
    @SerializedName("Configured Memory Speed") public String Configured_Memory_Speed;
    @SerializedName("Bank Locator") public String Bank_Locator;
    public String Speed;
    @SerializedName("Error Information Handle") public String Error_Information_Handle;
    public String Locator;
    @SerializedName("Serial Number") public String Serial_Number;
    @SerializedName("Total Width") public String Total_Width;
    @SerializedName("Asset Tag") public String Asset_Tag;
    @SerializedName("Type Detail") public String Type_Detail;
    @SerializedName("Minimum Voltage") public String Minimum_Voltage;
    @SerializedName("Array Handle") public String Array_Handle;
    @SerializedName("Form Factor") public String Form_Factor;
    public String Size;
}
