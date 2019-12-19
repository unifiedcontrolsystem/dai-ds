# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

floor-layout.json: floor-layout.js
	 sed '1,/JSON-start/d;/JSON-end/,$$d;s,/\*.*\*/,,g' floor-layout.js > floor-layout.json
