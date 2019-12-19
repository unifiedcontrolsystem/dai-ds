package com.intel.dai.dsimpl.voltdb;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.intel.dai.dsapi.HWInvApi;
import com.intel.dai.dsapi.HWInvSlot;
import com.intel.dai.dsapi.HWInvTree;
import com.intel.dai.dsapi.HWInvUtil;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.client.Client;
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
        for (HWInvSlot slot: hwInv.FRUS) {
            try {
                logger.info("Ingesting %s", slot.ID);
                client.callProcedure("UpsertFRUIntoHWInv",
                        slot.ID, slot.ParentID, slot.Type, slot.Ordinal,
                        slot.FRUID, slot.FRUType, slot.FRUSubType);
                status = 0;
            } catch (ProcCallException e) {
                // upsert errors are ignored
                logger.error("ProcCallException during UPSERT");
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

    private Logger logger;
    private String[] servers;
    private Client client = null;
}
