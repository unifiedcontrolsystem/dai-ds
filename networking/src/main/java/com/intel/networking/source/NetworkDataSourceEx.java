package com.intel.networking.source;

/**
 * Adds new functionality without modifying the parent interface.
 */
public interface NetworkDataSourceEx extends NetworkDataSource {
    /**
     * Add/set the additional properties needed to configure for a data publisher.
     * @param property name of publisher property
     * @param value value of publisher property
     */
    void setPublisherProperty(String property, Object value);

    /**
     * get the publisher property value needed to configure for data publisher.
     * @param property name of publisher property
     * @return value of publisher property
     */
    Object getPublisherProperty(String property);
}