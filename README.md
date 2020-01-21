What is DAI/DS?
====================

Thank you for your interest in DAI/DS!

DAI/DS (Data Access Interface and Data Store) is a data-centric framework supporting a suite of domain-specific adapters that organizes data gathered from components. The data store is a multi-tier "container" that hosts data defined by UCS (Unified Control System) and made available to components.

DAI/DS and UCS
====================

The Unified Control System (UCS) is intended to be part of a comprehensive software solution for the deployment and management of High Performance Computing (HPC) systems. The control system not only provides the system environment for the execution of applications and their workflows, but also provides the obvious point of control and service for administrators and operation staff who must configure, manage, track, tune, interpret and service the system to maximize availability of resource for the applications. 

UCS is a flexible software stack that assists development of new designs by OEMs and system vendors, often from new hardware targeted specifically for HPC. A unique feature of UCS, as compared to previous systems, is integration provided by a data-centric architecture. The control system gathers, organizes, and tracks system data to provide an accurate view of the system at all times. The data is organized by a Data Access Interface (DAI) component, and stored into the Data Store (DS) component. The DAI is an integrating component and is able to glean much of the data it organizes from its interaction with the other components.

https://unifiedcontrolsystem.github.io/


Travis CI &nbsp;&nbsp;&nbsp;&nbsp;[![Build Status](https://travis-ci.org/unifiedcontrolsystem/dai-ds.svg?branch=master)](https://travis-ci.org/unifiedcontrolsystem/dai-ds)
====================

The DAI/DS public GitHub project has been integrated with Travis continuous integration.

All pull requests will be built and tested automatically by Travis.


Building Instructions
=====================
DAI-DS is a Java 11 application with Python 3.6 command line tool(s). Both
methods require internet access to correctly build.

There are 2 ways to build DAI. The first is using docker and is by far the
most portable way. The second way is to use a host and install all the
prerequisites needed to do the build. Access to the internet is required in
both cases but proxies are allowed. In both build cases, the output will
appear in <repo_root>/build/ folder will be populated and the distribution
files location in <repo_root>/build/distributions/.

Docker Build
------------------------------------------------------------------------------
This is by far the most portable method. First install and configure docker.
The details depend on the specific OS distribution. The general
steps are:
1. Install and configure the docker package via the package manager on the
     OS (docker has documentation on this: https://docs.docker.com/).
   * Make sure the installed service is running and has internet access
   * Configure all docker users

2. Get and extract the DAI source tarball or another method to get the DAI
     source tree.
3. 'cd' to the extracted source tree's root.
4. In the source tree at the root do the following (Any proxy in
     'http_proxy' environment variable will be honored by the docker build
      script and execution):
    ```bash
    $ docker-build/docker-build.sh build
    ```
    or
    ```bash
    $ ./gradlew dockerBuild
    ```

This will build a docker image called 'dai_builder', then run the
container doing the ./gradlew build inside. The gradle persistent files
for the container will be stored at ~/.gradle/docker. This will save time
for subsequent builds.

Host Build
------------------------------------------------------------------------------
This is more complicated but is more useful for developers.
1. Make sure the host contains the following packages (actual names vary by
     OS distribution, the names below are for OpenSuse Leap 15.1):
    ```
    git
    rpm-build
    python3
    python3-devel
    python3-pip
    python3-setuptools
    java-11-openjdk
    java-11-openjdk-devel
    ```
__NOTE:__ Examine the docker-build/Dockerfile to how its done for a docker
    container.

2. If a proxy is required for external access then please make sure of the
     following items:
    * Make sure gradle can access the internet by placing the updated lines
         in the ~/.gradle/gradle.properties file:
    ```
    systemProp.http.proxyHost=<proxy_host>
    systemProp.http.proxyPort=<proxy_port>
    systemProp.https.proxyHost=<proxy_host>
    systemProp.https.proxyPort=<proxy_port>
    systemProp.https.proxyUser=<proxy_user_if_needed>
    systemProp.https.proxyPassword=<proxy_password_if_needed>
    ```
    * Python 3's pip requires the proxy variable 'HTTPS_PROXY' is set to
        the proxy of the environment the host is inside.
3. Set the following properties in the ~/.gradle/gradle.properties file:
    ```
    systemProp.includeDebugSymbols=true
    ```
4. Get and extract the DAI source tarball or another method to get the DAI
  source tree.
5. 'cd' to the extracted source tree's root.
6. Build the deployable components.
    ```bash
    $ ./gradlew build
    ```


Run Requirements and Instructions (Deployment)
=================================================
Installers to use after build:
```
build/distributions/install-docker_cp4_3rd_party_{version}.sh
build/distributions/install-docker_cp4_dai_{version}.sh
```

These install package includes 4 services to control docker-compose
containers. One each for postgres, voltdb, rabbitmq, and dai. It assumes all
adapters run on one system.

Pre-requisites:
----------------
1. System must have at least 32GB of RAM to run smoothly.
2. docker installed and configured for the root user.
3. docker-compose installed.
4. ALL configuration files changed to support the target system. This includes:
    * SystemManifest.json
    * MachineConfig.json
    * NearlineConfig.json
    * ProviderMonitoringNetworkForeignBus.json
    * ProviderProvisionerNetworkForeignBus.json
    * LocationTranslationMap.json - If the contents of this file do not correleate with
                                    the MachineConfig.json file's contents then the
                                    provisioning and monitoring providers may not
                                    function as expected. Make sure ALL Foreign
                                    names match a DAI name and no "location" is missing
                                    or unexpected.
5. The API (or eventsim) must be available for the
   ProviderMonitoringNetworkForeignBus and ProviderProvisionerNetworkForeignBus
   to connect.
6. If the deployment system does not have internet access you must download and save the following docker images from Docker Hub then copy and reload them onto the target system's local docker repository:
    * postgres:11.4
    * rabbitmq:3.7-management
    * voltdb/voltdb-community:9.2.1
    * openjdk:11.0.5-jdk


Installing DAI and Third Party Components:
-------------------------------------------
1. As root run the third party installer:
    ```bash
    # install-docker_cp4_3rd_party_{version}.sh
    ```
2. As root run the dai installer:
    ```bash
    # install-docker_cp4_dai_{version}.sh
    ```
__NOTE:__ The installed file tree is located at `/opt/dai-docker`.

__NOTE:__ After installation of the DAI components all systemd services are enabled
but not started automatically.

Starting/Stopping Services:
----------------------------
1. sudo systemctl start dai-postgres
2. sudo systemctl start dai-rabbitmq
3. sudo systemctl start dai-voltdb
4. sudo systemctl start dai-manager

__NOTE:__ If you start only the dai-manager service then all other services will
      start as dependencies if not already running but may experience timing issues and not start up properly.

These services all use the /opt/dai-docker/*.yml files for docker-compose
launching.  _Do not use docker or docker-compose directly, without first stopping the services and disabling them_. Attempting to stop a container directly will just cause it to restart as the service is set to restart the container on container exit.

Stopping or restarting is done as all services or all docker-compose. _Do not mix these control techniques_.


Uninstalling DAI and Third Party Components
--------------------------------------------
Note: The following commands will stop the services if they are running.

1. As root run the installer with "-U" to uninstall DAI.
    ```bash
    # install-docker_deploy_dai_{version}.sh -U
    ```
2. As root run the installer with "-U" to uninstall third party components.
    ```bash
    # install-docker_deploy_3rd_party_{version}.sh -U
    ```


Notes:
-------
* Postgres persistent data is stored in /opt/dai-docker/tier2/data/pgdata
    + To clear the data and start over do the following as root:
        ```bash
        # systemctl stop dai-manager dai-voltdb dai-rabbitmq dai-postgres
        # rm -rf /opt/dai-docker/tier2/data/pgdata
        ```
    + To backup the data (as root):
        ```bash
        # systemctl stop dai-manager dai-voltdb dai-rabbitmq dai-postgres
        # recursively copy/tar /opt/dai-docker/tier2/data/pgdata
        ```
* If you start all services together with one command, expect errors and
  restarts of the dai-manager service until all other services are running.
