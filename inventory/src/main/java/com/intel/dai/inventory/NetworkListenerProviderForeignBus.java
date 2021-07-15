// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.dai.dsapi.BootState;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.inventory.api.HWInvNotificationTranslator;
import com.intel.dai.inventory.api.pojo.scn.ForeignHWInvChangeNotification;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Description of class BootEventTransformer.
 */
public class NetworkListenerProviderForeignBus implements NetworkListenerProvider, Initializer {
    public NetworkListenerProviderForeignBus(Logger logger) {
        log_ = logger;
    }

    @Override
    public void initialize() { /* Not used but is required */ }

    /**
     * <p> Callback from com.intel.dai.network_listener.processMessage().
     * Translates the json in the data String into CommonDataFormat.  The json contains a list of components
     * that shares a common state update.  This translate into an array of CommonDataFormat entries, each
     * describing a single component. </p>
     *
     * @param topic The topic or topic associated with this message.
     * @param inventoryJson state change notification as a json string
     * @param config network listener configuration
     * @return an array of CommonDataFormat entries equivalent to scnJson
     * @throws NetworkListenerProviderException network listener provider exception
     */
    @Override
    public List<CommonDataFormat> processRawStringData(String topic, String inventoryJson, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        log_.debug("Received %s: %s", topic, inventoryJson);

        // First start with performing the voltdb update right here
        // CMC_TODO: Refactor working code into actOnData; actually, it is not worth it

        DatabaseSynchronizer synchronizer = new DatabaseSynchronizer(log_, config_);
        switch (topic) {
            case "kafka_dimm":
                synchronizer.ingestRawDimm(new ImmutablePair<>(topic, inventoryJson));
                break;
            case "kafka_fru_host":
                synchronizer.ingestRawFruHost(new ImmutablePair<>(topic, inventoryJson));
                break;
            default:
                log_.error("Unexpected kafka topic: %s", topic);
                break;
        }

//        DatabaseSynchronizer ds = new DatabaseSynchronizer(log_, null, config);
//        long currentTimestamp = currentUTCInNanoseconds();
//
//        ForeignHWInvChangeNotification foreignInventoryChangeNotification =
//                new HWInvNotificationTranslator(log_).toPOJO(scnJson);
//        if (foreignInventoryChangeNotification == null) {
//            throw new NetworkListenerProviderException("Cannot extract foreignInventoryChangeNotification");
//        }
//
//        BootState newComponentState = toBootState(foreignInventoryChangeNotification.State);
//        if (newComponentState == null) {
//            log_.info("HWI:%n  ignoring unsupported scn notification state: %s",
//                    foreignInventoryChangeNotification.State);
//            return new ArrayList<>();
//        }
//
        List<CommonDataFormat> workItems = new ArrayList<>();
//        for (String foreign: foreignInventoryChangeNotification.Components) {
//            String location;
//            try {
//                location = CommonFunctions.convertForeignToLocation(foreign);  // this fails if translation map is outdated
//            } catch(ConversionException e) {
//                location = foreign; // for debugging purpose
////                throw new NetworkListenerProviderException(
////                        "Failed to convert the foreign location to a DAI location", e);
//            }
//
//            CommonDataFormat workItem = new CommonDataFormat(
//                    currentTimestamp,
//                    location,
//                    DataType.InventoryChangeEvent);
//            workItem.setStateChangeEvent(newComponentState);
//            workItem.storeExtraData(FOREIGN_KEY, foreign);
//            workItems.add(workItem);
//        }
//        log_.info("HWI:%n  Extracted %d work items", workItems.size());
        return workItems;   // to be consumed by com.intel.dai.network_listener.processMessage()
    }

    /**
     * <p> Returns the current time as an UTC time stamp in nanoseconds. </p>
     * @return current UTC time in nanoseconds
     */
    private long currentUTCInNanoseconds() {
        Instant i = Instant.now();
        return i.getEpochSecond() * 1000000000L + i.getNano();
    }

    /**
     * <p> Converts a foreign component state into BootState.  Unsupported states are represented as null,
     * and unexpected states cause an exception to be thrown.
     * HWInvNotificationTranslator guarantees that the input string cannot be null.  Note that none of
     * the listed component state corresponds obvicously to BootState.NODE_BOOTING. </p>
     * @param foreignComponentState state in the SCN json
     * @return a BootState or null
     * @throws NetworkListenerProviderException network listener provider exception
     */
    BootState toBootState(String foreignComponentState)
            throws NetworkListenerProviderException {

        switch(foreignComponentState) {
            case "Off":
                return BootState.NODE_OFFLINE;
            case "On":
                return BootState.NODE_ONLINE;
            case "Unknown":
            case "Empty":
            case "Populated":
            case "Active":
            case "Standby":
            case "Halt":
            case "Ready":
            case "Paused":
                log_.info("HWI:%n  Unsupported foreign component state: %s",
                        foreignComponentState);
                return null;
            default:
                String msg = String.format("Unexpected foreign component state: %s",
                        foreignComponentState);
                log_.error("HWI:%n  %s", msg);
                throw new NetworkListenerProviderException(msg);
        }
    }

    /**
     * <p> Callback from com.intel.dai.network_listener.processMessage().
     * Acts on a work item described in common data format. </p>
     * @param workItem HW inventory update work item in common data format
     * @param config network listener config
     * @param systemActions system actions for processing work item
     */
    @Override
    public void actOnData(CommonDataFormat workItem, NetworkListenerConfig config, SystemActions systemActions) {
        if(config_ == null)
            getConfig(config);

        // Enqueue inventory json into a voltdb table so that a dedicated thread can consume them.
        // This means that inventory loading thread cannot shutdown by itself.  Use an interrupt
        // to shut the thread down.

//        BootState bootState = workItem.getStateEvent();
//        if (!isSupportedInventoryEvents(bootState)) {
//            log_.debug("Unsupported boot state=%d", bootState);
//            return;
//        }
//
//        log_.info("Starting InventoryUpdateThread ...");
//        Thread t = new Thread(new InventoryUpdateThread(log_, config));
//        t.start();  // background update of inventory
    }

    private boolean isSupportedInventoryEvents(BootState bootState) {
        return bootState == BootState.NODE_ONLINE || bootState == BootState.NODE_OFFLINE;
    }

    private void getConfig(NetworkListenerConfig config) {
        config_ = config;
        // System actions no longer used because the DB update code needs to run before
        // system actions are available.
    }

    private final Logger log_;
    private NetworkListenerConfig config_ = null;
    private final static String FOREIGN_KEY = "foreignLocationKey";
}
