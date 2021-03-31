// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

$(document).ready(function(){
    test(get_floor);
});

function get_start_date(end_time, days)
{
    var time_slice;

    var time;
    var start_time_new;
    if (end_time == null){
        time = new Date();
        console.log("START TIME", time.getFullYear()+'-'+("0" + (time.getMonth()+1)).slice(-2)+'-'+
                                          ("0" + (time.getDate())).slice(-2) + " 00:00:00.000");
        return time.getFullYear()+'-'+("0" + (time.getMonth()+1)).slice(-2)+'-'+
                                                         ("0" + (time.getDate())).slice(-2) + " 00:00:00.000";
    }
    else {
        time_slice = end_time.split(" ")[1];
        time = new Date(end_time);
        start_time_new = new Date(time.setDate(time.getDate() - days));
    console.log("START TIME", start_time_new.getFullYear()+'-'+("0" + (start_time_new.getMonth()+1)).slice(-2)+'-'+
        ("0" + (start_time_new.getDate())).slice(-2) +" "+time_slice);
    return start_time_new.getFullYear()+'-'+("0" + (start_time_new.getMonth()+1)).slice(-2)+'-'+
                   ("0" + (start_time_new.getDate())).slice(-2) +" "+time_slice;}
}
function timestringFromDate(date)
{
    return date.getUTCFullYear() + '-' + pad(date.getUTCMonth()+1, 2) + '-' + pad(date.getUTCDate(), 2)
        + ' ' + pad(date.getUTCHours(), 2) + ':' + pad(date.getUTCMinutes(), 2) + ':' + pad(date.getUTCSeconds(), 2)
        + '.' + pad(date.getUTCMilliseconds(), 3) + '000';
}
var ras_requested = false;
var env_requested = false;
var hwinfo_requested = false;
var nre = false;
function main()
{
    if (nre){
        $('div#tabs a.non-nre').css('display','none');
    }
    var init_mode = true;
    var sysname = floorLayout.sysname;
    document.getElementById("sys_name_header").innerHTML = " System : "+ sysname;
    $(".zoomer").checkboxradio({icon: false});
    $(".zoomer").on("change", function(event) {
        // ToDo: this is the only global var ref to floorv (I think)
        floorv.applyZoom(this.value).draw();
        $("#tabs").css("width", "calc(99% - "+$(floorv.canvas).parent().outerWidth()+"px)");
        $("#tabs2").css("width", "100%");
        $("#tabs3").css("width", "100%");
        $("#tabs5").css("width", "100%");
    });
    // Collect ID names of tabs for url #tab=<name>
        	$("ul#maintabs li a[href]").each(function(){
        		tabids.push($(this).attr("href").slice(1)) // slice off the leading #.
        	})
        	urloptions = parsehash(location.hash);
        	curviewidxmain = tabids.indexOf(urloptions["tab"])
        	if (curviewidxmain == -1) {		// didn't match a real tab
        		curviewidxmain = 1
        	}
    tabsobj = $("#tabs").tabs({
        active: curviewidxmain,
        activate: function(event, ui) {
        			urloptions["tab"]=tabids[tabsobj.tabs("option", "active")]
        			location.hash = generatehash(urloptions)	// update hash right away for bookmarking
        		}
    });

    tabsobj2 = $("#tabs2").tabs({
        active: curviewidx,
    });
    tabsobj3 = $("#tabs3").tabs({
        active: curviewidx,
    });

    tabsobj4 = $("#tabs4").tabs({
        active: curviewidx,

    });

    tabsobj5 = $("#tabs5").tabs({
        active: curviewidx,
    });

    $("#nowshowingtime").datepicker();

    $("#contextselector").autocomplete({
        delay: 0,
        minLength: 0,
        source: contextList,
    }).val("Now").on('click', function() {
        // QQQ: this loses track of which one we had (was in this.value)
        // Can we select the value we had from the list?
        this.oldvalue = this.value;
        this.value = "";
        $(this).autocomplete("search");
    }).on('autocompletechange change', function() {
        contextChanged(this);
    });
    $("a[href='#ras-view']").on('click', function(){
        ras_requested = true;
    });
    $("a[href='#env-view']").on('click', function(){
        env_requested = true;
    });
    $("a[href='#hardware-view']").on('click', function(){

    });
    systemDescriptions = systemDescriptionsConstructFromLayoutView(floorLayout.views.Full);
    systemInventory = systemInventoryConstructFromLayoutView(floorLayout.views.Full);
    console.log('computes=', systemInventory.hwtypes.get('compute-node').length,
    				'  service=', systemInventory.hwtypes.get('service-node').length);
    floorv = new FloorView("Full", document.getElementById("floor-canvas"), document.getElementById("floor-layout"), systemInventory);
    TimeIt("floorv.applyLayoutView", function() {
        floorv.applyLayoutView(floorLayout); });
    // NB: updating the zoom radiobox will trigger the first zoom and draw of the floorv canvas
    $("#zoom"+floorv.zoomlevel).attr("checked", "checked").change();

    // Now create the various tabs
    jobset = new JobSet();

    // Take the initial zoom canvas height for the tabs for now
//	$("#tabs").css("width", "calc(95% - "+floorv.canvas.width+"px)");

    jobtable = $("#JobsActiveData").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [5, 'desc'],		// end timestamp
        jQueryUI: true,
        autoWidth: false,
        rowId: 0,
        columns: [
            {title: "ID", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var jobid = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("jobid", jobid);		// add jobid to the <tr>
                    var job = jobset.getJobById(jobid);
                    if (job.color && rowData[2] == 'running job') {
                        $(cell).css("background-color", job.color);	// colorize cell for active jobs
                    }
                }},
            {title: "Name"},
            {title: "WLMState",createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var jobid = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("jobid", jobid);		// add jobid to the <tr>
                    var job = jobset.getJobById(jobid);
                    if (job.color && rowData[2] != 'running job') {
                        $(cell).css("color", job.color);	// colorize cell for active jobs},
                        $(cell).css("font-weight", "bold");
                    }
                }},
            {title: "User"},
            {title: "Nodes",	searchable: false},
            {title: "Start"},
            {title: "End"},
            {title: "State",    searchable: false},
            {title: "Exit",     searchable: false},
            {title: "RAS",      searchable: false},
            {title: "BSN"},

        ],
        rowGroup: {
            startRender: function(rows, group) {
                var heading = {'S': 'Running jobs', 'B': 'Booting jobs'};
                return $('<tr/>').append("<td colspan='11'>" + heading[group] + "</td>");
            },
            endRender: null,
            dataSrc: 7			// State
        }
    });

    $("#JobsActiveData").on("page.dt", function(){
        var table = $("#JobsActiveData").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[5];
            var start_time_requested = get_start_date(end_time_requested, 30);
            updateAJobsFromDB(dbAJobsresponse, end_time_requested);
        }
    }).DataTable();


    jobnonactivetable = $("#JobsNonActiveData").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [5, 'desc'],		// end timestamp
        jQueryUI: true,
        autoWidth: false,
        rowId: 0,
        columns: [
            {title: "ID", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var jobid = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("jobid", jobid);		// add jobid to the <tr>
                }},
            {title: "Name"},
            {title: "WLMState",createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var jobid = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("jobid", jobid);		// add jobid to the <tr>
                    var job = jobset.getJobById(jobid);
                    $(cell).css("color", job.color);	// colorize cell for active jobs},
                    $(cell).css("font-weight", "bold");
                }},
            {title: "User"},
            {title: "Nodes",	searchable: false},
            {title: "Start"},
            {title: "End"},
            {title: "State",    searchable: false},
            {title: "Exit",     searchable: false},
            {title: "RAS",      searchable: false},
            {title: "BSN"},

        ]
    });

    $("#JobsNonActiveData").on("page.dt", function(){
        var table = $("#JobsNonActiveData").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[6];
            var start_time_requested = get_start_date(end_time_requested, 5);
            updateNJobsFromDB(dbNJobsresponse, start_time_requested, end_time_requested);
        }
    }).DataTable();

    systemInventory.notifyObservers(0);		// due to jobs being added */

    // Add highlight job on touch for the jobs table
    $("#Jobs tbody").on('mouseenter', 'tr', function() {
        $(this).addClass('selected');
        var jobid = $(this).attr('jobid');
        if (jobid) {
            var job = jobset.getJobById(jobid);
            systemInventory.selectJob(job);
            systemInventory.notifyObservers(job.rackset);
        }
    });
    $("#Jobs tbody").on('mouseleave', 'tr', function() {
        $(this).removeClass('selected');
        var jobid = $(this).attr('jobid');
        if (jobid) {
            var job = jobset.getJobById(jobid);
            systemInventory.unselectJob(job);
            systemInventory.notifyObservers(job.rackset);
        }
    });

    wlmrestable = $("#WLMReservations").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Name"},
            {title: "Users"},
            {title: "Nodes", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[2];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "Start Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[3];  		// i.e., this cell
                    $(cell).attr('history', ['WLMRes', time, 'start', rowData[1]].join(' '));
                }},
            {title: "End Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[4];  		// i.e., this cell
                    $(cell).attr('history', ['WLMRes', time, 'end', rowData[1]].join(' '));
                }},
            {title: "Deleted Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[5];  		// i.e., this cell
                    $(cell).attr('history', ['WLMRes', time, 'del', rowData[1]].join(' '));
                }},
        ],
    });
    wlmrestable.draw();

    $("#WLMReservations").on("page.dt", function(){
        var table = $("#WLMReservations").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[4];
            var start_time_requested = get_start_date(end_time_requested, 10);
            updateReservationFromDB(dbReservationResponse, end_time_requested);
        }
    }).DataTable();


    rastable = $("#RAS").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [1, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Type"},
            {title: "Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[1];  		// i.e., this cell
                    $(cell).attr('history', ['RAS', time, rowData[3]].join(' '));
                }},
            {title: "Severity"},
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[3];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "Job(s)"},
            {title: "Control Action"},
            {title: "Message"},
            {title: "Instance Data"},
        ],
        rowGroup: {
            //endRender: null,
            dataSrc: 2,		// severity
            startRender: function (rows, group) {
                var heading = {'INFO': 'Info Events','ERROR': 'Error Events', 'WARNING': 'Warning Events', 'FATAL': 'Fatal Events', 'WARN':'Warning Events'};
                return $('<tr/>').append("<td colspan='11'>" + heading[group] + "</td>");
            },
            endRender: null,
        }
    });
    rastable.column(0,1,3).data().unique();
    rastable.draw();

    $("#RAS").on("page.dt", function(){
        var table = $("#RAS").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[1];
            var start_time_requested = get_start_date(end_time_requested, 30);
            updateRasFromDB(updateRasResult, start_time_requested , end_time_requested);
        }
    }).DataTable();

    envtable = $("#Env").DataTable({
        select: 'select',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [1, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[1];  		// i.e., this cell
                    $(cell).attr('history', ['ENV', time, rowData[3]].join(' '));
                }},
            {title: "Type"},
            {title: "Maximum", render: $.fn.dataTable.render.number(',','.',3)},
            {title: "Minimum", render: $.fn.dataTable.render.number(',','.',3)},
            {title: "Average", render: $.fn.dataTable.render.number(',','.',3)}
        ],
        columnDefs: [
            {render: $.fn.dataTable.render.number(',', '.', 3)}
        ]
    });
    envtable.column(0,1,2).data().unique();

    // Now add RAS from sample data.
    envtable.draw();

    $("#Env").on("page.dt", function(){
        var table = $("#Env").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[1];
            var start_time_requested = get_start_date(end_time_requested, 30);
            updateEnvFromDB(updateEnvResult, start_time_requested, end_time_requested);
        }
    }).DataTable();


    servicetable = $("#ServiceActions").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "ID"},
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[1];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "State"},
            {title: "Start User"},
            {title: "Start Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[4];  		// i.e., this cell
                    $(cell).attr('history', ['Service', time, 'start', rowData[1]].join(' '));
                }},
            {title: "End User"},
            {title: "End Time", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var time = rowData[6];  		// i.e., this cell
                    $(cell).attr('history', ['Service', time, 'end', rowData[1]].join(' '));
                }},
            {title: "Comments"},
        ],
    });
    // Now add Service Actions from sample data.
    for (var i=0; i<sampleServiceActionData.length; i++) {
// ID[0]   Location[1]   State[2]   Started by User[3]   Started Time[4]  Ended by User[5]   Ended Time[6]   Comments[7]
        servicetable.row.add(sampleServiceActionData[i]);
    }
    servicetable.draw();

    $("#ServiceActions").on("page.dt", function(){
        var table = $("#ServiceActions").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[1];
            var start_time_requested = get_start_date(end_time_requested, 30);
            // updateEnvFromDB(updateEnvResult, start_time_requested, end_time_requested);
        }
    }).DataTable();



    servicenodestable = $("#ServiceNodeData").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "Hostname"},
            {title: "Boot Image"},
            {title: "IP Address"},
            {title: "MAC Address"},
        ]
    });
    servicenodestable.draw();

    $("#ServiceNodeData").on("page.dt", function(){
        var table = $("#ServiceNodeData").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var end_time_requested = (table.row(":last", {order: 'applied'}).data())[1];
            var start_time_requested = get_start_date(end_time_requested, 30);
            //updateServiceInventoryFromDB(dbServiceInventory);
        }
    }).DataTable();


    computenodestable = $("#ComputeNodeData").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[0];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>

                }},
            {title: "Hostname"},
            {title: "Boot Image"},
            {title: "Environment"},
            {title: "IP Address"},
            {title: "MAC Address"},
            {title: "Sequence Number"}
        ],
        columnDefs: [
            {
                "targets": [ 6 ],
                "visible": false,
                "searchable": false
            }]
        });
    computenodestable.draw();

    $("#ComputeNodeData").on("page.dt", function(){
        var table = $("#ComputeNodeData").DataTable();
        var table_info = table.page.info();
        if (table_info.end == table_info.recordsTotal) {
            var last_seq = (table.row(":last", {order: 'applied'}).data())[6];
            //var start_time_requested = get_start_date(end_time_requested, 30);
            //updateComputeInventoryFromDB(dbComputeInventory, last_seq);
        }
    }).DataTable();


    inventoryinfotable = $("#InvInfo").DataTable({
        select: 'single',
        pageLength: 5,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Timestamp"},
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[1];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "Type"},
            {title: "Ordinal"},
            {title: "Fru Id"},
            {title: "Fru Type"},
            {title: "Fru Sub Type"},
        ],
    });
    inventoryinfotable.draw();

    $("#InvInfo").on("page.dt", function(){
         var table = $("#InvInfo").DataTable();
         var table_info = table.page.info();
         if (table_info.end == table_info.recordsTotal) {
             var end_time_requested = (table.row(":last", {order: 'applied'}).data())[1];
             var start_time_requested = get_start_date(end_time_requested, 30);
             updateInventoryInfoFromDB(dbInventoryInfoResponse, start_time_requested, end_time_requested);
         }
     }).DataTable();

    replacementhistorytable = $("#ReplacementHistory").DataTable({
        select: 'single',
        pageLength: 100,
        lengthMenu: [ 100, 200, 300, 400, 500 ],
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Timestamp"},
            {title: "Location", createdCell: function(cell, cellData, rowData, rowIndex, colIndex) {
                    var loc = rowData[1];  		// i.e., this cell
                    $(cell).parent().attr("location", loc);		// add location to the <tr>
                }},
            {title: "Action"},
            {title: "Fru Id"},
        ],
    });
    replacementhistorytable.draw();

    $("#ReplacementHistory").on("page.dt", function(){
             var table = $("#ReplacementHistory").DataTable();
             var table_info = table.page.info();
             if (table_info.end == table_info.recordsTotal) {
                 var end_time_requested = (table.row(":last", {order: 'applied'}).data())[1];
                 var start_time_requested = get_start_date(end_time_requested, 30);
                 updateReplacementHistoryFromDB(dbReplacementHistoryResponse, start_time_requested, end_time_requested);
             }
    }).DataTable();

    // Add highlight  location on touch for *both* the RAS and Service Action tables
    // Add highlight  location on touch for *both* the RAS and Service Action tables

    $("#InvSnap, #ReplacementHistory tbody, #WLMReservations tbody, #RAS tbody, #ServiceActions tbody, #ComputeNodeData tbody, #ServiceNodeData tbody, #Env tbody").on('mouseenter', 'tr', function() {
        $(this).addClass('selected');
        var location = $(this).attr('location');
        if (location) {
            var hwitem = systemInventory.getHwByLocation(location);
            hwitem.changeSelectedContent(true);
            var rackset = new Set();
            rackset.add(hwitem.rack);
            systemInventory.notifyObservers(rackset);
        }
    });
    $("#InvSnap, #ReplacementHistory tbody, #WLMReservations tbody, #RAS tbody, #ServiceActions tbody, #ComputeNodeData tbody, #ServiceNodeData tbody, #Env tbody").on('mouseleave', 'tr', function() {
        $(this).removeClass('selected');
        var location = $(this).attr('location');
        if (location) {
            var hwitem = systemInventory.getHwByLocation(location);
            hwitem.changeSelectedContent(false);
            var rackset = new Set();
            rackset.add(hwitem.rack);
            systemInventory.notifyObservers(rackset);
        }
    });

    $("#InvSnap, #ReplacementHistory, #WLMReservations tbody, #RAS tbody, #ServiceActions tbody, #ComputeNodeData tbody, #ServiceNodeData tbody, #Env tbody").on('click', 'td', function() {
        var history = $(this).attr('history');
        if (history) {
            console.log("history=", history);
            changeContext(history);
        }
    });

    initStateKey('compute-node');
    initStateKey('service-node');

    // Start with fictitious, but reasonable, start/end history ranges.  These will update.
    nodeMaxLastChgTimestamp = new Date();
    starthistory = new Date(nodeMaxLastChgTimestamp.getTime() - 10*60*1000);  // 100min ago for now.  We update accurately below.
    console.log(starthistory);
    // From here on we start talking to the server...

    // Now fetch the actual start of history (starthistory).
    // This is async, but that's ok.  Meanwhile the slider represents the past 10 min and works.
    // The async processing of starthistory will perform fetchDBChangeTimestamps().
    fetchHistoryInit();
}

function fetchHistoryInit() {
fetchHistoryStart(dbHistoryStartResponse);
}

function systemDescriptionsConstructFromLayoutView(layout)
{
    var sysdesc = {};
    for (var t in layout.definitions) {
        sysdesc[t] = layout.definitions[t].description;
    }
    return sysdesc;
}
 /*
  * This function parses the given location hash and returns an options mapping.
  * The hash part of the url looks like:
  *    #option=value&option=value&...
  *
  * All values are assumed to be simple so no special quoting is needed.
  */
 function parsehash(hash) {
    var options={}
	if (hash.length == 0){
        options["tab"] = "nodestate-view"
        }
    else{
	// chop off the # at the start of hash
	hash = hash.substr(1,hash.length-1)
	// split the assignments apart
	var assignments = hash.split("&")
	for (var i in assignments) {
 	 	 var nameval = assignments[i].split("=")
		 options[nameval[0]]=nameval[1]
 	 }
 	 }
	 return options
}
/* This is the inverse of parsehash().  Given options produced by
 * parsehash, create the URL hash (with leading #).
 *
 * Assumes options are simple with no need to quote.
 */
function generatehash(options) {
	var optlist=[]
	for (var opt in options) {
		optlist.push(opt+"="+options[opt])
	}
	return "#"+optlist.join("&")
}

function systemInventoryConstructFromLayoutView(layout)
{
    var system = new HWitem("system", "floor", "A");
    var sysinv = new HardwareSet();
    sysinv.addHw(system);

    // Note: we don't currently create objects for hwtypes.
    // Here the only thing we use with hwtype data is to enumerate
    // hardware elements.  We don't care about floor coordinates, etc.
    layout.floor.content.forEach(
        function(contentitem) {
            var hw = new HWitem(contentitem.name, contentitem.definition, "A");
            this.addContent(hw);
            sysinv.addHw(hw);
            systemInventoryContentFromLayoutView(hw, sysinv, layout);	// add children
        }, system
    );
    return sysinv;
}

function systemInventoryContentFromLayoutView(parenthw, sysinv, layout)
{
	// Use parenthw.definition to add content (if any)
	layout.definitions[parenthw.hwtype].content.sort(function(a,b){return a.name.localeCompare(b.name)});
	layout.definitions[parenthw.hwtype].content.forEach(
		function(contentitem) {
			var location = [parenthw.location, contentitem.name].join("-");
			// A bit of a hack here.   We artificially set hardware to A=Available unless it is a compute or service node
			// Eventually we must gather status of ALL hardware, not just these two node types
			try {
			var hwtype = layout.definitions[contentitem.definition].type;
			}
			catch(error){
			console.log(error);
			}
			var hw = new HWitem(location, contentitem.definition, hwtype,
				(hwtype == 'compute-node' || hwtype == 'service-node') ? "M" : "A");
			this.addContent(hw);
			sysinv.addHw(hw);
			systemInventoryContentFromLayoutView(hw, sysinv, layout);	// add children
		}, parenthw
	);
}

// replace the table in the state key, matching the states we have defined.
function initStateKey()
{
    var tabtxt = "";
    for (var s in States) {
        var sobj = States[s];
        var color = colormap[['compute-node-state', s].join('-')]
        tabtxt += "<tr title='" + sobj.description + "' class='masterTooltip'>"
            + "<td width='20px'>" + sobj.name + "</td>"
            + "<td><table width='300px'><tr>"
            + "<td id='stateKey" + s + "bar' style='width:0%;background-color:" + color +  "'> </td>"
            + "<td id='stateKey" + s + "val' style='width:100%; text-align:right'>0</td></tr></table></td></tr>\n"
    }
    $("#stateKey").html(tabtxt);
    $('.masterTooltip').hover(function(){
        // Hover over code
        var title = $(this).attr('title');
        $(this).data('tipText', title).removeAttr('title');
        $('<p class="tooltip" id="keytip"/p>')
            .text(title)
            .appendTo('body')
            .fadeIn('slow');
    }, function() {
        // Hover out code
        $(this).attr('title', $(this).data('tipText'));
        $('#keytip').remove();
    }).mousemove(function(e) {
        var mousex = e.pageX + 20; //Get X coordinates
        var mousey = e.pageY + 10; //Get Y coordinates
        $('#keytip')
            .css({ top: mousey, left: mousex })
    });
}

function initStateKey(hwtype)
{
	var tabtxt = "";
	for (var s in States) {
		var sobj = States[s];
		var color = colormap[[hwtype, '-state', s].join('-')]
		tabtxt += "<tr title='" + sobj.description + "' class='masterTooltip'>"
				+ "<td width='20px'>" + sobj.name + "</td>"
				+ "<td><table width='300px'><tr>"
				+ "<td id='stateKey" + hwtype + s + "bar' style='width:0%;background-color:" + color +  "'> </td>"
				+ "<td id='stateKey" + hwtype + s + "val' style='width:100%; text-align:right'>0</td></tr></table></td></tr>\n"
	}
	$("#stateKey"+hwtype).html(tabtxt);
	// Note: the master tooltip should probably be established elsewhere.
	// But we do this here anyway, even though it gets repeated for diff node types.
	$('.masterTooltip').hover(function(){
			// Hover over code
			var title = $(this).attr('title');
			$(this).data('tipText', title).removeAttr('title');
			$('<p class="tooltip" id="keytip"/p>')
			.text(title)
			.appendTo('body')
			.fadeIn('slow');
	}, function() {
			// Hover out code
			$(this).attr('title', $(this).data('tipText'));
			$('#keytip').remove();
	}).mousemove(function(e) {
			var mousex = e.pageX + 20; //Get X coordinates
			var mousey = e.pageY + 10; //Get Y coordinates
			$('#keytip')
			.css({ top: mousey, left: mousex })
	});
}


/* The context autocomplete box has changed due to user input
 */
function contextChanged(inputbox)
{
    var context = inputbox.value;
    var oldcontext = inputbox.oldvalue;

    if (context == "") {	// match any whitespace?
        context = inputbox.value = "Now";
    }
    if (context == oldcontext)
        return;	// harmless

    console.log("new: ", context, "  prev: ", oldcontext);
    changeContext(context);

    //	$("#contextselector").css("background-color", "red");	// Do this to show an error with the context?
}

/* The UI wants to change the current context.
 *
 * Do so, tracking history as well as changing the UI to make it clear where(when) we are.
 */
function changeContext(context)
{
    // QQQ extract a date/time.  We cheat here for now by making assumptions
    if (context == "Now") {
        if (contexttime == null)
            return;		// already at "Now"
        contexttime = null;
    } else {
        var w = context.split(" ");
        switch (w[0]) {
            case "RAS":
            case "Service":
                console.log("ras or service context");
                contexttime = new Date(w[1] + " " + w[2] + " UTC");
                break;
            case "Job":
                console.log("job context (not implemented!)");
                return; //QQQ
                break;
            default:
                console.log("assume raw timestamp (not implemented!)");
                return //QQQ
                break;
        }
    }
    addContextHistory(context);
    $('#contextselector').val(context);
    updateComputeNodeStatesFromDB(dbNodeStatesResponse);
    updateServiceNodeStatesFromDB(dbNodeStatesResponse);
    updateAdapterStats();
}

function addContextHistory(context)
{
    if (contextList.indexOf(context) == -1)
        contextList.push(context);	// New value
}

// Pad a number to n digits with leading zeros
function pad(num, digits)
{
    var str = ""+num;	// convert to string
    while (str.length < digits)
        str = "0" + str;
    return str;
}

// return a string time string suitable for voltdb queries.
function timestringFromDate(date)
{
    return date.getUTCFullYear() + '-' + pad(date.getUTCMonth()+1, 2) + '-' + pad(date.getUTCDate(), 2)
        + ' ' + pad(date.getUTCHours(), 2) + ':' + pad(date.getUTCMinutes(), 2) + ':' + pad(date.getUTCSeconds(), 2)
        + '.' + pad(date.getUTCMilliseconds(), 3) + '000';
}
