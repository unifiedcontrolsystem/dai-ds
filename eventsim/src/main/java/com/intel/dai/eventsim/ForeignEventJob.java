package com.intel.dai.eventsim;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ForeignEventJob extends ForeignEvent {

    ForeignEventJob() {
        subject = EVENT_SUB_TYPE.jobs;
    }

    /**
     * This method is used to set job message-id
     */
    void setMessageID(String id) {
        props_.put("MessageId", id);
    }

    /**
     * This method is used to set job value
     */
    void setJobValue(String jobValue) {
        jobSensorProps_.put("Index", jobValue);
        jobSensorProps_.put("ParentalIndex", jobValue);
        jobSensorProps_.put("SubIndex", jobValue);
        jobSensorProps_.put("Value", jobValue);
    }

    /**
     * This method is used to set job context value
     */
    void setJobContext(String context) {
        jobSensorProps_.put("DeviceSpecificContext", "local");
        jobSensorProps_.put("PhysicalContext", context);
    }


    /**
     * This method is used to set job context value
     */
    void setOEM() {
        PropertyArray data = new PropertyArray();
        data.add(jobSensorProps_);
        OEMSensorProps_.put("Sensors", data);
        OEMSensorProps_.put("TelemetrySource", "JobEvent");
        props_.put("Oem", OEMSensorProps_);
    }

    /**
     * This method is used to set creation time of events
     */
    public void setTimestamp() {
        jobSensorProps_.put("Timestamp", Instant.now().toString());
    }

    /**
     * This method is used to set location where job event is created.
     */
    public void setLocation(String location) {
        jobSensorProps_.put("Location", location);
    }

    private PropertyMap jobSensorProps_ = new PropertyMap();
    private PropertyMap OEMSensorProps_ = new PropertyMap();
}
