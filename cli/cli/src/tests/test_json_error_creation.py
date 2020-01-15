# -*- coding: utf-8 -*-
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the JSON Error creation class in cli implementation.
"""
from unittest import TestCase
import json
from ..json_error_creation import JsonError


class TestJsonError(TestCase):

    def test_construct_error_result(self):
        error_result = JsonError('creating a error value').construct_error_result()
        json_error = json.loads(error_result)
        self.assertEqual(json_error['result-status-code'], 1)
        self.assertEqual(json_error['error'][0], ['creating a error value'])
