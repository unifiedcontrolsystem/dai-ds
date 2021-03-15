#!/bin/bash
set -ex

sudo mkdir -p $etcDir
sudo cp $dataDir/HWInvDiscoveryConfig.json $etcDir

mkdir -p $tmpDir
source $scriptDir/start-voltdb.sh > $tmpDir/voltdb.log
