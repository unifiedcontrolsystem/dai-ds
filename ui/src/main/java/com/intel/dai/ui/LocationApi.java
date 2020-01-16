package com.intel.dai.ui;

import com.intel.dai.dsapi.Groups;
import com.intel.dai.dsapi.Location;
import com.intel.dai.exceptions.BadInputException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class LocationApi {
    LocationApi(Groups groupsMgr, Location locationMgr) {
        groupsMgr_ = groupsMgr;
        locationMgr_ = locationMgr;
        log_ = LoggerFactory.getInstance("UI", AdapterUIRest.class.getName(), "log4j2");
    }

    Set<String> convertHostnamesToLocations(String device) throws BadInputException {
        Set<String> nodes = convertGroupsToNodes(device);
        return locationMgr_.getLocationsFromNodes(nodes);
    }

    Set<String> convertLocationsToHostnames(String device) throws BadInputException {
        Set<String> nodes = convertGroupsToNodes(device);
        return locationMgr_.getNodesFromLocations(nodes);
    }

    Set <String> convertGroupsToNodes(String devices) {
        /* Here the devices will be a comma seperated mix of hostname, location, group and mix of all
         * Convert all groups to hostnames or locations (However they are stored in the database) */
        String[] deviceList = devices.split(",");
        Set<String> nodes = new HashSet<>();
        for(String device: deviceList) {
            Set<String> nodesInGroup;
            try {
                nodesInGroup = groupsMgr_.getDevicesFromGroup(device);
            } catch(DataStoreException e) {
                log_.exception(e);
                nodes.add(device);
                return nodes;
            }
            if(nodesInGroup.isEmpty()) {
                nodes.add(device);
            } else {
                /* It is a group */
                nodes.addAll(nodesInGroup);
            }
        }
        return nodes;
    }

    private Groups groupsMgr_;
    private Location locationMgr_;
    private Logger log_;
}
