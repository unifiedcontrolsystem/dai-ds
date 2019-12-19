#!/bin/bash
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

function is_running() {
  voltadmin validate >/dev/null 2>&1
  return $?
}

function add_jar_file() {
  sqlcmd --query="load classes ${1};"
  return $?
}

function add_sql_file() {
  sqlcmd <$1
  rv=$?
  [[ $rv -ne 0 ]] && echo "exec SetDbSchemaFailed 'Error: Loading SQL file ${1}!'" | sqlcmd
  return $rv
}

function wait_for_start() {
  is_running
  while [ $? -ne 0 ]; do is_running; done
}

function wait_for_stop() {
  is_running
  while [ $? -eq 0 ]; do sleep 5; is_running; done
}

[[ -z "${HTTP_PORT}" ]] && HTTP_PORT=8080

cd /opt/voltdb/schema
JARS=$(ls *.jar | sort)
cd -
echo "*** JAR Manifest:"
echo ${JARS}

cd /opt/voltdb/schema
SQLS=$(ls *.sql | sort)
cd -
echo "*** SQL Files To Load:"
echo $SQLS

errors=0
echo "Initializing VoltDB..."
voltdb init --force -D /var/voltdb
errors=$(expr $errors + $?)

echo "*** Starting VoltDB..."
[[ $errors -eq 0 ]] && voltdb start -B -D /var/voltdb --count=1 --http=${HTTP_PORT}
errors=$(expr $errors + $?)

echo "*** Waiting for VoltDB..."
wait_for_start
errors=$(expr $errors + $?)

echo "*** Loading jar file classes..."
for file in $JARS; do
  [[ $errors -eq 0 ]] && add_jar_file "/opt/voltdb/schema/${file}"
  errors=$(expr $errors + $?)
done

echo "*** Loading schema files..."
for file in $SQLS; do
  [[ $errors -eq 0 ]] && add_sql_file "/opt/voltdb/schema/${file}"
  errors=$(expr $errors + $?)
done

echo "*** Waiting for VoltDB to finish..."
[[ $errors -eq 0 ]] && wait_for_stop
errors=$(expr $errors + $?)

exit $errors
