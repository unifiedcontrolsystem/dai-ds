// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

$(document).ready(function(){
    test(get_floor);
    //main();
});

function main()
{
    var sysname = floorLayout.sysname;
    document.getElementById("sys_name_header").innerHTML = " System : "+ sysname;
    $(".zoomer").checkboxradio({icon: false});
    $(".zoomer").on("change", function(event) {
        // ToDo: this is the only global var ref to floorv (I think)
        floorv.applyZoom(this.value).draw();
        $("#tabs").css("width", "calc(99% - "+$(floorv.canvas).parent().outerWidth()+"px)");
        $("#tabs2").css("width", "100%");
    });

    tabsobj = $("#tabs").tabs({
        active: curviewidx,
    });

    tabsobj2 = $("#tabs2").tabs({
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
    systemDescriptions = systemDescriptionsConstructFromLayoutView(floorLayout.views.Full);
    systemInventory = systemInventoryConstructFromLayoutView(floorLayout.views.Full);
    console.log(systemInventory.hwtypes.get("dense-compute-node").length);

    floorv = new FloorView("Full", document.getElementById("floor-canvas"), document.getElementById("floor-layout"), systemInventory);
    TimeIt("floorv.applyLayoutView", function() {
        floorv.applyLayoutView(floorLayout); });
    // NB: updating the zoom radiobox will trigger the first zoom and draw of the floorv canvas
    $("#zoom"+floorv.zoomlevel).attr("checked", "checked").change();

    // Now create the various tabs
    jobset = new JobSet();

    // Take the initial zoom canvas height for the tabs for now
//	$("#tabs").css("width", "calc(95% - "+floorv.canvas.width+"px)");

    jobtable = $("#Jobs").DataTable({
        select: 'single',
        pageLength: 50,
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
                var heading = {'S': 'Running jobs', 'B': 'Booting jobs', 'T': 'Completed jobs'};
                return $('<tr/>').append("<td colspan='11'>" + heading[group] + "</td>");
            },
            endRender: null,
            dataSrc: 7			// State
        }
    });
    jobnonactivetable = $("#JobsNonActiveData").DataTable({
        select: 'single',
        pageLength: 100,
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
        pageLength: 40,
        order: [0, 'desc'],		// timestamp
        jQueryUI: true,
        autoWidth: false,
        columns: [
            {title: "Name"},
            {title: "Users"},
            {title: "Nodes"},
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

    rastable = $("#RAS").DataTable({
        select: 'single',
        pageLength: 50,
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
        ],
    });
    rastable.draw();

    envtable = $("#Env").DataTable({
        select: 'select',
        pageLength: 50,
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

    // Now add RAS from sample data.
    envtable.draw();


    servicetable = $("#ServiceActions").DataTable({
        select: 'single',
        pageLength: 50,
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

    servicenodestable = $("#ServiceNodeData").DataTable({
        select: 'single',
        pageLength: 50,
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
        ],
    });
    servicenodestable.draw()

    computenodestable = $("#ComputeNodeData").DataTable({
        select: 'single',
        pageLength: 50,
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
        ],
    });
    computenodestable.draw()

    // Add highlight  location on touch for *both* the RAS and Service Action tables
    // Add highlight  location on touch for *both* the RAS and Service Action tables
    $("#WLMReservations tbody, #RAS tbody, #ServiceActions tbody, #ComputeNodeData tbody, #ServiceNodeData tbody, #Env tbody").on('mouseenter', 'tr', function() {
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
    $("#WLMReservations tbody, #RAS tbody, #ServiceActions tbody, #ComputeNodeData tbody, #ServiceNodeData tbody, #Env tbody").on('mouseleave', 'tr', function() {
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

    $("#WLMReservations tbody, #RAS tbody, #ServiceActions tbody, #ComputeNodeData tbody, #ServiceNodeData tbody, #Env tbody").on('click', 'td', function() {
        var history = $(this).attr('history');
        if (history) {
            console.log("history=", history);
            changeContext(history);
        }
    });

    initStateKey();

    // Start with fictitious, but reasonable, start/end history ranges.  These will update.
    nodeMaxLastChgTimestamp = new Date();
    starthistory = new Date(nodeMaxLastChgTimestamp.getTime() - 10*60*1000);  // 100min ago for now.  We update accurately below.
    console.log(starthistory);
    // From here on we start talking to the server...

    // Now fetch the actual start of history (starthistory).
    // This is async, but that's ok.  Meanwhile the slider represents the past 10 min and works.
    // The async processing of starthistory will perform fetchDBChangeTimestamps().
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
    // Use parenthw.hwtype to add content (if any)
    layout.definitions[parenthw.hwtype].content.forEach(
        function(contentitem) {
            var location = [parenthw.location, contentitem.name].join("-");
            var hw = new HWitem(location, contentitem.definition, contentitem.definition == "dense-compute-node" ? "M" : "A");
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
        var color = colormap[['dense-compute-node-state', s].join('-')]
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
        console.log("go to now");
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
    if (contexttime) {
        $('#wayback').css("display", "block");	// Show Mister Peabody
        $('body').addClass("wayback-mode");
    } else {
        $('#contextselector').val("Now");
        $('#wayback').css("display", "none");	// Hide Mister Peabody
        $('body').removeClass("wayback-mode");
    }
    updateNodeStatesFromDB(dbNodeStatesResponse);
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