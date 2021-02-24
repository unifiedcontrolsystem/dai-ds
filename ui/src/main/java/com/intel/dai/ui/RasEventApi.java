package com.intel.dai.ui;

import com.intel.dai.dsapi.EventsLog;
import com.intel.dai.exceptions.ProviderException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.util.Map;
import java.util.Set;

public class RasEventApi {

    public RasEventApi(EventsLog eventLog) throws ProviderException {
        if (eventLog == null)
            throw  new ProviderException("Eventslog should be provided");
        _eventLog = eventLog;
    }

    public void createRasEvent(Map<String, String> param) throws ProviderException {
        String eventtype = param.getOrDefault("eventtype", null);
        validateRasDescriptiveName(eventtype);
        generateRasEvent(param);
    }

    private void validateRasDescriptiveName(String name) throws ProviderException {
        if (!_eventLog.checkDescriptiveName(name))
            throw new ProviderException("Error: Invalid descriptive name: " + name);
    }

    private void generateRasEvent(Map<String, String> param) throws ProviderException {
        try {
            _eventLog.createRasEvent(param);
        } catch (Exception e) {
            throw new ProviderException("Error in creating rasevents");
        }
    }

    private PropertyArray extractSchemaFromEventTypeObject(PropertyMap eventTypeObject) {
        PropertyArray schema = new PropertyArray();
        Set<String> objectKeys = eventTypeObject.keySet();
        for (String key: objectKeys) {
            PropertyMap columnInfo = new PropertyMap();
            columnInfo.put("data", key);
            columnInfo.put("unit", "string");
            columnInfo.put("heading", key);
            schema.add(columnInfo);
        }
        return schema;
    }

    private PropertyArray extractValuesFromEventTypes(PropertyArray eventTypes)
            throws PropertyNotExpectedType, ProviderException{
        PropertyArray data = new PropertyArray();
        for (int eventTypeIndex = 0; eventTypeIndex < eventTypes.size(); eventTypeIndex++) {
            PropertyMap eventTypeObject = eventTypes.getMap(eventTypeIndex);
            if (eventTypeObject == null) {
                throw new ProviderException("'nodeObject' cannot be null!");
            }
            data.add(eventTypeObject.values());
        }
        return data;
    }

    public PropertyMap getRasEventTypes(Map<String, String> parameters) throws ProviderException{
        PropertyArray result = _eventLog.listAllRasEventTypes(parameters);
        if (result == null || result.isEmpty()) {
            throw new ProviderException("No RasEventTypes found");
        }

        PropertyMap rasEventTypes = new PropertyMap();
        try {
            PropertyMap eventTypeObject = result.getMap(0);
            rasEventTypes.put("result-data-columns" ,eventTypeObject.size());
            rasEventTypes.put("result-data-lines" ,result.size());
            rasEventTypes.put("result-status-code" ,0);
            rasEventTypes.put("schema", extractSchemaFromEventTypeObject(eventTypeObject));
            rasEventTypes.put("data", extractValuesFromEventTypes(result));
        } catch (PropertyNotExpectedType e) {
            throw new ProviderException(e.getMessage());
        }
        return rasEventTypes;
    }

    private EventsLog _eventLog;
}
