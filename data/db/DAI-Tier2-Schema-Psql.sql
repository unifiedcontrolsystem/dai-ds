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
	lctn character varying(25),
	lastchgtimestamp timestamp without time zone,
	inventoryinfo character varying(16384)
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
	eventtype character varying(65),
	lastchgtimestamp timestamp without time zone,
	dbupdatedtimestamp timestamp without time zone,
	severity character varying(10),
	lctn character varying(100),
	jobid character varying(30),
	controloperation character varying(50),
	msg character varying(1000),
	instancedata character varying(500)
);

CREATE TYPE public.system_summary_count AS (
    state character varying(1),
    count bigint
);

--
-- Name: raseventtypewithdescriptivename; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.raseventtypewithdescriptivename AS (
	eventtype character varying(10),
	descriptivename character varying(65),
	lastchgtimestamp timestamp without time zone,
	dbupdatedtimestamp timestamp without time zone,
	severity character varying(10),
	lctn character varying(100),
	jobid character varying(30),
	controloperation character varying(50),
	msg character varying(1000),
	instancedata character varying(500)
);


--
-- Name: raseventtypewithoutmetadata; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.raseventtypewithoutmetadata AS (
	eventtype character varying(10),
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
	nodes character varying(5000),
	starttimestamp timestamp without time zone,
	endtimestamp timestamp without time zone,
	deletedtimestamp timestamp without time zone,
	lastchgtimestamp timestamp without time zone
);

--
-- Name: raseventalertdata; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.raseventalertdata AS (
 	entrynumber bigint,
 	descriptivename character varying(65),
 	eventtype character varying(10),
 	id bigint,
 	lctn character varying(100),
 	severity character varying(10),
 	generatealert character varying(1),
 	jobid character varying(30),
    lastchgtimestamp timestamp without time zone,
 	instancedata character varying(500)
 );





SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: tier2_aggregatedenvdata; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_aggregatedenvdata (
    lctn character varying(100) NOT NULL,
    "timestamp" timestamp without time zone NOT NULL,
    type character varying(35) NOT NULL,
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
    bootimageid character varying(30),
    environment character varying(120),
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
    entrynumber bigint NOT NULL
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
    results character varying(201144),
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
    results character varying(201144),
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
    id character varying(30) NOT NULL,
    description character varying(200),
    bootimagefile character varying(80) NOT NULL,
    bootimagechecksum character varying(32) NOT NULL,
    bootoptions character varying(80),
    bootstrapimagefile character varying(80) NOT NULL,
    bootstrapimagechecksum character varying(32) NOT NULL,
    state character varying(1),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    kernelargs character varying(300),
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
    bootimageid character varying(30),
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
    entrynumber bigint NOT NULL
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
    eventtype character varying(10) NOT NULL,
    descriptivename character varying(65) NOT NULL,
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
    nodes character varying(5000),
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
    id integer NOT NULL,
    reference boolean DEFAULT false NOT NULL
);


--
-- Name: tier2_alert; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tier2_alert (
    id bigint NOT NULL,
    alerttype character varying(10) NOT NULL,
    state character varying(1) DEFAULT 'O'::character varying NOT NULL,
    severity character varying(10) NOT NULL,
    category character varying(20) NOT NULL,
    component character varying(20) NOT NULL,
    msg character varying(1000),
    lctn character varying(100),
    assocraseventtype character varying(10),
    assocraseventid bigint,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL,
    entrynumber bigint NOT NULL
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
    eventtype character varying(10) NOT NULL,
    lctn character varying(100),
    sernum character varying(50),
    jobid character varying(30),
    numberrepeats integer DEFAULT 0 NOT NULL,
    controloperation character varying(50),
    controloperationdone character varying(1) NOT NULL,
    instancedata character varying(500),
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
    id character varying(30) NOT NULL,
    description character varying(200),
    bootimagefile character varying(80),
    bootimagechecksum character varying(32),
    bootoptions character varying(80),
    bootstrapimagefile character varying(80),
    bootstrapimagechecksum character varying(32),
    state character varying(1),
    dbupdatedtimestamp timestamp without time zone,
    lastchgtimestamp timestamp without time zone,
    lastchgadaptertype character varying(20),
    lastchgworkitemid bigint,
    kernelargs character varying(300),
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
    bootimageid character varying(30),
    environment character varying(120),
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
    inventorytimestamp timestamp without time zone
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
    bootimageid character varying(30),
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
    inventorytimestamp timestamp without time zone
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
    eventtype character varying(10) NOT NULL,
    descriptivename character varying(65),
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
    eventtype character varying(10) NOT NULL,
    lctn character varying(100),
    sernum character varying(50),
    jobid character varying(30),
    numberrepeats integer DEFAULT 0 NOT NULL,
    controloperation character varying(50),
    controloperationdone character varying(1) NOT NULL,
    instancedata character varying(500),
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

CREATE TABLE public.Tier2_HWInventoryFRU (
    FRUID VARCHAR(80) NOT NULL PRIMARY KEY,     -- perhaps <manufacturer>-<serial#>
    FRUType VARCHAR(16),                        -- Field_Replaceble_Unit category(HMS type)
    FRUSubType VARCHAR(32),                     -- perhaps specific model; NULL:unspecifed
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    entrynumber bigint NOT NULL
);

CREATE TABLE public.Tier2_HWInventoryLocation (
    ID VARCHAR(64) NOT NULL PRIMARY KEY, -- perhaps xname (path); as is from JSON
    Type VARCHAR(16) NOT NULL,           -- Location category(HMS type)
    Ordinal INTEGER NOT NULL,            -- singleton:0
    FRUID VARCHAR(80) NOT NULL,          -- perhaps <manufacturer>-<serial#>
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    entrynumber bigint NOT NULL
);

CREATE TABLE public.tier2_HWInventory_History (
    Action VARCHAR(16) NOT NULL,            -- INSERTED/DELETED
    ID VARCHAR(64) NOT NULL,                -- perhaps xname (path); as is from JSON
    FRUID VARCHAR(80) NOT NULL,             -- perhaps <manufacturer>-<serial#>
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    EntryNumber bigint NOT NULL
);


--- FUNCTION DEFINITIONS START HERE ----

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
            where Timestamp <= coalesce(p_end_time, current_timestamp) and
                Timestamp >= p_start_time
            order by Lctn, Timestamp desc LIMIT 200;
    else
        return query
            select * from Tier2_AggregatedEnvData
            where Timestamp <= coalesce(p_end_time, current_timestamp)
            order by Timestamp desc LIMIT 200;
    end if;
    return;
END
$$;


--
-- Name: aggregatedenvdatastore(character varying, timestamp without time zone, character varying, double precision, double precision, double precision, character varying, bigint); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.aggregatedenvdatastore(p_location character varying, p_timestamp timestamp without time zone, p_type character varying, p_max_value double precision, p_min_value double precision, p_avg_value double precision, p_adapter_type character varying, p_work_item_id bigint) RETURNS void
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
        p_timestamp,
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
select DISTINCT ON (sequencenumber) lctn, sequencenumber, state, hostname, bootimageid, environment, ipaddr, macaddr, bmcipaddr,
    bmcmacaddr, bmchostname, dbupdatedtimestamp,  lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp,
    wlmnodestate, constraintid, entrynumber from  tier2_computenode_history where lastchgtimestamp >= coalesce(p_starttime, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS')
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
          from tier2_hwinventorylocation;

      return query
          select 'Replacement_Max_Timestamp'::varchar,
            max(dbupdatedtimestamp)
          from tier2_hwinventory_history; 

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
    where D1.StartTimestamp <= coalesce(p_end_time, current_timestamp) and
        (D1.EndTimestamp is null or
        D1.EndTimestamp > coalesce(p_end_time, current_timestamp)) and
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
    where D1.EndTimestamp <= coalesce(p_end_time, current_timestamp) and
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
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and diagid = ANY(v_diagids) and
                case
                   when p_lctn ='%' then (lctn ~ '.*' or lctn is null)
                    when p_lctn != '%' then (select string_to_array(lctn, ' ')) <@ (select string_to_array(p_lctn, ','))
                end
            order by dbupdatedtimestamp LIMIT p_limit;
    else
    return query
    select * from  tier2_diagresults
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
            case
               when p_lctn ='%' then (lctn ~ '.*' or lctn is null)
                when p_lctn != '%' then (select string_to_array(lctn, ' ')) <@ (select string_to_array(p_lctn, ','))
            end
    order by dbupdatedtimestamp LIMIT p_limit;
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
    select H.SnLctn, H.AdapterType, H.NumInitialInstances, 0::bigint , H.Invocation, H.LogFile, H.DbUpdatedTimestamp
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
-- Name: get_latest_switch_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_switch_records() RETURNS SETOF public.tier2_switch_history
    LANGUAGE sql
    AS $$
    select H.*
    from Tier2_Switch_History H
    inner join
        (select Lctn, max(LastChgTimestamp) as max_date
         from Tier2_Switch_History
         group by Lctn) D
    on H.Lctn = D.Lctn and H.LastChgTimestamp = D.max_date;
$$;


--
-- Name: get_latest_workitem_records(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.get_latest_workitem_records() RETURNS SETOF public.tier2_workitem_SS
    LANGUAGE sql
    AS $$
    select *
    from Tier2_WorkItem_SS H
    where WorkToBeDone <> 'BaseWork' or State <> 'D';
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
        order by timestamp LIMIT p_limit; $$;


--
-- Name: getinventorychange(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getinventorychange(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_hwinventory_history
    LANGUAGE plpgsql
    AS $$
    DECLARE
        p_fruid character varying;
    BEGIN
        p_fruid := (select distinct on (fruid) fruid from tier2_hwinventory_history where fruid = p_lctn);
        if (p_fruid is not null) then
        return query
            select * from  tier2_hwinventory_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
                fruid like (p_fruid || '%')
            order by dbupdatedtimestamp, id, action, fruid desc limit p_limit;

        else
        return query
            select * from  tier2_hwinventory_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
                (select string_to_array(id, ' ')) <@  (select string_to_array(p_lctn, ','))
            order by dbupdatedtimestamp, id, action, fruid desc limit p_limit;
        end if;
        return;
    END

$$;


--
-- Name: getinventoryhistoryforlctn(timestamp without time zone, timestamp without time zone, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getinventoryhistoryforlctn(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS TABLE(id character varying(64), fruid character varying(80))
    LANGUAGE sql
    AS $$
        select id, fruid from  tier2_hwinventory_history
        where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
            dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
            (select string_to_array(id, ' ')) <@  (select string_to_array(p_lctn, ','))
        order by dbupdatedtimestamp, id, action, fruid desc limit p_limit;
$$;


--
-- Name: getinventoryinfoforlctn(tcharacter varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getinventoryinfoforlctn(p_lctn character varying, p_limit integer) RETURNS TABLE(id character varying(64), dbupdatedtimestamp timestamp without time zone, ordinal integer, fruid character varying(80), type character varying(16), frutype character varying(16), frusubtype character varying(32))
    LANGUAGE sql
    AS $$
        select HI.id,
        HI.dbupdatedtimestamp,
        HI.ordinal,
        HI.fruid,
        HI.type,
        HF.frutype,
        HF.frusubtype
        from tier2_hwinventorylocation HI
        inner join tier2_hwinventoryfru HF on
        HI.fruid = HF.fruid
        where
            HI.id like concat(p_lctn, '%')
        order by HI.DbUpdatedTimestamp, HI.id desc LIMIT p_limit;
$$;


--
-- Name: getinventorydataforlctn(timestamp without time zone, timestamp without time zone, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getinventorydataforlctn(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_computenode_history
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is null and p_end_time is null then
    return query
        select distinct on (lctn) * from  tier2_computenode_history
        where (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
        order by lctn, dbupdatedtimestamp desc limit p_limit;
    else
    return query
        select * from  tier2_computenode_history
        where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
            dbupdatedtimestamp >= coalesce(p_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and
            (lctn not like '') and ((select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')))
        order by dbupdatedtimestamp desc limit p_limit;
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
            where JP.jobpowertimestamp <= coalesce(p_end_time, current_timestamp) and
                JP.jobpowertimestamp >= coalesce(p_start_time, '1970-01-01 0:0:0') and
                (select string_to_array(JP.lctn, ' ')) <@  (select string_to_array(p_lctn, ',')) and
                JP.jobid like p_jobid
            order by JP.lctn, JP.jobid desc limit p_limit;

        else
        return query
            select JP.jobid, JP.lctn, JP.jobpowertimestamp, JP.totalruntime, JP.totalpackageenergy, JP.totaldramenergy from  tier2_job_power JP
            where JP.jobpowertimestamp <= coalesce(p_end_time, current_timestamp) and
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
        select MD.DescriptiveName,
        RE.LastChgTimestamp,
        RE.DbUpdatedTimestamp,
        MD.Severity,
        RE.Lctn,
        RE.JobId,
        RE.ControlOperation,
        MD.Msg,
        RE.InstanceData
        from Tier2_RasEvent RE
        inner join Tier2_RasMetaData MD on RE.EventType = MD.EventType
        where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp) and
            RE.DbUpdatedTimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
            RE.lctn = any (v_locslist)
        order by RE.DbUpdatedTimestamp desc, RE.EventType, RE.Id LIMIT p_limit;
    return;
END

$$;


--
-- Name: getraseventswithfilters(timestamp without time zone, timestamp without time zone, character varying, character varying, character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getraseventswithfilters(p_start_time timestamp without time zone, p_end_time timestamp without time zone, p_lctn character varying, p_event_type character varying, p_severity character varying, p_jobid character varying, p_limit integer, p_exclude character varying) RETURNS TABLE(type character varying(65), "time" timestamp without time zone, dbupdatedtimestamp timestamp without time zone, severity character varying(10), lctn character varying(100), jobid character varying(30), controloperation character varying(50), detail character varying(10000))
    LANGUAGE plpgsql
    AS $$

DECLARE
    v_start_time timestamp without time zone;
    v_end_time timestamp without time zone;
    v_lctn character varying;
    v_event_type character varying;
    v_severity character varying;
    v_jobid character varying;
    v_limit integer;

BEGIN
    v_start_time := p_start_time;
    v_end_time := p_end_time;
    v_lctn := p_lctn;
    v_event_type := coalesce((select descriptivename from Tier2_RasMetaData where eventtype = p_event_type), p_event_type);
    v_severity := p_severity;
    v_jobid := p_jobid;
    v_limit := p_limit;

    if v_severity != '%' then
        v_severity = upper(v_severity);
    end if;

    return query
        select  MD.descriptivename,
                RE.LastChgTimestamp,
                RE.DbUpdatedTimestamp,
                MD.Severity,
                RE.Lctn,
                RE.JobId,
                RE.ControlOperation,
                CAST(CONCAT(MD.msg, ' ', RE.instancedata) AS character varying(10000))
        from Tier2_RasEvent RE
            inner join Tier2_RasMetaData MD on
            RE.EventType = MD.EventType
        where RE.lastchgtimestamp <=
            coalesce(v_end_time, current_timestamp at time zone 'UTC') and
            RE.lastchgtimestamp >= coalesce(v_start_time, (current_timestamp at time zone 'UTC') - INTERVAL '6 MONTHS') and
            MD.Severity like v_severity and
            case
                when p_exclude = '%' then (MD.DescriptiveName ~ '.*')
                when p_exclude != '%' then (MD.DescriptiveName !~ p_exclude)
            end
            and
            case
                when v_event_type = '%' then (MD.descriptivename ~ '.*')
                when v_event_type != '%' then (MD.descriptivename ~ v_event_type)
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
        order by RE.DbUpdatedTimestamp desc, RE.EventType, RE.Id LIMIT v_limit;
    return;
END
$$;


--
-- Name: getrefsnapshotdataforlctn(character varying, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getrefsnapshotdataforlctn(p_lctn character varying, p_limit integer) RETURNS SETOF public.tier2_inventorysnapshot
    LANGUAGE sql
    AS $$
        select lctn, snapshottimestamp, inventoryinfo, id, reference from  tier2_inventorysnapshot
        where (select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ',')) and reference = true
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
            select lctn, snapshottimestamp, inventoryinfo, id, reference from  tier2_inventorysnapshot
            where (select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))
            order by snapshottimestamp desc limit p_limit;
    elsif p_start_time is null then
        return query
            select lctn, snapshottimestamp, inventoryinfo, id, reference from  tier2_inventorysnapshot
            where snapshottimestamp <= p_end_time and
            (select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))
            order by snapshottimestamp desc limit p_limit;
    elsif p_end_time is null then
        return query
            select lctn, snapshottimestamp, inventoryinfo, id, reference from  tier2_inventorysnapshot
            where snapshottimestamp > p_start_time and
            (select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))
            order by snapshottimestamp desc limit p_limit;
    else
        return query
            select lctn, snapshottimestamp, inventoryinfo, id, reference from  tier2_inventorysnapshot
            where snapshottimestamp > p_start_time and snapshottimestamp <= p_end_time and
            (select string_to_array(lctn, ' ')) <@  (select string_to_array(p_lctn, ','))
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

CREATE OR REPLACE FUNCTION public.insertorupdaterasevent(p_id bigint, p_eventtype character varying, p_lctn character varying, p_sernum character varying, p_jobid character varying, p_numberrepeats integer, p_controloperation character varying, p_controloperationdone character varying, p_instancedata character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint) RETURNS void
    LANGUAGE plpgsql
    AS $$ BEGIN
    insert into Tier2_RasEvent(
        Id,
        EventType,
        Lctn,
        Sernum,
        JobId,
        NumberRepeats,
        ControlOperation,
        ControlOperationDone,
        InstanceData,
        DbUpdatedTimestamp,
        LastChgTimestamp,
        LastChgAdapterType,
        LastChgWorkItemId)
    values(
        p_id,
        p_eventtype,
        p_lctn,
        p_sernum,
        p_jobid,
        p_numberrepeats,
        p_controloperation,
        p_controloperationdone,
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

CREATE OR REPLACE FUNCTION public.insertorupdaterasmetadata(p_eventtype character varying, p_descriptivename character varying, p_severity character varying, p_category character varying, p_component character varying, p_controloperation character varying, p_msg character varying, p_dbupdatedtimestamp timestamp without time zone, p_generatealert character varying) RETURNS void
     LANGUAGE sql
     AS $$
     insert into Tier2_RasMetaData(
         EventType,
         DescriptiveName,
         Severity,
         Category,
         Component,
         ControlOperation,
         Msg,
         DbUpdatedTimestamp,
 	    GenerateAlert)
     values(
         p_eventtype,
         p_descriptivename,
         p_severity,
         p_category,
         p_component,
         p_controloperation,
         p_msg,
         p_dbupdatedtimestamp,
 	p_generatealert)
     on conflict(EventType) do update set
         DescriptiveName = p_descriptivename,
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

CREATE OR REPLACE FUNCTION public.inventoryinfolist(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS TABLE(id character varying(64), dbupdatedtimestamp timestamp without time zone, ordinal integer, fruid character varying(80), type character varying(16), frutype character varying(16), frusubtype character varying(32))
    LANGUAGE sql
    AS $$
    select  HI.id,
            HI.dbupdatedtimestamp,
            HI.ordinal,
            HI.fruid,
            HI.type,
            HF.frutype,
            HF.frusubtype
    from tier2_hwinventorylocation HI
    inner join tier2_hwinventoryfru HF on
    HI.fruid = HF.fruid
    where
        case
            when p_start_time is null then HI.dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            when p_start_time is not null then HI.dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC') and
                                               HI.dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp at time zone 'UTC')
        end
    order by HI.DbUpdatedTimestamp, HI.id desc
$$;


--
-- Name: inventorysnapshotlist(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.inventorysnapshotlist(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.inventorytype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select * from tier2_inventorysnapshot
            where snapshottimestamp <= coalesce(p_end_time, current_timestamp) and
                snapshottimestamp >= p_start_time
            order by snapshottimestamp desc;
    else
        return query
            select * from tier2_inventorysnapshot
            where snapshottimestamp <= coalesce(p_end_time, current_timestamp)
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
        where LastChgTimestamp <= coalesce(p_end_time, current_timestamp)
        order by JobId desc,
            LastChgTimestamp desc,
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

CREATE OR REPLACE FUNCTION public.jobhistorylistofnonactivejobsattime(p_end_time timestamp without time zone) RETURNS SETOF public.jobnonactivetype
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
        where LastChgTimestamp <= coalesce(p_end_time, current_timestamp)
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
        from Tier2_Job_History
        where endtimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
        and starttimestamp >= coalesce(p_start_time, '1970-01-01 0:0:0')
        and endtimestamp >= coalesce(p_at_time, endtimestamp)
        and starttimestamp <= coalesce(p_at_time, starttimestamp)
        and
        case
                when p_jobid = '%' then (jobid ~ '.*')
                when p_jobid != '%' then (jobid = p_jobid)
        end
        and
        case
                when p_username = '%' then (username ~ '.*')
                when p_username != '%' then (username ~ p_username)
        end
                and
        case
                when p_state = '%' then (state ~ '.*')
                when p_state != '%' then (state ~ p_state)
        end
        order by JobId desc, starttimestamp desc
    loop
        if v_counter < p_limit then
            if v_job.JobId <> v_prev_job_id then
                v_nodes := GetListNodeLctns(v_job.Nodes);
                if p_locations = '%' or areNodesInJob(string_to_array(p_locations, ','), string_to_array(v_nodes,' ')) then
                    v_counter := v_counter + 1;
                    v_prev_job_id := v_job.JobId;
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

    foreach v_loc in array p_locations
    loop
        foreach v_jobloc in array p_jobLocations
        loop
            v_areNodesInJob = v_areNodesInJob or (v_loc = v_jobloc);
        end loop;
    end loop;

    return v_areNodesInJob;
END
$$;


--
-- Name: raseventlistattime(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.raseventlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.raseventtype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is not null then
        return query
            select MD.DescriptiveName,
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
                    RE.EventType = MD.EventType and
                    MD.DbUpdatedTimestamp =
                    (select max(T.DbUpdatedTimestamp) from Tier2_RasMetaData T
                    where T.EventType = MD.EventType)
            where RE.DbUpdatedTimestamp <=
                coalesce(p_end_time, current_timestamp) and
                RE.DbUpdatedTimestamp >= p_start_time  and
                MD.Severity = 'ERROR' or MD.Severity = 'FATAL'
            order by RE.DbUpdatedTimestamp desc, RE.EventType, RE.Id LIMIT 200;
    else
        return query
            select RE.EventType,
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
                    RE.EventType = MD.EventType and
                    MD.DbUpdatedTimestamp =
                    (select max(T.DbUpdatedTimestamp) from Tier2_RasMetaData T
                    where T.EventType = MD.EventType)
            where RE.DbUpdatedTimestamp <=
                coalesce(p_end_time, current_timestamp) and
                MD.Severity = 'ERROR' or MD.Severity = 'FATAL'
            order by RE.DbUpdatedTimestamp desc, RE.EventType, RE.Id LIMIT 200;
    end if;
    return;
END
$$;


--
-- Name: replacementhistorylist(timestamp without time zone, timestamp without time zone); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.replacementhistorylist(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF public.tier2_hwinventory_history
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select * from tier2_hwinventory_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= p_start_time
            order by dbupdatedtimestamp, id, action, fruid desc;
    else
        return query
            select * from tier2_hwinventory_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp)
            order by dbupdatedtimestamp, id, action, fruid desc;
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
            where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp)
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
            where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp) and
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
            where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
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
            where RE.DbUpdatedTimestamp <= coalesce(p_end_time, current_timestamp at time zone 'UTC')
            and RE.DbUpdatedTimestamp >= p_start_time
            and RE.ReservationName = coalesce(p_reservation_name, RE.ReservationName)
            and RE.Users LIKE '%' || coalesce(p_user, RE.Users) || '%'
            order by RE.DbUpdatedTimestamp desc, RE.ReservationName, RE.Users LIMIT p_limit;
    end if;
    return;
END
$$;

--
-- Name: GetComputeNodeSummary(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.GetComputeNodeSummary() RETURNS SETOF public.system_summary_count
    LANGUAGE sql
    AS $$
    select state, count(*) as count from tier2_computenode_history group by state;
$$;

--
-- Name: GetServiceNodeSummary(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.GetServiceNodeSummary() RETURNS SETOF public.system_summary_count
    LANGUAGE sql
    AS $$
    select state, count(*) as count from tier2_servicenode_history group by state;
$$;

--
-- Name: servicenodeinventorylist(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.servicenodeinventorylist(p_starttime timestamp without time zone, p_endtime timestamp without time zone) RETURNS SETOF public.tier2_servicenode_history
    LANGUAGE sql
    AS $$
    select DISTINCT ON (lctn) lctn, sequencenumber, hostname, state, bootimageid, ipaddr, macaddr, bmcipaddr, bmcmacaddr,
        bmchostname,  dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, aggregator, inventorytimestamp, constraintid, entrynumber
        from  tier2_servicenode_history where lastchgtimestamp >= coalesce(p_starttime, (current_timestamp at time zone 'UTC') - INTERVAL '3 MONTHS') and lastchgtimestamp <= coalesce(p_endtime, current_timestamp at time zone 'UTC') order by lctn, dbupdatedtimestamp desc;
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
        end if;

        update tier2_inventorysnapshot
        set reference = false
        where lctn = p_lctn;

        update tier2_inventorysnapshot
        set reference = true
        where id = p_id;
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

--
-- Name: getalertmanagerdata(bigint, bigint, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.getalertmanagerdata(p_entrynum_start bigint, p_entrynum_end bigint, p_limit integer) RETURNS SETOF public.raseventalertdata
    LANGUAGE sql
    AS $$
        SELECT Tier2_RasEvent.EntryNumber, Tier2_RasMetaData.DescriptiveName, Tier2_RasEvent.EventType,
        Tier2_RasEvent.Id, Tier2_RasEvent.Lctn, Tier2_RasMetaData.Severity, Tier2_RasMetaData.GenerateAlert,
        Tier2_RasEvent.Jobid, Tier2_RasEvent.LastChgTimestamp, Tier2_RasEvent.InstanceData
        FROM Tier2_RasEvent
        INNER JOIN Tier2_RasMetaData
        ON Tier2_RasEvent.EventType=Tier2_RasMetaData.EventType
        WHERE (Tier2_RasEvent.EntryNumber>p_entrynum_start AND Tier2_RasEvent.EntryNumber<=p_entrynum_end)
        ORDER BY Tier2_RasEvent.EntryNumber LIMIT p_limit;
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

CREATE OR REPLACE FUNCTION public.insertorupdatecomputenodedata(p_lctn character varying, p_sequencenumber integer, p_state character varying, p_hostname character varying, p_bootimageid character varying, p_environment character varying, p_ipaddr character varying, p_macaddr character varying, p_bmcipaddr character varying, p_bmcmacaddr character varying, p_bmchostname character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_owner character varying, p_aggregator character varying, p_inventorytimestamp timestamp without time zone, p_wlmnodestate character varying) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_ComputeNode_SS(Lctn, SequenceNumber, HostName, State, BootImageId, Environment,
               IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp,
               LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp,  Wlmnodestate)
               values(p_Lctn, p_SequenceNumber, p_HostName, p_State, p_BootImageId, p_environment,
               p_IpAddr, p_MacAddr, p_BmcIpAddr, p_BmcMacAddr, p_BmcHostName, p_DbUpdatedTimestamp,
               p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_Owner, p_Aggregator, p_Inventorytimestamp,  p_Wlmnodestate) on conflict(Lctn) do update set SequenceNumber = p_SequenceNumber, HostName = p_HostName,
State=p_State, BootImageId = p_BootImageId,
               IpAddr=p_IpAddr, MacAddr=p_MacAddr, BmcIpAddr=p_BmcIpAddr,
BmcMacAddr=p_BmcMacAddr, BmcHostName=p_BmcHostName, DbUpdatedTimestamp=p_DbUpdatedTimestamp,
               LastChgTimestamp=p_LastChgTimestamp, LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, Owner=p_Owner,
Aggregator=p_Aggregator, Inventorytimestamp=p_Inventorytimestamp,  Wlmnodestate=p_Wlmnodestate, environment=p_environment
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


CREATE OR REPLACE FUNCTION public.insertorupdateservicenodedata(p_lctn character varying, p_sequencenumber integer, p_hostname character varying, p_state character varying, p_bootimageid character varying, p_ipaddr character varying, p_macaddr character varying, p_bmcipaddr character varying, p_bmcmacaddr character varying, p_bmchostname character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint, p_owner character varying, p_aggregator character varying, p_inventorytimestamp timestamp without time zone) RETURNS void
    LANGUAGE sql
    AS $$
insert into Tier2_ServiceNode_SS(Lctn, SequenceNumber, HostName, State, BootImageId,
               IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp,
               LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp)
               values(p_Lctn, p_SequenceNumber, p_HostName, p_State, p_BootImageId,
               p_IpAddr, p_MacAddr, p_BmcIpAddr, p_BmcMacAddr, p_BmcHostName, p_DbUpdatedTimestamp,
               p_LastChgTimestamp, p_LastChgAdapterType, p_LastChgWorkItemId, p_Owner, p_Aggregator, p_Inventorytimestamp) on conflict(Lctn) do update set
sequenceNumber = p_SequenceNumber, HostName = p_HostName,
State=p_State, BootImageId = p_BootImageId,
               IpAddr=p_IpAddr, MacAddr=p_MacAddr,  BmcIpAddr=p_BmcIpAddr,
BmcMacAddr=p_BmcMacAddr, BmcHostName=p_BmcHostName, DbUpdatedTimestamp=p_DbUpdatedTimestamp,
               LastChgTimestamp=p_LastChgTimestamp, LastChgAdapterType=p_LastChgAdapterType, LastChgWorkItemId=p_LastChgWorkItemId, Owner=p_Owner,
Aggregator=p_Aggregator, Inventorytimestamp=p_Inventorytimestamp
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

CREATE OR REPLACE FUNCTION public.insertorupdateraseventdata_ss(p_id bigint, p_eventtype character varying, p_lctn character varying, p_sernum character varying, p_jobid character varying, p_numberrepeats integer, p_controloperation character varying, p_controloperationdone character varying, p_instancedata character varying, p_dbupdatedtimestamp timestamp without time zone, p_lastchgtimestamp timestamp without time zone, p_lastchgadaptertype character varying, p_lastchgworkitemid bigint) RETURNS void
    LANGUAGE plpgsql
    AS $$ BEGIN
    insert into Tier2_RasEvent_ss(
        Id,
        EventType,
        Lctn,
        Sernum,
        JobId,
        NumberRepeats,
        ControlOperation,
        ControlOperationDone,
        InstanceData,
        DbUpdatedTimestamp,
        LastChgTimestamp,
        LastChgAdapterType,
        LastChgWorkItemId)
    values(
        p_id,
        p_eventtype,
        p_lctn,
        p_sernum,
        p_jobid,
        p_numberrepeats,
        p_controloperation,
        p_controloperationdone,
        p_instancedata,
        p_dbupdatedtimestamp,
        p_lastchgtimestamp,
        p_lastchgadaptertype,
        p_lastchgworkitemid)
    on conflict(EventType, Id) do update set
        Lctn = p_lctn,
        Sernum = p_sernum,
        JobId = p_jobid,
        NumberRepeats = p_numberrepeats,
        ControlOperation = p_controloperation,
        ControlOperationDone = p_controloperationdone,
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

CREATE OR REPLACE FUNCTION public.generate_partition_purge_ddl(table_name text, timestamp_column_name text, purge_data boolean DEFAULT false)
 RETURNS text
 LANGUAGE plpgsql
 IMMUTABLE STRICT
AS $function$
        DECLARE
            ddl_script TEXT;
        BEGIN
            ddl_script:= '';

                        ddl_script := ddl_script || '

                        CREATE FUNCTION ' || table_name || '_create_partition_and_insert()
                                RETURNS trigger
                                LANGUAGE plpgsql
                                VOLATILE NOT LEAKPROOF
                        AS $BODY$
                                DECLARE
                                  partition_month TEXT;
                                  partition TEXT;
                                  startdate date;
                                  enddate date;
                                BEGIN
                                  partition_month := to_char(NEW.' || timestamp_column_name || ',''YYYY_MM'');
                                  partition := TG_RELNAME || ''_'' || partition_month;
                                  startdate:= to_char(NEW.' || timestamp_column_name || ',''YYYY-MM-01'');
                                  enddate := date_trunc(''MONTH'', NEW.' || timestamp_column_name || ') + INTERVAL ''1 MONTH - 1 DAY'';
                                  IF NOT EXISTS(SELECT relname FROM pg_class WHERE relname=partition) THEN
                                        EXECUTE ''CREATE TABLE '' || partition || '' (check (' || timestamp_column_name || ' >= DATE '''' || startdate || ''''  AND NEW.' || timestamp_column_name || ' <=  DATE '''' ||  enddate || '''' )) INHERITS ('' || TG_RELNAME || '');'';
                                  END IF;
                                   EXECUTE ''INSERT INTO '' || partition || '' SELECT('' || TG_RELNAME || '' ''|| quote_literal(NEW) || '').*;'';
                           RETURN NULL;
                           END;
                           $BODY$;';
                         --- END OF CREATE PARTITION FUNCTION ---

                        --- CREATE A TRIGGER IF NOT ALREADY THERE FOR PARTITION INSERTS ON TABLE ---

                        PERFORM 1 FROM information_schema.triggers WHERE trigger_name = 'partition_insert_' || table_name || '_trigger';
            IF (FOUND) THEN
                ddl_script := ddl_script || '-- Trigger of the same name already exists.
                -- ';
            END IF;
            ddl_script := ddl_script || 'CREATE TRIGGER partition_insert_' || table_name || '_trigger BEFORE INSERT ON ' || table_name || ' FOR EACH ROW EXECUTE PROCEDURE ' || table_name || '_create_partition_and_insert();';

            --- END OF CREATE TRIGGER FOR PARTITION FUNCTION ---

            --- ENABLING TRIGGER FOR PURGING THE TABLES OLDER THAN 6 MONTHS ---

                        ddl_script := ddl_script ||
                        'CREATE OR REPLACE FUNCTION ' || table_name || '_purge_data(table_to_purge text)
                        RETURNS trigger
              LANGUAGE plpgsql VOLATILE
                          AS $BODY$
                          BEGIN
                                EXECUTE(''DROP TABLE IF EXISTS '' || quote_ident( TG_RELNAME_'' || to_char(current_timestamp - INTERVAL ''6 MONTHS '', ''YYYYMM'')) || '' CASCADE;);
                          RETURN NULL;
                          END;
                          $$;';

                        IF (purge_data) THEN
                ddl_script := ddl_script || '
                                CREATE TRIGGER purge_' || table_name || '_trigger BEFORE INSERT ON ' || table_name || ' FOR EACH ROW EXECUTE PROCEDURE purge_' || table_name'();';
            ELSE
                ddl_script := ddl_script || '-- Purging is disabled.';
            END IF;

            RAISE NOTICE '%s', ddl_script;
            RETURN ddl_script;
        END;
        $function$;


CREATE OR REPLACE FUNCTION public.generate_partition_purge_rules(table_name text, timestamp_column_name text, retention_policy integer DEFAULT 6)
 RETURNS text
 LANGUAGE plpgsql
AS $function$
DECLARE
            ddl_script TEXT;
            colu_name text;
            table_name_from_arg ALIAS FOR table_name;
        BEGIN

           PERFORM 1 FROM information_schema.columns WHERE information_schema.columns.table_name = table_name_from_arg AND column_name = timestamp_column_name AND data_type LIKE 'timestamp%';
            IF (NOT FOUND) THEN
                RAISE EXCEPTION 'The specified column % in table % must exists and of type timestamp', quote_literal(timestamp_column_name), quote_literal(table_name);
            END IF;
            ddl_script:= '';
                ddl_script := ddl_script || '
                                                CREATE OR REPLACE FUNCTION createfuturepartitions_' || table_name ||'(
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
                                            execute format(''create table %%I partition of ' || table_name || ' for values from (%%L) to (%%L)'', futuretableName, monthStart, monthEndExclusive);
                                        end if;
                                                end;
                                                $BODY$;
                                                ';
                                --- END OF CREATE PARTITION FUNCTION ---

                        --- CREATE A RULE ON THE TABLE FOR EVERY INSERT ---
                                                ddl_script := ddl_script || ' CREATE OR REPLACE RULE autocall_createfuturepartitions_' || table_name || ' AS
                                            ON INSERT TO '|| table_name ||'
                                        DO (
                                                SELECT createfuturepartitions_'|| table_name || '(new.'|| timestamp_column_name ||') AS createfuturepartitions_'|| table_name || ' ;);';

                --- END OF CREATE RULE FOR PARTITION FUNCTION ---
--- ENABLING TRIGGER FOR PURGING THE TABLES OLDER THAN Retention_Policy MONTHS ---

                        ddl_script := ddl_script ||
                        'CREATE OR REPLACE FUNCTION purge_' || table_name || '()
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
                                                        select max(' ||timestamp_column_name ||')-INTERVAL '''|| retention_policy + 1  ||' MONTHS'' from ' || table_name || ' into endDate;
                                                        select  min(' || timestamp_column_name || ') from ' || table_name ||' into startDate;
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
                       'CREATE TRIGGER purge_' || table_name || '_trigger AFTER INSERT ON ' || table_name || ' FOR EACH STATEMENT EXECUTE PROCEDURE purge_' || table_name || '();';
            ELSE
                ddl_script := ddl_script || '-- Purging is disabled.';
            END IF;
 RETURN ddl_script;
        END;
$function$;

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


CREATE OR REPLACE FUNCTION public.insertorupdatehwinventoryfru(p_fruid character varying, p_frutype character varying, p_frusubtype character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE plpgsql
    AS $$ BEGIN
    insert into Tier2_HWInventoryFRU(
        FRUId,
        FRUType,
        FRUSubType,
        DbUpdatedTimestamp)
    values(
        p_fruid,
        p_frutype,
        p_frusubtype,
        p_dbupdatedtimestamp)
    on conflict(FRUId) do update set
        FRUType = p_frutype,
        FRUSubType = p_frusubtype,
        DbUpdatedTimestamp = p_dbupdatedtimestamp; END;
$$;

CREATE OR REPLACE FUNCTION public.insertorupdatehwinventorylocation(p_id character varying, p_type character varying, p_ordinal integer, p_fruid character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
    LANGUAGE plpgsql
    AS $$ BEGIN
    insert into Tier2_HWInventoryLocation(
        Id,
        Type,
        Ordinal,
        FruId,
        DbUpdatedTimestamp)
    values(
        p_id,
        p_type,
        p_ordinal,
        p_fruid,
        p_dbupdatedtimestamp)
    on conflict(Id) do update set
        FRUId = p_fruid,
        Type = p_type,
        Ordinal = p_ordinal,
        DbUpdatedTimestamp = p_dbupdatedtimestamp; END;
$$;

CREATE OR REPLACE FUNCTION public.get_hwinventoryhistory_records() RETURNS SETOF public.tier2_HWInventory_History
 LANGUAGE sql
    AS $$
    select *
    from Tier2_HwInventory_History;
$$;

CREATE OR REPLACE FUNCTION public.get_hwinventoryfru_records() RETURNS SETOF public.Tier2_HWInventoryFRU
 LANGUAGE sql
    AS $$
    select *
    from Tier2_HWInventoryFRU;
$$;

CREATE OR REPLACE FUNCTION public.get_hwinventorylocation_records() RETURNS SETOF public.Tier2_HWInventoryLocation
 LANGUAGE sql
    AS $$
    select *
    from Tier2_HWInventoryLocation;
$$;

----- ALTER TABLE SQLS START HERE ------

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
-- Name: tier2_alert_entrynumber_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_alert_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tier2_alert_entrynumber_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tier2_alert_entrynumber_seq OWNED BY public.tier2_alert.entrynumber;


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
-- Name: tier2_inventorysnapshot_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tier2_inventorysnapshot_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


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


CREATE SEQUENCE public.tier2_nonnodehw_history_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tier2_nonnodehw_history_entrynumber_seq OWNED BY public.tier2_nonnodehw_history.entrynumber;


CREATE SEQUENCE public.tier2_HWInventory_History_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tier2_HWInventory_History_entrynumber_seq OWNED BY public.tier2_HWInventory_History.entrynumber;
ALTER TABLE ONLY public.tier2_HWInventory_History ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_HWInventory_History_entrynumber_seq'::regclass);
SELECT pg_catalog.setval('public.tier2_HWInventory_History_entrynumber_seq', 1, false);

CREATE SEQUENCE public.tier2_HWInventoryFRU_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tier2_HWInventoryFRU_entrynumber_seq OWNED BY public.tier2_HWInventoryFRU.entrynumber;
ALTER TABLE ONLY public.tier2_HWInventoryFRU ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_HWInventoryFRU_entrynumber_seq'::regclass);
SELECT pg_catalog.setval('public.tier2_HWInventoryFRU_entrynumber_seq', 1, false);

CREATE SEQUENCE public.tier2_HWInventoryLocation_entrynumber_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tier2_HWInventoryLocation_entrynumber_seq OWNED BY public.tier2_HWInventoryLocation.entrynumber;
ALTER TABLE ONLY public.tier2_HWInventoryLocation ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_HWInventoryLocation_entrynumber_seq'::regclass);
SELECT pg_catalog.setval('public.tier2_HWInventoryLocation_entrynumber_seq', 1, false);

--
-- Name: tier2_adapter_history entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_adapter_history ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_adapter_history_entrynumber_seq'::regclass);


--
-- Name: tier2_aggregatedenvdata entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_aggregatedenvdata ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_aggregatedenvdata_entrynumber_seq'::regclass);


--
-- Name: tier2_alert entrynumber; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_alert ALTER COLUMN entrynumber SET DEFAULT nextval('public.tier2_alert_entrynumber_seq'::regclass);


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
-- Data for Name: tier2_alert; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_alert (id, alerttype, state, severity, category, component, msg, lctn, assocraseventtype, assocraseventid, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, entrynumber) FROM stdin;
\.


--
-- Name: tier2_alert_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_alert_entrynumber_seq', 1, false);


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
-- Data for Name: tier2_rasevent; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_rasevent (id, eventtype, lctn, sernum, jobid, numberrepeats, controloperation, controloperationdone, instancedata, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, entrynumber) FROM stdin;
\.


--
-- Name: tier2_rasevent_entrynumber_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tier2_rasevent_entrynumber_seq', 1, false);


--
-- Data for Name: tier2_rasmetadata; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tier2_rasmetadata (eventtype, descriptivename, severity, category, component, controloperation, msg, dbupdatedtimestamp, entrynumber) FROM stdin;
\.


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
    ADD CONSTRAINT tier2_rasevent_ss_pkey PRIMARY KEY (eventtype, id);


--
-- Name: tier2_rasmetadata tier2_rasmetadata_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tier2_rasmetadata_ss
    ADD CONSTRAINT tier2_rasmetadata_ss_pkey PRIMARY KEY (eventtype);

ALTER TABLE ONLY public.tier2_rasmetadata
    ADD CONSTRAINT tier2_rasmetadata_pkey PRIMARY KEY (eventtype);

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


--
-- Name: rasevent_dbupdatedtimeeventtypeid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX rasevent_dbupdatedtimeeventtypeid ON public.tier2_rasevent USING btree (dbupdatedtimestamp DESC, eventtype, id);


--
-- Name: rasmetadata_eventtype; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX rasmetadata_eventtype ON public.tier2_rasmetadata USING btree (eventtype);

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

call public.create_first_partition();

--
-- PostgreSQL database dump complete
--
