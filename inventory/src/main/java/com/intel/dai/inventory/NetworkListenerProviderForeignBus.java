// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.dai.dsapi.BootState;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.inventory.api.ForeignHWInvChangeNotification;
import com.intel.dai.inventory.api.HWInvNotificationTranslator;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;

import java.io.IOException;
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
     * <p> Translates the json in the data String into CommonDataFormat.  The json contains a list of components
     * that shares a common state update.  This translate into an array of CommonDataFormat entries, each
     * describing a single component. </p>
     *
     * @param scnJson state change notification as a json string
     * @param config network listener configuration
     * @return an array of CommonDataFormat entries equivalent to scnJson
     * @throws NetworkListenerProviderException network listener provider exception
     */
    @Override
    public List<CommonDataFormat> processRawStringData(String scnJson, NetworkListenerConfig config)
            throws NetworkListenerProviderException {

        long currentTimestamp = currentUTCInNanoseconds();  // all entries in the returned list must share a common timestamp
                                        // this is needed to avoid an update storm during the initial loading
                                        // of the HW inventory

        ForeignHWInvChangeNotification notif = new HWInvNotificationTranslator().toPOJO(scnJson);
        if (notif == null) {
            throw new NetworkListenerProviderException("scnJson is null");
        }

        BootState newComponentState = toBootState(notif.State);
        if (newComponentState == null) {
            throw new NetworkListenerProviderException(
                    String.format("Unsupported scn notification state: %s", notif.State));
        }

        List<CommonDataFormat> workItems = new ArrayList<>();
        for (String xname: notif.Components) {
            CommonDataFormat workItem = new CommonDataFormat(
                    currentTimestamp, CommonFunctions.convertXNameToLocation(xname), DataType.InventoryChangeEvent);
            workItem.setStateChangeEvent(newComponentState);
            workItem.storeExtraData(XNAME_KEY, xname);
            workItems.add(workItem);
        }

        return workItems;
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
     * <p> Convert a foreign component state into BootState.  Unsupported states are represented as null,
     * and unexpected states cause an exception to be thrown.
     * HWInvNotificationTranslator guarantees that the input string cannot be null.  Note that none of
     * the listed component state corresponds obvicously to BootState.NODE_BOOTING. </p>
     * @param foreignComponentState state in the SCN json
     * @return a BootState or null
     * @throws NetworkListenerProviderException network listener provider exception
     */
    BootState toBootState(String foreignComponentState) throws NetworkListenerProviderException {
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
                log_.info("Unsupported foreign component state: %s", foreignComponentState);
                return null;
            default:
                String msg = String.format("Unexpected foreign component state: %s", foreignComponentState);
                log_.error(msg);
                throw new NetworkListenerProviderException(msg);
        }
    }

    /**
     * <p> Act on work items described in common data format. </p>
     * @param workItem HW inventory update work item in common data format
     * @param config network listener config
     * @param systemActions system actions for processing work item
     */
    @Override
    public void actOnData(CommonDataFormat workItem, NetworkListenerConfig config, SystemActions systemActions) {
        if(config_ == null)
            getConfig(config, systemActions);

        String location = determineHWInventoryUpdateLocation(workItem);
        if (location != null) {
            Thread t = new Thread(new HWInventoryUpdate(location, actions_));
            t.start();  // background updates of HW inventory
        }
        // Possible TODOs: store RAZ event and publish to Rabbit MQ
    }

    /**
     * <p> Determine to perform HW inventory update of a component or all the components in the HPC. </p>
     * @param workItem HW inventory work item
     * @return node to be updated or "" if the DB is to be refreshed with all the components in the HPC
     */
    private String determineHWInventoryUpdateLocation(CommonDataFormat workItem) {
        long dataTimestamp = workItem.getNanoSecondTimestamp();

        if (dataTimestamp == lastSnapshotTimestamp) {
            log_.info("snapshot is already loaded/loading for this batch of notifications: %d",
                    lastSnapshotTimestamp);
            return null;
        }

        try {
            if (actions_.isHWInventoryEmpty()) {
                lastSnapshotTimestamp = dataTimestamp;
                return "";
            }
            else {
                return workItem.retrieveExtraData(XNAME_KEY);
            }
        } catch (IOException e) {
            log_.error("IO exception: %s", e.getMessage());
            return null;
        } catch (DataStoreException e) {
            log_.error("datastore exception: %s", e.getMessage());
            return null;
        }
    }

    private void getConfig(NetworkListenerConfig config, SystemActions systemActions) {
        config_ = config;
        actions_ = systemActions;

        // Possible TODOs: RAZ and Publisher config (Rabbit MQ)
    }

    private Logger log_;

    private NetworkListenerConfig config_ = null;
    private SystemActions actions_ = null;

    static long lastSnapshotTimestamp = -1;

    private final static String XNAME_KEY = "xname";
}

/**
 * Performs background update of the HW inventory DB.
 */
class HWInventoryUpdate implements Runnable {
    private String location_;
    private SystemActions actions_;

    public HWInventoryUpdate(String xname, SystemActions actions) {
        location_ = xname;
        actions_ = actions;
    }

    public void run() {
        actions_.upsertHWInventory(location_);
    }
}
