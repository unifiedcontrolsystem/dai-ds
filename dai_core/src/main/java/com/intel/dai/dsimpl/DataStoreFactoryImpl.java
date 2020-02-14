// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl;

import java.sql.Connection;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.jdbc.*;
import com.intel.dai.dsimpl.voltdb.*;
import org.voltdb.client.Client;

import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.logging.Logger;

public class DataStoreFactoryImpl implements DataStoreFactory {
    public DataStoreFactoryImpl(String[] args, Logger logger) {
        assert args != null;
        assert logger != null;
        logger_ = logger;
        if (args.length >= 1) {
            initServers(args[0]);
        } else {
            initServers(null);
        }
    }

    public DataStoreFactoryImpl(String sServers, Logger logger) {
        assert logger != null;
        logger_ = logger;
        initServers(sServers);
    }

    private void initServers(String serversInput) {
        // Define the default VoltDB server at this level (factory)
        if (serversInput == null || serversInput.isEmpty()) {
            servers = DEFAULT_VOLTDB_SERVER;
        } else {
            servers = serversInput;
        }
        parsedServers = servers.split(",");
    }

    @Override
    public VoltDbManager createGroups() {
        VoltDbManager instance = new VoltDbManager(parsedServers, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbManager createConfiguration() {
        VoltDbManager instance = new VoltDbManager(parsedServers, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbWorkQueue createWorkQueue(IAdapter adapter) throws DataStoreException {
        VoltDbWorkQueue instance = new VoltDbWorkQueue(parsedServers, adapter, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbWorkQueue createWorkQueue(Client client, IAdapter adapter) throws DataStoreException {
        VoltDbWorkQueue instance = new VoltDbWorkQueue(client, parsedServers, adapter, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbWorkQueue createWorkQueue(AdapterInformation adapter) {
        VoltDbWorkQueue instance = new VoltDbWorkQueue(parsedServers, adapter, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbRasEventLog createRasEventLog(IAdapter adapter) throws DataStoreException {
        VoltDbRasEventLog instance = new VoltDbRasEventLog(parsedServers, adapter, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbRasEventLog createRasEventLog(AdapterInformation adapter) {
        VoltDbRasEventLog instance = new VoltDbRasEventLog(parsedServers, adapter, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public InventorySnapshot createInventorySnapshotApi() {
        return new InventorySnapshotJdbc();
    }

    @Override
    public VoltDbBootImage createBootImageApi(IAdapter adapter) {
        VoltDbBootImage instance = new VoltDbBootImage(parsedServers, adapter.adapterType(), logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public VoltDbBootImage createBootImageApi(AdapterInformation adapter) {
        VoltDbBootImage instance = new VoltDbBootImage(parsedServers, adapter.getType(), logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public InventoryTrackingApi createInventoryTrackingApi() {
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        return new VoltDbInventoryTrackingApi(parser, logger_);
    }

    @Override
    public DataLoaderApi createDataLoaderApi() throws DataStoreException {
        Connection nearlineConn = createTier2Connection();
        Connection onlineConn = createTier1Connection();

        DataLoaderApi api = new DataLoaderApiJdbc(onlineConn, nearlineConn, logger_);
        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
            try {
                api.disconnectAll();
            } catch(Exception e) {
                logger_.exception(e);
            }
        }));
        return api;
    }

    @Override
    public DbStatusApi createDbStatusApi(Client client) throws DataStoreException {
        return new DbStatusApiImpl(logger_, client);
    }

    @Override
    public NodeInformation createNodeInformation() {
        VoltDbNodeInformation instance = new VoltDbNodeInformation(logger_, parsedServers);
        instance.initialize();
        return instance;
    }

    @Override
    public EventsLog createEventsLog(String adapterName, String adapterType) {
        VoltDbEventsLog instance = new VoltDbEventsLog(parsedServers , adapterName, adapterType, logger_);
        instance.initialize();
        return instance;
    }

    @Override
    public Location createLocation() {
        VoltDbLocation instance = new VoltDbLocation(logger_, parsedServers);
        instance.initialize();
        return instance;
    }

    @Override
    public ServiceInformation createServiceInformation() {
        VoltDbServiceInformation info = new VoltDbServiceInformation(logger_, parsedServers);
        info.initialize();
        return info;
    }

    @Override
    public WLMInformation createWLMInformation() {
        VoltDbWLMInformation info = new VoltDbWLMInformation(logger_, parsedServers);
        info.initialize();
        return info;
    }

    @Override
    public InventoryApi createInventoryApi(AdapterInformation adapter) {
        VoltInventoryApi instance = new VoltInventoryApi(logger_, adapter, parsedServers);
        instance.initialize();
        return instance;
    }
    @Override
    public HWInvApi createHWInvApi() {
        VoltHWInvApi instance = new VoltHWInvApi(logger_, new HWInvUtilImpl(), parsedServers);
        instance.initialize();
        return instance;
    }
    @Override
    public AdapterOperations createAdapterOperations(AdapterInformation information) {
        return new VoltDbAdapterOperations(logger_, parsedServers, information);
    }

    @Override
    public StoreTelemetry createStoreTelemetry() {
        return new JdbcStoreTelemetry(logger_);
    }

    @Override
    public LegacyVoltDbDirectAccess createVoltDbLegacyAccess() {
        return new VoltDbLegacyDirectAccess(parsedServers);
    }

    protected Connection createTier2Connection() throws DataStoreException {
        return DbConnectionFactory.createDefaultConnection();
    }

    protected Connection createTier1Connection() throws DataStoreException {
        return DbConnectionFactory.createAnonymousConnection(ONLINE_TIER_DRIVER,
                generateVoltDBJdbcUrl(servers), true);
    }

    @Override
    public Jobs createJobApi() {
        VoltDbJobs jobs = new VoltDbJobs(logger_, parsedServers);
        jobs.initialize();
        return jobs;
    }

    @Override
    public Reservations createReservationApi() {
        VoltDbReservations reservations = new VoltDbReservations(logger_, parsedServers);
        reservations.initialize();
        return reservations;
    }

    private String[] parsedServers;
    private String servers;

    private static String generateVoltDBJdbcUrl(String servers) {
        assert servers != null;

        String url = "jdbc:voltdb://" + servers;
        return url;
    }

    final static String DEFAULT_VOLTDB_SERVER = "localhost";
    final static String ONLINE_TIER_DRIVER = "org.voltdb.jdbc.Driver";
    final Logger logger_;
}
