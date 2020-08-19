file -inlinebatch END_OF_BATCH

CREATE TABLE RawHWInventory_History (
    Action VARCHAR(16) NOT NULL,                -- INSERTED/DELETED
    ID VARCHAR(64) NOT NULL,                    -- perhaps xname (path); as is from JSON
    FRUID VARCHAR(80) NOT NULL,                 -- perhaps <manufacturer>-<serial#>
    ForeignTimestamp VARCHAR(24) NOT NULL,      -- Foreign server timestamp string in RFC-3339 format
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (Action, ID, ForeignTimestamp)  -- allows the use of upsert to eliminate duplicates
);

CREATE TABLE HW_Inventory_FRU (
    FRUID VARCHAR(80) NOT NULL PRIMARY KEY,     -- perhaps <manufacturer>-<serial#>
    FRUType VARCHAR(16),                        -- Field_Replaceble_Unit category(HMS type)
    FRUSubType VARCHAR(32),                     -- perhaps specific model; NULL:unspecifed
    FRUInfo VARCHAR(8192),
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

CREATE TABLE HW_Inventory_Location (
    ID VARCHAR(64) NOT NULL PRIMARY KEY, -- Location ID translated from JSON
    Type VARCHAR(16) NOT NULL,           -- Location category(HMS type)
    Ordinal INTEGER NOT NULL,            -- singleton:0
    Info VARCHAR(8192),
    FRUID VARCHAR(80) NOT NULL,          -- perhaps <manufacturer>-<serial#>
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

CREATE INDEX HW_Inventory_Location_fru_id_index ON HW_Inventory_Location(FRUID);

END_OF_BATCH
