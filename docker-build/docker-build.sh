#!/bin/sh -x
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

# Check for docker and fail if missing.
which docker >/dev/null
have_docker=$?

if [[ ${have_docker} -ne 0 ]]; then
  echo "Error: The 'docker' program was not found, do you need to install it?" >&2
  exit 1
fi

cd $(dirname ${0})

# Create the persistent storage folder for the docker container.
mkdir -p ~/.gradle/docker

# Parse options
build_docker="false"
[[ "${1}" == "--rebuild-docker" ]] && build_docker="true" && shift

# Conditionally build the docker container if not already built.
if [[ -z "$(docker images | grep dai_builder)" || "${build_docker}" == "true" ]]; then
  cat <<EOF1 >~/.gradle/docker/gradle.properties
systemProp.ucs.includeDbgSymbols=true
systemProp.java8.bootstrapClasspath=/usr/lib64/jvm/java-1.8.0
org.gradle.daemon=false
org.gradle.console=plain
org.gradle.parallel=true
EOF1
  if [[ -n "${http_proxy}" ]]; then
    build_proxy="--build-arg PROXY=${http_proxy}"
    p1="${http_proxy##*://}"
    p1="${p1%/}"
    cat <<EOF2 >>~/.gradle/docker/gradle.properties
systemProp.http.proxyHost=${p1%%:*}
systemProp.http.proxyPort=${p1##*:}
systemProp.https.proxyHost=${p1%%:*}
systemProp.https.proxyPort=${p1##*:}
EOF2
  fi
  user_info="--build-arg UID=$(id -u) --build-arg GID=$(id -g)"
  docker build --no-cache=${build_docker} --tag dai_builder:latest ${build_proxy} ${user_info} .
  if [[ $? -ne 0 ]]; then
    echo "Error: failed to build the required docker image!" >&2
    rm -f gradle.properties
    exit 1
  fi
  rm -f gradle.properties
fi

# The container need access to the source tree root and the persistent storage for gradle...
mounts="--mount type=bind,src=$(pwd)/..,dst=/repo --mount type=bind,src=${HOME}/.gradle/docker,dst=/sandbox/.gradle"

# Execute the build under the docker container 'dai_builder'.
docker run --rm ${mounts} dai_builder:latest $@
rv=$?

# Exit with build status from docker.
exit ${rv}
