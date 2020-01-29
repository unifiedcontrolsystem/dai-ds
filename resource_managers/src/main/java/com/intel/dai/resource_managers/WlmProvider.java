package com.intel.dai.resource_managers;

import com.intel.dai.exceptions.ProviderException;
import com.intel.dai.result.Result;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public interface WlmProvider {
    static final String CONSUMER_TAG = "AdapterWlm";
    static final long SHUTDOWN_CHECK_INTERVAL_MS = 1000L;


    //---------------------------------------------------------
    // Handle input coming in from the "attached" external component (e.g., PBS Pro, Slurm, Cobalt, LSF),
    // looking for things that we are interested in / need to take action on.
    // Note: This work item is different than most in that this one work item will run for the length of time
    // that the Wlm component is active.
    // It does not start and stop, it starts and stays active processing the Wlm component's log for state changes, etc.
    //---------------------------------------------------------
    long handleInputFromExternalComponent(Map<String, String> aWiParms) throws InterruptedException, IOException, ProviderException;

}
