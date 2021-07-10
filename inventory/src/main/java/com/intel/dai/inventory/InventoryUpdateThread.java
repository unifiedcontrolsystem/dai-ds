// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.api.es.Elasticsearch;
import com.intel.dai.inventory.api.es.ElasticsearchIndexIngester;
import com.intel.dai.inventory.api.es.NodeInventoryIngester;
import com.intel.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.client.RestHighLevelClient;

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
    private final Logger log_;

    InventoryUpdateThread(Logger log) {
        log_ = log;
    }

    /**
     * Runs a background thread that updates the DAI inventory database tables using
     * information from the foreign server.
     */
    public void run() {
        log_.debug("run()");
        final DataStoreFactory factory_ = ProviderInventoryNetworkForeignBus.getDataStoreFactory();
        if (factory_ == null) {
            log_.error("ProviderInventoryNetworkForeignBus.getDataStoreFactory() => null");
            return;
        }
        try {
            log_.info("InventoryUpdateThread started");
            DatabaseSynchronizer synchronizer = new DatabaseSynchronizer(log_, factory_);
            synchronizer.updateDaiInventoryTables();
        } finally {
            log_.info("InventoryUpdateThread terminated");
        }
    }
}

/**
 * Synchronizes synchronizes the inventory data in voltdb, postgres and foreign server.
 */
class DatabaseSynchronizer {
    private final Logger log_;
    private final DataStoreFactory factory_;
    protected HWInvUtil util_;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    protected ForeignInventoryClient foreignInventoryDatabaseClient_;   // foreign inventory server
    InventorySnapshot nearLineInventoryDatabaseClient_;                 // postgres
    long totalNumberOfInjectedDocuments = 0;                            // for testing only

    DatabaseSynchronizer(Logger log, DataStoreFactory factory) {
        log_ = log;
        factory_ = factory;
    }

    void updateDaiInventoryTables() {
        try {
            initializeDependencies();

            String lastInventoryUpdate = getLastHWInventoryHistoryUpdate();
            if (lastInventoryUpdate == null) {
                log_.error("Cannot determine lastInventoryUpdate.  Database loading is skipped.");
                return;
            }
            log_.info("getLastHWInventoryHistoryUpdate() =>'%s'", lastInventoryUpdate);

            List<HWInvHistoryEvent> changedLocationEvents = ingestRawInventoryHistoryEvents(lastInventoryUpdate);
            log_.info("ingestRawInventoryHistoryEvents(lastInventoryUpdate=%s) => %d",
                    lastInventoryUpdate, changedLocationEvents.size());

            int numRawLocationsIngested = ingestRawInventorySnapshot(lastInventoryUpdate, changedLocationEvents);
            log_.info("ingestRawInventorySnapshot(lastInventoryUpdate=%s, changedLocationEvents=%s) => %d",
                    lastInventoryUpdate, util_.head(changedLocationEvents.toString(), 80),
                    numRawLocationsIngested);

            Map<String, String> changedNodeLocations = extractChangedNodeLocations(changedLocationEvents);
            int numCookedNodesIngested = ingestCookedNodes(changedNodeLocations);
            log_.info("ingestCookedNodes(changedNodeLocations=%s) => %d",
                    util_.head(changedNodeLocations.toString(), 80), numCookedNodesIngested);

            // New inventory code; TODO: old code needs to be deleted after new code works

            // TODO: Add config in the next pull request
            if (areEmptyInventoryTablesInPostgres()) {
                log_.info("areEmptyInventoryTablesInPostgres() => true");
                Elasticsearch es = new Elasticsearch(log_);
                RestHighLevelClient esClient = es.getRestHighLevelClient("cmcheung-centos-7.ra.intel.com", 9200,
                        "elkrest", "elkdefault");

                String[] elasticsearchIndices = {"kafka_fru_host", "kafka_dimm"};
                for (String index : elasticsearchIndices) {
                    ElasticsearchIndexIngester eii = new ElasticsearchIndexIngester(esClient, index, factory_, log_);
                    eii.ingestIndexIntoVoltdb();
                    totalNumberOfInjectedDocuments += eii.getNumberOfDocumentsEnumerated();
                    log_.info("Number of %s documents = %d", index, eii.getNumberOfDocumentsEnumerated());
                }
                es.close();

                NodeInventoryIngester ni = new NodeInventoryIngester(factory_, log_);
                ni.ingestInitialNodeInventoryHistory();
                totalNumberOfInjectedDocuments += ni.getNumberNodeInventoryJsonIngested();
                log_.info("Number of Raw_Node_Inventory_History documents = %d", ni.getNumberNodeInventoryJsonIngested());
                return;
            }
            log_.info("areEmptyInventoryTablesInPostgres() => false");
        } catch (DataStoreException e) {
            log_.error(e.getMessage());
        } finally {
            log_.info("updateDaiInventoryTables() completed");
        }
    }

    private void initializeDependencies() {
        util_ = new HWInvUtilImpl(log_);

        foreignInventoryDatabaseClient_ = new ForeignInventoryClient(log_);
        onlineInventoryDatabaseClient_ = factory_.createHWInvApi();
        nearLineInventoryDatabaseClient_ = factory_.createInventorySnapshotApi();
    }

    boolean areEmptyInventoryTablesInPostgres() {
        try {
            ImmutablePair<Long, String> lastRawDimm =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawDimmIngested();
            log_.info("lastRawDimm: %d %s", lastRawDimm.left, lastRawDimm.right);
            ImmutablePair<Long, String> lastRawFruHost =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawFruHostIngested();
            log_.info("lastRawFruHost: %d %s", lastRawFruHost.left, lastRawFruHost.right);

            return lastRawDimm.right == null && lastRawFruHost.right == null;
        } catch (DataStoreException e) {
            log_.error(e.getMessage());
        }
        return true;
    }

    /**
     * This method can return both null or the string "null".
     *
     * @return last raw inventory update timestamp, "null" if the raw history table is empty, or null if there was an error.
     */
    String getLastHWInventoryHistoryUpdate() {  // must not be private or Spy will not work
        log_.info(">> getLastHWInventoryHistoryUpdate()");
        try {
            String lastUpdateTimestamp = nearLineInventoryDatabaseClient_.getLastHWInventoryHistoryUpdate();
            return Objects.requireNonNullElse(lastUpdateTimestamp, "");
        } catch (DataStoreException e) {
            log_.error("getLastHWInventoryHistoryUpdate() threw (%s)", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
            return null;
        }
    }

    ImmutablePair<Long, String> getCharacteristicsOfLastRawDimm() {  // must not be private or Spy will not work
        log_.info(">> getCharacteristicsOfLastRawDimm()");
        try {
            ImmutablePair<Long, String> lastIngestedRawDimm =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawDimmIngested();
            return Objects.requireNonNullElse(lastIngestedRawDimm, ImmutablePair.nullPair());
        } catch (DataStoreException e) {
            log_.error("getCharacteristicsOfLastRawDimm() threw (%s)", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
            return null;
        }
    }

    ImmutablePair<Long, String> getCharacteristicsOfLastRawFruHost() {  // must not be private or Spy will not work
        log_.info(">> getCharacteristicsOfLastRawFruHost()");
        try {
            ImmutablePair<Long, String> lastIngestedRawFruHost =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawFruHostIngested();
            return Objects.requireNonNullElse(lastIngestedRawFruHost, ImmutablePair.nullPair());
        } catch (DataStoreException e) {
            log_.error("getCharacteristicsOfLastRawFruHost() threw (%s)", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
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
                log_.info("ingestInventorySnapshot(event.ID=%s)", record.ID);
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
            log_.info("ingestInventorySnapshot(changedNodeLocation=%s)", changedNodeLocation);
            numRawInventoryLocationsIngested += ingestInventorySnapshot(changedNodeLocation);
        }
        return numRawInventoryLocationsIngested;
    }

    private List<HWInvHistoryEvent> ingestRawInventoryHistoryEvents(String lastInventoryUpdate) {
        String history = foreignInventoryDatabaseClient_.getCanonicalHWInvHistoryJson(lastInventoryUpdate);
        log_.debug(
                "foreignInventoryDatabaseClient_.getCanonicalHWInvHistoryJson(lastInventoryUpdate=%s) =>%n  %s",
                lastInventoryUpdate,
                util_.head(history, 240));

        return ingestCanonicalHWInvHistoryJson(history);
        // At this point, numberOfRawInventoryRows() => 0 because the data had already been purged
        // most likely by another adapter instance if were were to drop the table at the beginning!
    }

    private int ingestInventorySnapshot(String location) {
        String inventory = foreignInventoryDatabaseClient_.getCanonicalHWInvJson(location);
        log_.debug("foreignInventoryDatabaseClient_.getCanonicalHWInvJson(location=%s) => %s",
                location, util_.head(inventory, 240));
        return ingestCanonicalHWInvJson(inventory);
    }

    /**
     * For some reason, drives are not part of the nested node structure even though a
     * drive event has a node event as a prefix.
     *
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
            log_.warn("Incomplete conversion: event.ID=%s is still in foreign namespace",
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
     *
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
            log_.info("canonicalHwInvHistJson is null");
            return new ArrayList<>();
        }

        try {
            return onlineInventoryDatabaseClient_.ingestHistory(canonicalHwInvHistJson);
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
        return new ArrayList<>();
    }
}
