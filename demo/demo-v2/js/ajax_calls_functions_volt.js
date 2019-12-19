// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

// Fetch starthistory, which we already initialized to now-10min as a guess.
// We only do this once as the start of history cannot change.
// Start of history is really only used to scale the history scrollbar.  Nothing more.
function test(callback_funct) {
    console.log("getting manifest : ", VOLTDBAPIURL, "Procedure=GetManifestContent&Parameters=[]&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
    "Procedure=GetManifestContent&Parameters=[]&jsonp=?",
        callback_funct, "json");
}
function get_floor(response) {
    if (response.status < 0) {
        console.log("get manifest response.status=", response.status);  // -3 when not ready
        return;
    }
    floorLayout = JSON.parse(response.results[0].data[0][0]);
    main();
}


function fetchHistoryStart()
{
    console.log("fetchHistoryStart :" ,VOLTDBAPIURL,
        "Procedure=ComputeNodeHistoryOldestTimestamp&Parameters=[]&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
    "Procedure=ComputeNodeHistoryOldestTimestamp&Parameters=[]&jsonp=?",
    dbHistoryStartResponse, "json");
}

function dbHistoryStartResponse(response)
{
    if (response.status < 0) {
		console.log("history start response.status=", response.status);  // -3 when not ready
        setTimeout(fetchHistoryStart, HISTPOLL*1000);		// kick it off again in the future.
        return;
    }
    console.log("HistoryStartResponse response good");
    if (response.results[0].data.length > 0) {
        var tstamp = response.results[0].data[0][0];
        starthistory = new Date(tstamp/1000);
//		console.log("tstamp=", tstamp, " starthistory=", timestringFromDate(starthistory));
        /*		historyslider.slider("option", "min", Math.floor(starthistory.getTime())); */
        nodeMaxLastChgTimestamp=null;
        fetchDBChangeTimestamps();
        fetchDBChangeTimestamps_postgres(fetchDBChangeTimestamps_postgresresponse)
    }
}
//todo remove this peice when using only VOLT.
function fetchDBChangeTimestamps_postgres(callback)
{
    $.ajax({
        url: '/query/changets',
        success : callback
    });
}
function fetchDBChangeTimestamps_postgresresponse(data) {
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = (JSON.parse((JSON.parse(data)).Result));
    for (var i = 0; i < resp.length; i++) {
        var name = resp[i].key;		// 2 columns, and probably 3 rows
        var val = resp[i].value;
        if (name == "Env_Max_Timestamp") {
            if (val == null){
                updateEnvFromDB(updateEnvResult, ((envMaxTimestamp != null) ?
                    envMaxTimestamp : get_start_date(val, 30)), val);
            }
            if (envMaxTimestamp != val && env_requested) {
                console.log("VAL", val);
                updateEnvFromDB(updateEnvResult, ((envMaxTimestamp != null) ?
                envMaxTimestamp : get_start_date(val, 30)), val);
                envMaxTimestamp = val;
            }
        }
    }
    setTimeout(fetchDBChangeTimestamps_postgres(fetchDBChangeTimestamps_postgresresponse), HISTPOLL*1000);		// kick it off again in the future.

}

function fetchDBChangeTimestamps()
{
    console.log("fetchDBChangeTimestamps :", VOLTDBAPIURL,
        "Procedure=DbChgTimestamps&Parameters=[]&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=DbChgTimestamps&Parameters=[]&jsonp=?",
        fetchDBChangeTimestampsResponse, "json");
}

function fetchDBChangeTimestampsResponse(response)
{
    if (response.status < 0) {
        console.log("db change response.status=", response.status);  // -3 when not ready
        return;
    }
    //console.log("db change response len=", response.results[0].data.length);
    for (var i=0; i < response.results[0].data.length; i++) {
        var name = response.results[0].data[i][0];		// 2 columns, and probably 3 rows
        var val = response.results[0].data[i][1];
        if (name == "Node_Max_DbUpdatedTimestamp") {
            console.log("NODE MAX DB TS", val);
            if (nodeMaxDBUpdatedTimestamp == null || nodeMaxDBUpdatedTimestamp < val) {
                nodeMaxDBUpdatedTimestamp = val
                if (contexttime == null){
                    updateNodeStatesFromDB();		// Need up-to-date data if showing "Now"
            }
        }} else if (name == "Job_Max_LastChgTimestamp") {
            // Not doing anything with this one just yet.
        } else if (name == "Job_Max_DbUpdatedTimestamp") {
            if (jobMaxDBUpdatedTimestamp != val) {
                updateJobsFromDB();
                jobMaxDBUpdatedTimestamp = val;
                if (hwinfo_requested){
                    //updateComputeInventoryFromDB(dbComputeInventory);
                    //updateServiceInventoryFromDB(dbServiceInventory);
                }
            }
            }
         else if (name == "Ras_Max_DbUpdatedTimestamp") {
            if (rasMaxTimestamp != val && ras_requested) {
                updateRasFromDB(updateRasResult, null, val);
                updateEnvFromDB(updateEnvResult, 'null', 'null');
                //updateDiagsFromDB();

                rasMaxTimestamp = val;
            }
        }
        else if (name == "Reservation_Max_DbUpdatedTimestamp") {
            if (reservationMaxTimestamp != val) {
                updateReservationFromDB(dbReservationResponse, reservationMaxTimestamp, val);
                reservationMaxTimestamp = val;
            }
        }
        else if (name == "Inv_Max_Timestamp") {
            if (invMaxTimestamp != val) {
                updateInventorySnapshotFromDB(dbInventorySnapShotResponse, invMaxTimestamp, val);
                invMaxTimestamp = val;
            }
        }
        else if (name == "Diags_Max_Timestamp") {
            if (diagsMaxTimestamp != val) {
                updateADiagsFromDB(updateADiagResult, diagsMaxTimestamp);
                updateNDiagsFromDB(updateNDiagResult, diagsMaxTimestamp);
                diagsMaxTimestamp = val;
            }
        }

            else {
           // console.log("got unknown DbChgTimestamps change ", name, " (ignored)");
        }
    }
    setTimeout(fetchDBChangeTimestamps, HISTPOLL*1000);		// kick it off again in the future.
}

updateComputeInventoryFromDB(dbComputeInventory);
updateServiceInventoryFromDB(dbServiceInventory);
// Update the current page based on contexttime
// contextime == null means "Now" so we use nodeMaxDBUpdatedTimestamp,
// else we really want to see the context time.
function updateNodeStatesFromDB()
{

    console.log("updateNodeStatesFromDB :", VOLTDBAPIURL,
        "Procedure=ComputeNodeHistoryListOfStateAtTime&Parameters=['"
        + nodeMaxDBUpdatedTimestamp + "',null]&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=ComputeNodeHistoryListOfStateAtTime&Parameters=['"
        + nodeMaxDBUpdatedTimestamp + "',null]&jsonp=?",
        dbNodeStatesResponse, "json");
}


function dbNodeStatesResponse(response)
{
    if (response.status < 0) {
        console.log("response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("dbNodeStatesResponse response : ", response);
    if (response.results[0].data.length > 0)
        updateNodeStates(response.results[0].data);
}

//
// Iterate through JSON update data.
// The data argument is <update>.results[0].data.
//
function updateNodeStates(data)
{
    if (data.length > 1) {
        for (var i=0; i<data.length; i++) {
            var hwitem = systemInventory.getHwByHwtypeRank("dense-compute-node", i);
            hwitem.changeState(data[i][0]);
        }
        systemInventory.notifyObservers(null);
    }
    // Update state key.   Use curdata since it is now current.
    for (var s in States) {
        var val=0;
        for (var i=0; i<data.length; i++) // QQQ: this won't work when data is a delta.
            if (data[i][0] == s)
                val++;
        $("#stateKey" + s + "val").html(val);
        $("#stateKey" + s + "bar").width(((val*100)/data.length)+"%");
        $("#stateKey" + s + "bar").css('background-color', val > 0 ? colormap[['dense-compute-node-state', s].join('-')] : "white");
    }
}

// Simple fetch all RAS


function updateServiceInventoryFromDB(callback){

    console.log("updateServiceInventoryFromDB : ", VOLTDBAPIURL,
        "Procedure=ServiceNodeInventoryList&Parameters=[]&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=ServiceNodeInventoryList&Parameters=[]&jsonp=?",
        dbServiceInventory, "json");
}

function updateComputeInventoryFromDB(callback)
{
    console.log("updateComputeInventoryFromDB :", VOLTDBAPIURL,
    "Procedure=ComputeNodeInventoryList&Parameters=[]&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=ComputeNodeInventoryList&Parameters=[]&jsonp=?",
        dbComputeInventory, "json");
}


function dbComputeInventory(response)
{

    if (response.status < 0) {
        console.log("compute inv response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("dbComputeInventory response: ", response);
    if (!computenodestable.data().any()) {
        computenodestable.rows().remove();
    }// removes all rows
    for (var i=0; i < response.results[0].data.length; i++) {
        var location = response.results[0].data[i][0];
        var hostname = response.results[0].data[i][3];
        var bootimg = response.results[0].data[i][5];
        var ipadd = response.results[0].data[i][6];
        var macadd = response.results[0].data[i][7];
        computenodestable.row.add([location, hostname, bootimg,ipadd,macadd]);
    }
    computenodestable.draw();
}

function dbServiceInventory(response)
{
    if (response.status < 0) {
        console.log("compute inv response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("dbServiceInventory response: ", response);
    if (!servicenodestable.data().any()) {
        servicenodestable.rows().remove();
    }// removes all rows
        for (var i=0; i < response.results[0].data.length; i++) {
            var location = response.results[0].data[i][0];
            var hostname = response.results[0].data[i][1];
            var bootimg = response.results[0].data[i][4];
            var ipadd = response.results[0].data[i][5];
            var macadd = response.results[0].data[i][6];
        servicenodestable.row.add([location, hostname, bootimg,ipadd,macadd]);
    }
    servicenodestable.draw(false);
}

function updateJob(jobid, info)
{
    var job;
    if (job = jobset.getJobById(jobid)) {
        // job exists already...do some info updates if the job we know isn't in terminated state.
        if (job.info.state != 'T') {
            console.log("existing active job", jobid, info, job);
            // Find the right row...
            // Update the job.
            job.info = info;		// wholesale replace the info
            if (info.state == 'T'){
                // completed job
                jobset.updateJob(job);	// handles color marking on transition
                jobtable.row('#'+jobid).remove();	// hackish to remove/add, but this does the job.color update as the row is re-created.
                jobtable.row.add([jobid, info.name, info.wlmstate, info.user, info.numnodes,
                    info.starttime, info.endtime, info.state, info.exitstatus, 0, info.BSN]);
            } else {
                // still running job
                jobtable.row('#'+jobid).data([jobid, info.name, info.wlmstate, info.user, info.numnodes,
                    info.starttime, ':', info.state, '-', 0, info.BSN]);
            }
        }
    } else {
        var rankset = systemInventory.locationsToRankSpec(info.nodes);
        job = new Job(jobid, info, rankset);
        jobset.addJob(job);
        if (info.state == 'T') {
            // completed job (seen for first time)
            jobtable.row.add([jobid, info.name, info.wlmstate, info.user, info.numnodes,
                info.starttime, info.endtime, info.state, info.exitstatus, 0, info.BSN]);
        } else {
            // running job
            jobtable.row.add([jobid, info.name, info.wlmstate, info.user, info.numnodes, info.starttime,
                ':', info.state, '-', 0, info.BSN]);
        }
    }
}


function updateRasFromDB(callback, startTime, EndTime)
{

    console.log("updateRasFromDB", VOLTDBAPIURL,
        "Procedure=RasEventListAtTime&Parameters=['"+EndTime+"','"+startTime+"']&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=RasEventListAtTime&Parameters=['"+EndTime+"',"+startTime+"]&jsonp=?",
        callback, "json");
}

function updateRasResult(response)
{

    if (response.status < 0) {
        console.log("ras response.status=", response.status);  // -3 when not ready
        return;
    }
    if (!rastable.data().any()) {
        rastable.rows().remove();
    }
    console.log("dbRASResponse response: ", response);
    for (var i=0; i< response.results[0].data.length; i++) {
        var eventtype = response.results[0].data[i][0];
        var timestamp = response.results[0].data[i][1];
        var severity = response.results[0].data[i][3];
        var location = response.results[0].data[i][4];
        var job = response.results[0].data[i][5];
        var controlaction = response.results[0].data[i][6];
        var message = response.results[0].data[i][7];
        var rasdate = timestringFromDate(new Date(timestamp/1000));
        var instance_data = response.results[0].data[i][8];
        rastable.row.add([eventtype, rasdate, severity, location, dashifnull(job), controlaction, message, instance_data]);
        rastable.draw(false);
    }
    rastable.draw(false);
}
//Todo Remove this commented section once data starts getting displayed from Volt alone even for ENV data.
/*function updateEnvFromDB()
{
    console.log("updateEnvFromDb: ",VOLTDBAPIURL, "Procedure=ComputeNodeListOfAggEnvAtTime&Parameters=['','']&jsonp=?",dbEnvresponse, "json");
    jQuery.post(VOLTDBAPIURL, "Procedure=ComputeNodeListOfAggEnvAtTime&Parameters=['','']&jsonp=?",dbEnvresponse, "json");
}

function dbEnvresponse(response)
{
    if (response.status < 0) {
        console.log("ras response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("dbEnvResponse response: ", response);
    envtable.rows().remove();	// removes all rows
    for (var i=0; i< response.results[0].data.length; i++) {
        var location = response.results[0].data[i][0];
        var timeStamp = timestringFromDate(new Date(response.results[0].data[i][1]/1000));
        var type_sensor = response.results[0].data[i][2];
        var max = response.results[0].data[i][3];
        var min = response.results[0].data[i][4];
        var avg = response.results[0].data[i][5];
        envtable.row.add([location, timeStamp, type_sensor, max, min, avg])
    }
    envtable.draw();
}*/

function updateEnvFromDB(callback, startTime, EndTime){
    console.log("Calling /query/aggenv?StartTime="+startTime+"&EndTime="+EndTime +"\t for ENV data");
    $.ajax({
        url: '/query/aggenv?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function updateEnvResult(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db env response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    console.log("ENV RESPONSE: ", resp);
    if (!envtable.data().any()) {
        envtable.rows().remove();
    }// removes all rows
    for (var i = 0; i < resp.length; i++) {
        var location = resp[i].lctn;
        var timeStamp = resp[i].timestamp;
        var type_sensor = resp[i].type;
        var max = resp[i].maximumvalue;
        var min = resp[i].minimumvalue;
        var avg = resp[i].averagevalue;
        envtable.row.add([location, timeStamp, type_sensor, max, min, avg])
    }
    envtable.draw(false)
}

function updateDiagsFromDB()
{
    console.log("updateDiagsFromDB", VOLTDBAPIURL, "Procedure=DiagListOfActiveDiagsAtTime&Parameters=['']&jsonp=?");
    jQuery.post(VOLTDBAPIURL, "Procedure=DiagListOfNonActiveDiagsAtTime&Parameters=['']&jsonp=?",dbDiagsNonActresponse, "json");
}

function dbDiagsActresponse(response)
{
    if (response.status < 0) {
        console.log("ras response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("dbDiagsActResponse response: ", response);
    if (!diagstable.data().any()) {
        diagstable.rows().remove();
    }// removes all rows
    for (var i=0; i< response.results[0].data.length; i++) {
        var diags_id = response.results[0].data[i][0];
        var location = response.results[0].data[i][1];
        var service_id = response.results[0].data[i][2];
        var diag_test = response.results[0].data[i][3];
        var state = response.results[0].data[i][4];
        var start_time = timestringFromDate(new Date(response.results[0].data[i][5]/1000));
        var end_time = timestringFromDate(new Date(response.results[0].data[i][6]/1000));
        var diag_results = response.results[0].data[i][7];
        diagstable.row.add([location, diag_test, start_time, end_time, diag_results, service_id]);
    }
    diagstable.draw(false);
}

function dbDiagsNonActresponse(response)
{

    if (response.status < 0) {
        console.log("ras response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("dbDiagsNonActResponse response: ", response);
    if (!diagstable.data().any()) {
        diagstable.rows().remove();
    }// removes all rows
    for (var i=0; i< response.results[0].data.length; i++) {
        var diags_id = response.results[0].data[i][0];
        var location = response.results[0].data[i][1];
        var service_id = response.results[0].data[i][2];
        var diag_test = response.results[0].data[i][3];
        var state = response.results[0].data[i][4];
        var start_time = timestringFromDate(new Date(response.results[0].data[i][5]/1000));
        var end_time = timestringFromDate(new Date(response.results[0].data[i][6]/1000));
        var diag_results = response.results[0].data[i][7];
        diagstable.row.add([location, diag_test, start_time, end_time, diag_results, service_id]);
    }
    diagstable.draw(false);
}

function updateJobsFromDB()
{
    console.log("updateJobsFromDB :", VOLTDBAPIURL,
        "Procedure=JobHistoryListOfNonActiveJobsAtTime&Parameters=['']&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=JobHistoryListOfNonActiveJobsAtTime&Parameters=['']&jsonp=?",
        dbNonActiveJobsresponse, "json");
}

function dbJobsresponse(response)
{
    if (response.status < 0) {
        console.log("job response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("job response :", response);
    console.log("job response len=", response.results[0].data.length);
    // Columns can be determined by the schema in the response.
    // JobId[0]  JobName[1]  State[2]  Bsn[3]  UserName[4]  StartTimestamp[5]  NumNodes[6]  Nodes[7]
    for (var i=0; i< response.results[0].data.length; i++) {
        var WLMjobid = response.results[0].data[i][0];
        var info = {
            id: WLMjobid,
            name: response.results[0].data[i][1],
            state: response.results[0].data[i][2],
            BSN: response.results[0].data[i][3],
            user: response.results[0].data[i][4],
            starttime: new Date(response.results[0].data[i][5]/1000),
            endtime: null,
            numnodes: response.results[0].data[i][6],
            nodes: response.results[0].data[i][7],	// jettison this once we know the rankspec is accurate
            wlmstate: "running job",
        };
        updateJob(WLMjobid, info);
    }
    jobtable.draw();	// assume something changed
    systemInventory.notifyObservers(null);
}

function dbNonActiveJobsresponse(response)
{
    if (response.status < 0) {
        console.log("na job response.status=", response.status);  // -3 when not ready
        return;
    }
    console.log("job response :", response);
    console.log("na job response len=", response.results[0].data.length);
    // Columns can be determined by the schema in the response.
    // JobId[0] JobName[1] State[2] Bsn[3] UserName[4] StartTimestamp[5] EndTimeStamp[6] ExitStatus[7] NumNodes[8] Nodes[9] JobAcctInfo[10]

    for (var i=0; i< response.results[0].data.length; i++) {
        var WLMjobid = response.results[0].data[i][0];
        var info = {
            id: WLMjobid,
            name: response.results[0].data[i][1],
            state: response.results[0].data[i][2],
            BSN: response.results[0].data[i][3],
            user: response.results[0].data[i][4],
            starttime: new Date(response.results[0].data[i][5]/1000),
            endtime: new Date(response.results[0].data[i][6]/1000),
            exitstatus: response.results[0].data[i][7],
            numnodes: response.results[0].data[i][8],
            nodes: response.results[0].data[i][9],	// jettison this once we know the rankspec is accurate
            jobacctinfo: response.results[0].data[i][10],
            wlmstate: response.results[0].data[i][11],
        };
        updateJob(WLMjobid, info);
    }
    jobnonactivetable.draw();	// assume something changed
    systemInventory.notifyObservers(null);

    // Now kick off an active job update.
    console.log(VOLTDBAPIURL,
        "Procedure=JobHistoryListOfActiveJobsAtTime&Parameters=['']&jsonp=?");
    jQuery.post(VOLTDBAPIURL,
        "Procedure=JobHistoryListOfActiveJobsAtTime&Parameters=['']&jsonp=?",
        dbJobsresponse, "json");
}


function dashifnull(str)
{
    return (str == null || str == "null") ? "-" : str;
}

