package com.intel.dai.ui;

import com.intel.dai.dsapi.Groups;
import com.intel.dai.dsapi.Location;
import com.intel.dai.exceptions.BadInputException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.LoggerFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocationApiTest {

    @Before
    public void setup() {

        groupsMgr_ = mock(Groups.class);
        locationMgr_= mock(Location.class);

        locationApi_ = new LocationApi(groupsMgr_, locationMgr_);
    }

    @Test
    public void convertHostnamesToLocationsForDeviceThatIsNotGroup() throws DataStoreException, BadInputException {
        String deviceName = "c01";

        Set<String> emptyGroup = new HashSet<>();
        when(groupsMgr_.getDevicesFromGroup(deviceName)).thenReturn(emptyGroup);

        String locationName="R0-CH0-N0";
        Set<String> deviceSet = new HashSet<>();
        deviceSet.add(deviceName);
        Set<String> locationSet = new HashSet<>();
        locationSet.add(locationName);
        when(locationMgr_.getLocationsFromNodes(deviceSet)).thenReturn(locationSet);

        assertEquals(locationSet, locationApi_.convertHostnamesToLocations(deviceName));
    }

    @Test
    public void convertHostnamesToLocationsForDeviceThatIsGroup() throws DataStoreException, BadInputException {
        String deviceName = "g1";

        Set<String> devicesInGroup = new HashSet<>();
        devicesInGroup.add("c01");
        devicesInGroup.add("c02");
        when(groupsMgr_.getDevicesFromGroup(deviceName)).thenReturn(devicesInGroup);

        Set<String> locationSet = new HashSet<>();
        locationSet.add("R0-CH0-N0");
        locationSet.add("R0-CH0-N1");
        when(locationMgr_.getLocationsFromNodes(devicesInGroup)).thenReturn(locationSet);

        assertEquals(locationSet, locationApi_.convertHostnamesToLocations(deviceName));
    }

    @Test
    public void convertHostnamesToLocationsGroupsMgrThrowsException() throws DataStoreException, BadInputException {
        String deviceName = "c01";

        doThrow(new DataStoreException("Error processing data")).when(groupsMgr_).getDevicesFromGroup(deviceName);

        String locationName="R0-CH0-N0";
        Set<String> deviceSet = new HashSet<>();
        deviceSet.add(deviceName);
        Set<String> locationSet = new HashSet<>();
        locationSet.add(locationName);
        when(locationMgr_.getLocationsFromNodes(deviceSet)).thenReturn(locationSet);

        assertEquals(locationSet, locationApi_.convertHostnamesToLocations(deviceName));
    }

    @Test
    public void convertLocationsToHostnamesForDeviceThatIsNotGroup() throws DataStoreException, BadInputException {
        String locationName="R0-CH0-N0";

        Set<String> emptyGroup = new HashSet<>();
        when(groupsMgr_.getDevicesFromGroup(locationName)).thenReturn(emptyGroup);

        String deviceName = "c01";
        Set<String> deviceSet = new HashSet<>();
        deviceSet.add(deviceName);
        Set<String> locationSet = new HashSet<>();
        locationSet.add(locationName);
        when(locationMgr_.getNodesFromLocations(locationSet)).thenReturn(deviceSet);

        assertEquals(deviceSet, locationApi_.convertLocationsToHostnames(locationName));
    }

    @Test
    public void convertLocationsToHostnamesForDeviceThatIsGroup() throws DataStoreException, BadInputException {
        String locationName = "g1";

        Set<String> deviceSet = new HashSet<>();
        deviceSet.add("c01");
        deviceSet.add("c02");

        Set<String> locationSet = new HashSet<>();
        locationSet.add("R0-CH0-N0");
        locationSet.add("R0-CH0-N1");
        when(groupsMgr_.getDevicesFromGroup(locationName)).thenReturn(locationSet);
        when(locationMgr_.getNodesFromLocations(locationSet)).thenReturn(deviceSet);

        assertEquals(deviceSet, locationApi_.convertLocationsToHostnames(locationName));
    }

    @Test
    public void convertLocationsToHostnamesGroupsMgrThrowsException() throws DataStoreException, BadInputException {
        String locationName="R0-CH0-N0";

        doThrow(new DataStoreException("Error processing data")).when(groupsMgr_).getDevicesFromGroup(locationName);

        String deviceName = "c01";
        Set<String> deviceSet = new HashSet<>();
        deviceSet.add(deviceName);
        Set<String> locationSet = new HashSet<>();
        locationSet.add(locationName);
        when(locationMgr_.getNodesFromLocations(locationSet)).thenReturn(deviceSet);

        assertEquals(deviceSet, locationApi_.convertLocationsToHostnames(locationName));
    }

    private Groups groupsMgr_;
    private Location locationMgr_;
    private LocationApi locationApi_;
}
