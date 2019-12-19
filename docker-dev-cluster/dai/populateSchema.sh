#!/usr/bin/env bash

VOLTDB_HOSTNAME=localhost
[[ -z "${1}" ]] && VOLTDB_HOSTNAME="${1}"

LOG="/opt/ucs/log/populate_schema.log"
ARGS="${VOLTDB_HOSTNAME} /opt/ucs/etc/SystemManifest.json /opt/ucs/etc/MachineConfig.json"
ARGS="${ARGS} /opt/ucs/etc/RasEventMetaData.json"
JAR="$(ls /opt/ucs/lib/populate_schema-*.jar)"

java -jar ${JAR} ${ARGS} 2>&1 | tee ${LOG}

exit $?
