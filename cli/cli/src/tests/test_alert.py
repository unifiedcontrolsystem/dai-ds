# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the alert class in cli implementation.
"""
import sys
import io
from unittest import TestCase
from mock import patch, PropertyMock
from ..parser import Parser
import json


class AlertTest(TestCase):

    def test_alert_help(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{close,history,list,view}', captured_output.getvalue())
        captured_output.close()

    def test_alert_help_1(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{close,history,list,view}', captured_output.getvalue())
        captured_output.close()

    def test_alert_help_2(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{close,history,list,view}', captured_output.getvalue())
        captured_output.close()

    def test_alert_list(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert', 'list', '--format=json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status": "F",
                                                                  "Result": ('[{"id": "1",'
                                                                             '"type": "TEST",'
                                                                             '"description": "",'
                                                                             '"creationtime": 0,'
                                                                             '"closetime": 0,'
                                                                             '"state": "OPEN",'
                                                                             '"events": ""}]')})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('id', captured_output.getvalue())
        captured_output.close()

    def test_alert_history(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert', 'history', '--format=json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status": "F",
                                                                  "Result": ('[{"id": "1",'
                                                                             '"type": "TEST",'
                                                                             '"description": "",'
                                                                             '"creationtime": 0,'
                                                                             '"closetime": 0,'
                                                                             '"state": "CLOSED",'
                                                                             '"events": ""}]')})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('id', captured_output.getvalue())
        captured_output.close()

    def test_alert_view(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert', 'view', '1', '--format=json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.put') as patched_put:
                type(patched_put.return_value).text = json.dumps({"Status": "F",
                                                                  "Result": ('[{"id": "1",'
                                                                             '"type": "TEST",'
                                                                             '"description": "",'
                                                                             '"creationtime": 0,'
                                                                             '"closetime": 0,'
                                                                             '"state": "OPEN",'
                                                                             '"events": ""}]')})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('id', captured_output.getvalue())
        captured_output.close()

    def test_alert_close(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'alert', 'close', '--id', '1']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.put') as patched_put:
                type(patched_put.return_value).text = json.dumps({"Status": "F", "Result": "Success"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('Success', captured_output.getvalue())
        captured_output.close()
