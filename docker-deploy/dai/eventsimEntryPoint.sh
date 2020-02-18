#!/usr/bin/env bash

[[ -z "${DAI_LOGGING_LEVEL}" ]] && DAI_LOGGING_LEVEL="INFO"

LOG="/opt/ucs/log/EventSim-${HOSTNAME}.log"
ARGS=""
CLASS=com.intel.dai.eventsim.EventSimApp
PROPS="-DdaiLoggingLevel=${DAI_LOGGING_LEVEL}"

mkdir -p /opt/ucs
echo "*** Logging level: ${DAI_LOGGING_LEVEL}" >${LOG}
echo >>${LOG}

java -cp '/opt/ucs/lib/*' ${PROPS} ${CLASS} ${ARGS} 2>&1 >>${LOG}
