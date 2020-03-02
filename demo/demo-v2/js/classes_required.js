// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

'use strict';
/* Simple Point used for x,y (top/left) corners as well as width,height */
class Point {
    constructor(x,y) {
        this.x = x;
        this.y = y;
    }
    scale(f) {
        return new Point(this.x*f, this.y*f);
    }
    offset(pt) {
        return new Point(this.x+pt.x, this.y+pt.y);
    }
}

/*
 * A HWitem tracks hardware for the UI.  As such, it has a feature
 * for tracking selections for the hardware so that multiple views
 * of the same hardware can be consistent.
 *
 * QQQ: perhaps we need a RackItem also?
 */
class HWitem {
    constructor(location, hwtype, state) {
        this.location = location;
        this.hwtype = hwtype;
        this.selected = false;		// Indicates the UI is trying to show this item as "selected"
        this.parent = null;
        this.state = state;
//		this.changeState(state);	// inits this.state and this.statecolor  QQQ done in addContent
        this.job = null;
        this.content = [];			// "child" hardware items
    }
    changeState(state) {

        if (this.state == 'E') {	// QQQ fix this hackery
            if (state != 'E')
                this.rack.rackerr--;
        } else {
            if (state == 'E')
                this.rack.rackerr++
        }
        if (this.state == 'M') {
            if (state != 'M')
                this.rack.rackmiss--;
        } else {
            if (state == 'M')
                this.rack.rackmiss++
        }
        this.state = state;
        this.statecolor = colormap[[this.hwtype, 'state', this.state].join('-')];  // QQQ needed optimization???

    }
    changeJob(job) {
        this.job = job;
    }
    changeSelected(bool) {
        this.selected = bool;
    }
    changeSelectedContent(bool) {
        this.selected = bool;
        if (bool)
            this.rack.somethingselected++;
        else
            this.rack.somethingselected--;
        this.forEachContent(function(child) {
            child.changeSelectedContent(bool);
        });
    }
    addContent(hwitem) {
        hwitem.parent = this;
        if (this.parent == null) {
            // hwitem must be a rack.  Only the floor has a null parent (and null rack)
            hwitem.rack = hwitem;
            hwitem.rackerr = 0;			// count locations in error within a rack (may need to generalize this)
            hwitem.rackmiss = 0;
            hwitem.somethingselected = 0;	// actually a count of "selected things"
        } else {
            hwitem.rack = this.rack;	// same rack as me.
        }
        var state = hwitem.state;		// save constructed state
        hwitem.state = null;
        hwitem.changeState(state);		// change it "for real" now.
        this.content.push(hwitem);
    }
    forEachContent(func, val) {
        this.content.forEach(func, val);
    }
}

/*
 * HWitemView
 *
 * View object for drawing a hardware item.  This object tracks specifics of drawing the item within
 * a particular Floorview due to the gridpos/sz given in construction.   The highlight details are
 * stored in the HWitem this view points to so that multiple HWitemViews can point to a single piece
 * of hardware and represent that hardware with the exact same state.
 *
 * QQQ: perhaps we need a RackItemView derivative?
 *
 */
class HWitemView {
    constructor(hwitem, name, rgridpos, gridsz, obscured) {
        this.parent   = null;
        this.hwitem   = hwitem;		// The hwitem data we display
        this.obscured = obscured;	// not really visible
        this.floorv   = null;		// gets from parent
        this.canvas	  = null;		// gets from parent
        this.ctx	  = null;		// canvas context
        this.name     = name;		// leaf name in location
        this.gridpos  = null;		// corrected wrt a parent
        this.rgridpos = rgridpos;	// relative pos wrt a parent
        this.gridsz   = gridsz;
        this.drawpos  = null;
        this.drawsz   = null;
        this.content  = [];			// "child" view items
    }
    draw() {						// Draws HWitemView and children
        if (!this.obscured)
            this.drawself();
        this.forEachContent(function(child) {
            child.draw();
        });
    }
    drawself() {						// Redraw only this HWitemView
        var ctx = this.ctx;
        var hwitem = this.hwitem;
        // color priority:  selected first, then job, then state
        var color;
        if (hwitem.selected)
            color = this.floorv.selectcolor;
        else if (hwitem.job && hwitem.job.color)
            color = hwitem.job.color;
        else
            color = hwitem.statecolor;
        ctx.fillStyle = color;
        ctx.fillRect(this.drawpos.x, this.drawpos.y, this.drawsz.x, this.drawsz.y);
        if (this.floorv.labelcomps && this.drawsz.y > 10 && this.drawsz.x > 15) {
            ctx.save();
            ctx.fillStyle = "gray";
            ctx.font = [8, "px sans-serif"].join('');
            ctx.fillText(this.name, this.drawpos.x+2, this.drawpos.y+8);
            ctx.restore();
        }
    }
    applyZoom(f) {
        this.drawpos = this.gridpos.scale(f);
        this.drawsz  = this.gridsz.scale(f);
        if (this.div) {
            // Now we can size/position the div floating above this HWitemView
            $(this.div).css({"width": this.drawsz.x, "height": this.drawsz.y, "left": this.drawpos.x, "top": this.drawpos.y});
        }
        if (!this.obscured) {
            // This adjustment separates boxes
            if (this.drawsz.x > 4) this.drawsz.x--;
            if (this.drawsz.y > 3) this.drawsz.y--;
        }
        this.forEachContent(function(child) {
            child.applyZoom(this);
        }, f);
        return this;
    }
    click(hwitemv) {
        // Open a zoomed-in dialog?
        console.log("click hw ", hwitemv.name, "  (", hwitemv.hwitem.hwtype, ")");
    }
    mouseenter(hwitemv) {
        var hwitem = hwitemv.hwitem;
        $(hwitemv.div).addClass("rack-highlight");	// QQQ: rename to hwelement-highlight?
        var msgwords = ["<b>", hwitem.location, " ", "   ", systemDescriptions[hwitem.hwtype], "</b>"];
        msgwords.push("<br>State: ", States[hwitem.state].name, " (", hwitem.state, ")");
        $('<div class="hoverinfo" id="hwhoverpop"/>').html(msgwords.join("")).appendTo('body').show(0);
    }
    mouseleave(hwitemv) {
        $(hwitemv.div).removeClass("rack-highlight");
        $('#hwhoverpop').remove();
    }
    mousemove(hwitemv, x, y) {
        $('#hwhoverpop').css({ top: y+10, left: x+20 });
    }
    addContent(hwitemv) {
        hwitemv.parent = this;
        hwitemv.gridpos = this.gridpos.offset(hwitemv.rgridpos);
        hwitemv.floorv = this.floorv;
        hwitemv.canvas = this.canvas;	// QQQ an optimization.  Needed?  Could use this.floorv.canvas
        hwitemv.ctx = this.ctx;			// QQQ an optimization.
        this.content.push(hwitemv);
    }
    forEachContent(func, val) {
        this.content.forEach(func, val);
    }
    // This should be protected.  It is called by the FloorView
    // for construction of the floor layout view objects.
    applyLayoutViewContent(locprefix, layout, inventory) {
        layout.definitions[this.hwitem.hwtype].content.forEach(
            function(contentitem) {
                var location = [locprefix, contentitem.name].join("-");
                var hwitem = inventory.getHwByLocation(location);
                var rgridpos = new Point(contentitem.x, contentitem.y);
                var hwtype = layout.definitions[contentitem.definition];
                var gridsz = new Point(hwtype.width, hwtype.height);
                var obscured = hwtype.obscured?true:false;
                var hwitemv = new HWitemView(hwitem, contentitem.name, rgridpos, gridsz, obscured);
                this.addContent(hwitemv);
                if (!obscured && hwitemv.floorv.labelcomps) {
                    // Create a div for this item
                    var hwdiv = document.createElement("div");
                    hwdiv.className = "hwelement";
                    // hwdiv.id = hwitem.location;   Not unique if there are multiple popups.
                    this.floorv.canvas.parentNode.appendChild(hwdiv);
                    hwitemv.div = hwdiv;
                    $(hwdiv).on('click', null, hwitemv, function(event) {
                        event.data.click(event.data);
                    });
                    $(hwdiv).on('mouseenter', null, hwitemv, function(event) {
                        event.data.mouseenter(event.data);
                    });
                    $(hwdiv).on('mouseleave', null, hwitemv, function(event) {
                        event.data.mouseleave(event.data);
                    });
                    $(hwdiv).on('mousemove', null, hwitemv, function(event) {
                        event.data.mousemove(event.data, event.pageX, event.pageY);
                    });
                }
                hwitemv.applyLayoutViewContent(location, layout, inventory);	// add children
            }, this
        );
    }
}

/*
 * FloorView represents a view of the system, or a subset of the system
 *
 * A FloorView is a set of racks.  We really only need the ability to draw() and draw when
 * underlying data has changed.
 *
 */
class FloorView {
    constructor(viewname, canvas, layoutdiv, inventory) {
        //	super(inventory.getHwByLocation("system"), "floorview-"+viewname, new Point(0,0), null, true);
        // Only partially init.   Call applyLayoutView() to finish
        this.viewname = viewname;				// Chooses layout view
        this.canvas = canvas;
        this.layoutdiv = layoutdiv,
            this.ctx = canvas.getContext("2d");
        this.description = null;				// from applyLayoutView()
        this.zoomlevel = 0;						// initial from applyLayoutView()
        this.zoomscales = null;					// from applyLayoutView()
        this.rackscale = 0;						// from applyLayoutView()
        this.rackbackgroundcolor = colormap["rack-background-color"];
        this.rackbordercolor     = colormap["rack-border-color"];
        this.racklabelcolor      = colormap["rack-label-color"];
        this.rackdimlabelcolor	 = colormap["rack-dim-label-color"];
        this.selectcolor 		 = colormap["selected-hw"];
        this.labelcomps	= false;				// Do we assign labels to individual components (not racks)
        this.racks = [];						// List of HWitemViews
        this.inventory = inventory;				// Full system inventory HardwareSet
        inventory.addObserver(this);
    }
    // Draw *all* the racks.
    draw() {
        this.drawracks(this.racks);
    }
    // Draw a specific set of racks
    drawracks(racks) {
        racks.forEach(function(rackv) {
            this.drawBackground(rackv);
            rackv.draw();
            this.drawBorder(rackv);
            this.drawLabel(rackv);
            if ((rackv.hwitem.rackerr + rackv.hwitem.rackmiss) > 0) {
                $(rackv.div).addClass("rack-attention");
            } else {
                $(rackv.div).removeClass("rack-attention");
            }
            if (rackv.hwitem.somethingselected > 0) {
                $(rackv.div).addClass("rack-highlight");
            } else {
                $(rackv.div).removeClass("rack-highlight");
            }
        }, this);
    }
    // Clear the background for a rack.
    drawBackground(rackv) {
        var ctx = this.ctx;
        ctx.fillStyle = this.rackbackgroundcolor;
        ctx.fillRect(rackv.drawpos.x, rackv.drawpos.y, rackv.drawsz.x, rackv.drawsz.y);
    }
    drawBorder(rackv) {
        // Add a border.  Use fillRect to make it crisp.
        var x = rackv.drawpos.x;
        var y = rackv.drawpos.y;
        var w = rackv.drawsz.x;
        var h = rackv.drawsz.y;
        var ctx = this.ctx;
        ctx.fillStyle = this.rackbordercolor;
        ctx.fillRect(x-1, y-1, w+2, 1);		// along top
        ctx.fillRect(x-1, y+h, w+2, 1);		// along bottom
        ctx.fillRect(x-1, y-1,   1, h+2);	// left side
        ctx.fillRect(x+w, y-1,   1, h+2);	// right side
    }
    drawLabel(rackv) {
        // Add a label.  Rotate going by aspect ratio of the rack.
        // Could add an attribute to the rack layout.
        var x = rackv.drawpos.x;
        var y = rackv.drawpos.y;
        var w = rackv.drawsz.x;
        var h = rackv.drawsz.y;
        var ctx = this.ctx;
        ctx.save();
        ctx.fillStyle = this.labelcomps ? this.rackdimlabelcolor : this.racklabelcolor;
        // Decide whether to rotate based on aspect ratio
        // Could add an explicit attribute to the rack layout
        if (h/w < 2.0) {
            var fontpx = ((w/rackv.name.length)/1.25)|0;	// estimate px by assuming square letters
            ctx.font = ["bold ", fontpx, "px sans-serif"].join('');
            var sz = ctx.measureText(rackv.name);
            var fx = x+((w-sz.width) >>> 1);
            var fy = y+fontpx;
            ctx.fillText(rackv.name, fx, fy);
        } else {
            // use rotated text
            var fontpx = (w/1.5)|0;
            ctx.font = ["bold ", fontpx, "px sans-serif"].join('');
            var sz = ctx.measureText(rackv.name);
            var fy = y+((h-sz.width) >>> 1)+sz.width;
            var fx = x+fontpx;
            ctx.translate(fx,fy);
            ctx.rotate(-Math.PI/2);
            ctx.fillText(rackv.name, 0, 0);
        }
        ctx.restore();
    }
    observedChange(obs, val) {
        // assume obs is this.inventory
        if (val) {
            // optimize...val is a Set of rack hwitems
            var drawracks = [];
            this.racks.forEach(function(rackv) {
                if (val.has(rackv.hwitem))
                    drawracks.push(rackv);
            });
            this.drawracks(drawracks);
        } else {
            this.draw();
        }
    }
    click(rackv) {
        // Open the rack as a dialog
        // The two upper divs are needed due to jqueryui's dialog implementation.
        // The dialog usurps some control over the object that is converted into the dialog.
        // This is the rackdialogdiv object.  We put another div under it to ensure it has
        // relative positioning (for the child absolute-positioned divs), and to avoid any
        // padding and/or margins that the dialog may define.
        var rackdialogdiv = document.createElement("div");
        var rackpositiondiv = document.createElement("div");
        rackpositiondiv.className = "rack-dialog";
        rackdialogdiv.appendChild(rackpositiondiv);
        var rackcanvas = document.createElement("canvas");
        rackpositiondiv.appendChild(rackcanvas);
        $(rackdialogdiv).attr("title", "Rack " + rackv.name + "  (" + systemDescriptions[rackv.hwitem.hwtype] + ")").change();
        var rackviewobj = $(rackdialogdiv).dialog({ autoOpen: false, position: {my: "left top", at: "right bottom", of: rackv.div}});
        var rackview = new FloorView(this.viewname, rackcanvas, rackviewobj, systemInventory);
        rackview.applyRackView(floorLayout, rackv.name, rackv.hwitem.hwtype);
        rackview.applyRackZoom().draw();
        $(rackviewobj).dialog("option", "width", rackview.drawsz.x+26);	// QQQ need extra for dialog width overhead
        $(rackviewobj).dialog("open");		// it has an X for the user to close
    }
    mouseenter(rackv) {
        $(rackv.div).addClass("rack-highlight");
        var msgwords = ["<b>Rack ", rackv.name, "   ", systemDescriptions[rackv.hwitem.hwtype], "</b>"];
        if (rackv.hwitem.rackerr > 0)
            msgwords.push("<br>", rackv.hwitem.rackerr, " locations in error");
        if (rackv.hwitem.rackmiss > 0)
            msgwords.push("<br>", rackv.hwitem.rackmiss, " locations marked missing");
        $('<div class="hoverinfo" id="hoverpop"/>').html(msgwords.join("")).appendTo('body').show(0);
    }
    mouseleave(rackv) {
        $(rackv.div).removeClass("rack-highlight");
        $('#hoverpop').remove();
    }
    mousemove(rackv, x, y) {
        $('#hoverpop').css({ top: y+10, left: x+20 });
    }
    applyZoom(zoomlevel) {
        this.zoomlevel = zoomlevel;
        return this.applyScaledZoom(this.zoomscales[zoomlevel]);
    }
    applyRackZoom() {
        return this.applyScaledZoom(this.rackscale);
    }
    applyScaledZoom(zoomscale) {
        this.drawsz  = this.gridsz.scale(zoomscale);
        this.canvas.width = this.drawsz.x;
        this.canvas.height = this.drawsz.y;
        $(this.layoutdiv).css("width", this.drawsz.x);  // This div is what really expands/shrinks the allotted space
        this.racks.forEach(function(rackv) {
            rackv.applyZoom(zoomscale);
        });
        return this;
    }
    applyLayoutView(floorlayout) {
        var layout = floorlayout.views[this.viewname];
        this.gridsz = new Point(layout.floor.width, layout.floor.height);
        this.description = layout.floor.description;
        this.zoomlevel = layout.initzoom;
        this.zoomscales = layout.zoomscales;
        this.rackscale = layout.rackscale;
        layout.floor.content.forEach(
            function(contentitem) {
                var hwitem = this.inventory.getHwByLocation(contentitem.name);
                var rgridpos = new Point(contentitem.x, contentitem.y);
                var hwtype = layout.definitions[contentitem.definition];
                var gridsz = new Point(hwtype.width, hwtype.height);
                var obscured = hwtype.obscured?true:false;
                var hwitemv = new HWitemView(hwitem, contentitem.name, rgridpos, gridsz, obscured);
                hwitemv.gridpos = rgridpos;   // QQQ: seems hackish.  Do we need a RackHwItemView?
                hwitemv.floorv = this;
                hwitemv.canvas = this.canvas;
                hwitemv.ctx = this.ctx;
                this.racks.push(hwitemv);
                hwitemv.applyLayoutViewContent(contentitem.name, layout, this.inventory);
                // Now add an absolute positioned div over the rack.  The id is the rack name
                var rackdiv = document.createElement("div");
                rackdiv.className = "rack";
                rackdiv.id = contentitem.name;
                this.canvas.parentNode.appendChild(rackdiv);
                hwitemv.div = rackdiv;
                $(rackdiv).on('click', null, hwitemv, function(event) {
                    event.data.floorv.click(event.data); // call floorv.click(hwitemv)...
                });
                $(rackdiv).on('mouseenter', null, hwitemv, function(event) {
                    event.data.floorv.mouseenter(event.data);
                });
                $(rackdiv).on('mouseleave', null, hwitemv, function(event) {
                    event.data.floorv.mouseleave(event.data);
                });
                $(rackdiv).on('mousemove', null, hwitemv, function(event) {
                    event.data.floorv.mousemove(event.data, event.pageX, event.pageY);
                });
            }, this
        );
    }
    // QQQ: perhaps we need a RackFloorView derivitive of FloorView?
    applyRackView(floorlayout, rackname, racktype) {
        this.labelcomps = true;		// This is turning into "I am a RackFloorView" flag
        var layout = floorlayout.views[this.viewname];
        var hwitem = this.inventory.getHwByLocation(rackname);
        var rgridpos = new Point(0,0);
        var hwtype = layout.definitions[racktype];
        var gridsz = new Point(hwtype.width, hwtype.height);
        var obscured = hwtype.obscured?true:false;
        var hwitemv = new HWitemView(hwitem, rackname, rgridpos, gridsz, obscured);
        hwitemv.gridpos = rgridpos;   // QQQ: seems hackish.  Do we need a RackItemView?
        hwitemv.floorv = this;
        hwitemv.canvas = this.canvas;
        hwitemv.ctx = this.ctx;
        this.racks.push(hwitemv);		// Only 1 rack in this special layout
        hwitemv.applyLayoutViewContent(rackname, layout, this.inventory);
        this.gridsz = gridsz;
        this.description = "Rack " + rackname;
        this.zoomlevel = layout.initzoom;
        this.zoomscales = layout.zoomscales;
        this.rackscale = layout.rackscale;
    }
}

/* HardwareSet
 *
 * A HardwareSet is a container for HWitems.  HWitems are tracked by both
 * location string as well as a per-hardware-type rank ID that is assigned
 * on insertion order.  The system inventory is tracked as a HardwareSet.
 *
 * A HardwareSet can be observed.  Notification of updates is performed
 * manually.  That is, the HardwareSet is manipulated and the caller will
 * eventually call notifyObservers().  Perhaps this can be automated.
 */
class HardwareSet {
    constructor() {
        this.locations = new Map();	// HWitem indexed by location
        this.hwtypes = new Map();	// HWitem ranked array indexed by hwtype
        this.observers = [];

    }
    getHwByLocation(location) {
        return this.locations.get(location);
    }
    getHwByHwtypeRank(hwtype, rank) {
        var hwt = this.hwtypes.get(hwtype);
        return hwt?hwt[rank]:null;
    }
    addHw(hwitem) {
        this.locations.set(hwitem.location, hwitem);
        var hwt = this.hwtypes.get(hwitem.hwtype);
        if (!hwt) {
            // New hwtype.  Add it.
            hwt = [];
            this.hwtypes.set(hwitem.hwtype, hwt);
        }
        hwitem.hwrank = hwt.length;		// cur length is the rank it will get
        hwt.push(hwitem);
    }
    addObserver(obs) {			// obs must have an observedChange() method
        this.observers.push(obs);
    }
    notifyObservers(val) {
        this.observers.forEach(function(obs) {
            obs.observedChange(this, val);
        }, this);
    }

    /* iterateRankSpec
     *
     * Iterates over rankspec calling func for all ranks (0..n-1)
     * with a single char value taken from rankspec.  It is assumed '-'
     * means 'skip' so these ranks will be skipped.
     *   func(rank, charval, arg)
     *   rankspec is "nnnLnnnL..."
     *
     * returns the number of ranks with values (skipping '-')
     */
    static iterateRankSpec(func, rankspec, arg)
    {
        if (rankspec == null || rankspec == 0)
        {
            var one = Math.floor(Math.random()*2);
            var two = Math.floor(Math.random()*2);
            rankspec = one.toString()+'-'+two.toString()+'+';
        }
        var count = 0;
        var rank = 0;
        var rslen = rankspec.length;
        for (let i=0; i<rslen; ) {
            let rep=1;
            let j=i;
            while (j < rslen && rankspec[j] >= "0" && rankspec[j] <= "9")
                j++;	// skip a number
            if (j > i) { // assume i..j is a number
                rep = parseInt(rankspec.slice(i,j));
            }
            let charval = rankspec[j];
            if (charval && charval != "-") {
                for (let k=0; k<rep; k++) {
                    func(rank+k, charval, arg);
                }
                count += rep;
            }
            rank += rep;
            i=j+1;
        }
        return count;
    }

    /* changeStateFromRankSpec
     *
     * Right now any char (other than '-') is assumed to be a state char
     * to be applied to a range of ranks.  Returns the count impacted.
     */
    changeStateFromRankSpec(hwtype, rankspec) {
        var hwt = this.hwtypes.get(hwtype);
        if (!hwt) return 0;	// empty set
        return HardwareSet.iterateRankSpec(function(rank, charval, arg) {
            arg[rank].changeState(charval);
        }, rankspec, hwt);
    }

    /* locationsToRankSpec
     *
     * Given a space-separated list of locations, produce a rankspec.
     * For now it is assumed locations are always CNs.  This isn't scalable and will be
     * deprecated anyway.
     */
    locationsToRankSpec(locations) {
        // Start with a "long hand" rankspec, then compress later.
        var numcns = this.hwtypes.get('compute-node').length;
        var spec = [];	// array of single chars.   String would be nice, but they are immutable in javascript
        if (locations != null) {
            locations.split(' ').forEach(function (loc) {
                var hwitem = this.getHwByLocation(loc);
                try{
                    spec[hwitem.hwrank] = '+';}
                catch (err){}

            }, this);
        }

        // Now RLE encode the rankspec data
        var rspec = [];		// will build list of items to join

        // spec is a sparse array with only + entries.  Iterate over these + entries.
        var previdx = -1;	// index of prev +
        var runlen = 0;		// current run of + (if any)
        spec.forEach(function (s, i) {
            if (i - 1 == previdx) {
                // we are continuing a line of +'s
                runlen++;
            } else {
                // We skipped ahead.  emit the prev '+' run and this run of '-'
                if (runlen > 0) {
                    if (runlen > 1)
                        rspec.push(runlen);
                    rspec.push('+');
                }
                var skiprun = i - previdx - 1;
                if (skiprun > 1)
                    rspec.push(skiprun);
                rspec.push('-');
                runlen = 1;		// The current + that got us here.
            }
            previdx = i;
        });
        if (runlen > 0) {
            if (runlen > 1)
                rspec.push(runlen);
            rspec.push('+');
        }
        if (rspec.length > 0) {
            return rspec.join('');
        }
    }

    /* assignJob
     *
     * Right now any char (other than '-') is assumed to select hardware
     * Returns the count impacted.  select is a boolean
     * Use tag=null to remove the tag.
     */
    assignJob(job) {
        var hwt = this.hwtypes.get("compute-node");  //QQQ what about "other" compute nodes??
        if (!hwt) return 0;	// empty set
        return HardwareSet.iterateRankSpec(function(rank, charval, arg) {
            arg[rank].changeJob(job);
        }, job.rankspec, hwt);
    }
    selectJob(job) {
        var hwt = this.hwtypes.get("compute-node");  //QQQ what about "other" compute nodes??
        if (!hwt) return 0;	// empty set
        return HardwareSet.iterateRankSpec(function(rank, charval, arg) {
            arg[rank].changeSelected(true);
        }, job.rankspec, hwt);
    }
    unselectJob(job) {
        var hwt = this.hwtypes.get("compute-node");  //QQQ what about "other" compute nodes??
        if (!hwt) return 0;	// empty set
        return HardwareSet.iterateRankSpec(function(rank, charval, arg) {
            arg[rank].changeSelected(false);
        }, job.rankspec, hwt);
    }
}


/*
 * Job -- represents a job on the system (active or completed)
 *
 * This object simply represents the existance and characteristics of a job.  At this
 * time it is not an active object with operations to be performed against it.
 *
 * Currently the job info tracks the attributes of a job (not adequate).
 */
class Job {
    constructor(jobid, info, rankspec) {
        this.jobid = jobid;
        this.info = info;			// QQQ set of attributes for now
        this.rankspec = rankspec;
        this.color = null;			// only applies to active jobs

        var rackset = new Set();
        var hwt = systemInventory.hwtypes.get("compute-node");  //QQQ what about "other" compute nodes??
        var count=0;
        HardwareSet.iterateRankSpec(function(rank, charval, arg) {
            rackset.add(hwt[rank].rack);    // Every node HWitem has a rack reference
            count++;
        }, rankspec, hwt);

        this.rackset = rackset;
        this.numnodes = count;
    }
}

/*
 * JobSet is a set of jobs, active or completed.
 *
 * For UI purposes, a color is assigned to active jobs.
 * We need a method to update job state, including a job completing
 * in which case the color should be returned as the job goes non-active.
 *
 * Note that adding a job here also assigns the job to the system inventory,
 * which may seem a little odd.  Perhaps a JobSet should be considered
 * part of the system inventory (system state).  A SystemState object may
 * be required and inventory and jobs reference it.
 */
class JobSet {
    constructor() {
        this.jobs = new Map();	// indexed by jobid (string), contains Job objects
    }
    addJob(job) {
        if (job.info.state != 'T') {
            // Adding an active job
            var curRed = Math.floor((Math.random()*200)+50);
            var curGreen = Math.floor((Math.random()*200)+50);
            var curBlue = Math.floor((Math.random()*150)+50);
            job.color = "rgb(" + curRed + "," + curGreen + "," + curBlue + ")";
            // systemInventory.assignJob(job);	// QQQ global
            //job.color = jobcolors[job.info.wlmstate];  // QQQ what if we run out?
            //systemInventory.assignJob(job);	// QQQ global
        }
        else
        {
            job.color = jobcolors[job.info.wlmstate];  // QQQ what if we run out?
            //systemInventory.assignJob(job);	// QQQ global
        }
        this.jobs.set(job.jobid, job);
    }
    updateJob(job) {
        if (job.info.state == 'T' && job.color) {	// Job terminated
            //jobcolors.push(job.color);
            job.color = jobcolors[job.info.wlmstate];
            //systemInventory.assignJob(job);	// picks up color=null
        }
    }
    removeJob(job) {
        if (job.color) {
            // implies an active job
            //jobcolors.push(job.color);
            job.color = jobcolors[job.info.wlmstate];
            systemInventory.assignJob(job);	// picks up color=null
        }
        return this.jobs.delete(job.jobid);	// returns job object
    }
    getJobById(jobid) {
        return this.jobs.get(jobid);
    }
}

var colormap = {
    "compute-node-state-M" : "#000d1a",
    "compute-node-state-A" : "#cce5ff",
    "compute-node-state-E" : "red",
    "compute-node-state-S" : "violet",
    "compute-node-state-B" : "#004d99",
    "compute-node-state-D" : "#0073e6",
    "compute-node-state-I" : "#3399ff",
    "compute-node-state-L" : "#66ccff",
    "compute-node-state-K" : "#aaddff",

    "dense-swblade-state-A"      : "#ffd9b3",
    "dense-rectifier-state-A"    : "#ffd9b3",
    "dense-cmm-state-A"          : "#ffbfbf",
    "compute-chassis-state-A"		: "#f5f5f5",
    "service-chassis-state-A"		: "#f5f5f5",
    "chassis-power-supply-state-A"	: "#ffbfbf",
    "compute-fan-state-A" 	: "#df9f9f",
    "compute-opa-hfi-state-A" : "#ffd9b3",
    "vCDU-state-A"               : "#eeeeee",
    "io-node-state-A"            : "#cce5ff",
    "storage-drawer-state-A"     : "#ffbfbf",
    "opa-dcs-state-A"            : "#ffd9b3",
    "subnet-sn-state-A"          : "#cce5ff",
    "eth-switch-state-A"         : "#ffd9b3",
    "opa-switch-state-A"         : "#ffd9b3",
    "service-node-state-A"       : "#cce5ff",
    "dense-service-node-state-A" : "#cce5ff",
    "frontend-node-state-A"      : "#cce5ff",

    "rack-background-color"		 : "#fbfbfb",  // should match canvas background, probably
    "rack-border-color"          : "#dddddd",
    "rack-label-color"           : "rgba(0,0,0,0.12)",
    "rack-dim-label-color"       : "rgba(0,0,0,0.07)",

    "selected-hw"				 : "green"
}
var jobcolors = {
    "running job":"#F5F910",
    "COMPLETED":"#2faf51",
    "NODE_FAIL": "#FA2026",
    "TIMEOUT": "#181896",
    "FAILED": "#FA2026",
    "CANCELLED": "#181896"
}

var systemDescriptions = null;	// mapping of "type"->"description
var systemInventory = null;	// HardwareSet mapping of all location -> hwitems
var floorv = null;			// Global for zoom buttons for now.  May be a subset of the system.
var jobset = null;			// Known jobs...active and completed.

var curviewidxmain = 1;
var serviceNodeTimestamp = null;
var jobset = null;			// Known jobs...active and completed.
var computeNodeTimestamp = null;
var curviewidx = 1;
var tabsobj;
var tabsobj2;
var tabsobj3;
var tabsobj4;
var jobtable;
var jobnonactivetable;
var wlmrestable;
var rastable;
var servicetable;
var diagstable;
var envtable;
var servicenodestable;
var computenodestable;
var inventorysnapshottable;
var inventoryinfotable;
var replacementhistorytable;
var tabids = ['hardware-view','nodestate-view', 'ras-view','env-view', 'invinfo-view', 'replacement-view',
'jobs-view', 'diags-view', 'wlm-reservation-view', 'invsnap-view', 'serviceop-view', 'alert-view'];			// tab id names (discovered on startup)
var urloptions;
var contextList = ["Now", "RAS 2017-06-01 22:15:12.004000 R00-CH2-CB1-PM0-CN0"];
    var tabLinks = new Array();
    var contentDivs = new Array();

// This state table should be queried from the datastore.  Colors are in the colormap.
var States = {
    M: {name: "Missing", description: "Nodes that are Missing or Powered Off)"},
    B: {name: "BIOS", description: "Nodes with BIOS Starting"},
    D: {name: "Discovered", description: "Nodes just Discovered"},
    I: {name: "IP Assigned",   description: "Nodes having IP addresses just assigned"},
    L: {name: "Loading", description: "Nodes that are Loading image"},
    K: {name: "Kernel Boot", description: "Linux kernel starting to boot"},
    A: {name: "Active", description: "Nodes that are Active (booted)"},
    E: {name: "Error",  description: "Nodes in Error"},
    S: {name: "Service", description: "Nodes in Service"},
};

// Polling stuff
//var VOLTDBAPIURL = 'http://10.23.183.26:18080/api/1.0/';
var VOLTDBAPIURL = 'http://localhost:18080/api/1.0/';
var HISTPOLL = 1;	// check every "this many" seconds for a history change.

var curdata;		// Initialized in initFloorLayout();
var contexttime;	// Current time the page should represent (via the datepicker.  null => "Now", else Date obj

var starthistory;				// Initialized in initHistorySlider();
var nodeMaxLastChgTimestamp = null;	// Continuously updated.  Last time the DB was updated
var nodeMaxDBUpdatedTimestamp = null;	// Last DB history update time (always >= nodeMaxLastChgTimestamp).
var rasMaxTimestamp = null;			// Time of last RAS event (string for now)
var jobMaxLastChgTimestamp = null;		// (not updating right now)
var jobMaxDBUpdatedTimestamp = null;	// Time of last Job update (string for now)
var reservationMaxTimestamp=null;		// Time of last WLM reservation table update
var envMaxTimestamp = null; // Time of last insert into the env table
var invMaxTimestamp = null; // Time of last insert into the inventoryinfo table
var invSSMaxTimestamp = null; // Time of last insert into the inventorysnapshot table
var diagsMaxTimestamp = null;
var replacementMaxTimestamp = null;
var serviceOperationTimestamp = null;
function TimeIt(msg, func)
{
    var startmsec = Date.now();
    func();
    var endmsec = Date.now();
    console.log(msg, ": ", endmsec-startmsec, "msec");
}
