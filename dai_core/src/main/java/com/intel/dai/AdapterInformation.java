// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Data Storage for common Adapter information.
 */
public class AdapterInformation {
    /**
     * Create the instance with the fields used.
     * @param type The adapter type.
     * @param name The adapter base name.
     * @param id The adapter id.
     */
    public AdapterInformation(String type, String name, String location, String hostname, long id) {
        type_ = type;
        name_ = name;
        location_ = location;
        hostname_ = hostname;
        setId(id);
        pid_ = ProcessHandle.current().pid();
    }

    /**
     * Create the instance with the fields used.
     * @param type The adapter type.
     * @param name The adapter base name.
     */
    public AdapterInformation(String type, String name, String location, String hostname) {
        this(type, name, location, hostname, -1L);
    }

    /**
     * Get the adapter base name.
     *
     * @return The string passed in the constructor.
     */
    public String getBaseName() { return name_; }

    /**
     * Get the adapter unique name (with ID included).
     *
     * @return The instance unique name.
     */
    public String getName() {
        return getBaseName() + "_" + ((uniqueNameExtension_ == null)?getId():uniqueNameExtension_);
    }

    /**
     * Get the adapter type.
     *
     * @return The adapter type.
     */
    public String getType() { return type_; }

    /**
     * Get the adapter instance running location.
     *
     * @return The location.
     */
    public String getLocation() { return location_; }

    /**
     * Get the hostname this adapter is running on.
     *
     * @return The hostname, usually from the adapter commandline arguments.
     */
    public String getHostname() { return hostname_; }

    /**
     * Get the adapter ID number.
     *
     * @return The adapter id.
     */
    public long getId() { return id_; }

    /**
     * Set the adapter ID after construction.
     *
     * @param id The ID for this adapter.
     */
    public void setId(long id) { id_ = id; }

    /**
     * Get the adapter instance PID.
     *
     * @return The PID value.
     */
    public long getPid() { return pid_; }

    /**
     * Get the shutdown flag.
     *
     * @return true is shutting down or false if not shutting down.
     */
    public boolean isShuttingDown() {
        return shutdown_.get();
    }

    /**
     * Set the shutdown flag to true.
     */
    public void signalToShutdown() {
        shutdown_.set(true);
    }

    /**
     * Get the base work item ID for the running adapter.
     *
     * @return The base work item ID is set or -1L is it was never set.
     */
    public long getBaseWorkItemId() { return baseWorkItemId_; }

    /**
     * Set the base work item ID.
     *
     * @param id_ The ID for the base work item.
     */
    public void setBaseWorkItemId(long id_) { baseWorkItemId_ = id_; }

    /**
     * Get the list of servers for the tier1 DB.
     *
     * @return The array of servers.  Can never ne null and defaults to one entry with 127.0.0.1 as the value.
     */
    public String[] getServers() { return servers_; }

    /**
     * Set the list of servers.
     *
     * @param servers The array od servers. Will assert if passed a null.
     */
    public void setServers(String[] servers) { assert servers != null; servers_ = servers; }

    /**
     * Instead of using the adapter ID any unique string can be used to make the adapter name unique. This will set
     * that string. Setting null will use the adapter ID.
     *
     * @param extension The new unique string or null to use the adapter ID.
     */
    public void setUniqueNameExtension(String extension) {
        uniqueNameExtension_ = extension;
    }

    private final String name_;
    private final String type_;
    private final String location_;
    private final String hostname_;
    private       long id_;
    private final long pid_;
    private final AtomicBoolean shutdown_ = new AtomicBoolean(false);
    private       long baseWorkItemId_ = -1L;
    private       String[] servers_ = new String[] {"127.0.0.1"};
    private       String uniqueNameExtension_ = null;
}
