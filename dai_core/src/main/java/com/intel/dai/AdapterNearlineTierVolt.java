// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.voltdb.client.*;
import org.voltdb.VoltTable;
import java.lang.*;
import java.util.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import com.intel.properties.*;
import com.intel.config_io.*;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.*;

/**
 * AdapterNearlineTierVolt for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
 *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 * Example invocation:
 *      java AdapterNearlineTierVolt voltdbserver1,voltdbserver2,10.11.12.13  (or java AdapterNearlineTierVolt  - this will default to using localhost)
 */
public class AdapterNearlineTierVolt extends AdapterNearlineTier {
    final String TimestampPrefix     = "Timestamp=";
    final String IntervalIdPrefix    = "IntervalId=";
    final String AmqpMessageIdPrefix = "AmqpMessageId=";
    final String TableNamePrefix     = "TableName=";

    // Constructor
    AdapterNearlineTierVolt(Logger logger) throws IOException, TimeoutException {
        super(logger);
        mPrevAmqpMessageId = 0L;
        mAmqpDataReceiverMsgConsumer = null;
        mEntryNumber_ACCELERATOR_HISTORY            = -99999L;
        mEntryNumber_ADAPTER_HISTORY                = -99999L;
        mEntryNumber_BOOTIMAGE_HISTORY              = -99999L;
        mEntryNumber_CHASSIS_HISTORY                = -99999L;
        mEntryNumber_COMPUTENODE_HISTORY            = -99999L;
      /*mEntryNumber_CONSTRAINT                     = -99999L;*/  // Note: The Constraint table does not need a entry number field, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        mEntryNumber_DIAG_HISTORY                   = -99999L;
      /*mEntryNumber_DIAG_LIST                      = -99999L;*/  // Note: The Diag_List table does not need a entry number field, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
      /*mEntryNumber_DIAGRESULTS                    = -99999L;*/  // Note: The DiagResults table does not need a entry number field, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
      /*mEntryNumber_DIAG_TOOLS                     = -99999L;*/  // Note: The Diag_Tools table does not need a entry number field, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        mEntryNumber_DIMM_HISTORY                   = -99999L;
        mEntryNumber_FABRICTOPOLOGY_HISTORY         = -99999L;
        mEntryNumber_HFI_HISTORY                    = -99999L;
        mEntryNumber_JOB_HISTORY                    = -99999L;
        mEntryNumber_JOBSTEP_HISTORY                = -99999L;
        mEntryNumber_LUSTRE_HISTORY                 = -99999L;
        mEntryNumber_MACHINE_HISTORY                = -99999L;
        mEntryNumber_MACHINEADAPTERINSTANCE_HISTORY = -99999L;
        mEntryNumber_NODEINVENTORY_HISTORY          = -99999L;
        mEntryNumber_NONNODEHW_HISTORY              = -99999L;
        mEntryNumber_NONNODEHWINVENTORY_HISTORY     = -99999L;
        mEntryNumber_PROCESSOR_HISTORY              = -99999L;
        mEntryNumber_RACK_HISTORY                   = -99999L;
        mEntryNumber_RASEVENT                       = -99999L;
        mEntryNumber_RASMETADATA                    = -99999L;
        mEntryNumber_REPLACEMENT_HISTORY            = -99999L;
        mEntryNumber_SERVICEOPERATION_HISTORY       = -99999L;
        mEntryNumber_SERVICENODE_HISTORY            = -99999L;
        mEntryNumber_SWITCH_HISTORY                 = -99999L;
      /*mEntryNumber_UCSCONFIGVALUE                 = -99999L;*/  // Note: The UcsConfigValue table does not need a entry number field, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
      /*mEntryNumber_UNIQUEVALUES                   = -99999L;*/  // Note: The UniqueValues table does not need a entry number field, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        mEntryNumber_WLMRESERVATION_HISTORY         = -99999L;
        mEntryNumber_WORKITEM_HISTORY               = -99999L;
        mEntryNumber_RAWHWINVENTORY_HISTORY         = -99999L;
    }   // ctor


    DataReceiverAmqp createDataReceiver(String host) throws IOException, TimeoutException {
        return new DataReceiverAmqp(host, adapter, log_, workQueue.workItemId());
    }

    // Member Data
    private long        mPrevAmqpMessageId;
    private Consumer    mAmqpDataReceiverMsgConsumer;                // logic that is invoked for each message that comes in from the DataMover.
    private long        mEntryNumber_ACCELERATOR_HISTORY;            // the last used entry number for the Tier2_ACCELERATOR_HISTORY table.
    private long        mEntryNumber_ADAPTER_HISTORY;                // the last used entry number for the Tier2_ADAPTER_HISTORY table.
    private long        mEntryNumber_BOOTIMAGE_HISTORY;              // the last used entry number for the Tier2_BOOTIMAGE_HISTORY table.
    private long        mEntryNumber_CHASSIS_HISTORY;                // the last used entry number for the Tier2_CHASSIS_HISTORY table.
    private long        mEntryNumber_COMPUTENODE_HISTORY;            // the last used entry number for the Tier2_COMPUTENODE_HISTORY table.
    private long        mEntryNumber_DIAG_HISTORY;                   // the last used entry number for the Tier2_DIAG_HISTORY table.
    private long        mEntryNumber_DIMM_HISTORY;                   // the last used entry number for the Tier2_DIMM_HISTORY table.
    private long        mEntryNumber_FABRICTOPOLOGY_HISTORY;         // the last used entry number for the Tier2_FABRICTOPOLOGY_HISTORY table.
    private long        mEntryNumber_HFI_HISTORY;                    // the last used entry number for the Tier2_HFI_HISTORY table.
    private long        mEntryNumber_JOB_HISTORY;                    // the last used entry number for the Tier2_JOB_HISTORY table.
    private long        mEntryNumber_JOBSTEP_HISTORY;                // the last used entry number for the Tier2_JOBSTEP_HISTORY table.
    private long        mEntryNumber_LUSTRE_HISTORY;                 // the last used entry number for the Tier2_LUSTRE_HISTORY table.
    private long        mEntryNumber_MACHINE_HISTORY;                // the last used entry number for the Tier2_MACHINE_HISTORY table.
    private long        mEntryNumber_MACHINEADAPTERINSTANCE_HISTORY; // the last used entry number for the Tier2_MACHINEADAPTERINSTANCE_HISTORY table.
    private long        mEntryNumber_NODEINVENTORY_HISTORY;          // the last used entry number for the Tier2_NODEINVENTORY_HISTORY table.
    private long        mEntryNumber_NONNODEHW_HISTORY;              // the last used entry number for the Tier2_NonNodeHw_History table.
    private long        mEntryNumber_NONNODEHWINVENTORY_HISTORY;     // the last used entry number for the Tier2_NonNodeHwInventory_History table.
    private long        mEntryNumber_PROCESSOR_HISTORY;              // the last used entry number for the Tier2_PROCESSOR_HISTORY table.
    private long        mEntryNumber_RACK_HISTORY;                   // the last used entry number for the Tier2_RACK_HISTORY table.
    private long        mEntryNumber_RASEVENT;                       // the last used entry number for the Tier2_RASEVENT table.
    private long        mEntryNumber_RASMETADATA;                    // the last used entry number for the Tier2_RASMETADATA table.
    private long        mEntryNumber_REPLACEMENT_HISTORY;            // the last used entry number for the Tier2_REPLACEMENT_HISTORY table.
    private long        mEntryNumber_SERVICEOPERATION_HISTORY;       // the last used entry number for the Tier2_SERVICEOPERATION_HISTORY table.
    private long        mEntryNumber_SERVICENODE_HISTORY;            // the last used entry number for the Tier2_SERVICENODE_HISTORY table.
    private long        mEntryNumber_SWITCH_HISTORY;                 // the last used entry number for the Tier2_SWITCH_HISTORY table.
    private long        mEntryNumber_WLMRESERVATION_HISTORY;         // the last used entry number for the Tier2_WLMRESERVATION_HISTORY table.
    private long        mEntryNumber_WORKITEM_HISTORY;               // the last used entry number for the Tier2_WORKITEM_HISTORY table.
    private long        mEntryNumber_RAWHWINVENTORY_HISTORY;         // the last used entry number for the tier2_RawHWInventory_History table.


    long getTablesMaxEntryNum(String sTableName) throws IOException, ProcCallException {
        VoltTable results = adapter.client().callProcedure("@AdHoc", ("SELECT MAX(EntryNumber) FROM " + sTableName + ";")).getResults()[0];
        long lRc = results.asScalarLong();
        if (results.wasNull())
            lRc = 0L;
        log_.info("getTablesMaxEntryNum - using a current max entry number of %d for table %s", lRc, sTableName);
        return lRc;
    }   // End getTablesMaxEntryNum(String sTableName)

    //--------------------------------------------------------------------------
    // This method handles receipt of the historical data that is being moved from Tier1 to Tier2.
    //--------------------------------------------------------------------------
    @Override
    public long receiveDataFromDataMover() throws InterruptedException, IOException, ProcCallException, TimeoutException {
        //---------------------------------------------------------
        // Handle receiving of the historical data that is being moved from Tier1 to Tier2.
        // Note 1: this work item is different than most in that it runs for the length of time that this adapter instance is active.
        //          It does not start and then finish, it starts and stays active handling any data that is being moved from Tier1 to Tier2.
        //---------------------------------------------------------
        // Set the previously used AmqpMessageId to 0 - this identifies the specific message that was last used (the last successfully processed AmqpMessageId).
        mPrevAmqpMessageId = 0L;
        // Check & see if this is a requeued work item (we need to know so we can figure out how far we already previously processed).
        if (workQueue.isThisNewWorkItem() == false) {
            // REQUEUED work item - need to check for previous progress for this work item so we know what to expect as far as the next Amqp message id.
            // Get the working results for this requeued work item - so we can tell where we were at when the previous instance terminated.
            if (workQueue.workingResults().startsWith("Processed through ")) {
                // found our last processed record.
                log_.info("In a requeued DataReceiver flow, will continue processing from where we stopped, working results = %s", workQueue.workingResults());
                //--------------------------------------------------------------
                // Extract pertinent info from the working results field.
                //--------------------------------------------------------------
                String[] aWorkingResults = workQueue.workingResults().split("\\(");
                for (int iColsCntr = 0; iColsCntr < aWorkingResults.length; ++iColsCntr) {
                    // Previous AmqpMessageId.
                    if (aWorkingResults[iColsCntr].startsWith(AmqpMessageIdPrefix)) {
                        mPrevAmqpMessageId = Long.parseLong( aWorkingResults[iColsCntr].substring(AmqpMessageIdPrefix.length(), aWorkingResults[iColsCntr].indexOf(")")) );  // convert string representation of a number into a long.
                    }
                }   // loop through the input columns
            }
            else {
                log_.warn("This is a requeued work item but the working results field does NOT start with the expected string of (Processed through ), WorkingResults=%s!", workQueue.workingResults());
            }
        }   // REQUEUED work item

        // Get the last used unique entry number for each of the subject tables.
        mEntryNumber_ACCELERATOR_HISTORY            = getTablesMaxEntryNum("Tier2_ACCELERATOR_HISTORY;");
        mEntryNumber_ADAPTER_HISTORY                = getTablesMaxEntryNum("Tier2_ADAPTER_HISTORY;");
        mEntryNumber_BOOTIMAGE_HISTORY              = getTablesMaxEntryNum("Tier2_BOOTIMAGE_HISTORY;");
        mEntryNumber_CHASSIS_HISTORY                = getTablesMaxEntryNum("Tier2_CHASSIS_HISTORY;");
        mEntryNumber_COMPUTENODE_HISTORY            = getTablesMaxEntryNum("Tier2_COMPUTENODE_HISTORY;");
        mEntryNumber_DIAG_HISTORY                   = getTablesMaxEntryNum("Tier2_DIAG_HISTORY;");
        mEntryNumber_DIMM_HISTORY                   = getTablesMaxEntryNum("Tier2_DIMM_HISTORY;");
        mEntryNumber_FABRICTOPOLOGY_HISTORY         = getTablesMaxEntryNum("Tier2_FABRICTOPOLOGY_HISTORY;");
        mEntryNumber_HFI_HISTORY                    = getTablesMaxEntryNum("Tier2_HFI_HISTORY;");
        mEntryNumber_JOB_HISTORY                    = getTablesMaxEntryNum("Tier2_JOB_HISTORY;");
        mEntryNumber_JOBSTEP_HISTORY                = getTablesMaxEntryNum("Tier2_JOBSTEP_HISTORY;");
        mEntryNumber_LUSTRE_HISTORY                 = getTablesMaxEntryNum("Tier2_LUSTRE_HISTORY;");
        mEntryNumber_MACHINE_HISTORY                = getTablesMaxEntryNum("Tier2_MACHINE_HISTORY;");
        mEntryNumber_MACHINEADAPTERINSTANCE_HISTORY = getTablesMaxEntryNum("Tier2_MACHINEADAPTERINSTANCE_HISTORY;");
        mEntryNumber_NODEINVENTORY_HISTORY          = getTablesMaxEntryNum("Tier2_NODEINVENTORY_HISTORY;");
        mEntryNumber_NONNODEHW_HISTORY              = getTablesMaxEntryNum("Tier2_NonNodeHw_History;");
        mEntryNumber_NONNODEHWINVENTORY_HISTORY     = getTablesMaxEntryNum("Tier2_NonNodeHwInventory_History;");
        mEntryNumber_PROCESSOR_HISTORY              = getTablesMaxEntryNum("Tier2_PROCESSOR_History;");
        mEntryNumber_RACK_HISTORY                   = getTablesMaxEntryNum("Tier2_RACK_HISTORY;");
        mEntryNumber_RASEVENT                       = getTablesMaxEntryNum("Tier2_RASEVENT;");
        mEntryNumber_RASMETADATA                    = getTablesMaxEntryNum("Tier2_RASMETADATA;");
        mEntryNumber_REPLACEMENT_HISTORY            = getTablesMaxEntryNum("Tier2_REPLACEMENT_HISTORY;");
        mEntryNumber_SERVICEOPERATION_HISTORY       = getTablesMaxEntryNum("Tier2_SERVICEOPERATION_HISTORY;");
        mEntryNumber_SERVICENODE_HISTORY            = getTablesMaxEntryNum("Tier2_SERVICENODE_HISTORY;");
        mEntryNumber_SWITCH_HISTORY                 = getTablesMaxEntryNum("Tier2_SWITCH_HISTORY;");
        mEntryNumber_WLMRESERVATION_HISTORY         = getTablesMaxEntryNum("Tier2_WLMRESERVATION_HISTORY;");
        mEntryNumber_WORKITEM_HISTORY               = getTablesMaxEntryNum("Tier2_WORKITEM_HISTORY;");

        // Setup AMQP for receiving data being moved from Tier1 to Tier2 (via the DataMover queue) AND for publishing that data for any components that subscribe for it (via the DataMoverExchange).
        DataReceiverAmqp oDataReceiver = createDataReceiver("localhost");
        mAmqpDataReceiverMsgConsumer = new AmqpDataReceiverMsgConsumer(oDataReceiver);
        // Start to consume messages from the DataMover queue that contains data store data that is being moving from Tier1 to Tier2 (DataMover -> DataReceiver)
        // (it will push us messages asynchronously, using the sub-class amqpDataReceiverMsgConsumer as a callback object).
        boolean UseManualAcknowledgement = false;  // false means do not automatically acknowledge this message upon receipt, rather we will manually acknowledge it.
        oDataReceiver.getChannel().basicConsume(Adapter.DataMoverQueueName, UseManualAcknowledgement, CONSUMER_TAG, mAmqpDataReceiverMsgConsumer);

        waitUntilFinishedProcessingMessages();

        // Stop receiving messages
        oDataReceiver.getChannel().basicCancel(CONSUMER_TAG);

        // Shutdown the DataReceiver AMQP infrastructure.
        oDataReceiver.close();
        return 0;
    }   // End receiveDataFromDataMover()

    void waitUntilFinishedProcessingMessages() throws InterruptedException {
        do {
            Thread.sleep(SHUTDOWN_CHECK_INTERVAL_MS);
        } while (!adapter.adapterShuttingDown());
    }


    long insertThisInfoIntoTier2Table(String sTableName, VoltTable vtFromMsg, String sTempStoredProcedure,
                                      long lAmqpMessageId, long lTempInitialTableEntryNum)
         throws IOException
    {
        // Loop through each of the entries in the VoltTable.
        long lTempCurrentTableEntryNum = lTempInitialTableEntryNum;  // working value the current table entry number.
        for (int iRowCntr = 0; iRowCntr < vtFromMsg.getRowCount(); ++iRowCntr) {
            vtFromMsg.advanceRow();
            String sPertinentInfo = sTableName + ",AmqpMessageId=" + lAmqpMessageId + ",MsgRowCntr=" + iRowCntr + ",TableEntryNumber=" + (++lTempCurrentTableEntryNum) + ",AdapterId=" + vtFromMsg.get(0, vtFromMsg.getColumnType(0));
            // Get the list of parameters for this stored procedure into an array.
            ArrayList<Object> alParmObjs = new ArrayList<Object>(vtFromMsg.getColumnCount());
            for (int iColCntr=0; iColCntr < vtFromMsg.getColumnCount(); ++iColCntr) {
                alParmObjs.add(vtFromMsg.get(iColCntr, vtFromMsg.getColumnType(iColCntr)));
            }
            alParmObjs.add(System.currentTimeMillis() * 1000L); // Tier2DbUpdatedTimestamp
            alParmObjs.add(lTempCurrentTableEntryNum);          // EntryNumber
            Object[] aParmObjs = alParmObjs.toArray(new Object[alParmObjs.size()]);
            // Call the stored procedure.
            adapter.client().callProcedure(adapter.createHouseKeepingCallbackNoRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue.baseWorkItemId())  // asynchronously invoke the procedure
                                          ,sTempStoredProcedure // stored procedure
                                          ,aParmObjs            // parameters
                                          );
        }   // loop through each of the entries in the VoltTable.
        log_.info("DataReceiver inserted AmqpMessageId=%d - TableName=%s, %d rows, Used Table Entry numbers %d-%d", lAmqpMessageId, sTableName, vtFromMsg.getRowCount(), lTempInitialTableEntryNum+1, lTempCurrentTableEntryNum);
        return lTempCurrentTableEntryNum;  // return the final value for the entry number so we can update the value when we return.
    }   // End insertThisInfoIntoTier2Table(String sTableName, VoltTable vtFromMsg, String sTempStoredProcedure, long lAmqpMessageId, long lTempInitialTableEntryNum)


    void upsertThisInfoIntoTier2Table(String sTableName, VoltTable vtFromMsg, String sTempStoredProcedure, long lAmqpMessageId)
         throws IOException
    {
        // Loop through each of the entries in the VoltTable.
        for (int iRowCntr = 0; iRowCntr < vtFromMsg.getRowCount(); ++iRowCntr) {
            vtFromMsg.advanceRow();
            String sPertinentInfo = sTableName + ",AmqpMessageId=" + lAmqpMessageId + ",MsgRowCntr=" + iRowCntr + ",AdapterId=" + vtFromMsg.get(0, vtFromMsg.getColumnType(0));
            // Get the list of parameters for this stored procedure into an array.
            ArrayList<Object> alParmObjs = new ArrayList<Object>(vtFromMsg.getColumnCount());
            for (int iColCntr=0; iColCntr < vtFromMsg.getColumnCount(); ++iColCntr) {
                alParmObjs.add(vtFromMsg.get(iColCntr, vtFromMsg.getColumnType(iColCntr)));
            }
            Object[] aParmObjs = alParmObjs.toArray(new Object[alParmObjs.size()]);
            // Call the stored procedure.
            adapter.client().callProcedure(adapter.createHouseKeepingCallbackNoRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue.baseWorkItemId())  // asynchronously invoke the procedure
                                          ,sTempStoredProcedure // stored procedure
                                          ,aParmObjs            // parameters
                                          );
        }   // loop through each of the entries in the VoltTable.
        log_.info("DataReceiver UPSERTED AmqpMessageId=%d - TableName=%s, %d rows", lAmqpMessageId, sTableName, vtFromMsg.getRowCount());
    }   // End upsertThisInfoIntoTier2Table(String sTableName, VoltTable vtFromMsg, String sTempStoredProcedure, long lAmqpMessageId)


    // This method is a specialized method only used in the rare case of a Tier2 row possibly being updated but the row has an entry number (so can't just use upsertThisInfoIntoTier2Table).
    long updateOrInsertThisInfoIntoTier2TableHasEntryNumber(String sTableName, VoltTable vtFromMsg, String sTempStoredProcedure,
                                                            long lAmqpMessageId, long lTempInitialTableEntryNum)
         throws IOException, ProcCallException
    {
        // Loop through each of the entries in the VoltTable.
        long lTempCurrentTableEntryNum = lTempInitialTableEntryNum;  // working value the current table entry number.
        for (int iRowCntr = 0; iRowCntr < vtFromMsg.getRowCount(); ++iRowCntr) {
            vtFromMsg.advanceRow();
            long lExistingRowsEntryNum = -99999;
            // Check & see if the row already exists (have to do an update) or should be inserted.
            VoltTable vtRowAlreadyExists = adapter.client().callProcedure("@AdHoc", ("SELECT EntryNumber FROM Tier2_RASEVENT WHERE DescriptiveName='" + vtFromMsg.getString("DescriptiveName") + "' AND Id=" + vtFromMsg.getLong("Id") + " ORDER By EntryNumber DESC Limit 1;")).getResults()[0];
            if ((vtRowAlreadyExists.wasNull()) || (vtRowAlreadyExists.getRowCount() == 0)) {
                // the row does not exist.
                lExistingRowsEntryNum = ++lTempCurrentTableEntryNum;
            }
            else {
                // the row does already exist.
                lExistingRowsEntryNum = vtRowAlreadyExists.asScalarLong();
            }

            String sPertinentInfo = sTableName + ",AmqpMessageId=" + lAmqpMessageId + ",MsgRowCntr=" + iRowCntr + ",TableEntryNumber=" + lExistingRowsEntryNum + ",AdapterId=" + vtFromMsg.get(0, vtFromMsg.getColumnType(0));

            // Get the list of parameters for this stored procedure into an array.
            ArrayList<Object> alParmObjs = new ArrayList<Object>(vtFromMsg.getColumnCount());
            for (int iColCntr=0; iColCntr < vtFromMsg.getColumnCount(); ++iColCntr) {
                alParmObjs.add(vtFromMsg.get(iColCntr, vtFromMsg.getColumnType(iColCntr)));
            }
            alParmObjs.add(System.currentTimeMillis() * 1000L);  // use current time for Tier2_DbUpdatedTimestamp.
            alParmObjs.add(lExistingRowsEntryNum);  // use the existing row's entry number.
            Object[] aParmObjs = alParmObjs.toArray(new Object[alParmObjs.size()]);
            // Call the stored procedure.
            adapter.client().callProcedure(adapter.createHouseKeepingCallbackNoRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue.baseWorkItemId())  // asynchronously invoke the procedure
                                          ,sTempStoredProcedure // stored procedure
                                          ,aParmObjs            // parameters
                                          );
        }   // loop through each of the entries in the VoltTable.
        log_.info("DataReceiver handled AmqpMessageId=%d - TableName=%s, %d rows, Used Table Entry numbers %d-%d",
                  lAmqpMessageId, sTableName, vtFromMsg.getRowCount(), lTempInitialTableEntryNum+1, lTempCurrentTableEntryNum);
        return lTempCurrentTableEntryNum;  // return the final value for the entry number so we can update the value when we return.
    }   // End updateOrInsertThisInfoIntoTier2TableHasEntryNumber(String sTableName, VoltTable vtFromMsg, String sTempStoredProcedure, long lAmqpMessageId, long lTempInitialTableEntryNum)


    //--------------------------------------------------
    // This is the callback/logic that is invoked for each message that comes into the DataReceiver queue (from the DataMover).
    //--------------------------------------------------
    class AmqpDataReceiverMsgConsumer extends DefaultConsumer {
        // Constructor
        AmqpDataReceiverMsgConsumer(DataReceiverAmqp oDataReceiver) {
            super(oDataReceiver.getChannel());
            mDataReceiver = oDataReceiver;
        }
        // Member data
        private DataReceiverAmqp mDataReceiver;


        // Handle a message received from the DataMover message queue.
        final @Override public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
            long lAmqpMessageId = -99999L;

            String sAmqpMsg="NotFilledIn"; long lIntervalId=-99999; String sTableName="NotFilledIn";
            try {
                // Grab the data out of this message we just received.
                sAmqpMsg = new String(body, "UTF-8");
                PropertyMap jsonMsgObject = adapter.jsonParser().fromString(sAmqpMsg).getAsMap();
                if (jsonMsgObject == null)  throw new RuntimeException("handleDelivery - Received a bad message from RabbitMQ: " + sAmqpMsg);

                // Grab message's header information and check for EOM message.
                boolean eom = jsonMsgObject.getBoolean("EOM");
                if (eom) {
                    //nearlineAdapter.signalEom();
                    //log_.info("Received EOM from data mover");
                    log_.warn("DataReceiver received EOM message from DataMover - ignoring this message");
                    return;
                }
                //else {
                //    nearlineAdapter.stillProcessingMessages();
                //}

                // Grab header information
                lAmqpMessageId                    = jsonMsgObject.getLong("AmqpMessageId");
                lIntervalId                       = jsonMsgObject.getLong("IntervalId");
                long   lEndIntvlTimeMs            = jsonMsgObject.getLong("EndIntvlTsInMsSinceEpoch");     // timestamp in form of millisecs since epoch.
                String sEndIntvlTimeMs            = sdfSqlDateFormat.format(new Date(lEndIntvlTimeMs));
                long   lStartIntvlTimeMs          = jsonMsgObject.getLong("StartIntvlTsInMsSinceEpoch");   // timestamp in form of millisecs since epoch.
                String sStartIntvlTimeMs          = sdfSqlDateFormat.format(new Date(lStartIntvlTimeMs));
                sTableName                        = jsonMsgObject.getString("TableName");
                long   lThisMsgsPartNum           = jsonMsgObject.getLong("Part");                         // this message is part x of the total number of parts that make up this interval's data for sTableName.
                long   lTotalNumPartsForThisTable = jsonMsgObject.getLong("Of");                           // the total number of parts that make up this interval's data for sTableName.

                log_.info("DataReceiver received AmqpMessageId=%d - TableName=%s, Part %d Of %d, IntervalId=%d, EndInterval=%s, StartInterval=%s, AmqpMsgLength=%d",
                          lAmqpMessageId, sTableName, lThisMsgsPartNum, lTotalNumPartsForThisTable, lIntervalId, sEndIntvlTimeMs, sStartIntvlTimeMs, sAmqpMsg.length());

                // Ensure that this AmqpMessageId is "valid/expected" (should be monotonically increasing with no skipped values).
                long lExpectedAmqpMessageId = mPrevAmqpMessageId + 1L;
                if (lAmqpMessageId != lExpectedAmqpMessageId) {
                    // invalid/unexpected AmqpMessageId!
                    // Cut a ras event to record that we received an unexpected (out of sequence) message id while receiving data from Tier1.
                    adapter.logRasEventNoEffectedJob("RasAntDataReceiverInvalidMsgId"
                            ,("IntervalId=" + lIntervalId + ", ReceivedAmqpMessageId=" + lAmqpMessageId + ", ExpectedAmqpMessageId=" + lExpectedAmqpMessageId)
                            ,null                                // Lctn associated with this ras event
                            ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                            ,adapter.adapterType()               // type of adapter that is generating this ras event
                            ,workQueue.workItemId()              // work item that is being worked on that resulted in the generation of this ras event
                            );
                    log_.error("DataReceiver received an unexpected/out of sequence AmqpMessageId, data may have been lost - IntervalId=%d, ReceivedAmqpMessageId=%d, ExpectedAmqpMessageId=%d, AdapterType=%s, ThisAdapterId=%d!",
                               lIntervalId, lAmqpMessageId, lExpectedAmqpMessageId, adapter.adapterType(), adapter.adapterId());
                    // Note: after logging that this has occurred, continue and processing this message!
                }

                // // Grab schema information
                // JSONArray listOfSchemaEntries  = (JSONArray) jsonMsgObject.get("schema");
                // Iterator<?> itSchemaEntries = listOfSchemaEntries.iterator();
                // while(itSchemaEntries.hasNext())
                // {
                //     JSONObject oSchemaEntry = (JSONObject) itSchemaEntries.next();
                //  logger().info("%s - Schema Entries = %s", adapterName(), (String)oSchemaEntry.get("name"));
                // }

                // // Grab data information out of the message.
                // JSONArray listOfMsgDataEntries = (JSONArray) jsonMsgObject.get("data");
                // Iterator<?> itDataEntries = listOfMsgDataEntries.iterator();
                // while(itDataEntries.hasNext())
                // {
                //  JSONArray oDataEntry = (JSONArray) itDataEntries.next();
                //  logger().info("%s - Data Entries = %s", adapterName(), oDataEntry.toString());
                // }

                //--------------------------------------------------------------
                // Publish this message on the pub-sub bus.
                //--------------------------------------------------------------
                String sAmqpRoutingKey = sTableName;  // use the table name as the routing key.
                mDataReceiver.getChannel().basicPublish(Adapter.DataMoverExchangeName, sAmqpRoutingKey, null, jsonMsgObject.toString().getBytes());
                log_.info("Published AmqpMessageId=%d - IntervalId=%d, EndIntervalTs=%s, StartIntervalTs=%s, RoutingKey=%s, TableName=%s, Part %d Of %d",
                          lAmqpMessageId, lIntervalId, sEndIntvlTimeMs, sStartIntvlTimeMs, sAmqpRoutingKey,
                          sTableName, lThisMsgsPartNum, lTotalNumPartsForThisTable);

                //--------------------------------------------------------------
                // Invoke the appropriate stored procedure to insert this table's information into the appropriate Tier2 table.
                //--------------------------------------------------------------
                // Reconstitute the original VoltTable from the specified Json string.
                VoltTable vtFromMsg = VoltTable.fromJSONString(sAmqpMsg);
                log_.info("Inserting AmqpMessageId=%d - IntervalId=%d, EndIntervalTs=%s, StartIntervalTs=%s, TableName=%s, Part %d Of %d, %d rows",
                          lAmqpMessageId, lIntervalId, sEndIntvlTimeMs,  sStartIntvlTimeMs,
                          sTableName, lThisMsgsPartNum, lTotalNumPartsForThisTable, vtFromMsg.getRowCount());
                // Determine which stored procedure that we need to use for this table.
                switch(sTableName) {
                    case "Accelerator":
                        mEntryNumber_ACCELERATOR_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_ACCELERATOR_HISTORY.insert", lAmqpMessageId, mEntryNumber_ACCELERATOR_HISTORY);
                        break;
                    case "Adapter":
                        mEntryNumber_ADAPTER_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_ADAPTER_HISTORY.insert", lAmqpMessageId, mEntryNumber_ADAPTER_HISTORY);
                        break;
                    case "BootImage":
                        mEntryNumber_BOOTIMAGE_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_BOOTIMAGE_HISTORY.insert", lAmqpMessageId, mEntryNumber_BOOTIMAGE_HISTORY);
                        break;
                    case "Chassis":
                        mEntryNumber_CHASSIS_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_CHASSIS_HISTORY.insert", lAmqpMessageId, mEntryNumber_CHASSIS_HISTORY);
                        break;
                    case "ComputeNode":
                        mEntryNumber_COMPUTENODE_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_COMPUTENODE_HISTORY.insert", lAmqpMessageId, mEntryNumber_COMPUTENODE_HISTORY);
                        break;
                    case "Constraint":
                        upsertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_Constraint.upsert", lAmqpMessageId);
                        break;
                    case "Diag":
                        mEntryNumber_DIAG_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_DIAG_HISTORY.insert", lAmqpMessageId, mEntryNumber_DIAG_HISTORY);
                        break;
                    case "Diag_List":
                        upsertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_DIAG_LIST.upsert", lAmqpMessageId);
                        break;
                    case "DiagResults":
                        upsertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_DIAGRESULTS.upsert", lAmqpMessageId);
                        break;
                    case "Diag_Tools":
                        upsertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_DIAG_TOOLS.upsert", lAmqpMessageId);
                        break;
                    case "Dimm":
                        mEntryNumber_DIMM_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_DIMM_HISTORY.insert", lAmqpMessageId, mEntryNumber_DIMM_HISTORY);
                        break;
                    case "FabricTopology":
                        mEntryNumber_FABRICTOPOLOGY_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_FABRICTOPOLOGY_HISTORY.insert", lAmqpMessageId, mEntryNumber_FABRICTOPOLOGY_HISTORY);
                        break;
                    case "Hfi":
                        mEntryNumber_HFI_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_HFI_HISTORY.insert", lAmqpMessageId, mEntryNumber_HFI_HISTORY);
                        break;
                    case "Job":
                        mEntryNumber_JOB_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_JOB_HISTORY.insert", lAmqpMessageId, mEntryNumber_JOB_HISTORY);
                        break;
                    case "JobStep":
                        mEntryNumber_JOBSTEP_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_JOBSTEP_HISTORY.insert", lAmqpMessageId, mEntryNumber_JOBSTEP_HISTORY);
                        break;
                    case "Lustre":
                        mEntryNumber_LUSTRE_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_LUSTRE_HISTORY.insert", lAmqpMessageId, mEntryNumber_LUSTRE_HISTORY);
                        break;
                    case "Machine":
                        mEntryNumber_MACHINE_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_MACHINE_HISTORY.insert", lAmqpMessageId, mEntryNumber_MACHINE_HISTORY);
                        break;
                    case "MachineAdapterInstance":
                        mEntryNumber_MACHINEADAPTERINSTANCE_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_MACHINEADAPTERINSTANCE_HISTORY.insert", lAmqpMessageId, mEntryNumber_MACHINEADAPTERINSTANCE_HISTORY);
                        break;
                    case "NodeInventory_History":
                        mEntryNumber_NODEINVENTORY_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_NODEINVENTORY_HISTORY.insert", lAmqpMessageId, mEntryNumber_NODEINVENTORY_HISTORY);
                        break;
                    case "NonNodeHw_History":
                        mEntryNumber_NONNODEHW_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_NONNODEHW_HISTORY.insert", lAmqpMessageId, mEntryNumber_NONNODEHW_HISTORY);
                        break;
                    case "NonNodeHwInventory_History":
                        mEntryNumber_NONNODEHWINVENTORY_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_NONNODEHWINVENTORY_HISTORY.insert", lAmqpMessageId, mEntryNumber_NONNODEHWINVENTORY_HISTORY);
                        break;
                    case "Processor":
                        mEntryNumber_PROCESSOR_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_PROCESSOR_HISTORY.insert", lAmqpMessageId, mEntryNumber_PROCESSOR_HISTORY);
                        break;
                    case "Rack":
                        mEntryNumber_RACK_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_RACK_HISTORY.insert", lAmqpMessageId, mEntryNumber_RACK_HISTORY);
                        break;
                    case "RasEvent":
                        mEntryNumber_RASEVENT = updateOrInsertThisInfoIntoTier2TableHasEntryNumber(sTableName, vtFromMsg, "TIER2_RASEVENT.upsert", lAmqpMessageId, mEntryNumber_RASEVENT);
                        break;
                    case "RasMetaData":
                        mEntryNumber_RASMETADATA = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_RASMETADATA.insert", lAmqpMessageId, mEntryNumber_RASMETADATA);
                        break;
                    case "Replacement_History":
                        mEntryNumber_REPLACEMENT_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_REPLACEMENT_HISTORY.insert", lAmqpMessageId, mEntryNumber_REPLACEMENT_HISTORY);
                        break;
                    case "ServiceOperation":
                        mEntryNumber_SERVICEOPERATION_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_SERVICEOPERATION_HISTORY.insert", lAmqpMessageId, mEntryNumber_SERVICEOPERATION_HISTORY);
                        break;
                    case "ServiceNode":
                        mEntryNumber_SERVICENODE_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_SERVICENODE_HISTORY.insert", lAmqpMessageId, mEntryNumber_SERVICENODE_HISTORY);
                        break;
                    case "Switch":
                        mEntryNumber_SWITCH_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_SWITCH_HISTORY.insert", lAmqpMessageId, mEntryNumber_SWITCH_HISTORY);
                        break;
                    case "UcsConfigValue":
                        upsertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_UCSCONFIGVALUE.upsert", lAmqpMessageId);
                        break;
                    case "UniqueValues":
                        upsertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_UNIQUEVALUES.upsert", lAmqpMessageId);
                        break;
                    case "WlmReservation_History":
                        mEntryNumber_WLMRESERVATION_HISTORY = insertThisInfoIntoTier2Table(sTableName, vtFromMsg, "TIER2_WLMRESERVATION_HISTORY.insert", lAmqpMessageId, mEntryNumber_WLMRESERVATION_HISTORY);
                        break;
                    case "WorkItem":
                        // Loop through each of the entries in the VoltTable.
                        long lTempInitialTableEntryNum = mEntryNumber_WORKITEM_HISTORY;  // save the value of the current table entry number (before any inserts).
                        for (int iRowCntr = 0; iRowCntr < vtFromMsg.getRowCount(); ++iRowCntr) {
                            int iCol = 0;
                            vtFromMsg.advanceRow();
                            String sRowInsertedIntoHistory = vtFromMsg.getString("RowInsertedIntoHistory");
                            if (sRowInsertedIntoHistory.equals("T")) {
                                // insert this record into the Tier2 history table.
                                String sTempStoredProcedure = "TIER2_WORKITEM_HISTORY.insert";
                                String sPertinentInfo = sTableName + ",AmqpMessageId=" + lAmqpMessageId + ",MsgRowCntr=" + iRowCntr + ",TableEntryNumber=" + (++mEntryNumber_WORKITEM_HISTORY) + ",WorkItemId=" + vtFromMsg.get(2, vtFromMsg.getColumnType(2));
                                adapter.client().callProcedure(adapter.createHouseKeepingCallbackNoRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue.baseWorkItemId())  // asynchronously invoke the procedure
                                                              ,sTempStoredProcedure
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,vtFromMsg.get(iCol, vtFromMsg.getColumnType(iCol++))
                                                              ,System.currentTimeMillis() * 1000L
                                                              ,mEntryNumber_WORKITEM_HISTORY
                                                              );
                                log_.info("Inserted AmqpMessageId=%d - TableName=%s, %d rows, Used Table Entry numbers %d-%d", lAmqpMessageId, sTableName, vtFromMsg.getRowCount(), lTempInitialTableEntryNum+1, mEntryNumber_WORKITEM_HISTORY);
                            }   // insert this record into the Tier2 history table.
                            else {
                                // update this record in the Tier2 history table.
                                //--------------------------------------------------
                                // This section of code that handles the "special update only records" for Tier2_WorkItemHistory table was commented out after discussion with Todd about the correct action to take.
                                //      We decided that best course of action for now was to ignore these updates for the time being (since these special update only history records will go away over time),
                                //      these update records should go away as we transition from having InputFromLogstash files being input (for long running adapters) to
                                //      having logstash instead use RabbitMq queues for input.
                                //--------------------------------------------------
                                log_.info("Ignoring the update AmqpMessageId=%d - TableName=%s, %d rows, Used Table Entry numbers %d-%d", lAmqpMessageId, sTableName, vtFromMsg.getRowCount(), lTempInitialTableEntryNum+1, mEntryNumber_WORKITEM_HISTORY);
                                // sTempStoredProcedure = "Tier2_UpdateWorkItem";
                                // sPertinentInfo = sTableName + ",AmqpMessageId=" + lAmqpMessageId + ",MsgRowCntr=" + iRowCntr + ",TableEntryNumber=" + (++mEntryNumber_WORKITEM_HISTORY) + ",WorkItemId=" + vtFromMsg.get(2, vtFromMsg.getColumnType(2));
                                // adapter.client().callProcedure(adapter.createHouseKeepingCallbackNoRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue.baseWorkItemId())  // asynchronously invoke the procedure
                                //                               ,sTempStoredProcedure
                                //                               ,vtFromMsg.getString("WorkingResults")
                                //                               ,vtFromMsg.getTimestampAsTimestamp("DbUpdatedTimestamp")
                                //                               ,vtFromMsg.getLong("RequestingWorkItemId")
                                //                               ,sRowInsertedIntoHistory                      // RowInsertedIntoHistory
                                //                               ,System.currentTimeMillis() * 1000L           // Tier2DbUpdatedTimestamp
                                //                               ,vtFromMsg.getString("WorkingAdapterType")
                                //                               ,vtFromMsg.getLong("Id")
                                //                               ,vtFromMsg.getString("State")
                                //                               ,vtFromMsg.getLong("WorkingAdapterId")
                                //                               );
                                // adapter.logger().info("%s - DataReceiver updated AmqpMessageId=%d - TableName=%s, %d rows, Used Table Entry numbers %d-%d", adapter.adapterName(), lAmqpMessageId, sTableName, vtFromMsg.getRowCount(), lTempInitialTableEntryNum+1, mEntryNumber_WORKITEM_HISTORY);
                            }   // update this record in the Tier2 history table.

                        }  // loop through each of the entries in the VoltTable.
                        break;
                    default:
                        log_.error("AmqpDataReceiverMsgConsumer - unable to handle this table %s as we have not defined the switch case for it - IntervalId=%d, EndInterval=%s, StartInterval=%s, TableName=%s, Part %d Of %d!",
                                   sTableName, lIntervalId, sEndIntvlTimeMs, sStartIntvlTimeMs, sTableName, lThisMsgsPartNum, lTotalNumPartsForThisTable);
                        adapter.logRasEventNoEffectedJob("RasAntMissingCaseStmt"
                                                        ,("TableName=" + sTableName)        // instanceData
                                                        ,null                               // lctn
                                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                        ,adapter.adapterType()              // reqAdapterType
                                                        ,workQueue.workItemId()             // reqWorkItemId
                                                        );
                        // Break out of this loop and bring adapter down!
                        adapter.adapterAbnormalShutdown(true);  // Set flag indicating that something bad caused this adapter to terminate!
                        break;
                }   // End switch(sTableName)


                //--------------------------------------------------------------
                // Save restart data indicating the timestamp of the last data that was moved from Tier1 to Tier2.
                //--------------------------------------------------------------
                String sRestartData = "Processed through (" + TimestampPrefix + sEndIntvlTimeMs + ") (" + IntervalIdPrefix + lIntervalId + ") (" + AmqpMessageIdPrefix + lAmqpMessageId + ") (" + TableNamePrefix + sTableName + ")";
                workQueue.saveWorkItemsRestartData(workQueue.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
                // Save the AmqpMessageId that we just used (so we have it available for the next message).
                mPrevAmqpMessageId = lAmqpMessageId;
            }
            catch (Exception e) {
                log_.error("AmqpDataReceiverMsgConsumer - Exception occurred (msg will be skipped): %s!", e.getMessage());
                log_.error("%s", Adapter.stackTraceToString(e));
                log_.error("Message that incurred the above exception - %s", sAmqpMsg);
                // Save the AmqpMessageId that we just used (so we have it available for the next message).
                mPrevAmqpMessageId = lAmqpMessageId;
                try {
                    String sTempInstanceData = "IntervalId=" + lIntervalId + ", AmqpMessageId=" + lAmqpMessageId + ", TableName=" + sTableName + "Exception=" + e;
                    adapter.logRasEventNoEffectedJob("RasAntException"
                            ,sTempInstanceData                  // instanceData
                            ,null                               // lctn
                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred
                            ,adapter.adapterType()              // reqAdapterType
                            ,workQueue.baseWorkItemId()         // reqWorkItemId
                            );
                    // Note: after logging that this has occurred, skip this message!
                }
                catch (Exception e2) { log_.error("AmqpDataReceiverMsgConsumer - Second exception occurred (exception within exception handler): %s!", e.getMessage()); }
            }   // catch
            finally {
                // Send a successful acknowledgment for receipt and successfully handling this message!
                mDataReceiver.getChannel().basicAck(envelope.getDeliveryTag(), false);
                log_.debug("DataReceiver acknowledged AmqpMessageId=%d", lAmqpMessageId);
            }   // finally
        }   // End handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)

    }   // End class AmqpDataReceiverMsgConsumer



    public static void main(String[] args) throws IOException, TimeoutException {
        Logger logger = LoggerFactory.getInstance("NEARLINE_TIER", AdapterNearlineTierVolt.class.getName(), "log4j2");
        AdapterSingletonFactory.initializeFactory("NEARLINE_TIER", AdapterNearlineTierVolt.class.getName(), logger);
        final AdapterNearlineTierVolt obj = new AdapterNearlineTierVolt(logger);
        // Start up the main processing flow for NearlineTier adapters.
        obj.mainProcessingFlow(args);
    }   // End main(String[] args)

}   // End class AdapterNearlineTierVolt
