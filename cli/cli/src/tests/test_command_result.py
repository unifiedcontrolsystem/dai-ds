# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the command result class in cli implementation.
"""
from unittest import TestCase
import json
from ..command_result import CommandResult


class CommandResultTest(TestCase):

    def test_stringify(self):
        result1 = CommandResult(1, "success")
        self.assertEqual('1 - success', str(result1))

    def test_equal(self):
        result1 = CommandResult(1, "success")
        result2 = CommandResult(1, "success")
        self.assertTrue(result1 == result2)

    def test_not_equal(self):
        result1 = CommandResult(-1, "Fail")
        result2 = CommandResult(1, "success")
        self.assertFalse(result1 == result2)

    def test_not_equal_2(self):
        result1 = "1"
        result2 = CommandResult(1, "success")
        self.assertFalse(result1 == result2)

    def test_parse_command_response(self):
        response = json.dumps({"Location": "Test", "Status": "FE", "Result": ('{"innerloop": "success"}')})
        result  = CommandResult.parse_command_response(response)
        self.assertIn("success", result)
