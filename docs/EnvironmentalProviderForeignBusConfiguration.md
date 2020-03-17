# Configuration for Provider Class EnvironmentalProviderForeignBus #
## Provider full Classname: __com.intel.dai.monitoring.EnvironmentalProviderForeignBus__ ##
## Example: ##
```json
"providerConfigurations": {
    "com.intel.dai.monitoring.EnvironmentalProviderForeignBus": {
        "publishRawTopic": "ucs_raw_data",
        "publishAggregatedTopic": "ucs_aggregated_data",
        "publish": true,
        "useAggregation": true,
        "useTimeWindow": false,
        "windowSize": 25,
        "useMovingAverage": false,
        "timeWindowSeconds": 300
    }
}
```
## JSON Config Reference ##

### publish ###
This enables publishing of all data if set to _true_.  The default is _false_.

### publishRawTopic ###
If enabled (publish=true) then this is the subject/topic/routing key used to publish the raw environmental data on. The default is _ucs_aggregate_data_.

### publishAggregatedTopic ###
If enabled (publish=true) then this is the subject/topic/routing key used to publish the aggregated environmental data on. The default is _ucs_raw_data_.

### useAggregation ###
This is a _true_ or _false_ boolean that allows simple data aggregation to occur (_true_) or not (_false_). The default is _true_.

### useTimeWindow ###
This is a _true_ or _false_ boolean that selects the use of a time window (_true_) or a count window (_false_) for data aggregation. The default is _false_.

### windowSize ###
If a count window is used (useTimeWindow=false), then this value is how many samples at a time will be aggreegated. THe default is a count of 25.

### timeWindowSeconds ###
If a time windows is used (useTimeWindow=true), then this is the number of seconds to wait for data to accumulate before aggregating it. The default is 600 seconds.

### useMovingAverage ###
This can change the behavior of the average from a simple window average (_false) to a moving average (_true_) over the selected window type and size. The default is _false_.
