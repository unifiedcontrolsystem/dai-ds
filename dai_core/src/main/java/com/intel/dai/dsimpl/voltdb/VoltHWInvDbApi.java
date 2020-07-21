// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.nio.file.Path;
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
     * @throws InterruptedException Interrupted Exception
     * @throws IOException IO Exception
     * @throws DataStoreException Datastore Exception
     */
    @Override
    public int ingestHistory(String canonicalHWInvHistoryJson) throws InterruptedException, IOException, DataStoreException {
        HWInvHistory hist = util.toCanonicalHistoryPOJO(canonicalHWInvHistoryJson);
        if (hist == null) {
            logger.error("Foreign HW inventory history conversion to POJO failed");
            return 1;
        }
        return ingest(hist);
    }

    private int ingest(HWInvHistory hist) throws IOException, DataStoreException, InterruptedException {
        int status = 1;
        // The HWInvHistory constructor guarantees that hwInv.locs is never null.
        for (HWInvHistoryEvent evt: hist.events) {
            logger.info("Ingesting %s %s %s %s", evt.Action, evt.ID, evt.FRUID, evt.Timestamp);
            insertHistoricalRecord(evt.Action, evt.ID, evt.FRUID, evt.Timestamp);
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
                client.callProcedure("UpsertLocationIntoHWInv",
                        slot.ID, slot.Type, slot.Ordinal,
                        slot.FRUID, slot.FRUType, slot.FRUSubType);
                status = 0;
            } catch (ProcCallException e) {
                // upsert errors are ignored
                logger.error("ProcCallException during UpsertLocationIntoHWInv");
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
            client.callProcedure("DeleteAllLocationsAtIdFromHWInv", locationName);
        } catch (ProcCallException e) {
            logger.error("ProcCallException during DeleteAllLocationsAtIdFromHWInv");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    @Override
    public HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws
            IOException, DataStoreException {
        try {
            HWInvTree t = new HWInvTree();
            ClientResponse cr = client.callProcedure("AllLocationsAtIdFromHWInv", rootLocationName);
            VoltTable vt = cr.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                HWInvLoc loc = new HWInvLoc();
                loc.ID = vt.getString("ID");
                loc.Type = vt.getString("Type");
                loc.Ordinal = (int) vt.getLong("Ordinal");
                loc.FRUID = vt.getString("FRUID");
                loc.FRUType = vt.getString("FRUType");
                loc.FRUSubType = vt.getString("FRUSubType");
                t.locs.add(loc);
            }
            if (outputJsonFileName != null) {
                util.toFile(util.toCanonicalJson(t), outputJsonFileName);
            }
            return t;
        } catch (ProcCallException e) {
            logger.error("ProcCallException during AllLocationsAtIdFromHWInv");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    @Override
    public long numberOfLocationsInHWInv() throws IOException, DataStoreException {
        try {
            ClientResponse response = client.callProcedure("NumberOfLocationsInHWInv");
            VoltTable result = response.getResults()[0];
            if (result.getRowCount() < 1) {
                return 0;
            }

            result.advanceRow();
            return result.asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during AllLocationsAtIdFromHWInv");
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
            ClientResponse response = client.callProcedure("HwInventoryHistoryLastUpdateTimestamp");
            VoltTable result = response.getResults()[0];
            if (result.getRowCount() < 1) {
                return "";
            }

            result.advanceRow();
            return result.getString(0);
        } catch (ProcCallException e) {
            logger.error("ProcCallException during HwInventoryHistoryLastUpdateTimestamp");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    @Override
    public void insertHistoricalRecord(String action, String id, String fru, String foreignServerTimestamp) throws
            InterruptedException, IOException, DataStoreException {
        logger.info("%s %s %s %s", action, id, fru, foreignServerTimestamp);

        if (hwInventoryHistoryEventCount(action, id, fru, foreignServerTimestamp) > 0) {
            logger.info("Duplicate detected.  History event is ignored.");
            return;
        }

        try {
            client.callProcedure("HwInventoryHistoryInsert", action, id, fru, foreignServerTimestamp);
        } catch (ProcCallException e) {
            // insert errors are only logged for now
            logger.error("ProcCallException during HwInventoryHistoryInsert");
        } catch (NullPointerException e) {
            logger.error("Null locs list");
            throw new DataStoreException(e.getMessage());
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
    }

    private long hwInventoryHistoryEventCount(String action, String id, String fru, String foreignServerTimestamp) throws
            IOException, DataStoreException {
        try {
            return client.callProcedure("HwInventoryHistoryEventCount", action, id, fru, foreignServerTimestamp).
                    getResults()[0].asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during HwInventoryHistoryEventCount");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }

    /**
     * <p> Dumps the history of HW inventory changes as a chronologically sorted list of records.
     * Each record is a csv of (DbUpdatedTimestamp, action, location, fru).  This method is
     * primarily used for debugging. </p>
     * @return a list of HW change records
     * @throws IOException i/o exception
     * @throws DataStoreException data exception
     */
    @Override
    public List<String> dumpHistoricalRecords() throws IOException, DataStoreException {
        try {
            ArrayList<String> hist = new ArrayList<>();
            ClientResponse cr = client.callProcedure("HwInventoryHistoryDump", "");
            VoltTable vt = cr.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                String rec = String.format("%s, %s, %s, %s",
                        vt.getString("DbUpdatedTimestamp"),
                        vt.getString("Action"),
                        vt.getString("ID"),
                        vt.getString("FRUID"));
                logger.debug(rec);
                hist.add(rec);
            }
            return hist;
        } catch (ProcCallException e) {
            logger.error("ProcCallException during AllLocationsAtIdFromHWInv");
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
