file -inlinebatch END_OF_BATCH

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
   AS SELECT State FROM ComputeNode WHERE Lctn = ?;
-- Get the Compute Node's MacAddr for the specified node location.
CREATE PROCEDURE ComputeNodeMacAddr
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT MacAddr FROM ComputeNode WHERE Lctn = ?;
CREATE PROCEDURE ComputeNodeListLctnAndSeqNum
   AS SELECT Lctn, SequenceNumber FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ComputeNodeInfo
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT * FROM ComputeNode WHERE Lctn = ?;
CREATE PROCEDURE ComputeNodeBasicInformation
   AS SELECT Lctn, HostName, SequenceNumber, Owner FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ServiceNodeBasicInformation
   AS SELECT Lctn, HostName, SequenceNumber, Owner FROM ServiceNode Order By Lctn;
CREATE PROCEDURE ComputeNodeListLctnAndState
   AS SELECT Lctn, State FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ComputeNodeListLctnAndHostname
   AS SELECT Lctn, HostName FROM ComputeNode Order By Lctn;
CREATE PROCEDURE ComputeNodeListLctnHostnameAndBmcHostname
   AS SELECT Lctn, HostName, BmcHostName FROM ComputeNode Order By Lctn;
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
-- Get the inventory info for the specified compute node lctn - 0 rows returned means either a bad lctn string or lack of inventory info (null).
CREATE PROCEDURE ComputeNodeInventoryInfo
   PARTITION ON TABLE ComputeNode COLUMN Lctn PARAMETER 0
   AS SELECT ComputeNode.Lctn, ComputeNode.InventoryTimestamp, NodeInventory_History.InventoryInfo FROM ComputeNode INNER JOIN NodeInventory_History ON (ComputeNode.Lctn=NodeInventory_History.Lctn AND ComputeNode.InventoryTimestamp=NodeInventory_History.InventoryTimestamp) WHERE (ComputeNode.Lctn=? AND ComputeNode.InventoryTimestamp IS NOT NULL);



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
   As SELECT Lctn, HostName, BmcHostName From ServiceNode Order By Lctn;
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



CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.InternalCachedJobsAddNodeEntry;
CREATE PROCEDURE InternalCachedJobsTerminated
   AS UPDATE InternalCachedJobs SET EndTimestamp=?, DbUpdatedTimestamp=? WHERE JobId=?;
CREATE PROCEDURE InternalCachedJobsCleanUp
   AS DELETE FROM InternalCachedJobs WHERE JobId=?;
-- Get the JobId for the specified Node location that was active at the specified time
--    If job is still active (EndTimestamp is null) then
--       we want entries where the job's StartTimestamp <= the specified timestamp
--    If job has terminated (EndTimestamp is NOT null) then
--       we want entries where the job's StartTimestamp <= the specified timestamp <= job's EndTimestamp)
CREATE PROCEDURE InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp
   PARTITION ON TABLE InternalCachedJobs COLUMN NodeLctn PARAMETER 0
   AS SELECT JobId FROM InternalCachedJobs WHERE ((NodeLctn=?) AND (StartTimestamp<=?) AND ((EndTimestamp IS NULL) OR (? <= EndTimestamp)));
-- Remove any "expired" jobs from the InternalCachedJobs table
--    The specified timestamp will be used for determining which jobs are expired.
CREATE PROCEDURE InternalCachedJobsRemoveExpiredJobs
   AS DELETE FROM InternalCachedJobs WHERE ((EndTimestamp IS NOT NULL) AND (DbUpdatedTimestamp < ?));



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

CREATE PROCEDURE
   PARTITION ON TABLE RasEvent COLUMN EventType PARAMETER 0
   FROM CLASS com.intel.dai.procedures.RasEventStore;

CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventProcessNewControlOperations;

CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventListThatNeedJobId;

-- Method to update a RasEvent's JobId value.
CREATE PROCEDURE RasEventUpdateJobId
   PARTITION ON TABLE RasEvent COLUMN EventType PARAMETER 1
   AS UPDATE RasEvent SET JobId=?, DbUpdatedTimestamp=NOW WHERE (EventType=? AND Id=?);
-- Method to update a RasEvent's ControlOperationDone value.
CREATE PROCEDURE RasEventUpdateControlOperationDone
   PARTITION ON TABLE RasEvent COLUMN EventType PARAMETER 1
   AS UPDATE RasEvent SET ControlOperationDone=?, DbUpdatedTimestamp=NOW WHERE (EventType=? AND Id=?);


CREATE PROCEDURE RasEventCountNodeResetRecently
   PARTITION ON TABLE RasEvent COLUMN EventType PARAMETER 0
   AS SELECT COUNT(*) FROM RasEvent WHERE EventType = ? AND Lctn = ? AND  LastChgTimestamp >= ?;


-- Check & see if there is already RAS Event Meta data in the data store.
CREATE PROCEDURE RasEventCountMetaDataEntries
   AS SELECT COUNT(*) FROM RasMetaData;
CREATE PROCEDURE getAllEventMetaData
   AS SELECT EventType, DescriptiveName, ControlOperation, Msg FROM RASMETADATA where (EventType like ? OR DescriptiveName like ?) LIMIT ?;

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



-- This procedure is used for getting a list of records that have been changed during the specified interval of time, from the list of tables that need to be "moved" from Tier1 to Tier2.
-- (this is an integral piece of the DataMover paradigm)
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.DataMoverGetListOfRecsToMove;

-- Temporary method needed when using Volt as Tier2 - this method updates an existing record in the Tier2_WorkItem_History table.
CREATE PROCEDURE Tier2_UpdateWorkItem
   PARTITION ON TABLE Tier2_WorkItem_History COLUMN WorkingAdapterType PARAMETER 5
   AS UPDATE Tier2_WorkItem_History SET WorkingResults=?, DbUpdatedTimestamp=?, RequestingWorkItemId=?, RowInsertedIntoHistory=?, Tier2DbUpdatedTimestamp=? WHERE WorkingAdapterType=? AND Id=? AND State=? AND WorkingAdapterId=?;



-- CREATE PROCEDURE RasEventList
--    AS  SELECT RasEvent.EventType, RasEvent.Timestamp,  RasMetaData.Severity, RasEvent.Lctn, RasEvent.ControlOperation, RasMetaData.Msg FROM RasEvent INNER JOIN RasMetaData on RasEvent.EventType=RasMetaData.EventType Order By Timestamp DESC, Lctn;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventListAtTime;
CREATE PROCEDURE
   FROM CLASS com.intel.dai.procedures.RasEventListByLimit;

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

-- This stored procedure returns all the contents in the LogicalGroups table
CREATE PROCEDURE ListLogicalGroups
   AS SELECT * FROM LogicalGroups WHERE GroupName=? ORDER BY GroupName;

-- This stored procedure returns GroupName(s) in the LogicalGroups table
CREATE PROCEDURE ListGroupNames
   AS SELECT GroupName FROM LogicalGroups ORDER BY GroupName;

CREATE PROCEDURE UpsertLogicalGroups
   PARTITION ON TABLE LogicalGroups COLUMN GroupName PARAMETER 0
   AS UPSERT INTO LogicalGroups (GroupName, DeviceList, GroupModifiedTimestamp)
             VALUES (?, ?, CURRENT_TIMESTAMP );

CREATE PROCEDURE DeleteGroupInLogicalGroups
   PARTITION ON TABLE LogicalGroups COLUMN GroupName PARAMETER 0
   AS DELETE FROM LogicalGroups WHERE GroupName=?;

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


CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.UpsertLocationIntoHWInv;

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.DeleteAllLocationsAtIdFromHWInv;

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.AllLocationsAtIdFromHWInv;

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.NumberOfLocationsInHWInv;

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.HwInventoryHistoryInsert;

CREATE PROCEDURE FROM
    CLASS com.intel.dai.procedures.HwInventoryHistoryDump;

END_OF_BATCH

UPDATE DbStatus SET Status='schema-loaded', Description='', SchemeCompletedStamp=CURRENT_TIMESTAMP WHERE Id=0;
