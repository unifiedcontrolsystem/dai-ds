package com.intel.dai.inventory;

import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.api.database.RawInventoryDataIngester;
import com.intel.dai.inventory.api.es.Elasticsearch;
import com.intel.dai.inventory.api.es.ElasticsearchIndexIngester;
import com.intel.dai.inventory.api.es.NodeInventoryIngester;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronizes synchronizes the inventory data in voltdb, postgres and foreign server.
 */
public class DatabaseSynchronizer {
    private final Logger log_;
    private final DataStoreFactory factory_;
    protected HWInvUtil util_;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    protected ForeignInventoryClient foreignInventoryDatabaseClient_;   // foreign inventory server
    InventorySnapshot nearLineInventoryDatabaseClient_;                 // postgres
    long totalNumberOfInjectedDocuments = 0;                            // for testing only
    ImmutablePair<Long, String> characteristicsOfLastRawDimmIngested;
    ImmutablePair<Long, String> characteristicsOfLastRawFruHostIngested;

    final long dataMoverTimeOutLimit = 60 * 1000;    // wait at most 1 minute

    private String hostName_ = "localhost";
    private int port_ = 9200;
    private String userName_ = "";
    private String password_ = "";

    public DatabaseSynchronizer(Logger log, NetworkListenerConfig config) {
        log_ = log;

        factory_ = ProviderInventoryNetworkForeignBus.getDataStoreFactory();
        if (factory_ == null) {
            log_.error("ProviderInventoryNetworkForeignBus.getDataStoreFactory() => null");
            return;
        }

        PropertyMap configMap = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if (configMap != null) {
            try {
                hostName_ = configMap.getString("hostName");
                port_ = configMap.getInt("port");
                userName_ = configMap.getString("userName");
                password_ = configMap.getString("password");
            } catch (PropertyNotExpectedType propertyNotExpectedType) {
                log_.error(propertyNotExpectedType.getMessage());
            }
            return;
        }
        log_.error("getProviderConfigurationFromClassName(%s) => null", getClass().getCanonicalName());
    }

    // For testing
    void setElasticsearchServerAttributes(String hostName, int port, String userName, String password) {
        hostName_ = hostName;
        port_ = port;
        userName_ = userName;
        password_ = password;
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

            log_.info("hostName:%s port:%s userName:%s password:%s", hostName_, port_, userName_, password_);
            if (areEmptyInventoryTablesInPostgres()) {
                log_.info("areEmptyInventoryTablesInPostgres() => true");
                Elasticsearch es = new Elasticsearch(log_);
                RestHighLevelClient esClient = es.getRestHighLevelClient(hostName_, port_,
                        userName_, password_);

                characteristicsOfLastRawDimmIngested = ingest(esClient, "kafka_dimm");
                characteristicsOfLastRawFruHostIngested = ingest(esClient, "kafka_fru_host");

                es.close();

                sleepForOneSecond();
                waitForDataMoverToFinish();

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

    void ingestRawDimm(ImmutablePair<String, String> doc) {
        RawInventoryDataIngester.ingestDimm(doc);
    }

    void ingestRawFruHost(ImmutablePair<String, String> doc) {
        RawInventoryDataIngester.ingestFruHost(doc);
    }

    private ImmutablePair<Long, String> ingest(RestHighLevelClient esClient, String index) throws DataStoreException {
        ElasticsearchIndexIngester eii = new ElasticsearchIndexIngester(esClient, index, 0, factory_, log_);
        eii.ingestIndexIntoVoltdb();
        totalNumberOfInjectedDocuments += eii.getNumberOfDocumentsEnumerated();
        log_.info("Number of %s documents = %d", index, eii.getNumberOfDocumentsEnumerated());
        return eii.getCharacteristicsOfLastDocIngested();
    }

    private void waitForDataMoverToFinish() {
        waitForDataMoverToFinishMovingRawDimms();
        waitForDataMoverToFinishMovingRawFruHosts();
    }

    private void waitForDataMoverToFinishMovingRawDimms() {
        if (characteristicsOfLastRawDimmIngested.right == null) {
            log_.error("characteristicsOfLastRawDimmIngested.right == null");
            return;
        }

        long timeOut = dataMoverTimeOutLimit;
        while (timeOut > 0) {
            ImmutablePair<Long, String> lastRawDimmTransferred = getCharacteristicsOfLastRawDimmIngestedIntoNearLine();
            if (lastRawDimmTransferred.right != null) {
                log_.info("Comparing characteristicsOfLastRawDimmIngested <%d, %s> against lastRawDimmTransferred <%d, %s>",
                        characteristicsOfLastRawDimmIngested.left, characteristicsOfLastRawDimmIngested.right,
                        lastRawDimmTransferred.left, lastRawDimmTransferred.right);
                if (characteristicsOfLastRawDimmIngested.equals(lastRawDimmTransferred)) {
                    log_.info("Raw DIMMs transfer completed at %dms", dataMoverTimeOutLimit - timeOut);
                    return;
                }
            } else {
                log_.error("lastRawDimmTransferred.right == null");
            }
            log_.info("Waiting for data mover - Raw DIMMs: %dms left", timeOut);
            timeOut -= sleepForOneSecond();
        }
    }

    private void waitForDataMoverToFinishMovingRawFruHosts() {
        if (characteristicsOfLastRawFruHostIngested.right == null) {
            log_.error("characteristicsOfLastRawFruHostIngested.right == null");
            return;
        }

        long timeOut = dataMoverTimeOutLimit;
        while (timeOut > 0) {
            ImmutablePair<Long, String> lastRawFruHostTransferred = getCharacteristicsOfLastRawFruHostIngestedIntoNearLine();
            if (lastRawFruHostTransferred.right != null) {
                log_.info("Comparing characteristicsOfLastRawFruHostIngested <%d, %s> against lastRawFruHostTransferred <%d, %s>",
                        characteristicsOfLastRawFruHostIngested.left, characteristicsOfLastRawFruHostIngested.right,
                        lastRawFruHostTransferred.left, lastRawFruHostTransferred.right);
                if (characteristicsOfLastRawFruHostIngested.equals(lastRawFruHostTransferred)) {
                    log_.info("Raw FRU Hosts transfer completed at %dms", dataMoverTimeOutLimit - timeOut);
                    return;
                }
            } else {
                log_.error("lastRawFruHostTransferred.right == null");
            }
            log_.info("Waiting for data mover - Raw FRU Hosts: %dms left", timeOut);
            timeOut -= sleepForOneSecond();
        }
    }

    /**
     * InterruptedExeptions are ignored.
     * @return sleepLength
     */
    private long sleepForOneSecond() {
        final long sleepLengthWaitingForDataMoverToFinish = 1000;
        try {
            Thread.sleep(sleepLengthWaitingForDataMoverToFinish);
        } catch (InterruptedException e) {
            log_.info(e.getMessage());
        }
        return sleepLengthWaitingForDataMoverToFinish;
    }

    private void initializeDependencies() {
        util_ = new HWInvUtilImpl(log_);

        foreignInventoryDatabaseClient_ = new ForeignInventoryClient(log_);
        onlineInventoryDatabaseClient_ = factory_.createHWInvApi();
        nearLineInventoryDatabaseClient_ = factory_.createInventorySnapshotApi();
    }

    boolean areEmptyInventoryTablesInPostgres() {
        ImmutablePair<Long, String> lastRawDimm =
                nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawDimmIngested();
        log_.info("lastRawDimm: %d %s", lastRawDimm.left, lastRawDimm.right);
        ImmutablePair<Long, String> lastRawFruHost =
                nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawFruHostIngested();
        log_.info("lastRawFruHost: %d %s", lastRawFruHost.left, lastRawFruHost.right);

        return lastRawDimm.right == null && lastRawFruHost.right == null;
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

    ImmutablePair<Long, String> getCharacteristicsOfLastRawDimmIngestedIntoNearLine() {
        log_.info(">> getCharacteristicsOfLastRawDimmIngestedIntoNearLine()");
        try {
            ImmutablePair<Long, String> lastIngestedRawDimm =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawDimmIngested();
            return Objects.requireNonNullElse(lastIngestedRawDimm, ImmutablePair.nullPair());
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
            return ImmutablePair.nullPair();
        }
    }

    ImmutablePair<Long, String> getCharacteristicsOfLastRawFruHostIngestedIntoNearLine() {  // must not be private or Spy will not work
        log_.info(">> getCharacteristicsOfLastRawFruHost()");
        try {
            ImmutablePair<Long, String> lastIngestedRawFruHost =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawFruHostIngested();
            return Objects.requireNonNullElse(lastIngestedRawFruHost, ImmutablePair.nullPair());
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
            return ImmutablePair.nullPair();
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
        log_.debug("Number of changed nodes: %d", changedNodeLocations.size());
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
