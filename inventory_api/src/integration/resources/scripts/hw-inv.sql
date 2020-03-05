-- HW_Location is always an element of an array.
-- HW_Location type is not necessarily the same as Field_Replaceble_Unit type.  They only need to be compatible.
-- There is a 1 to 1 onto relationship between subset(HW_Location) and subset(Field_Replaceble_Unit).
--   We represent the vacancy of an empty slot as UCS-PLACEHOLDER-<ID>.
-- Note that FOREIGN KEY constraint is not supported.
--   This can create some sloppiness when referencing HW locations from another table.
-- The HW_Location primary key also serves as a path to the FRU in the HPC organization hierarchy.

-- Unfortunately, a 1 table implementation is not possible because VoltDB
-- is not ANSI-compliant.  You cannot have multiple NULL values in a UNIQUE column.

LOAD CLASSES build/libs/procedures.jar;

file -inlinebatch END_OF_BATCH

--------------------------------------------------------------
-- Foreign HW Inventory
--------------------------------------------------------------
-- Records all FRUs that are and were in the HPC.
CREATE TABLE HW_Inventory_FRU (
    FRUID VARCHAR(80) NOT NULL PRIMARY KEY,     -- perhaps <manufacturer>-<serial#>
    FRUType VARCHAR(16),                        -- Field_Replaceble_Unit category(HMS type)
    FRUSubType VARCHAR(32),                     -- perhaps specific model; NULL:unspecifed
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

-- Corresponds to the current HPC HW architecture wrt to HW locations.
-- Note that FRUID is not unique in foreign data.  This is because node enclosures have no ID.
CREATE TABLE HW_Inventory_Location (
    ID VARCHAR(64) NOT NULL PRIMARY KEY, -- perhaps xname (path); as is from JSON
    Type VARCHAR(16) NOT NULL,           -- Location category(HMS type)
    Ordinal INTEGER NOT NULL,            -- singleton:0
    FRUID VARCHAR(80) NOT NULL,          -- perhaps <manufacturer>-<serial#>
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

-- History of FRU installation and removal from the HPC.  Note that the timestamp marks
-- the DB update event.  The foreign data does not have the time of actual HW modification.
CREATE TABLE HW_Inventory_History (
    Action VARCHAR(16) NOT NULL,            -- INSERTED/DELETED
    ID VARCHAR(64) NOT NULL,                -- perhaps xname (path); as is from JSON
    FRUID VARCHAR(80) NOT NULL,             -- perhaps <manufacturer>-<serial#>
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

--------------------------------------------------------------

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.UpsertLocationIntoHWInv;
CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.DeleteAllLocationsAtIdFromHWInv;
CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.AllLocationsAtIdFromHWInv;
CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.NumberOfLocationsInHWInv;
CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.HwInventoryHistoryInsert;
CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.HwInventoryHistoryDump;

END_OF_BATCH
