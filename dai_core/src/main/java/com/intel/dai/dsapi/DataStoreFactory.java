// Copyright (C) 2018-2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsimpl.voltdb.VoltDbWorkQueue;
import com.intel.logging.Logger;
import org.voltdb.client.Client;

import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.DataStoreException;

public interface DataStoreFactory {
    Groups createGroups();
    Configuration createConfiguration();
    WorkQueue createWorkQueue(IAdapter adapter) throws DataStoreException;
    WorkQueue createWorkQueue(Client client, IAdapter adapter) throws DataStoreException;
    InventorySnapshot createInventorySnapshotApi();
    BootImage createBootImageApi(IAdapter adapter);
    InventoryTrackingApi createInventoryTrackingApi();
    DataLoaderApi createDataLoaderApi() throws DataStoreException;
    DbStatusApi createDbStatusApi(Client client) throws DataStoreException;
    EventsLog createEventsLog(String adapterName, String adapterType);
    Location createLocation();
    ServiceInformation createServiceInformation();
    WLMInformation createWLMInformation();
    StoreTelemetry createStoreTelemetry();

    // New APIs
    AdapterOperations createAdapterOperations(AdapterInformation information);
    WorkQueue createWorkQueue(AdapterInformation adapter);
    RasEventLog createRasEventLog(IAdapter adapter) throws DataStoreException;
    RasEventLog createRasEventLog(AdapterInformation adapter);
    BootImage createBootImageApi(AdapterInformation adapter);
    StoreTelemetry createStoreTelemetry(Logger logger);
    NodeInformation createNodeInformation();
    InventoryApi createInventoryApi(AdapterInformation adapter);
    HWInvDbApi createHWInvApi();
    LegacyVoltDbDirectAccess createVoltDbLegacyAccess();
    Jobs createJobApi();
    Reservations createReservationApi();
}
