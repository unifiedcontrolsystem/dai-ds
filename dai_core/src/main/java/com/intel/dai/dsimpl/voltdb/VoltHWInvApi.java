// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.HWInvApi;
import com.intel.dai.dsapi.HWInvLoc;
import com.intel.dai.dsapi.HWInvTree;
import com.intel.dai.dsapi.HWInvUtil;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.nio.file.Path;

public class VoltHWInvApi implements HWInvApi {
    private final transient HWInvUtil util;

    public VoltHWInvApi(Logger logger, HWInvUtil util, String[] servers) {
        this.logger = logger;
        this.util = util;
        this.servers = servers;
    }
    public void initialize() {
        VoltDbClient.initializeVoltDbClient(servers);
        client = VoltDbClient.getVoltClientInstance();
    }

    /**
     * <p> Ingest the content of a json file containing part of the HW inventory tree in canonical
     * form.
     * This implementation uses volt DB. </p>
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

    public HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws
            IOException, DataStoreException {
        try {
            HWInvTree t = new HWInvTree();
            ClientResponse cr = client.callProcedure("AllLocationsAtIdFromHWInv", rootLocationName);
            VoltTable vt = cr.getResults()[0];
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                HWInvLoc e = new HWInvLoc();
                e.ID = vt.getString("ID");
                e.Type = vt.getString("Type");
                e.Ordinal = (int) vt.getLong("Ordinal");
                e.FRUID = vt.getString("FRUID");
                e.FRUType = vt.getString("FRUType");
                e.FRUSubType = vt.getString("FRUSubType");
                t.locs.add(e);
            }
            if (outputJsonFileName != null) {
                util.fromStringToFile(util.toCanonicalJson(t), outputJsonFileName);
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

    public long numberOfLocationsInHWInv() throws IOException, DataStoreException {
        try {
            return client.callProcedure("NumberOfLocationsInHWInv").getResults()[0].asScalarLong();
        } catch (ProcCallException e) {
            logger.error("ProcCallException during AllLocationsAtIdFromHWInv");
            throw new DataStoreException(e.getMessage());
        } catch (NullPointerException e) {
            logger.error("Null client");
            throw new DataStoreException(e.getMessage());
        }
    }
    private Logger logger;
    private String[] servers;
    private Client client = null;
}
