// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import java.util.BitSet;
import static java.lang.Math.toIntExact;
import org.voltdb.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handle the database processing that is necessary to store a RAS event.
 * NOTE:
 *
 *  Returns: long lUniqueId = The id generated for this instance of the RAS event.  NOTE: it will be a negative number (-1 * lUniqueId) if there IS a Control Operation associated with this ras event!
 *
 *  Input parameter:
 *      String  sDescriptiveName = Identifies which type of event occurred, e.g., "RasGenAdapterAbend", "RasWorkItemFindAndOwnFailed"
 *      String  sInstanceData    = Data specific to this instance of the event, may be appended to the Event message to have the complete information
 *      String  sLctn            = Location of the hardware that the event occurred on.  Note: string of "" indicates Lctn should be set to NULL in db
 *      String  sJobId           = Job Id - Note: value of null, indicates that JobId in data store record should be set to null.
 *      long    lTsInMicroSecs   = Time that the event that triggered this RAS Event occurred
 *      String  sReqAdapterType  = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId   = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class RasEventStore extends VoltProcedure {

    public final SQLStmt selectUniqueIdSql = new SQLStmt("SELECT NextValue FROM UniqueValues WHERE Entity = ? Order By Entity;");
    public final SQLStmt updateUniqueIdSql = new SQLStmt("UPDATE UniqueValues SET NextValue = NextValue + 1, DbUpdatedTimestamp = ? WHERE Entity = ?;");
    public final SQLStmt insertUniqueIdSql = new SQLStmt("INSERT INTO UniqueValues (Entity, NextValue, DbUpdatedTimestamp) VALUES (?, ?, ?);");

    public final SQLStmt selectRasEventControlOperationSql = new SQLStmt("SELECT ControlOperation FROM RasMetaData WHERE DescriptiveName = ?;");
    public final SQLStmt insertRasEventSql = new SQLStmt("INSERT INTO RasEvent (Id, DescriptiveName, Lctn, JobId, ControlOperation, Done, InstanceData, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                                                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");



    public long run(String sDescriptiveName, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Generate a unique id for this new RAS event.
        //--------------------------------------------------
        // Get the current "next unique id" for the specified entity.
        voltQueueSQL(selectUniqueIdSql, EXPECT_ZERO_OR_ONE_ROW, sDescriptiveName);
        VoltTable[] uniqueId = voltExecuteSQL();
        // Check and see if there is a matching record for the specified entity
        if (uniqueId[0].getRowCount() == 0) {
            // No matching record for the specified entity - add a new row for the specified entity
            voltQueueSQL(insertUniqueIdSql, sDescriptiveName, 1, this.getTransactionTime());
            voltExecuteSQL();
            // Now redo the above query (to get the current "next unique id" for the specified entity)
            voltQueueSQL(selectUniqueIdSql, EXPECT_ONE_ROW, sDescriptiveName);
            uniqueId = voltExecuteSQL();
        }
        // Save away the generated unique adapter id.
        long lUniqueId = uniqueId[0].asScalarLong();
        // Bump the current "next unique id" to generate the next "next unique id" for the specified entity.
        voltQueueSQL(updateUniqueIdSql, EXPECT_ONE_ROW, this.getTransactionTime(), sDescriptiveName);
        voltExecuteSQL();

        //--------------------------------------------------
        // Get this RAS event's Control Operation from its meta data
        //--------------------------------------------------
        boolean bCntrlActnPrsnt = false;
        String sEventsControlOperation = null;
        voltQueueSQL(selectRasEventControlOperationSql, EXPECT_ZERO_OR_ONE_ROW, sDescriptiveName);
        VoltTable[] aRasMetaData = voltExecuteSQL();
        if (aRasMetaData[0].getRowCount() == 0) {
            // There is NO meta data for this RAS DescriptiveName
            sEventsControlOperation = "@@@Unknown-NoMetaData@@@";  // there is no meta data, so don't know what the Control Operations are for this RAS Event
        } else {
            aRasMetaData[0].advanceRow();
            sEventsControlOperation = aRasMetaData[0].getString("ControlOperation");
            if (aRasMetaData[0].wasNull()) {
                sEventsControlOperation = "";
                // System.out.println("RasEventStore - ControlOperation was indeed NULL");
            }
            else {
                // there was a control operation for this ras event
                bCntrlActnPrsnt = true;
            }
        }

        //---------------------------------------------------------------------
        // Insert a new row into the RasEvent table
        //---------------------------------------------------------------------
        if ((sLctn != null) && (sLctn.length() == 0))
            sLctn  = null;
        if ((sJobId != null) && ((sJobId.length() == 0) || (sJobId.equals("null"))))
            sJobId = null;
        if (sEventsControlOperation.length() == 0)
            sEventsControlOperation = null;
        if (sInstanceData != null)
            sInstanceData = sInstanceData.substring(0, Math.min(sInstanceData.length(), 500));
        // Determine if there is anything more that the RAS adapter has to do with this event (or is this event done).
        String sDone = "X";  // unknown value of the Done flag.
        if ((sEventsControlOperation != null) ||        // Need to run a ControlOperation.
            ((sJobId != null) && (sJobId.equals("?")))) // Need to fill-in job id.
            // the event is not yet done (there is more for the RAS adapter to do with this event).
            sDone = "N";
        else
            // the event is done (there is nothing more for the RAS adapter to do with this event)
            sDone = "Y";
        // Insert this ras event into the RasEvent table.
        voltQueueSQL(insertRasEventSql
                    ,lUniqueId                  // Ras Event Id
                    ,sDescriptiveName           // DescriptiveName
                    ,sLctn                      // Lctn
                    ,sJobId                     // JobId
                    ,sEventsControlOperation    // ControlOperation
                    ,sDone                      // Done
                    ,sInstanceData              // InstanceData
                    ,this.getTransactionTime()  // Time that this record was inserted into data store (DbUpdatedTimestamp)
                    ,lTsInMicroSecs             // Time that this RAS Event was triggered (LastChgTimestamp)
                    ,sReqAdapterType            // LastChgAdapterType
                    ,lReqWorkItemId             // LastChgWorkItemId
                    );
        voltExecuteSQL(true);

        // Return this new RAS event's id to the caller.
        if (bCntrlActnPrsnt)
            return (-lUniqueId);  // Since there was a control operation associated with this ras event, send back negative form of the unique id (to indicate that there was a control operation)
        else
            return lUniqueId;
    }
}