// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

/**
 * Description of class NodeIpAndBmcIp.
 */
public class NodeIpAndBmcIp {
    public NodeIpAndBmcIp(String nodeIp, String bmcIp) {
        nodeIpAddress = nodeIp;
        bmcIpAddress = bmcIp;
    }
    public final String nodeIpAddress;
    public final String bmcIpAddress;
}
