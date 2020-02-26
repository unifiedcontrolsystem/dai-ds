// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.inventory.api.ForeignHWInvChangeNotification;
import com.intel.dai.inventory.api.HWInvDiscovery;
import com.intel.dai.inventory.api.HWInvNotificationTranslator;
import com.intel.dai.inventory.api.HWInvTranslator;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.intel.dai.inventory.api.HWInvDiscovery.queryHWInvTree;

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
            throw new NetworkListenerProviderException("scnJson translation failed");
        }

        BootState newComponentState = toBootState(notif.State);
        if (newComponentState == null) {
            log_.info("ignoring unsupported scn notification state: %s", notif.State);
            return new ArrayList<>();
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
            getConfig(config);  // , systemActions);

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

    private void getConfig(NetworkListenerConfig config) {
        config_ = config;
        actions_ = new MySystemActions(log_);

        // Possible TODOs: RAZ and Publisher config (Rabbit MQ)
    }

    private Logger log_;

    private NetworkListenerConfig config_ = null;
    private MySystemActions actions_ = null;

    static long lastSnapshotTimestamp = -1;

    private final static String XNAME_KEY = "xname";
}

class MySystemActions {
    MySystemActions(Logger logger) {
        log_ = logger;
        String[] servers = new String[1];
        String server = "localhost";
        servers[0] = server;
        DataStoreFactory factory = new DataStoreFactoryImpl(servers, logger);
        hwInvApi_ = factory.createHWInvApi();
    }
    /**
     * <p> Determines if the HW inventory DB is currently empty. </p>
     * @return true if the DB is empty, otherwise false
     * @throws IOException I/O exception
     * @throws DataStoreException datastore exception
     */
    public boolean isHWInventoryEmpty() throws IOException, DataStoreException {
        return hwInvApi_.numberOfLocationsInHWInv() == 0;
    }

    /**
     * <p> Updates the location entries of the HW inventory tree at the given root in the HW inventory DB. </p>
     * @param root root location in foreign format
     */
    public void upsertHWInventory(String root) {
        HWInvTree before = getHWInvSnapshot(root);

        try {
            ingestCanonicalHWInvJson(
                    toCanonicalHWInvJson(
                            getForeignHWInvJson(
                                    root)));
        } catch (NetworkListenerProviderException e) {
            log_.error("ingestion failed: %s", e.getMessage());
            return;
        }

        HWInvTree after = getHWInvSnapshot(root);

        if (before == null || after == null) {
            log_.error("before or after is null");
            return;
        }
        HWInvUtilImpl util = new HWInvUtilImpl();
        List<HWInvLoc> delList = util.subtract(before.locs, after.locs);
        for (HWInvLoc s : delList) {
            log_.info("deleted: %s from %s", s.FRUID, s.ID);
            insertHistoricalRecord("DELETED", s);
        }
        List<HWInvLoc> addList = util.subtract(after.locs, before.locs);
        for (HWInvLoc s : addList) {
            log_.info("inserted: %s into %s", s.FRUID, s.ID);
            insertHistoricalRecord("INSERTED", s);
        }
    }

    private void insertHistoricalRecord(String action, HWInvLoc s) {
        try {
            hwInvApi_.insertHistoricalRecord(action, s.ID, s.FRUID);
        } catch (InterruptedException e) {
            log_.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    private HWInvTree getHWInvSnapshot(String root) {
        if (root == null) {
            return null;
        }
        try {
            return hwInvApi_.allLocationsAt(root, null);
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
        return null;
    }

    /**
     * <p> Ingests the HW inventory locations in canonical form. </p>
     * @param canonicalHwInvJson json containing the HW inventory locations in canonical format
     */
    private void ingestCanonicalHWInvJson(String canonicalHwInvJson) {
        if (canonicalHwInvJson == null) return;

        try {
            hwInvApi_.ingest(canonicalHwInvJson);
        } catch (InterruptedException e) {
            log_.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    /**
     * <p> Converts the HW inventory locations in foreign format into canonical format. </p>
     * @param foreignHWInvJson json containing the HW inventory in foreign format
     * @return json containing the HW inventory in canonical format
     */
    private String toCanonicalHWInvJson(String foreignHWInvJson) throws NetworkListenerProviderException {
        if (foreignHWInvJson == null) return null;

        HWInvTranslator tr = new HWInvTranslator(new HWInvUtilImpl());

        ImmutablePair<String, HWInvTree> canonicalResult = tr.toCanonical(foreignHWInvJson);

        // Map xnames to DAI namespace
        // Perhaps the conversion function can be sent in as a lambda to tr.foreignToCanonical()
        // Best to wait for some functional tests to be available before doing this
        HWInvTree canonicalTree = canonicalResult.getValue();
        for (HWInvLoc loc: canonicalTree.locs) {
            loc.ID = CommonFunctions.convertLocationToXName(loc.ID);
        }

        return tr.toCanonicalJson(canonicalTree);
    }

    /**
     * <p> Obtains the HW inventory locations at the given root.  If the root is "", all locations of the
     * HPC is returned. </p>
     * @param root root location for a HW inventory tree or "" for the root of the entire HPC
     * @return json containing the requested locations
     */
    private String getForeignHWInvJson(String root) {
        if (root == null) return null;

        try {
            HWInvDiscovery.initialize(log_);
            log_.info("rest client created");

        } catch (RESTClientException e) {
            log_.fatal("Fail to create REST client: %s", e.getMessage());
            return null;
        }

        ImmutablePair<Integer, String> foreignHwInv;
        if (root.equals("")) {
            foreignHwInv = queryHWInvTree();
        } else {
            foreignHwInv = queryHWInvTree(root);
        }
        if (foreignHwInv.getLeft() != 0) {
            log_.error("failed to acquire foreign HW inventory json");
            return null;
        }
        return foreignHwInv.getRight();
    }

    private Logger log_;
    private HWInvApi hwInvApi_;
}

/**
 * Performs background update of the HW inventory DB.
 */
class HWInventoryUpdate implements Runnable {
    private String location_;
    private MySystemActions actions_;

    public HWInventoryUpdate(String xname, MySystemActions actions) {
        location_ = xname;
        actions_ = actions;
    }

    public void run() {
        actions_.upsertHWInventory(location_);
    }
}
