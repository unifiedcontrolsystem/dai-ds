#!/bin/bash

docker-compose -f docker-compose/db.yml down
docker-compose -f docker-compose/db.yml up -d
sleep 15
PGPASSWORD=postgres createdb --host=css-centos-8-00.ra.intel.com --username=postgres dai
PGPASSWORD=postgres psql --host=css-centos-8-00.ra.intel.com --username=postgres --dbname=dai < postgres_inventory.sql
sqlcmd < inventory.sql
