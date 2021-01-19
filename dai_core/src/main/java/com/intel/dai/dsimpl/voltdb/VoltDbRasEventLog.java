// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.AdapterInformation;
import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.dai.dsapi.RasEventLog;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.client.ProcedureCallback;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class VoltDbRasEventLog implements RasEventLog {

    Client voltClient;
    private Logger logger;
    private String[] servers_;
    private String adapterName;
    private String adapterType;
    // Map that takes a RAS DescriptiveName and gives you the corresponding EventType.
            Map<String, String> mRasDescNameToEventTypeMap = null;
    // "0001000013" is a Ras EventType indicating that the specified descriptive name does not exist in RasEventMetaData
    private static final String NON_DESCRIPTIVE_RAS_EVENT = "0001000013";
    private static final String query = "select EventType, DescriptiveName from RasMetaData;";
    private static final Map<Byte,String> statusMap_ = new HashMap<Byte,String>() {{
        put(ClientResponse.USER_ABORT, "USER_ABORT");
        put(ClientResponse.CONNECTION_LOST, "CONNECTION_LOST");
        put(ClientResponse.CONNECTION_TIMEOUT, "CONNECTION_TIMEOUT");
        put(ClientResponse.GRACEFUL_FAILURE, "GRACEFUL_FAILURE");
        put(ClientResponse.RESPONSE_UNKNOWN, "RESPONSE_UNKNOWN");
        put(ClientResponse.UNEXPECTED_FAILURE, "UNEXPECTED_FAILURE");
        put(ClientResponse.SUCCESS, "SUCCESS");
    }};
    // Map that takes a RAS DescriptiveName and gives you its corresponding MetaData.
    private Map<String, MetaData> mRasDescNameToMetaDataMap = new HashMap<>();

    public VoltDbRasEventLog(String[] servers, IAdapter adapter, Logger logger) throws DataStoreException {
        this(servers, fromOldAdapter(adapter), logger);
    }

    public VoltDbRasEventLog(String[] servers, AdapterInformation adapter, Logger logger) {
        servers_ = servers;
        this.adapterName = adapter.getName();
        this.adapterType = adapter.getType();
        this.logger = logger;
    }

    // RAS event's meta data (this has been copied from com.intel.dai.Adapter)
    public static class MetaData {
        // Member data
        private String  DescriptiveName = "<DescriptiveName>";
        private String  Severity = "<Severity>";
        private String  Category = "<Category>";
        private String  Component = "<Component>";
        private String  ControlOperation = "<ControlOperation>";
        private String  Msg = "<Msg>";
        private boolean GenerateAlert = false;
        // Methods
        public String  descriptiveName()          { return DescriptiveName; }
        public void    descriptiveName(String s)  { DescriptiveName = s; }
        public String  severity()                 { return Severity; }
        public void    severity(String s)         { Severity = s; }
        public String  category()                 { return Category; }
        public void    category(String s)         { Category = s; }
        public String  component()                { return Component; }
        public void    component(String s)        { Component = s; }
        public String  controlOperation()         { return ControlOperation; }
        public void    controlOperation(String s) { ControlOperation = s; }
        public String  msg()                      { return Msg; }
        public void    msg(String s)              { Msg = s; }
        public boolean generateAlert()            { return GenerateAlert; }
        public void    generateAlert(boolean b)   { GenerateAlert = b; }
    }   // End class MetaData

    public VoltDbRasEventLog(String[] servers, String name, String type, Logger logger) {
        assert servers != null:"servers parameter to VoltDbRasEventLog ctor cannot be null";
        servers_ = servers;
        this.adapterName = name;
        this.adapterType = type;
        this.logger = logger;
    }

    public void initialize() {
        voltClient = initializeVoltClient(servers_);
        loadRasMetadata();
    }

    protected Client initializeVoltClient(String[] servers) {
        VoltDbClient.initializeVoltDbClient(servers);
        return VoltDbClient.getVoltClientInstance();
    }


    // TODO: duplicate found in com.intel.dai.Adapter
    @Override
    public void loadRasMetadata() {
        ClientResponse response = null;
        try {
            response = voltClient.callProcedure("@AdHoc", "SELECT * FROM RasMetaData;");
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Unable to retrieve RAS meta data from the data store");
            throw new RuntimeException("Unable to retrieve RAS meta data from the data store", ex);
        }

        VoltTable rasMetaData = response.getResults()[0];
        if (response.getStatus() != ClientResponse.SUCCESS) {
            logger.error("Unable to retrieve RAS meta data from the data store. Client response status: " + response.getStatus());
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
            mRasDescNameToMetaDataMap.put(rasMetaData.getString("DescriptiveName"), metaData);
        }
    }   // End loadRasMetadata()


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
        try {
            logger.info("%s - logRasEventNoEffectedJob - before MyCallbackForHouseKeepingLongRtrnValue ctor - " +
                    "this=%s, sReqAdapterType=%s, adapterName=%s, sTempStoredProcedure=%s, sEventType=%s, " +
                    "lReqWorkItemId=%d", adapterName, this, sReqAdapterType, adapterName, sTempStoredProcedure,
                    sEventType, lReqWorkItemId);  // temporary debug for java.lang.NoClassDefFoundError
            VoltDbCallBackForHouseKeeping oTempCallback = new VoltDbCallBackForHouseKeeping(this, sReqAdapterType, adapterName, sTempStoredProcedure, sEventType, lReqWorkItemId, logger);
            voltClient.callProcedure(oTempCallback         // asynchronously invoke the procedure
                    ,sTempStoredProcedure  // stored procedure name
                    ,sEventType            // type of ras event
                    ,sInstanceData         // event's instance data
                    ,sLctn                 // location that this ras event occurred on
                    ,null                  // null indicates that we KNOW that no job was effected by the "event" whose occurrence caused the logging of this ras event.
                    ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                    ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                    ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
            );
            logger.info("logRasEventNoEffectedJob - called stored procedure %s asynchronously - EventType=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sInstanceData);
        }
        catch (Exception e) {
            logger.error("logRasEventNoEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            logger.exception(e, "logRasEventNoEffectedJob");
        }
    }   // End logRasEventNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)

    // Log the specified ras event used when the caller is certain that NO job was effected by the "event" whose occurrence caused the logging of this ras event.
    // Note: this use synchronous data store updates!
    public void logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        String sTempStoredProcedure = "RasEventStore";
        try {
            logger.info("logRasEventSyncNoEffectedJob - calling stored procedure %s synchronously - EventType=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sInstanceData);
            voltClient.callProcedure(sTempStoredProcedure  // stored procedure name
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
            logger.error("logRasEventSyncNoEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            logger.exception(e, "logRasEventSyncNoEffectedJob");
        }
    }   // End logRasEventSyncNoEffectedJob(String sEventType, String sInstanceData, String sLctn, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    // Log the specified ras event when the caller knows WHICH job was effected by the "event" whose occurrence caused the logging of this ras event.
    @Override
    public void logRasEventWithEffectedJob(String sEventType, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        String sTempStoredProcedure = "RasEventStore";
        try {
            voltClient.callProcedure(new VoltDbCallBackForHouseKeeping(this, sReqAdapterType, adapterName, sTempStoredProcedure, sEventType, lReqWorkItemId, logger)  // asynchronously invoke the procedure
                    ,sTempStoredProcedure  // stored procedure name
                    ,sEventType            // type of ras event
                    ,sInstanceData         // event's instance data
                    ,sLctn                 // location that this ras event occurred on
                    ,sJobId                // jobId of a job that was effected by this ras event
                    ,lTsInMicroSecs        // timestamp that the event which caused this ras event occurred, in micro-seconds since epoch
                    ,sReqAdapterType       // type of the adapter that is requesting/issuing this stored procedure (SessionAllocate)
                    ,lReqWorkItemId        // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure (SessionAllocate)
            );
            logger.info("logRasEventWithEffectedJob - called stored procedure %s asynchronously - EventType=%s, Lctn=%s, JobId=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sJobId, sInstanceData);
        }
        catch (Exception e) {
            logger.error("logRasEventWithEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            logger.exception(e, "logRasEventWithEffectedJob");
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
                logger.info("logRasEventCheckForEffectedJob - no lctn was specified on this invocation so we are invoking logRasEventNoEffectedJob() instead - EventType=%s, Lctn=%s, InstanceData='%s'", sEventType, sLctn, sInstanceData);
                logRasEventNoEffectedJob(sEventType, sInstanceData, sLctn, lTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                return;
            }
            voltClient.callProcedure(new VoltDbCallBackForHouseKeeping(this, sReqAdapterType, adapterName, sTempStoredProcedure, sEventType, lReqWorkItemId, logger)  // asynchronously invoke the procedure
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
            logger.info("logRasEventCheckForEffectedJob - called stored procedure %s asynchronously - EventType=%s, Lctn=%s, InstanceData='%s'", sTempStoredProcedure, sEventType, sLctn, sInstanceData);
        }
        catch (Exception e) {
            logger.error("logRasEventCheckForEffectedJob - exception occurred trying to log ras event %s!", sEventType);
            logger.exception(e, "logRasEventCheckForEffectedJob");
        }
    }

    @Override
    public void setRasEventAssociatedJobID(String jobID, String rasEventType, long rasEventID) throws IOException {
        String sTempStoredProcedure = "RasEventUpdateJobId";
        voltClient.callProcedure(clientResponse -> {
            if(clientResponse.getStatus() != ClientResponse.SUCCESS) {
                String rasType =  "RasGenAdapterMyCallbackForHouseKeepingNoRtrnValueFailed";
                String error = statusMap_.getOrDefault(clientResponse.getStatus(), "UNKNOWN_ERROR");
                logRasEventNoEffectedJob(rasType, "AdapterName=" + adapterName + ", SpThisIsCallbackFor=" +
                        sTempStoredProcedure + ", " + "StatusString=" + error, null, System.currentTimeMillis() * 1000L,
                        adapterType, -1L);
            }
        }, "RasEventUpdateJobId", jobID, rasEventType, rasEventID);
    }

    private long getNsTimestamp() {
        Instant point = Instant.now();
        return (point.getEpochSecond() * 1000000000L) + point.getNano();
    }

    private static AdapterInformation fromOldAdapter(IAdapter adapter) throws DataStoreException {
        return VoltDbWorkQueue.fromOldAdapter(adapter);
    }

    // This method ensures that the specified RAS event descriptive name is valid/known.
    @Override
    public String ensureRasDescrNameIsValid(String sDescriptiveName, long workItemId) {
        // Ensure that a known/valid descriptive name was specified.
        MetaData oMetaData = mRasDescNameToMetaDataMap.get(sDescriptiveName);
        if (oMetaData != null) {
            return sDescriptiveName;
        }
        else {
            // got an invalid/unknown RAS descriptive name.
            logRasEventNoEffectedJob("RasGenAdapterMissingRasEventDescrName"
                                    ,("AdapterName=" + adapterName + ", DescriptiveName=" + sDescriptiveName)
                                    ,null                               // lctn
                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,adapterType                       // type of the adapter that is requesting/issuing this invocation
                                    ,workItemId                         // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                    );
            return "RasUnknownEvent"; // "RasUnknownEvent" is used as a RAS DescriptiveName indicating that the specified descriptive name was unknown/invalid.
        }
    }   // End ensureRasDescrNameIsValid(String sDescriptiveName)
}
