// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import java.io.IOException;
import com.rabbitmq.client.*;
import java.util.concurrent.TimeoutException;
import com.intel.logging.Logger;

//--------------------------------------------------------------------------
// Class for the AMQP sending of Tier1 data to Tier2.
//--------------------------------------------------------------------------
public class DataMoverAmqp {
    // Class constructor
    DataMoverAmqp(String sHost, IAdapter adapter, Logger log, long lWorkItemId) throws IOException, TimeoutException {
        mFactory = new ConnectionFactory();
        mFactory.setHost(sHost);
        mFactory.setAutomaticRecoveryEnabled(true);
        // Connect to the AMQP (RabbitMQ).
        boolean bSuccessfulConnection = false;
        int iConnectionRetryCntr = 0;
        while (!bSuccessfulConnection && !adapter.adapterShuttingDown()) {
            try {
                mConnection = mFactory.newConnection();  // abstracts the socket connection and takes care of protocol version negotiation and authentication (represents a real TCP connection to the message broker).
                if (mConnection != null) {
                    bSuccessfulConnection = true;
                    log.info("Successfully connected to AMQP (RabbitMQ)");
                }
                else
                    log.error("ConnectionFactory returned a null connection - will keep trying!");
            }
            catch (Exception e) {
                if (iConnectionRetryCntr++ == 0) {
                    // only cut this RAS event the first time we try, NOT every time we retry the connection!
                    // Cut RAS event indicating that we currently cannot connect to RabbitMQ and that we will retry until we can.
                    adapter.logRasEventNoEffectedJob("RasGenAdapterUnableToConnectToAmqp"
                            ,("AdapterName=" + adapter.adapterName() + ", QueueName=" + Adapter.DataMoverQueueName)
                            ,null                               // Lctn
                            ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                            ,adapter.adapterType()              // type of the adapter that is requesting/issuing this stored procedure
                            ,lWorkItemId                        // requesting work item id
                            );
                }
                log.error("Unable to connect to AMQP (RabbitMQ) - will keep trying!");
                try { Thread.sleep(5 * 1000); }  catch (Exception e2) {}
            }
        }
        if(adapter.adapterShuttingDown() || mConnection == null)
            throw new IOException("During DataMover setup the adapter started shutting down!");
        mChannel = mConnection.createChannel();     // channel has most of the API for getting things done resides (virtual connection or AMQP connection) - you can use 1 channel for everything going via the tcp connection.
        // Create a queue that is used by the DataMover for sending Tier1 data to Tier2 - set up so messages have to be manually acknowledged and won't be lost.
        mChannel.queueDeclare(Adapter.DataMoverQueueName, Durable, false, false, null);  // set up our queue from DataMover to the DataReceiver.
    }   // End ctor

    void close() throws IOException, TimeoutException {
        mChannel.close();
        mConnection.close();
    }

    Channel getChannel()  { return mChannel; }

    // Member data
    final boolean           Durable = true;  // make sure that RabbitMQ will never lose our QUEUE.
    ConnectionFactory       mFactory;
    Connection              mConnection;
    Channel                 mChannel;
}   // End class DataMoverAmqp
