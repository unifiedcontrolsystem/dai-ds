// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

/**
 * Description of enum BootState.
 */
public enum BootState {
    NODE_OFFLINE,
    NODE_ONLINE,
    NODE_BOOTING,
    BIOS_STARTED_DUE_TO_RESET,
    SELECTING_BOOT_DEVICE,
    PXE_DOWNLOAD_NBP_FILE,
    KERNEL_BOOT_STARTED,
    ACTIVE,
    SHUTDOWN,
    UNKNOWN,
    IP_ADDRESS_ASSIGNED,
    DHCP_DISCOVERED,
}
