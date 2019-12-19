#!/usr/bin/env bash

[[ -z "${DAI_LOGGING_LEVEL}" ]] && DAI_LOGGING_LEVEL="INFO"

LOCATION="${SERVICE_LOCATION}"
LOG="/opt/ucs/log/Eventsim-${LOCATION}.log"
ARGS=""
CLASS=com.intel.dai.eventsim.EventSimApiApp
PROPS="-DdaiLoggingLevel=${DAI_LOGGING_LEVEL}"

hostname ${HOSTNAME}

java -DdaiLoggingLevel=DEBUG -cp /opt/ucs/lib/\* ${PROPS} ${CLASS} ${ARGS} 2>&1 >${LOG}
