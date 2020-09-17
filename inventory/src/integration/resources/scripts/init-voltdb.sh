#!/bin/bash

voltadmin shutdown --force || true

if [ "$USER" == "jenkins" ]; then
    echo "*** Running on IT VM"
else
    sudo bash -c "echo never > /sys/kernel/mm/transparent_hugepage/enabled"
    sudo bash -c "echo never > /sys/kernel/mm/transparent_hugepage/defrag"
fi

voltdb init --force
