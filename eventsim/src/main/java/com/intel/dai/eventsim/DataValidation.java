package com.intel.dai.eventsim;

import com.intel.properties.PropertyMap;

import java.util.Map;

/**
 * Description of class DataValidation.
 * validate data for given keys
 * If data is null/empty throws error message
 */
class DataValidation {

    /**
     * This method is used to validate keys in given data
     * @param keys data keys
     * @param data to be validated against
     * @param message error message
     * @throws SimulatorException if key is missing/key-data is empty
     */
    static void validateKeys(final PropertyMap data, final String[] keys, final String message)
            throws SimulatorException {
        if(data == null || data.isEmpty() || keys == null || keys.length == 0 || message == null || message.length() == 0)
            throw new SimulatorException("data/keys/message is empty or null");

        for(String key : keys) {
            Object value = data.get(key);
            if(value == null || value.toString().isEmpty())
                throw new SimulatorException(message + " " + key);
        }
    }

    /**
     * This method is used to validate key-value data contains not null values
     * @throws SimulatorException if input key-value data contains null values
     */
    static void validateData(final Map<String, String> data, final String message)
            throws SimulatorException {
        if(data == null || data.isEmpty() || message == null || message.length() == 0)
            throw new SimulatorException("data/message is empty or null");

        for(Map.Entry<String,String> entry : data.entrySet()) {
            if(entry.getKey() == null || entry.getValue() == null)
                throw new SimulatorException(message + " " + entry);
        }
    }
}