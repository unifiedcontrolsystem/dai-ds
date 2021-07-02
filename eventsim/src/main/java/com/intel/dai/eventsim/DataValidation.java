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
     * @param errMessage error message
     * @throws SimulatorException if key is missing/key-data is empty
     */
    static void validateKeysAndNullValues(final PropertyMap data, final String[] keys, final String errMessage)
            throws SimulatorException {
        if(data == null || data.isEmpty())
            throw new SimulatorException("given data to validate cannot be null or empty.");
        if(keys == null || keys.length == 0)
            throw new SimulatorException("keys to validate against cannot be null or empty.");
        if(errMessage == null || errMessage.isEmpty())
            throw new SimulatorException("error message to display error cannot be null or empty.");

        for(String key : keys) {
            Object value = data.get(key);
            if(value == null)
                throw new SimulatorException(errMessage + " " + key);
        }
    }

    /**
     * This method is used to validate key in given data
     * @param key data key
     * @param data to be validated against
     * @param message error message
     * @throws SimulatorException if key is missing/key-data is empty
     */
    static void validateKey(final PropertyMap data, final String key, final String message)
            throws SimulatorException {
        validateKeysAndNullValues(data, new String[] {key}, message);
    }

    /**
     * This method is used to validate key in given data
     * @param key data key
     * @param data to be validated against
     * @param message error message
     * @throws SimulatorException if key is missing/key-data is empty
     */
    static void validateKey(final Map<String, String> data, final String key, final String message)
            throws SimulatorException {
        validateKeysAndNullValues(new PropertyMap(data), new String[] {key}, message);
    }

    /**
     * This method is used to validate key-value data contains not null values
     * @throws SimulatorException if input key-value data contains null values
     */
    static void isValueNullOrEmpty(final Map<String, String> data, String errMessage)
            throws SimulatorException {
        if(data == null || data.isEmpty())
            throw new SimulatorException("given data to validate cannot be null or empty.");
        if(errMessage == null || errMessage.isEmpty())
            throw new SimulatorException("message to display error cannot be null or empty.");

        for(Map.Entry<String,String> entry : data.entrySet()) {
            if(entry.getValue() == null || entry.getValue().isEmpty())
                throw new SimulatorException(errMessage + " " + entry.getKey());
        }
    }

    /**
     * This method is used to validate key-value data contains not null values
     * @throws SimulatorException if input key-value data contains null values
     */
    static void isNull(Object data, String errMessage)
            throws SimulatorException {
        if(data == null)
            throw new SimulatorException("given data to validate cannot be null or empty.");
        if(errMessage == null || errMessage.isEmpty())
            throw new SimulatorException("error message to display error cannot be null or empty.");
    }
}