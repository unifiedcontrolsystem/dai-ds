#!/bin/bash
set -ex

voltadmin shutdown --force || true
voltdb init --force

# JENKINS_NODE_COOKIE=dontKillMe nohup voltdb start --http 8081 &
voltdb start --http 8081 &

total_time=6
sleep $total_time
for c in {0..30}
do
    if voltadmin status --host=localhost:21212; then
        echo "voltdb started after $total_time seconds"
        sqlcmd < $scriptDir/load_procedures.sql >> $tmpDir/voltdb.log
        sqlcmd < data/db/DAI-Volt-Tables.sql >> $tmpDir/voltdb.log
        sqlcmd < data/db/DAI-Volt-Procedures.sql >> $tmpDir/voltdb.log
        sqlcmd < inventory/src/integration/resources/scripts/inventory.sql >> $tmpDir/voltdb.log
        exit 0
    fi
    echo "voltdb still starting: $total_time seconds so far ..."
    sleep 1
    total_time=$((total_time + 1))
done
echo 'Timed out while waiting for voltdb to start'
exit 1
