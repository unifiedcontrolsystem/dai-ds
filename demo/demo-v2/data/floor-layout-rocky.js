// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

var floorLayout =
//JSON-start
{
	"sysname": "Rocky",
	"views": {
		"Full": {
			"view": "Full",
			"view-description": "Full Floor Layout",
			"initzoom": 0,
			"zoomscales": [4, 6, 8],
			"rackscale": 12,
			"floor" : {
				"description": "The full floor map",
				"width" : 126, "height" : 200,
				"content" : [
					{ "name": "R48", "type": "custom-rack48", "x": 5, "y": 1 },
					{ "name": "R50", "type": "custom-rack50", "x":45, "y": 1 },
					{ "name": "R51", "type": "custom-rack51", "x":85, "y": 1 }
				]
			}, /* floor */
			"definitions": {
				"custom-rack48" : {
				  "description": "Rocky Rack 48",
				  "width": 38, "height": 91, "obscured":true,
				  "content" : [
					{"name": "SW0", "type": "eth-switch", 		"x":0,  "y":0},
					{"name": "OSW0", "type": "opa-switch", 		"x":0,  "y":2},
					{"name": "OSW1", "type": "opa-switch", 		"x":0,  "y":4},
					{"name": "UPS0", "type": "ups", 			"x":0,	"y":8},
					{"name": "SMW",  "type": "service-node", 	"x":0, 	"y":10},
					{"name": "UPS1", "type":  "ups", 			"x":0,	"y":14},
					{"name": "UPS2", "type":  "ups", 			"x":0,	"y":18},
			
					{"name": "CH00", "type": "compute-chassis", "x":0,  "y":28},
					{"name": "CH01", "type": "compute-chassis", "x":0,  "y":33},		
					{"name": "CH02", "type": "service-chassis", "x":0,  "y":38},

					{"name": "KVM0", "type": "kvm",				"x":0,	"y":46},

					{"name": "CH03", "type": "service-chassis", "x":0,  "y":50},
					{"name": "CH04", "type": "compute-chassis", "x":0,  "y":56},
					{"name": "CH05", "type": "compute-chassis", "x":0,  "y":62},
					{"name": "CH06", "type": "compute-chassis", "x":0,  "y":68},
					{"name": "CH07", "type": "compute-chassis", "x":0,  "y":74},
					{"name": "CH08", "type": "compute-chassis", "x":0,  "y":80}

				  ]
				},
				"custom-rack50" : {
				  "description": "Rocky Rack 50",
				  "width": 38, "height": 91, "obscured":true,
				  "content" : [
					{"name": "SW0", "type": "eth-switch",  "x":0, "y":0},
					{"name": "SW1", "type": "eth-switch",  "x":0, "y":2},
					{"name": "SW2", "type": "eth-switch",  "x":0, "y":4},
					{"name": "OSW0", "type": "opa-switch", "x":0, "y":6},
					{"name": "OSW1", "type": "opa-switch", "x":0, "y":8},
					{"name": "OSW2", "type": "opa-switch", "x":0, "y":10},
					
					{"name": "CH00", "type": "compute-chassis", "x":0,  "y":12 },
					{"name": "CH01", "type": "compute-chassis", "x":0,  "y":17},
					{"name": "CH02", "type": "compute-chassis", "x":0,  "y":23},

					{"name": "KVM0", "type":  "kvm", "x":0, "y":27},
					
					{"name": "CH03", "type": "compute-chassis", "x":0,  "y":31},
					{"name": "CH04", "type": "compute-chassis", "x":0,  "y":36},
					
					{"name": "UPS0",  "type": "ups", "x":0, "y":44},
					{"name": "KVM1", "type":  "kvm", "x":0, "y":46},

					{"name": "CH05", "type": "compute-chassis", "x":0,  "y":50},
					{"name": "CH06", "type": "compute-chassis", "x":0,  "y":56},
					{"name": "CH07", "type": "compute-chassis", "x":0,  "y":62},
					{"name": "CH08", "type": "compute-chassis", "x":0,  "y":68},
					{"name": "CH09", "type": "compute-chassis", "x":0,  "y":74},
					{"name": "CH10", "type": "compute-chassis", "x":0,  "y":80}
				  ]
				},
				"custom-rack51" : {
				  "description": "Rocky Rack 51",
				  "width": 38, "height": 91, "obscured":true,
				  "content" : [
					{"name": "HD09", "type": "service-node", "x":0,  "y":12 },
					{"name": "HD10", "type": "service-node", "x":0,  "y":17 },
					{"name": "HD11", "type": "service-node", "x":0,  "y":22 },
					{"name": "HD12", "type": "service-node", "x":0,  "y":27 },
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
				"service-chassis": {
				  "description": "Compute Chassis",
				  "width": 38, "height": 4,
				  "content" : [
					{"name": "SN0", "type": "dense-service-node", "x":2,  "y":0},
					{"name": "SN1", "type": "dense-service-node", "x":22, "y":0},
					{"name": "SN2", "type": "dense-service-node", "x":2,  "y":2},
					{"name": "SN3", "type": "dense-service-node", "x":22, "y":2},
					{"name": "PS1", "type": "chassis-power-supply", "x":18, "y":0},
					{"name": "PS0", "type": "chassis-power-supply", "x":18, "y":2}
				  ]
				},
				"dense-service-node": {
				  "description": "Service Node",
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
				"kvm": {
				  "description": "KVM Switch",
				  "width": 38, "height": 2,
				  "content" : []
				},
				"ups": {
				  "description": "Uninterruptable Power Supply",
				  "width": 38, "height": 2,
				  "content" : []
				},
				"service-node": {
				  "description": "Wolf Pass Head Node",
				  "width": 38, "height": 4,
				  "content" : []
				},			}, /* types */
		}, /* Full view */
	} /* views */
}
//JSON-end
;

