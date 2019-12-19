# ***Flexible or Partitioned Monitoring Adapter Configuration***

## 1. Introduction
This documentation describes how to configure the *PartitionedMonitorAdapter* to support multiple partitions or sources.

## 2. Logger Specification & Benchmarking
You can specify the Logger implementation by using the key `logProvider` at the root level of the configuration map.  Examples:

```json
{
  "logProvider": "log4j2",
  "useBenchmarkingActions": true,
  ...
}
```
or
```json
{
  "logProvider": "console",
  "useBenchmarkingActions": true,
  ...
}
```
If the _logProvider_ key is missing then *log4j2* is assumed.

If the _useBenchmarkingActions_ key is missing __false__ is assumed for normal operations of system actions like
 storing and publishing data. If __true__ then the normal actions are replaced with metrics gathering and more log
  output to _Logger.info()_ are made for gathering timing data. 

## 3. Subjects Map (`subjectMap`)
This lists all possible subjects (or topics) supported by the *PartitionedMonitorAdapter*. The names are mapped to the *DataType* enum values internal to the adapter. This table should not be changed unless you also change the source code as well.

## 4. Provider Mapping for Created Classes (`providerClassMap`)
This section describes the map which maps an arbitrary configuration name to the string version of the canonical class name of the class to be created in the *PartitionedMonitorAdapter* class.  All providers created by this section must have a constructor that matches the signature:
```java
public ImplementationClassName(Logger logger) { ... }
```
All provider class implementation must be public and implement the correct interface (see below).

Example section:
```json
{
  ...
  "providerClassMap": {
    "telemetryData": "com.intel.dai.foreign_bus.transforms.ForeignApiDataTransformer",
    "telemetryAction": "com.intel.dai.foreign_bus.actions.ForeignApiDataAction",
    "postBuilder": "com.intel.dai.foreign_bus.builders.ForeignApiPostBuilder",
    "getBuilder": "com.intel.dai.foreign_bus.builders.ForeignApiGetBuilder",
    "authTokenRetriever": "com.intel.authentication.KeycloakTokenAuthentication"
    ...
  }
  ...
}
```

### 4.1 Incoming Data Transformation
When raw data is received over the network transport then it must be normalized into a common format that the PartitionedMonitorAdapter and other components understand. Providers that understand the raw data format and can convert this format to the common (or normalized) format are created for each external source. These classes are loaded dynamically by the *PartitionedMonitorAdapter* for each adapter profile (see below) and called to transform the data.

The class implementation must implement the interface *com.intel.partitioned_monitor.DataTransformer*. The implementation must only ever throw *com.intel.partitioned_monitor.DataTransformerException*. The constructor should never throw an exception. The logger is available because it was passed into the required constructor.

### 4.2 Acting on Transformed Data
After the data is normalized the *PartitionedMonitorAdapter* does not understand specifically how to process the data. So the *PartitionedMonitorAdapter* implements a interface exposing all possible calls into the tier1, tier2, and tier3 data stores. This interface along with the processed data is passed into the DataAction implementation for processing. This provider will perform actions which includes storing data, firing RAS events, and/or data aggregation.

The class implementation must implement the interface *com.intel.partitioned_monitor.DataAction*. The implementation must only ever throw *com.intel.partitioned_monitor.DataActionException*. The constructor should never throw an exception. The logger is available because it was passed into the required constructor.

### 4.3 Network Request Builders (HTTP based protocols only)
When making a new connection via an HTTP protocol like `sse`, the request is usually either a **GET** method (“sse” only) or a **POST** method (`sse`). In the case of a **GET**, the url path is usually followed by a URI query string with selection, filters or other variables. For a **POST**, there is usually a JSON body with the same type of information. Since the query and body depend on the server (source) the intended connection parameters, etc... the configuration cannot be stored and must be created dynamically.

To solve this problem a class of providers is supported to be injected into the query (for **GET**) or the body (for **POST**) of the initial connection HTTP request.

The class implementation must implement the interface *com.intel.networking.restclient.SSERequestBuilder*. The implementation must never throw any exceptions but may return an empty string. If the builder is for a **GET** URI query the “?” must be the first character of the returned string. If the build is for a **POST** then JSON must be used. The constructor should never throw an exception. The logger is available because it was passed into the required constructor.

### 4.4 Bearer Token Authentication (OAuth 2.0)
When making a new connection to a REST API that is protected by token based OAuth 2.0 a implementation class is needed to retrieve the token to send with each REST request. This string token will be the value used in the HTTP header line for each request:
```
Authorization: Bearer <token_string>
```
The class implementation must implement the interface *com.intel.authentication.TokenAuthentication*. The class must have a defined constructor with no parameters allowed. The *initialize* implementation takes the arguments from the *providersConfiguration* section below and a *com.intel.logging.logger* instance.

The getToken() object returns the OAuth 2.0 string token to be used in HTTP communications.

## 5 Provider and Other Configurations (`providerConfigurations`)
This section contains the required configurations for each provider or component that requires a configuration.  The map is keys off the canonical name of the class to which the configuration belongs to. The map is opaque to the *PartitionedMonitoAdapter* and is only parsed by the owning provider or component.

So far the only non-provider component requiring a configuration is the implementation of the SystemActions interface: *com.intel.partitioned_monitor.PartitionedMonitorSystemActions*.  In this case it describes where to publish the raw and aggregated data from the adapter.

## 6 Network Stream Connection Definitions (`networkStreams`)
So describe each type of connection there is a section called `networkStreams` that defines all possible connections in the system being monitored. Each map entry must contain the string name of the *NetworkDataSink* implementation as used in the *NetworkDataSinkFactory*.

The second is the argument containing the subset of the connection and instantiation parameters used by the named implementation. This subset excludes the query portion of **GET** operation and the subjects list to receive.

The query or request body is dynamically build as in Section 4.3 Network Request Builders (HTTP based protocols only). The subjects are listed in the individual profiles so that a set of subjects can be split or combined into running adapter instances.

Example Section:
```json
{
  ...
  "networkStreams": {
    "softwareStreamRabbitMQ1": {
      "name": "rabbitmq",
      "arguments": {
        "exchangeName": "simulation",
        "uri": "amqp://127.0.0.1"
      }
    },
    ...
  }
  ...
}
```
The example above is simple and specific to RabbitMQ *NetworkDataSink* implementation. Below are specifics for each type of `networkStream` and variations.
### 6.1 SSE (Server Sent Events from a REST server)
The name field of this type is always “sse”.
#### 6.1.1 SSE + POST arguments
```json
...
"name": "sse",
"arguments": {
  "parser": "parser_for_incoming_data",
  "implementation": "restclient_implementation_name_for_factory",
  "requestType": "POST",
  "requestBuilder": "postBuilder_or_other_post_body_builder_name",
  "requestBuilderSelectors": {
    "name1": "value1",
    "name2": "value2",
    ...
  },
  "connectAddress": "rest_server_ip_address_or_hostname",
  "connectPort": "rest_server_port",
  "urlPath": "url_path_without_any_query",
  "tokenRetriever": "Optional_TokenAuthenticator_provider_class_name_goes_here"
}
...
```
#### 6.1.2 SSE + GET arguments
```json
…
"name": "sse",
"arguments": {
  "parser": "parser_for_incoming_data",
  "implementation": "restclient_implementation_name_for_factory",
  "requestType": "GET",
  "requestBuilder": "getBuilder_or_other_get_query_builder_name",
  "requestBuilderSelectors": {
    "name1": "value1",
    "name2": "value2",
    ...
  },
  "connectAddress": "rest_server_ip_address_or_hostname",
  "connectPort": "rest_server_port",
  "urlPath": "url_path_without_any_query",
  "tokenRetriever": "Optional_TokenAuthenticator_provider_class_name_goes_here"
}
...
```
### 6.2 HTTP Callback Subscriptions (REST servers)
The name field of this type is always `http_callback`.  More specific information is in the implementation resources so the arguments are small.
```json
...
"name": "http_callback",
"arguments": {
  "serverAddress": "subscription_server_ip_address",
  "serverPort": "subscription_server_port",
  "bindAddress": "callback_server_bind_address",
  "bindPort": "callback_server_bind_port"
}
...
```
### 6.3 True Pub/Sub Implementations
This section describes implementations that do NOT use HTTP but other true pub/sub models.
#### 6.3.1 RabbitMQ Broker
The name field of this type is always `rabbitmq`.
```json
...
"name": "rabbitmq",
"arguments": {
  "exchangeName": "rabbitmq_exchange_name",
  "uri": "amqp://rabbitmq_broker_ip_address_or_hostname"
}
...
```
## 7 Adapter Profiles (adapterProfiles)
This section defines all of the possible profiles that can be used by the *PartitionedMonitorAdapter* class.  Each profile describes a single data transformer provider, action provider, subject(s) provided, and the network stream(s) used to gather the data. Example:
```json
...
"adapterProfiles": {
  "allTelemetry": {
    "dataTransformProvider": "telemetryData",
    "actionProvider": "telemetryAction",
    "networkStreamsRef": [
      "nodeTelemetry",
      "rackTelemetry"
    ],
    "subjects": [
      "telemetry"
    ]
  }
}
...
```
In this example the name of the single profile is `allTelemetry` which will be passed as the 4th parameter on the adapter’s application command line:
```json
adapter_launcher <voltdb_servers> <this_location> <this_hostname> "allTelemetry"
```
This means that a profile can connect to one or more network resources and can provide one or more subjects (constrained by the “subjects” map above). In the partitioned “topic” scenario, the network streams must match one to one with the topic-partition pair but can be combined here in the individual profiles so that profile has a one to many relationship.
### 7.1 `dataTransformProvider`
This defines a data transform provider whose sole job is transform the data from the source’s format into the internal common format used by the PartitionedMonitorProvider and any action providers.
### 7.2 `actionProvider`
This defines an action provider that takes the transformed data and does something with it like store it in a DB, publish it, or take any other actions required.
### 7.3 `networkStreamRef`
This is an array of network streams to connect to for this profile. There must be at least one. See the  Network Stream Connection Definitions (`networkStreams`) above.
### 7.4 `subjects`
This lists the types of subjects provided by this profile. Although it is allowed, it is not recommended to have more than one subject per profile. You would have to carefully construct the data transform provider to understand the multiple types since only one data transformer provider can be specified per profile.
## *Appendix 1:*  Simple Example File (JSON)
```json
{
  "logProvider": "log4j2",
  "subjectMap": {
    "telemetry": "EnvironmentalData",
    "inventoryChanges": "InventoryChangeEvent",
    "logs": "LogData",
    "events": "RasEvent",
    "stateChanges": "StateChangeEvent"
  },
  "providerClassMap": {
    "telemetryAction": "com.intel.dai.monitoring_providers.TelemetryActions",
    "telemetryData": "com.intel.dai.monitoring_providers.TelemetryTransformer"
  },
  "providerConfigurations": {
    "com.intel.dai.monitoring_providers.TelemetryTransformer": {
      "useAggregation": true,
      "useTimeWindow": false,
      "windowSize": 25,
      "timeWindowSeconds": 600,
      "useMovingAverage": false
    },
    "com.intel.partitioned_monitor.PartitionedMonitorSystemActions": {
      "sourceType": "rabbitmq",
      "uri": "amqp://127.0.0.1",
      "exchange": "ucs"
    },
    "com.intel.authentication.KeycloakTokenAuthentication": {
      "tokenServer": "https://authentication_server:8080/auth",
      "realm": "myKingdom",
      "clientId": "myApplicationId",
      "clientSecret": "shhhhhhhhhhhh"
    }
  },
  "networkStreams": {
    "rackTelemetry": {
      "arguments": {
        "connectPort": 5678,
        "connectAddress": "127.0.0.1",
        "requestBuilder": "getBuilder",
        "requestType": "GET",
        "urlPath": "/streams/rackTelemetry",
        "tokenAuthProvider": "com.intel.authentication.KeycloakTokenAuthentication"
      },
      "name": "sse"
    },
    "nodeTelemetry": {
      "arguments": {
        "connectPort": 5678,
        "connectAddreauthTokenRetrieverss": "127.0.0.1",
        "requestBuilder": "postBuilder",
        "urlPath": "/streams/nodeTelemetry",
        "tokenAuthProvider": "com.intel.authentication.KeycloakTokenAuthentication"
      },
      "name": "sse"
    }
  },
  "adapterProfiles": {
    "allTelemetry": {
      "dataTransformProvider": "telemetryData",
      "actionProvider": "telemetryAction",
      "networkStreamsRef": [
        "nodeTelemetry"
      ],
      "subjects": [
        "telemetry"
      ]
    }
  }
}
```
## *Appendix 2:* Commented YAML Version of Above File
```yaml
##################################################################
# Example file in YAML format for the JSON version of this file.
##################################################################

# Logger provider.  Can be "console" or "log4j2"
logProvider: log4j2


# Map of subject names to internal DataType enum values.
subjectMap:
  telemetry: EnvironmentalData
  inventoryChanges: InventoryChangeEvent
  logs: LogData
  events: RasEvent
  stateChanges: StateChangeEvent


# Map internal names to actual classes to instantiate for providers (action and data
# transformers)
providerClassMap:
  telemetryAction: com.intel.dai.monitoring_providers.telemetryActions
  telemetryData: com.intel.dai.monitoring_providers.TelemetryTransformer


# Configuration for action and data transform providers as well as the
# PartitionedMonitorSystemActions configuration.
providerConfigurations:

  # This defines where the incoming data will be republished to.
  com.intel.partitioned_monitor.PartitionedMonitorSystemActions:
    sourceType: rabbitmq
    exchange: ucs
    uri: amqp://127.0.0.1

  # This defines the config for the aggregation features of the
  # TelemetryTransformer data transformer provider.
  com.intel.dai.monitoring_providers.TelemetryTransformer:
    useAggregation: true
    timeWindowSeconds: 600
    useTimeWindow: false
    windowSize: 25
    useMovingAverage: false

  # This defines the OAuth 2.0 values needed by the implmentation class specified.
  com.intel.authentication.KeycloakTokenAuthenticator:
    tokenServer: https://authentication_server:8080/auth
    realm: myKingdom
    clientId: myApplicationId
    clientSecret: shhhhhhhhhhhh



  # Defines the SSE resource points for the incoming data streams(s).
  networkStreams:
    nodeTelemetry:
      name: sse
      arguments:
        connectPort: 5678
        connectAddress: '127.0.0.1'
        requestBuilder: postBuilder
        urlPath: /streams/nodeTelemetry
        tokenAuthProvider: com.intel.authentication.KeycloakTokenAuthentication


  # Defines the profile(s) passed on the commandline at adapter start.
  adapterProfiles:
    allTelemetry:
      dataTransformProvider: telemetryData
      actionProvider: telemetryAction
      networkStreamsRef:
      - nodeTelemetry
      subjects:
      - telemetry
```
