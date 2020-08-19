LOAD CLASSES procedures/build/libs/procedures.jar;

file -inlinebatch END_OF_BATCH

-- Raw inventory history from foreign servers
CREATE PROCEDURE RawInventoryHistoryInsert
   AS UPSERT INTO RawHWInventory_History(Action, ID, FRUID, ForeignTimestamp, DbUpdatedTimestamp)
        VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP);

CREATE PROCEDURE RawInventoryHistoryDump
    AS SELECT * FROM RawHWInventory_History;

CREATE PROCEDURE RawInventoryHistoryRowCount   -- needed for testing
   AS SELECT COUNT(*) FROM RawHWInventory_History;

CREATE PROCEDURE RawInventoryHistoryLastUpdate  -- Query from postgres instead
   AS SELECT MAX(foreignTimestamp) FROM RawHWInventory_History;


-- Raw inventory snapshots from foreign servers
CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.RawInventoryInsert;

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.RawInventoryDump;

CREATE PROCEDURE RawInventoryDelete   -- needed for testing
    AS DELETE FROM HW_Inventory_Location WHERE ID STARTS WITH ?;

CREATE PROCEDURE RawInventoryRowCount   -- needed for testing
   AS SELECT COUNT(*) FROM HW_Inventory_Location;


-- Processed node inventory history
CREATE PROCEDURE NodeHistoryInsert
   AS UPSERT INTO NodeInventory_History(Lctn, DbUpdatedTimestamp, InventoryTimestamp, InventoryInfo, Sernum)
        VALUES(?, CURRENT_TIMESTAMP, TO_TIMESTAMP(MILLISECOND, ?), ?, ?);

CREATE PROCEDURE NodeHistoryDump  -- needed for testing
   AS SELECT * FROM NodeInventory_History;

CREATE PROCEDURE NodeHistoryDelete   -- needed for testing
    AS DELETE FROM NodeInventory_History;

CREATE PROCEDURE NodeHistoryRowCount   -- needed for testing
   AS SELECT COUNT(*) FROM NodeInventory_History;

END_OF_BATCH
