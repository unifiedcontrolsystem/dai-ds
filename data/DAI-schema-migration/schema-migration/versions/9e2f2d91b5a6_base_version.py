"""Base version

Revision ID: 9e2f2d91b5a6
Revises: 
Create Date: 2019-01-11 11:11:44.474946

"""

# revision identifiers, used by Alembic.
revision = '9e2f2d91b5a6'
down_revision = None
branch_labels = None
depends_on = None

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy import false as false_just_for_sqlalchemy
from sqlalchemy.sql import func
import textwrap

def create_types():
    op.execute(textwrap.dedent("""
        CREATE TYPE inventorytype AS (
        lctn character varying(25),
        lastchgtimestamp timestamp without time zone,
        inventoryinfo character varying(1000));
        """))

    op.execute(textwrap.dedent("""
        CREATE TYPE jobactivetype AS (
        jobid character varying(30),
        jobname character varying(100),
        state character varying(1),
        bsn character varying(50),
        username character varying(30),
        starttimestamp timestamp without time zone,
        numnodes bigint,
        nodes character varying(4000),
        wlmjobstate character varying(50));
        """))

    op.execute(textwrap.dedent("""
        CREATE TYPE jobnonactivetype AS (
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
        wlmjobstate character varying(50));
    """))

    op.execute(textwrap.dedent("""
        CREATE TYPE raseventtype AS (
        eventtype character varying(10),
        lastchgtimestamp timestamp without time zone,
        dbupdatedtimestamp timestamp without time zone,
        severity character varying(10),
        lctn character varying(100),
        jobid character varying(30),
        controloperation character varying(50),
        msg character varying(1000),
        instancedata character varying(500));
    """))

    op.execute(textwrap.dedent("""
        CREATE TYPE reservationtype AS (
        reservationname character varying(35),
        users character varying(100),
        nodes character varying(100),
        starttimestamp timestamp without time zone,
        endtimestamp timestamp without time zone,
        deletedtimestamp timestamp without time zone,
        lastchgtimestamp timestamp without time zone);
    """))

    op.execute(textwrap.dedent("""
        CREATE TYPE raseventtypewithoutmetadata AS (
        eventtype character varying(10),
        lastchgtimestamp timestamp without time zone,
        dbupdatedtimestamp timestamp without time zone,
        lctn character varying(100),
        jobid character varying(30),
        controloperation character varying(50));
    """))

    op.execute(textwrap.dedent("""
        CREATE TYPE raseventtypewithdescriptivename AS (
        eventtype character varying(10),
        descriptivename character varying(65),
        lastchgtimestamp timestamp without time zone,
        dbupdatedtimestamp timestamp without time zone,
        severity character varying(10),
        lctn character varying(100),
        jobid character varying(30),
        controloperation character varying(50),
        msg character varying(1000),
        instancedata character varying(500));
    """))


def create_tables():

    op.execute(textwrap.dedent("""
        CREATE TABLE tier2_aggregatedenvdata (
        lctn character varying(100) NOT NULL,
        "timestamp" timestamp without time zone NOT NULL,
        type character varying(35) NOT NULL,
        maximumvalue double precision NOT NULL,
        minimumvalue double precision NOT NULL,
        averagevalue double precision NOT NULL,
        adaptertype character varying(20) NOT NULL,
        workitemid bigint NOT NULL);
    """))

    op.execute(textwrap.dedent("""
        CREATE TABLE tier2_computenode_history (
        lctn character varying(25) NOT NULL,
        sequencenumber integer NOT NULL,
        state character varying(1) NOT NULL,
        hostname character varying(63),
        sernum character varying(20),
        bootimageid character varying(30),
        environment character varying(120),
        ipaddr character varying(25),
        macaddr character varying(17),
        type character varying(20),
        bmcipaddr character varying(25),
        bmcmacaddr character varying(17),
        bmchostname character varying(63),
        dbupdatedtimestamp timestamp without time zone NOT NULL,
        lastchgtimestamp timestamp without time zone NOT NULL,
        lastchgadaptertype character varying(20) NOT NULL,
        lastchgworkitemid bigint NOT NULL,
        owner character varying(1) NOT NULL,
        inventoryinfo character varying(1000));
    """))

    op.execute(textwrap.dedent("""
        CREATE TABLE tier2_diag (
        diagid bigint NOT NULL,
        lctn character varying(50) NOT NULL,
        serviceoperationid bigint,
        diag character varying(32) NOT NULL,
        state character varying(1) NOT NULL,
        starttimestamp timestamp without time zone NOT NULL,
        endtimestamp timestamp without time zone,
        results character varying(250),
        lastchgadaptertype character varying(20) NOT NULL,
        lastchgworkitemid bigint NOT NULL);
    """))

    op.execute(textwrap.dedent("""
        CREATE TABLE tier2_servicenode_history (
        lctn character varying(30) NOT NULL,
        hostname character varying(63),
        state character varying(1) NOT NULL,
        sernum character varying(20),
        bootimageid character varying(30),
        ipaddr character varying(25),
        macaddr character varying(17),
        type character varying(20),
        bmcipaddr character varying(25),
        bmcmacaddr character varying(17),
        bmchostname character varying(63),
        dbupdatedtimestamp timestamp without time zone NOT NULL,
        lastchgtimestamp timestamp without time zone NOT NULL,
        lastchgadaptertype character varying(20) NOT NULL,
        lastchgworkitemid bigint NOT NULL,
        owner character varying(1) NOT NULL,
        inventoryinfo character varying(1000));
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_adapter_history (
    id bigint NOT NULL,
    adaptertype character varying(20) NOT NULL,
    sconrank bigint NOT NULL,
    state character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_alert (
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
    lastchgworkitemid bigint NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_bootimage_history (
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
    files character varying(300));
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_chassis_history (
    lctn character varying(12) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(20),
    type character varying(20),
    vpd character varying(4096),
    owner character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_fabrictopology_history (
    dbupdatedtimestamp timestamp without time zone NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_inventorysnapshot (
    lctn character varying(30) NOT NULL,
    snapshottimestamp timestamp without time zone NOT NULL,
    inventoryinfo character varying(2048) NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_job_history (
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
    wlmjobstate character varying(50));
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_jobstep_history (
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
    wlmjobstepstate character varying(50));
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_lustre_history (
    dbupdatedtimestamp timestamp without time zone NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_machine_history (
    sernum character varying(20) NOT NULL,
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
    usingsynthesizeddata character varying(1) NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_rack_history (
    lctn character varying(5) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(20),
    type character varying(20),
    vpd character varying(4096),
    owner character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_rasevent (
    id bigint NOT NULL,
    eventtype character varying(10) NOT NULL,
    lctn character varying(100),
    sernum character varying(20),
    jobid character varying(30),
    numberrepeats integer DEFAULT 0 NOT NULL,
    controloperation character varying(50),
    instancedata character varying(500),
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL);

    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_rasmetadata (
    eventtype character varying(10) NOT NULL,
    descriptivename character varying(65) NOT NULL,
    severity character varying(10) NOT NULL,
    category character varying(20) NOT NULL,
    component character varying(50) NOT NULL,
    controloperation character varying(50),
    msg character varying(1000),
    dbupdatedtimestamp timestamp without time zone NOT NULL);
"""))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_replacement_history (
    lctn character varying(100) NOT NULL,
    frutype character varying(30) NOT NULL,
    serviceoperationid bigint,
    oldsernum character varying(20),
    newsernum character varying(20) NOT NULL,
    oldstate character varying(1) NOT NULL,
    newstate character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_serviceoperation_history (
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
    logfile character varying(256) NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_switch_history (
    lctn character varying(25) NOT NULL,
    state character varying(1) NOT NULL,
    sernum character varying(20),
    type character varying(20),
    owner character varying(1) NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgtimestamp timestamp without time zone NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_wlmreservation_history (
    reservationname character varying(35) NOT NULL,
    users character varying(100) NOT NULL,
    nodes character varying(100),
    starttimestamp timestamp without time zone NOT NULL,
    endtimestamp timestamp without time zone,
    deletedtimestamp timestamp without time zone,
    lastchgtimestamp timestamp without time zone NOT NULL,
    dbupdatedtimestamp timestamp without time zone NOT NULL,
    lastchgadaptertype character varying(20) NOT NULL,
    lastchgworkitemid bigint NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    CREATE TABLE tier2_workitem_history (
    queue character varying(20) NOT NULL,
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
    rowinsertedintohistory character varying(1) NOT NULL);
    """))

    op.execute(textwrap.dedent("""
    ALTER TABLE ONLY tier2_aggregatedenvdata
    ADD CONSTRAINT tier2_aggregatedenvdata_pkey PRIMARY KEY (lctn, type, "timestamp");
    """))

    op.execute(textwrap.dedent("""
    ALTER TABLE ONLY tier2_diag
    ADD CONSTRAINT tier2_diag_pkey PRIMARY KEY (diagid);
    """))

    op.execute(textwrap.dedent("""
    ALTER TABLE ONLY tier2_inventorysnapshot
    ADD CONSTRAINT tier2_inventorysnapshot_pkey PRIMARY KEY (lctn, snapshottimestamp);
    """))

    op.execute(textwrap.dedent("""
    ALTER TABLE ONLY tier2_rasmetadata
    ADD CONSTRAINT tier2_rasmetadata_pkey PRIMARY KEY (eventtype);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX aggregatedenvdata_timelctn ON tier2_aggregatedenvdata USING btree ("timestamp", lctn);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX computenode_dbupdatedtime ON tier2_computenode_history USING btree (dbupdatedtimestamp);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX computenode_lastchgtimelctn ON tier2_computenode_history USING btree (lastchgtimestamp, lctn);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX computenode_seqnumdbupdatedtime ON tier2_computenode_history USING btree (sequencenumber, dbupdatedtimestamp);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX diag_endtimediagid ON tier2_diag USING btree (endtimestamp, diagid);
    """))
    
    op.execute(textwrap.dedent("""
    CREATE INDEX diag_startendtimediagid ON tier2_diag USING btree (starttimestamp, endtimestamp, diagid);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX jobhistory_dbupdatedtime ON tier2_job_history USING btree (dbupdatedtimestamp);
    """))
    
    op.execute(textwrap.dedent("""
    CREATE INDEX jobhistory_lastchgtime ON tier2_job_history USING btree (lastchgtimestamp);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX rasevent_dbupdatedtime ON tier2_rasevent USING btree (dbupdatedtimestamp);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX rasevent_dbupdatedtimeeventtypeid ON tier2_rasevent USING btree (dbupdatedtimestamp desc, eventtype, id);
    """))

    op.execute(textwrap.dedent("""
    CREATE INDEX rasmetadata_eventtype ON tier2_rasmetadata USING btree (eventtype);
    """))


def create_functions():
    op.execute(textwrap.dedent("""
            CREATE OR REPLACE FUNCTION aggregatedenvdatalistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF tier2_aggregatedenvdata
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
    """))

    op.execute(textwrap.dedent("""
            CREATE OR REPLACE FUNCTION aggregatedenvdatastore(p_location character varying, p_timestamp timestamp without time zone, p_type character varying, p_max_value double precision, p_min_value double precision, p_avg_value double precision, p_adapter_type character varying, p_work_item_id bigint) RETURNS void
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
       """))

    op.execute(textwrap.dedent("""
            CREATE OR REPLACE FUNCTION computenodehistorylistofstateattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS TABLE(lctn character varying, state character varying)
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
    """))
    
    op.execute(textwrap.dedent("""
            CREATE OR REPLACE FUNCTION computenodehistoryoldesttimestamp() RETURNS timestamp without time zone
        LANGUAGE sql
        AS $$
        select min(LastChgTimestamp) from Tier2_ComputeNode_History;
    $$;
    """))

    op.execute(textwrap.dedent("""
    CREATE OR REPLACE FUNCTION computenodeinventorylist(p_sequence_num integer) RETURNS SETOF tier2_computenode_history
    LANGUAGE sql
    AS $$
    select DISTINCT ON (sequencenumber) lctn, sequencenumber, state, hostname, sernum, bootimageid, environment, ipaddr, macaddr, type, bmcipaddr,
    bmcmacaddr, bmchostname, dbupdatedtimestamp,  lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, inventoryinfo
    from  tier2_computenode_history where sequencenumber > p_sequence_num order by sequencenumber, dbupdatedtimestamp desc limit 500;
    
    $$;
    """))
    
    op.execute(textwrap.dedent("""
    CREATE OR REPLACE FUNCTION dbchgtimestamps() RETURNS TABLE(key character varying, value timestamp without time zone)
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
              from Tier2_computenode_history;
    
          return query
              select 'InvSS_Max_Timestamp'::varchar,
                max(snapshottimestamp)
              from Tier2_inventorysnapshot;
    
          return query
              select 'Diags_Max_Timestamp'::varchar,
                max(starttimestamp)
              from Tier2_diag;
    
          return query
              select 'Replacement_Max_Timestamp'::varchar,
                max(dbupdatedtimestamp)
              from Tier2_replacement_history;
    
          return;
        END
    $$;
    """))

    op.execute(textwrap.dedent("""
        CREATE OR REPLACE FUNCTION diaglistofactivediagsattime(p_end_time timestamp without time zone) RETURNS SETOF tier2_diag
        LANGUAGE sql
        AS $$
        select * from Tier2_Diag
        where StartTimestamp <= coalesce(p_end_time, current_timestamp) and
        (EndTimestamp is null or
        EndTimestamp > coalesce(p_end_time, current_timestamp))
        order by DiagId desc;
        $$;
    """))

    op.execute(textwrap.dedent("""
        CREATE OR REPLACE FUNCTION diaglistofnonactivediagsattime(p_end_time timestamp without time zone) RETURNS SETOF tier2_diag
        LANGUAGE sql
        AS $$
        select * from Tier2_Diag
        where EndTimestamp <= coalesce(p_end_time, current_timestamp)
        order by DiagId desc;
        $$;
    """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getlistnodelctns(p_job_nodes bytea) RETURNS character varying
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
            where SequenceNumber = i
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
"""))
    
    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getmanifestcontent(OUT manifestcontent character varying) RETURNS character varying
    LANGUAGE sql
    AS $$
    select manifestcontent from tier2_machine_history order by dbupdatedtimestamp desc limit 1;
$$;
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION insertorupdaterasmetadata(p_eventtype character varying, p_descriptivename character varying, p_severity character varying, p_category character varying, p_component character varying, p_controloperation character varying, p_msg character varying, p_dbupdatedtimestamp timestamp without time zone) RETURNS void
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
        DbUpdatedTimestamp)
    values(
        p_eventtype,
        p_descriptivename,
        p_severity,
        p_category,
        p_component,
        p_controloperation,
        p_msg,
        p_dbupdatedtimestamp)
    on conflict(EventType) do update set
        DescriptiveName = p_descriptivename,
        Severity = p_severity,
        Category = p_category,
        Component = p_component,
        ControlOperation = p_controloperation,
        Msg = p_msg,
        DbUpdatedTimestamp = p_dbupdatedtimestamp;
$$;
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION jobhistorylistofactivejobsattime(p_end_time timestamp without time zone) RETURNS SETOF jobactivetype
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
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION jobhistorylistofnonactivejobsattime(p_end_time timestamp without time zone) RETURNS SETOF jobnonactivetype
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
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION raseventlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF raseventtype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is not null then
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
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION reservationlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone) RETURNS SETOF reservationtype
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
               """))
    

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION serviceinventorylist() RETURNS SETOF tier2_servicenode_history
    LANGUAGE sql
    AS $$
    select DISTINCT ON (lctn) lctn, hostname, state, sernum, bootimageid, ipaddr, macaddr, type, bmcipaddr, bmcmacaddr,
    bmchostname,  dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, inventoryinfo
    from  tier2_servicenode_history order by lctn, dbupdatedtimestamp desc;
$$;
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION inventorysnapshotlist(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone)
    RETURNS SETOF inventorytype
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
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION inventoryinfolist(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone)
    RETURNS SETOF inventorytype
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select distinct on (lctn) lctn, lastchgtimestamp, inventoryinfo from tier2_computenode_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= p_start_time and lastchgadaptertype != 'POPULATE'
            order by lctn, lastchgtimestamp desc;
    else
        return query
            select distinct on (lctn) lctn, lastchgtimestamp, inventoryinfo from tier2_computenode_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and lastchgadaptertype != 'POPULATE'
            order by lctn, lastchgtimestamp desc;
    end if;
    return;
END
$$;
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION replacementhistorylist(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone)
    RETURNS SETOF tier2_replacement_history
    LANGUAGE plpgsql
    AS $$
BEGIN
    if (p_start_time is not null) then
        return query
            select * from tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= p_start_time
            order by lastchgtimestamp desc;
    else
        return query
            select * from tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp)
            order by lastchgtimestamp desc;
    end if;
    return;
END
$$;
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getaggregatedevndatawithfilters(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_limit integer)
    RETURNS SETOF tier2_aggregatedenvdata
    LANGUAGE sql
    AS $$
        select * from  tier2_aggregatedenvdata
        where lctn similar to (p_lctn || '%') and
            timestamp <= coalesce(p_end_time, current_timestamp) and
            timestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS')
        order by timestamp LIMIT p_limit;
$$;
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getinventorychange(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_sernum character varying,
    p_limit integer)
    RETURNS SETOF tier2_replacement_history
    LANGUAGE plpgsql
    AS $$
    BEGIN
        if p_sernum != '%' then
        return query
            select * from  tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
                newsernum like (p_sernum || '%')
            order by lctn, dbupdatedtimestamp desc limit p_limit;

        else
        return query
            select * from  tier2_replacement_history
            where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
                dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
                lctn like (p_lctn || '%')
            order by lctn, dbupdatedtimestamp desc limit p_limit;
        end if;
        return;
    END

$$;
"""))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getinventorydataforlctn(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_limit integer)
    RETURNS SETOF tier2_computenode_history
    LANGUAGE plpgsql
    AS $$
BEGIN
    if p_start_time is null and p_end_time is null then
    return query
        select distinct on (lctn) lctn, sequencenumber, state, hostname, sernum, bootimageid, ipaddr, macaddr, type, bmcipaddr, bmcmacaddr, bmchostname, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, inventoryinfo from  tier2_computenode_history
        where lctn like (p_lctn || '%')
        order by dbupdatedtimestamp desc limit p_limit;
    else
    return query
        select lctn, sequencenumber, state, hostname, sernum, bootimageid, environment, ipaddr, macaddr, type, bmcipaddr, bmcmacaddr, bmchostname, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid, owner, inventoryinfo from  tier2_computenode_history
        where dbupdatedtimestamp <= coalesce(p_end_time, current_timestamp) and
            dbupdatedtimestamp >= coalesce(p_start_time, current_timestamp - INTERVAL '3 MONTHS') and
            lctn = p_lctn
        order by dbupdatedtimestamp desc limit p_limit;
    END IF;
    return;
END;

$$;

            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getlistnodelctnsastable(
    p_job_nodes bytea)
    RETURNS character varying[]
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
            where SequenceNumber = i
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
            """))

    op.execute(textwrap.dedent("""
CREATE OR REPLACE FUNCTION getraseventswithfilters(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_event_type character varying,
    p_severity character varying,
    p_limit integer)

    RETURNS SETOF raseventtype
    LANGUAGE plpgsql
    AS $$

DECLARE
    v_start_time timestamp without time zone;
    v_end_time timestamp without time zone;
    v_lctn character varying;
    v_event_type character varying;
    v_severity character varying;
    v_limit integer;

BEGIN
    v_start_time := p_start_time;
    v_end_time := p_end_time;
    v_lctn := p_lctn;
    v_event_type := p_event_type;
    v_severity := p_severity;
    v_limit := p_limit;

     if v_severity = '%' then
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
            RE.EventType = MD.EventType
        where RE.DbUpdatedTimestamp <=
            coalesce(v_end_time, current_timestamp) and
            RE.DbUpdatedTimestamp >= coalesce(v_start_time, current_timestamp - INTERVAL '6 MONTHS') and
            MD.descriptivename like (v_event_type || '%') and
            RE.lctn like (v_lctn || '%')
        order by RE.DbUpdatedTimestamp desc, RE.EventType, RE.Id LIMIT v_limit;
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
            RE.EventType = MD.EventType
        where RE.DbUpdatedTimestamp <=
            coalesce(v_end_time, current_timestamp) and
            RE.DbUpdatedTimestamp >= coalesce(v_start_time, current_timestamp - INTERVAL '6 MONTHS') and
            MD.Severity = upper(v_severity) and MD.descriptivename like (v_event_type || '%') and
            RE.lctn like (v_lctn || '%')
        order by RE.DbUpdatedTimestamp desc, RE.EventType, RE.Id LIMIT v_limit;
    end if;
    return;
END
$$;
    """))

    

def drop_tables():
    op.execute(textwrap.dedent("""
        DROP TABLE tier2_aggregatedenvdata;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_computenode_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_diag;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_servicenode_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_adapter_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_alert;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_bootimage_history;
            """))
    op.execute(textwrap.dedent("""
        DROP TABLE tier2_chassis_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_fabrictopology_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_inventorysnapshot;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_job_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_jobstep_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_lustre_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_machine_history;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_rack_history;
        """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_rasevent;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_rasmetadata;
            """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_replacement_history;
                """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_serviceoperation_history;
                """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_switch_history;
                """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_wlmreservation_history;
                    """))

    op.execute(textwrap.dedent("""
        DROP TABLE tier2_workitem_history;
                    """))

def drop_functions():
    op.execute(textwrap.dedent("""
        DROP FUNCTION aggregatedenvdatalistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION aggregatedenvdatastore(p_location character varying, p_timestamp timestamp without time zone, p_type character varying, p_max_value double precision, p_min_value double precision, p_avg_value double precision, p_adapter_type character varying, p_work_item_id bigint);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION computenodehistorylistofstateattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION computenodehistoryoldesttimestamp();
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION computenodeinventorylist(p_sequence_num integer);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION dbchgtimestamps();
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION diaglistofactivediagsattime(p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION diaglistofnonactivediagsattime(p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getlistnodelctns(p_job_nodes bytea);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getmanifestcontent(OUT manifestcontent character varying);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION insertorupdaterasmetadata(p_eventtype character varying, p_descriptivename character varying, p_severity character varying, p_category character varying, p_component character varying, p_controloperation character varying, p_msg character varying, p_dbupdatedtimestamp timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION jobhistorylistofactivejobsattime(p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION jobhistorylistofnonactivejobsattime(p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION raseventlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION reservationlistattime(p_start_time timestamp without time zone, p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION serviceinventorylist();
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION inventorysnapshotlist(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION inventoryinfolist(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION replacementhistorylist(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getaggregatedevndatawithfilters(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_limit integer);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getinventorychange(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_sernum character varying,
    p_limit integer);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getinventorydataforlctn(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_limit integer);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getlistnodelctnsastable(
    p_job_nodes bytea);
            """))

    op.execute(textwrap.dedent("""
        DROP FUNCTION getraseventswithfilters(
    p_start_time timestamp without time zone,
    p_end_time timestamp without time zone,
    p_lctn character varying,
    p_event_type character varying,
    p_severity character varying,
    p_limit integer);
            """))


def drop_types():
    op.execute(textwrap.dedent("""
        DROP TYPE inventorytype;
            """))
    op.execute(textwrap.dedent("""
        DROP TYPE jobactivetype;
            """))
    op.execute(textwrap.dedent("""
        DROP TYPE jobnonactivetype;
            """))

    op.execute(textwrap.dedent("""
        DROP TYPE raseventtype;
        """))

    op.execute(textwrap.dedent("""
        DROP TYPE reservationtype;
            """))
    op.execute(textwrap.dedent("""
        DROP TYPE raseventtypewithoutmetadata;
            """))
    op.execute(textwrap.dedent("""
        DROP TYPE raseventtypewithdescriptivename;
            """))


def upgrade():
    create_types();
    create_tables();
    create_functions();


def downgrade():
    drop_functions();
    drop_tables();
    drop_types();
