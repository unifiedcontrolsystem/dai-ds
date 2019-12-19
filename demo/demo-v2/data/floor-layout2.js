// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

// Sample floor layout for 198 rack (16x12 racks)
//
var floorLayout =
//JSON-start
{
	"sysname": "Skynet",
	"views": {
		"Full": {
			"view": "Full",
			"view-description": "Full Floor Layout",
			"initzoom": 1,
			"zoomscales": [1, 2, 5, 15],
			"definitions": {
				"dense-rack" : {
				  "description": "Dense compute rack",
				  "width": 20, "height": 39, "obscured":true,
				  "content" : [
					{"name": "CH0", "type": "dense-chassis", "x":0, "y":0},
					{"name": "CH1", "type": "dense-chassis", "x":0, "y":10},
					{"name": "CH2", "type": "dense-chassis", "x":0, "y":20},
					{"name": "CH3", "type": "dense-chassis", "x":0, "y":30}
				  ]
				},
				"dense-chassis": {
				  "description": "Dense compute chassis",
				  "width": 20, "height": 9, "obscured":true,
				  "content" : [
					{"name": "CB0", "type": "dense-blade", "x":2 , "y":0},
					{"name": "CB1", "type": "dense-blade", "x":3 , "y":0},
					{"name": "CB2", "type": "dense-blade", "x":4 , "y":0},
					{"name": "CB3", "type": "dense-blade", "x":5 , "y":0},
					{"name": "CB4", "type": "dense-blade", "x":6 , "y":0},
					{"name": "CB5", "type": "dense-blade", "x":7 , "y":0},
					{"name": "CB6", "type": "dense-blade", "x":8 , "y":0},
					{"name": "CB7", "type": "dense-blade", "x":9 , "y":0},
					{"name": "CB8", "type": "dense-blade", "x":10, "y":0},
					{"name": "CB9", "type": "dense-blade", "x":11, "y":0},
					{"name": "CBA", "type": "dense-blade", "x":12, "y":0},
					{"name": "CBB", "type": "dense-blade", "x":13, "y":0},
					{"name": "CBC", "type": "dense-blade", "x":14, "y":0},
					{"name": "CBD", "type": "dense-blade", "x":15, "y":0},
					{"name": "CBE", "type": "dense-blade", "x":16, "y":0},
					{"name": "CBF", "type": "dense-blade", "x":17, "y":0},
					{"name": "SW0", "type": "dense-swblade", "x":0 , "y":0},
					{"name": "SW1", "type": "dense-swblade", "x":0 , "y":1},
					{"name": "SW2", "type": "dense-swblade", "x":0 , "y":2},
					{"name": "SW3", "type": "dense-swblade", "x":0 , "y":3},
					{"name": "SW4", "type": "dense-swblade", "x":0 , "y":4},
					{"name": "SW5", "type": "dense-swblade", "x":0 , "y":5},
					{"name": "SW6", "type": "dense-swblade", "x":0 , "y":6},
					{"name": "SW7", "type": "dense-swblade", "x":0 , "y":7},
					{"name": "SW8", "type": "dense-swblade", "x":18, "y":0},
					{"name": "SW9", "type": "dense-swblade", "x":18, "y":1},
					{"name": "SWA", "type": "dense-swblade", "x":18, "y":2},
					{"name": "SWB", "type": "dense-swblade", "x":18, "y":3},
					{"name": "SWC", "type": "dense-swblade", "x":18, "y":4},
					{"name": "SWD", "type": "dense-swblade", "x":18, "y":5},
					{"name": "SWE", "type": "dense-swblade", "x":18, "y":6},
					{"name": "SWF", "type": "dense-swblade", "x":18, "y":7},
					{"name": "RF0", "type": "dense-rectifier", "x":1 , "y":8},
					{"name": "RF1", "type": "dense-rectifier", "x":3 , "y":8},
					{"name": "RF2", "type": "dense-rectifier", "x":5 , "y":8},
					{"name": "RF3", "type": "dense-rectifier", "x":7 , "y":8},
					{"name": "CMM", "type": "dense-cmm",       "x":9 , "y":8},
					{"name": "RF4", "type": "dense-rectifier", "x":11, "y":8},
					{"name": "RF5", "type": "dense-rectifier", "x":13, "y":8},
					{"name": "RF6", "type": "dense-rectifier", "x":15, "y":8},
					{"name": "RF7", "type": "dense-rectifier", "x":17, "y":8}
				  ]
				},
				"dense-blade": {
				  "description": "Dense compute blade",
				  "width": 1, "height": 8, "obscured":true,
				  "content" : [
					{"name": "PM0", "type": "dense-processor-module", "x":0, "y":0},
					{"name": "PM1", "type": "dense-processor-module", "x":0, "y":2},
					{"name": "PM2", "type": "dense-processor-module", "x":0, "y":4},
					{"name": "PM3", "type": "dense-processor-module", "x":0, "y":6}
				  ]
				},
				"dense-processor-module": {
				  "description": "Dense compute processor module",
				  "width": 1, "height": 2, "obscured":true,
				  "content" : [
					{"name": "CN0", "type": "dense-compute-node", "x":0, "y":0},
					{"name": "CN1", "type": "dense-compute-node", "x":0, "y":1}
				  ]
				},
				"dense-compute-node": {
				  "description": "Dense compute processor node",
				  "width": 1, "height": 1,
				  "content" : []
				},
				"dense-swblade": {
				  "description": "Dense switch blade",
				  "width": 2, "height": 1,
				  "content" : []
				},
				"dense-rectifier": {
				  "description": "Dense chassis power rectifier",
				  "width": 2, "height": 1,
				  "content" : []
				},
				"dense-cmm": {
				  "description": "Dense chassis management module",
				  "width": 2, "height": 1,
				  "content" : []
				},
				"vCDU": {
				  "description": "Minimal CDU",
				  "width": 7, "height": 39,
				  "content" : []
				},
				"DCS-rack": {
				  "description": "Director Class Switch rack",
				  "width": 10, "height": 42, "obscured":true, "rack":"rotate",
				  "content": [
					{ "name": "SW0", "type":"opa-dcs", "x":0, "y": 0 },
					{ "name": "SW1", "type":"opa-dcs", "x":0, "y": 9 },
					{ "name": "SW2", "type":"opa-dcs", "x":0, "y":18 },
					{ "name": "SW3", "type":"opa-dcs", "x":0, "y":27 }
				  ]
				},
				"opa-dcs": {
				  "description": "OPA Director Class Switch Drawer",
				  "width": 10, "height": 8,
				  "content" : []
				},
				"IO-rack": {
				  "description": "Dense I/O and BB",
				  "width": 10, "height": 42, "obscured":true, "rack":"rotate",
				  "content": [
					{ "name": "ION0", "type":"io-node", "x":0, "y":0  },
					{ "name": "ION1", "type":"io-node", "x":0, "y":3  },
					{ "name": "ION2", "type":"io-node", "x":0, "y":6  },
					{ "name": "ION3", "type":"io-node", "x":0, "y":9  },
					{ "name": "ION4", "type":"io-node", "x":0, "y":12 },
					{ "name": "ION5", "type":"io-node", "x":0, "y":15 },
					{ "name": "ION6", "type":"io-node", "x":0, "y":18 },
					{ "name": "ION7", "type":"io-node", "x":0, "y":21 },
					{ "name": "ION8", "type":"io-node", "x":0, "y":24 },
					{ "name": "ION9", "type":"io-node", "x":0, "y":27 },
					{ "name": "IONA", "type":"io-node", "x":0, "y":30 },
					{ "name": "IONB", "type":"io-node", "x":0, "y":33 }
				  ]
				},
				"io-node": {
				  "description": "IO Node Drawer",
				  "width": 10, "height": 3,
				  "content" : []
				},
				"storage": {
				  "description": "Lustre storage rack",
				  "width": 10, "height": 42, "obscured":true, "rack":"rotate",
				  "content": [
					{ "name": "SA0", "type":"storage-drawer", "x":0, "y":0  },
					{ "name": "SA1", "type":"storage-drawer", "x":0, "y":7  },
					{ "name": "SA2", "type":"storage-drawer", "x":0, "y":14 },
					{ "name": "SA3", "type":"storage-drawer", "x":0, "y":21 },
					{ "name": "SA4", "type":"storage-drawer", "x":0, "y":28 },
					{ "name": "SA5", "type":"storage-drawer", "x":0, "y":35 }
				  ]
				},
				"storage-drawer": {
				  "description": "Storage Drawer",
				  "width": 10, "height": 7,
				  "content" : []
				},
				"SN-rack": {
				  "description": "SN systems",
				  "width": 11, "height": 42, "obscured":true, "rack":"rotate",
				  "content" : [
					{ "name": "SW0",  "type":"eth-switch"    ,"x":0, "y":0  },
					{ "name": "SSN0", "type":"subnet-sn"     ,"x":0, "y":1  },
					{ "name": "SSN1", "type":"subnet-sn"     ,"x":0, "y":2  },
					{ "name": "SSN2", "type":"subnet-sn"     ,"x":0, "y":3  },
					{ "name": "SSN3", "type":"subnet-sn"     ,"x":0, "y":4  },
					{ "name": "SSN4", "type":"subnet-sn"     ,"x":0, "y":5  },
					{ "name": "SSN5", "type":"subnet-sn"     ,"x":0, "y":6  },
					{ "name": "SW1",  "type":"eth-switch"    ,"x":0, "y":7  },
					{ "name": "SSN6", "type":"subnet-sn"     ,"x":0, "y":8  },
					{ "name": "SSN7", "type":"subnet-sn"     ,"x":0, "y":9  },
					{ "name": "SSN8", "type":"subnet-sn"     ,"x":0, "y":10 },
					{ "name": "SSN9", "type":"subnet-sn"     ,"x":0, "y":11 },
					{ "name": "SSNA", "type":"subnet-sn"     ,"x":0, "y":12 },
					{ "name": "SSNB", "type":"subnet-sn"     ,"x":0, "y":13 },
					{ "name": "D0",   "type":"service-node"  ,"x":0, "y":15 },
					{ "name": "D1",   "type":"service-node"  ,"x":0, "y":17 },
					{ "name": "SA0",  "type":"storage-drawer","x":0, "y":20 },
					{ "name": "FEN0", "type":"frontend-node" ,"x":0, "y":28 },
					{ "name": "FEN1", "type":"frontend-node" ,"x":0, "y":32 },
					{ "name": "FEN2", "type":"frontend-node" ,"x":0, "y":36 }
				  ]
				},
				"eth-switch": {
				  "description": "Ethernet Switch",
				  "width": 10, "height": 1,
				  "content" : []
				},
				"subnet-sn": {
				  "description": "Subnet Service Node",
				  "width": 10, "height": 1,
				  "content" : []
				},
				"service-node": {
				  "description": "Service Node",
				  "width": 10, "height": 2,
				  "content" : []
				},
				"frontend-node": {
				  "description": "Frontend Node",
				  "width": 10, "height": 4,
				  "content" : []
				}
			}, /* types */
			"floor" : {
				"description": "The full floor map",
				"width" : 409, "height" : 597,
				"content" : [  /* 23 unit rack pitch.  42 unit row pitch */
					{ "name": "R00",    "type": "dense-rack", "x":   1, "y":   1 },
					{ "name": "R01",    "type": "dense-rack", "x":  24, "y":   1 },
					{ "name": "R02",    "type": "dense-rack", "x":  47, "y":   1 },
					{ "name": "R03",    "type": "dense-rack", "x":  70, "y":   1 },
					{ "name": "CDU00",  "type": "vCDU",       "x":  93, "y":   1 },
					{ "name": "CDU01",  "type": "vCDU",       "x": 103, "y":   1 },
					{ "name": "R04",    "type": "dense-rack", "x": 113, "y":   1 },
					{ "name": "R05",    "type": "dense-rack", "x": 136, "y":   1 },
					{ "name": "R06",    "type": "dense-rack", "x": 159, "y":   1 },
					{ "name": "R07",    "type": "dense-rack", "x": 182, "y":   1 },
					{ "name": "R08",    "type": "dense-rack", "x": 205, "y":   1 },
					{ "name": "R09",    "type": "dense-rack", "x": 228, "y":   1 },
					{ "name": "R0A",    "type": "dense-rack", "x": 251, "y":   1 },
					{ "name": "R0B",    "type": "dense-rack", "x": 274, "y":   1 },
					{ "name": "CDU02",  "type": "vCDU",       "x": 297, "y":   1 },
					{ "name": "CDU03",  "type": "vCDU",       "x": 307, "y":   1 },
					{ "name": "R0C",    "type": "dense-rack", "x": 317, "y":   1 },
					{ "name": "R0D",    "type": "dense-rack", "x": 340, "y":   1 },
					{ "name": "R0E",    "type": "dense-rack", "x": 363, "y":   1 },
					{ "name": "R0F",    "type": "dense-rack", "x": 386, "y":   1 },
										
					{ "name": "R10",    "type": "dense-rack", "x":   1, "y":  43 },
					{ "name": "R11",    "type": "dense-rack", "x":  24, "y":  43 },
					{ "name": "R12",    "type": "dense-rack", "x":  47, "y":  43 },
					{ "name": "R13",    "type": "dense-rack", "x":  70, "y":  43 },
					{ "name": "CDU10",  "type": "vCDU",       "x":  93, "y":  43 },
					{ "name": "CDU11",  "type": "vCDU",       "x": 103, "y":  43 },
					{ "name": "R14",    "type": "dense-rack", "x": 113, "y":  43 },
					{ "name": "R15",    "type": "dense-rack", "x": 136, "y":  43 },
					{ "name": "R16",    "type": "dense-rack", "x": 159, "y":  43 },
					{ "name": "R17",    "type": "dense-rack", "x": 182, "y":  43 },
					{ "name": "R18",    "type": "dense-rack", "x": 205, "y":  43 },
					{ "name": "R19",    "type": "dense-rack", "x": 228, "y":  43 },
					{ "name": "R1A",    "type": "dense-rack", "x": 251, "y":  43 },
					{ "name": "R1B",    "type": "dense-rack", "x": 274, "y":  43 },
					{ "name": "CDU12",  "type": "vCDU",       "x": 297, "y":  43 },
					{ "name": "CDU13",  "type": "vCDU",       "x": 307, "y":  43 },
					{ "name": "R1C",    "type": "dense-rack", "x": 317, "y":  43 },
					{ "name": "R1D",    "type": "dense-rack", "x": 340, "y":  43 },
					{ "name": "R1E",    "type": "dense-rack", "x": 363, "y":  43 },
					{ "name": "R1F",    "type": "dense-rack", "x": 386, "y":  43 },
										
					{ "name": "R20",    "type": "dense-rack", "x":   1, "y":  85 },
					{ "name": "R21",    "type": "dense-rack", "x":  24, "y":  85 },
					{ "name": "R22",    "type": "dense-rack", "x":  47, "y":  85 },
					{ "name": "R23",    "type": "dense-rack", "x":  70, "y":  85 },
					{ "name": "CDU20",  "type": "vCDU",       "x":  93, "y":  85 },
					{ "name": "CDU21",  "type": "vCDU",       "x": 103, "y":  85 },
					{ "name": "R24",    "type": "dense-rack", "x": 113, "y":  85 },
					{ "name": "R25",    "type": "dense-rack", "x": 136, "y":  85 },
					{ "name": "R26",    "type": "dense-rack", "x": 159, "y":  85 },
					{ "name": "R27",    "type": "dense-rack", "x": 182, "y":  85 },
					{ "name": "R28",    "type": "dense-rack", "x": 205, "y":  85 },
					{ "name": "R29",    "type": "dense-rack", "x": 228, "y":  85 },
					{ "name": "R2A",    "type": "dense-rack", "x": 251, "y":  85 },
					{ "name": "R2B",    "type": "dense-rack", "x": 274, "y":  85 },
					{ "name": "CDU22",  "type": "vCDU",       "x": 297, "y":  85 },
					{ "name": "CDU23",  "type": "vCDU",       "x": 307, "y":  85 },
					{ "name": "R2C",    "type": "dense-rack", "x": 317, "y":  85 },
					{ "name": "R2D",    "type": "dense-rack", "x": 340, "y":  85 },
					{ "name": "R2E",    "type": "dense-rack", "x": 363, "y":  85 },
					{ "name": "R2F",    "type": "dense-rack", "x": 386, "y":  85 },
										
					{ "name": "R30",    "type": "dense-rack", "x":   1, "y": 127 },
					{ "name": "R31",    "type": "dense-rack", "x":  24, "y": 127 },
					{ "name": "R32",    "type": "dense-rack", "x":  47, "y": 127 },
					{ "name": "R33",    "type": "dense-rack", "x":  70, "y": 127 },
					{ "name": "CDU30",  "type": "vCDU",       "x":  93, "y": 127 },
					{ "name": "CDU31",  "type": "vCDU",       "x": 103, "y": 127 },
					{ "name": "R34",    "type": "dense-rack", "x": 113, "y": 127 },
					{ "name": "R35",    "type": "dense-rack", "x": 136, "y": 127 },
					{ "name": "R36",    "type": "dense-rack", "x": 159, "y": 127 },
					{ "name": "R37",    "type": "dense-rack", "x": 182, "y": 127 },
					{ "name": "R38",    "type": "dense-rack", "x": 205, "y": 127 },
					{ "name": "R39",    "type": "dense-rack", "x": 228, "y": 127 },
					{ "name": "R3A",    "type": "dense-rack", "x": 251, "y": 127 },
					{ "name": "R3B",    "type": "dense-rack", "x": 274, "y": 127 },
					{ "name": "CDU32",  "type": "vCDU",       "x": 297, "y": 127 },
					{ "name": "CDU33",  "type": "vCDU",       "x": 307, "y": 127 },
					{ "name": "R3C",    "type": "dense-rack", "x": 317, "y": 127 },
					{ "name": "R3D",    "type": "dense-rack", "x": 340, "y": 127 },
					{ "name": "R3E",    "type": "dense-rack", "x": 363, "y": 127 },
					{ "name": "R3F",    "type": "dense-rack", "x": 386, "y": 127 },
										
					{ "name": "R40",    "type": "dense-rack", "x":   1, "y": 169 },
					{ "name": "R41",    "type": "dense-rack", "x":  24, "y": 169 },
					{ "name": "R42",    "type": "dense-rack", "x":  47, "y": 169 },
					{ "name": "R43",    "type": "dense-rack", "x":  70, "y": 169 },
					{ "name": "CDU40",  "type": "vCDU",       "x":  93, "y": 169 },
					{ "name": "CDU41",  "type": "vCDU",       "x": 103, "y": 169 },
					{ "name": "R44",    "type": "dense-rack", "x": 113, "y": 169 },
					{ "name": "R45",    "type": "dense-rack", "x": 136, "y": 169 },
					{ "name": "R46",    "type": "dense-rack", "x": 159, "y": 169 },
					{ "name": "R47",    "type": "dense-rack", "x": 182, "y": 169 },
					{ "name": "R48",    "type": "dense-rack", "x": 205, "y": 169 },
					{ "name": "R49",    "type": "dense-rack", "x": 228, "y": 169 },
					{ "name": "R4A",    "type": "dense-rack", "x": 251, "y": 169 },
					{ "name": "R4B",    "type": "dense-rack", "x": 274, "y": 169 },
					{ "name": "CDU42",  "type": "vCDU",       "x": 297, "y": 169 },
					{ "name": "CDU43",  "type": "vCDU",       "x": 307, "y": 169 },
					{ "name": "R4C",    "type": "dense-rack", "x": 317, "y": 169 },
					{ "name": "R4D",    "type": "dense-rack", "x": 340, "y": 169 },
					{ "name": "R4E",    "type": "dense-rack", "x": 363, "y": 169 },
					{ "name": "R4F",    "type": "dense-rack", "x": 386, "y": 169 },
										
					{ "name": "R50",    "type": "dense-rack", "x":   1, "y": 211 },
					{ "name": "R51",    "type": "dense-rack", "x":  24, "y": 211 },
					{ "name": "R52",    "type": "dense-rack", "x":  47, "y": 211 },
					{ "name": "R53",    "type": "dense-rack", "x":  70, "y": 211 },
					{ "name": "CDU50",  "type": "vCDU",       "x":  93, "y": 211 },
					{ "name": "CDU51",  "type": "vCDU",       "x": 103, "y": 211 },
					{ "name": "R54",    "type": "dense-rack", "x": 113, "y": 211 },
					{ "name": "R55",    "type": "dense-rack", "x": 136, "y": 211 },
					{ "name": "R56",    "type": "dense-rack", "x": 159, "y": 211 },
					{ "name": "R57",    "type": "dense-rack", "x": 182, "y": 211 },
					{ "name": "R58",    "type": "dense-rack", "x": 205, "y": 211 },
					{ "name": "R59",    "type": "dense-rack", "x": 228, "y": 211 },
					{ "name": "R5A",    "type": "dense-rack", "x": 251, "y": 211 },
					{ "name": "R5B",    "type": "dense-rack", "x": 274, "y": 211 },
					{ "name": "CDU52",  "type": "vCDU",       "x": 297, "y": 211 },
					{ "name": "CDU53",  "type": "vCDU",       "x": 307, "y": 211 },
					{ "name": "R5C",    "type": "dense-rack", "x": 317, "y": 211 },
					{ "name": "R5D",    "type": "dense-rack", "x": 340, "y": 211 },
					{ "name": "R5E",    "type": "dense-rack", "x": 363, "y": 211 },
					{ "name": "R5F",    "type": "dense-rack", "x": 386, "y": 211 },
										
					{ "name": "IO0",    "type": "IO-rack",    "x":25 ,  "y": 253 },
					{ "name": "S00",    "type": "storage",    "x":37 ,  "y": 253 },
					{ "name": "S01",    "type": "storage",    "x":49 ,  "y": 253 },
					{ "name": "S02",    "type": "storage",    "x":61 ,  "y": 253 },
					{ "name": "S03",    "type": "storage",    "x":73 ,  "y": 253 },
					{ "name": "S04",    "type": "storage",    "x":85 ,  "y": 253 },
					{ "name": "I01",    "type": "IO-rack",    "x":97 ,  "y": 253 },
					{ "name": "S10",    "type": "storage",    "x":109,  "y": 253 },
					{ "name": "S11",    "type": "storage",    "x":121,  "y": 253 },
					{ "name": "S12",    "type": "storage",    "x":133,  "y": 253 },
					{ "name": "S13",    "type": "storage",    "x":145,  "y": 253 },
					{ "name": "S14",    "type": "storage",    "x":157,  "y": 253 },

					{ "name": "DCS0",    "type": "DCS-rack",  "x":193,  "y": 253 },
					{ "name": "DCS1",    "type": "DCS-rack",  "x":205,  "y": 253 },
			 
					{ "name": "S24",    "type": "storage",    "x":241,  "y": 253 },
					{ "name": "S23",    "type": "storage",    "x":253,  "y": 253 },
					{ "name": "S22",    "type": "storage",    "x":265,  "y": 253 },
					{ "name": "S21",    "type": "storage",    "x":277,  "y": 253 },
					{ "name": "S20",    "type": "storage",    "x":289,  "y": 253 },
					{ "name": "IO2",    "type": "IO-rack",    "x":301,  "y": 253 },
					{ "name": "S34",    "type": "storage",    "x":313,  "y": 253 },
					{ "name": "S33",    "type": "storage",    "x":325,  "y": 253 },
					{ "name": "S32",    "type": "storage",    "x":337,  "y": 253 },
					{ "name": "S31",    "type": "storage",    "x":349,  "y": 253 },
					{ "name": "S30",    "type": "storage",    "x":361,  "y": 253 },
					{ "name": "IO3",    "type": "IO-rack",    "x":373,  "y": 253 },
															  
					{ "name": "IO4",    "type": "IO-rack",    "x":25 ,  "y": 299 },
					{ "name": "S40",    "type": "storage",    "x":37 ,  "y": 299 },
					{ "name": "S41",    "type": "storage",    "x":49 ,  "y": 299 },
					{ "name": "S42",    "type": "storage",    "x":61 ,  "y": 299 },
					{ "name": "S43",    "type": "storage",    "x":73 ,  "y": 299 },
					{ "name": "S44",    "type": "storage",    "x":85 ,  "y": 299 },
					{ "name": "IO5",    "type": "IO-rack",    "x":97 ,  "y": 299 },
					{ "name": "S50",    "type": "storage",    "x":109,  "y": 299 },
					{ "name": "S51",    "type": "storage",    "x":121,  "y": 299 },
					{ "name": "S52",    "type": "storage",    "x":133,  "y": 299 },
					{ "name": "S53",    "type": "storage",    "x":145,  "y": 299 },
					{ "name": "S54",    "type": "storage",    "x":157,  "y": 299 },

					{ "name": "SN0",    "type": "SN-rack",    "x":181,  "y": 299 },
					{ "name": "SN1",    "type": "SN-rack",    "x":193,  "y": 299 },
					{ "name": "SN2",    "type": "SN-rack",    "x":205,  "y": 299 },
					{ "name": "SN3",    "type": "SN-rack",    "x":217,  "y": 299 },

					{ "name": "S64",    "type": "storage",    "x":241,  "y": 299 },
					{ "name": "S63",    "type": "storage",    "x":253,  "y": 299 },
					{ "name": "S62",    "type": "storage",    "x":265,  "y": 299 },
					{ "name": "S61",    "type": "storage",    "x":277,  "y": 299 },
					{ "name": "S60",    "type": "storage",    "x":289,  "y": 299 },
					{ "name": "I06",    "type": "IO-rack",    "x":301,  "y": 299 },
					{ "name": "S74",    "type": "storage",    "x":313,  "y": 299 },
					{ "name": "S73",    "type": "storage",    "x":325,  "y": 299 },
					{ "name": "S72",    "type": "storage",    "x":337,  "y": 299 },
					{ "name": "S71",    "type": "storage",    "x":349,  "y": 299 },
					{ "name": "S70",    "type": "storage",    "x":361,  "y": 299 },
					{ "name": "IO7",    "type": "IO-rack",    "x":373,  "y": 299 },
										
					{ "name": "R60",    "type": "dense-rack", "x":   1, "y": 345 },
					{ "name": "R61",    "type": "dense-rack", "x":  24, "y": 345 },
					{ "name": "R62",    "type": "dense-rack", "x":  47, "y": 345 },
					{ "name": "R63",    "type": "dense-rack", "x":  70, "y": 345 },
					{ "name": "CDU60",  "type": "vCDU",       "x":  93, "y": 345 },
					{ "name": "CDU61",  "type": "vCDU",       "x": 103, "y": 345 },
					{ "name": "R64",    "type": "dense-rack", "x": 113, "y": 345 },
					{ "name": "R65",    "type": "dense-rack", "x": 136, "y": 345 },
					{ "name": "R66",    "type": "dense-rack", "x": 159, "y": 345 },
					{ "name": "R67",    "type": "dense-rack", "x": 182, "y": 345 },
					{ "name": "R68",    "type": "dense-rack", "x": 205, "y": 345 },
					{ "name": "R69",    "type": "dense-rack", "x": 228, "y": 345 },
					{ "name": "R6A",    "type": "dense-rack", "x": 251, "y": 345 },
					{ "name": "R6B",    "type": "dense-rack", "x": 274, "y": 345 },
					{ "name": "CDU62",  "type": "vCDU",       "x": 297, "y": 345 },
					{ "name": "CDU63",  "type": "vCDU",       "x": 307, "y": 345 },
					{ "name": "R6C",    "type": "dense-rack", "x": 317, "y": 345 },
					{ "name": "R6D",    "type": "dense-rack", "x": 340, "y": 345 },
					{ "name": "R6E",    "type": "dense-rack", "x": 363, "y": 345 },
					{ "name": "R6F",    "type": "dense-rack", "x": 386, "y": 345 },
										
					{ "name": "R70",    "type": "dense-rack", "x":   1, "y": 387 },
					{ "name": "R71",    "type": "dense-rack", "x":  24, "y": 387 },
					{ "name": "R72",    "type": "dense-rack", "x":  47, "y": 387 },
					{ "name": "R73",    "type": "dense-rack", "x":  70, "y": 387 },
					{ "name": "CDU70",  "type": "vCDU",       "x":  93, "y": 387 },
					{ "name": "CDU71",  "type": "vCDU",       "x": 103, "y": 387 },
					{ "name": "R74",    "type": "dense-rack", "x": 113, "y": 387 },
					{ "name": "R75",    "type": "dense-rack", "x": 136, "y": 387 },
					{ "name": "R76",    "type": "dense-rack", "x": 159, "y": 387 },
					{ "name": "R77",    "type": "dense-rack", "x": 182, "y": 387 },
					{ "name": "R78",    "type": "dense-rack", "x": 205, "y": 387 },
					{ "name": "R79",    "type": "dense-rack", "x": 228, "y": 387 },
					{ "name": "R7A",    "type": "dense-rack", "x": 251, "y": 387 },
					{ "name": "R7B",    "type": "dense-rack", "x": 274, "y": 387 },
					{ "name": "CDU72",  "type": "vCDU",       "x": 297, "y": 387 },
					{ "name": "CDU73",  "type": "vCDU",       "x": 307, "y": 387 },
					{ "name": "R7C",    "type": "dense-rack", "x": 317, "y": 387 },
					{ "name": "R7D",    "type": "dense-rack", "x": 340, "y": 387 },
					{ "name": "R7E",    "type": "dense-rack", "x": 363, "y": 387 },
					{ "name": "R7F",    "type": "dense-rack", "x": 386, "y": 387 },
										
					{ "name": "R80",    "type": "dense-rack", "x":   1, "y": 429 },
					{ "name": "R81",    "type": "dense-rack", "x":  24, "y": 429 },
					{ "name": "R82",    "type": "dense-rack", "x":  47, "y": 429 },
					{ "name": "R83",    "type": "dense-rack", "x":  70, "y": 429 },
					{ "name": "CDU80",  "type": "vCDU",       "x":  93, "y": 429 },
					{ "name": "CDU81",  "type": "vCDU",       "x": 103, "y": 429 },
					{ "name": "R84",    "type": "dense-rack", "x": 113, "y": 429 },
					{ "name": "R85",    "type": "dense-rack", "x": 136, "y": 429 },
					{ "name": "R86",    "type": "dense-rack", "x": 159, "y": 429 },
					{ "name": "R87",    "type": "dense-rack", "x": 182, "y": 429 },
					{ "name": "R88",    "type": "dense-rack", "x": 205, "y": 429 },
					{ "name": "R89",    "type": "dense-rack", "x": 228, "y": 429 },
					{ "name": "R8A",    "type": "dense-rack", "x": 251, "y": 429 },
					{ "name": "R8B",    "type": "dense-rack", "x": 274, "y": 429 },
					{ "name": "CDU82",  "type": "vCDU",       "x": 297, "y": 429 },
					{ "name": "CDU83",  "type": "vCDU",       "x": 307, "y": 429 },
					{ "name": "R8C",    "type": "dense-rack", "x": 317, "y": 429 },
					{ "name": "R8D",    "type": "dense-rack", "x": 340, "y": 429 },
					{ "name": "R8E",    "type": "dense-rack", "x": 363, "y": 429 },
					{ "name": "R8F",    "type": "dense-rack", "x": 386, "y": 429 },
										
					{ "name": "R90",    "type": "dense-rack", "x":   1, "y": 471 },
					{ "name": "R91",    "type": "dense-rack", "x":  24, "y": 471 },
					{ "name": "R92",    "type": "dense-rack", "x":  47, "y": 471 },
					{ "name": "R93",    "type": "dense-rack", "x":  70, "y": 471 },
					{ "name": "CDU90",  "type": "vCDU",       "x":  93, "y": 471 },
					{ "name": "CDU91",  "type": "vCDU",       "x": 103, "y": 471 },
					{ "name": "R94",    "type": "dense-rack", "x": 113, "y": 471 },
					{ "name": "R95",    "type": "dense-rack", "x": 136, "y": 471 },
					{ "name": "R96",    "type": "dense-rack", "x": 159, "y": 471 },
					{ "name": "R97",    "type": "dense-rack", "x": 182, "y": 471 },
					{ "name": "R98",    "type": "dense-rack", "x": 205, "y": 471 },
					{ "name": "R99",    "type": "dense-rack", "x": 228, "y": 471 },
					{ "name": "R9A",    "type": "dense-rack", "x": 251, "y": 471 },
					{ "name": "R9B",    "type": "dense-rack", "x": 274, "y": 471 },
					{ "name": "CDU92",  "type": "vCDU",       "x": 297, "y": 471 },
					{ "name": "CDU93",  "type": "vCDU",       "x": 307, "y": 471 },
					{ "name": "R9C",    "type": "dense-rack", "x": 317, "y": 471 },
					{ "name": "R9D",    "type": "dense-rack", "x": 340, "y": 471 },
					{ "name": "R9E",    "type": "dense-rack", "x": 363, "y": 471 },
					{ "name": "R9F",    "type": "dense-rack", "x": 386, "y": 471 },
										
					{ "name": "RA0",    "type": "dense-rack", "x":   1, "y": 513 },
					{ "name": "RA1",    "type": "dense-rack", "x":  24, "y": 513 },
					{ "name": "RA2",    "type": "dense-rack", "x":  47, "y": 513 },
					{ "name": "RA3",    "type": "dense-rack", "x":  70, "y": 513 },
					{ "name": "CDUA0",  "type": "vCDU",       "x":  93, "y": 513 },
					{ "name": "CDUA1",  "type": "vCDU",       "x": 103, "y": 513 },
					{ "name": "RA4",    "type": "dense-rack", "x": 113, "y": 513 },
					{ "name": "RA5",    "type": "dense-rack", "x": 136, "y": 513 },
					{ "name": "RA6",    "type": "dense-rack", "x": 159, "y": 513 },
					{ "name": "RA7",    "type": "dense-rack", "x": 182, "y": 513 },
					{ "name": "RA8",    "type": "dense-rack", "x": 205, "y": 513 },
					{ "name": "RA9",    "type": "dense-rack", "x": 228, "y": 513 },
					{ "name": "RAA",    "type": "dense-rack", "x": 251, "y": 513 },
					{ "name": "RAB",    "type": "dense-rack", "x": 274, "y": 513 },
					{ "name": "CDUA2",  "type": "vCDU",       "x": 297, "y": 513 },
					{ "name": "CDUA3",  "type": "vCDU",       "x": 307, "y": 513 },
					{ "name": "RAC",    "type": "dense-rack", "x": 317, "y": 513 },
					{ "name": "RAD",    "type": "dense-rack", "x": 340, "y": 513 },
					{ "name": "RAE",    "type": "dense-rack", "x": 363, "y": 513 },
					{ "name": "RAF",    "type": "dense-rack", "x": 386, "y": 513 },
										
					{ "name": "RB0",    "type": "dense-rack", "x":   1, "y": 555 },
					{ "name": "RB1",    "type": "dense-rack", "x":  24, "y": 555 },
					{ "name": "RB2",    "type": "dense-rack", "x":  47, "y": 555 },
					{ "name": "RB3",    "type": "dense-rack", "x":  70, "y": 555 },
					{ "name": "CDUB0",  "type": "vCDU",       "x":  93, "y": 555 },
					{ "name": "CDUB1",  "type": "vCDU",       "x": 103, "y": 555 },
					{ "name": "RB4",    "type": "dense-rack", "x": 113, "y": 555 },
					{ "name": "RB5",    "type": "dense-rack", "x": 136, "y": 555 },
					{ "name": "RB6",    "type": "dense-rack", "x": 159, "y": 555 },
					{ "name": "RB7",    "type": "dense-rack", "x": 182, "y": 555 },
					{ "name": "RB8",    "type": "dense-rack", "x": 205, "y": 555 },
					{ "name": "RB9",    "type": "dense-rack", "x": 228, "y": 555 },
					{ "name": "RBA",    "type": "dense-rack", "x": 251, "y": 555 },
					{ "name": "RBB",    "type": "dense-rack", "x": 274, "y": 555 },
					{ "name": "CDUB2",  "type": "vCDU",       "x": 297, "y": 555 },
					{ "name": "CDUB3",  "type": "vCDU",       "x": 307, "y": 555 },
					{ "name": "RBC",    "type": "dense-rack", "x": 317, "y": 555 },
					{ "name": "RBD",    "type": "dense-rack", "x": 340, "y": 555 },
					{ "name": "RBE",    "type": "dense-rack", "x": 363, "y": 555 },
					{ "name": "RBF",    "type": "dense-rack", "x": 386, "y": 555 }
				]
			} /* floor */
		}, /* Full view */
		"Rack": {
			"view": "Rack",
			"view-description": "Rack Layout",
			"initzoom": 0,
			"zoomscales": [16],
			"types": {  /* Q: should types be common (outside the view def)?  Both? */
				"dense-rack" : {
				  "description": "Dense compute rack",
				  "width": 20, "height": 39, "obscured":true,
				  "content" : [
					{"name": "CH0", "type": "dense-chassis", "x":0, "y":0},
					{"name": "CH1", "type": "dense-chassis", "x":0, "y":10},
					{"name": "CH2", "type": "dense-chassis", "x":0, "y":20},
					{"name": "CH3", "type": "dense-chassis", "x":0, "y":30}
				  ]
				},
				"dense-chassis": {
				  "description": "Dense compute chassis",
				  "width": 20, "height": 9, "obscured":true,
				  "content" : [
					{"name": "CB0", "type": "dense-blade", "x":2 , "y":0},
					{"name": "CB1", "type": "dense-blade", "x":3 , "y":0},
					{"name": "CB2", "type": "dense-blade", "x":4 , "y":0},
					{"name": "CB3", "type": "dense-blade", "x":5 , "y":0},
					{"name": "CB4", "type": "dense-blade", "x":6 , "y":0},
					{"name": "CB5", "type": "dense-blade", "x":7 , "y":0},
					{"name": "CB6", "type": "dense-blade", "x":8 , "y":0},
					{"name": "CB7", "type": "dense-blade", "x":9 , "y":0},
					{"name": "CB8", "type": "dense-blade", "x":10, "y":0},
					{"name": "CB9", "type": "dense-blade", "x":11, "y":0},
					{"name": "CBA", "type": "dense-blade", "x":12, "y":0},
					{"name": "CBB", "type": "dense-blade", "x":13, "y":0},
					{"name": "CBC", "type": "dense-blade", "x":14, "y":0},
					{"name": "CBD", "type": "dense-blade", "x":15, "y":0},
					{"name": "CBE", "type": "dense-blade", "x":16, "y":0},
					{"name": "CBF", "type": "dense-blade", "x":17, "y":0},
					{"name": "SW0", "type": "dense-swblade", "x":0 , "y":0},
					{"name": "SW1", "type": "dense-swblade", "x":0 , "y":1},
					{"name": "SW2", "type": "dense-swblade", "x":0 , "y":2},
					{"name": "SW3", "type": "dense-swblade", "x":0 , "y":3},
					{"name": "SW4", "type": "dense-swblade", "x":0 , "y":4},
					{"name": "SW5", "type": "dense-swblade", "x":0 , "y":5},
					{"name": "SW6", "type": "dense-swblade", "x":0 , "y":6},
					{"name": "SW7", "type": "dense-swblade", "x":0 , "y":7},
					{"name": "SW8", "type": "dense-swblade", "x":18, "y":0},
					{"name": "SW9", "type": "dense-swblade", "x":18, "y":1},
					{"name": "SWA", "type": "dense-swblade", "x":18, "y":2},
					{"name": "SWB", "type": "dense-swblade", "x":18, "y":3},
					{"name": "SWC", "type": "dense-swblade", "x":18, "y":4},
					{"name": "SWD", "type": "dense-swblade", "x":18, "y":5},
					{"name": "SWE", "type": "dense-swblade", "x":18, "y":6},
					{"name": "SWF", "type": "dense-swblade", "x":18, "y":7},
					{"name": "RF0", "type": "dense-rectifier", "x":1 , "y":8},
					{"name": "RF1", "type": "dense-rectifier", "x":3 , "y":8},
					{"name": "RF2", "type": "dense-rectifier", "x":5 , "y":8},
					{"name": "RF3", "type": "dense-rectifier", "x":7 , "y":8},
					{"name": "CMM", "type": "dense-cmm",       "x":9 , "y":8},
					{"name": "RF4", "type": "dense-rectifier", "x":11, "y":8},
					{"name": "RF5", "type": "dense-rectifier", "x":13, "y":8},
					{"name": "RF6", "type": "dense-rectifier", "x":15, "y":8},
					{"name": "RF7", "type": "dense-rectifier", "x":17, "y":8}
				  ]
				},
				"dense-blade": {
				  "description": "Dense compute blade",
				  "width": 1, "height": 8, "obscured":true,
				  "content" : [
					{"name": "PM0", "type": "dense-processor-module", "x":0, "y":0},
					{"name": "PM1", "type": "dense-processor-module", "x":0, "y":2},
					{"name": "PM2", "type": "dense-processor-module", "x":0, "y":4},
					{"name": "PM3", "type": "dense-processor-module", "x":0, "y":6}
				  ]
				},
				"dense-processor-module": {
				  "description": "Dense compute processor module",
				  "width": 1, "height": 2, "obscured":true,
				  "content" : [
					{"name": "CN0", "type": "dense-compute-node", "x":0, "y":0},
					{"name": "CN1", "type": "dense-compute-node", "x":0, "y":1}
				  ]
				},
				"dense-compute-node": {
				  "description": "Dense compute processor node",
				  "width": 1, "height": 1,
				  "content" : []
				},
				"dense-swblade": {
				  "description": "Dense switch blade",
				  "width": 2, "height": 1,
				  "content" : []
				},
				"dense-rectifier": {
				  "description": "Dense chassis power rectifier",
				  "width": 2, "height": 1,
				  "content" : []
				},
				"dense-cmm": {
				  "description": "Dense chassis management module",
				  "width": 2, "height": 1,
				  "content" : []
				},
			}, /* types */
			"floor" : {
				"description": "The full floor map",
				"width" : 24, "height" : 43,
				"content" : [  /* 23 unit rack pitch.  42 unit row pitch */
					{ "name": "R00",    "type": "dense-rack", "x":   1, "y":   1 }
				]
			} /* floor */
		} /* Rack view */
	} /* views */
}
//JSON-end
;

