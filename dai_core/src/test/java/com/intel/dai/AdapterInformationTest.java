package com.intel.dai;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AdapterInformationTest {

    @Before
    public void setUp() throws Exception {
        info_ = new AdapterInformation("TYPE", "NAME", "LOCATION", "HOSTNAME", 999L);
    }

    @Test
    public void getBaseName() {
        assertEquals("NAME", info_.getBaseName());
    }

    @Test
    public void getName() {
        assertEquals("NAME_999", info_.getName());
        info_.setUniqueNameExtension("PROFILENAME");
        assertEquals("NAME_PROFILENAME", info_.getName());
    }

    @Test
    public void getType() {
        assertEquals("TYPE", info_.getType());
    }

    @Test
    public void getLocation() {
        assertEquals("LOCATION", info_.getLocation());
    }

    @Test
    public void getHostname() {
        assertEquals("HOSTNAME", info_.getHostname());
    }

    @Test
    public void getId() {
        assertEquals(999L, info_.getId());
    }

    @Test
    public void setId() {
        info_.setId(333L);
        assertEquals(333L, info_.getId());
    }

    @Test
    public void getPid() {
        assertTrue(info_.getPid() >= 1);
    }

    @Test
    public void isShuttingDown() {
        assertFalse(info_.isShuttingDown());
    }

    @Test
    public void signalToShutdown() {
        info_.signalToShutdown();
        assertTrue(info_.isShuttingDown());
    }

    @Test
    public void getBaseWorkItemId() {
        assertEquals(-1L, info_.getBaseWorkItemId());
    }

    @Test
    public void setBaseWorkItemId() {
        info_.setBaseWorkItemId(9999L);
        assertEquals(9999L, info_.getBaseWorkItemId());
    }

    @Test
    public void serversTests() {
        assertEquals(1, info_.getServers().length);
        assertEquals("127.0.0.1", info_.getServers()[0]);
        info_.setServers(new String[] {"192.168.0.3", "192.168.0.4"});
        assertEquals(2, info_.getServers().length);
        assertEquals("192.168.0.3", info_.getServers()[0]);
        assertEquals("192.168.0.4", info_.getServers()[1]);
    }

    AdapterInformation info_;
}
