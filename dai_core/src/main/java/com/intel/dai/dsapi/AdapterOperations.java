// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.AdapterException;
import org.voltdb.client.ProcCallException;

import java.io.IOException;

/**
 * Description of interface AdapterOperations.
 */
public interface AdapterOperations {
    long getAdapterInstancesAdapterId() throws ProcCallException, IOException;
    void registerAdapter() throws IOException, ProcCallException;
    int shutdownAdapter() throws AdapterException;
    int shutdownAdapter(Throwable cause) throws AdapterException;
    void markNodeState(BootState newState, String location, long timestamp, boolean informWlm);
    void markNodeInErrorState(String location, boolean informWlm);
}
