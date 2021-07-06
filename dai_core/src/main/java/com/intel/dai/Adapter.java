// Copyright (C) 2017-2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.*;
import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.InventoryKeys;
import com.intel.config_io.*;
import com.intel.properties.*;
import com.intel.dai.transforms.DefaultLocations;
import org.voltdb.client.*;
import org.voltdb.client.Client;
import org.voltdb.VoltTable;

import java.io.*;
import java.lang.*;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.text.DecimalFormat;
import java.util.concurrent.TimeoutException;
import java.net.UnknownHostException;
import com.intel.xdg.XdgConfigFile;
import sun.misc.Signal;
import sun.misc.SignalHandler;

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
    public static String XDG_COMPONENT = "ucs";               // For the XDG configuration file locator library.
    static String RASMETADATA_FILE = "RasEventMetaData.json"; // File for RAS metadata.
    public final static String DataMoverQueueName    = "DAI-DataMover-Queue";
    public final static String DataMoverExchangeName = "DAI-DataMover-Exchange";
    public final String KeyForLctn = "Lctn=";
    public final String KeyForTimeInMicroSecs = "TimeInMicroSecs=";
    public final String KeyForUsingSynthData = "UsingSynthData";

    //--------------------------------------------------------------------------
    // Data Members
    //--------------------------------------------------------------------------
    private boolean                 mAdapterAbnormalShutdown              = false;
    private boolean                 mAdapterShutdownStarted               = false;
    private long                    mAdapterId                            = -99999L;
    private String                  mAdapterType                          = null;
    private String                  mAdapterName                          = null;
    private long                    mSconRank                             = -99999L;
    private ClientResponse          mResponse                             = null;
    private Client                  mClient                               = null;
    private Connection              mNearlineConn                         = null;
    private PreparedStatement       mAggEnvDataStmt                       = null;
    private Map<String, Long>       mCompNodeLctnToSeqNumMap              = null;       // Map that takes a Compute Node's Lctn and gives you the corresponding SequenceNumber.
    private Logger                  mLogger;
    private Map<String, MetaData>   mRasDescrNameToMetaDataMap            = null;       // Map that takes a RAS DescriptiveName and gives you its corresponding MetaData.
    private Map<Integer, String>    mDataMoverResultTblIndxToTableNameMap = null;       // Map that takes a DataMover result's table index and gives you the corresponding table name.
    SignalHandler                   mSignalHandler                        = null;
    ClientStatusListenerExt         mVoltDbStatusListener                 = null;
    private Map<String, String>     mCompNodeLctnToHostnameMap            = null;       // Map that takes a Compute Node's lctn and gives you the corresponding hostname.
    private Map<String, String>     mServNodeLctnToHostnameMap            = null;       // Map that takes a Service Node's lctn and gives you the corresponding hostname.
    private Map<String, String>     mCompNodeHostnameToLctnMap            = null;       // Map that takes a Compute Node's hostname and gives you the corresponding lctn.
    private Map<String, String>     mServNodeHostnameToLctnMap            = null;       // Map that takes a Service Node's hostname and gives you the corresponding lctn.
    private Map<String, String[]>   mNodeLctnToIpAddrAndBmcIpAddr         = null;
    private Map<String, String>     mBmcIpAddrToNodeLctn                  = null;
    private String                  mSnLctn                               = null;       // The lctn string of the service node that this adapter instance is running on.
    private long                    mPid                                  = -99999L;    // The pid (process id) for the process this adapter instance is running in.
    private Map<String, String>     mNodeLctnToAggregatorMap              = null;       // Map that takes a Service or Compute node's lctn and gives you its owning aggregator.
    private WorkQueue               mWorkQueue                            = null;
    private DataStoreFactory        mFactory                              = null;
    private RasEventLog             mRasEventLog                          = null;
    private AdapterShutdownHandler  mShutdownHandler                      = null;
    private ConfigIO                mJsonParser                           = null;       // Json parser.
    private long                    mNumLevelsInComputeNodeLctn           = -99999;     // Number of levels that make up compute node lctn strings in this machine (e.g., R2-CH03-N2 = 3 levels)
    private long                    mNumLevelsInServiceNodeLctn           = -99999;     // Number of levels that make up service node lctn strings in this machine (e.g., SN1-SSN3 = 2 levels)
    private Map<String, String>     mNodelctnAndSlotToLctnMap             = null;       // Map that takes a Service or Compute NodeLctn + Slot (e.g., "R0-CH00-CN0+3") and gives you its fully qualified FruLctn (e.g., R0-CH00-CN0-A3)
    private Map<String, String>     mNodelctnAndBusaddrToLctnMap          = null;       // Map that takes a Service or Compute NodeLctn + PCIE BusAddr (e.g., "R0-CH00-CN0+0000:44:03.0") and gives you its fully qualified FruLctn (e.g., R0-CH00-CN0-A3)
    private Map<String, String>     mNodeLctnAndSocketdesignationToLctnMap= null;       // Map that takes a Service or Compute node's lctn AND a Processor SocketDesignation (R0-CH00-CN0 and CPU1) and gives you its fully qualified Processor lctn (e.g., R0-CH00-CN0-P1)
    private Map<String, String>     mNodeLctnAndModulelocatorToLctnMap    = null;       // Map that takes a Service or Compute node's lctn AND a Dimm ModuleLocator (R0-CH00-CN0 and CPU2_DIMM_A2) and gives you its fully qualified Dimm lctn (e.g., R0-CH00-CN0-D3)


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
    final public ConfigIO        jsonParser()                            { return mJsonParser; }
    public long                  pid()                                   { return mPid; }
    public String                snLctn()                                { return mSnLctn; }
    public WorkQueue             workQueue()                             { return mWorkQueue; }
    public void                  workQueue(WorkQueue workQueue)          { mWorkQueue = workQueue; }

    @Override
    public void                  setShutdownHandler(AdapterShutdownHandler handler)  { mShutdownHandler = handler; }
    @Override
    public Map<String, MetaData> cachedRasDescrNameToMetaDataMap()       { return mRasDescrNameToMetaDataMap; }

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
        mFactory = new DataStoreFactoryImpl(servers, mLogger);
        try {
            mWorkQueue = mFactory.createWorkQueue(client(), this);
            mRasEventLog = mFactory.createRasEventLog(this);
        } catch(DataStoreException e) {
            mLogger.exception(e);
        }

        // Save away the number of levels that make up compute node lctn strings in this machine (e.g., R2-CH03-N2 = 3 levels)
        VoltTable vtTemp = null;
        try {
            vtTemp = client().callProcedure("UCSCONFIGVALUE.select", "UcsLctnCompNodeNumLevels").getResults()[0];
            if (vtTemp.getRowCount() == 1) {
                vtTemp.advanceRow();
                mNumLevelsInComputeNodeLctn = Long.parseLong(vtTemp.getString("Value"));
            }
            else
                mNumLevelsInComputeNodeLctn = 3;  // default if no entry exists in UcsConfigValue table.
        }
        catch (Exception e)  {
            mLogger.error("Adapter - exception occurred - can't get UCSCONFIGVALUE.select Key=UcsLctnCompNodeNumLevels!");
            mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            mNumLevelsInComputeNodeLctn = 3;  // default if no entry exists in UcsConfigValue table.
        }
        // Save away the number of levels that make up service node lctn strings in this machine (e.g., SN1-SSN3 = 2 levels)
        try {
            vtTemp = client().callProcedure("UCSCONFIGVALUE.select", "UcsLctnServiceNodeNumLevels").getResults()[0];
            if (vtTemp.getRowCount() == 1) {
                vtTemp.advanceRow();
                mNumLevelsInServiceNodeLctn = Long.parseLong(vtTemp.getString("Value"));
            }
            else
                mNumLevelsInServiceNodeLctn = 2;  // default if no entry exists in UcsConfigValue table.
        }
        catch (Exception e)  {
            mLogger.error("Adapter - exception occurred - can't get UCSCONFIGVALUE.select Key=UcsLctnServiceNodeNumLevels!");
            mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            mNumLevelsInServiceNodeLctn = 2;  // default if no entry exists in UcsConfigValue table.
        }

        // Adapter should be ready for normal operation, now :)

        return mWorkQueue;
    }   // End setUpAdapter(String servers, String sSnLctn)


    @Override
    public boolean isComputeNodeLctn(String sLctn)
    {
        if (mapCompNodeLctnToSeqNum().get(sLctn) == null)
            return false;
        return true;
    }

    @Override
    public boolean isServiceNodeLctn(String sLctn)
    {
        if (mapServNodeLctnToHostName().get(sLctn) == null)
            return false;
        return true;
    }

    @Override
    public String getNodeNameFromLctn(String sNodeLctn) {
        String sCurNodeName = mapCompNodeLctnToHostName().get(sNodeLctn);
        if (sCurNodeName == null)
            sCurNodeName = mapServNodeLctnToHostName().get(sNodeLctn);
        return sCurNodeName;
    }   // End getNodeNameFromLctn(String sNodeLctn)

    // Get the map of ComputeNode Lctns to SequenceNumbers - under the covers it will create and populate the map the first time it is accessed.
    // Note: this mapping should be invariant once the machine is defined, so there should be no need to periodically update it during the life of this Adapter.
    @Override
    public Map<String, Long> mapCompNodeLctnToSeqNum() {
        if (mCompNodeLctnToSeqNumMap == null) {
            // create and populate the map of Lctns and their SequenceNumbers.
            Map<String, Long> tempMapLctnToSeqNum = new HashMap<String, Long>();
            // Get list of Lctns and their SequenceNumbers.
            try {
                ClientResponse response = client().callProcedure("ComputeNodeListLctnAndSeqNum");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    tempMapLctnToSeqNum.put(vt.getString("Lctn"), vt.getLong("SequenceNumber"));
                }
                mCompNodeLctnToSeqNumMap = tempMapLctnToSeqNum;
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - ComputeNodeListLctnAndSeqNum stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            }
        }
        return mCompNodeLctnToSeqNumMap;
    }   // End mapCompNodeLctnToSeqNum()

    // Get the map of ComputeNode Lctns to Hostnames - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapCompNodeLctnToHostName() {
        if (mCompNodeLctnToHostnameMap == null) {
            // create and populate the map of Lctns and their Hostnames.
            Map<String, String> tempMapLctnToHostname = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            try {
                ClientResponse response = client().callProcedure("ComputeNodeListLctnAndHostname");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    tempMapLctnToHostname.put(vt.getString("Lctn"), vt.getString("HostName"));
                }
                mCompNodeLctnToHostnameMap = tempMapLctnToHostname;
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - ComputeNodeListLctnAndHostname stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            }
        }
        return mCompNodeLctnToHostnameMap;
    }   // End mapCompNodeLctnToHostName()

    // Get the map of ComputeNode Hostnames to Lctns - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapCompNodeHostNameToLctn() {
        if (mCompNodeHostnameToLctnMap == null) {
            // create and populate the map of Hostnames and their Lctns.
            Map<String, String> tempMapHostnameToLctn = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            try {
                ClientResponse response = client().callProcedure("ComputeNodeListLctnAndHostname");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    tempMapHostnameToLctn.put(vt.getString("HostName"), vt.getString("Lctn"));
                }
                mCompNodeHostnameToLctnMap = tempMapHostnameToLctn;
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - ComputeNodeListLctnAndHostname stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            }
        }
        return mCompNodeHostnameToLctnMap;
    }   // End mapCompNodeHostNameToLctn()


    // Get the map of ServiceNode Lctns to Hostnames - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapServNodeLctnToHostName() {
        if (mServNodeLctnToHostnameMap == null) {
            // create and populate the map of Lctns and their Hostnames.
            Map<String, String> tempMapLctnToHostname = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            try {
                ClientResponse response = client().callProcedure("ServiceNodeListLctnAndHostname");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    tempMapLctnToHostname.put(vt.getString("Lctn"), vt.getString("HostName"));
                }
                mServNodeLctnToHostnameMap = tempMapLctnToHostname;
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - ServiceNodeListLctnAndHostname stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            }
        }
        return mServNodeLctnToHostnameMap;
    }   // End mapServNodeLctnToHostName()

    // Get the map of ServiceNode Hostnames to Lctns - under the covers it will create and populate the map the first time it is accessed.
    @Override
    public Map<String, String> mapServNodeHostNameToLctn() {
        if (mServNodeHostnameToLctnMap == null) {
            // create and populate the map of Hostnames and their Lctns.
            Map<String, String> tempMapHostnameToLctn = new HashMap<String, String>();
            // Get list of Lctns and their HostNames.
            try {
                ClientResponse response = client().callProcedure("ServiceNodeListLctnAndHostname");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    tempMapHostnameToLctn.put(vt.getString("HostName"), vt.getString("Lctn"));
                }
                mServNodeHostnameToLctnMap = tempMapHostnameToLctn;
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - ServiceNodeListLctnAndHostname stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            }
        }
        return mServNodeHostnameToLctnMap;
    }   // End mapServNodeHostNameToLctn()


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
    }   // End mapBmcIpAddrToNodeLctn()


    @Override
    // Get the fully qualified lctn (e.g., R0-CH00-CN0-A3 or R1-CH01-CN1-H1) for a PCIE FRU by using the NodeLctn and PCIE BusAddr.
    public String getPcieFrusLctnUsingBusAddr(String sNodeLctn, String sBusAddr) {
        String sMapKey = sNodeLctn + "+" + sBusAddr;
        String sFullyQualifiedFruLctn = mapNodelctnAndBusaddrToLctn().get(sMapKey);
        // Check & see if we were able to get the fully qualified fru's lctn string.
        if (sFullyQualifiedFruLctn == null) {
            // we did not find the fully qualified FruLctn for the specified NodeLctn + BusAddr combination.
            mLogger.warn("Unable to find the fully qualified FruLctn for the specified NodeLctn and BusAddr - NodeLctn=%s, Busaddr=%s, MapKey=%s!",
                         sNodeLctn, sBusAddr, sMapKey);
            if (mapNodelctnAndBusaddrToLctn().size() == 0)
                mLogger.error("Map of node lctn and bus address to device lctn is completely empty!");
            // Cut a RAS event to capture this occurrence.
            String sPertinentInfo = "NodeLctn=" + sNodeLctn + ", BusAddr=" + sBusAddr + ", MapKey=" + sMapKey;
            logRasEventCheckForEffectedJob("RasGenAdapterUnableFindPcieFrusLctn"
                                          ,sPertinentInfo
                                          ,sNodeLctn                          // Lctn
                                          ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                          ,mAdapterType                       // the type of adapter that ran this diagnostic
                                          ,workQueue().workItemId()           // the work item id that the adapter was doing when this occurred
                                          );
            // Since we don't have the fully qualified lctn string just return the node's lctn string (it is better than nothing).
            return sNodeLctn;
        }
        // Return the fully qualified FruLctn.
        return sFullyQualifiedFruLctn;
    }   // End getPcieFrusLctnUsingBusAddr(String sNodeLctn, String sBusAddr)


    @Override
    // Return a map that has a key of a Service or Compute Nodelctn + Busaddr (e.g., "R0-CH00-CN0+0000:44:03.0") and a value of its fully qualified FruLctn (e.g., R0-CH00-CN0-A3).
    //  Note: this map is different than most of the similar maps, the others are mostly invariant (non-changing), since pcie bus addresses may change at boot time this map can not be invariant.
    //        It will be "populated/updated" when node's inventory info is obtained during boot!
    public Map<String, String> mapNodelctnAndBusaddrToLctn() {
        if (mNodelctnAndBusaddrToLctnMap == null) {
            // Get count of number of PCIE devices defined within this machine.
            ClientResponse response = null;
            try {
                response = client().callProcedure("@AdHoc", "SELECT COUNT(*) FROM Accelerator;");
            } catch (IOException | ProcCallException ex) {
                mLogger.exception(ex, "Unable to retrieve count of accelerators from the db");
                throw new RuntimeException("Unable to retrieve count of accelerators from the db", ex);
            }
            VoltTable vt = response.getResults()[0];
            if (response.getStatus() != ClientResponse.SUCCESS) {
                mLogger.error("Unable to retrieve count of accelerators from the db. Client response status: " + response.getStatus());
                throw new RuntimeException("Unable to retrieve count of accelerators from the db. Client response status: " + response.getStatus());
            }
            int iNumEntriesNeededInMap = vt.getRowCount();

            try {
                response = client().callProcedure("@AdHoc", "SELECT COUNT(*) FROM Hfi;");
            } catch (IOException | ProcCallException ex) {
                mLogger.exception(ex, "Unable to retrieve count of hfis from the db");
                throw new RuntimeException("Unable to retrieve count of hfis from the db", ex);
            }
            vt = response.getResults()[0];
            if (response.getStatus() != ClientResponse.SUCCESS) {
                mLogger.error("Unable to retrieve count of hfis from the db. Client response status: " + response.getStatus());
                throw new RuntimeException("Unable to retrieve count of hfis from the db. Client response status: " + response.getStatus());
            }
            iNumEntriesNeededInMap += vt.getRowCount();

            // Create the map of NodeLctns + BusAddrs to the fully qualified FruLctns - these entries are initially empty (they will be filled in as the node's boot).
            // Map<String, String> tempNodelctnAndBusaddrToLctn = new HashMap<String, String>(lNumEntriesNeededInMap);
            Map<String, String> tempNodelctnAndBusaddrToLctn = new HashMap<String, String> (iNumEntriesNeededInMap);
            try {
                // Get list of Accelerators that already have non-null BusAddr values.
                response = client().callProcedure("@AdHoc", "SELECT NodeLctn, Lctn, BusAddr FROM Accelerator WHERE BusAddr IS NOT NULL ORDER BY Lctn;");
                // Loop through each of the rows that were returned.
                vt = response.getResults()[0];
                for (int iCntr = 0; iCntr < vt.getRowCount(); ++iCntr) {
                    vt.advanceRow();
                    String sKey = vt.getString("NodeLctn") + "+" + vt.getString("BusAddr");
                    tempNodelctnAndBusaddrToLctn.put(sKey, vt.getString("Lctn"));
                }
                int iNumRows = vt.getRowCount();
                // Get list of HFIs that already have non-null BusAddr values.
                response = client().callProcedure("@AdHoc", "SELECT NodeLctn, Lctn, BusAddr FROM Hfi WHERE BusAddr IS NOT NULL ORDER BY Lctn;");
                // Loop through each of the rows that were returned.
                vt = response.getResults()[0];
                for (int iCntr = 0; iCntr < vt.getRowCount(); ++iCntr) {
                    vt.advanceRow();
                    String sKey = vt.getString("NodeLctn") + "+" + vt.getString("BusAddr");
                    tempNodelctnAndBusaddrToLctn.put(sKey, vt.getString("Lctn"));
                }
                iNumRows += vt.getRowCount();
                mLogger.info("Added %d entries into the newly created map of Nodelctn+Busaddr to Lctn", iNumRows);
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
            }
            mNodelctnAndBusaddrToLctnMap = tempNodelctnAndBusaddrToLctn;
        }
        return mNodelctnAndBusaddrToLctnMap;
    }   // End mapNodelctnAndBusaddrToLctn()


    @Override
    // Return a map that has a key of a Service or Compute Nodelctn + Slot (e.g., "R0-CH00-CN0+3") and a value of its fully qualified FruLctn (e.g., R0-CH00-CN0-A3) - slots are like 1, 2, 3, 4, 5, 6 for Accelerators, 7.1-1, 7.2-1, 8.1, 8.2 for HFIs
    public Map<String, String> mapNodelctnAndSlotToLctn() {
        if (mNodelctnAndSlotToLctnMap == null) {
            // create and populate the map of NodeLctns + Slots to the fully qualified FruLctns.
            Map<String, String> tempNodelctnAndSlotToLctn = new HashMap<> ();
            long lNumExpectedMapEntries = 0L;
            try {
                // Get list of Accelerators including their FruLctn and Slot (the node's lctn will be extracted from Accelerator's lctn).
                ClientResponse response = client().callProcedure("AcceleratorsList");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                lNumExpectedMapEntries += vt.getRowCount();
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    String sKey = extractNodeLctnFromLctn(vt.getString("Lctn")) + "+" + vt.getString("Slot");
                    tempNodelctnAndSlotToLctn.put(sKey, vt.getString("Lctn"));
                }
                // Get list of HFI nics including their FruLctn and Slot (the node's lctn will be extracted from HFI's lctn).
                response = client().callProcedure("HfisList");
                // Loop through each of the nodes that were returned.
                vt = response.getResults()[0];
                lNumExpectedMapEntries += vt.getRowCount();
                for (int iSnCntr = 0; iSnCntr < vt.getRowCount(); ++iSnCntr) {
                    vt.advanceRow();
                    String sKey = extractNodeLctnFromLctn(vt.getString("Lctn")) + "+" + vt.getString("Slot");
                    tempNodelctnAndSlotToLctn.put(sKey, vt.getString("Lctn"));
                }
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
                tempNodelctnAndSlotToLctn = new HashMap<>();
            }
            mNodelctnAndSlotToLctnMap = tempNodelctnAndSlotToLctn;
            if (mNodelctnAndSlotToLctnMap.size() != lNumExpectedMapEntries)
                mLogger.error("Number of entries in mapNodelctnAndSlotToLctn (%d) does not match expected value (%d)!", mNodelctnAndSlotToLctnMap.size(), lNumExpectedMapEntries);
        }
        return mNodelctnAndSlotToLctnMap;
    }   // End mapNodelctnAndSlotToLctn()


    @Override
    // Return a map that takes a Service or Compute node's lctn AND a Dimm ModuleLocator (R0-CH00-CN0 and CPU2_DIMM_A2) and gives you its fully qualified Dimm lctn (e.g., R0-CH00-CN0-D3)
    public Map<String, String> mapNodelctnAndModulelocatorToLctn() {
        if (mNodeLctnAndModulelocatorToLctnMap == null) {
            // create and populate the map of Node Lctns And ModuleLocator to fully qualified processor Lctn strings.
            Map<String, String> tempNodelctnAndModulelocatorToLctn = new HashMap<>();
            try {
                // Get list of Dimms including their Lctn and ModuleLocator (node's lctn will be extracted from Dimm's lctn).
                ClientResponse response = client().callProcedure("DimmsList");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    String sKey = extractNodeLctnFromLctn(vt.getString("Lctn")) + "+" + vt.getString("ModuleLocator");
                    tempNodelctnAndModulelocatorToLctn.put(sKey, vt.getString("Lctn"));
                }
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - DimmsList stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
                tempNodelctnAndModulelocatorToLctn = null;
            }
            mNodeLctnAndModulelocatorToLctnMap = tempNodelctnAndModulelocatorToLctn;
        }
        return mNodeLctnAndModulelocatorToLctnMap;
    }   // End mapNodelctnAndModulelocatorToLctn()


    @Override
    // Return a map that takes a Service or Compute node's lctn AND a Processor SocketDesignation (R0-CH00-CN0 and CPU1) and gives you its fully qualified Processor lctn (e.g., R0-CH00-CN0-P1)
    public Map<String, String> mapNodelctnAndSocketdesignationToLctn() {
        if (mNodeLctnAndSocketdesignationToLctnMap == null) {
            // create and populate the map of Node Lctns And SocketDesignation to fully qualified processor Lctn strings.
            Map<String, String> tempNodelctnAndSocketdesignationToLctn = new HashMap<>();
            try {
                // Get list of Processors including their Lctn and SocketDesignation (node's lctn will be extracted from Processor's lctn).
                ClientResponse response = client().callProcedure("ProcessorsList");
                // Loop through each of the nodes that were returned.
                VoltTable vt = response.getResults()[0];
                for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                    vt.advanceRow();
                    String sKey = extractNodeLctnFromLctn(vt.getString("Lctn")) + "+" + vt.getString("SocketDesignation");
                    tempNodelctnAndSocketdesignationToLctn.put(sKey, vt.getString("Lctn"));
                }
            }
            catch (IOException | ProcCallException e) {
                mLogger.error("Adapter - exception occurred - ProcessorsList stored procedure failed!");
                mLogger.error("Adapter - %s", Adapter.stackTraceToString(e));
                tempNodelctnAndSocketdesignationToLctn = null;
            }
            mNodeLctnAndSocketdesignationToLctnMap = tempNodelctnAndSocketdesignationToLctn;
        }
        return mNodeLctnAndSocketdesignationToLctnMap;
    }   // End mapNodelctnAndSocketdesignationToLctn()


    ClientResponse response()       { return mResponse; }
    SignalHandler  signalHandler()  { return mSignalHandler; }

    // Constructor
    public Adapter(String sThisAdaptersAdapterType, String sAdapterName, Logger logger) throws IOException {
        mAdapterType = sThisAdaptersAdapterType;
        mAdapterName = sAdapterName;
        mLogger = logger;
        mJsonParser = ConfigIOFactory.getInstance("json");
        if (mJsonParser == null)  throw new RuntimeException("Adapter - Failed to create a JSON parser!");
        mRasDescrNameToMetaDataMap = new HashMap<String, MetaData>();  // map that takes a RAS DescriptiveName and gives you its corresponding MetaData.
        mPid = IAdapter.getProcessPid();  // grab the pid of the process this adapter instance is running in.
        initializeClient();
        initializeSignalHandler();

        // Create mapping of DataMover result's table index and gives you the corresponding table name.
        // Note: when changing, also change in the DataMoverGetListOfRecsToMove.java stored procedure and ALSO in the AdapterOnlineTier constructor (the list of tables that should be purged from)!!!
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
        mDataMoverResultTblIndxToTableNameMap.put(28, "Constraint");
        mDataMoverResultTblIndxToTableNameMap.put(29, "Dimm");
        mDataMoverResultTblIndxToTableNameMap.put(30, "Processor");
        mDataMoverResultTblIndxToTableNameMap.put(31, "Accelerator");
        mDataMoverResultTblIndxToTableNameMap.put(32, "Hfi");
        mDataMoverResultTblIndxToTableNameMap.put(33, "RawHWInventory_History");
        mDataMoverResultTblIndxToTableNameMap.put(34, "Raw_DIMM");
        mDataMoverResultTblIndxToTableNameMap.put(35, "Raw_FRU_Host");
    }   // End Adapter(String sThisAdaptersAdapterType, String sAdapterName)

    void initializeSignalHandler() {
        // Set up the signal handler object and method.
        mSignalHandler = new SignalHandler() {
            public void handle(Signal signal) {
                try {

                    if (signal.getName().equals("USR2")) {
                        mLogger.warn("SignalHandler - a SIG%s signal (%d) has come in, updating adapters cached information",
                                     signal.getName(), signal.getNumber());
                        loadRasMetadata();
                        mLogger.warn("SignalHandler - adapters cached RAS Meta data has been updated!");
                        mRasEventLog.loadRasMetadata();
                        mLogger.warn("SignalHandler - workQueues cached RAS Meta data has been updated!");
                        mLogger.warn("SignalHandler - a SIG%s signal (%d) has come in, updated  adapters cached information",
                                     signal.getName(), signal.getNumber());
                        logRasEventSyncNoEffectedJob("RasGenAdapterUpdatedCachedRasMetaData"
                                                    ,("SignalName=SIG" + signal.getName() + ", SignalNumber=" + signal.getNumber())
                                                    ,mSnLctn                            // lctn of node this adapter is running on
                                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                    ,mAdapterType                       // type of the adapter that is requesting/issuing this invocation
                                                    ,workQueue().baseWorkItemId()       // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                    );
                    }
                    else {
                        mLogger.fatal("SignalHandler - a SIG%s signal (%d) has come in, this adapter is terminating now!",
                                      signal.getName(), signal.getNumber());
                        adapterShutdownStarted(true);  // Set flag indicating that we are going through adapter shutdown.
                        mLogger.fatal("SignalHandler - starting wait");
                        Thread.sleep(10 * 1000L);      // wait a little bit to give adapters a chance to clean up.
                        mLogger.fatal("SignalHandler - ending   wait");
                        mShutdownHandler.handleShutdown();
                    }
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
            @Override public void backpressure(boolean status) {
                mLogger.warn("Backpressure from the database is causing a delay in processing requests.");
            }
            @Override public void uncaughtException(ProcedureCallback callback, ClientResponse r, Throwable e) {
                mLogger.error("ClientStatusListenerExt - a uncaught exception occurred!");
                mLogger.error("ClientStatusListenerExt - %s", Adapter.stackTraceToString(e));
                mLogger.exception(e, "An error has occurred in a callback procedure. Check the following stack trace for details.");
            }
            @Override public void lateProcedureResponse(ClientResponse response, String hostname, int port) {
                mLogger.error("A procedure that timed out on host %s:%d has now responded.", hostname, port);
            }
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
            // Populate the RAS DescriptiveName to the MetaData map.
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
            mRasDescrNameToMetaDataMap.put(rasMetaData.getString("DescriptiveName"), metaData);
        }
    }   // End loadRasMetadata()


    @Override
    public void enableSignalHandlers() {
        if (mShutdownHandler == null) {
            mLogger.error("enableSignalHandlers() - Shutdown handler must be set before enabling signal handlers!");
            throw new RuntimeException("enableSignalHandlers() - Shutdown handler must be set before enabling signal handlers!");
        }
        // Set up signal handlers for this process.
        Signal.handle(new Signal("HUP"),  signalHandler());  // SIGUSR1 - signal  1
        Signal.handle(new Signal("INT"),  signalHandler());  // SIGINT  - signal  2
        Signal.handle(new Signal("TERM"), signalHandler());  // SIGTERM - signal 15
        Signal.handle(new Signal("USR2"), signalHandler());  // SIGUSR2 - signal 12
        Signal.handle(new Signal("ABRT"), signalHandler());  // SIGABRT - signal  6
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
    class MyCallbackForHouseKeepingNoRtrnValue implements ProcedureCallback {
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
                mObj.logRasEventNoEffectedJob("RasGenAdapterMyCallbackForHouseKeepingNoRtrnValueFailed"
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
                            mObj.logRasEventNoEffectedJob("RasProvCompNodeDiscFailedInvalidMacAddr"
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
                            mObj.logRasEventNoEffectedJob("RasProvServiceNodeDiscFailedInvalidMacAddr"
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
                            mObj.logRasEventNoEffectedJob("RasProvCompNodeSaveIpAddrFailedInvalidMacAddr"
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // requesting adapter work item id
                                                         );
                            bLoggedRasEvent = true;
                        } else if (response.getStatusString().contains("not the same as the expected IP address")) {
                            mObj.logRasEventNoEffectedJob("RasProvCompNodeSaveIpAddrFailedInvalidIpAddr"
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
                            mObj.logRasEventNoEffectedJob("RasProvServiceNodeSaveIpAddrFailedInvalidMacAddr"
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // requesting adapter work item id
                                                         );
                            bLoggedRasEvent = true;
                        } else if (response.getStatusString().contains("not the same as the expected IP address")) {
                            mObj.logRasEventNoEffectedJob("RasProvServiceNodeSaveIpAddrFailedInvalidIpAddr"
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
                            mObj.logRasEventNoEffectedJob("RasProvCompNodeSetStateFailedInvalidNode"
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
                            mObj.logRasEventNoEffectedJob("RasProvServiceNodeSetStateFailedInvalidNode"
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
                            mObj.logRasEventNoEffectedJob("RasProvCompNodeSaveBootImageFailedInvalidNode"
                                                         ,("AdapterName=" + mAdapterName + ", SpThisIsCallbackFor=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo + ", StatusString=" + response.getStatusString())
                                                         ,mPertinentInfo                        // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L    // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                          // type of adapter that is generating this ras event
                                                         ,mWorkItemId                           // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                            bLoggedRasEvent = true;
                        }
                        break;
                }

                // IFF another RAS event has not been logged, log the generic callback failed RAS event.
                if (bLoggedRasEvent == false) {
                    mObj.logRasEventNoEffectedJob("RasGenAdapterMyCallbackForHouseKeepingLongRtrnValueFailed"
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
                long lRtrnValue = response.getResults()[0].asScalarLong();  // save the stored procedure's results (returned as a scalar long).
                mObj.mLogger.info("MyCallbackForHouseKeepingLongRtrnValue - %s was successful, PertinentInfo='%s', WorkItemId=%d, RtrnValue=%d", mSpThisIsCallbackFor, mPertinentInfo, mWorkItemId, lRtrnValue);
                switch (mSpThisIsCallbackFor) {
                    ////case "RasEventStore":
                    ////    if (lRtrnValue < 0) {
                    ////        // Ras event was successfully generated AND this particular ras event does have a ControlOperation associated with it.
                    ////        // Wake up a RAS adapter to notify it that a RAS event with a ControlOperation has been stored in the data store!
                    ////        mObj.mLogger.warn("MyCallbackForHouseKeepingLongRtrnValue - a ras event was stored that has a ControlOperation associated with it, unable to wake up the RAS adapter as scon wakeup has not been implemented!");
                    ////        // ToDo: implement scon mechanism to wake up a RAS adapter (so it is aware that ras event with ControlOperation has been added to data store)!!!
                    ////    }
                    ////    break;
                    case "ComputeNodeDiscovered": case "ComputeNodeSaveBootImageInfo": case "ComputeNodeSaveIpAddr": case "ComputeNodeReplaced": case "ComputeNodeSaveEnvironmentInfo":
                    case "ServiceNodeDiscovered":                                      case "ServiceNodeSaveIpAddr": case "ServiceNodeReplaced": case "ServiceNodeSetState":
                        if (lRtrnValue == 1L) {
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
                            mObj.logRasEventNoEffectedJob("RasGenAdapterDetectedOutOfTsOrderItem"
                                                         ,("AdapterName=" + mAdapterName + ", StoredProcedure=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo)
                                                         ,sTempLctn                          // Lctn
                                                         ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                       // type of adapter that is generating this ras event
                                                         ,mWorkItemId                        // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                        }
                        else if (lRtrnValue == 2L) {
                            // this record occurred OUT OF timestamp order but within a reasonable time range (this appears to have been a 'time is not a river' occurrence.
                            mObj.mLogger.info("MyCallbackForHouseKeepingLongRtrnValue - %s - %s - fyi, this item occurred out of timestamp order but within a reasonable time range (time is not a river), but it has been handled properly",
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
                            mObj.logRasEventNoEffectedJob("RasGenAdapterDetectedOutOfTsOrderItemWiReasonableTime"
                                                         ,("AdapterName=" + mAdapterName + ", StoredProcedure=" + mSpThisIsCallbackFor + ", " + "PertinentInfo=" + mPertinentInfo)
                                                         ,sTempLctn                          // Lctn
                                                         ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,mAdapterType                       // type of adapter that is generating this ras event
                                                         ,mWorkItemId                        // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                        }
                        else if (lRtrnValue == -1L) {
                            // this record occurred outside of the normal boot flow, so no changes were made to the database (ignored from a database point of view).
                            mObj.mLogger.warn("MyCallbackForHouseKeepingLongRtrnValue - %s - %s - fyi, this item occurred outside of the normal boot flow so NO CHANGES were made to the db",
                                              mSpThisIsCallbackFor, mPertinentInfo);
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
                            mObj.logRasEventNoEffectedJob("RasGenAdapterDetectedOutOfTsOrderItem"
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
                                mObj.logRasEventNoEffectedJob("RasProvNodeActive"
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
        mSconRank = lThisAdaptersAdapterId;  // we are temporarily using the new AdapterId also as the SCON rank - until we figure out what to do with and how to assign scon ranks.
        mLogger.info("registerAdapter - successfully started adapter, AdapterType=%s, AdapterId=%d, Lctn=%s, Pid=%d", mAdapterType, mAdapterId, sSnLctn, pid());
    }   // End registerAdapter(String sSnLctn)

    void adapterTerminating(String sBaseWorkItemResults) throws IOException, InterruptedException, AdapterException  {
        // Remove this adapter from the adapter table, indicating that this adapter has terminated
        teardownAdapter();
        // Mark this adapter's base work item as Finished (and implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
        // (this will be done synchronously).
        teardownAdaptersBaseWorkItem(sBaseWorkItemResults);
        // Close the connections to db nodes.
//        client().drain(); // ensure that all async calls have completed.
//        client().close(); // close all of the connections and release any resources associated with the client.
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
        logRasEventSyncNoEffectedJob("RasGenAdapterAbend"
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
    public void logRasEventNoEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        sEventDescrName = ensureRasDescrNameIsValid(sEventDescrName);
        String sTempStoredProcedure = "RasEventStore";
        MyCallbackForHouseKeepingLongRtrnValue oTempCallback = null;
        try {
            //mLogger.info("%s - logRasEventNoEffectedJob - before MyCallbackForHouseKeepingLongRtrnValue ctor - this=%s, sReqAdapterType=%s, mAdapterName=%s, sTempStoredProcedure=%s, sEventDescrName=%s, lReqWorkItemId=%d", mAdapterName, this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventDescrName, lReqWorkItemId);  // temporary debug for java.lang.NoClassDefFoundError
            oTempCallback = new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventDescrName, lReqWorkItemId);
            client().callProcedure(oTempCallback         // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sEventDescrName       // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,null                  // null indicates that we KNOW that no job was effected by the "event" whose occurrence caused the logging of this ras event.
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
            mLogger.info("logRasEventNoEffectedJob - called stored procedure %s asynchronously - DescrName=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventDescrName, sLctn, sInstanceData);
        }
        catch (NoConnectionsException nce) {
            mLogger.error("logRasEventNoEffectedJob - NoConnectionsException exception occurred while trying to log ras event %s, RAS event can NOT be logged, pausing for 10 seconds!", sEventDescrName);
            try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
        }
        catch (Exception e) {
            mLogger.error("logRasEventNoEffectedJob - exception occurred trying to log ras event %s!", sEventDescrName);
            mLogger.error("logRasEventNoEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventNoEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)

    // Log the specified ras event used when the caller is certain that NO job was effected by the "event" whose occurrence caused the logging of this ras event.
    // Note: this use synchronous data store updates!
    public void logRasEventSyncNoEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        sEventDescrName = ensureRasDescrNameIsValid(sEventDescrName);
        String sTempStoredProcedure = "RasEventStore";
        try {
            mLogger.info("logRasEventSyncNoEffectedJob - calling stored procedure %s synchronously - DescrName=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventDescrName, sLctn, sInstanceData);
            client().callProcedure(sTempStoredProcedure  // stored procedure name
                                  ,sEventDescrName       // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,null                  // null indicates that we KNOW that no job was effected by the "event" whose occurrence caused the logging of this ras event.
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
        }
        catch (NoConnectionsException nce) {
            mLogger.error("logRasEventSyncNoEffectedJob - NoConnectionsException exception occurred while trying to log ras event %s, RAS event can NOT be logged, pausing for 10 seconds!", sEventDescrName);
            try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
        }
        catch (Exception e) {
            mLogger.error("logRasEventSyncNoEffectedJob - exception occurred trying to log ras event %s!", sEventDescrName);
            mLogger.error("logRasEventSyncNoEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventSyncNoEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    // Log the specified ras event when the caller knows WHICH job was effected by the "event" whose occurrence caused the logging of this ras event.
    @Override
    public void logRasEventWithEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        sEventDescrName = ensureRasDescrNameIsValid(sEventDescrName);
        String sTempStoredProcedure = "RasEventStore";
        try {
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventDescrName, lReqWorkItemId)  // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sEventDescrName       // type of ras event
                                  ,sInstanceData         // event's instance data
                                  ,sLctn                 // location that this ras event occurred on
                                  ,sJobId                // jobId of a job that was effected by this ras event
                                  ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                                  ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                                  ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
                                  );
            mLogger.info("logRasEventWithEffectedJob - called stored procedure %s asynchronously - DescrName=%s, Lctn=%s, JobId=%s, InstanceData='%s'", sTempStoredProcedure, sEventDescrName, sLctn, sJobId, sInstanceData);
        }
        catch (NoConnectionsException nce) {
            mLogger.error("logRasEventWithEffectedJob - NoConnectionsException exception occurred while trying to log ras event %s, RAS event can NOT be logged, pausing for 10 seconds!", sEventDescrName);
            try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
        }
        catch (Exception e) {
            mLogger.error("logRasEventWithEffectedJob - exception occurred trying to log ras event %s!", sEventDescrName);
            mLogger.error("logRasEventWithEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventWithEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)

    // Log the specified ras event used when the caller is not sure whether or not a job was effected by the "event" whose occurrence caused the logging of this ras event.
    @Override
    public void logRasEventCheckForEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        sEventDescrName = ensureRasDescrNameIsValid(sEventDescrName);
        String sTempStoredProcedure = "RasEventStore";
        try {
            // Ensure that a lctn was specified, no use looking for an associated job if there was no lctn specified.
            if ((sLctn == null) || (sLctn.trim().isEmpty())) {
                mLogger.info("logRasEventCheckForEffectedJob - no lctn was specified on this invocation so we are invoking logRasEventNoEffectedJob() instead - DescrName=%s, Lctn=%s, InstanceData='%s'", sEventDescrName, sLctn, sInstanceData);
                logRasEventNoEffectedJob(sEventDescrName, sInstanceData, sLctn, lTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                return;
            }
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sReqAdapterType, mAdapterName, sTempStoredProcedure, sEventDescrName, lReqWorkItemId)  // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sEventDescrName       // type of ras event
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
            mLogger.info("logRasEventCheckForEffectedJob - called stored procedure %s asynchronously - DescrName=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventDescrName, sLctn, sInstanceData);
        }
        catch (NoConnectionsException nce) {
            mLogger.error("logRasEventCheckForEffectedJob - NoConnectionsException exception occurred while trying to log ras event %s, RAS event can NOT be logged, pausing for 10 seconds!", sEventDescrName);
            try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
        }
        catch (Exception e) {
            mLogger.error("logRasEventCheckForEffectedJob - exception occurred trying to log ras event %s!", sEventDescrName);
            mLogger.error("logRasEventCheckForEffectedJob - %s", Adapter.stackTraceToString(e));
        }
    }   // End logRasEventCheckForEffectedJob(String sEventDescrName, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    // This method ensures that the specified RAS event descriptive name is valid/known.
    @Override
    public String ensureRasDescrNameIsValid(String sEventDescrName) {
        // Ensure that a known/valid descriptive name was specified.
        MetaData oMetaData = mRasDescrNameToMetaDataMap.get(sEventDescrName);
        if (oMetaData != null) {
            return sEventDescrName;
        }
        else {
            // got an invalid/unknown RAS descriptive name.
            logRasEventNoEffectedJob("RasGenAdapterMissingRasEventDescrName"
                                    ,("AdapterName=" + mAdapterName + ", DescriptiveName=" + sEventDescrName)
                                    ,null                               // lctn
                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,mAdapterType                       // type of the adapter that is requesting/issuing this invocation
                                    ,workQueue().baseWorkItemId()       // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                    );
            return "RasUnknownEvent"; // "RasUnknownEvent" is used as a RAS DescriptiveName indicating that the specified descriptive name was unknown/invalid.
        }
    }   // End ensureRasDescrNameIsValid(String sEventDescrName)


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
                PropertyMap jsonObject = mJsonParser.readConfig(stream).getAsMap();
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
                        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, mAdapterType, mAdapterName, "RASMETADATA.upsert", event.getString("DescriptiveName"), workQueue().baseWorkItemId()),
                                "RASMETADATA.upsert", event.getString("DescriptiveName"),
                                event.getString("Severity"), event.getString("Category"), event.getString("Component"),
                                null, event.getString("Msg"), (System.currentTimeMillis() * 1000), event.getStringOrDefault("GenerateAlert", "N"));
                    }
                    else {
                        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, mAdapterType, mAdapterName, "RASMETADATA.upsert", event.getString("DescriptiveName"), workQueue().baseWorkItemId()),
                                "RASMETADATA.upsert", event.getString("DescriptiveName"),
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
    // Extract a node's lctn (compute node or service node) from what may be a more fully qualified lctn string
    // (might for instance be R2-CH03-N2-IVOC4, and we need the node's lctn string which would be R2-CH03-N2).
    //--------------------------------------------------------------------------
    @Override
    public String extractNodeLctnFromLctn(String sLctn) {
        String sTempLctn = DefaultLocations.extractFruLocation(sLctn, mNumLevelsInComputeNodeLctn);  // needed because maybe a subfru lctn string was specified, R2-CH03-N2-IVOC4
        if (!isComputeNodeLctn(sTempLctn))
            sTempLctn = DefaultLocations.extractFruLocation(sLctn, mNumLevelsInServiceNodeLctn);  // needed because maybe a subfru lctn string was specified, SN0-SSN3-IVOC1
        return sTempLctn;
    }   // End extractNodeLctnFromLctn(String sLctn)

    //--------------------------------------------------------------------------
    // Kill a job.
    //--------------------------------------------------------------------------
    @Override
    public void killJob(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException
    {
        // Ensure that a JobId was specified.
        if (sRasEventJobId == null) {
            mLogger.warn("killJob - attempted to run a control operation that included killing a job, but there is no job associated with this RAS event - " +
                         "skipping that portion of the ControlOperation - NodeLctn=%s, EventDescrName=%s, EventId=%d, CtrlOp=%s",
                         sNodeLctn, sRasEventDescrName, lRasEventId, sRasEventCntrlOp);
            return;
        }
        // Ensure that this is a compute node.
        sNodeLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInComputeNodeLctn);  // needed because maybe a subfru lctn string was specified, R2-CH03-N2-IVOC4
        if (!isComputeNodeLctn(sNodeLctn)) {
            // this is not a compute node so there is no reason to issue the kill job work item, ignoring this directive.
            mLogger.warn("killJob - received a request to kill a job on this node, but this node is not a compute node so silently ignoring this directive - " +
                         "skipping sending the work item - NodeLctn=%s, EventDescrName=%s, EventId=%d, CtrlOp=%s, JobId=%s",
                         sNodeLctn, sRasEventDescrName, lRasEventId, sRasEventCntrlOp, sRasEventJobId);
            return;
        }

        // Send a work item to the WLM adapter/provider to kill the specified job.
        Map<String, String> parameters = new HashMap<>();
        parameters.put("jobid", sRasEventJobId);
        long lKjWorkItemId = workQueue().queueWorkItem("WLM"                // type of adapter that needs to handle this work
                                                      ,null                 // queue this work should be put on
                                                      ,"KillJob"            // work that needs to be done
                                                      ,parameters           // parameters for this work
                                                      ,false                // false indicates that we do NOT want to know when this work item finishes
                                                      ,sWorkItemAdapterType // type of adapter that requested this work to be done
                                                      ,lWorkItemId          // work item that the requesting adapter was working on when it requested this work be done
                                                      );
        mLogger.info("killJob - successfully queued KillJob work item - EventDescrName=%s, EventId=%d, ControlOperation=%s, Lctn=%s, JobId=%s, WorkItemId=%d",
                     sRasEventDescrName, lRasEventId, sRasEventCntrlOp, sNodeLctn, sRasEventJobId, lKjWorkItemId);
    }   // End killJob(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Mark this node as Active / Available / Usable, including telling WLM that it can attempt to use this node.
    // Parms:
    //  boolean bUsingSynthData - flag indicating whether synthesized data or "real" data is being used to drive the machine
    //--------------------------------------------------------------------------
    @Override
    public void markNodeActive(String sNodeLctn, boolean bUsingSynthData, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException
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
    // Parms:
    //      bHonorTsOrder - flag indicating whether this invocation should detect an out of timestamp order occurrence
    //                      false = always tell WLM to drain node even if this is occurring out of timestamp order
    //                      true  = if this is occurring out of timestamp order, do not tell WLM to drain the node
    //--------------------------------------------------------------------------
    @Override
    public void markNodePoweredOff(String sNodeLctn, Boolean bHonorTsOrder, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException, ProcCallException, InterruptedException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        String sTempLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInComputeNodeLctn);  // needed because maybe a subfru lctn string was specified, R2-CH03-N2-IVOC4
        if (isComputeNodeLctn(sTempLctn)) {
            sTempStoredProcedure = "ComputeNodeSetState";
            sNodeLctn = sTempLctn;
        }
        else {
            sTempLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInServiceNodeLctn);  // needed because maybe a subfru lctn string was specified, SN0-SSN3-IVOC1
            if (isServiceNodeLctn(sTempLctn)) {
                sTempStoredProcedure = "ServiceNodeSetState";
                sNodeLctn = sTempLctn;
            }
            else {
                mLogger.warn("markNodePoweredOff - Cannot update state for a non-compute or non-service node location: %s ", sNodeLctn);
                return;
            }
        }
        // Check & see if we need to "honor timestamp order" of occurrence.
        long lRc = 0L;  // indicates whether or not this state transition is "occurring out of timestamp order".
        String sTempNewState = "M";  // state of M is Missing (not available/active in the machine).
        if (!bHonorTsOrder) {
            // Set the node's state to Missing state (asynchronously) - simply recording state change info, no need to wait for ack that this work has completed
            client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                                  ,sTempStoredProcedure  // stored procedure name
                                  ,sNodeLctn             // node's location string
                                  ,sTempNewState         // node's new state
                                  ,lTsInMicroSecs        // time that this occurred in microseconds since epoch
                                  ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                                  ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
            mLogger.info("markNodePoweredOff - called stored procedure %s - Lctn=%s, NewState=%s", sTempStoredProcedure, sNodeLctn, sTempNewState);
        }
        else {
            // Set the node's state to Missing state (SYNCHRONOUSLY) - record state change info, need to know if "out of timestamp order" occurred
            // (being done synchronously as we need to know whether or not the stored procedure detected an "out of timestamp order" when setting the node's state).
            mLogger.info("markNodePoweredOff - calling stored procedure %s - Lctn=%s, NewState=%s", sTempStoredProcedure, sNodeLctn, sTempNewState);
            lRc = client().callProcedure(sTempStoredProcedure // stored procedure name
                                        ,sNodeLctn            // node's location string
                                        ,sTempNewState        // node's new state
                                        ,lTsInMicroSecs       // time that this occurred in microseconds since epoch
                                        ,sWorkItemAdapterType // type of the adapter that is requesting/issuing this stored procedure
                                        ,lWorkItemId          // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                                        ).getResults()[0].asScalarLong();
        }

        // Change all of the components within this node (processors, dimms, accelerators, hfis) to have a state of 'M' (only chg those that are not already M).
        chgAllNodeComponentsToPoweredOff(sNodeLctn, lTsInMicroSecs, sWorkItemAdapterType, lWorkItemId);

        // Tell the WLM to not use (drain) the specified node (if appropriate).
        if (lRc == 0L)
            // either this occurred in timestamp order or else the caller did not want us to honor timestamp order.
            tellWlmToNotUseThisNode(sNodeLctn, sWorkItemAdapterType, lWorkItemId, "NodeWasPoweredOff");
        else {
            // caller wants us to honor timestamp order AND this occurred outside of timestamp order - so do NOT tell WLM to drain this node.
            mLogger.info("markNodePoweredOff - detected this occurred out of timestamp order, so we did not tell the WLM to drain this node - Lctn=%s!", sNodeLctn);
            // Cut a RAS event to capture this occurrence.
            logRasEventCheckForEffectedJob("RasGenAdapterSkippedDrainingNodeDueToOutOfTsOrder"
                                          ,("Method=markNodePoweredOff,Lctn=" + sNodeLctn)
                                          ,sNodeLctn                          // Lctn
                                          ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                          ,sWorkItemAdapterType               // the type of adapter that ran this diagnostic
                                          ,lWorkItemId                        // the work item id that the adapter was doing when this occurred
                                          );
        }
    }   // End markNodePoweredOff(String sNodeLctn, Boolean bHonorTsOrder, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)


    //----------------------------------------------------------------------
    // Change all of the specified node's components (processors, dimms, accelerators, hfis) to have a state of 'M' (only chg those which are not already M).
    // - E.g., R5-CH15-CN3-A1, R2-CH13-CN0-H1
    //----------------------------------------------------------------------
    @Override
    public void chgAllNodeComponentsToPoweredOff(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException, ProcCallException
    {
        // Go through all of the sub-frus for this node board and change those that are NOT ALREADY Missing to Missing.
        final String StateDesired = "M";
        String sProcedureName = "NodeComponentSetStateUnlessInThatState";
        String sPertinentInfo = sNodeLctn + ",ComponentState=" + StateDesired;
        client().callProcedure(new MyCallbackForHouseKeepingLongRtrnValue(this, sWorkItemAdapterType, mAdapterName, sProcedureName, sPertinentInfo, lWorkItemId) // asynchronously invoke the procedure
                              ,sProcedureName       // stored procedure name
                              ,sNodeLctn            // node's location string
                              ,StateDesired         // component's new state
                              ,lTsInMicroSecs       // time that this occurred in microseconds since epoch
                              ,sWorkItemAdapterType // type of the adapter that is requesting/issuing this stored procedure
                              ,lWorkItemId);        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("chgAllNodeComponentsToPoweredOff - called stored procedure %s - NodeLctn=%s, NewState=%s", sProcedureName, sNodeLctn, StateDesired);
    }   // End chgAllNodeComponentsToPoweredOff


    //--------------------------------------------------------------------------
    // Mark this node as in BiosStarting, reiterate that WLM can't use this node for running jobs.
    //--------------------------------------------------------------------------
    @Override
    public void markNodeBiosStarting(String sNodeLctn, long lTsInMicroSecs, String sWorkItemAdapterType, long lWorkItemId) throws IOException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sNodeLctn)) {
            sTempStoredProcedure = "ComputeNodeSetState";
        } else if(isServiceNodeLctn(sNodeLctn)) {
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
    public void markNodeInErrorState(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        String sTempLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInComputeNodeLctn);  // needed because maybe a subfru lctn string was specified, R2-CH03-N2-IVOC4
        if (isComputeNodeLctn(sTempLctn)) {
            sTempStoredProcedure = "ErrorOnComputeNode";
            sNodeLctn = sTempLctn;
        }
        else {
            sTempStoredProcedure = "ErrorOnServiceNode";
            sNodeLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInServiceNodeLctn);  // needed because maybe a subfru lctn string was specified, SN0-SSN3-IVOC1
        }
        // Set the node's state to Error state - simply recording state change info, no need to wait for ack that this work has completed
        client().callProcedure(new MyCallbackForHouseKeepingNoRtrnValue(this, sWorkItemAdapterType, mAdapterName, sTempStoredProcedure, sNodeLctn, lWorkItemId) // asynchronously invoke the procedure
                              ,sTempStoredProcedure  // stored procedure name
                              ,sNodeLctn             // node's location string
                              ,sWorkItemAdapterType  // type of the adapter that is requesting/issuing this stored procedure
                              ,lWorkItemId);         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
        mLogger.info("markNodeInErrorState - called stored procedure %s - Lctn=%s, DescrName=%s, EventId=%d, ControlOperation=%s, JobId=%s", sTempStoredProcedure, sNodeLctn, sRasEventDescrName, lRasEventId, sRasEventCntrlOp, sRasEventJobId);
        //----------------------------------------------------------------------
        // Tell WLM to not use this specified node.
        //----------------------------------------------------------------------
        // Get the descriptive name that corresponds to this RAS event type - if no descriptive name for this event type, use the event type.
        String sTempSlurmReason = null;
        MetaData oMetaData = mRasDescrNameToMetaDataMap.getOrDefault(sRasEventDescrName, null);
        if (oMetaData == null) {
            // meta data is not available for the specified ras event type.
            mLogger.error("markNodeInErrorState - the event meta data is not available - Lctn=%s, DescrName=%s, EventId=%d, ControlOperation=%s, JobId=%s", sTempStoredProcedure, sNodeLctn, sRasEventDescrName, lRasEventId, sRasEventCntrlOp, sRasEventJobId);
            sTempSlurmReason = sRasEventDescrName;  // since the event's meta data is not available, use the ras event type for the slurm reason.
        }
        else {
            sTempSlurmReason = oMetaData.descriptiveName();  // use the event's descriptive name for the slurm reason.
        }
        tellWlmToNotUseThisNode(sNodeLctn, sWorkItemAdapterType, lWorkItemId, sTempSlurmReason);
    }   // End markNodeInErrorState(String sNodeLctn, String sRasEventDescrName, long lRasEventId, String sRasEventCntrlOp, String sRasEventJobId, String sWorkItemAdapterType, long lWorkItemId)

    // Note: this version is used for a non-RAS event scenarios!
    @Override
    public void markNodeInErrorState(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)
                throws IOException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        String sTempLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInComputeNodeLctn);  // needed because maybe a subfru lctn string was specified, R2-CH03-N2-IVOC4
        if (isComputeNodeLctn(sTempLctn)) {
            sTempStoredProcedure = "ErrorOnComputeNode";
            sNodeLctn = sTempLctn;
        }
        else {
            sTempStoredProcedure = "ErrorOnServiceNode";
            sNodeLctn = DefaultLocations.extractFruLocation(sNodeLctn, mNumLevelsInServiceNodeLctn);  // needed because maybe a subfru lctn string was specified, SN0-SSN3-IVOC1
        }
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
    public void tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReasonForDraining) throws IOException
    {
        if (isComputeNodeLctn(sNodeLctn)) {
            Map<String, String> parameters = new HashMap<>();
            // Send a work item to the WLM adapter/provider so the WLM will stop scheduling jobs on this node.
            parameters.put("locations", sNodeLctn);
            // false indicates that we do NOT want to perform the check ensuring that this node actually COMPLETES a wlm drain before proceeding.
            parameters.put("ensure_node_drained", "false");
            parameters.put("reason_for_draining", sReasonForDraining);  // reason we want this node to be drained.
            String sTempWork = "DontUseNode";
            long lDunWorkItemId = workQueue().queueWorkItem("WLM"                   // type of adapter that needs to handle this work
                                                           ,null                    // queue this work should be put on
                                                           ,sTempWork               // work that needs to be done
                                                           ,parameters              // parameters for this work
                                                           ,false                   // false indicates that we do NOT want to know when this work item finishes
                                                           ,sWorkItemAdapterType    // type of adapter that requested this work to be done
                                                           ,lWorkItemId             // work item that the requesting adapter was working on when it requested this work be done
                                                           );
            mLogger.info("tellWlmToNotUseThisNode - successfully queued %s work item - Node=%s, NewWorkItemId=%d", sTempWork, sNodeLctn, lDunWorkItemId);
        }
    }   // End tellWlmToNotUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId, String sReasonForDraining)


    //--------------------------------------------------------------------------
    // Helper method to actually tell the WLM to go ahead and to start to use this node.
    //--------------------------------------------------------------------------
    @Override
    public void tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId) throws IOException, ProcCallException, InterruptedException
    {
        // Ensure that this is a compute node - can not tell WLM to start using a non-compute node.
        if (isComputeNodeLctn(sNodeLctn)) {
            // this is a compute node.
            // Ensure that this node is owned by the WLM - cannot tell the WLM to use this node if it is not owned by the WLM subsystem.
            if (!isThisHwOwnedByWlm(sNodeLctn)) {
                String sNodeOwningSubsystem = getOwningSubsystem(sNodeLctn);
                mLogger.error("tellWlmToUseThisNode - cannot tell WLM to begin using this compute node because the WLM does not own this node, OwningSubsystem='%s' - Node=%s!", sNodeOwningSubsystem, sNodeLctn);
                // Cut a RAS event to capture this occurrence.
                logRasEventNoEffectedJob("RasWlmCantTellWlmToUseUnownedNode"
                                        ,("OwningSubsystem=" + sNodeOwningSubsystem)
                                        ,sNodeLctn                          // Lctn
                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                        ,sWorkItemAdapterType               // the type of adapter that ran this diagnostic
                                        ,lWorkItemId                        // the work item id that the adapter was doing when this diagnostic ended
                                        );
                return;  // return without telling WLM to start using this node.
            }
            // Ensure that this node is not in an error state - do not want to tell the WLM to use this node if it is in an error state.
            String sNodeState = client().callProcedure("ComputeNodeState", sNodeLctn).getResults()[0].fetchRow(0).getString(0);
            if (sNodeState.equals("E")) {
                mLogger.error("tellWlmToUseThisNode - cannot tell WLM to begin using this compute node (%s) because the node is in error state, so the WLM was not told that it can use this node!", sNodeLctn);
                return;
            }
            // Send a work item to the WLM adapter/provider so the WLM knows that it can start scheduling jobs on this node.
            Map<String, String> parameters = new HashMap<>();
            parameters.put("locations", sNodeLctn);
            String sTempWork = "UseNode";
            long lUnWorkItemId = workQueue().queueWorkItem("WLM"                // type of adapter that needs to handle this work
                                                          ,null                 // queue this work should be put on
                                                          ,sTempWork            // work that needs to be done
                                                          ,parameters           // parameters for this work
                                                          ,false                // false indicates that we do NOT want to know when this work item finishes
                                                          ,sWorkItemAdapterType // type of adapter that requested this work to be done
                                                          ,lWorkItemId          // work item that the requesting adapter was working on when it requested this work be done
                                                          );
            mLogger.info("tellWlmToUseThisNode - successfully queued %s work item - Node=%s, NewWorkItemId=%d", sTempWork, sNodeLctn, lUnWorkItemId);
        }
        else {
            mLogger.error("tellWlmToUseThisNode - cannot tell WLM to begin using this node because the specified node is NOT a compute node - Node=%s!", sNodeLctn);
            // Cut a RAS event to capture this occurrence.
            logRasEventNoEffectedJob("RasWlmCantTellWlmToUseNonComputeNode"
                                    ,null                               // instance data
                                    ,sNodeLctn                          // Lctn
                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,sWorkItemAdapterType               // the type of adapter that ran this diagnostic
                                    ,lWorkItemId                        // the work item id that the adapter was doing when this diagnostic ended
                                    );
        }
    }   // End tellWlmToUseThisNode(String sNodeLctn, String sWorkItemAdapterType, long lWorkItemId)


    //--------------------------------------------------------------------------
    // Check the specified node lctn's WlmNodeState within the db and fix it up if it is stale.
    //  Parms:
    //      String sNodeLctn - lctn string of the node who's WlmNodeState in the DB needs to be checked
    //      String sNodeName - node name of the node who's WlmNodeState in the DB needs to be checked
    //      String sCorrectWlmNodeState - is the "correct" (non-stale) WlmNodeState value
    //      String sActualWlmNodeInfo - is the node's actual wlm node info as obtained from the wlm (used when logging info)
    //--------------------------------------------------------------------------
    public void chkAndFixupStaleWlmNodeState(String sNodeLctn, String sNodeName, String sCorrectWlmNodeState, String sActualWlmNodeInfo)
                throws IOException, ProcCallException
    {
        // Check node's current db WlmNodeState and fix it up if it is stale/out of date.
        String sDbWlmNodeState = client().callProcedure("ComputeNodeState", sNodeLctn).getResults()[0].fetchRow(0).getString("WlmNodeState");
        if (sDbWlmNodeState != null) {
            if (!sDbWlmNodeState.equals(sCorrectWlmNodeState)) {
                // db has a different WlmNodeState value than the wlm itself.
                mLogger.warn("Detected stale WlmNodeState in the db for %s (%s), fixing up this info - DbWlmNodestate=%s, CorrectWlmNodeState=%s, ActualWlmNodeInfo='%s'",
                             sNodeLctn, sNodeName, sDbWlmNodeState, sCorrectWlmNodeState, sActualWlmNodeInfo);
                // Update the node's WlmNodeState.
                final String TempStoredProcedure = "ComputeNodeSetWlmNodeState";
                String sPertinentInfo = "Reason=StaleWlmNodeState,Lctn=" + sNodeLctn + ",ActualWlmNodeInfo='" + sActualWlmNodeInfo + "',DbWlmNodestate=" + sDbWlmNodeState + ",CorrectWlmNodeState=" + sCorrectWlmNodeState;
                client().callProcedure(createHouseKeepingCallbackNoRtrnValue(adapterType(), adapterName(), TempStoredProcedure, sPertinentInfo, workQueue().workItemId()) // asynchronously invoke the procedure
                                      ,TempStoredProcedure                // stored procedure name
                                      ,sNodeLctn                          // Lctn
                                      ,sCorrectWlmNodeState               // node's wlm node state
                                      ,System.currentTimeMillis() * 1000L // utc timestamp for the time this occurred, in micro-seconds since epoch
                                      ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                      ,workQueue().workItemId()           // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                                      );
                mLogger.info("Called stored procedure %s - Lctn=%s, CorrectWlmNodeState=%s", TempStoredProcedure, sNodeLctn, sCorrectWlmNodeState);
                // Cut RAS event.
                String sTempInstanceData = "ActualWlmNodeInfo=" + sActualWlmNodeInfo + ", DbWlmNodestate=" + sDbWlmNodeState + ", CorrectWlmNodeState=" + sCorrectWlmNodeState;
                logRasEventNoEffectedJob("RasWlmFixedupStaleDbWlmNodeState"
                                        ,sTempInstanceData                   // Instance data
                                        ,sNodeLctn                           // Lctn
                                        ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                        ,adapterType()                       // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()            // requesting work item id
                                        );
            }
            else {
                // db's WlmNodeState already matches what the wlm indicates.
                mLogger.info("DB already has the correct WlmNodeState value for %s (%s), so there is no need to do anything else - DbWlmNodestate=%s, CorrectWlmNodeState=%s, ActualWlmNodeInfo='%s'",
                             sNodeLctn, sNodeName, sDbWlmNodeState, sCorrectWlmNodeState, sActualWlmNodeInfo);
            }
        }
        else {
            mLogger.error("Unable to get the db WlmNodeState for %s (%s), we got a NULL value - CorrectWlmNodeState=%s, ActualWlmNodeInfo='%s'",
                          sNodeLctn, sNodeName, sCorrectWlmNodeState, sActualWlmNodeInfo);
        }
    }   // End chkAndFixupStaleWlmNodeState(String sNodeLctn, String sNodeName, String sCorrectWlmNodeState, String sActualWlmNodeInfo)


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
            logRasEventSyncNoEffectedJob("RasGenAdapterException"
                                        ,("Exception=" + e)                 // instance data
                                        ,null                               // Lctn
                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                        ,mAdapterType                       // type of adapter that is requesting this
                                        ,workQueue().baseWorkItemId()       // requesting work item
                                        );
            mAdapterShutdownStarted = true;  // Set flag indicating that we are going through adapter shutdown.
            abend("exception");
        } catch (Exception e2) {}
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

        /* Set the node's Owner to Service - simply recording state change info, no need to wait for ack that this work has completed
        // [TBD] commenting below as it will be taken care of by Service Adapter when diagnostics is launched from service CLI
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
        } */
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
                throws IOException, ProcCallException, InterruptedException
    {
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
                sState = "F";    // F = failure occurred, diagnostics completed running
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
            // Also log a ras event to document this failure - one ras event for each lctn.
            String[] saLctns = sTruncatedNodeLctns.split(",");
            for (String sTempLctn : saLctns) {
                // Don't log a RAS event for a lctn that was truncated above.
                if (!sTempLctn.endsWith("...")) {
                    logRasEventNoEffectedJob("RasDiagFailed"
                                            ,("DiagId=" + lDiagId + ", Diag=" + sDiag + ", DiagResults=" + sTruncResults + ", AdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId)
                                            ,sTempLctn                          // Lctn
                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                            ,sReqAdapterType                    // the type of adapter that ran this diagnostic
                                            ,lReqWorkItemId                     // the work item id that the adapter was doing when this diagnostic ended
                                            );
                }
            }
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

        /* Set the node's Owner back to Working as diagnostic run has completed
        // [TBD] commenting below as this will be taken care of by Service Adapter when diagnostics is launched from service CLI
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
        } */
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


    //--------------------------------------------------------------------------
    // Get the specified node's inventory info out of the database.
    //--------------------------------------------------------------------------
    public final String getNodesInvInfoFromDb(String sTempLctn) {
        try {
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
                              sTempStoredProcedure, IAdapter.statusByteAsString(responseGetInventoryInfo.getStatus()),
                              responseGetInventoryInfo.getStatusString(), adapterType(), adapterId());
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
        }
        catch (Exception e) {
            // Log the exception and generate a RAS event.
            mLogger.error("Exception occurred!");
            mLogger.error("%s", Adapter.stackTraceToString(e));
            logRasEventNoEffectedJob("RasGenAdapterException"
                                    ,("AdapterName=" + adapterName() + ", Exception=" + e)
                                    ,null                               // node's location
                                    ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()           // requesting work item id
            );
            return null;
        }   // catch
    }   // End getNodesInvInfoFromDb(String sTempLctn)


    //--------------------------------------------------------------------------
    // Get the specified node's bios info out of the database.
    //--------------------------------------------------------------------------
    @Override
    public final String getNodesBiosInfoFromDb(String sTempLctn) {
        try {
            // Check & see if this is a compute node or a service node.
            String sTempStoredProcedure = null;
            if (isComputeNodeLctn(sTempLctn)) {
                sTempStoredProcedure = "ComputeNodeBiosInfo";
            }
            else if (isServiceNodeLctn(sTempLctn)) {
                sTempStoredProcedure = "ServiceNodeBiosInfo";
            }
            else {
                // unexpected lctn string.
                String sErrorMsg = "Specified lctn is neither a compute node nor a service node - Lctn=" + sTempLctn + "!";
                mLogger.error(sErrorMsg);
                throw new RuntimeException(sErrorMsg);
            }
            // Get the lctn's existing bios info out of the DB.
            String sDbBiosInfo = null;
            ClientResponse responseGetBiosInfo = client().callProcedure(sTempStoredProcedure, sTempLctn);
            if (responseGetBiosInfo.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                mLogger.error("stored procedure %s failed - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                              sTempStoredProcedure, IAdapter.statusByteAsString(responseGetBiosInfo.getStatus()),
                              responseGetBiosInfo.getStatusString(), adapterType(), adapterId());
                throw new RuntimeException(responseGetBiosInfo.getStatusString());
            }
            VoltTable vtGetBiosInfo = responseGetBiosInfo.getResults()[0];
            // Check & see if we got bios info for this node.
            if (vtGetBiosInfo.getRowCount() == 0) {
                // no bios information was returned.
                mLogger.warn("There is no bios information for lctn %s in the db", sTempLctn);
                return null;
            }
            // Grab the bios info and return it to the caller.
            vtGetBiosInfo.advanceRow();
            sDbBiosInfo = vtGetBiosInfo.getString("BiosInfo");
            return sDbBiosInfo;
        }
        catch (Exception e) {
            // Log the exception and generate a RAS event.
            mLogger.error("Exception occurred!");
            mLogger.error("%s", Adapter.stackTraceToString(e));
            logRasEventNoEffectedJob("RasGenAdapterException"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sTempLctn + ", Exception=" + e)
                                    ,null                               // node's location
                                    ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()           // requesting work item id
            );
            return null;
        }   // catch
    }   // End getNodesBiosInfoFromDb(String sTempLctn)


    // Update this node's database inventory info.
    private final void updateNodesInvInfoInDb(Date dLineTimestamp, String sTempLctn, String sTrackMismatchedInvInfo, PropertyMap pmDbInvInfo, String sSernum, String sFruType)
                       throws IOException
    {
        // Generate a new json string that has the updated inventory info.
        String sNewInventoryAsJson = mJsonParser.toString(pmDbInvInfo);
        // Put this updated inventory info into the db - simply updating this inv info, so no need to wait for this to complete.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sTempLctn))
            sTempStoredProcedure = "ComputeNodeReplaced";  // note: misleading stored procedure name, this does not mean that the node was necessarily replaced.
        else
            sTempStoredProcedure = "ServiceNodeReplaced";  // note: misleading stored procedure name, this does not mean that the node was necessarily replaced.
        String sTempBiosInfo = null;
        client().callProcedure(createHouseKeepingCallbackLongRtrnValue(adapterType(), adapterName(), sTempStoredProcedure, sTempLctn, workQueue().workItemId()) // asynchronously invoke the procedure
                ,sTempStoredProcedure               // stored procedure name
                ,sTempLctn                          // node's location string
                ,sSernum                            // node's serial number (this could of course be extracted from sNewInventoryAsJson but we do not want to do that w/i stored procedure)
                ,sFruType                           // node's fru type
                ,null                               // node's state, null indicates that the node's state should not be changed (leave the state in the db as it is)
                ,sNewInventoryAsJson                // node's new inventory information
                ,sTempBiosInfo                      // node's bios information
                ,dLineTimestamp.getTime() * 1000L   // time that this occurred in microseconds since epoch
                ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                ,workQueue().workItemId()           // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                );
        mLogger.info("Called stored procedure %s, Lctn=%s, Sernum='%s', FruType='%s', State='%s', InvInfo='%s'",
                     sTempStoredProcedure, sTempLctn, sSernum, sFruType, null, sNewInventoryAsJson);
        // Cut the ras event - I know that there is no job effected by this occurrence.
        logRasEventNoEffectedJob("RasGenAdapterUpdatingDbInvInfo"
                                ,("AdapterName=" + adapterName() + ", Lctn=" + sTempLctn + ", DetectedInvMismatch='" + sTrackMismatchedInvInfo + "'")
                                ,sTempLctn                          // lctn
                                ,dLineTimestamp.getTime() * 1000L   // time this occurred, in micro-seconds since epoch
                                ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                ,workQueue().workItemId()           // requesting work item id
                                );
    }   // End updateNodesInvInfoInDb(Date dLineTimestamp, String sTempLctn, String sTrackMismatchedInvInfo, PropertyMap pmDbInvInfo, String sSernum, String sFruType)


    // Update the property map with any changes from the specified BiosVersion inventory info (if appropriate).
    private final void updateNodesDbInvInfoPmWithBiosInfoFromBootMsg(String sNodeLctn, PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp) {
        String sBios_versionKey = InventoryKeys.BIOS_VERSION;
        // Ensure that the boot msg inventory information has the BiosVersion section.
        if ((oBootMsgInvInfo.mBiosVersion != null) && (!oBootMsgInvInfo.mBiosVersion.trim().isEmpty())) {
            // the boot msg does have the BiosVersion section in it.
            // Get the BiosVersion section out of the DB's LctnInfo inventory info.
            PropertyMap pmDbBiosVersionInfo = pmDbLctnInfo.getMapOrDefault("BiosVersion", null);
            if (pmDbBiosVersionInfo != null) {
                // got the db's BiosVersion info section - update the db's BiosVersion info if there is a mismatch with the boot message.
                // Get the InventoryKeys.BIOS_VERSION section, out of the DB's BiosVersion inventory info.
                PropertyMap pmDbBios_version = pmDbBiosVersionInfo.getMapOrDefault(sBios_versionKey, null);
                if (pmDbBios_version != null) {
                    // got the db's pmDbBios_version section - update the db's Bios_version info if there is a mismatch with the boot message.
                    String sDbBiosVersion = pmDbBios_version.getStringOrDefault("value", "-99999");
                    if (!sDbBiosVersion.equals("-99999")) {
                        // got the db's BiosVersion - update the db's BiosVersion info if there is a mismatch with the boot message.
                        // Check & see if the BiosVersion value has changed.
                        if (!sDbBiosVersion.equals(oBootMsgInvInfo.mBiosVersion)) {
                            // there is a mismatch - update the db inventory property map with the new value from the boot message.
                            pmDbBios_version.put("value", oBootMsgInvInfo.mBiosVersion);
                            pmDbBiosVersionInfo.put(sBios_versionKey, pmDbBios_version);
                            pmDbLctnInfo.put("BiosVersion", pmDbBiosVersionInfo);
                            // Track that there was a mismatch in the inventory information.
                            sbTrackMismatchedInvInfo.append(sBios_versionKey + ",");
                        }   // there is a mismatch - update the db inventory property map with the new value from the boot message.
                    }   // got the db's BiosVersion - update the db's BiosVersion info if there is a mismatch.
                    else {
                        // the db inventory info does have a Bios_version section in it, but it does not have a Bios_version value.
                        mLogger.error("This node has db inventory info, a BiosVersion info section, and a Bios_version section, but it does not contain a Bios_version value - Lctn=%s", sNodeLctn);
                        logRasEventNoEffectedJob("RasGenAdapterDbInvMissingBios_versionSctnValue"
                                                ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                                ,sNodeLctn                         // lctn
                                                ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                                ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                                ,workQueue().workItemId()          // requesting work item id
                                                );
                        // There is a mismatch - update the db inventory property map with the new value from the boot message.
                        pmDbBios_version.put("value", oBootMsgInvInfo.mBiosVersion);
                        pmDbBiosVersionInfo.put(sBios_versionKey, pmDbBios_version);
                        pmDbLctnInfo.put("BiosVersion", pmDbBiosVersionInfo);
                        // Track that there was a mismatch in the inventory information.
                        sbTrackMismatchedInvInfo.append(sBios_versionKey + ",");
                    }
                }   // got the db's pmDbBios_version section - update the db's Bios_version info if there is a mismatch with the boot message.
                else {
                    // the db inventory info does not have a Bios_version section in it.
                    mLogger.error("This node has db inventory info and a BiosVersion info section, but it does not contain a Bios_version section - Lctn=%s", sNodeLctn);
                    logRasEventNoEffectedJob("RasGenAdapterDbInvMissingBios_versionSctn"
                                            ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                            ,sNodeLctn                          // lctn
                                            ,dLineTimestamp.getTime() * 1000L   // time this occurred, in micro-seconds since epoch
                                            ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                            ,workQueue().workItemId()           // requesting work item id
                                            );
                    // There is a mismatch - update the db inventory property map with the new value from the boot message.
                    pmDbBios_version = new PropertyMap();
                    pmDbBios_version.put("value", oBootMsgInvInfo.mBiosVersion);
                    pmDbBiosVersionInfo.put(sBios_versionKey, pmDbBios_version);
                    pmDbLctnInfo.put("BiosVersion", pmDbBiosVersionInfo);
                    // Track that there was a mismatch in the inventory information.
                    sbTrackMismatchedInvInfo.append(sBios_versionKey + ",");
                }
            }   // got the db's BiosVersion info section - update the db's BiosVersion info if there is a mismatch with the boot message.
            else {
                // the db inventory info does not have a BiosVersion info section in it.
                mLogger.error("This node has db inventory info but it does not contain a BiosVersion info section - Lctn=%s", sNodeLctn);
                logRasEventNoEffectedJob("RasGenAdapterDbInvMissingBiosVersInfoSctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                        ,sNodeLctn                          // lctn
                                        ,dLineTimestamp.getTime() * 1000L   // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()           // requesting work item id
                                        );
                // There is a mismatch - update the db inventory property map with the new value from the boot message.
                PropertyMap pmDbBios_version = new PropertyMap();
                pmDbBios_version.put("value", oBootMsgInvInfo.mBiosVersion);
                pmDbBiosVersionInfo = new PropertyMap();
                pmDbBiosVersionInfo.put(sBios_versionKey, pmDbBios_version);
                pmDbLctnInfo.put("BiosVersion", pmDbBiosVersionInfo);
                // Track that there was a mismatch in the inventory information.
                sbTrackMismatchedInvInfo.append("BiosVersion,");
            }   // the db inventory info does not have a BiosVersion info section in it.
        }   // the boot msg does have the BiosVersion section in it.
        else {
            // the boot msg inventory info does not have the BiosVersion section - so just return to the caller.
            mLogger.error("This node's boot message has inventory info, but it does not contain BiosVersion - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingBiosVersionSctn"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
        }
    }   // End updateNodesDbInvInfoPmWithBiosInfoFromBootMsg(PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo)


    //--------------------------------------------------------------------------
    // Compare the SocketCoresInfo between the boot message inventory info and the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareSocketCoresInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbProcessorSummary, PropertyMap pmBootMsgProcessorSummary, String[] aBootMsgSocketCoresKey, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
    {
        // Loop through checking each of the BootMsg's SocketCoresKey values against the info in the DB.
        for (String sSocketCoresKey : aBootMsgSocketCoresKey) {
            // Grab the total cores for this socket from the boot message property map.
            long lBootmsgSocketCoresValue = -99999;
            PropertyMap pmBootMsgSocketCores = pmBootMsgProcessorSummary.getMapOrDefault(sSocketCoresKey, null);
            if (pmBootMsgSocketCores != null) {
                lBootmsgSocketCoresValue = pmBootMsgSocketCores.getLongOrDefault("value", -99999);
            }
            // Get the SocketCores section for this socket, out of the DB's ProcessorSummary inventory info.
            PropertyMap pmDbSocketCores = pmDbProcessorSummary.getMapOrDefault(sSocketCoresKey, null);
            if (pmDbSocketCores != null) {
                // got the db's SocketCores section - update the db's SocketCores info if there is a mismatch with the boot message.
                long lDbSocketCoresValue = pmDbSocketCores.getLongOrDefault("value", -99999);
                if (lDbSocketCoresValue != -99999) {
                    // got the db's SocketCores value - update the db's SocketCores info if there is a mismatch with the boot message.
                    // Check & see if the SocketCores value has changed.
                    if (lDbSocketCoresValue != lBootmsgSocketCoresValue) {
                        // there is a mismatch - update the db inventory property map with the new value from the boot message.
                        pmDbSocketCores.put("value", lBootmsgSocketCoresValue);
                        pmDbProcessorSummary.put(sSocketCoresKey, pmDbSocketCores);
                        pmDbLctnInfo.put("ProcessorSummary", pmDbProcessorSummary);
                        // Track that there was a mismatch in the inventory information.
                        sbTrackMismatchedInvInfo.append(sSocketCoresKey + ",");
                    }   // there is a mismatch - update the db inventory property map with the new value from the boot message.
                }   // got the db's SocketCores value - update the db's SocketCores info if there is a mismatch.
                else {
                    // the db inventory info does have a SocketCores section in it, but it does not have a SocketCores value.
                    mLogger.error("This node has db inventory info, a ProcessorSummary section, and the SocketCores section, but it does not contain a SocketCores value - Lctn=%s, SocketCoresKey=%s",
                                  sNodeLctn, sSocketCoresKey);
                    logRasEventNoEffectedJob("RasGenAdapterDbInvMissingSocketCoresSctnValue"
                                            ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn + ", SocketCoresKey=" + sSocketCoresKey)
                                            ,sNodeLctn                         // lctn
                                            ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                            ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                            ,workQueue().workItemId()          // requesting work item id
                                            );
                    // There is a mismatch - update the db inventory property map with the new value from the boot message.
                    pmDbSocketCores.put("value", lBootmsgSocketCoresValue);
                    pmDbProcessorSummary.put(sSocketCoresKey, pmDbSocketCores);
                    pmDbLctnInfo.put("ProcessorSummary", pmDbProcessorSummary);
                    // Track that there was a mismatch in the inventory information.
                    sbTrackMismatchedInvInfo.append(sSocketCoresKey + ",");
                }   // the db inventory info does have a SocketCores section in it, but it does not have a SocketCores value.
            }   // got the db's SocketCores section - update the db's SocketCores info if there is a mismatch with the boot message.
            else {
                // the db inventory info ProcessorSummary section does not have the SocketCores section in it.
                mLogger.error("This node has db inventory info, a ProcessorSummary section, but it does not contain a SocketCores section - Lctn=%s, SocketCoresKey=%s",
                              sNodeLctn, sSocketCoresKey);
                logRasEventNoEffectedJob("RasGenAdapterDbInvMissingSocketCoresSctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn + ", SocketCoresKey=" + sSocketCoresKey)
                                        ,sNodeLctn                         // lctn
                                        ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()          // requesting work item id
                                        );
                // There is a mismatch - update the db inventory property map with the new value from the boot message.
                pmDbSocketCores = new PropertyMap();
                pmDbSocketCores.put("value", lBootmsgSocketCoresValue);
                pmDbProcessorSummary.put(sSocketCoresKey, pmDbSocketCores);
                pmDbLctnInfo.put("ProcessorSummary", pmDbProcessorSummary);
                // Track that there was a mismatch in the inventory information.
                sbTrackMismatchedInvInfo.append(sSocketCoresKey + ",");
            }   // the db inventory info ProcessorSummary section does not have the SocketCores section in it.
        }   // loop through checking each of the SocketCores info.
        return;
    }   // End compareSocketCoresInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbProcessorSummary, PropertyMap pmBootMsgProcessorSummary, String[] aBootMsgSocketCoresKey, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)


    //--------------------------------------------------------------------------
    // Compare the TotalSystemCores field in the boot message inventory info and the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareTotalSystemCoresInfo(PropertyMap pmDbLctnInfo, PropertyMap pmDbProcessorSummary, PropertyMap pmBootMsgProcessorSummary, StringBuilder sbTrackMismatchedInvInfo)
    {
        long lBootMsgTotalSystemCores = pmBootMsgProcessorSummary.getLongOrDefault("TotalSystemCores", -99999);
        long lDbTotalSystemCores = pmDbProcessorSummary.getLongOrDefault("TotalSystemCores", -99999);
        if (lBootMsgTotalSystemCores != lDbTotalSystemCores)
        {   // there is a mismatch - update the db inventory property map with the new value from the boot message.
            pmDbProcessorSummary.put("TotalSystemCores", lBootMsgTotalSystemCores);
            pmDbLctnInfo.put("ProcessorSummary", pmDbProcessorSummary);
            // Track that there was a mismatch in the inventory information.
            sbTrackMismatchedInvInfo.append("TotalSystemCores" + ",");
        }
        return;
    }   // End compareTotalSystemCoresInfo(PropertyMap pmDbLctnInfo, PropertyMap pmDbProcessorSummary, PropertyMap pmBootMsgProcessorSummary, StringBuilder sbTrackMismatchedInvInfo)


    //--------------------------------------------------------------------------
    // Compare the ProcessorSummary's Processor array in the boot message inventory info and the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareProcessorInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbProcessorSummary, PropertyMap pmBootMsgProcessorSummary, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        final String ProcessorKey = "Processor";
        PropertyArray paBootMsgProcessor = pmBootMsgProcessorSummary.getArrayOrDefault(ProcessorKey, new PropertyArray());
        PropertyArray paDbProcessor = pmDbProcessorSummary.getArrayOrDefault(ProcessorKey, new PropertyArray());
        // Check & see if they have the same number of entries in the Processor section.
        boolean bMismatchOccurred = false;
        if (paBootMsgProcessor.size() == paDbProcessor.size()) {
            // both the boot msg and the db have the same number of entries in Processor array.
            // Loop through each of the processor entries, checking to see if they match.
            for (int iBootMsgProcessorCntr=0; iBootMsgProcessorCntr < paBootMsgProcessor.size(); ++iBootMsgProcessorCntr) {
                boolean bFndProcessorEntry = false;
                PropertyMap pmBootMsgProcessorEntry = paBootMsgProcessor.getMap(iBootMsgProcessorCntr);
                String sBootMsgProcessorLoc = pmBootMsgProcessorEntry.getStringOrDefault("loc", null);
                if (sBootMsgProcessorLoc == null) {
                    mLogger.warn("This processor does not have a loc in the boot message inv info, skipping check for matching processor info in db inv info - Lctn=%s, BootMsgProcessorEntry=%s",
                                 sNodeLctn, pmBootMsgProcessorEntry.toString());
                    continue;
                }
                // Find the corresponding loc entry within the db processor array.
                PropertyMap pmDbProcessorEntry = new PropertyMap();
                for (int iDbProcessorCntr=0; iDbProcessorCntr < paDbProcessor.size(); ++iDbProcessorCntr) {
                    pmDbProcessorEntry = paDbProcessor.getMap(iDbProcessorCntr);
                    String sDbProcessorLoc = pmDbProcessorEntry.getStringOrDefault("loc", null);
                    if (sDbProcessorLoc.equals(sBootMsgProcessorLoc)) {
                        // found the corresponding processor entry here in the db processor array.
                        bFndProcessorEntry = true;
                        break;
                    }
                }
                // See if we found the corresponding loc entry in the db processor array.
                if (bFndProcessorEntry) {
                    // found the corresponding loc entry in the db processor array.
                    // Check & see if these two property maps are the same.
                    String sMismatch = comparePropertyMaps(pmBootMsgProcessorEntry, pmDbProcessorEntry);
                    if (sMismatch != null) {
                        // there is a mismatch between these two property maps..
                        mLogger.warn("There was a mismatch in the Processor array portion of the ProcessorSummary section, same number of entries but at least 1 mismatch - Lctn=%s", sNodeLctn);
                        bMismatchOccurred = true;
                        break;  // short-circuit the rest of the comparisons.
                    }
                }
                else {
                    // could not find the corresponding loc entry in the db processor array.
                    mLogger.warn("There was a mismatch in the Processor array portion of the ProcessorSummary section, at least 1 difference in Processor loc - Lctn=%s", sNodeLctn);
                    bMismatchOccurred = true;
                    break;  // short-circuit the rest of the comparisons.
                }
            }   // loop through each of the processor entries.
        }   // both the boot msg and the db have the same number of entries in Processor array.
        else if (paBootMsgProcessor.size() == 0) {
            // there is a mismatch in number of entries in the boot msg and db Processor arrays, there are no entries in the boot msg Processor array.
            mLogger.warn("This nodes boot message has a ProcessorSummary section, but it does NOT have any Processor entries - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingProcessorArrayEntries"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            bMismatchOccurred = true;
        }
        else {
            // there is a mismatch in number of entries between the boot msg and db Processor arrays.
            mLogger.warn("There was a mismatch in the Processor array portion of the ProcessorSummary section, different number of entries - Lctn=%s", sNodeLctn);
            bMismatchOccurred = true;
        }

        // Update the db inventory's processor array information (if a mismatch was detected).
        if (bMismatchOccurred) {
            pmDbProcessorSummary.put(ProcessorKey, paBootMsgProcessor);
            pmDbLctnInfo.put("ProcessorSummary", pmDbProcessorSummary);
            // Track that there was a mismatch in the inventory information.
            sbTrackMismatchedInvInfo.append(ProcessorKey + ",");
        }

        return;
    }   // End compareProcessorInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbProcessorSummary, PropertyMap pmBootMsgProcessorSummary, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)


    //--------------------------------------------------------------------------
    // Update the property map with any changes in the specified ProcessorSummary inventory info (if appropriate).
    //--------------------------------------------------------------------------
    private final void updateNodesDbInvInfoPmWithCpuInfoFromBootMsg(String sNodeLctn, PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                       throws PropertyNotExpectedType
    {
        // Process the boot message's ProcessorSummary information.
        if (oBootMsgInvInfo.mPaProcessor != null) {
            // Create and fill in the property map for the BootMsg's ProcessorSummary info.
            PropertyMap pmBootMsgProcessorSummary = new PropertyMap();
            String[] aBootMsgSocketCoresKey = new String[oBootMsgInvInfo.mPaProcessor.size()];  // array to hold the SocketCoresKey values from the BootMsg.
            for (int i=0; i < oBootMsgInvInfo.mPaProcessor.size(); ++i) {
                PropertyMap pmProcessorInfo = oBootMsgInvInfo.mPaProcessor.getMap(i);
                String sProcessorLoc = pmProcessorInfo.getStringOrDefault("loc", null);
                long lProcessorCores = Long.parseLong(pmProcessorInfo.getStringOrDefault("cores", "-99999"));
                PropertyMap pmProcessorTotalCores = new PropertyMap();
                pmProcessorTotalCores.put("value", lProcessorCores);
                String sTempKey = "inventory/cpuinfo/" + sProcessorLoc + "/TotalCores";
                aBootMsgSocketCoresKey[i] = sTempKey;  // save this key value in the array of BootMsg's SocketCoresKey values.
                pmBootMsgProcessorSummary.put(sTempKey, pmProcessorTotalCores);
            }
            pmBootMsgProcessorSummary.put("Processor", oBootMsgInvInfo.mPaProcessor);
            pmBootMsgProcessorSummary.put("TotalSystemCores",  oBootMsgInvInfo.mTotalSystemCores);
            // Get the ProcessorSummary section out of the DB's inventory info.
            PropertyMap pmDbProcessorSummary = pmDbLctnInfo.getMapOrDefault("ProcessorSummary", null);
            if (pmDbProcessorSummary != null) {
                // got the db's ProcessorSummary section - update the db's ProcessorSummary info if there is a mismatch with the boot message.
                boolean bUpdatedDbProcessorSummaryPm = false;
                // Compare the SocketCoresInfo in the boot message inventory info to the existing db inventory info.
                compareSocketCoresInfo(sNodeLctn, pmDbLctnInfo, pmDbProcessorSummary, pmBootMsgProcessorSummary, aBootMsgSocketCoresKey, sbTrackMismatchedInvInfo, dLineTimestamp);
                // Compare the TotalSystemCores field in the boot message inventory info to the existing db inventory info.
                compareTotalSystemCoresInfo(pmDbLctnInfo, pmDbProcessorSummary, pmBootMsgProcessorSummary, sbTrackMismatchedInvInfo);
                // Compare the ProcessorSummary's Process array in the boot message inventory info to the existing db inventory info.
                compareProcessorInfo(sNodeLctn, pmDbLctnInfo, pmDbProcessorSummary, pmBootMsgProcessorSummary, sbTrackMismatchedInvInfo, dLineTimestamp);
            }   // got the db's ProcessorSummary section - update the db's ProcessorSummary info if there is a mismatch.
            else {
                // the db's inventory info does not have a ProcessorSummary section in it.
                mLogger.error("This node has db inventory info, but it does not contain a ProcessorSummary section - Lctn=%s", sNodeLctn);
                logRasEventNoEffectedJob("RasGenAdapterDbInvMissingProcessorSummarySctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                        ,sNodeLctn                         // lctn
                                        ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()          // requesting work item id
                                        );
                // Put the boot message's ProcessorSummary section into the db inventory info for this lctn (from the boot message info).
                pmDbLctnInfo.put("ProcessorSummary", pmBootMsgProcessorSummary);
                // Track that there was a mismatch in the inventory information.
                sbTrackMismatchedInvInfo.append("ProcessorSummary,");
            }
        }   // the boot message does have ProcessorSummary info.
        else {
            // the boot message's inventory info does not have any ProcessorSummary info.
            mLogger.error("This node's boot message has inventory info, but it does NOT have any ProcessorSummary info - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingProcessorSummarySctn"
                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                    ,sNodeLctn                         // lctn
                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                    ,workQueue().workItemId()          // requesting work item id
                    );
        }

        return;
    }   // End updateNodesDbInvInfoPmWithCpuInfoFromBootMsg(PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo)


    //--------------------------------------------------------------------------
    // Update the DbInfo property map, MemorySummary section, with any changes to the Dimm info.
    //--------------------------------------------------------------------------
    private void updateNodesDbInvInfoPmMemorySummarySctn(String sNodeLctn, PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        // Check for TotalSystemMemory and Dimm info within the boot message.
        boolean bMissingDimmInBootMsg = false;
        if (oBootMsgInvInfo.mPaMemory == null) {
            // the boot message's inventory info does not have any Dimm info.
            mLogger.error("This node's boot message has inventory info, but it does NOT have any Dimm/Memory info - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingMemorySummaryDimmArrayEntries"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            bMissingDimmInBootMsg = true;
        }
        boolean bMissingTotSysMemInBootMsg = false;
        if (oBootMsgInvInfo.mTotalSystemMemoryGiB == -99999) {
            // the boot message's inventory info does not have any TotSysMem info.
            mLogger.error("This node's boot message has inventory info, but it does NOT have any TotSysMem info - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingMemorySummaryTotSysMemInfo"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            bMissingTotSysMemInBootMsg = true;
        }
        if ((bMissingDimmInBootMsg) && (bMissingTotSysMemInBootMsg)) {
            // no MemorySummary info at all in the boot msg, might as well short-circuit.
            mLogger.error("This node's boot message has inventory info, but it does NOT have ANY MemorySummary info, so skipping update of memory inventory info - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingAllMemoryInfo"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            return;
        }
        // Get the MemorySummary section out of the DB's inventory info.
        PropertyMap pmDbMemorySummary = pmDbLctnInfo.getMapOrDefault("MemorySummary", new PropertyMap());
        if (pmDbMemorySummary != null) {
            // got the db's MemorySummary section - update the db's MemorySummary info if there is a mismatch with the boot message info.
            // Compare the MemorySummary's Dimm array in the boot message inventory info to the existing db inventory info.
            if (bMissingDimmInBootMsg == false) {
                compareDimmInfo(sNodeLctn, pmDbLctnInfo, pmDbMemorySummary, oBootMsgInvInfo.mPaMemory, sbTrackMismatchedInvInfo, dLineTimestamp);
            }
            else {
                // the boot message inv info does not have a dimm property array.
                compareDimmInfo(sNodeLctn, pmDbLctnInfo, pmDbMemorySummary, new PropertyArray(), sbTrackMismatchedInvInfo, dLineTimestamp);
            }
            // Compare the TotalSystemMemoryGiB in the boot message inventory info to the existing db inventory info.
            compareTotalSystemMemoryGiBInfo(sNodeLctn, pmDbLctnInfo, pmDbMemorySummary, oBootMsgInvInfo.mTotalSystemMemoryGiB, sbTrackMismatchedInvInfo, dLineTimestamp);
        }   // got the db's MemorySummary section - update the db's MemorySummary info if there is a mismatch.
        return;
    }   // End updateNodesDbInvInfoPmMemorySummarySctn(String sNodeLctn, PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)

    //--------------------------------------------------------------------------
    // Compare the TotalSystemMemoryGiBInfo between the boot message inventory info and the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareTotalSystemMemoryGiBInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbMemorySummary, long lBootmsgTotalSystemMemoryGiBValue, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
    {
        boolean bMismatchOccurred = false;
        String sTotalSystemMemoryGiBKey = InventoryKeys.SYSTEM_MEMORY;
        // Get the TotalSystemMemoryGiB section out of the DB's MemorySummary inventory info.
        PropertyMap pmDbTotalSystemMemoryGiB = pmDbMemorySummary.getMapOrDefault(sTotalSystemMemoryGiBKey, null);
        if (pmDbTotalSystemMemoryGiB != null) {
            // got the db's TotalSystemMemoryGiB section - update the db's TotalSystemMemoryGiB info if there is a mismatch with the boot message.
            long lDbTotalSystemMemoryGiBValue = pmDbTotalSystemMemoryGiB.getLongOrDefault("value", -99999);
            if (lDbTotalSystemMemoryGiBValue != -99999) {
                // got the db's TotalSystemMemoryGiB value - update the db's TotalSystemMemoryGiB info if there is a mismatch with the boot message.
                // Check & see if the TotalSystemMemoryGiB value has changed.
                if (lDbTotalSystemMemoryGiBValue != lBootmsgTotalSystemMemoryGiBValue) {
                    // there is a mismatch between the values.
                    mLogger.error("There is a mismatch on TotalSystemMemoryGiB between the DbInvInfo and the BootMsgInvInfo - Lctn=%s, DbTotalSystemMemoryGiBValue=%d, BootmsgTotalSystemMemoryGiBValue=%d",
                                  sNodeLctn, lDbTotalSystemMemoryGiBValue, lBootmsgTotalSystemMemoryGiBValue);
                    bMismatchOccurred = true;
                }
            }   // got the db's TotalSystemMemoryGiB value - update the db's TotalSystemMemoryGiB info if there is a mismatch.
            else {
                // the db inventory info does have a TotalSystemMemoryGiB section in it, but it does not have a TotalSystemMemoryGiB value w/i the section.
                mLogger.error("This node has db inventory info, a MemorySummary section, and the TotalSystemMemoryGiB section, but it does not contain a TotalSystemMemoryGiB value - Lctn=%s, TotalSystemMemoryGiBKey=%s",
                              sNodeLctn, sTotalSystemMemoryGiBKey);
                logRasEventNoEffectedJob("RasGenAdapterDbInvMissingTotSysMemGiBSctnValue"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn + ", TotalSystemMemoryGiBKey=" + sTotalSystemMemoryGiBKey)
                                        ,sNodeLctn                         // lctn
                                        ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()          // requesting work item id
                                        );
                // There is a mismatch.
                bMismatchOccurred = true;
            }   // the db inventory info does have a TotalSystemMemoryGiB section in it, but it does not have a TotalSystemMemoryGiB value.
        }   // got the db's TotalSystemMemoryGiB section - update the db's TotalSystemMemoryGiB info if there is a mismatch with the boot message.
        else {
            // the db inventory info MemorySummary section does not have the TotalSystemMemoryGiB section in it.
            mLogger.error("This node has db inventory info, a MemorySummary section, but it does not contain a TotalSystemMemoryGiB section - Lctn=%s, TotalSystemMemoryGiBKey=%s",
                          sNodeLctn, sTotalSystemMemoryGiBKey);
            logRasEventNoEffectedJob("RasGenAdapterDbInvMissingTotalSystemMemoryGiBSctn"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn + ", TotalSystemMemoryGiBKey=" + sTotalSystemMemoryGiBKey)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            // There is a mismatch.
            pmDbTotalSystemMemoryGiB = new PropertyMap();
            bMismatchOccurred = true;
        }   // the db inventory info MemorySummary section does not have the TotalSystemMemoryGiB section in it.
        // Update the db inventory property map with the new value from the boot message (if appropriate).
        if (bMismatchOccurred) {
            // there is a mismatch - update the db inventory property map with the new value from the boot message.
            pmDbTotalSystemMemoryGiB.put("value", lBootmsgTotalSystemMemoryGiBValue);
            pmDbMemorySummary.put(sTotalSystemMemoryGiBKey, pmDbTotalSystemMemoryGiB);
            pmDbLctnInfo.put("MemorySummary", pmDbMemorySummary);
            sbTrackMismatchedInvInfo.append(sTotalSystemMemoryGiBKey + ",");
        }
        return;
    }   // End compareTotalSystemMemoryGiBInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbMemorySummary, long lBootmsgTotalSystemMemoryGiBValue, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)

    //--------------------------------------------------------------------------
    // Compare the MemorySummary's Dimm array, the boot message inventory info to the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareDimmInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbMemorySummary, PropertyArray paBootMsgDimm, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        final String DimmKey = "Dimm";
        PropertyArray paDbDimm = pmDbMemorySummary.getArrayOrDefault(DimmKey, new PropertyArray());
        // Check & see if they have the same number of entries in the Dimm section.
        boolean bMismatchOccurred = false;
        if (paBootMsgDimm.size() == paDbDimm.size()) {
            // both the boot msg and the db have the same number of entries in Dimm array.
            // Loop through each of the Dimm entries, checking to see if they match.
            for (int iBootMsgDimmCntr=0; iBootMsgDimmCntr < paBootMsgDimm.size(); ++iBootMsgDimmCntr) {
                boolean bFndDimmEntry = false;
                PropertyMap pmBootMsgDimmEntry = paBootMsgDimm.getMap(iBootMsgDimmCntr);
                String sBootMsgDimmLoc = pmBootMsgDimmEntry.getStringOrDefault("loc", null);
                if (sBootMsgDimmLoc == null) {
                    mLogger.warn("This Dimm does not have a loc in the boot message inv info, skipping check for matching Dimm info in db inv info - Lctn=%s, BootMsgDimmEntry=%s",
                                 sNodeLctn, pmBootMsgDimmEntry.toString());
                    continue;
                }
                // Find the corresponding loc entry within the db Dimm array.
                PropertyMap pmDbDimmEntry = new PropertyMap();
                for (int iDbDimmCntr=0; iDbDimmCntr < paDbDimm.size(); ++iDbDimmCntr) {
                    pmDbDimmEntry = paDbDimm.getMap(iDbDimmCntr);
                    String sDbDimmLoc = pmDbDimmEntry.getStringOrDefault("loc", null);
                    if (sDbDimmLoc.equals(sBootMsgDimmLoc)) {
                        // found the corresponding Dimm entry here in the db Dimm array.
                        bFndDimmEntry = true;
                        break;
                    }
                }
                // See if we found the corresponding loc entry in the db Dimm array.
                if (bFndDimmEntry) {
                    // found the corresponding loc entry in the db Dimm array.
                    // Check & see if these two property maps are the same.
                    String sMismatch = comparePropertyMaps(pmBootMsgDimmEntry, pmDbDimmEntry);
                    if (sMismatch != null) {
                        // there is a mismatch between these two property maps..
                        mLogger.warn("There was a mismatch in the Dimm array portion of the MemorySummary section, same number of entries but at least 1 mismatch - Lctn=%s", sNodeLctn);
                        bMismatchOccurred = true;
                        break;  // short-circuit the rest of the comparisons.
                    }
                }
                else {
                    // could not find the corresponding loc entry in the db Dimm array.
                    mLogger.warn("There was a mismatch in the Dimm array portion of the MemorySummary section, at least 1 difference in Dimm loc - Lctn=%s", sNodeLctn);
                    bMismatchOccurred = true;
                    break;  // short-circuit the rest of the comparisons.
                }
            }   // loop through each of the Dimm entries.
        }   // both the boot msg and the db have the same number of entries in Dimm array.
        else if (paBootMsgDimm.size() == 0) {
            // there is a mismatch in number of entries in the boot msg and db Dimm arrays, there are no entries in the boot msg Dimm array.
            mLogger.warn("This nodes boot message has a MemorySummary section, but it does NOT have any Dimm entries - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingDimmArrayEntries"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            bMismatchOccurred = true;
        }
        else {
            // there is a mismatch in number of entries between the boot msg and the db Dimm arrays.
            mLogger.warn("There was a mismatch in the Dimm array portion of the MemorySummary section, different number of entries - Lctn=%s", sNodeLctn);
            bMismatchOccurred = true;
        }

        // Update the db inventory's Dimm array information (if a mismatch was detected).
        if (bMismatchOccurred) {
            pmDbMemorySummary.put(DimmKey, paBootMsgDimm);
            pmDbLctnInfo.put("MemorySummary", pmDbMemorySummary);
            // Track that there was a mismatch in the inventory information.
            sbTrackMismatchedInvInfo.append(DimmKey + ",");
        }

        return;
    }   // End compareDimmInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbMemorySummary, PropertyArray paBootMsgDimm, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)


    //--------------------------------------------------------------------------
    // Update the DbInfo property map, AcceleratorSummary section, with any changes to the Dimm info.
    //--------------------------------------------------------------------------
    private void updateNodesDbInvInfoPmAcceleratorSummarySctn(String sNodeLctn, PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        // Create and fill in the property map for the AcceleratorSummary information out of the boot msg inventory info.
        PropertyMap pmBootMsgAcceleratorSummary = new PropertyMap();
        if (oBootMsgInvInfo.mPaGpu == null) {
            // the boot message's inventory info does not have any Accelerator info.
            mLogger.info("This node's boot message has inventory info, but it does NOT have any Accelerator info - Lctn=%s", sNodeLctn);
        }
        else {
            pmBootMsgAcceleratorSummary.put("Accelerator", oBootMsgInvInfo.mPaGpu);
        }
        // Create and fill in the property map for the AcceleratorSummary information out of the existing DB inventory info.
        PropertyMap pmDbAcceleratorSummary = pmDbLctnInfo.getMapOrDefault("AcceleratorSummary", new PropertyMap());
        // Compare the AcceleratorSummary's Accelerator array in the boot message inventory info to the existing db inventory info.
        compareAcceleratorInfo(sNodeLctn, pmDbLctnInfo, pmDbAcceleratorSummary, pmBootMsgAcceleratorSummary, sbTrackMismatchedInvInfo, dLineTimestamp);
        return;
    }   // End updateNodesDbInvInfoPmAcceleratorSummarySctn()


    //--------------------------------------------------------------------------
    // Compare the AcceleratorSummary's Accelerator array in the boot message inventory info and the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareAcceleratorInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbAcceleratorSummary, PropertyMap pmBootMsgAcceleratorSummary, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        final String AcceleratorKey = "Accelerator";
        PropertyArray paBootMsgAccelerator = pmBootMsgAcceleratorSummary.getArrayOrDefault(AcceleratorKey, new PropertyArray());
        PropertyArray paDbAccelerator = pmDbAcceleratorSummary.getArrayOrDefault(AcceleratorKey, new PropertyArray());
        // Check & see if they have the same number of entries in the Accelerator section.
        boolean bMismatchOccurred = false;
        if (paBootMsgAccelerator.size() == paDbAccelerator.size()) {
            // both the boot msg and the db have the same number of entries in Accelerator array.
            // Loop through each of the Accelerator entries, checking to see if they match.
            for (int iBootMsgAcceleratorCntr=0; iBootMsgAcceleratorCntr < paBootMsgAccelerator.size(); ++iBootMsgAcceleratorCntr) {
                boolean bFndAcceleratorEntry = false;
                PropertyMap pmBootMsgAcceleratorEntry = paBootMsgAccelerator.getMap(iBootMsgAcceleratorCntr);
                String sBootMsgAcceleratorBusAddr = pmBootMsgAcceleratorEntry.getStringOrDefault("busaddr", null);
                if (sBootMsgAcceleratorBusAddr == null) {
                    mLogger.warn("This accelerator does not have a busaddr in the boot message inv info, skipping check for matching accelerator info in db inv info - Lctn=%s, BootMsgAcceleratorEntry=%s",
                                 sNodeLctn, pmBootMsgAcceleratorEntry.toString());
                    continue;
                }
                // Find the corresponding busaddr entry within the db Accelerator array.
                PropertyMap pmDbAcceleratorEntry = new PropertyMap();
                for (int iDbAcceleratorCntr=0; iDbAcceleratorCntr < paDbAccelerator.size(); ++iDbAcceleratorCntr) {
                    pmDbAcceleratorEntry = paDbAccelerator.getMap(iDbAcceleratorCntr);
                    String sDbAcceleratorBusAddr = pmDbAcceleratorEntry.getStringOrDefault("busaddr", null);
                    if (sDbAcceleratorBusAddr.equals(sBootMsgAcceleratorBusAddr)) {
                        // found the corresponding Accelerator entry here in the db Accelerator array.
                        bFndAcceleratorEntry = true;
                        break;
                    }
                }
                // See if we found the corresponding busaddr entry in the db Accelerator array.
                if (bFndAcceleratorEntry) {
                    // found the corresponding busaddr entry in the db Accelerator array.
                    // Check & see if these two property maps are the same.
                    String sMismatch = comparePropertyMaps(pmBootMsgAcceleratorEntry, pmDbAcceleratorEntry);
                    if (sMismatch != null) {
                        // there is a mismatch between these two property maps..
                        mLogger.warn("There was a mismatch in the Accelerator array portion of the AcceleratorSummary section, same number of entries but at least 1 mismatch - Lctn=%s", sNodeLctn);
                        bMismatchOccurred = true;
                        break;  // short-circuit the rest of the comparisons.
                    }
                }
                else {
                    // could not find the corresponding busaddr entry in the db Accelerator array.
                    mLogger.warn("There was a mismatch in the Accelerator array portion of the AcceleratorSummary section, could not find at least 1 Accelerator busaddr - Lctn=%s", sNodeLctn);
                    bMismatchOccurred = true;
                    break;  // short-circuit the rest of the comparisons.
                }
            }   // loop through each of the Accelerator entries.
        }   // both the boot msg and the db have the same number of entries in Accelerator array.
        else if (paBootMsgAccelerator.size() == 0) {
            // there is a mismatch in number of entries in the boot msg and db Accelerator arrays, there are no entries in the boot msg Accelerator array.
            mLogger.warn("The node boot message inv info has an AcceleratorSummary section, but it does NOT have any Accelerator entries - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingAccelSummaryAccelArrayEntries"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            bMismatchOccurred = true;
        }
        else {
            // there is a mismatch in number of entries between the boot msg and db Accelerator arrays.
            mLogger.warn("There was a mismatch in the Accelerator array portion of the AcceleratorSummary section, different number of entries - Lctn=%s", sNodeLctn);
            bMismatchOccurred = true;
        }
        // Update the db inventory's Accelerator array information, with the info from the boot message (if a mismatch was detected).
        if (bMismatchOccurred) {
            pmDbAcceleratorSummary.put(AcceleratorKey, paBootMsgAccelerator);
            pmDbLctnInfo.put("AcceleratorSummary", pmDbAcceleratorSummary);
            // Track that there was a mismatch in the inventory information.
            sbTrackMismatchedInvInfo.append(AcceleratorKey + ",");
        }
        return;
    }   // End compareAcceleratorInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbAcceleratorSummary, PropertyMap pmBootMsgAcceleratorSummary, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)


    //--------------------------------------------------------------------------
    // Update the DbInfo property map, HfiSummary section, with any changes to the Dimm info.
    //--------------------------------------------------------------------------
    private void updateNodesDbInvInfoPmHfiSummarySctn(String sNodeLctn, PropertyMap pmDbLctnInfo, BootMsgInvInfo oBootMsgInvInfo, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        // Create and fill in the property map for the HfiSummary information out of the boot msg inventory info.
        PropertyMap pmBootMsgHfiSummary = new PropertyMap();
        if (oBootMsgInvInfo.mPaHfi == null) {
            // the boot message's inventory info does not have any Hfi info.
            mLogger.info("This node's boot message has inventory info, but it does NOT have any Hfi info - Lctn=%s", sNodeLctn);
        }
        else {
            pmBootMsgHfiSummary.put("Hfi", oBootMsgInvInfo.mPaHfi);
        }
        // Create and fill in the property map for the HfiSummary information out of the existing DB inventory info.
        PropertyMap pmDbHfiSummary = pmDbLctnInfo.getMapOrDefault("HfiSummary", new PropertyMap());
        // Compare the HfiSummary's Hfi array in the boot message inventory info to the existing db inventory info.
        compareHfiInfo(sNodeLctn, pmDbLctnInfo, pmDbHfiSummary, pmBootMsgHfiSummary, sbTrackMismatchedInvInfo, dLineTimestamp);
        return;
    }   // End updateNodesDbInvInfoPmHfiSummarySctn()


    //--------------------------------------------------------------------------
    // Compare the HfiSummary's Hfi array in the boot message inventory info and the existing db inventory info.
    //--------------------------------------------------------------------------
    private void compareHfiInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbHfiSummary, PropertyMap pmBootMsgHfiSummary, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)
                 throws PropertyNotExpectedType
    {
        final String HfiKey = "Hfi";
        PropertyArray paBootMsgHfi = pmBootMsgHfiSummary.getArrayOrDefault(HfiKey, new PropertyArray());
        PropertyArray paDbHfi = pmDbHfiSummary.getArrayOrDefault(HfiKey, new PropertyArray());
        // Check & see if they have the same number of entries in the Hfi section.
        boolean bMismatchOccurred = false;
        if (paBootMsgHfi.size() == paDbHfi.size()) {
            // both the boot msg and the db have the same number of entries in Hfi array.
            // Loop through each of the Hfi entries, checking to see if they match.
            for (int iBootMsgHfiCntr=0; iBootMsgHfiCntr < paBootMsgHfi.size(); ++iBootMsgHfiCntr) {
                boolean bFndHfiEntry = false;
                PropertyMap pmBootMsgHfiEntry = paBootMsgHfi.getMap(iBootMsgHfiCntr);
                String sBootMsgHfiBusAddr = pmBootMsgHfiEntry.getStringOrDefault("busaddr", null);
                if (sBootMsgHfiBusAddr == null) {
                    mLogger.warn("This Hfi does not have a busaddr in the boot message inv info, skipping check for matching Hfi info in db inv info - Lctn=%s, BootMsgHfiEntry=%s",
                                 sNodeLctn, pmBootMsgHfiEntry.toString());
                    continue;
                }
                // Find the corresponding busaddr entry within the db Hfi array.
                PropertyMap pmDbHfiEntry = new PropertyMap();
                for (int iDbHfiCntr=0; iDbHfiCntr < paDbHfi.size(); ++iDbHfiCntr) {
                    pmDbHfiEntry = paDbHfi.getMap(iDbHfiCntr);
                    String sDbHfiBusAddr = pmDbHfiEntry.getStringOrDefault("busaddr", null);
                    if (sDbHfiBusAddr.equals(sBootMsgHfiBusAddr)) {
                        // found the corresponding Hfi entry here in the db Hfi array.
                        bFndHfiEntry = true;
                        break;
                    }
                }
                // See if we found the corresponding busaddr entry in the db Hfi array.
                if (bFndHfiEntry) {
                    // found the corresponding busaddr entry in the db Hfi array.
                    // Check & see if these two property maps are the same.
                    String sMismatch = comparePropertyMaps(pmBootMsgHfiEntry, pmDbHfiEntry);
                    if (sMismatch != null) {
                        // there is a mismatch between these two property maps..
                        mLogger.warn("There was a mismatch in the Hfi array portion of the HfiSummary section, same number of entries but at least 1 mismatch - Lctn=%s", sNodeLctn);
                        bMismatchOccurred = true;
                        break;  // short-circuit the rest of the comparisons.
                    }
                }
                else {
                    // could not find the corresponding busaddr entry in the db Hfi array.
                    mLogger.warn("There was a mismatch in the Hfi array portion of the HfiSummary section, could not find at least 1 Hfi busaddr - Lctn=%s", sNodeLctn);
                    bMismatchOccurred = true;
                    break;  // short-circuit the rest of the comparisons.
                }
            }   // loop through each of the Hfi entries.
        }   // both the boot msg and the db have the same number of entries in Hfi array.
        else if (paBootMsgHfi.size() == 0) {
            // there is a mismatch in number of entries in the boot msg and db Hfi arrays, there are no entries in the boot msg Hfi array.
            mLogger.warn("This nodes boot message has a HfiSummary section, but it does NOT have any Hfi entries - Lctn=%s", sNodeLctn);
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingHfiSummaryHfiArrayEntries"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
            bMismatchOccurred = true;
        }
        else {
            // there is a mismatch in number of entries between the boot msg and db Hfi arrays.
            mLogger.warn("There was a mismatch in the Hfi array portion of the HfiSummary section, different number of entries - Lctn=%s, BootMsgArraySize=%d, DbArraySize=%d,",
                         sNodeLctn, paBootMsgHfi.size(), paDbHfi.size());
            bMismatchOccurred = true;
        }
        // Update the db inventory's Hfi array information, with the info from the boot message (if a mismatch was detected).
        if (bMismatchOccurred) {
            pmDbHfiSummary.put(HfiKey, paBootMsgHfi);
            pmDbLctnInfo.put("HfiSummary", pmDbHfiSummary);
            // Track that there was a mismatch in the inventory information.
            sbTrackMismatchedInvInfo.append(HfiKey + ",");
        }
        return;
    }   // End compareHfiInfo(String sNodeLctn, PropertyMap pmDbLctnInfo, PropertyMap pmDbHfiSummary, PropertyMap pmBootMsgHfiSummary, StringBuilder sbTrackMismatchedInvInfo, Date dLineTimestamp)


    //--------------------------------------------------------------------------
    // Perform the necessary inventory checks for the specified FRU, if there is a mismatch also update the db's inventory information.
    //--------------------------------------------------------------------------
    @Override
    final public void performFrusInvChkAndUpdate(String sNodeLctn, PropertyMap pmDbInvInfo, PropertyMap pmDbLctnInfo, PropertyMap pmDbFruInfo, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)
                      throws IOException, ProcCallException, PropertyNotExpectedType
    {
        StringBuilder sbTrackMismatchedInvInfo = new StringBuilder();  // tracks which inventory info had a mismatch between the db info and the node boot msg info.
        //----------------------------------------------------------
        // Check & see if the boot message has the same fru as is in the db.
        //----------------------------------------------------------
        // Ensure that the boot msg inventory information contains the fru's serial number.
        if ((oBootMsgInvInfo.mBoardSerial != null) && (!oBootMsgInvInfo.mBoardSerial.trim().isEmpty())) {
            // the boot msg does contain the fru's serial number field.
            // Get the sernum info out of the db's fru inventory info.
            PropertyMap pmFruSernumInfo = pmDbFruInfo.getMapOrDefault(InventoryKeys.SERIAL_NUMBER, null);
            if (pmFruSernumInfo == null) {
                // there is no sernum info in the db's fru inventory info.
                // Put the boot msg's inventory info into the db.
                mLogger.error("%s's db inv info does not have the fru sernum info, so we are putting the information that was gathered during boot into the DB", sNodeLctn);
                // Log a ras event - I know that there is no job effected by this occurrence.
                if (!bUsingSynthData) {
                    logRasEventNoEffectedJob("RasGenAdapterDbInvMissingFruSernumSctn"
                                            ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn + ", DbInvInfo='" + pmDbInvInfo + "'")
                                            ,sNodeLctn                        // lctn
                                            ,dLineTimestamp.getTime() * 1000L // time this occurred, in micro-seconds since epoch
                                            ,adapterType()                    // type of the adapter that is requesting/issuing this stored procedure
                                            ,workQueue().workItemId()         // requesting work item id
                                            );
                }
                else {
                    // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                    mLogger.warn("@@@ did not cut the RasGenAdapterDbInvMissingFruSernumSctn ras event (since machine is being run with synthesized data) - %s is active @@@",
                                 sNodeLctn);
                }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                // Replace the node's db inventory info with the boot message's inventory info.
                replaceNodesInvInfoWithInfoFromBootMsg(sNodeLctn, oBootMsgInvInfo, dLineTimestamp, bUsingSynthData);
            }
            else {
                // there is sernum info in the db's fru inventory info.
                String sDbFruSerial = pmFruSernumInfo.getStringOrDefault("value", "-99999");
                // Check & see if the FRU has been replaced (mismatch in fru's serial number).
                if (!sDbFruSerial.equals(oBootMsgInvInfo.mBoardSerial)) {
                    // the fru has been replaced (there is a mismatch on the serial number).
                    mLogger.error("Detected that %s now has a different FRU - was '%s', but now is '%s' - so we are putting the boot msg inventory info into the DB - DbFruInfo=%s!",
                                  sNodeLctn, sDbFruSerial, oBootMsgInvInfo.mBoardSerial, pmDbFruInfo);
                    // Log a ras event - I know that there is no job effected by this occurrence.
                    if (!bUsingSynthData) {
                        logRasEventNoEffectedJob("RasGenAdapterDuringBootInvChkDetectedNewFru"
                                ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn + ", DbFruSerial='" + sDbFruSerial + "', BootMsgFruSerial='" + oBootMsgInvInfo.mBoardSerial +"'")
                                ,sNodeLctn                         // lctn
                                ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                ,workQueue().workItemId()          // requesting work item id
                                );
                    }   // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
                    else {
                        // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                        mLogger.warn("@@@ did not cut the RasGenAdapterDuringBootInvChkDetectedNewFru ras event (since machine is being run with synthesized data) - %s is active @@@",
                                     sNodeLctn);
                    }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                    // Replace the node's db inventory info with the boot message's inventory info.
                    replaceNodesInvInfoWithInfoFromBootMsg(sNodeLctn, oBootMsgInvInfo, dLineTimestamp, bUsingSynthData);
                }   // the fru has been replaced (there is a mismatch on the serial number).
                else {
                    // the fru has NOT been replaced (the db inv and boot msg inv have the same fru serial number).
                    // NOTE: we decided that if the serial number matches that there is no need to check the other fields in the fru section for a mismatch.
                    // Update the db inventory info property map with any changes for the BiosInfo.
                    updateNodesDbInvInfoPmWithBiosInfoFromBootMsg(sNodeLctn, pmDbLctnInfo, oBootMsgInvInfo, sbTrackMismatchedInvInfo, dLineTimestamp);
                    // Update the db inventory info property map with any changes for the ProcessorSummary.
                    updateNodesDbInvInfoPmWithCpuInfoFromBootMsg(sNodeLctn, pmDbLctnInfo, oBootMsgInvInfo, sbTrackMismatchedInvInfo, dLineTimestamp);
                    // Update the db inventory info property map with any changes for the MemorySummary section.
                    updateNodesDbInvInfoPmMemorySummarySctn(sNodeLctn, pmDbLctnInfo, oBootMsgInvInfo, sbTrackMismatchedInvInfo, dLineTimestamp);
                    // Update the db inventory info property map with any changes for the AcceleratorSummary.
                    updateNodesDbInvInfoPmAcceleratorSummarySctn(sNodeLctn, pmDbLctnInfo, oBootMsgInvInfo, sbTrackMismatchedInvInfo, dLineTimestamp);
                    // Update the db inventory info property map with any changes for the HfiSummary.
                    updateNodesDbInvInfoPmHfiSummarySctn(sNodeLctn, pmDbLctnInfo, oBootMsgInvInfo, sbTrackMismatchedInvInfo, dLineTimestamp);
                    // Check & see if we need to update the node's db inventory info using the changed info within pmDbInvInfo
                    // (we only need to update this information if there was a mismatch between what was in the db and what came up during the node boot msg).
                    if (sbTrackMismatchedInvInfo.length() > 0) {
                        // there was a mismatch between the db inv info and the inv info in the node booted msg - update the db inventory info.
                        // Remove the trailing comma (for readability).
                        sbTrackMismatchedInvInfo.deleteCharAt(sbTrackMismatchedInvInfo.length()-1);
                        mLogger.warn("Found a mismatch in the inventory info for %s between the existing db info and the info contained in the node boot msg, so updating the db info with the info gathered during the boot - DetectedInvMismatch='%s'",
                                     sNodeLctn, sbTrackMismatchedInvInfo);
                        // Put the node's updated inventory LctnInfo into the DbInvInfo property map.
                        pmDbInvInfo.put(sNodeLctn, pmDbLctnInfo);
                        // Put the node's updated inventory info in the db.
                        updateNodesInvInfoInDb(dLineTimestamp, sNodeLctn, sbTrackMismatchedInvInfo.toString(),
                                               pmDbInvInfo, oBootMsgInvInfo.mBoardSerial, pmDbFruInfo.getStringOrDefault(InventoryKeys.PRODUCT_NAME, "NoProductNameAvailable"));
                    }   // there was a mismatch between the db inv info and the inv info in the node booted msg - update the db inventory info.
                    else {
                        // there has been no change in the inventory info - no need to update the db inventory info.
                        mLogger.info("Inventory info has not changed - %s", sNodeLctn);
                    }
                }   // the fru has NOT been replaced (the db inv and boot msg inv have same fru serial number).
            }   // there is sernum info in the fru inventory info.
        }   // the boot msg does contain the fru's serial number field.
        else {
            // the boot msg does NOT contain the fru's serial number field.
            //--------------------------------------------------------------
            // The fru serial number field is NOT in the node's boot msg inventory information.
            //--------------------------------------------------------------
            mLogger.error("%s does not have a FRU serial number within the boot msg inventory info", sNodeLctn);
            // Log a ras event - I know that there is no job effected by this occurrence.
            logRasEventNoEffectedJob("RasGenAdapterNodeBootInvMsgMissingFruSerialNumber"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sNodeLctn)
                                    ,sNodeLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
        }   // the boot msg does NOT contain the fru's serial number field.
    }   // End performFrusInvChkAndUpdate(String sNodeLctn, PropertyMap pmDbInvInfo, PropertyMap pmDbLctnInfo, PropertyMap pmDbFruInfo, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)


    //--------------------------------------------------------------------------
    // Compare these 2 PropertyMaps to see whether they have the same set of keys and values.
    // Returns:
    //      Null if strings match each other
    //      String containing the mismatching keys
    //--------------------------------------------------------------------------
    @Override
    final public String comparePropertyMaps(PropertyMap pm1, PropertyMap pm2)
    {
        // Compare the two property maps.
        StringBuilder sbTrackMismatches = new StringBuilder();  // tracks the key values with mismatches between these 2 maps.
        // Create an ArrayList that contains a complete set of the keys that exist within either (or both) property maps.
        Set<String> setKeys2 = pm2.keySet();
        Set<String> setKeys  = pm1.keySet();
        ArrayList<String> alKeys = new ArrayList<>(setKeys);
        for (String sKey2 : setKeys2) {
            if (!alKeys.contains(sKey2))
                alKeys.add(sKey2);
        }
        // Loop through each of the combined set of keys - compare the value from each PropertyMap against each other.
        for (String sKey : alKeys) {
            String sPm1Value  = pm1.getStringOrDefault(sKey, "TempValueIndicatingMap1KeyIsNotPresent");
            String sPm2Value = pm2.getStringOrDefault(sKey, "TempValueIndicatingMap2KeyIsNotPresent");
            if (!sPm1Value.equals(sPm2Value)) {
                // mismatch between the two property maps for this key.
                // Track that there was a mismatch in these 2 maps.
                sbTrackMismatches.append(sKey + ",");
            }
        }
        // Check & see if there were any mismatches between these 2 maps.
        if (sbTrackMismatches.length() > 0) {
            // there was at least 1 mismatch between these 2 maps.
            // Remove the trailing comma from the string (for readability).
            sbTrackMismatches.deleteCharAt(sbTrackMismatches.length()-1);
            return sbTrackMismatches.toString();
        }   // there was at least 1 mismatch between these 2 maps.
        else
            // the maps have same keys and values.
            return null;  // these maps have same keys and values.
    }   // End comparePropertyMaps(PropertyMap pm1, PropertyMap pm2)


    //--------------------------------------------------------------------------
    // Replace the specified node's inventory information in the DB, using the information obtained from the node's boot message.
    //--------------------------------------------------------------------------
    @Override
    final public void replaceNodesInvInfoWithInfoFromBootMsg(String sTempLctn, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)
                      throws IOException, PropertyNotExpectedType
    {
        // // Example of what inventory json should look like.
        // String sExampleNewInventoryAsJson =
        //     "{ " +
        //         "\"" + sTempLctn + "\": { " +
        //             "\"HWInfo\": { " +
        //                 "\"fru/baseboard/board/manufacturer\" : { " +
        //                     "\"value\" : \"" + oBootMsgInvInfo.mBoardManufacturer + "\" " +
        //                 "}, " +
        //                 "\"fru/baseboard/board/product_name\" : { " +
        //                     "\"value\" : \"" + oBootMsgInvInfo.mBoardProductName + "\" " +
        //                 "}, " +
        //                 "\"fru/baseboard/board/serial_number\" : { " +
        //                     "\"value\" : \"" + oBootMsgInvInfo.mBoardSerial + "\" " +
        //                 "}, " +
        //                 "\"fru/baseboard/board/part_number\" : { " +
        //                     "\"value\" : \"" + oBootMsgInvInfo.mBoardPartNumber + "\" " +
        //                 "} " +
        //             "}, " +
        //             "\"BiosVersion\" : { " +
        //                 "\"bios/bios_version\" : { " +
        //                     "\"value\" : \"" + oBootMsgInvInfo.mBiosVersion + "\" " +
        //                 "}, " +
        //             "}, " +
        //             "\"ProcessorSummary\" : { " +
        //                 "\"inventory/cpuinfo/CPU1/TotalCores\" : { " +
        //                     "\"value\" : " + oBootMsgInvInfo.mSocketCores.get(0) + " " +
        //                 "}, " +
        //                 "\"inventory/cpuinfo/CPU2/TotalCores\" : { " +
        //                     "\"value\" : " + oBootMsgInvInfo.mSocketCores.get(1) + " " +
        //                 "}, " +
        //             "}, " +
        //             "\"MemorySummary\": { " +
        //                 "\"inventory\/memory\/TotalSystemMemoryGiB\" : { " +
        //                     "\"value\" : " + oBootMsgInvInfo.mTotalSystemMemoryGiB + " " +
        //                 "}, " +
        //             "} " +
        //         "} " +
        //     "} ";
        // mLogger.fatal("sExampleNewInventoryAsJson = '%s'", sExampleNewInventoryAsJson);

        //----------------------------------------------------------------------
        // Create a property map for the HwInfo section of the inventory information.
        //----------------------------------------------------------------------
        PropertyMap pmBoardManufacturerInfo = new PropertyMap();
        pmBoardManufacturerInfo.put("value", oBootMsgInvInfo.mBoardManufacturer);
        PropertyMap pmBoardProductNameInfo = new PropertyMap();
        pmBoardProductNameInfo.put("value", oBootMsgInvInfo.mBoardProductName);
        PropertyMap pmBoardSernumInfo = new PropertyMap();
        pmBoardSernumInfo.put("value", oBootMsgInvInfo.mBoardSerial);
        PropertyMap pmBoardPartnumInfo = new PropertyMap();
        pmBoardPartnumInfo.put("value", oBootMsgInvInfo.mBoardPartNumber);

        PropertyMap pmFruInfo = new PropertyMap();
        pmFruInfo.put(InventoryKeys.MANUFACTURER, pmBoardManufacturerInfo);
        pmFruInfo.put(InventoryKeys.PRODUCT_NAME, pmBoardProductNameInfo);
        pmFruInfo.put(InventoryKeys.SERIAL_NUMBER, pmBoardSernumInfo);
        pmFruInfo.put(InventoryKeys.PART_NUMBER, pmBoardPartnumInfo);

        //----------------------------------------------------------------------
        // Create and fill in the property map for the MemorySummary information.
        //----------------------------------------------------------------------
        PropertyMap pmTotSysMemGib = new PropertyMap();
        pmTotSysMemGib.put("value", oBootMsgInvInfo.mTotalSystemMemoryGiB);
        PropertyMap pmMemorySummary = new PropertyMap();
        pmMemorySummary.put(InventoryKeys.SYSTEM_MEMORY, pmTotSysMemGib);
        pmMemorySummary.put("Dimm", oBootMsgInvInfo.mPaMemory);

        //----------------------------------------------------------------------
        // Create and fill in the property map for the BiosVersion information.
        //----------------------------------------------------------------------
        PropertyMap pmBios_version = new PropertyMap();
        pmBios_version.put("value", oBootMsgInvInfo.mBiosVersion);
        PropertyMap pmBiosVersionInfo = new PropertyMap();
        pmBiosVersionInfo.put(InventoryKeys.BIOS_VERSION, pmBios_version);
        pmBiosVersionInfo.put("bios/bios_date",    oBootMsgInvInfo.mBiosDate);
        pmBiosVersionInfo.put("bios/bios_vendor",  oBootMsgInvInfo.mBiosVendor);

        //----------------------------------------------------------------------
        // Create and fill in the property map for the ProcessorSummary information.
        //----------------------------------------------------------------------
        PropertyMap pmProcessorSummary = new PropertyMap();
        if (oBootMsgInvInfo.mPaProcessor != null) {
            // got the array of processor entries.
            // Loop through each of the processor entries.
            for (int i=0; i < oBootMsgInvInfo.mPaProcessor.size(); ++i) {
                PropertyMap pmProcessorInfo = oBootMsgInvInfo.mPaProcessor.getMap(i);
                String sProcessorLoc = pmProcessorInfo.getStringOrDefault("loc", null);
                long lProcessorCores = Long.parseLong(pmProcessorInfo.getStringOrDefault("cores", "-99999"));
                PropertyMap pmProcessorTotalCores = new PropertyMap();
                pmProcessorTotalCores.put("value", lProcessorCores);
                pmProcessorSummary.put("inventory/cpuinfo/" + sProcessorLoc + "/TotalCores", pmProcessorTotalCores);
            }
        }
        pmProcessorSummary.put("Processor", oBootMsgInvInfo.mPaProcessor);
        pmProcessorSummary.put("TotalSystemCores",  oBootMsgInvInfo.mTotalSystemCores);

        //----------------------------------------------------------------------
        // Create and fill in the property map for the AcceleratorSummary information.
        //----------------------------------------------------------------------
        PropertyMap pmAcceleratorSummary = new PropertyMap();
        pmAcceleratorSummary.put("Accelerator", oBootMsgInvInfo.mPaGpu);

        //----------------------------------------------------------------------
        // Create and fill in the property map for the HfiSummary information.
        //----------------------------------------------------------------------
        PropertyMap pmHfiSummary = new PropertyMap();
        pmHfiSummary.put("Hfi", oBootMsgInvInfo.mPaHfi);

        //----------------------------------------------------------------------
        // Create and fill in the property map for this lctn's over-arching db inventory info.
        //----------------------------------------------------------------------
        PropertyMap pmLctnInvInfo = new PropertyMap();
        pmLctnInvInfo.put("HWInfo", pmFruInfo);  // put the property map for the Fru section into the property map.
        pmLctnInvInfo.put("BiosVersion", pmBiosVersionInfo);  // put the property map for the BiosVersion section into property map.
        pmLctnInvInfo.put("MemorySummary", pmMemorySummary);  // put the property map for the MemorySummary section into the property map.
        pmLctnInvInfo.put("ProcessorSummary", pmProcessorSummary);  // put the property map for the ProcessorSummary section into the property map.
        pmLctnInvInfo.put("AcceleratorSummary", pmAcceleratorSummary);  // put the property map for the ProcessorSummary section into the property map.
        pmLctnInvInfo.put("HfiSummary", pmHfiSummary);  // put the property map for the ProcessorSummary section into the property map.
        PropertyMap pmDbInvInfo = new PropertyMap();
        pmDbInvInfo.put(sTempLctn, pmLctnInvInfo);

        String sNewInventoryAsJson = mJsonParser.toString(pmDbInvInfo);

        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sTempLctn))
            sTempStoredProcedure = "ComputeNodeReplaced";  // note: misleading stored procedure name, this does not mean that the node was necessarily replaced.
        else
            sTempStoredProcedure = "ServiceNodeReplaced";  // note: misleading stored procedure name, this does not mean that the node was necessarily replaced.
        String sTempBiosInfo = null;
        // Update the node's inventory info - we are simply recording this inventory info change, so no need to wait for ack that this work has completed
        client().callProcedure(createHouseKeepingCallbackLongRtrnValue(adapterType(), adapterName(), sTempStoredProcedure, sTempLctn, workQueue().workItemId()) // asynchronously invoke the procedure
                ,sTempStoredProcedure               // stored procedure name
                ,sTempLctn                          // node's location string
                ,oBootMsgInvInfo.mBoardSerial       // node's new serial number (this could of course be extracted from sNewInventoryAsJson but we do not want to do that w/i stored procedure)
                ,oBootMsgInvInfo.mBoardProductName  // node's fru type
                ,null                               // node's state, null indicates that the node's state should not be changed (leave the state in the db as it is)
                ,sNewInventoryAsJson                // node's new inventory information
                ,sTempBiosInfo                      // node's bios information
                ,dLineTimestamp.getTime() * 1000L   // time that this occurred in microseconds since epoch
                ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                ,workQueue().workItemId()           // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                );
        mLogger.info("Called stored procedure %s, Lctn=%s, Sernum='%s', FruType='%s', State='%s', InvInfo='%s'",
                     sTempStoredProcedure, sTempLctn, oBootMsgInvInfo.mBoardSerial, oBootMsgInvInfo.mBoardProductName, null, sNewInventoryAsJson);
        // Cut the ras event - I know that there is no job effected by this occurrence.
        if (!bUsingSynthData) {
            // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
            logRasEventNoEffectedJob("RasGenAdapterReplacedDbInvInfoWithBootMsgInfo"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sTempLctn + ", NewInventoryInfo='" + sNewInventoryAsJson + "'")
                                    ,sTempLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
        }   // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
        else {
            // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
            mLogger.warn("@@@ did not cut the RasGenAdapterReplacedDbInvInfoWithBootMsgInfo ras event (since machine is being run with synthesized data) - %s @@@",
                         sTempLctn);
        }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
    }   // End replaceNodesInvInfoWithInfoFromBootMsg(String sTempLctn, BootMsgInvInfo oBootMsgInvInfo, Date dLineTimestamp, boolean bUsingSynthData)


    //--------------------------------------------------------------------------
    // Replace the specified node's DB bios information with the information from the node's UcsBios message.
    //--------------------------------------------------------------------------
    @Override
    final public void replaceNodesBiosInfoWithInfoFromBootMsg(String sTempLctn, String sNewBiosInfoAsJson, Date dLineTimestamp, boolean bUsingSynthData)
                      throws IOException
    {
        // Check & see if this is a compute node or a service node.
        String sTempStoredProcedure = null;
        if (isComputeNodeLctn(sTempLctn))
            sTempStoredProcedure = "ComputeNodeSaveBiosInfo";
        else
            sTempStoredProcedure = "ServiceNodeSaveBiosInfo";
        // Update the node's bios info - we are simply recording this bios info change, so no need to wait for ack indicating that this work has completed
        client().callProcedure(createHouseKeepingCallbackLongRtrnValue(adapterType(), adapterName(), sTempStoredProcedure, sTempLctn, workQueue().workItemId()) // asynchronously invoke the procedure
                ,sTempStoredProcedure             // stored procedure name
                ,sTempLctn                        // node's location string
                ,sNewBiosInfoAsJson               // node's new bios information
                ,dLineTimestamp.getTime() * 1000L // time that this occurred in microseconds since epoch
                ,adapterType()                    // type of the adapter that is requesting/issuing this stored procedure
                ,workQueue().workItemId()         // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                );
        mLogger.info("Called stored procedure %s - Lctn=%s, BiosInfo='%s'", sTempStoredProcedure, sTempLctn, sNewBiosInfoAsJson);
        // Cut the ras event - I know that there is no job effected by this occurrence.
        if (!bUsingSynthData) {
            // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
            logRasEventNoEffectedJob("RasGenAdapterUpdatingDbBiosInfoWithBootMsgInfo"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sTempLctn + ", NewBiosInfo='" + sNewBiosInfoAsJson + "'")
                                    ,sTempLctn                         // lctn
                                    ,dLineTimestamp.getTime() * 1000L  // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                     // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()          // requesting work item id
                                    );
        }   // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
        else {
            // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
            mLogger.warn("@@@ did not cut the RasGenAdapterReplacedDbBiosInfoWithBootMsgInfo ras event (since machine is being run with synthesized data) - %s @@@",
                         sTempLctn);
        }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
    }   // End replaceNodesBiosInfoWithInfoFromBootMsg(String sTempLctn, String sNewBiosInfoAsJson, Date dLineTimestamp, boolean bUsingSynthData)


    //--------------------------------------------------------------------------
    // Check that the specified node's inventory info meets any specified constraints for this lctn.
    // Returns:
    //      String that contains information on the constraint(s) that this node does NOT meet.
    //      null = the specified node meets all the specified constraints for this node.
    //--------------------------------------------------------------------------
    // INSERT INTO Constraint VALUES ('S2600BPB', '{ "TotalSystemMemoryGiB" : 2, "TotalCores" : 36, "BiosVersion" : "SE5C620.86B.00.01.0016.020120190930" }', NOW);
    // UPDATE ComputeNode SET ConstraintId='S2600BPB' WHERE Lctn='R0-00-CH00-CB0-CN00';
    public final String checkConstraints(String sTempLctn, boolean bUsingSynthData) {
        StringBuilder sbTrackFailedConstraints = new StringBuilder();  // tracks which constraints were not met by the hardware in this lctn.
        try {
            //----------------------------------------------------------------------
            // Grab the list of constraints specified for this lctn (into a PropertyMap).
            //----------------------------------------------------------------------
            // Check & see if this is a compute node or a service node.
            String sTempStoredProcedure = null;
            String sNodeConstraints = null;
            if (isComputeNodeLctn(sTempLctn))
                sTempStoredProcedure = "ComputeNodeInfo";
            else if (isServiceNodeLctn(sTempLctn))
                sTempStoredProcedure = "ServiceNodeInfo";
            else {
                // unexpected lctn string.
                String sErrorMsg = sTempLctn + " is neither a compute node nor a service node, cannot check its constraints!";
                mLogger.error(sErrorMsg);
                throw new RuntimeException(sErrorMsg);
            }

            // Get the constraint id (if any) out of the node table.
            String sNodeConstraintId = client().callProcedure(sTempStoredProcedure, sTempLctn).getResults()[0].fetchRow(0).getString("ConstraintId");
            if (sNodeConstraintId != null) {
                // Get the actual constraints associated with this constraint id.
                sNodeConstraints = client().callProcedure("ConstraintInfo", sNodeConstraintId).getResults()[0].fetchRow(0).getString("Constraints");
            }
            // Short-circuit if there aren't any constraints specified for this lctn.
            if (sNodeConstraints == null) {
                // there are no constraints for this node.
                mLogger.info("%s does not have any constraints specified for it", sTempLctn);
                return null;  // this node meets all the specified constraints for this lctn.
            }

            // Get the lctn's constraints into a PropertyMap.
            PropertyMap pmConstraintInfo = null;
            try { pmConstraintInfo = mJsonParser.fromString(sNodeConstraints).getAsMap(); }
            catch (ConfigIOParseException e) {
                mLogger.exception(e, "Unable to parse the node's (%s) constraint info as a json string (%s)!", sTempLctn, sNodeConstraints);
                return null;
            }

            // Grab the specific constraints we are interested in out of the constraint property map.
            long lConstraintsTotSysMem = pmConstraintInfo.getLongOrDefault("TotalSystemMemoryGiB", -99999);
            String sConstraintsBiosVersion = pmConstraintInfo.getStringOrDefault("BiosVersion", null);
            long lConstraintsTotCores = pmConstraintInfo.getLongOrDefault("TotalCores", -99999);

            // Short-circuit if none of the constraints we are interested in are in the constraint property map.
            if ((lConstraintsTotSysMem < 0) && (sConstraintsBiosVersion == null) && (lConstraintsTotCores < 0)) {
                mLogger.warn("%s does have constraints specified for it, but none that we enforce", sTempLctn);
                return null;
            }

            //----------------------------------------------------------------------
            // Check & see if the actual inventory info for this FRU matches the specified constraint.
            //----------------------------------------------------------------------
            // Grab the inventory information for the specified lctn.
            InventoryInfoSections oInvInfoSctns = getInventoryInfoSections(sTempLctn, bUsingSynthData);
            if (sConstraintsBiosVersion != null) {
                // constraint was specified for BiosVersion.
                // Get the BiosVersion  section out of the inventory info.
                String sInvBiosVersion = null;
                if (oInvInfoSctns.mPmDbBiosVersion != null) {
                    PropertyMap pmBios_version = oInvInfoSctns.mPmDbBiosVersion.getMapOrDefault(InventoryKeys.BIOS_VERSION, null);
                    if (pmBios_version != null) {
                        sInvBiosVersion = pmBios_version.getStringOrDefault("value", "-99999");
                    }
                }
                // Check & see if they match.
                if (!sConstraintsBiosVersion.equals(sInvBiosVersion)) {
                    // the inventory info does not match the specified constraint.
                    mLogger.error("%s constraint violation - BiosVersion - Constraint(%s), InvInfo(%s)!", sTempLctn, sConstraintsBiosVersion, sInvBiosVersion);
                    logRasEventNoEffectedJob("RasGenAdapterCheckConstraintsViolationBiosVers"
                                            ,("AdapterName=" + adapterName() + ", Constraint=" + sConstraintsBiosVersion + ", InvInfo=" + sInvBiosVersion)
                                            ,sTempLctn                          // node's location
                                            ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                            ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                            ,workQueue().workItemId()           // requesting work item id
                                            );
                    // Track that there was a mismatch between the specified constraint and the BiosVersion inv info.
                    sbTrackFailedConstraints.append("BiosVersion,");
                }
            }
            if (lConstraintsTotSysMem >= 0) {
                // constraint was specified for TotalSystemMemory.
                // Get the TotalSystemMemory section out of the inventory info.
                long lInvTotSysMem = -99999;
                if (oInvInfoSctns.mPmDbMemorySummary != null) {
                    PropertyMap pmDbTotalSystemMemoryGiB = oInvInfoSctns.mPmDbMemorySummary.getMapOrDefault(InventoryKeys.SYSTEM_MEMORY, null);
                    if (pmDbTotalSystemMemoryGiB != null) {
                        lInvTotSysMem = pmDbTotalSystemMemoryGiB.getLongOrDefault("value", -99999);
                    }
                }
                // Check & see if they match.
                if (lConstraintsTotSysMem != lInvTotSysMem) {

                    // the inventory info does not match the specified constraint.
                    mLogger.error("%s constraint violation - TotalSystemMemory - Constraint(%d), InvInfo(%d)!", sTempLctn, lConstraintsTotSysMem, lInvTotSysMem);
                    logRasEventNoEffectedJob("RasGenAdapterCheckConstraintsViolationTotSysMem"
                                            ,("AdapterName=" + adapterName() + ", Constraint=" + lConstraintsTotSysMem + ", InvInfo=" + lInvTotSysMem)
                                            ,sTempLctn                          // node's location
                                            ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                            ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                            ,workQueue().workItemId()           // requesting work item id
                                            );
                    // Track that there was a mismatch between the specified constraint and the TotalSystemMemory inv info.
                    sbTrackFailedConstraints.append("TotalSystemMemory,");
                }
            }
            if (lConstraintsTotCores >= 0) {
                // constraint was specified for TotalCores.
                // Get the TotalCores information out of the inventory info.
                long lInvTotCores = 0L;
                if (oInvInfoSctns.mPmDbProcessorSummary != null) {
                    // Loop through each socket and accumulate the total number of cores on this board.
                    for (int i=0; i <= 4; ++i) {
                        String sSocketCoresKey = "inventory/cpuinfo/CPU" + i + "/TotalCores";
                        PropertyMap pmDbSocketCores = oInvInfoSctns.mPmDbProcessorSummary.getMapOrDefault(sSocketCoresKey, null);
                        if (pmDbSocketCores != null) {
                            lInvTotCores += pmDbSocketCores.getLongOrDefault("value", 0);
                        }
                    }   // loop through checking each of the SocketCores.
                }
                // Check & see if they match.
                if (lConstraintsTotCores != lInvTotCores) {

                    // the inventory info does not match the specified constraint.
                    mLogger.error("%s constraint violation - TotalCores - Constraint(%d), InvInfo(%d)!", sTempLctn, lConstraintsTotCores, lInvTotCores);
                    logRasEventNoEffectedJob("RasGenAdapterCheckConstraintsViolationTotCores"
                                            ,("AdapterName=" + adapterName() + ", Constraint=" + lConstraintsTotCores + ", InvInfo=" + lInvTotCores)
                                            ,sTempLctn                          // node's location
                                            ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                            ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                            ,workQueue().workItemId()           // requesting work item id
                                            );
                    // Track that there was a mismatch between the specified constraint and the TotalCores inv info.
                    sbTrackFailedConstraints.append("TotalCores,");
                }
            }
            // Check & see if there was at least 1 mismatched constraint.
            if (sbTrackFailedConstraints.length() > 0) {
                // there was a constraint mismatch.
                // Remove the trailing comma (for readability).
                sbTrackFailedConstraints.deleteCharAt(sbTrackFailedConstraints.length()-1);
                mLogger.error("%s found a mismatch between the specified constraints and the inventory info - FailedConstraints='%s'",
                              sTempLctn, sbTrackFailedConstraints);
                return sbTrackFailedConstraints.toString();
            }
            else {
                mLogger.info("%s met all the specified constraints", sTempLctn);
                return null;
            }
        }
        catch (Exception e) {
            // Log the exception, generate a RAS event and continue parsing the console and varlog messages
            mLogger.error("Exception occurred!");
            mLogger.error("%s", Adapter.stackTraceToString(e));
            logRasEventNoEffectedJob("RasGenAdapterException"
                                    ,("AdapterName=" + adapterName() + ", Exception=" + e)
                                    ,null                               // node's location
                                    ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()           // requesting work item id
            );
            return null;
        }   // catch
    }   // End checkConstraints(String sTempLctn, boolean bUsingSynthData)


    //--------------------------------------------------------------------------
    // Class containing the property maps for the sections of a lctn's db inventory info.
    //--------------------------------------------------------------------------
    class InventoryInfoSections {
        // Constructor
        InventoryInfoSections(PropertyMap pmDbInv, PropertyMap pmDbLctn, PropertyMap pmDbFru, PropertyMap pmDbBiosVersion, PropertyMap pmDbMemorySummary, PropertyMap pmDbProcessorSummary) {
            mPmDbInv              = pmDbInv;
            mPmDbLctn             = pmDbLctn;
            mPmDbFru              = pmDbFru;              // HWInfo section.
            mPmDbBiosVersion      = pmDbBiosVersion;
            mPmDbMemorySummary    = pmDbMemorySummary;
            mPmDbProcessorSummary = pmDbProcessorSummary;
        }
        // Member data
        PropertyMap mPmDbInv;
        PropertyMap mPmDbLctn;
        PropertyMap mPmDbFru;
        PropertyMap mPmDbBiosVersion;
        PropertyMap mPmDbMemorySummary;
        PropertyMap mPmDbProcessorSummary;
    }  // End class InventoryInfoSections


    //--------------------------------------------------------------------------
    // Get property maps for the various sections of this lctn's db inventory info.
    //--------------------------------------------------------------------------
    InventoryInfoSections getInventoryInfoSections(String sLctn, boolean bUsingSynthData) {
        // Get the node's existing inventory info out of the DB (json string).
        String sDbInvInfo = getNodesInvInfoFromDb(sLctn);
        // Ensure that the specified lctn has inventory info in the db.
        if ((sDbInvInfo == null) || (sDbInvInfo.trim().isEmpty())) {
            // the db does not have any inventory info for this node.
            mLogger.warn("Cannot perform constraint checks as %s does not have any DB inventory info", sLctn);
            // Log a ras event - I know that there is no job effected by this occurrence.
            if (!bUsingSynthData) {
                // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
                logRasEventNoEffectedJob("RasGenAdapterNoDbInvInfo"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sLctn)
                                        ,sLctn                              // lctn
                                        ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()           // requesting work item id
                                        );
            }   // normal flow - we are NOT using synthesized data - go ahead and cut the ras event.
            else {
                // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
                mLogger.warn("@@@ did not cut the RasGenAdapterNoDbInvInfo ras event (since machine is being run with synthesized data) - %s @@@",
                             sLctn);
            }   // NON-normal flow - we ARE using synthesized data - short-circuit the processing, as we don't really want to cut the ras event.
            // Short-circuit as there is not any db inv info.
            return new InventoryInfoSections(null, null, null, null, null, null);
        }

        // Split the node's db inventory info into a property map.
        PropertyMap pmDbInv=null, pmDbLctn=null, pmDbFru=null, pmDbBiosVersion=null, pmDbMemorySummary=null, pmDbProcessorSummary=null;
        try { pmDbInv = mJsonParser.fromString(sDbInvInfo).getAsMap(); }
        catch (ConfigIOParseException e) {
            mLogger.exception(e, "Unable to parse the DB inventory information's json string (%s)!", sDbInvInfo);
            throw new RuntimeException("Unable to parse the DB inventory information's json string (" + e + ")!");
        }

        // Get the node's lctn section out of the DB's inventory info.
        pmDbLctn = pmDbInv.getMapOrDefault(sLctn, null);
        if (pmDbLctn == null) {
            // there is no lctn info in the db inventory info.
            // Put the boot msg's inventory info into the db.
            mLogger.error("%s's db inv info is malformed as it does not have a lctn section", sLctn);
            // Log a ras event - I know that there is no job effected by this occurrence.
            logRasEventNoEffectedJob("RasGenAdapterDbInvNoLctnSctn"
                                    ,("AdapterName=" + adapterName() + ", Lctn=" + sLctn + ", DbInvInfo='" + sDbInvInfo + "'")
                                    ,sLctn                              // lctn
                                    ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                    ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                    ,workQueue().workItemId()           // requesting work item id
                                    );
            // Short-circuit as we can not use this malformed db inv info.
            return new InventoryInfoSections(pmDbInv, null, null, null, null, null);
        }
        else {
            // there is lctn info in the db inventory info.
            // Get the node's fru section out of the DB's inventory info.
            pmDbFru = pmDbLctn.getMapOrDefault("HWInfo", null);
            // Check & see if there is a FRU section in the db's inventory info.
            if (pmDbFru == null) {
                // the db inventory info does NOT contain any FRU section.
                mLogger.error("%s's db inv info does not include a FRU/HWInfo sctn", sLctn);
                // Log a ras event - I know that there is no job effected by this occurrence.
                logRasEventNoEffectedJob("RasGenAdapterDbInvNoHwinfoSctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sLctn + ", DbInvInfo='" + sDbInvInfo + "'")
                                        ,sLctn                              // lctn
                                        ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()           // requesting work item id
                                        );
            }   // the db inventory info does NOT contain any FRU section.
            // Get the node's BiosVersion section out of the DB's inventory info.
            pmDbBiosVersion = pmDbLctn.getMapOrDefault("BiosVersion", null);
            // Check & see if there is a BiosVersion section in the db's inventory info.
            if (pmDbBiosVersion == null) {
                // the db inventory info does NOT contain any BiosVersion section.
                mLogger.error("%s's db inv info does not include a BiosVersion sctn", sLctn);
                // Log a ras event - I know that there is no job effected by this occurrence.
                logRasEventNoEffectedJob("RasGenAdapterDbInvNoBiosVersionSctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sLctn + ", DbInvInfo='" + sDbInvInfo + "'")
                                        ,sLctn                              // lctn
                                        ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()           // requesting work item id
                                        );
            }   // the db inventory info does NOT contain any BiosVersion section.
            // Get the node's MemorySummary section out of the DB's inventory info.
            pmDbMemorySummary = pmDbLctn.getMapOrDefault("MemorySummary", null);
            // Check & see if there is a MemorySummary section in the db's inventory info.
            if (pmDbMemorySummary == null) {
                // the db inventory info does NOT contain any MemorySummary section.
                mLogger.error("%s's db inv info does not include a MemorySummary sctn", sLctn);
                // Log a ras event - I know that there is no job effected by this occurrence.
                logRasEventNoEffectedJob("RasGenAdapterDbInvNoMemSummarySctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sLctn + ", DbInvInfo='" + sDbInvInfo + "'")
                                        ,sLctn                              // lctn
                                        ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()           // requesting work item id
                                        );
            }   // the db inventory info does NOT contain any MemorySummary section.
            // Get the node's ProcessorSummary section out of the DB's inventory info.
            pmDbProcessorSummary = pmDbLctn.getMapOrDefault("ProcessorSummary", null);
            // Check & see if there is a ProcessorSummary section in the db's inventory info.
            if (pmDbProcessorSummary == null) {
                // the db inventory info does NOT contain any ProcessorSummary section.
                mLogger.error("%s's db inv info does not include a ProcessorSummary sctn", sLctn);
                // Log a ras event - I know that there is no job effected by this occurrence.
                logRasEventNoEffectedJob("RasGenAdapterDbInvNoProcessorSummarySctn"
                                        ,("AdapterName=" + adapterName() + ", Lctn=" + sLctn + ", DbInvInfo='" + sDbInvInfo + "'")
                                        ,sLctn                              // lctn
                                        ,System.currentTimeMillis() * 1000L // time this occurred, in micro-seconds since epoch
                                        ,adapterType()                      // type of the adapter that is requesting/issuing this stored procedure
                                        ,workQueue().workItemId()           // requesting work item id
                                        );
            }   // the db inventory info does NOT contain any ProcessorSummary section.
        }   // there is lctn info in the db inventory info.

        return new InventoryInfoSections(pmDbInv, pmDbLctn, pmDbFru, pmDbBiosVersion, pmDbMemorySummary, pmDbProcessorSummary);
    }   // End getInventoryInfoSections(String sLctn)


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
        try (BufferedReader brStdOut = new BufferedReader(new InputStreamReader(process.getInputStream()))){
            while ((sLine = brStdOut.readLine()) != null) {
                ///mLogger.warn("%s - extractListOfWlmNodes - JobId=%s - %s - stdout - '%s'", adapterName(), sJobId, sNodeList, sLine);
                // Add this node to the list of nodes.
                alNodes.add(sLine);
            }
        }

        // Get the command's stderr (i.e. its ErrorStream).
        boolean bErrorOccurredDuringExpansionOfNodeList = false;
        try (BufferedReader brStdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
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
            logRasEventWithEffectedJob("RasWlmFailureExtractingListOfNodes"
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
                logRasEventCheckForEffectedJob("RasGenAdapterExceptionButContinue"
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


    //--------------------------------------------------------------------------
    // Get the lctn string that corresponds to the specified hostname (either a compute node or a service node hostname may be specified).
    //--------------------------------------------------------------------------
    @Override
    public String getLctnFromHostname(String sHostname) {
        // Ensure that a hardware lctn was specified.
        if ((sHostname != null) && (!sHostname.isEmpty())) {
            // there is a specified hostname.
            sHostname = sHostname.toLowerCase();
            String sTempHostname = mapCompNodeHostNameToLctn().get(sHostname);
            if (sTempHostname != null)
                return sTempHostname;
            else {
                sTempHostname = mapServNodeHostNameToLctn().get(sHostname);
                if (sTempHostname != null)
                    return sTempHostname;
                else {
                    mLogger.error("getLctnFromHostname - unexpected type of hardware location was specified - lctn=%s", sHostname);
                    return "UNKNOWN";  // since we don't know how to retrieve the location for this type of hostname, return a value of UNKNOWN.
                }
            }
        }
        return "UNKNOWN";  // since there is no hostname specified, we are unable to give you the corresponding lctn string.
    }   // End getLctnFromHostname(String sHostname)


    //--------------------------------------------------------------------------
    // Check the specified linux PID to see if it is still active.
    //--------------------------------------------------------------------------
    public boolean isPidActive(long lPid) {
        try {
            //----------------------------------------------------------------------
            // Note: ps will return an exit code 0 if the process exists, a 1 if it doesn't.
            //----------------------------------------------------------------------
            // Check & see if the node is already active.
            String sTempCmd = "ps -p " + lPid;
            Process process = Runtime.getRuntime().exec(sTempCmd);
            int iTempExitCode = process.waitFor();
            return (iTempExitCode == 0);  // 0=process exists, 1= process does not.
        }
        catch (Exception e) {
            mLogger.error("isPidActive - exception occurred - pid=%d!", lPid);
            mLogger.error("isPidActive - %s", Adapter.stackTraceToString(e));
            return true;  // since an exception occurred we can't tell you whether or not the pid is still active.
        }
    }   // End isPidActive(long lPid)



}   // End class Adapter
