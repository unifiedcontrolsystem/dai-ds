// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

/**
 * Description of class StoreTelemetry.
 */
public interface StoreTelemetry {
    long logEnvDataAggregated(String sTypeOfData, String sLctn, long lTsInMicroSecs, double dMaxValue,
                              double dMinValue, double dAvgValue, String sReqAdapterType, long lReqWorkItemId)
            throws DataStoreException;
}
