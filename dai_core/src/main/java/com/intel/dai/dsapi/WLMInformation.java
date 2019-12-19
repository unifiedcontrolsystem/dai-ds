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
 * Description of interface WLMInformation.
 */
public interface WLMInformation extends AutoCloseable, Closeable {
    /**
     * Get state of a compute node
     *
     * @param lctn location of node to get reservation information
     * @return HashMap with active wlm reservations information for location
     */
    HashMap<String, String>  getUsersForActiveReservation(String lctn) throws IOException, ProcCallException;
}