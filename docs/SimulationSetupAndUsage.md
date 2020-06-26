# Configuration Guide to setup and use EventSim

## 1. Introduction
This documentation describes prerequisites, configuration and setup to start EventSim with docker container.
EventSim is a simulation tool which acts as a rest-server serving several rest api's. It is used to generate events, gather and view information from DAI database through the EventSim CLI interface.
It enables client to make subscriptions(SSE or http callback) with respective network steam(s) to gather data.

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
 "eventsimConfig" : {
      "SensorMetadata": "/resources/ForeignSensorMetaData.json",
      "RASMetadata": "/resources/ForeignEventMetaData.json",
      ...
      "eventCount": 10,
      "timeDelayMus": 1,
      "eventRatioSensorToRas": 1,
      "randomizerSeed": "234"
  }
...
```

 **_eventCount_** = default value of number of events to be generated.
 
 **_timeDelayMus_** = default time delay to send between event-to-event.
 
Note: Field **_eventCount_** can be overridden by EventSim CLI.

Below json explains about the client subscriptions details.

```json
...
"networkConfig" : {
      "network" : "sse",
      "sseConfig": {
          "serverAddress": "rest_server_ip_address_or_hostname" ,
          "serverPort": "rest_server_port" ,
          "urls": {
            "network_stream_telemety_url": [
              "monitor_adapter_profile_subject_name"
            ] ,
            "network_stream_boot_states_url": [
              "provisioner_adapter_profile_subject_name"
            ] ,
            "network_stream_ras_url": [
              "monitor_adapter_profile_subject_name"
            ]
          }
      } ,
      "rabbitmq": {
          "exchangeName": "rabbitmq_exchange_name" ,
          "uri": "amqp://127.0.0.1"
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
"networkConfig" : {
      "network" : "sse",
      "sseConfig": {
          "serverAddress": "rest_server_ip_address_or_hostname" , //data-point 3
          "serverPort": "rest_server_port" , //data-point 4
          "urls": {
            "network_stream_telemety_url": [ //data-point 1
              "monitor_adapter_profile_subject_name" //data-point 2
            ]
          }
      } ,
      "rabbitmq": {
          "exchangeName": "rabbitmq_exchange_name" ,
          "uri": "amqp://127.0.0.1"
      }
  }
...
```
**ProviderMonitoringNetworkForeignBus.json** file

```json
...
  "networkStreams": {
    "dtmfResourceEvents": { //data-point 5
      "arguments": {
        "connectPort": "rest_server_ip_address_or_hostname", //data-point 3
        "connectAddress": "rest_server_port", //data-point 4
        "urlPath": "network_stream_telemety_url", //data-point 1
        "connectTimeout": "30",
        "requestBuilder": "com.intel.dai.monitoring.SSEStreamRequestBuilder",
        "requestType": "GET",
        "requestBuilderSelectors": {
          "stream_id": "dmtfEvents"
        }
      },
      "name": "sse"
    }
  },

  "adapterProfiles": {
    "environmental": {
      "networkStreamsRef": [
        "dtmfResourceEvents" //point 5
      ],
      "subjects": [
        "monitor_adapter_profile_subject_name" //point 2
      ],
      "adapterProvider": "environmentalData"
    }
  },
...
```

**Note: Data-Point numbers should match between simulation server and respective SSE adapter subscription configuration file. 
In above example compare data-points between EventSim.json (simulation-server) and ProviderMonitoringNetworkForeignBus.json (SSE adapter subscription)**

#### 2.2.3.2 For Http Callback type client subscription

**EventSim.json** file 

```json
...
"networkConfig" : {
      "network" : "sse",
      "sseConfig": {
          "serverAddress": "rest_server_ip_address_or_hostname" , //data-point 3
          "serverPort": "rest_server_port" , //data-point 4
          "urls": {
            "network_stream_telemety_url": [ //data-point 1
              "provisioner_adapter_profile_subject_name" //data-point 2
            ]
          }
      } ,
      "rabbitmq": {
          "exchangeName": "rabbitmq_exchange_name" ,
          "uri": "amqp://127.0.0.1"
      }
  }
...
```
**ProviderProvisionerNetworkForeignBus.json** file

```json
...

"networkStreams": {
    "stateChangeSource": { //data-point 5
      "arguments": {
        "connectAddress": "subscription_server_ip_address", //data-point 3
        "connectPort": "subscription_server_port", //data-point 4
        "bindAddress": "callback_server_bind_address", //ip address where this adapter is launched
        "bindPort": "callback_server_bind_port", // call back listening port
        "urlPath": "/apis/smd/hsm/v1/Subscriptions/SCN", //data-point 1
        "subjects": "provisioner_adapter_profile_subject_name", //data-point 2
        "requestBuilder": "com.intel.dai.provisioners.ForeignSubscriptionRequest",
        "responseParser": "com.intel.dai.provisioners.ForeignSubscriptionResponseParser",
        "subscriberName": "daiSubscriptionID",
        "use-ssl": false
      },
      "name": "http_callback"
    }
  },

  "adapterProfiles": {
    "default": {
      "networkStreamsRef": [
        "stateChangeSource" //data-point 5
      ],
      "subjects": [
        "stateChanges" //data-point 2
      ],
      "adapterProvider": "bootEventData"
    }
  }
...
```

**Note: Data-Point numbers should match between simulation server and respective callback adapter subscription configuration file. 
In above example compare data-points between EventSim.json (simulation-server) and ProviderProvisionerNetworkForeignBus.json (callback adapter subscription)**


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
