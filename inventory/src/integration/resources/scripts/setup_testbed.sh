#!/bin/bash
set -ex

# This is one way to setup a component testbed.
docker-compose -f $scriptDir/json-server/json-server.yml up -d

sudo mkdir -p $etcDir
sudo cp $dataDir/HWInvDiscoveryConfig.json $etcDir

mkdir -p $tmpDir
source $scriptDir/start-voltdb.sh > $tmpDir/voltdb.log
