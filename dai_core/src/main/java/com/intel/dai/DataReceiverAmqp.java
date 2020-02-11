package com.intel.dai;

import java.io.IOException;
import com.rabbitmq.client.*;
import java.util.concurrent.TimeoutException;
import com.intel.logging.Logger;

//--------------------------------------------------------------------------
// Class that handles the AMQP functionalities needed for the Data Receiver portion of the Data Mover.
// - Receives off a queue (data being moved from Tier1 to Tier2 data store) and puts the data into Tier2 tables
// - Publishes data received from Tier1 on a pub-sub so it is available for any subscribers
//--------------------------------------------------------------------------
public class DataReceiverAmqp {
    // Class constructor
    DataReceiverAmqp(String sHost, IAdapter adapter, Logger log, long lWorkItemId) throws IOException, TimeoutException {
        mFactory = new ConnectionFactory();
        mFactory.setHost(sHost);
        mFactory.setAutomaticRecoveryEnabled(true);
        // Connect to the AMQP (RabbitMQ).
        boolean bSuccessfulConnection = false;  int iConnectionRetryCntr = 0;
        while (!bSuccessfulConnection) {
            try {
                mConnection = mFactory.newConnection();  // abstracts the socket connection and takes care of protocol version negotiation and authentication (represents a real TCP connection to the message broker).
                if (mConnection != null) {
                    bSuccessfulConnection = true;
                    log.info("Successfully connected to AMQP (RabbitMQ)");
                }
                else
                    log.error("ConnectionFactory returned a null connection - will keep trying!");
            }
            catch (IOException e) {
                if (iConnectionRetryCntr++ == 0) {
                    // only cut this RAS event the first time we try, NOT every time we retry the connection!
                    // Cut RAS event indicating that we currently cannot connect to RabbitMQ and that we will retry until we can.
                    adapter.logRasEventNoEffectedJob(adapter.getRasEventType("RasGenAdapterUnableToConnectToAmqp")
                            ,("AdapterName=" + adapter.adapterName() + ", QueueName=" + Adapter.DataMoverQueueName)
                            ,null                               // Lctn
                            ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                            ,adapter.adapterType()              // type of the adapter that is requesting/issuing this stored procedure
                            ,lWorkItemId                        // requesting work item id
                            );
                }
                log.error("Unable to connect to AMQP (RabbitMQ) - will keep trying!");
                try { Thread.sleep(5 * 1000); }  catch (Exception e2) { log.exception(e2); }
            }
        }
        mChannel = mConnection.createChannel();     // channel has most of the API for getting things done resides (virtual connection or AMQP connection) - you can use 1 channel for everything going via the tcp connection.
        assert mChannel != null : "No RabbitMQ channel created!";
        // Create a queue that is used by the DataMover to send Tier1 data to Tier2 - set up so messages have to be manually acknowledged and won't be lost.
        mChannel.queueDeclare(Adapter.DataMoverQueueName, Durable, false, false, null);  // set up our queue from DataMover to the DataReceiver.
        // Configure this consumer of DataMover queue messages to prefetch up to 100 message at a time from the queue.
        final int PrefetchCount = 100;
        mChannel.basicQos(PrefetchCount, false); // this sets the limit for this consumer only.
        // Create an exchange for sending pub-sub traffic to all subscribers (this is idempotent so won't hurt if the exchange has already been set up).
        mChannel.exchangeDeclare(Adapter.DataMoverExchangeName, "topic");  // set up our topic-type exchange.
    }   // End ctor

    void close() throws IOException, TimeoutException {
        mChannel.close();
        mConnection.close();
    }

    Channel getChannel()  { return mChannel; }

    // Member data
    final static boolean    Durable = true;  // make sure that RabbitMQ will never lose our QUEUE.
    ConnectionFactory       mFactory;
    Connection              mConnection;
    Channel                 mChannel;
}   // End class DataReceiverAmqp
