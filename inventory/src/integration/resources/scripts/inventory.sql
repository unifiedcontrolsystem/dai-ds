-- All non-source columns contain data duplicated from the JSON blob.  This redundancy allows us to leverage the power
-- of a relational database.

file -inlinebatch END_OF_BATCH

CREATE TABLE Raw_DIMM
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    serial             VARCHAR(50) UNIQUE NOT NULL,
    mac                VARCHAR(50)        NOT NULL, -- foreign key into Raw_FRU_host
    locator            VARCHAR(50)        NOT NULL,
    source             VARCHAR(5000)      NOT NULL,
    doc_timestamp      BIGINT             NOT NULL, -- epoch seconds
    DbUpdatedTimestamp TIMESTAMP UNIQUE   NOT NULL,
    PRIMARY KEY (serial)
);

CREATE TABLE tier2_Raw_DIMM
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    serial             VARCHAR(50) UNIQUE NOT NULL,
    mac                VARCHAR(50)        NOT NULL, -- foreign key into Raw_FRU_host
    locator            VARCHAR(50)        NOT NULL,
    source             VARCHAR(5000)      NOT NULL,
    doc_timestamp      BIGINT             NOT NULL, -- epoch seconds
    DbUpdatedTimestamp TIMESTAMP UNIQUE   NOT NULL,
    EntryNumber        BigInt             NOT NULL,
    PRIMARY KEY (serial)
);

CREATE TABLE Raw_FRU_Host
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    boardSerial        VARCHAR(50),
    mac                VARCHAR(50) UNIQUE NOT NULL,
    source             VARCHAR(10000)     NOT NULL,
    doc_timestamp      BIGINT             NOT NULL, -- epoch seconds
    DbUpdatedTimestamp TIMESTAMP UNIQUE   NOT NULL,
    PRIMARY KEY (mac)
);

CREATE TABLE tier2_Raw_FRU_Host
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    boardSerial        VARCHAR(50),
    mac                VARCHAR(50) UNIQUE NOT NULL,
    source             VARCHAR(10000)     NOT NULL,
    doc_timestamp      BIGINT             NOT NULL, -- epoch seconds
    DbUpdatedTimestamp TIMESTAMP UNIQUE   NOT NULL,
    EntryNumber        BigInt             NOT NULL,
    PRIMARY KEY (mac)
);

CREATE TABLE Raw_Node_Inventory_History
(
    source             VARCHAR(70000)   NOT NULL,
    DbUpdatedTimestamp TIMESTAMP UNIQUE NOT NULL,
    PRIMARY KEY (DbUpdatedTimestamp)
);

CREATE PROCEDURE Raw_DIMM_Insert AS
    UPSERT INTO Raw_DIMM(id, serial, mac, locator, source, doc_timestamp, DbUpdatedTimestamp)
        VALUES(?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);

CREATE PROCEDURE Raw_FRU_Host_Insert AS
    UPSERT INTO Raw_FRU_Host(id, boardSerial, mac, source, doc_timestamp, DbUpdatedTimestamp)
        VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP);

-- The parameter is the mac address of the FRU host under consideration
CREATE PROCEDURE Get_Dimms_on_FRU_Host AS
SELECT *
FROM Raw_DIMM
WHERE mac = ?;

CREATE PROCEDURE Get_FRU_Hosts AS SELECT * FROM Raw_FRU_Host ORDER BY doc_timestamp;

CREATE PROCEDURE Raw_Node_Inventory_History_insert AS
    INSERT INTO Raw_Node_Inventory_History(source, DbUpdatedTimestamp)
        VALUES(?, CURRENT_TIMESTAMP);

END_OF_BATCH
