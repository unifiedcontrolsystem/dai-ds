package com.intel.dai.dsapi;

import org.junit.Test;

import static org.junit.Assert.*;

public class NodeIpAndBmcIpTest {
    @Test
    public void allTests() {
        NodeIpAndBmcIp info = new NodeIpAndBmcIp("10.1.2.3", "10.2.2.3");
        assertEquals("10.1.2.3", info.nodeIpAddress);
        assertEquals("10.2.2.3", info.bmcIpAddress);
    }
}
