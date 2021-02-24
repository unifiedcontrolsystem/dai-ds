--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.11
-- Dumped by pg_dump version 9.6.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

--CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

--COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: inventorytype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.inventorytype AS (
	lctn character varying(30),
	lastchgtimestamp timestamp without time zone
);


--
-- Name: jobactivetype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.jobactivetype AS (
	jobid character varying(30),
	jobname character varying(100),
	state character varying(1),
	bsn character varying(50),
	username character varying(30),
	starttimestamp timestamp without time zone,
	numnodes bigint,
	nodes character varying(4000),
	wlmjobstate character varying(50)
);


--
-- Name: jobnonactivetype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.jobnonactivetype AS (
	jobid character varying(30),
	jobname character varying(100),
	state character varying(1),
	bsn character varying(50),
	username character varying(30),
	starttimestamp timestamp without time zone,
	endtimestamp timestamp without time zone,
	exitstatus bigint,
	numnodes bigint,
	nodes character varying(4000),
	jobacctinfo character varying(120),
	wlmjobstate character varying(50)
);


--
-- Name: raseventtype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.raseventtype AS (
    descriptivename character varying(65),
	lastchgtimestamp timestamp without time zone,
	dbupdatedtimestamp timestamp without time zone,
	severity character varying(10),
	lctn character varying(100),
	jobid character varying(30),
	controloperation character varying(50),
	msg character varying(1000),
	instancedata character varying(10000)
);

--
-- Name: inventorydatatype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.inventorydatatype AS (
    lctn character varying(30),
    sequencenumber integer,
    state character varying(1),
    hostname character varying(63),
    bootimageid character varying(50),
    environment character varying(240),
    ipaddr character varying(25),
    macaddr character varying(17),
    bmcipaddr character varying(25),
    bmcmacaddr character varying(17),
    bmchostname character varying(63),
    dbupdatedtimestamp timestamp without time zone,
    lastchgtimestamp timestamp without time zone,
    lastchgadaptertype character varying(20),
    lastchgworkitemid bigint,
    owner character varying(1),
    aggregator character varying(63),
    inventorytimestamp timestamp without time zone,
    wlmnodestate character varying(1),
    ConstraintId character varying(50),
    entrynumber bigint,
    ProofOfLifeTimestamp timestamp without time zone
);


--
-- Name: raseventtypewithdescriptivename; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.raseventtypewithdescriptivename AS (
	type character varying(65),
	lastchgtimestamp timestamp without time zone,
	dbupdatedtimestamp timestamp without time zone,
	severity character varying(10),
	lctn character varying(100),
	jobid character varying(30),
	controloperation character varying(50),
	msg character varying(1000),
	instancedata character varying(10000)
);


--
-- Name: raseventtypewithoutmetadata; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.raseventtypewithoutmetadata AS (
	descriptivename character varying(65),
	lastchgtimestamp timestamp without time zone,
	dbupdatedtimestamp timestamp without time zone,
	lctn character varying(100),
	jobid character varying(30),
	controloperation character varying(50)
);


--
-- Name: reservationtype; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.reservationtype AS (
	reservationname character varying(35),
	users character varying(100),
	nodes character varying(1000),
	starttimestamp timestamp without time zone,
	endtimestamp timestamp without time zone,
	deletedtimestamp timestamp without time zone,
	lastchgtimestamp timestamp without time zone
);

CREATE TYPE public.alert_kind AS ENUM ('DEFAULT', 'CUSTOM');

CREATE TYPE public.alert_state AS ENUM ('OPEN', 'CLOSED');

CREATE TYPE public.alert_full AS (
    id BIGINT,
    alert_type varchar(100),
    description varchar(250),
    kind public.alert_kind,
    created timestamp without time zone,
    closed timestamp without time zone,
    locations text,
    state public.alert_state
);

CREATE TYPE public.alert_rasevent AS (
    id BIGINT,
    name character varying(65),
    lctn character varying(100),
    data character varying(10000),
    jobid character varying(65),
    timestamp timestamp without time zone
);

CREATE TYPE public.cnos_container AS ENUM ('rich', 'lean');

CREATE TYPE public.system_summary_count AS (
    state character varying(1),
    count bigint
);



SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: tier2_aggregatedenvdata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_aggregatedenvdata (
    lctn character varying(100) NOT NULL,
    "timestamp" timestamp without time zone NOT NULL,
    type text NOT NULL,
    maximumvalue double precision NOT NULL,
    minimumvalue double precision NOT NULL,
    averagevalue double precision NOT NULL,
    adaptertype character varying(20) NOT NULL,
    workitemid bigint NOT NULL,
    entrynumber bigint NOT NULL
)PARTITION BY RANGE ("timestamp");

--
-- Name: tier2_computenode_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_computenode_history (
    lctn character varying(25) NOT NULL,
    sequencenumber integer NOT NULL,
    state character varying(1) NOT NULL,
    hostname character varying(63),
    bootimageid character varying(50),
    environment character varying(240),
    ipaddr character varying(25),
    macaddr character varying(17),
    bmcipaddr character varying(25),
    bmcmacaddr character varying(17),
    bmchostname character varying(63),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    owner character varying(1) NOT NULL,
    aggregator character varying(63) NOT NULL,
    inventorytimestamp timestamp without time zone,
    wlmnodestate character varying(1) NOT NULL,
    ConstraintId character varying(50),
    entrynumber bigint NOT NULL,
    ProofOfLifeTimestamp timestamp without time zone
);


--
-- Name: tier2_diag_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_diag_history (
    diagid bigint NOT NULL,
    lctn character varying(20000) NOT NULL,
    serviceoperationid bigint,
    diag character varying(500) NOT NULL,
    diagparameters character varying(20000),
    state character varying(1) NOT NULL,
    starttimestamp timestamp without time zone NOT NULL,
    endtimestamp timestamp without time zone,
    results character varying(262144),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL
);

--
-- Name: tier2_diag_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_diagresults
(
    diagid bigint NOT NULL,
    lctn character varying(50) NOT NULL,
    state character varying(1) NOT NULL,
    results character varying(262144),
    dbupdatedtimestamp timestamp without time zone NOT NULL
);

--
-- Name: tier2_diag_list; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_diag_list (
    diaglistid character varying(40) NOT NULL,
    diagtoolid character varying(40) NOT NULL,
    description character varying(240) NOT NULL,
    defaultparameters character varying(240),
    dbupdatedtimestamp timestamp without time zone NOT NULL
);


--
-- Name: tier2_adapter_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_adapter_history (
    id bigint NOT NULL,
    adaptertype character varying(20) NOT NULL,
    sconrank bigint NOT NULL,
    state character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    lctn character varying(25) NOT NULL,
    pid bigint NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_chassis_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_chassis_history (
    lctn character varying(12) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(50),
    type character varying(20),
    vpd character varying(4096),
    owner character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL
);



--
-- Name: tier2_bootimage_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_bootimage_history (
    id character varying(50) NOT NULL,
    description character varying(200),
    bootimagefile character varying(100) NOT NULL,
    bootimagechecksum character varying(32) NOT NULL,
    bootoptions character varying(80),
    bootstrapimagefile character varying(100) NOT NULL,
    bootstrapimagechecksum character varying(32) NOT NULL,
    state character varying(1),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    kernelargs character varying(800),
    files character varying(300),
    entrynumber bigint NOT NULL
);

--
-- Name: tier2_job_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_job_history (
    jobid character varying(30) NOT NULL,
    jobname character varying(100),
    state character varying(1) NOT NULL,
    bsn character varying(50),
    numnodes bigint DEFAULT 0 NOT NULL,
    nodes bytea,
    powercap bigint DEFAULT 0 NOT NULL,
    username character varying(30),
    executable character varying(500),
    initialworkingdir character varying(500),
    arguments character varying(4096),
    environmentvars character varying(8192),
    starttimestamp timestamp without time zone NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    endtimestamp timestamp without time zone,
    exitstatus bigint,
    jobacctinfo character varying(120),
    powerused bigint,
    wlmjobstate character varying(50),
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_jobstep_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_jobstep_history (
    jobid character varying(30) NOT NULL,
    jobstepid character varying(35) NOT NULL,
    state character varying(1) NOT NULL,
    numnodes bigint,
    nodes bytea,
    numprocessespernode bigint,
    executable character varying(500),
    initialworkingdir character varying(500),
    arguments character varying(4096),
    environmentvars character varying(8192),
    mpimapping character varying(100),
    starttimestamp timestamp without time zone NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    endtimestamp timestamp without time zone,
    exitstatus bigint,
    wlmjobstepstate character varying(50),
    entrynumber bigint NOT NULL
);

--
-- Name: tier2_machine_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_machine_history (
    sernum character varying(50) NOT NULL,
    description character varying(80),
    type character varying(20),
    numrows integer,
    numcolsinrow integer,
    numchassisinrack integer,
    state character varying(1) NOT NULL,
    clockfreq integer,
    manifestlctn character varying(128) NOT NULL,
    manifestcontent character varying(75000) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    usingsynthesizeddata character varying(1) NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_machineadapterinstance_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_machineadapterinstance_history (
    snlctn character varying(63) NOT NULL,
    adaptertype character varying(20) NOT NULL,
    numinitialinstances bigint NOT NULL,
    numstartedinstances bigint NOT NULL,
    invocation character varying(400) NOT NULL,
    logfile character varying(100) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL
);

--
-- Name: tier2_rack_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_rack_history (
    lctn character varying(5) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(50),
    type character varying(20),
    vpd character varying(4096),
    owner character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL
);

--
-- Name: tier2_servicenode_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_servicenode_history (
    lctn character varying(30) NOT NULL,
    sequencenumber integer NOT NULL,
    hostname character varying(63),
    state character varying(1) NOT NULL,
    bootimageid character varying(50),
    ipaddr character varying(25),
    macaddr character varying(17),
    bmcipaddr character varying(25),
    bmcmacaddr character varying(17),
    bmchostname character varying(63),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    owner character varying(1) NOT NULL,
    aggregator character varying(63) NOT NULL,
    inventorytimestamp timestamp without time zone,
    ConstraintId character varying(50),
    entrynumber bigint NOT NULL,
    ProofOfLifeTimestamp timestamp without time zone
);

--
-- Name: tier2_serviceoperation_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_serviceoperation_history (
    serviceoperationid bigint NOT NULL,
    lctn character varying(32) NOT NULL,
    typeofserviceoperation character varying(32) NOT NULL,
    userstartedservice character varying(32) NOT NULL,
    userstoppedservice character varying(32),
    state character varying(1) NOT NULL,
    status character varying(1) NOT NULL,
    starttimestamp timestamp without time zone NOT NULL,
    stoptimestamp timestamp without time zone,
    startremarks character varying(256),
    stopremarks character varying(256),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    logfile character varying(256) NOT NULL,
    entrynumber bigint NOT NULL
);

--
-- Name: tier2_switch_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_switch_history (
    lctn character varying(25) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(50),
    type character varying(20),
    owner character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_workitem_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_workitem_history (
    queue character varying(20),
    workingadaptertype character varying(20) NOT NULL,
    id bigint NOT NULL,
    worktobedone character varying(40) NOT NULL,
    parameters character varying(15000),
    notifywhenfinished character varying(1) NOT NULL,
    state character varying(1) NOT NULL,
    requestingworkitemid bigint NOT NULL,
    requestingadaptertype character varying(20) NOT NULL,
    workingadapterid bigint,
    workingresults character varying(15000),
    results character varying(262144),
    starttimestamp timestamp without time zone NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    endtimestamp timestamp without time zone,
    rowinsertedintohistory character varying(1) NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_rasmetadata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_rasmetadata (
    descriptivename character varying(65) PRIMARY KEY,
    severity character varying(10) NOT NULL,
    category character varying(20) NOT NULL,
    component character varying(50) NOT NULL,
    controloperation character varying(50),
    msg character varying(1000),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL,
    generatealert character varying(1) NOT NULL DEFAULT 'N'
);

--
-- Name: tier2_ucsconfigvalue; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_ucsconfigvalue (
    key character varying(50) NOT NULL,
    value character varying(100) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL
);


--
-- Name: tier2_wlmreservation_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_wlmreservation_history (
    reservationname character varying(35) NOT NULL,
    users character varying(100) NOT NULL,
    nodes character varying(1000),
    starttimestamp timestamp without time zone NOT NULL,
    endtimestamp timestamp without time zone,
    deletedtimestamp timestamp without time zone,
    lastchgtimestamp timestamp without time zone NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_uniquevalues; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_uniquevalues (
    entity character varying(100) NOT NULL,
    nextvalue bigint NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL
);


--
-- Name: tier2_replacement_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_replacement_history (
    lctn character varying(100) NOT NULL,
    frutype character varying(30) NOT NULL,
    serviceoperationid bigint,
    oldsernum character varying(20),
    newsernum character varying(20) NOT NULL,
    oldstate character varying(1) NOT NULL,
    newstate character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_inventorysnapshot; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_inventorysnapshot (
    lctn character varying(30) NOT NULL,
    snapshottimestamp timestamp without time zone NOT NULL,
    inventoryinfo character varying(16384) NOT NULL,
    id SERIAL,
    reference boolean DEFAULT false NOT NULL
);

--
-- Name: tier2_alert; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_alert (
    id BIGSERIAL NOT NULL,
    alert_type varchar(100) NOT NULL,
    description varchar(250),
    kind public.alert_kind NOT NULL,
    created timestamp without time zone DEFAULT NOW(),
    locations text,
    PRIMARY KEY(id)
);

CREATE TABLE public.tier2_alert_history (
    id bigint NOT NULL,
    alert_type varchar(100) NOT NULL,
    description varchar(250),
    kind public.alert_kind NOT NULL,
    created timestamp without time zone NOT NULL,
    closed timestamp without time zone DEFAULT NOW(),
    locations text,
    PRIMARY KEY(id)
);

CREATE TABLE public.tier2_alert_rasevent (
    internalid BIGSERIAL NOT NULL,
    id BIGINT NOT NULL,
    descriptivename character varying(65) NOT NULL,
    lctn character varying(100),
    jobid character varying(30),
    instancedata character varying(10000),
    lastchgtimestamp timestamp without time zone NOT NULL,
    PRIMARY KEY(internalid)
);

CREATE TABLE public.tier2_alert_has_ras (
    alertid bigint NOT NULL,
    eventid bigint NOT NULL,
    PRIMARY KEY(alertid, eventid)
);


CREATE TABLE public.tier2_cnos_config (
    name varchar(100) NOT NULL,
    container public.cnos_container NOT NULL,
    partition text,
    PRIMARY KEY(name)
);


--
-- Name: tier2_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_config (
    key character varying(50) NOT NULL,
    value character varying(50) NOT NULL,
    description character varying(500)
);


--
-- Name: tier2_fabrictopology_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_fabrictopology_history (
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_job_power; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_job_power (
    jobid character varying(30) NOT NULL,
    lctn character varying(100) NOT NULL,
    jobpowertimestamp timestamp without time zone DEFAULT now() NOT NULL,
    profile character varying(50) NOT NULL,
    totalruntime double precision NOT NULL,
    totalpackageenergy double precision NOT NULL,
    totaldramenergy double precision NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_lustre_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_lustre_history (
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    entrynumber bigint NOT NULL
);


--
-- Name: tier2_rasevent; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_rasevent (
    id bigint NOT NULL,
    descriptivename character varying(65) NOT NULL,
    lctn character varying(100),
    sernum character varying(50),
    jobid character varying(30),
    numberrepeats integer DEFAULT 0 NOT NULL,
    controloperation character varying(50),
    done character varying(1) NOT NULL,
    instancedata character varying(10000),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    entrynumber bigint NOT NULL
)PARTITION BY RANGE (dbupdatedtimestamp);

--
-- Name: tier2_adapter_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_adapter_ss (
    id bigint,
    adaptertype character varying(20),
    sconrank bigint,
    state character varying(1),
    dbupdatedtimestamp timestamp without time zone,
    lastchgadaptertype character varying(20),
    lastchgworkitemid bigint,
    lctn character varying(25),
    pid bigint
);

--
-- Name: tier2_bootimage_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_bootimage_ss (
    id character varying(50) NOT NULL,
    description character varying(200),
    bootimagefile character varying(100),
    bootimagechecksum character varying(32),
    bootoptions character varying(80),
    bootstrapimagefile character varying(100),
    bootstrapimagechecksum character varying(32),
    state character varying(1),
    dbupdatedtimestamp timestamp without time zone,
    lastchgtimestamp timestamp without time zone,
    lastchgadaptertype character varying(20),
    lastchgworkitemid bigint,
    kernelargs character varying(800),
    files character varying(300),
    entrynumber bigint
);

--
-- Name: tier2_chassis_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_chassis_ss (
    lctn character varying(12) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(50),
    type character varying(20),
    vpd character varying(4096),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    owner character varying(1) NOT NULL
);


--
-- Name: tier2_computenode_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_computenode_ss (
    lctn character varying(25) NOT NULL,
    sequencenumber integer NOT NULL,
    state character varying(1) NOT NULL,
    hostname character varying(63),
    bootimageid character varying(50),
    environment character varying(240),
    ipaddr character varying(25),
    macaddr character varying(17),
    bmcipaddr character varying(25),
    bmcmacaddr character varying(17),
    bmchostname character varying(63),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    owner character varying(1) NOT NULL,
    aggregator character varying(63) NOT NULL,
    wlmnodestate character varying(1) NOT NULL,
    inventorytimestamp timestamp without time zone,
    ConstraintId character varying(50),
    ProofOfLifeTimestamp timestamp without time zone
);


--
-- Name: tier2_machine_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_machine_ss (
    sernum character varying(50) NOT NULL,
    description character varying(80),
    type character varying(20),
    numrows bigint,
    numcolsinrow bigint,
    numchassisinrack bigint,
    state character varying(1) NOT NULL,
    clockfreq bigint,
    manifestlctn character varying(128) NOT NULL,
    manifestcontent character varying(75000) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    usingsynthesizeddata character varying(1) DEFAULT 'N'::character varying NOT NULL
);

--
-- Name: tier2_machineadapterinstance_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_machineadapterinstance_ss (
    snlctn character varying(63),
    adaptertype character varying(20),
    numinitialinstances bigint,
    numstartedinstances bigint,
    invocation character varying(400),
    logfile character varying(100),
    dbupdatedtimestamp timestamp without time zone
);

--
-- Name: tier2_servicenode_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_servicenode_ss (
    lctn character varying(30) NOT NULL,
    sequencenumber integer NOT NULL,
    hostname character varying(63),
    state character varying(1) NOT NULL,
    bootimageid character varying(50),
    ipaddr character varying(25),
    macaddr character varying(17),
    bmcipaddr character varying(25),
    bmcmacaddr character varying(17),
    bmchostname character varying(63),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    owner character varying(1) NOT NULL,
    aggregator character varying(63) NOT NULL,
    inventorytimestamp timestamp without time zone,
    ConstraintId character varying(50),
    ProofOfLifeTimestamp timestamp without time zone
);

--
-- Name: tier2_workitem_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_workitem_ss (
    queue character varying(20),
    workingadaptertype character varying(20) NOT NULL,
    id bigint NOT NULL,
    worktobedone character varying(40) NOT NULL,
    parameters character varying(15000),
    notifywhenfinished character varying(1) NOT NULL,
    state character varying(1) NOT NULL,
    requestingworkitemid bigint NOT NULL,
    requestingadaptertype character varying(20) NOT NULL,
    workingadapterid bigint,
    workingresults character varying(15000),
    results character varying(262144),
    starttimestamp timestamp without time zone NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    endtimestamp timestamp without time zone,
    rowinsertedintohistory character varying(1) NOT NULL
);

--
-- Name: tier2_rasmetadata_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_rasmetadata_ss (
    descriptivename character varying(65) NOT NULL,
    severity character varying(10),
    category character varying(20),
    component character varying(50),
    controloperation character varying(50),
    msg character varying(1000),
    dbupdatedtimestamp timestamp without time zone,
    entrynumber bigint,
    generatealert character varying(1)
);


--
-- Name: tier2_diag_tools; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_diag_tools (
    diagtoolid character varying(40) NOT NULL,
    description character varying(240) NOT NULL,
    unittype character varying(25) NOT NULL,
    unitsize integer NOT NULL,
    provisionreqd character varying(1) NOT NULL,
    rebootbeforereqd character varying(1) NOT NULL,
    rebootafterreqd character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL
);

--
-- Name: tier2_diag_tools_ss; Type: TABLE; Schema: public; Owner:
--

CREATE TABLE public.tier2_diag_tools_ss (
    diagtoolid character varying(40) NOT NULL,
    description character varying(240),
    unittype character varying(25),
    unitsize integer,
    provisionreqd character varying(1),
    rebootbeforereqd character varying(1),
    rebootafterreqd character varying(1),
    dbupdatedtimestamp timestamp without time zone
);

CREATE TABLE public.tier2_rack_ss (
    lctn character varying(5) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(50),
    type character varying(20),
    vpd character varying(4096),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    owner character varying(1) NOT NULL
);

CREATE TABLE public.tier2_ucsconfigvalue_ss (
    key character varying(50) NOT NULL,
    value character varying(100),
    dbupdatedtimestamp timestamp without time zone
);

CREATE TABLE public.tier2_uniquevalues_ss (
    entity character varying(100) NOT NULL,
    nextvalue bigint,
    dbupdatedtimestamp timestamp without time zone
);

CREATE TABLE public.tier2_rasevent_ss (
    id bigint NOT NULL,
    descriptivename character varying(65) NOT NULL,
    lctn character varying(100),
    sernum character varying(50),
    jobid character varying(30),
    numberrepeats integer DEFAULT 0 NOT NULL,
    controloperation character varying(50),
    done character varying(1) NOT NULL,
    instancedata character varying(10000),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    entrynumber bigint NOT NULL
);

CREATE TABLE public.tier2_nonnodehw_history (
   Lctn                 VarChar(50)       NOT NULL,
   SequenceNumber       Integer           NOT NULL,
   Type                 VarChar(30)       NOT NULL,
   State                VarChar(1)        NOT NULL,
   HostName             VarChar(63),
   IpAddr               VarChar(25),
   MacAddr              VarChar(17),
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,
   LastChgTimestamp     TIMESTAMP         NOT NULL,
   LastChgAdapterType   VarChar(20)       NOT NULL,
   LastChgWorkItemId    BigInt            NOT NULL,
   Owner                VarChar(1)        NOT NULL,
   Aggregator           VarChar(63)       NOT NULL,
   InventoryTimestamp   TIMESTAMP,
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,
   EntryNumber             BigInt            NOT NULL
);
CREATE INDEX nonnodehwhistorybybbupdatedtimestamp ON public.tier2_nonnodehw_history(dbupdatedtimestamp);


CREATE TABLE public.tier2_nodeinventory_history (
   Lctn                    VarChar(25)       NOT NULL,
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,
   InventoryTimestamp      TIMESTAMP         NOT NULL,
   InventoryInfo           VarChar(16384),
   Sernum                  VarChar(50),
   BiosInfo             VarChar(30000),
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,
   EntryNumber             BigInt            NOT NULL,
   PRIMARY KEY (Lctn, InventoryTimestamp)
);

--
-- Name: tier2_UserActionsOnApi
--
CREATE TABLE PUBLIC.tier2_UserActionsOnApi (
    id SERIAL,
    userid character varying NOT NULL,
    parameters character varying NOT NULL,
    apicall character varying NOT NULL,
    apicalltimestamp timestamp without time zone NOT NULL
);

--
-- Name: tier2_authorized_user
--
CREATE TABLE PUBLIC.tier2_authorized_user (
    id SERIAL,
    userid character varying NOT NULL,
    roleid character varying NOT NULL
);


CREATE TABLE public.tier2_RawHWInventory_History (
    Action VARCHAR(16) NOT NULL,                -- Added/Removed
    ID VARCHAR(64) NOT NULL,                    -- perhaps xname (path); as is from JSON
    FRUID VARCHAR(80) NOT NULL,                 -- perhaps <manufacturer>-<serial#>
    ForeignTimestamp VARCHAR(32) NOT NULL,      -- Foreign server timestamp string in RFC-3339 format
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    EntryNumber bigint NOT NULL,
    PRIMARY KEY (Action, ID, ForeignTimestamp)  -- allows the use of upsert to eliminate duplicates
);

CREATE TABLE public.tier2_NonNodeHwInventory_History (
   Lctn                    VarChar(50)       NOT NULL,
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,
   InventoryTimestamp      TIMESTAMP         NOT NULL,
   InventoryInfo           VarChar(16384),
   Sernum                  VarChar(50),
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,
   EntryNumber             BigInt            NOT NULL,
   PRIMARY KEY (Lctn, InventoryTimestamp)
);

CREATE TABLE public.tier2_nonnodehw_ss (
   Lctn                 VarChar(50)       NOT NULL,
   SequenceNumber       Integer           NOT NULL,
   Type                 VarChar(30)       NOT NULL,
   State                VarChar(1)        NOT NULL,
   HostName             VarChar(63),
   IpAddr               VarChar(25),
   MacAddr              VarChar(17),
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,
   LastChgTimestamp     TIMESTAMP         NOT NULL,
   LastChgAdapterType   VarChar(20)       NOT NULL,
   LastChgWorkItemId    BigInt            NOT NULL,
   Owner                VarChar(1)        NOT NULL,
   Aggregator           VarChar(63)       NOT NULL,
   InventoryTimestamp   TIMESTAMP,
   PRIMARY KEY (Lctn)
);

CREATE TABLE public.Tier2_Constraint (
   ConstraintId         VarChar(50)    NOT NULL,   -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   Constraints          VarChar(1000),             -- Constraints that need to be enforced for entities with this constraint id.
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (ConstraintId)
);

CREATE TABLE public.tier2_switch_ss (
   lctn character varying(25) NOT NULL,
   state character varying(1) NOT NULL,
   sernum character varying(50),
   type character varying(20),
   owner character varying(1) NOT NULL,
   dbupdatedtimestamp timestamp without time zone NOT NULL,
   lastchgtimestamp timestamp without time zone NOT NULL,
   entrynumber bigint NOT NULL,
   PRIMARY KEY (lctn)
);

CREATE TABLE public.Tier2_Processor_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-P6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   SocketDesignation       VarChar(25)       NOT NULL,         -- E.g., CPU0, CPU1
   DbUpdatedTimestamp      timestamp without time zone NOT NULL,     -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        timestamp without time zone NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Processor is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this Processor, rather the service operation is done on the node!
   EntryNumber             BigInt            NOT NULL         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);

CREATE TABLE public.Tier2_Processor_ss (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-P6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   SocketDesignation       VarChar(25)       NOT NULL,         -- E.g., CPU0, CPU1
   DbUpdatedTimestamp      timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Processor is actually recorded in the node's inventory info.
   PRIMARY KEY(NodeLctn, Lctn)
            -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);

CREATE TABLE public.tier2_Accelerator_ss (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-A6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not populated, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   BusAddr              VarChar(12),         -- E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                 VarChar(10)       NOT NULL,                        -- Slot that this device is in.
   DbUpdatedTimestamp   timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   PRIMARY KEY (NodeLctn, Lctn)
);

CREATE TABLE public.Tier2_Accelerator_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-A6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   BusAddr                 VarChar(12),         -- E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                    VarChar(10)       NOT NULL,                        -- Slot that this device is in.
   DbUpdatedTimestamp      timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Accelerator is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this Accelerator, rather the service operation is done on the node!
   EntryNumber             BigInt            NOT NULL         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);

CREATE TABLE public.tier2_Hfi_ss (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-H6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not populated, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   BusAddr              VarChar(12) ,         -- E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                 VarChar(10)       NOT NULL,                        -- Slot that this device is in.
   DbUpdatedTimestamp   timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Hfi is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Hfi, rather the service operation is done on the node!
   PRIMARY KEY (NodeLctn, Lctn)
);

CREATE TABLE public.Tier2_Hfi_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-H6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   BusAddr                 VarChar(12),         -- E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                    VarChar(10)       NOT NULL,                        -- Slot that this device is in.
   DbUpdatedTimestamp      timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   timestamp without time zone,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Hfi is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this Hfi, rather the service operation is done on the node!
   EntryNumber             BigInt            NOT NULL         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);

CREATE TABLE public.tier2_Dimm_ss (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-D8
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not installed, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   SizeMB               BigInt            NOT NULL,         -- Size of this memory module in megabyte units
   ModuleLocator        VarChar(25)       NOT NULL,         -- E.g., CPU1_DIMM_A2, CPU2_DIMM_F1
   BankLocator          VarChar(25),                        -- E.g., Node 1, Node 2
   DbUpdatedTimestamp   timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   timestamp without time zone,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this dimm is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this dimm, rather the service operation is done on the node!
   PRIMARY KEY (NodeLctn, Lctn)
);

CREATE TABLE public.Tier2_Dimm_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-D8
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   SizeMB                  BigInt            NOT NULL,         -- Size of this memory module in megabyte units
   ModuleLocator           VarChar(25)       NOT NULL,         -- E.g., CPU1_DIMM_A2, CPU2_DIMM_F1
   BankLocator             VarChar(25),                        -- E.g., Node 1, Node 2
   DbUpdatedTimestamp      timestamp without time zone         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        timestamp without time zone         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this dimm is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this dimm, rather the service operation is done on the node!
   EntryNumber             BigInt            NOT NULL        -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);


--- FUNCTION DEFINITIONS START HERE ----

-- ForeignTimeStamp is keep as a RFC-3339 string by design.
CREATE OR REPLACE FUNCTION public.LastRawReplacementHistoryUpdate()
    RETURNS varchar
AS $$
DECLARE
    max_time_str varchar;
BEGIN
    EXECUTE 'SELECT MAX(ForeignTimestamp) FROM tier2_RawHWInventory_History'
        into max_time_str;
    RETURN max_time_str;
END
$$ LANGUAGE plpgsql;


--
-- Name: aggregatedenvdatalistattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.aggregatedenvdatalistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.tier2_aggregatedenvdata
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select * from Tier2_AggregatedEnvData
            where Timestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                Timestamp >= p_start_time
            order by Timestamp desc, Lctn LIMIT 200;
    else
        return query
            select * from Tier2_AggregatedEnvData
            where Timestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            order by Timestamp desc, Lctn LIMIT 200;
    end if;
    return;
END
$$;


--
-- Name: aggregatedenvdatastore(character varying, timestamptz, character varying, double precision, double precision, double precision, character varying, bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.aggregatedenvdatastore(p_location character varying, p_timestamp timestamptz, p_type character varying, p_max_value double precision, p_min_value double precision, p_avg_value double precision, p_adapter_type character varying, p_work_item_id bigint) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_AggregatedEnvData(
        Lctn,
        Timestamp,
        Type,
        MaximumValue,
        MinimumValue,
        AverageValue,
        AdapterType,
        WorkItemId)
    values(
        p_location,
        p_timestamp at time zone 'UTC',
        p_type,
        p_max_value,
        p_min_value,
        p_avg_value,
        p_adapter_type,
        p_work_item_id);
$$;


--
-- Name: computenodehistorylistofstateattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.computenodehistorylistofstateattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS TABLE(lctn character varying, state character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    prev_lctn varchar(20) := '';
BEGIN
    if (p_end_time is null) then
        for Lctn, State in
            select CN.Lctn, CN.State from Tier2_ComputeNode_History CN
            order by Lctn, LastChgTimestamp desc
        loop
            if (Lctn <> prev_lctn) then
                prev_lctn := Lctn;
                return next;
            end if;
        end loop;
    elsif (p_start_time is null) then
        for Lctn, State in
            select CN.Lctn, CN.State from Tier2_ComputeNode_History CN
            where LastChgTimestamp <= p_end_time
            order by Lctn, LastChgTimestamp desc
        loop
            if (Lctn <> prev_lctn) then
                prev_lctn := Lctn;
                return next;
            end if;
        end loop;
    else
        for Lctn, State in
            select CN.Lctn, CN.State from Tier2_ComputeNode_History CN
            where LastChgTimestamp <= p_end_time and
                LastChgTimestamp >= p_start_time
            order by Lctn, LastChgTimestamp desc
        loop
            if (Lctn <> prev_lctn) then
                prev_lctn := Lctn;
                return next;
            end if;
        end loop;
    end if;
    return;
END
$$;

--
-- Name: servicenodehistorylistofstateattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.servicenodehistorylistofstateattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS TABLE(lctn character varying, state character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    prev_lctn varchar(20) := '';
BEGIN
    if (p_end_time is null) then
        for Lctn, State in
            select CN.Lctn, CN.State from Tier2_ServiceNode_History CN
            order by Lctn, LastChgTimestamp desc
        loop
            if (Lctn <> prev_lctn) then
                prev_lctn := Lctn;
                return next;
            end if;
        end loop;
    elsif (p_start_time is null) then
        for Lctn, State in
            select CN.Lctn, CN.State from Tier2_ServiceNode_History CN
            where LastChgTimestamp <= p_end_time
            order by Lctn, LastChgTimestamp desc
        loop
            if (Lctn <> prev_lctn) then
                prev_lctn := Lctn;
                return next;
            end if;
        end loop;
    else
        for Lctn, State in
            select CN.Lctn, CN.State from Tier2_ServiceNode_History CN
            where LastChgTimestamp <= p_end_time and
                LastChgTimestamp >= p_start_time
            order by Lctn, LastChgTimestamp desc
        loop
            if (Lctn <> prev_lctn) then
                prev_lctn := Lctn;
                return next;
            end if;
        end loop;
    end if;
    return;
END
$$;

--
-- Name: computenodehistoryoldesttimestamp(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.computenodehistoryoldesttimestamp() RETURNS timestamp without time zone
    LANGUAGE sql
    AS $$
    select min(LastChgTimestamp) from Tier2_ComputeNode_History;
$$;


--
-- Name: computenodeinventorylist(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.computenodeinventorylist(p_starttime timestamp without time zone, p_endtime timestamp without time zone) RETURNS SETOF public.tier2_computenode_history
    LANGUAGE sql
    AS $$
select DISTINCT ON (sequencenumber) * from  tier2_computenode_history where lastchgtimestamp >= coalesce(p_starttime, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS')
    and lastchgtimestamp <= coalesce(p_endtime, current_timestamp at time zone 'UTC') order by sequencenumber, dbupdatedtimestamp desc;
   $$;

--
-- Name: dbchgtimestamps(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.dbchgtimestamps() RETURNS TABLE(key character varying, value timestamp without time zone)
    LANGUAGE plpgsql
    AS $$
    BEGIN
      return query
          select 'Node_Max_LastChgTimestamp'::varchar,
              max(LastChgTimestamp)
          from Tier2_ComputeNode_History;

      return query
          select 'Node_Max_DbUpdatedTimestamp'::varchar,
              max(DbUpdatedTimestamp)
          from Tier2_ComputeNode_History;

      return query
          select 'Ras_Max_DbUpdatedTimestamp'::varchar,
              max(DbUpdatedTimestamp)
          from Tier2_RasEvent;

      return query
          select 'Job_Max_LastChgTimestamp'::varchar,
              max(LastChgTimestamp)
          from Tier2_Job_History;

      return query
          select 'Job_Max_DbUpdatedTimestamp'::varchar,
              max(DbUpdatedTimestamp)
          from Tier2_Job_History;

      return query
          select 'Reservation_Max_DbUpdatedTimestamp'::varchar,
              max(DbUpdatedTimestamp)
          from Tier2_wlmreservation_history;

      return query
          select 'Env_Max_Timestamp'::varchar,
            max("timestamp")
          from Tier2_aggregatedenvdata;

      return query
          select 'Inv_Max_Timestamp'::varchar,
            max(dbupdatedtimestamp)
          from Tier2_nodeinventory_history;

      return query
          select 'InvSS_Max_Timestamp'::varchar,
            max(snapshottimestamp)
          from Tier2_inventorysnapshot;

      return query
          select 'Diags_Max_Timestamp'::varchar,
            max(starttimestamp)
          from Tier2_Diag_History;

      return query
          select 'Replacement_Max_Timestamp'::varchar,
            max(dbupdatedtimestamp)
          from Tier2_replacement_history;

      return query
          select 'Service_Operation_Max_Timestamp'::varchar,
            max(dbupdatedtimestamp)
          from Tier2_serviceoperation_history;

      return query
          select 'Service_Node_LastChg_Timestamp'::varchar,
             max(lastchgtimestamp)
          from tier2_servicenode_history;

      return query
          select 'Compute_Node_LastChg_Timestamp'::varchar,
              max(lastchgtimestamp)
          from tier2_computenode_history;

      return;
    END
$$;


--
-- Name: diaglistofactivediagsattime(timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.diaglistofactivediagsattime(p_end_time timestamp without time zone) RETURNS SETOF public.tier2_diag_history
    LANGUAGE sql
    AS $$
    select D1.* from Tier2_Diag_History D1
    where D1.StartTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
        (D1.EndTimestamp is null or
        D1.EndTimestamp > coalesce(p_end_time, current_timestamp at time zone 'UTC')) and
        D1.LastChgTimestamp = (select max(D2.LastChgTimestamp) from Tier2_Diag_History D2 where D2.DiagId = D1.DiagId)
    order by DiagId desc;
$$;


--
-- Name: diaglistofnonactivediagsattime(timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.diaglistofnonactivediagsattime(p_end_time timestamp without time zone) RETURNS SETOF public.tier2_diag_history
    LANGUAGE sql
    AS $$
    select D1.* from Tier2_Diag_History D1
    where D1.EndTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
        D1.LastChgTimestamp = (select max(D2.LastChgTimestamp) from Tier2_Diag_History D2 where D2.DiagId = D1.DiagId)
    order by DiagId desc;
$$;

--
-- Name:getdiagdata(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--
CREATE OR REPLACE FUNCTION public.getdiagdata(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_diagid character varying, p_limit integer) RETURNS SETOF public.tier2_diagresults
    LANGUAGE plpgsql
    AS $$
DECLARE
v_diagids int[];
BEGIN
    if p_diagid != '%' then
    v_diagids := (string_to_array(p_diagid, ',') ::int[]);
    return query
            select * from  tier2_diagresults
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and diagid = ANY(v_diagids) and
                case
                   when p_lctn ='%' then (lctn ~ '.*' or lctn is null)
                    when p_lctn != '%' then ((lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))))
                end
            order by dbupdatedtimestamp DESC LIMIT p_limit;
    else
    return query
    select * from  tier2_diagresults
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
            case
               when p_lctn ='%' then (lctn ~ '.*' or lctn is null)
                when p_lctn != '%' then ((lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))))
            end
    order by dbupdatedtimestamp DESC LIMIT p_limit;
    end if;
    return;
END $$;

--
-- Name: get_cacheipaddrtolctn_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_cacheipaddrtolctn_records() RETURNS TABLE(ipaddr character varying, lctn character varying)
    LANGUAGE plpgsql
    AS $$ BEGIN
    return query
            select H.IpAddr, H.Lctn
            from Tier2_ServiceNode_SS H;

        return query
            select H.IpAddr, H.Lctn
            from Tier2_ComputeNode_SS H;
END $$;


--
-- Name: get_cachemacaddrtolctn_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_cachemacaddrtolctn_records() RETURNS TABLE(macaddr character varying, lctn character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN
return query
        select H.MacAddr, H.Lctn
        from Tier2_ServiceNode_SS H;

    return query
        select H.MacAddr, H.Lctn
        from Tier2_ComputeNode_SS H;
END
$$;




--
-- Name: get_diag_list_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_diag_list_records() RETURNS SETOF public.tier2_diag_list
    LANGUAGE sql
    AS $$
    select *
    from Tier2_Diag_list;
$$;




--
-- Name: get_diag_tools_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_diag_tools_records() RETURNS SETOF public.tier2_diag_tools_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_Diag_Tools_ss;
$$;


--
-- Name: get_latest_adapter_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_adapter_records() RETURNS SETOF public.tier2_adapter_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_Adapter_SS where state='A';
$$;

--
-- Name: get_latest_bootimage_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_bootimage_records() RETURNS SETOF public.tier2_bootimage_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_BootImage_SS ;
$$;


--
-- Name: get_latest_chassis_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_chassis_records() RETURNS SETOF public.tier2_chassis_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_Chassis_ss;
$$;


--
-- Name: get_latest_computenode_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_computenode_records() RETURNS SETOF public.tier2_computenode_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_ComputeNode_SS;
$$;


--
-- Name: get_latest_diag_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_diag_records() RETURNS SETOF public.tier2_diag_history
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_Diag_History H
    inner join
        (select DiagId, max(LastChgTimestamp) as max_date
         from Tier2_Diag_History
         group by DiagId) D
    on H.DiagId = D.DiagId and H.LastChgTimestamp = D.max_date;
$$;


--
-- Name: get_latest_job_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_job_records() RETURNS SETOF public.tier2_job_history
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_Job_History H
    inner join
        (select JobId, max(DbUpdatedTimestamp) as max_date
         from Tier2_Job_History
         group by JobId) D
    on H.JobId = D.JobId and H.DbUpdatedTimestamp = D.max_date;
$$;


--
-- Name: get_latest_jobstep_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_jobstep_records() RETURNS SETOF public.tier2_jobstep_history
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_JobStep_History H
    inner join
        (select JobId, JobStepId, max(LastChgTimestamp) as max_date
         from Tier2_JobStep_History
         group by JobId, JobStepId) D
    on H.JobId = D.JobId and
        H.JobStepId = D.JobStepId and
        H.LastChgTimestamp = D.max_date;
$$;


--
-- Name: get_latest_machine_records(); Type: FUNCTION; Schema: public; Owner: -
--
CREATE OR REPLACE FUNCTION public.get_latest_machine_records() RETURNS SETOF public.tier2_machine_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_Machine_SS;
$$;

--
-- Name: get_latest_machineadapterinstance_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_machineadapterinstance_records() RETURNS SETOF public.tier2_machineadapterinstance_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_MachineAdapterInstance_ss H;
$$;

--
-- Name: get_latest_rack_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_rack_records() RETURNS SETOF public.tier2_rack_ss
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_Rack_ss H;
$$;


--
-- Name: get_latest_servicenode_records(); Type: FUNCTION; Schema: public; Owner: -
--
CREATE OR REPLACE FUNCTION public.get_latest_servicenode_records() RETURNS SETOF public.tier2_servicenode_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_ServiceNode_ss;
$$;



--
-- Name: get_latest_serviceoperation_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_serviceoperation_records() RETURNS SETOF public.tier2_serviceoperation_history
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_ServiceOperation_History H
    inner join
        (select Lctn, max(DbUpdatedTimestamp) as max_date
         from Tier2_ServiceOperation_History
         group by Lctn) D
    on H.Lctn = D.Lctn and
        H.DbUpdatedTimestamp = D.max_date;
$$;

--
-- Name: get_latest_serviceoperation_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.ServiceOperationAtTime(p_end_time timestamp without time zone) RETURNS SETOF public.tier2_serviceoperation_history
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_ServiceOperation_History H
    where H.dbupdatedtimestamp = (select max(H2.dbupdatedtimestamp) from tier2_serviceoperation_history H2 where H2.serviceoperationid = H.serviceoperationid);
$$;


--
-- Name: get_latest_workitem_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_workitem_records() RETURNS SETOF public.tier2_workitem_SS
    LANGUAGE sql
    AS $$
    select *
    from Tier2_WorkItem_SS H
    where WorkToBeDone <> 'BaseWork' and State <> 'D';
$$;


--
-- Name: get_machineadapterinstance_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_machineadapterinstance_records() RETURNS SETOF public.tier2_machineadapterinstance_history
    LANGUAGE sql
    AS $$
    select *
    from Tier2_MachineAdapterInstance_History;
$$;


--
-- Name: get_rasmetadata_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_rasmetadata_records() RETURNS SETOF public.tier2_rasmetadata
    LANGUAGE sql
    AS $$
    select *
    from Tier2_RasMetadata;
$$;


--
-- Name: get_ucsconfigvalue_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_ucsconfigvalue_records() RETURNS SETOF public.tier2_ucsconfigvalue_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_UcsConfigValue_ss;
$$;


--
-- Name: get_uniquevalues_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_uniquevalues_records() RETURNS SETOF public.tier2_uniquevalues_ss
    LANGUAGE sql
    AS $$
    select *
    from Tier2_UniqueValues_ss;
$$;


--
-- Name: get_wlmreservation_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_wlmreservation_records() RETURNS SETOF public.tier2_wlmreservation_history
    LANGUAGE sql
    AS $$
    select *
    from Tier2_WlmReservation_History;
$$;


--
-- Name: getaggregatedevndatawithfilters(timestamp without time zone, timestamp without time zone, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getaggregatedevndatawithfilters(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_aggregatedenvdata
    LANGUAGE sql
    AS $$
        select * from  tier2_aggregatedenvdata
        where timestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
            timestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
            case
                when p_lctn='%' then (lctn ~ '.*' or lctn is null)
                when p_lctn is not null then ((lctn not like '') and ((select string_to_array(lctn, ' ')) <@ (select string_to_array(p_lctn, ','))))
            end
        order by timestamp DESC LIMIT p_limit; $$;


--
-- Name: getinventorychange(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getinventorychange(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_replacement_history
    LANGUAGE plpgsql
    AS $$
    DECLARE
        p_sernum character varying;
    BEGIN
        p_sernum := (select distinct on (newsernum) newsernum from tier2_replacement_history where newsernum = p_lctn);
        if (p_sernum is not null) then
        return query
            select * from  tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
                newsernum like (p_sernum || '%')
            order by lctn, dbupdatedtimestamp desc limit p_limit;

        else
        return query
            select * from  tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
                (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
            order by lctn, dbupdatedtimestamp desc limit p_limit;
        end if;
        return;
    END

$$;

--
-- Name: getnodeinventoryinfoforlctn(p_lctn character varying, p_limit integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getnodeinventoryinfoforlctn(p_lctn character varying, p_sernum character varying, p_limit integer) RETURNS TABLE(Lctn VarChar(25), InventoryInfo VarChar(16384), InventoryTimestamp TIMESTAMP, Sernum VarChar(50), EntryNumber BigInt)
    LANGUAGE sql
    AS $$
        select IH.Lctn, IH.InventoryInfo, IH.InventoryTimestamp, IH.Sernum, IH.EntryNumber  from tier2_nodeinventory_history IH
        where
        case
            when p_lctn = '%' then (IH.Lctn ~ '.*' or IH.Lctn is null)
            when p_lctn != '%' then (IH.Lctn not like '') and ((select string_to_array(IH.Lctn, '')) <@ (select string_to_array(p_lctn, ',')))
        end
        and
        case
            when p_sernum = '%' then (IH.Sernum ~ '.*' or IH.Sernum is null)
            when p_sernum != '%' then (IH.Sernum not like '') and ((select string_to_array(IH.Sernum, '')) <@ (select string_to_array(p_sernum, ',')))
        end
        order by IH.InventoryTimestamp desc limit p_limit;
$$;


--
-- Name: getinventorydataforlctn(timestamp without time zone, timestamp without time zone, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getinventorydataforlctn(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS SETOF public.inventorydatatype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is null and p_end_time is null then
    return query
        select distinct on (lctn)
        CAST(lctn AS VARCHAR(30)), sequencenumber, state, hostname, bootimageid, environment, ipaddr, macaddr, bmcipaddr, bmcmacaddr, bmchostname,
        dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp,
        wlmnodestate, ConstraintId, entrynumber, ProofOfLifeTimestamp
        from  tier2_computenode_history
        where (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
        order by lctn, lastchgtimestamp desc limit p_limit;

    return query
        select distinct on (lctn)
        lctn, sequencenumber, state, hostname, bootimageid, CAST('' AS VARCHAR(240)), ipaddr, macaddr, bmcipaddr, bmcmacaddr, bmchostname,
        dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp,
        CAST('U' AS VARCHAR(1)), ConstraintId, entrynumber, ProofOfLifeTimestamp
        from  tier2_servicenode_history
        where (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
        order by lctn, lastchgtimestamp desc limit p_limit;
    else
    return query
        select CAST(lctn AS VARCHAR(30)), sequencenumber, state, hostname, bootimageid, environment, ipaddr, macaddr, bmcipaddr, bmcmacaddr, bmchostname,
       dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp,
       wlmnodestate, ConstraintId, entrynumber, ProofOfLifeTimestamp
        from  tier2_computenode_history
        where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
            dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
            (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
        order by lastchgtimestamp desc limit p_limit;
    return query
        select lctn, sequencenumber, state, hostname, bootimageid, CAST('' AS VARCHAR(240)), ipaddr, macaddr, bmcipaddr, bmcmacaddr, bmchostname,
        dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp,
        CAST('U' AS VARCHAR(1)), ConstraintId, entrynumber, ProofOfLifeTimestamp
        from  tier2_servicenode_history
        where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
            dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
            (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
        order by lastchgtimestamp desc limit p_limit;
    END IF;
    return;
END;

$$;


--
-- Name: getjobpowerdata(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getjobpowerdata(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_jobid character varying, p_limit integer) RETURNS TABLE(jobid character varying, lctn character varying, jobpowertimestamp timestamp without time zone, totalruntime double precision, totalpackageenergy double precision, totaldramenergy double precision)
    LANGUAGE plpgsql
    AS $$
    BEGIN
        if p_lctn != '%' and p_lctn != '' then
        return query
            select JP.jobid, JP.lctn, JP.jobpowertimestamp, JP.totalruntime, JP.totalpackageenergy, JP.totaldramenergy from  tier2_job_power JP
            where JP.jobpowertimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                JP.jobpowertimestamp >= coalesce(p_start_time, '1970-01-01 0:0:0') and
                (JP.lctn not like '') and ((select string_to_array(JP.lctn, ' ')) <@  (select string_to_array(p_lctn, ','))) and
                JP.jobid like p_jobid
            order by JP.lctn, JP.jobid desc limit p_limit;

        else
        return query
            select JP.jobid, JP.lctn, JP.jobpowertimestamp, JP.totalruntime, JP.totalpackageenergy, JP.totaldramenergy from  tier2_job_power JP
            where JP.jobpowertimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                JP.jobpowertimestamp >= coalesce(p_start_time, '1970-01-01 0:0:0') and
                JP.lctn like (p_lctn || '%') and
                JP.jobid like p_jobid
            order by JP.lctn, JP.jobid desc limit p_limit;
        end if;
        return;
    END

$$;


--
-- Name: getlistnodelctns(bytea); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getlistnodelctns(p_job_nodes bytea) RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_lctn varchar;
    v_list varchar;
    v_first boolean := true;
    v_num_bits integer;
    v_bit_index integer;

BEGIN
    v_num_bits := length(p_job_nodes) * 8;
    for i in 0 .. v_num_bits - 1 loop
        v_bit_index := v_num_bits - 1 - i;
        if get_bit(p_job_nodes, v_bit_index) = 1 then
            select Lctn into v_lctn
            from Tier2_ComputeNode_History
            where SequenceNumber = v_bit_index
            order by DbUpdatedTimestamp
            limit 1;

            if v_lctn is null then
                raise exception 'GetListNodeLctns - can''t find corresponding Lctn string for node sequence number = %!', i;
            end if;

            if v_first then
                v_list := v_lctn;
                v_first := false;
            else
                v_list := v_list || ' ' || v_lctn;
            end if;
        end if;
    end loop;

    return v_list;
END
$$;


--
-- Name: getlistnodelctnsastable(bytea); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getlistnodelctnsastable(p_job_nodes bytea) RETURNS character varying[]
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_lctn varchar;
    v_list varchar[];
    v_first boolean := true;
    v_num_bits integer;
    v_bit_index integer;

BEGIN
    CREATE temporary TABLE nodelisttable (lctn character varying(10) not null) on commit drop;
    v_num_bits := length(p_job_nodes) * 8;
    for i in 0 .. v_num_bits - 1 loop
        v_bit_index := v_num_bits - 1 - i;
        if get_bit(p_job_nodes, v_bit_index) = 1 then
            select Lctn into v_lctn from Tier2_ComputeNode_History
            where SequenceNumber = v_bit_index
            order by DbUpdatedTimestamp limit 1;

            if v_lctn is null then
                raise exception 'GetListNodeLctns - can''t find corresponding Lctn string for node sequence number = %!', i;
            end if;
            v_list := array_append(v_list, v_lctn);
        end if;
    end loop;
    return v_list;
END
$$;


--
-- Name: getmanifestcontent(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getmanifestcontent(OUT manifestcontent character varying) RETURNS character varying
    LANGUAGE sql
    AS $$
    select manifestcontent from tier2_machine_history order by dbupdatedtimestamp desc limit 1;
$$;


--
-- Name: getraseventforjob(timestamp without time zone, timestamp without time zone, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--


CREATE OR REPLACE FUNCTION public.getraseventforjob(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_job_id character varying, p_limit integer) RETURNS SETOF public.raseventtype
    LANGUAGE plpgsql
    AS $$

DECLARE
    v_prev_job_id varchar := '';
    v_node_list varchar;
    v_job Tier2_Job_History;
    v_rec JobActiveType%rowtype;
    v_nodenums bytea;
    v_locslist varchar[];

BEGIN
    select numnodes into v_nodenums from tier2_job_history where jobid=p_job_id limit 1;
    select * into v_locslist from getlistnodelctnsastable(v_nodenums);
    return query
        select RE.DescriptiveName,
        RE.LastChgTimestamp,
        RE.DbUpdatedTimestamp,
        MD.Severity,
        RE.Lctn,
        RE.JobId,
        RE.ControlOperation,
        MD.Msg,
        RE.InstanceData
        from Tier2_RasEvent RE
        inner join Tier2_RasMetaData MD on RE.DescriptiveName = MD.DescriptiveName
        where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
            RE.DbUpdatedTimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
            RE.lctn = any (v_locslist)
        order by RE.DbUpdatedTimestamp desc, RE.DescriptiveName, RE.Id LIMIT p_limit;
    return;
END

$$;


--
-- Name: getraseventswithfilters(timestamp without time zone, timestamp without time zone, character varying, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getraseventswithfilters(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_event_type character varying, p_severity character varying, p_jobid character varying, p_exclude character varying, p_limit integer) RETURNS TABLE(type character varying(65), "time" timestamp without time zone, dbupdatedtimestamp timestamp without time zone, severity character varying(10), lctn character varying(100), jobid character varying(30), controloperation character varying(50), detail character varying(10000))
    LANGUAGE plpgsql
    AS $$

DECLARE
    v_start_time timestamp without time zone;
    v_end_time timestamp without time zone;
    v_lctn character varying;
    v_severity character varying;
    v_jobid character varying;
    v_limit integer;

BEGIN
    v_start_time := p_start_time;
    v_end_time := p_end_time;
    v_lctn := p_lctn;
    v_severity := p_severity;
    v_jobid := p_jobid;
    v_limit := p_limit;

    if v_severity != '%' then
        v_severity = upper(v_severity);
    end if;

    return query
        select  RE.descriptivename,
                RE.lastchgtimestamp,
                RE.dbupdatedtimestamp,
                MD.Severity,
                RE.lctn,
                RE.jobid,
                RE.controloperation,
                CAST(CONCAT(MD.msg, ' ', RE.instancedata) AS character varying(10000))
        from Tier2_RasEvent RE
            inner join Tier2_RasMetaData MD on
            RE.descriptivename = MD.descriptivename
        where RE.lastchgtimestamp <=
            coalesce(v_end_time, current_timestamp at time zone 'UTC') and
            RE.lastchgtimestamp >= coalesce(v_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '6 MONTHS') and
            MD.Severity like v_severity and
            case
                when p_exclude = '%' then (RE.descriptivename ~ '.*')
                when p_exclude != '%' then (RE.descriptivename !~ p_exclude)
            end
            and
            case
                when p_event_type = '%' then (RE.descriptivename ~ '.*')
                when p_event_type != '%' then (RE.descriptivename ~ p_event_type)
            end
            and
            case
                when v_jobid is null then (RE.jobid ~ '.*' or RE.jobid is null)
                when v_jobid is not null then (RE.jobid not like '') and ((select string_to_array(RE.jobid, '')) <@ (select string_to_array(v_jobid, ',')))
            end

            and
            case
                when v_lctn is null then (RE.lctn ~ '.*' or RE.lctn is null)
                when v_lctn is not null then (RE.lctn not like '') and ((select string_to_array(RE.lctn, '')) <@ (select string_to_array(v_lctn, ',')))
            end
        order by RE.lastchgtimestamp desc, RE.descriptivename, RE.id LIMIT v_limit;
    return;
END
$$;


--
-- Name: getrefsnapshotdataforlctn(character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getrefsnapshotdataforlctn(p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_inventorysnapshot
    LANGUAGE sql
    AS $$
        select * from  tier2_inventorysnapshot
        where (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))) and reference = true
        order by snapshottimestamp desc limit p_limit;
$$;


--
-- Name: getsnapshotdataforlctn(timestamp without time zone, timestamp without time zone, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getsnapshotdataforlctn(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_inventorysnapshot
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is null and p_end_time is null then
        return query
            select * from  tier2_inventorysnapshot
            where (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
            order by snapshottimestamp desc limit p_limit;
    elsif p_start_time is null then
        return query
            select * from  tier2_inventorysnapshot
            where snapshottimestamp <= p_end_time and
            (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
            order by snapshottimestamp desc limit p_limit;
    elsif p_end_time is null then
        return query
            select * from  tier2_inventorysnapshot
            where snapshottimestamp > p_start_time and
            (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
            order by snapshottimestamp desc limit p_limit;
    else
        return query
            select * from  tier2_inventorysnapshot
            where snapshottimestamp > p_start_time and snapshottimestamp <= p_end_time and
            (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
            order by snapshottimestamp desc limit p_limit;
    end if;
    return;
END
$$;


--
-- Name: insertorupdatediaglist(character varying, character varying, character varying, character varying, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.insertorupdatediaglist(p_diaglistid character varying, p_diagtoolid character varying, p_description character varying, p_defaultparameters character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_Diag_List(
        DiagListId,
        DiagToolId,
        Description,
        DefaultParameters,
        DbUpdatedTimestamp)
    values(
        p_diaglistid,
        p_diagtoolid,
        p_description,
        p_defaultparameters,
        p_dbupdatedtimestamp)
    on conflict(DiagListId) do update set
        Description = p_description,
        DefaultParameters = p_defaultparameters,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;


--
-- Name: insertorupdatediagtools(character varying, character varying, character varying, integer, character varying, character varying, character varying, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.insertorupdatediagtools(p_diagtoolid character varying, p_description character varying, p_unittype character varying, p_unitsize integer, p_provisionreqd character varying, p_rebootbeforereqd character varying, p_rebootafterreqd character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_Diag_Tools(
        DiagToolId,
        Description,
        UnitType,
        UnitSize,
        ProvisionReqd,
        RebootBeforeReqd,
        RebootAfterReqd,
        DbUpdatedTimestamp)
    values(
        p_diagtoolid,
        p_description,
        p_unittype,
        p_unitsize,
        p_provisionreqd,
        p_rebootbeforereqd,
        p_rebootafterreqd,
        p_dbupdatedtimestamp)
    on conflict(DiagToolId) do update set
        Description = p_description,
        UnitType = p_unittype,
        UnitSize = p_unitsize,
        ProvisionReqd = p_provisionreqd,
        RebootBeforeReqd = p_rebootbeforereqd,
        RebootAfterReqd = p_rebootafterreqd,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;


--
-- Name: insertorupdaterasevent(bigint, character varying, character varying, character varying, character varying, integer, character varying, character varying, character varying, timestamp without time zone, timestamp without time zone, character varying, bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.insertorupdaterasevent(p_id bigint, p_descriptivename character varying, p_lctn character varying, p_sernum character varying, p_jobid character varying, p_numberrepeats integer, p_controloperation character varying, p_done character varying, p_instancedata character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint) RETURNS void
    LANGUAGE plpgsql
    AS $$ BEGIN
    insert into Tier2_RasEvent(
        Id,
        DescriptiveName,
        Lctn,
        Sernum,
        JobId,
        NumberRepeats,
        ControlOperation,
        Done,
        InstanceData,
        DbUpdatedTimestamp,
        LastChgTimestamp,
        LastChgAdapterType,
        LastChgWorkItemId)
    values(
        p_id,
        p_descriptivename,
        p_lctn,
        p_sernum,
        p_jobid,
        p_numberrepeats,
        p_controloperation,
        p_done,
        p_instancedata,
        p_dbupdatedtimestamp,
        p_lastchgtimestamp,
        p_lastchgadaptertype,
        p_lastchgworkitemid);
  END;
$$;


--
-- Name: insertorupdaterasmetadata(character varying, character varying, character varying, character varying, character varying, character varying, character varying, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.insertorupdaterasmetadata(p_descriptivename character varying, p_severity character varying, p_category character varying, p_component character varying, p_controloperation character varying, p_msg character varying, p_dbupdatedtimestamp timestamp without time zone, p_generatealert character varying) RETURNS void
     LANGUAGE sql
     AS $$
     insert into Tier2_RasMetaData(
         DescriptiveName,
         Severity,
         Category,
         Component,
         ControlOperation,
         Msg,
         DbUpdatedTimestamp,
 	    GenerateAlert)
     values(
         p_descriptivename,
         p_severity,
         p_category,
         p_component,
         p_controloperation,
         p_msg,
         p_dbupdatedtimestamp,
 	p_generatealert)
     on conflict(DescriptiveName) do update set
         Severity = p_severity,
         Category = p_category,
         Component = p_component,
         ControlOperation = p_controloperation,
         Msg = p_msg,
         DbUpdatedTimestamp = p_dbupdatedtimestamp,
 	    generatealert=p_generatealert;
 $$;

--
-- Name: insertorupdateucsconfigvalue(character varying, character varying, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.insertorupdateucsconfigvalue(p_key character varying, p_value character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_UcsConfigValue(
        Key,
        Value,
        DbUpdatedTimestamp)
    values(
        p_key,
        p_value,
        p_dbupdatedtimestamp)
    on conflict(Key) do update set
        Value = p_value,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;


--
-- Name: insertorupdateuniquevalues(character varying, bigint, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.insertorupdateuniquevalues(p_entity character varying, p_nextvalue bigint, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_UniqueValues(
        Entity,
        NextValue,
        DbUpdatedTimestamp)
    values(
        p_entity,
        p_nextvalue,
        p_dbupdatedtimestamp)
    on conflict(Entity) do update set
        NextValue = p_nextvalue,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;


--
-- Name: inventoryinfolist(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.inventoryinfolist(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS TABLE(Lctn VarChar(25), InventoryInfo VarChar(16384), inventorytimestamp TIMESTAMP, Sernum VarChar(50))
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select distinct on (ni.Lctn) ni.Lctn, ni.InventoryInfo, ni.dbupdatedtimestamp, ni.Sernum  from tier2_nodeinventory_history ni
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                dbupdatedtimestamp >= p_start_time
            order by Lctn, dbupdatedtimestamp desc;
    else
        return query
            select distinct on (ni.Lctn) ni.Lctn, ni.InventoryInfo, ni.dbupdatedtimestamp , ni.Sernum  from tier2_nodeinventory_history ni
                        where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            order by Lctn, dbupdatedtimestamp desc;
    end if;
    return;
END
$$;


--
-- Name: inventorysnapshotlist(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.inventorysnapshotlist(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.tier2_inventorysnapshot
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select * from tier2_inventorysnapshot
            where snapshottimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                snapshottimestamp >= p_start_time
            order by snapshottimestamp desc;
    else
        return query
            select * from tier2_inventorysnapshot
            where snapshottimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            order by snapshottimestamp desc;
    end if;
    return;
END
$$;


--
-- Name: jobhistorylistofactivejobsattime(timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.jobhistorylistofactivejobsattime(p_end_time timestamp without time zone) RETURNS SETOF public.jobactivetype
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_prev_job_id varchar := '';
    v_node_list varchar;
    v_job Tier2_Job_History;
    v_rec JobActiveType%rowtype;

BEGIN
    for v_job  in
        select *
        from Tier2_Job_History
        where LastChgTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
        order by JobId desc,
            DbUpdatedTimestamp desc
    loop
        if v_job.JobId <> v_prev_job_id then
            v_prev_job_id := v_job.JobId;
            if v_job.State = 'S' then
                v_rec.JobId := v_job.JobId;
                v_rec.JobName := v_job.JobName;
                v_rec.State := v_job.State;
                v_rec.Bsn := v_job.Bsn;
                v_rec.UserName := v_job.UserName;
                v_rec.StartTimestamp := v_job.StartTimestamp;
                v_rec.NumNodes := v_job.NumNodes;
                v_rec.Nodes := GetListNodeLctns(v_job.Nodes);
                v_rec.Wlmjobstate := v_job.Wlmjobstate;
                return next v_rec;
            end if;
        end if;
    end loop;
    return;
END
$$;


--
-- Name: jobhistorylistofnonactivejobsattime(timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--


CREATE OR REPLACE FUNCTION public.jobhistorylistofnonactivejobsattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.jobnonactivetype
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_prev_job_id varchar := '';
    v_job Tier2_Job_History;
    v_rec JobNonActiveType%rowtype;

BEGIN
    for v_job  in
        select *
        from Tier2_Job_History
        where LastChgTimestamp >  coalesce(p_start_time, current_timestamp at time zone 'UTC' - INTERVAL '5 DAYS')
        and LastChgTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
        order by JobId desc,
            DbUpdatedTimestamp desc,
            LastChgTimestamp desc
    loop
        if v_job.JobId <> v_prev_job_id then
            v_prev_job_id := v_job.JobId;
            if v_job.State <> 'S' then
                v_rec.JobId := v_job.JobId;
                v_rec.JobName := v_job.JobName;
                v_rec.State := v_job.State;
                v_rec.Bsn := v_job.Bsn;
                v_rec.UserName := v_job.UserName;
                v_rec.StartTimestamp := v_job.StartTimestamp;
                v_rec.EndTimestamp := v_job.EndTimestamp;
                v_rec.ExitStatus := v_job.ExitStatus;
                v_rec.NumNodes := v_job.NumNodes;
                v_rec.Nodes := GetListNodeLctns(v_job.Nodes);
                v_rec.JobAcctInfo := v_job.JobAcctInfo;
                v_rec.Wlmjobstate := v_job.Wlmjobstate;
                return next v_rec;
            end if;
        end if;
    end loop;
    return;
END
$$;

--
-- Name: GetJobInfo(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.GetJobInfo(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_at_time timestamp without time zone, p_jobid character varying, p_username character varying, p_state character varying, p_locations character varying, p_limit integer) RETURNS SETOF public.jobnonactivetype
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_prev_job_id varchar := '';
    v_job Tier2_Job_History;
    v_rec JobNonActiveType%rowtype;
    v_nodes varchar := '';
    v_counter integer := 0;

BEGIN
    for v_job  in
        select *
        from Tier2_Job_History j
        where coalesce(j.endtimestamp, current_timestamp at time zone 'UTC') <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
        and j.starttimestamp >= coalesce(p_start_time, '1970-01-01 0:0:0')
        and
        case
                when p_at_time is not null then j.endtimestamp >= p_at_time and j.starttimestamp <= p_at_time and j.state = 'T'
                when p_at_time is null then j.state ~ '.*'
        end
        and
        case
                when p_jobid = '%' then (j.jobid ~ '.*')
                when p_jobid != '%' then (j.jobid = p_jobid)
        end
        and
        case
                when p_username = '%' then (j.username ~ '.*')
                when p_username != '%' then (j.username = p_username)
        end
        and j.state = 'S'
        and not exists(select * from tier2_job_history where jobid = j.jobid and state = 'T')
        UNION
        select *
        from Tier2_Job_History
        where coalesce(endtimestamp, current_timestamp at time zone 'UTC') <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
        and starttimestamp >= coalesce(p_start_time, '1970-01-01 0:0:0')
        and
        case
                when p_at_time is not null then endtimestamp >= p_at_time and starttimestamp <= p_at_time and state = 'T'
                when p_at_time is null then state ~ '.*'
        end
        and
        case
                when p_jobid = '%' then (jobid ~ '.*')
                when p_jobid != '%' then (jobid = p_jobid)
        end
        and
        case
                when p_username = '%' then (username ~ '.*')
                when p_username != '%' then (username = p_username)
        end
        and state = 'T'
        order by dbupdatedtimestamp desc, JobId desc, State desc
    loop
        if v_counter < p_limit then
            if v_job.JobId <> v_prev_job_id then
                v_prev_job_id := v_job.JobId;
                if p_state = '%' or (p_state = v_job.state) then
                    v_nodes := GetListNodeLctns(v_job.Nodes);
                    if p_locations = '%' or areNodesInJob(string_to_array(p_locations, ','), string_to_array(v_nodes,' ')) then
                        v_counter := v_counter + 1;
                        v_rec.JobId := v_job.JobId;
                        v_rec.JobName := v_job.JobName;
                        v_rec.State := v_job.State;
                        v_rec.Bsn := v_job.Bsn;
                        v_rec.UserName := v_job.UserName;
                        v_rec.StartTimestamp := v_job.StartTimestamp;
                        v_rec.EndTimestamp := v_job.EndTimestamp;
                        v_rec.ExitStatus := v_job.ExitStatus;
                        v_rec.NumNodes := v_job.NumNodes;
                        v_rec.Nodes := v_nodes;
                        v_rec.JobAcctInfo := v_job.JobAcctInfo;
                        v_rec.Wlmjobstate := v_job.Wlmjobstate;
                        return next v_rec;
                    end if;
                end if;
            end if;
        end if;
    end loop;
    return;
END
$$;

CREATE OR REPLACE FUNCTION public.areNodesInJob(p_locations character varying[], p_jobLocations character varying[]) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE

    v_areNodesInJob boolean := false;
    v_loc varchar;
    v_jobloc varchar;

BEGIN

    if p_jobLocations is null then
        v_areNodesInJob = false;
    else
        foreach v_loc in array p_locations
            loop
                foreach v_jobloc in array p_jobLocations
                loop
                    v_areNodesInJob = v_areNodesInJob or (v_loc = v_jobloc);
                end loop;
            end loop;
    end if;

    return v_areNodesInJob;
END
$$;


--
-- Name: raseventlistattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.raseventlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.raseventtypewithdescriptivename
    LANGUAGE plpgsql
    AS $$
BEGIN
     if p_start_time is not null then
            return query
                 select RE.DescriptiveName,
                                RE.LastChgTimestamp,
                                RE.DbUpdatedTimestamp,
                                MD.Severity,
                                RE.Lctn,
                                RE.JobId,
                                RE.ControlOperation,
                                MD.Msg,
                                RE.InstanceData
                            from Tier2_RasEvent RE
                    inner join Tier2_RasMetaData MD on
                                        RE.DescriptiveName = MD.DescriptiveName and
                                        MD.DbUpdatedTimestamp =
                                        (select max(T.DbUpdatedTimestamp) from Tier2_RasMetaData T
                                        where T.DescriptiveName = MD.DescriptiveName)
                                where RE.LastChgTimestamp <=
                                    coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                                    RE.LastChgTimestamp >= p_start_time  and
                                    MD.Severity in ('ERROR','FATAL')
                                order by RE.DbUpdatedTimestamp desc, RE.DescriptiveName, RE.Id;
                            else
            return query
                        select RE.DescriptiveName, RE.LastChgTimestamp,
                            RE.DbUpdatedTimestamp,
                            MD.Severity,
                            RE.Lctn,
                            RE.JobId,
                            RE.ControlOperation,
                            MD.Msg,
                            RE.InstanceData
                        from Tier2_RasEvent RE
                            inner join Tier2_RasMetaData MD on
                                RE.DescriptiveName = MD.DescriptiveName and
                                MD.DbUpdatedTimestamp =
                                (select max(T.DbUpdatedTimestamp) from Tier2_RasMetaData T
                                where T.DescriptiveName = MD.DescriptiveName)
                        where  RE.LastChgTimestamp <=
                                              coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                                               RE.LastChgTimestamp >
                                              (coalesce(p_end_time, current_timestamp at time zone 'UTC') - INTERVAL '75 DAYS') and
                                              MD.Severity in ('ERROR','FATAL')
                        order by RE.DbUpdatedTimestamp desc, RE.DescriptiveName, RE.Id;
            end if;
            return;
END
$$;


--
-- Name: replacementhistorylist(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.replacementhistorylist(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.tier2_replacement_history
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select * from tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                dbupdatedtimestamp >= p_start_time
            order by lastchgtimestamp desc;
    else
        return query
            select * from tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            order by lastchgtimestamp desc;
    end if;
    return;
END
$$;


--
-- Name: reservationlistattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.reservationlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.reservationtype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is null then
        return query
            select RE.ReservationName,
                RE.Users,
                RE.Nodes,
                RE.StartTimestamp,
                RE.EndTimestamp,
                RE.DeletedTimestamp,
                RE.LastChgTimestamp
            from Tier2_WlmReservation_History RE
            where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            order by RE.DbUpdatedTimestamp desc, RE.ReservationName, RE.Users LIMIT 200;
    else
        return query
           select RE.ReservationName,
                RE.Users,
                RE.Nodes,
                RE.StartTimestamp,
                RE.EndTimestamp,
                RE.DeletedTimestamp,
                RE.LastChgTimestamp
            from Tier2_WlmReservation_History RE
            where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
            RE.DbUpdatedTimestamp >= p_start_time
            order by RE.DbUpdatedTimestamp desc, RE.ReservationName, RE.Users LIMIT 200;
    end if;
    return;
END
$$;

--
-- Name: reservationlistattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getReservationInfo(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_reservation_name character varying, p_user character varying, p_limit integer) RETURNS SETOF public.reservationtype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is null then
        return query
            select RE.ReservationName,
                RE.Users,
                RE.Nodes,
                RE.StartTimestamp,
                RE.EndTimestamp,
                RE.DeletedTimestamp,
                RE.LastChgTimestamp
            from Tier2_WlmReservation_History RE
            where coalesce(RE.EndTimestamp, current_timestamp at time zone 'UTC') <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            and RE.ReservationName = coalesce(p_reservation_name, RE.ReservationName)
            and RE.Users LIKE '%' || coalesce(p_user, RE.Users) || '%'
            order by RE.DbUpdatedTimestamp desc, RE.ReservationName, RE.Users LIMIT p_limit;
    else
        return query
           select RE.ReservationName,
                RE.Users,
                RE.Nodes,
                RE.StartTimestamp,
                RE.EndTimestamp,
                RE.DeletedTimestamp,
                RE.LastChgTimestamp
            from Tier2_WlmReservation_History RE
            where coalesce(RE.EndTimestamp, current_timestamp at time zone 'UTC') <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            and RE.StartTimestamp >= p_start_time
            and RE.ReservationName = coalesce(p_reservation_name, RE.ReservationName)
            and RE.Users LIKE '%' || coalesce(p_user, RE.Users) || '%'
            order by RE.DbUpdatedTimestamp desc, RE.ReservationName, RE.Users LIMIT p_limit;
    end if;
    return;
END
$$;

--
-- Name: getServiceInfo Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getServiceInfo(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_service_id character varying, p_lctn character varying, p_type character varying, p_open character varying, p_limit integer) RETURNS SETOF public.tier2_serviceoperation_history
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_open = 'True' then
        return query
                select *
                from tier2_serviceoperation_history s1
                where s1.stoptimestamp IS null
                and s1.serviceoperationid = coalesce(CAST(p_service_id AS int), s1.serviceoperationid)
                and
                case
                    when p_lctn = '%' then (s1.lctn ~ '.*' or s1.lctn is null)
                    else (s1.lctn not like '') and ((select string_to_array(s1.lctn, '')) <@ (select string_to_array(p_lctn, ',')))
                end
                and s1.typeofserviceoperation = coalesce(p_type, s1.typeofserviceoperation)
                and s1.dbupdatedtimestamp = (select MAX(dbupdatedtimestamp) from tier2_serviceoperation_history s2 where s2.serviceoperationid = s1.serviceoperationid)
                order by s1.dbupdatedtimestamp desc, s1.serviceoperationid LIMIT p_limit;
    else
        if p_start_time is null then
            return query
                select *
                from tier2_serviceoperation_history s1
                where coalesce(stoptimestamp, current_timestamp at time zone 'UTC') <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
                and serviceoperationid = coalesce(CAST(p_service_id AS int), serviceoperationid)
                and
                case
                    when p_lctn = '%' then (s1.lctn ~ '.*' or s1.lctn is null)
                    else (s1.lctn not like '') and ((select string_to_array(s1.lctn, '')) <@ (select string_to_array(p_lctn, ',')))
                end
                and typeofserviceoperation = coalesce(p_type, typeofserviceoperation)
                order by dbupdatedtimestamp desc, serviceoperationid LIMIT p_limit;
        else
            return query
                select *
                from tier2_serviceoperation_history s1
                where coalesce(stoptimestamp, current_timestamp at time zone 'UTC') <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
                and starttimestamp >= p_start_time
                and serviceoperationid = coalesce(CAST(p_service_id AS int), serviceoperationid)
                and
                case
                    when p_lctn = '%' then (s1.lctn ~ '.*' or s1.lctn is null)
                    else (s1.lctn not like '') and ((select string_to_array(s1.lctn, '')) <@ (select string_to_array(p_lctn, ',')))
                end
                and typeofserviceoperation = coalesce(p_type, typeofserviceoperation)
                order by dbupdatedtimestamp desc, serviceoperationid LIMIT p_limit;
        end if;
    end if;
    return;
END
$$;

--
-- Name: getServiceInfo Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.GetSubfruState(p_lctn character varying, p_subfru character varying) RETURNS TABLE(NodeLctn VarChar(25), SubFruLctn VarChar(30), State VarChar(1))
    LANGUAGE plpgsql
    AS $$
BEGIN
    return query
        SELECT T0.NodeLctn, T0.Lctn as SubFruLctn, T0.State
        FROM Tier2_Processor_History T0
        WHERE (p_subfru = 'all' OR p_subfru='cpu')
        AND (select string_to_array(T0.NodeLctn, '')) <@ (select string_to_array(p_lctn, ','))
        AND T0.Lctn in (SELECT DISTINCT lctn from Tier2_Processor_History T1)
        AND DbUpdatedTimestamp = (SELECT MAX(DbUpdatedTimestamp) FROM Tier2_Processor_History T2 WHERE T2.Lctn = T0.Lctn)
        UNION
        SELECT T0.NodeLctn, T0.Lctn as SubFruLctn, T0.State
        FROM Tier2_Accelerator_History T0
        WHERE (p_subfru = 'all' OR p_subfru='gpu')
        AND (select string_to_array(T0.NodeLctn, '')) <@ (select string_to_array(p_lctn, ','))
        AND T0.Lctn in (SELECT DISTINCT lctn from Tier2_Accelerator_History T1)
        AND DbUpdatedTimestamp = (SELECT MAX(DbUpdatedTimestamp) FROM Tier2_Accelerator_History T2 WHERE T2.Lctn = T0.Lctn)
        UNION
        SELECT T0.NodeLctn, T0.Lctn as SubFruLctn, T0.State
        FROM Tier2_Hfi_History T0
        WHERE (p_subfru = 'all' OR p_subfru='hfi')
        AND (select string_to_array(T0.NodeLctn, '')) <@ (select string_to_array(p_lctn, ','))
        AND T0.Lctn in (SELECT DISTINCT lctn from Tier2_Hfi_History T1)
        AND DbUpdatedTimestamp = (SELECT MAX(DbUpdatedTimestamp) FROM Tier2_Hfi_History T2 WHERE T2.Lctn = T0.Lctn)
        UNION
        SELECT T0.NodeLctn, T0.Lctn as SubFruLctn, T0.State
        FROM Tier2_Dimm_History T0
        WHERE (p_subfru = 'all' OR p_subfru='dimm')
        AND (select string_to_array(T0.NodeLctn, '')) <@ (select string_to_array(p_lctn, ','))
        AND T0.Lctn in (SELECT DISTINCT lctn from Tier2_Dimm_History T1)
        AND DbUpdatedTimestamp = (SELECT MAX(DbUpdatedTimestamp) FROM Tier2_Dimm_History T2 WHERE T2.Lctn = T0.Lctn)
        ORDER BY NodeLctn;
    return;
END
$$;

--
-- Name: GetComputeNodeSummary(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.GetComputeNodeSummary() RETURNS SETOF public.system_summary_count
    LANGUAGE sql
    AS $$
    SELECT state, count(*)
    FROM
    (SELECT *
    FROM
    (SELECT  lctn, state, ROW_NUMBER() OVER(PARTITION BY lctn ORDER BY dbupdatedtimestamp DESC) rn
    FROM tier2_computenode_history) a
    WHERE rn = 1) b
    group by state;
$$;

--
-- Name: GetServiceNodeSummary(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.GetServiceNodeSummary() RETURNS SETOF public.system_summary_count
    LANGUAGE sql
    AS $$
        SELECT state, count(*)
        FROM
        (SELECT *
        FROM
        (SELECT  lctn, state, ROW_NUMBER() OVER(PARTITION BY lctn ORDER BY dbupdatedtimestamp DESC) rn
        FROM tier2_servicenode_history) a
        WHERE rn = 1) b
        group by state;
$$;

--
-- Name: serviceinventorylist(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.servicenodeinventorylist(p_starttime timestamp without time zone, p_endtime timestamp without time zone) RETURNS SETOF public.tier2_servicenode_history
    LANGUAGE sql
    AS $$
    select DISTINCT ON (sequencenumber) * from  tier2_servicenode_history where lastchgtimestamp >= coalesce(p_starttime, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and lastchgtimestamp <= coalesce(p_endtime, current_timestamp at time zone 'UTC') order by sequencenumber, dbupdatedtimestamp desc;
$$;


--
-- Name: setrefsnapshotdataforlctn(integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.setrefsnapshotdataforlctn(p_id integer) RETURNS SETOF public.tier2_inventorysnapshot
    LANGUAGE plpgsql
    AS $$
DECLARE
    p_lctn varchar;

BEGIN
        select lctn into p_lctn
        from  tier2_inventorysnapshot
        where id = p_id;

        if p_lctn is null then
            raise exception 'Inventory snapshot not found: %', p_id;
        else
            update tier2_inventorysnapshot
            set reference = false
            where lctn = p_lctn;

            update tier2_inventorysnapshot
            set reference = true
            where id = p_id;
        end if;
END
$$;

--
-- Name: addAuthorizedUserWithRole
--

CREATE OR REPLACE FUNCTION public.addAuthorizedUserWithRole(p_userid character varying, p_roleid character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    role_exists character varying;
BEGIN
        select roleid into role_exists
        from tier2_authorized_user
        where userid = p_userid;

        if role_exists is null then
            insert into tier2_authorized_user(
                userid,
                roleid)
            values(
                p_userid,
                p_roleid);
        else
            update tier2_authorized_user
            set roleid = p_roleid
            where userid = p_userid;

        end if;
END
$$;

--
-- Name: removeAuthorizedUserWithRole
--

CREATE OR REPLACE FUNCTION public.removeAuthorizedUserWithRole(p_userid character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    user_exists character varying;
BEGIN
            select userid into user_exists
            from tier2_authorized_user
            where userid = p_userid;

            if user_exists is null then
                raise exception 'user doesnot exist.';
            else
                delete from tier2_authorized_user where
                userid = p_userid;
            end if;

END
$$;


--
-- Name: removeAuthorizedUserWithRole
--

CREATE OR REPLACE FUNCTION public.updateAuthorizedUserWithRole(p_userid character varying, p_roleid character varying) RETURNS void
    LANGUAGE sql
    AS $$
    update tier2_authorized_user
    set roleid = p_roleid where
    userid = p_userid;
$$;


--
-- Name: getUserRoles
--

CREATE OR REPLACE FUNCTION public.getUserRoles(p_userid character varying) RETURNS TABLE(roleid character varying)
    LANGUAGE sql
    AS $$
    select roleid from tier2_authorized_user
    where userid = p_userid;
$$;

--
-- Name: getRoleUsers
--

CREATE OR REPLACE FUNCTION public.getRoleUsers(p_roleid character varying) RETURNS TABLE(userid character varying)
    LANGUAGE sql
    AS $$
    select userid from tier2_authorized_user
    where roleid = p_roleid;
$$;


CREATE OR REPLACE FUNCTION public.insertorupdateadapterdata(p_id bigint, p_adaptertype character varying, p_sconrank bigint, p_state character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_lctn character varying, p_pid bigint) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_Adapter_SS(
        id, adaptertype, sconrank, state,dbupdatedtimestamp, lastchgadaptertype, lastchgworkitemid,lctn,pid
        )
    values(
p_id, p_adaptertype, p_sconrank, p_state,p_dbupdatedtimestamp, p_lastchgadaptertype, p_lastchgworkitemid,p_lctn,p_pid)
    on conflict(id,Adaptertype) do update set
       sconrank= p_sconrank, state = p_state,dbupdatedtimestamp =p_dbupdatedtimestamp, lastchgadaptertype = p_lastchgAdaptertype, lastchgworkitemid = p_lastchgworkitemid,lctn = p_lctn;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatebootimagedata(p_id character varying, p_description character varying, p_bootimagefile character varying, p_bootimagechecksum character varying, p_bootoptions character varying, p_bootstrapimagefile character varying, p_bootstrapimagechecksum character varying, p_state character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_kernelargs character varying, p_files character varying) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_bootimage_ss(
        Id, Description, BootImageFile,BootImageChecksum, BootOptions, BootStrapImageFile, BootStrapImageChecksum, State,
        DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, KernelArgs, Files
        )
    values(p_Id, p_Description, p_BootImageFile, p_BootImageChecksum, p_BootOptions, p_BootStrapImageFile, p_BootStrapImageChecksum, p_State,
        p_DbUpdatedTimestamp, p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_KernelArgs, p_Files)
    on conflict(Id) do update set
        Id=p_id, Description=p_description, BootImageFile=p_BootImageFile, BootImageChecksum=p_BootImageChecksum, BootOptions=p_BootOptions, BootStrapImageFile=p_BootStrapImageFile, BootStrapImageChecksum=p_BootStrapImageChecksum, State=p_State,
        DbUpdatedTimestamp=p_DbUpdatedTimestamp, LastChgTimestamp=p_LastChgTimestamp, LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, KernelArgs=p_KernelArgs, Files=p_Files;
$$;


CREATE OR REPLACE FUNCTION public.insertorupdatechassisdata(p_lctn character varying, p_state character varying, p_sernum character varying, p_type character varying, p_vpd character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_owner character varying) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_Chassis_SS(Lctn, State, Sernum, Type, Vpd, DbUpdatedTimestamp,
               LastChgTimestamp, Owner) values(p_Lctn , p_State , p_Sernum , p_Type , p_Vpd , p_DbUpdatedTimestamp ,
               p_LastChgTimestamp , p_Owner ) on conflict(Lctn) do update set
		State= p_State , Sernum=p_Sernum , Type=p_Type , Vpd=p_Vpd , DbUpdatedTimestamp=p_DbUpdatedTimestamp ,
               LastChgTimestamp=p_LastChgTimestamp , Owner=p_Owner
;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatecomputenodedata(p_lctn character varying, p_sequencenumber integer, p_state character varying,
p_hostname character varying, p_bootimageid character varying, p_environment character varying, p_ipaddr character varying, p_macaddr character varying, p_bmcipaddr character varying, p_bmcmacaddr character varying,
p_bmchostname character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_owner character varying,
p_aggregator character varying, p_inventorytimestamp timestamp without time zone, p_wlmnodestate character varying, p_constraintid character varying, p_proofoflifetimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_ComputeNode_SS(Lctn, SequenceNumber, HostName, State, BootImageId, Environment,
               IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp,
               LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp,  Wlmnodestate, ConstraintId, ProofOfLifeTimestamp)
               values(p_Lctn, p_SequenceNumber, p_HostName, p_State, p_BootImageId, p_environment,
               p_IpAddr, p_MacAddr, p_BmcIpAddr, p_BmcMacAddr, p_BmcHostName, p_DbUpdatedTimestamp,
               p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_Owner, p_Aggregator, p_Inventorytimestamp,  p_Wlmnodestate, p_constraintid, p_proofoflifetimestamp) on conflict(Lctn) do update set SequenceNumber = p_SequenceNumber, HostName = p_HostName,
State=p_State, BootImageId = p_BootImageId,
               IpAddr=p_IpAddr, MacAddr=p_MacAddr, BmcIpAddr=p_BmcIpAddr,
BmcMacAddr=p_BmcMacAddr, BmcHostName=p_BmcHostName, DbUpdatedTimestamp=p_DbUpdatedTimestamp,
               LastChgTimestamp=p_LastChgTimestamp, LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, Owner=p_Owner,
Aggregator=p_Aggregator, Inventorytimestamp=p_Inventorytimestamp,  Wlmnodestate=p_Wlmnodestate, environment=p_environment, constraintid=p_constraintid, ProofOfLifeTimestamp=p_proofoflifetimestamp
;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatediagtools_ss(p_diagtoolid character varying, p_description character varying, p_unittype character varying, p_unitsize integer, p_provisionreqd character varying, p_rebootbeforereqd character varying, p_rebootafterreqd character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_Diag_Tools_SS(
        DiagToolId,
        Description,
        UnitType,
        UnitSize,
        ProvisionReqd,
        RebootBeforeReqd,
        RebootAfterReqd,
        DbUpdatedTimestamp)
    values(
        p_diagtoolid,
        p_description,
        p_unittype,
        p_unitsize,
        p_provisionreqd,
        p_rebootbeforereqd,
        p_rebootafterreqd,
        p_dbupdatedtimestamp)
    on conflict(DiagToolId) do update set
        Description = p_description,
        UnitType = p_unittype,
        UnitSize = p_unitsize,
        ProvisionReqd = p_provisionreqd,
        RebootBeforeReqd = p_rebootbeforereqd,
        RebootAfterReqd = p_rebootafterreqd,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatemachineadapterinstancedata(p_snlctn character varying, p_adaptertype character varying, p_numinitialinstances bigint, p_numstartedinstances bigint, p_invocation character varying, p_logfile character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_machineadapterinstance_ss(
        SnLctn, AdapterType, NumInitialInstances, NumStartedInstances, Invocation, LogFile, DbUpdatedTimestamp)
    values(p_SnLctn, p_AdapterType, p_NumInitialInstances, p_NumStartedInstances, p_Invocation, p_LogFile, p_DbUpdatedTimestamp)
    on conflict(SnLctn, AdapterType) do update set
        SnLctn=p_SnLctn, AdapterType=p_AdapterType, NumInitialInstances=p_NumInitialInstances, NumStartedInstances=p_NumStartedInstances, Invocation=p_Invocation, LogFile=p_LogFile, DbUpdatedTimestamp=p_DbUpdatedTimestamp;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatemachinedata(p_sernum character varying, p_description character varying, p_type character varying, p_numrows bigint, p_numcolsinrow bigint, p_numchassisinrack bigint, p_state character varying, p_clockfreq bigint, p_manifestlctn character varying, p_manifestcontent character varying, p_dbupdatedtimestamp timestamp without time zone, p_usingsynthesizeddata character varying) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_Machine_SS(Sernum, Description, Type, NumRows, NumColsInRow,
               NumChassisInRack, State, ClockFreq, ManifestLctn, ManifestContent,
               DbUpdatedTimestamp, UsingSynthesizedData) values(p_Sernum, p_Description, p_Type, p_NumRows, p_NumColsInRow,
               p_NumChassisInRack, p_State, p_ClockFreq, p_ManifestLctn, p_ManifestContent,
               p_DbUpdatedTimestamp, p_UsingSynthesizedData) on conflict(Sernum) do update set
Sernum = p_Sernum, Description = p_Description, Type=p_Type, NumRows=p_NumRows, NumColsInRow=p_NumColsInRow,
               NumChassisInRack=p_NumChassisInRack, State=p_State, ClockFreq =p_ClockFreq, ManifestLctn=p_ManifestLctn, ManifestContent = p_ManifestContent,
               DbUpdatedTimestamp=p_DbUpdatedTimestamp, UsingSynthesizedData =p_UsingSynthesizedData
;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdaterackdata(p_lctn character varying, p_state character varying, p_sernum character varying, p_type character varying, p_vpd character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_owner character varying) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_Rack_SS(Lctn, State, Sernum, Type, Vpd, DbUpdatedTimestamp,
               LastChgTimestamp, Owner) values(p_Lctn, p_State, p_Sernum, p_Type, p_Vpd, p_DbUpdatedTimestamp,
               p_LastChgTimestamp, p_Owner) on conflict(Lctn) do update set
	State=p_State, Sernum=p_Sernum, Type=p_Type, Vpd=p_Vpd, DbUpdatedTimestamp=p_DbUpdatedTimestamp,
               LastChgTimestamp=p_LastChgTimestamp, Owner=p_Owner
;
$$;


CREATE OR REPLACE FUNCTION public.insertorupdateservicenodedata(p_lctn character varying, p_sequencenumber integer, p_hostname character varying, p_state character varying, p_bootimageid character varying, p_ipaddr character varying, p_macaddr character varying, p_bmcipaddr character varying, p_bmcmacaddr character varying, p_bmchostname character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_owner character varying, p_aggregator character varying, p_inventorytimestamp timestamp without time zone, p_constraintid character varying, p_ProofOfLifeTimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_ServiceNode_SS(Lctn, SequenceNumber, HostName, State, BootImageId,
               IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp,
               LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, ConstraintId, ProofOfLifeTimestamp)
               values(p_Lctn, p_SequenceNumber, p_HostName, p_State, p_BootImageId,
               p_IpAddr, p_MacAddr, p_BmcIpAddr, p_BmcMacAddr, p_BmcHostName, p_DbUpdatedTimestamp,
               p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_Owner, p_Aggregator, p_Inventorytimestamp, p_constraintid, p_ProofOfLifeTimestamp) on conflict(Lctn) do update set
sequenceNumber = p_SequenceNumber, HostName = p_HostName,
State=p_State, BootImageId = p_BootImageId,
               IpAddr=p_IpAddr, MacAddr=p_MacAddr,  BmcIpAddr=p_BmcIpAddr,
BmcMacAddr=p_BmcMacAddr, BmcHostName=p_BmcHostName, DbUpdatedTimestamp=p_DbUpdatedTimestamp,
               LastChgTimestamp=p_LastChgTimestamp, LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, Owner=p_Owner,
Aggregator=p_Aggregator, Inventorytimestamp=p_Inventorytimestamp, constraintid=p_constraintid, ProofOfLifeTimestamp=p_ProofOfLifeTimestamp
;
$$;

--
-- Name: insertorupdateucsconfigvalue_ss(character varying, character varying, timestamp without time zone); Type: FUNCTION; Schema: public; Owner:
--

CREATE OR REPLACE FUNCTION public.insertorupdateucsconfigvalue_ss(p_key character varying, p_value character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_UcsConfigValue_SS(
        Key,
        Value,
        DbUpdatedTimestamp)
    values(
        p_key,
        p_value,
        p_dbupdatedtimestamp)
    on conflict(Key) do update set
        Value = p_value,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdateuniquevalues_ss(p_entity character varying, p_nextvalue bigint, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_UniqueValues_ss(
        Entity,
        NextValue,
        DbUpdatedTimestamp)
    values(
        p_entity,
        p_nextvalue,
        p_dbupdatedtimestamp)
    on conflict(Entity) do update set
        NextValue = p_nextvalue,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdateworkitemdata(p_queue character varying, p_workingadaptertype character varying, p_id bigint, p_worktobedone character varying, p_parameters character varying, p_notifywhenfinished character varying, p_state character varying, p_requestingworkitemid bigint, p_requestingadaptertype character varying, p_workingadapterid bigint, p_workingresults character varying, p_results character varying, p_starttimestamp timestamp without time zone, p_dbupdatedtimestamp timestamp without time zone, p_endtimestamp timestamp without time zone, p_rowinsertedintohistory character varying) RETURNS void
    LANGUAGE sql
    AS $$

insert into Tier2_WorkItem_SS(Queue, WorkingAdapterType, Id, WorkToBeDone,
                Parameters, NotifyWhenFinished, State, RequestingWorkItemId,
                RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp,
                DbUpdatedTimestamp, endtimestamp, rowinsertedintohistory)
                values(p_queue, p_workingadaptertype,
    p_id ,
    p_worktobedone,
    p_parameters ,
    p_notifywhenfinished ,
    p_state ,
    p_requestingworkitemid ,
    p_requestingadaptertype ,
    p_workingadapterid ,
    p_workingresults ,
    p_results ,
    p_starttimestamp ,
    p_dbupdatedtimestamp, p_endtimestamp, p_rowinsertedintohistory) on conflict(WorkingAdapterType,Id) do update set
state = p_state,
 WorkToBeDone=p_worktobedone,
                Parameters=p_parameters,
NotifyWhenFinished = p_notifywhenfinished,
RequestingWorkItemId = p_requestingworkitemid ,
RequestingAdapterType= p_requestingadaptertype,
WorkingAdapterId=p_workingadapterid,
WorkingResults=p_workingresults,
Results=p_results,
DbUpdatedTimestamp = p_dbupdatedtimestamp
, endtimestamp = p_endtimestamp ,
rowinsertedintohistory =p_rowinsertedintohistory
;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdateraseventdata_ss(p_id bigint, p_descriptivename character varying, p_lctn character varying, p_sernum character varying, p_jobid character varying, p_numberrepeats integer, p_controloperation character varying, p_done character varying, p_instancedata character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint) RETURNS void
    LANGUAGE plpgsql
    AS $$ BEGIN
    insert into Tier2_RasEvent_ss(
        Id,
        DescriptiveName,
        Lctn,
        Sernum,
        JobId,
        NumberRepeats,
        ControlOperation,
        Done,
        InstanceData,
        DbUpdatedTimestamp,
        LastChgTimestamp,
        LastChgAdapterType,
        LastChgWorkItemId)
    values(
        p_id,
        p_descriptivename,
        p_lctn,
        p_sernum,
        p_jobid,
        p_numberrepeats,
        p_controloperation,
        p_done,
        p_instancedata,
        p_dbupdatedtimestamp,
        p_lastchgtimestamp,
        p_lastchgadaptertype,
        p_lastchgworkitemid)
    on conflict(DescriptiveName, Id) do update set
        Lctn = p_lctn,
        Sernum = p_sernum,
        JobId = p_jobid,
        NumberRepeats = p_numberrepeats,
        ControlOperation = p_controloperation,
        Done = p_done,
        InstanceData = p_instancedata,
        DbUpdatedTimestamp = p_dbupdatedtimestamp,
        LastChgTimestamp = p_lastchgtimestamp,
        LastChgAdapterType = p_lastchgadaptertype,
        LastChgWorkItemId = p_lastchgworkitemid; END;
$$;


CREATE OR REPLACE FUNCTION public.truncatesnapshottablerecords()
 RETURNS void
 LANGUAGE sql
AS $$
       truncate table tier2_adapter_ss, tier2_bootimage_ss, tier2_chassis_ss, tier2_computenode_ss, tier2_diag_tools_ss,
       tier2_machineadapterinstance_ss, tier2_machine_ss, tier2_rack_ss, tier2_servicenode_ss, tier2_rasmetadata,
       tier2_ucsconfigvalue_ss, tier2_uniquevalues_ss, tier2_workitem_ss, tier2_rasevent_ss;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatenonnodehwdata(p_lctn character varying, p_sequencenumber integer, p_type character varying, p_state character varying, p_hostname character varying, p_ipaddr character varying, p_macaddr character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_owner character varying, p_aggregator character varying, p_inventorytimestamp timestamp without time zone, p_tier2dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_tier2_nonnodehw_history(Lctn, SequenceNumber, HostName, type, State, IpAddr, MacAddr, DbUpdatedTimestamp,
               LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp,  Tier2DbUpdatedTimestamp)
               values(p_Lctn, p_SequenceNumber, p_HostName, p_type, p_State, p_IpAddr, p_MacAddr, p_DbUpdatedTimestamp,
               p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_Owner, p_Aggregator, p_Inventorytimestamp,
               p_Tier2DbUpdatedTimestamp) on conflict(Lctn) do update set SequenceNumber = p_SequenceNumber, HostName = p_HostName,
State=p_State, Type = p_type,
               IpAddr=p_IpAddr, MacAddr=p_MacAddr, DbUpdatedTimestamp=p_DbUpdatedTimestamp,
               LastChgTimestamp=p_LastChgTimestamp, LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, Owner=p_Owner,
Aggregator=p_Aggregator, Inventorytimestamp=p_Inventorytimestamp,  Tier2DbUpdatedTimestamp=p_Tier2DbUpdatedTimestamp;
$$;


CREATE OR REPLACE FUNCTION public.insertorupdatenonnodehwdata_ss(p_Lctn VarChar, p_SequenceNumber Integer, p_Type VarChar, p_State VarChar, p_HostName VarChar, p_IpAddr VarChar,
p_MacAddr VarChar, p_DbUpdatedTimestamp TIMESTAMP, p_LastChgTimestamp TIMESTAMP, p_LastChgAdapterType VarChar, p_LastChgWorkItemId BigInt, p_Owner VarChar, p_Aggregator VarChar, p_InventoryTimestamp TIMESTAMP) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_NonNodeHw_ss(
    Lctn, SequenceNumber, Type, State, HostName,IpAddr, MacAddr, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp
        )
    values(p_Lctn, p_SequenceNumber, p_Type, p_State, p_HostName, p_IpAddr, p_MacAddr, p_DbUpdatedTimestamp, p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_Owner, p_Aggregator, p_InventoryTimestamp)
    on conflict(Lctn) do update set SequenceNumber=p_SequenceNumber, Type=p_Type, State=p_State, HostName=p_HostName,
    IpAddr=p_IpAddr, MacAddr=p_MacAddr, DbUpdatedTimestamp=p_DbUpdatedTimestamp, LastChgTimestamp=p_LastChgTimestamp,
    LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, Owner=p_Owner, Aggregator=p_Aggregator,
    InventoryTimestamp=p_InventoryTimestamp;

$$;

CREATE OR REPLACE FUNCTION public.get_nonnodehw_records() RETURNS SETOF public.tier2_nonnodehw_ss
    LANGUAGE sql
    AS $$
    select *
    from tier2_nonnodehw_ss;
$$;

CREATE OR REPLACE FUNCTION public.get_raseventdata_records() RETURNS SETOF public.tier2_rasevent_ss
    LANGUAGE sql
    AS $$
    select *
    from tier2_rasevent_ss where done = 'N' and controloperation <> null and descriptivename <> 'RasDaimgrShuttingDownDaiMgr';
$$;



CREATE OR REPLACE FUNCTION public.insertorupdatenodeinventorydata(p_Lctn VarChar, p_DbUpdatedTimestamp TIMESTAMP, p_InventoryTimestamp TIMESTAMP,  p_InventoryInfo VarChar, p_Sernum VarChar, p_BiosInfo VarChar) RETURNS void
    LANGUAGE sql
    AS $$
    insert into tier2_nodeinventory_history(Lctn, DbUpdatedTimestamp, InventoryTimestamp, InventoryInfo, Sernum, BiosInfo, Tier2DbUpdatedTimestamp)
    values(p_Lctn, p_DbUpdatedTimestamp, p_InventoryTimestamp, p_InventoryInfo, p_Sernum, p_BiosInfo, current_timestamp at time zone 'UTC')
    on conflict(Lctn, InventoryTimestamp) do update set DbUpdatedTimestamp=p_DbUpdatedTimestamp, InventoryInfo=p_InventoryInfo,
    Sernum= p_Sernum, BiosInfo=p_BiosInfo, Tier2DbUpdatedTimestamp = current_timestamp at time zone 'UTC';
$$;


CREATE OR REPLACE FUNCTION public.get_cnos_cfg() RETURNS SETOF public.tier2_cnos_config
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT * FROM public.tier2_cnos_config;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_cnos_cfg(_name varchar) RETURNS SETOF public.tier2_cnos_config
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT * FROM public.tier2_cnos_config WHERE name=_name;
END;
$$;


CREATE OR REPLACE FUNCTION public.create_cnos_cfg(_name varchar, _cont varchar, _part varchar)
RETURNS SETOF public.tier2_cnos_config
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO public.tier2_cnos_config (name, container, partition)
        VALUES (_name, public.cnos_container(_cont), _part);
    RETURN QUERY SELECT * FROM public.tier2_cnos_config WHERE name=_name;
END;
$$;


CREATE OR REPLACE FUNCTION public.update_cnos_cfg(_name varchar, _cont varchar, _part varchar)
RETURNS SETOF public.tier2_cnos_config
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO public.tier2_cnos_config (name, container, partition)
        VALUES (_name, public.cnos_container(_cont), _part)
        ON CONFLICT (name) DO UPDATE
            SET name=_name, container=public.cnos_container(_cont), partition=_part;
    RETURN QUERY SELECT * FROM public.tier2_cnos_config WHERE name=_name;
END;
$$;


CREATE OR REPLACE FUNCTION public.delete_cnos_cfg(_name varchar) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM public.tier2_cnos_config WHERE name=_name;
END;
$$;


CREATE OR REPLACE FUNCTION public.create_alert(alert_type varchar, description varchar,
    kind varchar, locations varchar, events public.alert_rasevent[]) RETURNS SETOF public.alert_full
LANGUAGE plpgsql
AS $$
DECLARE
    i BIGINT;
    alertid BIGINT;
    eventid BIGINT;
BEGIN
    INSERT INTO public.tier2_alert (alert_type, description, kind, locations)
        VALUES (alert_type, description, public.alert_kind(kind), locations) RETURNING id INTO alertid;
    FOR i IN 1..array_length(events, 1) LOOP
        INSERT INTO public.tier2_alert_rasevent(id, descriptivename, lctn, instancedata, jobid, lastchgtimestamp)
            VALUES (events[i].id, events[i].name, events[i].lctn,
                    events[i].data, events[i].jobid, events[i].timestamp) RETURNING internalid INTO eventid;
        INSERT INTO public.tier2_alert_has_ras (alertid, eventid)
            VALUES (alertid, eventid);
    END LOOP;
    RETURN QUERY SELECT * FROM public.get_open_alerts() WHERE id=alertid;
END;
$$;


CREATE OR REPLACE FUNCTION public.close_alert(alertids BIGINT[]) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    alert_count BIGINT;
    alertid BIGINT;
BEGIN
    FOREACH alertid IN ARRAY alertids LOOP
        INSERT INTO public.tier2_alert_history(id, alert_type, description, kind, created, locations)
            SELECT * FROM public.tier2_alert WHERE id=alertid;
        DELETE FROM public.tier2_alert WHERE id=alertid;
    END LOOP;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_open_alerts() RETURNS SETOF public.alert_full
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT
        a.id,
        a.alert_type,
        a.description,
        a.kind,
        a.created,
        null::timestamp,
        a.locations,
        'OPEN'::public.alert_state
    FROM public.tier2_alert AS a
    ORDER BY a.created DESC;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_open_alerts(lim BIGINT) RETURNS SETOF public.alert_full
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT
        a.id,
        a.alert_type,
        a.description,
        a.kind,
        a.created,
        null::timestamp,
        a.locations,
        'OPEN'::public.alert_state
    FROM public.tier2_alert AS a
    ORDER BY a.created DESC
    LIMIT lim;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_closed_alerts() RETURNS SETOF public.alert_full
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT
        a.id,
        a.alert_type,
        a.description,
        a.kind,
        a.created,
        a.closed,
        a.locations,
        'CLOSED'::public.alert_state
    FROM public.tier2_alert_history AS a
    ORDER BY a.closed DESC;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_closed_alerts(lim BIGINT) RETURNS SETOF public.alert_full
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT
        a.id,
        a.alert_type,
        a.description,
        a.kind,
        a.created,
        a.closed,
        a.locations,
        'CLOSED'::public.alert_state
    FROM public.tier2_alert_history AS a
    ORDER BY a.closed DESC
    LIMIT lim;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_alert(alert BIGINT) RETURNS SETOF public.alert_full
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT * FROM public.get_open_alerts() WHERE id=alert
        UNION SELECT * FROM public.get_closed_alerts() WHERE id=alert;
END;
$$;


CREATE OR REPLACE FUNCTION public.get_alert_events(alert BIGINT) RETURNS TABLE(
    id bigint,
    lctn varchar(100),
    jobid varchar(30),
    instancedata varchar(10000),
    lastchgtimestamp timestamp with time zone,
    descriptivename varchar(65),
    severity varchar(10),
    msg varchar(1000),
    generatealert varchar(1)
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY SELECT
        ras.id, ras.lctn, ras.jobid, ras.instancedata, timezone('utc', ras.lastchgtimestamp),
        ras.descriptivename, meta.severity, meta.msg, meta.generatealert
    FROM public.tier2_alert_rasevent AS ras
    LEFT JOIN public.tier2_rasmetadata AS meta ON ras.descriptivename=meta.descriptivename
    LEFT JOIN public.tier2_alert_has_ras AS h ON h.eventid=ras.internalid
    WHERE h.alertid=alert;
END;
$$;

CREATE OR REPLACE FUNCTION public.generate_partition_purge_rules(table_name text, timestamp_column_name text, retention_policy integer DEFAULT 6, fromscript boolean DEFAULT true)
RETURNS text
LANGUAGE plpgsql
        AS $function$
        DECLARE
            ddl_script TEXT;
            colu_name text;
            table_name_from_arg ALIAS FOR table_name;
            partition_table_section1 text;
            partition_table_section2 text;
        BEGIN

                    PERFORM 1 FROM information_schema.columns WHERE information_schema.columns.table_name = table_name_from_arg AND column_name = timestamp_column_name AND data_type LIKE 'timestamp%';
                    IF (NOT FOUND) THEN
                    RAISE EXCEPTION 'The specified column % in table % must exists and of type timestamp', quote_literal(timestamp_column_name), quote_literal(table_name);
                    END IF;
                    ddl_script:= '';
                    if (NOT fromscript) THEN
                    partition_table_section1 := 'execute format(''create table %I partition of public.';
                    partition_table_section2 :=  ' for values from (%L) to (%L)'', futuretableName, monthStart, monthEndExclusive);';
                    else
                    partition_table_section1 := 'execute format(''create table %%I partition of public.';
                    partition_table_section2 :=  ' for values from (%%L) to (%%L)'', futuretableName, monthStart, monthEndExclusive);';
                    end if;
                    ddl_script := ddl_script || '
                                                CREATE OR REPLACE FUNCTION public.createfuturepartitions_' || table_name ||'(
                                                ts timestamp without time zone)
                                        RETURNS void
                                        LANGUAGE ''plpgsql''
                                        VOLATILE
                                                AS $BODY$
                                                declare returned_res text;
                                                declare monthStart date;
                                                declare monthEndExclusive date;
                                        -- Create partition table name with suffix YYYYMM

                                        declare futuretableName text := '''|| table_name || '_'' || to_char(ts + interval ''1 MONTH'', ''YYYYmm'');
                                                begin
                                        -- Check if the partition table we need exists.
                                        if to_regclass(futuretableName) is null then
                                        -- Generate a new partition for the table
                                            monthStart := date_trunc(''MONTH'', ts) + interval ''1 MONTH'';
                                            monthEndExclusive := monthStart + interval ''1 MONTH'';

                                            ' || partition_table_section1 || table_name || partition_table_section2 || '
                                        end if;
                                                end;
                                                $BODY$;
                                                ';
                --- END OF CREATE PARTITION FUNCTION ---
                --- CREATE A RULE ON THE TABLE FOR EVERY INSERT ---
                ddl_script := ddl_script || ' CREATE OR REPLACE RULE autocall_createfuturepartitions_' || table_name || ' AS
                                            ON INSERT TO public.'|| table_name ||'
                                        DO (
                                                SELECT public.createfuturepartitions_'|| table_name || '(new.'|| timestamp_column_name ||') AS createfuturepartitions_'|| table_name || ' ;);';

--- END OF CREATE RULE FOR PARTITION FUNCTION ---
--- ENABLING TRIGGER FOR PURGING THE TABLES OLDER THAN Retention_Policy MONTHS ---

ddl_script := ddl_script ||
'CREATE OR REPLACE FUNCTION public.purge_' || table_name || '()
                        RETURNS trigger
                                        LANGUAGE ''plpgsql''
                                        COST 100
                                        VOLATILE
                                                AS $BODY$
                                                DECLARE
                                                startDate date;
                                                endDate date;
                                                tableNames text;
                                                dropScript text := ''DROP TABLE IF EXISTS '';
                                                partition text := '''|| table_name ||''';
                                                dates text[];
                                                i text;
                                                BEGIN
                                                        select max(' ||timestamp_column_name ||')-INTERVAL '''|| retention_policy + 1  ||' MONTHS'' from public.' || table_name || ' into endDate;
                                                        select  min(' || timestamp_column_name || ') from public.' || table_name ||' into startDate;
                                                        select array(select partition || ''_'' || to_char(GENERATE_SERIES( startDate::DATE,endDate::DATE, ''1 month'' ), ''YYYYMM'')) into dates;
                                                        IF array_length(dates,1) > 0 THEN
                                                        select array_to_string(dates, '','') into tableNames;
                                                        execute(dropScript || tableNames|| '';'');
                                                        END IF;
                                                        return NULL;
                                                END;
                                                $BODY$;';

PERFORM 1 FROM information_schema.triggers WHERE trigger_name = 'purge_' || table_name || '_trigger';
IF (NOT FOUND and retention_policy > 0) THEN
ddl_script := ddl_script ||
'CREATE TRIGGER purge_' || table_name || '_trigger AFTER INSERT ON public.' || table_name || ' FOR EACH STATEMENT EXECUTE PROCEDURE public.purge_' || table_name || '();';
ELSE
ddl_script := ddl_script || '-- Purging is disabled.';
END IF;
RETURN ddl_script;
END;
$function$;



CREATE OR REPLACE FUNCTION public.insertorupdateconstraint(p_constraintid character varying, p_constraints character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_Constraint(
        ConstraintId,
        "Constraints",
        DbUpdatedTimestamp)
    values(
        p_constraintid,
        p_constraints,
        p_dbupdatedtimestamp)
    on conflict(constraintid) do update set
        constraints = p_constraints,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;

CREATE OR REPLACE FUNCTION public.get_constraint_records() RETURNS SETOF public.tier2_constraint
    LANGUAGE sql
    AS $$
    select * from tier2_constraint;
$$;


CREATE PROCEDURE public.create_first_partition()
LANGUAGE plpgsql
AS $$
declare monthStart date := date_trunc('MONTH', current_timestamp);
declare monthEndExclusive date := monthStart + interval '1 MONTH';
declare tablepostfix text := to_char(current_timestamp, 'YYYYmm');
BEGIN
    execute format('create table %s partition of public.tier2_rasevent for values from (%L) to (%L)', 'public.tier2_rasevent_' || tablepostfix, monthStart, monthEndExclusive);
    execute format('create table %s partition of public.tier2_aggregatedenvdata for values from (%L) to (%L)', 'public.tier2_aggregatedenvdata_'|| tablepostfix, monthStart, monthEndExclusive);
END;
$$;


CREATE OR REPLACE FUNCTION public.insertorupdateserviceoperation(p_serviceoperationid bigint,
                                                                 p_lctn character varying,
                                                                 p_typeofserviceoperation character varying,
                                                                 p_userstartedservice character varying,
                                                                 p_userstoppedservice character varying,
                                                                 p_state character varying,
                                                                 p_status character varying,
                                                                 p_starttimestamp timestamp without time zone,
                                                                 p_stoptimestamp timestamp without time zone,
                                                                 p_startremarks character varying,
                                                                 p_stopremarks character varying,
                                                                 p_dbupdatedtimestamp timestamp without time zone,
                                                                 p_logfile character varying) RETURNS void
    LANGUAGE sql
    AS $$
    insert into Tier2_ServiceOperation_History(serviceoperationid,
        lctn,
        typeofserviceoperation,
        userstartedservice,
        userstoppedservice,
        state,
        status,
        starttimestamp,
        stoptimestamp,
        startremarks,
        stopremarks,
        dbupdatedtimestamp,
        logfile)
    values(
        p_serviceoperationid,
        p_lctn,
        p_typeofserviceoperation,
        p_userstartedservice,
        p_userstoppedservice,
        p_state,
        p_status,
        p_starttimestamp,
        p_stoptimestamp,
        p_startremarks,
        p_stopremarks,
        p_dbupdatedtimestamp,
        p_logfile)
    on conflict(lctn, dbupdatedtimestamp) do update set
        serviceoperationid = p_serviceoperationid,
        typeofserviceoperation = p_typeofserviceoperation,
        userstartedservice = p_userstartedservice,
        userstoppedservice = p_userstoppedservice,
        state = p_state,
        status = p_status,
        starttimestamp = p_starttimestamp,
        stoptimestamp = p_stoptimestamp,
        startremarks = p_startremarks,
        stopremarks = p_stopremarks,
        dbupdatedtimestamp = p_dbupdatedtimestamp,
        logfile = p_logfile;
$$;



CREATE OR REPLACE FUNCTION public.get_latest_switch_records() RETURNS SETOF public.tier2_switch_ss
LANGUAGE sql
AS $$
select *
from Tier2_Switch_SS ;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdateswitchdata_ss(p_lctn character varying, p_state character varying, p_sernum character varying, p_type character varying, p_owner character varying, p_dbupdatedtimestamp timestamp without time zone,
p_lastchgtimestamp timestamp without time zone, p_entrynumber bigint) RETURNS void
LANGUAGE sql
AS $$
insert into Tier2_switch_ss(
lctn, state, sernum, type, owner, dbupdatedtimestamp, lastchgtimestamp, entrynumber)
values(p_lctn, p_state, p_sernum, p_type, p_owner, p_dbupdatedtimestamp, p_lastchgtimestamp, p_entrynumber)
    on conflict(lctn) do update set
state=p_state, sernum=p_sernum, type=p_type, owner=p_owner, dbupdatedtimestamp=p_dbupdatedtimestamp,
lastchgtimestamp=p_lastchgtimestamp, entrynumber=p_entrynumber;
$$;

CREATE OR REPLACE FUNCTION public.get_processor_records() RETURNS SETOF public.tier2_processor_ss
LANGUAGE sql
AS $$
select *
from Tier2_processor_SS ;
$$;
CREATE OR REPLACE FUNCTION public.get_accelerator_records() RETURNS SETOF public.tier2_accelerator_ss
LANGUAGE sql
AS $$
select *
from Tier2_accelerator_SS ;
$$;
CREATE OR REPLACE FUNCTION public.get_hfi_records() RETURNS SETOF public.tier2_hfi_ss
LANGUAGE sql
AS $$
select *
from Tier2_hfi_SS ;
$$;
CREATE OR REPLACE FUNCTION public.get_dimm_records() RETURNS SETOF public.tier2_dimm_ss
LANGUAGE sql
AS $$
select *
from Tier2_dimm_SS ;
$$;

create or replace function public.insertorupdateprocessordata_ss(p_NodeLctn varchar, p_Lctn varchar, p_State varchar, p_SocketDesignation varchar,
p_DbUpdatedTimestamp timestamp without time zone, p_LastChgTimestamp timestamp without time zone, p_LastChgAdapterType varchar, p_LastChgWorkItemId bigint) returns void
LANGUAGE sql
AS $$
insert into Tier2_Processor_ss(NodeLctn , Lctn, State, SocketDesignation,
DbUpdatedTimestamp,LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId)
values (p_NodeLctn , p_Lctn, p_State, p_SocketDesignation,
p_DbUpdatedTimestamp, p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId)
on conflict(NodeLctn, Lctn) do update set
State = p_State, SocketDesignation = p_SocketDesignation,
DbUpdatedTimestamp = p_DbUpdatedTimestamp, LastChgAdapterType = p_LastChgAdapterType,
LastChgWorkItemId = p_LastChgWorkItemId, LastChgTimestamp=p_LastChgTimestamp;
$$;

create or replace function public.insertorupdateacceleratordata_ss(p_NodeLctn varchar, p_Lctn varchar, p_State varchar, p_BusAddr varchar, p_Slot varchar,
p_DbUpdatedTimestamp timestamp without time zone, p_LastChgTimestamp timestamp without time zone, p_LastChgAdapterType varchar, p_LastChgWorkItemId bigint) returns void
LANGUAGE sql
AS $$
insert into Tier2_accelerator_ss(NodeLctn , Lctn, State, BusAddr, Slot,
DbUpdatedTimestamp,LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId)
values (p_NodeLctn , p_Lctn, p_State, p_BusAddr, p_Slot,
p_DbUpdatedTimestamp, p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId)
    on conflict(NodeLctn, Lctn) do update set
State = p_State, BusAddr = p_BusAddr, Slot = p_Slot,
DbUpdatedTimestamp = p_DbUpdatedTimestamp, LastChgAdapterType = p_LastChgAdapterType,
LastChgWorkItemId = p_LastChgWorkItemId, LastChgTimestamp=p_LastChgTimestamp;
$$;

create or replace function public.insertorupdatehfidata_ss(p_NodeLctn varchar, p_Lctn varchar, p_State varchar, p_BusAddr varchar, p_Slot varchar,
p_DbUpdatedTimestamp timestamp without time zone, p_LastChgTimestamp timestamp without time zone, p_LastChgAdapterType varchar, p_LastChgWorkItemId bigint) returns void
LANGUAGE sql
AS $$
insert into Tier2_hfi_ss(NodeLctn , Lctn, State, BusAddr, Slot,
DbUpdatedTimestamp,LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId)
values (p_NodeLctn , p_Lctn,  p_State, p_BusAddr, p_Slot,
p_DbUpdatedTimestamp, p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId)
    on conflict(NodeLctn, Lctn) do update set
State = p_State, BusAddr = p_BusAddr, Slot = p_Slot,
DbUpdatedTimestamp = p_DbUpdatedTimestamp, LastChgAdapterType = p_LastChgAdapterType,
LastChgWorkItemId = p_LastChgWorkItemId, LastChgTimestamp=p_LastChgTimestamp;
$$;

create or replace function public.insertorupdatedimmdata_ss(p_NodeLctn varchar, p_Lctn varchar, p_State varchar, p_Sizemb bigint, p_ModuleLocator varchar, p_BankLocator varchar,
p_DbUpdatedTimestamp timestamp without time zone, p_LastChgTimestamp timestamp without time zone, p_LastChgAdapterType varchar, p_LastChgWorkItemId bigint) returns void
LANGUAGE sql
AS $$
insert into Tier2_dimm_ss(NodeLctn , Lctn, State, SizeMB, ModuleLocator, BankLocator,
DbUpdatedTimestamp,LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId)
values (p_NodeLctn , p_Lctn, p_State, p_Sizemb, p_ModuleLocator, p_BankLocator,
p_DbUpdatedTimestamp, p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId)
    on conflict(NodeLctn, Lctn) do update set
State = p_State, SizeMB=p_Sizemb, ModuleLocator= p_ModuleLocator, BankLocator = p_BankLocator,
DbUpdatedTimestamp = p_DbUpdatedTimestamp, LastChgAdapterType = p_LastChgAdapterType,
LastChgWorkItemId = p_LastChgWorkItemId, LastChgTimestamp=p_LastChgTimestamp;
$$;

--CREATE OR REPLACE FUNCTION public.insertorupdatediaghistory(diagid bigint,
--                                                            lctn character varying,
--                                                            serviceoperationid bigint,
--                                                            diag character varying,
--                                                            diagparameters character varying,
--                                                            state character varying(1),
--                                                            starttimestamp timestamp without time zone,
--                                                            endtimestamp timestamp without time zone,
--                                                            results character varying,
--                                                            dbupdatedtimestamp timestamp without time zone,
--                                                            lastchgtimestamp timestamp without time zone,
--                                                            lastchgadaptertype character varying,
--                                                            lastchgworkitemid bigint) RETURNS void
--    LANGUAGE sql
--    AS $$
--    insert into Tier2_Diag_History(
--        diagid,
--        lctn,
--        serviceoperationid,
--        diag,
--        diagparameters,
--        state,
--        starttimestamp,
--        endtimestamp,
--        results,
--        dbupdatedtimestamp,
--        lastchgtimestamp,
--        lastchgadaptertype,
--        lastchgworkitemid)
--    values(
--        p_diagid,
--        p_lctn,
--        p_serviceoperationid,
--        p_diag,
--        p_diagparameters,
--        p_state,
--        p_starttimestamp,
--        p_endtimestamp,
--        p_results,
--        p_dbupdatedtimestamp,
--        p_lastchgtimestamp,
--        p_lastchgadaptertype,
--        p_lastchgworkitemid)
--    on conflict(DiagListId) do update set
--        Description = p_description,
--        DefaultParameters = p_defaultparameters,
--        DbUpdatedTimestamp = p_dbupdatedtimestamp;
--$$;


----- ALTER TABLE SQLS START HERE ------

--
-- Name: tier2_nonnodehw_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_nonnodehw_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tier2_adapter_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_nonnodehw_history_entrynumber_seq OWNED BY public.tier2_nonnodehw_history.entrynumber;



--
-- Name: tier2_adapter_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_adapter_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_adapter_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_adapter_history_entrynumber_seq OWNED BY public.tier2_adapter_history.entrynumber;


--
-- Name: tier2_aggregatedenvdata_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_aggregatedenvdata_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_aggregatedenvdata_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_aggregatedenvdata_entrynumber_seq OWNED BY public.tier2_aggregatedenvdata.entrynumber;


--
-- Name: tier2_bootimage_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_bootimage_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_bootimage_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_bootimage_history_entrynumber_seq OWNED BY public.tier2_bootimage_history.entrynumber;


--
-- Name: tier2_chassis_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_chassis_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_chassis_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_chassis_history_entrynumber_seq OWNED BY public.tier2_chassis_history.entrynumber;


--
-- Name: tier2_computenode_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_computenode_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_computenode_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_computenode_history_entrynumber_seq OWNED BY public.tier2_computenode_history.entrynumber;


--
-- Name: tier2_fabrictopology_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_fabrictopology_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_fabrictopology_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_fabrictopology_history_entrynumber_seq OWNED BY public.tier2_fabrictopology_history.entrynumber;


--
-- Name: tier2_inventorysnapshot_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_inventorysnapshot_id_seq OWNED BY public.tier2_inventorysnapshot.id;


--
-- Name: tier2_job_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_job_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_job_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_job_history_entrynumber_seq OWNED BY public.tier2_job_history.entrynumber;


--
-- Name: tier2_job_power_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_job_power_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_job_power_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_job_power_entrynumber_seq OWNED BY public.tier2_job_power.entrynumber;


--
-- Name: tier2_jobstep_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_jobstep_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_jobstep_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_jobstep_history_entrynumber_seq OWNED BY public.tier2_jobstep_history.entrynumber;


--
-- Name: tier2_lustre_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_lustre_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_lustre_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_lustre_history_entrynumber_seq OWNED BY public.tier2_lustre_history.entrynumber;


--
-- Name: tier2_machine_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_machine_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_machine_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_machine_history_entrynumber_seq OWNED BY public.tier2_machine_history.entrynumber;


--
-- Name: tier2_rack_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_rack_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_rack_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_rack_history_entrynumber_seq OWNED BY public.tier2_rack_history.entrynumber;


--
-- Name: tier2_rasevent_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_rasevent_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_rasevent_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_rasevent_entrynumber_seq OWNED BY public.tier2_rasevent.entrynumber;

CREATE SEQUENCE public.tier2_rasevent_ss_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_rasevent_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_rasevent_ss_entrynumber_seq OWNED BY public.tier2_rasevent_ss.entrynumber;



--
-- Name: tier2_rasmetadata_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_rasmetadata_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_rasmetadata_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_rasmetadata_entrynumber_seq OWNED BY public.tier2_rasmetadata.entrynumber;


--
-- Name: tier2_replacement_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_replacement_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_replacement_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_replacement_history_entrynumber_seq OWNED BY public.tier2_replacement_history.entrynumber;


--
-- Name: tier2_servicenode_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_servicenode_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_servicenode_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_servicenode_history_entrynumber_seq OWNED BY public.tier2_servicenode_history.entrynumber;


--
-- Name: tier2_serviceoperation_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_serviceoperation_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_serviceoperation_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_serviceoperation_history_entrynumber_seq OWNED BY public.tier2_serviceoperation_history.entrynumber;


--
-- Name: tier2_switch_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_switch_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_switch_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_switch_history_entrynumber_seq OWNED BY public.tier2_switch_history.entrynumber;


--
-- Name: tier2_wlmreservation_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_wlmreservation_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_wlmreservation_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_wlmreservation_history_entrynumber_seq OWNED BY public.tier2_wlmreservation_history.entrynumber;


--
-- Name: tier2_workitem_history_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_workitem_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_workitem_history_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_workitem_history_entrynumber_seq OWNED BY public.tier2_workitem_history.entrynumber;


CREATE SEQUENCE public.tier2_nodeinventory_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.tier2_nodeinventory_history_entrynumber_seq OWNED BY public.tier2_nodeinventory_history.entrynumber;
ALTER TABLE ONLY public.tier2_nodeinventory_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_nodeinventory_history_entrynumber_seq'::regclass);


CREATE SEQUENCE public.tier2_nonodehw_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tier2_nonodehw_history_entrynumber_seq OWNED BY public.tier2_nonnodehw_history.entrynumber;
ALTER SEQUENCE public.tier2_adapter_history_entrynumber_seq OWNED BY public.tier2_adapter_history.entrynumber;


--
-- Name: tier2_adapter_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_adapter_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_adapter_history_entrynumber_seq'::regclass);
ALTER TABLE ONLY public.tier2_nonnodehw_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_nonodehw_history_entrynumber_seq'::regclass);


--
-- Name: tier2_aggregatedenvdata entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_aggregatedenvdata ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_aggregatedenvdata_entrynumber_seq'::regclass);


--
-- Name: tier2_bootimage_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_bootimage_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_bootimage_history_entrynumber_seq'::regclass);


--
-- Name: tier2_chassis_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_chassis_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_chassis_history_entrynumber_seq'::regclass);


--
-- Name: tier2_computenode_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_computenode_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_computenode_history_entrynumber_seq'::regclass);


--
-- Name: tier2_fabrictopology_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_fabrictopology_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_fabrictopology_history_entrynumber_seq'::regclass);


--
-- Name: tier2_inventorysnapshot id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_inventorysnapshot ALTER COLUMN id SET DEFAULT nextval('public.tier2_inventorysnapshot_id_seq'::regclass);


--
-- Name: tier2_job_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_job_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_job_history_entrynumber_seq'::regclass);


--
-- Name: tier2_job_power entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_job_power ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_job_power_entrynumber_seq'::regclass);


--
-- Name: tier2_jobstep_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_jobstep_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_jobstep_history_entrynumber_seq'::regclass);


--
-- Name: tier2_lustre_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_lustre_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_lustre_history_entrynumber_seq'::regclass);


--
-- Name: tier2_machine_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_machine_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_machine_history_entrynumber_seq'::regclass);


--
-- Name: tier2_rack_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_rack_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_rack_history_entrynumber_seq'::regclass);


--
-- Name: tier2_rasevent entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_rasevent ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_rasevent_entrynumber_seq'::regclass);
ALTER TABLE ONLY public.tier2_rasevent_ss ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_rasevent_ss_entrynumber_seq'::regclass);


--
-- Name: tier2_rasmetadata entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_rasmetadata ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_rasmetadata_entrynumber_seq'::regclass);


--
-- Name: tier2_replacement_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_replacement_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_replacement_history_entrynumber_seq'::regclass);


--
-- Name: tier2_servicenode_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_servicenode_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_servicenode_history_entrynumber_seq'::regclass);


--
-- Name: tier2_serviceoperation_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_serviceoperation_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_serviceoperation_history_entrynumber_seq'::regclass);


--
-- Name: tier2_switch_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_switch_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_switch_history_entrynumber_seq'::regclass);


--
-- Name: tier2_wlmreservation_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_wlmreservation_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_wlmreservation_history_entrynumber_seq'::regclass);


--
-- Name: tier2_workitem_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_workitem_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_workitem_history_entrynumber_seq'::regclass);


--
-- Data for Name: tier2_adapter_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_adapter_history (id, adaptertype, sconrank, state, dbupdatedtimestamp, lastchgadaptertype, lastchgworkitemid, lctn, pid, entrynumber) FROM stdin;
\.


--
-- Name: tier2_adapter_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_adapter_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_aggregatedenvdata; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_aggregatedenvdata (lctn, "timestamp", type, maximumvalue, minimumvalue, averagevalue, adaptertype, workitemid, entrynumber) FROM stdin;
\.


--
-- Name: tier2_aggregatedenvdata_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_aggregatedenvdata_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_bootimage_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_bootimage_history (id, description, bootimagefile, bootimagechecksum, bootoptions, bootstrapimagefile, bootstrapimagechecksum, state, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, kernelargs, files, entrynumber) FROM stdin;
\.


--
-- Name: tier2_bootimage_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_bootimage_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_chassis_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_chassis_history (lctn, state, sernum, type, vpd, owner, dbupdatedtimestamp, lastchgtimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_chassis_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_chassis_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_computenode_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_computenode_history (lctn, sequencenumber, state, hostname, bootimageid, environment, ipaddr, macaddr, bmcipaddr, bmcmacaddr, bmchostname, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp, wlmnodestate, entrynumber) FROM stdin;
\.


--
-- Name: tier2_computenode_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_computenode_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_config (key, value, description) FROM stdin;
schema_version	1.0	Current schema version in the following format: <major>.<minor>.<revision>
tier2_valid	false	Indicates whether the state of this tier 2 database is valid to use for initializing tier 1
\.


--
-- Data for Name: tier2_diag_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_diag_history (diagid, lctn, serviceoperationid, diag, diagparameters, state, starttimestamp, endtimestamp, results, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid) FROM stdin;
\.


--
-- Data for Name: tier2_diag_list; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_diag_list (diaglistid, diagtoolid, description, defaultparameters, dbupdatedtimestamp) FROM stdin;
\.


--
-- Data for Name: tier2_diag_tools; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_diag_tools (diagtoolid, description, unittype, unitsize, provisionreqd, rebootbeforereqd, rebootafterreqd, dbupdatedtimestamp) FROM stdin;
\.


--
-- Data for Name: tier2_fabrictopology_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_fabrictopology_history (dbupdatedtimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_fabrictopology_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_fabrictopology_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_inventorysnapshot; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_inventorysnapshot (lctn, snapshottimestamp, inventoryinfo, id, reference) FROM stdin;
\.


--
-- Name: tier2_inventorysnapshot_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_inventorysnapshot_id_seq', 1, false);


--
-- Data for Name: tier2_job_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_job_history (jobid, jobname, state, bsn, numnodes, nodes, powercap, username, executable, initialworkingdir, arguments, environmentvars, starttimestamp, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, endtimestamp, exitstatus, jobacctinfo, powerused, wlmjobstate, entrynumber) FROM stdin;
\.


--
-- Name: tier2_job_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_job_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_job_power; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_job_power (jobid, lctn, jobpowertimestamp, profile, totalruntime, totalpackageenergy, totaldramenergy, entrynumber) FROM stdin;
\.


--
-- Name: tier2_job_power_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_job_power_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_jobstep_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_jobstep_history (jobid, jobstepid, state, numnodes, nodes, numprocessespernode, executable, initialworkingdir, arguments, environmentvars, mpimapping, starttimestamp, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, endtimestamp, exitstatus, wlmjobstepstate, entrynumber) FROM stdin;
\.


--
-- Name: tier2_jobstep_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_jobstep_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_lustre_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_lustre_history (dbupdatedtimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_lustre_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_lustre_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_machine_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_machine_history (sernum, description, type, numrows, numcolsinrow, numchassisinrack, state, clockfreq, manifestlctn, manifestcontent, dbupdatedtimestamp, usingsynthesizeddata, entrynumber) FROM stdin;
\.


--
-- Name: tier2_machine_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_machine_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_machineadapterinstance_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_machineadapterinstance_history (snlctn, adaptertype, numinitialinstances, numstartedinstances, invocation, logfile, dbupdatedtimestamp) FROM stdin;
\.


--
-- Data for Name: tier2_rack_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_rack_history (lctn, state, sernum, type, vpd, owner, dbupdatedtimestamp, lastchgtimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_rack_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_rack_history_entrynumber_seq', 1, false);


--
-- Name: tier2_rasevent_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_rasevent_entrynumber_seq', 1, false);



--
-- Name: tier2_rasmetadata_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_rasmetadata_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_replacement_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_replacement_history (lctn, frutype, serviceoperationid, oldsernum, newsernum, oldstate, newstate, dbupdatedtimestamp, lastchgtimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_replacement_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_replacement_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_servicenode_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_servicenode_history (lctn, sequencenumber, hostname, state, bootimageid, ipaddr, macaddr, bmcipaddr, bmcmacaddr, bmchostname, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_servicenode_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_servicenode_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_serviceoperation_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_serviceoperation_history (serviceoperationid, lctn, typeofserviceoperation, userstartedservice, userstoppedservice, state, status, starttimestamp, stoptimestamp, startremarks, stopremarks, dbupdatedtimestamp, logfile, entrynumber) FROM stdin;
\.


--
-- Name: tier2_serviceoperation_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_serviceoperation_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_switch_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_switch_history (lctn, state, sernum, type, owner, dbupdatedtimestamp, lastchgtimestamp, entrynumber) FROM stdin;
\.


--
-- Name: tier2_switch_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_switch_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_ucsconfigvalue; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_ucsconfigvalue (key, value, dbupdatedtimestamp) FROM stdin;
\.


--
-- Data for Name: tier2_uniquevalues; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_uniquevalues (entity, nextvalue, dbupdatedtimestamp) FROM stdin;
\.


--
-- Data for Name: tier2_wlmreservation_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_wlmreservation_history (reservationname, users, nodes, starttimestamp, endtimestamp, deletedtimestamp, lastchgtimestamp, dbupdatedtimestamp, lastchgadaptertype, lastchgworkitemid, entrynumber) FROM stdin;
\.


--
-- Name: tier2_wlmreservation_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_wlmreservation_history_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_workitem_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_workitem_history (queue, workingadaptertype, id, worktobedone, parameters, notifywhenfinished, state, requestingworkitemid, requestingadaptertype, workingadapterid, workingresults, results, starttimestamp, dbupdatedtimestamp, endtimestamp, rowinsertedintohistory, entrynumber) FROM stdin;
\.


--
-- Name: tier2_workitem_history_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_workitem_history_entrynumber_seq', 1, false);


--
-- Name: tier2_diag_list diag_list_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_diag_list
    ADD CONSTRAINT diag_list_pkey PRIMARY KEY (diaglistid);


--
-- Name: tier2_diag_tools diag_tools_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_diag_tools
    ADD CONSTRAINT diag_tools_pkey PRIMARY KEY (diagtoolid);


--
-- Name: tier2_aggregatedenvdata tier2_aggregatedenvdata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_aggregatedenvdata
    ADD CONSTRAINT tier2_aggregatedenvdata_pkey PRIMARY KEY (lctn, type, "timestamp");


--
-- Name: tier2_config tier2_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_config
    ADD CONSTRAINT tier2_config_pkey PRIMARY KEY (key);


--
-- Name: tier2_inventorysnapshot tier2_inventorysnapshot_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_inventorysnapshot
    ADD CONSTRAINT tier2_inventorysnapshot_pkey PRIMARY KEY (lctn, snapshottimestamp);


--
-- Name: tier2_rasevent tier2_rasevent_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--


ALTER TABLE ONLY public.tier2_rasevent_ss
    ADD CONSTRAINT tier2_rasevent_ss_pkey PRIMARY KEY (descriptivename, id);

--
-- Name: tier2_ucsconfigvalue ucsconfigvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_ucsconfigvalue_ss
    ADD CONSTRAINT ucsconfigvalue_ss_pkey PRIMARY KEY (key);

ALTER TABLE ONLY public.tier2_ucsconfigvalue
    ADD CONSTRAINT ucsconfigvalue_pkey PRIMARY KEY (key);


--
-- Name: tier2_uniquevalues uniquevalues_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_uniquevalues_ss
    ADD CONSTRAINT uniquevalues_ss_pkey PRIMARY KEY (entity);

ALTER TABLE ONLY public.tier2_uniquevalues
    ADD CONSTRAINT uniquevalues_pkey PRIMARY KEY (entity);



--
-- Name: aggregatedenvdata_timelctn; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aggregatedenvdata_timelctn ON public.tier2_aggregatedenvdata USING btree ("timestamp", lctn);


--
-- Name: computenode_dbupdatedtime; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX computenode_dbupdatedtime ON public.tier2_computenode_history USING btree (dbupdatedtimestamp);


--
-- Name: computenode_lastchgtimelctn; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX computenode_lastchgtimelctn ON public.tier2_computenode_history USING btree (lastchgtimestamp, lctn);


--
-- Name: computenode_seqnumdbupdatedtime; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX computenode_seqnumdbupdatedtime ON public.tier2_computenode_history USING btree (sequencenumber, dbupdatedtimestamp);


--
-- Name: diag_endtimediagid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX diag_endtimediagid ON public.tier2_diag_history USING btree (endtimestamp, diagid);


--
-- Name: diag_startendtimediagid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX diag_startendtimediagid ON public.tier2_diag_history USING btree (starttimestamp, endtimestamp, diagid);


--
-- Name: jobhistory_dbupdatedtime; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jobhistory_dbupdatedtime ON public.tier2_job_history USING btree (dbupdatedtimestamp);


--
-- Name: jobhistory_lastchgtime; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX jobhistory_lastchgtime ON public.tier2_job_history USING btree (lastchgtimestamp);

--
-- Name: machinehistory_dbupdatedtimestamp; Type: INDEX; Schema: public; Owner: ucsadmin
--

CREATE INDEX machinehistory_dbupdatedtimestamp ON public.tier2_machine_history USING btree (dbupdatedtimestamp DESC);

--
-- Name: rasevent_dbupdatedtime; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX rasevent_dbupdatedtime ON public.tier2_rasevent USING btree (dbupdatedtimestamp);


CREATE INDEX rasevent_descriptivename_id ON public.tier2_rasevent USING btree (descriptivename, id);
CREATE INDEX raseventdbupdatedtimestampdescriptivename ON public.tier2_rasevent USING btree (dbupdatedtimestamp desc, descriptivename);
CREATE INDEX raseventlasttimestampdescriptivename ON public.tier2_rasevent USING btree (lastchgtimestamp desc, descriptivename);
CREATE INDEX raseventlasttimestamp ON public.tier2_rasevent USING btree (lastchgtimestamp desc);

--
-- Name: rasevent_dbupdatedtimeeventtypeid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX rasevent_dbupdatedtimedescriptivenameid ON public.tier2_rasevent USING btree (dbupdatedtimestamp DESC, descriptivename, id);


--
-- Name: rasmetadata_eventtype; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX rasmetadata_descriptivename ON public.tier2_rasmetadata USING btree (DescriptiveName);

--
-- Name: tier2_bootimage_ss bootimage_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--

ALTER TABLE ONLY public.tier2_bootimage_ss
    ADD CONSTRAINT bootimage_ss_pkey PRIMARY KEY (id);


--
-- Name: tier2_chassis_ss tier2_chassis_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--

ALTER TABLE ONLY public.tier2_chassis_ss
    ADD CONSTRAINT tier2_chassis_ss_pkey PRIMARY KEY (lctn);


--
-- Name: tier2_computenode_ss tier2_computenode_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--

ALTER TABLE ONLY public.tier2_computenode_ss
    ADD CONSTRAINT tier2_computenode_ss_pkey PRIMARY KEY (lctn);

--
-- Name: tier2_machine_ss tier2_machine_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--

ALTER TABLE ONLY public.tier2_machine_ss
    ADD CONSTRAINT tier2_machine_ss_pkey PRIMARY KEY (sernum);


--
-- Name: tier2_rack_ss tier2_rack_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--

ALTER TABLE ONLY public.tier2_rack_ss
    ADD CONSTRAINT tier2_rack_ss_pkey PRIMARY KEY (lctn);


--
-- Name: tier2_servicenode_ss tier2_servicenode_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--

ALTER TABLE ONLY public.tier2_servicenode_ss
    ADD CONSTRAINT tier2_servicenode_ss_pkey PRIMARY KEY (lctn);


--
-- Name: tier2_uniquevalues_ss uniquevalues_ss_pkey; Type: CONSTRAINT; Schema: public; Owner:
--


ALTER TABLE ONLY public.tier2_diag_tools_ss
    ADD CONSTRAINT diag_tools_ss_pkey PRIMARY KEY (diagtoolid);


ALTER TABLE ONLY public.tier2_serviceoperation_history
    ADD CONSTRAINT serviceoperation_history_pkey PRIMARY KEY (lctn,dbupdatedtimestamp);


--
-- Name: adapterssbyadaptertypeandid; Type: INDEX; Schema: public; Owner:
--

CREATE UNIQUE INDEX adapterssbyadaptertypeandid ON public.tier2_adapter_ss USING btree (adaptertype, id);

--
-- Name: machineadapterinstancebysnlctnandadaptertype; Type: INDEX; Schema: public; Owner:
--

CREATE UNIQUE INDEX machineadapterinstancebysnlctnandadaptertype ON public.tier2_machineadapterinstance_ss USING btree (snlctn, adaptertype);

--
-- Name: workitembyadaptertypeandid; Type: INDEX; Schema: public; Owner:
--

CREATE UNIQUE INDEX workitembyadaptertypeandid ON public.tier2_workitem_ss USING btree (workingadaptertype, id);

CREATE INDEX computenode_lctnlastchgtime ON public.tier2_computenode_history USING btree (lctn, lastchgtimestamp desc);

COPY public.tier2_authorized_user (id, userid, roleid) FROM stdin;
1	root	ucs-admin
2	anpatel1	ucs-admin
3	lrrountr	ucs-admin
4	avincigu	ucs-admin
5	nsai	ucs-admin
\.

call public.create_first_partition();

CREATE OR REPLACE FUNCTION public.automatic_rule_creation() RETURNS VOID
AS $$
DECLARE
    rasevent_t text;
    aggenv_t text;
BEGIN
    select into rasevent_t public.generate_partition_purge_rules('tier2_rasevent', 'dbupdatedtimestamp', 6, FALSE);
    select into aggenv_t public.generate_partition_purge_rules('tier2_aggregatedenvdata', 'timestamp', 6, FALSE);
    execute (rasevent_t);
    execute (aggenv_t);
END $$
language plpgsql;

select public.automatic_rule_creation();

CREATE SEQUENCE public.tier2_dimm_history_entrynumber_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE SEQUENCE public.tier2_accelerator_history_entrynumber_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE SEQUENCE public.tier2_processor_history_entrynumber_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE SEQUENCE public.tier2_hfi_history_entrynumber_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

ALTER SEQUENCE public.tier2_dimm_history_entrynumber_seq OWNED BY public.tier2_dimm_history.entrynumber;
ALTER SEQUENCE public.tier2_accelerator_history_entrynumber_seq OWNED BY public.tier2_accelerator_history.entrynumber;
ALTER SEQUENCE public.tier2_processor_history_entrynumber_seq OWNED BY public.tier2_processor_history.entrynumber;
ALTER SEQUENCE public.tier2_hfi_history_entrynumber_seq OWNED BY public.tier2_hfi_history.entrynumber;


ALTER TABLE ONLY public.tier2_dimm_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_dimm_history_entrynumber_seq'::regclass);
ALTER TABLE ONLY public.tier2_accelerator_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_accelerator_history_entrynumber_seq'::regclass);
ALTER TABLE ONLY public.tier2_processor_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_processor_history_entrynumber_seq'::regclass);
ALTER TABLE ONLY public.tier2_hfi_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_hfi_history_entrynumber_seq'::regclass);


SELECT pg_catalog.setval('public.tier2_dimm_history_entrynumber_seq', 1, false);
SELECT pg_catalog.setval('public.tier2_accelerator_history_entrynumber_seq', 1, false);
SELECT pg_catalog.setval('public.tier2_processor_history_entrynumber_seq', 1, false);
SELECT pg_catalog.setval('public.tier2_hfi_history_entrynumber_seq', 1, false);

--
-- PostgreSQL database dump complete
--
CREATE OR REPLACE FUNCTION public.IsComponentInCurrentInventorySnapshotRaw(
    p_fru_id varchar
) RETURNS boolean
AS
$$
DECLARE
    last_inserted varchar;
    last_deleted  varchar;
BEGIN
    EXECUTE FORMAT('SELECT LastTimestampOfActionOnFruRaw(''INSERTED'', ''%s'')', p_fru_id) INTO last_inserted;
    EXECUTE FORMAT('SELECT LastTimestampOfActionOnFruRaw(''DELETED'', ''%s'')', p_fru_id) INTO last_deleted;
    RETURN last_inserted > last_deleted;
END
$$ LANGUAGE plpgsql;


-- Trying to get the slot address will require a lot more coding.
-- Fru ids are assumed to be globally unique.
CREATE OR REPLACE FUNCTION public.ComponentInCurrentInventorySnapshot(p_node_sernum varchar
                                                                     , p_fru_id varchar)
    RETURNS
        TABLE
        (
            inventory_timestamp timestamp without time zone,
            node_location       character varying,
            node_serial_number  character varying
        )
AS
$$
DECLARE
    node_sernum varchar := coalesce(p_node_sernum, 'ANY');
    fru_id varchar := coalesce(p_fru_id, 'ANY');
    fru_id_json_value varchar := '"' || fru_id || '"';
BEGIN
    RETURN QUERY
        SELECT t.inventory_timestamp
             , t.node_location
             , t.node_serial_number
        FROM CurrentInventorySnapshotAt(p_lctn_prefix := NULL) AS t
        WHERE (node_sernum = 'ANY' OR node_sernum = t.node_serial_number)
          AND (fru_id = 'ANY' OR position(fru_id_json_value in t.InventoryInfo::text) > 0);
    RETURN;
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION public.IsComponentInCurrentInventorySnapshot(p_node_sernum varchar
                                                                       , p_fru_id varchar) RETURNS boolean
AS
$$
DECLARE
    node_sernum varchar := coalesce(p_node_sernum, 'ANY');
    fru_id varchar := coalesce(p_fru_id, 'ANY');
    cnt bigint;
BEGIN
    EXECUTE FORMAT('SELECT COUNT(*) FROM ComponentInCurrentInventorySnapshot(''%s'', ''%s'')',
                   node_sernum, fru_id) into cnt;
    RETURN cnt > 0;
END
$$ LANGUAGE plpgsql;


-- List of node locations that hosted the node with p_node_sernum.  This stored procedure only necessary if
-- Sernum is not the same as the node fru id.
CREATE OR REPLACE FUNCTION public.MigrationHistoryOfNode(p_node_sernum varchar
                                                        , p_start_time timestamp without time zone
                                                        , p_end_time timestamp without time zone
                                                        , p_limit integer)
    RETURNS
        TABLE
        (
            inventory_timestamp timestamp without time zone,
            node_serial_number  character varying,
            node_location       varchar
        )
AS
$$
DECLARE
    start_time timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time   timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
BEGIN
    RETURN QUERY
        SELECT t.InventoryTimestamp
             , t.Sernum AS node_serial_number
             , t.Lctn   AS node_location
        FROM public.tier2_nodeinventory_history AS t
        WHERE t.Sernum = p_node_sernum
          AND t.dbupdatedtimestamp <= end_time
          AND t.dbupdatedtimestamp >= start_time
        ORDER BY t.InventoryTimestamp DESC, node_serial_number, node_location
        LIMIT p_limit;
    RETURN;
END
$$ LANGUAGE plpgsql;


-- Occupation history of an fru in the p_slot_loc of any machine in the HPC.
-- This needs to be invoked once for each slot of a typical node in order to reconstruct the full occupation
-- history of the fru.
CREATE OR REPLACE FUNCTION public.OccupationHistoryOfFruInSlot(p_fru_id varchar
                                                              , p_slot_loc varchar
                                                              , p_start_time timestamp without time zone
                                                              , p_end_time timestamp without time zone
                                                              , p_limit integer)
    RETURNS
        TABLE
        (
            InventoryTimestamp      timestamp without time zone,
            component_serial_number text,
            node_serial_number      character varying,
            node_location           character varying,
            component_location      text
        )
AS
$$
DECLARE
    json_fru_id_path           varchar   := format('((t.InventoryInfo->''HWInfo'')->''fru/%s/fru_id'')', p_slot_loc);
    json_fru_id_value          varchar   := format('(%s::json->>''value'')', json_fru_id_path);
    json_fru_loc_path          varchar   := format('((t.InventoryInfo->''HWInfo'')->''fru/%s/loc'')', p_slot_loc);
    json_fru_loc_value         varchar   := format('(%s::json->>''value'')', json_fru_loc_path);
    target_json_element_fru_id jsonb     := format('{"value": "%s"}', p_fru_id);
    start_time                 timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time                   timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
    max_records                varchar   := coalesce(to_char(p_limit, '9999999'), 'ALL');
BEGIN
    RETURN QUERY EXECUTE format(
                                                    'SELECT InventoryTimestamp' ||
                                                    ', %s AS component_serial_number' ||
                                                    ', Sernum AS node_serial_number' ||
                                                    ', Lctn As node_location' ||
                                                    ', %s AS component_location' ||
                                                    ' FROM public.tier2_nodeinventory_history AS t' ||
                                                    ' WHERE %s @> ''%s''' ||
                                                    ' AND t.dbupdatedtimestamp >= ''%s''' ||
                                                    ' AND t.dbupdatedtimestamp <= ''%s''' ||
                                                    ' ORDER BY InventoryTimestamp DESC, node_location ASC, component_location ASC' ||
                                                    ' LIMIT %s',
                                                    json_fru_id_value, json_fru_loc_value, json_fru_id_path,
                                                    target_json_element_fru_id
        , start_time, end_time, max_records);
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION public.MigrationHistoryOfFru(p_start_time timestamp without time zone
                                                       , p_end_time timestamp without time zone
                                                       , p_fru_id varchar
                                                       , p_limit integer)
    RETURNS
        TABLE
        (
            inventory_timestamp     timestamp without time zone,
            node_location           character varying,
            component_location      text,
            node_serial_number      character varying,
            component_serial_number text
        )
AS
$$
DECLARE
    arr_split_data text[];
    slot           varchar;
    start_time     timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time       timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
    max_records    integer   := coalesce(p_limit, 9999999);
BEGIN
    SELECT INTO arr_split_data regexp_split_to_array(
                                                   'DIMM0,DIMM1,DIMM2,DIMM3,DIMM4,DIMM5,DIMM6,DIMM7,DIMM8,DIMM9,DIMM10,DIMM11' ||
                                                   ',CPU0,CPU1' ||
                                                   ',DRIVE0,DRIVE1,DRIVE2' ||
                                                   ',NODE', ',');
    FOREACH slot IN array arr_split_data
        LOOP
            RETURN QUERY EXECUTE format(
                                        'SELECT InventoryTimestamp' ||
                                        ', node_location' ||
                                        ', component_location' ||
                                        ', node_serial_number' ||
                                        ', component_serial_number' ||
                                        ' FROM OccupationHistoryOfFruInSlot(''%s'', ''%s'', ''%s'', ''%s'', %s);'
                , p_fru_id, slot, start_time, end_time, max_records);
        END LOOP;
    RETURN;
END
$$ LANGUAGE plpgsql;


-- Occupation history at the node location
CREATE OR REPLACE FUNCTION public.OccupationHistoryAtNodeLocation(p_lctn varchar
                                                                 , p_start_time timestamp without time zone
                                                                 , p_end_time timestamp without time zone
                                                                 , p_limit integer)
    RETURNS
        TABLE
        (
            inventory_timestamp timestamp without time zone,
            node_location       character varying,
            Sernum              character varying
        )
AS
$$
DECLARE
    start_time timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time   timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
BEGIN
    RETURN QUERY
        SELECT t.InventoryTimestamp
             , t.Lctn   As node_location
             , t.Sernum AS node_serial_number
        FROM public.tier2_nodeinventory_history AS t
        WHERE Lctn = p_lctn
          AND t.dbupdatedtimestamp >= start_time
          AND t.dbupdatedtimestamp <= end_time
        ORDER BY t.InventoryTimestamp DESC, t.Sernum
        LIMIT p_limit;
    RETURN;
END
$$ LANGUAGE plpgsql;


-- Occupation history of the specified relative fru location in the specified node location.
--   The fru location specification is analogous to specified a file location as directory path containing the file,
--   and relative path of the file within the directory.
CREATE OR REPLACE FUNCTION public.OccupationHistoryAtComponentLocation(p_node_loc varchar -- node that contains the fru slot
                                                                      , p_slot_loc varchar -- slot in the specified node
                                                                      , p_start_time timestamp without time zone
                                                                      , p_end_time timestamp without time zone
                                                                      , p_limit integer)
    RETURNS
        TABLE
        (
            inventory_timestamp timestamp without time zone -- timestamp of fru occupancy of the specified slot
            ,
            component_location  text,
            component_fru_id    text                        -- fru in the slot at the time specified by the timestamp
        )
AS
$$
DECLARE
    json_fru_id_path   varchar   := format('((t.InventoryInfo->''HWInfo'')->''fru/%s/fru_id'')', p_slot_loc);
    json_fru_id_value  varchar   := format('(%s::json->>''value'')', json_fru_id_path);
    json_fru_loc_path  varchar   := format('((t.InventoryInfo->''HWInfo'')->''fru/%s/loc'')', p_slot_loc);
    json_fru_loc_value varchar   := format('(%s::json->>''value'')', json_fru_loc_path);
    start_time         timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time           timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
    max_records        integer   := coalesce(p_limit, 9999999);
BEGIN
    RETURN QUERY EXECUTE format(
            'SELECT InventoryTimestamp
                , %s AS component_location
                , %s AS component_fru_id
            FROM public.tier2_nodeinventory_history AS t
            WHERE Lctn = ''%s''
                AND t.dbupdatedtimestamp >= ''%s''
                AND t.dbupdatedtimestamp <= ''%s''
            ORDER BY t.InventoryTimestamp DESC, component_fru_id ASC
            LIMIT %s',
            json_fru_loc_value, json_fru_id_value, p_node_loc
        , start_time, end_time, max_records);
    RETURN;
END;
$$ LANGUAGE plpgsql;


-- -- Name: GetRawReplacementHistoryForLctn(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
-- --
CREATE OR REPLACE FUNCTION public.RawReplacementHistoryForLctn(p_lctn character varying
                                                              , p_start_time timestamp without time zone
                                                              , p_end_time timestamp without time zone
                                                              , p_limit integer)
    RETURNS TABLE
            (
                foreign_timestamp  character varying,
                action             character varying,
                component_location character varying,
                component_fru_id   character varying
            )
AS
$$
DECLARE
    lctn     varchar   := coalesce(p_lctn, '');
    end_time timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
BEGIN
    return query
        SELECT t.foreign_timestamp
             , t.action
             , t.component_location
             , t.component_fru_id
        FROM NoDuplicateRawReplacementHistory(p_start_time := p_start_time, p_end_time := end_time) AS t
        WHERE (lctn = '' OR lctn = t.component_location OR position(lctn || '-' in t.component_location) = 1)
        ORDER BY t.foreign_timestamp DESC, t.component_location, t.action, t.component_fru_id
        LIMIT p_limit;
    RETURN;
END
$$
    LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION public.NoDuplicateRawReplacementHistory(p_start_time timestamp without time zone
                                                                  , p_end_time timestamp without time zone)
    RETURNS TABLE
            (
                foreign_timestamp  character varying,
                Action             character varying,
                component_location character varying,
                component_fru_id   character varying
            )
AS
$$
DECLARE
    start_time timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time   timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
BEGIN
    return query
        SELECT DISTINCT t.ForeignTimestamp
                      , t.Action
                      , t.id    AS component_location
                      , t.fruid as component_fru_id
        FROM public.tier2_RawHWInventory_History AS t
        WHERE t.dbupdatedtimestamp >= start_time
          AND t.dbupdatedtimestamp <= end_time;
    RETURN;
END
$$
    LANGUAGE plpgsql;


-- This is for testing only.
CREATE OR REPLACE FUNCTION public.MigrationHistoryOfFruIndirectInclusion(p_fru_id varchar
                                                                        , p_start_time timestamp without time zone
                                                                        , p_end_time timestamp without time zone
                                                                        , p_limit integer)
    RETURNS
        TABLE
        (
            inventory_timestamp     timestamp without time zone,
            component_serial_number text,
            node_serial_number      character varying,
            node_location           character varying,
            component_location      text
        )
AS
$$
DECLARE
    arr_split_data text[];
    slot           varchar;
    start_time     timestamp := coalesce(p_start_time, to_timestamp('0001', 'YYYY'));
    end_time       timestamp := coalesce(p_end_time, to_timestamp('9999', 'YYYY'));
    max_records    integer   := coalesce(p_limit, 9999999);
BEGIN
    SELECT INTO arr_split_data regexp_split_to_array(
                                                   'DIMM0,DIMM1,DIMM2,DIMM3,DIMM4,DIMM5,DIMM6,DIMM7,DIMM8,DIMM9,DIMM10,DIMM11' ||
                                                   ',CPU0,CPU1' ||
                                                   ',DRIVE0,DRIVE1,DRIVE2' ||
                                                   ',NODE', ',');
    FOREACH slot IN array arr_split_data
        LOOP
            RETURN QUERY EXECUTE format(
                    'SELECT * FROM OccupationHistoryOfFruInHWInfo(''%s'', ''%s'', ''%s'', ''%s'', %s);'
                , p_fru_id, slot, start_time, end_time, max_records);
        END LOOP;
    RETURN;
END
$$ LANGUAGE plpgsql;

--------- New Inventory Stored Procedures End


call public.create_first_partition();

--
-- PostgreSQL database dump complete
--
