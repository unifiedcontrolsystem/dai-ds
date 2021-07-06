#!/bin/bash
set -ex

echo 'truncate table raw_DIMM;' | sqlcmd --servers=css-centos-8-00.ra.intel.com
echo 'truncate table raw_FRU_Host;' | sqlcmd --servers=css-centos-8-00.ra.intel.com
echo 'truncate table Raw_Node_Inventory_History;' | sqlcmd --servers=css-centos-8-00.ra.intel.com
