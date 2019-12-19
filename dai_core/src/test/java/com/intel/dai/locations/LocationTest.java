/* Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

*/
package com.intel.dai.locations;

import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.BadInputException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocationTest {

    private Logger log = mock(Logger.class);

    @Before
    public void setUp() {
    }

    @Test(expected = BadInputException.class)
    public void getLocationEmptyCompNodeHostNameToLctnMap() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(new HashMap<>());
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getLocation();
    }

    @Test
    public void getLocationMapCompNodeHostNameToLctnthrowsException() throws BadInputException, IOException, ProcCallException
    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        when(adapter.mapCompNodeHostNameToLctn()).thenThrow(IOException.class);
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getLocation();
    }

    @Test
    public void getLocationForOneNode() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);
        Location NodeLocation = new Location(adapter, nodes, log);
        Map<String, String> result = NodeLocation.getLocation();
        assertEquals("test_location1", result.get("test_node1"));
    }

    @Test
    public void getLocationForTwoNodes() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        /*Mix of hostname and locations is allowed */
        nodes.add("test_node1");
        nodes.add("test_location2");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        hostnameToLctn.put("test_node2", "test_location2");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);
        Location NodeLocation = new Location(adapter, nodes, log);
        Map<String, String> result = NodeLocation.getLocation();
        assertEquals("test_location1", result.get("test_node1"));
        assertEquals("test_location2", result.get("test_location2"));
    }

    @Test(expected = BadInputException.class)
    public void getLocationForTwoNodesWithOneNodeNotInMap() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        nodes.add("test_node2");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);
        Location NodeLocation = new Location(adapter, nodes, log);
        Map<String, String> result = NodeLocation.getLocation();
    }

    @Test(expected = BadInputException.class)
    public void getHostnameEmptyCompNodeHostNameToLctnMap() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        when(adapter.mapCompNodeLctnToHostName()).thenReturn(new HashMap<>());
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getHostname();
    }

    @Test
    public void getHostnameMapCompNodeHostNameToLctnthrowsException() throws BadInputException, IOException, ProcCallException
    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        when(adapter.mapCompNodeLctnToHostName()).thenThrow(IOException.class);
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getHostname();
    }

    @Test
    public void getHostnameForOneNode() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_location1", "test_node1");
        when(adapter.mapCompNodeLctnToHostName()).thenReturn(hostnameToLctn);
        Location NodeLocation = new Location(adapter, nodes, log);
        Map<String, String> result = NodeLocation.getHostname();
        assertEquals( "test_node1", result.get("test_node1"));
    }

    @Test
    public void getHostnameForTwoNodes() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        /*Mix of hostname and locations is allowed */
        nodes.add("test_node1");
        nodes.add("test_location2");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_location1", "test_node1");
        hostnameToLctn.put("test_location2", "test_node2");
        when(adapter.mapCompNodeLctnToHostName()).thenReturn(hostnameToLctn);
        Location NodeLocation = new Location(adapter, nodes, log);
        Map<String, String> result = NodeLocation.getHostname();
        assertEquals("test_node1", result.get("test_node1"));
        assertEquals("test_node2", result.get("test_location2"));
    }

    @Test(expected = BadInputException.class)
    public void getHostnameForTwoNodesWithOneNodeNotInMap() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        nodes.add("test_node2");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_location1", "test_node1");
        when(adapter.mapCompNodeLctnToHostName()).thenReturn(hostnameToLctn);
        Location NodeLocation = new Location(adapter, nodes, log);
        Map<String, String> result = NodeLocation.getHostname();
    }


    @Test(expected = BadInputException.class)
    public void getAggregatorNodeLocationsEmptyCompNodeHostNameToLctnMap() throws BadInputException, IOException, ProcCallException
    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        when(adapter.mapCompNodeLctnToHostName()).thenReturn(new HashMap<>());
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getAggregatorNodeLocations();
    }

    @Test(expected = BadInputException.class)
    public void getAggregatorNodeLocationsMapNodeLctnToAggregatorthrowsException() throws BadInputException, IOException, ProcCallException
    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);
        when(adapter.mapNodeLctnToAggregator()).thenThrow(IOException.class);
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getAggregatorNodeLocations();
    }

    @Test(expected = BadInputException.class)
    public void getAggregatorNodeLocationsEmptyNodeLctnToAggregatorMap() throws BadInputException, IOException, ProcCallException
    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);
        when(adapter.mapNodeLctnToAggregator()).thenReturn(new HashMap<>());
        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getAggregatorNodeLocations();
    }

    @Test
    public void getAggregatorNodeLocationsForOneNode() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");

        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);

        Map<String, String> NodeLctnToAggregator = new HashMap<>();
        NodeLctnToAggregator.put("test_location1", "test_aggregator1");
        when(adapter.mapNodeLctnToAggregator()).thenReturn(NodeLctnToAggregator);


        Location NodeLocation = new Location(adapter, nodes, log);

        Set<String> expectedNodes = new HashSet<>();
        expectedNodes.add("test_node1");
        Map<String, Set<String>> result = NodeLocation.getAggregatorNodeLocations();
        assertEquals(expectedNodes, result.get("test_aggregator1"));
    }

    @Test
    public void getAggregatorNodeLocationsForTwoNodes() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        /*Mix of hostname and locations is allowed */
        nodes.add("test_node1");
        nodes.add("test_location2");

        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        hostnameToLctn.put("test_node2", "test_location2");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);

        Map<String, String> NodeLctnToAggregator = new HashMap<>();
        NodeLctnToAggregator.put("test_location1", "test_aggregator1");
        NodeLctnToAggregator.put("test_location2", "test_aggregator2");
        when(adapter.mapNodeLctnToAggregator()).thenReturn(NodeLctnToAggregator);

        Location NodeLocation = new Location(adapter, nodes, log);

        Set<String> expectedNodes = new HashSet<>();
        expectedNodes.add("test_node1");

        Set<String> expectedNodes1 = new HashSet<>();
        expectedNodes1.add("test_location2");

        Map<String, Set<String>> result = NodeLocation.getAggregatorNodeLocations();
        assertEquals(expectedNodes, result.get("test_aggregator1"));
        assertEquals(expectedNodes1, result.get("test_aggregator2"));
    }

    @Test(expected = BadInputException.class)
    public void getAggregatorNodeLocationsForTwoNodesWithOneNodeNotInMap() throws BadInputException, IOException, ProcCallException

    {
        IAdapter adapter = mock(IAdapter.class);
        Set<String> nodes = new HashSet<>();
        nodes.add("test_node1");
        nodes.add("test_node2");

        Map<String, String> hostnameToLctn = new HashMap<>();
        hostnameToLctn.put("test_node1", "test_location1");
        hostnameToLctn.put("test_node2", "test_location2");
        when(adapter.mapCompNodeHostNameToLctn()).thenReturn(hostnameToLctn);

        Map<String, String> NodeLctnToAggregator = new HashMap<>();
        NodeLctnToAggregator.put("test_location1", "test_aggregator1");
        when(adapter.mapNodeLctnToAggregator()).thenReturn(NodeLctnToAggregator);


        Location NodeLocation = new Location(adapter, nodes, log);
        NodeLocation.getAggregatorNodeLocations();
    }
}