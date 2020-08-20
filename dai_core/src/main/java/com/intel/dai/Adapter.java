// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.xdg.XdgConfigFile;
import org.voltdb.VoltTable;
import org.voltdb.client.*;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Adapter base class (used as super class for component adapters)
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them.
 *  E.g., dbServer1, dbServer2, ...
 *
 *
 *
 */

public class Adapter implements IAdapter {
    public final static String XDG_COMPONENT = "ucs"; // For the XDG configuration file locator library.
    final static String RASMETADATA_FILE = "RasEventMetaData.json"; // File for RAS metadata.
    private ConfigIO mParser;
    public final static String DataMoverQueueName    = "DAI-DataMover-Queue";
    public final static String DataMoverExchangeName = "DAI-DataMover-Exchange";
    public final static String KeyForLctn = "Lctn=";
    public final static String KeyForTimeInMicroSecs = "TimeInMicroSecs=";
    public final static String KeyForUsingSynthData = "UsingSynthData";

    //--------------------------------------------------------------------------
    // Data Members
    //--------------------------------------------------------------------------
    private boolean                 mAdapterAbnormalShutdown              = false;
    private boolean                 mAdapterShutdownStarted               = false;
    private long                    mAdapterId                            = -99999L;
    private String                  mAdapterType                          = null;
    private String                  mAdapterName                          = null;
    private Client                  mClient                               = null;
    private Connection              mNearlineConn                         = null;
    private PreparedStatement       mAggEnvDataStmt                       = null;
    private Map<String, Long>       mCompNodeLctnToSeqNumMap              = null;       // Map that takes a Compute Node's Lctn and gives you the corresponding SequenceNumber.
    private Logger                  mLogger;
    private Map<String, String>     mRasDescNameToEventTypeMap            = null;       // Map that takes a RAS DescriptiveName and gives you the corresponding EventType.
    private Map<String, MetaData>   mRasEventTypeToMetaDataMap            = null;       // Map that takes a RAS EventType and gives you its corresponding MetaData.
    private Map<Integer, String>    mDataMoverResultTblIndxToTableNameMap = null;       // Map that takes a DataMover result's table index and gives you the corresponding table name.
    SignalHandler                   mSignalHandler                        = null;
    ClientStatusListenerExt         mVoltDbStatusListener                 = null;
    private Map<String, String>     mCompNodeLctnToHostnameMap            = null;       // Map that takes a Compute Node's lctn and gives you the corresponding hostname.
    private Map<String, String>     mServNodeLctnToHostnameMap            = null;       // Map that takes a Service Node's lctn and gives you the corresponding hostname.
    private Map<String, String>     mCompNodeHostnameToLctnMap            = null;       // Map that takes a Compute Node's hostname and gives you the corresponding lctn.
    private Map<String, String[]>   mNodeLctnToIpAddrAndBmcIpAddr         = null;
    private Map<String, String[]>   mCompNodeLctnToIpAddrAndBmcIpAddr     = null;
    private Map<String, String>     mBmcIpAddrToNodeLctn                  = null;
    private String                  mSnLctn                               = null;       // The lctn string of the service node that this adapter instance is running on.
    private long                    mPid                                  = -99999L;    // The pid (process id) for the process this adapter instance is running in.
    private Map<String, String>     mNodeLctnToAggregatorMap              = null;       // Map that takes a Service or Compute node's lctn and gives you its owning aggregator.
    private WorkQueue               mWorkQueue                            = null;
    private AdapterShutdownHandler  mShutdownHandler                      = null;


    //--------------------------------------------------------------------------
    // Methods
    //--------------------------------------------------------------------------
    public boolean               adapterAbnormalShutdown()               { return mAdapterAbnormalShutdown; }
    public void                  adapterAbnormalShutdown(boolean bFlag)  { mAdapterAbnormalShutdown = bFlag; }
    public long                  adapterId()                             { return mAdapterId; }
    public String                adapterName()                           { return mAdapterName; }
    public void                  adapterName(String sNewValue)           { mAdapterName = sNewValue; }
    public void                  adapterShutdownStarted(boolean bFlag)   { mAdapterShutdownStarted = bFlag; }
    public boolean               adapterShuttingDown()                   { return mAdapterShutdownStarted || mAdapterAbnormalShutdown; }
    public String                adapterType()                           { return mAdapterType; }
    public Client                client()                                { return mClient; }
    public void                  client(Client oNewValue)                { mClient = oNewValue; }
    public Map<Integer, String>  dataMoverResultTblIndxToTableNameMap()  { return mDataMoverResultTblIndxToTableNameMap; }
    public String                snLctn()                                { return mSnLctn; }
    public long                  pid()                                   { return mPid; }
    public WorkQueue             workQueue()                             { return mWorkQueue; }
    public void                  workQueue(WorkQueue workQueue)          { mWorkQueue = workQueue; }

    @Override
    public void                  setShutdownHandler(AdapterShutdownHandler handler)  { mShutdownHandler = handler; }
    @Override
    public Map<String, MetaData> cachedRasEventTypeToMetaDataMap()       { return mRasEventTypeToMetaDataMap; }

    @Override
    public WorkQueue setUpAdapter(String servers, String sSnLctn) throws AdapterException {
        AdapterShutdownHandler handler = new AdapterShutdownHandler() {
            @Override
            public void handleShutdown() {
                // Default is to do nothing and allow adapter to shut down immediately
            }
        };

        return setUpAdapter(servers, sSnLctn, handler);
    }

    @Override
    public WorkQueue setUpAdapter(String servers, String sSnLctn, AdapterShutdownHandler handler) throws AdapterException {
        mShutdownHandler = handler;

        enableSignalHandlers();

        try {
            mLogger.info("connecting to VoltDB servers - %s", servers);
            connectToDataStore(servers);
        } catch (IOException ex) {
            throw new AdapterException("Unable to establish connection to data store", ex);
        }

        loadRasMetadata();

        // These must be set before registering the adapter
        if (mAdapterName.isEmpty()) {
            mLogger.error("setUpAdapter() - Adapter name must be set before registering adapter!");
            throw new AdapterException("setUpAdapter() - Adapter name must be set before registering adapter!");
        }
        if (mAdapterType.isEmpty()) {
            mLogger.error("setUpAdapter() - Adapter type must be set before registering adapter!");
            throw new AdapterException("setUpAdapter() - Adapter type must be set before registering adapter!");
        }

        try {
            // Set up this adapter in the adapter table, indicating that this adapter has started and is active
            registerAdapter(sSnLctn);
        } catch (IOException | ProcCallException ex) {
            throw new AdapterException("An error occurred while registering this adapter with the data store", ex);
        }

        // Change this adapter instance's name to be unique (reflecting it adapter id)
        // - needs to occur after registering the adapter, as that assigns it its adapter id.
        if (mAdapterId <= 0) {
            mLogger.error("setUpAdapter() - Adapter ID is required to assign a unique name to adapter!");
            throw new AdapterException("setUpAdapter() - Adapter ID is required to assign a unique name to adapter!");
        }
        adapterName(mAdapterName + mAdapterId);
        mLogger.info("Setting adapter's name to %s", adapterName());

        // Create WorkQueue from the Factory (this includes setting up this adapter's base work item).
        DataStoreFactory factory = new DataStoreFactoryImpl(servers, mLogger);
        try {
            mWorkQueue = factory.createWorkQueue(client(), this);
        } catch(DataStoreException e) {
            throw new AdapterException("Failed to create the work item queue DataStore instance", e);
        }

        // Adapter should be ready for normal operation, now :)

        return mWorkQueue;
    }   // End setUpAdapter(String servers, String sSnLctn)

    @Override
    public boolean isComputeNodeLctn(String sLctn) throws IOException, ProcCallException
    {
        if (mapCompNodeLctnToSeqNum().get(sLctn) == null)
            return false;
        return true;
    }

    @Override
    public boolean isServiceNodeLctn(String sLctn) throws IOException, ProcCallException
    {
        if (mapServNodeLctnToHostName().get(sLctn) == null)
            return false;
        return true;
    }

    // Get the map of ComputeNode Lctns to SequenceNumbers - under the covers it will create and populate the map the first time it is accessed.
    // Note: this mapping should be invariant once the machine is defined, so there should be no need to periodically update it during the life of this Adapter.
    @Override
    public Map<String, Long> mapCompNodeLctnToSeqNum() throws IOException, ProcCallException {
        if (mCompNodeLctnToSeqNumMap == null) {
            // create and populate the map of Lctns and their Hostnames.
            Map<String, Long> tempMapLctnToSeqNum = new HashMap<String, Long>();
            // Get list of Lctns and their HostNames.
            ClientResponse response = client().callProcedure("ComputeNodeListLctnAndSeqNum");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapLctnToSeqNum.put(vt.getString("Lctn"), vt.getLong("SequenceNumber"));
            }
            mCompNodeLctnToSeqNumMap = tempMapLctnToSeqNum;
        }
        return mCompNodeLctnToSeqNumMap;
    }   // End mapCompNodeLctnToSeqNum()

    // Get the map of ComputeNode Lctns to Hostnames - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String>  mapCompNodeLctnToHostName() throws IOException, ProcCallException {
        if (mCompNodeLctnToHostnameMap == null) {
            // create and populate the map of Lctns and their Hostnames.
            Map<String, String> tempMapLctnToHostname = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            ClientResponse response = client().callProcedure("ComputeNodeListLctnAndHostname");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapLctnToHostname.put(vt.getString("Lctn"), vt.getString("HostName"));
            }
            mCompNodeLctnToHostnameMap = tempMapLctnToHostname;
        }
        return mCompNodeLctnToHostnameMap;
    }   // End mapCompNodeLctnToHostName()

    // Get the map of ComputeNode Hostnames to Lctns - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapCompNodeHostNameToLctn() throws IOException, ProcCallException {
        if (mCompNodeHostnameToLctnMap == null) {
            // create and populate the map of Hostnames and their Lctns.
            Map<String, String> tempMapHostnameToLctn = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            ClientResponse response = client().callProcedure("ComputeNodeListLctnAndHostname");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapHostnameToLctn.put(vt.getString("HostName"), vt.getString("Lctn"));
            }
            mCompNodeHostnameToLctnMap = tempMapHostnameToLctn;
        }
        return mCompNodeHostnameToLctnMap;
    }   // End mapCompNodeHostNameToLctn()

    // Get the map of ServiceNode Lctns to Hostnames - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapServNodeLctnToHostName() throws IOException, ProcCallException {
        if (mServNodeLctnToHostnameMap == null) {
            // create and populate the map of Lctns and their Hostnames.
            Map<String, String> tempMapLctnToHostname = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            ClientResponse response = client().callProcedure("ServiceNodeListLctnAndHostname");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapLctnToHostname.put(vt.getString("Lctn"), vt.getString("HostName"));
            }
            mServNodeLctnToHostnameMap = tempMapLctnToHostname;
        }
        return mServNodeLctnToHostnameMap;
    }   // End mapServNodeLctnToHostName()

    // Get the map that takes a Service or Compute node's lctn and gives you its owning aggregator - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapNodeLctnToAggregator() throws IOException, ProcCallException {
        if (mNodeLctnToAggregatorMap == null) {
            // create and populate the map of node lctns (compute and service) and their owning aggregator.
            Map<String, String> tempMapNodeLctnToAggregator = new HashMap<String, String>();

            // Get list of Compute node Lctns and their aggregator.
            ClientResponse response = client().callProcedure("ComputeNodesList");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapNodeLctnToAggregator.put(vt.getString("Lctn"), vt.getString("Aggregator"));
            }

            // Get list of Service node Lctns and their aggregator.
            response = client().callProcedure("ServiceNodesList");
            // Loop through each of the nodes that were returned.
            vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapNodeLctnToAggregator.put(vt.getString("Lctn"), vt.getString("Aggregator"));
            }

            mNodeLctnToAggregatorMap = tempMapNodeLctnToAggregator;
        }
        return mNodeLctnToAggregatorMap;
    }   // End mapNodeLctnToAggregator()


    @Override
    public String getNodesBmcIpAddr(String sNodeLctn) {
        try {
            String[] saIpAndBmcAddrs = mapNodeLctnToIpAddrAndBmcIpAddr().get(sNodeLctn);
            if (saIpAndBmcAddrs != null)
                return saIpAndBmcAddrs[1];
            return null;
        }
        catch (Exception e) {
            mLogger.exception(e, "Failed to get the nodes BMC IP address!");
            return null;
        }
    }
    @Override
    public Map<String, String[]> mapNodeLctnToIpAddrAndBmcIpAddr() throws IOException, ProcCallException {
        if (mNodeLctnToIpAddrAndBmcIpAddr == null) {
            // create and populate the map of Lctns and their IpAddr and BmcIpAddr.
            Map<String, String[]> tempMapLctnToIpAddrAndBmcIpAddr = new HashMap<>();

            // Get list of Compute node Lctns and their IP and BMC addresses.
            ClientResponse response = client().callProcedure("ComputeNodesList");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapLctnToIpAddrAndBmcIpAddr.put(vt.getString("Lctn")
                                                   ,new String[] { vt.getString("IpAddr"), vt.getString("BmcIpAddr") }
                                                   );
            }

            // Get list of Service node Lctns and their IP and BMC addresses.
            response = client().callProcedure("ServiceNodesList");
            // Loop through each of the nodes that were returned.
            vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapLctnToIpAddrAndBmcIpAddr.put(vt.getString("Lctn")
                                                   ,new String[] { vt.getString("IpAddr"), vt.getString("BmcIpAddr") }
                                                   );
            }

            mNodeLctnToIpAddrAndBmcIpAddr = tempMapLctnToIpAddrAndBmcIpAddr;
        }
        return mNodeLctnToIpAddrAndBmcIpAddr;
    }   // End mapNodeLctnToIpAddrAndBmcIpAddr()

    @Override
    public Map<String, String> mapBmcIpAddrToNodeLctn() throws IOException, ProcCallException {
        if (mBmcIpAddrToNodeLctn == null) {
            // create and populate the map of BmcIpAddr and Lctns.
            Map<String, String> tempBmcIpAddrToLctn = new HashMap<>();

            // Get list of Compute node Lctns and their IP and BMC addresses.
            ClientResponse response = client().callProcedure("ComputeNodesList");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempBmcIpAddrToLctn.put(vt.getString("BmcIpAddr"), vt.getString("Lctn"));
            }

            // Get list of Service node Lctns and their IP and BMC addresses.
            response = client().callProcedure("ServiceNodesList");
            // Loop through each of the nodes that were returned.
            vt = response.getResults()[0];
            for (int iSnCntr = 0; iSnCntr < vt.getRowCount(); ++iSnCntr) {
                vt.advanceRow();
                tempBmcIpAddrToLctn.put(vt.getString("BmcIpAddr"), vt.getString("Lctn"));
            }

            mBmcIpAddrToNodeLctn = tempBmcIpAddrToLctn;
        }
        return mBmcIpAddrToNodeLctn;
    } // End mapBmcIpAddrToNodeLctn()

    @Override
    public Map<String, String[]> mapCompNodeLctnToIpAddrAndBmcIpAddr() throws IOException, ProcCallException {
        if (mCompNodeLctnToIpAddrAndBmcIpAddr == null) {
            // create and populate the map of Lctns and their IpAddr and BmcIpAddr.
            Map<String, String[]> tempMapLctnToIpAddrAndBmcIpAddr = new HashMap<>();
            // Get list of Lctns and their HostNames.
            ClientResponse response = client().callProcedure("ComputeNodeListLctnIpAddrAndBmcIpAddr");
            // Loop through each of the nodes that were returned.
            VoltTable vt = response.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                tempMapLctnToIpAddrAndBmcIpAddr.put(vt.getString("Lctn"), new String[] {
                        vt.getString("IpAddr"),vt.getString("BmcIpAddr")});
            }
            mCompNodeLctnToIpAddrAndBmcIpAddr = tempMapLctnToIpAddrAndBmcIpAddr;
        }
        return mCompNodeLctnToIpAddrAndBmcIpAddr;
    }   // End mapCompNodeLctnToIpAddrAndBmcIpAddr()

    SignalHandler  signalHandler()  { return mSignalHandler; }

    // Constructor
    public Adapter(String sThisAdaptersAdapterType, String sAdapterName, Logger logger) throws IOException {
        mAdapterType = sThisAdaptersAdapterType;
        mAdapterName = sAdapterName;
        mLogger = logger;
        mParser = ConfigIOFactory.getInstance("json");
        if (mParser == null)  throw new RuntimeException("Adapter - Failed to create a JSON parser!");
        mRasDescNameToEventTypeMap = new HashMap<String, String>();    // map that takes a RAS DescriptiveName and gives you its corresponding EventType.
        mRasEventTypeToMetaDataMap = new HashMap<String, MetaData>();  // map that takes a RAS EventType and gives you its corresponding MetaData.
        mPid = IAdapter.getProcessPid();  // grab the pid of the process this adapter instance is running in.


        // Create mapping of DataMover result's table index and gives you the corresponding table name.
        // Note: when changing, also change in the DataMoverGetListOfRecsToMove.java stored procedure and ALSO in the AdapterOnlineTier constructor (list of tables that we will purge from)!!!
        mDataMoverResultTblIndxToTableNameMap = new HashMap<>();  // Map that takes a DataMover result's table index and gives you the corresponding table name.
        mDataMoverResultTblIndxToTableNameMap.put( 0, "Machine");
        mDataMoverResultTblIndxToTableNameMap.put( 1, "Job");
        mDataMoverResultTblIndxToTableNameMap.put( 2, "JobStep");
        mDataMoverResultTblIndxToTableNameMap.put( 3, "Rack");
        mDataMoverResultTblIndxToTableNameMap.put( 4, "Chassis");
        mDataMoverResultTblIndxToTableNameMap.put( 5, "ComputeNode");
        mDataMoverResultTblIndxToTableNameMap.put( 6, "ServiceNode");
        mDataMoverResultTblIndxToTableNameMap.put( 7, "ServiceOperation");
        mDataMoverResultTblIndxToTableNameMap.put( 8, "Replacement_History");
        mDataMoverResultTblIndxToTableNameMap.put( 9, "NonNodeHw_History");
        mDataMoverResultTblIndxToTableNameMap.put(10, "RasEvent");
        mDataMoverResultTblIndxToTableNameMap.put(11, "WorkItem");
        mDataMoverResultTblIndxToTableNameMap.put(12, "Adapter");
        mDataMoverResultTblIndxToTableNameMap.put(13, "BootImage");
        mDataMoverResultTblIndxToTableNameMap.put(14, "Switch");
        mDataMoverResultTblIndxToTableNameMap.put(15, "FabricTopology");
        mDataMoverResultTblIndxToTableNameMap.put(16, "Lustre");
        mDataMoverResultTblIndxToTableNameMap.put(17, "RasMetaData");
        mDataMoverResultTblIndxToTableNameMap.put(18, "WlmReservation_History");
        mDataMoverResultTblIndxToTableNameMap.put(19, "Diag");
        mDataMoverResultTblIndxToTableNameMap.put(20, "MachineAdapterInstance");
        mDataMoverResultTblIndxToTableNameMap.put(21, "UcsConfigValue");
        mDataMoverResultTblIndxToTableNameMap.put(22, "UniqueValues");
        mDataMoverResultTblIndxToTableNameMap.put(23, "Diag_Tools");
        mDataMoverResultTblIndxToTableNameMap.put(24, "Diag_List");
        mDataMoverResultTblIndxToTableNameMap.put(25, "DiagResults");
        mDataMoverResultTblIndxToTableNameMap.put(26, "NodeInventory_History");
        mDataMoverResultTblIndxToTableNameMap.put(27, "NonNodeHwInventory_History");
        mDataMoverResultTblIndxToTableNameMap.put(28, "HW_Inventory_Fru");
        mDataMoverResultTblIndxToTableNameMap.put(29, "HW_Inventory_Location");
        mDataMoverResultTblIndxToTableNameMap.put(30, "RawHWInventory_History");
    }   // End Adapter(String sThisAdaptersAdapterType, String sAdapterName)

    public void initialize() {
        initializeClient();
        initializeSignalHandler();
    }

    void initializeSignalHandler() {
        // Set up the signal handler object and method.
        mSignalHandler = new SignalHandler() {
            public void handle(Signal signal) {
                try {
                    mLogger.fatal("SignalHandler - a SIG%s signal (%d) has come in, this adapter is terminating now!",
                                  signal.getName(), signal.getNumber());
                    adapterShutdownStarted(true);  // Set flag indicating that we are going through adapter shutdown.
                    mLogger.fatal("SignalHandler - starting wait");
                    Thread.sleep(10 * 1000L);      // wait a little bit to give adapters a chance to clean up.
                    mLogger.fatal("SignalHandler - ending   wait");
                    mShutdownHandler.handleShutdown();
                    abend("SIG" + signal.getName());
                }
                catch (Exception e) {
                    mLogger.exception(e, "SignalHandler - an Exception occurred!");
                    System.exit(-1);
                }
            }
        };
    }

    void initializeClient() {
        // Set up the VoltDB Status Listener callback routine - handles any error conditions that occur outside of normal procedure execution.
        mVoltDbStatusListener = new ClientStatusListenerExt() {
            @Override public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
                if (!mAdapterShutdownStarted) {
                    mLogger.error("A connection to the database has been lost (%s:%d) - %s. There are %d connections remaining.", hostname, port, cause, connectionsLeft);
                }
            }
            @Override public void backpressure(boolean status) { mLogger.warn("Backpressure from the database is causing a delay in processing requests."); }
            @Override public void uncaughtException(ProcedureCallback callback, ClientResponse r, Throwable e) {
                mLogger.exception(e, "An error has occurred in a callback procedure. Check the following stack trace for details.");
            }
            @Override public void lateProcedureResponse(ClientResponse response, String hostname, int port) { mLogger.error("A procedure that timed out on host %s:%d has now responded.", hostname, port); }
        };
    }


    // TODO: duplicate found in com.intel.dai.dsimpl.voltdb.VoltDbRasEventLog
    @Override
    public void loadRasMetadata() {
        ClientResponse response = null;
        try {
            response = client().callProcedure("@AdHoc", "SELECT * FROM RasMetaData;");
        } catch (IOException | ProcCallException ex) {
            mLogger.exception(ex, "Unable to retrieve RAS meta data from the data store");
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store", ex);
        }

        VoltTable rasMetaData = response.getResults()[0];
        if (response.getStatus() != ClientResponse.SUCCESS) {
            mLogger.error("Unable to retrieve RAS meta data from the data store. Client response status: " + response.getStatus());
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store. Client response status: " + response.getStatus());
        }

        while (rasMetaData.advanceRow()) {
            // Populate the RAS DescriptiveName to EventType map.
            mRasDescNameToEventTypeMap.put(rasMetaData.getString("DescriptiveName"), rasMetaData.getString("EventType"));
            // Populate the RAS EventType to MetaData map.
            MetaData metaData = new MetaData();
            metaData.descriptiveName( response.getResults()[0].getString("DescriptiveName") );
            metaData.severity( response.getResults()[0].getString("Severity") );
            metaData.category( response.getResults()[0].getString("Category") );
            metaData.component( response.getResults()[0].getString("Component") );
            metaData.controlOperation( response.getResults()[0].getString("ControlOperation") );
            metaData.msg( response.getResults()[0].getString("Msg") );
            if (response.getResults()[0].getString("GenerateAlert").equals("Y"))
                metaData.generateAlert(true);
            else
                metaData.generateAlert(false);
            mRasEventTypeToMetaDataMap.put(rasMetaData.getString("EventType"), metaData);
        }
    }   // End loadRasMetadata()


    @Override
    public void enableSignalHandlers() {
        if (mShutdownHandler == null) {
            mLogger.error("enableSignalHandlers() - Shutdown handler must be set before enabling signal handlers!");
            throw new RuntimeException("enableSignalHandlers() - Shutdown handler must be set before enabling signal handlers!");
        }
        // Set up signal handlers for this process.
        Signal.handle(new Signal("HUP"),  signalHandler());
        Signal.handle(new Signal("INT"),  signalHandler());
        Signal.handle(new Signal("TERM"), signalHandler());
        Signal.handle(new Signal("USR2"), signalHandler());
        Signal.handle(new Signal("ABRT"), signalHandler());
    }

    //-------------------------------------------------------------------------
    // Async callback handler - used when people want to make async calls to stored procedures that only require that success or failure (in the ClientResponse) be checked for.
    // Parms:
    //      Adapter obj                 - adapter object
    //      String sAdapterType         - Type of the adapter that is "calling" this callback
    //      String sAdapterName         - The name of the adapter that is "calling" this callback
    //      String sSpThisIsCallbackFor - The stored procedure that was executed "by" this callback
    //      String sPertinentInfo       - Info that might be pertinent to this particular execution of this callback
    //      long   lWorkItemId          - The work item id that the adapter is "processing" during which it ends up using this callback
    //-------------------------------------------------------------------------
    static class MyCallbackForHouseKeepingNoRtrnValue implements ProcedureCallback {
        // Class constructor
        MyCallbackForHouseKeepingNoRtrnValue(Adapter obj, String sAdapterType, String sAdapterName, String sSpThisIsCallbackFor, String sPertinentInfo, long lWorkItemId) {
            mObj            = obj;
            mAdapterType    = sAdapterType;
            mAdapterName    = sAdapterName;
            mSpThisIsCallbackFor = sSpThisIsCallbackFor;
            mPertinentInfo  = sPertinentInfo;
            mWorkItemId     = lWorkItemId;
        }

        // Member data
        Adapter mObj;
        String mAdapterType;
        String mAdapterName;
        String mSpThisIsCallbackFor;    // stored procedure that this is a callback for (which stored procedure was being done by this callback)
        String mPertinentInfo;          // info that might be pertinent (included in log messages)
        long   mWorkItemId;

        // Member methods
        @Override
        public void clientCallback(ClientResponse response) throws IOException, ProcCallException, InterruptedException {
            // Ensure that the stored procedure was successful.
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                mObj.mLogger.error("MyCallbackForHouseKeepingNoRtrnValue - %s callback FAILED - Status=%s, StatusString='%s', PertinentInfo=%s!!!",
                             mSpThisIsCallbackFor, IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), mPertinentInfo);

            // IFF another RAS event has not been logged, log the generic callback failed RAS event.
                mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasGenAdapterMyCallbackForHouseKeepingNoRtrnValueFailed")
                                             ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                             ,null                                  // Lctn
                                             ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                             ,mAdapterType                          // type of adapter that is generating this ras event
                                             ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                             );
            }
            else {
                // stored procedure was successful.
                mObj.mLogger.info("MyCallbackForHouseKeepingNoRtrnValue - %s was successful, PertinentInfo=%s", mSpThisIsCallbackFor, mPertinentInfo);
                // Note: there is no additional processing needed here in this callback, anybody using this callback does not need/want anything else (except that the function that was invoked completed successfully)
            }
        }
    }   // End class MyCallbackForHouseKeepingNoRtrnValue

    @Override
    public ProcedureCallback createHouseKeepingCallbackNoRtrnValue(String sAdapterType,
                                                                   String sAdapterName,
                                                                   String sSpThisIsCallbackFor,
                                                                   String sPertinentInfo,
                                                                   long lWorkItemId) {
        return new MyCallbackForHouseKeepingNoRtrnValue(this, sAdapterType, sAdapterName, sSpThisIsCallbackFor,
                sPertinentInfo, lWorkItemId);
    }


    //-------------------------------------------------------------------------
    // Async Stored Procedure callback handler - used when people want to make async calls to stored procedures that returns a long numeric value.
    // Parms:
    //      Adapter obj                 - adapter object
    //      String sAdapterType         - Type of the adapter that is "calling" this callback
    //      String sAdapterName         - The name of the adapter that is "calling" this callback
    //      String sSpThisIsCallbackFor - The stored procedure that was executed "by" this callback
    //      String sPertinentInfo       - Info that might be pertinent to this particular execution of this callback
    //      long   lWorkItemId          - The work item id that the adapter is "processing" during which it ends up using this callback
    //-------------------------------------------------------------------------
    class MyCallbackForHouseKeepingLongRtrnValue implements ProcedureCallback {
        // Class constructor
        MyCallbackForHouseKeepingLongRtrnValue(Adapter obj, String sAdapterType, String sAdapterName, String sSpThisIsCallbackFor, String sPertinentInfo, long lWorkItemId) {
            mObj                    = obj;
            mAdapterType            = sAdapterType;
            mAdapterName            = sAdapterName;
            mSpThisIsCallbackFor    = sSpThisIsCallbackFor;
            mPertinentInfo          = sPertinentInfo;
            mWorkItemId             = lWorkItemId;
        }

        // Member data
        Adapter mObj;
        String  mAdapterType;
        String  mAdapterName;
        String  mSpThisIsCallbackFor;    // stored procedure that this is a callback for (which stored procedure was being done by this callback)
        String  mPertinentInfo;          // info that might be pertinent (included in log messages)
        long    mWorkItemId;

        // Member methods
        @Override
        public void clientCallback(ClientResponse response) throws IOException, ProcCallException, InterruptedException {
            // Ensure that the stored procedure was successful.
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                boolean bLoggedRasEvent = false;
                mObj.mLogger.error("MyCallbackForHouseKeepingLongRtrnValue - %s callback FAILED - Status=%s, StatusString='%s', PertinentInfo=%s, WorkItemId=%d!!!",
                                   mSpThisIsCallbackFor, IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), mPertinentInfo, mWorkItemId);

                switch(mSpThisIsCallbackFor) {
                    case "ComputeNodeDiscovered":
                        if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvCompNodeDiscFailedInvalidMacAddr")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,null                                  // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                    case "ServiceNodeDiscovered":
                        if (response.getStatusString().contains("no entry in the ServiceNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvServiceNodeDiscFailedInvalidMacAddr")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,null                                  // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                    case "ComputeNodeSaveIpAddr":
                        if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvCompNodeSaveIpAddrFailedInvalidMacAddr")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // requesting adapter work item id
                                                         );
                            bLoggedRasEvent = true;
                        } else if (response.getStatusString().contains("not the same as the expected IP address")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvCompNodeSaveIpAddrFailedInvalidIpAddr")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // requesting adapter work item id
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                    case "ServiceNodeSaveIpAddr":
                        if (response.getStatusString().contains("no entry in the ServiceNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvServiceNodeSaveIpAddrFailedInvalidMacAddr")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // requesting adapter work item id
                                                         );
                            bLoggedRasEvent = true;
                        } else if (response.getStatusString().contains("not the same as the expected IP address")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvServiceNodeSaveIpAddrFailedInvalidIpAddr")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // requesting adapter work item id
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                    case "ComputeNodeSetState":
                        if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvCompNodeSetStateFailedInvalidNode")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                            bLoggedRasEvent = true;
                        }
                        else {
                            // Check & see if attempting to change the node's state from error to active (that is an invalid transition).
                            if (response.getStatusString().contains("Invalid state change was attempted from ERROR to ACTIVE")) {
                                mObj.mLogger.error("MyCallbackForHouseKeepingLongRtrnValue - ComputeNodeSetState would not change the node state from error to active, node state left unchanged!");
                                // Indicate that we do not want the default RAS event logged.
                                bLoggedRasEvent = true;
                            }
                        }
                        break;
                    case "ServiceNodeSetState":
                        if (response.getStatusString().contains("no entry in the ServiceNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvServiceNodeSetStateFailedInvalidNode")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                    case "ComputeNodeSaveBootImageInfo":
                        if (response.getStatusString().contains("no entry in the ComputeNode table")) {
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvCompNodeSaveBootImageFailedInvalidNode")
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                    default:
                        mLogger.warn("Default case for was hit: %s", mSpThisIsCallbackFor);
                }

                // IFF another RAS event has not been logged, log the generic callback failed RAS event.
                if (!bLoggedRasEvent) {
                    mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasGenAdapterMyCallbackForHouseKeepingLongRtrnValueFailed")
                                                 ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                 ,null                                  // Lctn
                                                 ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                 ,mAdapterType                          // type of adapter that is generating this ras event
                                                 ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                 );
                    bLoggedRasEvent = true;
                }
            }   // stored procedure failed.
            else
            {   // stored procedure was successful.
                mObj.mLogger.info("MyCallbackForHouseKeepingLongRtrnValue - %s was successful, PertinentInfo=%s, WorkItemId=%d", mSpThisIsCallbackFor, mPertinentInfo, mWorkItemId);
                long lRtrnValue = response.getResults()[0].asScalarLong();  // save the stored procedure's results (returned as a scalar long).
                switch (mSpThisIsCallbackFor) {
                    ////case "RasEventStore":
                    ////    if (lRtrnValue < 0) {
                    ////        // Ras event was successfully generated AND this particular ras event does have a ControlOperation associated with it.
                    ////        // Wake up a RAS adapter to notify it that a RAS event with a ControlOperation has been stored in the data store!
                    ////        mObj.mLogger.warn("MyCallbackForHouseKeepingLongRtrnValue - a ras event was stored that has a ControlOperation associated with it, unable to wake up the RAS adapter as scon wakeup has not been implemented!");
                    ////        // ToDo: implement scon mechanism to wake up a RAS adapter (so it is aware that ras event with ControlOperation has been added to data store)!!!
                    ////    }
                    ////    break;
                    case "ComputeNodeDiscovered": case "ComputeNodeSaveBootImageInfo": case "ComputeNodeSaveIpAddr":
                    case "ServiceNodeDiscovered": case "ServiceNodeSaveIpAddr": case "ServiceNodeSetState":
                        if (lRtrnValue > 0) {
                            // everything completed fine but we detected that this item did occur out of timestamp order.
                            mObj.mLogger.info("MyCallbackForHouseKeepingLongRtrnValue - %s - %s - fyi, this item occurred out of timestamp order in regards to similar items, but it still has been handled properly",
                                              mSpThisIsCallbackFor, mPertinentInfo);
                            String sTempLctn = mPertinentInfo;
                            if (mPertinentInfo.contains(KeyForLctn)) {
                                String[] sParms = mPertinentInfo.split(",");
                                for (String parms: sParms) {
                                    if (parms.startsWith(KeyForLctn)) {
                                        sTempLctn = parms.substring(KeyForLctn.length());
                                        break;
                                    }
                                }
                            }
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasGenAdapterDetectedOutOfTsOrderItem")
                                 ,("AdapterName=" + mAdapterName + ", StoredProcedure=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo)
                                 ,sTempLctn                             // Lctn
                                 ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                 ,mAdapterType                          // type of adapter that is generating this ras event
                                 ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                 );
                        }
                        break;
                    case "ComputeNodeSetState":
                        String sTempLctn = null;
                        String sTempTimeInMicroSecs = null;
                        String[] sParms = null;
                        if (lRtrnValue > 0) {
                            // everything completed fine but we detected that this item did occur out of timestamp order.
                            mObj.mLogger.info("MyCallbackForHouseKeepingLongRtrnValue - %s - %s - fyi, this item occurred out of timestamp order in regards to similar items, but it still has been handled properly",
                                              mSpThisIsCallbackFor, mPertinentInfo);
                            sTempLctn = mPertinentInfo;
                            if (mPertinentInfo.contains(KeyForLctn)) {
                                if (sParms == null)
                                    sParms = mPertinentInfo.split(",");
                                for (String parms: sParms) {
                                    if (parms.startsWith(KeyForLctn)) {
                                        sTempLctn = parms.substring(KeyForLctn.length());
                                        break;
                                    }
                                }
                            }
                            mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasGenAdapterDetectedOutOfTsOrderItem")
                                 ,("AdapterName=" + mAdapterName + ", StoredProcedure=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo)
                                 ,sTempLctn                           // Lctn
                                 ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                 ,mAdapterType                        // type of adapter that is generating this ras event
                                 ,mWorkItemId                         // work item that is being worked on that resulted in the generation of this ras event
                                 );
                        }
                        // Cut a ras event to indicate that this node is now active (if appropriate) - I know that there is no job effected by this occurrence.
                        if (mPertinentInfo.contains("NewState=A")) {
                            // we just set the compute node state to active.
                            // Get the lctn string (if appropriate)
                            if (sTempLctn == null) {
                                if (mPertinentInfo.contains(KeyForLctn)) {
                                    if (sParms == null)
                                        sParms = mPertinentInfo.split(",");
                                    for (String parms: sParms) {
                                        if (parms.startsWith(KeyForLctn)) {
                                            sTempLctn = parms.substring(KeyForLctn.length());
                                            break;
                                        }
                                    }
                                }
                            }
                            // Get the TimeInMicroSecs string (if appropriate)
                            if (sTempTimeInMicroSecs == null) {
                                if (mPertinentInfo.contains(KeyForTimeInMicroSecs)) {
                                    if (sParms == null)
                                        sParms = mPertinentInfo.split(",");
                                    for (String parms: sParms) {
                                        if (parms.startsWith(KeyForTimeInMicroSecs)) {
                                            sTempTimeInMicroSecs = parms.substring(KeyForTimeInMicroSecs.length());
                                            break;
                                        }
                                    }
                                }
                                if (sTempTimeInMicroSecs == null)
                                    sTempTimeInMicroSecs = Long.toString(System.currentTimeMillis() * 1000L);
                            }
                            if (!mPertinentInfo.contains(KeyForUsingSynthData)) {
                                // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
                                mObj.logRasEventNoEffectedJob(mObj.getRasEventType("RasProvNodeActive")
                                     ,("AdapterName=" + mAdapterName + ", Lctn=" + sTempLctn)
                                     ,sTempLctn                            // lctn
                                     ,Long.parseLong(sTempTimeInMicroSecs) // time this occurred, in micro-seconds since epoch
                                     ,mAdapterType                         // type of adapter that is generating this ras event
                                     ,mWorkItemId                          // work item that is being worked on that resulted in the generation of this ras event
                                     );
                            }   // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
                            else {
                                // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                                mObj.mLogger.warn("@@@ did not cut the node active ras event (since machine is being run with synthesized data) - %s is active @@@",
                                                  sTempLctn);
                            }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                        }
                        break;
                    default:
                        break;
                }
            }   // stored procedure was successful.
        }
    }   // End class MyCallbackForHouseKeepingLongRtrnValue

    @Override
    public ProcedureCallback createHouseKeepingCallbackLongRtrnValue(String sAdapterType,
                                                                     String sAdapterName,
                                                                     String sSpThisIsCallbackFor,
                                                                     String sPertinentInfo,
                                                                     long lWorkItemId) {
        return new MyCallbackForHouseKeepingLongRtrnValue(this, sAdapterType, sAdapterName, sSpThisIsCallbackFor,
                sPertinentInfo, lWorkItemId);
    }

    // Set up this adapter in the adapter table, indicating that this adapter has started and is active
    // (this will be done synchronously as we really can't do anything else before this is done anyway).
    @Override
    public void registerAdapter(String sSnLctn) throws IOException, ProcCallException {
        // Add entry for this particular adapter in the adapter table.
        ClientResponse response = client().callProcedure("AdapterStarted"
                                                        ,mAdapterType        // this particular adapter's type
                                                        ,-1                  // work item id that requested this method, -1 being used since there is no work item yet associated with this new adapter
                                                        ,sSnLctn             // lctn of the service node that this adapter instance is running on.
                                                        ,pid()               // pid of this adapter instance's process
                                                        );
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            mLogger.error("registerAdapter - AdapterStarted stored procedure FAILED for this adapter - Status=%s, StatusString=%s, AdapterType=%s, Lctn=%s, Pid=%d!",
                          IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), mAdapterType, sSnLctn, pid());
            throw new RuntimeException(response.getStatusString());
        }
        // Set/save the value for this adapter's adapter id.
        long lThisAdaptersAdapterId = response.getResults()[0].asScalarLong();
        mAdapterId = lThisAdaptersAdapterId;
        // Set/save the lctn string of the service node this adapter instance is running on.
        mSnLctn = sSnLctn;
        mLogger.info("registerAdapter - successfully started adapter, AdapterType=%s, AdapterId=%d, Lctn=%s, Pid=%d", mAdapterType, mAdapterId, sSnLctn, pid());
    }   // End registerAdapter(String sSnLctn)

    void adapterTerminating(String sBaseWorkItemResults) throws IOException, InterruptedException, AdapterException  {
        // Remove this adapter from the adapter table, indicating that this adapter has terminated
        teardownAdapter();
        // Mark this adapter's base work item as Finished (and implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
        // (this will be done synchronously).
        teardownAdaptersBaseWorkItem(sBaseWorkItemResults);
        // Close the connections to db nodes.
        client().drain(); // ensure that all async calls have completed.
        client().close(); // close all of the connections and release any resources associated with the client.
    }   // End adapterTerminating(String sBaseWorkItemResults)


    //--------------------------------------------------------------------------
    // Mark this adapter's base work item as Finished (and also implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
    //--------------------------------------------------------------------------
    // This version of the method is used when self-initiated.
    private void teardownAdaptersBaseWorkItem(String sBaseWorkItemResults) throws IOException, AdapterException { teardownAdaptersBaseWorkItem(sBaseWorkItemResults, mAdapterType, workQueue().baseWorkItemId()); }
    // This version of the method is used when the DaiMgr detects an adapter instance ended, and needs to clean up its adapter entry and its base work item.

    public void teardownAdaptersBaseWorkItem(String sBaseWorkItemResults, String sTermAdapterType, long lTermAdapterBaseWorkItemId) throws IOException, AdapterException {
        ClientResponse response;
        try {
            response = client().callProcedure("WorkItemFinished"
                    ,sTermAdapterType            // the type of adapter that is being terminated
                    ,lTermAdapterBaseWorkItemId  // the specific base work item for the adapter that is being terminated
                    ,sBaseWorkItemResults        // results for the specific base work item for the adapter that is being terminated
            );
        } catch(ProcCallException e) {
            mLogger.exception(e, "teardownAdaptersBaseWorkItem - stored procedure WorkItemFinished FAILED");
            throw new AdapterException("Datastore procedure call failed", e);
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            mLogger.error("teardownAdaptersBaseWorkItem - stored procedure WorkItemFinished FAILED - Status=%s, StatusString=%s, TermAdapterType=%s, TermAdapterBaseWorkItemId=%d, TermAdapterBaseWorkItemResults=%s!",
                          IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), sTermAdapterType, lTermAdapterBaseWorkItemId, sBaseWorkItemResults);
            throw new RuntimeException(response.getStatusString());
        }
        mLogger.info("teardownAdaptersBaseWorkItem - successfully finished this adapters base work item - TermAdapterType=%s, TermAdapterBaseWorkItemId=%d, TermAdapterBaseWorkItemResults=%s",
                     sTermAdapterType, lTermAdapterBaseWorkItemId, sBaseWorkItemResults);
    }   // End teardownAdaptersBaseWorkItem(String sBaseWorkItemResults, String sTermAdapterType, long lTermAdapterBaseWorkItemId)


    //--------------------------------------------------------------------------
    // Remove this adapter from the adapter table, indicating that this adapter has terminated
    // (this will be done synchronously).
    //--------------------------------------------------------------------------
    // This version of the method is used when self-initiated.
    private void teardownAdapter() throws IOException, AdapterException { teardownAdapter(mAdapterType, mAdapterId, mAdapterType, workQueue().baseWorkItemId()); }
    // This version of the method is used when the DaiMgr detects an adapter instance ended, and needs to clean up its adapter entry and its base work item.
    public void teardownAdapter(String sTermAdapterType, long lTermAdapterId, String sReqAdapterType,
                                long lReqWorkItemId) throws IOException, AdapterException {
        ClientResponse response;

        try {
            response = client().callProcedure("AdapterTerminated"
                    ,sTermAdapterType    // the type of adapter that is being terminated
                    ,lTermAdapterId      // the adapter id of the adapter that is being terminated
                    ,sReqAdapterType     // the type of adapter that requested the specified adapter be terminated
                    ,lReqWorkItemId      // work item Id that the requesting adapter was performing when it requested this adapter be terminated
            );
        } catch(ProcCallException e) {
            mLogger.exception(e, "teardownAdapter - stored procedure AdapterTerminated FAILED");
            throw new AdapterException("Datastore procedure call failed", e);
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            mLogger.error("teardownAdapter - stored procedure AdapterTerminated FAILED - Status=%s, StatusString=%s, TermAdapterType=%s, TermAdapterId=%d, ReqAdapterType=%s, ReqWorkItemId=%d!",
                          IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), sTermAdapterType, lTermAdapterId, sReqAdapterType, lReqWorkItemId);
            throw new RuntimeException(response.getStatusString());
        }
        mLogger.info("teardownAdapter - successfully terminated adapter, TermAdapterType=%s, TermAdapterId=%d, ReqAdapterType=%s, ReqWorkItemId=%d",
                     sTermAdapterType, lTermAdapterId, sReqAdapterType, lReqWorkItemId);
    }   // End teardownAdapter(String sAdapterType, long lAdapterId, String sReqAdapterType, long lReqWorkItemId)


    @Override
    public void abend(String sReason) throws IOException, InterruptedException, AdapterException {
        logRasEventSyncNoEffectedJob(getRasEventType("RasGenAdapterAbend")
                                    ,("AdapterName=" + mAdapterName + ", Reason=" + sReason)
                                    ,null                               // lctn
                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,mAdapterType                       // type of the adapter that is requesting/issuing this invocation
                                    ,workQueue().baseWorkItemId()       // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                    );
        adapterTerminating("Work Item terminated ABNORMALLY (" + sReason + ")");
        mLogger.fatal("Finished ABNORMALLY (%s)!", sReason);
        System.exit(-1);
    }   // End abend(String sReason)


    //--------------------------------------------------------------------------
    // Multiple methods which log RAS Events into the data store.
    // 1) logRasEventNoEffectedJob       - this method should be used when the caller is certain that NO job was effected by the "event" whose occurrence triggered the need to log this RAS event.
    // 2) logRasEventWithEffectedJob     - this method should be used when the caller knows WHICH job was effected by the "event" whose occurrence triggered the need to log this RAS event.
    // 3) logRasEventCheckForEffectedJob - this method should be used when the caller does NOT know if any job was was effected by the "event" whose occurrence triggered the need to log this RAS event.
    //                                      (this method forces the RAS adapter to try and determine which job, if any, was effected.
    //                                       The adapter will update the RAS event's JobID field with null if no job was effected, or the JobId of the effected job.)
    //  Parameters:
    //      sLctn indicates what hardware location was effected by the "event" whose occurrence triggered the need to log this RAS event.
    //          A value of either null or zero length will result in null value being stored in the data store.
    //--------------------------------------------------------------------------
    // Log the specified ras event used when the caller is certain that NO job was effected by the "event" whose occurrence caused the logging of this ras event.
    @Override
    public void logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        String sTempStoredProcedure = "RasEventStore";
        MyCallbackForHouseKeepingLongRtrnValue oTempCallback = null;
        try {
            //mLogger.info("%s - logRasEventNoEffectedJob - before MyCallbackForHouseKeepingLongRtrnValue ctor - this=%s, sReqAdapterType=%s, mAdapterName=%s, sTempStoredProcedure=%s, sEventType=%s, lReqWorkItemId=%d", mAdapterName, this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventType, lReqWorkItemId);  // temporary debug for java.lang.NoClassDefFoundError
            oTempCallback = new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventType, lReqWorkItemId);
            client().callProcedure(oTempCallback         // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sEventType            // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,null                  // null indicates that we KNOW that no job was effected by the "event" whose occurrence caused the logging of this ras event.
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
            mLogger.info("logRasEventNoEffectedJob - called stored procedure %s asynchronously - EventType=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sInstanceData);
        }
        catch (Exception e) {
            mLogger.error("logRasEventNoEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            mLogger.error("logRasEventNoEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)

    // Log the specified ras event used when the caller is certain that NO job was effected by the "event" whose occurrence caused the logging of this ras event.
    // Note: this use synchronous data store updates!
    public void logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        String sTempStoredProcedure = "RasEventStore";
        try {
            mLogger.info("logRasEventSyncNoEffectedJob - calling stored procedure %s synchronously - EventType=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sInstanceData);
            client().callProcedure(sTempStoredProcedure  // stored procedure name
                                  ,sEventType            // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,null                  // null indicates that we KNOW that no job was effected by the "event" whose occurrence caused the logging of this ras event.
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
        }
        catch (Exception e) {
            mLogger.error("logRasEventSyncNoEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            mLogger.error("logRasEventSyncNoEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    // Log the specified ras event when the caller knows WHICH job was effected by the "event" whose occurrence caused the logging of this ras event.
    @Override
    public void logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        String sTempStoredProcedure = "RasEventStore";
        try {
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventType, lReqWorkItemId)  // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sEventType            // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,sJobId                // jobId of a job that was effected by this ras event
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
            mLogger.info("logRasEventWithEffectedJob - called stored procedure %s asynchronously - EventType=%s, Lctn=%s, JobId=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sJobId, sInstanceData);
        }
        catch (Exception e) {
            mLogger.error("logRasEventWithEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            mLogger.error("logRasEventWithEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)

    // Log the specified ras event used when the caller is not sure whether or not a job was effected by the "event" whose occurrence caused the logging of this ras event.
    @Override
    public void logRasEventCheckForEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        String sTempStoredProcedure = "RasEventStore";
        try {
            // Ensure that a lctn was specified, no use looking for an associated job if there was no lctn specified.
            if ((sLctn == null) || (sLctn.trim().isEmpty())) {
                mLogger.info("logRasEventCheckForEffectedJob - no lctn was specified on this invocation so we are invoking logRasEventNoEffectedJob() instead - EventType=%s, Lctn=%s, InstanceData='%s'", sEventType, sLctn, sInstanceData);
                logRasEventNoEffectedJob(sEventType, sInstanceData, sLctn, lTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                return;
            }
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventType, lReqWorkItemId)  // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sEventType            // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,"?"                   // ? indicates that we don't know whether or not any job was effected by the "event" whose occurrence caused the logging of this ras event.
                                                         //   This means that the Ras Adapter will have to try and determine if a job was effected:
                                                         //      - if a job was effected it will update the RAS event record with its JobId
                                                         //      - if a job was not effected it will update the RAS event record to have null as the JobId
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
            mLogger.info("logRasEventCheckForEffectedJob - called stored procedure %s asynchronously - EventType=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sInstanceData);
        }
        catch (Exception e) {
            mLogger.error("logRasEventCheckForEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            mLogger.error("logRasEventCheckForEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventCheckForEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    // This method gets the Ras EventType that corresponds to the specified DescriptiveName.
    @Override
    public String getRasEventType(String sDescriptiveName) {
        String sRasEventType = mRasDescNameToEventTypeMap.get(sDescriptiveName);
        // Ensure that we got a "valid" EventType back.
        if (sRasEventType != null && !sRasEventType.isEmpty()) {
            return sRasEventType;
        }
        else {
            // got an invalid ras EventType.
            logRasEventNoEffectedJob("0001000013"                           // "0001000013" is a Ras EventType indicating that the specified descriptive name does not exist in RasEventMetaData
                                    ,("AdapterName=" + mAdapterName + ", DescriptiveName=" + sDescriptiveName)
                                    ,null                                   // lctn
                                    ,System.currentTimeMillis() * 1000L     // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,mAdapterType                           // type of the adapter that is requesting/issuing this invocation
                                    ,workQueue().baseWorkItemId()           // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                    );
            return "RasUnknownEvent"; // "RasUnknownEvent" is used as a RAS EventType indicating that the specified descriptive could not be translated into the corresponding EventType
        }
    }   // End getRasEventType(String sDescriptiveName)


    //--------------------------------------------------------------------------
    // Add the RAS Meta Data into the RasMetaData table in the data store (if appropriate)
    // (reads the json data out of a file named RasEventMetaData.json).
    //--------------------------------------------------------------------------
    @Override
    public void addRasMetaDataIntoDataStore() throws IOException, ProcCallException {
        DecimalFormat decimalFormatter = new DecimalFormat("#,###,###");  // pretty formatting
        long lCountOfEventMetaData = client().callProcedure("RasEventCountMetaDataEntries").getResults()[0].asScalarLong();
        if (lCountOfEventMetaData > 0) {
            // the data store already contains ras event meta data.
            mLogger.info("Did NOT insert the RAS Event Meta data (as there are already %s event meta data entries in the schema)", decimalFormatter.format(lCountOfEventMetaData));  // pretty formatting
        }
        else {
            // schema does not have any ras event meta data in it so go ahead and add it.
            mLogger.info("Inserting the RAS Meta Data into the data store");
            // Actually add the Ras Event Meta Data into the data store.
            XdgConfigFile config = new XdgConfigFile(XDG_COMPONENT);
            InputStream stream = config.Open(RASMETADATA_FILE);
            if(stream == null) {
                Exception e = new FileNotFoundException(String.format("File '%s' was not found in the XDG_PATH!", RASMETADATA_FILE));
                mLogger.exception(e);
                System.exit(-1);
            }
            try {
                PropertyMap jsonObject = mParser.readConfig(stream).getAsMap();
                PropertyArray listOfEvents = jsonObject.getArray("Events");
                if(listOfEvents == null)
                    throw new RuntimeException(String.format("'%s' has no RAS metadata!", RASMETADATA_FILE));
                for(int index =0; index < listOfEvents.size(); index++) {
                    PropertyMap event = listOfEvents.getMap(index);
                    if (event == null) {
                        mLogger.error("'event' cannot be null!");
                        throw new RuntimeException("'event' cannot be null!");
                    }
                    // Check & see if the ControlOperation field is really null (note: this is different that a string value of "null").
                    String operation = event.getStringOrDefault("ControlOperation", null);
                    if (operation == null) {
                        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, mAdapterType, mAdapterName, "RASMETADATA.upsert", event.getString("EventType"), workQueue().baseWorkItemId()),
                                "RASMETADATA.upsert", event.getString("EventType"), event.getString("DescriptiveName"),
                                event.getString("Severity"), event.getString("Category"), event.getString("Component"),
                                null, event.getString("Msg"), (System.currentTimeMillis() * 1000), event.getStringOrDefault("GenerateAlert", "N"));
                    }
                    else {
                        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, mAdapterType, mAdapterName, "RASMETADATA.upsert", event.getString("EventType"), workQueue().baseWorkItemId()),
                                "RASMETADATA.upsert", event.getString("EventType"), event.getString("DescriptiveName"),
                                event.getString("Severity"), event.getString("Category"), event.getString("Component"),
                                event.getString("ControlOperation"), event.getString("Msg"), (System.currentTimeMillis() * 1000), event.getStringOrDefault("GenerateAlert", "N"));
                    }
                }
            } catch (IOException | RuntimeException | ConfigIOParseException | PropertyNotExpectedType e) {
                mLogger.exception(e);
                System.exit(-1);
            } finally {
                stream.close();
            }
        }
    }   // End addRasMetaDataIntoDataStore()



    //--------------------------------------------------------------------------
    // Kill a job.
    //--------------------------------------------------------------------------
    @Override
    public void killJob(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
                        throws IOException, ProcCallException, InterruptedException
    {
        // Ensure that a JobId was specified.
        if (sRasEventJobId == null) {
            mLogger.warn("killJob - attempted to run a control operation that included killing a job, but there is no job associated with this RAS event - " +
                         "skipping that portion of the ControlOperation - NodeLctn=%s, EventType=%s, EventId=%d, CtrlOp=%s",
                         sNodeLctn, sRasEventType, lRasEventId, sRasEventCntrlOp);
            return;
        }
        // Ensure that this is a compute node.
        if (!isComputeNodeLctn(sNodeLctn)) {
            // this is not a compute node so there is no reason to issue the kill job work item, ignoring this directive.
            mLogger.warn("killJob - received a request to kill a job on this node, but this node is not a compute node so silently ignoring this directive - " +
                         "skipping sending the work item - NodeLctn=%s, EventType=%s, EventId=%d, CtrlOp=%s, JobId=%s",
                         sNodeLctn, sRasEventType, lRasEventId, sRasEventCntrlOp, sRasEventJobId);
            return;
        }

        // Send a work item to the WLM adapter/provider to kill the specified job.
//        Map<String, String> parameters = new HashMap<>();
//        parameters.put("jobid", sRasEventJobId);
//        long lKjWorkItemId = workQueue().queueWorkItem("WLM"                // type of adapter that needs to handle this work
//                                                      ,null                 // queue this work should be put on
//                                                      ,"KillJob"            // work that needs to be done
//                                                      ,parameters             // parameters for this work
//                                                      ,false                // false indicates that we do NOT want to know when this work item finishes
//                                                      ,sWorkItemAdapterType // type of adapter that requested this work to be done
//                                                      ,lWorkItemId          // work item that the requesting adapter was working on when it requested this work be done
//                                                      );
//        mLogger.info("killJob - successfully queued KillJob work item - EventType=%s, EventId=%d, ControlOperation=%s, Lctn=%s, JobId=%s, WorkItemId=%d",
//                     sRasEventType, lRasEventId, sRasEventCntrlOp, sNodeLctn, sRasEventJobId, lKjWorkItemId);
    }   // End killJob(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Mark this node as Active / Available / Usable, including telling WLM that it can attempt to use this node.
    // Parms:
    //  boolean bUsingSynthData - flag indicating whether synthesized data or "real" data is being used to drive the machine
    //--------------------------------------------------------------------------
    @Override
    public void markNodeActive(String sNodeLctn, boolean bUsingSynthData, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException, ProcCallException, InterruptedException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ComputeNodeSetState";
        }
        else if (isServiceNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ServiceNodeSetState";
        }
        else {
            mLogger.warn("markNodeActive - Cannot update state for a non-compute or non-service location: %s ", sNodeLctn);
            return;
        }
        // Set the node's state to Active state - simply recording state change info, no need to wait for ack that this work has completed
        String sTempNewState = "A";  // state of A is Active/Available/Usable.
        String sPertinentInfo = KeyForLctn + sNodeLctn + ",NewState=A," + KeyForTimeInMicroSecs + Long.toString(lTsInMicroSecs);
        if (bUsingSynthData)
            sPertinentInfo += ("," + KeyForUsingSynthData);
        client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sPertinentInfo, lWorkItemId) // asynchronously invoke the procedure
                              ,sTempStoredProcedure  // stored procedure name
                              ,sNodeLctn             // node's location string
                              ,sTempNewState         // node's new state
                              ,lTsInMicroSecs        // time that this occurred in microseconds since epoch
                              ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                              ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("markNodeActive - called stored procedure %s - Lctn=%s, NewState=%s, PertinentInfo=%s",
                     sTempStoredProcedure, sNodeLctn, sTempNewState, sPertinentInfo);
        //----------------------------------------------------------------------
        // Note: when ComputeNodes become active here, we NO longer tell the WLM that it can start using this node!
        //       Rather this is handled when the 'UcsState "state":"wlm-daemon-active"' message comes up through the node's console!
        //----------------------------------------------------------------------
    }   // End markNodeActive(String sNodeLctn, boolean bUsingSynthData, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Mark this node as Powered Off / Missing / Unusable, including taking this node away from WLM so it doesn't attempt to run jobs on it.
    //--------------------------------------------------------------------------
    @Override
    public void markNodePoweredOff(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException, ProcCallException, InterruptedException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ComputeNodeSetState";
        } else if(isServiceNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ServiceNodeSetState";
        } else {
            mLogger.warn("markNodePoweredOff - Cannot update state for a non-compute or non-service location: %s ", sNodeLctn);
            return;
        }
        // Set the node's state to Missing state - simply recording state change info, no need to wait for ack that this work has completed
        String sTempNewState = "M";  // state of M is Missing (not available/active in the machine).
        client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                             ,sTempStoredProcedure  // stored procedure name
                             ,sNodeLctn             // node's location string
                             ,sTempNewState         // node's new state
                             ,lTsInMicroSecs        // time that this occurred in microseconds since epoch
                             ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                             ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("markNodePoweredOff - called stored procedure %s - Lctn=%s, NewState=%s", sTempStoredProcedure, sNodeLctn, sTempNewState);
        // Tell WLM to not use the specified node.
        tellWlmToNotUseThisNode(sNodeLctn, sWorkItemAdapterType, lWorkItemId, "NodeWasPoweredOff");
    }   // End markNodePoweredOff(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)

    //--------------------------------------------------------------------------
    // Mark this node as in BiosStarting, reiterate that WLM can't use this node for running jobs.
    //--------------------------------------------------------------------------
    @Override
    public void markNodeBiosStarting(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
            throws IOException, ProcCallException, InterruptedException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ComputeNodeSetState";
        }else if(isServiceNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ServiceNodeSetState";
        } else {
            mLogger.warn("Cannot update state for a non-compute or non-service location: %s ", sNodeLctn);
            return;
        }
        // Set the node's state to Missing state - simply recording state change info, no need to wait for ack that this work has completed
        String sTempNewState = "B";  // state of B is BiosStarting.
        client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                ,sTempStoredProcedure  // stored procedure name
                ,sNodeLctn             // node's location string
                ,sTempNewState         // node's new state
                ,lTsInMicroSecs        // time that this occurred in microseconds since epoch
                ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("markNodeBooting - called stored procedure %s - Lctn=%s, NewState=%s", sTempStoredProcedure, sNodeLctn, sTempNewState);
        // Tell WLM to not use the specified node.
        tellWlmToNotUseThisNode(sNodeLctn, sWorkItemAdapterType, lWorkItemId, "BiosStarting");
    }   // End markNodeBiosStarting(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Mark this node as being in Error state, including taking this node away from WLM so it doesn't attempt to run jobs on it.
    //--------------------------------------------------------------------------
    // Note: this version is used when handling a RAS event's control operation, other versions can be created for non-Control Operation scenarios!
    @Override
    public void markNodeInErrorState(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException, ProcCallException, InterruptedException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ErrorOnComputeNode";
        }
        else {
            sTempStoredProcedure = "ErrorOnServiceNode";
        }
        // Set the node's state to Error state - simply recording state change info, no need to wait for ack that this work has completed
        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                             ,sTempStoredProcedure  // stored procedure name
                             ,sNodeLctn             // node's location string
                             ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                             ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("markNodeInErrorState - called stored procedure %s - Lctn=%s, EventType=%s, EventId=%d, ControlOperation=%s, JobId=%s", sTempStoredProcedure, sNodeLctn, sRasEventType, lRasEventId, sRasEventCntrlOp, sRasEventJobId);
        //----------------------------------------------------------------------
        // Tell WLM to not use this specified node.
        //----------------------------------------------------------------------
        // Get the descriptive name that corresponds to this RAS event type - if no descriptive name for this event type, use the event type.
        String sTempSlurmReason = null;
        MetaData oMetaData = mRasEventTypeToMetaDataMap.getOrDefault(sRasEventType, null);
        if (oMetaData == null) {
            // meta data is not available for the specified ras event type.
            mLogger.error("markNodeInErrorState - the event meta data is not available - Lctn=%s, EventType=%s, EventId=%d, ControlOperation=%s, JobId=%s", sTempStoredProcedure, sNodeLctn, sRasEventType, lRasEventId, sRasEventCntrlOp, sRasEventJobId);
            sTempSlurmReason = sRasEventType;  // since the event's meta data is not available, use the ras event type for the slurm reason.
        }
        else {
            sTempSlurmReason = oMetaData.descriptiveName();  // use the event's descriptive name for the slurm reason.
        }
        tellWlmToNotUseThisNode(sNodeLctn, sWorkItemAdapterType, lWorkItemId, sTempSlurmReason);
    }   // End markNodeInErrorState(String sNodeLctn, String sRasEventType, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)

    // Note: this version is used for a non-RAS event scenarios!
    @Override
    public void markNodeInErrorState(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException, ProcCallException, InterruptedException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sNodeLctn))
            sTempStoredProcedure = "ErrorOnComputeNode";
        else
            sTempStoredProcedure = "ErrorOnServiceNode";
        // Set the node's state to Error state - simply recording state change info, no need to wait for ack that this work has completed
        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                              ,sTempStoredProcedure  // stored procedure name
                              ,sNodeLctn             // node's location string
                              ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                              ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("markNodeInErrorState - called stored procedure %s - Lctn=%s", sTempStoredProcedure, sNodeLctn);
        // Tell WLM to not use this specified node.
        tellWlmToNotUseThisNode(sNodeLctn, sWorkItemAdapterType, lWorkItemId, "markNodeInErrorState");
    }   // End markNodeInErrorState(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)


    @Override
    public void updateNodeMacAddress(String sNodeLctn, String nodeMacAddr, String nodeBmcMacAddr, String sWorkItemAdapterType, long lWorkItemId) throws IOException {
        String udpateMacProcedure = "TempUpdateNodeMacAddrs";
        // Update ComputeNode table.
        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, sWorkItemAdapterType, mAdapterName, udpateMacProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                ,udpateMacProcedure         // stored procedure name
                ,sNodeLctn                  // node's location string
                ,nodeMacAddr.toLowerCase()  // node's new MAC address
                ,nodeBmcMacAddr);
    }

    //--------------------------------------------------------------------------
    // Helper method to actually tell the WLM to not use this node.
    //--------------------------------------------------------------------------
    @Override
    public void tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReasonForDraining)
                throws IOException, ProcCallException, InterruptedException
    {
//        if (isComputeNodeLctn(sNodeLctn)) {
//            Map<String, String> parameters = new HashMap<>();
//            // Send a work item to the WLM adapter/provider so the WLM will stop scheduling jobs on this node.
//            parameters.put("locations", sNodeLctn);
//            // false indicates that we do NOT want to perform the check ensuring that this node actually COMPLETES a wlm drain before proceeding.
//            parameters.put("ensure_node_drained", "false");
//            parameters.put("reason_for_draining", sReasonForDraining);  // reason we want this node to be drained.
//            String sTempWork = "DontUseNode";
//            long lDunWorkItemId = workQueue().queueWorkItem("WLM"                   // type of adapter that needs to handle this work
//                                                           ,null                    // queue this work should be put on
//                                                           ,sTempWork               // work that needs to be done
//                                                           ,parameters              // parameters for this work
//                                                           ,false                   // false indicates that we do NOT want to know when this work item finishes
//                                                           ,sWorkItemAdapterType    // type of adapter that requested this work to be done
//                                                           ,lWorkItemId             // work item that the requesting adapter was working on when it requested this work be done
//                                                           );
//            mLogger.info("tellWlmToNotUseThisNode - successfully queued %s work item - Node=%s, NewWorkItemId=%d", sTempWork, sNodeLctn, lDunWorkItemId);
//        }
    }   // End tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReasonForDraining)


    //--------------------------------------------------------------------------
    // Helper method to actually tell the WLM to go ahead and to start to use this node.
    //--------------------------------------------------------------------------
    @Override
    public void tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId) throws IOException, ProcCallException, InterruptedException
    {
//        // Ensure that this is a compute node - can not tell WLM to start using a non-compute node.
//        if (isComputeNodeLctn(sNodeLctn)) {
//            // this is a compute node.
//            // Ensure that this node is owned by the WLM - cannot tell the WLM to use this node if it is not owned by the WLM subsystem.
//            if (!isThisHwOwnedByWlm(sNodeLctn)) {
//                String sNodeOwningSubsystem = getOwningSubsystem(sNodeLctn);
//                mLogger.error("tellWlmToUseThisNode - cannot tell WLM to begin using this compute node because the WLM does not own this node, OwningSubsystem='%s' - Node=%s!", sNodeOwningSubsystem, sNodeLctn);
//                // Cut a RAS event to capture this occurrence.
//                logRasEventNoEffectedJob(getRasEventType("RasWlmCantTellWlmToUseUnownedNode")
//                                        ,("OwningSubsystem=" + sNodeOwningSubsystem)
//                                        ,sNodeLctn                          // Lctn
//                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
//                                        ,sWorkItemAdapterType               // the type of adapter that ran this diagnostic
//                                        ,lWorkItemId                        // the work item id that the adapter was doing when this diagnostic ended
//                                        );
//                return;  // return without telling WLM to start using this node.
//            }
//            // Ensure that this node is not in an error state - do not want to tell the WLM to use this node if it is in an error state.
//            String sNodeState = client().callProcedure("ComputeNodeState", sNodeLctn).getResults()[0].fetchRow(0).getString(0);
//            if (sNodeState.equals("E")) {
//                mLogger.error("tellWlmToUseThisNode - cannot tell WLM to begin using this compute node (%s) because the node is in error state, so the WLM was not told that it can use this node!", sNodeLctn);
//                return;
//            }
//            // Send a work item to the WLM adapter/provider so the WLM knows that it can start scheduling jobs on this node.
//            Map<String, String> parameters = new HashMap<>();
//            parameters.put("locations", sNodeLctn);
//            String sTempWork = "UseNode";
//            long lUnWorkItemId = workQueue().queueWorkItem("WLM"                // type of adapter that needs to handle this work
//                                                          ,null                 // queue this work should be put on
//                                                          ,sTempWork            // work that needs to be done
//                                                          ,parameters           // parameters for this work
//                                                          ,false                // false indicates that we do NOT want to know when this work item finishes
//                                                          ,sWorkItemAdapterType // type of adapter that requested this work to be done
//                                                          ,lWorkItemId          // work item that the requesting adapter was working on when it requested this work be done
//                                                          );
//            mLogger.info("tellWlmToUseThisNode - successfully queued %s work item - Node=%s, NewWorkItemId=%d", sTempWork, sNodeLctn, lUnWorkItemId);
//        }
//        else {
//            mLogger.error("tellWlmToUseThisNode - cannot tell WLM to begin using this node because the specified node is NOT a compute node - Node=%s!", sNodeLctn);
//            // Cut a RAS event to capture this occurrence.
//            logRasEventNoEffectedJob(getRasEventType("RasWlmCantTellWlmToUseNonComputeNode")
//                                    ,null                               // instance data
//                                    ,sNodeLctn                          // Lctn
//                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
//                                    ,sWorkItemAdapterType               // the type of adapter that ran this diagnostic
//                                    ,lWorkItemId                        // the work item id that the adapter was doing when this diagnostic ended
//                                    );
//        }
    }   // End tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)


    //-----------------------------------------------------------------
    // Connect to the VoltDB database servers/nodes
    // Parms:
    //      sListOfVoltDbServers - is a string containing a comma separated list of VoltDB servers
    //-----------------------------------------------------------------
    @Override
    public void connectToDataStore(String sListOfVoltDbServers) throws IOException {
        // Instantiate a client.
        ClientConfig config = null;
        config = new ClientConfig("", "", mVoltDbStatusListener);   // username and password is specified here if security is turned on, 3rd parameter is the Status Listener routine for any error conditions outside of normal procedure execution!
        config.setReconnectOnConnectionLoss(true);                  // tells the client library to attempt to reestablish any lost connections to VoltDB servers (retries every 1 - 8 seconds).
        mClient = ClientFactory.createClient(config);

        // Create the list of specified VoltDB servers using the arguments specified to this program
        // (expecting a comma-separated list of servers - if one is not specified use localhost instead).
        String serverlist = sListOfVoltDbServers;
        String[] servers = serverlist.split(",");

        // Connect to all the VoltDB servers
        for (String server: servers) {
            try {
                client().createConnection(server, Client.VOLTDB_SERVER_PORT);
            }
            catch (Exception uhe) {
                mLogger.exception(uhe, "connectToDataStore - unable to connect to the database server on %s!",
                        server);
                throw uhe;
            }
        }

        try {
            // TODO: this (and all DB connections) should be moved to the DS API.  At the moment, a connection to the
            // Nearline database is needed here to provide Adapter API functions like: logEnvDataAggregated.
            mNearlineConn = DbConnectionFactory.createDefaultConnection();
        } catch (Exception ex) {
            mLogger.exception(ex, "connectToDataStore - unable to connect to the nearline tier DB. Error: %s!",
                    ex.getMessage());
        }
    }   // End connectToDataStore(String sListOfVoltDbServers)


    //-----------------------------------------------------------------
    // Handles processing necessary in an Adapter's mainline catch block.
    //-----------------------------------------------------------------
    @Override
    public void handleMainlineAdapterException(Exception e) {
        mLogger.exception(e, "Exception occurred - %s!", e.getMessage());
        try {
            logRasEventSyncNoEffectedJob(getRasEventType("RasGenAdapterException")
                                        ,("Exception=" + e)                 // instance data
                                        ,null                               // Lctn
                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                        ,mAdapterType                       // type of adapter that is requesting this
                                        ,workQueue().baseWorkItemId()       // requesting work item
                                        );
            mAdapterShutdownStarted = true;  // Set flag indicating that we are going through adapter shutdown.
            abend("exception");
        } catch (Exception e2) {
            mLogger.exception(e2);
        }
    }   // End handleMainlineAdapterException(Exception e)


    //-----------------------------------------------------------------
    // Handles cleanup necessary in an Adapter's main() method, just before main() terminates.
    //-----------------------------------------------------------------
    @Override
    public void handleMainlineAdapterCleanup(boolean bAbnormalShutdown) throws IOException,
            InterruptedException, AdapterException {
        mAdapterShutdownStarted = true;  // set flag indicating that we are going through adapter shutdown.
        if (bAbnormalShutdown) {
            abend("mainline");
        } else {
            adapterTerminating("Adapter terminating normally");
            mLogger.info("Finished normally");
        }
    }   // End handleMainlineAdapterCleanup(boolean bAbnormalShutdown)


    //-----------------------------------------------------------------
    // Log aggregated environmental telemetry into the Tier2 data store.
    // Parms:
    //      String  sTypeOfEnvData  - tells us what this tuple of environmental data is
    //                                  e.g., Temp, AirFlow, RPM, CoolantFlow, VoltageIn, VoltageOut, CurrentIn, CurrentOut, Power, etc.
    //      String  sLctn           - location string of the entity that this environmental data is for,
    //                                  e.g., R51-CH00-CN9, R48-SW0, R48-OSW3, R50-CH00-CN3-FAN5, R50-CH00-CN3-CPU1, R0-UPS1, SN0-PS6
    //      long    lTsInMicroSecs  - time environmental data occurred in micro-seconds since epoch (in UTC)
    //      double  dMaxValue       - maximum value from all of the samples that occurred within this interval
    //      double  dMinValue       - minimum value from all of the samples that occurred within this interval
    //      double  dAvgValue       - average value from all of the samples that occurred within this interval
    //      String  sReqAdapterType - type of the adapter that invoked this method
    //      long    lReqWorkItemId  - work item that the invoking adapter was working on when this method was invoked
    //-----------------------------------------------------------------
    @Override
    public long logEnvDataAggregated(String sTypeOfData, String sLctn, long lTsInMicroSecs, double dMaxValue,
                                     double dMinValue, double dAvgValue, String sReqAdapterType, long lReqWorkItemId)
                    throws IOException, SQLException, TimeoutException, ConfigIOParseException, ClassNotFoundException
    {
        String sTempStoredProcedure = "AggregatedEnvDataStore";
        String sPertinentInfo = "Lctn=" + sLctn + ",TypeOfData=" + sTypeOfData + ",lTsInMicroSecs=" + lTsInMicroSecs;

        if (mAggEnvDataStmt == null) {
            mAggEnvDataStmt = mNearlineConn.prepareCall("{call " + sTempStoredProcedure + "(?,?,?,?,?,?,?,?)}");
        }

        try {
            mAggEnvDataStmt.setString(1, sLctn);
            mAggEnvDataStmt.setTimestamp(2, new Timestamp(lTsInMicroSecs / 1000));
            mAggEnvDataStmt.setString(3, sTypeOfData);
            mAggEnvDataStmt.setDouble(4, dMaxValue);
            mAggEnvDataStmt.setDouble(5, dMinValue);
            mAggEnvDataStmt.setDouble(6, dAvgValue);
            mAggEnvDataStmt.setString(7, sReqAdapterType);
            mAggEnvDataStmt.setLong(8, lReqWorkItemId);
            mAggEnvDataStmt.execute();
            mNearlineConn.commit();
        } catch(SQLException ex) {
            mNearlineConn.rollback(); // Cancel current transaction.
            mLogger.exception(ex, "An error occurred while executing stored procedure: %s", sTempStoredProcedure);
            throw ex; // Rethrow...
        }

        mLogger.debug("called stored procedure %s - Lctn=%s, TypeOfData=%s, PertinentInfo='%s'", sTempStoredProcedure, sLctn, sTypeOfData, sPertinentInfo);

        return 0L;
    }   // End logEnvDataAggregated(String sTypeOfData, String sLctn, long lTsInMicroSecs, double dMaxValue, double dMinValue, double dAvgValue, String sReqAdapterType, long lReqWorkItemId)


    //--------------------------------------------------------------------------
    // Get a map that has the mapping of compute node HostNames to their Lctns.
    //--------------------------------------------------------------------------
    Map<String, String> mapOfCompNodesHostnameToLctn() throws IOException, ProcCallException {
        Map<String, String> mapHostNameToLctn = new HashMap<String, String>();  // Map that takes a Compute Node's hostname and gives you the corresponding lctn.

        // Get list of HostNames and their Lctns.
        ClientResponse response = client().callProcedure("ComputeNodeListLctnAndHostname");
        // Loop through each of the compute nodes that were returned.
        VoltTable vt = response.getResults()[0];
        for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
            vt.advanceRow();
            mapHostNameToLctn.put(vt.getString("HostName"), vt.getString("Lctn"));
        }

        return mapHostNameToLctn;
    }   // End mapOfCompNodesHostnameToLctn()


    //--------------------------------------------------------------------------
    // Diagnostics - log that a diagnostic instance has started
    //  Parms:
    //      String  sLctn               - hardware location string of the hardware that this diagnostic is running on, a compute node, a whole rack, a cdu, a switch, etc.
    //      long    lServiceOperationId    - the Service Operation ID that requested this diagnostic be run (-99999 indicates that this diagnostic was submitted outside of a Service Operation)
    //      String  sDiag               - identifies which diagnostic(s) is being run, e.g., the CPU bucket, Memory bucket, Compute blade bucket, Power bucket, etc.
    //      String  sReqAdapterType     - type of the adapter that is running the diagnostic
    //      long    lReqWorkItemId      - work item that the invoking adapter is working on when this method was invoked
    //  Returns:
    //      long    lUniqueDiagId       - this is the unique diagnostic id that has been assigned to this diagnostic instance.
    //--------------------------------------------------------------------------
    @Override
    public long diagStarted(String sLctn, long lServiceOperationId, String sDiag, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, ProcCallException, InterruptedException {
        // Get a new unique diagnostic id to use for this instance.
        long lUniqueDiagId = client().callProcedure("GetUniqueId", "DiagId").getResults()[0].asScalarLong();
        // Log that this diagnostic has started in the data store.
        // If nodes provided string is longer than schema length truncate it for now.
        // [ToDo] implement heirarchy and remove this
        int iMaxLctnSize = 20000;
        String sTruncatedNodeLctns = sLctn;
        if ( sLctn.length() > iMaxLctnSize ) {
            sTruncatedNodeLctns = sLctn.substring(0,(iMaxLctnSize - 3) ) + "...";
        }
        String sTempStoredProcedure = "DiagStarted";
        String sPertinentInfo = "DiagId=" + lUniqueDiagId + ",Lctn=" + sTruncatedNodeLctns + ",Diag=" + sDiag;
        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sPertinentInfo, lReqWorkItemId) // asynchronously invoke the procedure
                              ,sTempStoredProcedure  // stored procedure name
                              ,lUniqueDiagId         // this diagnostic instance's id
                              ,sTruncatedNodeLctns   // location string of the hardware this diagnostic is running on
                              ,lServiceOperationId   // service operation id that requested this diag be run
                              ,sDiag                 // the diagnostic being run
                              ,sReqAdapterType       // the type of adapter that is running this diagnostic
                              ,lReqWorkItemId        // the work item id that the adapter is doing when it started this diagnostic
                              );
        mLogger.info("called stored procedure %s - DiagId=%d, Lctn=%s, Diag=%s, ServiceOperationId=%d", sTempStoredProcedure, lUniqueDiagId, sTruncatedNodeLctns, sDiag, lServiceOperationId);

        // Set the node's Owner to Service - simply recording state change info, no need to wait for ack that this work has completed
        ArrayList<String> alNodeLctns = new ArrayList<String>(Arrays.asList(sLctn.split(",")));
        for ( String sCurNodeLctn : alNodeLctns ) {
            String sTempOwner = "S";  // Owner is Service (S).
            sTempStoredProcedure = "ComputeNodeSetOwner";
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sLctn, lReqWorkItemId) // asynchronously invoke the procedure
                ,sTempStoredProcedure               // stored procedure name
                ,sCurNodeLctn                       // node's location string
                ,sTempOwner                         // node's new owner - Service (S)
                ,System.currentTimeMillis()*1000L   // time that this occurred in microseconds since epoch
                ,sReqAdapterType                    // type of the adapter that is requesting/issuing this stored procedure
                ,lReqWorkItemId                     // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
            );
            mLogger.info("called stored procedure %s, Lctn=%s, NewOwner=%s", sTempStoredProcedure, sCurNodeLctn, sTempOwner);
        }
        // Return the new diagnostic id to the caller.
        return lUniqueDiagId;
    }   // End diagStarted(String sLctn, long lServiceOperationId, String sDiag, String sReqAdapterType, long lReqWorkItemId)


    //--------------------------------------------------------------------------
    // Diagnostics - log that a diagnostic instance has ended
    //  Parms:
    //      long    lDiagId         - id that uniquely identifies this specific instance of a diagnostic run
    //      String  sLctn           - hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, a switch, etc.
    //      String  sDiag           - identifies which diagnostic(s) was run, e.g., the CPU bucket, Memory bucket, Compute blade bucket, Power bucket, etc.
    //      String  sResults        - result string that the diagnostic produced
    //      String  sReqAdapterType - type of the adapter that is running the diagnostic
    //      long    lReqWorkItemId  - work item that the invoking adapter was working on when this method was invoked
    //--------------------------------------------------------------------------
    @Override
    public long diagEnded(long lDiagId, String sLctn, String sDiag, String sResults, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, ProcCallException, InterruptedException {
        String sCheckForFailure = "fail";
        String sCheckForCompletion = "return code";
        // If nodes provided string is longer than schema length truncate it for now.[ToDo] implement heirarchy and remove this
        int iMaxLctnSize = 20000;
        String sTruncatedNodeLctns = sLctn;
        if ( sLctn.length() > iMaxLctnSize ) {
            sTruncatedNodeLctns = sLctn.substring(0,(iMaxLctnSize - 3) ) + "...";
        }
        // if result string exceeds table size truncate [ToDo] find a scalable mechanism for this for TC4
        int iMaxResultSize = 200000;
        String sTruncateError = " Result too long and Truncated ";
        String sTruncResults = sResults;
        if ( sTruncResults.length() > iMaxResultSize ) {
            sTruncResults = sTruncResults.substring(0,(iMaxResultSize - sTruncateError.length()) ) + sTruncateError;
        }
        // Check & see if the diagnostic was successful or not.
        String sState = null;
        if (sResults.toLowerCase().contains(sCheckForCompletion)) {
            if (sResults.toLowerCase().contains(sCheckForFailure)) {
                // diagnostic completed with failure
                sState = "F";    // F = failure occured, diagnostics completed running
             }
             else {
                // diagnostic completed, and all tests passed
                sState = "P";    // P = diagnostic completed with all tests passing
             }
        }
        else {
             // diagnostic did not start/complete running
             sState = "E";  // E = error occurred, diag dir not run/complete
        }
        if ( sState.equals("E") || sState.equals("F") ) {
            // Also log a ras event to document this failure.
            logRasEventNoEffectedJob(getRasEventType("RasDiagFailed")
                                    ,("DiagId=" + lDiagId + ", Diag=" + sDiag + ", DiagResults=" + sTruncResults + ", AdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId)
                                    ,sTruncatedNodeLctns                // Lctn
                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,sReqAdapterType                    // the type of adapter that ran this diagnostic
                                    ,lReqWorkItemId                     // the work item id that the adapter was doing when this diagnostic ended
                                    );
        }
        // Log that this diagnostic has completed in the data store.
        String sTempStoredProcedure = "DiagTerminated";
        String sPertinentInfo = "DiagId=" + lDiagId + ",Lctn=" + sTruncatedNodeLctns + ",Diag=" + sDiag + ",State=" + sState;
        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sPertinentInfo, lReqWorkItemId) // asynchronously invoke the procedure
                             ,sTempStoredProcedure  // stored procedure name
                             ,lDiagId               // this diagnostic instance's id
                             ,sState                // the ending state of this diagnostic - E = error occurred/diag failed,  F = diagnostic ended without failure
                             ,sResults              // result string that this diagnostic produced
                             ,sReqAdapterType       // the type of adapter that ran this diagnostic
                             ,lReqWorkItemId        // the work item id that the adapter was doing when this diagnostic ended
                             );
        mLogger.info("diagEnded - called stored procedure %s - DiagId=%d, Lctn=%s, Diag=%s, State=%s", sTempStoredProcedure, lDiagId, sTruncatedNodeLctns, sDiag, sState);

        // Set the node's Owner back to Working as diagnostic run has completed
        ArrayList<String> alNodeLctns = new ArrayList<String>(Arrays.asList(sLctn.split(",")));
        for ( String sCurNodeLctn : alNodeLctns ) {
            String sTempOwner = "W";  // Owner is WLM (W).
            sTempStoredProcedure = "ComputeNodeSetOwner";
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sLctn, lReqWorkItemId) // asynchronously invoke the procedure
                ,sTempStoredProcedure               // stored procedure name
                ,sCurNodeLctn                       // node's location string
                ,sTempOwner                         // node's new owner - (W)
                ,System.currentTimeMillis()*1000L   // time that this occurred in microseconds since epoch
                ,sReqAdapterType                    // type of the adapter that is requesting/issuing this stored procedure
                ,lReqWorkItemId                     // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
            );
            mLogger.info("called stored procedure %s, Lctn=%s, NewOwner=%s", sTempStoredProcedure, sCurNodeLctn, sTempOwner);
        }
        return 0L;
    }   // End diagEnded(long lDiagId, String sLctn, String sDiag, String sResults, String sReqAdapterType, long lReqWorkItemId)


    //--------------------------------------------------------------------------
    // Get a copy of the stack trace into a string.
    //--------------------------------------------------------------------------
    public static String stackTraceToString(final Throwable throwable) {
         final StringWriter oSW = new StringWriter();
         final PrintWriter oPW  = new PrintWriter(oSW, true);
         throwable.printStackTrace(oPW);
         return oSW.getBuffer().toString();
    }   // End stackTraceToString(final Throwable throwable)


    // Get the specified node's inventory info out of the database.
    public final String getNodesInvInfoFromDb(String sTempLctn) throws IOException, ProcCallException {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sTempLctn)) {
            sTempStoredProcedure = "ComputeNodeInventoryInfo";
        }
        else if (isServiceNodeLctn(sTempLctn)) {
            sTempStoredProcedure = "ServiceNodeInventoryInfo";
        }
        else {
            // unexpected lctn string.
            String sErrorMsg = "Specified lctn is neither a compute node nor a service node - Lctn=" + sTempLctn + "!";
            mLogger.error(sErrorMsg);
            throw new RuntimeException(sErrorMsg);
        }

        // Get the lctn's existing inventory info out of the DB.
        String sDbInvInfo = null;
        ClientResponse responseGetInventoryInfo = client().callProcedure(sTempStoredProcedure, sTempLctn);
        if (responseGetInventoryInfo.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            mLogger.error("stored procedure %s failed - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                          sTempStoredProcedure, IAdapter.statusByteAsString(responseGetInventoryInfo.getStatus()), responseGetInventoryInfo.getStatusString(), adapterType(), adapterId());
            throw new RuntimeException(responseGetInventoryInfo.getStatusString());
        }
        VoltTable vtGetInventoryInfo = responseGetInventoryInfo.getResults()[0];

        // Check & see if we got inventory info for this node.
        if (vtGetInventoryInfo.getRowCount() == 0) {
            // no inventory information was returned.
            mLogger.warn("There is no inventory information for lctn %s", sTempLctn);
            return null;
        }
        // Grab the inventory info and return it to the caller.
        vtGetInventoryInfo.advanceRow();
        sDbInvInfo = vtGetInventoryInfo.getString("InventoryInfo");
        return sDbInvInfo;
    }   // End getNodesInvInfoFromDb(String sTempLctn)


    //--------------------------------------------------------------------------
    // Obtains the list of the fully qualified WLM nodes from the specified string representing the list of nodes (this will expand any wildcards, etc.).
    //--------------------------------------------------------------------------
    @Override
    public ArrayList<String> extractListOfWlmNodes(String sNodeList, String sJobId, long lWorkItemId) throws InterruptedException, IOException, ProcCallException {
        ArrayList<String> alNodes = new ArrayList<String>();

        // Run slurm command to expand the node list to a full list, rather than the possibly compressed version.
        String sTempCmd = ("/usr/bin/scontrol show hostname " + sNodeList);
        Process process = Runtime.getRuntime().exec(sTempCmd);

        // Get the command's stdout (i.e. its InputStream).
        String sLine = "";
        try (BufferedReader brStdOut = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8))) {
            while ((sLine = brStdOut.readLine()) != null) {
                ///mLogger.warn("%s - extractListOfWlmNodes - JobId=%s - %s - stdout - '%s'", adapterName(), sJobId, sNodeList, sLine);
                // Add this node to the list of nodes.
                alNodes.add(sLine);
            }
        }

        // Get the command's stderr (i.e. its ErrorStream).
        boolean bErrorOccurredDuringExpansionOfNodeList = false;
        try (BufferedReader brStdErr = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                StandardCharsets.UTF_8))) {
            while ((sLine = brStdErr.readLine()) != null) {
                mLogger.error("extractListOfWlmNodes - JobId=%s - %s - stderr - '%s'", sJobId, sNodeList, sLine);
                bErrorOccurredDuringExpansionOfNodeList = true;
            }
        }

        // Wait for the command to finish.
        process.waitFor();

        // Log a RAS event if an error occurred during expansion to full node list.
        if (bErrorOccurredDuringExpansionOfNodeList) {
            // Cut RAS event indicating that the job has been killed - we do know which job was effected by this occurrence.
            logRasEventWithEffectedJob(getRasEventType("RasWlmFailureExtractingListOfNodes")
                                      ,("JobId=" + sJobId + ", AdapterName=" + adapterName())     // Instance data
                                      ,null                                                       // Lctn
                                      ,sJobId                                                     // JobId of a job that was effected by this ras event
                                      ,System.currentTimeMillis() * 1000L                         // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                      ,adapterType()                                              // type of the adapter that is requesting/issuing this stored procedure
                                      ,lWorkItemId                                                // requesting work item id
                                      );
            return alNodes;
        }

        return alNodes;
    }   // End extractListOfWlmNodes(String sNodeList, String sJobId, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Obtains the list of jobs that are currently running on the specified node
    // (can also be used to see if there are any jobs on the specified node).
    //--------------------------------------------------------------------------
    @Override
    public ArrayList<String> listOfJobsOnNode(String sNodeLctn) throws IOException, ProcCallException {
        //------------------------------------------------------------------
        // Check and see if the specified lctn is one that could have a job associated with it.
        //------------------------------------------------------------------
        ArrayList<String> alJobs = new ArrayList<String>();
        // Check & see if this is a fully qualified ComputeNode location.
        if (isComputeNodeLctn(sNodeLctn)) {
            // this is a fully qualified ComputeNode location - it is capable of having a job associated with it.
            // Get the JobIds (if any) that are running on the specified ComputeNode lctn.
            ClientResponse response = client().callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp", sNodeLctn, (System.currentTimeMillis() * 1000L), null);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                mLogger.error("listOfJobsOnNode - InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                        IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapterType(), adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            // Loop through each of the jobs that were returned.
            VoltTable vtInternalCachedJobsList = response.getResults()[0];
            for (int iJobCntr = 0; iJobCntr < vtInternalCachedJobsList.getRowCount(); ++iJobCntr) {
                vtInternalCachedJobsList.advanceRow();
                alJobs.add(vtInternalCachedJobsList.getString("JobId"));
            }
        }   // this is a fully qualified ComputeNode location - it is capable of having a job associated with it.
        return alJobs;
    }   // End listOfJobsOnNode(String sNodeLctn)


    //--------------------------------------------------------------
    // Get the specified adapter instance's adapter id.
    //--------------------------------------------------------------
    public long getAdapterInstancesAdapterId(String sAdapterType, String sAdapterLctn, long lAdapterPid) throws IOException, ProcCallException {
        ClientResponse response = client().callProcedure("AdapterInfoUsingTypeLctnPid", sAdapterType, sAdapterLctn, lAdapterPid);
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            mLogger.fatal("%s - getAdapterInstancesAdapterId - Stored procedure AdapterInfoUsingTypeLctnPid FAILED - AdapterType=%s, AdapterLctn=%s, AdapterPid=%d, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                          adapterName(), sAdapterType, sAdapterLctn, lAdapterPid,
                          IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapterType(), adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        // Ensure that the adapter entry still exists in the table (it may have already been cleaned up during the shutdown of the adapter instance itself).
        VoltTable vt = response.getResults()[0];
        if (vt.getRowCount() > 0) {
            // the adapter entry still exists in the table.
            vt.advanceRow();
            return vt.getLong("Id");
        }
        return -1;  // could not find the adapter entry for the specified adapter instance.
    }   // End getAdapterInstancesAdapterId(String sAdapterType, String sAdapterLctn, long lAdapterPid)


    //--------------------------------------------------------------
    // Get the specified adapter instance's base work item id.
    //--------------------------------------------------------------
    public long getAdapterInstancesBaseWorkItemId(String sAdapterType, long lAdapterId) throws IOException, ProcCallException {
        ClientResponse response = client().callProcedure("WorkItemGetAdaptersWorkItemId", sAdapterType, lAdapterId, "BaseWork");
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            mLogger.fatal("getAdapterInstancesBaseWorkItemId - stored procedure WorkItemGetAdaptersWorkItemId FAILED - ReqAdapterType=%s, ReqAdapterId=%d, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                          sAdapterType, lAdapterId,
                          IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapterType(), adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        // Ensure that the specified adapter's base work item entry still exists in the table (it may have already been cleaned up during the shutdown of the adapter instance itself).
        VoltTable vt = response.getResults()[0];
        if (vt.getRowCount() > 0) {
            // the adapter entry still exists in the table.
            vt.advanceRow();
            return vt.getLong("Id");
        }
        return -1;  // could not find the base work item for the specified adapter instance.
    }   // End getAdapterInstancesBaseWorkItemId(String sAdapterType, long lAdapterId)


    // Helper method that strips the new line character off the end of this message (don't want to do this until after we have written it to the debug file).
    public final static String stripNewlineOffEndOfMsg(String sMsg) {
        int i = sMsg.length() - 1;
        if (sMsg.charAt(i) == '\n')
            return sMsg.substring(0, i);
        return sMsg;
    }   // End stripNewlineOffEndOfMsg(String sMsg)


    //--------------------------------------------------------------------------
    // Check & see if the specified piece of hardware is in the process of being serviced (is owned by the Service subsystem).
    //--------------------------------------------------------------------------
    @Override
    public boolean isThisHwBeingServiced(String sLctn) {
        String sOwningSubsystem = getOwningSubsystem(sLctn);
        if (sOwningSubsystem.equalsIgnoreCase("S"))
            return true;
        // If we get an unknown owning subsystem then to be safe we must assume it is being serviced (safety issue).
        if (sOwningSubsystem.equalsIgnoreCase("UNKNOWN"))
            return true;
        return false;
    }
    @Override
    public boolean isThisHwOwnedByService(String sLctn) {
        return isThisHwBeingServiced(sLctn);
    }

    //--------------------------------------------------------------------------
    // Check & see if the specified piece of hardware is owned by WLM subsystem.
    //--------------------------------------------------------------------------
    @Override
    public boolean isThisHwOwnedByWlm(String sLctn) {
        if (getOwningSubsystem(sLctn).equalsIgnoreCase("W"))
            return true;
        return false;
    }

    //--------------------------------------------------------------------------
    // Find out which subsystem owns this piece of hardware.
    //--------------------------------------------------------------------------
    @Override
    public String getOwningSubsystem(String sLctn) {
        // Ensure that a hardware lctn was specified.
        if ((sLctn != null) && (!sLctn.isEmpty())) {
            // there is a specified hardware lctn.
            try {
                // Determine the correct stored procedure to use.
                String sTempStoredProcedure = null;
                if (isComputeNodeLctn(sLctn)) {
                    sTempStoredProcedure = "ComputeNodeOwner";
                }
                else if (isServiceNodeLctn(sLctn)) {
                    sTempStoredProcedure = "ServiceNodeOwner";
                }
                else {
                    mLogger.error("getOwningSubsystem - unexpected type of hardware location - lctn=%s", sLctn);
                    return "UNKNOWN";  // since we don't know how to check which subsystem owns this kind of hardware, return a value of UNKNOWN.
                }
                // Find out which subsystem owns this hardware.
                String sOwningSubsystem = client().callProcedure(sTempStoredProcedure, sLctn).getResults()[0].fetchRow(0).getString("Owner");
                return sOwningSubsystem;
            }
            catch (Exception e) {
                mLogger.error("getOwningSubsystem - exception occurred - lctn=%s!", sLctn);
                mLogger.error("getOwningSubsystem - %s", Adapter.stackTraceToString(e));
                // Cut a RAS event to capture this occurrence.
                logRasEventCheckForEffectedJob(getRasEventType("RasGenAdapterExceptionButContinue")
                                              ,("Method=getOwningSubsystem, Exception=" + e.getMessage())
                                              ,sLctn                              // Lctn
                                              ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                              ,mAdapterType                       // the type of adapter that ran this diagnostic
                                              ,workQueue().baseWorkItemId()       // the work item id that the adapter was doing when this occurred
                                              );
                return "UNKNOWN";  // since an exception occurred we can't tell you which subsystem owns it.
            }
        }   // there is a specified hardware lctn.

        return "UNKNOWN";  // since there is no hardware lctn then we can't tell you which subsystem owns it.
    }   // End getOwningSubsystem(String sLctn)


}   // End class Adapter
