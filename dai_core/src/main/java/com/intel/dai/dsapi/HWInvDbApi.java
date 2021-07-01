// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsapi.pojo.NodeInventory;
import com.intel.dai.exceptions.DataStoreException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Interface that allows HW inventory to be stored in an underlying DB.  The HW
 * inventory is encoded in canonical form which is a list of HW locations.  The DB stores
 * each HW location as a row.  If the location is occupied, the row contains a index into
 * the FRU table.  Each entry of the FRU table describes a FRU that ever occupied a HW
 * location.
 */
public interface HWInvDbApi {
    /**
     * <p> Initialize a client connection to the online tier database. </p>
     */
    void initialize();

    /**
     * <p> Ingest part of the HW inventory tree canonical form encoded as the given json string. </p>
     * @param canonicalHWInvJson json string containing a canonical HW inventory
     * @return number of HW inventory locations ingested
     * @throws DataStoreException when ingestion into online database failed
     */
    int ingest(String canonicalHWInvJson) throws DataStoreException;

    /**
     * <p> Ingest json string containing HW inventory history in the online database. </p>
     * @param canonicalHWInvHistoryJson json file containing HW inventory history in canonical form
     * @return list of inventory history events ingested
     * @throws DataStoreException when ingestion into online database failed
     */
    List<HWInvHistoryEvent> ingestHistory(String canonicalHWInvHistoryJson) throws DataStoreException;

    /**
     * <p> Ingests cooked nodes changed into the unified node history table. </p>
     * @param lastNodeLocationChangeTimestamp map of pairs of DAI node location and the timestamp when it changed
     * @return number of nodes ingested
     * @throws DataStoreException when node ingestion failed
     */
    int ingestCookedNodesChanged(Map<String, String> lastNodeLocationChangeTimestamp) throws DataStoreException;

    int ingest(String id, Dimm dimm) throws DataStoreException;
    int ingest(String id, FruHost fruHost) throws DataStoreException;
    int ingest(NodeInventory nodeInventory) throws DataStoreException;

    List<FruHost> enumerateFruHosts();
    Map<String, String> getDimmJsonsOnFruHost(String fruHostMac);
}
