package com.intel.dai.eventsim;

/**
 * Description of class ForeignEventRAS.
 * creates ras events
 */
public class ForeignEventRAS extends ForeignEvent {

    public ForeignEventRAS() {
        // TODO: Store some paramters as hardcoded values for now
        props_.put("pri", "0x0");
        props_.put("seqnum", "0x7220");
    }

    public String getMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append("src::::");
        msg.append(props_.getStringOrDefault("src","unknown"));
        msg.append("|pri:");
        msg.append(props_.getStringOrDefault("pri", "unknown"));
        msg.append("|seqnum:");
        msg.append(props_.getStringOrDefault("seqnum", "unknown"));
        msg.append("::");
        msg.append(getLocation());
        msg.append("|");
        msg.append(getEventPayload());

        return msg.toString();
    }

    public String getEventPayload() {
        return props_.getStringOrDefault("payload", "UNKNOWN");
    }

    public void setEventType(String eventType) {
        props_.put("event-type", eventType);
        props_.put("payload", "Cause:"+eventType);
    }

    @Override
    public String getJSON() {
        props_.put("message", getMessage());
        return super.getJSON();
    }

    public String getBootImageId() {
        return props_.getStringOrDefault("bootImageId", "");
    }


    public void setBootImageId(String bootImageId) {
        props_.put("bootImageId", bootImageId);
    }
}