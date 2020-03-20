## Introduction

EventSim CLI is an interface or user tool which enables user to interact with rest server. It has dependency on EventSim server, Voltdb, Postgres database and RabbitMQ.

Refer to the **SimulationSetupAndUsage.md** for details.


## Setup and Configuration

After successful build
Install EventSim CLI by running following command.

##### Command: (Navigate to dai-ds directory)

```bash
sudo ./build/distributions/install-cli-eventsim*.sh
```

Navigate to eventsim_cli_config.json fould in /opt/ucs/etc directory

```json
{
  "rest_server_address" : "rest_server_ip_address_or_hostname",
  "rest_server_port" : "rest_server_port"
}
```

In order to get the overall/general help for EventSim CLI. Just type following command. 

```bash
admin@/opt/ucs/bin:~> eventsim
admin@/opt/ucs/bin:~> eventsim --help
```

After executing the above command, user will be able to see help of all UCS subcommands. The example of help is shown below:


##### user@/opt/ucs/bin:~> eventsim
```bash
usage: eventsim [-h] [-V] {events,wlm} ...

EventSim CLI Parser

optional arguments:
  -h, --help     show this help message and exit
  -V, --version  Provides the version of the tool

Sub Commands:
  List of Valid Sub Commands

  {events,wlm}
    events       Generate events of specified type.
    wlm          Generate wlm events of specified type.
```

### EventSim events subcommand


This subcommand can be used to generate events of 3 different types like ras, environmental and boot state change events.

```bash
user@/opt/ucs/bin:~> eventsim events --help
usage: eventsim events [-h] {ras,sensor,boot} ...

positional arguments:
  {ras,sensor,boot}  Subparser for events
    ras              generate ras events.
    sensor           generate sensor events.
    boot             generate boot events.

optional arguments:
  -h, --help         show this help message and exit
```

### Events subcommand – RAS

This subcommand is used to generate ras events.

Events RAS  Command:
##### user@/opt/ucs/bin:~> eventsim events ras --help
```bash
usage: eventsim events ras [-h] [--count COUNT] [--location LOCATION]
                           [--burst] [--label LABEL]

optional arguments:
  -h, --help           show this help message and exit
  --count COUNT        Provide number of ras events to be generated. The
                       default values are in config file.
  --location LOCATION  generate ras events at a given location.
  --burst              generate events with or without delay.
  --label LABEL        generate ras events of a particular type


Example:
user@/opt/ucs/bin:~> eventsim events ras --location R0.*
```

### Events subcommand – Sensor

This subcommand is used to generate sensor events.

Events Sensor Command:
##### user@/opt/ucs/bin:~> eventsim events sensor --help
```bash
usage: eventsim events sensor [-h] [--count COUNT] [--location LOCATION]
                              [--burst] [--label LABEL]

optional arguments:
  -h, --help           show this help message and exit
  --count COUNT        Provide number of ras events to be generated. The
                       default values are in config file.
  --location LOCATION  generate sensor events at a given location.
  --burst              generate events with or without delay.
  --label LABEL        generate sensor events of a particular type


Example:
user@/opt/ucs/bin:~> eventsim events sensor --location R0.*
```

### Events subcommand – Boot

This subcommand is used to generate boot events. (Only compute or service node)

Events Boot Command:
##### user@/opt/ucs/bin:~> eventsim events boot --help
```bash
usage: eventsim events boot [-h] [--type {off,on,ready}]
                            [--probability PROBABILITY] [--burst]
                            [--location LOCATION]

optional arguments:
  -h, --help            show this help message and exit
  --type {off,on,ready}
                        types of boot events to generate. Default will
                        generate all types of boot events
  --probability PROBABILITY
                        generate boot events with probability failure
  --burst               generate events with or without delay.
  --location LOCATION   generate boot events at a given location.


Example:
user@/opt/ucs/bin:~> eventsim events boot --location R0.*.CN* (locations should be compute/service node only)
```
**Note:**

By default, events are sent to network with constant delays between events.
If burst mode is enabled, events are sent to network without any delays between events.

**time delay is configured in EventSim.json file**

### Cobalt Job Simulation

It is possible to generate artificial log entries to simulate Cobalt entries that would perform actions like creating a reservation, modifying a reservation, deleting a reservation, starting a job and terminating a job. This is all done with a simple set of command lines.

### Eventsim Command Line Usage

The eventsim wlm subcommand has the following options:

•	Create Reservation: generate a create reservation log entry

•	Modify Reservation: generate a modify reservation log entry

•	Delete Reservation: generate a delete reservation log entry

•	Start Job: generate a start job log entry

•	Terminate Job: generate a terminate job log entry

•	Simulate: Simulate a series of random job events to simulate real world use case scenarios.


```bash
eventsim wlm --help
usage: eventsim wlm [-h]
                    {create_reservation,modify_reservation,delete_reservation,start_job,terminate_job,simulate}
                    ...

positional arguments:
  {create_reservation,modify_reservation,delete_reservation,start_job,terminate_job,simulate}
                        Subparser for wlm
    create_reservation  generate a log event for a created reservation.
    modify_reservation  generate a log event for a modified reservation.
    delete_reservation  generate a log event for a deleted reservation.
    start_job           generate a log event for a started job.
    terminate_job       generate a log event for a terminated job.
    simulate            generate random log events for jobs and reservations.

optional arguments:
  -h, --help            show this help message and exit
```

### WLM Subcommand – Create Reservation

Generates a log event for a created reservation with a specific name. Command line can provide users, nodes, start time and duration, otherwise they will be randomized.

```bash
eventsim wlm create_reservation --help
usage: eventsim wlm create_reservation [-h] [--users USERS] [--nodes NODES]
                                       [--start-time START_TIME]
                                       [--duration DURATION]
                                       name

positional arguments:
  name                  Name of the reservation to generate.

optional arguments:
  -h, --help            show this help message and exit
  --users USERS         Users that own reservation.
  --nodes NODES         Nodes in the reservation.
  --start-time START_TIME
                        Start time of the reservation.
  --duration DURATION   Duration of the reservation in microseconds.
```

### WLM Subcommand – Modify Reservation

Generates a log event for a modified reservation for a specific name. Command line provides the option to modify either the users, nodes or start time.

```bash
eventsim wlm modify_reservation --help
usage: eventsim wlm modify_reservation [-h] [--users USERS] [--nodes NODES]
                                       [--start-time START_TIME]
                                       name

positional arguments:
  name                  Name of the reservation to modify.

optional arguments:
  -h, --help            show this help message and exit
  --users USERS         New users that own reservation.
  --nodes NODES         New nodes in the reservation.
  --start-time START_TIME
                        New start time for the reservation.
```

### WLM Subcommand – Delete Reservation

Generates a log event for a deleted reservation for a specific reservation name.

```bash
eventsim wlm delete_reservation --help
usage: eventsim wlm delete_reservation [-h] name

positional arguments:
  name        Name of the reservation to delete.

optional arguments:
  -h, --help  show this help message and exit
```

### WLM Subcommand – Start Job

Generates a log event for a start job event with a specific jobid. Command line can provide name, users, nodes, start time and work directory, otherwise they will be randomized.

```bash
eventsim wlm start_job --help
usage: eventsim wlm start_job [-h] [--name NAME] [--users USERS]
                              [--nodes NODES] [--start-time START_TIME]
                              [--workdir WORKDIR]
                             jobid

positional arguments:
  jobid                 job ID of the job to generate.

optional arguments:
  -h, --help            show this help message and exit
  --name NAME           Name of the started job.
  --users USERS         Users that started job.
  --nodes NODES         Nodes for the job.
  --start-time START_TIME
                        Start time of the job.
  --workdir WORKDIR     Work directory of the job.
```

### WLM Subcommand – Terminate Job

Generates a log event for a terminate job event for a specific jobid. Command line can provide name, users, nodes, start time, exit status and work directory, otherwise they will be randomized.

```bash
eventsim wlm terminate_job --help
usage: eventsim wlm terminate_job [-h] [--name NAME] [--users USERS]
                                  [--nodes NODES] [--start-time START_TIME]
                                  [--workdir WORKDIR]
                                  [--exit-status EXIT_STATUS]
                                  jobid

positional arguments:
  jobid                 job ID of the job to terminate.

optional arguments:
  -h, --help            show this help message and exit
  --name NAME           Name of the started job.
  --users USERS         Users that started job.
  --nodes NODES         Nodes for the job.
  --start-time START_TIME
                        Start time of the job.
  --workdir WORKDIR     Work directory of the job.
  --exit-status EXIT_STATUS
                        Exit status of the job.
```

### WLM Subcommand – Simulate

Generates a specific number of real life use case scenarios of a series of randomized reservation and job events. For example if we set the number of reservations input to 3, we could generate a create reservation, start job, terminate job series; followed by a create reservation, delete reservation series; followed by a create reservation, modify reservation, start job, terminate job series. All job events would happen in order for each series, but randomized between them so you can get events from each of them at different times.

```bash
eventsim wlm simulate --help
usage: eventsim wlm simulate [-h] reservations

positional arguments:
  reservations  Number of reservations to simulate

optional arguments:
  -h, --help    show this help message and exit
```

### WLM Adapter Configuration Change

To configure the WLM Adapter it is necessary to add the following lines to the MachineConfi.json file located in /opt/ucs/etc:

In the Adapter Instances Section:

```xml
{
  "LogFile": "$UCSLOGFILEDIRECTORY/AdapterWLM-$LCTN-$INSTANCE.log",
  "ServiceNode": “<SERVICE NODE HOSTNAME>”,
  "Invocation": "com.intel.dai.resource_managers.cobalt.AdapterWlmCobalt",
  "TypeOfAdapter": "WLM",
  "NumberOfInstances": 1
}
```
In the Initial Workitems Section:

```xml
{
  "Parms": "RabbitMQHost=<RABBITMQ HOSTNAME>",
  "TypeOfAdapter": "WLM",
  "NotifyWhenFinished": "F",
  "WorkToBeDone": "HandleInputFromExternalComponent",
  "Queue": ""
}
```

## Logstash Configuration

For the log entries to be processed by the WLM Adapter it is necessary to have Logstash properly configured and running. 

First, the files cobalt-bgsched.log and cobalt-cqm.log located in the data directory of the ucs code tree need to be copied to the logstash configuration directory: /etc/logstash/conf.d/

Make sure that both configuration files point to the correct rabbitMQ server.

After this, the logstash service needs to be restarted with the new configuration using the command: systemctl restart logstash
