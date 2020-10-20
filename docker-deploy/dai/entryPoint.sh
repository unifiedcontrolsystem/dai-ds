#!/usr/bin/env bash

[[ -z "${DAI_LOGGING_LEVEL}" ]] && DAI_LOGGING_LEVEL="INFO"

LOG="/opt/ucs/log/AdapterDaiMgr-${HOSTNAME}.log"
ARGS="${VOLTDB_SERVERS} - ${HOSTNAME}"
CLASS=com.intel.dai.AdapterDaiMgr
PROPS="-Dlog4j.configurationFile=/opt/ucs/etc/log4j2.xml -DdaiLoggingLevel=${DAI_LOGGING_LEVEL}"

mkdir -p /opt/ucs
echo "*** Logging level: ${DAI_LOGGING_LEVEL}" >${LOG}
echo >>${LOG}

java ${COVERAGE_OPTIONS} -cp '/opt/ucs/lib/*' ${PROPS} ${CLASS} ${ARGS} 2>&1 >>${LOG}
