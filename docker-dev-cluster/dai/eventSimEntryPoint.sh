#!/usr/bin/env bash

[[ -z "${DAI_LOGGING_LEVEL}" ]] && DAI_LOGGING_LEVEL="INFO"

LOCATION="${SERVICE_LOCATION}"
LOG="/opt/ucs/log/Eventsim-${LOCATION}.log"
ARGS="localhost /opt/ucs/etc/EventSim.json"
CLASS=com.intel.dai.eventsim.EventSimApp
PROPS="-DdaiLoggingLevel=${DAI_LOGGING_LEVEL}"

hostname ${HOSTNAME}

java -DdaiLoggingLevel=DEBUG -cp /opt/ucs/lib/\* ${PROPS} ${CLASS} ${ARGS} 2>&1 >${LOG}
