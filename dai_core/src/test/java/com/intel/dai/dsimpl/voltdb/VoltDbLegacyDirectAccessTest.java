package com.intel.dai.dsimpl.voltdb;

import org.junit.Test;

import static org.junit.Assert.*;

public class VoltDbLegacyDirectAccessTest {
    @Test
    public void ctor() {
        new VoltDbLegacyDirectAccess(new String[] { "127.0.0.1" });
        new VoltDbLegacyDirectAccess(new String[0]);
        new VoltDbLegacyDirectAccess(null);
    }
}
