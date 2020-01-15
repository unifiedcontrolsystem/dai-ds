# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Helper class to resolve device regex.
"""
from ClusterShell.NodeSet import NodeSet, RESOLVER_NOGROUP, NodeSetParseError, RangeSetParseError, fold


class DeviceRegexResolver(object):

    @staticmethod
    def get_devices(devices):
        """Gets the device name"""
        device_list = ''
        if devices:
            try:
                device_list = list(NodeSet(devices, resolver=RESOLVER_NOGROUP))
            except (NodeSetParseError, RangeSetParseError) as e:
                raise RuntimeError("Not a valid device name")
        return ','.join(device_list)

    @staticmethod
    def fold_devices(device_list):
        """Fold the devices in list to a regex"""
        devices = ",".join(device_list)
        device_set = NodeSet(devices)
        return fold(device_set)
