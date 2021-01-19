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

END_OF_BATCH

UPDATE DbStatus SET Status='schema-loaded', Description='', SchemeCompletedStamp=CURRENT_TIMESTAMP WHERE Id=0;
