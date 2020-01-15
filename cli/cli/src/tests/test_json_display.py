# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the JSON Display class in cli implementation.
"""
import json
from unittest import TestCase
from ..json_display import JsonDisplay


class TestJsonDisplay(TestCase):

    def test_positive_status_code(self):
        json_display = JsonDisplay(json.dumps({"result-data-columns": 3, "result-status-code": 0,
                                               "result-data-lines": 3,
                                               "schema": [{"unit": "string", "data": "sub_property_name",
                                                           "heading": "sub_property_name"},
                                                          {"unit": "string", "data": "actual", "heading": "actual"},
                                                          {"unit": "string", "data": "reference",
                                                           "heading": "reference"}],
                                               "data": [["kernel_version", "31.2", "42.3"], ["os_version", "", ""],
                                                        ["version_level", "", ""]]}))
        self.assertIn('ACTUAL', json_display.display_json_in_tabular_format())

    def test_non_positive_status_code(self):
        json_display = JsonDisplay(json.dumps({"result-data-columns": 3, "result-status-code": 1,
                                               "result-data-lines": 3,
                                               "schema": [{"unit": "string", "data": "sub_property_name",
                                                           "heading": "sub_property_name"},
                                                          {"unit": "string", "data": "actual", "heading": "actual"},
                                                          {"unit": "string", "data": "reference",
                                                           "heading": "reference"}]}))
        with self.assertRaises(RuntimeError):
            json_display.display_json_in_tabular_format()

    def test_zero_columns_in_schema_returned(self):
        json_display = JsonDisplay(json.dumps({"result-data-columns": 0, "result-status-code": 0,
                                               "result-data-lines": 3,
                                               "schema": [], "data":[[]]}))
        with self.assertRaises(RuntimeError):
            json_display.display_json_in_tabular_format()

    def test_zero_data_lines_returned(self):
        json_display = JsonDisplay(json.dumps({"result-data-columns": 3, "result-status-code": 0,
                                               "result-data-lines": 0,
                                               "schema": [{"unit": "string", "data": "sub_property_name",
                                                           "heading": "sub_property_name"},
                                                          {"unit": "string", "data": "actual", "heading": "actual"},
                                                          {"unit": "string", "data": "reference",
                                                           "heading": "reference"}],
                                               "data": [[]]}))
        self.assertTrue("No data returned." in json_display.display_json_in_tabular_format())

    def test_json_missing_filed_key_error(self):
        json_display = JsonDisplay(json.dumps({"result-status-code": 0,
                                               "result-data-lines": 0,
                                               "schema": [{"unit": "string", "data": "sub_property_name",
                                                           "heading": "sub_property_name"},
                                                          {"unit": "string", "data": "actual", "heading": "actual"},
                                                          {"unit": "string", "data": "reference",
                                                           "heading": "reference"}],
                                               "data": [[]]}))
        with self.assertRaises(RuntimeError):
            json_display.display_json_in_tabular_format()

    def test_empty_json_data_to_display(self):
        with self.assertRaises(RuntimeError):
            JsonDisplay(None)
        with self.assertRaises(RuntimeError):
            JsonDisplay([])

    def test_display_raw_json(self):
        json_display = JsonDisplay(json.dumps({"result-data-columns": 3, "result-status-code": 0,
                                               "result-data-lines": 3,
                                               "schema": [{"unit": "string", "data": "sub_property_name",
                                                           "heading": "sub_property_name"},
                                                          {"unit": "string", "data": "actual", "heading": "actual"},
                                                          {"unit": "string", "data": "reference",
                                                           "heading": "reference"}],
                                               "data": [["kernel_version", "31.2", "42.3"], ["os_version", "", ""],
                                                        ["version_level", "", ""]]}))
        self.assertEqual([{'actual': '31.2', 'reference': '42.3', 'sub_property_name': 'kernel_version'},
                          {'actual': '', 'reference': '', 'sub_property_name': 'os_version'},
                          {'actual': '', 'reference': '', 'sub_property_name': 'version_level'}],
                         json.loads(json_display.display_raw_json()))
