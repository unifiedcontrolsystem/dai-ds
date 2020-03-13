#!/bin/bash
# Copyright (C) 2019-2020 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

LOGFILE="/var/voltdb/voltdbroot/log/voltdb-startup.log"

function is_running() {
  voltadmin validate >/dev/null 2>&1
  return ${?}
}

function add_jar_file() {
  sqlcmd --query="load classes ${1};" 2>&1 | tee -a "${LOGFILE}"
  return ${?}
}

function add_sql_file() {
  sqlcmd <"${1}" 2>&1 | tee -a "${LOGFILE}"
  rv=${?}
  [[ ${rv} -ne 0 ]] && echo "exec SetDbSchemaFailed 'Error: Loading SQL file ${1}!'" | sqlcmd 2>&1 | tee -a "${LOGFILE}"
  return ${rv}
}

function wait_for_start() {
  is_running
  while [ ${?} -ne 0 ]; do is_running; done
}

function wait_for_stop() {
  is_running
  while [ ${?} -eq 0 ]; do sleep 5; is_running; done
}

umask 077

[[ -z "${HTTP_PORT}" ]] && HTTP_PORT=8080
>"${LOGFILE}"

cd /opt/voltdb/schema
JARS=$(ls *.jar | sort)
cd -
echo "*** JAR Manifest:" | tee -a "${LOGFILE}"
echo "${JARS}" | tee -a "${LOGFILE}"

cd /opt/voltdb/schema
SQLS=$(ls *.sql | sort)
cd -
echo "*** SQL Files To Load:" | tee -a "${LOGFILE}"
echo "${SQLS}" | tee -a "${LOGFILE}"

errors=0
echo "Initializing VoltDB..." | tee -a "${LOGFILE}"
voltdb init --force -D /var/voltdb 2>&1 | tee -a "${LOGFILE}"
errors=$(expr ${errors} + ${?})

echo "*** Starting VoltDB..." | tee -a "${LOGFILE}"
[[ ${errors} -eq 0 ]] && voltdb start -B -D /var/voltdb --count=1 --http=${HTTP_PORT} 2>&1 | tee -a "${LOGFILE}"
errors=$(expr ${errors} + ${?})

echo "*** Waiting for VoltDB..." | tee -a "${LOGFILE}"
wait_for_start
errors=$(expr ${errors} + ${?})

echo "*** Loading jar file classes..." | tee -a "${LOGFILE}"
for file in $JARS; do
  [[ ${errors} -eq 0 ]] && add_jar_file "/opt/voltdb/schema/${file}"
  errors=$(expr ${errors} + ${?})
done

echo "*** Loading schema files..."
for file in $SQLS; do
  [[ $errors -eq 0 ]] && add_sql_file "/opt/voltdb/schema/${file}"
  errors=$(expr ${errors} + ${?})
done

echo "*** Waiting for VoltDB to finish..."
[[ ${errors} -eq 0 ]] && wait_for_stop
errors=$(expr ${errors} + ${?})

exit ${errors}
