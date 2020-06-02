package com.intel.dai.eventsim;

/**
 * Description of class ForeignEventBoot.
 * creates sensor events
 */
class ForeignEventSensor extends ForeignEvent {
    ForeignEventSensor() {
        subject = EVENT_SUB_TYPE.telemetry;
    }

    /**
     * This method is used to set sensor event-id
     */
    void setEventID(int id) {
        props_.put("event_id", id);
    }

    /**
     * This method is used to fetch sensor event-id
     */
    int getEventID() {
        return props_.getIntOrDefault("event_id", 0);
    }

    /**
     * This method is used to set sensor name
     */
    void setSensorName(String sensorName) {
        props_.put("sensor", sensorName);
    }

    /**
     * This method is used to fetch sensor name
     */
    String getSensorName() {
        return props_.getStringOrDefault("sensor", null);
    }

    /**
     * This method is used to set sensor value
     */
    void setSensorValue(String sensorValue) {
        props_.put("value", sensorValue);
    }

    /**
     * This method is used to fetch sensor value
     */
    String getSensorValue() {
        return props_.getStringOrDefault("value", null);
    }

    /**
     * This method is used to set sensor units
     */
    void setSensorUnits(String sensorUnits) {
        props_.put("unit", sensorUnits);
    }

    /**
     * This method is used to fetch sensor units
     */
    String getSensorUnits() {
        return props_.getStringOrDefault("unit", null);
    }

    /**
     * This method is used to set sensor id
     */
    void setSensorID(String sensorID) {
        props_.put("id", sensorID);
    }

    /**
     * This method is used to fetch sensor id
     */
    String getSensorID() {
        return props_.getStringOrDefault("id", null);
    }
}