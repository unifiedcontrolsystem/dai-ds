// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.transforms;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.IAdapter;
import com.intel.logging.Logger;

import java.util.NoSuchElementException;

public class DefaultLocations implements Locations {

    public static String CpuPrefix  = "-P";         // R2-CH03-N2-P1 (P = processor socket)
    public static String DimmPrefix = "-D";         // R2-CH03-N2-D8 (D = memory dimm)
    public static String AcceleratorPrefix  = "-A"; // R2-CH03-N2-A5 (i.e., gpu, fpga, etc.)
    public static String IvocPrefix = "-IVOC";      // R2-CH03-N2-IVOC8
    public static String HfiPrefix  = "-H";         // R2-CH03-N2-H3 (H = high-speed fabric nic)

    /**
     * Create a instance of Locations where the foreign* methods are unimplemented.
     *
     * @param log The logger to use for logging errors.
     * @throws IllegalArgumentException when the adapter parameter is null.
     */
    public DefaultLocations(Logger log) {
        log_ = log;
        try {
            initialize();
        } catch(Exception e) {
            // Should never happen because getAdapter will return an already created instance.
        }
    }
    /**
     * From a DAI known hostname, get the location string.
     *
     * @param hostname The hostname to lookup. Cannot be empty or null.
     * @return The location string.
     * @throws RuntimeException thrown if the hostname was empty or null, or the lookup failed.
     */
    @Override
    public String hostnameToLocation(String hostname) throws RuntimeException {
        try {
            String result = adapter_.mapCompNodeHostNameToLctn().get(hostname);
            if (result == null) throw new NoSuchElementException("hostname");
            return result;
        } catch(Exception e) {
            exception(e);
            throw new RuntimeException("Failed to lookup the hostname string: " + hostname);
        }
    }

    /**
     * From a DAI location string, get the DAI known hostname.
     *
     * @param location The DAI location string. Cannot be empty or null.
     * @return The DAI known hostname.
     * @throws RuntimeException thrown if the location was empty or null, or the lookup failed.
     */
    @Override
    public String locationToHostName(String location) throws RuntimeException {
        try {
            String result = adapter_.mapServNodeLctnToHostName().get(location);
            if (result == null) throw new NoSuchElementException("location");
            return result;
        } catch(Exception e1) {
            try {
                String result = adapter_.mapCompNodeLctnToHostName().get(location);
                if (result == null) throw new NoSuchElementException("location");
                return result;
            } catch(Exception e2) {
                exception(e2);
                throw new RuntimeException("Failed to lookup the location string: " + location);
            }
        }
    }

    /**
     * Return a DAI location string from a foreign location string. This is not implemented and will throw a
     * RuntimeException.
     *
     * @param foreignLocation The foreign location string. Cannot be empty or null.
     * @param otherArgs For some derived classes this is used, unused here.
     * @return The DAI known location string.
     * @throws RuntimeException thrown if the foreignLocation was empty or null, or the lookup failed.
     */
    @Override
    public String foreignLocationToLocation(String foreignLocation, String... otherArgs) throws RuntimeException {
        throw new RuntimeException("foreignLocationToLocation is not implemented!");
    }

    /**
     * Return a foreign location string from a DAI location string. This is not implemented and will throw a
     * RuntimeException.
     *
     * @param location The location string. Cannot be empty or null.
     * @return The foreign location string.
     * @throws RuntimeException thrown if the location was empty or null, or the lookup failed.
     */
    @Override
    public String locationToForeignLocation(String location) throws RuntimeException {
        throw new RuntimeException("locationToForeignLocation is not implemented!");
    }

    /**
     * Return the fru's lctn from the specified location string.
     *     E.g., if lctn was specified as "R2-CH03-N2-CPU1-DIMM8" and numLevelsInFruLctn is 3, this method will return "R2-CH03-N2"
     * @param lctn The fully qualified lctn string from which we want to extract the lctn string for the fru.
     * @param numLevelsInFruLctn Specifies the number of "level" make up the fru's lctn
     * @throws IllegalArgumentException when the adapter parameter is null.
     */
    public static String extractFruLocation(String lctn, long numLevelsInFruLctn) {
        String[] aSctns = lctn.split("-");
        if (aSctns.length > numLevelsInFruLctn) {
            StringBuilder sbCnLctn = new StringBuilder();
            for (int iCntr=0; iCntr < numLevelsInFruLctn; ++iCntr) {
                if (iCntr > 0)
                    sbCnLctn.append("-");
                sbCnLctn.append(aSctns[iCntr]);
            }
            return sbCnLctn.toString();
        }
        return lctn;
    }   // End extractFruLocation(String lctn, int numDashesForFru)

    /**
     * Add the logger after creation.
     *
     * @param logger The {@link Logger} instance to use in this class.
     */
    @Override
    public void setLogger(Logger logger) {
        log_ = logger;
    }

    protected void initialize() throws Exception {
        adapter_ = AdapterSingletonFactory.getAdapter();
    }

    protected void exception(Exception e) {
        if(log_ != null) {
            for(StackTraceElement element: e.getStackTrace())
                log_.error("%s: %s", getClass().getCanonicalName(), element.toString());
        }
    }

    protected IAdapter adapter_;
    private Logger log_;
}
