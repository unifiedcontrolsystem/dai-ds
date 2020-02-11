package com.intel.dai.eventsim;

/**
 * Description of class ForeignEventBoot.
 * creates boot events
 */
public class ForeignEventBoot extends ForeignEvent{

    /**
     * This method is used to set creation location of boot event
     */
    public void setLocation(String location) { props_.put("Components",location); }

    /**
     * This method is used to set role of event.
     */
    void setRole(String role) {
        props_.put("Role", role);
    }

    /**
     * This method is used to set state of event.
     */
    void setState(String nodeState) {
        props_.put("State", nodeState);
    }

    /**
     * This method is used to set software staus of event.
     */
    public void setStatus(String status) {
        props_.put("SoftwareStatus", status);
    }

    public void setBootImageId(String bootImageId) { props_.put("bootImageId", bootImageId);  }
}