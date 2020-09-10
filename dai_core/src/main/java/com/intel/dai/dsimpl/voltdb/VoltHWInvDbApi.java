// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
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
import java.util.Map;

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
     * @return number of HW inventory locations ingested
     * @throws IOException i/o exception is possible because this includes a file operation
     * @throws InterruptedException user may interrupt if this were to be run in a CLI
     * @throws DataStoreException DAI Data Store Exception
     */
    @Override
    public int ingest(Path canonicalHWInvPath) throws IOException, InterruptedException, DataStoreException {
        HWInvTree hwInv = util.toCanonicalPOJO(canonicalHWInvPath);
        if (hwInv == null) {
            logger.error("Foreign HW inventory conversion to POJO failed");
            return 0;
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
    public List<HWInvHistoryEvent> ingestHistory(String canonicalHWInvHistoryJson) throws IOException, DataStoreException {
        HWInvHistory hist = util.toCanonicalHistoryPOJO(canonicalHWInvHistoryJson);
        if (hist == null) {
            logger.error("HWI:%n  Foreign HW inventory history conversion to POJO failed");
            return new ArrayList<>();
        }
        return ingest(hist);
    }

    private List<HWInvHistoryEvent> ingest(HWInvHistory hist) throws IOException, DataStoreException {

        List<HWInvHistoryEvent> changedLocations = new ArrayList<>();

        // The HWInvHistory constructor guarantees that hist.events is never null.
        for (HWInvHistoryEvent evt: hist.events) {
            logger.debug("Ingest historic record: %s", evt.toString());
            insertRawHistoricalRecord(evt);
            changedLocations.add(evt);
        }
        return changedLocations;
    }

    /**
     * <p> Ingest json string containing part of the HW inventory tree in canonical form encoded
     * as the given json string. </p>
     * @param canonicalHWInvJson json string containing a canonical HW inventory
     * @return number of HW inventory locations ingested
     */
    @Override
    public int ingest(String canonicalHWInvJson) throws InterruptedException, IOException, DataStoreException {
        HWInvTree hwInv = util.toCanonicalPOJO(canonicalHWInvJson);
        if (hwInv == null) {
            logger.error("HWI:%n  Foreign HW inventory conversion to POJO failed");
            return 0;
        }
        return ingest(hwInv);
    }

    /**
     *
     * @param hwInv
     * @return number of items ingested
     * @throws IOException
     * @throws DataStoreException
     * @throws InterruptedException
     */
    private int ingest(HWInvTree hwInv) throws IOException, DataStoreException, InterruptedException {
        logger.debug("HWI:%n  ingest(HWInvTree hwInv=%s) >>", util.head(toString(), 360));
        int numRowsIngested = 0;

        // The HWInvTree constructor guarantees that hwInv.locs is never null.
        for (HWInvLoc loc : hwInv.locs) {
            try {
                logger.debug("Ingest loc %s", loc.toString());
                ClientResponse cr = client.callProcedure("RawInventoryInsert",
                        loc.ID, loc.Type, loc.Ordinal, loc.Info,
                        loc.FRUID, loc.FRUType, loc.FRUSubType, loc.FRUInfo);
                if (cr.getStatus() != ClientResponse.SUCCESS) {
                    logger.error("HWI:%n  RawInventoryInsert(loc=%s) => %d",
                            loc.toString(), cr.getStatus());
                }
                numRowsIngested++;
            } catch (ProcCallException e) {
                // upsert errors are ignored
                logger.error("HWI:%n  ProcCallException during RawInventoryInsert: %s", e.getMessage());
            } catch (NullPointerException e) {
                logger.error("HWI:%n  Null locs list");
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
        return numRowsIngested;
    }

    @Override
    public void delete(String locationName) throws IOException, DataStoreException {
        try {
            ClientResponse cr = client.callProcedure("RawInventoryDelete", locationName);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n  RawInventoryDelete(locationName=%s) => %d",
                        locationName, cr.getStatus());
            }
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryDelete");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    public int ingestCookedNode(String nodeLocation, String foreignTimestamp)
            throws IOException, DataStoreException {

        ImmutablePair<String, String> hwInfo = generateHWInfoJsonBlob(nodeLocation);
        String nodeSerialNumber = hwInfo.left;
        String hwInfoJson = hwInfo.right;

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
            ClientResponse cr = client.callProcedure("NodeHistoryDelete");
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n  callProcedure() => %d", cr.getStatus());
            }
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
            ClientResponse cr = client.callProcedure(
                    "NodeHistoryInsert"
                    , nodeLocation
                    , foreignTimestampInMillisecondsSinceEpoch
                    , hwInfoJson
                    , nodeSerialNumber
            );
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n  NodeHistoryInsert(nodeLocation=%s) => %d",
                        nodeLocation, cr.getStatus());
            }
        } catch (ProcCallException e) {
            logger.error("HWI:%n  ProcCallException during NodeHistoryInsert");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("HWI:%n  Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    private ImmutablePair<String, String> generateHWInfoJsonBlob(String nodeLocation)
            throws IOException, DataStoreException {
        String node_serial_number = "";

        ConfigIO parser = ConfigIOFactory.getInstance("json");
        PropertyMap hwInfoJsonEntries = new PropertyMap();

        HWInvTree t = allLocationsAt(nodeLocation, null);
        for (HWInvLoc loc : t.locs) {
            if (loc.Type.equals("Node")) {
                node_serial_number = loc.FRUID;
            }
            JsonObject entries = loc.toHWInfoJsonFields();
            hwInfoJsonEntries.putAll(entries); // all entries are distinct
        }

        PropertyMap hwInfo = new PropertyMap();
        hwInfo.put("HWInfo", hwInfoJsonEntries);

        String hwInfoJson = null;
        if (parser != null) {
            hwInfoJson = parser.toString(hwInfo);
            hwInfoJson = hwInfoJson.replace("\\/", "/");    // escaping / makes queries very difficult
        }

        return new ImmutablePair<>(node_serial_number, hwInfoJson);
    }

    // @Override
    public HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws
            IOException, DataStoreException {
        try {
            HWInvTree t = new HWInvTree();
            ClientResponse cr = client.callProcedure("RawInventoryDump", rootLocationName);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n  RawInventoryDump(rootLocationName=%s) => %d",
                        rootLocationName, cr.getStatus());
                return t;
            }
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

    /**
     *
     * @param evt
     * @return number of raw history events ingested
     * @throws IOException
     * @throws DataStoreException
     */
    public int insertRawHistoricalRecord(HWInvHistoryEvent evt)
            throws IOException, DataStoreException {
        try {
            ClientResponse cr = client.callProcedure("RawInventoryHistoryInsert",
                    evt.Action, evt.ID, evt.FRUID, evt.Timestamp);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n   RawInventoryHistoryInsert(evt=%s) => %d",
                        evt.toString(), cr.getStatus());
                return 0;
            }
        } catch (ProcCallException e) {
            // insert errors are only logged for now
            logger.error("HWI:%n  ProcCallException during RawInventoryHistoryInsert:%s", e.getMessage());
        } catch (NullPointerException e) {
            logger.error("HWI:%n  Null locs list");
            throw new DataStoreException(e.getMessage());
        }
        return 1;
    }

    @Override
    public void deleteAllRawHistoricalRecords() throws IOException, DataStoreException {
        try {
            ClientResponse cr = client.callProcedure("RawInventoryHistoryDelete");
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n   NodeHistoryDump() => %d", cr.getStatus());
            }
        } catch (ProcCallException e) {
            logger.error("ProcCallException during RawInventoryHistoryDelete");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
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
     *
     * Note that if the history table only contains the updated history then
     * there is no need to consider start time.  We can guarantee this at
     * the client side by always dropping the history and snapshot tables prior
     * to calling this method.  Failing to do so will results in a lot of
     * upsert operations.
     */
    @Override
    public int ingestCookedNodesChanged(Map<String, String> lastNodeLocationChangeTimestamp) {
        int numCookedNodesIngested = 0;
        for (Map.Entry<String,String> entry : lastNodeLocationChangeTimestamp.entrySet()) {
            try {
                ingestCookedNode(entry.getKey(), entry.getValue());
                numCookedNodesIngested++;
            } catch (IOException e) {
                logger.error("IOException:%s", e.getMessage());
            } catch (DataStoreException e) {
                logger.error("DataStoreException:%s", e.getMessage());
            }
        }
        return numCookedNodesIngested;
    }

    public List<String> dumpCookedNodes() throws DataStoreException, IOException {
        List<String> dump = new ArrayList<>();

        try {
            ClientResponse cr = client.callProcedure("NodeHistoryDump");
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("HWI:%n   NodeHistoryDump() => %d", cr.getStatus());
                return new ArrayList<>();
            }
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

    public long numberCookedNodeInventoryHistoryRows()
            throws IOException, DataStoreException {
        try {
            return client.callProcedure("NodeHistoryRowCount").
                    getResults()[0].asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during NodeHistoryRowCount");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    private final Logger logger;
    private final String[] servers;
    private Client client = null;
}
