file -inlinebatch END_OF_BATCH

CREATE TABLE NodeInventory_History (
   Lctn                 VarChar(25)       NOT NULL,
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   InventoryTimestamp   TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this inventory being changed.
                                                      -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                      --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   InventoryInfo        VarChar(65536),               -- Additional inventory details not part of the standard manifest, e.g. part numbers, CPU details (CPU ID, speed, sockets, hyper threads), memory module details (type, size, speed)
   Sernum               VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   PRIMARY KEY (Lctn, InventoryTimestamp)
);
PARTITION TABLE NodeInventory_History ON COLUMN Lctn;

CREATE INDEX NodeInventoryHistoryByDbUpdatedTimestamp ON NodeInventory_History(DbUpdatedTimestamp);

-- New Sernum index added for performance study
CREATE INDEX NodeInventoryHistoryBySernum ON NodeInventory_History(Sernum);

-- New field indices added for performance study
CREATE INDEX NodeInventoryHistoryByDimm1 ON NodeInventory_History(field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM1/fru_id'), 'value'));
-- CREATE INDEX NodeInventoryHistoryByDimm3 ON NodeInventory_History(field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM3/fru_id'), 'value'));

-- Index for a field that is current nonexistent
-- CREATE INDEX NodeInventoryHistoryByGpu0 ON NodeInventory_History(field(field(field(InventoryInfo,'HWInfo'),'fru/GPU3/fru_id'), 'value'));

-- List of node locations that hosted Node.0
-- CREATE PROCEDURE v_NodeMigration AS
--     SELECT Lctn AS "Node Location" FROM NodeInventory_History
--     WHERE Sernum='Node.0.334ced60-d1e7-11ea-b50f-801f0258d541'
--     ORDER BY Lctn ASC;
--
-- -- List of DIMM_0 locations which hosted Memory.3
-- --   Note that we need to search in all the DIMM slots to determine the migration of a DIMM to get the full
-- --   Memory.3 migration history
-- CREATE PROCEDURE v_DimmMigration0 AS
--     SELECT Lctn AS "Node Location", Sernum AS "Sequence Number" FROM NodeInventory_History
--     WHERE field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM0/fru_id'), 'value')='Memory.0.1ed2b836-d23e-11ea-b51d-801f0258d541'
--     ORDER BY Lctn ASC;
--
-- -- List of DIMM_1 locations which hosted Memory.3
-- CREATE PROCEDURE v_DimmMigration1 AS
--     SELECT Lctn AS "Node Location", Sernum AS "Sequence Number" FROM NodeInventory_History
--     WHERE field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM1/fru_id'), 'value')='Memory.0.1ed2b836-d23e-11ea-b51d-801f0258d541'
--     ORDER BY Lctn ASC;
--
-- -- -- List of DIMM_2 locations which hosted Memory.2
-- -- CREATE PROCEDURE DimmMigration2 AS
-- --     SELECT Lctn AS "Node Location", Sernum AS "Sequence Number" FROM NodeInventory_History
-- --     WHERE field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM2/fru_id'), 'value') LIKE 'Memory.2.%'
-- --     ORDER BY Lctn ASC;
-- --
-- -- -- List of DIMM_3 locations which hosted Memory.3
-- -- CREATE PROCEDURE DimmMigration3 AS
-- --     SELECT Lctn AS "Node Location", Sernum AS "Sequence Number" FROM NodeInventory_History
-- --     WHERE field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM3/fru_id'), 'value') LIKE 'Memory.3.%'
-- --     ORDER BY Lctn ASC;
--
-- -- History of the node location R0-CB0-CN3
-- CREATE PROCEDURE HistoryAtNodeLocation AS
--     SELECT Lctn AS "Node Location",
--            InventoryTimestamp AS "Work Order Time",
--            field(field(field(InventoryInfo,'HWInfo'),'fru/NODE/fru_id'), 'value') AS "Node fru id"
--     FROM NodeInventory_History
--     WHERE Lctn='R0-CB0-CN3'
--     ORDER BY InventoryTimestamp ASC;
--
-- -- History of the DIMM location R0-CB0-CN3-DIMM3
-- CREATE PROCEDURE HistoryAtComponentLocation AS
--     SELECT Lctn AS "Node Location Contain DIMM_3",
--            InventoryTimestamp AS "Work Order Time",
--            field(field(field(InventoryInfo,'HWInfo'),'fru/DIMM3/fru_id'), 'value') AS "Component fru id"
--     FROM NodeInventory_History
--     WHERE Lctn='R0-CB0-CN3'
--     ORDER BY InventoryTimestamp ASC;
--
-- -- Inventory snapshot of blade R0-CB0
-- CREATE PROCEDURE CurrentInventorySnapshot AS
--     SELECT t.InventoryTimestamp AS "Last Maintained",
--            t.Lctn AS "Node Location",
--            t.Sernum AS "Serial Number"
--     FROM NodeInventory_History AS t
--     INNER JOIN
--     (SELECT Lctn, MAX(InventoryTimestamp) AS MaxInventoryTimestamp
--     FROM NodeInventory_History
--     GROUP BY Lctn) AS groupedt
--     ON t.Lctn = groupedt.Lctn AND t.InventoryTimestamp = groupedt.MaxInventoryTimestamp
--     WHERE t.Lctn LIKE 'R0-CB0-%'
--     ORDER BY t.Lctn ASC;

END_OF_BATCH
