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

JARS=$(cat /opt/voltdb/jars/manifest.txt)
echo "*** JAR Manifest:"
cat  /opt/voltdb/jars/manifest.txt

SQLS=$(ls /opt/voltdb/schema/*.sql | sort)
echo "*** SQL Files To Load:"
echo $SQLS

errors=0
echo "Initializing VoltDB..."
voltdb init -D /var/voltdb
errors=$(expr $errors + $?)

echo "*** Starting VoltDB..."
[[ $errors -eq 0 ]] && voltdb start -B -D /var/voltdb --count=1
errors=$(expr $errors + $?)

echo "*** Waiting for VoltDB..."
wait_for_start
errors=$(expr $errors + $?)

echo "*** Loading jar file classes..."
for file in $JARS; do
  [[ $errors -eq 0 ]] && add_jar_file "/opt/voltdb/jars/${file}"
  errors=$(expr $errors + $?)
done

echo "*** Loading schema files..."
for file in $SQLS; do
  [[ $errors -eq 0 ]] && add_sql_file $file
  errors=$(expr $errors + $?)
done

echo "*** Waiting for VoltDB to finish..."
[[ $errors -eq 0 ]] && wait_for_stop
errors=$(expr $errors + $?)

exit $errors
