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
 *  Returns: long lUniqueId = The id generated for this instance of the ras event.  NOTE: it will be a negative number (-1 * lUniqueId) if there IS a Control Operation associated with this ras event!
 *
 *  Input parameter:
 *      String  sEventType      = Identifies which type of event occurred, e.g., "0001000001", "0001000005", "0005000007"
 *      String  sInstanceData   = Data specific to this instance of the event, may be appended to the Event message to have the complete information
 *      String  sLctn           = Location of the hardware that the event occurred on.  Note: string of "" indicates Lctn should be set to NULL in db
 *      String  sJobId          = Job Id - Note: value of null, indicates that JobId in data store record should be set to null.
 *      long    lTsInMicroSecs  = Time that the event that triggered this RAS Event occurred
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class RasEventStore extends VoltProcedure {

    public final SQLStmt selectUniqueIdSql = new SQLStmt("SELECT NextValue FROM UniqueValues WHERE Entity = ? Order By Entity;");
    public final SQLStmt updateUniqueIdSql = new SQLStmt("UPDATE UniqueValues SET NextValue = NextValue + 1, DbUpdatedTimestamp = ? WHERE Entity = ?;");
    public final SQLStmt insertUniqueIdSql = new SQLStmt("INSERT INTO UniqueValues (Entity, NextValue, DbUpdatedTimestamp) VALUES (?, ?, ?);");

    public final SQLStmt selectRasEventControlOperationSql = new SQLStmt("SELECT ControlOperation FROM RasMetaData WHERE EventType = ?;");
    public final SQLStmt insertRasEventSql = new SQLStmt("INSERT INTO RasEvent (Id, EventType, Lctn, JobId, ControlOperation, InstanceData, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                                                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");



    public long run(String sEventType, String sInstanceData, String sLctn, String sJobId, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Generate a unique id for this new RAS event.
        //--------------------------------------------------
        final String Entity = sEventType.toUpperCase();
        // Get the current "next unique id" for the specified entity.
        voltQueueSQL(selectUniqueIdSql, EXPECT_ZERO_OR_ONE_ROW, Entity);
        VoltTable[] uniqueId = voltExecuteSQL();
        // Check and see if there is a matching record for the specified entity
        if (uniqueId[0].getRowCount() == 0) {
            // No matching record for the specified entity - add a new row for the specified entity
            voltQueueSQL(insertUniqueIdSql, Entity, 1, this.getTransactionTime());
            voltExecuteSQL();
            // Now redo the above query (to get the current "next unique id" for the specified entity)
            voltQueueSQL(selectUniqueIdSql, EXPECT_ONE_ROW, Entity);
            uniqueId = voltExecuteSQL();
        }
        // Save away the generated unique adapter id.
        long lUniqueId = uniqueId[0].asScalarLong();
        // Bump the current "next unique id" to generate the next "next unique id" for the specified entity.
        voltQueueSQL(updateUniqueIdSql, EXPECT_ONE_ROW, this.getTransactionTime(), Entity);
        voltExecuteSQL();

        //--------------------------------------------------
        // Get this RAS event's Control Operation from its meta data
        //--------------------------------------------------
        boolean bCntrlActnPrsnt = false;
        String sEventsControlOperation;
        voltQueueSQL(selectRasEventControlOperationSql, EXPECT_ZERO_OR_ONE_ROW, Entity);
        VoltTable[] aRasMetaData = voltExecuteSQL();
        if (aRasMetaData[0].getRowCount() == 0) {
            // There is NO meta data for this RAS EventType
            sEventsControlOperation = "@@@Unknown@@@";  // there is no meta data, so don't know what the Control Operations are for this RAS Event
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

        voltQueueSQL(insertRasEventSql
                    ,lUniqueId                  // Ras Event Id
                    ,Entity                     // EventType
                    ,sLctn                      // Lctn
                    ,sJobId                     // JobId
                    ,sEventsControlOperation    // ControlOperation
                    ,sInstanceData              // InstanceData
                    ,this.getTransactionTime()  // Time that this record was inserted into data store (DbUpdatedTimestamp)
                    ,lTsInMicroSecs             // Time that this RAS Event was triggered (LastChgTimestamp)
                    ,sReqAdapterType            // LastChgAdapterType
                    ,lReqWorkItemId             // LastChgWorkItemId
                    );
        voltExecuteSQL(true);

        // Return this new RAS event's id to the caller.
        if (bCntrlActnPrsnt) {
            return (-lUniqueId);  // Since there was a control operation associated with this ras event, send back negative form of the unique id (to indicate that there was a control operation)
        } else {
            return lUniqueId;
        }
    }
}