// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.network_listener;

import com.intel.dai.dsapi.DataStoreFactory;

public interface NetworkListenerProviderEx extends NetworkListenerProvider {
    void setFactory(DataStoreFactory factory);
}
