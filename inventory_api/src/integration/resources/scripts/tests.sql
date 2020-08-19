SHOW tables;
SHOW classes;
-- SHOW procedures;

exec UpsertLocationIntoHWInv '1', '2', 3, '4', '5', '6', '7', '8';
exec UpsertLocationIntoHWInv 'n0', 'Type1', 1, '{}', 'fruId0', 'fruType1', 'fruSubType1', '{}';
exec UpsertLocationIntoHWInv 'n0p0', 'Type1', 1, 'fruId1', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p1', 'Type1', 1, 'fruId2', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p0g0', 'Type1', 1, 'fruId3', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p1g0', 'Type1', 1, 'fruId4', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n1', 'Type1', 1, 'fruId1', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n3', 'Type1', 1, 'fruId1', 'fruType1', 'fruSubType1';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;

exec RawInventoryDump '';
exec RawInventoryDump 'n0';
exec RawInventoryDump 'n1';
exec RawInventoryDump 'none';

exec RawInventoryDelete '1';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;


exec RawInventoryDelete 'n0';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;

exec RawInventoryDelete 'none';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;


exec RawInventoryDelete 'n1';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;


exec RawInventoryDelete 'n3';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;


exec RawInventoryDelete 'none';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;


exec RawInventoryDelete '';
exec RawInventoryDump '';
SELECT COUNT(ID) FROM HW_Inventory_Location;

-- SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
-- SELECT * FROM HW_Inventory_Location ORDER BY ID;
--
-- SELECT * FROM HW_Inventory_Location WHERE ID LIKE '%';
-- SELECT * FROM HW_Inventory_Location WHERE ID LIKE 'n0%';
--
-- SELECT * FROM HW_Inventory_Location FULL OUTER JOIN HW_Inventory_FRU ON
--     HW_Inventory_Location.FRUID=HW_Inventory_FRU.FRUID ORDER BY ID;
--
-- SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE
--     HW_Inventory_Location.FRUID=HW_Inventory_FRU.FRUID ORDER BY ID;
--
-- SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID;
--
-- SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE
--     I.FRUID = F.FRUID ORDER BY I.ID;
--
-- SELECT * FROM
--     (SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE HW_Inventory_Location.FRUID = HW_Inventory_FRU.FRUID)
--         AS HW_Inventory;
--
-- SELECT * FROM
--     (SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE HW_Inventory_Location.FRUID = HW_Inventory_FRU.FRUID)
--         AS HW_Inventory
--             WHERE HW_Inventory.ID = 'n0';
--
-- SELECT * FROM
--     (SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE HW_Inventory_Location.FRUID = HW_Inventory_FRU.FRUID)
--         AS HW_Inventory
--             WHERE HW_Inventory.ID = 'n0'
--                 ORDER BY HW_Inventory.ID;
--
-- SELECT * FROM
--     (SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID)
--         AS HW_Inventory
--             WHERE HW_Inventory.ID = 'n0'
--                 ORDER BY HW_Inventory.ID;
--
-- SELECT * FROM
--     (SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID)
--         AS HW_Inventory
--             WHERE HW_Inventory.ID STARTS WITH 'n0'
--                 ORDER BY HW_Inventory.ID;
--
-- SELECT * FROM HW_Inventory_Location WHERE ID STARTS WITH 'n0';

