// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.populate;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.dai.transforms.DefaultLocations;
import org.voltdb.VoltTable;
import org.voltdb.client.*;
import org.voltdb.types.TimestampType;

import java.lang.*;
import java.util.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.text.SimpleDateFormat;

/**
 * Populates the VoltDB database schema for UCS.
 *
 *  Returns:
 *      Positive number is the number of compute nodes that we inserted into this schema.
 *      Negative number indicates that the schema was not empty, so we did not populate it!
 *          Additionally the number of compute nodes that were already in the schema is returned as a negative number.
 *
 *  Sample invocation:
        cp /home/ddreed/dai/install-configs/RasEventMetaData.json ~/.config/ucs/ ; java -Dlog4j.configurationFile=/home/ddreed/dai/install-configs/log4j2.xml com.intel.dai.DefaultOnlineTierDataLoader localhost /home/ddreed/dai/example-files/system/SystemManifest-100K.json /home/ddreed/dai/example-files/system/MachineConfig-100K.json > /tmp/DefaultOnlineTierDataLoader.log 2>&1 &
 *
 */

public class DefaultOnlineTierDataLoader {
    private Map<String, MachineConfigEntry> lctnToMchCfgMap_ = new HashMap<String, MachineConfigEntry>();

    // Constructor
    protected DefaultOnlineTierDataLoader(Logger log) {
        log_ = log;
        jsonParser_ = ConfigIOFactory.getInstance("json");
        if (jsonParser_ == null)  throw new RuntimeException("Failed to create a JSON parser!");
    }   // ctor

    private Integer iNextComputeNodeSeqNum = 0;
    private Integer iNextServiceNodeSeqNum = 0;
    private Integer iNextNonNodeHwSeqNum   = 0;
    private Integer mNumberOfErrors = 0;



    static class MachineConfigEntry {
        MachineConfigEntry(String sBmcAddr, String sIpAddr, String sBmcMacAddr, String sMacAddr, String sBmcHostName,
                           String sHostName, String sBootImageId, String environment, String sAggregator,
                           String sBankLocator, String sModuleLocator, String sSocketDesignation, String sSlot)
        {
            mBmcAddr           = sBmcAddr;
            mIpAddr            = sIpAddr;
            mBmcMacAddr        = sBmcMacAddr;
            mMacAddr           = sMacAddr;
            mBmcHostName       = sBmcHostName;
            mHostName          = sHostName;
            mBootImageId       = sBootImageId;
            this.environment   = environment;
            mAggregator        = sAggregator;
            mBankLocator       = sBankLocator;
            mModuleLocator     = sModuleLocator;
            mSocketDesignation = sSocketDesignation;
            mSlot              = sSlot;
        }
        String mBmcAddr;
        String mIpAddr;
        String mBmcMacAddr;
        String mMacAddr;
        String mBmcHostName;
        String mHostName;
        String mBootImageId;
        String environment;
        String mAggregator;
        String mBankLocator;
        String mModuleLocator;
        String mSocketDesignation;
        String mSlot;
    };  // End class MachineConfigEntry

    static class MachineConfigWiInfo {
        MachineConfigWiInfo(String sQueue, String sTypeOfAdapter, String sWorkToBeDone, String sParms,
                            String sNotifyWhenFinished)
        {
            mQueue              = sQueue;
            mTypeOfAdapter      = sTypeOfAdapter;
            mWorkToBeDone       = sWorkToBeDone;
            mParms              = sParms;
            mNotifyWhenFinished = sNotifyWhenFinished;
        }
        String queue()              { return mQueue; }
        String typeOfAdapter()      { return mTypeOfAdapter; }
        String work()               { return mWorkToBeDone; }
        String parms()              { return mParms; }
        String notifyWhenFinished() { return mNotifyWhenFinished; }
        // Data members
        String mQueue;
        String mTypeOfAdapter;
        String mWorkToBeDone;
        String mParms;
        String mNotifyWhenFinished;
    };  // End class MachineConfigWiInfo

    static class ManifestContent {
        ManifestContent(String sName, String sDefinition)  { mName=sName; mDefinition=sDefinition; }
        String name()        { return mName; }
        String definition()  { return mDefinition; }
        String mName;
        String mDefinition;
    };  // End class ManifestContent


    ClientResponse internalCallProcedure(String procedure, Object... args) throws IOException, ProcCallException {
        return client_.callProcedure(procedure, args);
    }

    private VoltTable[] callProcedure(String procedure, Object... args) throws IOException {
        try {
            log_.debug("Calling procedure:  %s", procedure);
            ClientResponse response = internalCallProcedure(procedure, args);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                log_.error("'%s' from procedure '%s'", response.getStatusString(), procedure);
                throw new RuntimeException("Stored procedure failed!");
            }
            return response.getResults();
        } catch(ProcCallException e) {
            log_.exception(e, "Failed to call the procedure '%s'!", procedure);
            throw new RuntimeException("Stored procedure failed!", e);
        }
    }


    private void traverseToJsonDefinitionAndProcessItsContents(PropertyMap jsonDefinitionsObj, String sPrevLctn, String sLctnSuffix, String sDef) throws IOException
    {
        log_.debug("traverseToJsonDefinitionAndProcessItsContents - sPrevLctn=%s, sLctnSuffix=%s, sDef=%s",
                   sPrevLctn, sLctnSuffix, sDef);
        // Calculate this object's fully qualified location string.
        String sTempLctn;
        if (sPrevLctn.length() == 0)
            sTempLctn = sLctnSuffix;
        else
            sTempLctn = sPrevLctn + "-" + sLctnSuffix;

        // Traverse to the individual definition for this object.
        PropertyMap jsonDefObj = jsonDefinitionsObj.getMapOrDefault(sDef, null);
        //System.out.println("PopulateSchema - Definition=" + sDef + ", Type=" + jsonDefObj.get("type"));

        // Check & see if this is a pertinent type of hardware - if so insert it into the appropriate table.
        if (jsonDefObj == null)  {
            log_.info("traverseToJsonDefinitionAndProcessItsContents - missing definition object, " +
                    "ignoring Lctn=%s, Definition=%s!!!", sTempLctn, sDef);
            return;
        }
        String sTempType = (String)jsonDefObj.get("type");
        if (sTempType == null)  {
            log_.info("traverseToJsonDefinitionAndProcessItsContents - missing definition type, ignoring " +
                    "Lctn=%s, Definition=%s!!!", sTempLctn, sDef);
            return;
        }

        String sTempStoredProcedure;
        String sPertinentInfo;
        switch ( (String)jsonDefObj.get("type") ) {
            case "Rack":
                // Insert this object into the table.
                sTempStoredProcedure = "RACK.insert";
                sPertinentInfo = "Inserting Rack value - Lctn=" + sTempLctn;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,sTempLctn
                                     ,"M"
                                     ,null
                                     ,sDef
                                     ,null
                                     ,System.currentTimeMillis() * 1000L
                                     ,System.currentTimeMillis() * 1000L
                                     ,"G"                                // this piece of hardware's owning subsystem is the "general system"
                                     );
                sTempStoredProcedure = "RACK_HISTORY.insert";
                sPertinentInfo = "Inserting Rack_History value - Lctn=" + sTempLctn;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,sTempLctn
                                     ,"M"
                                     ,null
                                     ,sDef
                                     ,null
                                     ,System.currentTimeMillis() * 1000L
                                     ,System.currentTimeMillis() * 1000L
                                     ,"G"                                // this piece of hardware's owning subsystem is the "general system"
                                     );

                break;
            case "Chassis":
                // Insert this object into the table.
                sTempStoredProcedure = "CHASSIS.insert";
                sPertinentInfo = "Inserting Chassis value - Lctn=" + sTempLctn;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,sTempLctn
                                     ,"M"
                                     ,null
                                     ,sDef
                                     ,null
                                     ,System.currentTimeMillis() * 1000L
                                     ,System.currentTimeMillis() * 1000L
                                     ,"G"                                // this piece of hardware's owning subsystem is the "general system"
                                     );
                sTempStoredProcedure = "CHASSIS_HISTORY.insert";
                sPertinentInfo = "Inserting Chassis_History value - Lctn=" + sTempLctn;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,sTempLctn
                                     ,"M"
                                     ,null
                                     ,sDef
                                     ,null
                                     ,System.currentTimeMillis() * 1000L
                                     ,System.currentTimeMillis() * 1000L
                                     ,"G"                                // this piece of hardware's owning subsystem is the "general system"
                                     );

                break;
            case "Switch":
                // Insert this object into the table.
                sTempStoredProcedure = "SWITCH.insert";
                sPertinentInfo = "Inserting Switch value - Lctn=" + sTempLctn;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,sTempLctn
                                     ,"A"
                                     ,null
                                     ,sDef
                                     ,System.currentTimeMillis() * 1000L
                                     ,System.currentTimeMillis() * 1000L
                                     ,"G"                                // this piece of hardware's owning subsystem is the "general system"
                                     );
                sTempStoredProcedure = "SWITCH_HISTORY.insert";
                sPertinentInfo = "Inserting Switch_History value - Lctn=" + sTempLctn;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,sTempLctn
                                     ,"A"
                                     ,null
                                     ,sDef
                                     ,System.currentTimeMillis() * 1000L
                                     ,System.currentTimeMillis() * 1000L
                                     ,"G"                                // this piece of hardware's owning subsystem is the "general system"
                                     );
                break;
            case "ServiceNode":
                {
                    // Explicitly handle this node's configuration information based on the specified configuration file.
                    String sBmcAddr              = null;
                    String sIpAddr               = null;
                    String sBmcMacAddr           = null;
                    String sMacAddr              = null;
                    String sBmcHostName          = null;
                    String sHostName             = null;
                    String sBootImageId          = null;
                    String sAggregator           = null;
                    String sConstraintId         = null;
                    Long   lProofOfLifeTimestamp = null;
                    MachineConfigEntry sMapEntry = lctnToMchCfgMap_.get(sTempLctn);
                    if (sMapEntry == null)
                        throw new RuntimeException("Unable to load the MachineConfigEntry information for ServiceNode " + sTempLctn + "!");
                    sBmcAddr            = sMapEntry.mBmcAddr;
                    sIpAddr             = sMapEntry.mIpAddr;
                    sBmcMacAddr         = sMapEntry.mBmcMacAddr;
                    sMacAddr            = sMapEntry.mMacAddr;
                    sBmcHostName        = sMapEntry.mBmcHostName;
                    sHostName           = sMapEntry.mHostName;
                    sBootImageId        = sMapEntry.mBootImageId;
                    sAggregator         = sMapEntry.mAggregator;
                    // Insert this object into the table.
                    sTempStoredProcedure = "SERVICENODE.insert";
                    sPertinentInfo = "Inserting ServiceNode - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sTempLctn, iNextServiceNodeSeqNum, sHostName, "M", sBootImageId, sIpAddr, sMacAddr.toLowerCase(),
                                          sBmcAddr, sBmcMacAddr.toLowerCase(), sBmcHostName, System.currentTimeMillis() * 1000L, System.currentTimeMillis() * 1000L, "POPULATE", -1,
                                          "G", sAggregator, null, sConstraintId, lProofOfLifeTimestamp);
                    sTempStoredProcedure = "SERVICENODE_HISTORY.insert";
                    sPertinentInfo = "Inserting ServiceNode_History - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sTempLctn, iNextServiceNodeSeqNum, sHostName, "M", sBootImageId, sIpAddr, sMacAddr.toLowerCase(),
                                          sBmcAddr, sBmcMacAddr.toLowerCase(), sBmcHostName, System.currentTimeMillis() * 1000L, System.currentTimeMillis() * 1000L, "POPULATE", -1,
                                          "G", sAggregator, null, sConstraintId, lProofOfLifeTimestamp);
                    ++iNextServiceNodeSeqNum;
                    // Insert the cache info into the CacheMacAddrToLctn table.
                    sTempStoredProcedure = "CACHEMACADDRTOLCTN.insert";
                    sPertinentInfo = "Inserting CachedMacaddrToLctn - Lctn=" + sTempLctn + ",MacAddr=" + sMacAddr.toLowerCase();
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sMacAddr.toLowerCase(), sTempLctn);
                    // Insert the cache info into the CacheIpAddrToLctn table.
                    sTempStoredProcedure = "CACHEIPADDRTOLCTN.insert";
                    sPertinentInfo = "Inserting CachedIpaddrToLctn - Lctn=" + sTempLctn + ",IpAddr=" + sIpAddr;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sIpAddr, sTempLctn);
                }
                break;
            case "ComputeNode":
                {
                    // Explicitly handle this node's configuration information based on the specified configuration file.
                    String sBmcAddr              = null;
                    String sIpAddr               = null;
                    String sBmcMacAddr           = null;
                    String sMacAddr              = null;
                    String sBmcHostName          = null;
                    String sHostName             = null;
                    String sBootImageId          = null;
                    String environment           = null;
                    String sAggregator           = null;
                    String sConstraintId         = null;
                    Long   lProofOfLifeTimestamp = null;
                    MachineConfigEntry sMapEntry = lctnToMchCfgMap_.get(sTempLctn);
                    if (sMapEntry == null)
                        throw new RuntimeException("Unable to load the MachineConfigEntry information for ComputeNode " + sTempLctn + "!");
                    sBmcAddr            = sMapEntry.mBmcAddr;
                    sIpAddr             = sMapEntry.mIpAddr;
                    sBmcMacAddr         = sMapEntry.mBmcMacAddr;
                    sMacAddr            = sMapEntry.mMacAddr;
                    sBmcHostName        = sMapEntry.mBmcHostName;
                    sHostName           = sMapEntry.mHostName;
                    sBootImageId        = sMapEntry.mBootImageId;
                    environment         = sMapEntry.environment;
                    sAggregator         = sMapEntry.mAggregator;
                    // Insert this object into the ComputeNode table.
                    sTempStoredProcedure = "COMPUTENODE.insert";
                    sPertinentInfo = "Inserting ComputeNode - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sTempLctn, iNextComputeNodeSeqNum, "M", sHostName, sBootImageId, environment, sIpAddr, sMacAddr.toLowerCase(),
                                          sBmcAddr, sBmcMacAddr.toLowerCase(), sBmcHostName, System.currentTimeMillis() * 1000L, System.currentTimeMillis() * 1000L, "POPULATE", -1,
                                          "W", sAggregator, null, "U", sConstraintId, lProofOfLifeTimestamp);
                    sTempStoredProcedure = "COMPUTENODE_HISTORY.insert";
                    sPertinentInfo = "Inserting ComputeNode_History - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sTempLctn, iNextComputeNodeSeqNum, "M", sHostName, sBootImageId, environment, sIpAddr, sMacAddr.toLowerCase(),
                                          sBmcAddr, sBmcMacAddr.toLowerCase(), sBmcHostName, System.currentTimeMillis() * 1000L, System.currentTimeMillis() * 1000L, "POPULATE", -1,
                                          "W", sAggregator, null, "U", sConstraintId, lProofOfLifeTimestamp);
                    ++iNextComputeNodeSeqNum;
                    // Insert the cache info into the CacheMacAddrToLctn table.
                    sTempStoredProcedure = "CACHEMACADDRTOLCTN.insert";
                    sPertinentInfo = "Inserting CachedMacaddrToLctn - Lctn=" + sTempLctn + ",MacAddr=" + sMacAddr.toLowerCase();
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sMacAddr.toLowerCase(), sTempLctn);
                    // Insert the cache info into the CacheIpAddrToLctn table.
                    sTempStoredProcedure = "CACHEIPADDRTOLCTN.insert";
                    sPertinentInfo = "Inserting CachedIpaddrToLctn - Lctn=" + sTempLctn + ",IpAddr=" + sIpAddr;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                          sTempStoredProcedure, sIpAddr, sTempLctn);
                }
                break;
            case "SuperNode":
            case "PowerSupply":
            case "Fan":
                log_.info("traverseToJsonDefinitionAndProcessItsContents - not yet processing a defintion type " +
                        "of %s, currently ignoring these items - Lctn=%s",
                        jsonDefObj.getStringOrDefault("type", null), sTempLctn);
                break;


            case "PDU":
            case "CDU":
            case "ChilledDoor":
            case "CoolingTower":
                {
                    // Explicitly handle this node's configuration information based on the specified configuration file.
                    String sTypeOfHw = (String)jsonDefObj.get("type");
                    MachineConfigEntry sMapEntry = lctnToMchCfgMap_.get(sTempLctn);
                    if (sMapEntry == null)
                        throw new RuntimeException("Unable to load the MachineConfigEntry information for " + sTypeOfHw + " " + sTempLctn + "!");
                    String sIpAddr      = sMapEntry.mIpAddr;
                    String sMacAddr     = sMapEntry.mMacAddr;
                    String sHostName    = sMapEntry.mHostName;
                    String sAggregator  = sMapEntry.mAggregator;
                    // Insert this object into the table.
                    sTempStoredProcedure = "NONNODEHW.insert";
                    sPertinentInfo = "Inserting " + sTypeOfHw + " value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,sTempLctn
                                         ,iNextNonNodeHwSeqNum                  // sequence number
                                         ,sTypeOfHw                             // type of hardware
                                         ,"U"                                   // state - unknown
                                         ,sHostName                             // hostname
                                         ,sIpAddr                               // ip addr
                                         ,sMacAddr                              // mac addr
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         ,"G"                                   // Owner - this piece of hardware's owning subsystem is the "general system"
                                         ,sAggregator                           // Aggregator
                                         ,null                                  // InventoryTimestamp
                                         );
                    sTempStoredProcedure = "NONNODEHW_HISTORY.insert";
                    sPertinentInfo = "Inserting " + sTypeOfHw + "_History value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,sTempLctn
                                         ,iNextNonNodeHwSeqNum                  // sequence number
                                         ,sTypeOfHw                             // type of hardware
                                         ,"U"                                   // state - unknown
                                         ,sHostName                             // hostname
                                         ,sIpAddr                               // ip addr
                                         ,sMacAddr                              // mac addr
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         ,"G"                                   // Owner - this piece of hardware's owning subsystem is the "general system"
                                         ,sAggregator                           // Aggregator
                                         ,null                                  // InventoryTimestamp
                                         );

                    ++iNextNonNodeHwSeqNum;
                }
                break;


            case "Dimm":
                {
                    // Explicitly handle this node's configuration information based on the specified configuration file.
                    String sTypeOfHw = (String)jsonDefObj.get("type");
                    MachineConfigEntry sMapEntry = lctnToMchCfgMap_.get(sTempLctn);
                    if (sMapEntry == null)
                        throw new RuntimeException("Unable to load the MachineConfigEntry information for " + sTypeOfHw + " " + sTempLctn + "!");
                    String sBankLocator   = sMapEntry.mBankLocator;
                    String sModuleLocator = sMapEntry.mModuleLocator;
                    // Determine the number of levels in this sub-fru's fru lctn (the node board).
                    String[] aTemp = sTempLctn.split("-");
                    int iNumLevelInNodeLctn = aTemp.length - 1;
                    // Insert this object into the table.
                    sTempStoredProcedure = "DIMM.insert";
                    sPertinentInfo = "Inserting " + sTypeOfHw + " value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,DefaultLocations.extractFruLocation(sTempLctn, iNumLevelInNodeLctn)
                                         ,sTempLctn
                                         ,"U"                                   // State - unknown
                                         ,0L                                    // SizeMB - size of this dimm in MBs
                                         ,sModuleLocator                        // ModuleLocator
                                         ,sBankLocator                          // BankLocator
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         );
                    sTempStoredProcedure = "DIMM_HISTORY.insert";
                    sPertinentInfo = "Inserting " + sTypeOfHw + "_History value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,DefaultLocations.extractFruLocation(sTempLctn, iNumLevelInNodeLctn)
                                         ,sTempLctn
                                         ,"U"                                   // State - unknown
                                         ,0L                                    // SizeMB - size of this dimm in MBs
                                         ,sModuleLocator                        // ModuleLocator
                                         ,sBankLocator                          // BankLocator
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         );
                }
                break;


            case "Processor":
                {
                    // Explicitly handle this node's configuration information based on the specified configuration file.
                    String sTypeOfHw = (String)jsonDefObj.get("type");
                    MachineConfigEntry sMapEntry = lctnToMchCfgMap_.get(sTempLctn);
                    if (sMapEntry == null)
                        throw new RuntimeException("Unable to load the MachineConfigEntry information for " + sTypeOfHw + " " + sTempLctn + "!");
                    String sSocketDesignation = sMapEntry.mSocketDesignation;
                    // Determine the number of levels in this sub-fru's fru lctn (the node board).
                    String[] aTemp = sTempLctn.split("-");
                    int iNumLevelInNodeLctn = aTemp.length - 1;
                    // Insert this object into the table.
                    sTempStoredProcedure = "PROCESSOR.insert";
                    sPertinentInfo = "Inserting " + sTypeOfHw + " value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,DefaultLocations.extractFruLocation(sTempLctn, iNumLevelInNodeLctn)
                                         ,sTempLctn
                                         ,"U"                                   // State - unknown
                                         ,sSocketDesignation                    // SocketDesignation
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         );
                    sTempStoredProcedure = "PROCESSOR_HISTORY.insert";
                    sPertinentInfo = "Inserting " + sTypeOfHw + "_History value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,DefaultLocations.extractFruLocation(sTempLctn, iNumLevelInNodeLctn)
                                         ,sTempLctn
                                         ,"U"                                   // State - unknown
                                         ,sSocketDesignation                    // SocketDesignation
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         );
                }
                break;


            case "Accelerator":
            case "Hfi":
                {
                    // Explicitly handle this node's configuration information based on the specified configuration file.
                    String sTypeOfHw = (String)jsonDefObj.get("type");
                    if (sTypeOfHw == null)
                        throw new RuntimeException("Unable to determine what type of pcie hw this is for " + sTempLctn + ", missing type designation in json!");
                    MachineConfigEntry sMapEntry = lctnToMchCfgMap_.get(sTempLctn);
                    if (sMapEntry == null)
                        throw new RuntimeException("Unable to load the MachineConfigEntry information for " + sTypeOfHw + " " + sTempLctn + "!");
                    String sSlot = sMapEntry.mSlot;
                    // Determine the number of levels in this sub-fru's fru lctn (the node board).
                    String[] aTemp = sTempLctn.split("-");
                    int iNumLevelInNodeLctn = aTemp.length - 1;
                    // Insert this object into the table.
                    String sTempHistoryStoredProcedure;
                    if (sTypeOfHw.equals("Accelerator")) {
                        sTempStoredProcedure = "ACCELERATOR.insert";
                        sTempHistoryStoredProcedure = "ACCELERATOR_HISTORY.insert";
                    }
                    else {
                        sTempStoredProcedure = "HFI.insert";
                        sTempHistoryStoredProcedure = "HFI_HISTORY.insert";
                    }
                    sPertinentInfo = "Inserting " + sTypeOfHw + " value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,DefaultLocations.extractFruLocation(sTempLctn, iNumLevelInNodeLctn)
                                         ,sTempLctn
                                         ,"U"                                   // State - unknown
                                         ,null                                  // BusAddr
                                         ,sSlot                                 // Slot
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         );
                    sTempStoredProcedure = sTempHistoryStoredProcedure;
                    sPertinentInfo = "Inserting " + sTypeOfHw + "_History value - Lctn=" + sTempLctn;
                    client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                         ,sTempStoredProcedure
                                         ,DefaultLocations.extractFruLocation(sTempLctn, iNumLevelInNodeLctn)
                                         ,sTempLctn
                                         ,"U"                                   // State - unknown
                                         ,null                                  // BusAddr
                                         ,sSlot                                 // Slot
                                         ,System.currentTimeMillis() * 1000L    // DbUpdatedTimestamp
                                         ,System.currentTimeMillis() * 1000L    // LastChgTimestamp
                                         ,"POPULATE"                            // LastChgAdapterType
                                         ,-1                                    // LastChgWorkItemId
                                         );
                }
                break;


            default:
                log_.info("traverseToJsonDefinitionAndProcessItsContents - unhandled definition type of " +
                          "%s - Lctn=%s!!!", jsonDefObj.getStringOrDefault("type", null), sTempLctn);
                break;
        }

        //----------------------------------------------------------------------
        // Loop through the contents of the child objects.
        //----------------------------------------------------------------------
        PropertyArray paChildObjects = jsonDefObj.getArrayOrDefault("content", null);
        if (paChildObjects == null)
            return;

        //----------------------------------------------------------------------
        // Sort the list of children by their name field (so that we will be able to loop through them in the sorted order).
        //----------------------------------------------------------------------
        // Get the child objects into a collection (e.g., ArrayList) so they can be sorted.
        ArrayList<PropertyMap> alChildPropMapObjects = new ArrayList<PropertyMap>();
        for (Object oChildPropMap : paChildObjects) {
            PropertyMap pmChild = (PropertyMap)oChildPropMap;
            alChildPropMapObjects.add(pmChild);
        }

        // Actually sort the collection of child property maps.
        alChildPropMapObjects.sort(new Comparator<PropertyMap>() {
            private static final String KEY = "name";
            @Override
            public int compare(PropertyMap a, PropertyMap b) {
                String sTempKeyA = a.getStringOrDefault(KEY, "");
                String sTempKeyB = b.getStringOrDefault(KEY, "");
                return sTempKeyA.compareTo(sTempKeyB);  // to change sort order just change to use
                                                        // "return -sTempKeyA.compareTo(sTempKeyB);"
            }
        });

        // Loop through the now sorted list of children.
        for(PropertyMap pmChild : alChildPropMapObjects) {
            //System.out.println("PopulateSchema - Definition=" + sDef + ", Type=" + jsonDefObj.get("type") + ", Name=" + pmChild.getStringOrDefault("name", "") + ", Definition=" + pmChild.getStringOrDefault("definition", ""));
            // Recursively call this same method to handle this child definition!
            traverseToJsonDefinitionAndProcessItsContents(jsonDefinitionsObj, sTempLctn,
                    pmChild.getStringOrDefault("name", ""),
                    pmChild.getStringOrDefault("definition", ""));
        }
    }   // End traverseToJsonDefinitionAndProcessItsContents(JSONObject jsonDefinitionsObj, String sPrevLctn, String sLctnSuffix, String sDef)


    static class MyClientStatusListenerExt extends ClientStatusListenerExt {
        MyClientStatusListenerExt(Logger log, AtomicBoolean shuttingDown) { log_ = log; shuttingDown_ = shuttingDown; }

        @Override public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
            if(!shuttingDown_.get())
                log_.error("a connection to the database has been lost (%s:%d) - %s. There are %d connections " +
                        "remaining.", hostname, port, cause, connectionsLeft);
        }
        @Override public void backpressure(boolean status) {
            log_.warn("backpressure from the database is causing a delay in processing requests.");
        }
        @Override public void uncaughtException(ProcedureCallback callback, ClientResponse r, Throwable e) {
            log_.exception(e, "an error has occurred in a callback procedure. Check the following stack " +
                    "trace for details.");
        }
        @Override public void lateProcedureResponse(ClientResponse response, String hostname, int port) {
            log_.error("a procedure that timed out on host %s:%d has now responded.", hostname, port);
        }
        private Logger log_;
        private AtomicBoolean shuttingDown_;
    }


    Client connectToVoltDB(String servers) throws IOException {
        ClientConfig config = new ClientConfig("", "", new MyClientStatusListenerExt(log_, shuttingDown_));
        config.setReconnectOnConnectionLoss(true);
        config.setInitialConnectionRetryInterval(500);
        config.setConnectionResponseTimeout(5000);
        config.setProcedureCallTimeout(15000);
        Client client = ClientFactory.createClient(config);

        // Connect to all the VoltDB servers
        for (String server: servers.split(",")) {
            try {
                client.createConnection(server, Client.VOLTDB_SERVER_PORT);
            }
            catch (Exception uhe) {
                log_.exception(uhe, "connectToDataStore - unable to connect to the database server on %s!",
                        server);
                throw uhe;
            }
        }
        return client;
    }


    static String keywordSubstitutions(String sTempStr, String sUcsLogfileDirectory) {
        if (sTempStr == null)
            return null;

        final String LogfileDirectorySubstitution = "$UCSLOGFILEDIRECTORY";
        while ( true ) {
            int iIndex = sTempStr.indexOf(LogfileDirectorySubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + sUcsLogfileDirectory
                 + sTempStr.substring( (iIndex + LogfileDirectorySubstitution.length()) )
                 ;
        }

        return sTempStr;
    }   // End keywordSubstitutions(String sTempStr, String sUcsLogfileDirectory)


    class MyCallbackForHouseKeepingNoRtrnValue implements ProcedureCallback {
        // Class constructor
        MyCallbackForHouseKeepingNoRtrnValue(String sSpThisIsCallbackFor, String sPertinentInfo) {
            mSpThisIsCallbackFor = sSpThisIsCallbackFor;
            mPertinentInfo  = sPertinentInfo;
        }
        // Member data
        String mSpThisIsCallbackFor;    // stored procedure that this is a callback for (which stored procedure was being done by this callback)
        String mPertinentInfo;          // info that might be pertinent (included in log_ messages)


        String statusByteAsString(byte bStatus) {
            String sStatusByteAsString = null;
            if (bStatus == ClientResponse.USER_ABORT)               { sStatusByteAsString = "USER_ABORT"; }
            else if (bStatus == ClientResponse.CONNECTION_LOST)     { sStatusByteAsString = "CONNECTION_LOST"; }
            else if (bStatus == ClientResponse.CONNECTION_TIMEOUT)  { sStatusByteAsString = "CONNECTION_TIMEOUT"; }
            else if (bStatus == ClientResponse.GRACEFUL_FAILURE)    { sStatusByteAsString = "GRACEFUL_FAILURE"; }
            else if (bStatus == ClientResponse.RESPONSE_UNKNOWN)    { sStatusByteAsString = "RESPONSE_UNKNOWN"; }
            else if (bStatus == ClientResponse.UNEXPECTED_FAILURE)  { sStatusByteAsString = "UNEXPECTED_FAILURE"; }
            else if (bStatus == ClientResponse.SUCCESS)             { sStatusByteAsString = "SUCCESS"; }
            else    { sStatusByteAsString = Byte.toString( bStatus ); }
            return sStatusByteAsString;
        }   // End statusByteAsString(byte bStatus)

        // Member methods
        @Override
        public void clientCallback(ClientResponse response) throws IOException, ProcCallException, InterruptedException {
            // Ensure that the stored procedure was successful.
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                ++mNumberOfErrors;
                log_.error("MyCallbackForHouseKeepingNoRtrnValue - %s callback FAILED - Status=%s, StatusString='%s', PertinentInfo=%s!!!",
                           mSpThisIsCallbackFor, statusByteAsString(response.getStatus()), response.getStatusString(), mPertinentInfo);
            }
            else {
                // stored procedure was successful.
                log_.info("MyCallbackForHouseKeepingNoRtrnValue - %s was successful, PertinentInfo=%s", mSpThisIsCallbackFor, mPertinentInfo);
                // Note: there is no additional processing needed here in this callback, anybody using this callback does not need/want anything else (except that the function that was invoked completed successfully)
            }
        }
    }   // End class MyCallbackForHouseKeepingNoRtrnValue


    public ProcedureCallback createHouseKeepingCallbackNoRtrnValue(String sSpThisIsCallbackFor, String sPertinentInfo) {
        return new MyCallbackForHouseKeepingNoRtrnValue(sSpThisIsCallbackFor, sPertinentInfo);
    }

    public void populateRasEventMetaData(String sRasEventMetaDataFileName)
            throws ProcCallException, PropertyNotExpectedType, IOException {
        PropertyMap pmRasMetaData;
        try (FileReader configReader = new FileReader(sRasEventMetaDataFileName)) {
            pmRasMetaData = jsonParser_.readConfig(configReader).getAsMap();
        } catch (ConfigIOParseException | IOException e) {
            pmRasMetaData = null;
            log_.exception(e);
        }
        if (pmRasMetaData == null)
            throw new RuntimeException("unable to load the RasEventMetaData file, map failed to load!");

        PropertyArray listOfEvents = pmRasMetaData.getArray("Events");
        if (listOfEvents == null)
            throw new RuntimeException(String.format("'%s' has no RAS metadata!", sRasEventMetaDataFileName));
        for (int index = 0; index < listOfEvents.size(); index++) {
            PropertyMap event = listOfEvents.getMap(index);
            if (event == null)
                throw new RuntimeException("'event' cannot be null!");
            // Check & see if the ControlOperation field is really null (note: this is different than a string value of "null").
            String operation = event.getStringOrDefault("ControlOperation", null);
            String sTempStoredProcedure = "RASMETADATA.insert";
            String sPertinentInfo = "Inserting RAS Event Meta Data - DescriptiveName=" + event.getString("DescriptiveName");

            if (operation == null) {
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)
                                     ,sTempStoredProcedure
                                     ,event.getString("DescriptiveName")
                                     ,event.getString("Severity")
                                     ,event.getString("Category")
                                     ,event.getString("Component")
                                     ,null
                                     ,event.getString("Msg")
                                     ,(System.currentTimeMillis() * 1000)
                                     ,event.getStringOrDefault("GenerateAlert", "N")
                                     );
            }
            else {
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)
                                     ,sTempStoredProcedure
                                     ,event.getString("DescriptiveName")
                                     ,event.getString("Severity")
                                     ,event.getString("Category")
                                     ,event.getString("Component")
                                     ,event.getString("ControlOperation")
                                     ,event.getString("Msg")
                                     ,(System.currentTimeMillis() * 1000)
                                     ,event.getStringOrDefault("GenerateAlert", "N")
                                     );
            }
        }
    }


    int doPopulate(String DbServers, String sMnfstFileName, String sMachineConfig, String sRasEventMetaDataFileName) {
        String sTempStoredProcedure;
        int result = 0;
        String sPertinentInfo = null;
        final long TimeDbPopulateStartedMilliSecs = System.currentTimeMillis();
        try {
            log_.info("this adapter instance is using VoltServers=%s, Manifest='%s', MachineConfig='%s'",
                      DbServers, sMnfstFileName, sMachineConfig);
            SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone

            // Connect to the VoltDB servers/nodes - args[0] is a comma separated list of VoltDb servers
            // (e.g., voltdbserver1,voltdbserver2,10.11.12.13 )
            log_.info("connecting to VoltDB servers - %s", DbServers);
            client_ = connectToVoltDB(DbServers);
            log_.info("connected to VoltDB servers - %s", DbServers);

            //---------------------------------------------------------------------
            // Ensure that the schema does not already have information in it
            //---------------------------------------------------------------------
            if (callProcedure("ComputeNodeCount")[0].asScalarLong() > 0L)
                throw new RuntimeException("there is already data in the schema, since the schema is NOT empty we " +
                                           "do not want to proceed with this flow!");

            //----------------------------------------------------------------------
            // Populate RAS event meta data from configuration file.
            //----------------------------------------------------------------------
            populateRasEventMetaData(sRasEventMetaDataFileName);

            //---------------------------------------------------------------------
            // Log RAS event to indicate that the Tier1 data store is really being "populated" (this is a synchronous call, want to ensure that it is indeed logged in the data store).
            //---------------------------------------------------------------------
            sTempStoredProcedure = "RASEVENT.insert";
            String sInstanceData = "Using Manifest=" + sMnfstFileName + ", MachineConfig=" + sMachineConfig;
            client_.callProcedure(sTempStoredProcedure
                                 ,1
                                 ,"RasDbSchemaPopulateFillingInTier1"
                                 ,null
                                 ,null
                                 ,null
                                 ,0
                                 ,null
                                 ,"Y"                   // Done
                                 ,sInstanceData         // InstanceData
                                 ,new TimestampType()
                                 ,new TimestampType()
                                 ,"INITIALIZATION"      // Type of adapter
                                 ,-1
                                 );

            //------------------------------------------------------------------
            // Open up the machine configuration file.
            //------------------------------------------------------------------
            PropertyMap pmMachCfg;
            try (FileReader configReader = new FileReader(sMachineConfig)) {
                pmMachCfg = jsonParser_.readConfig(configReader).getAsMap();
            }
            catch(ConfigIOParseException | IOException e) {
                pmMachCfg = null;
                log_.exception(e);
            }
            if (pmMachCfg == null)
                throw new RuntimeException("unable to load the MachineConfig file, map failed to load!");


            // Insert the list of UCS configuration values into the UcsConfigValue table.
            // Note: this should be the FIRST section processed out of the MachineConfig file, so that its values are available for substitution.
            PropertyArray paUcsCfgEntries = pmMachCfg.getArrayOrDefault("UcsConfigValues", null);
            if (paUcsCfgEntries == null) {
                log_.fatal("Unable to load the MachineConfig file!");
                return -3;
            }
            sTempStoredProcedure = "UCSCONFIGVALUE.insert";
            for (Object oUcsCfgEntry : paUcsCfgEntries) {
                PropertyMap pmUcsCfgEntry = (PropertyMap)oUcsCfgEntry;
                // Actually insert this config value into the UcsConfigValue table.
                sPertinentInfo = "Inserting UCS config value - " + pmUcsCfgEntry.getStringOrDefault("Key", "");
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,pmUcsCfgEntry.getStringOrDefault("Key", "")
                                     ,pmUcsCfgEntry.getStringOrDefault("Value", "")
                                     ,new TimestampType()
                                     );
            }
            // Save away the time that this dbPopulate started in the UcsConfigValue table!
            String sTempKey = "UcsDbPopulateSchemaStartTsMs";
            sPertinentInfo = "Inserting UCS config value - " + sTempKey;
            client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                 ,sTempStoredProcedure
                                 ,sTempKey
                                 ,TimeDbPopulateStartedMilliSecs
                                 ,new TimestampType()
                                 );

            log_.info("Put a value of %d millisecs into %s - corresponds to %s", TimeDbPopulateStartedMilliSecs, sTempKey, sdfSqlDateFormat.format(new Date(TimeDbPopulateStartedMilliSecs)));
            // Sleep for a little bit to ensure that the UCS configuration values are all up to date.
            Thread.sleep(500);  // half a second.
            // Grab the UCS log file directory out of this machine's configuration table (that was just populated above).
            String sUcsLogfileDir = client_.callProcedure("UCSCONFIGVALUE.select", "UcsLogfileDirectory").getResults()[0].fetchRow(0).getString("Value");
            if (sUcsLogfileDir == null)  throw new RuntimeException("Unable to get the UcsLogFileDirectory!");

            // Fill in the list of machine configuration entries for the pertinent sections of the machine configuration.
            final boolean ThrowExcptnIfSctnMissing = true;  final boolean LogWarnMsgIfSctnMissing = false;
            fillInMachineCfgEntries(pmMachCfg, "Nodes", ThrowExcptnIfSctnMissing);  // DO want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "CoolingTowers", LogWarnMsgIfSctnMissing); // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "ChilledDoors", LogWarnMsgIfSctnMissing);  // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "CDUS", LogWarnMsgIfSctnMissing);  // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "PDUS", LogWarnMsgIfSctnMissing);  // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "Dimms", LogWarnMsgIfSctnMissing); // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "Processors", LogWarnMsgIfSctnMissing); // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "Accelerators", LogWarnMsgIfSctnMissing); // do not want to terminate if this section is not present in machine config.
            fillInMachineCfgEntries(pmMachCfg, "Hfis", LogWarnMsgIfSctnMissing); // do not want to terminate if this section is not present in machine config.

            // Fill in the initial list of work items.
            PropertyArray paWorkItemEntries = pmMachCfg.getArrayOrDefault("InitialWorkItems", null);
            if (paWorkItemEntries == null)
                throw new ConfigIOParseException("MachineConfig 'InitialWorkItems' is missing, the incorrect type or null!");
            for (Object oWorkItem : paWorkItemEntries) {
                PropertyMap pmWorkItem = (PropertyMap)oWorkItem;
                // Perform keyword substitution on this work item's parameters.
                String sSubstitutedParms = keywordSubstitutions(pmWorkItem.getStringOrDefault("Parms", ""), sUcsLogfileDir);
                MachineConfigWiInfo tempNewMchCfgWiInfo = new MachineConfigWiInfo((pmWorkItem.getStringOrDefault("Queue", "")).toUpperCase()
                                                                                 ,pmWorkItem.getStringOrDefault("TypeOfAdapter", "")
                                                                                 ,pmWorkItem.getStringOrDefault("WorkToBeDone", "")
                                                                                 ,sSubstitutedParms
                                                                                 ,pmWorkItem.getStringOrDefault("NotifyWhenFinished", "")
                                                                                 );
                alWorkItems_.add(tempNewMchCfgWiInfo);
            }


            // Fill in the initial adapter instances (into the MachineAdapterInstance table).
            PropertyArray paAdapterInstanceEntries = pmMachCfg.getArrayOrDefault("AdapterInstances", null);
            if (paAdapterInstanceEntries == null)
                throw new ConfigIOParseException("MachineConfig 'AdapterInstances' is missing, the incorrect type or null!");
            for (Object oAdapterInstance : paAdapterInstanceEntries) {
                PropertyMap pmAdapterInstance = (PropertyMap)oAdapterInstance;
                //--------------------------------------------------------------
                // Insert this set of adapter instances (into the MachineAdapterInstance and MachineAdapterInstance_History tables).
                //--------------------------------------------------------------
                sTempStoredProcedure = "MACHINEADAPTERINSTANCE.insert";
                sPertinentInfo = "Inserting MachineAdapterInstance value - ServiceNode=" + (pmAdapterInstance.getStringOrDefault("ServiceNode", "")).toUpperCase()
                               + ",AdapterType=" + (pmAdapterInstance.getStringOrDefault("TypeOfAdapter", "")).toUpperCase()
                               + ",NumInstances=" + pmAdapterInstance.getLongOrDefault("NumberOfInstances", -99997L);
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,(pmAdapterInstance.getStringOrDefault("ServiceNode", "")).toUpperCase()
                                     ,(pmAdapterInstance.getStringOrDefault("TypeOfAdapter", "")).toUpperCase()
                                     ,pmAdapterInstance.getLongOrDefault("NumberOfInstances", -99997L)
                                     ,0L
                                     ,pmAdapterInstance.getStringOrDefault("Invocation", "")
                                     ,pmAdapterInstance.getStringOrDefault("LogFile", "")
                                     ,System.currentTimeMillis() * 1000L
                                     );

                sTempStoredProcedure = "MACHINEADAPTERINSTANCE_HISTORY.insert";
                sPertinentInfo = "Inserting MachineAdapterInstance_History value - ServiceNode=" + (pmAdapterInstance.getStringOrDefault("ServiceNode", "")).toUpperCase()
                               + ",AdapterType=" + (pmAdapterInstance.getStringOrDefault("TypeOfAdapter", "")).toUpperCase()
                               + ",NumInstances=" + pmAdapterInstance.getLongOrDefault("NumberOfInstances", -99997L);
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,(pmAdapterInstance.getStringOrDefault("ServiceNode", "")).toUpperCase()
                                     ,(pmAdapterInstance.getStringOrDefault("TypeOfAdapter", "")).toUpperCase()
                                     ,pmAdapterInstance.getLongOrDefault("NumberOfInstances", -99997L)
                                     ,0L
                                     ,pmAdapterInstance.getStringOrDefault("Invocation", "")
                                     ,pmAdapterInstance.getStringOrDefault("LogFile", "")
                                     ,System.currentTimeMillis() * 1000L
                                     );
            }


            //------------------------------------------------------------------
            // Open up the system manifest.
            //------------------------------------------------------------------
            // Read the system manifest file into a string - so we can save its content in the Machine table
            // (so available for later reference).
            String sEntireManifestFile = new Scanner(new File(sMnfstFileName)).useDelimiter("\\A").next();
            // Process the manifest's contents.
            PropertyMap oJsonMnfstObj = null;
            try { oJsonMnfstObj = jsonParser_.fromString(sEntireManifestFile).getAsMap(); }
            catch(ConfigIOParseException e) { /* Defaults to null */ }
            if (oJsonMnfstObj == null)  throw new RuntimeException("Failed to parse the System Manifest file!");

            String sSystemName = oJsonMnfstObj.getStringOrDefault("sysname", "");
            log_.info("SystemName=%s", sSystemName);

            PropertyMap jsonViewsObj = oJsonMnfstObj.getMapOrDefault("views", null);
            if (jsonViewsObj == null)
                throw new ConfigIOParseException("Missing 'views' in System Manifest!");

            PropertyMap jsonFullObj  = jsonViewsObj.getMapOrDefault("Full", null);
            if (jsonFullObj == null)
                throw new ConfigIOParseException("Missing 'Full' in System Manifest!");

            // Traverse to the definitions section of the file.
            PropertyMap jsonDefinitionsObj = jsonFullObj.getMapOrDefault("definitions", null);
            if (jsonDefinitionsObj == null)
                throw new ConfigIOParseException("Missing 'definitions' in System Manifest!");

            // Traverse to the machine room floor section of the file.
            PropertyMap jsonFloorObj = jsonFullObj.getMapOrDefault("floor", null);
            if (jsonFloorObj == null)
                throw new ConfigIOParseException("Missing 'floor' in System Manifest!");

            // Get the list of racks (ManifestContent objects in the machine room floor section of the file) - the
            // object name and object definition.
            ArrayList<ManifestContent> alFloorManifestContentObjects = new ArrayList<ManifestContent>();
            PropertyArray paManifestContentObjects = jsonFloorObj.getArrayOrDefault("content", null);
            if (paManifestContentObjects == null)
                throw new ConfigIOParseException("Missing 'content' in System Manifest!");
            for (Object listOfObject : paManifestContentObjects) {
                PropertyMap object = (PropertyMap)listOfObject;
                alFloorManifestContentObjects.add(new ManifestContent(object.getStringOrDefault("name", null),
                                                  object.getStringOrDefault("definition", null)));
            }
            // Sort the list of racks (floor manifest content entities) by their name field (so that they will
            // be looped through in order).
            alFloorManifestContentObjects.sort((a, b) -> {
                String sTempKeyA = a.name();
                String sTempKeyB = b.name();
                return sTempKeyA.compareTo(sTempKeyB);  // to change sort order use "return -sTempKeyA.compareTo(sTempKeyB);"
            });

            // Loop through the sorted list of racks (manifest content entries in the floor section of the file).
            for(ManifestContent oManifestContent:alFloorManifestContentObjects) {
                String sName = oManifestContent.name();
                String sDef  = oManifestContent.definition();
                // Traverse to this child definition and process its contents.
                // Note: this method will also recurse to handle any children of this child!
                traverseToJsonDefinitionAndProcessItsContents(jsonDefinitionsObj, "", sName, sDef);
            }


            //----------------------------------------------------------------------
            // Populate information about the machine as a whole.
            //----------------------------------------------------------------------
            sTempStoredProcedure = "MACHINE.insert";
            sPertinentInfo = "Inserting Machine info - SystemName=" + sSystemName + ",SystemManifest=" + sMnfstFileName;
            client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                  sTempStoredProcedure, "1", sSystemName, null, null, null, null, "A", null, sMnfstFileName, sEntireManifestFile, (System.currentTimeMillis() * 1000L), "N");
            sTempStoredProcedure = "MACHINE_HISTORY.insert";
            sPertinentInfo = "Inserting Machine_History info - SystemName=" + sSystemName + ",SystemManifest=" + sMnfstFileName;
            client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                  sTempStoredProcedure, "1", sSystemName, null, null, null, null, "A", null, sMnfstFileName, sEntireManifestFile, (System.currentTimeMillis() * 1000L), "N");

            //--------------------------------------------------
            // Insert a special value representing the "next session id" that should be used when creating a new session
            // (this value will be used by the SessionAllocate stored procedure).
            //--------------------------------------------------
            sTempStoredProcedure = "UNIQUEVALUES.insert";
            sPertinentInfo = "Inserting UniqueValue - Entity=NextSessionId,Nextvalue=1";
            client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                  sTempStoredProcedure, "NextSessionId", 1, new TimestampType());  // 1 represents that we want the first session id to start with the value of 1.

            //------------------------------------------------------
            // Insert Diagnostic tool entry for HPC offline Diagnostics.
            //------------------------------------------------------
            sTempStoredProcedure = "DIAG_TOOLS.insert";
            sPertinentInfo = "Inserting DiagTool - DiagToolId=hpc_offline_diagnostics";
            client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                  sTempStoredProcedure, "hpc_offline_diagnostics", "HPC offline diagnostics runs inventory check on system. Runs memory disk checks, runs performance tests",
                                           "node", 1, "T", "T", "T", new TimestampType());

            //------------------------------------------------------
            // Insert Diagnostic list entry for HPC offline Diagnostics.
            //------------------------------------------------------
            sTempStoredProcedure = "DIAG_LIST.insert";
            sPertinentInfo = "Inserting DiagList - DiagListId=RunInbandDiagnostics,DiagToolId=hpc_offline_diagnostics";
            client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                  sTempStoredProcedure, "RunInbandDiagnostics", "hpc_offline_diagnostics",
                                  "Runs all the tests in HPC offline diagnostics", "", new TimestampType());  // Diagnostic List entry for HPC offline Diagnostics tool. No parameters indicate default of all tests run.


            //------------------------------------------------------
            // Insert WorkItems for various service nodes in this machine.
            //------------------------------------------------------
            // Loop through this list of work items - grab all the work items with the same AdapterType value and
            // insert them into the WorkItem table so they will be processed when the adapters start up.
            while ( !alWorkItems_.isEmpty() ) {
                // The array list of work items is not empty, so grab the first entry and use its AdapterType value
                // when looking through the rest of the list.
                String sAdapterType = null;
                long   lNextWorkItemId = 1L;
                // Note: we loop through from the back to the front, in order that we can safely remove entries.
                for (int iIndexCntr = (alWorkItems_.size() - 1); iIndexCntr >= 0; --iIndexCntr) {
                    MachineConfigWiInfo tempMchCfgWiInfo = alWorkItems_.get(iIndexCntr);
                    // Get the comparison value to use when looking for all of the work items with the same AdapterType.
                    if (sAdapterType == null)
                        sAdapterType = tempMchCfgWiInfo.typeOfAdapter();
                    // Check and see if this entry's AdapterType matches the one we are looking for.
                    if (tempMchCfgWiInfo.typeOfAdapter().equals(sAdapterType)) {
                        // we found an entry in the array list with the same AdapterType value.
                        // Put the work item into both the WorkItem and WorkItem_History table.
                        sTempStoredProcedure = "WORKITEM.insert";
                        sPertinentInfo = "Inserting initial WorkItem info - Queue=" + tempMchCfgWiInfo.queue() + ",AdapterType=" + tempMchCfgWiInfo.typeOfAdapter() + ",WorkItemId=" + lNextWorkItemId;
                        client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                              sTempStoredProcedure, tempMchCfgWiInfo.queue(), tempMchCfgWiInfo.typeOfAdapter(), lNextWorkItemId, tempMchCfgWiInfo.work(), tempMchCfgWiInfo.parms(),
                                              tempMchCfgWiInfo.notifyWhenFinished(), "Q", -1, "PopulateSchema", null, null, null,
                                              (System.currentTimeMillis() * 1000L), (System.currentTimeMillis() * 1000L));
                        sTempStoredProcedure = "WORKITEM_HISTORY.insert";
                        sPertinentInfo = "Inserting initial WorkItem_History info - SystemName=" + sSystemName + ",SystemManifest=" + sMnfstFileName;
                        client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                              sTempStoredProcedure, tempMchCfgWiInfo.queue(), tempMchCfgWiInfo.typeOfAdapter(), lNextWorkItemId, tempMchCfgWiInfo.work(), tempMchCfgWiInfo.parms(),
                                              tempMchCfgWiInfo.notifyWhenFinished(), "Q", -1, "PopulateSchema", null, null, null,
                                              (System.currentTimeMillis() * 1000L), (System.currentTimeMillis() * 1000L), null, "T");
                        // Remove this entry out of the array list - so we don't process it again.
                        alWorkItems_.remove(iIndexCntr);
                        // Bump to the next work item id.
                        ++lNextWorkItemId;
                    }
                }   // loop through the list of work items looking for those with matching sAdapterType values.
                // Update the unique value table to have the correct id value to use for the next adapter of this type.
                sTempStoredProcedure = "UNIQUEVALUES.insert";
                sPertinentInfo = "Inserting UniqueValue - Entity=" + sAdapterType + ",Nextvalue=" +  lNextWorkItemId;
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo),  // asynchronously invoke the procedure
                                      sTempStoredProcedure, sAdapterType, lNextWorkItemId, new TimestampType());
            }


            //----------------------------------------------------------------------
            // Insert placeholder/default entry into the BootImage table.
            // - sudo wwsh bootstrap list
            //      BOOTSTRAP NAME            SIZE (M)
            //      3.10.0-327.36.3.el7.x86_64 5.6
            //      sudo wwsh object print -p checksum 3.10.0-327.36.3.el7.x86_64
            //          6f07d63f2cb38e68a32e2f5240b02e0d
            // - sudo wwsh vnfs list
            //      VNFS NAME            SIZE (M) CHROOT LOCATION
            //      centos7.2-1511       89.1     /opt/ohpc/admin/images/centos7.2-1511
            //      centos7.2-1511-PBSpro 107.0    /opt/ohpc/admin/images/centos7.2-1511-PBSpro
            //      - sudo wwsh object print -p checksum centos7.2-1511
            //          e92e91862c9cebc44f14eaaf6172a28e
            //      - sudo wwsh object print -p checksum centos7.2-1511-PBSpro
            //          1bb2e59d7700c86f8b1b87574acb8af0
            // - sudo wwsh provision print
            //----------------------------------------------------------------------
            PropertyMap jsonBootImagesObj = jsonFullObj.getMapOrDefault("boot-images", null);
            if (jsonBootImagesObj == null)
                throw new ConfigIOParseException("Missing 'boot-images' in System Manifest!");
            PropertyArray listOfBootImages = jsonBootImagesObj.getArrayOrDefault("content", null);
            if (listOfBootImages == null)
                throw new ConfigIOParseException("Missing 'content' in System Manifest!");
            // Loop through the list of boot-images.
            for (Object listOfBootImage : listOfBootImages) {
                PropertyMap bootImg = (PropertyMap)listOfBootImage;
                // Insert these boot images into the BootImage and BootImage_History table.
                sTempStoredProcedure = "BOOTIMAGE.insert";
                sPertinentInfo = "Inserting BootImage info - ID=" + (String) bootImg.get("id") + ",Description=" + (String) bootImg.get("description");
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,(String) bootImg.get("id")
                                     ,(String) bootImg.get("description")
                                     ,(String) bootImg.get("BootImageFile")
                                     ,(String) bootImg.get("BootImageChecksum")
                                     ,(String) bootImg.get("BootOptions")
                                     ,(String) bootImg.get("BootStrapImageFile")
                                     ,(String) bootImg.get("BootStrapImageChecksum")
                                     ,"A"
                                     ,(System.currentTimeMillis() * 1000L)
                                     ,(System.currentTimeMillis() * 1000L)
                                     ,"POPULATE"
                                     ,-1
                                     ,bootImg.getStringOrDefault("KernelArgs", "")
                                     ,""
                                     );
                sTempStoredProcedure = "BOOTIMAGE_HISTORY.insert";
                sPertinentInfo = "Inserting BootImage_History info - ID=" + (String) bootImg.get("id") + ",Description=" + (String) bootImg.get("description");
                client_.callProcedure(createHouseKeepingCallbackNoRtrnValue(sTempStoredProcedure, sPertinentInfo)  // asynchronously invoke the procedure
                                     ,sTempStoredProcedure
                                     ,(String) bootImg.get("id")
                                     ,(String) bootImg.get("description")
                                     ,(String) bootImg.get("BootImageFile")
                                     ,(String) bootImg.get("BootImageChecksum")
                                     ,(String) bootImg.get("BootOptions")
                                     ,(String) bootImg.get("BootStrapImageFile")
                                     ,(String) bootImg.get("BootStrapImageChecksum")
                                     ,"A"
                                     ,(System.currentTimeMillis() * 1000L)
                                     ,(System.currentTimeMillis() * 1000L)
                                     ,"POPULATE"
                                     ,-1
                                     ,bootImg.getStringOrDefault("KernelArgs", "")
                                     , ""
                                     );
            }


            //------------------------------------------------------------------
            // Check & see if too many errors occurred during this populate.
            //------------------------------------------------------------------
            if (mNumberOfErrors != 0) {
                String sTempMsg = "Unexpected number of ERRORS occurred during the populate (" + mNumberOfErrors + ")!!!";
                log_.fatal(sTempMsg);
                throw new Exception(sTempMsg);
            }

            String sTempSql = "SELECT COUNT(*) FROM Rack;";
            log_.info("Total number of racks             populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM Chassis;";
            log_.info("Total number of chassis           populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            long lNumOfComputeNodesInDb = callProcedure("ComputeNodeCount")[0].asScalarLong();
            log_.info("Total number of compute nodes     populated = %d", lNumOfComputeNodesInDb);

            sTempSql = "SELECT COUNT(*) FROM ServiceNode;";
            log_.info("Total number of service nodes     populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM Switch;";
            log_.info("Total number of switches          populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM BootImage;";
            log_.info("Total number of boot images       populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM WorkItem;";
            log_.info("Total number of work items        populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM NonNodeHw WHERE Type='CoolingTower';";
            log_.info("Total number of cooling towers    populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM NonNodeHw WHERE Type='CDU';";
            log_.info("Total number of CDUs              populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM NonNodeHw WHERE Type='ChilledDoor';";
            log_.info("Total number of chilled doors     populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM NonNodeHw WHERE Type='PDU';";
            log_.info("Total number of PDUs              populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM Dimm;";
            log_.info("Total number of Dimms             populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM Processor;";
            log_.info("Total number of Processors        populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM Accelerator;";
            log_.info("Total number of Accelerators      populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());

            sTempSql = "SELECT COUNT(*) FROM Hfi;";
            log_.info("Total number of Hfis              populated = %d",
                      callProcedure("@AdHoc", sTempSql)[0].asScalarLong());
        }   // End try
        catch (Exception e) {
            log_.exception(e);
            result = 1;
        } finally {
            shuttingDown_.set(true);
        }
        return result;
    }   // End doPopulate(String[] args)


    void fillInMachineCfgEntries(PropertyMap pmMachCfg, String sSctnName, Boolean bThrowExcptnIfSctnMissing)
         throws ConfigIOParseException
    {
        PropertyArray paEntries = pmMachCfg.getArrayOrDefault(sSctnName, null);
        if (paEntries == null) {
            if (bThrowExcptnIfSctnMissing)
                throw new ConfigIOParseException("MachineConfig section '" + sSctnName + "' is missing - terminating!");
            else
                log_.warn("MachineConfig section '" + sSctnName + "' is missing - assuming there are not supposed to be any in this machine!");
        }
        else {
            for (Object oTemp : paEntries) {
                PropertyMap pm = (PropertyMap)oTemp;
                MachineConfigEntry tempNewCfgEntry = new MachineConfigEntry(pm.getStringOrDefault("BmcAddr", "")
                                                                           ,pm.getStringOrDefault("IpAddr", "")
                                                                           ,pm.getStringOrDefault("BmcMacAddr", "")
                                                                           ,pm.getStringOrDefault("MacAddr", "")
                                                                           ,pm.getStringOrDefault("BmcHostName", "")
                                                                           ,pm.getStringOrDefault("HostName", "")
                                                                           ,pm.getStringOrDefault("BootImageId", "")
                                                                           ,pm.getStringOrDefault("Environment", null)
                                                                           ,pm.getStringOrDefault("Aggregator", "")
                                                                           ,pm.getStringOrDefault("BankLocator", null)
                                                                           ,pm.getStringOrDefault("ModuleLocator", null)
                                                                           ,pm.getStringOrDefault("SocketDesignation", null)
                                                                           ,pm.getStringOrDefault("Slot", null)
                                                                           );
                lctnToMchCfgMap_.put(pm.getStringOrDefault("Lctn", ""), tempNewCfgEntry);
            }
        }
    }


    public static void main(String[] args) {
        Logger log = LoggerFactory.getInstance("INITIALIZATION", DefaultOnlineTierDataLoader.class.getName(),
                "log4j2");
        if(args.length != 4) {
            log.error("starting - Invalid set of arguments were specified, first arg needs to be the " +
                    "VoltDB server, second arg needs to be the System Manifest file name, third arg needs to be " +
                    "the Machine Configuration file name!");
            System.exit(1);
        }
        final DefaultOnlineTierDataLoader obj = new DefaultOnlineTierDataLoader(log);
        // Start up the main processing flow for OnlineTier adapters.
        System.exit(obj.doPopulate(args[0], args[1], args[2], args[3]));
    }   // End main(String[] args)

    private AtomicBoolean shuttingDown_ = new AtomicBoolean(false);
    private Client client_;
    private Logger log_;
    private ConfigIO jsonParser_;
    private ArrayList<MachineConfigWiInfo> alWorkItems_ = new ArrayList<>();
}   // End class DefaultOnlineTierDataLoader
