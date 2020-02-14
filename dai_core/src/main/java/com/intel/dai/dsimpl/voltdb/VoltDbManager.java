// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.Configuration;
import com.intel.dai.dsapi.Groups;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.properties.*;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;


public class VoltDbManager implements Configuration, Groups {

    private Logger logger;
    private Client client_;
    private String[] servers_;

    public VoltDbManager(String[] servers, Logger logger) {
        servers_ = servers;
        this.logger = logger;
    }

    public void initialize() {
        VoltDbClient.initializeVoltDbClient(servers_);
        client_ = getClient();
    }

    protected Client getClient() {
        return VoltDbClient.getVoltClientInstance();
    }
    private static PropertyArray convertToJSON(VoltTable resultData) {
        PropertyArray jsonArray = new PropertyArray();
        int totalRows = resultData.getRowCount();
        int totalColumns = resultData.getColumnCount();
        for (int row = 0; row < totalRows; row++) {
            PropertyMap obj = new PropertyMap();
            VoltTableRow rowData = resultData.fetchRow(row);
            for (int column = 0; column < totalColumns; column++) {
                String column_name = resultData.getColumnName(column);
                if(resultData.getColumnType(column)== VoltType.TIMESTAMP){
                    if (rowData.getTimestampAsSqlTimestamp(column_name) == null)
                        obj.put(resultData.getColumnName(column), null);
                    else {
                        Timestamp ts = rowData.getTimestampAsSqlTimestamp(column);
                        if(ts != null)
                            obj.put(column_name, ts.toString());
                        else
                            obj.put(column_name, null);
                    }
                }
                else {
                    obj.put(resultData.getColumnName(column), rowData.get(column, rowData.getColumnType(column)));
                }
            }
            jsonArray.add(obj);
        }
        return jsonArray;
    }

    private PropertyArray getTableData(String tableName) throws DataStoreException {
        ClientResponse response;
        try {
            response = client_.callProcedure(tableName);
        } catch (IOException | ProcCallException ie) {
            ie.printStackTrace();
            throw new DataStoreException("Error occurred while retrieving the data. " +
                    "Additional information may be provided in exception stack");
        }
        VoltTable vt = response.getResults()[0];
        return convertToJSON(vt);
    }

    private PropertyArray getTableData(String tableName, String parameter0) throws DataStoreException {
        ClientResponse response;
        try {
            response = client_.callProcedure(tableName, parameter0);
        } catch (IOException | ProcCallException ie) {
            ie.printStackTrace();
            throw new DataStoreException("Error occurred while retrieving the data. " +
                    "Additional information may be provided in exception stack");
        }
        VoltTable vt = response.getResults()[0];
        return convertToJSON(vt);
    }

    Set<String> generateSetForKey(PropertyArray jsonArray, String key) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            PropertyMap jsonobject;
            try {
                jsonobject = jsonArray.getMap(i);
                if(jsonobject == null) throw new NullPointerException();
            } catch(PropertyNotExpectedType e) {
                logger.exception(e, "'jsonobject' array at index %s is not a map object!", i);
                return result;
            } catch(NullPointerException e) {
                logger.exception(e,"'jsonobject' array at index %s is null!", i);
                return result;
            }
            String value = jsonobject.getStringOrDefault(key, "");
            if((!value.equals("")))
                Collections.addAll(result, value.split(","));
        }
        return result;
    }


    private int upsertToLogicalGroupsTable(String groupName, String devices) {
        try {
            client_.callProcedure("UpsertLogicalGroups", groupName, devices);
        } catch (IOException | ProcCallException ie) {
            ie.printStackTrace();
            return -1;
        }
        return 0;
    }

    private int DeleteGroupInLogicalGroupsTable(String groupNameToDelete) throws DataStoreException {
        try {
            client_.callProcedure("DeleteGroupInLogicalGroups", groupNameToDelete);
        } catch (IOException | ProcCallException ie) {
            ie.printStackTrace();
            return -1;
        }
        Set<String> groups =  listGroups();
        Set<String> devicesToDelete = new HashSet<>();
        devicesToDelete.add(groupNameToDelete);
        for(String group: groups) {
            try {
                deleteDevicesFromGroup(group, devicesToDelete);
            } catch (DataStoreException e) {
                //Do nothing. This is expected as this group may not be subgroup at all or
                // may be a subgroup in only only one group
            }
        }

        return 0;
    }

    private Set<String> expandSubGroupsInGroup(Set<String> devicesInGroup) throws DataStoreException {
        Set<String> modifiedDevicesInGroup = new HashSet<>();
        for ( String groupName : devicesInGroup) {
            PropertyArray jsonArray = getTableData("ListLogicalGroups", groupName);
            if (jsonArray.size() != 0) {
                Set<String> devicesInSubGroup = generateSetForKey(jsonArray, "DEVICELIST");
                modifiedDevicesInGroup.addAll(devicesInSubGroup);
            }
            else {
                modifiedDevicesInGroup.add(groupName);
            }
        }
        return modifiedDevicesInGroup;
    }

    private String modifyDevicesInGroup(String groupName, Set<String> devicesInGroup) throws DataStoreException
    {
        StringJoiner modifiedDevicesInGroup = new StringJoiner(",");

        for (String s : devicesInGroup) {
            modifiedDevicesInGroup.add(s);
        }
        if (upsertToLogicalGroupsTable(groupName, modifiedDevicesInGroup.toString()) == 0)
        {
            return "Successfully modified the devices in group";
        }
        throw new DataStoreException("Error in modifying devices in group. " +
                "Additional information may be provided in exception stack");
    }

    public PropertyArray getComputeNodeConfiguration()  throws DataStoreException {
        return getTableData("ComputeNodesList");
    }

    public PropertyArray getServiceNodeConfiguration() throws DataStoreException {
        return getTableData("ServiceNodesList");
    }

    public PropertyArray getRackConfiguration() throws DataStoreException {
        return getTableData("RackList");
    }

    public String addDevicesToGroup(String groupName, Set<String> devices) throws DataStoreException {
        assert devices != null:"Passed devices==null into method addDevicesToGroup";
        Set<String> devicesInGroup;
        logger.info("Add Devices %s to a group %s", devices.toString(), groupName);
        PropertyArray jsonArray = getTableData("ListLogicalGroups", groupName);
        if (jsonArray.size() == 0) {
            return modifyDevicesInGroup(groupName, devices);
        }
        else {
            devicesInGroup = generateSetForKey(jsonArray, "DEVICELIST");
            devicesInGroup.addAll(devices);
            return modifyDevicesInGroup(groupName, devicesInGroup);
        }
    }

    public String deleteDevicesFromGroup(String groupName, Set<String> devices) throws DataStoreException {

        Set<String> devicesInGroup;
        logger.info("Remove Devices %s from a group %s", devices.toString(), groupName);
        PropertyArray jsonArray = getTableData("ListLogicalGroups", groupName);
        devicesInGroup = generateSetForKey(jsonArray, "DEVICELIST");

        logger.info("deleteDevicesFromGroup: Current devices %s in group %s", devicesInGroup.toString(),
                groupName);

        if(!devicesInGroup.containsAll(devices)) {
            throw new DataStoreException("Error occurred. Some of the devices user is trying to remove don't " +
                    "exist in the group");
        }
        if(devicesInGroup.equals(devices)) {
            logger.info("Removing all the devices in a group %s", groupName);
            if (DeleteGroupInLogicalGroupsTable(groupName) == 0) {
                return "Successfully removed all the devices in the group. Removing the group too";
            }
            else {
                throw new DataStoreException("Error occurred. Removing devices and the group failed");
            }
        }
        else {
            logger.info("Removing set of devices in a group %s", groupName);
            devicesInGroup.removeAll(devices);
            return modifyDevicesInGroup(groupName, devicesInGroup);
        }
    }

    public Set<String> getDevicesFromGroup(String groupName) throws DataStoreException {
        logger.info("Get Devices from a group %s", groupName);
        PropertyArray jsonArray = getTableData("ListLogicalGroups", groupName);
        return expandSubGroupsInGroup(generateSetForKey(jsonArray, "DEVICELIST"));
    }

    public Set<String> listGroups() throws DataStoreException {
        PropertyArray jsonArray = getTableData("ListGroupNames");
        return generateSetForKey(jsonArray, "GROUPNAME");
    }
}
