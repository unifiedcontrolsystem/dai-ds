// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

// Fetch starthistory, which we already initialized to now-10min as a guess.
// We only do this once as the start of history cannot change.
// Start of history is really only used to scale the history scrollbar.  Nothing more.
function test(callback) {
    $.ajax({
        url: "query/filedata",
        success: callback
    });
}
function get_floor(data) {
    var resp = (JSON.parse(data)).Result;
    floorLayout = JSON.parse(JSON.parse(resp)[0].manifestcontent);
    main();
}

function fetchHistoryStart(callback)
{
    $.ajax({
        url: '/query/computehistoldestts',
        success : callback
    });
}

function dbHistoryStartResponse(data)
{
    var status= (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        setTimeout(fetchHistoryStart(dbHistoryStartResponse), 6000000);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (resp.length > 0) {
        starthistory = resp.lastchgtimestamp;
        nodeMaxLastChgTimestamp=null;
        fetchDBChangeTimestamps(fetchDBChangeTimestampsResponse);
        fetchDBChangeTimestamps(fetchDBChangeTimestampsResponseNonTrivial);
    }
}

function fetchDBChangeTimestamps(callback)
{

    $.ajax({
        url: '/query/changets',
        success : callback
    });
}

function fetchDBChangeTimestampsResponse(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = (JSON.parse((JSON.parse(data)).Result));
    for (var i=0; i < resp.length; i++) {
        var name = resp[i].key;		// 2 columns, and probably 3 rows
        var val = resp[i].value;
        if (name == "Node_Max_LastChgTimestamp") {
            if (nodeMaxLastChgTimestamp == null || nodeMaxLastChgTimestamp < val) {
                //console.log('in node_max')
                // History end has advanced.  Readjust the history slider.
                nodeMaxLastChgTimestamp = val;
            }
        } else if (name == "Node_Max_DbUpdatedTimestamp") {
            if (nodeMaxDBUpdatedTimestamp == null || nodeMaxDBUpdatedTimestamp < val) {
                //console.log('in node');
                // Database updating has advanced.  Track it.
                nodeMaxDBUpdatedTimestamp = val;
                if (contexttime == null) {
                    updateNodeStatesFromDB(dbNodeStatesResponse, null,
                        nodeMaxDBUpdatedTimestamp);		// Need up-to-date data if showing "Now"
                }
            }
        }
    }
    setTimeout(fetchDBChangeTimestamps(fetchDBChangeTimestampsResponse), 6000000);		// kick it off again in the future.
}

function fetchDBChangeTimestampsResponseNonTrivial(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = (JSON.parse((JSON.parse(data)).Result));
    for (var i=0; i < resp.length; i++) {
        var name = resp[i].key;		// 2 columns, and probably 3 rows
        var val = resp[i].value;
        if (name == "Job_Max_LastChgTimestamp") {
            // Not doing anything with this one just yet.
        } else if (name == "Job_Max_DbUpdatedTimestamp" && !nre) {
            if (jobMaxDBUpdatedTimestamp < val || jobMaxDBUpdatedTimestamp == null) {
                //var timetill = get_start_date(val, 30)
                updateAJobsFromDB(dbAJobsresponse, val);
                updateNJobsFromDB(dbNJobsresponse, val);
                jobMaxDBUpdatedTimestamp = val;
            }
        } else if (name == "Ras_Max_DbUpdatedTimestamp") {
            if (rasMaxTimestamp < val || rasMaxTimestamp == null ) {
                updateRasFromDB(updateRasResult, ((rasMaxTimestamp != null) ?
                    rasMaxTimestamp : get_start_date(val, 30)), val);
                rasMaxTimestamp = val;
            }
        } else if (name == "Reservation_Max_DbUpdatedTimestamp") {
            if (reservationMaxTimestamp != val && !nre) {
                updateReservationFromDB(dbReservationResponse, reservationMaxTimestamp, val);
                reservationMaxTimestamp = val;
            }
        }
        else if (name == "Env_Max_Timestamp") {
            if (envMaxTimestamp != val) {
                updateEnvFromDB(updateEnvResult, ((envMaxTimestamp != null) ?
                    envMaxTimestamp : get_start_date(val, 30)), val);
                envMaxTimestamp = val;
            }
        }
        else if (name == "Inv_Max_Timestamp") {
            if (invMaxTimestamp != val ) {
                updateInventoryInfoFromDB(dbInventoryInfoResponse, invMaxTimestamp, val);
                invMaxTimestamp = val;
            }
        }
        else if (name == "InvSS_Max_Timestamp") {
            if (invSSMaxTimestamp != val && !nre) {
                updateInventorySnapshotFromDB(dbInventorySnapShotResponse, invSSMaxTimestamp, val);

                invSSMaxTimestamp = val;
            }
        }
        else if (name == "Replacement_Max_Timestamp") {
            if (replacementMaxTimestamp != val) {
                updateReplacementHistoryFromDB(dbReplacementHistoryResponse, invMaxTimestamp, null);
                replacementMaxTimestamp = val;
            }
        }
        else if (name == "Diags_Max_Timestamp") {
            if (diagsMaxTimestamp != val && !nre) {
                updateADiagsFromDB(updateADiagResult, diagsMaxTimestamp);
                updateNDiagsFromDB(updateNDiagResult, diagsMaxTimestamp);
                diagsMaxTimestamp = val;
            }
        }
        else if (name == "Service_Operation_Max_Timestamp") {
            if (serviceOperationTimestamp != val && !nre) {
                updateServiceOperationsFromDB(updateServiceOperationResult, serviceOperationTimestamp);
                serviceOperationTimestamp = val;
            }
        }

    }
    setTimeout(fetchDBChangeTimestamps(fetchDBChangeTimestampsResponseNonTrivial), 6000000);		// kick it off again in the future.
}
updateComputeInventoryFromDB(dbComputeInventory, -1);
updateServiceInventoryFromDB(dbServiceInventory, -1);
// Update the current page based on contexttime
// contextime == null means "Now" so we use nodeMaxDBUpdatedTimestamp,
// else we really want to see the context time.
function updateNodeStatesFromDB(callback, startTime, EndTime)
{
    console.log('/query/nodestatehistory?StartTime='+startTime+'&EndTime='+EndTime);

    $.ajax({
        url: '/query/nodestatehistory?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function dbNodeStatesResponse(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = (JSON.parse((JSON.parse(data)).Result))
    if (resp.length > 0)
        updateNodeStates(resp);
}

//
// Iterate through JSON update data.
// The data argument is <update>.results[0].data.
//
function updateNodeStates(data)
{
    if (data.length > 1) {
        for (var i=0; i<data.length; i++) {
            var hwitem = systemInventory.getHwByLocation(data[i].lctn);
            try {hwitem.changeState(data[i].state);}
            catch (err){
                continue;
            }
        }
        systemInventory.notifyObservers(null);
    }
    // Update state key.   Use curdata since it is now current.
    for (var s in States) {
        var val=0;
        for (var i=0; i<data.length; i++) // QQQ: this won't work when data is a delta.
            if (data[i].state == s)
                val++;
        $("#stateKey" + s + "val").html(val);
        $("#stateKey" + s + "bar").width(((val*100)/data.length)+"%");
        $("#stateKey" + s + "bar").css('background-color', val > 0 ? colormap[['dense-compute-node-state', s].join('-')] : "white");
    }
}

// Simple fetch all RAS
function updateServiceInventoryFromDB(callback, seq_num){
    $.ajax({
        url: '/query/serviceinv?SeqNum='+seq_num,
        success : callback
    });
}

function updateComputeInventoryFromDB(callback, seq_num)
{
    $.ajax({
        url: '/query/computeinv?SeqNum='+seq_num,
        success : callback
    });
}


function dbComputeInventory(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!computenodestable.data().any()) {
        computenodestable.rows().remove();}	// removes all rows
    for (var i=0; i < resp.length; i++) {
        var location = resp[i].lctn;
        var hostname = resp[i].hostname;
        var bootimg = resp[i].bootimageid;
        var environment = resp[i].environment;
        var ipadd = resp[i].ipaddr;
        var macadd = resp[i].macaddr;
        var seqnum = resp[i].sequencenumber;
        computenodestable.row.add([location, hostname, bootimg, environment, ipadd, macadd, seqnum]);
    }
    computenodestable.draw(false);
}

function dbServiceInventory(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!servicenodestable.data().any()) {
        servicenodestable.rows().remove();	}// removes all rows

    for (var i=0; i < resp.length; i++) {
        var location = resp[i].lctn;
        var hostname = resp[i].hostname;
        var bootimg = resp[i].bootimageid;
        var ipadd = resp[i].ipaddr;
        var macadd = resp[i].macaddr;
        servicenodestable.row.add([location, hostname, bootimg,ipadd,macadd]);
    }
    servicenodestable.draw(false);
}


// Simple fetch all Jobs.  We kick off a fetch for both active and non-active
function updateAJobsFromDB(callback, EndTime)
{
    console.log('/query/jobsact?EndTime='+EndTime);
    $.ajax({
        url: '/query/jobsact?EndTime='+EndTime,
        success : callback
    });

}

function updateNJobsFromDB(callback, EndTime)
{
    console.log('/query/jobsnonact?EndTime='+EndTime);
    $.ajax({
        url: '/query/jobsnonact?EndTime='+EndTime,
        success : callback
    });

}

function dbAJobsresponse(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    for (var i=0; i < resp.length; i++) {
        var WLMjobid = resp[i].jobid;
        var info = {
            id: WLMjobid,
            name: resp[i].jobname,
            state: resp[i].state,
            BSN: resp[i].bsn,
            user: resp[i].username,
            starttime: resp[i].starttimestamp,
            endtime: null,
            numnodes: resp[i].numnodes,
            nodes: resp[i].nodes,	// jettison this once we know the rankspec is accurate
            wlmstate: "running job",
        };
        updateJob(WLMjobid, info);
    }
    jobtable.draw();	// assume something changed
    systemInventory.notifyObservers(null);
}

function dbNJobsresponse(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    for (var i=0; i < resp.length; i++) {
        var WLMjobid = resp[i].jobid;
        if(resp[i].wlmjobstate == null)
            wlmstate_val = "null";
        else
            wlmstate_val = resp[i].wlmjobstate;
        var info = {
            id: WLMjobid,
            name: resp[i].jobname,
            state: resp[i].state,
            BSN: resp[i].bsn,
            user: resp[i].username,
            starttime: resp[i].starttimestamp,
            endtime: resp[i].endtimestamp,
            exitstatus: resp[i].exitstatus,
            numnodes: resp[i].numnodes,
            nodes: resp[i].nodes,	// jettison this once we know the rankspec is accurate
            jobacctinfo: resp[i].jobacctinfo,
            wlmstate: wlmstate_val
        };
        updateJob(WLMjobid, info);
    }
    jobnonactivetable.draw();	// assume something changed
    systemInventory.notifyObservers(null);

    // Now kick off an active job update.
    updateAJobsFromDB();
}


function updateJob(jobid, info)
{
    var job;
    if (job = jobset.getJobById(jobid)) {
        // job exists already...do some info updates if the job we know isn't in terminated state.
        if (job.info.state != 'T') {
            // Find the right row...
            // Update the job.
            job.info = info;		// wholesale replace the info
            if (info.state == 'T'){
                // completed job
                jobset.updateJob(job);	// handles color marking on transition
                jobtable.row('#'+jobid).remove();	// hackish to remove/add, but this does the job.color update as the row is re-created.
                jobnonactivetable.row.add([jobid, info.name, info.wlmstate, info.user, info.numnodes,
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

            jobnonactivetable.row.add([jobid, info.name, info.wlmstate, info.user, info.numnodes,
                info.starttime, info.endtime, info.state, info.exitstatus, 0, info.BSN]);
        } else {
            // running job
            jobtable.row.add([jobid, info.name, info.wlmstate, info.user, info.numnodes, info.starttime,
                ':', info.state, '-', 0, info.BSN]);
        }
    }
}


function updateRasFromDB(callback, startTime, EndTime){
    console.log('/query/rasevent?StartTime='+startTime+'&EndTime='+EndTime)
    $.ajax({
        url: '/query/rasevent?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function updateRasResult(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!rastable.data().any()) {
        rastable.rows().remove();
    }
    // removes all rows
    for (var i=0; i < resp.length; i++) {
        var eventtype = resp[i].eventtype;
        var timestamp = resp[i].dbupdatedtimestamp;
        var severity = resp[i].severity;
        var location = resp[i].lctn;
        var job = resp[i].jobId;
        var controlaction = resp[i].controloperation;
        var message = resp[i].msg;
        var rasdate = timestamp;
        var instance_data = resp[i].instancedata;
        rastable.row.add([eventtype, rasdate, severity, location, dashifnull(job), controlaction, message, instance_data]);
    }
    rastable.draw(false)
}

function updateEnvFromDB(callback, startTime, EndTime){
    console.log('/query/aggenv?StartTime='+startTime+'&EndTime='+EndTime)
    $.ajax({
        url: '/query/aggenv?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function updateEnvResult(data){
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
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

function updateADiagsFromDB(callback, EndTime){
    console.log('/query/diagsact?EndTime='+EndTime);
    $.ajax({
        url: '/query/diagsact?EndTime='+EndTime,
        success : callback
    });
}

function updateNDiagsFromDB(callback, EndTime){
    console.log('/query/diagsnonact?EndTime='+EndTime);
    $.ajax({
        url: '/query/diagsnonact?EndTime='+EndTime,
        success : callback
    });
}

function updateADiagResult(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!diagstable.data().any()) {
        diagstable.rows().remove();
    }// removes all rows
    for (var i=0; i < resp.length; i++) {
        var diags_id = resp[i].diagid;
        var location = resp[i].lctn;
        var service_id = resp[i].serviceactionid;
        var diag_test = resp[i].diag;
        var state = resp[i].state;
        var start_time = resp[i].starttimestamp;
        var end_time = resp[i].endtimestamp;
        var diag_results = resp[i].results;
        diagstable.row.add([location, dashifnull(diag_test), dashifnull(start_time), dashifnull(end_time),dashifnull(diag_results), dashifnull(service_id)]);
    }
    diagstable.draw(false)
}

function updateNDiagResult(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    for (var i=0; i < resp.length; i++) {
        var diags_id = resp[i].diagid;
        var location = resp[i].lctn;
        var service_id = resp[i].serviceactionid;
        var diag_test = resp[i].diag;
        var state = resp[i].state;
        var start_time = resp[i].starttimestamp;
        var end_time = resp[i].endtimestamp;
        var diag_results = resp[i].results;
        diagstable.row.add([location, dashifnull(diag_test), dashifnull(start_time), dashifnull(end_time), dashifnull(diag_results), dashifnull(service_id)]);
    }
    diagstable.draw(false)
}

function updateServiceOperationsFromDB(callback, EndTime){
    console.log('/query/serviceadapterdata?EndTime='+EndTime);
    $.ajax({
        url: '/query/serviceadapterdata?EndTime='+EndTime,
        success : callback
    });
}

function updateServiceOperationResult(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!serviceOptable.data().any()) {
        serviceOptable.rows().remove();
    }// removes all rows
    for (var i=0; i < resp.length; i++) {
        var serviceop_id = resp[i].serviceoperationid;
        var location = resp[i].lctn;
        var type = resp[i].typeofserviceoperation;
        var user_start = resp[i].userstartedservice;
        var user_stop = resp[i].userstoppedservice;
        var state = resp[i].state;
        var status = resp[i].status;
        var starttimestamp = resp[i].starttimestamp;
        var stoptimestamp = resp[i].stoptimestamp;
        var startremarks = resp[i].startremarks;
        var stopremarks = resp[i].stopremarks;
        var dbupdatedtimestamp = resp[i].dbupdatedtimestamp;
        var logfile = resp[i].logfile;
        var entrynumber = resp[i].entrynumber;
        serviceOptable.row.add([location, dashifnull(type), dashifnull(starttimestamp), dashifnull(stoptimestamp),dashifnull(status)]);
    }
    serviceOptable.draw(false)
}

function updateReservationFromDB(callback, startTime, EndTime)
{
    console.log('/query/reservationlist?StartTime='+startTime+'&EndTime='+EndTime);
    $.ajax({
        url: '/query/reservationlist?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function dbReservationResponse(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);

    // Columns can be determined by the schema in the response.
    // They are (in order)
    // 0 ReservationName
    // 1 Users
    // 2 Nodes
    // 3 StartTimeStamp: as usec (not shown here)
    // 4 EndTimeStamp: as usec (not shown here)
    // 5 DeletedTimeStamp: as usec (not shown here)
    if (!wlmrestable.data().any()) {
        wlmrestable.rows().remove();}	// removes all rows

    for (var i = 0;  i < resp.length; i++) {
        var name  = resp[i].reservationname;
        var users = resp[i].users;
        var location = resp[i].nodes;
        var stime = resp[i].starttimestamp;
        var etime = resp[i].endtimestamp;
        var dtime = resp[i].deletedtimestamp;
        wlmrestable.row.add([name, users, location, stime, etime, dtime]);
    }
    wlmrestable.draw(false);
}

function updateInventorySnapshotFromDB(callback, startTime, EndTime){
    console.log('/query/inventoryss?StartTime='+startTime+'&EndTime='+EndTime)
    $.ajax({
        url: '/query/inventoryss?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function dbInventorySnapShotResponse(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }

    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!inventorysnapshottable.data().any()) {
        inventorysnapshottable.rows().remove();}
    for (var i = 0;  i < resp.length; i++) {
        var inv_data_key = JSON.parse(resp[i].inventoryinfo);
        var counter = 0
        var html_table = "" ;
        var html_table_shown_data = "";
        var total_html_table_data = "";
        if (inv_data_key != null) {
            for (var key in inv_data_key) {
                if (counter < 1) {
                    counter++;
                    html_table_shown_data += "<tr class='inv_inside_table shown-content-in-tabular-form'>" + append_content_to_inner_list(inv_data_key, key) + "</tr>";
                    //html_table_shown_data +=  append_content_to_inner_table(inv_data_key, key, 'shown');
                }
                else {
                    html_table += "<tr class='inv_inside_table hidden-content-in-tabular-form'>" + append_content_to_inner_list(inv_data_key, key) + "</tr>";
                    //html_table += append_content_to_inner_table(inv_data_key, key, 'hidden');
                }

            }
            total_html_table_data = "<table class='inv_inside_table'>" + html_table_shown_data + "<tr class='shown-content-in-tabular-form'><td align='right'>" +
                "<a class='test'  href='#' class='expand-table-show-hidden-rows' onclick='show_table_details(this)'>More</a></td></tr>" + html_table + "</table>";
            //total_html_table_data = html_table_shown_data + html_table;
        }
        inventorysnapshottable.row.add([resp[i].lctn, dashifnull(resp[i].snapshottimestamp), "<div class='inner_table_inventory_ss'>" +total_html_table_data +"</div>"]);
    }
    inventorysnapshottable.draw(false);
}

function updateInventoryInfoFromDB(callback, startTime, EndTime){
    console.log('/query/inventoryinfo?StartTime='+startTime+'&EndTime='+EndTime)
    $.ajax({
        url: '/query/inventoryinfo?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function dbInventoryInfoResponse(data){
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }

    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!inventoryinfotable.data().any()) {
        inventoryinfotable.rows().remove();}
    for (var i = 0;  i < resp.length; i++) {
        var inv_data_key = JSON.parse(resp[i].inventoryinfo);
        var counter = 0
        var html_table = "" ;
        var html_table_shown_data = "";
        var total_html_table_data = "";
        if (inv_data_key != null) {
            for (var key in inv_data_key) {
                if (counter < 1) {
                    counter++;
                    html_table_shown_data += "<tr class='inv_inside_table shown-content-in-tabular-form'>" + append_content_to_inner_list(inv_data_key, key) + "</tr>";
                    //html_table_shown_data +=  append_content_to_inner_table(inv_data_key, key, 'shown');
                }
                else {
                    html_table += "<tr class='inv_inside_table hidden-content-in-tabular-form'>" + append_content_to_inner_list(inv_data_key, key) + "</tr>";
                    //html_table += append_content_to_inner_table(inv_data_key, key, 'hidden');
                }

            }
            total_html_table_data = "<table class='inv_inside_table'>" + html_table_shown_data + "<tr class='shown-content-in-tabular-form'><td align='right'>" +
                "<a class='test'  href='#' class='expand-table-show-hidden-rows' onclick='show_table_details(this)'>More</a></td></tr>" + html_table + "</table>";
            //total_html_table_data = html_table_shown_data + html_table;
        }
        inventoryinfotable.row.add([resp[i].lctn, dashifnull(resp[i].lastchgtimestamp), "<div class='inner_table_inventory_ss'>" +total_html_table_data +"</div>"]);
    }
    inventoryinfotable.draw(false);
}


function append_content_to_inner_list(parent_json_key, key_in)
{
    var html_table_data;
    html_table_data = "<td><ul style='font-weight: normal; list-style: none'><li style='font-weight: bold'>" + key_in

    // Object.keys(parent_json_key[key_in]).forEach(function (key_val) {
    if (typeof parent_json_key[key_in] == 'string')
    {
        html_table_data += ' : <span style="font-weight:normal; font-style:italic">' + parent_json_key[key_in] + '</span></ul>'

    }
    else {

        for (var key_val in parent_json_key[key_in]) {
            if (typeof parent_json_key[key_in][key_val] == "object") {
                Object.keys(parent_json_key[key_in][key_val]).forEach(function (p_key) {
                    html_table_data += '<ul style="font-weight: normal; list-style: none"><li>' + p_key + ':    <span style="font-weight:normal; font-style:italic">' + parent_json_key[key_in][key_val][p_key] + '</span></li></ul>'
                });
            }
            else {
                html_table_data += '<ul style="font-weight: normal; list-style: none"><li>' + key_val + ':    <span style="font-weight:normal; font-style:italic">' + parent_json_key[key_in][key_val] + '</span></li></ul>'
            }
        }
    }
    html_table_data += '</td>';
    return html_table_data;
}

function updateReplacementHistoryFromDB(callback, startTime, EndTime)
{
    console.log('/query/replacementhistory?StartTime='+startTime+'&EndTime='+EndTime)
    $.ajax({
        url: '/query/replacementhistory?StartTime='+startTime+'&EndTime='+EndTime,
        success : callback
    });
}

function dbReplacementHistoryResponse(data)
{
    var status = (JSON.parse(data)).Status;
    if (status == "Failed") {
        console.log("db change response.status=", status, " result= ", (JSON.parse(data)).Result);
        return;
    }
    var resp = JSON.parse((JSON.parse(data)).Result);
    if (!replacementhistorytable.data().any()) {
        replacementhistorytable.rows().remove();}	// removes all rows

    for (var i = 0;  i < resp.length; i++) {
        var location = resp[i].lctn;
        var time = resp[i].dbupdatedtimestamp;
        var newsern = resp[i].newsernum;
        var oldsernum = resp[i].oldsernum;
        var frutype = resp[i].frutype;
        replacementhistorytable.row.add([location, newsern, oldsernum, frutype, time]);
    }
    replacementhistorytable.draw(false);
}



function dashifnull(str)
{
    return (str == null || str == "null" || str == "") ? "-" : str;
}

function show_table_details(clicked_item){
    var value_of_click = clicked_item.innerHTML;
    if (value_of_click == 'More')
        clicked_item.innerHTML = 'Less';
    else
        clicked_item.innerHTML = 'More';
    var tr_to_find_siblings = $(clicked_item).closest('tr');
    tr_to_find_siblings.nextAll('tr').toggleClass('inv_inside_table hidden-content-in-tabular-form inv_inside_table shown-content-in-tabular-form');//'shown-content-in-tabular-form', false)
}
