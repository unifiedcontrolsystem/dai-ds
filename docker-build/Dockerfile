# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

# Use OpenSuse Leap 15.1 for the base image...
FROM opensuse/leap:15.1

# Get arguments from docker build (in the docker-build.sh script)...
ARG PROXY
ARG UID
ARG GID

# Set environment variables...
ENV http_proxy $PROXY
ENV HTTP_PROXY $PROXY
ENV HTTPS_PROXY $PROXY
ENV new_uid $UID
ENV new_gid $GID

# Install required packages...
RUN zypper install -y git rpm-build python3 python3-devel python3-pip python3-setuptools python3-wheel \
                      java-1_8_0-openjdk java-1_8_0-openjdk-devel java-11-openjdk java-11-openjdk-devel

# Update setuptools and pip to the latest versions...
RUN python3 -m pip install -U pip setuptools PyYAML

# Create a User and group to preserve permisions for the user of the container...
RUN groupadd -o -g "${new_gid}" builder; exit 0
RUN useradd -M -s /bin/bash -d /sandbox -u "${new_uid}" -g "${new_gid}" builder

# Create and setup the entry point and build sandbox for the user...
RUN mkdir -p /sandbox /repo
COPY entrypoint.sh /sandbox
RUN chown -R $UID:$GID /sandbox

# Run the container as the user...
USER $UID:$GID

# Define the entry point script and default arguments (overridden on docker script commandline)...
ENTRYPOINT [ "/sandbox/entrypoint.sh" ]
CMD ["clean", "build"]
