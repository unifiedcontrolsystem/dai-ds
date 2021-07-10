CREATE TABLE public.tier2_Raw_DIMM
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    serial             VARCHAR(50) UNIQUE NOT NULL,
    mac                VARCHAR(50)        NOT NULL, -- foreign key into Raw_FRU_host
    locator            VARCHAR(50)        NOT NULL,
    source             VARCHAR(5000)      NOT NULL,
    doc_timestamp      BIGINT,                      -- 1 second resolution
    DbUpdatedTimestamp TIMESTAMP          NOT NULL,
    EntryNumber        BigInt             NOT NULL,
    PRIMARY KEY (serial)
);

CREATE TABLE public.tier2_Raw_FRU_Host
(
    id                 VARCHAR(50), -- Elasticsearch doc id
    boardSerial        VARCHAR(50),
    mac                VARCHAR(50) UNIQUE NOT NULL,
    source             VARCHAR(10000)     NOT NULL,
    doc_timestamp      BIGINT,      -- 1 second resolution
    DbUpdatedTimestamp TIMESTAMP          NOT NULL,
    EntryNumber        BigInt             NOT NULL,
    PRIMARY KEY (mac)
);


CREATE SEQUENCE public.tier2_Raw_DIMM_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.tier2_Raw_FRU_Host_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tier2_Raw_DIMM_entrynumber_seq OWNED BY public.tier2_Raw_DIMM.entrynumber;
ALTER TABLE ONLY public.tier2_Raw_DIMM
    ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_Raw_DIMM_entrynumber_seq'::regclass);
SELECT pg_catalog.setval('public.tier2_Raw_DIMM_entrynumber_seq', 1, false);

ALTER SEQUENCE public.tier2_Raw_FRU_Host_entrynumber_seq OWNED BY public.tier2_Raw_FRU_Host.entrynumber;
ALTER TABLE ONLY public.tier2_Raw_FRU_Host
    ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_Raw_FRU_Host_entrynumber_seq'::regclass);
SELECT pg_catalog.setval('public.tier2_Raw_FRU_Host_entrynumber_seq', 1, false);


CREATE OR REPLACE FUNCTION public.insertorupdaterawdimmdata(
    p_id VarChar,
    p_serial VarChar,
    p_mac VarChar,
    p_locator VarChar,
    p_source VarChar,
    p_timestamp BIGINT,
    p_DbUpdatedTimestamp TIMESTAMP) RETURNS void
    LANGUAGE sql
AS
$$
insert into tier2_Raw_DIMM(id, serial, mac, locator, source, doc_timestamp, DbUpdatedTimestamp)
values (p_id, p_serial, p_mac, p_locator, p_source, p_timestamp, p_DbUpdatedTimestamp)
on conflict(serial) do update set id=p_id,
                                  mac=p_mac,
                                  locator=p_locator,
                                  source=p_source,
                                  doc_timestamp=p_timestamp,
                                  DbUpdatedTimestamp=p_DbUpdatedTimestamp
    ;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdaterawfruhostdata(
    p_id VarChar,
    p_boardSerial VarChar,
    p_mac VarChar,
    p_source VarChar,
    p_timestamp BIGINT,
    p_DbUpdatedTimestamp TIMESTAMP) RETURNS void
    LANGUAGE sql
AS
$$
insert into tier2_Raw_FRU_Host(id, boardSerial, mac, source, doc_timestamp, DbUpdatedTimestamp)
values (p_id, p_boardSerial, p_mac, p_source, p_timestamp, p_DbUpdatedTimestamp)
on conflict(mac) do update set id=p_id,
                               boardSerial=p_boardSerial,
                               source=p_source,
                               doc_timestamp=p_timestamp,
                               DbUpdatedTimestamp=p_DbUpdatedTimestamp
    ;
$$;
