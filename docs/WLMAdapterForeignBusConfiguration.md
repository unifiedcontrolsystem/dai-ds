# Configuration for Provider Class com.intel.dai.resource_managers.cobalt.AdapterWlmCobalt; #
## Example: ##
```json
{
    "bootstrap.servers": "kafka_hosted_machine:9092",
    "group.id": "respective_subscribed_group_id",
    "schema.registry.url": "http://kafka_hosted_machine:8081",
    "auto.commit.enable": "true",
    "auto.offset.reset": "earliest",
    "specific.avro.reader": "true",
    "topics": "respective_subscribed_topic"
}
```
## JSON Config Reference ##

### bootstrap.servers ###
To establish an initial connection to the Kafka cluster. Server hosted details of kafka uses/runs port 9092

### group.id ###
Identifies the consumer group of a given consumer. group_id of the topic names of kafka bus

### schema.registry.url ###
Registered schema URL the provider is expecting message to send to. URL of the registered schemas of kafka bus

### auto.commit.enable ###
Consumer's offset will be periodically committed in the background. By default, it is set to false

### auto.offset.reset ###
Offset from which next new record will be fetched. Used ONLY if consumer doesn't have a valid offset committed. 
(earliest/latest). By default, it is earliest.

### specific.avro.reader ###
Consumer expects avro type messages. By default, it is set to true.

### topic ###
topic name to which provider should be subscribed to listen data.
