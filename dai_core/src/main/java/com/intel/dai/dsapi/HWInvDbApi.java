// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface that allows HW inventory to be stored in an underlying DB.  The HW
 * inventory is encoded in canonical form which is a list of HW locations.  The DB stores
 * each HW location as a row.  If the location is occupied, the row contains a index into
 * the FRU table.  Each entry of the FRU table describes a FRU that ever occupied a HW
 * location.
 */
public interface HWInvDbApi {
    void initialize();

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
    int ingest(Path canonicalHWInvPath) throws IOException, InterruptedException, DataStoreException;

    /**
     * <p> Ingest part of the HW inventory tree canonical form encoded as the given json string. </p>
     * @param canonicalHWInvJson json string containing a canonical HW inventory
     * @return 0 if any location is ingested, otherwise 1
     */
    int ingest(String canonicalHWInvJson) throws InterruptedException, IOException, DataStoreException;

    void delete(String locationName) throws IOException, DataStoreException;
    // HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws IOException,
    //         DataStoreException;
    long numberOfRawInventoryRows() throws IOException, DataStoreException;

    /**
     * Return the latest HW inventory history update timestamp string in RFC-3339 format.
     *
     * @return string containing the latest update timestamp if it can be determined; otherwise null
     * @throws IOException io exception
     * @throws DataStoreException datastore exception
     */
    String lastHwInvHistoryUpdate() throws IOException, DataStoreException;

    void insertRawHistoricalRecord(String action, String id, String fru, String foreignServerTimestamp)
            throws IOException, DataStoreException;

    int ingestHistory(String canonicalHWInvHistoryJson) throws IOException, DataStoreException;
}
