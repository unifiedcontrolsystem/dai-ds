// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import java.io.Closeable;
import java.io.IOException;
import org.voltdb.client.ProcCallException;
import java.util.HashMap;

/**
 * Description of interface ServiceInformation.
 */
public interface ServiceInformation {
    /**
     * Get service operation information for a given location
     *
     * @param lctn location of node to get service operation information
     * @return HashMap with service operation information for the location
     */
    HashMap<String, Object> getServiceOperationInfo(String lctn) throws IOException, ProcCallException;
}