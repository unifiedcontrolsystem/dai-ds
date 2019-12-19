#!/bin/sh -x
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

# Make sure Java home is set...
export JAVA_HOME="/usr/lib64/jvm/java"

# Copy repo to sandbox...
cp -r /repo/* /repo/.git /sandbox/

# Go into sandbox...
cd /sandbox 2>&1

# Run the gradle wrapper...
IN_DOCKER=true ./gradlew $@
rv=$?

# Copy even partial results back to the repo...
[[ -d "build" ]] && cp -rf build /repo/ 2>&1

# Return proper exit code to docker...
exit ${rv}
