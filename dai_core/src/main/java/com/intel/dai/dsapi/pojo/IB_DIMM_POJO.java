package com.intel.dai.dsapi.pojo;


import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class IB_DIMM_POJO {
    @SerializedName("Maximum Voltage") public String Maximum_Voltage;
    @SerializedName("Memory Technology") public String Memory_Technology;
    @SerializedName("Volatile Size") public String Volatile_Size;
    @SerializedName("Logical Size") public String Logical_Size;
    @SerializedName("Error Information Handle") public String Error_Information_Handle;
    @SerializedName("Non-Volatile Size") public String Non_Volatile_Size;
    @SerializedName("Type") public String Type;
    @SerializedName("Array Handle") public String Array_Handle;
    @SerializedName("Module Manufacturer ID") public String Module_Manufacturer_ID;
    @SerializedName("Serial Number") public String Serial_Number;
    @SerializedName("Total Width") public String Total_Width;
    @SerializedName("Form Factor") public String Form_Factor;
    @SerializedName("Manufacturer") public String Manufacturer;
    @SerializedName("Data Width") public String Data_Width;
    @SerializedName("Asset Tag") public String Asset_Tag;
    @SerializedName("Bank Locator") public String Bank_Locator;
    @SerializedName("Module Product ID") public String Module_Product_ID;
    @SerializedName("Firmware Version") public String Firmware_Version;
    @SerializedName("Minimum Voltage") public String Minimum_Voltage;
    @SerializedName("Memory Operating Mode Capability") public String Memory_Operating_Mode_Capability;
    @SerializedName("Cache Size") public String Cache_Size;
    @SerializedName("Configured Voltage") public String Configured_Voltage;
    @SerializedName("Part Number") public String Part_Number;
    @SerializedName("Set") public String Set;
    @SerializedName("Configured Memory Speed") public String Configured_Memory_Speed;
    @SerializedName("Rank") public String Rank;
    @SerializedName("Locator") public String Locator;
    @SerializedName("Type Detail") public String Type_Detail;
    @SerializedName("Memory Subsystem Controller Manufacturer ID") public String Memory_Subsystem_Controller_Manufacturer_ID;
    @SerializedName("Speed") public String Speed;
    @SerializedName("Memory Subsystem Controller Product ID") public String Memory_Subsystem_Controller_Product_ID;
    @SerializedName("Size") public String Size;
}
