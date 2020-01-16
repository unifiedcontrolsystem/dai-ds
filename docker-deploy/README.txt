Install and User Guide for DAI on CP4:
=======================================

This installed package includes 4 services to control docker-compose
containers. One each for postgres, voltdb, rabbitmq, and dai. It assumes all
adapters run on one system.


Pre-requisites:
----------------
1. System must have at least 32GB or RAM to run smoothly.
2. docker installed and configured for the root user.
3. docker-compose installed.
4. ALL configuration files changed to support the target system. This includes:
    a. SystemManifest.json
    b. MachineConfig.json
    c. NearlineConfig.json
    d. ProviderMonitoringNetworkForeignBus.json
    e. ProviderProvisionerNetworkForeignBus.json
    f. LocationTranslationMap.json - If this doesn't match the machine config
                                     The PartitionedMonitorAdapter may not
                                     function. Make sure to include ALL Foreign
                                     names.
5. The API (or eventsim) must be available for the
   ProviderMonitoringNetworkForeignBus and ProviderProvisionerNetworkForeignBus
   to connect.


Installing DAI and Third Party Components:
-------------------------------------------
1. As root run the third party installer:
    # install-docker_cp4_3rd_party_{version}.sh
2. As root run the dai installer:
    # install-docker_cp4_dai_{version}.sh


Starting/Stopping Services:
----------------------------
NOTE: After installation of the DAI components all services are enabled
but not started automatically.

1. sudo systemctl start dai-postgres
2. sudo systemctl start dai-rabbitmq
3. sudo systemctl start dai-voltdb
4. sudo systemctl start dai-manager

or

1. sudo systemctl start dai-postgres dai-rabbitmq dai-voltdb dai-manager

NOTE: If you start only the dai-manager service then all other services will
      start as dependencies if not already running.

These services all use the /opt/dai-docker/*.yml files for docker-compose
launching.  Do not use docker or docker-compose directly, use the services.
Attempting to stop a container directly will just restart it as the service
is set to restart the container.

Stopping or restarting is done as all services.


Uninstalling DAI and Third Party Components
--------------------------------------------
Note: The following commands will stop the services is they are running.

1. As root run the installer with "-U" to uninstall DAI.
    # install-docker_deploy_dai_{version}.sh -U
2. As root run the installer with "-U" to uninstall third party components.
    # install-docker_deploy_3rd_party_{version}.sh -U


NOTES:
======
* Postgres persistent data is stored in /opt/dai-docker/tier2/data/pgdata
    + To clear the data and start over do the following as root:
        # systemctl stop dai-manager dai-voltdb dai-rabbitmq dai-postgres
        # rm -rf /opt/dai-docker/tier2/data/pgdata

    + To backup the data (as root):
        # systemctl stop dai-manager dai-voltdb dai-rabbitmq dai-postgres
        # recursively copy/tar /opt/dai-docker/tier2/data/pgdata

* If you start all services together with one command, expect errors and
  restarts of the dai-manager service until all other services are running.
