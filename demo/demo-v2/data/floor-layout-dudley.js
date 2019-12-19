// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

var floorLayout =
//JSON-start
{
	"sysname": "Dudley",
	"views": {
		"Full": {
			"view": "Full",
			"view-description": "Full Floor Layout",
			"initzoom": 0,
			"zoomscales": [4, 6, 8],
			"rackscale": 12,
			"floor" : {
				"description": "The full floor map",
				"width" : 100, "height" : 200,
				"content" : [
					{ "name": "SN0", "type": "service-rack", "x": 10, "y": 1 },
					{ "name": "R0",  "type": "compute-rack", "x": 55, "y": 1 }
				]
			}, /* floor */
			"definitions": {
				"compute-rack" : {
				  "description": "Compute Rack",
				  "width": 38, "height": 91, "obscured":true,
				  "content" : [
					{"name": "SW0", "type": "eth-switch", "x":0,  "y":0},
					{"name": "SW1", "type": "eth-switch", "x":0,  "y":2},
					{"name": "CH00", "type": "compute-chassis", "x":0,  "y":4 },
					{"name": "CH01", "type": "compute-chassis", "x":0,  "y":9 },
					
					{"name": "OSW0", "type": "opa-switch", 		"x":0,  "y":13},

					{"name": "CH02", "type": "compute-chassis", "x":0,  "y":15},
					{"name": "CH03", "type": "compute-chassis", "x":0,  "y":20},
					{"name": "CH04", "type": "compute-chassis", "x":0,  "y":25},
					{"name": "CH05", "type": "compute-chassis", "x":0,  "y":30},
					
					{"name": "OSW1", "type": "opa-switch", 		"x":0,  "y":34},

					{"name": "CH06", "type": "compute-chassis", "x":0,  "y":36},
					{"name": "CH07", "type": "compute-chassis", "x":0,  "y":41},
					{"name": "CH08", "type": "compute-chassis", "x":0,  "y":46},
					{"name": "CH09", "type": "compute-chassis", "x":0,  "y":51},
					
					{"name": "OSW2", "type": "opa-switch", 		"x":0,  "y":55},

					{"name": "CH10", "type": "compute-chassis", "x":0,  "y":57},
					{"name": "CH11", "type": "compute-chassis", "x":0,  "y":62},					
					{"name": "CH12", "type": "compute-chassis", "x":0,  "y":67},
					{"name": "CH13", "type": "compute-chassis", "x":0,  "y":72},
					
					{"name": "OSW3", "type": "opa-switch", 		"x":0,  "y":76},

					{"name": "CH14", "type": "compute-chassis", "x":0,  "y":78},
					{"name": "CH15", "type": "compute-chassis", "x":0,  "y":83},
					
					{"name": "SW2", "type": "eth-switch", "x":0,  "y":87},
					{"name": "SW3", "type": "eth-switch", "x":0,  "y":89}
				  ]
				},
				"compute-chassis": {
				  "description": "Compute Chassis",
				  "width": 38, "height": 4,
				  "content" : [
					{"name": "CN0", "type": "dense-compute-node", "x":2,  "y":0},
					{"name": "CN1", "type": "dense-compute-node", "x":22, "y":0},
					{"name": "CN2", "type": "dense-compute-node", "x":2,  "y":2},
					{"name": "CN3", "type": "dense-compute-node", "x":22, "y":2},
					{"name": "PS1", "type": "chassis-power-supply", "x":18, "y":0},
					{"name": "PS0", "type": "chassis-power-supply", "x":18, "y":2}
				  ]
				},
				"dense-compute-node": {
				  "description": "Compute Node",
				  "width": 16, "height": 2,
				  "content" : [
					{"name": "FAN0", "type": "dense-compute-fan", "x":10,  "y":0},
					{"name": "FAN1", "type": "dense-compute-fan", "x":11,  "y":0},
					{"name": "FAN2", "type": "dense-compute-fan", "x":12,  "y":0},
					{"name": "FAN3", "type": "dense-compute-fan", "x":13,  "y":0},
					{"name": "FAN4", "type": "dense-compute-fan", "x":14,  "y":0},
					{"name": "FAN5", "type": "dense-compute-fan", "x":15,  "y":0},
					{"name": "OPA0", "type": "dense-compute-opa-hfi", "x":10,  "y":1},
					{"name": "OPA1", "type": "dense-compute-opa-hfi", "x":13,  "y":1},
				  ]
				},
				"chassis-power-supply": {
				  "description": "Chassis Power Supply",
				  "width": 4, "height": 2,
				  "content" : []
				},
				"dense-compute-fan": {
				  "description": "Compute Node Fan",
				  "width": 1, "height": 1,
				  "content" : []
				},
				"dense-compute-opa-hfi": {
				  "description": "OmniPath HFA Adapter",
				  "width": 3, "height": 1,
				  "content" : []
				},
				"service-rack": {
				  "description": "Dudley Service Rack",
				  "width": 38, "height": 91, "obscured":true,
				  "content" : [
					{ "name": "SW0",  "type":"eth-switch"    ,"x":0, "y":0 },
					{ "name": "SW1",  "type":"eth-switch"    ,"x":0, "y":2 },
					{ "name": "SMW0",  "type":"service-node" ,"x":0, "y":4 },
					{ "name": "UAN0", "type":"service-node"  ,"x":0, "y":9 },
					{ "name": "SMG0", "type":"service-node"  ,"x":0, "y":14 },
					{ "name": "DBG0", "type":"service-node"  ,"x":0, "y":19 },
					{ "name": "IOG0", "type":"service-node"  ,"x":0, "y":24 },
					{ "name": "NFS0", "type":"service-node"  ,"x":0, "y":29 },
					{ "name": "BT0",  "type":"service-node"  ,"x":0, "y":34 },
					{ "name": "MDS", "type":"service-node"   ,"x":0, "y":39 },
					{ "name": "OSS1", "type":"service-node"  ,"x":0, "y":44 },
					{ "name": "OSS2", "type":"service-node"  ,"x":0, "y":49 }
				  ]
				},
				"eth-switch": {
				  "description": "Ethernet Switch",
				  "width": 38, "height": 2,
				  "content" : []
				},
				"opa-switch": {
				  "description": "Omni-path Switch",
				  "width": 38, "height": 2,
				  "content" : []
				},
				"service-node": {
				  "description": "Service Node",
				  "width": 38, "height": 4,
				  "content" : []
				},
			}, /* types */
		}, /* Full view */
	} /* views */
}
//JSON-end
;

