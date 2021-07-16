package com.intel.dai.inventory.api.database;

import com.google.gson.Gson;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.pojo.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class RawInventoryDataIngester {
    private static Logger log_;
    private static DataStoreFactory factory_;

    private final static Gson gson = new Gson();
    private static long totalNumberOfDocumentsEnumerated = 0;
    private static long totalNumberOfDocumentsIngested = 0;
    protected static HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb

    private static String LastIdIngested = null;
    private static String lastKeyIngested = null;
    private static long lastDocTimestampIngested = 0;


    public static void initialize(DataStoreFactory factory, Logger logger) {
        logger.info(">> initialize()");
        factory_ = factory;
        log_ = logger;
        onlineInventoryDatabaseClient_ = factory.createHWInvApi();
        onlineInventoryDatabaseClient_.initialize();
    }

    public static void ingestDimm(ImmutablePair<String, String> doc) {
        String id = doc.left;
        Dimm dimm = gson.fromJson(doc.right, Dimm.class);
        dimm.ib_dimm = gson.fromJson(dimm.rawIbDimm, IbDimmPojo.class);
        dimm.rawIbDimm = null;
        dimm.locator = dimm.ib_dimm.Locator;

        try {
            int numRawDimmIngested = onlineInventoryDatabaseClient_.ingest(id, dimm);
            if (numRawDimmIngested != 1) {
                log_.error("Failed to ingest raw DIMM");
                return;
            }
            totalNumberOfDocumentsIngested += numRawDimmIngested;
            LastIdIngested = doc.left;
            lastKeyIngested = dimm.serial;
            lastDocTimestampIngested = dimm.timestamp;
            log_.debug("ES ingested %s: %d, %s", doc.left, dimm.timestamp, dimm.serial);
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    public static void ingestFruHost(ImmutablePair<String, String> doc) {
        String id = doc.left;
        FruHost fruHost = gson.fromJson(doc.right, FruHost.class);

        fruHost.oob_fru = gson.fromJson(fruHost.rawOobFru, OobFruPojo.class);
        fruHost.rawOobFru = null;
        fruHost.oob_rev_info = gson.fromJson(fruHost.rawOobRevInfo, OobRevInfoPojo.class);
        fruHost.rawOobRevInfo = null;

        fruHost.ib_bios = gson.fromJson(fruHost.rawIbBios, IbBiosPojo.class);
        fruHost.rawIbBios = null;

        fruHost.boardSerial = fruHost.oob_fru.Board_Serial;

        try {
            int numRawFruHostIngested = onlineInventoryDatabaseClient_.ingest(id, fruHost);
            if (numRawFruHostIngested != 1) {
                log_.error("Failed to ingest raw FRU host");
                return;
            }
            totalNumberOfDocumentsIngested += numRawFruHostIngested;
            LastIdIngested = doc.left;
            lastKeyIngested = fruHost.mac;
            lastDocTimestampIngested = fruHost.timestamp;
            log_.debug("ES ingested %s: %d, %s", doc.left, fruHost.timestamp, fruHost.mac);
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }
}
