// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.google.gson.Gson;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.*;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsapi.pojo.NodeInventory;
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
import java.util.HashMap;
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

    /**
     * <p> Initialize a client connection to the online tier database. </p>
     */
    @Override
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
     * @throws DataStoreException DAI Data Store Exception
     */
    int ingest(Path canonicalHWInvPath) throws DataStoreException {
        HWInvTree hwInv = util.toCanonicalPOJO(canonicalHWInvPath);
        if (hwInv == null) {
            logger.error("Foreign HW inventory conversion to POJO failed");
            return 0;
        }
        try {
            return ingest(hwInv);
        } catch (IOException | InterruptedException e) {
            throw new DataStoreException(e.getMessage());
        }
    }

    /**
     * <p> Ingest json string containing HW inventory history in the online database. </p>
     * @param canonicalHWInvHistoryJson json file containing HW inventory history in canonical form
     * @return list of inventory history events ingested
     * @throws DataStoreException when ingestion into online database failed
     */
    @Override
    public List<HWInvHistoryEvent> ingestHistory(String canonicalHWInvHistoryJson) throws DataStoreException {
        HWInvHistory hist = util.toCanonicalHistoryPOJO(canonicalHWInvHistoryJson);
        if (hist == null) {
            logger.error("HWI:%n  Foreign HW inventory history conversion to POJO failed");
            return new ArrayList<>();
        }
        try {
            return ingest(hist);
        } catch (IOException e) {
            throw new DataStoreException(e.getMessage());
        }
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
     * @throws DataStoreException when ingestion into online database failed
     */
    @Override
    public int ingest(String canonicalHWInvJson) throws DataStoreException {
        HWInvTree hwInv = util.toCanonicalPOJO(canonicalHWInvJson);
        if (hwInv == null) {
            logger.error("HWI:%n  Foreign HW inventory conversion to POJO failed");
            return 0;
        }
        try {
            return ingest(hwInv);
        } catch (InterruptedException | IOException e) {
            throw new DataStoreException(e.getMessage());
        }
    }

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

    int ingestCookedNode(String nodeLocation, String foreignTimestamp)
            throws IOException, DataStoreException {

        ImmutablePair<String, String> hwInfo = generateHWInfoJsonBlob(nodeLocation);
        String nodeSerialNumber = hwInfo.left;
        String hwInfoJson = hwInfo.right;

        long foreignTimestampInMillisecondsSinceEpoch = Instant.parse(foreignTimestamp).toEpochMilli();
        return insertNodeHistory(nodeLocation, foreignTimestampInMillisecondsSinceEpoch, hwInfoJson,
                nodeSerialNumber);
    }

    long numberOfCookedNodes() throws DataStoreException, IOException {
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

    void deleteAllCookedNodes() throws IOException, DataStoreException {
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

    int insertNodeHistory(String nodeLocation, long foreignTimestampInMillisecondsSinceEpoch,
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
                return 0;
            }
            logger.info("HWI:%n  NodeHistoryInsert(nodeLocation=%s, foreignTimestampInMillisecondsSinceEpoch=%d) was successful",
                    nodeLocation, foreignTimestampInMillisecondsSinceEpoch);
        } catch (ProcCallException e) {
            logger.error("HWI:%n  ProcCallException during NodeHistoryInsert");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("HWI:%n  Null client");
            throw new DataStoreException(e.getMessage());
        }
        return 1;
    }

    ImmutablePair<String, String> generateHWInfoJsonBlob(String nodeLocation)
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

    HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws
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
            throw new DataStoreException(e.getMessage());
        }
    }

    long numberOfRawInventoryRows() throws IOException, DataStoreException {
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

    int insertRawHistoricalRecord(HWInvHistoryEvent evt)
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

    /**
     * Deletes all raw historical records from the raw inventory history voltdb table.  Note that this
     * method is only used during testing.
     * @throws DataStoreException when raw inventory history cannot be deleted
     */
    void deleteAllRawHistoricalRecords() throws DataStoreException {
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
        } catch (IOException e) {
            logger.error("IO IOException");
            throw new DataStoreException(e.getMessage());
        }
    }

    long numberRawInventoryHistoryRows()
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
     * <p> Ingests cooked nodes changed into the unified node history table. </p>
     * @param lastNodeLocationChangeTimestamp map of pairs of DAI node location and the timestamp when it changed
     * @return number of nodes ingested
     * @throws DataStoreException when node ingestion failed
     */
    @Override
    public int ingestCookedNodesChanged(Map<String, String> lastNodeLocationChangeTimestamp)
            throws DataStoreException {
        int numCookedNodesIngested = 0;
        for (Map.Entry<String,String> entry : lastNodeLocationChangeTimestamp.entrySet()) {
            try {
                ingestCookedNode(entry.getKey(), entry.getValue());
                numCookedNodesIngested++;
            } catch (IOException e) {
                logger.error("IOException:%s", e.getMessage());
                throw new DataStoreException(e.getMessage());
            }
        }
        return numCookedNodesIngested;
    }

    List<String> dumpCookedNodes() throws DataStoreException, IOException {
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


    public int ingest(String id, Dimm dimm) throws DataStoreException {
        try {
            return upsertRawDimm(id, dimm);
        } catch (IOException e) {
            logger.error("IOException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        } catch (ProcCallException e) {
            logger.error("ProcCallException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    public int ingest(String id, FruHost fruHost) throws DataStoreException {
        try {
            return upsertRawFruHost(id, fruHost);
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    public int ingest(NodeInventory nodeInventory) throws DataStoreException {
        try {
            return insertNodeInventoryHistory(nodeInventory);
        } catch (IOException e) {
            logger.error("IOException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        } catch (ProcCallException e) {
            logger.error("ProcCallException:%s", e.getMessage());
            throw new DataStoreException(e.getMessage());
        }
    }

    int upsertRawDimm(String id, Dimm dimm) throws IOException, ProcCallException {
        String source = gson.toJson(dimm);
        ClientResponse cr = client.callProcedure("Raw_DIMM_Insert",
                id, dimm.serial, dimm.mac, dimm.locator, source, dimm.timestamp);

        if (cr.getStatus() != ClientResponse.SUCCESS) {
            logger.error("upsertRawDimm(id=%s) => %d", id, cr.getStatus());
            return 0;
        }
        return 1;
    }

    int upsertRawFruHost(String id, FruHost fruHost) throws IOException, ProcCallException {
        String source = gson.toJson(fruHost);
        ClientResponse cr = client.callProcedure("Raw_FRU_Host_Insert",
                id, fruHost.boardSerial, fruHost.mac, source, fruHost.timestamp);

        if (cr.getStatus() != ClientResponse.SUCCESS) {
            logger.error("upsertRawFruHost(id=%s) => %d", id, cr.getStatus());
            return 0;
        }
        return 1;
    }

    int insertNodeInventoryHistory(NodeInventory nodeInventory) throws IOException, ProcCallException {
        String source = gson.toJson(nodeInventory);
        ClientResponse cr = client.callProcedure("Raw_Node_Inventory_History_insert", source);

        if (cr.getStatus() != ClientResponse.SUCCESS) {
            logger.error("insertNodeInventoryHistory(source=%s) => %d", source, cr.getStatus());
            return 0;
        }
        return 1;
    }

    public List<FruHost> enumerateFruHosts() {
        try {
            ClientResponse cr = client.callProcedure("Get_FRU_Hosts");
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error("cr.getStatusString(): %d", cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
            logger.info("Number of FRUs = %s", tuples.getRowCount());
            tuples.resetRowPosition();
            ArrayList<FruHost> fruHosts = new ArrayList<>();
            long numberFrusEnumerated = 0;
            while (tuples.advanceRow()) {
                numberFrusEnumerated += 1;
                String source = tuples.getString(3);
                logger.debug("%d: %s%n", numberFrusEnumerated, source);
                fruHosts.add(gson.fromJson(source, FruHost.class));
            }
            return fruHosts;
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public Map<String, String> getDimmJsonsOnFruHost(String fruHostMac) {
        try {
            ClientResponse cr = client.callProcedure("Get_Dimms_on_FRU_Host", fruHostMac);
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                logger.error(cr.getStatusString());
                return null;
            }
            VoltTable tuples = cr.getResults()[0];
            logger.info("Number of DIMM associated with %s = %d", fruHostMac, tuples.getRowCount());
            tuples.resetRowPosition();
            HashMap<String, String> dimmMap = new HashMap<>();
            int i = 0;
            while (tuples.advanceRow()) {
                i += 1;
//                String serial = tuples.getString(0);
//                String mac = tuples.getString(1);
//                long timestamp = tuples.getTimestampAsLong(2);
                String locator = tuples.getString(3);
                String source = tuples.getString(4);
//                long DbUpdatedTimestamp = tuples.getTimestampAsLong(5);

                logger.debug("%d: %s", i, source);
                dimmMap.put(locator, source);
            }
            return dimmMap;
        } catch (IOException | ProcCallException e) {
            logger.error(e.getMessage());
        }
        return null;
    }
    private final static Gson gson = new Gson();
    private final Logger logger;
    private final String[] servers;
    private Client client = null;
}
