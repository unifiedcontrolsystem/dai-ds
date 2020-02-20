// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface that allows HW inventory to be stored in an underlying DB.  The HW
 * inventory is encoded in canonical form which is a list of HW locations.  The DB stores
 * each HW location as a row.  If the location is occupied, the row contains a index into
 * the FRU table.  Each entry of the FRU table describes a FRU that ever occupied a HW
 * location.
 */
public interface HWInvApi {
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
     * @return 0 if any location is ingested, otherwise
     */
    int ingest(String canonicalHWInvJson) throws InterruptedException, IOException, DataStoreException;

    void delete(String locationName) throws IOException, DataStoreException;
    HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws IOException, DataStoreException;
    long numberOfLocationsInHWInv() throws IOException, DataStoreException;

    void insertHistoricalRecord(String action, String id, String fru) throws
            InterruptedException, IOException, DataStoreException;

    List<String> dumpHistoricalRecords() throws IOException, DataStoreException;
}