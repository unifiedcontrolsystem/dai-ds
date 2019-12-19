#!/usr/bin/env bash

[[ -z "${DAI_LOGGING_LEVEL}" ]] && DAI_LOGGING_LEVEL="INFO"

LOCATION="${SERVICE_LOCATION}"
LOG="/opt/ucs/log/AdapterDaiMgr-${LOCATION}.log"
ARGS="voltdb - ${HOSTNAME}"
CLASS=com.intel.dai.AdapterDaiMgr
PROPS="-Dlog4j.configurationFile=/opt/ucs/etc/log4j2.xml -DdaiLoggingLevel=${DAI_LOGGING_LEVEL}"

hostname ${HOSTNAME}

mkdir -p /opt/ucs
echo "*** Logging level: ${DAI_LOGGING_LEVEL}" >${LOG}
echo >>${LOG}

java -cp '/opt/ucs/lib/*' ${PROPS} ${CLASS} ${ARGS} 2>&1 >>${LOG}
