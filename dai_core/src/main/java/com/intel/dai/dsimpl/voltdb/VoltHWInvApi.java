// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
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
    public int ingest(String inputJsonFileName) throws IOException, InterruptedException, DataStoreException {
        int status = 1;
        HWInvTree hwInv;
        try {
            hwInv = util.toCanonicalPOJO(inputJsonFileName);
        } catch (JsonIOException | JsonSyntaxException e) {
            logger.error("JSON conversion to POJO failed: %s", e.getMessage());
            return 1;
        } catch (IOException e) {
            logger.error("IOException: %s", e.getMessage());
            return 1;
        }
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

    private Logger logger;
    private String[] servers;
    private Client client = null;
}
