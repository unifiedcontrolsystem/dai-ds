package com.intel.dai.dsapi;

import org.junit.Test;

import static org.junit.Assert.*;

public class DbStatusEnumTest {
    @Test
    public void fromDbStringNull() {
        DbStatusEnum e = DbStatusEnum.fromDbString(null);
        assertEquals(DbStatusEnum.SCHEMA_MISSING, e);
        assertNull(e.getDbString());
        assertTrue(e.isError());
    }

    @Test
    public void fromStringValue() {
        DbStatusEnum e = DbStatusEnum.fromDbString("schema-loading");
        assertEquals(DbStatusEnum.SCHEMA_LOADING, e);
        assertEquals(DbStatusEnum.SCHEMA_LOADING.getDbString(), e.getDbString());
        assertFalse(e.isError());
    }

    @Test
    public void fromStringUnknown() {
        DbStatusEnum e = DbStatusEnum.fromDbString("unknown");
        assertEquals(DbStatusEnum.BAD_DB_STRING, e);
        assertNull(e.getDbString());
        assertTrue(e.isError());
    }
}
