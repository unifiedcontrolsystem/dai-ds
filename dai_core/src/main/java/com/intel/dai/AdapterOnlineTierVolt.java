// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.voltdb.client.*;
import org.voltdb.VoltTable;
import java.lang.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import com.intel.config_io.*;
import com.intel.properties.*;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.*;

/**
 * AdapterOnlineTierVolt for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
 *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 * Example invocation:
 *      java AdapterOnlineTierVolt voltdbserver1,voltdbserver2,10.11.12.13  (or java AdapterOnlineTierVolt  - this will default to using localhost)
 */
public class AdapterOnlineTierVolt extends AdapterOnlineTier {
    final String TimestampPrefix     = "Timestamp=";
    final String IntervalIdPrefix    = "IntervalId=";
    final String AmqpMessageIdPrefix = "AmqpMessageId=";

    // Constructor
    AdapterOnlineTierVolt(Logger logger) throws IOException, TimeoutException {
        super(logger);
        mDataMoverAmqpMessageId         = 0L;
        mDataMoverIntervalId            = -99999L;
        mDataMoverPrevProcessedTimeMs   = 0L;
        mLastIntvlWasOnlyDataMoverWIs   = false;  // flag that records whether or not the last interval consisted only of either a DataMover WorkItem workitem, a DataReceiver WorkItem, or 1 of each.
        mParser = ConfigIOFactory.getInstance("json");
        assert mParser != null : "sendThisTablesChangesToTier2 - Failed to create a JSON parser!";
    }   // ctor
    // Member Data
    private long    mDataMoverAmqpMessageId;        // this identifies a specific message that was sent over the AMQP bus (can detect "lost" messages).
    private long    mDataMoverIntervalId;           // this identifies the sequence of intervals (the number of times we found sets of data that needs to be moved from Tier1 to Tier2).
    private long    mDataMoverPrevProcessedTimeMs;  // the timestamp (in millisecs since epoch) of the already handled data, used to determine the starting time of this interval.
    // Flag that records whether or not the last interval consisted only of either a DataMover WorkItem workitem, a DataReceiver WorkItem, or 1 of each
    // (we are tracking this so that we do not end up with a constant stream consisting simply of DataMover/DataReceiver work items, that "recurse" because
    //  we can't update the DataMover/DataReceiver work items WorkingResults info until after that DataMover interval has finished, so then the next interval it would send the DataMover update, which results in an update, which would then be sent, on, on, on).
    private boolean mLastIntvlWasOnlyDataMoverWIs;
    private ConfigIO mParser;


    //---------------------------------------------------------
    // Handle moving the historical data from Tier1 to Tier2.
    // Note 1: this is also the mechanism that supports the ability for subscriptions to data store changes.
    // Note 2: this work item is different than most in that it runs for the length of time that this adapter instance is active.
    //          It does not start and then finish, it starts and stays active handling any data that needs to move from Tier1 to Tier2.
    //---------------------------------------------------------
    public final long sendDataMoverData() throws IOException, TimeoutException, InterruptedException, ProcCallException, java.text.ParseException, ConfigIOParseException {
        // Set previous timestamp to zeros (so we process all data, don't skip over any of the data).
        String sTempTimestamp = "2000-01-01 00:00:00.000000"; // note: using sql format!
        // Set the IntervalId to 0 - this identifies the sequence of times through this while loop.
        mDataMoverIntervalId = 0L;
        // Initialize the AmqpMessageId to 0 - this identifies a specific message that was sent over the AMQP bus (can identify any "lost" messages).
        mDataMoverAmqpMessageId = 0L;
        // Check & see if this is a requeued work item (we need to know so we can figure out how far we already previously processed).
        if (workQueue.isThisNewWorkItem() == false) {
            // REQUEUED work item - need to check for previous progress for this work item so we don't move data that has already been handled.
            // Get the working results for this requeued work item - so we can tell where we were at when the previous instance terminated.
            if (workQueue.workingResults().startsWith("Processed through ")) {
                // found our last processed record.
                log_.info("In a requeued DataMover flow, working results = %s", workQueue.workingResults());
                //--------------------------------------------------------------
                // Extract pertinent info from the working results field.
                //--------------------------------------------------------------
                String[] aWorkingResults = workQueue.workingResults().split("\\(");
                for (int iColsCntr = 0; iColsCntr < aWorkingResults.length; ++iColsCntr) {
                    // Previous ending timestamp.
                    if (aWorkingResults[iColsCntr].startsWith(TimestampPrefix)) {
                        sTempTimestamp = aWorkingResults[iColsCntr].substring(TimestampPrefix.length(), aWorkingResults[iColsCntr].indexOf(")"));
                    }
                    // Previous IntervalId.
                    else if (aWorkingResults[iColsCntr].startsWith(IntervalIdPrefix)) {
                        mDataMoverIntervalId = Long.parseLong( aWorkingResults[iColsCntr].substring(IntervalIdPrefix.length(), aWorkingResults[iColsCntr].indexOf(")")) );
                    }
                    // Previous AmqpMessageId.
                    else if (aWorkingResults[iColsCntr].startsWith(AmqpMessageIdPrefix)) {
                        mDataMoverAmqpMessageId = Long.parseLong( aWorkingResults[iColsCntr].substring(AmqpMessageIdPrefix.length(), aWorkingResults[iColsCntr].indexOf(")")) );
                    }
                }   // loop through the input columns
                log_.warn("Will skip over any record with a timestamp before %s - starting with an IntervalId=%d and AmqpMessageId=%d", sTempTimestamp, mDataMoverIntervalId+1, mDataMoverAmqpMessageId+1);
            }
        }
        // Get the previously processed timestamp into units of milliseconds since epoch.
        SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        mDataMoverPrevProcessedTimeMs = sdfSqlDateFormat.parse(sTempTimestamp).getTime();  // the timestamp (in millisecs since epoch) of the already handled data, used to determine the starting time of this interval.

        // Set up AMQP for directly moving data from Tier1 to Tier2 (via a queue, not pub-sub).
        DataMoverAmqp oDataMover = new DataMoverAmqp(rabbitMQHost, adapter, log_, workQueue.workItemId());

        //----------------------------------------------------------------------
        // Loop forever moving data from Tier1 to Tier2 (each iteration through this loop is referred to as an interval).
        //----------------------------------------------------------------------
        long lLastTimePurgeDataChkInMillis = 0L;  // timestamp in millisecs when we last checked for data to purged.
        while(adapter.adapterShuttingDown() == false) {  // loop forever processing as a "DataMover"
            //------------------------------------------------------------------
            // Handle any data that needs to be moved from Tier1 to Tier2
            //------------------------------------------------------------------
            handleDataNeedingToMoveFromTier1ToTier2(oDataMover);
            //------------------------------------------------------------------
            // Periodically purge data that has already been moved to Tier2 (by handleDataNeedingToMoveFromTier1ToTier2) AND
            // that is older than the LengthOfTimeToRetainPurgeableData.
            // - this is envisioned to wake up once an hour and purge data from the pertinent tables that is 24 hours older than
            //   the timestamp of the last moved data.
            //------------------------------------------------------------------
            if ((System.currentTimeMillis() - timeBetweenCheckingForDataToPurgeInMillis()) >= lLastTimePurgeDataChkInMillis) {
                // Purge pertinent data from the Tier1 tables
                // (will only purge data that has already been moved from Tier1 to Tier2, so the data being purged is already in Tier2).
                handlePurgingData(mDataMoverPrevProcessedTimeMs, timeToKeepMovedDataBeforePurgingInMillis());
                // Save current timestamp so we know when we last checked to see if there is any data to be purged.
                lLastTimePurgeDataChkInMillis = System.currentTimeMillis();
            }

            // Sleep for 1 second between iterations.
            //      Optimization would be to keep a summary indicating which tables had updates for this interval, and how many updates have shown up this interval.
            //      These would let us know which tables should be queried (only query those tables that we know have changed) and also could use the summary of number of changes to possibly increase or decrease the 1 second interval based on amount of "traffic".
            Thread.sleep(1 * 1000);
        }   // loop forever processing as a "DataMover"

        // Signal the data receiver that no more messages will be sent as we're shutting down
        sendFinalMessage(oDataMover);

        // Shutdown the DataMover AMQP infrastructure.
        oDataMover.close();
        return 0;
    }   // End sendDataMoverData()

    private final void sendFinalMessage(DataMoverAmqp oDataMover) throws IOException, ConfigIOParseException {
        PropertyMap jsonAmqpObj = new PropertyMap();
        // Special message indicating that this is the final message
        jsonAmqpObj.put("EOM", true);
        jsonAmqpObj.put("IntervalId", -1);
        jsonAmqpObj.put("AmqpMessageId", -1);
        oDataMover.getChannel().basicPublish("", Adapter.DataMoverQueueName, MessageProperties.PERSISTENT_BASIC, mParser.toString(jsonAmqpObj).getBytes(StandardCharsets.UTF_8));
        log_.info("DataMover sent final message");
    }


    //---------------------------------------------------------
    // Handle purging of data from appropriate Tier1 tables.
    // - Can't purge data if it has not yet been moved to Tier2 (need to be sure that a copy persists elsewhere before purging it from Tier1)
    // - Can only purge from historical tables (e.g., ComputeNode_History table), can't purge from current/active tables (e.g., ComputeNode table)
    // - Need to keep data in Tier1 for an interval of time (lTimeToKeepMovedDataBeforePurgingInMillis) even after it has been moved to Tier2.
    // Parms:
    //      long lTimeOfLastMovedTier1DataInMillis          - the timestamp (in number of millisecs) of the last historical data that has been moved to Tier2.
    //      long lTimeToKeepMovedDataBeforePurgingInMillis  - the number of millisecs that we want to also retain the data in Tier1 even AFTER it has been moved to Tier2.
    //---------------------------------------------------------
    long lSavePreviousPurgeTimeInMillis = -1L;
    final public long handlePurgingData(long lTimeOfLastMovedTier1DataInMillis, long lTimeToKeepMovedDataBeforePurgingInMillis) throws InterruptedException, IOException, ProcCallException {
        // Calculate the timestamp of data that we want to purge (any record with a DbUpdatedTimestamp earlier than this value will be deleted)
        // Note: take the earlier of these 2 values:
        //  1) Timestamp of the last data that has been moved to Tier2.
        //  2) Current timestamp - Amount of time we want to keep data after it has been moved before we purge it from Tier1.
        long lCurrentPurgeTimeInMillis = Math.min((System.currentTimeMillis() - lTimeToKeepMovedDataBeforePurgingInMillis), lTimeOfLastMovedTier1DataInMillis);

        // Convert the purge timestamp back into string format.
        SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        String sCurrentPurgeTimestamp = sdfSqlDateFormat.format(new Date(lCurrentPurgeTimeInMillis));

        log_.info("handlePurgingData - starting - using a purge time of '%s'", sCurrentPurgeTimestamp);

        // Ensure that we are advancing with the data purging from Tier1 (only reason it would not be changing is if data is not being moved from Tier1 to Tier2).
        if (lCurrentPurgeTimeInMillis <= lSavePreviousPurgeTimeInMillis) {
            // there was no advancement in purging of data from Tier1 between this interval and the previous interval.
            String sPreviousPurgeTimestamp = sdfSqlDateFormat.format(new Date(lSavePreviousPurgeTimeInMillis));
            log_.warn("handlePurgingData - no advancement in data purging since the last purging interval - CurrentPurgeTime='%s', PreviousPurgeTime='%s'",
                    sCurrentPurgeTimestamp, sPreviousPurgeTimestamp);
            adapter.logRasEventNoEffectedJob(adapter.getRasEventType("RasAotAutoPurgeOfTier1HistoricalDataFinished")
                                    ,("CurrentPurgeTimestamp='" + sCurrentPurgeTimestamp + "', PreviousPurgeTimestamp='" + sPreviousPurgeTimestamp + "'") // Instance data
                                    ,null                               // lctn associated with this ras event
                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,adapter.adapterType()              // type of adapter that is generating this ras event
                                    ,workQueue.workItemId()             // work item that is being worked on that resulted in the generation of this ras event
                                    );
            return -1L;
        }

        // Loop through each of the tables and delete the pertinent info.
        long lTotalNumRowsPurged = 0L;
        for(String sTableName : setOfTablesToBePurged()) {
            long lTempNumRowsDeleted = 0L;
            // Ensure that we don't purge data out of the RasMetaData table.
            if (sTableName.equals("RasMetaData")) {
                // this is a very special "historical table", its changes are indeed "moved" to Tier2 by DataMover BUT we don't ever want to purge from this table!!!
                log_.warn("handlePurgingData - attempted to purge data from the %s table, but that is not allowed - skipping this table!", sTableName);
                continue;  // skip to the next table.
            }
            else if (sTableName.equals("NodeInventory_History")) {
                // this table needs special processing as we do not want to purge the current inventory info for a node.
                String sTempStoredProcedure = "NodePurgeInventory_History";
                log_.debug("handlePurgingData - purging data from the %s table that is older than %s (except for newest rec per lctn) - stored procedure %s", sTableName, sCurrentPurgeTimestamp, sTempStoredProcedure);
                // Call a stored procedure to handle the deletion of this table's "old" data.
                lTempNumRowsDeleted = adapter.client().callProcedure(sTempStoredProcedure, sCurrentPurgeTimestamp).getResults()[0].asScalarLong();
            }
            else {
                // handle all of the tables that do NOT need special purge processing.
                // Build up the sql statement that we are going to use to purge the data from this table.
                String sTempDeleteSql = "DELETE FROM " + sTableName + " WHERE DbUpdatedTimestamp <= '" + sCurrentPurgeTimestamp + "'";
                // Actually delete this "old" data.
                log_.debug("handlePurgingData - purging data from the %s table that is older than %s - %s", sTableName, sCurrentPurgeTimestamp, sTempDeleteSql);
                lTempNumRowsDeleted = adapter.client().callProcedure("@AdHoc", sTempDeleteSql).getResults()[0].asScalarLong();
            }

            // Log the number of rows that were deleted from this table.
            if (lTempNumRowsDeleted > 0)
                log_.info("handlePurgingData - purged %d rows of data from the %s table", lTempNumRowsDeleted, sTableName);
            else
                log_.debug("handlePurgingData - purged %d rows of data from the %s table", lTempNumRowsDeleted, sTableName);
            // Add this table's number of rows purged to the others that were already deleted during this interval.
            lTotalNumRowsPurged += lTempNumRowsDeleted;
        }

        // Log an event indicating the total number of rows of data were purged during this purge invocation.
        if (lTotalNumRowsPurged > 0L) {
            adapter.logRasEventNoEffectedJob(adapter.getRasEventType("RasAotAutoPurgeOfTier1HistoricalDataFinished")
                    ,("PurgedDataEarlierThan=" + sCurrentPurgeTimestamp + ", TotalNumberOfRowsPurged=" + lTotalNumRowsPurged) // Instance data
                    ,null                               // lctn associated with this ras event
                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                    ,adapter.adapterType()              // type of adapter that is generating this ras event
                    ,workQueue.workItemId()             // work item that is being worked on that resulted in the generation of this ras event
                    );
        }

        // Save away the purge timestamp that was just used as the "saved previous purge timestamp".
        lSavePreviousPurgeTimeInMillis = lCurrentPurgeTimeInMillis;
        log_.info("handlePurgingData - finished - purged a total of %d rows", lTotalNumRowsPurged);
        return 0L;
    }   // End handlePurgingData(long lTimeOfLastMovedTier1DataInMillis, long lTimeToKeepMovedDataBeforePurgingInMillis)


    private final void handleDataNeedingToMoveFromTier1ToTier2(DataMoverAmqp oDataMover) throws IOException, ProcCallException, InterruptedException, ConfigIOParseException {
        DecimalFormat decimalFormatter = new DecimalFormat("#,###,###");  // pretty formatting
        // Take the current time and subtract a little bit of time from it, so that we do not need to be concerned about possibly only getting part of the records for that time in case there are still records in flight!
        // - Subtracting 25 millisecond to back up so we can be certain that we have included all of the data for the entire current millisecond, as opposed to possibly a partial millisecond (if events currently being inserted in same millisecond).
        long lEndIntvlTimeMs = System.currentTimeMillis() - 25L;
        SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        String sEndIntvlTimestamp   = sdfSqlDateFormat.format(new Date(lEndIntvlTimeMs));
        String sStartIntvlTimestamp = sdfSqlDateFormat.format(new Date(mDataMoverPrevProcessedTimeMs));

        // Do a "big" query to Volt w/i a single stored procedure to get any changes to the specified historical tables for THIS interval
        // (want to do this all in a single stored procedure as I think that will minimize the amount of time that the db will be locked, as this is not a partitioned procedure).
        // NOTE: The records that are "harvested" should be based on the DbUpdatedTimestamp field in each table, so we are getting a known quantity (don't need to worry about records showing up out of order)!!!
        String sTempStoredProcedure = "DataMoverGetListOfRecsToMove";
        ClientResponse response = adapter.client().callProcedure(sTempStoredProcedure, lEndIntvlTimeMs*1000L, ((mDataMoverPrevProcessedTimeMs*1000L) + 1L)); // note: adding 1 microsecond to the prev timestamp so we don't "regrab" records that we already handled!
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            // Cut a ras event to record that a fatal event occurred
            // while moving data from Tier1 to Tier2 - I know that no job is effected by this RAS event.
            adapter.logRasEventNoEffectedJob(adapter.getRasEventType("RasAotDataMoverQueryFailed")
                                    ,("StoredProcedure=" + sTempStoredProcedure + ", Status=" + IAdapter.statusByteAsString(response.getStatus()) + ", StatusString=" + response.getStatusString())
                                    ,null                               // Lctn associated with this ras event
                                    ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                    ,adapter.adapterType()              // type of adapter that is generating this ras event
                                    ,workQueue.workItemId()             // work item that is being worked on that resulted in the generation of this ras event
                                    );
            log_.fatal("DataMover query FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }

        // Create an array of JSON strings, each string contains the json formatted data for one table (a json string has the information that is being moved for a single tier1 table).
        VoltTable[] aVt = response.getResults();
        String[] aVtJson = new String[aVt.length];  // VoltTables in form of JSON-formatted strings, one json string per table.
        int iNumRecsBeingMoved = 0;  // initialize the total number of db rows being moved from Tier1 to Tier2 this interval.
        for (int iVtCntr=0; iVtCntr < aVt.length; ++iVtCntr) {
            aVt[iVtCntr].advanceRow();
            // Add the number of rows being moved for this table to the total number being moved this interval.
            iNumRecsBeingMoved += aVt[iVtCntr].getRowCount();
            // Get the Json-formatted string for this table (if appropriate).
            if (aVt[iVtCntr].getRowCount() != 0) {
                aVtJson[iVtCntr] = aVt[iVtCntr].toJSONString();  // convert the rows being moved for this table into a single json formatted string.
            }
        }

        // Check & see if there were any records to move this interval, if no records to move then short-circuit this flow.
        if (iNumRecsBeingMoved == 0) {
            log_.debug("DataMover - recurse check - there aren't any rows of data moving this interval, so no recurse is occurring");
            // Do NOT move this data (short-circuit because there is no data to move).
            return;  // there aren't any records to move this interval, short-circuit and return to caller.
        }

        //----------------------------------------------------------------------
        // Prevent a constant stream of the DataMover moving only DataMover/DataReceiver work items, this "recursion" occurs because we can't update the DataMover/DataReceiver work item's WorkingResults field
        // until AFTER that DataMover interval has finished, which means that in the next interval it would then send the DataMover update, which results in an update, which would then be sent, on, on, on).
        //----------------------------------------------------------------------
        long lRc = seeIfInDataMoverRecursion(iNumRecsBeingMoved, aVt, decimalFormatter);
        if (lRc < 0L)
            return;  // recursion was detected, we should suppress it (i.e., do not move this data to Tier2)

        //----------------------------------------------
        // There is data to be moved.
        //----------------------------------------------
        log_.info("DataMover - moving %s rows of data this interval", decimalFormatter.format(iNumRecsBeingMoved));
        ++mDataMoverIntervalId;  // get the next interval id to use for this iteration of moving a set of data from Tier1 to Tier2.

        //----------------------------------------------
        // Loop through the tables, sending each table's rows (for this interval) to the Tier2 DataReceiver
        // (each table's results will be sent in its own message, if there is too large of a number of results for a given table they will be divided and sent in multiple msgs).
        //      The DataReceiver (AdapterNearlineTier) will be responsible for taking this data we are sending to it and adding it into the corresponding Tier2 data store.
        //      - Additionally the DataReceiver will publish the data it receives over the DataMover exchange, so any component interested in the changes that are occurring
        //        can subscribe to all or a subset of the data changes.
        //                  For instance component 1 might want all of the data from the ComputeNode table.
        //                  Component 2 might want only the compute node data when a node is going into error state.
        //                  Component 3 might only want data from the Job table when jobs go into ended state.
        //      - The DataReceiver may also create "Delta summary / Aggregated summary" records that would for instance summarize the state of the system at possibly 1 minute intervals.
        //          Before implementing these summary records it should be discussed with Todd and the GUI/UI owner to ascertain if this functionality is needed!
        //----------------------------------------------
        for (int iVtCntr=0; iVtCntr < aVtJson.length; ++iVtCntr) {
            // Send this table's changes to the Tier2 DataReceiver.
            sendThisTablesChangesToTier2(lEndIntvlTimeMs, sEndIntvlTimestamp, sStartIntvlTimestamp,
                    adapter.dataMoverResultTblIndxToTableNameMap().get(iVtCntr), aVt[iVtCntr].getRowCount(), aVtJson[iVtCntr], oDataMover);
        }

        //--------------------------------------------------------------
        // Save restart data indicating the timestamp of the last data that was moved from Tier1 to Tier2.
        //--------------------------------------------------------------
        String sRestartData = "Processed through (" + TimestampPrefix + sEndIntvlTimestamp + ") (" + IntervalIdPrefix + mDataMoverIntervalId + ") (" + AmqpMessageIdPrefix + mDataMoverAmqpMessageId + ")";
        workQueue.saveWorkItemsRestartData(workQueue.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)

        // Set the "next" value that will be used as the previously processed timestamp (indicates which data has already been moved).
        mDataMoverPrevProcessedTimeMs = lEndIntvlTimeMs;
    }   // End handleDataNeedingToMoveFromTier1ToTier2(DataMoverAmqp oDataMover)


    @SuppressWarnings("unchecked") /*To temporarily suppress unchecked warnings when using JSONObject*/
    private final void sendThisTablesChangesToTier2(long lEndIntvlTimeMs, String sEndIntvlTimestamp, String sStartIntvlTimestamp,
                                                    String sTableName, long lNumRowsOfDataInThisTable, String sThisTablesInfoAsJson,
                                                    DataMoverAmqp oDataMover) throws IOException, ConfigIOParseException
    {
        DecimalFormat decimalFormatter = new DecimalFormat("#,###,###");  // pretty formatting
        final long    NumRowsPerAmqpMsg = 1000L;  // max number of "data" entries to include in a AMQP message.

        // Check & see if there is any data to be sent for this table.
        if (sThisTablesInfoAsJson != null) {
            //logger().info(" aVtJson[%02d] = %s", iVtCntr, sThisTablesInfoAsJson);  // display the json-formatted string for this table.

            // Get the information out of the table's results.
            PropertyMap  jsonTableObject         = mParser.fromString(sThisTablesInfoAsJson).getAsMap();
            PropertyArray   listOfSchemaEntries     = jsonTableObject.getArrayOrDefault("schema", null);
            PropertyArray   listOfTableDataEntries  = jsonTableObject.getArrayOrDefault("data", null);
            String oStatus = jsonTableObject.getStringOrDefault("status", null);

            if (listOfSchemaEntries == null || listOfTableDataEntries == null || oStatus == null) {
                log_.warn("DataMover sent: Errors in parsing Json object");
                return;
            }
            long lStatus = Long.parseLong(oStatus);

            //--------------------------------------
            // Build json messages containing the changes to this table and put them onto the DataMover queue.
            //--------------------------------------
            long lTotalNumPartsForThisTable = (lNumRowsOfDataInThisTable + (NumRowsPerAmqpMsg-1)) / NumRowsPerAmqpMsg;
            PropertyMap jsonAmqpObj = new PropertyMap();
            jsonAmqpObj.put("EOM", false);
            jsonAmqpObj.put("IntervalId", mDataMoverIntervalId);
            jsonAmqpObj.put("EndIntvlTsInMsSinceEpoch",   lEndIntvlTimeMs);                 // timestamp in form of millisecs since epoch
            jsonAmqpObj.put("StartIntvlTsInMsSinceEpoch", mDataMoverPrevProcessedTimeMs);   // timestamp in form of millisecs since epoch
            jsonAmqpObj.put("TableName", sTableName);
            // Include the status field from the json representation of the VoltTable.
            jsonAmqpObj.put("status", lStatus);
            // Add the schema information to only the first "part" of the table's info.
            jsonAmqpObj.put("schema", listOfSchemaEntries);

            // Include the rows of "data" from the table (up to the number allowed per amqp message).
            Iterator<?> itTableDataEntries = listOfTableDataEntries.iterator();
            for (int lThisMsgsPartNum=1; lThisMsgsPartNum <= lTotalNumPartsForThisTable; ++lThisMsgsPartNum) {
                jsonAmqpObj.put("Part", lThisMsgsPartNum);              // which message is this, of the messages for this table.
                jsonAmqpObj.put("Of", lTotalNumPartsForThisTable);  // how many total messages are there for this table.
                jsonAmqpObj.put("AmqpMessageId", ++mDataMoverAmqpMessageId); // this specific AmqpMessage's id.
                // Add the appropriate number of data rows to this message.
                PropertyArray jaDataRows = new PropertyArray();
                int iNumRowsAddedToAmqpMsg = 0;
                while((iNumRowsAddedToAmqpMsg < NumRowsPerAmqpMsg) && (itTableDataEntries.hasNext()))
                {
                    jaDataRows.add(itTableDataEntries.next());
                    ++iNumRowsAddedToAmqpMsg;
                }
                jsonAmqpObj.put("data", jaDataRows);
                // Put this message onto the DataMover queue (MessageProperties.PERSISTENT_BASIC says to mark this message as persistent, i.e. save the message to disk).
                oDataMover.getChannel().basicPublish("", Adapter.DataMoverQueueName, MessageProperties.PERSISTENT_BASIC, mParser.toString(jsonAmqpObj).getBytes(StandardCharsets.UTF_8));
                log_.info("DataMover sent AmqpMessageId=%d - IntervalId=%d, EndIntervalTs=%s, StartIntervalTs=%s, AmqpQueue=%s, TableName=%s, Part %d Of %d, NumDataRows=%s",
                          mDataMoverAmqpMessageId, mDataMoverIntervalId, sEndIntvlTimestamp, sStartIntvlTimestamp, Adapter.DataMoverQueueName,
                          sTableName, lThisMsgsPartNum, lTotalNumPartsForThisTable, decimalFormatter.format(iNumRowsAddedToAmqpMsg));
            }   // build json messages containing the changes to this table and put them onto the DataMover queue.
        }   // there is data to be sent for this table.
    }   // End sendThisTablesChangesToTier2(...)


    //--------------------------------------------------------------------------
    // This method checks for and prevents a constant stream of the DataMover moving only DataMover/DataReceiver work items, this "recursion" occurs because we can't update the DataMover/DataReceiver work item's WorkingResults field
    // until AFTER that DataMover interval has finished, which means that in the next interval it would then send the DataMover update, which results in an update, which would then be sent, on, on, on).
    //--------------------------------------------------------------------------
    private final long seeIfInDataMoverRecursion(int iNumRecsBeingMoved, VoltTable[] aVt, DecimalFormat decimalFormatter) {
        int iDataMoverWi=0, iDataReceiverWi=0;  // count the number of DataMover and DataReciver work items that occurred (for short-circuiting the "recurse" problem).
        boolean bGoAheadAndMoveThisData = false;
        if (iNumRecsBeingMoved >= 3) {
            // there are at least 3 rows of data to be moved - so not in the recursion flow.
            // Indicate that something else besides data mover work items is being sent this time.
            mLastIntvlWasOnlyDataMoverWIs = false;
            // Go ahead and move this data.
            bGoAheadAndMoveThisData = true;
            log_.debug("DataMover - recurse check - there are a total of %s rows of data moving this interval, so no recurse is occurring", decimalFormatter.format(iNumRecsBeingMoved));
        }   // there are at least 3 rows of data to be moved - so not in the recursion flow.
        else {
            // less than 3 rows of data so we might be in the "recursion flow".
            // Check & see if the only items available to be moved are from WorkItem table.
            for (int iVtCntr=0; ((iVtCntr < aVt.length) && !bGoAheadAndMoveThisData); ++iVtCntr) {
                // See if this table had any data to be moved.
                if (aVt[iVtCntr].getRowCount() != 0) {
                    // this table had at least one row to be moved.
                    String value = adapter.dataMoverResultTblIndxToTableNameMap().get(iVtCntr);
                    if (value != null && !value.equals("WorkItem")) {
                        // at least 1 of the records to be moved is not from the WorkItem table, go ahead and move this data.
                        // Indicate that something else besides data mover work items is being sent this time.
                        mLastIntvlWasOnlyDataMoverWIs = false;
                        // Go ahead and move this data.
                        bGoAheadAndMoveThisData = true;
                        log_.debug("DataMover - recurse check - there is at least 1 row from a non-WorkItem table, so no recurse is occurring");
                    }
                    else {
                        // this record is from the WorkItem table
                        // Count the number of DataMover and DataReceiver work items being moved.
                        VoltTable vt = aVt[iVtCntr];  // get addressability to the volt table for this table.
                        vt.resetRowPosition();  // reset so we start again at the beginning of the VoltTable (since we have already advanced through it once).
                        for (int iRowCntr = 0; iRowCntr < vt.getRowCount(); ++iRowCntr) {
                            vt.advanceRow();
                            String sTemp = vt.getString("WorkToBeDone");
                            //logger().info("DataMover - sTemp=%s, iRowCntr=%d, vt.getRowCount()=%d", sTemp, iRowCntr, vt.getRowCount());
                            // Increment the appropriate counters for DataMover and DataReceiver WorkItems.
                            if (sTemp.equals("DataMover")) {
                                ++iDataMoverWi;
                            }
                            else if (sTemp.equals("DataReceiver")) {
                                ++iDataReceiverWi;
                            }
                        }
                    }
                }
            }
            // Check & see if we have at most 1 DataMover work item, at most 1 DataReceiver work item, AND the number of DataMover plus number of DataReceiver = entire number of records being moved.
            if ((bGoAheadAndMoveThisData == false) && (iDataMoverWi <= 1) && (iDataReceiverWi <= 1) && ((iDataMoverWi + iDataReceiverWi) == iNumRecsBeingMoved)) {
                // could be a recurse situation - this set of data to be moved consisted only DataMover/DataReceiver work items (and at most 1 of each).
                // Check & see if the PREVIOUS interval also only consisted of only DataMover/DataReceiver work items (and at most 1 of each).
                if (mLastIntvlWasOnlyDataMoverWIs == true) {
                    // the last/previous interval did only consist of DataMover/DataReceiver work items (and at most 1 of each).
                    // This is a "recurse" situation that we want to suppress.
                    log_.debug("DataMover - recurse check - this IS A RECURSE that is being suppressed, iDataMoverWi=%s, iDataReceiverWi=%s, iNumRecsBeingMoved=%s",
                            decimalFormatter.format(iDataMoverWi), decimalFormatter.format(iDataReceiverWi), decimalFormatter.format(iNumRecsBeingMoved));
                    // Indicate that ONLY data mover work items were moved this time.
                    mLastIntvlWasOnlyDataMoverWIs = true;
                    // Do NOT move this data (this is a "recurse" situation).
                    return -1;  // this is a "recurse" situation so we do not want to send this data via DataMover/DataReceiver.
                }   // the last/previous interval did only consist of DataMover/DataReceiver work items (and at most 1 of each).
                else {
                    // the previous interval did NOT only consist of only DataMover/DataReceiver work items (and at most 1 of each).
                    // This is not yet a "recurse" situation, but if a similar set of records occurs in the next interval that one will be a recurse situation.
                    // Indicate that ONLY data mover work items were moved this time.
                    mLastIntvlWasOnlyDataMoverWIs = true;
                    // Go ahead and move this data.
                    bGoAheadAndMoveThisData = true;
                    log_.debug("DataMover - recurse check - previous interval was NOT made up of only DataMover/DataReceiver work items, so no recurse is occurring (but next one may be)");
                }   // the previous interval did NOT only consist of only DataMover/DataReceiver work items (and at most 1 of each).
            }   // could be a recurse situation - this set of data to be moved consisted only DataMover/DataReceiver work items (and at most 1 of each).
            else {
                // more than 1 DataMover work item, or more than 1 DataReceiver work item, or there are records other than DataMover plus DataReceiver records.
                // Indicate that something else besides data mover work items is being sent this time.
                mLastIntvlWasOnlyDataMoverWIs = false;
                // Go ahead and move this data.
                bGoAheadAndMoveThisData = true;
                log_.debug("DataMover - recurse check - the number of DataMover/DataReceiver work items that are being moved is inconsistent with a recurse, so no recurse is occurring, iDataMoverWi=%s, iDataReceiverWi=%s, iNumRecsBeingMoved=%s",
                        decimalFormatter.format(iDataMoverWi), decimalFormatter.format(iDataReceiverWi), decimalFormatter.format(iNumRecsBeingMoved));
            }   // more than 1 DataMover work item, or more than 1 DataReceiver work item, or there are records other than DataMover plus DataReceiver records.
        }   // less than 3 rows of data so we might be in the "recursion flow".
        assert(bGoAheadAndMoveThisData);
        return 0L;
    }   // End seeIfInDataMoverRecursion(int iNumRecsBeingMoved, VoltTable[] aVt, DecimalFormat decimalFormatter)



    public static void main(String[] args) throws IOException, TimeoutException {
        Logger logger = LoggerFactory.getInstance("ONLINE_TIER", AdapterOnlineTierVolt.class.getName(), "console");
        AdapterSingletonFactory.initializeFactory("ONLINE_TIER", AdapterOnlineTierVolt.class.getName(), logger);
        final AdapterOnlineTierVolt obj = new AdapterOnlineTierVolt(logger);
        obj.initializeAdapter();
        // Start up the main processing flow for OnlineTier adapters.
        obj.mainProcessingFlow(args);
    }   // End main(String[] args)

}   // End class AdapterOnlineTierVolt
