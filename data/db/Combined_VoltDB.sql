--------------------------------------------------------------
-- Machine/System Table  (holds machine configuration information)
-- - Info in this table is filled in either by
--    a)  processing of the system manifest
--    b)  manual update by the install team
--------------------------------------------------------------
CREATE TABLE Machine (
   Sernum               VarChar(50)             NOT NULL,   -- Serial number of this machine
   Description          VarChar(80),
   Type                 VarChar(20),                        -- Type of machine this is
   NumRows              BigInt,                             -- Number of rows in this machine
   NumColsInRow         BigInt,                             -- Number of columns in each row
   NumChassisInRack     BigInt,                             -- Number of chassis in each compute rack
   State                VarChar(1)              NOT NULL,   -- Actual state that this item is in - Active, Error, ...
   ClockFreq            BigInt,
   ManifestLctn         VarChar(128)            NOT NULL,   -- Location of the manifest information
   ManifestContent      VarChar(75000)          NOT NULL,   -- Contents of the above-mentioned ManifestLctn
   DbUpdatedTimestamp   TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   UsingSynthesizedData VarChar(1) DEFAULT 'N'  NOT NULL,   -- Flag indicating whether or not we are using synthesized data (for testing purposes)
   PRIMARY KEY (Sernum)
);
PARTITION TABLE Machine ON COLUMN Sernum;

--------------------------------------------------------------
-- Machine history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Machine table
--    b)  automatically by changes occurring in the Machine table (i.e., db trigger or export functionality)
-- Subscription Routing Key: Machine.State
--------------------------------------------------------------
CREATE TABLE Machine_History (
   Sernum               VarChar(50)             NOT NULL,   -- Serial number of this machine
   Description          VarChar(80),
   Type                 VarChar(20),                        -- Type of machine this is
   NumRows              BigInt,                             -- Number of rows in this machine
   NumColsInRow         BigInt,                             -- Number of columns in each row
   NumChassisInRack     BigInt,                             -- Number of chassis in each compute rack
   State                VarChar(1)              NOT NULL,   -- Actual state that this item is in - Active, Error, ...
   ClockFreq            BigInt,
   ManifestLctn         VarChar(128)            NOT NULL,   -- Location of the manifest information
   ManifestContent      VarChar(75000)          NOT NULL,   -- Contents of the above-mentioned ManifestLctn
   DbUpdatedTimestamp   TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   UsingSynthesizedData VarChar(1)              NOT NULL,   -- Flag indicating whether or not we are using synthesized data (for testing purposes)
);
PARTITION TABLE Machine_History ON COLUMN Sernum;
CREATE INDEX MachineHistoryByDbUpdatedTimestamp ON Machine_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Machine_History (
   Sernum                  VarChar(50)          NOT NULL,   -- Serial number of this machine
   Description             VarChar(80),
   Type                    VarChar(20),                     -- Type of machine this is
   NumRows                 BigInt,                          -- Number of rows in this machine
   NumColsInRow            BigInt,                          -- Number of columns in each row
   NumChassisInRack        BigInt,                          -- Number of chassis in each compute rack
   State                   VarChar(1)           NOT NULL,   -- Actual state that this item is in - Active, Error, ...
   ClockFreq               BigInt,
   ManifestLctn            VarChar(128)         NOT NULL,   -- Location of the manifest information
   ManifestContent         VarChar(75000)       NOT NULL,   -- Contents of the above-mentioned ManifestLctn
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   UsingSynthesizedData    VarChar(1)           NOT NULL,   -- Flag indicating whether or not we are using synthesized data (for testing purposes)
   Tier2DbUpdatedTimestamp TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt               NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Machine_History ON COLUMN Sernum;
--CREATE ASSUMEUNIQUE INDEX Tier2_Machine_History_EntryNum ON Tier2_Machine_History(EntryNumber);

--------------------------------------------------------------
-- UcsConfigValue Table  (holds UCS/DAI configuration information for this particular machine)
-- - Info in this table is filled in either by
--    a)  processing of the system manifest (or MachineConfig file)
--    b)  ucs-start-dai-mgr script
--------------------------------------------------------------
CREATE TABLE UcsConfigValue (
   Key                  VarChar(50)    NOT NULL,   -- particular configuration value's identifier e.g., "VoltIpAddrs", "UcsHome", "UcsClasspath", "UcsLog4jConfigurationFile", "UcsRasEventMetaDataFile"
   Value                VarChar(100)   NOT NULL,   -- actual value for this particular machine    e.g., "192.168.10.1,192.168.10.2", "/home/ddreed/dai", "/opt/voltdb/voltdb/*:/home/ddreed/dai/build/jars/dai-0.5.0.jar", "/home/ddreed/dai/install-configs/log4j2.xml", "/home/ddreed/dai/install-configs/RasEventMetaData.json"
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (Key)
);
PARTITION TABLE UcsConfigValue ON COLUMN Key;
CREATE INDEX UcsConfigValueByDbUpdatedTimestamp ON UcsConfigValue(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_UcsConfigValue (
   Key                  VarChar(50)    NOT NULL,   -- particular configuration value's identifier e.g., "VoltIpAddrs", "UcsHome", "UcsClasspath", "UcsLog4jConfigurationFile", "UcsRasEventMetaDataFile"
   Value                VarChar(100)   NOT NULL,   -- actual value for this particular machine    e.g., "192.168.10.1,192.168.10.2", "/home/ddreed/dai", "/opt/voltdb/voltdb/*:/home/ddreed/dai/build/jars/dai-0.5.0.jar", "/home/ddreed/dai/install-configs/log4j2.xml", "/home/ddreed/dai/install-configs/RasEventMetaData.json"
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (Key)
);
PARTITION TABLE Tier2_UcsConfigValue ON COLUMN Key;


--------------------------------------------------------------
-- MachineAdapterInstance Table  (holds information on which adapter instances need to be running on which service nodes)
-- - Info in this table is filled in by
--    a)  PopulateSchema stored procedure using information from the MachineConfiguration file
--------------------------------------------------------------
CREATE TABLE MachineAdapterInstance (
   SnLctn               VarChar(63)             NOT NULL,   -- Service node's lctn string
   AdapterType          VarChar(20)             NOT NULL,   -- Type of adapter
   NumInitialInstances  BigInt                  NOT NULL,   -- Number of instances of this type of adapter that should be spun up on this service node when this service node is started
   NumStartedInstances  BigInt                  NOT NULL,   -- Number of instances of this type of adapter that have been started on this service node
   Invocation           VarChar(400)            NOT NULL,   -- String that should be used to start adapter instances of this type, substitution will occur for "$HOSTNAME" and "$INSTANCE"
   LogFile              VarChar(100)            NOT NULL,   -- Log file for stdout and stderr
   DbUpdatedTimestamp   TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
PARTITION TABLE MachineAdapterInstance ON COLUMN SnLctn;
CREATE INDEX MachineAdapterInstanceBySnLctnAndAdaptertype ON MachineAdapterInstance(SnLctn, AdapterType);

--------------------------------------------------------------
-- MachineAdapterInstance history Table
--------------------------------------------------------------
CREATE TABLE MachineAdapterInstance_History (
   SnLctn               VarChar(63)             NOT NULL,   -- Service node's lctn string
   AdapterType          VarChar(20)             NOT NULL,   -- Type of adapter
   NumInitialInstances  BigInt                  NOT NULL,   -- Number of instances of this type of adapter that should be spun up on this service node when this service node is started
   NumStartedInstances  BigInt                  NOT NULL,   -- Number of instances of this type of adapter that have been started on this service node
   Invocation           VarChar(400)            NOT NULL,   -- String that should be used to start adapter instances of this type, substitution will occur for "$HOSTNAME" and "$INSTANCE"
   LogFile              VarChar(100)            NOT NULL,   -- Log file for stdout and stderr
   DbUpdatedTimestamp   TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
PARTITION TABLE MachineAdapterInstance_History ON COLUMN SnLctn;
CREATE INDEX MachineAdapterInstanceByDbUpdatedTimestamp ON MachineAdapterInstance_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when we do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_MachineAdapterInstance_History (
   SnLctn                   VarChar(63)             NOT NULL,   -- Service node's lctn string
   AdapterType              VarChar(20)             NOT NULL,   -- Type of adapter
   NumInitialInstances      BigInt                  NOT NULL,   -- Number of instances of this type of adapter that should be spun up on this service node when this service node is started
   NumStartedInstances      BigInt                  NOT NULL,   -- Number of instances of this type of adapter that have been started on this service node
   Invocation               VarChar(400)            NOT NULL,   -- String that should be used to start adapter instances of this type, substitution will occur for "$HOSTNAME" and "$INSTANCE"
   LogFile                  VarChar(100)            NOT NULL,   -- Log file for stdout and stderr
   DbUpdatedTimestamp       TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   Tier2DbUpdatedTimestamp  TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber              BigInt                  NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_MachineAdapterInstance_History ON COLUMN SnLctn;
--CREATE ASSUMEUNIQUE INDEX Tier2_MachineAdapterInstance_History ON Tier2_MachineAdapterInstance_History(EntryNumber);


--------------------------------------------------------------
-- Job Table
-- - Info in this table is filled in either by
--    a)  Workload Manager adapter
--    b)  Rm-Rte adapter
--------------------------------------------------------------
CREATE TABLE Job (
   JobId                VarChar(30)          NOT NULL,               -- Job id (given to us by WLM) e.g., 1234567.head-5
   JobName              VarChar(100),                                -- Job name (given to us by WLM) e.g., DonsJobStep3Job
   State                VarChar(1)           NOT NULL,               -- Actual state that this item is in - Started, Terminated, ...
   Bsn                  VarChar(50),                                 -- Lctn of the Batch Service Node / FEN / Login node that this job was launched from (note: lctn not the hostname)
   NumNodes             BigInt               NOT NULL,               -- Number of nodes in Nodes field
   Nodes                VarBinary(14000)     NOT NULL,               -- Byte array representation of a BitSet representing the nodes in this job
   PowerCap             BigInt DEFAULT 0     NOT NULL,               -- Power cap for this job in watts
   UserName             VarChar(30),                                 -- Username for this job
   Executable           VarChar(500),                                -- Executable or job script
   InitialWorkingDir    VarChar(500),                                -- Initial working directory for this job step
   Arguments            VarChar(4096),
   EnvironmentVars      VarChar(8192),                               -- Environment variables
   StartTimestamp       TIMESTAMP            NOT NULL,               -- Timestamp of when the job started
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,               -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,               -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType   VarChar(20)          NOT NULL,               -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt               NOT NULL,               -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   PRIMARY KEY (JobId)
);
PARTITION TABLE Job ON COLUMN JobId;

--------------------------------------------------------------
-- Job history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Job table
--    b)  automatically by changes occurring in the Job table (i.e., db trigger or export functionality)
-- Subscription Routing Key: Job.State (or possible Job.State.BSN.ExitStatus)
--------------------------------------------------------------
CREATE TABLE Job_History (
   JobId                VarChar(30)          NOT NULL,     -- Job id (given to us by WLM) e.g., 1234567.head-5
   JobName              VarChar(100),                      -- Job name (given to us by WLM) e.g., DonsJobStep3Job
   State                VarChar(1)           NOT NULL,     -- Actual state that this item is in - Started, Terminated, ...
   Bsn                  VarChar(50),                       -- Lctn of the Batch Service Node / FEN / Login node that this job was launched from (note: lctn not the hostname)
   NumNodes             BigInt DEFAULT 0     NOT NULL,     -- Number of nodes in Nodes field
   Nodes                VarBinary(14000),                  -- Byte array representation of a BitSet representing the nodes in this job
   PowerCap             BigInt DEFAULT 0     NOT NULL,     -- Power cap for this job in watts
   UserName             VarChar(30),                       -- Username for this job
   Executable           VarChar(500),                      -- Executable or job script
   InitialWorkingDir    VarChar(500),                      -- Initial working directory for this job step
   Arguments            VarChar(4096),
   EnvironmentVars      VarChar(8192),                     -- Environment variables
   StartTimestamp       TIMESTAMP            NOT NULL,     -- Timestamp of when the job started
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,     -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,     -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType   VarChar(20)          NOT NULL,     -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt               NOT NULL,     -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   EndTimestamp         TIMESTAMP,                         -- Time this job ended/terminated
   ExitStatus           BigInt,                            -- Jobs exit status
   -- ErrorText            VarChar(120),                      -- Any error text associated with this job
   JobAcctInfo          VarChar(120),                      -- Job accounting information
   PowerUsed            BigInt,                            -- Amount of power used during this job
   WlmJobState          VarChar(50),                       -- Jobs WLM state - state of this job as obtained from the WLM, e.g., COMPLETED, CANCELLED, FAILED, NODE_FAIL
);
PARTITION TABLE Job_History ON COLUMN JobId;
CREATE UNIQUE INDEX JobHistoryByJobIdAndLastChgTimestamp on Job_History(JobId, LastChgTimestamp);
CREATE INDEX JobHistoryByDbUpdatedTimestamp ON Job_History(DbUpdatedTimestamp);
CREATE INDEX JobHistoryByLastChgTimestamp ON Job_History(LastChgTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Job_History (
   JobId                   VarChar(30)          NOT NULL,  -- Job id (given to us by WLM) e.g., 1234567.head-5
   JobName                 VarChar(100),                   -- Job name (given to us by WLM) e.g., DonsJobStep3Job
   State                   VarChar(1)           NOT NULL,  -- Actual state that this item is in - Started, Terminated, ...
   Bsn                     VarChar(50),                    -- Lctn of the Batch Service Node / FEN / Login node that this job was launched from (note: lctn not the hostname)
   NumNodes                BigInt DEFAULT 0     NOT NULL,  -- Number of nodes in Nodes field
   Nodes                   VarBinary(14000),               -- Byte array representation of a BitSet representing the nodes in this job
   PowerCap                BigInt DEFAULT 0     NOT NULL,  -- Power cap for this job in watts
   UserName                VarChar(30),                    -- Username for this job
   Executable              VarChar(500),                   -- Executable or job script
   InitialWorkingDir       VarChar(500),                   -- Initial working directory for this job step
   Arguments               VarChar(4096),
   EnvironmentVars         VarChar(8192),                  -- Environment variables
   StartTimestamp          TIMESTAMP            NOT NULL,  -- Timestamp of when the job started
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL,  -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP            NOT NULL,  -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType      VarChar(20)          NOT NULL,  -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt               NOT NULL,  -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   EndTimestamp            TIMESTAMP,                      -- Time this job ended/terminated
   ExitStatus              BigInt,                         -- Jobs exit status
   -- ErrorText            VarChar(120),                   -- Any error text associated with this job
   JobAcctInfo             VarChar(120),                   -- Job accounting information
   PowerUsed               BigInt,                         -- Amount of power used during this job
   WlmJobState             VarChar(50),                    -- Jobs WLM state - state of this job as obtained from the WLM, e.g., COMPLETED, CANCELLED, FAILED, NODE_FAIL
   Tier2DbUpdatedTimestamp TIMESTAMP            NOT NULL,  -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt               NOT NULL,  -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Job_History ON COLUMN JobId;
CREATE ASSUMEUNIQUE INDEX Tier2_Job_History_EntryNum ON Tier2_Job_History(EntryNumber);


--------------------------------------------------------------
-- Internal Cached Jobs Table
-- - This table contains info on cached jobs.
--    Contains active jobs or recently terminated jobs.
--    Used for amongst other things the filling in of "missing" job ids in RAS events.
--------------------------------------------------------------
CREATE TABLE InternalCachedJobs (
   NodeLctn             VarChar(25)          NOT NULL,
   JobId                VarChar(30)          NOT NULL,               -- Job id (given to us by WLM) e.g., 1234567.head-5
   StartTimestamp       TIMESTAMP            NOT NULL,               -- Timestamp of when the job started
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,               -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than the other timestamps in this record.
   EndTimestamp         TIMESTAMP,                                   -- Timestamp of when the job ended
);
PARTITION TABLE InternalCachedJobs ON COLUMN NodeLctn;
CREATE UNIQUE INDEX InternalCachedJobsByNodeLctnAndJobId ON InternalCachedJobs(NodeLctn, JobId);
CREATE INDEX InternalCachedJobsByDbUpdatedTimestamp      ON InternalCachedJobs(DbUpdatedTimestamp) WHERE EndTimestamp IS NOT NULL;
CREATE INDEX InternalCachedJobsByJobId                   ON InternalCachedJobs(JobId);
CREATE INDEX InternalCachedJobsByNodeLctnStartTsAndEndTs ON InternalCachedJobs(NodeLctn, StartTimestamp, EndTimestamp);

--------------------------------------------------------------
-- Internal JobInfo Table
-- - This table contains information that is used by the Slurm WLM adapter for tracking information for jobs.
--   Originally this information was contained in a map internal to the WLM adapter instance, but in order to allow
--     another instance to pick up and continue work items that have failed, it was necessary to move this information someplace more resilient.
--------------------------------------------------------------
CREATE TABLE InternalJobInfo (
   JobId                VarChar(30)    NOT NULL,   -- Job id (given to us by WLM) e.g., 1234567.head-5 (pbs pro), 25 (slurm)
   WlmJobStarted        VarChar(1)     NOT NULL,   -- Flag indicating whether or not we have seen the msg from WLM that indicates that the Job has started (slurmctld.log)
                                                   --    T = true  - we have seen the Job started message
                                                   --    F = false - we have not yet seen the Job started message
   WlmJobStartTime      TIMESTAMP      NOT NULL,   -- Timestamp that this Job started
   WlmJobEndTime        TIMESTAMP,                 -- Timestamp that this Job ended (slurm_jobcomp.log)
   WlmJobWorkDir        VarChar(500),              -- Jobs working directory information from the job completion message (slurm_jobcomp.log)
   WlmJobCompleted      VarChar(1)     NOT NULL,   -- Flag indicating whether or not we have seen the msg from WLM that indicates that the Job has completed (slurm_jobcomp.log)
                                                   --    T = true  - we have seen the Job completed message
                                                   --    F = false - we have not yet seen the Job completed message
   WlmJobState          VarChar(50),               -- Jobs WLM state - state of this job as obtained from the WLM, e.g., COMPLETED, CANCELLED, FAILED, NODE_FAIL (slurm_jobcomp.log)
);
PARTITION TABLE InternalJobInfo ON COLUMN JobId;
CREATE INDEX InternalJobInfoByJobidStarttimeAndEndtime ON InternalJobInfo(JobId, WlmJobStartTime, WlmJobEndTime);
CREATE INDEX InternalJobInfoByWlmjobstartedWlmjobcompletedAndWlmjobstarttime ON InternalJobInfo(WlmJobStarted, WlmJobCompleted, WlmJobStartTime);

--------------------------------------------------------------
-- Internal JobStepInfo Table
-- - This table contains information that is used by the Slurm WLM adapter for tracking information for job steps.
--   Originally this information was contained in a map internal to the WLM adapter instance, but in order to allow
--     another instance to pick up and continue work items that have failed, it was necessary to move this information someplace more resilient.
--------------------------------------------------------------
CREATE TABLE InternalJobStepInfo (
   JobId                VarChar(30)    NOT NULL,   -- Job id (given to us by WLM) e.g., 1234567.head-5 (pbs pro), 25 (slurm)
   JobStepId            VarChar(35)    NOT NULL,   -- Identifies this JobStep within the job e.g., 25.7 (slurm), 7 (pbs pro)
   WlmJobStepStarted    VarChar(1)     NOT NULL,   -- Flag indicating whether or not we have seen the msg from WLM that indicates that the JobStep has started
                                                   --    T = true  - we have seen the JobStep started message
                                                   --    F = false - we have not yet seen the JobStep started message
   WlmJobStepEndTime    TIMESTAMP,                 -- Timestamp that this JobStep ended (taken from the timestamp on the slurmctld log message)
   WlmJobStepEnded      VarChar(1)     NOT NULL,   -- Flag indicating whether or not we have seen the msg from WLM that indicates that the JobStep has ended.
                                                   --    T = true  - we have seen the JobStep ended message
                                                   --    F = false - we have not yet seen the JobStep ended message
);
PARTITION TABLE InternalJobStepInfo ON COLUMN JobId;
CREATE UNIQUE INDEX InternalJobStepInfoByJobIdAndJobStepId ON InternalJobStepInfo(JobId, JobStepId);

--------------------------------------------------------------
-- Internal Inflight WLM Prolog/Epilog Requests Table
-- - This table contains information that is used by the Slurm WLM adapter for tracking information about inflight requests from the WLM prolog or epilog code.
--   Originally this information was contained in a map internal to the WLM adapter instance, but in order to allow
--     another instance to pick up and continue work items that have failed, it was necessary to move this information someplace more resilient.
--------------------------------------------------------------
CREATE TABLE InternalInflightWlmPrologOrEpilogRequests (
   WorkingAdapterType   VarChar(20)    NOT NULL,   -- Type of adapter that is working on this request (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   WorkItemId           BigInt         NOT NULL,   -- Work item id that is handling this request
   JobId                VarChar(30)    NOT NULL,   -- Job id associated with the prolog/epilog request e.g., 1234567.head-5 (pbs pro), 25 (slurm)
   JobConstraints       VarChar(500)   NOT NULL,   -- Job constraints associated with the prolog/epilog request
   JobNodeList          VarChar(1000)  NOT NULL,   -- List of nodes associated with the prolog/epilog request
   ResultFifoName       VarChar(500)   NOT NULL,   -- Name of the fifo that we should use when sending back the results of the request
   RequestStartTime     TIMESTAMP      NOT NULL,   -- Timestamp that this request was queued/started
);
PARTITION TABLE InternalInflightWlmPrologOrEpilogRequests ON COLUMN WorkingAdapterType;
CREATE UNIQUE INDEX InternalInflightWlmPrologOrEpilogRequestsByWorkingAdapterTypeAndWorkItemId ON InternalInflightWlmPrologOrEpilogRequests(WorkingAdapterType, WorkItemId);


--------------------------------------------------------------
-- JobStep Table
-- - Info in this table is filled in either by
--    a)  Workload Manager adapter
--    b)  Rm-Rte adapter
--------------------------------------------------------------
CREATE TABLE JobStep (
   JobId                VarChar(30)          NOT NULL,      -- Identifies the JOB that this job step is running under
   JobStepId            VarChar(35)          NOT NULL,      -- Identifies this JobStep within the job
   State                VarChar(1)           NOT NULL,      -- Actual state that this item is in - Started/Created, Error, Terminated, ...
   NumNodes             BigInt,                             -- Number of nodes in Nodes field
   Nodes                VarBinary(14000),                   -- Byte array representation of a BitSet representing the nodes in this job step
   NumProcessesPerNode  BigInt,
   Executable           VarChar(500),                       -- Executable or job script
   InitialWorkingDir    VarChar(500),                       -- Initial working directory for this job step
   Arguments            VarChar(4096),
   EnvironmentVars      VarChar(8192),                      -- Environment variables
   MpiMapping           VarChar(100),                       -- Algorithm for mapping MPI ranks to nodes.
   StartTimestamp       TIMESTAMP            NOT NULL,      -- Time this job step was created (if passed as String, the text must be formatted as either YYYY-MM-DD hh.mm.ss.nnnnnn or just the date portion YYYY-MM-DD)
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,      -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,      -- Timestamp of the last change to this row
   LastChgAdapterType   VarChar(20)          NOT NULL,      -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt               NOT NULL,      -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   CONSTRAINT pkey PRIMARY KEY (JobId, JobStepId)
);
PARTITION TABLE JobStep ON COLUMN JobId;

--------------------------------------------------------------
-- JobStep history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the JobStep table
--    b)  automatically by changes occurring in the JobStep table (i.e., db trigger or export functionality)
-- Subscription Routing Key: JobStep.State (or possibly JobStep.State.ExitStatus)
--------------------------------------------------------------
CREATE TABLE JobStep_History (
   JobId                VarChar(30)          NOT NULL,      -- Identifies the JOB that this job step is running under
   JobStepId            VarChar(35)          NOT NULL,      -- Identifies this JobStep within the job
   State                VarChar(1)           NOT NULL,      -- Actual state that this item is in - Started/Created, Error, Terminated, ...
   NumNodes             BigInt,                             -- Number of nodes in Nodes field
   Nodes                VarBinary(14000),                   -- Byte array representation of a BitSet representing the nodes in this job step
   NumProcessesPerNode  BigInt,
   Executable           VarChar(500),                       -- Executable or job script
   InitialWorkingDir    VarChar(500),                       -- Initial working directory for this job step
   Arguments            VarChar(4096),
   EnvironmentVars      VarChar(8192),                      -- Environment variables
   MpiMapping           VarChar(100),                       -- Algorithm for mapping MPI ranks to nodes.
   StartTimestamp       TIMESTAMP            NOT NULL,      -- Time this job step was created (if passed as String, the text must be formatted as either YYYY-MM-DD hh.mm.ss.nnnnnn or just the date portion YYYY-MM-DD)
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,      -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,      -- Timestamp of the last change to this row
   LastChgAdapterType   VarChar(20)          NOT NULL,      -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt               NOT NULL,      -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   EndTimestamp         TIMESTAMP,                          -- Time this job step ended (if passed as String, the text must be formatted as either YYYY-MM-DD hh.mm.ss.nnnnnn or just the date portion YYYY-MM-DD)
   ExitStatus           BigInt,                             -- JobSteps exit status
   WlmJobStepState      VarChar(50),                        -- JobSteps WLM state - state of this JobStep as obtained from the WLM, e.g., COMPLETED, CANCELLED, FAILED, NODE_FAIL
   -- ErrorText            VarChar(120),
);
PARTITION TABLE JobStep_History ON COLUMN JobId;
CREATE UNIQUE INDEX JobStepHistoryByJobIdAndJobStepIdAndLastChgTimestamp on JobStep_History(JobId, JobStepId, LastChgTimestamp);
CREATE INDEX JobStepHistoryByDbUpdatedTimestamp ON JobStep_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_JobStep_History (
   JobId                    VarChar(30)          NOT NULL,  -- Identifies the JOB that this job step is running under
   JobStepId                VarChar(35)          NOT NULL,  -- Identifies this JobStep within the job
   State                    VarChar(1)           NOT NULL,  -- Actual state that this item is in - Started/Created, Error, Terminated, ...
   NumNodes                 BigInt,                         -- Number of nodes in Nodes field
   Nodes                    VarBinary(14000),               -- Byte array representation of a BitSet representing the nodes in this job step
   NumProcessesPerNode      BigInt,
   Executable               VarChar(500),                   -- Executable or job script
   InitialWorkingDir        VarChar(500),                   -- Initial working directory for this job step
   Arguments                VarChar(4096),
   EnvironmentVars          VarChar(8192),                  -- Environment variables
   MpiMapping               VarChar(100),                   -- Algorithm for mapping MPI ranks to nodes.
   StartTimestamp           TIMESTAMP            NOT NULL,  -- Time this job step was created (if passed as String, the text must be formatted as either YYYY-MM-DD hh.mm.ss.nnnnnn or just the date portion YYYY-MM-DD)
   DbUpdatedTimestamp       TIMESTAMP            NOT NULL,  -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp         TIMESTAMP            NOT NULL,  -- Timestamp of the last change to this row
   LastChgAdapterType       VarChar(20)          NOT NULL,  -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId        BigInt               NOT NULL,  -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   EndTimestamp             TIMESTAMP,                      -- Time this job step ended (if passed as String, the text must be formatted as either YYYY-MM-DD hh.mm.ss.nnnnnn or just the date portion YYYY-MM-DD)
   ExitStatus               BigInt,                         -- JobSteps exit status
   WlmJobStepState          VarChar(50),                    -- JobSteps WLM state - state of this JobStep as obtained from the WLM, e.g., COMPLETED, CANCELLED, FAILED, NODE_FAIL
   -- ErrorText                VarChar(120),
   Tier2DbUpdatedTimestamp  TIMESTAMP            NOT NULL,  -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber              BigInt               NOT NULL,  -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_JobStep_History ON COLUMN JobId;
CREATE ASSUMEUNIQUE INDEX Tier2_JobStep_History_EntryNum ON Tier2_JobStep_History(EntryNumber);


--------------------------------------------------------------
-- WLM Reservation table
-- - Info in this table is filled in by the WLM adapter.
--------------------------------------------------------------
CREATE TABLE WlmReservation_History (
   ReservationName      Varchar(35)    NOT NULL,            -- Identifier that identifies a specific WLM reservation
   Users                VarChar(100)   NOT NULL,            -- Users of this reservation
   Nodes                VarChar(1000),                      -- Nodes assigned to this reservation
   StartTimestamp       TIMESTAMP      NOT NULL,            -- Time that this reservation begins - from timestamp on event record
   EndTimestamp         TIMESTAMP,                          -- Time that this reservation ends - from timestamp on event record
   DeletedTimestamp     TIMESTAMP,                          -- Time that this reservation was deleted (if appropriate) - from timestamp on event record
   LastChgTimestamp     TIMESTAMP      NOT NULL,            -- Time that the event that caused the last "change" to this reservation occurred.  This is NOT the time that the event was put into the data store.  This is different than the DbUpdatedTimestamp field.
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,            -- Time that this record was recorded in the database.  It is the actual time that the db insert/update occurred.  This is different than LastChgTimestamp field.
   LastChgAdapterType   VarChar(20)    NOT NULL,            -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt         NOT NULL,            -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
);
PARTITION TABLE WlmReservation_History ON COLUMN ReservationName;
CREATE INDEX WlmReservation_HistoryByReservationNameAndLastChgTimestamp ON WlmReservation_History(ReservationName, LastChgTimestamp);
CREATE INDEX WlmReservation_HistoryByDbUpdatedTimestamp                 ON WlmReservation_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_WlmReservation_History (
   ReservationName         Varchar(35)    NOT NULL,         -- Identifier that identifies a specific WLM reservation
   Users                   VarChar(100)   NOT NULL,         -- Users of this reservation
   Nodes                   VarChar(1000),                   -- Nodes assigned to this reservation
   StartTimestamp          TIMESTAMP      NOT NULL,         -- Time that this reservation begins - from timestamp on event record
   EndTimestamp            TIMESTAMP,                       -- Time that this reservation ends - from timestamp on event record
   DeletedTimestamp        TIMESTAMP,                       -- Time that this reservation was deleted (if appropriate) - from timestamp on event record
   LastChgTimestamp        TIMESTAMP      NOT NULL,         -- Time that the event that caused the last "change" to this reservation occurred.  This is NOT the time that the event was put into the data store.  This is different than the DbUpdatedTimestamp field.
   DbUpdatedTimestamp      TIMESTAMP      NOT NULL,         -- Time that this record was recorded in the database.  It is the actual time that the db insert/update occurred.  This is different than LastChgTimestamp field.
   LastChgAdapterType      VarChar(20)    NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt         NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Tier2DbUpdatedTimestamp TIMESTAMP      NOT NULL,         -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt         NOT NULL,         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_WlmReservation_History ON COLUMN ReservationName;
CREATE ASSUMEUNIQUE INDEX Tier2_WlmReservation_History_EntryNum ON Tier2_WlmReservation_History(EntryNumber);


--------------------------------------------------------------
-- Rack Table
--    192 racks, 12 rows of 16 columns
--    196 racks, 14 rows of 14 columns
-- - Info in this table is filled in either by
--    a)  System discovery process
--    b)  Provisioner adapter
--    c)  Service Operation post-processing
--    d)  Operator interface / WLM adapter
--------------------------------------------------------------
CREATE TABLE Rack (
   Lctn                    VarChar(5) UNIQUE NOT NULL,
   State                   VarChar(1)        NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum                  VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                    VarChar(20),                  -- Type of item this is (i.e., Product Name)
   Vpd                     VarChar(4096),                -- Item VPD information
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   Owner                   VarChar(1)        NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   PRIMARY KEY (Lctn)
);
PARTITION TABLE Rack ON COLUMN Lctn;

--------------------------------------------------------------
-- Rack history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Rack table
--    b)  automatically by changes occurring in the Rack table (i.e., db trigger or export functionality)
-- Subscription Routing Key: Rack.State (or possibly Rack.Lctn.State.Type)
--------------------------------------------------------------
CREATE TABLE Rack_History (
   Lctn                 VarChar(5)           NOT NULL,
   State                VarChar(1)           NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum               VarChar(50),                     -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                 VarChar(20),                     -- Type of item this is (i.e., Product Name)
   Vpd                  VarChar(4096),                   -- Item VPD information
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   Owner                VarChar(1)           NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
);
PARTITION TABLE Rack_History ON COLUMN Lctn;
CREATE INDEX RackHistoryByDbUpdatedTimestamp ON Rack_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Rack_History (
   Lctn                    VarChar(5)        NOT NULL,
   State                   VarChar(1)        NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum                  VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                    VarChar(20),                  -- Type of item this is (i.e., Product Name)
   Vpd                     VarChar(4096),                -- Item VPD information
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   Owner                   VarChar(1)        NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Rack_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Rack_History_EntryNum ON Tier2_Rack_History(EntryNumber);


--------------------------------------------------------------
-- Chassis Table
--    4 per Rack
-- - Info in this table is filled in either by
--    a)  System discovery process
--    b)  Provisioner adapter
--    c)  Service Operation post-processing
--    d)  Operator interface / WLM adapter
--------------------------------------------------------------
CREATE TABLE Chassis (
   Lctn                 VarChar(12)    UNIQUE   NOT NULL,   -- R123-CH00
   State                VarChar(1)              NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum               VarChar(50),                        -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                 VarChar(20),                        -- Type of item this is (i.e., Product Name)
   Vpd                  VarChar(4096),                      -- Item VPD information
   DbUpdatedTimestamp   TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP               NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   Owner                VarChar(1)              NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   PRIMARY KEY (Lctn)
);
PARTITION TABLE Chassis ON COLUMN Lctn;

--------------------------------------------------------------
-- Chassis history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Chassis table
--    b)  automatically by changes occurring in the Chassis table (i.e., db trigger or export functionality)
-- Subscription Routing Key: Chassis.State (or possibly Chassis.Lctn.State.Type)
--------------------------------------------------------------
CREATE TABLE Chassis_History (
   Lctn                 VarChar(12)             NOT NULL,   -- R123-CH00
   State                VarChar(1)              NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum               VarChar(50),                        -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                 VarChar(20),                        -- Type of item this is (i.e., Product Name)
   Vpd                  VarChar(4096),                      -- Item VPD information
   DbUpdatedTimestamp   TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP               NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   Owner                VarChar(1)              NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
);
PARTITION TABLE Chassis_History ON COLUMN Lctn;
CREATE INDEX ChassisHistoryByDbUpdatedTimestamp ON Chassis_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Chassis_History (
   Lctn                    VarChar(12)             NOT NULL,   -- R123-CH00
   State                   VarChar(1)              NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum                  VarChar(50),                        -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                    VarChar(20),                        -- Type of item this is (i.e., Product Name)
   Vpd                     VarChar(4096),                      -- Item VPD information
   DbUpdatedTimestamp      TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP               NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   Owner                   VarChar(1)              NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Tier2DbUpdatedTimestamp TIMESTAMP               NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt                  NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Chassis_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Chassis_History_EntryNum ON Tier2_Chassis_History(EntryNumber);


--------------------------------------------------------------
-- Compute Node Table
-- - Info in this table is filled in either by
--    a)  System discovery process
--    b)  Provisioner adapter
--    c)  Service Operation post-processing
--    d)  Operator interface / WLM adapter
--
-- NOTE: Whenever a service operation is performed, such that a nodes MAC Address OR IP Address changes!!!
--       It is necessary to make the appropriate changes in the CacheMacAddrToLctn and CacheIpAddrToLctn tables.
--       It is also necessary that the Provisioner Adapter is informed, so that the actual provisioner can be informed (so internal state is updated).
--------------------------------------------------------------
CREATE TABLE ComputeNode (
   Lctn                 VarChar(25)       UNIQUE NOT NULL,
   SequenceNumber       Integer           NOT NULL,         -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - R = Bios is being started due to a reset (the bios itself is being started due to a reset occurring) aka BiosStartedDueToReset
                                                            --    - S = Selecting boot device (selecting what device will be used for booting) aka SelectBootDevice
                                                            --    - P = PXE downloading NBP file (pxe is being used to download the Network Boot Program file) aka PxeDownloadingNbpFile
                                                            --    - B = Bios starting a network boot (the node's bios has started to perform a network boot) aka BiosStartingNetworkBoot
                                                            --    - D = Discovered (dhcp discover)
                                                            --    - I = IP address assigned (dhcp request)
                                                            --    - L = Started loading the linux boot image onto the node aka LoadingLinuxBootImage
                                                            --    - K = Starting kernel boot (kernel boot started)
                                                            --    - A = Active  (available, booted)
                                                            --    - H = Halting / Shutting Down (started to shutdown node)
                                                            --    - M = Missing (powered off)
                                                            --    - E = Error
                                                            --    - U = Unknown (do not yet know what state this node is in)
   HostName             VarChar(63),                        -- Compute nodes hostname
   BootImageId          VarChar(50),                        -- ID that identifies the BootTimages information that should be used when booting this node (see BootImage table)
   Environment          VarChar(240),                       -- Boot images can have multiple environments associated with them.
   IpAddr               VarChar(25),                        -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   MacAddr              VarChar(17),                        -- E.g., 52:54:00:fc:4a:87
   BmcIpAddr            VarChar(25),                        -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   BmcMacAddr           VarChar(17),                        -- E.g., 52:54:00:fc:4a:87
   BmcHostName          VarChar(63),
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                VarChar(1)        NOT NULL,         -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator           VarChar(63)       NOT NULL,         -- Location of the service node that "controls/owns" this node.
   InventoryTimestamp   TIMESTAMP,                          -- Time the event occurred that resulted in this inventory being changed.
   WlmNodeState         VarChar(1)        NOT NULL,         -- Indicates the state of this node from the WLM's perspective: "A" - available (WLM will use this node for running jobs), "U" - unavailable (WLM will not use this node for running jobs), "M" - maintenance (WLM will not use this node for running jobs), "X" - Unexpected/unknown value
   ConstraintId         VarChar(50),                        -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   ProofOfLifeTimestamp TIMESTAMP,                          -- Time that this node last reported Proof of Life.  This message is generated every 15 minutes by an active node.  Proves that node's serial console messages are flowing all the way into the Provisioner adapter.
   PRIMARY KEY (Lctn)
);
PARTITION TABLE ComputeNode ON COLUMN Lctn;
CREATE INDEX ComputeNodeState         on ComputeNode(State);
CREATE INDEX ComputeNodeSeqNum        on ComputeNode(SequenceNumber);
CREATE INDEX ComputeNodeMacAddr       on ComputeNode(MacAddr) WHERE MacAddr IS NOT NULL;
CREATE INDEX ComputeNodeAggregator    on ComputeNode(Aggregator);
CREATE INDEX ComputeNodeOwner         on ComputeNode(Owner);

--------------------------------------------------------------
-- Compute Node history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the ComputeNode table
--    b)  automatically by changes occurring in the ComputeNode table (i.e., db trigger or export functionality)
-- Subscription Routing Key: ComputeNode.State (or possibly ComputeNode.Lctn.State.Type)
--------------------------------------------------------------
CREATE TABLE ComputeNode_History (
   Lctn                 VarChar(25)       NOT NULL,
   SequenceNumber       Integer           NOT NULL,         -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - R = Bios is being started due to a reset (the bios itself is being started due to a reset occurring) aka BiosStartedDueToReset
                                                            --    - S = Selecting boot device (selecting what device will be used for booting) aka SelectBootDevice
                                                            --    - P = PXE downloading NBP file (pxe is being used to download the Network Boot Program file) aka PxeDownloadingNbpFile
                                                            --    - B = Bios starting a network boot (the node's bios has started to perform a network boot) aka BiosStartingNetworkBoot
                                                            --    - D = Discovered (dhcp discover)
                                                            --    - I = IP address assigned (dhcp request)
                                                            --    - L = Started loading the linux boot image onto the node aka LoadingLinuxBootImage
                                                            --    - K = Starting kernel boot (kernel boot started)
                                                            --    - A = Active  (available, booted)
                                                            --    - H = Halting / Shutting Down (started to shutdown node)
                                                            --    - M = Missing (powered off)
                                                            --    - E = Error
                                                            --    - U = Unknown (do not yet know what state this node is in)
   HostName             VarChar(63),                        -- Compute nodes hostname
   BootImageId          VarChar(50),                        -- ID that identifies the BootTimages information that should be used when booting this node (see BootImage table)
   Environment          VarChar(240),                       -- Boot images can have multiple environments associated with them.
   IpAddr               VarChar(25),                        -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   MacAddr              VarChar(17),                        -- E.g., 52:54:00:fc:4a:87
   BmcIpAddr            VarChar(25),                        -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   BmcMacAddr           VarChar(17),                        -- E.g., 52:54:00:fc:4a:87
   BmcHostName          VarChar(63),
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                VarChar(1)        NOT NULL,         -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator           VarChar(63)       NOT NULL,         -- Location of the service node that "controls/owns" this node.
   InventoryTimestamp   TIMESTAMP,                          -- Time the event occurred that resulted in this inventory being changed.
   WlmNodeState         VarChar(1)        NOT NULL,         -- Indicates the state of this node from the WLM's perspective: "A" - available (WLM will use this node for running jobs), "U" - unavailable (WLM will not use this node for running jobs), "M" - maintenance (WLM will not use this node for running jobs), "X" - Unexpected/unknown value
   ConstraintId         VarChar(50),                        -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   ProofOfLifeTimestamp TIMESTAMP,                          -- Time that this node last reported Proof of Life.  This message is generated every 15 minutes by an active node.  Proves that node's serial console messages are flowing all the way into the Provisioner adapter.
);
PARTITION TABLE ComputeNode_History ON COLUMN Lctn;
CREATE UNIQUE INDEX ComputeNodeHistoryByLctnAndLastChgTimestamp on ComputeNode_History(Lctn, LastChgTimestamp);
CREATE INDEX ComputeNodeHistoryByDbUpdatedTimestamp ON ComputeNode_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_ComputeNode_History (
   Lctn                    VarChar(25)       NOT NULL,
   SequenceNumber          Integer           NOT NULL,      -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   State                   VarChar(1)        NOT NULL,      -- Actual state that this item is in:
                                                            --    - R = Bios is being started due to a reset (the bios itself is being started due to a reset occurring) aka BiosStartedDueToReset
                                                            --    - S = Selecting boot device (selecting what device will be used for booting) aka SelectBootDevice
                                                            --    - P = PXE downloading NBP file (pxe is being used to download the Network Boot Program file) aka PxeDownloadingNbpFile
                                                            --    - B = Bios starting a network boot (the node's bios has started to perform a network boot) aka BiosStartingNetworkBoot
                                                            --    - D = Discovered (dhcp discover)
                                                            --    - I = IP address assigned (dhcp request)
                                                            --    - L = Started loading the linux boot image onto the node aka LoadingLinuxBootImage
                                                            --    - K = Starting kernel boot (kernel boot started)
                                                            --    - A = Active  (available, booted)
                                                            --    - H = Halting / Shutting Down (started to shutdown node)
                                                            --    - M = Missing (powered off)
                                                            --    - E = Error
                                                            --    - U = Unknown (do not yet know what state this node is in)
   HostName                VarChar(63),                     -- Compute nodes hostname
   BootImageId             VarChar(50),                     -- ID that identifies the BootTimages information that should be used when booting this node (see BootImage table)
   Environment             VarChar(240),                    -- Boot images can have multiple environments associated with them
   IpAddr                  VarChar(25),                     -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   MacAddr                 VarChar(17),                     -- E.g., 52:54:00:fc:4a:87
   BmcIpAddr               VarChar(25),                     -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   BmcMacAddr              VarChar(17),                     -- E.g., 52:54:00:fc:4a:87
   BmcHostName             VarChar(63),
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,      -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,      -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,      -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,      -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                   VarChar(1)        NOT NULL,      -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator              VarChar(63)       NOT NULL,      -- Location of the service node that "controls/owns" this node.
   InventoryTimestamp      TIMESTAMP,                       -- Time the event occurred that resulted in this inventory being changed.
   WlmNodeState            VarChar(1)        NOT NULL,      -- Indicates the state of this node from the WLM's perspective: "A" - available (WLM will use this node for running jobs), "U" - unavailable (WLM will not use this node for running jobs), "M" - maintenance (WLM will not use this node for running jobs), "X" - Unexpected/unknown value
   ConstraintId            VarChar(50),                     -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   ProofOfLifeTimestamp    TIMESTAMP,                       -- Time that this node last reported Proof of Life.  This message is generated every 15 minutes by an active node.  Proves that node's serial console messages are flowing all the way into the Provisioner adapter.
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,      -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,      -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_ComputeNode_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_ComputeNode_History_EntryNum ON Tier2_ComputeNode_History(EntryNumber);


--------------------------------------------------------------
-- Node Inventory History Table
--------------------------------------------------------------
CREATE TABLE NodeInventory_History (
   Lctn                 VarChar(25)       NOT NULL,
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   InventoryTimestamp   TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this inventory being changed.
                                                      -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                      --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   InventoryInfo        VarChar(65536),               -- Additional inventory details not part of the standard manifest, e.g. part numbers, CPU details (CPU ID, speed, sockets, hyper threads), memory module details (type, size, speed)
   Sernum               VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   BiosInfo             VarChar(30000),               -- Json string containing the bios information.
   PRIMARY KEY (Lctn, InventoryTimestamp)
);
PARTITION TABLE NodeInventory_History ON COLUMN Lctn;
CREATE INDEX NodeInventoryHistoryByDbUpdatedTimestamp ON NodeInventory_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_NodeInventory_History (
   Lctn                    VarChar(25)       NOT NULL,
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   InventoryTimestamp      TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this inventory being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   InventoryInfo           VarChar(65536),               -- Additional inventory details not part of the standard manifest, e.g. part numbers, CPU details (CPU ID, speed, sockets, hyper threads), memory module details (type, size, speed)
   Sernum                  VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   BiosInfo                VarChar(30000),               -- Json string containing the bios information.
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
   PRIMARY KEY (Lctn, InventoryTimestamp)
);
PARTITION TABLE Tier2_NodeInventory_History ON COLUMN Lctn;



--------------------------------------------------------------
-- Dimm Table (Memory)
--------------------------------------------------------------
CREATE TABLE Dimm (
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
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this dimm is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this dimm, rather the service operation is done on the node!
   PRIMARY KEY (NodeLctn, Lctn)
);
PARTITION TABLE Dimm ON COLUMN NodeLctn;
CREATE INDEX DimmByState ON Dimm(State);

--------------------------------------------------------------
-- Dimm History Table
--------------------------------------------------------------
CREATE TABLE Dimm_History (
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
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this dimm is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this dimm, rather the service operation is done on the node!
);
PARTITION TABLE Dimm_History ON COLUMN NodeLctn;
CREATE UNIQUE INDEX DimmHistoryByNodelctnLctnAndLastchgtimestamp ON Dimm_History(NodeLctn, Lctn, LastChgTimestamp);
CREATE INDEX DimmHistoryByDbUpdatedTimestamp ON Dimm_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Dimm_History (
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
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this dimm is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this dimm, rather the service operation is done on the node!
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Dimm_History ON COLUMN NodeLctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Dimm_History_EntryNum ON Tier2_Dimm_History(EntryNumber);



--------------------------------------------------------------
-- Processor (Socket) Table
--------------------------------------------------------------
CREATE TABLE Processor (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-P6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not populated, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   SocketDesignation    VarChar(25)       NOT NULL,         -- E.g., CPU0, CPU1
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Processor is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Processor, rather the service operation is done on the node!
   PRIMARY KEY (NodeLctn, Lctn)
);
PARTITION TABLE Processor ON COLUMN NodeLctn;
CREATE INDEX ProcessorByState ON Processor(State);

--------------------------------------------------------------
-- Processor History Table
--------------------------------------------------------------
CREATE TABLE Processor_History (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-P6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not installed, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   SocketDesignation    VarChar(25)       NOT NULL,         -- E.g., CPU0, CPU1
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Processor is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Processor, rather the service operation is done on the node!
);
PARTITION TABLE Processor_History ON COLUMN NodeLctn;
CREATE UNIQUE INDEX ProcessorHistoryByNodelctnLctnAndLastchgtimestamp ON Processor_History(NodeLctn, Lctn, LastChgTimestamp);
CREATE INDEX ProcessorHistoryByDbUpdatedTimestamp ON Processor_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Processor_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-P6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   SocketDesignation       VarChar(25)       NOT NULL,         -- E.g., CPU0, CPU1
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Processor is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this Processor, rather the service operation is done on the node!
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Processor_History ON COLUMN NodeLctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Processor_History_EntryNum ON Tier2_Processor_History(EntryNumber);



--------------------------------------------------------------
-- Accelerator Table
--------------------------------------------------------------
CREATE TABLE Accelerator (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-A6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not populated, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   BusAddr              VarChar(12),                        -- PCIE bus address, E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                 VarChar(10)       NOT NULL,         -- Slot that this device is in, e.g., 1, 2, 3, 4, 5, 6 (Accelerators)  7.1-1, 7.2-1, 8.1, 8.2 (HFIs)
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Accelerator is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Accelerator, rather the service operation is done on the node!
   PRIMARY KEY (NodeLctn, Lctn)
);
PARTITION TABLE Accelerator ON COLUMN NodeLctn;
CREATE INDEX AcceleratorByState ON Accelerator(State);

--------------------------------------------------------------
-- Accelerator History Table
--------------------------------------------------------------
CREATE TABLE Accelerator_History (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-A6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not installed, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   BusAddr              VarChar(12),                        -- PCIE bus address, E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                 VarChar(10)       NOT NULL,         -- Slot that this device is in, e.g., 1, 2, 3, 4, 5, 6 (Accelerators)  7.1-1, 7.2-1, 8.1, 8.2 (HFIs)
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Accelerator is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Accelerator, rather the service operation is done on the node!
);
PARTITION TABLE Accelerator_History ON COLUMN NodeLctn;
CREATE UNIQUE INDEX AcceleratorHistoryByNodelctnLctnAndLastchgtimestamp ON Accelerator_History(NodeLctn, Lctn, LastChgTimestamp);
CREATE INDEX AcceleratorHistoryByDbUpdatedTimestamp ON Accelerator_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Accelerator_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-A6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   BusAddr                 VarChar(12),                        -- PCIE bus address, E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                    VarChar(10)       NOT NULL,         -- Slot that this device is in, e.g., 1, 2, 3, 4, 5, 6 (Accelerators)  7.1-1, 7.2-1, 8.1, 8.2 (HFIs)
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Accelerator is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this Accelerator, rather the service operation is done on the node!
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Accelerator_History ON COLUMN NodeLctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Accelerator_History_EntryNum ON Tier2_Accelerator_History(EntryNumber);



--------------------------------------------------------------
-- Hfi (High-speed Fabric Interface) Table
--------------------------------------------------------------
CREATE TABLE Hfi (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-H6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not populated, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   BusAddr              VarChar(12),                        -- PCIE bus address, E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                 VarChar(10)       NOT NULL,         -- Slot that this device is in, e.g., 1, 2, 3, 4, 5, 6 (Accelerators)  7.1-1, 7.2-1, 8.1, 8.2 (HFIs)
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Hfi is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Hfi, rather the service operation is done on the node!
   PRIMARY KEY (NodeLctn, Lctn)
);
PARTITION TABLE Hfi ON COLUMN NodeLctn;
CREATE INDEX HfiState  ON Hfi(State);

--------------------------------------------------------------
-- Hfi History Table
--------------------------------------------------------------
CREATE TABLE Hfi_History (
   NodeLctn             VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                 VarChar(30)       NOT NULL,         -- R2-CH03-N2-H6
   State                VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                            --    - M = Missing (not installed, disabled)
                                                            --    - A = Active  (available, booted)
                                                            --    - E = Error
                                                            --    - U = Unknown
   BusAddr              VarChar(12),                        -- PCIE bus address, E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                 VarChar(10)       NOT NULL,         -- Slot that this device is in, e.g., 1, 2, 3, 4, 5, 6 (Accelerators)  7.1-1, 7.2-1, 8.1, 8.2 (HFIs)
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                            -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                            --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                       -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Hfi is actually recorded in the node's inventory info.
                                                            -- Note: there is not a Service Operation to replace this Hfi, rather the service operation is done on the node!
);
PARTITION TABLE Hfi_History ON COLUMN NodeLctn;
CREATE UNIQUE INDEX HfiHistoryByNodelctnLctnAndLastchgtimestamp ON Hfi_History(NodeLctn, Lctn, LastChgTimestamp);
CREATE INDEX HfiHistoryByDbUpdatedTimestamp ON Hfi_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Hfi_History (
   NodeLctn                VarChar(25)       NOT NULL,         -- R2-CH03-N2
   Lctn                    VarChar(30)       NOT NULL,         -- R2-CH03-N2-H6
   State                   VarChar(1)        NOT NULL,         -- Actual state that this item is in:
                                                               --    - M = Missing (not installed, disabled)
                                                               --    - A = Active  (available, booted)
                                                               --    - E = Error
                                                               --    - U = Unknown
   BusAddr                 VarChar(12),                        -- PCIE bus address, E.g., 0000:33:00.0, 0000:4d:00.0, 0000:b3:00.0
   Slot                    VarChar(10)       NOT NULL,         -- Slot that this device is in, e.g., 1, 2, 3, 4, 5, 6 (Accelerators)  7.1-1, 7.2-1, 8.1, 8.2 (HFIs)
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                               -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                               --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   ---InventoryTimestamp   TIMESTAMP,                          -- Note: there is no need for an InventoryTimestamp here as the inventory information for this Hfi is actually recorded in the node's inventory info.
                                                               -- Note: there is not a Service Operation to replace this Hfi, rather the service operation is done on the node!
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,         -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,         -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Hfi_History ON COLUMN NodeLctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Hfi_History_EntryNum ON Tier2_Hfi_History(EntryNumber);



--------------------------------------------------------------------------------
-- Aggregated Environmental Data Table - note this is not the raw telemetry, that is in Tier3.
--------------------------------------------------------------------------------
CREATE TABLE Tier2_AggregatedEnvData (
   Lctn                 VarChar(100)   NOT NULL,  -- Location this environmental data represents, e.g., R51-CH00-CN9, R48-SW0, R48-OSW3, R50-CH00-CN3-FAN5, R50-CH00-CN3-CPU1, R0-UPS1, SN0-PS6
   Timestamp            TIMESTAMP      NOT NULL,  -- Time that this environmental data entry occurred.
                                                  -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time, specified by the monitor component, that this data is for.
   Type                 VarChar(35)    NOT NULL,  -- Type of environmental data, e.g., Temp, AirFlow, RPM, CoolantFlow, VoltageIn, VoltageOut, CurrentIn, CurrentOut, Power, etc.
   MaximumValue         Float          NOT NULL,  -- Maximum value from all of the samples that occurred for this lctn during this interval
   MinimumValue         Float          NOT NULL,  -- Minimum value from all of the samples that occurred for this lctn during this interval
   AverageValue         Float          NOT NULL,  -- Average value from all of the samples that occurred for this lctn during this interval
   AdapterType          VarChar(20)    NOT NULL,  -- Type of adapter that inserted this data - may be needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   WorkItemId           BigInt         NOT NULL,  -- Work item id that the adapter was working on when it inserted this data - may be needed for work item recovery flows for failed adapters (-1 is used when there is no work item yet associated with this adapter)
   PRIMARY KEY (Lctn, Type, Timestamp)
);
PARTITION TABLE Tier2_AggregatedEnvData ON COLUMN Lctn;


--------------------------------------------------------------
-- CacheMacAddrToLctn Table
--------------------------------------------------------------
CREATE TABLE CacheMacAddrToLctn (
   MacAddr                 VarChar(17)    UNIQUE NOT NULL,  -- E.g., 52:54:00:fc:4a:87
   Lctn                    VarChar(25)    NOT NULL,         -- R51-CH00-CN9
   PRIMARY KEY (MacAddr)
);
PARTITION TABLE CacheMacAddrToLctn ON COLUMN MacAddr;

--------------------------------------------------------------
-- CacheIpAddrToLctn Table
--------------------------------------------------------------
CREATE TABLE CacheIpAddrToLctn (
   IpAddr                  VarChar(25)    UNIQUE NOT NULL,  -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   Lctn                    VarChar(25)    NOT NULL,         -- Rbf-CH3-CBf-PM3-CN1
   PRIMARY KEY (IpAddr)
);
PARTITION TABLE CacheIpAddrToLctn ON COLUMN IpAddr;


--------------------------------------------------------------
-- Service node table (nodes other than Compute Nodes)
-- - Info in this table is filled in either by
--    a)  Information from manifest
--    b)  Provisioner adapter
--    c)  Service Operation post-processing
--    d)  Operator interface / WLM adapter
--------------------------------------------------------------
CREATE TABLE ServiceNode (
   Lctn                 VarChar(30)    UNIQUE NOT NULL,  -- Location string
   SequenceNumber       Integer        NOT NULL,         -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   HostName             VarChar(63),                     -- Host name
   State                VarChar(1)     NOT NULL,         -- Actual state that this item is in - BiosStarting, Discovered, IP address assigned, Loading boot images, Kernel boot started, Active, Halting, Missing, Error
   BootImageId          VarChar(50),                     -- ID that identifies the BootTimages information that should be used when booting this node (see BootImage table)
   IpAddr               VarChar(25),
   MacAddr              VarChar(17),
   BmcIpAddr            VarChar(25),                     -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   BmcMacAddr           VarChar(17),                     -- E.g., 52:54:00:fc:4a:87
   BmcHostName          VarChar(63),
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP      NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)    NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt         NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                VarChar(1)     NOT NULL,         -- Indicates which subsystem "owns" this entity, e.g., "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator           VarChar(63)    NOT NULL,         -- Location of the service node that "controls/owns" this node.
   InventoryTimestamp   TIMESTAMP,                       -- Time the event occurred that resulted in this inventory being changed.
   ConstraintId         VarChar(50),                     -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   ProofOfLifeTimestamp TIMESTAMP,                       -- Time that this node last reported Proof of Life.  This message is generated every 15 minutes by an active node.  Proves that node's serial console messages are flowing all the way into the Provisioner adapter.
   PRIMARY KEY (Lctn)
);
PARTITION TABLE ServiceNode ON COLUMN Lctn;
CREATE INDEX ServiceNodeAggregator    on ServiceNode(Aggregator);
CREATE INDEX ServiceNodeSeqNum        on ServiceNode(SequenceNumber);
CREATE INDEX ServiceNodeOwner         on ServiceNode(Owner);


--------------------------------------------------------------
-- Service Node history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the ServiceNode table
--    b)  automatically by changes occurring in the ServiceNode table (i.e., db trigger or export functionality)
-- Subscription Routing Key: ServiceNode.State (or possibly ServiceNode.Lctn.State.Type.HostName)
--------------------------------------------------------------
CREATE TABLE ServiceNode_History (
   Lctn                 VarChar(30)    NOT NULL,         -- Location string
   SequenceNumber       Integer        NOT NULL,         -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   HostName             VarChar(63),                     -- Host name
   State                VarChar(1)     NOT NULL,         -- Actual state that this item is in - BiosStarting, Discovered, IP address assigned, Loading boot images, Kernel boot started, Active, Halting, Missing, Error
   BootImageId          VarChar(50),                     -- ID that identifies the BootTimages information that should be used when booting this node (see BootImage table)
   IpAddr               VarChar(25),
   MacAddr              VarChar(17),
   BmcIpAddr            VarChar(25),                     -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   BmcMacAddr           VarChar(17),                     -- E.g., 52:54:00:fc:4a:87
   BmcHostName          VarChar(63),
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,         -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP      NOT NULL,         -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)    NOT NULL,         -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt         NOT NULL,         -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                VarChar(1)     NOT NULL,         -- Indicates which subsystem "owns" this entity, e.g., "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator           VarChar(63)    NOT NULL,         -- Location of the service node that "controls/owns" this node.
   InventoryTimestamp   TIMESTAMP,                       -- Time the event occurred that resulted in this inventory being changed.
   ConstraintId         VarChar(50),                     -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   ProofOfLifeTimestamp TIMESTAMP,                       -- Time that this node last reported Proof of Life.  This message is generated every 15 minutes by an active node.  Proves that node's serial console messages are flowing all the way into the Provisioner adapter.
);
PARTITION TABLE ServiceNode_History ON COLUMN Lctn;
CREATE UNIQUE INDEX ServiceNodeHistoryByLctnAndLastChgTimestamp on ServiceNode_History(Lctn, LastChgTimestamp);
CREATE INDEX ServiceNodeHistoryByDbUpdatedTimestamp ON ServiceNode_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_ServiceNode_History (
   Lctn                    VarChar(30)    NOT NULL,      -- Location string
   SequenceNumber          Integer        NOT NULL,      -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   HostName                VarChar(63),                  -- Host name
   State                   VarChar(1)     NOT NULL,      -- Actual state that this item is in - BiosStarting, Discovered, IP address assigned, Loading boot images, Kernel boot started, Active, Halting, Missing, Error
   BootImageId             VarChar(50),                  -- ID that identifies the BootTimages information that should be used when booting this node (see BootImage table)
   IpAddr                  VarChar(25),
   MacAddr                 VarChar(17),
   BmcIpAddr               VarChar(25),                  -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   BmcMacAddr              VarChar(17),                  -- E.g., 52:54:00:fc:4a:87
   BmcHostName             VarChar(63),
   DbUpdatedTimestamp      TIMESTAMP      NOT NULL,      -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP      NOT NULL,      -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)    NOT NULL,      -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt         NOT NULL,      -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                   VarChar(1)     NOT NULL,      -- Indicates which subsystem "owns" this entity, e.g., "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator              VarChar(63)    NOT NULL,      -- Location of the service node that "controls/owns" this node.
   InventoryTimestamp      TIMESTAMP,                    -- Time the event occurred that resulted in this inventory being changed.
   ConstraintId            VarChar(50),                  -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   ProofOfLifeTimestamp    TIMESTAMP,                    -- Time that this node last reported Proof of Life.  This message is generated every 15 minutes by an active node.  Proves that node's serial console messages are flowing all the way into the Provisioner adapter.
   Tier2DbUpdatedTimestamp TIMESTAMP      NOT NULL,      -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt         NOT NULL,      -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_ServiceNode_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_ServiceNode_History_EntryNum ON Tier2_ServiceNode_History(EntryNumber);


--------------------------------------------------------------
-- ServiceOperation Table
-- - Info in this table is filled in by
--    a)  Service infrastructure
--------------------------------------------------------------
CREATE TABLE ServiceOperation (
   ServiceOperationId         BigInt         NOT NULL,   -- Unique id for this service operation
   Lctn                       VarChar(32)    NOT NULL,   -- Hardware location specified for this service operation
   TypeOfServiceOperation     VarChar(32)    NOT NULL,   -- Type of Service Operation that is being performed
                                                         --      Repair (FRU repair or replacement)
                                                         --      Exclusive (Exclusive engineering access)
                                                         --      FirmwareUpdate (Update firmware)
                                                         --      Diagnostic (Run hardware diagnostics)
   UserStartedService         VarChar(32)    NOT NULL,   -- User that started this service operation
   UserStoppedService         VarChar(32),               -- User that stopped this service operation
   State                      VarChar(1)     NOT NULL,   -- Actual state of this service operation
                                                         --      Open
                                                         --      Prepare
                                                         --      End
                                                         --      Close
   Status                     VarChar(1)     NOT NULL,   -- Status within the aforementioned State
                                                         --      Active
                                                         --      Prepared
                                                         --      Error
                                                         --      Closed
                                                         --      ForceClosed
   StartTimestamp             TIMESTAMP      NOT NULL,   -- Time that this service operation was started
   StopTimestamp              TIMESTAMP,                 -- Time that this service operation was ended
   StartRemarks               VarChar(256),              -- Remarks entered during start of this service operation
   StopRemarks                VarChar(256),              -- Remarks entered during stop  of this service operation
   DbUpdatedTimestamp         TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   LogFile                    VarChar(256)   NOT NULL,   -- Log file containing information that occurs during life of service operation
);
PARTITION TABLE ServiceOperation    ON COLUMN Lctn;
CREATE UNIQUE INDEX ServiceOperationByLctn ON ServiceOperation(Lctn);

--------------------------------------------------------------
-- ServiceOperation_History Table
-- - Info in this table is filled in by
--    a)  Service infrastructure
-- Subscription Routing Key: ServiceOperation.State (or possibly ServiceOperation.Lctn.State)
--------------------------------------------------------------
CREATE TABLE ServiceOperation_History (
   ServiceOperationId         BigInt         NOT NULL,   -- Unique id for this service operation
   Lctn                       VarChar(32)    NOT NULL,   -- Hardware location specified for this service operation
   TypeOfServiceOperation     VarChar(32)    NOT NULL,   -- Type of Service Operation that is being performed
                                                         --      Repair (FRU repair or replacement)
                                                         --      Exclusive (Exclusive engineering access)
                                                         --      FirmwareUpdate (Update firmware)
                                                         --      Diagnostic (Run hardware diagnostics)
   UserStartedService         VarChar(32)    NOT NULL,   -- User that started this service operation
   UserStoppedService         VarChar(32),               -- User that stopped this service operation
   State                      VarChar(1)     NOT NULL,   -- Actual state of this service operation
                                                         --      Open
                                                         --      Prepare
                                                         --      End
                                                         --      Close
   Status                     VarChar(1)     NOT NULL,   -- Status within the aforementioned State
                                                         --      Active
                                                         --      Prepared
                                                         --      Error
                                                         --      Closed
                                                         --      ForceClosed
   StartTimestamp             TIMESTAMP      NOT NULL,   -- Time that this service operation was started
   StopTimestamp              TIMESTAMP,                 -- Time that this service operation was ended
   StartRemarks               VarChar(256),              -- Remarks entered during start of this service operation
   StopRemarks                VarChar(256),              -- Remarks entered during stop  of this service operation
   DbUpdatedTimestamp         TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   LogFile                    VarChar(256)   NOT NULL,   -- Log file containing information that occurs during life of service operation
);
PARTITION TABLE ServiceOperation_History ON COLUMN Lctn;
CREATE UNIQUE INDEX ServiceOperationHistoryByLctnAndDbUpdatedTimestamp on ServiceOperation_History(Lctn, DbUpdatedTimestamp);
CREATE INDEX ServiceOperationHistoryByDbUpdatedTimestamp ON ServiceOperation_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_ServiceOperation_History (
   ServiceOperationId         BigInt         NOT NULL,   -- Unique id for this service operation
   Lctn                       VarChar(32)    NOT NULL,   -- Hardware location specified for this service operation
   TypeOfServiceOperation     VarChar(32)    NOT NULL,   -- Type of Service Operation that is being performed
                                                         --      Repair (FRU repair or replacement)
                                                         --      Exclusive (Exclusive engineering access)
                                                         --      FirmwareUpdate (Update firmware)
                                                         --      Diagnostic (Run hardware diagnostics)
   UserStartedService         VarChar(32)    NOT NULL,   -- User that started this service operation
   UserStoppedService         VarChar(32),               -- User that stopped this service operation
   State                      VarChar(1)     NOT NULL,   -- Actual state of this service operation
                                                         --      Open
                                                         --      Prepare
                                                         --      End
                                                         --      Close
   Status                     VarChar(1)     NOT NULL,   -- Status within the aforementioned State
                                                         --      Active
                                                         --      Prepared
                                                         --      Error
                                                         --      Closed
                                                         --      ForceClosed
   StartTimestamp             TIMESTAMP      NOT NULL,   -- Time that this service operation was started
   StopTimestamp              TIMESTAMP,                 -- Time that this service operation was ended
   StartRemarks               VarChar(256),              -- Remarks entered during start of this service operation
   StopRemarks                VarChar(256),              -- Remarks entered during stop  of this service operation
   DbUpdatedTimestamp         TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   LogFile                    VarChar(256)   NOT NULL,   -- Log file containing information that occurs during life of service operation
   Tier2DbUpdatedTimestamp    TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber                BigInt         NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_ServiceOperation_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_ServiceOperation_History_EntryNum ON Tier2_ServiceOperation_History(EntryNumber);


--------------------------------------------------------------
-- Replacement History Table
-- - Info in this table is filled in either by
--    a)  Service Operations
--    b)  Provisioner adapter
-- Subscription Routing Key: Replacement_History.NewState (or possibly Replacement_History.Lctn.NewState)
--------------------------------------------------------------
CREATE TABLE Replacement_History (
   Lctn                 Varchar(100)    NOT NULL,  -- Hardware location where FRU was replaced
   FruType              Varchar(30)     NOT NULL,  -- Type of FRU that was replaced (i.e., Product Name)
   ServiceOperationId   BigInt,                    -- ID of the Service Operation that resulted in this replacement
                                                   --    -99999 is used to indicate that replacement was detected outside of a Service Operation
   OldSernum            VarChar(50),               -- Serial number of the FRU that was replaced (i.e., Product Serial)
   NewSernum            VarChar(50)     NOT NULL,  -- Serial number of the replacement FRU (i.e., Product Serial)
   OldState             VarChar(1)      NOT NULL,  -- State of the FRU that was replaced (before the Service Operation occurred) [Needed??]
   NewState             VarChar(1)      NOT NULL,  -- State of the replacement FRU after replacement  [Needed??]
   DbUpdatedTimestamp   TIMESTAMP       NOT NULL,  -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP       NOT NULL   -- Time the event occurred that resulted in this entry being changed.
);
PARTITION TABLE Replacement_History                 ON COLUMN Lctn;
CREATE INDEX ReplacementHistoryByLctn               ON Replacement_History(Lctn);
CREATE INDEX ReplacementHistoryByOldSernum          ON Replacement_History(OldSernum) WHERE OldSernum IS NOT NULL;
CREATE INDEX ReplacementHistoryByDbUpdatedTimestamp ON Replacement_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Replacement_History (
   Lctn                       Varchar(100)   NOT NULL,   -- Hardware location where FRU was replaced
   FruType                    Varchar(30)    NOT NULL,   -- Type of FRU that was replaced (i.e., Product Name)
   ServiceOperationId         BigInt,                    -- ID of the Service Operation that resulted in this replacement
   OldSernum                  VarChar(50),               -- Serial number of the FRU that was replaced (i.e., Product Serial)
   NewSernum                  VarChar(50)    NOT NULL,   -- Serial number of the replacement FRU (i.e., Product Serial)
   OldState                   VarChar(1)     NOT NULL,   -- State of the FRU that was replaced (before the Service Operation occurred) [Needed??]
   NewState                   VarChar(1)     NOT NULL,   -- State of the replacement FRU after replacement  [Needed??]
   DbUpdatedTimestamp         TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp           TIMESTAMP      NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   Tier2DbUpdatedTimestamp    TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber                BigInt         NOT NULL    -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Replacement_History          ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Replacement_History_EntryNum ON Tier2_Replacement_History(EntryNumber);


--------------------------------------------------------------
-- RAS Event Table
--    Subscription Routing Key: RasEvent (or possibly RasEvent.Lctn.ControlOperation)
--------------------------------------------------------------
CREATE TABLE RasEvent (
   Id                   BigInt         NOT NULL,               -- Identifier that uniquely identifies a specific instance WITHIN a specific DescriptiveName (it is not unique over all ras events, but unique for all instances of specific DescriptiveName e.g., RasGenAdapterAbend)
   DescriptiveName      VarChar(65)    NOT NULL,               -- Descriptive name for this type of event, e.g., RasGenAdapterAbend, RasWorkItemFindAndOwnFailed
   Lctn                 VarChar(100),                          -- Location of the hardware that the event occurred on
   Sernum               VarChar(50),                           -- Serial number of the piece of hw the event occurred on
   JobId                VarChar(30),                           -- WLMs job id, external value e.g., 1234567.head-5
                                                               --    Note: value of null indicates that the generator of this RAS Event is declaring that NO job was effected by the "event" whose occurrence caused the logging of this ras event.
                                                               --    Note: value of "?"  indicates that the generator of this RAS Event does not know whether or not any job was effected by the "event" whose occurrence caused the logging of this ras event.
                                                               --    Note: value of JobId that the generator of this RAS Event knows was effected by the "event" whose occurrence caused the logging of this ras event.
   NumberRepeats        Integer        DEFAULT 0 NOT NULL,     -- Number of times this event occurred w/i the interval
   ControlOperation     VarChar(50),                           -- Control Operation that should be taken as a result of this event occurring - ErrorOnComputeNode, ErrorAndKillJobOnComputeNode, ErrorAndPwrOffComputeNode, ErrorAndKillJobAndPwrOffComputeNode, IncreaseFanSpeed, ... (this value may be overridden from the default in certain instances)
   Done                 VarChar(1)     DEFAULT 'N' NOT NULL,   -- Flag indicating whether or not everything has been "done" for this event - e.g., is the JobId filled in if requested, has the ControlOperation been invoked, etc. - Y (done with this event), N (event still needs RAS adapter to do work for it).
   InstanceData         VarChar(10000),                        -- Data specific to this instance of the event, may be appended to the Event message to have the complete information
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,               -- Time that this record was recorded in the database.  It is the actual time that the db insert/update occurred.  This is different than LastChgTimestamp field.
   LastChgTimestamp     TIMESTAMP      NOT NULL,               -- Time that the event that caused this RAS Event to be generated occurred.  This is NOT the time that the event was put into the data store.  This is different than the DbUpdatedTimestamp field.
   LastChgAdapterType   VarChar(20)    NOT NULL,               -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt         NOT NULL                -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
);
PARTITION TABLE RasEvent ON COLUMN DescriptiveName;
CREATE UNIQUE INDEX RasEventByDescriptiveNameAndId ON RasEvent(DescriptiveName, Id);

CREATE INDEX RasEventsNotDoneByDbUpdatedTimestamp  ON RasEvent(DbUpdatedTimestamp) WHERE Done != 'Y';  -- Note: do NOT change this to "WHERE Done = 'N'" w/o also changing the stored procedure that is using this.

CREATE INDEX RasEventByLctnAndTime                 ON RasEvent(Lctn, LastChgTimestamp) WHERE Lctn IS NOT NULL;
CREATE INDEX RasEventByDbUpdatedTimestamp          ON RasEvent(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_RasEvent (
   Id                      BigInt         NOT NULL,            -- Identifier that uniquely identifies a specific instance WITHIN a specific DescriptiveName (it is not unique over all ras events, but unique for all instances of specific DescriptiveName e.g., RasGenAdapterAbend)
   DescriptiveName         VarChar(65)    NOT NULL,            -- Descriptive name for this type of event, e.g., RasGenAdapterAbend, RasWorkItemFindAndOwnFailed
   Lctn                    VarChar(100),                       -- Location of the hardware that the event occurred on
   Sernum                  VarChar(50),                        -- Serial number of the piece of hw the event occurred on
   JobId                   VarChar(30),                        -- WLMs job id, external value e.g., 1234567.head-5
                                                               --    Note: value of null indicates that the generator of this RAS Event is declaring that NO job was effected by the "event" whose occurrence caused the logging of this ras event.
                                                               --    Note: value of "?"  indicates that the generator of this RAS Event does not know whether or not any job was effected by the "event" whose occurrence caused the logging of this ras event.
                                                               --    Note: value of JobId that the generator of this RAS Event knows was effected by the "event" whose occurrence caused the logging of this ras event.
   NumberRepeats           Integer        NOT NULL,            -- Number of times this event occurred w/i the interval
   ControlOperation        VarChar(50),                        -- Control Operation that should be taken as a result of this event occurring - ErrorOnComputeNode, ErrorAndKillJobOnComputeNode, ErrorAndPwrOffComputeNode, ErrorAndKillJobAndPwrOffComputeNode, IncreaseFanSpeed, ... (this value may be overridden from the default in certain instances)
   Done                    VarChar(1)     NOT NULL,            -- Flag indicating whether or not everything has been "done" for this event - e.g., is the JobId filled in if requested, has the ControlOperation been invoked, etc. - Y (done with this event), N (event still needs RAS adapter to do work for it).
   InstanceData            VarChar(10000),                     -- Data specific to this instance of the event, may be appended to the Event message to have the complete information
   DbUpdatedTimestamp      TIMESTAMP      NOT NULL,            -- Time that this record was recorded in the database.  It is the actual time that the db insert/update occurred.  This is different than LastChgTimestamp field.
   LastChgTimestamp        TIMESTAMP      NOT NULL,            -- Time that the event that caused this RAS Event to be generated occurred.  This is NOT the time that the event was put into the data store.  This is different than the DbUpdatedTimestamp field.
   LastChgAdapterType      VarChar(20)    NOT NULL,            -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt         NOT NULL,            -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Tier2DbUpdatedTimestamp TIMESTAMP      NOT NULL,            -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt         NOT NULL,            -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
   PRIMARY KEY (DescriptiveName, Id)
);
PARTITION TABLE Tier2_RasEvent ON COLUMN DescriptiveName;
CREATE UNIQUE INDEX Tier2_RasEventByDescriptiveNameAndId ON Tier2_RasEvent(DescriptiveName, Id);
CREATE ASSUMEUNIQUE INDEX Tier2_RasEvent_EntryNum ON Tier2_RasEvent(EntryNumber);


--------------------------------------------------------------
-- Meta data for each of the different RAS Event Types (DescriptiveName)
--------------------------------------------------------------
CREATE TABLE RasMetaData (
   DescriptiveName      VarChar(65)    NOT NULL,               -- Descriptive name for this type of event, e.g., RasGenAdapterAbend, RasWorkItemFindAndOwnFailed
   Severity             VarChar(10)    NOT NULL,               -- FATAL, ERROR, WARN, INFO, DEBUG
   Category             VarChar(20)    NOT NULL,               -- Category of RAS event this belongs to - DAI, Adapter, Memory, Processor, Ethernet, Fan, Temp, ...
   Component            VarChar(50)    NOT NULL,               -- Component that owns this RAS event - AdapterProvisioner, AdapterOnlineTier, AdapterRas, AdapterWlm, AdapterGeneric, RmRte, DataStore, ...
   ControlOperation     VarChar(50),                           -- Control Operation that should be taken as a result of this event occurring - ErrorOnComputeNode, ErrorAndPwrOffComputeNode, IncreaseFanSpeed, ErrorBlade, ErrorCdu, ErrorPdu, StopJob, FreeJob, ...
   Msg                  VarChar(1000),
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,               -- Time that this record was recorded in the database.  It is the actual time that the db insert/update occurred.
   GenerateAlert        VarChar(1)     DEFAULT 'N' NOT NULL,   -- Flag indicating whether or not the AlertMgr should automatically generate an alert for this RAS event.
   PRIMARY KEY (DescriptiveName)
);
PARTITION TABLE RasMetaData ON COLUMN DescriptiveName;
CREATE INDEX RasMetaDataByDbUpdatedTimestamp ON RasMetaData(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_RasMetaData (
   DescriptiveName         VarChar(65)    NOT NULL,               -- Descriptive name for this type of event, e.g., RasGenAdapterAbend, RasWorkItemFindAndOwnFailed
   Severity                VarChar(10)    NOT NULL,               -- FATAL, ERROR, WARN, INFO, DEBUG
   Category                VarChar(20)    NOT NULL,               -- Category of RAS event this belongs to - DAI, Adapter, Memory, Processor, Ethernet, Fan, Temp, ...
   Component               VarChar(50)    NOT NULL,               -- Component that owns this RAS event - AdapterProvisioner, AdapterOnlineTier, AdapterRas, AdapterWlm, AdapterGeneric, RmRte, DataStore, ...
   ControlOperation        VarChar(50),                           -- Control Operation that should be taken as a result of this event occurring - ErrorOnComputeNode, ErrorAndPwrOffComputeNode, IncreaseFanSpeed, ErrorBlade, ErrorCdu, ErrorPdu, StopJob, FreeJob, ...
   Msg                     VarChar(1000),
   DbUpdatedTimestamp      TIMESTAMP      NOT NULL,               -- Time that this record was recorded in the database.  It is the actual time that the db insert/update occurred.
   GenerateAlert           VarChar(1)     DEFAULT 'N' NOT NULL,   -- Flag indicating whether or not the AlertMgr should automatically generate an alert for this RAS event Y=yes, N=no.
   Tier2DbUpdatedTimestamp TIMESTAMP      NOT NULL,               -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt         NOT NULL,               -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_RasMetaData ON COLUMN DescriptiveName;
--CREATE ASSUMEUNIQUE INDEX Tier2_RasMetaData_EntryNum ON Tier2_RasMetaData(EntryNumber);


--------------------------------------------------------------
-- WorkItem Table
--------------------------------------------------------------
CREATE TABLE WorkItem (
   Queue                   VarChar(20),            -- Type of work item this is (what is this item about - e.g., Job, Hardware, RAS, EnvData)
   WorkingAdapterType      VarChar(20) NOT NULL,   -- Type of adapter that should work on this item (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   Id                      BigInt      NOT NULL,   -- Generated work item id that when combined with the WorkingAdapterType is unique i.e., this id is unique within the context of a WorkingAdapterType.
   WorkToBeDone            VarChar(40) NOT NULL,   -- Requested work (what specific work was requested)
   Parameters              VarChar(15000),         -- Parameters for this work item (may be in JSON format)
   NotifyWhenFinished      VarChar(1)  NOT NULL,   -- Flag indicating whether requester wants to be informed when this work item is finished
                                                   --    T = Wants normal default flow (all acks will be returned)
                                                   --    F = Shortcut, does not want any notification when this work item is finished
   State                   VarChar(1)  NOT NULL,   -- Actual state that this item is in
                                                   -- - 'Q' - Queued - this work item has been created and inserted into the WorkItem table
                                                   -- - 'W' - Working - work item has been grabbed by an adapter (WorkingAdapterId) and is being worked on
                                                   -- - 'R' - Requeued - this item has been requeued/restarted due to an adapter "failure" within the adapter that had been working on this work item. The WorkingResults field can be used for restarting this item.
                                                   -- - 'F' - Finished - all processing for this work item has been finished from the WorkingAdapterId’s point of view BUT this work item has not yet been marked as done. The Results field contains the results from this work.
                                                   -- - 'E' - Finished DUE TO ERROR - all processing for this work item has ended - Note: it did fail though, i.e., did not complete successfully. This work item has not yet been marked as done. The Results field contains the results from this work.
                                                   -- - 'D' - Done-done - Work item has been marked as done-done. Note: items marked as done-done will be put into the WorkItem_History table and then removed from this table (WorkItem)
   RequestingWorkItemId    BigInt      NOT NULL,   -- Which work item id requested this new work item (aka sender)
   RequestingAdapterType   VarChar(20) NOT NULL,   -- What type of adapter requested this new work item (aka sender)
   WorkingAdapterId        BigInt,                 -- Which adapter is working on this work item
   WorkingResults          VarChar(15000),         -- Intermediate results that can be used when working on this work item, e.g., the working adapter might want to create addtl work items to accomplish the overarching work item (may be in JSON format)
   Results                 VarChar(262144),        -- "Final" results for this work item (may be in JSON format)
   StartTimestamp          TIMESTAMP   NOT NULL,   -- Time this WorkItem was created
   DbUpdatedTimestamp      TIMESTAMP   NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
PARTITION TABLE WorkItem ON COLUMN WorkingAdapterType;
CREATE        INDEX WorkItemByAdapterTypeQueueAndId   on WorkItem(WorkingAdapterType, Queue, Id);
CREATE UNIQUE INDEX WorkItemByAdapterTypeAndId  on WorkItem(WorkingAdapterType, Id);

--------------------------------------------------------------
-- WorkItem history Table
-- Subscription Routing Key: WorkItem.State (or possibly WorkItem.State.WorkingAdapterType.NotifyWhenFinished)
--------------------------------------------------------------
CREATE TABLE WorkItem_History (
   Queue                   VarChar(20),            -- Type of work item this is (what is this item about - e.g., Job, Hardware, RAS, EnvData)
   WorkingAdapterType      VarChar(20) NOT NULL,   -- Type of adapter that should work on this item (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   Id                      BigInt      NOT NULL,   -- Generated work item id that when combined with the WorkingAdapterType is unique i.e., this id is unique within the context of a WorkingAdapterType.
   WorkToBeDone            VarChar(40) NOT NULL,   -- Requested work (what specific work was requested)
   Parameters              VarChar(15000),         -- Parameters for this work item (may be in JSON format)
   NotifyWhenFinished      VarChar(1)  NOT NULL,   -- Flag indicating whether requester wants to be informed when this work item is finished
                                                   --    T = Wants normal default flow (all acks will be returned)
                                                   --    F = Shortcut, does not want any notification when this work item is finished
   State                   VarChar(1)  NOT NULL,   -- Actual state that this item is in
                                                   -- - 'Q' - Queued - this work item has been created and inserted into the WorkItem table
                                                   -- - 'W' - Working - work item has been grabbed by an adapter (WorkingAdapterId) and is being worked on
                                                   -- - 'R' - Requeued - this item has been requeued/restarted due to an adapter "failure" within the adapter that had been working on this work item. The WorkingResults field can be used for restarting this item.
                                                   -- - 'F' - Finished - all processing for this work item has been finished from the WorkingAdapterId’s point of view BUT this work item has not yet been marked as done. The Results field contains the results from this work.
                                                   -- - 'E' - Finished DUE TO ERROR - all processing for this work item has ended - Note: it did fail though, i.e., did not complete successfully. This work item has not yet been marked as done. The Results field contains the results from this work.
                                                   -- - 'D' - Done-done - Work item has been marked as done-done. Note: items marked as done-done will be put into the WorkItem_History table and then removed from this table (WorkItem)
   RequestingWorkItemId    BigInt      NOT NULL,   -- Which work item id requested this new work item (aka sender)
   RequestingAdapterType   VarChar(20) NOT NULL,   -- What type of adapter requested this new work item (aka sender)
   WorkingAdapterId        BigInt,                 -- Which adapter is working on this work item
   WorkingResults          VarChar(15000),         -- Intermediate results that can be used when working on this work item, e.g., the working adapter might want to create addtl work items to accomplish the overarching work item (may be in JSON format)
   Results                 VarChar(262144),        -- "Final" results for this work item (may be in JSON format)
   StartTimestamp          TIMESTAMP   NOT NULL,   -- Time this WorkItem was created
   DbUpdatedTimestamp      TIMESTAMP   NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   EndTimestamp            TIMESTAMP,              -- Time this work item was completely done
                                                   --    If NotifyWhenFinished=T, EndTimestamp will be filled in when Requester marks this item as done-done
                                                   --    If NotifyWhenFinished=F, EndTimestamp will be filled in when WorkingAdapterId has finished all of its processing
   RowInsertedIntoHistory  VarChar(1)  NOT NULL,   -- Flag indicating whether this history record was inserted into the history table OR if it was updated
                                                   --    T = Normal flow, this record was inserted into the history table
                                                   --    F = Unusual flow, this record was updated in the history table (only used for special cases in which the WorkItems WorkingResults field is updated very frequently)
);
PARTITION TABLE WorkItem_History ON COLUMN WorkingAdapterType;
CREATE UNIQUE INDEX WorkItemHistoryByAdapterTypeAndIdAndEndtime on WorkItem_History(WorkingAdapterType, Id, EndTimestamp) WHERE EndTimestamp IS NOT NULL;
CREATE INDEX WorkItemHistoryByDbUpdatedTimestamp on WorkItem_History(DbUpdatedTimestamp);
CREATE INDEX WorkItemHistoryByAdapterTypeAndIdAndState on WorkItem_History(WorkingAdapterType, Id, State);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_WorkItem_History (
   Queue                   VarChar(20),            -- Type of work item this is (what is this item about - e.g., Job, Hardware, RAS, EnvData)
   WorkingAdapterType      VarChar(20) NOT NULL,   -- Type of adapter that should work on this item (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   Id                      BigInt      NOT NULL,   -- Generated work item id that when combined with the WorkingAdapterType is unique i.e., this id is unique within the context of a WorkingAdapterType.
   WorkToBeDone            VarChar(40) NOT NULL,   -- Requested work (what specific work was requested)
   Parameters              VarChar(15000),         -- Parameters for this work item (may be in JSON format)
   NotifyWhenFinished      VarChar(1)  NOT NULL,   -- Flag indicating whether requester wants to be informed when this work item is finished
                                                   --    T = Wants normal default flow (all acks will be returned)
                                                   --    F = Shortcut, does not want any notification when this work item is finished
   State                   VarChar(1)  NOT NULL,   -- Actual state that this item is in
                                                   -- - 'Q' - Queued - this work item has been created and inserted into the WorkItem table
                                                   -- - 'W' - Working - work item has been grabbed by an adapter (WorkingAdapterId) and is being worked on
                                                   -- - 'R' - Requeued - this item has been requeued/restarted due to an adapter "failure" within the adapter that had been working on this work item. The WorkingResults field can be used for restarting this item.
                                                   -- - 'F' - Finished - all processing for this work item has been finished from the WorkingAdapterId’s point of view BUT this work item has not yet been marked as done. The Results field contains the results from this work.
                                                   -- - 'E' - Finished DUE TO ERROR - all processing for this work item has ended - Note: it did fail though, i.e., did not complete successfully. This work item has not yet been marked as done. The Results field contains the results from this work.
                                                   -- - 'D' - Done-done - Work item has been marked as done-done. Note: items marked as done-done will be put into the WorkItem_History table and then removed from this table (WorkItem)
   RequestingWorkItemId    BigInt      NOT NULL,   -- Which work item id requested this new work item (aka sender)
   RequestingAdapterType   VarChar(20) NOT NULL,   -- What type of adapter requested this new work item (aka sender)
   WorkingAdapterId        BigInt,                 -- Which adapter is working on this work item
   WorkingResults          VarChar(15000),         -- Intermediate results that can be used when working on this work item, e.g., the working adapter might want to create addtl work items to accomplish the overarching work item (may be in JSON format)
   Results                 VarChar(262144),        -- "Final" results for this work item (may be in JSON format)
   StartTimestamp          TIMESTAMP   NOT NULL,   -- Time this WorkItem was created
   DbUpdatedTimestamp      TIMESTAMP   NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   EndTimestamp            TIMESTAMP,              -- Time this work item was completely done
                                                   --    If NotifyWhenFinished=T, EndTimestamp will be filled in when Requester marks this item as done-done
                                                   --    If NotifyWhenFinished=F, EndTimestamp will be filled in when WorkingAdapterId has finished all of its processing
   RowInsertedIntoHistory  VarChar(1)  NOT NULL,   -- Flag indicating whether this history record was inserted into the history table OR if it was updated
                                                   --    T = Normal flow, this record was inserted into the history table
                                                   --    F = Unusual flow, this record was updated in the history table (only used for special cases in which the WorkItems WorkingResults field is updated very frequently)
   Tier2DbUpdatedTimestamp TIMESTAMP   NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt      NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_WorkItem_History ON COLUMN WorkingAdapterType;
CREATE ASSUMEUNIQUE INDEX Tier2_WorkItem_History_EntryNum ON Tier2_WorkItem_History(EntryNumber);


--------------------------------------------------------------
-- Adapter Table
--------------------------------------------------------------
CREATE TABLE Adapter (
   Id                   BigInt      NOT NULL,   -- Uniquely identifies this adapter
   AdapterType          VarChar(20) NOT NULL,   -- Type of adapter that should work on this item (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, DIAGNOSTICS, DAI_MGR, SERVICE, INITIALIZATION, etc.)
   SconRank             BigInt      NOT NULL,   -- Information needed in order to communicate with this adapter
   State                VarChar(1)  NOT NULL,   -- Actual state that this item is in
                                                -- - Active
                                                -- - Missing
                                                -- - Error
                                                -- - Terminated
   DbUpdatedTimestamp   TIMESTAMP   NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   LastChgAdapterType   VarChar(20) NOT NULL,   -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, ALERT_MGR, etc.)
   LastChgWorkItemId    BigInt      NOT NULL,   -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Lctn                 VarChar(25) NOT NULL,   -- Lctn this adapter instance was started on/for
   Pid                  BigInt      NOT NULL,   -- Pid (process id) for this adapter instance
);
PARTITION TABLE Adapter ON COLUMN AdapterType;
CREATE UNIQUE INDEX AdapterByAdapterTypeAndId on Adapter(AdapterType, Id);

--------------------------------------------------------------
-- Adapter history Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Adapter table
--    b)  automatically by changes occurring in the Adapter table (i.e., db trigger or export functionality)
-- Subscription Routing Key: Adapter.State (or possibly Adapter.State.AdapterType)
--------------------------------------------------------------
CREATE TABLE Adapter_History (
   Id                   BigInt      NOT NULL,   -- Uniquely identifies this adapter
   AdapterType          VarChar(20) NOT NULL,   -- Type of adapter that should work on this item (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   SconRank             BigInt      NOT NULL,   -- Information needed in order to communicate with this adapter
   State                VarChar(1)  NOT NULL,   -- Actual state that this item is in
                                                -- - Active
                                                -- - Missing
                                                -- - Error
                                                -- - Terminated
   DbUpdatedTimestamp   TIMESTAMP   NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   LastChgAdapterType   VarChar(20) NOT NULL,   -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, ALERT_MGR, etc.)
   LastChgWorkItemId    BigInt      NOT NULL,   -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Lctn                 VarChar(25) NOT NULL,   -- Lctn this adapter instance was started on/for
   Pid                  BigInt      NOT NULL,   -- Pid (process id) for this adapter instance
);
PARTITION TABLE Adapter_History ON COLUMN AdapterType;
CREATE INDEX AdapterHistoryByDbUpdatedTimestamp ON Adapter_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Adapter_History (
   Id                       BigInt      NOT NULL,  -- Uniquely identifies this adapter
   AdapterType              VarChar(20) NOT NULL,  -- Type of adapter that should work on this item (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   SconRank                 BigInt      NOT NULL,  -- Information needed in order to communicate with this adapter
   State                    VarChar(1)  NOT NULL,  -- Actual state that this item is in
                                                   -- - Active
                                                   -- - Missing
                                                   -- - Error
                                                   -- - Terminated
   DbUpdatedTimestamp       TIMESTAMP   NOT NULL,  -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   LastChgAdapterType       VarChar(20) NOT NULL,  -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, ALERT_MGR, etc.)
   LastChgWorkItemId        BigInt      NOT NULL,  -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Lctn                     VarChar(25) NOT NULL,  -- Lctn this adapter instance was started on/for
   Pid                      BigInt      NOT NULL,  -- Pid (process id) for this adapter instance
   Tier2DbUpdatedTimestamp  TIMESTAMP   NOT NULL,  -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber              BigInt      NOT NULL,  -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Adapter_History ON COLUMN AdapterType;
CREATE ASSUMEUNIQUE INDEX Tier2_Adapter_History_EntryNum ON Tier2_Adapter_History(EntryNumber);


--------------------------------------------------------------
-- BootImage Table
-- - This table is the definitive authority on all things Boot Image.
-- - This information will be used by the Provisioner adapter to configure the actual external provisioner component.
-- - There will be a reference in each node pointing back to this table about what images should be used when booting that node.
--------------------------------------------------------------
CREATE TABLE BootImage (
   Id                      VarChar(50) UNIQUE   NOT NULL,   -- Uniquely identifies this particular set of Boot Image information aka AOE
   Description             VarChar(200),                    -- Description of this boot image
   BootImageFile           VarChar(100)          NOT NULL,   -- Boot image file name
   BootImageChecksum       VarChar(32)          NOT NULL,   -- Checksum for the above-mentioned BootImage - e.g., 7d94c67a1b23f5fdd799a72411158709
   BootOptions             VarChar(80),                     -- Boot options / Boot command that should be used with this image
   BootStrapImageFile      VarChar(100)          NOT NULL,   -- BootStrap image file name
   BootStrapImageChecksum  VarChar(32)          NOT NULL,   -- Checksum for the above-mentioned BootStrapImage - e.g., 7d94c67a1b23f5fdd799a72411158709
   State                   VarChar(1),                      -- State of this image information
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP            NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType      VarChar(20)          NOT NULL,   -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt               NOT NULL,   -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   KernelArgs              VarChar(800),                    -- Kernel arguments to use with this boot image
   Files                   varchar(300),                    -- File arguments to use with thia boot image
   PRIMARY KEY (Id)
);
PARTITION TABLE BootImage ON COLUMN Id;

--------------------------------------------------------------
-- BootImage_History Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the BootImage table
--    b)  automatically by changes occurring in the BootImage table (i.e., db trigger or export functionality)
-- Subscription Routing Key: BootImage.State
--------------------------------------------------------------
CREATE TABLE BootImage_History (
   Id                      VarChar(50)          NOT NULL,   -- Uniquely identifies this particular set of Boot Image information aka AOE
   Description             VarChar(200),                    -- Description of this boot image
   BootImageFile           VarChar(100)          NOT NULL,   -- Boot image file name
   BootImageChecksum       VarChar(32)          NOT NULL,   -- Checksum for the above-mentioned BootImage - e.g., 7d94c67a1b23f5fdd799a72411158709
   BootOptions             VarChar(80),                     -- Boot options / Boot command that should be used with this image
   BootStrapImageFile      VarChar(100)          NOT NULL,   -- BootStrap image file name
   BootStrapImageChecksum  VarChar(32)          NOT NULL,   -- Checksum for the above-mentioned BootStrapImage - e.g., 7d94c67a1b23f5fdd799a72411158709
   State                   VarChar(1),                      -- State of this image information
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP            NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType      VarChar(20)          NOT NULL,   -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt               NOT NULL,   -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   KernelArgs              VarChar(800),                    -- Kernel arguments to use with this boot image
   Files                   varchar(300),                    -- File arguments to use with thia boot image
);
PARTITION TABLE BootImage_History ON COLUMN Id;
CREATE INDEX BootImageHistoryByDbUpdatedTimestamp ON BootImage_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_BootImage_History (
   Id                      VarChar(50)          NOT NULL,   -- Uniquely identifies this particular set of Boot Image information aka AOE
   Description             VarChar(200),                    -- Description of this boot image
   BootImageFile           VarChar(100)          NOT NULL,   -- Boot image file name
   BootImageChecksum       VarChar(32)          NOT NULL,   -- Checksum for the above-mentioned BootImage - e.g., 7d94c67a1b23f5fdd799a72411158709
   BootOptions             VarChar(80),                     -- Boot options / Boot command that should be used with this image
   BootStrapImageFile      VarChar(100)          NOT NULL,   -- BootStrap image file name
   BootStrapImageChecksum  VarChar(32)          NOT NULL,   -- Checksum for the above-mentioned BootStrapImage - e.g., 7d94c67a1b23f5fdd799a72411158709
   State                   VarChar(1),                      -- State of this image information
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP            NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType      VarChar(20)          NOT NULL,   -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt               NOT NULL,   -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   KernelArgs              VarChar(800),                    -- Kernel arguments to use with this boot image
   Files                   varchar(300),                    -- File arguments to use with thia boot image
   Tier2DbUpdatedTimestamp TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt               NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_BootImage_History ON COLUMN Id;
CREATE ASSUMEUNIQUE INDEX Tier2_BootImage_History_EntryNum ON Tier2_BootImage_History(EntryNumber);


--------------------------------------------------------------
-- Diagnostics Table
-- - Info in this table is filled in either by
--    a)  Diagnostics Framework
--    b)  Service Operation post-processing
--------------------------------------------------------------
CREATE TABLE Diag (
   DiagId               BigInt          NOT NULL,   -- The id that uniquely identifies this specific instance of a diagnostic run (think a monotonically incrementing value)
   Lctn                 VarChar(20000)  NOT NULL,   -- Hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, a pdu, a switch, etc.
   ServiceOperationId   BigInt,                     -- The Service Operation ID that requested this diagnostic be run (NULL indicates that this diagnostic was submitted outside of a Service Operation)
   Diag                 VarChar(500)    NOT NULL,   -- Identifies which diagnostic(s) was run, this refers to the  DiagListId in Diag_List table.
   DiagParameters       VarChar(20000),             -- The Parameters passed to run this diagnostic run
   State                VarChar(1)      NOT NULL,   -- Actual state that this diagnostic is in
                                                    -- - 'W' - Working - diagnostic is being worked on
                                                    -- - 'P' - Passed - all processing for this diagnostic has been finished and passed. The Results field contains the results from this work.
                                                    -- - 'F' - Failed - all processing for this diagnostic has been finished but it failed. The Results field contains the results from this work.
                                                    -- - 'E' - ERROR in running or completing the diagnostics (could be due to provisioning issues or timeout due to test hang etc.,). The Results field may contain results if it was partially run.
   StartTimestamp       TIMESTAMP       NOT NULL,   -- Time this particular diagnostic started
   EndTimestamp         TIMESTAMP,                  -- Time this particular diagnostic ended (would be NULL if the diagnostic has not yet ended)
   Results              VarChar(262144),            -- Result string produced by the diagnostic
   DbUpdatedTimestamp   TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP       NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType   VarChar(20)     NOT NULL,   -- Type of adapter that made the last change to this item - may be needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt          NOT NULL,   -- Work item id that "caused" the last change to this item  - may be needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   PRIMARY KEY (DiagId)
);
PARTITION TABLE Diag ON COLUMN DiagId;
CREATE INDEX DiagByLctn on Diag(Lctn);

--------------------------------------------------------------
-- Diagnostics Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Diag table
--    b)  automatically by changes occurring in the Diag table (i.e., db trigger or export functionality)
--------------------------------------------------------------
CREATE TABLE Diag_History (
   DiagId               BigInt          NOT NULL,   -- The id that uniquely identifies this specific instance of a diagnostic run (think a monotonically incrementing value)
   Lctn                 VarChar(20000)  NOT NULL,   -- Hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, a pdu, a switch, etc.
   ServiceOperationId   BigInt,                     -- The Service Operation ID that requested this diagnostic be run (NULL indicates that this diagnostic was submitted outside of a Service Operation)
   Diag                 VarChar(500)    NOT NULL,   -- Identifies which diagnostic(s) was run, this refers to the  DiagListId in Diag_List table.
   DiagParameters       VarChar(20000),             -- The Parameters passed to run this diagnostic run
   State                VarChar(1)      NOT NULL,   -- Actual state that this diagnostic is in
                                                    -- - 'W' - Working - diagnostic is being worked on
                                                    -- - 'P' - Passed - all processing for this diagnostic has been finished and passed. The Results field contains the results from this work.
                                                    -- - 'F' - Failed - all processing for this diagnostic has been finished but it failed. The Results field contains the results from this work.
                                                    -- - 'E' - ERROR in running or completing the diagnostics (could be due to provisioning issues or timeout due to test hang etc.,). The Results field may contain results if it was partially run.
   StartTimestamp       TIMESTAMP       NOT NULL,   -- Time this particular diagnostic started
   EndTimestamp         TIMESTAMP,                  -- Time this particular diagnostic ended (would be NULL if the diagnostic has not yet ended)
   Results              VarChar(262144),            -- Result string produced by the diagnostic
   DbUpdatedTimestamp   TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP       NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType   VarChar(20)     NOT NULL,   -- Type of adapter that made the last change to this item - may be needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt          NOT NULL,   -- Work item id that "caused" the last change to this item  - may be needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
);
PARTITION TABLE Diag_History ON COLUMN DiagId;
CREATE INDEX DiagHistoryByDbUpdatedTimestamp ON Diag_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Diag_History (
   DiagId               BigInt          NOT NULL,   -- The id that uniquely identifies this specific instance of a diagnostic run (think a monotonically incrementing value)
   Lctn                 VarChar(20000)  NOT NULL,   -- Hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, a pdu, a switch, etc.
   ServiceOperationId   BigInt,                     -- The Service Operation ID that requested this diagnostic be run (NULL indicates that this diagnostic was submitted outside of a Service Operation)
   Diag                 VarChar(500)    NOT NULL,   -- Identifies which diagnostic(s) was run, this refers to the  DiagListId in Diag_List table.
   DiagParameters       VarChar(20000),             -- The Parameters passed to run this diagnostic run
   State                VarChar(1)      NOT NULL,   -- Actual state that this diagnostic is in
                                                    -- - 'W' - Working - diagnostic is being worked on
                                                    -- - 'P' - Passed - all processing for this diagnostic has been finished and passed. The Results field contains the results from this work.
                                                    -- - 'F' - Failed - all processing for this diagnostic has been finished but it failed. The Results field contains the results from this work.
                                                    -- - 'E' - ERROR in running or completing the diagnostics (could be due to provisioning issues or timeout due to test hang etc.,). The Results field may contain results if it was partially run.
   StartTimestamp       TIMESTAMP       NOT NULL,   -- Time this particular diagnostic started
   EndTimestamp         TIMESTAMP,                  -- Time this particular diagnostic ended (would be NULL if the diagnostic has not yet ended)
   Results              VarChar(262144),            -- Result string produced by the diagnostic
   DbUpdatedTimestamp   TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP       NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   LastChgAdapterType   VarChar(20)     NOT NULL,   -- Type of adapter that made the last change to this item - may be needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt          NOT NULL,   -- Work item id that "caused" the last change to this item  - may be needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   EntryNumber          BigInt          NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Diag_History ON COLUMN DiagId;
--CREATE ASSUMEUNIQUE INDEX Tier2_Diag_History_EntryNum ON Tier2_Diag_History(EntryNumber);


--------------------------------------------------------------
-- Diagnostic Results per unit HW Table
-- - Info in this table is filled in either by
--    a)  Diagnostics Framework
--    b)  Service Operation post-processing
--------------------------------------------------------------
CREATE TABLE DiagResults (
   DiagId               BigInt         NOT NULL,   -- The id that links the results to the specific instance of a diagnostic run (think a monotonically incrementing value)
   Lctn                 VarChar(50)    NOT NULL,   -- Hardware location string of the smallest unit of hardware that this diagnostic result refers to - a compute node, a service node, a cdu, a pdu, a switch, etc.
   State                VarChar(1)     NOT NULL,   -- Actual state the diagnostic result for this hardware unit is in
                                                   -- - 'W' - Working - diagnostic is being worked on
                                                   -- - 'P' - Passed - all processing for this diagnostic has been finished and passed. The Results field contains the results from this work.
                                                   -- - 'F' - Failed - all processing for this diagnostic has been finished but it failed. The Results field contains the results from this work.
                                                   -- - 'E' - ERROR in running or completing the diagnostics (could be due to provisioning issues or timeout due to test hang etc.,). The Results field may contain results if it was partially run.
                                                   -- - 'U' - UNKNOWN Diagnostics completed and end detected but pass/fail could not be determined. The Results field may contain results if it was partially run.
   Results              VarChar(262144),           -- Result string produced by the diagnostic
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   PRIMARY KEY (DiagId,Lctn)
);
PARTITION TABLE DiagResults ON COLUMN DiagId;
CREATE INDEX DiagResultsByRun on DiagResults(DiagId);
CREATE INDEX DiagResultsByDbUpdatedTimestamp ON DiagResults(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Tier2 Diagnostic Results per unit HW Table
-- - Info in this table is filled in either by
--    a)  Diagnostics Framework
--    b)  Service Operation post-processing
--------------------------------------------------------------
CREATE TABLE Tier2_DiagResults (
   DiagId               BigInt         NOT NULL,   -- The id that links the results to the specific instance of a diagnostic run (think a monotonically incrementing value)
   Lctn                 VarChar(50)    NOT NULL,   -- Hardware location string of the smallest unit of hardware that this diagnostic result refers to - a compute node, a service node, a cdu, a pdu, a switch, etc.
   State                VarChar(1)     NOT NULL,   -- Actual state the diagnostic result for this hardware unit is in
                                                   -- - 'W' - Working - diagnostic is being worked on
                                                   -- - 'P' - Passed - all processing for this diagnostic has been finished and passed. The Results field contains the results from this work.
                                                   -- - 'F' - Failed - all processing for this diagnostic has been finished but it failed. The Results field contains the results from this work.
                                                   -- - 'E' - ERROR in running or completing the diagnostics (could be due to provisioning issues or timeout due to test hang etc.,). The Results field may contain results if it was partially run.
                                                   -- - 'U' - UNKNOWN Diagnostics completed and end detected but pass/fail could not be determined. The Results field may contain results if it was partially run.
   Results              VarChar(262144),             -- Result string produced by the diagnostic
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   PRIMARY KEY (DiagId,Lctn)
);
PARTITION TABLE Tier2_DiagResults ON COLUMN DiagId;
CREATE INDEX Tier2_DiagResultsByRun on Tier2_DiagResults(DiagId);

--------------------------------------------------------------
-- Diagnostics List Table
-- - Info in this table is filled in by either
--    a)  Admin in parallel when diagnostic tools table is updated
--    b)  Admin when new diagnostic test list needs to be created for existing tool in diagnostic tools table (Diag_Tools table)
--------------------------------------------------------------
CREATE TABLE Diag_List (
   DiagListId           VarChar(40)    NOT NULL,   -- Unique ID/short command to identify this diagnostic test. The CLI and other
                                                   -- components will invoke this diagnostic test by this short command.
   DiagToolId           VarChar(40)    NOT NULL,   -- Unique diagnostic tool in the Diag_Tools table that this command will run
   Description          VarChar(240)   NOT NULL,   -- Detail description of focus of this diagnostic test
                                                   -- (e.g., runs HPC Offline Diagnostic memory test)
   DefaultParameters    VarChar(240),              -- Default Parameters to use when running this test. This may be NULL if the tool does
                                                   -- not take any parameters, or it is intended to run the tool without any parameters.
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (DiagListId)
);
PARTITION TABLE Diag_List ON COLUMN DiagListId;
CREATE INDEX Diag_ListByDbUpdatedTimestamp ON Diag_List(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Diag_List (
   DiagListId           VarChar(40)    NOT NULL,   -- Unique ID/short command to identify this diagnostic test. The CLI and other
                                                   -- components will invoke this diagnostic test by this short command.
   DiagToolId           VarChar(40)    NOT NULL,   -- Unique diagnostic tool in the Diag_Tools table that this command will run
   Description          VarChar(240)   NOT NULL,   -- Detail description of focus of this diagnostic test
                                                   -- (e.g., runs HPC Offline Diagnostic memory test)
   DefaultParameters    VarChar(240),              -- Default Parameters to use when running this test. This may be NULL if the tool does
                                                   -- not take any parameters, or it is intended to run the tool without any parameters.
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (DiagListId)
);
PARTITION TABLE Tier2_Diag_List ON COLUMN DiagListId;


--------------------------------------------------------------
-- Diagnostics Tools Table
-- - Info in this table is filled in by UCS developer when
--   new diagnostic tools are added to UCS
--------------------------------------------------------------
CREATE TABLE Diag_Tools (
   DiagToolId           VarChar(40)    NOT NULL,   -- Unique Diagnostic tool ID to identify this diagnostic tool
   Description          VarChar(240)   NOT NULL,   -- Detail description of this diagnostic tool (e.g. HPC Offline diagnostics identifies
                                                   -- any failing component in system by doing inventory check on processor,memory,disk,
                                                   -- PCIE devices in system and resources assigned. Runs and verifies  performance of Processor,
                                                   --  memory, disk )
   UnitType             VarChar(25)    NOT NULL,   -- Gives the unit the diagnostic is targeted for  (ex) node/Rack/Switch/blade
   UnitSize             Integer        NOT NULL,   -- The minimum number of UnitType required to run this diagnostic tool on the cluster
   ProvisionReqd        VarChar(1)     NOT NULL,   -- Indicates if provisioning is required before running this diagnostic tool
                                                   -- 'T' -  provisioning is required
                                                   -- 'F' -  provisioning is not required
   RebootBeforeReqd     VarChar(1)     NOT NULL,   -- Indicates if Reboot of unit is required before running this diagnostic tool
                                                   -- 'T' -  Reboot before running tool is required
                                                   -- 'F' -  Reboot before running tool is not required
   RebootAfterReqd      VarChar(1)     NOT NULL,   -- Indicates if Reboot of unit is required after running this diagnostic tool
                                                   -- 'T' -  Reboot after running tool is required
                                                   -- 'F' -  Reboot after running tool is not required
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (DiagToolId)
);
PARTITION TABLE Diag_Tools ON COLUMN DiagToolId;
CREATE INDEX Diag_ToolsByDbUpdatedTimestamp ON Diag_Tools(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Diag_Tools (
   DiagToolId           VarChar(40)    NOT NULL,   -- Unique Diagnostic tool ID to identify this diagnostic tool
   Description          VarChar(240)   NOT NULL,   -- Detail description of this diagnostic tool (e.g. HPC Offline diagnostics identifies
                                                   -- any failing component in system by doing inventory check on processor,memory,disk,
                                                   -- PCIE devices in system and resources assigned. Runs and verifies  performance of Processor,
                                                   --  memory, disk )
   UnitType             VarChar(25)    NOT NULL,   -- Gives the unit the diagnostic is targeted for  (ex) node/Rack/Switch/blade
   UnitSize             Integer        NOT NULL,   -- The minimum number of UnitType required to run this diagnostic tool on the cluster
   ProvisionReqd        VarChar(1)     NOT NULL,   -- Indicates if provisioning is required before running this diagnostic tool
                                                   -- 'T' -  provisioning is required
                                                   -- 'F' -  provisioning is not required
   RebootBeforeReqd     VarChar(1)     NOT NULL,   -- Indicates if Reboot of unit is required before running this diagnostic tool
                                                   -- 'T' -  Reboot before running tool is required
                                                   -- 'F' -  Reboot before running tool is not required
   RebootAfterReqd      VarChar(1)     NOT NULL,   -- Indicates if Reboot of unit is required after running this diagnostic tool
                                                   -- 'T' -  Reboot after running tool is required
                                                   -- 'F' -  Reboot after running tool is not required
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (DiagToolId)
);
PARTITION TABLE Tier2_Diag_Tools ON COLUMN DiagToolId;


--------------------------------------------------------------
-- Switch Table
-- - Info in this table is filled in either by
--    a)  System discovery process
--    b)  Fabric Manager
--    c)  Service Operation post-processing
--------------------------------------------------------------
CREATE TABLE Switch (
   Lctn                 VarChar(25) UNIQUE   NOT NULL,   -- R0-SW0, SN0-OSW1, etc.
   State                VarChar(1)           NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum               VarChar(50),                     -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                 VarChar(20),                     -- Type of item this is (i.e., Product Name)
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   Owner                VarChar(1)           NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   PRIMARY KEY (Lctn)
);
PARTITION TABLE Switch ON COLUMN Lctn;

--------------------------------------------------------------
-- Switch History Table
-- - Info in this table is filled in either by
--    a)  in parallel with changes going in to the Switch table
--    b)  automatically by changes occurring in the Switch table (i.e., db trigger or export functionality)
-- Subscription Routing Key: Switch.State (or possibly Switch.Lctn.State)
--------------------------------------------------------------
CREATE TABLE Switch_History (
   Lctn                 VarChar(25)          NOT NULL,   -- R0-SW0, SN0-OSW1, etc.
   State                VarChar(1)           NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum               VarChar(50),                     -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Type                 VarChar(20),                     -- Type of item this is (i.e., Product Name)
   DbUpdatedTimestamp   TIMESTAMP            NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP            NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   Owner                VarChar(1)           NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
);
PARTITION TABLE Switch_History ON COLUMN Lctn;
CREATE INDEX SwitchHistoryByDbUpdatedTimestamp ON Switch_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Switch_History (
   Lctn                       VarChar(25)     NOT NULL,   -- R0-SW0, SN0-OSW1, etc.
   State                      VarChar(1)      NOT NULL,   -- Actual state that this item is in - Active, Missing, Error
   Sernum                     VarChar(50),                -- Identifies the specific hw currently in this location
   Type                       VarChar(20),                -- Type of item this is
   DbUpdatedTimestamp         TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp           TIMESTAMP       NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
   Owner                      VarChar(1)      NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "W" - owned by WLM, "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Tier2DbUpdatedTimestamp    TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber                BigInt          NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_Switch_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_Switch_History_EntryNum ON Tier2_Switch_History(EntryNumber);


--------------------------------------------------------------
-- Non-Node hardware Table
-- (note: compute nodes, service nodes, and switches are in their own tables)
--------------------------------------------------------------
CREATE TABLE NonNodeHw (
   Lctn                 VarChar(50)       NOT NULL,
   SequenceNumber       Integer           NOT NULL,      -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   Type                 VarChar(30)       NOT NULL,      -- Type of hardware this is e.g., "CDU", "PDU", "ChilledDoor", "CoolingTower"
   State                VarChar(1)        NOT NULL,      -- Actual state that this item is in:
                                                         --    - A = Active    (available, booted)
                                                         --    - M = Missing   (powered off)
                                                         --    - E = Error
                                                         --    - U = Unknown (do not yet know what state this is in)
   HostName             VarChar(63),                     -- Hostname
   IpAddr               VarChar(25),                     -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   MacAddr              VarChar(17),                     -- Mac address
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,      -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,      -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,      -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,      -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                VarChar(1)        NOT NULL,      -- Indicates which subsystem "owns" this entity, e.g., "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator           VarChar(63)       NOT NULL,      -- Location string of the service node that "controls/owns" this hardware.
   InventoryTimestamp   TIMESTAMP,                       -- Time the event occurred that resulted in this inventory being changed.
   PRIMARY KEY (Lctn)
);
PARTITION TABLE NonNodeHw ON COLUMN Lctn;
CREATE INDEX NonNodeHwState      on NonNodeHw(State);
CREATE INDEX NonNodeHwIpAddr     on NonNodeHw(IpAddr)  WHERE IpAddr IS NOT NULL;
CREATE INDEX NonNodeHwAggregator on NonNodeHw(Aggregator);

--------------------------------------------------------------
-- Non-Node hardware history Table
--------------------------------------------------------------
CREATE TABLE NonNodeHw_History (
   Lctn                 VarChar(50)       NOT NULL,
   SequenceNumber       Integer           NOT NULL,      -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   Type                 VarChar(30)       NOT NULL,      -- Type of hardware this is e.g., "CDU", "PDU", "ChilledDoor", "CoolingTower"
   State                VarChar(1)        NOT NULL,      -- Actual state that this item is in:
                                                         --    - A = Active    (available, booted)
                                                         --    - M = Missing   (powered off)
                                                         --    - E = Error
                                                         --    - U = Unknown (do not yet know what state this is in)
   HostName             VarChar(63),                     -- Hostname
   IpAddr               VarChar(25),                     -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   MacAddr              VarChar(17),                     -- Mac address
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,      -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp     TIMESTAMP         NOT NULL,      -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType   VarChar(20)       NOT NULL,      -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId    BigInt            NOT NULL,      -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                VarChar(1)        NOT NULL,      -- Indicates which subsystem "owns" this entity, e.g., "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator           VarChar(63)       NOT NULL,      -- Location string of the service node that "controls/owns" this hardware.
   InventoryTimestamp   TIMESTAMP,                       -- Time the event occurred that resulted in this inventory being changed.
);
PARTITION TABLE NonNodeHw_History ON COLUMN Lctn;
CREATE UNIQUE INDEX NonNodeHwHistoryByLctnAndLastChgTimestamp on NonNodeHw_History(Lctn, LastChgTimestamp);
CREATE INDEX NonNodeHwHistoryByDbUpdatedTimestamp ON NonNodeHw_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_NonNodeHw_History (
   Lctn                    VarChar(50)       NOT NULL,
   SequenceNumber          Integer           NOT NULL,   -- Unique sequence number that can be used to correlate Lctn to index - assigned during PopulateSchema
   Type                    VarChar(30)       NOT NULL,   -- Type of hardware this is e.g., "CDU", "PDU", "ChilledDoor", "CoolingTower"
   State                   VarChar(1)        NOT NULL,   -- Actual state that this item is in:
                                                         --    - A = Active    (available, booted)
                                                         --    - M = Missing   (powered off)
                                                         --    - E = Error
                                                         --    - U = Unknown (do not yet know what state this is in)
   HostName                VarChar(63),                  -- Hostname
   IpAddr                  VarChar(25),                  -- E.g., (IPV4) 192.168.122.115  (IPV6) fe80::428d:5cff:fe51:d45a
   MacAddr                 VarChar(17),                  -- Mac address
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   LastChgTimestamp        TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this entry being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   LastChgAdapterType      VarChar(20)       NOT NULL,   -- Type of adapter that made the last change to this item - needed for work item recovery flows for failed adapters (e.g., WLM, PROVISIONER, RAS, ONLINE_TIER, NEARLINE_TIER, MONITOR, RM_RTE, FM, UI, CONTROL, etc.)
   LastChgWorkItemId       BigInt            NOT NULL,   -- Work item id that "caused" the last change to this item  - needed for work item recovery flows for failed adapters (-1 used when there is no work item yet associated with this change)
   Owner                   VarChar(1)        NOT NULL,   -- Indicates which subsystem "owns" this entity, e.g., "S" - owned by Service, "G" - owned by General System, "F" - unowned / in the free pool
   Aggregator              VarChar(63)       NOT NULL,   -- Location string of the service node that "controls/owns" this hardware.
   InventoryTimestamp      TIMESTAMP,                    -- Time the event occurred that resulted in this inventory being changed.
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
PARTITION TABLE Tier2_NonNodeHw_History ON COLUMN Lctn;
CREATE ASSUMEUNIQUE INDEX Tier2_NonNodeHw_History_EntryNum ON Tier2_NonNodeHw_History(EntryNumber);


--------------------------------------------------------------
-- Non-Node Inventory History Table
--------------------------------------------------------------
CREATE TABLE NonNodeHwInventory_History (
   Lctn                 VarChar(50)       NOT NULL,
   DbUpdatedTimestamp   TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   InventoryTimestamp   TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this inventory being changed.
                                                      -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                      --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   InventoryInfo        VarChar(16384),               -- Additional inventory details not part of the standard manifest, e.g. part numbers, CPU details (CPU ID, speed, sockets, hyper threads), memory module details (type, size, speed)
   Sernum               VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   PRIMARY KEY (Lctn, InventoryTimestamp)
);
PARTITION TABLE NonNodeHwInventory_History ON COLUMN Lctn;
CREATE INDEX NonNodeHwInventoryHistoryByDbUpdatedTimestamp ON NonNodeHwInventory_History(DbUpdatedTimestamp);

--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_NonNodeHwInventory_History (
   Lctn                    VarChar(50)       NOT NULL,
   DbUpdatedTimestamp      TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.  This is different than LastChgTimestamp.
   InventoryTimestamp      TIMESTAMP         NOT NULL,   -- Time the event occurred that resulted in this inventory being changed.
                                                         -- Note: this timestamp is not necessarily the time the change occurred in the database.  Rather it is the time (maybe from a log) that the change actually occurred.
                                                         --       See the DbUpdatedTimestamp field, if you want the time the change was recorded in the database.
   InventoryInfo           VarChar(16384),               -- Additional inventory details not part of the standard manifest, e.g. part numbers, CPU details (CPU ID, speed, sockets, hyper threads), memory module details (type, size, speed)
   Sernum                  VarChar(50),                  -- Identifies the specific hw currently in this location (i.e., Product Serial)
   Tier2DbUpdatedTimestamp TIMESTAMP         NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt            NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
   PRIMARY KEY (Lctn, InventoryTimestamp)
);
PARTITION TABLE Tier2_NonNodeHwInventory_History ON COLUMN Lctn;


--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
-- Need to define the FabricTopology data
-- - Included below is just a place holder the fields defined are exemplary NOT concrete!
-- - This info will need to be defined in concert with the Fabric Manager team!
--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
--------------------------------------------------------------
-- FabricTopology Table
-- - Info in this table is filled in either by
--    a)  System discovery process
--    b)  Fabric Manager
--------------------------------------------------------------
CREATE TABLE FabricTopology (
-- Cables
-- Links
-- State/Status
-- Congestion Indicators
-- Performance Indicators
-- Type of Fabric
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
-- PARTITION TABLE FabricTopology ON COLUMN Xxxxxx;

--------------------------------------------------------------
-- FabricTopology_History Table
-- Subscription Routing Key: FabricTopology.State
--------------------------------------------------------------
CREATE TABLE FabricTopology_History (
-- Cables
-- Links
-- State/Status
-- Congestion Indicators
-- Performance Indicators
-- Type of Fabric
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
-- PARTITION TABLE FabricTopology_History ON COLUMN Xxxxxx;
CREATE INDEX FabricTopologyHistoryByDbUpdatedTimestamp ON FabricTopology_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_FabricTopology_History (
-- Cables
-- Links
-- State/Status
-- Congestion Indicators
-- Performance Indicators
-- Type of Fabric
   DbUpdatedTimestamp      TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   Tier2DbUpdatedTimestamp TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt          NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
-- PARTITION TABLE Tier2_FabricTopology_History ON COLUMN Xxxxxx;
--CREATE ASSUMEUNIQUE INDEX Tier2_FabricTopology_History_EntryNum ON Tier2_FabricTopology_History(EntryNumber);


--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
-- Need to define the Lustre data
-- - Included below is just a place holder the fields defined are exemplary NOT concrete!
-- - This info will need to be defined in concert with the Lustre team!
--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
--------------------------------------------------------------
-- Lustre Table
-- - Info in this table is filled in either by
--    a)  Lustre filesystem
--------------------------------------------------------------
CREATE TABLE Lustre (
-- File system topology
-- State/Status
-- Congestion Indicators
-- Performance Indicators
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
-- PARTITION TABLE Lustre ON COLUMN Xxxxxx;

--------------------------------------------------------------
-- Lustre_History Table
-- Subscription Routing Key: Lustre.State
--------------------------------------------------------------
CREATE TABLE Lustre_History (
-- File system topology
-- State/Status
-- Congestion Indicators
-- Performance Indicators
   DbUpdatedTimestamp      TIMESTAMP            NOT NULL   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
);
-- PARTITION TABLE Lustre_History ON COLUMN Xxxxxx;
CREATE INDEX LustreHistoryByDbUpdatedTimestamp ON Lustre_History(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Lustre_History (
-- File system topology
-- State/Status
-- Congestion Indicators
-- Performance Indicators
   DbUpdatedTimestamp      TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   Tier2DbUpdatedTimestamp TIMESTAMP       NOT NULL,   -- Time the last change to this record was recorded in the Tier2 database.  It is the actual time that the db update occurred.
   EntryNumber             BigInt          NOT NULL,   -- Unique entry number which is assigned when the data is inserted into this Tier2 table.  This value is used when paging/windowing through this table.
);
-- PARTITION TABLE Tier2_Lustre_History ON COLUMN Xxxxxx;
--CREATE ASSUMEUNIQUE INDEX Tier2_Lustre_History_EntryNum ON Tier2_Lustre_History(EntryNumber);


--------------------------------------------------------------
-- Unique Values Table
-- - Info in this table is filled in by
--    a)  Stored procedures calling GetUniqueId stored procedure
--------------------------------------------------------------
CREATE TABLE UniqueValues (
   Entity             VarChar(100)     NOT NULL,   -- "Entity" that the unique value is for.
   NextValue          BigInt DEFAULT 1 NOT NULL,   -- Next unique value for this entity.
   DbUpdatedTimestamp TIMESTAMP        NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (Entity)
);
PARTITION TABLE UniqueValues ON COLUMN Entity;
CREATE INDEX UniqueValuesByDbUpdatedTimestamp ON UniqueValues(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_UniqueValues (
   Entity             VarChar(100)     NOT NULL,   -- "Entity" that the unique value is for.
   NextValue          BigInt DEFAULT 1 NOT NULL,   -- Next unique value for this entity.
   DbUpdatedTimestamp TIMESTAMP        NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (Entity)
);
PARTITION TABLE Tier2_UniqueValues ON COLUMN Entity;


--------------------------------------------------------------
-- Hardware constraints table
--------------------------------------------------------------
CREATE TABLE Constraint (
   ConstraintId         VarChar(50)    NOT NULL,   -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   Constraints          VarChar(1000),             -- Constraints that need to be enforced for entities with this constraint id.
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (ConstraintId)
);
PARTITION TABLE Constraint ON COLUMN ConstraintId;
CREATE INDEX ConstraintByDbUpdatedTimestamp ON UniqueValues(DbUpdatedTimestamp);
--------------------------------------------------------------
-- Temporary table being used in the prototype (when do not actually have a Tier2)
--------------------------------------------------------------
CREATE TABLE Tier2_Constraint (
   ConstraintId         VarChar(50)    NOT NULL,   -- Constraint ID - identifies which set of constraints apply to this node (see Constraint table).
   Constraints          VarChar(1000),             -- Constraints that need to be enforced for entities with this constraint id.
   DbUpdatedTimestamp   TIMESTAMP      NOT NULL,   -- Time the last change to this record was recorded in the database.  It is the actual time that the db update occurred.
   PRIMARY KEY (ConstraintId)
);
PARTITION TABLE Tier2_Constraint ON COLUMN ConstraintId;


--------------------------------------------------------------
-- Logical groups Table
--------------------------------------------------------------
CREATE TABLE LogicalGroups (
   GroupCreatedTimestamp   TIMESTAMP,
   GroupName               varchar(100)       NOT NULL,
   DeviceList              varchar(4192)      NOT NULL,
   AccessRights            varchar(32),
   GroupModifiedTimestamp  TIMESTAMP,
   PRIMARY KEY (GroupName)
);
PARTITION TABLE LogicalGroups ON COLUMN GroupName;


--------------------------------------------------------------
-- Temporary table being used when we do not actually have a Tier2
--------------------------------------------------------------
CREATE TABLE Tier2_Config (
   Key         Varchar(50) NOT NULL,
   Value       Varchar(50) NOT NULL,
   Description Varchar(500)
);


--------------------------------------------------------------
-- Foreign F/W Inventory
--------------------------------------------------------------
-- Records all F/W versions in the HPC.
CREATE TABLE FW_Version (
    ID VARCHAR(64) NOT NULL PRIMARY KEY, -- Location ID translated from JSON
    TargetID VARCHAR(64) NOT NULL,       -- BIOS, BMC, ME, SDR, etc.
    Version VARCHAR(64) NOT NULL,
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

CREATE TABLE Tier2_FW_Version (
    ID VARCHAR(64) NOT NULL PRIMARY KEY, -- Location ID translated from JSON
    TargetID VARCHAR(64) NOT NULL,       -- BIOS, BMC, ME, SDR, etc.
    Version VARCHAR(64) NOT NULL,
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    EntryNumber BigInt NOT NULL
);

-- History of F/W updates on the HPC.  Note that the timestamp marks
-- the DB update event.  The foreign data does not have the time of actual F/W update.
CREATE TABLE FW_Version_History (
    ID VARCHAR(64) NOT NULL,             -- Location ID translated from JSON
    TargetID VARCHAR(64) NOT NULL,       -- BIOS, BMC, ME, SDR, etc.
    Version VARCHAR(64) NOT NULL,
    DbUpdatedTimestamp TIMESTAMP NOT NULL
);

CREATE TABLE tier2_FW_Version_History (
    ID VARCHAR(64) NOT NULL,             -- Location ID translated from JSON
    TargetID VARCHAR(64) NOT NULL,       -- BIOS, BMC, ME, SDR, etc.
    Version VARCHAR(64) NOT NULL,
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    EntryNumber BigInt NOT NULL
);

--------------------------------------------------------------
-- >>> Foreign HW Inventory
--------------------------------------------------------------
-- Records all FRUs that are and were in the HPC.
CREATE TABLE HW_Inventory_FRU (
    FRUID VARCHAR(80) NOT NULL PRIMARY KEY,     -- perhaps <manufacturer>-<serial#>
    FRUType VARCHAR(16),                        -- Field_Replaceble_Unit category(HMS type)
    FRUSubType VARCHAR(32),                     -- perhaps specific model; NULL:unspecifed
    FRUInfo VARCHAR(8192),
);

-- Corresponds to the current HPC HW architecture wrt to HW locations.
-- Note that FRUID is not unique in foreign data.  This is because node enclosures have no ID.
CREATE TABLE HW_Inventory_Location (
    ID VARCHAR(64) NOT NULL PRIMARY KEY, -- Location ID translated from JSON
    Type VARCHAR(16) NOT NULL,           -- Location category(HMS type)
    Ordinal INTEGER NOT NULL,            -- singleton:0
    Info VARCHAR(8192),
    FRUID VARCHAR(80),                      -- perhaps <manufacturer>-<serial#>
);

-- History of FRU installation and removal from the HPC.  Note that the timestamp marks
-- the DB update event.  The foreign data does not have the time of actual HW modification.
CREATE TABLE RawHWInventory_History (
    Action VARCHAR(16) NOT NULL,            -- Added/Removed
    ID VARCHAR(64) NOT NULL,                -- perhaps xname (path); as is from JSON
    FRUID VARCHAR(80) NOT NULL,             -- perhaps <manufacturer>-<serial#>
    ForeignTimestamp VARCHAR(32) NOT NULL,  -- Foreign server timestamp string in RFC-3339 format
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (Action, ID, ForeignTimestamp)  -- allows the use of upsert to eliminate duplicates
);

CREATE TABLE tier2_RawHWInventory_History (
    Action VARCHAR(16) NOT NULL,            -- Added/Removed
    ID VARCHAR(64) NOT NULL,                -- Location ID translated from JSON
    FRUID VARCHAR(80) NOT NULL,             -- perhaps <manufacturer>-<serial#>
    ForeignTimestamp VARCHAR(32) NOT NULL,  -- Foreign server timestamp string in RFC-3339 format
    DbUpdatedTimestamp TIMESTAMP NOT NULL,
    EntryNumber BigInt NOT NULL,
    PRIMARY KEY (Action, ID, ForeignTimestamp)  -- allows the use of upsert to eliminate duplicates
);

-- <<< Foreign HW Inventory
--------------------------------------------------------------
-- Stored Procedure information
--------------------------------------------------------------
CREATE PROCEDURE
   PARTITION ON TABLE UniqueValues COLUMN Entity PARAMETER 0
   FROM CLASS com.intel.dai.procedures.GetUniqueId;


CREATE PROCEDURE MachineDescription
   AS SELECT Description FROM MACHINE;
CREATE PROCEDURE MachineNumRows
   AS SELECT NumRows FROM Machine;
CREATE PROCEDURE MachineNumColumnsInRow
   AS SELECT NumColsInRow FROM Machine;
CREATE PROCEDURE MachineNumChassisInRack
   AS SELECT NumChassisInRack FROM Machine;
CREATE PROCEDURE
   PARTITION ON TABLE Machine COLUMN Sernum PARAMETER 0
   FROM CLASS com.intel.dai.procedures.MachineUpdateSynthesizedDataFlag;
-- Get the flag indicating whether or not we are using synthesized data for UCS right now.
--    echo "exec MachineAreWeUsingSynthesizedData '1';" | sqlcmd
CREATE PROCEDURE MachineAreWeUsingSynthesizedData
   PARTITION ON TABLE Machine COLUMN Sernum PARAMETER 0
   AS SELECT UsingSynthesizedData FROM Machine WHERE Sernum=?;


-- Get the MachineAdapterInstances entries for the specified service node hostname.
CREATE PROCEDURE MachineAdapterInstancesForServiceNode
   PARTITION ON TABLE MachineAdapterInstance COLUMN SnLctn PARAMETER 0
   AS SELECT * FROM MachineAdapterInstance WHERE SnLctn=? Order By AdapterType;
-- Get the adapter invocation information out of the MachineAdapterInstance table.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.MachineAdapterInvocationInformation;
-- Get the next NumStartedInstances value from the MachineAdapterInstance table (bump the value in the db and return that value to caller).
CREATE PROCEDURE
   PARTITION ON TABLE MachineAdapterInstance COLUMN SnLctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.MachineAdapterInstanceBumpNextInstanceNumAndReturn;


CREATE PROCEDURE CountOfRacks
   AS SELECT COUNT(*) FROM Rack;
-- This stored procedure retrieves all the information stored in Rack table
CREATE PROCEDURE RackList
   AS SELECT * FROM Rack ORDER BY Lctn;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ErrorOnRack;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ErrorOnChassis;
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ErrorOnComputeNode;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ErrorOnComputeNodeViaMacAddr;


CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeDiscovered;
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSaveIpAddr;
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSaveBootImageInfo;
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSaveEnvironmentInfo;
-- Set a compute node's state to the specified value.
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSetState;
-- Set a list of compute node's state to the specified value.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ComputeNodeSetStates;
-- Handle the processing necessary when a compute node has been "replaced".
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeReplaced;
-- Set which subsystem "owns" this piece of hardware.
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSetOwner;
-- Set the WlmNodeState for this piece of hardware.
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSetWlmNodeState;
-- Set a compute node's proof of life time to the specified value.
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSetProofOfLifeTs;
-- Save new BIOS info into the DB for the specified compute node.
CREATE PROCEDURE
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ComputeNodeSaveBiosInfo;

-- Get the count of compute nodes that have the specified State.
CREATE PROCEDURE ComputeNodeCountWithThisState
   AS SELECT COUNT(*) FROM ComputeNode WHERE State = ?;
-- Get a list of compute nodes that have the specified State.
CREATE PROCEDURE ComputeNodeListWithThisState
   AS SELECT Lctn, SequenceNumber, LastChgTimestamp FROM ComputeNode WHERE (State = ?) Order By Lctn;
CREATE PROCEDURE ComputeNodeListWithThisStateByLimit
   AS SELECT Lctn, SequenceNumber, LastChgTimestamp FROM ComputeNode WHERE (State = ?) Order By Lctn LIMIT 512 Offset ?;
-- Get the number of compute nodes in this machine.
CREATE PROCEDURE ComputeNodeCount
   AS SELECT COUNT(*) FROM ComputeNode;
-----CREATE PROCEDURE ComputeNodeListWithThisType
-----   AS SELECT Lctn, SequenceNumber, State, LastChgTimestamp FROM ComputeNode WHERE Type = ? Order By Lctn;
-- Get the Compute Node State for the specified compute node location.
CREATE PROCEDURE ComputeNodeState
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT State, WlmNodeState FROM ComputeNode WHERE Lctn = ?;
-- Get the Compute Node's MacAddr for the specified node location.
CREATE PROCEDURE ComputeNodeMacAddr
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT MacAddr FROM ComputeNode WHERE Lctn = ?;
CREATE PROCEDURE ComputeNodeListLctnAndSeqNum
   AS SELECT Lctn, SequenceNumber FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ComputeNodeInfo
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT * FROM ComputeNode WHERE Lctn=?;
CREATE PROCEDURE ComputeNodeBasicInformation
   AS SELECT Lctn, HostName, SequenceNumber, Owner FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ServiceNodeBasicInformation
   AS SELECT Lctn, HostName, SequenceNumber, Owner FROM ServiceNode Order By Lctn;
CREATE PROCEDURE ComputeNodeListLctnAndState
   AS SELECT Lctn, State FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ComputeNodeListLctnAndHostname
   AS SELECT Lctn, HostName FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ComputeNodeListLctnHostnameAndBmcHostname
   AS SELECT Lctn, HostName, BmcHostName, Aggregator FROM ComputeNode Order By Lctn;
-- Get the BootImageId of compute node
CREATE PROCEDURE ComputeNodeGetBootImageId
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT BootImageId FROM ComputeNode WHERE Lctn = ?;
CREATE PROCEDURE ComputeNodeListLctnIpAddrAndBmcIpAddr
   AS SELECT Lctn, IpAddr, BmcIpaddr FROM ComputeNode Order By Lctn;
-- Get the list of ComputeNodes that this specified service node is the aggregator/controller for
-- (i.e., get the list of compute nodes that are "children" of the specified aggregator service node).
CREATE PROCEDURE ComputeNodeListOfChildren
   AS SELECT * FROM ComputeNode WHERE Aggregator = ? Order By Lctn;
CREATE PROCEDURE ComputeNodeOwner
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT Owner FROM ComputeNode WHERE LCTN = ?;
-- Get the state of the specified ComputeNodes.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ComputeNodeStates;
-- Get list of Aggregator nodes that have computenodes as children
CREATE PROCEDURE ComputeNodeAggregators
   AS SELECT DISTINCT Aggregator FROM ComputeNode;
-- Get the list of active Compute Nodes that have not recorded a proof of life message since the specified timestamp.
CREATE PROCEDURE ComputeNodeListOfMissingProofOfLifeMsgs
   AS SELECT Lctn, State, ProofOfLifeTimestamp FROM ComputeNode WHERE (State='A'  AND (ProofOfLifeTimestamp IS NULL  OR  ProofOfLifeTimestamp<?));
-- Get the list of currently halting Compute Nodes that have been halting since the specified timestamp (i.e. can be used to find nodes stuck shutting down).
CREATE PROCEDURE ComputeNodeListOfNodesStuckHalting
   AS SELECT Lctn, State, DbUpdatedTimestamp FROM ComputeNode WHERE (State='H'  AND  DbUpdatedTimestamp<?);

-- New location mapping queries.
CREATE PROCEDURE ComputeNodeLocationInformation
   AS SELECT Lctn, HostName, IpAddr, MacAddr, BmcHostName FROM ComputeNode;
CREATE PROCEDURE ServiceNodeLocationInformation
   AS SELECT Lctn, HostName, IpAddr, MacAddr, BmcHostName FROM ServiceNode;

-- This stored procedure returns a single VoltTable:
--    - that contains the Lctn, State, and BootImageId information for each of the nodes in the specified node list that are NOT booted with the specified BootImageId
--    (one row in the VoltTable for each node).
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ComputeNodesFromListWithoutThisBootImageId;
-- This stored procedure returns an array of VoltTables:
--    - the first VoltTable contains the Lctn, State, and BootImageId information for each of the nodes in the specified node list, that are NOT in one of the states that was specified in the state list.
--    - the subsequent VoltTables contain the same information for each of the nodes that ARE in one of the states that was specified in the state list.
--    So this means that if there were 2 states in the specified state list ("A", "E") then there would be a total of 3 entries in the array of VoltTables that will be returned.
--       1)  This volt table would have the information for each of the specified nodes that have a state that is not A and is not E.
--       2)  This volt table would have the information for each of the specified nodes that have a state of "A"
--       3)  This volt table would have the information for each of the specified nodes that have a state of "E"
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ComputeNodesFromListWithoutAndWithTheseStates;

-- This stored procedure returns all the contents in the ComputeNode table
CREATE PROCEDURE ComputeNodesList
   AS SELECT * FROM ComputeNode ORDER BY Lctn;
-- Get the inventory info for the specified compute node lctn - 0 rows returned means either a bad lctn string or lack of inventory information (null).
CREATE PROCEDURE ComputeNodeInventoryInfo
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT ComputeNode.Lctn, ComputeNode.InventoryTimestamp, NodeInventory_History.InventoryInfo FROM ComputeNode INNER JOIN NodeInventory_History ON (ComputeNode.Lctn=NodeInventory_History.Lctn AND ComputeNode.InventoryTimestamp=NodeInventory_History.InventoryTimestamp) WHERE (ComputeNode.Lctn=? AND ComputeNode.InventoryTimestamp IS NOT NULL);
-- Get the bios info for the specified compute node lctn - 0 rows returned means either a bad lctn string or lack of bios information (null).
CREATE PROCEDURE ComputeNodeBiosInfo
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT ComputeNode.Lctn, ComputeNode.InventoryTimestamp, NodeInventory_History.BiosInfo FROM ComputeNode INNER JOIN NodeInventory_History ON (ComputeNode.Lctn=NodeInventory_History.Lctn AND ComputeNode.InventoryTimestamp=NodeInventory_History.InventoryTimestamp) WHERE (ComputeNode.Lctn=? AND ComputeNode.InventoryTimestamp IS NOT NULL);



CREATE PROCEDURE CacheMacAddrToLctnGetLctn
   PARTITION ON TABLE CacheMacAddrToLctn COLUMN MacAddr PARAMETER 0
   AS SELECT Lctn FROM CacheMacAddrToLctn WHERE MacAddr=?;



CREATE PROCEDURE CacheIpAddrToLctnGetLctn
   PARTITION ON TABLE CacheIpAddrToLctn COLUMN IpAddr PARAMETER 0
   AS SELECT Lctn FROM CacheIpAddrToLctn WHERE IpAddr=?;



CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ComputeNodeHistoryListOfStateAtTime;
CREATE PROCEDURE ComputeNodeHistoryOldestTimestamp
   AS SELECT MIN(LastChgTimestamp) FROM ComputeNode_History;
-- To check for duplicate mac address entry, get all nodes with given mac address
CREATE PROCEDURE ComputeNodeCheckMacAddr
   AS SELECT COUNT(*) FROM ComputeNode WHERE Owner!='S' AND (MacAddr = ? OR BmcMacAddr = ? );

-- Update a service or compute node's MacAddr and BmcMacAddr (including updating the CacheMacAddrToLctn table as well).
--  Note: this can not be a partitioned stored procedure because the CacheMacAddrToLctn table is partitioned by MacAddr, not Lctn.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.TempUpdateNodeMacAddrs;


-- List of defined Service Nodes and their hostnames.
CREATE PROCEDURE ServiceNodeListLctnAndHostname
   AS SELECT Lctn, HostName FROM ServiceNode Order By Lctn;
CREATE PROCEDURE ServiceNodeListLctnHostnameAndBmcHostname
   As SELECT Lctn, HostName, BmcHostName, Aggregator From ServiceNode Order By Lctn;
-- Set the Service Node's state.
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeSetState;
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeDiscovered;
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeSaveIpAddr;
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ErrorOnServiceNode;
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeReplaced;
-- Get the Service Node's MacAddr for the specified node location.
CREATE PROCEDURE ServiceNodeMacAddr
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   AS SELECT MacAddr FROM ServiceNode WHERE Lctn = ?;
-- This stored procedure returns all the contents in the ServiceNode table
CREATE PROCEDURE ServiceNodesList
   AS SELECT * FROM ServiceNode ORDER BY Lctn;
-- Get the list of ServiceNodes that this specified service node is the aggregator/controller for
-- (i.e., get the list of service nodes that are "children" of the specified aggregator service node).
CREATE PROCEDURE ServiceNodeListOfChildren
   AS SELECT * FROM ServiceNode WHERE Aggregator = ? Order By Lctn;
-- Get the ServiceNode's Lctn for the service node with the specified hostname
-- (this is a temporary stored procedure being used when starting up the DaiMgr during StartUcs script).
CREATE PROCEDURE TempServiceNodeLctn
   AS SELECT Lctn FROM ServiceNode WHERE Hostname = ?;
-- Set which subsystem "owns" this piece of hardware.
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeSetOwner;
-- Set a service node's proof of life time to the specified value.
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeSetProofOfLifeTs;

CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ServiceNodeHistoryListOfStateAtTime;
CREATE PROCEDURE ServiceNodeHistoryOldestTimestamp
   AS SELECT MIN(LastChgTimestamp) FROM ServiceNode_History;
CREATE PROCEDURE ServiceNodeOwner
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   AS SELECT Owner FROM ServiceNode WHERE LCTN = ?;
-- To check for duplicate mac address entry, get all nodes with given mac address
CREATE PROCEDURE ServiceNodeCheckMacAddr
   AS SELECT COUNT(*) FROM ServiceNode WHERE Owner!='S' AND (MacAddr = ? OR BmcMacAddr = ? );
-- Get the inventory info for the specified Service node lctn - 0 rows returned means either a bad lctn string or lack of inventory info (null).
CREATE PROCEDURE ServiceNodeInventoryInfo
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   AS SELECT ServiceNode.Lctn, ServiceNode.InventoryTimestamp, NodeInventory_History.InventoryInfo FROM ServiceNode INNER JOIN NodeInventory_History ON (ServiceNode.Lctn=NodeInventory_History.Lctn AND ServiceNode.InventoryTimestamp=NodeInventory_History.InventoryTimestamp) WHERE (ServiceNode.Lctn=? AND ServiceNode.InventoryTimestamp IS NOT NULL);
-- Get the bios info for the specified service node lctn - 0 rows returned means either a bad lctn string or lack of bios information (null).
CREATE PROCEDURE ServiceNodeBiosInfo
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   AS SELECT ServiceNode.Lctn, ServiceNode.InventoryTimestamp, NodeInventory_History.BiosInfo FROM ServiceNode INNER JOIN NodeInventory_History ON (ServiceNode.Lctn=NodeInventory_History.Lctn AND ServiceNode.InventoryTimestamp=NodeInventory_History.InventoryTimestamp) WHERE (ServiceNode.Lctn=? AND ServiceNode.InventoryTimestamp IS NOT NULL);
CREATE PROCEDURE ServiceNodeInfo
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   AS SELECT * FROM ServiceNode WHERE Lctn=?;
-- Get the list of active Service Nodes that have not recorded a proof of life message since the specified timestamp.
CREATE PROCEDURE ServiceNodeListOfMissingProofOfLifeMsgs
   AS SELECT Lctn, State, ProofOfLifeTimestamp FROM ServiceNode WHERE (State='A'  AND (ProofOfLifeTimestamp IS NULL  OR  ProofOfLifeTimestamp<?));
-- Get the list of currently halting Service Nodes that have been halting since the specified timestamp (i.e. can be used to find nodes stuck shutting down).
CREATE PROCEDURE ServiceNodeListOfNodesStuckHalting
   AS SELECT Lctn, State, DbUpdatedTimestamp FROM ServiceNode WHERE (State='H'  AND  DbUpdatedTimestamp<?);
-- Save new BIOS info into the DB for the specified service node.
CREATE PROCEDURE
   PARTITION ON TABLE ServiceNode COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceNodeSaveBiosInfo;

CREATE PROCEDURE NonNodeHwListLctnHostname
   As SELECT Lctn, Hostname, '' AS BmcHostname, Aggregator From NonNodeHw Order by Lctn;


-- Store a triplet (Max, Min, Avg) of aggregated environmental telemetry that occurred for the specified location over the last interval.
CREATE PROCEDURE
   PARTITION ON TABLE Tier2_AggregatedEnvData COLUMN Lctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.AggregatedEnvDataStore;
-- Retrieve the aggregated environmental telemetry for the specified time range.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.AggregatedEnvDataListAtTime;



CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.DbChgTimestamps;



CREATE PROCEDURE
   PARTITION ON TABLE Job COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.JobStarted;
CREATE PROCEDURE
   PARTITION ON TABLE Job COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.JobTerminated;
CREATE PROCEDURE JobHistoryOldestTimestamp
   AS SELECT MIN(LastChgTimestamp) FROM Job_History;
-- Get job information for a specified JobId.
CREATE PROCEDURE
   PARTITION ON TABLE Job COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.JobGetInfo;




CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.InternalCachedJobsAddNodeEntry;
CREATE PROCEDURE InternalCachedJobsTerminated
   AS UPDATE InternalCachedJobs SET EndTimestamp=?, DbUpdatedTimestamp=? WHERE JobId=?;
CREATE PROCEDURE InternalCachedJobsCleanUp
   AS DELETE FROM InternalCachedJobs WHERE JobId=?;
-- Remove any "expired" jobs from the InternalCachedJobs table
--    The specified timestamp will be used for determining which jobs are expired.
CREATE PROCEDURE InternalCachedJobsRemoveExpiredJobs
   AS DELETE FROM InternalCachedJobs WHERE ((EndTimestamp IS NOT NULL) AND (DbUpdatedTimestamp < ?));
-- Get the JobId for the specified Node location that was active at the specified time
--    If job is still active (EndTimestamp is null) then
--       we want entries where the job's StartTimestamp <= the specified timestamp
--    If job has terminated (EndTimestamp is NOT null) then
--       we want entries where the job's StartTimestamp <= the specified timestamp <= job's EndTimestamp)
CREATE PROCEDURE InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp
   PARTITION ON TABLE InternalCachedJobs COLUMN NodeLctn PARAMETER 0
   AS SELECT JobId FROM InternalCachedJobs WHERE ((NodeLctn=?) AND (StartTimestamp<=?) AND ((EndTimestamp IS NULL) OR (? <= EndTimestamp)));
-- Get the JobId (if any) for the specified Node location that is active right now
CREATE PROCEDURE InternalCachedJobsGetCurrentlyActiveJobidForNodeLctn
   PARTITION ON TABLE InternalCachedJobs COLUMN NodeLctn PARAMETER 0
   AS SELECT JobId FROM InternalCachedJobs WHERE (NodeLctn=? AND StartTimestamp<=NOW AND EndTimestamp IS NULL);
-- Get the list of InternalCachedJobs information for jobs that may have been active at the specified time,
-- this information will then be used when there are multiple entries that need to be checked for.
-- Note: if there is only 1 entry to be checked for it is likely more efficient to use something like
--       InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp stored procedure.
-- Flow:
--    If job is still active (EndTimestamp is null) then
--       we want entries where the job's StartTimestamp <= the specified timestamp
--    If job has terminated (EndTimestamp is NOT null) then
--       we want entries where the job's StartTimestamp <= the specified timestamp <= job's EndTimestamp)
CREATE PROCEDURE InternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp
   AS SELECT * FROM InternalCachedJobs WHERE ((StartTimestamp<=?) AND ((EndTimestamp IS NULL) OR (? <= EndTimestamp))) ORDER BY NodeLctn, StartTimestamp;


-- Handle processing that is necessary in the InternalJobInfo table so the JobInfo reflects that the job has started.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobInfoJobStarted;
-- Handle processing that is necessary in the InternalJobInfo table so the JobInfo reflects that the job has completed.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobInfoJobCompleted;
-- Remove the specified job information row from this table.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobInfoJobRemove;
-- Go through the JobInfo table and mark the row for the specified job such that WlmJobStarted is set to 'T'
-- AND
-- Also return the updated JobInfo as a VoltTable.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobInfoSpecialJobCleanup;
-- This procedure is used for cleaning up stale data entries form the InternalJobInfo table.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.InternalJobInfoCheckForStaleData;



-- Handle processing that is necessary in the InternalJobStepInfo table so the JobStepInfo reflects that the JobStep has started.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobStepInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobStepInfoJobStepStarted;
-- Handle processing that is necessary in the InternalJobStepInfo table so the JobStepInfo reflects that the JobStep has ended.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobStepInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobStepInfoJobStepEnded;
-- Remove the specified JobStep information row from this table.
CREATE PROCEDURE InternalJobStepInfoRemove
   PARTITION ON TABLE InternalJobStepInfo COLUMN JobId PARAMETER 0
   AS DELETE FROM InternalJobStepInfo WHERE (JobId=? AND JobStepId=?);
-- Remove any "expired" JobStepInfo rows from this table
--    (where it is specified JobId and specified JobStepId and the WlmJobStepEndTime <= specified timestamp).
CREATE PROCEDURE InternalJobStepInfoRemoveExpiredSpeciallyCleanedUpJobSteps
   PARTITION ON TABLE InternalJobStepInfo COLUMN JobId PARAMETER 0
   AS DELETE FROM InternalJobStepInfo WHERE (JobId=? AND JobStepId=? AND WlmJobStepEndTime <=?);
-- Go through the JobStepInfo table and mark any in-flight JobSteps as ended AND get the VoltTable for the JobStepInfo that were marked ended by this processing.
CREATE PROCEDURE
   PARTITION ON TABLE InternalJobStepInfo COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.InternalJobStepInfoSpecialJobCleanup;



-- Insert an entry into the InternalInflightWlmPrologOrEpilogRequests table.
CREATE PROCEDURE InternalInflightWlmPrologOrEpilogRequestsInsert
   PARTITION ON TABLE InternalInflightWlmPrologOrEpilogRequests COLUMN WorkingAdapterType PARAMETER 0
   AS INSERT INTO InternalInflightWlmPrologOrEpilogRequests (WorkingAdapterType, WorkItemId, JobId, JobConstraints, JobNodeList, ResultFifoName, RequestStartTime)
         VALUES (?, ?, ?, ?, ?, ?, ?);
-- Get all of the inflight entries from the InternalInflightWlmPrologOrEpilogRequests table that are being handled by the specified type of adapter.
CREATE PROCEDURE InternalInflightWlmPrologOrEpilogRequestsGet
   PARTITION ON TABLE InternalInflightWlmPrologOrEpilogRequests COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT * FROM InternalInflightWlmPrologOrEpilogRequests WHERE WorkingAdapterType=?;
-- Delete an entry into the InternalInflightWlmPrologOrEpilogRequests table.
CREATE PROCEDURE InternalInflightWlmPrologOrEpilogRequestsDelete
   PARTITION ON TABLE InternalInflightWlmPrologOrEpilogRequests COLUMN WorkingAdapterType PARAMETER 0
   AS DELETE FROM InternalInflightWlmPrologOrEpilogRequests WHERE WorkingAdapterType=? AND WorkItemId=?;



CREATE PROCEDURE
   PARTITION ON TABLE JobStep COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.JobStepStarted;
CREATE PROCEDURE
   PARTITION ON TABLE JobStep COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.JobStepTerminated;
CREATE PROCEDURE
   PARTITION ON TABLE JobStep COLUMN JobId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.JobStepCleanup;
CREATE PROCEDURE JobStepHistoryOldestTimestamp
   AS SELECT MIN(LastChgTimestamp) FROM JobStep_History;



CREATE PROCEDURE
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 1
   FROM CLASS com.intel.dai.procedures.WorkItemQueue;
CREATE PROCEDURE
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.WorkItemFindAndOwn;
CREATE PROCEDURE
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.WorkItemFinished;
CREATE PROCEDURE
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.WorkItemFinishedDueToError;
CREATE PROCEDURE
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.WorkItemDone;
-- Handle the database processing that is necessary to find any zombie work items (that were being worked on by an adapter of this type that failed without releasing the work item)
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.WorkItemRequeueZombies;
-- Gets the State and Results (both Results and WorkingResults) fields from the WorkItem table for the specified WorkingAdapterType and WorkItem ID.
CREATE PROCEDURE WorkItemStateAndResults
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT State, Results, WorkingResults FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ? Order By WorkingAdapterType, Id;
-- Gets the results from the finished work item - will not return data unless the specified work item has "finished" (i.e., the working adapter is done with it)
-- Note: "finished" refers to either Finished or FinishedDueToError, need to check returned State to tell which is the case!
CREATE PROCEDURE WorkItemFinishedResults
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT State, Results FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ? AND (State = 'F' OR State = 'E') Order By WorkingAdapterType, Id;
-- Check & see if there are any work items queued for this type of adapter to HandleInputFromExternalComponent.
-- Note: We only want to know those work items that are runnable (State of Queued, Working, Requeued)
CREATE PROCEDURE WorkItemCountInputHandlers
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT COUNT(*) FROM WorkItem WHERE WorkingAdapterType = ? AND WorkToBeDone = 'HandleInputFromExternalComponent' AND (State='Q' OR State='W' OR State='R');
-- Check & see if there are any DataMover work items queued for this type of Adapter.
-- Note: We only want to know those work items that are runnable (State of Queued, Working, Requeued)
CREATE PROCEDURE WorkItemCountDataMovers
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT COUNT(*) FROM WorkItem WHERE WorkingAdapterType = ? AND WorkToBeDone = 'DataMover' AND (State='Q' OR State='W' OR State='R');
-- Check & see if there are any DataReceiver work items queued for this type of Adapter.
-- Note: We only want to know those work items that are runnable (State of Queued, Working, Requeued)
CREATE PROCEDURE WorkItemCountDataReceivers
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT COUNT(*) FROM WorkItem WHERE WorkingAdapterType = ? AND WorkToBeDone = 'DataReceiver' AND (State='Q' OR State='W' OR State='R');
-- Handle the database processing that is necessary to store data necessary to be able to restart this work item in the event of a failure of the adapter "working on" this work item.
CREATE PROCEDURE
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.WorkItemSaveRestartData;
-- Get the specified adapter instance's work item id.
CREATE PROCEDURE WorkItemGetAdaptersWorkItemId
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT Id FROM WorkItem WHERE (WorkingAdapterType=? AND WorkingAdapterId=? AND WorkToBeDone=?);
-- Check and see if there is any work item backlogged
-- (any work items that are not being worked in a timely manner).
CREATE PROCEDURE WorkItemBackLog
   AS SELECT Queue, WorkingAdapterType, Id, WorkToBeDone, State, DbUpdatedTimestamp FROM WorkItem WHERE (WorkToBeDone!='BaseWork' AND State IN ('Q', 'R') AND DbUpdatedTimestamp<=?) ORDER BY WorkingAdapterType, Id;
-- Get the information for active non-BaseWork work items that are have the specified work item state (e.g., state = 'W') of the specified type of adapter (e.g., 'PROVISIONER') on a specified service node (e.g., Queue='SN0-SSN2').
CREATE PROCEDURE WorkItemInfoNonBaseworkUsingAdaptertypeQueueState
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT Queue, WorkingAdapterId, WorkingAdapterType, Id, WorkToBeDone, State, DbUpdatedTimestamp FROM WorkItem WHERE  (WorkingAdapterType=? AND Queue=? AND State=? AND WorkToBeDone!='BaseWork')  ORDER BY WorkingAdapterType, Queue, Id;
-- Get the work item information for SMW and SSN DaiMgrs
CREATE PROCEDURE WorkItemsForSmwAndSsnDaimgrs
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT Queue, WorkingAdapterType, Id, State, WorkToBeDone, WorkingResults, DbUpdatedTimestamp FROM WorkItem WHERE (WorkingAdapterType=? AND WorkToBeDone In ('MotherSuperiorDaiMgr', 'ChildDaiMgr') AND State IN ('W', 'R')) Order By ID, DbUpdatedTimestamp;
-- Get the work item information for work items of specified working adapter type, queue, and work to be done.
CREATE PROCEDURE WorkItemInfoWrkadaptrtypeQueueWorktobddone
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT * FROM WorkItem WHERE (WorkingAdapterType=? AND Queue=? AND WorkToBeDone=?);
CREATE PROCEDURE WorkItemInfoWrkadaptrtypeWorktobddoneNotBaseWorkItem
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT * FROM WorkItem WHERE (WorkingAdapterType=? AND Queue!='BaseWorkItem' AND WorkToBeDone=?);
-- Get work item information for specified work item using WorkingAdapterType and WorkItemId.
--     Note: WorkingAdapterType and WorkItemId together provide a unique result.  NO need to loop through a number of rows!!!
CREATE PROCEDURE WorkItemInfoUsingWorkadaptertypeId
   PARTITION ON TABLE WorkItem COLUMN WorkingAdapterType PARAMETER 0
   AS SELECT * FROM WorkItem WHERE (WorkingAdapterType=? AND ID=?);


CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.AdapterStarted;
CREATE PROCEDURE
   PARTITION ON TABLE Adapter COLUMN AdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.AdapterTerminated;
-- Get the information for an adapter instance by specifying the type of adapter, the lctn the adapter instance is running on, and the instance's pid.
CREATE PROCEDURE
   PARTITION ON TABLE Adapter COLUMN AdapterType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.AdapterInfoUsingTypeLctnPid;
-- Get the list of active adapter instances and their current work item information.
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.AdapterListOfActiveAndWorkItems;
-- Get the list of active adapter instances running on the specified service node.
CREATE PROCEDURE AdapterInfoUsingSnLctn
   AS SELECT * FROM Adapter WHERE Lctn=? ORDER BY AdapterType, Pid;
-- Get adapter instance information for specified adapter using AdapterType and AdapterId.
--     Note: AdapterType and AdapterId together give a unique result.  NO need to loop through a number of rows!!!
CREATE PROCEDURE AdapterInstanceInfoUsingTypeId
   PARTITION ON TABLE Adapter COLUMN AdapterType PARAMETER 0
   AS SELECT * FROM Adapter WHERE (AdapterType=? AND ID=?);


-- Check & see if the specified BootImage entry exists in the data store and grab its associated information.
CREATE PROCEDURE BootImageGetInfo
   PARTITION ON TABLE BootImage COLUMN Id PARAMETER 0
   AS SELECT Description, State, BootImageFile, BootImageChecksum, BootStrapImageFile, BootStrapImageChecksum,
   BootOptions, KernelArgs, Files FROM BootImage WHERE Id=?;

-- Create/Update Bootimage Table
CREATE PROCEDURE BootImageUpdateInfo
   PARTITION ON TABLE BootImage COLUMN Id PARAMETER 0
   AS UPSERT INTO BootImage (Id, Description, BootImageFile, BootImageChecksum, BootOptions, BootStrapImageFile,
   BootStrapImageChecksum, State, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType,
   LastChgWorkItemId, KernelArgs, Files)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?);

-- Delete a row in Bootimage Table
CREATE PROCEDURE BootImageDeleteInfo
   PARTITION ON TABLE BootImage COLUMN Id PARAMETER 0
   AS DELETE FROM BootImage WHERE Id=?;

-- Print all Bootimage row Id's
CREATE PROCEDURE BootImageGetIds
   AS SELECT Id FROM BootImage;

--Get the ServiceOperation by location
CREATE PROCEDURE ServiceOperationGetInfo
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 0
   AS SELECT * FROM ServiceOperation WHERE Lctn=?;

CREATE PROCEDURE
   PARTITION ON TABLE Diag COLUMN DiagId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.DiagStarted;

CREATE PROCEDURE
   PARTITION ON TABLE Diag COLUMN DiagId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.DiagTerminated;

CREATE PROCEDURE
   PARTITION ON TABLE DiagResults COLUMN DiagId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.DiagResultSavePerUnit;



-- Store / insert a RAS event into the data store.
CREATE PROCEDURE
   PARTITION ON TABLE RasEvent COLUMN DescriptiveName PARAMETER 0
   FROM CLASS com.intel.dai.procedures.RasEventStore;

-- Method that updates the specified RAS event's JobId, Done, and DbUpdatedTimestamp columns.
CREATE PROCEDURE RasEventUpdate
  PARTITION ON TABLE RasEvent COLUMN DescriptiveName PARAMETER 2
  AS UPDATE RasEvent SET JobId=?, Done=?, DbUpdatedTimestamp=NOW WHERE (DescriptiveName=? AND Id=?);

CREATE PROCEDURE RasEventCountNodeResetRecently
   PARTITION ON TABLE RasEvent COLUMN DescriptiveName PARAMETER 0
   AS SELECT COUNT(*) FROM RasEvent WHERE DescriptiveName = ? AND Lctn = ? AND LastChgTimestamp >= ?;

-- Check & see if there is already RAS Event Meta data in the data store.
CREATE PROCEDURE RasEventCountMetaDataEntries
   AS SELECT COUNT(*) FROM RasMetaData;
CREATE PROCEDURE RasEventGetMetaDataUsingDescrName
   AS SELECT DescriptiveName AS Type, ControlOperation, Msg FROM RASMETADATA where (DescriptiveName like ?) LIMIT ?;
-- CREATE PROCEDURE RasEventList
--    AS  SELECT RasEvent.DescriptiveName, RasEvent.Timestamp, RasMetaData.Severity, RasEvent.Lctn, RasEvent.ControlOperation, RasMetaData.Msg FROM RasEvent INNER JOIN RasMetaData on RasEvent.DescriptiveName=RasMetaData.DescriptiveName Order By Timestamp DESC, Lctn;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventListAtTime;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventListByLimit;
-- This stored procedure returns an array of VoltTables:
--    the first contains the details for the list of RasEvents which the RAS adapter needs to work on
--    the second contains the maximum value for the LastChgTimestamp column in the above-mentioned list of RasEvents
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventListThatNeedToBeDone;



CREATE PROCEDURE
   PARTITION ON TABLE WlmReservation_History COLUMN ReservationName PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ReservationCreated;
CREATE PROCEDURE
   PARTITION ON TABLE WlmReservation_History COLUMN ReservationName PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ReservationUpdated;
CREATE PROCEDURE
   PARTITION ON TABLE WlmReservation_History COLUMN ReservationName PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ReservationDeleted;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ReservationListAtTime;
-- This stored procedure handles the processing needed when purging data from the WlmReservation_History table
-- (we don't want to purge a reservation that is still "in effect", i.e., reservation has not ended and has not been deleted).
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.ReservationPurging;



-- This procedure is used for getting a list of records that have been changed during the specified interval of time, from the list of tables that need to be "moved" from Tier1 to Tier2.
-- (this is an integral piece of the DataMover paradigm)
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.DataMoverGetListOfRecsToMove;

-- Temporary method needed when using Volt as Tier2 - this method updates an existing record in the Tier2_WorkItem_History table.
CREATE PROCEDURE Tier2_UpdateWorkItem
   PARTITION ON TABLE Tier2_WorkItem_History COLUMN WorkingAdapterType PARAMETER 5
   AS UPDATE Tier2_WorkItem_History SET WorkingResults=?, DbUpdatedTimestamp=?, RequestingWorkItemId=?, RowInsertedIntoHistory=?, Tier2DbUpdatedTimestamp=? WHERE WorkingAdapterType=? AND Id=? AND State=? AND WorkingAdapterId=?;


-- Get the information for the specified ConstraintId.
CREATE PROCEDURE ConstraintInfo
   PARTITION ON TABLE Constraint COLUMN ConstraintId PARAMETER 0
   AS SELECT * FROM Constraint WHERE ConstraintId=?;



CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.DiagListOfActiveDiagsAtTime;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.DiagListOfNonActiveDiagsAtTime;
CREATE PROCEDURE
   PARTITION ON TABLE Diag_List COLUMN DiagListId PARAMETER 0
   FROM CLASS com.intel.dai.procedures.DiagGetDiagToolId;

CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.JobHistoryListOfActiveJobsAtTime;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.JobHistoryListOfNonActiveJobsAtTime;

-- This stored procedure returns all the contents in the LogicalGroups table for the specified GroupName.
CREATE PROCEDURE ListLogicalGroups
   PARTITION ON TABLE LogicalGroups COLUMN GroupName PARAMETER 0
   AS SELECT * FROM LogicalGroups WHERE GroupName=?;

-- This stored procedure returns GroupName(s) in the LogicalGroups table
CREATE PROCEDURE ListGroupNames
   AS SELECT DISTINCT GroupName FROM LogicalGroups ORDER BY GroupName;

CREATE PROCEDURE UpsertLogicalGroups
   PARTITION ON TABLE LogicalGroups COLUMN GroupName PARAMETER 0
   AS UPSERT INTO LogicalGroups (GroupName, DeviceList, GroupModifiedTimestamp)
             VALUES (?, ?, CURRENT_TIMESTAMP );

CREATE PROCEDURE DeleteGroupInLogicalGroups
   PARTITION ON TABLE LogicalGroups COLUMN GroupName PARAMETER 0
   AS DELETE FROM LogicalGroups WHERE GroupName=?;


CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.TempStoredProcToTestTimeouts;


CREATE PROCEDURE GetManifestContent
   AS SELECT manifestcontent from Machine;


-- This stored procedure adds a new entry to ServiceOperation to indicate start of a service operation
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceStarted;

-- This stored procedure adds a new entry to ServiceOperation to indicate completion of service-start operation
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceStartPrepared;

-- This stored procedure adds a new entry to ServiceOperation to indicate failure of service-start operation
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ServiceStartFailed;

-- This stored procedure updates an existing  ServiceOperation of type "Repair"  to indicate end of repair
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.ServiceEndRepair;

-- This stored procedure updates an existing  ServiceOperation of type "Repair"  to indicate end of repair
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.ServiceEndRepairError;

-- This stored procedure updates an existing  ServiceOperation as "Complete" and removes it after adding it to history
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.ServiceCloseOperation;

-- This stored procedure updates an existing  ServiceOperation as "Complete" and status "Error"(forceclosed) removes it after adding it to history
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.ServiceForceCloseOperation;

-- Updates the specified operation and inserts a new record into corresponding history table
CREATE PROCEDURE
   PARTITION ON TABLE ServiceOperation COLUMN Lctn PARAMETER 1
   FROM CLASS com.intel.dai.procedures.ServiceUpdateRemarks;

CREATE PROCEDURE ComputeNodeInventoryList
  AS SELECT DISTINCT(lctn) lctn, state, hostname, bootimageid, ipaddr, macaddr, bmcipaddr,
  bmcmacaddr, bmchostname, dbupdatedtimestamp, owner, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid from
  ComputeNode order by lctn, dbupdatedtimestamp desc;

CREATE PROCEDURE ServiceNodeInventoryList
  AS SELECT DISTINCT(lctn) lctn, hostname, state, bootimageid, ipaddr, macaddr, bmcipaddr, bmcmacaddr,
  bmchostname, owner, dbupdatedtimestamp, lastchgtimestamp, lastchgadaptertype, lastchgworkitemid from
  ServiceNode order by lctn, dbupdatedtimestamp desc;

CREATE PROCEDURE
   PARTITION ON TABLE UcsConfigValue COLUMN Key PARAMETER 0
  FROM CLASS com.intel.dai.procedures.UcsConfigValueSet;

-- Get the UcsConfig value for the key provided
CREATE PROCEDURE UcsConfigValueGet
   PARTITION ON TABLE UcsConfigValue COLUMN Key PARAMETER 0
   AS SELECT Value from UcsConfigValue where Key = ?;


-- This stored procedure handles the processing needed when purging data out of the NodeInventory_History table
-- (this table needs special processing as we do not want to purge the inventory info for the "active" compute node).
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.NodePurgeInventory_History;
-- This stored procedure returns an array of VoltTables:
--    the first contains the list of compute nodes that are being serviced (compute nodes which have an Owner = 'S')
--    the second contains the list of service nodes that are being serviced (service nodes which have an Owner = 'S')
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.NodeListBeingServiced;
-- This procedure goes through all of the node components (e.g., Dimms, Processors, Accelerators, Hfi Nics) and ensures that they all have the specified state.
--    It will only change the state of those components that currently have a different state (it does not change those that already have the specified state).
--    Note: it is NOT A MISTAKE that "PARTITION ON TABLE Dimm" is specified, even though other tables are referenced in this procedure.
--          Since all of these tables have the exact same node lctn strings, the partitioning is done correctly and successfully!
CREATE PROCEDURE
   PARTITION ON TABLE Dimm COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.NodeComponentSetStateUnlessInThatState;


-- This stored procedure returns all the contents in the Dimm table
CREATE PROCEDURE DimmsList
   AS SELECT Lctn, ModuleLocator FROM Dimm ORDER BY Lctn;
-- Get the specified Dimm's current info.
CREATE PROCEDURE DimmInfo
   PARTITION ON TABLE Dimm COLUMN NodeLctn PARAMETER 0
   AS SELECT * FROM Dimm WHERE NodeLctn=? AND Lctn=?;
-- Set this Dimm's state.
CREATE PROCEDURE
   PARTITION ON TABLE Dimm COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.DimmSetState;
-- Set this Dimm's State, SizeMB, and BankLocator.
CREATE PROCEDURE
   PARTITION ON TABLE Dimm COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.DimmSetStateSizeBank;



-- This stored procedure returns all the contents in the Processor table
CREATE PROCEDURE ProcessorsList
   AS SELECT * FROM Processor ORDER BY Lctn;
-- Get the specified Processor's current info.
CREATE PROCEDURE ProcessorInfo
   PARTITION ON TABLE Processor COLUMN NodeLctn PARAMETER 0
   AS SELECT * FROM Processor WHERE NodeLctn=? AND Lctn=?;
-- Set this Processor's state.
CREATE PROCEDURE
   PARTITION ON TABLE Processor COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.ProcessorSetState;



-- This stored procedure returns all the contents in the Accelerator table
CREATE PROCEDURE AcceleratorsList
   AS SELECT Lctn, Slot FROM Accelerator ORDER BY Lctn;
---------------------------------------- CREATE PROCEDURE AcceleratorsListByLimit
----------------------------------------   AS SELECT Lctn, Slot FROM Accelerator Limit 10000 Offset ?;
-- Get the specified Accelerator's current info.
CREATE PROCEDURE AcceleratorInfo
   PARTITION ON TABLE Accelerator COLUMN NodeLctn PARAMETER 0
   AS SELECT * FROM Accelerator WHERE NodeLctn=? AND Lctn=?;
-- Set this Accelerator's state.
CREATE PROCEDURE
   PARTITION ON TABLE Accelerator COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.AcceleratorSetState;
-- Set this Accelerator's State and Slot.
CREATE PROCEDURE
   PARTITION ON TABLE Accelerator COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.AcceleratorSetStateBusAddr;


-- This stored procedure returns all the contents in the Hfi table
CREATE PROCEDURE HfisList
   AS SELECT Lctn, Slot FROM Hfi ORDER BY Lctn;
-------------------- CREATE PROCEDURE HfisListByLimit
--------------------    AS SELECT Lctn, Slot FROM Hfi Limit 10000 Offset ?;
-- Get the specified Hfi's current info.
CREATE PROCEDURE HfiInfo
   PARTITION ON TABLE Hfi COLUMN NodeLctn PARAMETER 0
   AS SELECT * FROM Hfi WHERE NodeLctn=? AND Lctn=?;
-- Set this Hfi's state.
CREATE PROCEDURE
   PARTITION ON TABLE Hfi COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.HfiSetState;
-- Set this Hfi's State and Slot.
CREATE PROCEDURE
   PARTITION ON TABLE Hfi COLUMN NodeLctn PARAMETER 0
   FROM CLASS com.intel.dai.procedures.HfiSetStateBusAddr;


-- >>> Inventory stored procedures

-- Raw inventory history from foreign servers
CREATE PROCEDURE RawInventoryHistoryInsert
   AS UPSERT INTO RawHWInventory_History(Action, ID, FRUID, ForeignTimestamp, DbUpdatedTimestamp)
        VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP);

CREATE PROCEDURE RawInventoryHistoryDelete   -- needed for testing
    AS DELETE FROM RawHWInventory_History;

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

-- <<< Inventory stored procedures

--- >>> New Inventory
-- All non-source columns contain data duplicated from the JSON blob.  This redundancy allows us to leverage the power
-- of a relational database.

CREATE TABLE Raw_DIMM
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    serial             VARCHAR(50) UNIQUE NOT NULL,
    mac                VARCHAR(50)        NOT NULL, -- foreign key into Raw_FRU_host
    locator            VARCHAR(50)        NOT NULL,
    source             VARCHAR(5000)      NOT NULL,
    doc_timestamp      TIMESTAMP,                   -- 1 second resolution
    DbUpdatedTimestamp TIMESTAMP          NOT NULL,
    PRIMARY KEY (serial)
);

CREATE TABLE tier2_Raw_DIMM
(
    id                 VARCHAR(50),                 -- Elasticsearch doc id
    serial             VARCHAR(50) UNIQUE NOT NULL,
    mac                VARCHAR(50)        NOT NULL, -- foreign key into Raw_FRU_host
    locator            VARCHAR(50)        NOT NULL,
    source             VARCHAR(5000)      NOT NULL,
    doc_timestamp      TIMESTAMP,                   -- 1 second resolution
    DbUpdatedTimestamp TIMESTAMP          NOT NULL,
    EntryNumber        BigInt             NOT NULL,
    PRIMARY KEY (serial)
);

CREATE TABLE Raw_FRU_Host
(
    id                 VARCHAR(50), -- Elasticsearch doc id
    boardSerial        VARCHAR(50),
    mac                VARCHAR(50) UNIQUE NOT NULL,
    source             VARCHAR(10000)     NOT NULL,
    doc_timestamp      TIMESTAMP,   -- 1 second resolution
    DbUpdatedTimestamp TIMESTAMP          NOT NULL,
    PRIMARY KEY (mac)
);

CREATE TABLE tier2_Raw_FRU_Host
(
    id                 VARCHAR(50), -- Elasticsearch doc id
    boardSerial        VARCHAR(50),
    mac                VARCHAR(50) UNIQUE NOT NULL,
    source             VARCHAR(10000)     NOT NULL,
    doc_timestamp      TIMESTAMP,   -- 1 second resolution
    DbUpdatedTimestamp TIMESTAMP          NOT NULL,
    EntryNumber        BigInt             NOT NULL,
    PRIMARY KEY (mac)
);

CREATE TABLE Raw_Node_Inventory_History
(
    source             VARCHAR(70000) NOT NULL,
    DbUpdatedTimestamp TIMESTAMP      NOT NULL,
    PRIMARY KEY (DbUpdatedTimestamp)
);

CREATE PROCEDURE Raw_DIMM_Insert AS
    UPSERT INTO Raw_DIMM(id, serial, mac, locator, source, doc_timestamp, DbUpdatedTimestamp)
        VALUES(?, ?, ?, ?, ?, TO_TIMESTAMP(SECOND, ?), CURRENT_TIMESTAMP);

CREATE PROCEDURE Raw_FRU_Host_Insert AS
    UPSERT INTO Raw_FRU_Host(id, boardSerial, mac, source, doc_timestamp, DbUpdatedTimestamp)
        VALUES(?, ?, ?, ?, TO_TIMESTAMP(SECOND, ?), CURRENT_TIMESTAMP);

-- The parameter is the mac address of the FRU host under consideration
CREATE PROCEDURE Get_Dimms_on_FRU_Host AS
SELECT *
FROM Raw_DIMM
WHERE mac = ?;

CREATE PROCEDURE Get_FRU_Hosts AS SELECT * FROM Raw_FRU_Host ORDER BY doc_timestamp;

CREATE PROCEDURE Raw_Node_Inventory_History_insert AS
    INSERT INTO Raw_Node_Inventory_History(source, DbUpdatedTimestamp)
        VALUES(?, CURRENT_TIMESTAMP);

--- <<< New Inventory