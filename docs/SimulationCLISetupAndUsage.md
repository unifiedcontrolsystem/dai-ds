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