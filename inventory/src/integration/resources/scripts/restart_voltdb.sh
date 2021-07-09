#!/bin/bash

docker-compose -f docker-compose/db.yml down
docker-compose -f docker-compose/db.yml up -d
sleep 15
createdb --host=css-centos-8-00.ra.intel.com --username=postgres dai
psql --host=css-centos-8-00.ra.intel.com --username=postgres --dbname=dai < postgres_inventory.sql
psql --host=css-centos-8-00.ra.intel.com --username=postgres --dbname=dai < postgres_inventory_data.sql
sqlcmd < inventory.sql
