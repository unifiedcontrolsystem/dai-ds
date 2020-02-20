#!/bin/bash

sleep 6     # voltdb takes about 7 seconds to start
for c in {0..20}
do
    if voltadmin status --host=localhost:$1; then
        break
    fi
    sleep 1
done