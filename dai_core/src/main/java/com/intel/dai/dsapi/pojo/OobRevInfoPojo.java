package com.intel.dai.dsapi.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class OobRevInfoPojo {
    @SerializedName("Product ID") public String Product_ID;
    @SerializedName("Device GUID") public String Device_GUID;
    @SerializedName("System GUID") public String System_GUID;
    @SerializedName("Manufacturer ID") public String Manufacturer_ID;
    @SerializedName("Firmware Revision") public String Firmware_Revision;
    @SerializedName("Auxiliary Firmware Revision Information") public String Auxiliary_Firmware_Revision_Information;
}
