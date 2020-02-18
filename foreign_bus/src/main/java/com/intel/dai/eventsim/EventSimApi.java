package com.intel.dai.eventsim;

import com.intel.properties.PropertyArray;

import java.util.Map;

public interface EventSimApi {
    // To generate ras events
    String generatRasEvents(Map<String, String> parameters) throws Exception;

    //To generate environmental events
    String generateEnvEvents(Map<String, String> param) throws Exception;

    //To generate boot events
    String generateBootEvents(Map<String, String> param) throws Exception;

    //API to get boot parameters
    String getBootParameters(Map<String, String> param) throws Exception;

    //API to get HW Inventory data
    String getInventoryHardware(Map<String, String> param) throws Exception;

    //API to get Discovery Status for all locations
    String getAllInventoryDiscoverStatus(Map<String, String> parameters);

    //API to get initiate inventory discovery
    String initiateInventoryDiscover(Map<String, String> param) throws Exception;

    //API to generate a create reservation log message
    String createReservation(Map<String, String> parameters) throws Exception;

    //API to generate a modify reservation log message
    String modifyReservation(Map<String, String> parameters) throws Exception;

    //API to generate a delete reservation log message
    String deleteReservation(Map<String, String> parameters) throws Exception;

    //API to generate a start job log message
    String startJob(Map<String, String> parameters) throws Exception;

    //API to generate a terminate job log message
    String terminateJob(Map<String, String> parameters) throws Exception;

    //API to simulate wlm behavior
    String simulateWlm(Map<String, String> parameters) throws Exception;
}
