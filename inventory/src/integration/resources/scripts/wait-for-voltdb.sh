#!/bin/bash

total_time=6  # seems to be very fast
sleep $total_time
for c in {0..30}
do
    if voltadmin status --host=localhost:$1; then
        echo "voltdb started after $total_time seconds"
        exit 0
    fi
    echo "voltdb still starting: $total_time seconds so far ..."
    sleep 1
    total_time=$((total_time + 1))
done
exit 1
