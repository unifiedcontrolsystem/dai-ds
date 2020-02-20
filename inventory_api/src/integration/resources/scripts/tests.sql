SHOW tables;
SHOW classes;
-- SHOW procedures;

exec UpsertLocationIntoHWInv '1', '2', 3,  '4', '5', '6';
exec UpsertLocationIntoHWInv 'n0', 'Type1', 1, 'fruId0', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p0', 'Type1', 1, 'fruId1', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p1', 'Type1', 1, 'fruId2', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p0g0', 'Type1', 1, 'fruId3', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n0p1g0', 'Type1', 1, 'fruId4', 'fruType1', 'fruSubType1';

exec UpsertLocationIntoHWInv 'n1', 'Type1', 1, 'fruId1', 'fruType1', 'fruSubType1';
exec UpsertLocationIntoHWInv 'n3', 'Type1', 1, 'fruId1', 'fruType1', 'fruSubType1';

SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

SELECT * FROM HW_Inventory_Location WHERE ID LIKE '%';
SELECT * FROM HW_Inventory_Location WHERE ID LIKE 'n0%';

SELECT * FROM HW_Inventory_Location FULL OUTER JOIN HW_Inventory_FRU ON
    HW_Inventory_Location.FRUID=HW_Inventory_FRU.FRUID ORDER BY ID;

SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE
    HW_Inventory_Location.FRUID=HW_Inventory_FRU.FRUID ORDER BY ID;

SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID;

SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE
    I.FRUID = F.FRUID ORDER BY I.ID;

SELECT * FROM
    (SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE HW_Inventory_Location.FRUID = HW_Inventory_FRU.FRUID)
        AS HW_Inventory;

SELECT * FROM
    (SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE HW_Inventory_Location.FRUID = HW_Inventory_FRU.FRUID)
        AS HW_Inventory
            WHERE HW_Inventory.ID = 'n0';

SELECT * FROM
    (SELECT * FROM HW_Inventory_Location, HW_Inventory_FRU WHERE HW_Inventory_Location.FRUID = HW_Inventory_FRU.FRUID)
        AS HW_Inventory
            WHERE HW_Inventory.ID = 'n0'
                ORDER BY HW_Inventory.ID;

SELECT * FROM
    (SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID)
        AS HW_Inventory
            WHERE HW_Inventory.ID = 'n0'
                ORDER BY HW_Inventory.ID;

SELECT * FROM
    (SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID)
        AS HW_Inventory
            WHERE HW_Inventory.ID STARTS WITH 'n0'
                ORDER BY HW_Inventory.ID;

SELECT * FROM HW_Inventory_Location WHERE ID STARTS WITH 'n0';

SELECT COUNT(ID) FROM HW_Inventory_Location;

exec AllLocationsAtIdFromHWInv '';
exec AllLocationsAtIdFromHWInv 'n0';
exec AllLocationsAtIdFromHWInv 'n1';
exec AllLocationsAtIdFromHWInv 'none';

exec DeleteAllLocationsAtIdFromHWInv '1';
SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

exec DeleteAllLocationsAtIdFromHWInv 'n0';
SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

exec DeleteAllLocationsAtIdFromHWInv 'none';
SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

exec DeleteAllLocationsAtIdFromHWInv 'n1';
SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

exec DeleteAllLocationsAtIdFromHWInv 'n3';
SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

exec DeleteAllLocationsAtIdFromHWInv 'none';
SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;

exec DeleteAllLocationsAtIdFromHWInv '';

SELECT * FROM HW_Inventory_FRU ORDER BY FRUID;
SELECT * FROM HW_Inventory_Location ORDER BY ID;
SELECT COUNT(ID) FROM HW_Inventory_Location;
