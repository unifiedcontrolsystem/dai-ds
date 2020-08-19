// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface that allows HW inventory to be stored in an underlying DB.  The HW
 * inventory is encoded in canonical form which is a list of HW locations.  The DB stores
 * each HW location as a row.  If the location is occupied, the row contains a index into
 * the FRU table.  Each entry of the FRU table describes a FRU that ever occupied a HW
 * location.
 */
public class VoltHWInvDbApi implements HWInvDbApi {
    private final transient HWInvUtil util;

    public VoltHWInvDbApi(Logger logger, HWInvUtil util, String[] servers) {
        this.logger = logger;
        this.util = util;
        this.servers = servers;
    }
    public void initialize() {
        VoltDbClient.initializeVoltDbClient(servers);
        client = VoltDbClient.getVoltClientInstance();
    }

    /**
     * <p> Ingest the content of a json file containing the part of the HW inventory tree in
     * canonical form.
     * The choice of DB is made in the implementation. </p>
     * @param canonicalHWInvPath path to the json containing the HW inventory in canonical form
     * @return 0 if any location is ingested, otherwise 1
     * @throws IOException i/o exception is possible because this includes a file operation
     * @throws InterruptedException user may interrupt if this were to be run in a CLI
     * @throws DataStoreException DAI Data Store Exception
     */
    @Override
    public int ingest(Path canonicalHWInvPath) throws IOException, InterruptedException, DataStoreException {
        HWInvTree hwInv = util.toCanonicalPOJO(canonicalHWInvPath);
        if (hwInv == null) {
            logger.error("Foreign HW inventory conversion to POJO failed");
            return 1;
        }
        return ingest(hwInv);
    }

    /**
     * <p> Ingest json string containing HW inventory history. </p>
     * @param canonicalHWInvHistoryJson json file containing HW inventory history in canonical form
     * @return 0 if any location is ingested, otherwise 1
     * @throws IOException IO Exception
     * @throws DataStoreException DataStore Exception
     */
    @Override
    public int ingestHistory(String canonicalHWInvHistoryJson) throws IOException, DataStoreException {
        HWInvHistory hist = util.toCanonicalHistoryPOJO(canonicalHWInvHistoryJson);
        if (hist == null) {
            logger.error("Foreign HW inventory history conversion to POJO failed");
            return 1;
        }
        return ingest(hist);
    }

    private int ingest(HWInvHistory hist) throws IOException, DataStoreException {
        int status = 1;
        // The HWInvHistory constructor guarantees that hwInv.locs is never null.
        for (HWInvHistoryEvent evt: hist.events) {
            logger.info("Ingesting %s %s %s %s", evt.Action, evt.ID, evt.FRUID, evt.Timestamp);
            insertRawHistoricalRecord(evt.Action, evt.ID, evt.FRUID, evt.Timestamp);
            status = 0;
        }
        return status;
    }

    /**
     * <p> Ingest json string containing part of the HW inventory tree in canonical form encoded
     * as the given json string. </p>
     * @param canonicalHWInvJson json string containing a canonical HW inventory
     * @return 0 if any location is ingested, otherwise
     */
    @Override
    public int ingest(String canonicalHWInvJson) throws InterruptedException, IOException, DataStoreException {
        HWInvTree hwInv = util.toCanonicalPOJO(canonicalHWInvJson);
        if (hwInv == null) {
            logger.error("Foreign HW inventory conversion to POJO failed");
            return 1;
        }
        return ingest(hwInv);
    }

    private int ingest(HWInvTree hwInv) throws IOException, DataStoreException, InterruptedException {
        int status = 1;
        // The HWInvTree constructor guarantees that hwInv.locs is never null.
        for (HWInvLoc slot: hwInv.locs) {
            try {
                logger.info("Ingesting %s", slot.ID);
                client.callProcedure("RawInventoryInsert",
                        slot.ID, slot.Type, slot.Ordinal, slot.Info,
                        slot.FRUID, slot.FRUType, slot.FRUSubType, slot.FRUInfo);
                status = 0;
            } catch (ProcCallException e) {
                // upsert errors are ignored
                logger.error("ProcCallException during RawInventoryInsert");
            } catch (NullPointerException e) {
                logger.error("Null locs list");
                throw new DataStoreException(e.getMessage());
            }
        }
        try {
            client.drain();
        } catch (NoConnectionsException e) {
            logger.error("No DB Connections");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
        return status;
    }

    @Override
    public void delete(String locationName) throws IOException, DataStoreException {
        try {
            client.callProcedure("RawInventoryDelete", locationName);
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryDelete");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    public int ingestCookedNode(String foreignTimestamp, String action, String nodeLocation)
            throws IOException, DataStoreException {

        String hwInfoJson = null;
        String nodeSerialNumber = null;

        if (action.equals("Added")) {
            ImmutablePair<String, String> hwInfo = generateHWInfoJsonBlob(nodeLocation);
            nodeSerialNumber = hwInfo.left;
            hwInfoJson = hwInfo.right;
        }

        long foreignTimestampInMillisecondsSinceEpoch = Instant.parse(foreignTimestamp).toEpochMilli();
        insertNodeHistory(nodeLocation, foreignTimestampInMillisecondsSinceEpoch, hwInfoJson,
                nodeSerialNumber);

        return 1;
    }

    public long numberOfCookedNodes() throws DataStoreException, IOException {
        try {
            return client.callProcedure("NodeHistoryRowCount").getResults()[0].asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during NodeHistoryRowCount");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    public void deleteAllCookedNodes() throws IOException, DataStoreException {
        try {
            client.callProcedure("NodeHistoryDelete");
        } catch (ProcCallException e) {
            logger.error("ProcCallException during NodeHistoryDelete");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    private void insertNodeHistory(String nodeLocation, long foreignTimestampInMillisecondsSinceEpoch,
                                   String hwInfoJson, String nodeSerialNumber) throws IOException, DataStoreException {
        try {
            ClientResponse response = client.callProcedure(
                    "NodeHistoryInsert"
                    , nodeLocation
                    , foreignTimestampInMillisecondsSinceEpoch
                    , hwInfoJson
                    , nodeSerialNumber
            );
            logger.info("NodeHistoryInsert getStatus():", response.getStatus());
        } catch (ProcCallException e) {
            logger.error("ProcCallException during NodeHistoryInsert");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    private ImmutablePair<String, String> generateHWInfoJsonBlob(String nodeLocation)
            throws IOException, DataStoreException {
        String node_serial_number = "";

        HWInvTree t = allLocationsAt(nodeLocation, null);

        JsonObject hwInfoJsonEntries = new JsonObject();
        for (HWInvLoc loc : t.locs) {
            if (loc.Type.equals("Node")) {
                node_serial_number = loc.FRUID;
            }
            JsonObject entries = loc.toHWInfoJsonFields();
            hwInfoJsonEntries.putAll(entries); // all entries are distinct
        }

        JsonObject hwInfo = new JsonObject();
        hwInfo.put("HWInfo", hwInfoJsonEntries);

        String hwInfoJson = hwInfo.toJson();
        hwInfoJson = hwInfoJson.replace("\\/", "/");    // escaping / makes queries very difficult

        return new ImmutablePair<>(node_serial_number, hwInfoJson);
    }

    // @Override
    public HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws
            IOException, DataStoreException {
        try {
            HWInvTree t = new HWInvTree();
            ClientResponse cr = client.callProcedure("RawInventoryDump", rootLocationName);
            VoltTable vt = cr.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                HWInvLoc loc = new HWInvLoc();
                loc.ID = vt.getString("ID");
                loc.Type = vt.getString("Type");
                loc.Ordinal = (int) vt.getLong("Ordinal");
                loc.Info = vt.getString("Info");
                loc.FRUID = vt.getString("FRUID");
                loc.FRUType = vt.getString("FRUType");
                loc.FRUSubType = vt.getString("FRUSubType");
                loc.FRUInfo = vt.getString("FRUInfo");
                t.locs.add(loc);
            }
            if (outputJsonFileName != null) {
                util.toFile(util.toCanonicalJson(t), outputJsonFileName);
            }
            return t;
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryDump");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    @Override
    public long numberOfRawInventoryRows() throws IOException, DataStoreException {
        try {
            return client.callProcedure("RawInventoryRowCount").getResults()[0].asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryRowCount");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    /**
     * Return the latest HW inventory history update timestamp string in RFC-3339 format.  Notice that the foreign
     * timestamp is stored as a string in the DB.  This is because we only use this timestamp as a string argument when
     * making a rest api call to the foreign server.
     *
     * @return string containing the latest update timestamp if it can be determined; otherwise null
     * @throws IOException io exception
     * @throws DataStoreException datastore exception
     */
    @Override
    public String lastHwInvHistoryUpdate() throws IOException, DataStoreException {
        try {
            return client.callProcedure("RawInventoryHistoryLastUpdate").getResults()[0].
                    fetchRow(0).getString(0);
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryHistoryLastUpdate");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    @Override
    public void insertRawHistoricalRecord(String action, String id, String fru, String foreignServerTimestamp)
            throws IOException, DataStoreException {
        logger.info("%s %s %s %s", action, id, fru, foreignServerTimestamp);

        try {
            client.callProcedure("RawInventoryHistoryInsert", action, id, fru, foreignServerTimestamp);
        } catch (ProcCallException e) {
            // insert errors are only logged for now
            logger.error("ProcCallException during RawInventoryHistoryInsert");
        } catch (NullPointerException e) {
            logger.error("Null locs list");
            throw new DataStoreException(e.getMessage());
        }
    }

    public long numberRawInventoryHistoryRows()
            throws IOException, DataStoreException {
        try {
            return client.callProcedure("RawInventoryHistoryRowCount").
                    getResults()[0].asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryHistoryRowCount");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    /**
     * Ingests changed nodes from the raw inventory tables.
     */
    public int ingestCookedNodesChanged()
            throws DataStoreException, IOException {
        int numberOfNodesChanged = 0;
        try {
            ClientResponse cr = client.callProcedure("RawInventoryHistoryDump");
            VoltTable vt = cr.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();

                logger.debug(String.format("%s, %s, %s, %s, %s",
                        vt.getTimestampAsSqlTimestamp("DbUpdatedTimestamp").toString(),
                        vt.getString("ForeignTimestamp"),
                        vt.getString("Action"),
                        vt.getString("ID"),
                        vt.getString("FRUID")));

                String foreignTimestamp = vt.getString("ForeignTimestamp");
                String location = vt.getString("ID");
                String actionOnNode = vt.getString("Action");  // Added or Removed

                if (!isNode(location)) {
                    continue;
                }
                ingestCookedNode(foreignTimestamp, actionOnNode, location);
                numberOfNodesChanged++;
            }
            return numberOfNodesChanged;
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryHistoryDump");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    private boolean isNode(String location) {
        return StringUtils.countMatches(location, "-") == 2
                && StringUtils.countMatches(location, "CN") == 1;  // can handle null location string
    }

    public List<String> dumpCookedNodes() throws DataStoreException, IOException {
        List<String> dump = new ArrayList<>();

        try {
            ClientResponse cr = client.callProcedure("NodeHistoryDump");
            VoltTable vt = cr.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();

                String hwInfoJson = vt.getString("InventoryInfo");
                if (hwInfoJson != null) {
                    hwInfoJson = hwInfoJson.substring(0, 64) + " ...}";
                }
                dump.add(String.format("(%s -> [%s, %s, %s, %s])%n",
                        vt.getString("Lctn"),
                        vt.getTimestampAsSqlTimestamp("DbUpdatedTimestamp").toString(),
                        vt.getTimestampAsSqlTimestamp("InventoryTimestamp").toString(),
                        vt.getString("Sernum"),
                        hwInfoJson)
                );
            }

            return dump;
        } catch (ProcCallException e) {
            logger.error("ProcCallException during NodeHistoryDump");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null Pointer Exception");
            throw new DataStoreException(e.getMessage());
        }
    }

    private final Logger logger;
    private final String[] servers;
    Client client = null;
}
