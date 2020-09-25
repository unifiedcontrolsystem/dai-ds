# Configuration Guide to setup and use EventSim

## 1. Introduction
This documentation describes prerequisites, configuration and setup to start EventSim with docker container.
EventSim is a simulation tool which acts as a rest-server serving several rest api's. It is used to generate events, gather and view information from DAI database through the EventSim CLI interface.
It enables client to make subscriptions(SSE request) with respective network steam(s) to gather data.

## 2. Steps to start EventSim with docker container(s).
### 2.1 Prerequisites with docker container
a.) Install docker.

b.) If you restart your machine, execute these 2 lines before starting docker services 

```bash
sudo bash -c "echo never >  sys/kernel/mm/transparent_hugepage/enabled"

sudo bash -c "echo never > /sys/kernel/mm/transparent_hugepage/defrag"
```

### 2.2 Steps to start EventSim

a.) Build project

b.) Run docker scripts

c.) Configure **EventSim.json** file.

d.) Start **Postgres** databse.

e.) Start **RabbitMQ**.

f.) Start **Volt** database.

g.) Start EventSim

h.) Start client subscriptions.

h.) Verify Installation

### 2.2.1 Build Project

Refer to the **README.md** for commands.

### 2.2.2 Run docker scripts

Refer to the **README.md** for commands.


### 2.2.3 Configure EventSim.json file along with client subscriptions
This section defines all the default values and client subscription details which EventSim server will use.
This file is located at /opt/dai-docker/etc directory (default installation is in /opt directory)

Below json explains about the default data for EventSim seerver.
```json
...

  "api-simulator-config" : {
      "boot-parameters" : "simulation_data_file_boot_parameters_api",
      "hw-inventory" : "simulation_data_file_hw_inventory",
      "hw-inventory-path" : "path_to_hw_inventory",
      "hw-inventory-query-path" : "path_to_hw_inventory_query",
      "hw-inv-discover-status-url" : "uri_hw_inventory_discovery_status"
  },
  "events-simulator-config" : {
    "count": 10,
    "events-template-config" : "/opt/ucs/etc/EventsTemplate.json",
    "seed": "1234",
    "time-delay-mus": 1,
    "timezone": "GMT"
  },
  
...
```

 **_count_** = default value of number of events to be generated.
 
 **_events-template-config_** = events template configuration file. 
 
 **_timeDelayMus_** = default time delay to send between event-to-event.
 
Note: Field **_eventCount_** can be overridden by EventSim CLI.

Below json explains about the client subscriptions details.

```json
...
 
    "network-config" : {
        "network" : "sse",
        "sse": {
            "server-address": "rest_server_ip_address_or_hostname" ,
            "server-port": "rest_server_port" ,
            "urls": {
             "network_stream_telemety_url" : "network_stream_telemety_url_id"
            }
        } ,
        "rabbitmq": {
            "exchangeName": "name",
            "uri": "amqp://rabbitmq_hosted_server"
        }
    }
    
...
```

Note: 

**urls --> "network_stream_url" key** should match with the client subscription adapter network stream "**urlPath**.

**urls --> "network_stream_url" value** should match with the client subscription adapter profile subject name.

#### 2.2.3.1 For SSE type client subscription

**EventSim.json** file 

```json
...

    "network-config" : {
        "network" : "sse",
        "sse": {
            "server-address": "rest_server_ip_address_or_hostname" ,
            "server-port": "rest_server_port" ,
            "urls": {
             "network_stream_telemety_url" : "network_stream_telemety_url_id"
            }
        } ,
        "rabbitmq": {
            "exchangeName": "name",
            "uri": "amqp://rabbitmq_hosted_server"
        }
    }
...
```

**Note: The "network_stream_telemety_url" in EventSim.json should match the url in the "fullUrl" of client subscriptions**


Refer to the **README.md** for commands.

### 2.2.4 Start **Postgres** docker

To Start postgres docker navigate to directory containing postgres.yml file
In this case it is /opt/dai-docker

Refer to the **README.md** for commands.

### 2.2.5 Start **RabbitMQ** docker

To Start postgres docker navigate to directory containing rabbitmq.yml file
In this case it is /opt/dai-docker

Refer to the **README.md** for commands.

### 2.2.6 Start **Voltdb** docker

To Start postgres docker navigate to directory containing voldb.yml file
In this case it is /opt/dai-docker

Refer to the **README.md** for commands.

### 2.2.7 Start EventSim
Requirements:

service named "eventsim-server" exists.

voltdb details

path to rest server configuration file.

Command:

```bash
eventsim-server <voltdb_ip_address_or_hostname> <path_toserver_configuration_file>
```

### 2.2.8 Start **Subscription Clients**  using docker

To Start postgres docker navigate to directory containing dai.yml file
In this case it is /opt/dai-docker

Refer to the **README.md** for commands.

### 2.2.9 Verification

In Eventsim server console we can see the added client subscrption log.

To enable debug mode

add following line in eventsim-server service file found in /opt/ucs/bin directory.

-DdaiLoggingLevel = DEBUG


## 3. Usage of template based data generation EventSim.
### 3.1 Prerequisites events template file
This file contains all the possible types of events data that can be generated using this EventSim data generation tool. This template configuration file requires following details.

1. Types of events and its sub-categories. (Ex: sensor = power, temperature, voltage etc)
2. Sample template of data of each sub-type/category event.

### 3.2 Configure events template file

Below json explains about the configuration of template based data generation.

```json

{
  "event": { //provide event types and its sub-types/categories
    "ras" : {
      "default" : "old-ras",
      "types" :[
        "old-ras"
      ]
    },

    "sensor" : {
      "default" : "energy",
      "types" :[
        "energy"
      ]
    }
  } ,
  "event-types": { //define event-type/catergory template structure for each mentioned above
    "energy": { //name from sensor sub-type
      "template": "file_path_to_sample_data_template" , //sample template data how data looks like/want to create
      "stream-type": "energyTelemetry" , //to which uri/stream you want to send data check detials in EvnetSim.json configuration file
      "single-template": {
        "json_path_field_1": ".*" , //provide json-path from field in template
        "json_path_field_2": ".*" //provide json-path from field in template
      },
      "single-template-count": {
        "json_path_1" : 1, //Drill down from n level template to 1 level template at each json-path level
        "json_path_2" : 1
      },
      "path-count" : {
        "json_path_field_1" : 4, //number of items under the json-path-level
        "json_path_field_2" : 1
      },
      "update-fields": {
        "json_path_1": {
          "Location": {
            "metadata": "DB-Locations" , //to updtae field from outside resource/file
            "metadata-filter": ".*" //filter data from outside resource/file
          } ,
          "Value": {
            "metadata": "Integer" ,
            "metadata-filter": [10, 12]  //range of values
          }
        }
      },
      "generate-data-and-overflow-path" : { //path-count key restrict count under json-path-levl, if excess needed where should they move
        "json_path_field_1" : "metrics/messages[*]",
        "json_path_field_2" : "new"
      },
      "timestamp" : "json_path_time_stamp" //to update time in template data
    },

    "old-ras": {
      "template": "/resources/templates/old-ras.json" ,
      "stream-type": "dmtfEvent" ,
      "single-template": {
        "json_path_field_1": ".*" ,
        "json_path_field_2": ".*"
      },
      "single-template-count": {
        "json_path_1":1
      },
      "path-count" : {
        "json_path_1" : 1
      },
      "update-fields": {
        "json_path_1": {
          "event-type": {
            "metadata": "/resources/metadata/dataValues.json" ,
            "metadata-filter": ".*"
          } ,
          "location": {
            "metadata": "DB-Locations" ,
            "metadata-filter": ".*"
          }
        }
      },
      "generate-data-and-overflow-path" : {
        "json_path_field_1" : "new"
      },
      "timestamp" : "path_timestamp"
    }
  }
}

``` 

