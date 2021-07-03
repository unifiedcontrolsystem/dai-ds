#!/bin/bash

docker stop node1
docker rm node1
docker run -d -P -e HOST_COUNT=1 -e HOSTS=node1 -p 21212:21212 -p 8081:8080 --name=node1 --network=voltLocalCluster voltdb/voltdb-community
sleep 12
sqlcmd < inventory.sql
