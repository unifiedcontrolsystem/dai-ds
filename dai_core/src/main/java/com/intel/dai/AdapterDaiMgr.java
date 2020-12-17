package com.intel.dai;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.DbStatusApi;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.*;

import org.voltdb.client.*;
import org.voltdb.VoltTable;
import java.lang.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import com.intel.runtime_utils.RuntimeCommand;

/**
 * AdapterDaiMgr for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
 *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 */
public class AdapterDaiMgr {
          long    BacklogChkInterval           =      30 * 1000L;       // Number of milliseconds that we want to wait between checking for a backlog of work items.
    final long    DaimgrChkProofOfLifeInterval =      45 * 1000L;       // Number of milliseconds that we want to wait between checking to ensure that the SMW and SSN DaiMgrs are still active.
    final long    DaimgrLogProofOfLifeInterval =      15 * 1000L;       // Number of milliseconds that we want to wait between logging a DaiMgr Proof of Life for this DaiMgr.
    final long    ZombieChkInterval            =  2 * 60 * 1000L;       // Number of milliseconds that we want to wait between checks for zombie work items.
    final long    MaxNumMillisecDifferenceForSyncedClocks = 1000L;      // Maximum number of milliseconds difference between 2 system clocks in order to consider them "synced".
    final long    MaxNumMillisecDifferenceForDataMover    = 15 * 1000L; // Maximum number of millisecs difference between DataMover and DataReceiver work items (before classifying DataReceiver being stuck).
    final String  TimestampPrefix              = "Timestamp=";
    final String  MillisecPrefix               = "Millisecs=";
    final long    NodeChkMissingConsoleMsgsInterval = 60 * 60 * 1000L;  // Number of milliseconds that we want to wait between checking to ensure that the SMW and SSN DaiMgrs are still active.
    final long    NodeChkStuckShuttingDownInterval  =  1 * 60 * 1000L;  // Number of milliseconds that we want to wait between checking for nodes that are stuck shutting down (have started halting but have not finished in reasonable time).
    final long    NodeMaxShuttingDownInterval       =  5 * 60 * 1000L;  // Number of milliseconds that is the reasonable time that a node should take to finish shutting down.

    static String DbServers                    = null;                  // IP addresses for the VoltDB servers on this machine.  E.g., 192.168.10.1
    static String JavaExe                      = null;                  // Java executable path for this machine
    static String UcsClassPath                 = null;                  // Classpath for UCS for this machine
    static String UcsLogfileDirectory          = null;                  // UCS's log file directory for this machine
    static String UcsLog4jConfigurationFile    = null;                  // UCS's Log4j configuration file for this machine

    private static final long CONNECTION_TIMEOUT       = 10 * 1000L;    // Each attempt to connect to a VoltDB server will last up to 10 seconds.
    private static final long CONNECTION_LOOP_DELAY    = 15 * 1000L;    // Each attempt to connect to VoltDB will pause up to 15 seconds per loop of all servers.
    private static final long CONNECTION_TOTAL_TIMEOUT = 900 * 1000L;   // Total timeout for attempts to connect to VoltDB will last up to 15 minutes.

    // Customization
    boolean useConsoleMsgLogic;
    String systemDServiceName;

    // Constructor
    AdapterDaiMgr(IAdapter adapter, Logger logger, DataStoreFactory factory) {
        log_ = logger;
        this.adapter = adapter;
        this.factory = factory;
        mAdapterInstanceInfoList = new ArrayList<>();
        mTimeLastProvedAlive = 0L;
        mHaveStartedAdapterInstancesForThisSn = false;
        mAlreadyChkdInitialNodeStates = true;
        useConsoleMsgLogic = false;
        systemDServiceName = "dai-manager.service";
        mTimeLastCheckedZombies = 0L;
        mJavaHome = System.getProperty("java.home");
        JavaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        mTimeLastCheckedBacklog = System.currentTimeMillis();           // initialize this value to the current timestamp, so that the first backlog check isn't done until we have a chance to start the child adapters, etc.
        mTimeLastCheckedDaiMgrsActive = System.currentTimeMillis();     // initialize this value to the current timestamp, so that the first check isn't done until we have a chance to start stuff up.
        mInactiveDaiMgrWorkitemMap = new HashMap<String, Long>();
        mUsingSynthData = false;                                        // initialize this value to indicate that we are using real data, this may be overridden later.
        mProgChkIntrvlDataReceiver = 60 * 1000L;                        // Number of millisec to wait between checking that the DataReceiver is making progress (dynamic value that will rise and fall)
        mTimeLastCheckedDataRecvProgress = System.currentTimeMillis();  // initialize this value to the current timestamp, so that the first backlog check isn't done until we have a chance to start the child adapters, etc.
        mTimeLastCheckedNodesMissingConsoleMsgs = System.currentTimeMillis();  // initialize this value to the current timestamp, so that the first check isn't done until we have a chance to start the child adapters, etc.
        mTimeLastCheckedNodesStuckShuttingDown  = System.currentTimeMillis();  // initialize this value to the current timestamp, so that the first check isn't done until we have a chance to start the child adapters, etc.

        // Create a VoltDB Client with shorter timeout.
        ClientConfig config = new ClientConfig("", "", null);
        config.setConnectionResponseTimeout(CONNECTION_TIMEOUT); // 2 minutes is too long in this case, use much less.
        quickClient_ = ClientFactory.createClient(config);
    }   // End AdapterDaiMgr(String sThisAdaptersAdapterType, String sAdapterName) constructor

    // Member Data
    IAdapter                                adapter;
    Logger                                  log_;
    WorkQueue                               workQueue;
    private ArrayList<AdapterInstanceInfo>  mAdapterInstanceInfoList;                // array list of the adapter instances that this DAI Manager started on this service node.
    private long                            mTimeLastProvedAlive;                    // timestamp that we last provided DaiMgr Proof of Life (that this instance of DAI Manager is still alive).
    private boolean                         mHaveStartedAdapterInstancesForThisSn;
    private boolean                         mAlreadyChkdInitialNodeStates;           // flag indicating whether or not we have already checked initial state of "child" service / compute nodes.
    private long                            mTimeLastCheckedZombies;                 // timestamp that we last checked for zombie work items
    private String                          mJavaHome;
    private long                            mTimeLastCheckedBacklog;                 // timestamp that we last checked for backlog of work items.
    private long                            mTimeLastCheckedDaiMgrsActive;           // timestamp that we last checked to ensure that DaiMgrs are still active (on SMW and SSNs).
    private Map<String, Long>               mInactiveDaiMgrWorkitemMap;              // map that tracks the list of "known / already alerted" DaiMgr work items.
    private boolean                         mUsingSynthData;                         // flag indicating whether synthesized data or "real" data is being used to drive the machine.
    private long                            mProgChkIntrvlDataReceiver;              // number of millisec to wait between checking that the DataReceiver is making progress (dynamic value that will rise and fall)
    private long                            mTimeLastCheckedDataRecvProgress;        // timestamp that we last checked to make sure DataReceiver is making progress.
    private long                            mTimeLastCheckedNodesMissingConsoleMsgs; // timestamp that we last checked to make sure that the pertinent nodes have been updating their Node Proof of Life.
    private long                            mTimeLastCheckedNodesStuckShuttingDown;  // timestamp that we last checked for nodes that are stuck shutting down.
    String                                  mSnLctn;                                 // lctn string of the service node this adapter instance is running on.
    private DataStoreFactory                factory;
    private Client                          quickClient_;                            // Short timeout client.



    //--------------------------------------------------------------------------
    // This class tracks the information for an adapter instance that this DaiMgr started.
    //--------------------------------------------------------------------------
    static class AdapterInstanceInfo {
        // Constructor
        AdapterInstanceInfo(String sTypeOfAdapter, long lAdapterTypeInstanceNum, String sStartAdapterCmd, String sLogFile, Process oProcess, String sAdapterLctn, long lAdapterPid) {
            mAdapterType               = sTypeOfAdapter;
            mAdapterTypeInstanceNumber = lAdapterTypeInstanceNum;
            mStartAdapterCmd           = sStartAdapterCmd;
            mLogFile                   = sLogFile;
            mProcess                   = oProcess;
            mAdapterLctn               = sAdapterLctn;
            mAdapterPid                = lAdapterPid;
        }
        // Member data
        String  mAdapterType;
        long    mAdapterTypeInstanceNumber;
        String  mStartAdapterCmd;
        String  mLogFile;
        Process mProcess;
        String  mAdapterLctn;
        long    mAdapterPid;
    }   // End class AdapterInstanceInfo


    //--------------------------------------------------------------------------
    // This class is used within a thread to monitor a specific adapter instance so we know when it ends.
    //--------------------------------------------------------------------------
    static class MonitorAdapterInstance implements Runnable {
        MonitorAdapterInstance(AdapterInstanceInfo oAdapterInstanceInfo, long lTheDaiMgrsWorkItemId, Logger logger, IAdapter adapter) {
            log_ = logger;
            this.adapter = adapter;
            mAdapterInstanceInfo  = oAdapterInstanceInfo;
            mTheDaiMgrsWorkItemId = lTheDaiMgrsWorkItemId;
        }
        AdapterInstanceInfo mAdapterInstanceInfo;
        long                mTheDaiMgrsWorkItemId;
        final Logger        log_;
        final IAdapter      adapter;

        public void run() {
            String sTempAdapterInfo = "Type=" + mAdapterInstanceInfo.mAdapterType + ", Instance=" + mAdapterInstanceInfo.mAdapterTypeInstanceNumber +
                                      ", StartCmd=" + mAdapterInstanceInfo.mStartAdapterCmd + ", LogFile=" + mAdapterInstanceInfo.mLogFile +
                                      ", Process=" + mAdapterInstanceInfo.mProcess + ", AdapterLctn=" + mAdapterInstanceInfo.mAdapterLctn +
                                      ", PID=" + mAdapterInstanceInfo.mAdapterPid;
            try {
                //--------------------------------------------------------------
                // Wait for this adapter instance to end.
                //--------------------------------------------------------------
                log_.info("MonitorAdapterInstance - waiting for an adapter to end - %s", sTempAdapterInfo);
                int iExitValue = mAdapterInstanceInfo.mProcess.waitFor();
                //--------------------------------------------------------------
                // The adapter instance being monitored just ended!
                //--------------------------------------------------------------
                log_.warn("MonitorAdapterInstance - found that an adapter ended - %s", sTempAdapterInfo);
                //--------------------------------------------------------------
                // Clean up the deceased adapter instance's:
                // - Adapter table entry
                // - Base work item
                // (necessary so that it is possible to identify zombie work items and requeue them).
                //--------------------------------------------------------------
                // Check and see that the deceased adapter's adapter table entry still exists in the table (it may have already been cleaned up during the shutdown of the adapter instance itself).
                long lTermAdapterId = adapter.getAdapterInstancesAdapterId(mAdapterInstanceInfo.mAdapterType, mAdapterInstanceInfo.mAdapterLctn, mAdapterInstanceInfo.mAdapterPid);
                if (lTermAdapterId > 0) {
                    // the adapter table entry does still exist.
                    // Remove the deceased adapter instance's entry from the Adapter table
                    // (this is needed so that it is possible to identify zombie work items and requeue them).
                    adapter.teardownAdapter(mAdapterInstanceInfo.mAdapterType, lTermAdapterId, adapter.adapterType(), mTheDaiMgrsWorkItemId);
                    log_.warn("MonitorAdapterInstance - removed the entry from the Adapter table for the deceased adapter instance - %s", sTempAdapterInfo);
                    // Mark the deceased adapter's base work item as Finished (and implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
                    // (this will be done synchronously).
                    long lTermAdapterBaseWorkItemId = adapter.getAdapterInstancesBaseWorkItemId(mAdapterInstanceInfo.mAdapterType, lTermAdapterId);
                    if (lTermAdapterBaseWorkItemId > 0) {
                        adapter.teardownAdaptersBaseWorkItem("MonitorAdapterInstance - cleaning up base work item for deceased adapter instance", mAdapterInstanceInfo.mAdapterType, lTermAdapterBaseWorkItemId);
                        log_.warn("MonitorAdapterInstance - cleaned up the base work item for the deceased adapter instance - DeceasedAdapterType=%s, DeceasedAdapterWorkItemId=%d",
                                  mAdapterInstanceInfo.mAdapterType, lTermAdapterBaseWorkItemId);
                    }
                    else {
                        // the deceased adapter instance's base work item does not exist.
                        log_.info("MonitorAdapterInstance - did not 'finish' the deceased adapters base work item as it had already been removed - %s", sTempAdapterInfo);
                    }
                }
                else {
                    // the deceased adapter's adapter table entry does not exist.
                    log_.info("MonitorAdapterInstance - did not remove the deceased adapter instance from the Adapter table as it had already been removed - %s", sTempAdapterInfo);
                }
                //--------------------------------------------------------------
                // Log the fact that we detected that the adapter instance ended (may not be a problem, but is still something that should be logged in case it is needed for debug purposes).
                //--------------------------------------------------------------
                adapter.logRasEventNoEffectedJob("RasDaimgrDiscAnAdapterEnded"
                                                ,sTempAdapterInfo                   // instance data
                                                ,mAdapterInstanceInfo.mAdapterLctn  // Lctn
                                                ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                ,adapter.adapterType()              // type of adapter that is requesting this
                                                ,mTheDaiMgrsWorkItemId              // requesting work item
                                                );
            }
            catch (Exception e) {
                log_.error("MonitorAdapterInstance - %s - Exception occurred - %s!", sTempAdapterInfo, e);
                log_.error("%s", Adapter.stackTraceToString(e));
                try {
                    adapter.logRasEventNoEffectedJob("RasGenAdapterExceptionButContinue"
                                                    ,("Method=MonitorAdapterInstance, Exception=" + e)  // instance data
                                                    ,null                               // Lctn
                                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                    ,adapter.adapterType()              // type of adapter that is requesting this
                                                    ,mTheDaiMgrsWorkItemId              // requesting work item
                                                    );
                }
                catch (Exception e2) {}
            }
        }   // End run()
    }   // End class MonitorAdapterInstance


    static String keywordSubstitutions(String sTempStr, String sHostname, String sLctn, long lAdapterTypeInstanceNum) {
        final String JavaSubstitution = "$JAVA";
        while ( true ) {
            int iIndex = sTempStr.indexOf(JavaSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + JavaExe
                 + sTempStr.substring( (iIndex + JavaSubstitution.length()) )
                 ;
        }

        final String ClasspathSubstitution = "$CLASSPATH";
        while ( true ) {
            int iIndex = sTempStr.indexOf(ClasspathSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + UcsClassPath
                 + sTempStr.substring( (iIndex + ClasspathSubstitution.length()) )
                 ;
        }

        final String UcsClasspathSubstitution = "$UCSCLASSPATH";
        while ( true ) {
            int iIndex = sTempStr.indexOf(UcsClasspathSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + UcsClassPath
                 + sTempStr.substring( (iIndex + UcsClasspathSubstitution.length()) )
                 ;
        }

        final String HostnameSubstitution = "$HOSTNAME";
        while ( true ) {
            int iIndex = sTempStr.indexOf(HostnameSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + sHostname
                 + sTempStr.substring( (iIndex + HostnameSubstitution.length()) )
                 ;
        }

        final String LctnSubstitution = "$LCTN";
        while ( true ) {
            int iIndex = sTempStr.indexOf(LctnSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + sLctn
                 + sTempStr.substring( (iIndex + LctnSubstitution.length()) )
                 ;
        }

        final String AdapterTypeInstanceSubstitution = "$INSTANCE";
        while ( true ) {
            int iIndex = sTempStr.indexOf(AdapterTypeInstanceSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + lAdapterTypeInstanceNum
                 + sTempStr.substring( (iIndex + AdapterTypeInstanceSubstitution.length()) )
                 ;
        }

        final String VoltIpAddrsSubstitution = "$VOLTIPADDRS";
        while ( true ) {
            int iIndex = sTempStr.indexOf(VoltIpAddrsSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + DbServers
                 + sTempStr.substring( (iIndex + VoltIpAddrsSubstitution.length()) )
                 ;
        }

        final String Log4jConfigFileSubstitution = "$UCSLOG4JCONFIGURATIONFILE";
        while ( true ) {
            int iIndex = sTempStr.indexOf(Log4jConfigFileSubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + UcsLog4jConfigurationFile
                 + sTempStr.substring( (iIndex + Log4jConfigFileSubstitution.length()) )
                 ;
        }

        final String LogfileDirectorySubstitution = "$UCSLOGFILEDIRECTORY";
        while ( true ) {
            int iIndex = sTempStr.indexOf(LogfileDirectorySubstitution);
            if (iIndex == -1)
                break;
            sTempStr = sTempStr.substring(0, iIndex)
                 + UcsLogfileDirectory
                 + sTempStr.substring( (iIndex + LogfileDirectorySubstitution.length()) )
                 ;
        }

        return sTempStr;
    }   // End keywordSubstitutions(String sTempStr, String sHostname, String sLctn, long lAdapterTypeInstanceNum)


    //--------------------------------------------------------------------------
    // Check for any zombie work items - if we find any then requeue them so another adapter instance can pick up that work and finish them.
    // - Zombie work items refer to those work items that were being worked on by an adapter that subsequently died.
    //   When it died it left the work item in a state so that it looks like it is being worked on BUT in reality it is not.
    //   This procedure will requeue that work so it can be finished by a different adapter instance.
    //--------------------------------------------------------------------------
    public long requeueAnyZombieWorkItems() throws IOException, ProcCallException, InterruptedException {
        if ((System.currentTimeMillis() - mTimeLastCheckedZombies) >= ZombieChkInterval) {
            log_.info("requeueAnyZombieWorkItems - zombie work item check fired...");

            ClientResponse response = adapter.client().callProcedure("WorkItemRequeueZombies", adapter.adapterType());
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("requeueAnyZombieWorkItems - stored procedure WorkItemRequeueZombies failed - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                            IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            VoltTable vt = response.getResults()[0];

            // Get this adapter instance's base work item id
            // (it is possible that this particular method can be called during startup, when there is not yet an assigned base work item id).
            long lBaseWorkItemId = -99999L;  // initialize to a value indicating that there is not yet an assigned base work item id.
            if (workQueue != null)
                lBaseWorkItemId = workQueue.baseWorkItemId();

            // Handle any requeued work items.
            log_.info("requeueAnyZombieWorkItems - %d zombie work items were requeued", vt.getRowCount());
            if (vt.getRowCount() > 0) {
                // one or more work items were requeued.
                // Loop through the requeued zombie work items - logging the appropriate information.
                for (int iWorkItemCntr = 0; iWorkItemCntr < vt.getRowCount(); ++iWorkItemCntr) {
                    vt.advanceRow();
                    long   lWorkitemId                  = vt.getLong("WorkitemId");
                    String sWorkitemWorkingAdapterType  = vt.getString("WorkitemWorkingAdapterType");
                    long   lWorkitemWorkingAdapterId    = vt.getLong("WorkitemWorkingAdapterId");
                    String sWorkitemWorkToBeDone        = vt.getString("WorkitemWorkToBeDone");
                    log_.warn("requeueAnyZombieWorkItems - requeued WorkitemId=%d, WorkitemWorkingAdapterType=%s, WorkitemWorkingAdapterId=%d, WorkitemWorkToBeDone=%s",
                              lWorkitemId, sWorkitemWorkingAdapterType, lWorkitemWorkingAdapterId, sWorkitemWorkToBeDone);
                    String sTempPertinentInfo = "AdapterName=" + adapter.adapterName() + ", WorkitemId=" + lWorkitemId + ", WorkitemWorkingAdapterType=" + sWorkitemWorkingAdapterType +
                                                ", WorkitemWorkingAdapterId=" + lWorkitemWorkingAdapterId + ", WorkitemWorkToBeDone=" + sWorkitemWorkToBeDone;
                    adapter.logRasEventNoEffectedJob("RasGenAdapterRequeuedWorkItem"
                                                    ,sTempPertinentInfo                 // instance data
                                                    ,null                               // lctn
                                                    ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                    ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                                    ,lBaseWorkItemId                    // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                    );
                }   // loop through the requeued zombie work items - logging the appropriate information.
            }   // one or more work items were requeued.

            mTimeLastCheckedZombies = System.currentTimeMillis();  // reset last time we checked for zombie work items.
            return vt.getRowCount();  // number of zombie work items that were requeued.
        }

        return -1;  // interval has not yet expired so we didn't check for any zombie work items.
    }   // End requeueAnyZombieWorkItems()


    //--------------------------------------------------------------------------
    // Check and see if there is a DaiMgr adapter instance currently running on the specified Service Node.
    // Parms:
    //      snLctn - the lctn of the service node that we want to know if there is a DaiMgr adapter instance running on
    // Returns:
    //      true  = there is a DaiMgr adapter instance running the specified service node
    //      false = there is NOT a DaiMgr adapter instance running the specified service node
    //--------------------------------------------------------------------------
    boolean isDaiMgrRunningOnThisSn(String sSnLctn) throws IOException, ProcCallException {
        // Get a list of all the DaiMgr adapter instances running on the specified service node.
        String sAdapterType = "DAI_MGR";
        long   lAdapterPid  = -99999;  // -99999 (actually any negative number) indicates we want a list of adapter instances with specified type on the specified service node.
        ClientResponse response = adapter.client().callProcedure("AdapterInfoUsingTypeLctnPid", sAdapterType, sSnLctn, lAdapterPid);  // -99999 (actually any negative number) indicates we want a list of adapter instances with specified type on the specified service node.
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("isDaiMgrRunningOnThisSn - Stored procedure AdapterInfoUsingTypeLctnPid FAILED - AdapterType=%s, AdapterLctn=%s, AdapterPid=%d, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       sAdapterType, sSnLctn, lAdapterPid,
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        // Ensure that there is at least one DaiMgr adapter instance.
        if (response.getResults()[0].getRowCount() > 0) {
            // there is at least one DaiMgr adapter instance on that service node.
            return true;
        }
        else {
            // there are no DaiMgr adapter instances on that service node.
            return false;
        }
    }   // End isDaiMgrRunningOnThisSn(String sSnLctn)


    //--------------------------------------------------------------------------
    // Check and see if there is a free/available adapter instance that can handle work of this specified "type".
    // Parms:
    //      sWorkitemWorkingAdapterType - the type of adapter we want to check for
    //      sWorkitemQueue              - the work queue we want to know if it can handle
    // Returns:
    //      true  = there is a free/available adapter instance available to handle work of the specified type
    //      false = there is NOT a free/available adapter instance available to handle work of the specified type
    //--------------------------------------------------------------------------
    boolean isThereFreeAdapterInstance(String sWorkitemWorkingAdapterType, String sWorkitemQueue) throws IOException, ProcCallException {

        // Get the number of active adapter instances of the specified adapter type which are on the specified service node.
        String sSnLctn = sWorkitemQueue;  // for non-null values, the work item queue == service node location.
        long   lAdapterPid = -99999;      // -99999 (actually any negative number) indicates we want a list of adapter instances with specified type on the specified service node.
        String sStoredProc = "AdapterInfoUsingTypeLctnPid";
        ClientResponse response = adapter.client().callProcedure(sStoredProc, sWorkitemWorkingAdapterType, sSnLctn, lAdapterPid);  // -99999 (actually any negative number) indicates we want a list of adapter instances with specified type on the specified service node.
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("isThereFreeAdapterInstance - Stored procedure %s FAILED - AdapterType=%s, AdapterLctn=%s, AdapterPid=%d, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       sStoredProc, sWorkitemWorkingAdapterType, sSnLctn, lAdapterPid,
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        long lNumActiveAdapterInstances = response.getResults()[0].getRowCount();

        // Get the number of non-BaseWork work items that are being worked on by an adapter instance (state = 'W') of the specified type of adapter on the specified service node.
        String sWorkItemState = "W";  // state of W indicates that the work item is being actively worked on.
        response = adapter.client().callProcedure("WorkItemInfoNonBaseworkUsingAdaptertypeQueueState", sWorkitemWorkingAdapterType, sWorkitemQueue, sWorkItemState);
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("isThereFreeAdapterInstance - Stored procedure %s FAILED - WiWorkingAdapterType=%s, WiQueue=%s, WiState=%s, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       sStoredProc, sWorkitemWorkingAdapterType, sWorkitemQueue, sWorkItemState,
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        long lNumActiveWorkItems = response.getResults()[0].getRowCount();

        // Check and see if there are more active adapters then the number of active work items - this will tell us if there are any free/available adapter instances.
        if (lNumActiveAdapterInstances > lNumActiveWorkItems) {
            // there is at least one free/available active adapter instance (since there are more active adapter instances then active work items being worked on).
            return true;
        }
        else {
            // there are no free/available active adapter instances (since the number of active adapter instances is the same as the number of active work items being worked on).
            return false;
        }
    }   // End isThereFreeAdapterInstance(String sWorkitemWorkingAdapterType, String sWorkitemQueue)


    //--------------------------------------------------------------------------
    // Start up an additional adapter instance of the specified type (PROVISIONER, WLM, RAS) on the correct Service Node, servicing the specified work item queue.
    // - This method is used when a backlog of work items is detected by the MoS DaiMgr, this method is called to add an additional adapter instance to help alleviate the backlog.
    // Parms:
    // Returns:
    //      0 = additional adapter instance was started
    //     -1 = did not start an additional adapter instance as there is not yet a DaiMgr adapter instance running on the subject service node.
    //     -2 = did not start an additional adapter instance as there is already a free adapter instance available to handle this type of work.
    //     -3 = unable to start an additional adapter instance as we could not retrieve its invocation information (the information on how to startup this adapter instance)!
    //--------------------------------------------------------------------------
    public long startAdditionalAdapterInstance(String sWorkitemWorkingAdapterType, String sWorkitemQueue) throws IOException, ProcCallException, InterruptedException {
        //----------------------------------------------------------------------
        // Get the information for how to start this new adapter instance out of the MachineAdapterInstance table.
        //----------------------------------------------------------------------
        String sTempStoredProcedure = "MachineAdapterInvocationInformation";
        ClientResponse response = null;
        try {
            response = adapter.client().callProcedure(sTempStoredProcedure, sWorkitemWorkingAdapterType, sWorkitemQueue);
        }
        catch (ProcCallException pce) {
            // stored procedure exception - could not get the info on how to start this adapter instance.
            log_.error("Exception occurred in stored procedure %s - SpecifiedWorkingAdapterType=%s, SpecifiedWorkItemQueue=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                       sTempStoredProcedure, sWorkitemWorkingAdapterType, sWorkitemQueue, adapter.adapterType(), adapter.adapterId());
            log_.error("%s", Adapter.stackTraceToString(pce));
            String sTempInstanceData = "StoredProcedure=" + sTempStoredProcedure + ", WorkingAdapterType=" + sWorkitemWorkingAdapterType + ", WorkItemQueue=" + sWorkitemQueue + ", Exception=" + pce;
            adapter.logRasEventNoEffectedJob("RasDaimgrStartAdapterInstanceFailed"
                                            ,sTempInstanceData                  // instance data
                                            ,null                               // Lctn
                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                            ,adapter.adapterType()              // type of adapter that is requesting this
                                            ,workQueue.workItemId()             // requesting work item
                                            );
            return -3L;
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("startAdditionalAdapterInstance - stored procedure %s failed - Status=%s, StatusString=%s, SpecifiedAdapterType=%s, SpecifiedWorkItemQueue=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                       sTempStoredProcedure, IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(),
                       sWorkitemWorkingAdapterType, sWorkitemQueue, adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        VoltTable vt = response.getResults()[0];
        vt.advanceRow();
        String sMaiiAdapterType     = vt.getString("AdapterType");
        String sMaiiSnLctn          = vt.getString("SnLctn");
        String sMaiiStartInvocation = vt.getString("Invocation");
        String sMaiiLogFile         = vt.getString("LogFile");

        //----------------------------------------------------------------------
        // Ensure that there is a DaiMgr adapter up and running on the Service Node that we want to start this additional instance on
        // (if not don't bother starting an additional adapter instance).
        // - note: this usually occurs with the Service Node has yet even been started / powered on.
        //         Queueing additional work items in this case will result in a very large number of adapters to start whenever the Service Node does come up!
        //----------------------------------------------------------------------
        if (!isDaiMgrRunningOnThisSn(sMaiiSnLctn)) {
            // there is NOT a dai mgr adapter running on this service node.
            // Short-circuit there is no reason to bother starting an additional adapter instance as there is no DaiMgr running on this Service Node.
            log_.warn("startAdditionalAdapterInstance - did NOT start the requested adapter instance, as there is not yet a DaiMgr adapter running on the subject Service Node (%s) - SpecifiedAdapterType=%s, SpecifiedWorkItemQueue=%s",
                      sMaiiSnLctn, sWorkitemWorkingAdapterType, sWorkitemQueue);
            String sTempPertinentInfo = "SpecifiedAdapterType=" + sWorkitemWorkingAdapterType + ", SpecifiedWorkItemQueue=" + sWorkitemQueue;
            adapter.logRasEventNoEffectedJob("RasDaimgrDidNotStartAddtlAdapterInstanceNoDaiMgr"
                                            ,sTempPertinentInfo                 // event instance data
                                            ,null                               // lctn
                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                            ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                            ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                            );
            return -1L;
        }

        //----------------------------------------------------------------------
        // Double check to ensure that there is not already a "free" adapter instance of this same adapter type
        // (if there is already a free instance it can/will handle any work, starting an additional instance is not necessary).
        //----------------------------------------------------------------------
        if (isThereFreeAdapterInstance(sWorkitemWorkingAdapterType, sWorkitemQueue)) {
            // there is already a free adapter instance that will handle this work.
            // Short-circuit there is no reason to bother starting an additional adapter instance as there already is a free adapter instance that can/will handle this type of work.
            log_.warn("startAdditionalAdapterInstance - did NOT start the requested adapter instance, as there is already a free/available adapter instance that will handle this work - SpecifiedAdapterType=%s, SpecifiedWorkItemQueue=%s",
                      sMaiiSnLctn, sWorkitemWorkingAdapterType, sWorkitemQueue);
            String sTempPertinentInfo = "SpecifiedAdapterType=" + sWorkitemWorkingAdapterType + ", SpecifiedWorkItemQueue=" + sWorkitemQueue;
            adapter.logRasEventNoEffectedJob("RasDaimgrDidNotStartAddtlAdapterInstanceAlreadyFreeInstance"
                                            ,sTempPertinentInfo                 // event instance data
                                            ,null                               // lctn
                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                            ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                            ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                            );
            return -2L;
        }

        //----------------------------------------------------------------------
        // Start the new adapter instance
        // (if we are on the correct Service Node we just start the adapter instance directly,
        //  otherwise we send a work item to the DaiMgr on the correct service node).
        //----------------------------------------------------------------------
        if (sMaiiSnLctn.equals(adapter.snLctn())) {
            // the new adapter instance should be started on the same service node we are on now.
            // Start up this requested adapter instance.
            startupAdapterInstanceOnThisSn(adapter.snLctn(), adapter.mapServNodeLctnToHostName().get(adapter.snLctn()), sMaiiAdapterType,
                                           sMaiiStartInvocation, sMaiiLogFile, workQueue.workItemId());
        }
        else {
            // the new adapter instance should be started on a different service node then we are on now.
            //----------------------------------------------------------------------
            // Queue a work item to a DaiMgr adapter (DAI_MGR) on the correct service node to start an additional instance of the specified adapter.
            //----------------------------------------------------------------------
            String sTypeOfAdapterToQueueWorkTo = adapter.adapterType();
            // Specify which adapter instance needs to do this new work item (which aggregator/service node can do this work).
            String sTempQueueForWi = sMaiiSnLctn;
            // Specify the work that needs to be done and its parameters.
            String sTempWorkToBeDone = "StartAdditionalChildAdapterInstance";
            String sTempParms = sMaiiAdapterType + "|" + sMaiiStartInvocation + "|" + sMaiiLogFile;
            // Actually queue the work item.
            long lQueuedWorkItemId = workQueue.queueWorkItem(sTypeOfAdapterToQueueWorkTo, sTempQueueForWi, sTempWorkToBeDone, sTempParms, false,  // false indicates that we do NOT want to know when this work item finishes
                                                             adapter.adapterType(), workQueue.workItemId());
            log_.info("startAdditionalAdapterInstance - successfully queued %s work item to start up an additional adapter instance - WorkToBeDone=%s, TypeOfAdapterToDoWork=%s, Aggregator=%s, Parms='%s', NewWorkItemId=%d",
                      sTempWorkToBeDone, sTempWorkToBeDone, sTypeOfAdapterToQueueWorkTo, sTempQueueForWi, sTempParms, lQueuedWorkItemId);
        }

        return 0L;
    }   // End startAdditionalAdapterInstance(String sWorkitemWorkingAdapterType, String sWorkitemQueue)


    //--------------------------------------------------------------------------
    // Check for a backlog of work items, work items that are not being executed in a timely manner, due to a lack of adapter instances capable of handling the work.
    //     When such an imbalance is detected, this DAI Mgr will cause additional adapter instance(s) to be started, alleviating the backlog.
    //--------------------------------------------------------------------------
    public long performAdapterInstanceLoadBalancing() throws IOException, ProcCallException, InterruptedException {
        if ((System.currentTimeMillis() - mTimeLastCheckedBacklog) >= BacklogChkInterval) {
            log_.info("performAdapterInstanceLoadBalancing - load balancing check fired...");
            // Calculate the timestamp to use when looking for backlog of WorkItems.
            long lBacklogTimestampMillisecs = (System.currentTimeMillis() - (35 * 1000L));  // only want to detect a backlog if the work item has been queued but is not being worked on for at least 35 seconds (value is in micro-seconds since epoch).
            // Get information on any backlogged work items.
            String sTempStoredProcedure = "WorkItemBackLog";
            ClientResponse response = adapter.client().callProcedure(sTempStoredProcedure, (lBacklogTimestampMillisecs * 1000L));
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("performAdapterInstanceLoadBalancing - stored procedure %s failed - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                           sTempStoredProcedure, IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            VoltTable vt = response.getResults()[0];

            // Get the timestamp we used for the backlog check into a "normalized" string format.
            SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
            Date dTemp = new Date(lBacklogTimestampMillisecs);
            String sTempBacklogSqlTimestamp = sqlDateFormat.format(dTemp);
            // Handle any backlogged work items.
            if (vt.getRowCount() > 0) {
                log_.warn("performAdapterInstanceLoadBalancing - there is a back log of %d work items - BacklogTimestamp='%s'!", vt.getRowCount(), sTempBacklogSqlTimestamp);
                // there is a backlog of one or more work items.
                // Loop through the backlog of work items.
                long lNumAddtlAdaptersStarted = 0L;
                String sPrevWiAdapterType = null;  // variables used to save away the previous work item's working adapter type - used to determine if we already started up another instance of this type.
                String sPrevQueue         = null;  // variables used to save away the previous work item's queue value - used to determine if we already started up another instance of this type.
                for (int iWorkItemCntr = 0; iWorkItemCntr < vt.getRowCount(); ++iWorkItemCntr) {
                    vt.advanceRow();
                    // Get pertinent info for this backlogged work item.
                    String sWorkitemWorkingAdapterType  = vt.getString("WorkingAdapterType");
                    long   lWorkitemId                  = vt.getLong("Id");
                    String sWorkitemQueue               = vt.getString("Queue");
                    String sWorkitemState               = vt.getString("State");
                    String sWorkitemWorkToBeDone        = vt.getString("WorkToBeDone");
                    //long   lWorkItemDbUpdatedTimestamp  = vt.getLong("DbUpdatedTimestamp");
                    // Log info about the backlogged work item.
                    log_.info("performAdapterInstanceLoadBalancing - backlogged workitem - WorkingAdapterType=%s, WorkitemId=%d, Queue='%s', State=%s, WorkToBeDone=%s",
                              sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemQueue, sWorkitemState, sWorkitemWorkToBeDone);
                    // Check & see if this is a "new type of adapter type" or if it is the same as the previous backlogged work item
                    // (the stored procedure sorts the work items so all work items of the same adapter type will appear next to each other).
                    if ((!Objects.equals(sPrevWiAdapterType, sWorkitemWorkingAdapterType)) || (!Objects.equals(sPrevQueue, sWorkitemQueue))) {  // safe comparison for strings when either or both may be null
                        // this is a different adapter type - start up an additional adapter instance of the specified type on the correct service node.
                        // Start up an additional adapter instance of the specified type (PROVISIONER, WLM, RAS) on the correct Service Node, servicing the specified work item queue.
                        long lRc = startAdditionalAdapterInstance(sWorkitemWorkingAdapterType, sWorkitemQueue);
                        if (lRc == 0L) {
                            // started an additional adapter instance.
                            // Log the fact that we created an additional adapter instance of this type.
                            log_.warn("performAdapterInstanceLoadBalancing - backlogged workitem - started an additional adapter of this type - AdapterType=%s, Queue='%s'!",
                                      sWorkitemWorkingAdapterType, sWorkitemQueue);
                            // Cut a ras event to capture the fact that we just created an additional instance.
                            String sTempPertinentInfo = "AdapterName=" + adapter.adapterName() + ", AdapterType=" + sWorkitemWorkingAdapterType + ", Queue=" + sWorkitemQueue;
                            adapter.logRasEventNoEffectedJob("RasDaimgrStartedAddtlAdapterInstance"
                                                            ,sTempPertinentInfo
                                                            ,null                               // lctn
                                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                            ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                                            ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                            );
                            ++lNumAddtlAdaptersStarted;  // bump the number of additional adapter instances that we started.
                        }
                        else if (lRc == -3L) {
                            // unable to start an additional adapter instance as we could not retrieve its invocation information (the information on how to startup this adapter instance)!
                            // Won't ever be able to start up an adapter instance to handle this work item - so "fail" this work item rather than let it keep retrying forever.
                            log_.error("Unable to get invocation info on how to start an additional adapter instance of this type to handle this backlogged workitem - failing this work item - WorkAdapterType=%s, Queue='%s', WorkToBeDone=%s, WorkItemId=%d!",
                                       sWorkitemWorkingAdapterType, sWorkitemQueue, sWorkitemWorkToBeDone, lWorkitemId);
                            workQueue.finishedWorkItemDueToError(sWorkitemWorkToBeDone, sWorkitemWorkingAdapterType, lWorkitemId,
                                                                 "startAdditionalAdapterInstance failed - no invocation info for this adapter!",
                                                                 "T");  // "T" = special case, skip the check for the WorkItem's state and mark the work item finished due to error regardless.
                            // Cut a ras event to capture that we failed this work item (since we could not start an adapter instance to handle it).
                            String sTempPertinentInfo = "WorkingAdapterType=" + sWorkitemWorkingAdapterType + ", WorkItemQueue=" + sWorkitemQueue + ", WorkToBeDone=" + sWorkitemWorkToBeDone + ", WorkItemId=" + lWorkitemId;
                            adapter.logRasEventNoEffectedJob("RasDaimgrLoadBalancingFailedWorkItemDueToNoInvocationInfo"
                                                            ,sTempPertinentInfo
                                                            ,null                               // lctn
                                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                            ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                                            ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                            );
                        }
                        // Save away the new values for previous work item's working adapter type and queue.
                        sPrevWiAdapterType = sWorkitemWorkingAdapterType;  // variables used to save away the previous work item's working adapter type - used to determine if we already started up another instance of this type.
                        sPrevQueue         = sWorkitemQueue;               // variables used to save away the previous work item's queue value - used to determine if we already started up another instance of this type.
                    }   // this is a different adapter type.
                    else {
                        // this is a "repeat" adapter type - no need to start another adapter instance.
                        log_.info("performAdapterInstanceLoadBalancing - backlogged workitem - did not start an additional adapter of this type as one was already started - AdapterType=%s, Queue='%s'",
                                  sWorkitemWorkingAdapterType, sWorkitemQueue);
                    }   // this is a "repeat" adapter type - no need to start another adapter instance.
                }   // loop through the backlog of work items.
                log_.info("performAdapterInstanceLoadBalancing - load balancing check finished - started %d additional adapter instances", lNumAddtlAdaptersStarted);
            }   // one or more work items are backlogged.
            else {
                log_.info("performAdapterInstanceLoadBalancing - load balancing check finished - there is no back log of work items - BacklogTimestamp='%s'", sTempBacklogSqlTimestamp);
            }

            mTimeLastCheckedBacklog = System.currentTimeMillis();  // reset the last time we checked for a backlog of work items.
            return vt.getRowCount();  // number of back logged work items.
        }

        return -1;  // interval has not yet expired so we didn't check for any work item backlog.
    }   // End performAdapterInstanceLoadBalancing()


    //--------------------------------------------------------------------------
    // Ensure that CRITICAL child work items are "not stuck".
    //--------------------------------------------------------------------------
    public long ensureChildWrkItemsMakingProgress(String sSnLctn) throws IOException, ParseException, ProcCallException, InterruptedException {
        //----------------------------------------------------------------------
        // Ensure that DataReceiver (nearline tier adapter) is making progress (not stuck).
        //----------------------------------------------------------------------
        if ((System.currentTimeMillis() - mTimeLastCheckedDataRecvProgress) >= mProgChkIntrvlDataReceiver) {
            log_.info("ensureChildWrkItemsMakingProgress - DataReceiver check fired...");

            SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone

            //------------------------------------------------------------------
            // Get information on DataMover's work item.
            //------------------------------------------------------------------
            String sTempStoredProcedure = "WorkItemInfoWrkadaptrtypeQueueWorktobddone";
            String sWrkAdptrType="ONLINE_TIER", sQueue="", sWorkToBeDone="DataMover";
            long lDataMoverMillisecs = -99999;
            ClientResponse response = adapter.client().callProcedure(sTempStoredProcedure, sWrkAdptrType, sQueue, sWorkToBeDone);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("ensureChildWrkItemsMakingProgress - stored procedure %s failed - WorkingAdapterType='%s', Queue='%s', WorkToBeDone='%s' - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                           sTempStoredProcedure, sWrkAdptrType, sQueue, sWorkToBeDone,
                           IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            VoltTable vtDataMoverWi = response.getResults()[0];
            String sDataMoverWrkResults = null;
            if (vtDataMoverWi.getRowCount() > 0) {
                vtDataMoverWi.advanceRow();
                sDataMoverWrkResults = vtDataMoverWi.getString("WorkingResults");
                //--------------------------------------------------------------
                // Extract pertinent info from the working results field.
                //--------------------------------------------------------------
                String sDataMoverTs = workQueue.getTsFromWorkingResults(sDataMoverWrkResults);
                lDataMoverMillisecs = sdfSqlDateFormat.parse(sDataMoverTs).getTime();  // get millisecs since epoch for this timestamp.
            }

            //------------------------------------------------------------------
            // Get information on DataReceiver's work items.
            //------------------------------------------------------------------
            sTempStoredProcedure = "WorkItemInfoWrkadaptrtypeQueueWorktobddone";
            sWrkAdptrType="NEARLINE_TIER"; sQueue=""; sWorkToBeDone="DataReceiver";
            long lDataRcvrMillisecs = -99999;
            response = adapter.client().callProcedure(sTempStoredProcedure, sWrkAdptrType, sQueue, sWorkToBeDone);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("ensureChildWrkItemsMakingProgress - stored procedure %s failed - WorkingAdapterType='%s', Queue='%s', WorkToBeDone='%s' - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                           sTempStoredProcedure,
                           sWrkAdptrType, sQueue, sWorkToBeDone,
                           IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            VoltTable vtDataRcvrWi = response.getResults()[0];
            String sDataRcvrWrkResults = null;

            if (vtDataRcvrWi.getRowCount() > 0) {
                vtDataRcvrWi.advanceRow();
                sDataRcvrWrkResults = vtDataRcvrWi.getString("WorkingResults");
                //--------------------------------------------------------------
                // Extract pertinent info from the working results field.
                //--------------------------------------------------------------
                String sDataRcvrTs = workQueue.getTsFromWorkingResults(sDataRcvrWrkResults);
                lDataRcvrMillisecs = sdfSqlDateFormat.parse(sDataRcvrTs).getTime();  // get millisecs since epoch for this timestamp.
            }

            //------------------------------------------------------------------
            // Check & see that both DataMover and DataReceiver work items are present.
            //------------------------------------------------------------------
            if ((sDataMoverWrkResults != null) && (sDataRcvrWrkResults != null)) {
                // both DataMover and DataReceiver work items are present.
                //--------------------------------------------------------------
                // Check & see if DataReceiver is making progress.
                //--------------------------------------------------------------
                if ((lDataMoverMillisecs - lDataRcvrMillisecs) < MaxNumMillisecDifferenceForDataMover) {
                    // DataReceiver appears to be making progress.
                    log_.info("DataReceiver appears to be making progress");
                    // Reset the interval that will be used for determining when to check this item again.
                    mProgChkIntrvlDataReceiver = 60 * 1000L;  // check again in 1 minute.
                }
                else {
                    // appears that the DataReceiver may be stuck (DataReceiver is significantly behind the DataMover).
                    log_.error("DataReceiver appears stuck - DataMoverWrkResults='%s', DataReceiverWrkResults='%s'", sDataMoverWrkResults, sDataRcvrWrkResults);
                    // Cut a ras event to capture the fact that DataReceiver appears to be stuck.
                    String sTempPertinentInfo = "DetectingAdapterName=" + adapter.adapterName() + ", DataMoverWrkResults='" + sDataMoverWrkResults + "', DataReceiverWrkResults='" + sDataRcvrWrkResults + "'";
                    adapter.logRasEventNoEffectedJob("RasAntDataReceiverAppearsStuck"
                                                    ,sTempPertinentInfo
                                                    ,sSnLctn                            // lctn of this machine this executing adapter is running on
                                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                    ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                                    ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                    );
                    // Adjust the interval that will be used for determining when to check this item again - increase the interval so we don't flood admins with similar notifications.
                    mProgChkIntrvlDataReceiver = Math.min((60 * 60 * 1000L), (mProgChkIntrvlDataReceiver * 2));
                }
            }
            else {
                log_.warn("ensureChildWrkItemsMakingProgress - neither DataMover nor DataReceiver appear to be running, assume they are running using the enterprise edition of volt and do not need this mechanism");
                // Adjust the interval that will be used for determining when to check this item again - increase the interval so we don't flood admins with similar notifications.
                mProgChkIntrvlDataReceiver = Math.min((60 * 60 * 1000L), (mProgChkIntrvlDataReceiver * 2));
            }

            // Update the value for the last time we checked for a backlog of work items.
            mTimeLastCheckedDataRecvProgress = System.currentTimeMillis();
        }

        return 0;
    }   // End ensureChildWrkItemsMakingProgress(String sSnLctn)


    //--------------------------------------------------------------------------
    // Ensure that serial console messages are successfully flowing through into the DAI for the "pertinent" nodes
    // (via periodic "Node Proof of Life" messages).
    //--------------------------------------------------------------------------
    public long ensureNodeConsoleMsgsFlowingIntoDai() throws IOException, ParseException, ProcCallException, InterruptedException {
        if(!useConsoleMsgLogic)
            return 0L;
        if ((System.currentTimeMillis() - mTimeLastCheckedNodesMissingConsoleMsgs) >= NodeChkMissingConsoleMsgsInterval) {
            // it is time to check to ensure that the nodes are receiving the expected Node Proof of Life messages (proves that the DAI is receiving serial console messages from the nodes).
            SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
            long lTimeForNodePolMsgBeingMissing = System.currentTimeMillis() - NodeChkMissingConsoleMsgsInterval;  // timestamp - all nodes should have reported in after this time.
            String sTimeForNodePolMsgBeingMissing = sdfSqlDateFormat.format(new Date(lTimeForNodePolMsgBeingMissing));
            log_.info("Checking for missing console messages (%s)...", sTimeForNodePolMsgBeingMissing);
            //------------------------------------------------------------------
            // Get information on any nodes that have not reported a Node Proof of Life w/i the configured interval.
            //------------------------------------------------------------------
            final String[] aStoredProcs = new String[] { "ComputeNodeListOfMissingProofOfLifeMsgs", "ServiceNodeListOfMissingProofOfLifeMsgs" };
            for (String sTempStoredProc : aStoredProcs) {
                ClientResponse response = adapter.client().callProcedure(sTempStoredProc, sTimeForNodePolMsgBeingMissing);
                if (response.getStatus() != ClientResponse.SUCCESS) {
                    // stored procedure failed.
                    log_.error("Stored procedure %s failed - TimeForNodePolMsgBeingMissing='%s' - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                               sTempStoredProc, sTimeForNodePolMsgBeingMissing,
                               IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                    throw new RuntimeException(response.getStatusString());
                }
                VoltTable vtNodesWithMissingPolMsgs = response.getResults()[0];
                // Loop through the list of nodes with missing Node Proof of Life messages and cut a RAS event for each one.
                for (int iCntr=0; iCntr < vtNodesWithMissingPolMsgs.getRowCount(); ++iCntr) {
                    vtNodesWithMissingPolMsgs.advanceRow();
                    // Get this node's last proof of life timestamp into a UTC string value (this string value may be null).
                    String sNodesLastPolTs = null;
                    long lNodesLastPolTs = vtNodesWithMissingPolMsgs.getTimestampAsLong("ProofOfLifeTimestamp");
                    if (!vtNodesWithMissingPolMsgs.wasNull())
                        sNodesLastPolTs = sdfSqlDateFormat.format(new Date( lNodesLastPolTs / 1000L )); // get volt timestamp into UTC string value ( getTimestampAsTimestamp.toString() does NOT work).
                    // Log that this node has proof of life messages that have not flowed into the DAI.
                    log_.error("Node has missing Proof of Life messages - Lctn=%s, State=%s, LastProofOfLifeTs=%s",
                               vtNodesWithMissingPolMsgs.getString("Lctn"), vtNodesWithMissingPolMsgs.getString("State"), sNodesLastPolTs);
                    // Cut a ras event to capture the fact that we have not received expected serial console messages from this node!
                    String sTempPertinentInfo = "State=" + vtNodesWithMissingPolMsgs.getString("State") + ", LastProofOfLifeTs=" + sNodesLastPolTs;
                    adapter.logRasEventNoEffectedJob("RasDaimgrDetectedMissingConsoleMsgsForThisNode"
                                                    ,sTempPertinentInfo
                                                    ,vtNodesWithMissingPolMsgs.getString("Lctn")  // node that is missing console msgs
                                                    ,System.currentTimeMillis() * 1000L           // time that this was detected, in micro-seconds since epoch
                                                    ,adapter.adapterType()                        // type of the adapter that is logging ras event
                                                    ,workQueue.workItemId()                       // work item id for the work item that is being processed/executing
                                                    );
                }
            }
            // Update the value for the last time we checked for nodes with missing console messages.
            mTimeLastCheckedNodesMissingConsoleMsgs = System.currentTimeMillis();
        }
        return 0;
    }   // End ensureNodeConsoleMsgsFlowingIntoDai()


    //--------------------------------------------------------------------------
    // Detect and handle any nodes that are "stuck" shutting down / halting
    // (checks for nodes that have been in halting state for NodeMaxShuttingDownInterval or more minutes AND handles any outliers).
    //--------------------------------------------------------------------------
    public long checkNodesStuckShuttingDown() throws IOException, ParseException, ProcCallException, InterruptedException {
        if ((System.currentTimeMillis() - mTimeLastCheckedNodesStuckShuttingDown) >= NodeChkStuckShuttingDownInterval) {
            // it is time to check for and handle any nodes that are stuck shutting down.
            SimpleDateFormat sdfSqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sdfSqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // this line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
            long lTimeIndicatingNodeStuckShuttingDown = System.currentTimeMillis() - NodeMaxShuttingDownInterval;  // timestamp - if any node started halting on or before this time it is stuck.
            String sTimeIndicatingNodeStuckShuttingDown = sdfSqlDateFormat.format(new Date(lTimeIndicatingNodeStuckShuttingDown));
            log_.info("Checking for nodes that are stuck shutting down (started to halt before %s)...", sTimeIndicatingNodeStuckShuttingDown);
            //------------------------------------------------------------------
            // Get information on any nodes that started to shut down before the above calculated time.
            //------------------------------------------------------------------
            final String[] aStoredProcs = new String[] { "ComputeNodeListOfNodesStuckHalting", "ServiceNodeListOfNodesStuckHalting" };
            for (String sTempStoredProc : aStoredProcs) {
                ClientResponse response = adapter.client().callProcedure(sTempStoredProc, sTimeIndicatingNodeStuckShuttingDown);
                if (response.getStatus() != ClientResponse.SUCCESS) {
                    // stored procedure failed.
                    log_.error("Stored procedure %s failed - TimeIndicatingNodeStuckShuttingDown='%s' - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                               sTempStoredProc, sTimeIndicatingNodeStuckShuttingDown,
                               IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                    throw new RuntimeException(response.getStatusString());
                }
                VoltTable vtNodesStuckShuttingDown = response.getResults()[0];
                long lNumStuckNodes = vtNodesStuckShuttingDown.getRowCount();
                // Loop through the list of nodes 'that are stuck shutting down' and cut a RAS event for each one.
                if (sTempStoredProc.equals(aStoredProcs[0])) {
                    if (lNumStuckNodes == 0)
                        log_.info("There were %d compute nodes that were found to be stuck during shutdown", lNumStuckNodes);
                    else
                        log_.error("There were %d compute nodes that were found to be stuck during shutdown", lNumStuckNodes);
                }
                else {
                    if (lNumStuckNodes == 0)
                        log_.info("There were %d service nodes that were found to be stuck during shutdown", lNumStuckNodes);
                    else
                        log_.error("There were %d service nodes that were found to be stuck during shutdown", lNumStuckNodes);
                }
                for (int iCntr=0; iCntr < lNumStuckNodes; ++iCntr) {
                    vtNodesStuckShuttingDown.advanceRow();
                    String sNodeLctn = vtNodesStuckShuttingDown.getString("Lctn");  // grab this node's lctn.
                    String sNodeState= vtNodesStuckShuttingDown.getString("State");
                    // Get this node's last DbUpdateTimestamp into a UTC string value.
                    long   lNodeLastDbUptdTs = vtNodesStuckShuttingDown.getTimestampAsLong("DbUpdatedTimestamp");
                    String sNodeLastDbUptdTs = sdfSqlDateFormat.format(new Date( lNodeLastDbUptdTs / 1000L )); // get volt timestamp into UTC string value ( getTimestampAsTimestamp.toString() does NOT work).
                    log_.error("Node appears to be stuck while halting / shutting down - Lctn=%s, State=%s, NodeLastDbUptdTs=%s",
                               sNodeLctn, sNodeState, sNodeLastDbUptdTs);
                    // Cut a ras event to capture this occurrence.
                    // Note: we are using 2 different RAS events for this situation as we think that we may end up wanting 2 different RAS event control operations, one for each type of node.
                    String sTempPertinentInfo = "State=" + sNodeState + ", NodeLastDbUptdTs=" + sNodeLastDbUptdTs;
                    if (adapter.isComputeNodeLctn(sNodeLctn)) {
                        // compute node
                        adapter.logRasEventNoEffectedJob("RasDaimgrDetectedComputeNodeDidNotShutdown"
                                                        ,sTempPertinentInfo
                                                        ,sNodeLctn                          // node that is missing console msgs
                                                        ,System.currentTimeMillis() * 1000L // time that this was detected, in micro-seconds since epoch
                                                        ,adapter.adapterType()              // type of the adapter that is logging ras event
                                                        ,workQueue.workItemId()             // work item id for the work item that is being processed/executing
                                                        );
                    }
                    else {
                        // service node
                        adapter.logRasEventNoEffectedJob("RasDaimgrDetectedServiceNodeDidNotShutdown"
                                                        ,sTempPertinentInfo
                                                        ,sNodeLctn                          // node that is missing console msgs
                                                        ,System.currentTimeMillis() * 1000L // time that this was detected, in micro-seconds since epoch
                                                        ,adapter.adapterType()              // type of the adapter that is logging ras event
                                                        ,workQueue.workItemId()             // work item id for the work item that is being processed/executing
                                                        );
                    }
                }
            }
            // Update the value for the last time we checked for nodes that are stuck shutting down.
            mTimeLastCheckedNodesStuckShuttingDown = System.currentTimeMillis();
        }
        return 0;
    }   // End checkNodesStuckShuttingDown()


    //--------------------------------------------------------------------------
    // Ensure that all of the SMW and Child DAI Managers are alive and active on the SSNs.
    //      This is done by querying all of the DaiMgr work items that have WorkToBeDone of MotherSuperiorDaiMgr or ChildDaiMgr and checking the DaiMgr ProofOfLife indicator in the WorkingResults field.
    //      When such an imbalance is detected, this DAI Mgr will cut a RAS event to notify that this event occurred.
    //--------------------------------------------------------------------------
    public long ensureDaiMgrsStillActive() throws IOException, AdapterException, ProcCallException, InterruptedException {
        long lCurMillisecs = System.currentTimeMillis();
        if ((lCurMillisecs - mTimeLastCheckedDaiMgrsActive) >= DaimgrChkProofOfLifeInterval) {
            log_.info("ensureDaiMgrsStillActive - check that the DaiMgr work items are still active...");
            // Get information on the pertinent DaiMgr work items.
            String sTempStoredProcedure = "WorkItemsForSmwAndSsnDaimgrs";
            ClientResponse response = adapter.client().callProcedure(sTempStoredProcedure, "DAI_MGR");
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("ensureDaiMgrsStillActive - stored procedure %s failed - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                           sTempStoredProcedure, IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            VoltTable vt = response.getResults()[0];

            // Calculate the timestamp to use when checking that they are still active (will compare this value to the ProofOfLife value in the working results field).
            long lActiveTimeChkMillisecs = (lCurMillisecs - DaimgrChkProofOfLifeInterval);  // only want to detect an inactive DaiMgr if the work item does not have a DaiMgr Proof of Life within last xx milliseconds (value is in milliseconds since epoch).
            // Check each of the returned DaiMgr work items.
            long lNumDetectedInactiveDaiMgrs = 0L;
            if (vt.getRowCount() > 0) {
                // there are DaiMgr work items to check (to ensure they are still active).
                // Get the active timestamp check value into human readable format.
                SimpleDateFormat sqlDateFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
                Date   dTemp = new Date(lActiveTimeChkMillisecs);
                String sTempActiveTimeChkSqlTimestamp = sqlDateFormat.format(dTemp);
                log_.info("ensureDaiMgrsStillActive - checking that %d DaiMgr work items are active - they need to have a DaiMgr ProofOfLife >= %d milliseconds / '%s'",
                          vt.getRowCount(), lActiveTimeChkMillisecs, sTempActiveTimeChkSqlTimestamp);
                // Loop through the work items.
                for (int iWorkItemCntr = 0; iWorkItemCntr < vt.getRowCount(); ++iWorkItemCntr) {
                    vt.advanceRow();
                    // Get pertinent info for this DaiMgr work item.
                    String sWorkitemQueue               = vt.getString("Queue");
                    String sWorkitemWorkingAdapterType  = vt.getString("WorkingAdapterType");
                    long   lWorkitemId                  = vt.getLong("Id");
                    String sWorkitemState               = vt.getString("State");
                    String sWorkitemWorkToBeDone        = vt.getString("WorkToBeDone");
                    String sWorkingResults              = vt.getString("WorkingResults");
                    ///long   lWorkItemDbUpdatedTimestamp  = vt.getLong("DbUpdatedTimestamp");
                    // Log info about this DaiMgr work item.
                    log_.info("ensureDaiMgrsStillActive - DaiMgr workitem - WorkingAdapterType=%s, WorkitemId=%d, Queue='%s', State=%s, WorkToBeDone=%s, WorkingResults='%s'",
                              sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemQueue, sWorkitemState, sWorkitemWorkToBeDone, sWorkingResults);
                    // Check & see if this DaiMgr work item is "inactive", i.e., has not updated its DaiMgr ProofOfLife recently.
                    // - Example of a DaiMgr WorkingResult's field "ProofOfLife (Millisecs=1561731561282) (Timestamp=2019-06-28 14:19:21.282)"
                    if (sWorkingResults != null) {
                        // working results have been filled in.
                        if (sWorkingResults.startsWith("ProofOfLife ")) {
                            // working results does have the expected format.
                            // Calculate the map key for this work item.
                            String sMapKey = sWorkitemWorkingAdapterType + " " + Long.toString(lWorkitemId);
                            // Get this work item's last proof of life value.
                            String[] aCols = sWorkingResults.split(MillisecPrefix);
                            long lWorkItemsLastProofOfLife = Long.parseLong( aCols[1].substring(0, aCols[1].indexOf(")")) );
                            // Check & see if this work item is inactive.
                            if (lWorkItemsLastProofOfLife < lActiveTimeChkMillisecs) {
                                // this work item is "inactive" - its ProofOfLife is too old.
                                // Check & see if this is an already known inactive DaiMgr work item.
                                if (mInactiveDaiMgrWorkitemMap.get(sMapKey) != null) {
                                    // this is an already known inactive work item (it is already in the map).
                                    log_.warn("ensureDaiMgrsStillActive - detected an already known inactive DaiMgr work item - Queue='%s', AdapterType=%s, WorkItemId=%d, State=%s, WorkToBeDone=%s, WorkingResults=%s",
                                              sWorkitemQueue, sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemState, sWorkitemWorkToBeDone, sWorkingResults);
                                }
                                else {
                                    // this is a "new" inactive work item (is not yet in the map).
                                    log_.error("ensureDaiMgrsStillActive - detected a new inactive DaiMgr work item - Queue='%s', AdapterType=%s, WorkItemId=%d, State=%s, WorkToBeDone=%s, WorkingResults=%s!",
                                               sWorkitemQueue, sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemState, sWorkitemWorkToBeDone, sWorkingResults);

                                    //------------------------------------------
                                    // Check & see if the SSN is already missing (it has been powered off), if it is already missing there is no need to use the RAS event that marks the SSN in error!
                                    //------------------------------------------
                                    // Get the SSN's current state.
                                    String sSsnState = "NotAState";  // initialize to a value that is obviously not an actual state value.
                                    String sTempStoredProcedureGetSsnInfo = "SERVICENODE.select";
                                    ClientResponse responseGetSsnInfo = adapter.client().callProcedure(sTempStoredProcedureGetSsnInfo, sWorkitemQueue);
                                    if (responseGetSsnInfo.getStatus() != ClientResponse.SUCCESS) {
                                        // stored procedure failed.
                                        log_.error("ensureDaiMgrsStillActive - stored procedure %s failed - Status=%s, StatusString=%s, ThisAdapterType=%s, ThisAdapterId=%d!",
                                                   sTempStoredProcedureGetSsnInfo, IAdapter.statusByteAsString(responseGetSsnInfo.getStatus()), responseGetSsnInfo.getStatusString(), adapter.adapterType(), adapter.adapterId());
                                        throw new RuntimeException(responseGetSsnInfo.getStatusString());
                                    }
                                    VoltTable vtGetSsnInfo = responseGetSsnInfo.getResults()[0];
                                    if (vtGetSsnInfo.getRowCount() > 0) {
                                        vtGetSsnInfo.advanceRow();
                                        sSsnState = vtGetSsnInfo.getString("State");
                                    }
                                    // Cut a ras event to capture this occurrence
                                    String sTempPertinentInfo = "Queue=" + sWorkitemQueue + ", AdapterType=" + sWorkitemWorkingAdapterType + ", WorkItemId=" + lWorkitemId +
                                                                ", State=" + sWorkitemState + ", WorkToBeDone=" + sWorkitemWorkToBeDone + ", WorkingResults=" + sWorkingResults;
                                    if (sSsnState.equals("A")) {
                                        // the SSN is currently considered active, so cut the flavor of ras event that also marks the SSN in error state
                                        // (we consider the SSN to be in error state because the SSN was active but now no longer has a DaiMgr adapter instance running).
                                        adapter.logRasEventNoEffectedJob("RasDaimgrDetectedInactiveDaiManagerAndPutSsnInError"
                                                                        ,sTempPertinentInfo
                                                                        ,sWorkitemQueue                     // lctn - for DaiMgr work items it is specified in the queue field
                                                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                                        ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                                                        ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                                        );
                                    }
                                    else {
                                        // the SSN is not in an active state, so NO need to cut the flavor of ras event that also marks the SSN in error (just use the ras event that logs when and why this event occurred).
                                        adapter.logRasEventNoEffectedJob("RasDaimgrDetectedInactiveDaiManager"
                                                                        ,sTempPertinentInfo
                                                                        ,sWorkitemQueue                     // lctn - for DaiMgr work items it is specified in the queue field
                                                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                                        ,adapter.adapterType()              // type of the adapter that is requesting/issuing this invocation
                                                                        ,workQueue.workItemId()             // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
                                                                        );
                                    }

                                    // Add this work item to the map of known inactive DaiMgr work items.
                                    mInactiveDaiMgrWorkitemMap.put(sMapKey, lWorkItemsLastProofOfLife);

                                    //------------------------------------------
                                    // Clean up the deceased adapter instances on this service node.
                                    //------------------------------------------
                                    // Get a list of all the DaiMgr adapter instances running on the specified service node.
                                    String sAdapterType = "DAI_MGR";
                                    long   lAdapterPid  = -99999;  // -99999 (actually any negative number) indicates we want a list of adapter instances with specified type on the specified service node.
                                    ClientResponse responseDeceasedAdapters = adapter.client().callProcedure("AdapterInfoUsingTypeLctnPid", sAdapterType, sWorkitemQueue, lAdapterPid);  // -99999 (actually any negative number) indicates we want a list of adapter instances with specified type on the specified service node.
                                    if (responseDeceasedAdapters.getStatus() != ClientResponse.SUCCESS) {
                                        // stored procedure failed.
                                        log_.fatal("ensureDaiMgrsStillActive - Stored procedure AdapterInfoUsingTypeLctnPid FAILED - AdapterType=%s, AdapterLctn=%s, AdapterPid=%d, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                                                   sAdapterType, sWorkitemQueue, lAdapterPid, IAdapter.statusByteAsString(responseDeceasedAdapters.getStatus()),
                                                   responseDeceasedAdapters.getStatusString(), adapter.adapterType(), adapter.adapterId());
                                        throw new RuntimeException(responseDeceasedAdapters.getStatusString());
                                    }
                                    // Loop through the list cleaning up these deceased adapter instances.
                                    VoltTable vtDeceasedAdapters = responseDeceasedAdapters.getResults()[0];
                                    for (int iAdapterCntr = 0; iAdapterCntr < vtDeceasedAdapters.getRowCount(); ++iAdapterCntr) {
                                        vtDeceasedAdapters.advanceRow();
                                        String sDeceasedAdapterType = vtDeceasedAdapters.getString("AdapterType");
                                        long   lDeceasedAdapterId   = vtDeceasedAdapters.getLong("Id");
                                        // Remove the deceased adapter instance's entry from the Adapter table
                                        // (this is needed so that it is possible to identify zombie work items and requeue them).
                                        adapter.teardownAdapter(sDeceasedAdapterType, lDeceasedAdapterId, adapter.adapterType() , workQueue.workItemId());
                                        log_.warn("ensureDaiMgrsStillActive - removed the entry from the Adapter table for the deceased adapter instance - AdapterType=%s, AdapterId=%d",
                                                  sDeceasedAdapterType, lDeceasedAdapterId);
                                        // Mark the deceased adapter's base work item as Finished (and implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
                                        // (this will be done synchronously).
                                        long lDeceasedAdapterBaseWorkItemId = adapter.getAdapterInstancesBaseWorkItemId(sDeceasedAdapterType, lDeceasedAdapterId);
                                        if (lDeceasedAdapterBaseWorkItemId > 0) {
                                            adapter.teardownAdaptersBaseWorkItem("ensureDaiMgrsStillActive - cleaning up base work item for deceased adapter instance", sDeceasedAdapterType, lDeceasedAdapterBaseWorkItemId);
                                            log_.warn("ensureDaiMgrsStillActive - cleaned up the base work item for the deceased adapter instance - DeceasedAdapterType=%s, DeceasedAdapterBaseWorkItemId=%d",
                                                      sDeceasedAdapterType, lDeceasedAdapterBaseWorkItemId);
                                        }
                                        else {
                                            // the deceased adapter instance's base work item does not exist.
                                            log_.info("ensureDaiMgrsStillActive - did not 'finish' the deceased adapters base work item as it had already been removed - AdapterType=%s, AdapterId=%d",
                                                      sDeceasedAdapterType, lDeceasedAdapterId);
                                        }
                                    }
                                }
                                // Bump the number of inactive DaiMgrs that were detected.
                                ++lNumDetectedInactiveDaiMgrs;
                            }   // this work item is "inactive" - its ProofOfLife is too old.
                            else {
                                // this work item is "active".
                                log_.debug("ensureDaiMgrsStillActive - detected an active DaiMgr work item - Queue='%s', AdapterType=%s, WorkItemId=%d, State=%s, WorkToBeDone=%s, WorkingResults=%s",
                                           sWorkitemQueue, sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemState, sWorkitemWorkToBeDone, sWorkingResults);
                                // Remove this work item from the map of known inactive DaiMgr work items (for the case when it was inactive but is now once again active).
                                mInactiveDaiMgrWorkitemMap.remove(sMapKey);
                            }   // this work item is "active".
                        }   // working results does have the expected format.
                        else {
                            // working results does not have the expected format.
                            log_.warn("ensureDaiMgrsStillActive - work item's working results does not start with 'ProofOfLife' - Queue='%s', AdapterType=%s, WorkItemId=%d, State=%s, WorkToBeDone=%s, WorkingResults=%s!",
                                      sWorkitemQueue, sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemState, sWorkitemWorkToBeDone, sWorkingResults);
                        }   // working results does not have the expected format.
                    }   // working results have been filled in.
                    else {
                        // working results have not yet been filled in.
                        log_.warn("ensureDaiMgrsStillActive - work item's working results have not yet been filled in - Queue='%s', AdapterType=%s, WorkItemId=%d, State=%s, WorkToBeDone=%s, WorkingResults=%s!",
                                  sWorkitemQueue, sWorkitemWorkingAdapterType, lWorkitemId, sWorkitemState, sWorkitemWorkToBeDone, sWorkingResults);
                        // Log the fact that we detected that this DaiMgr has not yet filled in any proof of life value (may not be a problem, but is still something that should be logged in case it is needed for debug purposes).
                        String sTempInstanceData = "Queue='" + sWorkitemQueue + "', AdapterType=" + sWorkitemWorkingAdapterType + ", WorkItemId=" + lWorkitemId
                                                 + ", State=" + sWorkitemState + ", WorkToBeDone=" + sWorkitemWorkToBeDone + ", WorkingResults='" + sWorkingResults + "'";
                        adapter.logRasEventNoEffectedJob("RasDaimgrDetectedDaimgrHasNotFilledInProofoflifeYet"
                                                        ,sTempInstanceData                  // instance data
                                                        ,sWorkitemQueue                     // Lctn
                                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                        ,adapter.adapterType()              // type of adapter that is requesting this
                                                        ,workQueue.workItemId()             // requesting work item
                                                        );
                    }   // working results have not yet been filled in.
                }   // loop through the work items.
                log_.info("ensureDaiMgrsStillActive - detected %d inactive DaiMgr work items", lNumDetectedInactiveDaiMgrs);
            }   // there are DaiMgr work items to check (to ensure they are still active).

            mTimeLastCheckedDaiMgrsActive = System.currentTimeMillis();  // reset the last time we checked for inactive DaiMgrs.
            return lNumDetectedInactiveDaiMgrs;  // number of DaiMgr work items that we detected.
        }

        return -1;  // interval has not yet expired so we didn't check for any inactive DaiMgr work items.
    }   // End ensureDaiMgrsStillActive()


    //--------------------------------------------------------------------------
    // Determine the initial state (e.g., active or missing) of the compute nodes which are "controlled/aggregated" by this DAI Manager and sets these state values in the data store
    // (this is used to handle the situation when UCS is started when there are already 1 or more nodes are already booted and ready to be used).
    // - Note: this is only being done for Compute nodes!  Service nodes are only marked active/available when their DaiMgr comes active.
    //--------------------------------------------------------------------------
    public long determineInitialNodeStates(String sSnLctn) throws InterruptedException, IOException, ProcCallException {
        // Short-circuit if we have already done this processing (also short-circuit if using synthesized data).
        if ((mAlreadyChkdInitialNodeStates) || (mUsingSynthData) || (adapter.client().callProcedure("ComputeNodeCount").getResults()[0].asScalarLong() > 100000L)) {
            return 1;  // we have previously done this processing, so we skipped it this time.
        }

        log_.info("determineInitialNodeStates - updating the initial node state for compute nodes which are controlled/aggregated by %s", sSnLctn);
        //----------------------------------------------------------------------
        // ToDo: This method currently uses ping to determine whether or not the nodes are active and available.
        // This needs to be replaced by a more in-depth ActSys mechanism but until that is available we will simply use ping.
        //    - Initial thought is that it might be best to handle this by using the node's serial console (Todd has a lot of good ideas on how this could be done)!
        //----------------------------------------------------------------------
        class NodePingInfo {
            NodePingInfo(String sLctn, String sIpAddr, Process oProcess)  { mNodeLctn=sLctn; mNodeIpAddr=sIpAddr; mNodeProcess=oProcess; }
            String  mNodeLctn;
            String  mNodeIpAddr;
            Process mNodeProcess;
        };

        //----------------------------------------------------------------------
        // Loop through the list of compute nodes.
        //----------------------------------------------------------------------
        // Set up the correct stored procedure name that should be used for setting the node state values.
        String sTempStoredProcedureForSavingStateInfo       = "ComputeNodeSetState";
        // Set up the correct stored procedure name that should be used for getting the list of child nodes.
        String sTempStoredProcedureForGettingListOfChildren = "ComputeNodeListOfChildren";

        // Get the volt table with the list of nodes that this ServiceNode is the aggregator for.
        ClientResponse response = adapter.client().callProcedure(sTempStoredProcedureForGettingListOfChildren, sSnLctn);
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("determineInitialNodeStates - stored procedure %s FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       sTempStoredProcedureForGettingListOfChildren, IAdapter.statusByteAsString(response.getStatus()),
                       response.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        VoltTable vt = response.getResults()[0];

        //------------------------------------------------------------------
        // Loop through each of the children nodes that were returned in the volt table - issue a ping to see if that node is already active.
        //------------------------------------------------------------------
        ArrayList<NodePingInfo> alNodePingInfo = new ArrayList<NodePingInfo>();
        int iNodeCntr = 1;
        for (int iChildCntr = 0; iChildCntr < vt.getRowCount(); ++iChildCntr)
        {
            vt.advanceRow();
            String sTempChildNodeLctn   = vt.getString("Lctn");
            String sTempChildNodeIpAddr = vt.getString("IpAddr");
            // Check & see if the node is already active.
            String sTempCmd = "/usr/bin/ping -q -c 1 " + sTempChildNodeIpAddr;
            log_.info("determineInitialNodeStates - checking to see if %s (%s) is already active - issuing '%s'", sTempChildNodeLctn, sTempChildNodeIpAddr, sTempCmd);
            Process process = Runtime.getRuntime().exec(sTempCmd);
            // Save away info about this node's ping.
            alNodePingInfo.add( new NodePingInfo(sTempChildNodeLctn, sTempChildNodeIpAddr, process) );
            // Ensure that don't consume too many processes doing these pings.
            if (iNodeCntr % 500 == 0) {
                Thread.sleep(1 * 1000);  // delay a little so we don't run out of processes (i.e., Too many open files)
            }
            ++iNodeCntr;
        }

        //------------------------------------------------------------------
        // Loop through checking the results of the ping for each node.
        //------------------------------------------------------------------
        for(NodePingInfo oNodePingInfo:alNodePingInfo) {
            // Wait for this node's ping to finish.
            int iTempExitCode = oNodePingInfo.mNodeProcess.waitFor();
            if (iTempExitCode == 0) {
                // ping was successful
                String sTempNewState = "A";  // state of A is Active/Available/Usable.
                // Set the node's state to Active state - simply recording state change info, no need to wait for ack that this work has completed
                // NOTE: we PURPOSELY are not using the markNodeActive() method because we need a special flow to handle this "special" situation!
                //       The same is true for the PertinentInfo specified in the creation of this callback, we do NOT want the "NewState=A" specified in the pertinent info,
                //       even though we are setting the node to active because we do NOT want processing done in the callback for the general case of setting a node active.
                //       This is a special case (not the general case), so be careful making any changes in this flow!!
                String sPertinentInfo = "Lctn=" + oNodePingInfo.mNodeLctn;
                adapter.client().callProcedure(adapter.createHouseKeepingCallbackLongRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedureForSavingStateInfo, sPertinentInfo, workQueue.baseWorkItemId()) // asynchronously invoke the procedure
                                              ,sTempStoredProcedureForSavingStateInfo   // stored procedure name
                                              ,oNodePingInfo.mNodeLctn                  // node's location string
                                              ,sTempNewState                            // node's new state, M = Missing
                                              ,System.currentTimeMillis() * 1000L       // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                              ,adapter.adapterType()                    // type of the adapter that is requesting/issuing this stored procedure
                                              ,workQueue.baseWorkItemId());             // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                // NOTE: we PURPOSELY decided to NOT tell the WLM that it can start to use this node here in this situation!!!
                //       The reason is that the WLM may already have taken the node offline for another reason, so if we
                //       change the internal state of the WLM here, it could cause confusion and disruption!
                // Cut a ras event indicating that we reset the initial state to active - since the db was reloaded, there isn't any job information available...
                adapter.logRasEventNoEffectedJob("RasProvFoundNodeAlreadyActive"
                                                ,("AdapterName=" + adapter.adapterName() + ", Lctn=" + oNodePingInfo.mNodeLctn + ", IpAddr=" + oNodePingInfo.mNodeIpAddr + ", Newstate=" + sTempNewState)  // Instance data
                                                ,oNodePingInfo.mNodeLctn                // lctn
                                                ,System.currentTimeMillis() * 1000L     // time this occurred, in micro-seconds since epoch
                                                ,adapter.adapterType()                  // type of the adapter that is requesting/issuing this stored procedure
                                                ,workQueue.baseWorkItemId()             // requesting work item id
                                                );
                log_.info("determineInitialNodeStates - node is already active, called stored procedure %s - Lctn=%s, IpAddr=%s, NewState=%s",
                          sTempStoredProcedureForSavingStateInfo, oNodePingInfo.mNodeLctn, oNodePingInfo.mNodeIpAddr, sTempNewState);
            }
            else {
                // ping did not get a response
                // Set the node's state to Missing state - simply recording state change info, no need to wait for ack that this work has completed
                String sTempNewState = "M";  // state of M is Missing/PoweredOff/Unusable.
                adapter.client().callProcedure(adapter.createHouseKeepingCallbackNoRtrnValue(adapter.adapterType(), adapter.adapterName(), sTempStoredProcedureForSavingStateInfo, oNodePingInfo.mNodeLctn, workQueue.baseWorkItemId()) // asynchronously invoke the procedure
                                              ,sTempStoredProcedureForSavingStateInfo   // stored procedure name
                                              ,oNodePingInfo.mNodeLctn                  // node's location string
                                              ,sTempNewState                            // node's new state, M = Missing
                                              ,System.currentTimeMillis() * 1000L       // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                              ,adapter.adapterType()                    // type of the adapter that is requesting/issuing this stored procedure
                                              ,workQueue.baseWorkItemId());             // work item id for the work item that is being processed/executing, that is requesting/issuing this stored procedure
                log_.info("determineInitialNodeStates - node is not active, called stored procedure %s - Lctn=%s, IpAddr=%s, NewState=%s, ExitCode=%d",
                          sTempStoredProcedureForSavingStateInfo, oNodePingInfo.mNodeLctn, oNodePingInfo.mNodeIpAddr, sTempNewState, iTempExitCode);
            }
        }   // loop through each of the nodes in the result table.

        log_.info("determineInitialNodeStates - updated  the initial node state for compute nodes");
        // Indicate that we have already determined the initial node states of this ServiceNode's child nodes.
        mAlreadyChkdInitialNodeStates = true;
        return 0L;
    }   // End determineInitialNodestates(String sSnLctn)


    //--------------------------------------------------------------------------
    // Clean up any "stale" adapter instances that were inadvertently left marked as active
    // (specifically this involves checking for any adapter instances that are still marked active on this service node).
    //--------------------------------------------------------------------------
    long cleanupStaleAdapterInstancesOnThisServiceNode(String sSnLctn) throws IOException, ProcCallException, AdapterException
    {
        long iNumStaleAdaptersCleanedUp = 0L;
        log_.info("cleanupStaleAdapterInstancesOnThisServiceNode - started...");
        // Get this process's pid.
        long lMyAdapterInstancesPid = IAdapter.getProcessPid();  // get the pid of the process this adapter instance is running in.

        // Get a list of all the adapter instances that are marked as running on the specified service node.
        ClientResponse responseAdaptersToCheck = adapter.client().callProcedure("AdapterInfoUsingSnLctn", sSnLctn);
        if (responseAdaptersToCheck.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("cleanupStaleAdapterInstancesOnThisServiceNode - Stored procedure AdapterInfoUsingSnLctn FAILED - AdapterLctn=%s, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       sSnLctn, IAdapter.statusByteAsString(responseAdaptersToCheck.getStatus()),
                       responseAdaptersToCheck.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(responseAdaptersToCheck.getStatusString());
        }

        // Loop through the list and clean up any adapter instances that are marked as active (except for this specific adapter)!
        VoltTable vtAdaptersToCheck = responseAdaptersToCheck.getResults()[0];
        for (int iAdapterCntr = 0; iAdapterCntr < vtAdaptersToCheck.getRowCount(); ++iAdapterCntr) {
            vtAdaptersToCheck.advanceRow();
            String sTempAdapterType = vtAdaptersToCheck.getString("AdapterType");
            long   lTempAdapterId   = vtAdaptersToCheck.getLong("Id");
            long   lTempAdapterPid  = vtAdaptersToCheck.getLong("Pid");

            // Check and see if this adapter instance is actually my process - if so skip it.
            if (lMyAdapterInstancesPid != lTempAdapterPid) {
                ++iNumStaleAdaptersCleanedUp;  // bump the number of adapter instances we have cleaned up.
                // the adapter instance being checked is NOT my adapter instance - so go ahead and clean it up!
                // Remove the stale adapter instance's entry from the Adapter table
                // (this is needed so that it is possible to identify zombie work items and requeue them).
                adapter.teardownAdapter(sTempAdapterType, lTempAdapterId, adapter.adapterType(), workQueue.workItemId());
                log_.warn("cleanupStaleAdapterInstancesOnThisServiceNode - removed the entry from the Adapter table for the stale adapter instance - StaleAdapterType=%s, StaleAdapterId=%d",
                          sTempAdapterType, lTempAdapterId);
                // Mark the stale adapter's base work item as Finished (and implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
                // (this will be done synchronously).
                long lTempAdapterBaseWorkItemId = adapter.getAdapterInstancesBaseWorkItemId(sTempAdapterType, lTempAdapterId);
                if (lTempAdapterBaseWorkItemId > 0) {
                    adapter.teardownAdaptersBaseWorkItem("cleanupStaleAdapterInstancesOnThisServiceNode - cleaning up base work item for stale adapter instance", sTempAdapterType, lTempAdapterBaseWorkItemId);
                    log_.warn("cleanupStaleAdapterInstancesOnThisServiceNode - cleaned up the base work item for the stale adapter instance - StaleAdapterType=%s, StaleAdapterId=%d, StaleAdapterBaseWorkItemId=%d",
                              sTempAdapterType, lTempAdapterId, lTempAdapterBaseWorkItemId);
                }
                else {
                    // the stale adapter instance's base work item does not exist.
                    log_.info("cleanupStaleAdapterInstancesOnThisServiceNode - did not 'finish' the stale adapters base work item as it had already been removed - StaleAdapterType=%s, StaleAdapterId=%d",
                              sTempAdapterType, lTempAdapterId);
                }
            }   // the adapter instance being checked is NOT my adapter instance - so go ahead and clean it up!
        }   // loop through the list and clean up any adapter instances that are still marked as active.

        log_.info("cleanupStaleAdapterInstancesOnThisServiceNode - ended, cleaned up adapters = %d", iNumStaleAdaptersCleanedUp);
        return iNumStaleAdaptersCleanedUp;
    }   // End cleanupStaleAdapterInstancesOnThisServiceNode(String sSnLctn)


    //--------------------------------------------------------------------------
    // Clean up any adapter instances on this service node which are still marked as "active" but who's pid is no longer active.
    //--------------------------------------------------------------------------
    long cleanupAdapterInstancesWithoutActivePidOnThisServiceNode(String sSnLctn) throws IOException, ProcCallException, AdapterException
    {
        long iNumStaleAdaptersCleanedUp = 0L;
        log_.info("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - started...");
        // Get this process's pid.
        long lMyAdapterInstancesPid = IAdapter.getProcessPid();  // get the pid of the process this adapter instance is running in.

        // Get a list of all the adapter instances that are marked as running on the specified service node.
        ClientResponse responseAdaptersToCheck = adapter.client().callProcedure("AdapterInfoUsingSnLctn", sSnLctn);
        if (responseAdaptersToCheck.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - Stored procedure AdapterInfoUsingSnLctn FAILED - AdapterLctn=%s, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       sSnLctn, IAdapter.statusByteAsString(responseAdaptersToCheck.getStatus()),
                       responseAdaptersToCheck.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(responseAdaptersToCheck.getStatusString());
        }

        // Loop through the list and check any adapter instances that are marked as active (except for this specific adapter)!
        VoltTable vtAdaptersToCheck = responseAdaptersToCheck.getResults()[0];
        for (int iAdapterCntr = 0; iAdapterCntr < vtAdaptersToCheck.getRowCount(); ++iAdapterCntr) {
            vtAdaptersToCheck.advanceRow();
            String sTempAdapterType = vtAdaptersToCheck.getString("AdapterType");
            long   lTempAdapterId   = vtAdaptersToCheck.getLong("Id");
            long   lTempAdapterPid  = vtAdaptersToCheck.getLong("Pid");

            // Check and see if this adapter instance is actually my process - if so skip it.
            if (lMyAdapterInstancesPid != lTempAdapterPid) {
                // the adapter instance being checked is NOT my adapter instance.
                // Check & see if the adapter's pid is still active.
                if (adapter.isPidActive(lTempAdapterPid)) {
                    // this adapter's pid is still active.
                    log_.info("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - adapter instances pid is still active, so it was not removed from the Adapter table - AdapterType=%s, AdapterId=%d",
                              sTempAdapterType, lTempAdapterId);
                }
                else {
                    // this adapter's pid is not active.
                    ++iNumStaleAdaptersCleanedUp;  // bump the number of adapter instances we have cleaned up.
                    // Remove the stale adapter instance's entry from the Adapter table
                    // (this is needed so that it is possible to identify zombie work items and requeue them).
                    adapter.teardownAdapter(sTempAdapterType, lTempAdapterId, adapter.adapterType(), workQueue.baseWorkItemId());
                    log_.warn("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - removed the entry from the Adapter table for the stale adapter instance - StaleAdapterType=%s, StaleAdapterId=%d",
                              sTempAdapterType, lTempAdapterId);
                    // Mark the stale adapter's base work item as Finished (and implicitly also as Done as Base Work Items are NotifyWhenFinished = F)
                    // (this will be done synchronously).
                    long lTempAdapterBaseWorkItemId = adapter.getAdapterInstancesBaseWorkItemId(sTempAdapterType, lTempAdapterId);
                    if (lTempAdapterBaseWorkItemId > 0) {
                        adapter.teardownAdaptersBaseWorkItem("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - cleaning up base work item for stale adapter instance", sTempAdapterType, lTempAdapterBaseWorkItemId);
                        log_.warn("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - cleaned up the base work item for the stale adapter instance - StaleAdapterType=%s, StaleAdapterId=%d, StaleAdapterBaseWorkItemId=%d",
                                  sTempAdapterType, lTempAdapterId, lTempAdapterBaseWorkItemId);
                    }
                    else {
                        // the stale adapter instance's base work item does not exist.
                        log_.info("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - did not 'finish' the stale adapters base work item as it had already been removed - StaleAdapterType=%s, StaleAdapterId=%d",
                                  sTempAdapterType, lTempAdapterId);
                    }
                }
            }   // the adapter instance being checked is NOT my adapter instance.
        }   // loop through the list and check any adapter instances that are marked as active.

        log_.info("cleanupAdapterInstancesWithoutActivePidOnThisServiceNode - ended, cleaned up adapters = %d", iNumStaleAdaptersCleanedUp);
        return iNumStaleAdaptersCleanedUp;
    }   // End cleanupAdapterInstancesWithoutActivePidOnThisServiceNode(String sSnLctn)


    //--------------------------------------------------------------------------
    // This method starts the adapter instances that should be running on this service node.
    //--------------------------------------------------------------------------
    long startupAdapterInstancesForThisSn(String sSnLctn, String sSnHostname, long lTheDaiMgrsWorkItemId) throws IOException, ProcCallException {
        // Short-circuit if we have already done this processing.
        if (mHaveStartedAdapterInstancesForThisSn)
            return 1;  // we have previously done this processing, so we skipped it this time.

        // Get the list of adapter instances that "need" to be started on this service node.
        log_.info("startupAdapterInstancesForThisSn - starting adapter instances");
        ClientResponse response = adapter.client().callProcedure("MachineAdapterInstancesForServiceNode", sSnLctn);
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("startupAdapterInstancesForThisSn - Stored procedure MachineAdapterInstancesForServiceNode FAILED - SnLctn=%s, SnHostname=%s, Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                sSnLctn, sSnHostname, IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter.adapterType(), adapter.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        // Loop through each of the pertinent MachineAdapterInstance entries and start the appropriate number of each type of adapter here on this service node.
        VoltTable vt = response.getResults()[0];
        for (int iMaiCntr = 0; iMaiCntr < vt.getRowCount(); ++iMaiCntr) {
            vt.advanceRow();
            // Grab the pertinent info.
            String sAdapterType    = vt.getString("AdapterType");
            long   lNumOfInstances = vt.getLong("NumInitialInstances");
            String sTempInvocation = vt.getString("Invocation");
            String sTempLogFile    = vt.getString("LogFile");
            // Loop through creating each of the requested instances of this type of adapter on this service node.
            for (long lNumOfInstancesCntr = 1; lNumOfInstancesCntr <= lNumOfInstances; ++lNumOfInstancesCntr) {
                // Start up this requested adapter instance.
                startupAdapterInstanceOnThisSn(sSnLctn, sSnHostname, sAdapterType, sTempInvocation, sTempLogFile, lTheDaiMgrsWorkItemId);
            }   // loop through creating the requested number of instances for this adapter type.
        }   // loop through each of the pertinent MachineAdapterInstance entries.
        log_.info("startupAdapterInstancesForThisSn - started %d adapter instances", mAdapterInstanceInfoList.size());
        // Indicate that we have started the adapter instances for this Sn.
        mHaveStartedAdapterInstancesForThisSn = true;
        return 0;
    }   // End startupAdapterInstancesForThisSn(String sSnLctn, String sSnHostname, long lTheDaiMgrsWorkItemId)


    //--------------------------------------------------------------------------
    // This method starts up the requested adapter instance on this service node.
    //--------------------------------------------------------------------------
    long startupAdapterInstanceOnThisSn(String sSnLctn, String sSnHostname, String sNewAdapterType, String sNewAdapterInvocation, String sNewAdapterLogFile, long lThisDaiMgrsWorkItemId) throws IOException, ProcCallException {
        // Get the next adapter instance number (includes bumping the value in the MachineAdapterInstance table entry).
        long lThisNewAdaptersInstanceNum = adapter.client().callProcedure("MachineAdapterInstanceBumpNextInstanceNumAndReturn", sNewAdapterType, sSnLctn).getResults()[0].asScalarLong();
        // Perform the substitutions in the invocation string and the stdout/stderr log file name.
        String sStartAdapterCmd = keywordSubstitutions(sNewAdapterInvocation, sSnHostname, sSnLctn, lThisNewAdaptersInstanceNum);
        String sLogFileName     = keywordSubstitutions(sNewAdapterLogFile,    sSnHostname, sSnLctn, lThisNewAdaptersInstanceNum);
        // Split the invocation into a string array for usage on the ProcessBuild command.
        String aInvocation[] = sStartAdapterCmd.split(" ");
        //for (int iCntr=0; iCntr < aInvocation.length; ++iCntr)  log_.info("aInvocation[%d] = '%s' ", iCntr, aInvocation[iCntr]);
        ProcessBuilder pb = new ProcessBuilder(aInvocation);
        File fLogFile = new File(sLogFileName);
        pb.redirectErrorStream(true);  // merge standard error with standard output and send to the same destination.
        pb.redirectOutput(ProcessBuilder.Redirect.to(fLogFile)); // set the stdout destination previous contents will be discarded, Redirect.appendTo(log) could be used to append to file.
        log_.info("startupAdapterInstanceOnThisSn - starting adapter instance %s #%d - issuing '%s'", sNewAdapterType, lThisNewAdaptersInstanceNum, pb.command());
        Process process = pb.start();
        // Get this new adapter instance's pid.
        long lNewAdapterInstancesPid = getProcessPid(process);
        // Add this adapter type instance into the array list of adapter instance information.
        AdapterInstanceInfo oAdapterInstanceInfo = new AdapterInstanceInfo(sNewAdapterType, lThisNewAdaptersInstanceNum, sNewAdapterInvocation, sLogFileName, process, sSnLctn, lNewAdapterInstancesPid);
        mAdapterInstanceInfoList.add(oAdapterInstanceInfo);
        // Begin monitoring this started adapter instance (in order to detect and resolve any failures) - this will be done in a separate thread.
        monitorAdapterInstance(oAdapterInstanceInfo, lThisDaiMgrsWorkItemId);
        return 0L;
    }   // End startupAdapterInstanceOnThisSn(String sSnLctn, String sSnHostname, String sNewAdapterType, String sNewAdapterInvocation, String sNewAdapterLogFile, long lThisDaiMgrsWorkItemId)


    private void monitorAdapterInstance(AdapterInstanceInfo oAdapterInstanceInfo, long lThisDaiMgrsWorkItemId) {
        // Spin up a thread to monitor this adapter instance (waiting for it to terminate, these adapter instances are not expected to terminate).
        MonitorAdapterInstance mai = new MonitorAdapterInstance(oAdapterInstanceInfo, lThisDaiMgrsWorkItemId, log_,
                adapter);
        Thread thread = new Thread(mai);
        thread.start();
    }   // End monitorAdapterInstance(AdapterInstanceInfo oAdapterInstanceInfo, long lThisDaiMgrsWorkItemId)


    //--------------------------------------------------------------------------
    // Log that this DaiMgr adapter instance is still alive (when appropriate).
    //--------------------------------------------------------------------------
    private void logDaimgrProofOfLife() throws IOException, InterruptedException, ProcCallException  {
        // Check & see if it is time to "prove that I am still alive"
        long lCurMillisecs = System.currentTimeMillis();
        if ((lCurMillisecs - mTimeLastProvedAlive) >= DaimgrLogProofOfLifeInterval) {
            // Ensure that this DaiMgr instance (MoS or ChildDaiMgr) has not been zombiefied.
            ensureNotZombie();
            // Save restart data indicating "hey this DAI Manager is still alive".
            log_.info("logDaimgrProofOfLife - entered - logging DaiMgr Proof of Life - lCurMillisecs = %d", lCurMillisecs);  // TEMPORARY message while debugging a potential problem.
            SimpleDateFormat sqlDateFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
            Date   dTemp = new Date(lCurMillisecs);
            String sTempSqlTimestamp = sqlDateFormat.format(dTemp);
            String sRestartData = "ProofOfLife (" + MillisecPrefix + Long.toString(lCurMillisecs) + ") (" + TimestampPrefix + sTempSqlTimestamp + ")";
            workQueue.saveWorkItemsRestartData(workQueue.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
            log_.info("logDaimgrProofOfLife - logged ProofOfLife value - AdapterType=%s, workQueue.workItemId()=%d, RestartData=%s ", adapter.adapterType(), workQueue.workItemId(), sRestartData);  // TEMPORARY message while debugging a potential problem.
            mTimeLastProvedAlive = lCurMillisecs;  // reset last time we provided DaiMgr Proof of Life.
        }
        else { log_.info("logDaimgrProofOfLife - entered - it is not time to do anything - lCurMillisecs = %d", lCurMillisecs); }  // TEMPORARY message while debugging a potential problem.
    }   // End logDaimgrProofOfLife()


    //--------------------------------------------------------------------------
    // Ensure that this adapter instance has not become a zombie.
    // - If it has, essentially commit suicide and be reborn.
    //--------------------------------------------------------------------------
    private void ensureNotZombie() throws IOException, ProcCallException {
        boolean bZombie = false;

        //----------------------------------------------------------------------
        // Make sure that "my" adapter instance is still alive (has not become a zombie)
        // - That this adapter instance still exists AND that it has a state of 'A'.
        //----------------------------------------------------------------------
        String sStoredProc = "AdapterInstanceInfoUsingTypeId";
        ClientResponse response = adapter.client().callProcedure(sStoredProc, adapter.adapterType(), adapter.adapterId());
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("ensureNotZombie - Stored procedure %s FAILED - ThisAdapterType=%s, ThisAdapterId=%d, AdapterLctn=%s, AdapterPid=%d, Status=%s, StatusString=%s!",
                       sStoredProc, adapter.adapterType(), adapter.adapterId(), mSnLctn, IAdapter.getProcessPid(),
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new RuntimeException(response.getStatusString());
        }
        VoltTable vt = response.getResults()[0];
        if (vt.getRowCount() == 0) {
            // this adapter no longer "exists" - it is a zombie.
            bZombie = true;
            log_.error("this adapter instance is a zombie, there is no row in the Adapter table for this adapter instance -  ThisAdapterType=%s, ThisAdapterId=%d!",
                       adapter.adapterType(), adapter.adapterId());
        }
        else {
            // this adapter does exist.
            vt.advanceRow();
            if (!vt.getString("State").equals("A")) {
                // this adapter is "no longer active" - it is a zombie.
                bZombie = true;
                log_.error("this adapter instance is a zombie, this adapter has a non-active state (%s) -  ThisAdapterType=%s, ThisAdapterId=%d!",
                           vt.getString("State"), adapter.adapterType(), adapter.adapterId());
            }
        }

        //----------------------------------------------------------------------
        // Make sure that "my" work item is still being worked on by "my" AdapterId AND that the work item is still active (State = 'W').
        //----------------------------------------------------------------------
        sStoredProc = "WorkItemInfoUsingWorkadaptertypeId";
        response = adapter.client().callProcedure(sStoredProc, adapter.adapterType(), workQueue.workItemId());
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.fatal("ensureNotZombie - Stored procedure %s FAILED - WorkItemId=%d, ThisAdapterType=%s, ThisAdapterId=%d, AdapterLctn=%s, AdapterPid=%d, Status=%s, StatusString=%s!",
                       sStoredProc, workQueue.workItemId(), adapter.adapterType(), adapter.adapterId(), mSnLctn, IAdapter.getProcessPid(),
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString());
            throw new RuntimeException(response.getStatusString());
        }
        vt = response.getResults()[0];
        if (vt.getRowCount() == 0) {
            // this work item no longer "exists" - so this adapter instance is a zombie.
            bZombie = true;
            log_.error("this adapter instance is a zombie, there is no row in the WorkItem table for this adapter instances work item (%d) - ThisAdapterType=%s, ThisAdapterId=%d!",
                       workQueue.workItemId(), adapter.adapterType(), adapter.adapterId());
        }
        else {
            // this work item does exist.
            vt.advanceRow();
            if (vt.getLong("WorkingAdapterId") != adapter.adapterId()) {
                // this work item is being worked on by a different adapter instance - so this adapter instance is a zombie.
                bZombie = true;
                log_.error("this adapter instance is a zombie, this work item (%d) is now being worked on by a different adapter id (%d) -  ThisAdapterType=%s, ThisAdapterId=%d!",
                           workQueue.workItemId(), vt.getLong("WorkingAdapterId"), adapter.adapterType(), adapter.adapterId());
            }
            else if (!vt.getString("State").equals("W")) {
                // this work item is still mine but it is not "being worked on" - so this adapter instance is a zombie.
                bZombie = true;
                log_.error("this adapter instance is a zombie, this work item (%d) is not active (%s) -  ThisAdapterType=%s, ThisAdapterId=%d!",
                           workQueue.workItemId(), vt.getString("State"), adapter.adapterType(), adapter.adapterId());
            }
        }

        //----------------------------------------------------------------------
        // Check & see if I am a zombie or not.
        //----------------------------------------------------------------------
        if (bZombie) {
            // Cut RAS event indicating that the clocks are at least currently out of sync.
            String sInstanceData = "SnLctn=" + mSnLctn + ", AdapterType=" + adapter.adapterType() + ", AdapterId=" + adapter.adapterId();
            adapter.logRasEventNoEffectedJob("RasDaimgrDetectedThisIsZombieInstance"
                                            ,sInstanceData                      // instance data
                                            ,mSnLctn                            // Lctn
                                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                            ,adapter.adapterType()              // type of adapter that is requesting this
                                            ,-1                                 // requesting work item
                                            );
            // Commit suicide and be reborn (restarting the entire systemctl service).
            try {
                String[] sTempCmd = new String[] {"systemctl", "restart", systemDServiceName};
                ProcessBuilder builder = new ProcessBuilder(sTempCmd);
                log_.fatal("this adapter instance is a zombie, issuing '%s' to clean up this problem -  ThisAdapterType=%s, ThisAdapterId=%d!",
                           sTempCmd, adapter.adapterType(), adapter.adapterId());
                Process process = builder.start();
                process.wait();
                if(process.exitValue() != 0) {
                    byte[] buffer = new byte[process.getErrorStream().available()];
                    process.getErrorStream().read(buffer);
                    String errorOut = new String(buffer, StandardCharsets.UTF_8);
                    log_.error("Error in ensureNotZombie() when restarting %s - %s", errorOut);
                    sInstanceData = "SnLctn=" + mSnLctn + ", AdapterType=" + adapter.adapterType() + ", AdapterId=" + adapter.adapterId();
                    adapter.logRasEventNoEffectedJob("RasDaimgrErrorDuringZombieRestart"
                                                    ,sInstanceData                      // instance data
                                                    ,mSnLctn                            // Lctn
                                                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                    ,adapter.adapterType()              // type of adapter that is requesting this
                                                    ,-1                                 // requesting work item
                                                    );
                    throw new RuntimeException("Failed to restart '" + systemDServiceName + "' systemctl service - " + errorOut);
                }
            }
            catch (InterruptedException | IOException ex) {
                log_.error("Exception occurred in ensureNotZombie() when restarting " + systemDServiceName + "!");
                log_.error("%s", Adapter.stackTraceToString(ex));
                // Cut RAS event indicating that the clocks are at least currently out of sync.
                sInstanceData = "SnLctn=" + mSnLctn + ", AdapterType=" + adapter.adapterType() + ", AdapterId=" + adapter.adapterId();
                adapter.logRasEventNoEffectedJob("RasDaimgrExceptionDuringZombieRestart"
                                                ,sInstanceData                      // instance data
                                                ,mSnLctn                            // Lctn
                                                ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                ,adapter.adapterType()              // type of adapter that is requesting this
                                                ,-1                                 // requesting work item
                                                );
                throw new RuntimeException("Exception occurred when restarting " + systemDServiceName + " systemctl service - " + ex);
            }
        }
        else  { log_.info("ensureNotZombie - this adapter is not a zombie"); }  // temporary message while debug this new functionality

    }   // End ensureNotZombie()


    //---------------------------------------------------------
    // Handles all the processing that needs to be done by the lead / Mother Superior instance of the DAI Manager.
    // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
    //          It does not start and stop, it starts and stays active.
    //---------------------------------------------------------
    public long handleMotherSuperiorDaiMgr(String sSnLctn, String sSnHostname) throws InterruptedException, IOException, ProcCallException, AdapterException
    {
        log_.info("handleMotherSuperiorDaiMgr - starting");

        // Clean up any "stale" adapter instances on this service node, that were inadvertently left as active
        // (specifically this involves checking for any adapter instances that are marked as still running on this service node and "cleaning them up").
        cleanupStaleAdapterInstancesOnThisServiceNode(mSnLctn);

        // Determine the initial state (e.g., active or missing) of the compute nodes which are "controlled" by this DAI Manager and sets these state values in the data store
        // (this is used to handle the situation when UCS is started after 1 or more nodes are already booted and ready to be used).
        // - Note: this is only being done for Compute nodes!  Service nodes are only marked active/available when their DaiMgr comes active.
        determineInitialNodeStates(sSnLctn);

        // Startup the adapter instances that should be running on this service node.
        startupAdapterInstancesForThisSn(sSnLctn, sSnHostname, workQueue.workItemId());

        // Sleep for a little bit to ensure that the adapter instances have been started up.
        Thread.sleep(2 * 1000);  // 2 secs

        while(adapter.adapterShuttingDown() == false) {
            try {
                // Check for a backlog of work items, work items that are not being executed in a timely manner, due to a lack of adapter instances capable of handling the work.
                //     When such an imbalance is detected, this DAI Mgr will cause additional adapter instance(s) to be started, alleviating the backlog.
                // Note: this operation is only done here within the MoS DaiMgr adapter instance, none of the other adapters should be doing this checking.
                performAdapterInstanceLoadBalancing();

                // Ensure that there aren't any "zombie" work items that are not being worked on.
                // - Zombie work items refer to those work items that were being worked on by an adapter that died, leaving the work item looking like it is being worked on when in reality it is not.
                // Note: this operation is only done here within the DaiMgr adapter, specifically the MoS instance of the DAI Mgr, none of the other adapters should be doing this checking.
                requeueAnyZombieWorkItems();

                // Ensure that all the child DAI Managers are alive and well on the SSNs.
                ensureDaiMgrsStillActive();

                // Ensure that critical child work items are "not stuck".
                ensureChildWrkItemsMakingProgress(sSnLctn);

                // Ensure that serial console messages are successfully flowing through into the DAI for the "pertinent" nodes
                // (via checking for periodic "Node Proof of Life" messages).
                ensureNodeConsoleMsgsFlowingIntoDai();

                // Detect and handle any nodes that are "stuck" shutting down / halting
                // (checks for nodes that have been in halting state for NodeMaxShuttingDownInterval or more minutes and handles any outliers).
//                checkNodesStuckShuttingDown();    //TODO: Revisit after the current milestone

                // Periodically log a "DaiMgr Proof of Life" operation so that the Mother Superior (MoS) DAI Manager's backup instance knows this MoS DAI Mgr instance is still alive.
                logDaimgrProofOfLife();

            }   // End try
            catch (NoConnectionsException nce) {
                log_.error("NoConnectionsException exception occurred during handleMotherSuperiorDaiMgr, RAS event can NOT be logged, pausing for 10 seconds!");
                try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
            }
            catch (Exception e) {
                log_.error("Exception occurred during handleMotherSuperiorDaiMgr - will catch and then continue processing!");
                log_.error("%s", Adapter.stackTraceToString(e));
                try {
                    adapter.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                        ,("Exception=" + e)                  // instance data
                                                        ,null                                // Lctn
                                                        ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                        ,adapter.adapterType()               // type of adapter that is requesting this
                                                        ,workQueue.baseWorkItemId()          // requesting work item
                                                        );
                }
                catch (Exception e2) {}
            }
            finally {
                // Sleep for a little bit.
                Thread.sleep(5 * 1000L);  // 5 secs
            }   // finally
        }   // End while loop
        return -99999;
    }   // End handleMotherSuperiorDaiMgr(String sSnLctn, String sSnHostname)


    //---------------------------------------------------------
    // Handles all the processing that needs to be done by the lead DaiMgr on a Subnet Service Node (SSN).
    // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
    //          It does not start and stop, it starts and stays active.
    //---------------------------------------------------------
    public long handleChildDaiMgr(String sSnLctn, String sSnHostname) throws InterruptedException, IOException, ProcCallException, AdapterException
    {
        log_.info("handleChildDaiMgr - starting");

        // Clean up any "stale" adapter instances on this service node, that were inadvertently left as active
        // (specifically this involves checking for any adapter instances that are marked as still running on this service node and "cleaning them up").
        cleanupStaleAdapterInstancesOnThisServiceNode(mSnLctn);

        // Determine the initial state (e.g., active or missing) of the compute nodes which are "controlled" by this DAI Manager and sets these state values in the data store
        // (this is used to handle the situation when UCS is started after 1 or more nodes are already booted and ready to be used).
        // - Note: this is only being done for Compute nodes!  Service nodes are only marked active/available when their DaiMgr comes active.
        determineInitialNodeStates(sSnLctn);

        // Startup the adapter instances that should be running on this service node.
        startupAdapterInstancesForThisSn(sSnLctn, sSnHostname, workQueue.workItemId());

        while(adapter.adapterShuttingDown() == false) {
            try {
                // Periodically log a "DaiMgr Proof of Life" operation so that the Mother Superior (MoS) DAI Manager knows this Child DAI Mgr instance is still alive.
                // NOTE: it may appear to be nonsensical to have this here as it appears that this Work Item isn't doing anything, but it is, it is monitoring all the adapter instances that it started on this SSN!!
                logDaimgrProofOfLife();
            }   // End try
            catch (NoConnectionsException nce) {
                log_.error("NoConnectionsException exception occurred during handleChildDaiMgr, RAS event can NOT be logged, pausing for 10 seconds!");
                try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
            }
            catch (Exception e) {
                log_.error("Exception occurred during handleChildDaiMgr - will catch and then continue processing!");
                log_.error("%s", Adapter.stackTraceToString(e));
                try {
                    adapter.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                        ,("Exception=" + e)                  // instance data
                                                        ,null                                // Lctn
                                                        ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                        ,adapter.adapterType()               // type of adapter that is requesting this
                                                        ,workQueue.baseWorkItemId()          // requesting work item
                                                        );
                }
                catch (Exception e2) {}
            }
            finally {
                // Sleep for a little bit.
                Thread.sleep(5 * 1000L);  // 5 secs
            }   // finally
        }   // End while loop
        return -99999;
    }   // End handleChildDaiMgr(String sSnLctn, String sSnHostname)


    //--------------------------------------------------------------------------
    // Get the linux pid for the specified java Process object.
    //--------------------------------------------------------------------------
    public static synchronized long getProcessPid(Process oProcess) {
        long lPid = -1;
        try {
            if (oProcess.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field oTempField = oProcess.getClass().getDeclaredField("pid");
                oTempField.setAccessible(true);
                lPid = oTempField.getLong(oProcess);
                oTempField.setAccessible(false);
            }
        }
        catch (Exception e) {
            lPid = -2;
        }
        return lPid;
    }   // End getProcessPid(Process oProcess)

    // Attempt multiple connections over a period of time before giving up with a TimeoutException.
    // This will succeed on the first successful connection.
    private Client connectRetryPhase(String[] servers, long targetTime, long delayBetweenTriesMs)
            throws TimeoutException {
        Client result = null; // Resulting Client instance or TimeoutException...

        // Loop attempting connections...
        result = loopAttemptingToConnect(servers, targetTime, delayBetweenTriesMs, result, quickClient_);
        if(result == null) {
            log_.error("Failed to connect to a single VoltDB server during startup wait period");
            throw new TimeoutException("Failed to connect to any VoltDB server in the allowed time");
        }
        return result; // Return non-null Client result.
    }

    // Refactored the top while loop into its own method.
    private Client loopAttemptingToConnect(String[] servers, long targetTime, long delayBetweenTriesMs, Client result, Client voltClient) {
        while(result == null && Instant.now().toEpochMilli() < targetTime) {
            result = tryConnectionsToServers(servers, result, voltClient);
            if(result == null) {
                // Sleep for a non-negative, non-zero amount of time up to delayBetweenTriesMs.
                try {
                    Thread.sleep(Math.min(delayBetweenTriesMs, Math.max(targetTime - Instant.now().toEpochMilli(), 1)));
                } catch (InterruptedException e) { /* Ignore interruption, and continue next try or exit. */ }
            }
        }
        return result;
    }

    // Refactored the servers connection loop into its own method.
    private Client tryConnectionsToServers(String[] servers, Client result, Client voltClient) {
        for (String server : servers) { // Try all servers...
            try {
                voltClient.createConnection(server, Client.VOLTDB_SERVER_PORT);
                result = voltClient;
                break; // Take the first connection and run...
            } catch (IOException ie) { /* Assume no connection, move on */ }
        }
        return result;
    }

    // First wait for a VoltDB connection (at least one connection) or a TimeoutException.
    // Second wait for the DbStatus table to show DbStatusEnum.POPULATE_DATA_COMPLETED or a TimeoutException.
    // If no TimeoutException is thrown then its OK to proceed.
    private void waitForVoltDB(String servers, long totalTimeoutMs, long connectDelayMs) throws TimeoutException {
        long targetTime = Instant.now().toEpochMilli() + totalTimeoutMs;
        Client client = connectRetryPhase(servers.split(","), targetTime, connectDelayMs);
        try {
            DbStatusApi status = factory.createDbStatusApi(client);
            if (!status.waitForDataPopulated(targetTime))
                throw new TimeoutException("The VoltDB database failed to reach a usable state before the timeout expired");
        } catch(DataStoreException e) {
            log_.exception(e);
            throw new TimeoutException("After connected the DbStatusAPI threw an exception");
        }
    }

    //--------------------------------------------------------------------------
    // This method handles the general processing flow for DaiMgr adapters (regardless of specific implementation).
    //--------------------------------------------------------------------------
    public void mainProcessingFlow(String[] args) throws IOException, TimeoutException {
        try {
            log_.info("starting");

            // Set up signal handlers for this process.
            AdapterShutdownHandler tempShutdownHandler = new AdapterShutdownHandler() {
                @Override
                public void handleShutdown() {
                    // Default is to do nothing and allow adapter to shut down immediately
                }
            };
            adapter.setShutdownHandler(tempShutdownHandler);
            adapter.enableSignalHandlers();

            DbServers = (args.length >= 1) ? args[0] : "localhost";

            // Wait for VoltDB Servers to appear and be in a usable state or throw a TimeoutException.
            waitForVoltDB(DbServers, CONNECTION_TOTAL_TIMEOUT, CONNECTION_LOOP_DELAY);

            // Connect to the VoltDB servers/nodes - args[0] if present is a comma separated list of VoltDb servers (e.g., voltdbserver1,voltdbserver2,10.11.12.13 )
            log_.info("connecting to VoltDB servers - %s", DbServers);
            adapter.connectToDataStore(DbServers);
            adapter.loadRasMetadata();

            // Get the Lctn and Hostname of the Service Node that this adapter is running on - args[1] and args[2]
            mSnLctn = (args.length >= 2) ? args[1] : "UnknownLctn";
            final String SnHostname = (args.length >= 3) ? args[2] : "UnknownHostName";
            UcsClassPath = adapter.client().callProcedure("UCSCONFIGVALUE.select", "UcsClasspath").getResults()[0].fetchRow(0).getString("Value");
            UcsLogfileDirectory = adapter.client().callProcedure("UCSCONFIGVALUE.select", "UcsLogfileDirectory").getResults()[0].fetchRow(0).getString("Value");
            UcsLog4jConfigurationFile = adapter.client().callProcedure("UCSCONFIGVALUE.select", "UcsLog4jConfigurationFile").getResults()[0].fetchRow(0).getString("Value");
            log_.info("this adapter instance is running on lctn=%s, hostname=%s, pid=%d, VoltIpAddrs=%s, UcsClassPath=%s, UcsLogfileDirectory=%s",
                      mSnLctn, SnHostname, adapter.pid(), DbServers, UcsClassPath, UcsLogfileDirectory);

            // Ensure that this system's clock is at least reasonably in sync with the Tier1 data store clock - loop retrying until it is.
            boolean bClocksAreSynced = false;  int iClockSyncRetryCntr = 0;
            while (!bClocksAreSynced) {
                // Get volt's time.
                log_.info("Get volts current time");  // temporary message to help debug time sync issue.
                long lTier1ClkInMillis = adapter.client().callProcedure("@AdHoc", "SELECT SINCE_EPOCH(Millisecond, NOW) FROM Machine WHERE Sernum='1';").getResults()[0].asScalarLong();
                // Get this machine's current time.
                long lCurMillisecs = System.currentTimeMillis();
                log_.info("Got volts current time");  // temporary message to help debug time sync issue.
                // Check and see if the times are in sync.
                long lClkDiffInMillisecs = lCurMillisecs - lTier1ClkInMillis;
                if ((lClkDiffInMillisecs <= MaxNumMillisecDifferenceForSyncedClocks) && (lClkDiffInMillisecs >= -MaxNumMillisecDifferenceForSyncedClocks)) {
                    bClocksAreSynced = true;
                    log_.info("This system's clock seems 'synced' with the Tier1 data store's clock - DifferenceInMillis=%d, SysClkInMillis=%d, Tier1ClkInMillis=%d", lClkDiffInMillisecs, lCurMillisecs, lTier1ClkInMillis);
                }
                else {
                    // this system clock is not synced with the Tier1 data store's clock.
                    if (iClockSyncRetryCntr++ > 300) {
                        String sTempMsg = "Detected that this system's clock is NOT synced with the Tier1 data store's clock, even after retrying - " +
                                          "DifferenceInMillis=" + lClkDiffInMillisecs + ", SysClkInMillis=" + lCurMillisecs + ", Tier1ClkInMillis=" + lTier1ClkInMillis + "!";
                        log_.fatal(sTempMsg);
                        // Cut RAS event indicating that the clocks are at least currently out of sync.
                        String sInstanceData = "SnLctn=" + mSnLctn + ", DifferenceInMillis=" + lClkDiffInMillisecs + ", SystemClkInMillis=" + lCurMillisecs + ", Tier1ClkInMillis=" + lTier1ClkInMillis;
                        adapter.logRasEventNoEffectedJob("RasDaimgrSysClkNotSynced"
                                                        ,sInstanceData                      // instance data
                                                        ,mSnLctn                            // Lctn
                                                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                        ,adapter.adapterType()              // type of adapter that is requesting this
                                                        ,-1                                 // requesting work item
                                                        );
                        throw new AdapterException(sTempMsg);
                    }
                    log_.warn("Detected that this system's clock is NOT synced with the Tier1 data store's clock - DifferenceInMillis=%d, SysClkInMillis=%d, Tier1ClkInMillis=%d - will keep trying!",
                              lClkDiffInMillisecs, lCurMillisecs, lTier1ClkInMillis);
                    Thread.sleep(1 * 1000);  // sleep 1 sec before retrying
                }
            }

            // Check & see if this machine is being driven by synthesized or real data.
            String sUsingSynthData = adapter.client().callProcedure("MachineAreWeUsingSynthesizedData", "1").getResults()[0].fetchRow(0).getString(0);
            if (sUsingSynthData.equals("Y"))
                mUsingSynthData = true;
            else
                mUsingSynthData = false;

            // Set up this adapter in the adapter table, indicating that this adapter has started and is active
            adapter.registerAdapter(mSnLctn);
            // Also change this adapter instance's name to be unique (reflecting it adapter id).
            adapter.adapterName( adapter.adapterName() + adapter.adapterId() );
            log_.info("Setting adapter's name to %s", adapter.adapterName());

            // Create WorkQueue from the Factory (this includes setting up this adapter's base work item).
            DataStoreFactory factory = new DataStoreFactoryImpl(DbServers, log_);
            workQueue = factory.createWorkQueue(adapter);
            adapter.workQueue(workQueue);  // also set the workQueue field in the base adapter.

            // Clean up any adapter instances on this service node which are still marked as "active" but who's pid is no longer active.
            cleanupAdapterInstancesWithoutActivePidOnThisServiceNode(mSnLctn);

            // Ensure that any work items for this type of adapter that still indicate that they are being worked on after a restart, get requeued so they aren't "left hanging".
            // Note: it is essential that this check is performed before creating the base work item for this adapter.
            // - this poster child for this is the work item that is monitoring the component's log file (e.g., WareWulf's log file)
            // - zombie work items refer to those work items that were being worked on by an adapter that died, leaving the work item looking like it is being worked on when in reality it is not.
            requeueAnyZombieWorkItems();

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(adapter.adapterShuttingDown() == false) {
                try {
                    // Handle any work items that have been queued for this type of adapter.
                    boolean bGotWorkItem = workQueue.grabNextAvailWorkItem(mSnLctn);
                    if (bGotWorkItem == true) {
                        // did get a work item
                        String[] aWiParms = workQueue.getClientParameters(Pattern.quote("|"));
                        long rc = -99999;
                        switch(workQueue.workToBeDone()) {
                            case "MotherSuperiorDaiMgr":
                                // Set this service node's state to active (since it is actively running a MoS DaiMgr instance).
                                adapter.markNodeActive(mSnLctn, mUsingSynthData, (System.currentTimeMillis() * 1000L), adapter.adapterType(), workQueue.workItemId());
                                //---------------------------------------------------------
                                // Handles all the processing that needs to be done by the lead / Mother Superior instance of the DAI Manager.
                                // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
                                //          It does not start and stop, it starts and stays active.
                                //---------------------------------------------------------
                                rc = handleMotherSuperiorDaiMgr(mSnLctn, SnHostname);
                                break;

                            case "ChildDaiMgr":
                                // Set this service node's state to active (since it is actively running a child DaiMgr instance).
                                adapter.markNodeActive(mSnLctn, mUsingSynthData, (System.currentTimeMillis() * 1000L), adapter.adapterType(), workQueue.workItemId());
                                //---------------------------------------------------------
                                // Handles all the processing that needs to be done by the lead DaiMgr on a Subnet Service Node (SSN).
                                // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
                                //          It does not start and stop, it starts and stays active.
                                //---------------------------------------------------------
                                rc = handleChildDaiMgr(mSnLctn, SnHostname);
                                break;

                            case "StartAdditionalChildAdapterInstance":
                                //---------------------------------------------------------
                                // Start up an additional child adapter instance of the specified type, etc.
                                // Note: work items of this type are intended to only be queued to a child DaiMgr, not the MoS DaiMgr.
                                //---------------------------------------------------------
                                String sNewAdapterType       = aWiParms[0];
                                String sNewAdapterInvocation = aWiParms[1];
                                String sNewAdapterLogFile    = aWiParms[2];
                                log_.info("processing work item StartAdditionalChildAdapterInstance - SnLctn=%s, SnHostName=%s, NewAdaptertype=%s, NewAdapterInvocation=%s, NewAdapterLogFile=%s",
                                          mSnLctn, SnHostname, sNewAdapterType, sNewAdapterInvocation, sNewAdapterLogFile);
                                startupAdapterInstanceOnThisSn(mSnLctn, SnHostname, sNewAdapterType, sNewAdapterInvocation, sNewAdapterLogFile, workQueue.workItemId());
                                // We have finished all OUR work for this work item - Mark the work item that WE have been working on as finished.
                                workQueue.finishedWorkItem(workQueue.workToBeDone(), workQueue.workItemId(), (workQueue.workToBeDone() + " successful"));
                                break;

                            default:
                                workQueue.handleProcessingWhenUnexpectedWorkItem();
                                break;
                        }   // end of switch - workToBeDone()
                    }   // did get a work item

                    // Sleep for a little bit if this adapter type doesn't have any work.
                    if (!workQueue.wasWorkDone())
                        Thread.sleep( 8 * 1000L );  // 8 secs

                }   // End try
                catch (NoConnectionsException nce) {
                    log_.error("NoConnectionsException exception occurred during main processing loop, RAS event can NOT be logged, pausing for 10 seconds!");
                    try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
                }
                catch (Exception e) {
                    log_.error("Exception occurred during main processing loop - will catch and then continue processing!");
                    log_.error("%s", Adapter.stackTraceToString(e));
                    try {
                        adapter.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                            ,("Exception=" + e)                  // instance data
                                                            ,null                                // Lctn
                                                            ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                            ,adapter.adapterType()               // type of adapter that is requesting this
                                                            ,workQueue.baseWorkItemId()          // requesting work item
                                                            );
                    }
                    catch (Exception e2) {}
                }
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            adapter.handleMainlineAdapterCleanup(adapter.adapterAbnormalShutdown());
            return;
        }   // End try
        catch (Exception e) {
            adapter.handleMainlineAdapterException(e);
        }
    }   // End mainProcessingFlow(String[] args)



    public static void main(String[] args) throws IOException, TimeoutException {
        final String type = "DAI_MGR";
        final Logger logger = LoggerFactory.getInstance(type, AdapterDaiMgr.class.getName(), "console");
        for (String arg : args) logger.info("CP: arg: %s", arg);
        AdapterSingletonFactory.initializeFactory(type, AdapterDaiMgr.class.getName(), logger);
        final IAdapter adapter = AdapterSingletonFactory.getAdapter();
        final AdapterDaiMgr obj = new AdapterDaiMgr(adapter, logger,
                new DataStoreFactoryImpl((args.length >= 1) ? args[0] : "localhost", logger));
        // Start up the main processing flow for DaiMgr adapters.
        obj.mainProcessingFlow(args);
    }   // End main(String[] args)

}   // End class AdapterDaiMgr
