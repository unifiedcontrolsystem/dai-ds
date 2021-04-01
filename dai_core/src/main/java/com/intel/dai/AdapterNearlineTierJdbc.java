// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import java.sql.SQLException;

import com.intel.logging.*;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.perflogging.BenchmarkHelper;
import org.voltdb.client.*;
import org.voltdb.VoltTable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import com.intel.properties.*;
import com.intel.config_io.*;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.DataLoaderApi;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;

/**
 * AdapterNearlineTierJdbc for the tier 2 database.
 *
 * Params:
 * Connection details:
 * - URL
 * - user
 * - password
 *
 * Example invocation:
 * java AdapterNearlineTierPsql jdbc:postgresql://localhost:5433/ds_tier2 dsuser ds@123
 */
public class AdapterNearlineTierJdbc extends AdapterNearlineTier {
    static final String TimestampPrefix     = "Timestamp=";
    static final String IntervalIdPrefix    = "IntervalId=";
    static final String AmqpMessageIdPrefix = "AmqpMessageId=";
    static final String TableNamePrefix     = "TableName=";

    // Member data

    // Used for interacting with the data mover (using AMQP)
    private long mPrevAmqpMessageId;
    private Consumer mAmqpDataReceiverMsgConsumer;

    // DB connection
    private java.sql.Connection mConn;
    private NearlineTableUpdater mTableUpdater;
    private DataLoaderApi dataLoader;

    private AtomicBoolean receivedEom;
    private BenchmarkHelper benchmarking_;

    // Constructor
    AdapterNearlineTierJdbc(DataStoreFactory dsFactory, Logger logger) throws TimeoutException, IOException, ClassNotFoundException, DataStoreException {
        super(logger);
        mPrevAmqpMessageId = 0L;
        mAmqpDataReceiverMsgConsumer = null;
        receivedEom = new AtomicBoolean(false);

        mTableUpdater = createNearlineTableUpdater(logger);

        dataLoader = dsFactory.createDataLoaderApi();

        benchmarking_ = new BenchmarkHelper("AdapterNearlineTier-Benchmarking.json",
                "/opt/ucs/log/AdapterNearlineTier-Benchmarking.json", 15);
        mTableUpdater.setBenchmarker(benchmarking_);

    }

    NearlineTableUpdater createNearlineTableUpdater(Logger logger) throws DataStoreException {
        return new NearlineTableUpdater(logger);
    }

    DataReceiverAmqp createDataReceiver(String host) throws IOException, TimeoutException {
        return new DataReceiverAmqp(host, adapter, log_, workQueue.workItemId());
    }

    void waitUntilFinishedProcessingMessages() throws InterruptedException {
        log_.info("Main thread sleeping until end of message processing...");
        do {
            Thread.sleep(SHUTDOWN_CHECK_INTERVAL_MS);

            // To finish processing messages, two things must be true:
            // - We must have received the signal to shut down
            // - We must have finished processing the messages in the queue (i.e. we must have received the data
            // mover's final message)
            benchmarking_.tick();
        } while (!adapter.adapterShuttingDown() || !receivedEom.get());
        log_.info("Shutdown signal and EOM from data mover received");
    }

    void signalEom() {
        receivedEom.set(true);
    }

    void stillProcessingMessages() {
        receivedEom.compareAndSet(true, false);
    }

    //--------------------------------------------------------------------------
    // Queue a work item(s) to my own type of adapter (AdapterNearlineTier) to start "handling"
    // the receipt of data from the "DataMover/DataSender" which is sending history data from Tier1
    // to Tier2.  We do not need nor want to receive notification of when it completes.
    //--------------------------------------------------------------------------
    public void queueWorkItemsToHandleDataReceiver()
            throws IOException, ProcCallException, InterruptedException {
        long lCountOfDataReceivers = adapter.client()
                .callProcedure("WorkItemCountDataReceivers", adapter.adapterType())
                .getResults()[0].asScalarLong();

        if (lCountOfDataReceivers == 0) {
            // Start up whichever DataReceiver instances should be running.
            String sWiParms = "";  // parameter values for the DataReceiver work item.
            long lWorkItemId = workQueue.queueWorkItem("NEARLINE_TIER", "StartUp", "DataReceiver", sWiParms,
                                                       false,  // no indication when work item finishes
                                                       adapter.adapterType(), workQueue.baseWorkItemId());
            log_.info("successfully queued DataReceiver work item - %s, %s, WorkItemId=%d",
                    "DataReceiver", sWiParms, lWorkItemId);
        } else {
            log_.info("did not start a DataReceiver work item, as there already is such a work item");
        }
    }

    //--------------------------------------------------------------------------
    // This method handles receipt of the historical data that is being moved from Tier1 to Tier2.
    // Note 1: this work item is different than most in that it runs for the length of time that
    // this adapter instance is active.  It does not start and then finish, it starts and stays
    // active handling any data that is being moved from Tier1 to Tier2.
    //--------------------------------------------------------------------------
    @Override
    public long receiveDataFromDataMover() throws IOException, ProcCallException, TimeoutException, InterruptedException, DataStoreException {
        // Set the previously used AmqpMessageId to 0 - this identifies the specific message that
        // was last used (the last successfully processed AmqpMessageId).
        mPrevAmqpMessageId = 0L;
        // Check & see if this is a requeued work item (we need to know so we can figure out how
        // far we already previously processed).
        if (!workQueue.isThisNewWorkItem()) {
            // REQUEUED work item - need to check for previous progress for this work item so we
            // know what to expect as far as the next Amqp message id.  Get the working results for
            // this requeued work item - so we can tell where we were at when the previous instance
            // terminated.
            if (workQueue.workingResults().startsWith("Processed through ")) {
                // found our last processed record.
                log_.info("In a requeued DataReceiver flow, working results = %s",
                        workQueue.workingResults());
                //--------------------------------------------------------------
                // Extract pertinent info from the working results field.
                //--------------------------------------------------------------
                String[] aWorkingResults = workQueue.workingResults().split("\\(");
                for (int iColsCntr = 0; iColsCntr < aWorkingResults.length; ++iColsCntr) {
                    // Previous AmqpMessageId.
                    if (aWorkingResults[iColsCntr].startsWith(AmqpMessageIdPrefix)) {
                        mPrevAmqpMessageId = Long.parseLong( aWorkingResults[iColsCntr].substring(AmqpMessageIdPrefix.length(), aWorkingResults[iColsCntr].indexOf(")")) );  // convert string representation of a number into a long.
                        log_.warn("starting with a previous AmqpMessageId=%d", mPrevAmqpMessageId);
                    }
                }   // loop through the input columns
            }
            else {
                log_.warn("This is a requeued work item but the working results field does NOT start with the expected string of (Processed through ), WorkingResults=%s!", workQueue.workingResults());
            }
        }   // REQUEUED work item

        // Setup AMQP for receiving data being moved from Tier1 to Tier2.
        DataReceiverAmqp oDataReceiver = createDataReceiver("localhost");
        mAmqpDataReceiverMsgConsumer = new AmqpDataReceiverMsgConsumer(oDataReceiver, mPrevAmqpMessageId, adapter, log_,
                mTableUpdater, this);

        // Start to consume messages containing data that is being moving from Tier1 to Tier2
        // (DataMover -> DataReceiver). It will push us messages asynchronously, using the
        // sub-class amqpDataReceiverMsgConsumer as a callback object.

        // During operation, mark Nearline tier as not valid until a graceful shutdown is achieved
        dataLoader.setNearlineTierValid(false);

        // do not automatically acknowledge this message upon receipt, rather we will manually
        // acknowledge it.
        log_.info("Start consuming messages...");
        boolean useManualAcknowledgement = false;
        oDataReceiver.getChannel().basicConsume(Adapter.DataMoverQueueName,
                                             useManualAcknowledgement,
                                             CONSUMER_TAG,
                                             mAmqpDataReceiverMsgConsumer);

        waitUntilFinishedProcessingMessages();

        log_.info("Canceling message consumer...");
        // Stop receiving messages
        oDataReceiver.getChannel().basicCancel(CONSUMER_TAG);

        // Shutdown the DataReceiver AMQP infrastructure.
        oDataReceiver.close();

        log_.info("Updating Nearline tier's status...");
        // Mark the Nearline tier valid for populating Online tier in the next startup
        dataLoader.setNearlineTierValid(true);
        return 0;
    }

    //--------------------------------------------------
    // This is the callback/logic that is invoked for each message that comes into the DataReceiver
    // queue (from the DataMover).
    //--------------------------------------------------
    final static class AmqpDataReceiverMsgConsumer extends DefaultConsumer {
        // Member data
        private DataReceiverAmqp   mDataReceiver;
        private Logger log_;
        private IAdapter adapter;
        private long mPrevAmqpMessageId;
        private NearlineTableUpdater mTableUpdater;
        private WorkQueue workQueue;
        private AdapterNearlineTierJdbc nearlineAdapter;

        // Constructor
        AmqpDataReceiverMsgConsumer(DataReceiverAmqp oDataReceiver, long previousId, IAdapter a, Logger l,
                                    NearlineTableUpdater tableUpdater, AdapterNearlineTierJdbc nearlineAdapter) {
            super(oDataReceiver.getChannel());
            mDataReceiver = oDataReceiver;
            mPrevAmqpMessageId = previousId;
            adapter = a;
            log_ = l;
            mTableUpdater = tableUpdater;
            workQueue = adapter.workQueue();
            this.nearlineAdapter = nearlineAdapter;
        }

        // Handle a message received from the DataMover message queue.
        @Override
        final public void handleDelivery(String consumerTag, Envelope envelope,
                                         AMQP.BasicProperties properties, byte[] body)
                                         throws IOException {
            SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            long lAmqpMessageId = -99999L;

            String sAmqpMsg="NotFilledIn"; long lIntervalId=-99999; String sTableName="NotFilledIn";
            try {
                // Grab data out of this message we just received.
                sAmqpMsg = new String(body, "UTF-8");
                PropertyMap jsonMsgObject= adapter.jsonParser().fromString(sAmqpMsg).getAsMap();

                // Grab header information
                boolean eom = jsonMsgObject.getBoolean("EOM");
                if (eom) {
                    nearlineAdapter.signalEom();
                    log_.info("Received EOM from data mover");
                    return;
                } else {
                    nearlineAdapter.stillProcessingMessages();
                }

                lAmqpMessageId = jsonMsgObject.getLong("AmqpMessageId");
                lIntervalId = jsonMsgObject.getLong("IntervalId");
                long lEndIntvlTimeMs = jsonMsgObject.getLong("EndIntvlTsInMsSinceEpoch");
                String sEndIntvlTimeMs = sdfSqlDateFormat.format(new Date(lEndIntvlTimeMs));
                long lStartIntvlTimeMs = jsonMsgObject.getLong("StartIntvlTsInMsSinceEpoch");
                String sStartIntvlTimeMs = sdfSqlDateFormat.format(new Date(lStartIntvlTimeMs));
                sTableName = jsonMsgObject.getString("TableName");

                // this message is part x of the total number of parts that make up this interval's
                // data for sTableName.
                long lThisMsgsPartNum = jsonMsgObject.getLong("Part");
                // the total number of parts that make up this interval's data for sTableName.
                long lTotalNumParts = jsonMsgObject.getLong("Of");

                log_.info("DataReceiver received AmqpMessageId=%d - TableName=%s, "
                              + "Part %d Of %d, IntervalId=%d, EndInterval=%s, StartInterval=%s, "
                              + "AmqpMsgLength=%d", lAmqpMessageId, sTableName,
                              lThisMsgsPartNum, lTotalNumParts, lIntervalId, sEndIntvlTimeMs,
                              sStartIntvlTimeMs, sAmqpMsg.length());

                if (sTableName == null) {
                    throw new RuntimeException("DataReceiver received an invalid message with empty table name");
                }

                // Ensure that this AmqpMessageId is "valid/expected" (should be monotonically
                // increasing with no skipped values).
                long lExpectedAmqpMessageId = mPrevAmqpMessageId + 1L;
                if (lAmqpMessageId != lExpectedAmqpMessageId) {
                    // invalid/unexpected AmqpMessageId!
                    // Cut a ras event to record that we received an unexpected (out of sequence) message id while receiving data from Tier1.
                    adapter.logRasEventNoEffectedJob("RasAntDataReceiverInvalidMsgId",
                                             ("IntervalId=" + lIntervalId
                                              + ", ReceivedAmqpMessageId=" + lAmqpMessageId
                                              + ", ExpectedAmqpMessageId="
                                              + lExpectedAmqpMessageId), null, // location
                                              // Time that the event that triggered this ras event
                                              // occurred, in microseconds since epoch
                                             System.currentTimeMillis() * 1000L,
                                             adapter.adapterType(),
                                             workQueue.workItemId());
                    log_.fatal("DataReceiver received an unexpected/out of sequence AmqpMessageId, data may have been lost - IntervalId=%d, ReceivedAmqpMessageId=%d, ExpectedAmqpMessageId=%d, AdapterType=%s, ThisAdapterId=%d!",
                               lIntervalId, lAmqpMessageId, lExpectedAmqpMessageId, adapter.adapterType(), adapter.adapterId());
                    // Note: after logging that this has occurred, continue and processing this message!
                }

                String sAmqpRoutingKey = sTableName;  // use the table name as the routing key.

                mDataReceiver.getChannel().basicPublish(Adapter.DataMoverExchangeName, sAmqpRoutingKey, null, adapter.jsonParser().toString(jsonMsgObject).getBytes());
                log_.info("Published AmqpMessageId=%d - IntervalId=%d, EndIntervalTs=%s, StartIntervalTs=%s, RoutingKey=%s, TableName=%s, Part %d Of %d",
                          lAmqpMessageId, lIntervalId, sEndIntvlTimeMs, sStartIntvlTimeMs, sAmqpRoutingKey,
                          sTableName, lThisMsgsPartNum, lTotalNumParts);

                // Reconstitute the original VoltTable from the specified Json string.
                VoltTable vtFromMsg = VoltTable.fromJSONString(sAmqpMsg);

                // Update the corresponding table in nearline tier
                mTableUpdater.Update(sTableName, vtFromMsg);

                log_.info("AmqpDataReceiverMsgConsumer - updated nearline table - "
                              + "AmqpMessageId=%d, TableName=%s", lAmqpMessageId, sTableName);

                // Save restart data indicating the timestamp of the last data that was moved from
                // Tier1 to Tier2.
                String sRestartData = "Processed through (" + TimestampPrefix + sEndIntvlTimeMs + ") (" + IntervalIdPrefix + lIntervalId + ") (" + AmqpMessageIdPrefix + lAmqpMessageId + ") (" + TableNamePrefix + sTableName + ")";
                // false means to update this workitem's history record rather than doing an insert
                // of another history record - this is "unusual" (only used when a workitem is
                // updating its working results fields very often)
                workQueue.saveWorkItemsRestartData(workQueue.workItemId(), sRestartData, false);
                // Save the AmqpMessageId that we just used (so we have it available for the next
                // message).
                mPrevAmqpMessageId = lAmqpMessageId;
            } catch (Exception e) {
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
            } finally {
                // Send a successful acknowledgment for receipt and successfully handling this
                // message!
                mDataReceiver.getChannel().basicAck(envelope.getDeliveryTag(), false);
                log_.debug("DataReceiver acknowledged AmqpMessageId=%d", lAmqpMessageId);
            }
        }
    }

    // TODO: App and adapter need to be decoupled.
    public static void main(String[] args)
            throws IOException, TimeoutException, SQLException, ConfigIOParseException, FileNotFoundException,
            ClassNotFoundException, DataStoreException {
        Logger logger = LoggerFactory.getInstance("NEARLINE_TIER", AdapterNearlineTierJdbc.class.getName(), "log4j2");
        AdapterSingletonFactory.initializeFactory("NEARLINE_TIER", AdapterNearlineTierVolt.class.getName(), logger);
        DataStoreFactory dsFactory = new DataStoreFactoryImpl(args, logger);
        final AdapterNearlineTierJdbc obj = new AdapterNearlineTierJdbc(dsFactory, logger);

        // Start up the main processing flow for NearlineTier adapters.
        obj.mainProcessingFlow(args);
    }
}
