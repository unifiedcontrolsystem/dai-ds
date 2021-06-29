package com.intel.dai.dsapi.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class IbBiosPojo {
    @SerializedName("Vendor") public String Vendor;
    @SerializedName("Characteristics") public String Characteristics;
    @SerializedName("Runtime Size") public String Runtime_Size;
    @SerializedName("Version") public String Version;
    @SerializedName("ROM Size") public String ROM_Size;
    @SerializedName("Address") public String Address;
    @SerializedName("Release Date") public String Release_Date;
}
