*** Copyright (C) 2019 Intel Corporation
***
*** SPDX-License-Identifier: Apache-2.0

Prerequisites:
===============
0. System must have at least 32GB or RAM to run smoothly.
1. Docker version 17+
2. Docker has access internet for repo access
3. The account being used has permission to use docker
4. Docker Compose version 1.8+ is installed.
5. No component of UCS is running on the host.

Install:
=========
0. cd to the repo root for the software
1. Using the gradle wrapper build the software:
    $ ./gradlew build
2. Copy the created tarball to a location to extract it (will create a "docker" folder).
    $ cp build/distributions/dai-docker-dev-{version}.tar.gz {your test folder}
3. cd to the target folder and extract the tarball:
    $ tar -xf dai-docker-dev-{version}.tar.gz
    $ cd docker
4. Jump to Use section.

Use:
=====
1. "cd" to the extracted "docker" folder (see Install).
2. Run "docker-compose up -d" to start the full cluster.
    a. Use "docker-compose down" to stop and remove the containers.

Using Just Postgres, VoltDB, and RabbitMQ:
===========================================
1. "cd" to the same folder as this README.txt file.
    a. Make sure ALL the configuration is correct for running in this hybrid mode with DAI on host.
2. Run "docker-compose --file docker-compose-third-party-only up -d" to start the cluster.
    a. Use "docker-compose --file docker-compose-third-party-only down" to stop and remove the containers.


NOTE: You cannot use the source folder "docker-dev-cluster" directly from
      the source tree! Please extract the tarball and use that instead.
