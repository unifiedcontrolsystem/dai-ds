// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

/**
 * Description of class BasicNodeInfo.
 */
class BasicNodeInfo {
    BasicNodeInfo(String hostname, long sequenceNumber, boolean serviceNode, String owner) {
        hostname_ = hostname;
        sequenceNumber_ = sequenceNumber;
        serviceNode_ = serviceNode;
        owner_ = owner;
    }

    String getHostname() { return hostname_; }
    long getSequenceNumber() { return sequenceNumber_; }
    boolean isServiceNode() { return serviceNode_; }
    boolean isComputeNode() { return !serviceNode_; }
    boolean isOwnedByWlm() { return owner_.equalsIgnoreCase("W"); }
    String getOwningSubSystem() { return owner_; }

    private final String hostname_;
    private final long sequenceNumber_;
    private final boolean serviceNode_;
    private final String owner_;
}
