// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Performs background update of the HW inventory DB.
 * Note that we assume the the initial loading of inventory to the
 * voltdb tables is performed by a thread started by app.postInitialize(),
 * and that this may be ongoing!
 */
class InventoryUpdateThread implements Runnable {
    InventoryUpdateThread(Logger log) {
        log_ = log;
    }

    /**
     * Runs a background thread that updates the DAI inventory database tables using
     * information from the foreign server.
     */
    public void run() {
        final DataStoreFactory factory_ = ProviderInventoryNetworkForeignBus.getDataStoreFactory();
        if (factory_ == null) {
            log_.error("HWI:%n %s", "ProviderInventoryNetworkForeignBus.getDataStoreFactory() => null");
            return;
        }
        try {
            log_.info("HWI:%n  %s", "InventoryUpdateThread started");
            DatabaseSynchronizer synchronizer = new DatabaseSynchronizer(log_, factory_);
            synchronizer.updateDaiInventoryTables();
        } finally {
            log_.info("HWI:%n  %s", "InventoryUpdateThread terminated");
        }
    }

    private final Logger log_;
}

/**
 * Synchronizes synchronizes the inventory data in voltdb, postgres and foreign server.
 */
class DatabaseSynchronizer {
    DatabaseSynchronizer(Logger log, DataStoreFactory factory) {
        log_ = log;
        factory_ = factory;
    }

    void updateDaiInventoryTables() {
        try {
            initializeDependencies();

            String lastInventoryUpdate = getLastHWInventoryHistoryUpdate();
            if (lastInventoryUpdate == null) {
                log_.error("HWI:%n   Cannot determine lastInventoryUpdate.  Database loading is skipped.");
                return;
            }
            log_.info("HWI:%n  getLastHWInventoryHistoryUpdate() =>'%s'", lastInventoryUpdate);

            List<HWInvHistoryEvent> changedLocationEvents = ingestRawInventoryHistoryEvents(lastInventoryUpdate);
            log_.info("HWI:%n  ingestRawInventoryHistoryEvents(lastInventoryUpdate=%s) => %d",
                    lastInventoryUpdate, changedLocationEvents.size());

            int numRawLocationsIngested = ingestRawInventorySnapshot(lastInventoryUpdate, changedLocationEvents);
            log_.info("HWI:%n  ingestRawInventorySnapshot(lastInventoryUpdate=%s, changedLocationEvents=%s) => %d",
                    lastInventoryUpdate, util_.head(changedLocationEvents.toString(), 80),
                    numRawLocationsIngested);

            Map<String, String> changedNodeLocations = extractChangedNodeLocations(changedLocationEvents);
            int numCookedNodesIngested = ingestCookedNodes(changedNodeLocations);
            log_.info("HWI:%n  ingestCookedNodes(changedNodeLocations=%s) => %d",
                    util_.head(changedNodeLocations.toString(), 80), numCookedNodesIngested);
        } finally {
            log_.info("HWI:%n  %s","updateDaiInventoryTables() completed");
        }
    }

    private void initializeDependencies() {
        util_ = new HWInvUtilImpl(log_);

        foreignInventoryDatabaseClient_ = new ForeignInventoryClient(log_);
        onlineInventoryDatabaseClient_ = factory_.createHWInvApi();
        nearLineInventoryDatabaseClient_ = factory_.createInventorySnapshotApi();
    }

    /**
     * This method can return both null or the string "null".
     * @return last raw inventory update timestamp, "null" if the raw history table is empty, or null if there was an error.
     */
    private String getLastHWInventoryHistoryUpdate() {
        try {
            String lastUpdateTimestamp = nearLineInventoryDatabaseClient_.getLastHWInventoryHistoryUpdate();
            return Objects.requireNonNullElse(lastUpdateTimestamp, "");
        } catch (DataStoreException e) {
            log_.error("HWI:%n  getLastHWInventoryHistoryUpdate() threw (%s)", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log_.exception(e,"HWI:%n  null pointer exception: %s", e.getMessage());
            return null;
        }
    }

    private Map<String, String> extractChangedNodeLocations(List<HWInvHistoryEvent> changedLocationEvents) {
        Map<String, String> changedNodeLocations = new HashMap<>();
        for (HWInvHistoryEvent event : changedLocationEvents) {
            String nodeLocation = extractNodeLocationFromChangeEvent(event);
            if (nodeLocation == null) {
                log_.debug("extractNodeLocationFromChangeEvent(event=%s) => null", event.toString());  // may be a node enclosure, etc.
                continue;
            }
            String timestampCurrentInMap = changedNodeLocations.get(nodeLocation);
            try {
                if (timestampCurrentInMap == null || event.Timestamp.compareTo(timestampCurrentInMap) > 0) {
                    changedNodeLocations.put(extractNodeLocationFromChangeEvent(event), event.Timestamp);
                }
            } catch (NullPointerException e) {
                log_.error("%s.Timestamp.compareTo(timestampCurrentInMap=%s) threw %s",
                        event.toString(), timestampCurrentInMap, e.getMessage());
                // event.Timestamp cannot be null
            }
        }
        log_.debug("HWI:%n  Number of changed nodes: %d", changedNodeLocations.size());
        return changedNodeLocations;
    }

    private int ingestRawInventorySnapshot(
            String lastInventoryUpdate,
            List<HWInvHistoryEvent> changedLocationEvents) {

        if (lastInventoryUpdate.equals("")) {    // initial loading
            return ingestInventorySnapshot("all");
        } else {
            // We must modify the raw snapshot tables here since dai_core does not know how to communicate
            // with the foreign server.

            // Update the container nodes first because of the following reasons:
            //  1. They were purged when the adapter was offline.
            //  2. We will need it to construct cooked nodes containing the record.IDs.
            int numRawInventoryLocationsIngested = ingestChangedNodeLocationSnapshots(
                    changedLocationEvents);

            // Now we patch in the actual locations that were changed.
            for (HWInvHistoryEvent record : changedLocationEvents) {
                log_.info("HWI:%n  ingestInventorySnapshot(event.ID=%s)", record.ID);
                numRawInventoryLocationsIngested += ingestInventorySnapshot(record.ID);
            }

            return numRawInventoryLocationsIngested;
        }
    }

    private int ingestChangedNodeLocationSnapshots(
            List<HWInvHistoryEvent> changedLocationEvents) {
        // Extract a set of changed node locations.
        Set<String> changedNodeLocations = new HashSet<>();
        for (HWInvHistoryEvent event : changedLocationEvents) {
            String nodeLocation = extractNodeLocationFromChangeEvent(event);
            if (nodeLocation != null) { // some locations such as node enclosure does not have a parent node
                changedNodeLocations.add(nodeLocation);
            }
        }

        int numRawInventoryLocationsIngested = 0;
        for (String changedNodeLocation : changedNodeLocations) {
            log_.info("HWI:%n  ingestInventorySnapshot(changedNodeLocation=%s)", changedNodeLocation);
            numRawInventoryLocationsIngested += ingestInventorySnapshot(changedNodeLocation);
        }
        return numRawInventoryLocationsIngested;
    }

    private List<HWInvHistoryEvent> ingestRawInventoryHistoryEvents(String lastInventoryUpdate) {
        String history = foreignInventoryDatabaseClient_.getCanonicalHWInvHistoryJson(lastInventoryUpdate);
        log_.debug(
                "HWI:%n  foreignInventoryDatabaseClient_.getCanonicalHWInvHistoryJson(lastInventoryUpdate=%s) =>%n  %s",
                lastInventoryUpdate,
                util_.head(history, 240));

        return ingestCanonicalHWInvHistoryJson(history);
        // At this point, numberOfRawInventoryRows() => 0 because the data had already been purged
        // most likely by another adapter instance if were were to drop the table at the beginning!
    }

    private int ingestInventorySnapshot(String location) {
        String inventory = foreignInventoryDatabaseClient_.getCanonicalHWInvJson(location);
        log_.debug("HWI:%n  foreignInventoryDatabaseClient_.getCanonicalHWInvJson(location=%s) => %s",
                location, util_.head(inventory, 240));
        return ingestCanonicalHWInvJson(inventory);
    }

    /**
     * For some reason, drives are not part of the nested node structure even though a
     * drive event has a node event as a prefix.
     * @param event historic record of a fru change
     * @return parent node location of the event location
     */
    private String extractNodeLocationFromChangeEvent(HWInvHistoryEvent event) {
        for (String patternStr : Arrays.asList("^(.+-.+N\\d+)", "^(.+-AM\\d+)")) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(event.ID);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        Pattern pattern = Pattern.compile("^(.+n\\d+)");
        Matcher matcher = pattern.matcher(event.ID);
        if (matcher.find()) {
            log_.warn("HWI:%n, Incomplete conversion: event.ID=%s is still in foreign namespace",
                    event.ID);  // this may be caused by an incomplete namespace conversion map
            return null;
        }

        return null;    // cannot extract a node changed event
    }

    private int ingestCookedNodes(Map<String, String> lastNodeLocationChangeTimestamp) {
        try {
            return onlineInventoryDatabaseClient_.ingestCookedNodesChanged(lastNodeLocationChangeTimestamp);
        } catch (DataStoreException e) {
            log_.error("HWI:%n  DataStoreException: %s", e.getMessage());
            return 0;
        }
    }

    /**
     * Ingests the HW inventory locations in canonical form.
     * N.B. System actions are not available to the caller of this method.
     * @param canonicalHwInvJson json containing the HW inventory locations in canonical format
     */
    private int ingestCanonicalHWInvJson(String canonicalHwInvJson) {
        if (canonicalHwInvJson == null) {
            log_.info("HWI:%n  canonicalHwInvJson is null");
            return 0;
        }

        try {
            return onlineInventoryDatabaseClient_.ingest(canonicalHwInvJson);
        } catch (DataStoreException e) {
            log_.error("HWI:%n  DataStoreException: %s", e.getMessage());
        }

        log_.error("HWI:%n  Unknown number of canonical HW inventory locations ingested!");
        return 0;
    }

    private List<HWInvHistoryEvent> ingestCanonicalHWInvHistoryJson(String canonicalHwInvHistJson) {
        if (canonicalHwInvHistJson == null) {
            log_.info("HWI:%n  %s", "canonicalHwInvHistJson is null");
            return new ArrayList<>();
        }

        try {
            return onlineInventoryDatabaseClient_.ingestHistory(canonicalHwInvHistJson);
        } catch (DataStoreException e) {
            log_.error("HWI:%n  DataStoreException: %s", e.getMessage());
        }
        return new ArrayList<>();
    }

    private final Logger log_;
    private final DataStoreFactory factory_;

    protected HWInvUtil util_;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    protected ForeignInventoryClient foreignInventoryDatabaseClient_;   // foreign inventory server
    InventorySnapshot nearLineInventoryDatabaseClient_;                 // postgres
}
