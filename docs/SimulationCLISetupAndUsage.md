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


This subcommand is used to generate different types of events like ras, environmental, boot state change and job events. It also enables user to generate these different events (except job) in a group by using scenario command. 
It helps user to get seed value used to generate events in-order to replicate data.

```bash
user@/opt/ucs/bin:~> eventsim events --help
usage: eventsim events [-h] {ras,sensor,job,boot,scenario,get-seed} ...

positional arguments:
  {ras,sensor,job,boot,scenario,get-seed}
                        subparser for events
    ras                 generate ras events
    sensor              generate sensor events
    job                 generate job events
    boot                generate boot events
    scenario            generate events for a given scenario
    get-seed            fetch prior seed to replicate same data.

optional arguments:
  -h, --help            show this help message and exit
```

### Events subcommand – RAS

This subcommand is used to generate ras events.

Events RAS  Command:
##### user@/opt/ucs/bin:~> eventsim events ras --help
```bash
usage: eventsim events ras [-h] [--burst] [--count COUNT] [--delay DELAY]
                           [--label LABEL] [--locations LOCATIONS]
                           [--output OUTPUT] [--seed SEED] [--timeout TIMEOUT]

optional arguments:
  -h, --help            show this help message and exit
  --burst               generate ras events without delay. Default is constant
                        mode with delay.
  --count COUNT         given number of ras events are generated. The default
                        values exists in eventsim config file.
  --delay DELAY         pause for given value in microseconds to generate ras
                        events. The default values exists in eventsim config
                        file.
  --label LABEL         generate ras events for a given type/description
  --locations LOCATIONS
                        generate ras events at a given location. Provide regex
                        for multiple locations.
  --output OUTPUT       store data in a file
  --seed SEED           seed to duplicate data
  --timeout TIMEOUT     ras sub-command execution timeout
  

Example:
user@/opt/ucs/bin:~> eventsim events ras --location R0.*
```

### Events subcommand – Sensor

This subcommand is used to generate sensor events.

Events Sensor Command:
##### user@/opt/ucs/bin:~> eventsim events sensor --help
```bash
usage: eventsim events sensor [-h] [--burst] [--count COUNT] [--delay DELAY]
                              [--label LABEL] [--locations LOCATIONS]
                              [--output OUTPUT] [--seed SEED]
                              [--timeout TIMEOUT]

optional arguments:
  -h, --help            show this help message and exit
  --burst               generate sensor events without delay. Default is
                        constant mode with delay.
  --count COUNT         given number of sensor events are generated. The
                        default values exists in eventsim config file.
  --delay DELAY         pause for given value in microseconds to generate
                        sensor events. The default values exists in eventsim
                        config file.
  --label LABEL         generate sensor events for a given type/description
  --locations LOCATIONS
                        generate sensor events at a given location. Provide
                        regex for multiple locations.
  --output OUTPUT       store data in a file
  --seed SEED           seed to duplicate data
  --timeout TIMEOUT     sensor sub-command execution timeout


Example:
user@/opt/ucs/bin:~> eventsim events sensor --location R0.*
```

### Events subcommand – Boot

This subcommand is used to generate all available or specific boot types (off, on, ready). (Only compute or service node locations)

Events Boot Command:
##### user@/opt/ucs/bin:~> eventsim events boot --help
```bash
usage: eventsim events boot [-h] [--burst] [--delay DELAY]
                            [--locations LOCATIONS] [--output OUTPUT]
                            [--probability PROBABILITY] [--seed SEED]
                            [--timeout TIMEOUT] [--type {off,on,ready}]

optional arguments:
  -h, --help            show this help message and exit
  --burst               generate boot events without delay. Default is
                        constant mode with delay.
  --delay DELAY         pause for given value in microseconds to generate boot
                        events. The default values exists in eventsim config
                        file.
  --locations LOCATIONS
                        generate boot events at a given location. Provide
                        regex for multiple locations.
  --output OUTPUT       store data in a file
  --probability PROBABILITY
                        generate boot events with probability failure. Default
                        no failure.
  --seed SEED           seed to duplicate data
  --timeout TIMEOUT     boot sub-command execution timeout
  --type {off,on,ready}
                        generate given type of boot events. Default generates
                        all [on/off/ready] types of boot events.


Example:
user@/opt/ucs/bin:~> eventsim events boot --location R0.*.CN* (locations should be compute/service node only)
```

### Events subcommand – Scenario

This subcommand is used to generate all types (ras, sensor, boot) events in a group. end-user should configure scenario using the template given below.
Currently, there are 3 modes in scenario. They are
1. burst = given ras/sensor/boot events are sent in groups (events/group-rate) randomly
2. group-burst = given ras/sensor/boot events are sent in groups (specific ras/sensor/boot numbers)
3. repeat = re-run above burst/group-burst mode continuosly either by using counter, duration or start-time.

counter = (how many times scenario should run)

duration = (how long scenario should run accepts in minutes only)

start-time = (schedule to run scenario at a specific time)


Events Scenario Command:
##### user@/opt/ucs/bin:~> eventsim events scenario --help
```bash
usage: eventsim events scenario [-h] [--burst] [--counter COUNTER]
                                [--delay DELAY] [--duration DURATION]
                                [--locations LOCATIONS] [--output OUTPUT]
                                [--probability PROBABILITY]
                                [--ras-label RAS_LABEL]
                                [--sensor-label SENSOR_LABEL] [--seed SEED]
                                [--start-time START_TIME] [--timeout TIMEOUT]
                                [--mode {burst,group-burst,repeat}]
                                file

positional arguments:
  file                  scenario configuration file

optional arguments:
  -h, --help            show this help message and exit
  --burst               generate events for a given scenario without delay.
                        Default is constant mode with delay.
  --counter COUNTER     repeat scenario for a given counter
  --delay DELAY         pause for given value in microseconds to generate
                        events for a given scenario. The default values exists
                        in eventsim config file.
  --duration DURATION   scenario occurs for a given duration
  --locations LOCATIONS
                        generate events for a given scenario at a given
                        location. Provide regex for multiple locations.
  --output OUTPUT       Store data in a file.
  --probability PROBABILITY
                        generate boot events with probability failure
  --ras-label RAS_LABEL
                        generate ras events of a particular type/description
  --sensor-label SENSOR_LABEL
                        generate sensor events of a particular
                        type/description
  --seed SEED           seed to duplicate data
  --start-time START_TIME
                        start time to generate events for a given scenario
  --timeout TIMEOUT     scenario sub-command execution timeout
  --mode {burst,group-burst,repeat}
                        generate events given type of scenario. Default
                        generates burst type scenario. Scenario data exists in
                        scenario config file.


Example:
user@/opt/ucs/bin:~> eventsim events scenario /tmp/scenario.json --type repeat --counter 3
```

#####Events scenario configuration template
```json
{
  "mode" : "repeat", // mode-name = {burst, group-burst, repeat}
  "group-burst" : {
    "totalRas" : "700000", //Total ras events to generate
    "totalSensor" : "300000",  //Total sensor events to generate
    "totalBootOn" : "0",  //Total boot-on events to generate
    "totalBootOff" : "0",  //Total boot-off events to generate
    "totalBootReady" : "0",  //Total boot-ready events to generate
    "ras" : "500000", //In a group how many ras events to send
    "sensor" : "100000", //In a group how many sensor events to send
    "boot-on" : "0", //In a group how many boot-on events to send
    "boot-off" : "0", //In a group how many boot-off events to send
    "boot-ready" : "0", //In a group how many boot-ready events to send
    "seed" : "123" //to replicate data
  },
  "burst" : {
    "ras" : "600000", //Total ras events to generate
    "sensor" : "400000",  //Total sensor events to generate
    "boot-on" : "0", //Total boot-on events to generate
    "boot-off" : "0", //Total boot-off events to generate
    "boot-ready" : "0", //Total boot-ready events to generate
    "rate" : "500000", //randomly picks events from ras/sensor/boot and form group of count rate
    "seed" : "123" //to  replicate data
  },
  "repeat" : {
    "mode" : "burst", //mode to repeat
    "clock-mode" : "duration", //how to re-run counter/counter/start-time
    "duration" : "1", //how long to run
    "counter" : "1", //how many times
    "start-time" : "2020-05-27 16:34:50.607Z" //schedule start-time
  },
  "delay" : "2000000" //delay between bursts microseconds only, here it is 2 seconds
}
``` 

### Events subcommand – get-seed

This subcommand is used to fetch prior used seed to generate events.

Events Get-seed Command:
##### user@/opt/ucs/bin:~> eventsim events get-seed --help
```bash
usage: eventsim events get-seed [-h] [--seed SEED] [--timeout TIMEOUT]

optional arguments:
  -h, --help         show this help message and exit
  --seed SEED        seed to duplicate data
  --timeout TIMEOUT  get-seed sub-command execution timeout


Example:
user@/opt/ucs/bin:~> eventsim events get-seed
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

First, the files cobalt-bgsched.log and cobalt-cqm.log located in the data/etc-logstash/conf.d directory of the ucs code tree need to be copied to the logstash configuration directory: /etc/logstash/conf.d/

Make sure that both configuration files point to the correct rabbitMQ server. With the default docker deployment, this is sms01-nmn.local.

After this, the logstash service needs to be restarted with the new configuration using the command: systemctl restart logstash
