# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the DeviceRegexResolver class.
"""
from unittest import TestCase
from ..device_regex_resolver import DeviceRegexResolver


class DeviceRegexResolverTest(TestCase):

    def test_get_devices(self):
        self.assertEqual(DeviceRegexResolver.get_devices('node00[1-3]'), 'node001,node002,node003')

    def test_get_devices_empty_devices(self):
        self.assertEqual(DeviceRegexResolver.get_devices(None), '')

    def test_get_devices_one_device(self):
        self.assertEqual(DeviceRegexResolver.get_devices('node001'), 'node001')

    def test_get_devices_raise_NodeSet_exception(self):
        try:
            DeviceRegexResolver.get_devices('!')
            self.fail()
        except RuntimeError:
            pass
