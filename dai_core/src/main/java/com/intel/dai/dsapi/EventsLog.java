package com.intel.dai.dsapi;

import com.intel.properties.PropertyArray;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.Map;

public interface EventsLog {

    //To log rasevent into system
    void createRasEvent(Map<String, String> param);

    //To get all the rasevent types
    PropertyArray listAllRasEventTypes();

    //To get all the rasevent types
    PropertyArray listAllRasEventTypes(Map<String, String> param);

    //To check RasEventDescriptive Name
    boolean checkDescriptiveName(String eventtype);
}
