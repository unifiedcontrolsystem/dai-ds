// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

/**
 * Description of enum DbStatusEnum.
 */
public enum DbStatusEnum {
    /**
     * Happens only when a bad DB string is sent to the fromDbString method.
     */
    BAD_DB_STRING(null, true),
    /**
     * Database not present or DbStatus table is missing or has no rows.
     */
    SCHEMA_MISSING(null, true),
    /**
     * The DB schema has started loading.
     */
    SCHEMA_LOADING("schema-loading", false),
    /**
     * The DB schema loaded successfully.
     */
    SCHEMA_LOADED("schema-loaded", false),
    /**
     * The DB schema load failed.
     */
    SCHEMA_ERROR("schema-error", true),
    /**
     * The DB initial data populate has started.
     */
    POPULATE_DATA_STARTED("populate-started", false),
    /**
     * The DB initial data populate has succeeded.
     */
    POPULATE_DATA_COMPLETED("populate-completed", false),
    /**
     * The DB initial data populate has failed.
     */
    POPULATE_DATA_ERROR("populate-error", true);

    /**
     * Return the DB status string for the enum value.
     *
     * @return The DB String or null if enum is BAD_DB_STRING or SCHEMA_MISSING.
     */
    public String getDbString() { return dbString_; }

    /**
     * Check if the given enum is an error value.
     *
     * @return <b>true</b> if the enum is an error value or <b>false</b> otherwise.
     */
    public boolean isError() { return error_; }

    /**
     * Create an enum instance give the DB status string.
     *
     * @param dbString Must be <b>null</b> or a valid string or a BAD_DB_STRING instance is created.
     *
     * @return The created enum instance value.
     */
    public static DbStatusEnum fromDbString(String dbString) {
        if(dbString == null)
            return SCHEMA_MISSING;
        for(DbStatusEnum e: DbStatusEnum.values()) {
            if(e.equals(SCHEMA_MISSING) || e.equals(BAD_DB_STRING))
                continue;
            if(e.getDbString().equals(dbString))
                return e;
        }
        return BAD_DB_STRING;
    }

    DbStatusEnum(String dbString, boolean errorState) { dbString_ = dbString; error_ = errorState; }
    private String dbString_;
    private boolean error_;
}
