# Configuration for Provider Class NetworkListenerProviderForeignBus #
## Provider full Classname: __com.intel.dai.provisioners.NetworkListenerProviderForeignBus__ ##
## Example: ##
```json
"providerConfigurations": {
    "com.intel.dai.provisioners.NetworkListenerProviderForeignBus": {
        "publishTopic": "ucs_ras_event",
        "publish": true
    }
}
```
## JSON Config Reference ##

### publish ###
This enables publishing of all RAS Events if set to _true_.  The default is _false_.

### publishTopic ###
If enabled (publish=true) then this is the subject/topic/routing key used to publish the RAS Events on. The default is _ucs_ras_event_.
