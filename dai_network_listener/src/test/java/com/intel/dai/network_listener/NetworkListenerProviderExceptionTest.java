// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import org.junit.Test;

public class NetworkListenerProviderExceptionTest {
    @Test
    public void allConstructors() {
        new NetworkListenerProviderException();
        new NetworkListenerProviderException("Message");
        new NetworkListenerProviderException(new Exception("CAUSE"));
        new NetworkListenerProviderException("Message", new Exception("CAUSE"));
    }
}
