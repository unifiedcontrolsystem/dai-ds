// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;


import com.intel.dai.dsapi.BootImage;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.*;


public class VoltDbBootImage implements BootImage {

    private Client voltClient;
    private Logger logger;
    private String adapterType;

    public VoltDbBootImage(String[] servers, String type, Logger logger) {
        VoltDbClient.initializeVoltDbClient(servers);
        adapterType = type;
        this.logger = logger;
    }

    protected Client getClient() {
        return VoltDbClient.getVoltClientInstance();
    }

    public void initialize() {
        voltClient = getClient();
    }

    public String addBootImageProfile(Map<String, String> bootImg) throws DataStoreException
    {

        ClientResponse response;
        try {
            if (bootImg.get("target_id") == null) {
                logger.info("%s %s  %s %s %s %s %s %s %s", bootImg.get("target_id"), bootImg.get("description"),
                        bootImg.get("bootimagefile"), bootImg.get("bootimagechecksum"), bootImg.get("bootoptions"),
                        bootImg.get("bootstrapimagefile"), bootImg.get("bootstrapimagechecksum"),
                        bootImg.get("kernelargs"), bootImg.get("files"));
                // Queue this base work item for this particular adapter.
                response = voltClient.callProcedure("BootImageUpdateInfo"
                        , bootImg.getOrDefault("target_id", "")
                        , bootImg.getOrDefault("description", "")
                        , bootImg.getOrDefault("bootimagefile", "")
                        , bootImg.getOrDefault("bootimagechecksum", "")
                        , bootImg.getOrDefault("bootoptions", "")
                        , bootImg.getOrDefault("bootstrapimagefile", "")
                        , bootImg.getOrDefault("bootstrapimagechecksum", "")
                        ,"A"
                        ,adapterType
                        ,-1
                        , bootImg.getOrDefault("kernelargs", "")
                        , bootImg.getOrDefault("files", "")
                );
            } else {
                response = copyBootImageProfile(bootImg);
            }
        }catch (ProcCallException | IOException  | NullPointerException ie) {
            logger.exception(ie);
            throw new DataStoreException("There is an error when BootImage profile is being updated");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage profile update has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return updateBootImageProfileInHistoryTable(bootImg);
    }

    public String editBootImageProfile(Map<String, String> bootImg) throws DataStoreException
    {
        String id = bootImg.get("id");
        logger.debug("Data passed to editBootImageProfile for image '%s':  %s %s %s %s %s %s %s %s %s", id,
                bootImg.get("id"), bootImg.get("description"), bootImg.get("bootimagefile"),
                bootImg.get("bootimagechecksum"), bootImg.get("bootoptions"), bootImg.get("bootstrapimagefile"),
                bootImg.get("bootstrapimagechecksum"), bootImg.get("kernelargs"), bootImg.get("files"));
        Map<String, String> existingInfo = retrieveBootImageProfile(id);
        ClientResponse response;
        Map<String, String> mergedMap = mergeMapsFromUserAndVolt(bootImg, existingInfo);
        try {
            // Queue this base work item for this particular adapter.
            response = voltClient.callProcedure("BootImageUpdateInfo"
                    , mergedMap.getOrDefault("id", "")
                    , mergedMap.getOrDefault("description", "")
                    , mergedMap.getOrDefault("bootimagefile", "")
                    , mergedMap.getOrDefault("bootimagechecksum", "")
                    , mergedMap.getOrDefault("bootoptions", "")
                    , mergedMap.getOrDefault("bootstrapimagefile", "")
                    , mergedMap.getOrDefault("bootstrapimagechecksum", "")
                    , mergedMap.getOrDefault("state", "A")
                    , adapterType
                    , -1
                    , mergedMap.getOrDefault("kernelargs", "")
                    , mergedMap.getOrDefault("files", "")
            );
        }catch (ProcCallException | IOException  | NullPointerException ie) {
            logger.exception(ie);
            throw new DataStoreException("There is an error when BootImage profile is being updated in history table");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage profile update into history table has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return updateBootImageProfileInHistoryTable(mergedMap);
    }

    private Map<String, String> mergeMapsFromUserAndVolt(Map<String, String> bootImage,
                                                         Map<String, String> existingBootImageInfo) {
        String[] expectedKeys = new String[] {
                "description", "bootimagefile", "bootimagechecksum", "bootoptions", "bootstrapimagefile",
                "bootstrapimagechecksum", "state", "kernelargs", "files"
        };
        Map<String, String> mergedMap = new HashMap<>();
        Map<String,String> modifiedExisting = new HashMap<>();
        for(Map.Entry<String,String> entry: existingBootImageInfo.entrySet())
            modifiedExisting.put(entry.getKey().toLowerCase(), entry.getValue());
        mergedMap.put("id", bootImage.get("id"));
        for(String key: expectedKeys)
            mergedMap.put(key, bootImage.getOrDefault(key, modifiedExisting.get(key)));

        return mergedMap;
    }

    private String updateBootImageProfileInHistoryTable(Map<String, String> bootImg) throws DataStoreException {
        ClientResponse response;
        try {
            // Queue this base work item for this particular adapter.
            response = voltClient.callProcedure("BOOTIMAGE_HISTORY.insert"
                    , bootImg.getOrDefault("id", "")
                    , bootImg.getOrDefault("description", "")
                    , bootImg.getOrDefault("bootimagefile", "")
                    , bootImg.getOrDefault("bootimagechecksum", "")
                    , bootImg.getOrDefault("bootoptions", "")
                    , bootImg.getOrDefault("bootstrapimagefile", "")
                    , bootImg.getOrDefault("bootstrapimagechecksum", "")
                    ,"A"
                    ,(System.currentTimeMillis() * 1000L)
                    ,(System.currentTimeMillis() * 1000L)
                    ,adapterType
                    ,-1
                    , bootImg.getOrDefault("kernelargs", "")
                    , bootImg.getOrDefault("files", "")
            );
        }catch (ProcCallException | IOException  | NullPointerException ie) {
            logger.exception(ie);
            throw new DataStoreException("There is an error when BootImage profile is being updated in history table");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage profile update into history table has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return "Succeeded in performing operation";
    }

    private ClientResponse copyBootImageProfile(Map<String, String> bootImg) throws
        DataStoreException, ProcCallException, IOException, NullPointerException
    {
        // We are going to let addBootImageProfile() handle all exceptions.  Basically, Copy Profile is
        // now a special case of Add Profile.
        //
        // Note that at this moment, voltDB neither supports CREATE nor EXEC.  This makes it
        // difficult to perform the duplicate operation within one stored procedure.

        String profileId = bootImg.get("id");
        String targetId = bootImg.get("target_id");
        logger.info("%s %s", profileId, targetId);

        Map<String, String> srcImg = retrieveBootImageProfile(profileId);

        // Because of the different in key names expected by used in
        // addOrEditBootImageProfile() and the key names returned by
        // retrieveBootImageProfile(), we have to remap the profile values.

        // Queue this base work item for this particular adapter.
        return voltClient.callProcedure("BootImageUpdateInfo"
                , targetId
                , srcImg.get("DESCRIPTION")
                , srcImg.get("BOOTIMAGEFILE")
                , srcImg.get("BOOTIMAGECHECKSUM")
                , srcImg.get("BOOTOPTIONS")
                , srcImg.get("BOOTSTRAPIMAGEFILE")
                , srcImg.get("BOOTSTRAPIMAGECHECKSUM")
                ,"A"
                ,adapterType
                ,-1
                , srcImg.get("KERNELARGS")
                , srcImg.get("FILES")
        );
    }

    public String deleteBootImageProfile(String profile_id) throws DataStoreException
    {
        ClientResponse response;
        try {
            // Queue this base work item for this particular adapter.
            response = voltClient.callProcedure("BootImageDeleteInfo", profile_id);
        }catch (ProcCallException | IOException ie) {
            ie.printStackTrace();
            throw new DataStoreException("There is an error when BootImage profile is being deleted");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage profile delete has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return "Succeeded in deleting bootimage";
    }

    public Map<String, String> retrieveBootImageProfile(String profile_id) throws DataStoreException
    {
        ClientResponse response;
        try {
            // Queue this base work item for this particular adapter.
            logger.info(profile_id);
            response = voltClient.callProcedure("BootImageGetInfo", profile_id);
        }catch (ProcCallException | IOException ie) {
            ie.printStackTrace();
            throw new DataStoreException("There is an error when BootImage profile is being retrieved");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage profile retrieval has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return convertToMap(response.getResults()[0]);
    }

    public List<String> listBootImageProfiles() throws DataStoreException
    {
        ClientResponse response;
        try {
            // Queue this base work item for this particular adapter.
            response = voltClient.callProcedure("BootImageGetIds");
        }catch (ProcCallException | IOException ie) {
            ie.printStackTrace();
            throw new DataStoreException("There is an error when BootImage names are being retrieved");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage names retrieval has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return convertToList(response.getResults()[0], 0);
    }

    private Map<String, String> convertToMap(VoltTable resultData) {
        Map<String, String> result = new HashMap<>();
        int totalRows = resultData.getRowCount();
        int totalColumns = resultData.getColumnCount();
        for (int row = 0; row < totalRows; row++) {
            VoltTableRow rowData = resultData.fetchRow(row);
            for (int column = 0; column < totalColumns; column++) {
                result.put(resultData.getColumnName(column), rowData.getString(column));
                logger.info("convert data to Map: %s %s", resultData.getColumnName(column),
                        rowData.getString(column));
            }
        }
        return result;
    }

    private List<String> convertToList(VoltTable resultData, int column) {
        List<String> result = new ArrayList<>();
        int totalRows = resultData.getRowCount();
        for (int row = 0; row < totalRows; row++) {
            VoltTableRow rowData = resultData.fetchRow(row);
            result.add( rowData.getString(column));
        }
        return result;
    }

    public Map<String, String> getComputeNodesBootImageId(Set<String> computeNodes) {

        ClientResponse response;
        Map <String, String> result = new HashMap<>();
        for (String node: computeNodes) {
            logger.info("Get Bootimage info for Node = %s", node);
            try {
                response = voltClient.callProcedure("ComputeNodeGetBootImageId",
                        node);
            } catch (IOException | ProcCallException ie) {
                ie.printStackTrace();
                result.put(node, "An exception occurred while retrieving bootimage Id");
                continue;
            }
            if (response.getResults()[0].getRowCount() == 0) {
                logger.info("Get Bootimage info Node = %s Bootimage = doesn't exist", node);
                result.put(node, "Device doesn't exist");
                continue;
            }
            //get the current BootImageId for this node location and save in the hashmap
            String currentBootImageId = response.getResults()[0].fetchRow(0).getString(0);
            logger.info("Get Bootimage info Node = %s Bootimage = %s", node, currentBootImageId);
            result.put(node, currentBootImageId);
        }
        return result;
    }

    @Override
    public String updateComputeNodeBootImageId(String lctn, String id, String adapterType ) throws DataStoreException {
        ClientResponse response;
        try {
            logger.info("updating compute node boot image: Location=%s; NewBootImageId=%s", lctn, id);
            // Queue this base work item for this particular adapter.
            response = voltClient.callProcedure("ComputeNodeSaveBootImageInfo"
                    , lctn
                    , id
                    , System.currentTimeMillis() * 1000L
                    , adapterType
                    ,-1
            );
        }catch (ProcCallException | IOException  | NullPointerException ie) {
            logger.exception(ie);
            throw new DataStoreException("There is an error when BootImage profile is being updated");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("BootImage profile update has FAILED - Status=%s, StatusString=%s",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new DataStoreException(response.getStatusString());
        }
        return "Succeeded in performing operation";
    }
}