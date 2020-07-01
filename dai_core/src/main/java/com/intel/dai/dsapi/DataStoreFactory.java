// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import com.intel.dai.AdapterInformation;
import com.intel.logging.Logger;
import org.voltdb.client.Client;

import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.DataStoreException;

public interface DataStoreFactory {
    Groups createGroups();
    Configuration createConfiguration();
    WorkQueue createWorkQueue(IAdapter adapter) throws DataStoreException;
    WorkQueue createWorkQueue(Client client, IAdapter adapter) throws DataStoreException;
    WorkQueue createWorkQueue(AdapterInformation adapter);
    RasEventLog createRasEventLog(IAdapter adapter) throws DataStoreException;
    RasEventLog createRasEventLog(AdapterInformation adapter);
    InventorySnapshot createInventorySnapshotApi();
    BootImage createBootImageApi(IAdapter adapter);
    BootImage createBootImageApi(AdapterInformation adapter);
    InventoryTrackingApi createInventoryTrackingApi();
    DataLoaderApi createDataLoaderApi() throws DataStoreException;
    DbStatusApi createDbStatusApi(Client client) throws DataStoreException;
    AdapterOperations createAdapterOperations(AdapterInformation information);

    StoreTelemetry createStoreTelemetry();
    StoreTelemetry createStoreTelemetry(Logger logger);
    NodeInformation createNodeInformation();
    EventsLog createEventsLog(String adapterName, String adapterType);
    Location createLocation();
    ServiceInformation createServiceInformation();
    WLMInformation createWLMInformation();

    InventoryApi createInventoryApi(AdapterInformation adapter);
    HWInvDbApi createHWInvApi();

    LegacyVoltDbDirectAccess createVoltDbLegacyAccess();

    Jobs createJobApi();
    Reservations createReservationApi();
}
