#!/usr/bin/python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

import json

fd = open("floor-layout.json")
floorLayout = json.load(fd)

for r in floorLayout['rackinfo']:
	print r['name'], r['type']
