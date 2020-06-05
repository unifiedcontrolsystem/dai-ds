Introduction
============

UCS CLI is an end user tool which is meant to provide a common solution for different third party tools. It can be used to view the information stored in the DAI database.

UCS CLI is installed on UAN (User Access Node).

In order to get the overall/general help for UCS CLI. Just type following command on UAN node: 

```bash
user@uan1:~> ucs
or
user@uan1:~> ucs --help
```

After executing the above command, user will be able to see help of all UCS subcommands. The example of help is shown below:

```bash
user@uan1:~> ucs
usage: ucs [-h] {group,view} ...

UCS CLI

optional arguments:
  -h, --help    show this help message and exit

Sub Commands:
  List of Valid Sub Commands

  {group,view}
    group       Logical group management commands
    view        View data in database commands
```


UCS Group Subcommand
--------------------

This subcommand can be used to create logical groups of compute nodes. This allows user to execute commands with complex node regexes with simplicity. This subcommand has four subcommands:

1.  Add – Add location regex to a group

2.  Get – Get the location regex of a group

3.  List – List the groups created so far.

4.  Remove – Remove location regex from group

```bash
user@uan1:~> ucs group --help
usage: ucs group [-h] {add,get,list,remove} ...

positional arguments:
  {add,get,list,remove}
                        Subparser for group
    add                 Add location regex to a group. Add locations to one
                        group at a time. Use: group add node_regex group_name
    get                 Get the location regex from a group. Get locations
                        from one group at a time. Use: group get group_name
    list                List the groups created so far. Use: group list
    remove              Remove location regex from a group. Remove locations
                        from one group at a time. Use: group remove
                        location_regex group_name

optional arguments:
  -h, --help            show this help message and exit
```

### Group Subcommand – Add

Adds a set of nodes in a group.

```bash
Group Add Command:
user@uan1:~> ucs group add --help
usage: ucs group add [-h] locations group_name

positional arguments:
  locations     Provide comma separated location list or location regex
  group_name    Group name where the locations have to be added to

optional arguments:
  -h, --help  show this help message and exit

Example:
user@uan1:~> ucs group add c001n00[01-08] rack1
```

### Group Subcommand – Get

This subcommand can be used to expand the nodes in a group.

```bash
Group Get Command:
user@uan1:~> ucs group get --help
usage: ucs group get [-h] group_name

positional arguments:
  group_name  Group name where the locations have to be added to

optional arguments:
  -h, --help  show this help message and exit

Example
user@uan1:~> ucs group get rack1
0 - c001n0008,c001n0004,c001n0005,c001n0006,c001n0007,c001n0001,c001n0002,c001n0003
```

### Group Subcommand – List

Lists the groups that were created by user.

```bash
Group List Command:
user@uan1:~> ucs group list --help
usage: ucs group list [-h]

optional arguments:
  -h, --help  show this help message and exit

Example:
user@uan1:~> ucs group list
0 - rack1 (This is the group we created with ‘add’ subcommand above)
```

### Group Subcommand – Remove

This subcommand lets user remove the node(s) from a group.

```bash
Group Remove Command:
user@uan1:~> ucs group remove --help
usage: ucs group remove [-h] locations group_name

positional arguments:
  locations     Provide comma separated location list or location regex
  group_name    Group name where the locations have to be added to

optional arguments:
  -h, --help  show this help message and exit

Example:
user@uan1:~> ucs group remove c001n0001 rack1
0 - Successfully modified the devices in group

user@uan1:~> ucs group get rack1
0 - c001n0008,c001n0004,c001n0005,c001n0006,c001n0007,c001n0002,c001n0003
```

UCS View Subcommand
--------------------

This subcommand is set of canned queries which retrieves the information NearLine Tier DB. It has following subcommands:

1.  Env – It retrieves the aggregated environment data.

2.  Event – It retrieves the RAS Event Data.

3.  Inventory-History - It retrieves inventory history data for a given location

4.  Inventory-Info - It retrieves inventory information for a given location

5.  Job – It prints job information of cluster

6.  Network Config – Provides the network config data of a given location

7.  Replacement-History - It retrieves inventory information and history for a given location

8.  Reservation – It prints reservation information of cluster

9.  State – Provides the state data of a given location

10.  System-info – It prints the System Map from UCS DAI’s perspective.

```bash
user@uan1:~> ucs view --help
usage: ucs view [-h]
             {env,event,inventory-history,inventory-info,job,network-config,replacement-history,reservation,state,system-info}
             ...

positional arguments:
{env,event,inventory-history,inventory-info,job,network-config,replacement-history,reservation,state,system-info}
                     subparser for quering/viewing data from the database
 env                 view the environmental data
 event               view the events data
 inventory-history   view the history of inventory changes for a location
 inventory-info      view the latest inventory info data for a specific
                     location
 job                 view the job information for the cluster
 network-config      view the latest network info data for a specific
                     location
 replacement-history
                     view the replacement history data
 reservation         view the reservation information for the cluster
 state               view the latest state info data for a specific
                     location
 system-info         view the system information

optional arguments:
-h, --help            show this help message and exit
```

### View Subcommand – Env

Retrieves the aggregated environment data from NearLine Tier DB.

```bash
Env Subcommand:
user@uan1:~> ucs view env --help
usage: ucs view env [-h] [--start-time START_TIME] [--end-time END_TIME]
                    [--limit LIMIT] [--locations LOCATIONS]
                    [--format {json,table}] [--timeout TIMEOUT] [--all]

optional arguments:
  -h, --help            show this help message and exit
  --start-time START_TIME
                        provide the start time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --end-time END_TIME   provide the end time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --limit LIMIT         Provide a limit to the number of records of data being
                        retrieved. The default value is 100
  --locations LOCATIONS
                        Filter all environmental data for a given locations.
                        Provide comma separated location list or location
                        group. Example: R2-CH0[1-4]-N[1-4]
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
  --all                 Specify all output fields for more information than
                        default view
```

#### Example
```bash
user@uan1:~> ucs view env

+----------------+---------+--------------+--------------+--------------+---------------+
|      LCTN      |  TYPE   | MINIMUMVALUE | MAXIMUMVALUE | AVERAGEVALUE |   TIMESTAMP   |
+================+=========+==============+==============+==============+===============+
| X0-CH2-SN2-BC_ | Temp    | 20.0         | 38.0         | 27.52        | 2020-05-28    |
| T_NODE2_KNC_CP |         |              |              |              | 20:37:51.389  |
| U_TEMP         |         |              |              |              |               |
+----------------+---------+--------------+--------------+--------------+---------------+
| X0-CH3-SN3-BC_ | Power   | 20.0         | 39.0         | 30.36        | 2020-05-28    |
| P_NODE1_KNC_IN |         |              |              |              | 20:37:51.449  |
| ST_PWR_MAX     |         |              |              |              |               |
+----------------+---------+--------------+--------------+--------------+---------------+
| X0-CH6-CN2-BC_ | Voltage | 21.0         | 39.0         | 30.44        | 2020-05-28    |
| V_NODE1_KNC_PV |         |              |              |              | 20:37:51.622  |
| _VOLT          |         |              |              |              |               |
+----------------+---------+--------------+--------------+--------------+---------------+
```

### View Subcommand – Event

Retrieves the RAS Event Data from UCS NearLine Tier database.

```bash
Event Subcommand:
user@uan1:~> ucs view event --help
usage: ucs view event [-h] [--start-time START_TIME] [--end-time END_TIME]
                      [--limit LIMIT] [--locations LOCATIONS] [--jobid JOBID]
                      [--type TYPE] [--type-exclude EXCLUDE]
                      [--severity SEVERITY] [--format {json,table}]
                      [--timeout TIMEOUT] [--all] [--summary]

optional arguments:
  -h, --help            show this help message and exit
  --start-time START_TIME
                        provide the start time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --end-time END_TIME   provide the end time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --limit LIMIT         Provide a limit to the number of records of data being
                        retrieved. The default value is 100
  --locations LOCATIONS
                        Filter all event data for a given locations. Provide
                        comma separated location list or location group.
                        Example: R2-CH0[1-4]-N[1-4]
  --jobid JOBID         Filter all event data for a jobid.
  --type TYPE           Filter all event data for the descriptive name of the
                        event type in the RASMetadata.json. Example: RasGen or
                        Ras. Regex are allowed too
  --type-exclude EXCLUDE
                        excludes all event data for the descriptive name of
                        the event type in theRASMetadata.json. Example: RasGen
                        or Ras. Regex are allowed too
  --severity SEVERITY   Filter all event data for a given severity {INFO,
                        FATAL, ERROR, CRITICAL}. This option does not take
                        wildcards or RegEx
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
  --all                 Specify all output fields for more information than
                        default view
  --summary             Summary of RAS Events. This command is not to be used
                        with --format option
```

####Example
```bash
user@uan1:~> ucs view event --limit 3 --location X0-CH7-CN3

+-------------------------+------------+---------------------------------+----------+-------+------------------+------------------------------------------------------------------+
|          TIME           |    LCTN    |              TYPE               | SEVERITY | JOBID | CONTROLOPERATION |                              DETAIL                              |
+=========================+============+=================================+==========+=======+==================+==================================================================+
| 2020-05-21 17:45:47.003 | X0-CH7-CN3 | RasMntrForeignUpdateNodeListRsp | INFO     | None  | None             | Foreign update node list response: Cause:ec_update_node_list_rsp |
+-------------------------+------------+---------------------------------+----------+-------+------------------+------------------------------------------------------------------+
| 2020-05-21 17:45:47.004 | X0-CH7-CN3 | RasMntrForeignUpdateNodeListRsp | INFO     | None  | None             | Foreign update node list response: Cause:ec_update_node_list_rsp |
+-------------------------+------------+---------------------------------+----------+-------+------------------+------------------------------------------------------------------+
| 2020-05-21 17:45:47.005 | X0-CH7-CN3 | RasMntrForeignUpdateNodeListRsp | INFO     | None  | None             | Foreign update node list response: Cause:ec_update_node_list_rsp |
+-------------------------+------------+---------------------------------+----------+-------+------------------+------------------------------------------------------------------+
```

### View Subcommand – Inventory-History

Retrieves the inventory history for a given location from UCS NearLine Tier database.

```bash
Inventory-History Subcommand:
user@uan1:~> ucs view inventory-history  --help
             usage: ucs view inventory-history [-h] [--start-time START_TIME]
                                               [--end-time END_TIME] [--limit LIMIT]
                                               [--format {json,table}] [--timeout TIMEOUT]
                                               locations
             
             positional arguments:
               locations             Filter all inventory history data for a given
                                     locations. Provide comma separated location list or
                                     location group. Example: R2-CH0[1-4]-N[1-4]
             
             optional arguments:
               -h, --help            show this help message and exit
               --start-time START_TIME
                                     provide the start time. The preferred format for the
                                     date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                                     precision
               --end-time END_TIME   provide the end time. The preferred format for the
                                     date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                                     precision
               --limit LIMIT         Provide a limit to the number of records of data being
                                     retrieved. The default value is 100
               --format {json,table}
                                     Display data either in JSON or table format. Default
                                     will be to display data in tabular format
               --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
```

#### Example
```bash
user@uan1:~> ucs view inventory-history X0-CH[4-7]-CN[0-3]

+------------+----------------------+
|     ID     |        FRUID         |
+============+======================+
| X0-CH6-CN2 | Node.WO105483L01S010 |
+------------+----------------------+
| X0-CH7-CN3 | Node.WO105483L01S005 |
+------------+----------------------+

```
### View Subcommand – Inventory-Info

Retrieves the inventory information for a given location from UCS NearLine Tier database.

```bash
Inventory-Info Subcommand:
user@uan1:~> ucs view inventory-info  --help
             usage: ucs view inventory-info [-h] [--limit LIMIT] [--format {json,table}]
                                            [--timeout TIMEOUT]
                                            locations
             
             positional arguments:
               locations             Filter all inventory info data for a given locations.
                                     Provide comma separated location list or location
                                     group. Example: R2-CH0[1-4]-N[1-4]
             
             optional arguments:
               -h, --help            show this help message and exit
               --limit LIMIT         Provide a limit to the number of records of data being
                                     retrieved. The default value is 100
               --format {json,table}
                                     Display data either in JSON or table format. Default
                                     will be to display data in tabular format
               --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
```

#### Example
```bash
user@uan1:~> ucs view inventory-info X0-CH7-CN3 --limit 4
             
+-------------------------+------------------+-----------+---------+-----------------------+-----------+------------+
|   DBUPDATEDTIMESTAMP    |        ID        |   TYPE    | ORDINAL |         FRUID         |  FRUTYPE  | FRUSUBTYPE |
+=========================+==================+===========+=========+=======================+===========+============+
| 2020-06-04 21:58:00.277 | X0-CH7-CN3       | Node      | 0       | Node.WO105483L01S005  | Node      |            |
+-------------------------+------------------+-----------+---------+-----------------------+-----------+------------+
| 2020-06-04 21:58:00.279 | X0-CH7-CN3_CPU0  | Processor | 0       | FRUIDforx0c0s26b0n0p0 | Processor |            |
+-------------------------+------------------+-----------+---------+-----------------------+-----------+------------+
| 2020-06-04 21:58:00.281 | X0-CH7-CN3_CPU1  | Processor | 1       | FRUIDforx0c0s26b0n0p1 | Processor |            |
+-------------------------+------------------+-----------+---------+-----------------------+-----------+------------+
| 2020-06-04 21:58:00.283 | X0-CH7-CN3_DIMM0 | Memory    | 0       | FRUIDforx0c0s26b0n0d0 | Memory    |            |
+-------------------------+------------------+-----------+---------+-----------------------+-----------+------------+
```

### View Subcommand – Job

Retrieves the jobs that have run on the cluster.

```bash
Job Subcommand:
user@uan1:~> ucs view job --help
usage: ucs view job [-h] [--start-time START_TIME] [--end-time END_TIME]
                    [--jobid JOBID] [--user USERNAME] [--limit LIMIT]
                    [--format {json,table}] [--all] [--active]
                    [--timeout TIMEOUT]

optional arguments:
  -h, --help            show this help message and exit
  --start-time START_TIME
                        provide the start time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --end-time END_TIME   provide the end time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --jobid JOBID         Filter all job data for a given jobid. This will also
                        display accounting information and nodes for given
                        jobid
  --user USERNAME       Filter all job data for a given username
  --limit LIMIT         Provide a limit to the number of records of data being
                        retrieved. The default value is 100
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --all                 Specify all output fields for more information than
                        default view
  --active              Show only jobs that are currently running
  --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
```

#### Example
```bash
user@uan1:~> ucs view job
+-------+---------+---------+----------+----------+-----------------------+--------------+
| JOBID | JOBNAME |  STATE  | NUMNODES | USERNAME |    STARTTIMESTAMP     | ENDTIMESTAMP |
+=======+=========+=========+==========+==========+=======================+==============+
| 72    | job3    | Started | 2        | user4    | 2020-06-04 23:50:42.0 | None         |
+-------+---------+---------+----------+----------+-----------------------+--------------+
| 34    | job2    | Started | 2        | user5    | 2020-06-04 23:50:23.0 | None         |
+-------+---------+---------+----------+----------+-----------------------+--------------+
```

### View Subcommand – Network Config

Retrieves the network configuration of each location.

```bash
Network Config Subcommand:
user@uan1:~> ucs view network-config --help
usage: ucs view network-config [-h] [--limit LIMIT] [--format {json,table}]
                               [--timeout TIMEOUT]
                               locations

positional arguments:
  locations             Filter all network config data for given locations.
                        Provide comma separated location list or location
                        group. Example: R2-CH0[1-4]-N[1-4]

optional arguments:
  -h, --help            show this help message and exit
  --limit LIMIT         Provide a limit to the number of records of data being
                        retrieved. The default value is 100
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --timeout TIMEOUT     Timeout value for HTTP request. Uses default of 900s
```
### View Subcommand – Replacement-History

Retrieves the inventory information and history for a given location from UCS NearLine Tier database.

```bash
Replacement-History Subcommand:
user@uan1:~> ucs view replacement-history  --help
             usage: ucs view replacement-history [-h] [--start-time START_TIME]
                                                 [--end-time END_TIME] [--limit LIMIT]
                                                 [--format {json,table}]
                                                 [--timeout TIMEOUT] [--all]
                                                 (locations | sernum)
             
             positional arguments:
               locations             Filter all inventory info and history for a given
                                     locations. Provide comma separated location list or
                                     location group. Example: R2-CH0[1-4]-N[1-4]
               sernum                Filter all inventory history data for a given serial
                                     number
             
             optional arguments:
               -h, --help            show this help message and exit
               --start-time START_TIME
                                     provide the start time. The preferred format for the
                                     date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                                     precision
               --end-time END_TIME   provide the end time. The preferred format for the
                                     date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                                     precision
               --limit LIMIT         Provide a limit to the number of records of data being
                                     retrieved. The default value is 100
               --format {json,table}
                                     Display data either in JSON or table format. Default
                                     will be to display data in tabular format
               --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
               --all                 Specify all output fields for more information than
                                     default view
```

#### Example
```bash
user@uan1:~> ucs view replacement-history X0-CH7-CN3

+-------------------------+------------+---------+----------------------+
|   DBUPDATEDTIMESTAMP    |     ID     | ACTION  |        FRUID         |
+=========================+============+=========+======================+
| 2020-06-04 21:58:52.164 | X0-CH7-CN3 | DELETED | Node.WO105483L01S005 |
+-------------------------+------------+---------+----------------------+

```

### View Subcommand – Reservation

Retrieves the reservations that have been allocated on the cluster.

```bash
Reservation Subcommand:
user@uan1:~> ucs view reservation --help
usage: ucs view reservation [-h] [--start-time START_TIME]
                            [--end-time END_TIME] [--name NAME]
                            [--user USERNAME] [--limit LIMIT]
                            [--format {json,table}] [--timeout TIMEOUT]

optional arguments:
  -h, --help            show this help message and exit
  --start-time START_TIME
                        provide the start time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --end-time END_TIME   provide the end time. The preferred format for the
                        date is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher
                        precision
  --name NAME           Filter all reservation data for a given reservation
                        name.
  --user USERNAME       Filter all reservation data for a given username
  --limit LIMIT         Provide a limit to the number of records of data being
                        retrieved. The default value is 100
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
```
#### Example

```bash
user@uan1:~> ucs view reservation                                                                    
+-----------------+-------+---------------+-----------------------+-----------------------+------------------+
| RESERVATIONNAME | USERS |     NODES     |    STARTTIMESTAMP     |     ENDTIMESTAMP      | DELETEDTIMESTAMP |
+=================+=======+===============+=======================+=======================+==================+
| resv3           | user3 | node24,node52 | 2020-06-04 23:44:48.0 | 2023-03-01 23:44:48.0 | None             |
+-----------------+-------+---------------+-----------------------+-----------------------+------------------+
```

### View Subcommand – State

Retrieves the state of each location.

```bash
State Subcommand:
user@uan1:~> ucs view state --help
usage: ucs view state [-h] [--limit LIMIT] [--format {json,table}]
                      [--timeout TIMEOUT]
                      locations

positional arguments:
  locations             Location that you want to get the state info for.
                        Partial locations are accepted

optional arguments:
  -h, --help            show this help message and exit
  --limit LIMIT         Provide a limit to the number of records of data being
                        retrieved. The default value is 100
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --timeout TIMEOUT     Timeout value for HTTP request. Uses a default of 900s
```

#### Example

```bash
user@uan1:~> sudo ucs view state X0-CH7-CN3

+------------+--------------+-------+--------------+-------+-------------+--------------+
|    LCTN    |   HOSTNAME   | STATE | WLMNODESTATE | OWNER | ENVIRONMENT | BOOTIMAGEID  |
+============+==============+=======+==============+=======+=============+==============+
| X0-CH7-CN3 | nid000004-nm | Error | Unavailable  | WLM   | None        | centos7.3-sl |
|            | n            |       |              |       |             | urm          |
+------------+--------------+-------+--------------+-------+-------------+--------------+
```

### View Subcommand – System Info

Allows user to retrieve the system information which is configured in UCS
DAI.
```bash
System-Info Subcommand:
user@uan1:~> ucs view system-info --help
usage: ucs view system-info [-h] [--all] [--format {json,table}] [--summary]

optional arguments:
  -h, --help            show this help message and exit
  --all                 Specify all output fields for more information than
                        default view
  --format {json,table}
                        Display data either in JSON or table format. Default
                        will be to display data in tabular format
  --summary             Display a summary of cluster by showing the state
                        count of all compute and service nodes
```

#### Example
```bash
user@uan1:~> sudo ucs view system-info                     
+-----------+-----------+-----------+-------+----------+----------+----------+----------+
|   LCTN    | HOSTNAME  | AGGREGATO | STATE |  IPADDR  | MACADDR  | BMCIPADD | BOOTIMAG |
|           |           |     R     |       |          |          |    R     |   EID    |
+===========+===========+===========+=======+==========+==========+==========+==========+
| X0-CH4-CN | nid000001 | X0-CH0-SN | E     | 10.2.0.5 | aa:bb:cc | 10.4.0.5 | centos7. |
| 0         | -nmn      | 0         |       |          | :dd:ee:8 |          | 3        |
|           |           |           |       |          | 0        |          |          |
+-----------+-----------+-----------+-------+----------+----------+----------+----------+
| X0-CH5-CN | nid000002 | X0-CH0-SN | E     | 10.2.0.6 | aa:bb:cc | 10.4.0.6 | centos7. |
| 1         | -nmn      | 0         |       |          | :dd:ee:8 |          | 3        |
|           |           |           |       |          | 1        |          |          |
+-----------+-----------+-----------+-------+----------+----------+----------+----------+
| X0-CH6-CN | nid000003 | X0-CH0-SN | E     | 10.2.0.7 | aa:bb:cc | 10.4.0.7 | centos7. |
| 2         | -nmn      | 0         |       |          | :dd:ee:8 |          | 3        |
|           |           |           |       |          | 2        |          |          |
+-----------+-----------+-----------+-------+----------+----------+----------+----------+
| X0-CH7-CN | nid000004 | X0-CH0-SN | E     | 10.2.0.8 | aa:bb:cc | 10.4.0.8 | centos7. |
| 3         | -nmn      | 0         |       |          | :dd:ee:8 |          | 3-slurm  |
|           |           |           |       |          | 3        |          |          |
+-----------+-----------+-----------+-------+----------+----------+----------+----------+

```

