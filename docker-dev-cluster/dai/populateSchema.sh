#!/usr/bin/env bash

LOG="/opt/ucs/log/populate_schema.log"

echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" >${LOG}
echo "Running:  populateSchema $@" >>${LOG}
echo "   With:  VOLTDB_HOSTNAME=${VOLTDB_HOSTNAME}" >>${LOG}

[[ -z "${VOLTDB_HOSTNAME}" ]] && VOLTDB_HOSTNAME=localhost
[[ -n "${1}" ]] && VOLTDB_HOSTNAME="${1}"

ARGS="${VOLTDB_HOSTNAME} /opt/ucs/etc/SystemManifest.json /opt/ucs/etc/MachineConfig.json"
ARGS="${ARGS} /opt/ucs/etc/RasEventMetaData.json"
JAR="$(ls /opt/ucs/lib/populate_schema-*.jar)"

echo "Launching with arguments: ${ARGS}" >>${LOG}
echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" >>${LOG}
java -jar ${JAR} ${ARGS} 2>&1 | tee -a ${LOG}

exit $?
