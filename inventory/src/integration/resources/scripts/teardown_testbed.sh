#!/bin/bash
set -ex

rm -f $etcDir/HWInvDiscoveryConfig.json"
docker-compose -f $scriptDir/json-server/json-server.yml down" || true
mkdir -p $tmpDir
source $scriptDir/stop-voltdb.sh >> $tmpDir/voltdb.log
