# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the service class in cli implementation.
"""
import json
import sys
import io
from unittest import TestCase

from mock import patch

from ..parser import Parser


class EventsCliTest(TestCase):

    def test_events_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events [-h] {ras,sensor,job,boot,scenario,get-seed} ...\n\npositional '
                      'arguments:\n  {ras,sensor,job,boot,scenario,get-seed}\n                        subparser for '
                      'events\n    ras                 generate ras events\n    sensor              generate sensor '
                      'events\n    job                 generate job events\n    boot                generate boot '
                      'events\n    scenario            generate events for a given scenario\n    get-seed            '
                      'fetch prior seed to replicate same data.\n\noptional arguments:\n  -h, --help            show '
                      'this help message and exit\n',
            captured_output.getvalue())
        captured_output.close()

    def test_events_help_1(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events [-h] {ras,sensor,job,boot,scenario,get-seed} ...\n\npositional '
                      'arguments:\n  {ras,sensor,job,boot,scenario,get-seed}\n                        subparser for '
                      'events\n    ras                 generate ras events\n    sensor              generate sensor '
                      'events\n    job                 generate job events\n    boot                generate boot '
                      'events\n    scenario            generate events for a given scenario\n    get-seed            '
                      'fetch prior seed to replicate same data.\n\noptional arguments:\n  -h, --help            show '
                      'this help message and exit\n',
            captured_output.getvalue())
        captured_output.close()

    def test_ras_events_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'ras', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events ras [-h] [--burst] [--count COUNT] [--delay DELAY]\n                    '
                      '       [--label LABEL] [--locations LOCATIONS]\n                           [--output OUTPUT] ['
                      '--seed SEED] [--timeout TIMEOUT]\n\noptional arguments:\n  -h, --help            show this '
                      'help message and exit\n  --burst               generate ras events without delay. Default is '
                      'constant\n                        mode with delay.\n  --count COUNT         given number of '
                      'ras events are generated. The default\n                        values exists in eventsim '
                      'config file.\n  --delay DELAY         pause for given value in microseconds to generate ras\n  '
                      '                      events. The default values exists in eventsim config\n                   '
                      '     file.\n  --label LABEL         generate ras events for a given type/description\n  '
                      '--locations LOCATIONS\n                        generate ras events at a given location. '
                      'Provide regex\n                        for multiple locations.\n  --output OUTPUT       store '
                      'data in a file\n  --seed SEED           seed to duplicate data\n  --timeout TIMEOUT     ras '
                      'sub-command execution timeout\n', captured_output.getvalue())
        captured_output.close()

    def test_sensor_events_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'sensor', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events sensor [-h] [--burst] [--count COUNT] [--delay DELAY]\n                 '
                      '             [--label LABEL] [--locations LOCATIONS]\n                              [--output '
                      'OUTPUT] [--seed SEED]\n                              [--timeout TIMEOUT]\n\noptional '
                      'arguments:\n  -h, --help            show this help message and exit\n  --burst               '
                      'generate sensor events without delay. Default is\n                        constant mode with '
                      'delay.\n  --count COUNT         given number of sensor events are generated. The\n             '
                      '           default values exists in eventsim config file.\n  --delay DELAY         pause for '
                      'given value in microseconds to generate\n                        sensor events. The default '
                      'values exists in eventsim\n                        config file.\n  --label LABEL         '
                      'generate sensor events for a given type/description\n  --locations LOCATIONS\n                 '
                      '       generate sensor events at a given location. Provide\n                        regex for '
                      'multiple locations.\n  --output OUTPUT       store data in a file\n  --seed SEED           '
                      'seed to duplicate data\n  --timeout TIMEOUT     sensor sub-command execution timeout\n',
            captured_output.getvalue())
        captured_output.close()

    def test_boot_events_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'boot', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events boot [-h] [--burst] [--delay DELAY]\n                            ['
                      '--locations LOCATIONS] [--output OUTPUT]\n                            [--probability '
                      'PROBABILITY] [--seed SEED]\n                            [--timeout TIMEOUT] [--type {off,on,'
                      'ready}]\n\noptional arguments:\n  -h, --help            show this help message and exit\n  '
                      '--burst               generate boot events without delay. Default is\n                        '
                      'constant mode with delay.\n  --delay DELAY         pause for given value in microseconds to '
                      'generate boot\n                        events. The default values exists in eventsim config\n  '
                      '                      file.\n  --locations LOCATIONS\n                        generate boot '
                      'events at a given location. Provide\n                        regex for multiple locations.\n  '
                      '--output OUTPUT       store data in a file\n  --probability PROBABILITY\n                      '
                      '  generate boot events with probability failure. Default\n                        no '
                      'failure.\n  --seed SEED           seed to duplicate data\n  --timeout TIMEOUT     boot '
                      'sub-command execution timeout\n  --type {off,on,ready}\n                        generate given '
                      'type of boot events. Default generates\n                        all [on/off/ready] types of '
                      'boot events.\n', captured_output.getvalue())
        captured_output.close()

    def test_scenario_events_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'scenario', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events scenario [-h] [--burst] [--counter COUNTER]\n                           '
                      '     [--delay DELAY] [--duration DURATION]\n                                [--locations '
                      'LOCATIONS] [--output OUTPUT]\n                                [--probability PROBABILITY]\n    '
                      '                            [--ras-label RAS_LABEL]\n                                ['
                      '--sensor-label SENSOR_LABEL] [--seed SEED]\n                                [--start-time '
                      'START_TIME] [--timeout TIMEOUT]\n                                [--mode {burst,group-burst,'
                      'repeat}]\n                                file\n\npositional arguments:\n  file                '
                      '  scenario configuration file\n\noptional arguments:\n  -h, --help            show this help '
                      'message and exit\n  --burst               generate events for a given scenario without '
                      'delay.\n                        Default is constant mode with delay.\n  --counter COUNTER     '
                      'repeat scenario for a given counter\n  --delay DELAY         pause for given value in '
                      'microseconds to generate\n                        events for a given scenario. The default '
                      'values exists\n                        in eventsim config file.\n  --duration DURATION   '
                      'scenario occurs for a given duration. The default\n                        units is minutes '
                      'only.\n  --locations LOCATIONS\n                        generate events for a given scenario '
                      'at a given\n                        location. Provide regex for multiple locations.\n  '
                      '--output OUTPUT       Store data in a file.\n  --probability PROBABILITY\n                     '
                      '   generate boot events with probability failure\n  --ras-label RAS_LABEL\n                    '
                      '    generate ras events of a particular type/description\n  --sensor-label SENSOR_LABEL\n      '
                      '                  generate sensor events of a particular\n                        '
                      'type/description\n  --seed SEED           seed to duplicate data\n  --start-time START_TIME\n  '
                      '                      start time to generate events for a given scenario\n  --timeout TIMEOUT  '
                      '   scenario sub-command execution timeout\n  --mode {burst,group-burst,repeat}\n               '
                      '         generate events given type of scenario. Default\n                        generates '
                      'burst type scenario. Scenario data exists in\n                        scenario config file.\n',
            captured_output.getvalue())
        captured_output.close()

    def test_get_seed_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'get-seed', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events get-seed [-h] [--seed SEED] [--timeout TIMEOUT]\n\noptional '
                      'arguments:\n  -h, --help         show this help message and exit\n  --seed SEED        seed to '
                      'duplicate data\n  --timeout TIMEOUT  get-seed sub-command execution timeout\n',
            captured_output.getvalue())
        captured_output.close()

    def test_ras_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'ras']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_ras_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'ras']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create ras events"
                                })
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create ras events\n', captured_output.getvalue())
        captured_output.close()

    def test_sensor_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'sensor']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_sensor_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'sensor']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create sensor events"
                                })
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create sensor events\n', captured_output.getvalue())
        captured_output.close()

    def test_jobs_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'job']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_jobs_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'job']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create job events"
                                })
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create job events\n', captured_output.getvalue())
        captured_output.close()

    def test_boot_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'boot']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_boot_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'boot']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create boot events"
                                })
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create boot events\n', captured_output.getvalue())
        captured_output.close()

    def test_scenario_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'scenario', '/tmp/scenario.json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_scenario_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'scenario', '/tmp/scenario.json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create scenario events"
                                })
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create scenario events\n', captured_output.getvalue())
        captured_output.close()

    def test_get_seed_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'get-seed']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "123"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - 123', captured_output.getvalue())
        captured_output.close()

    def test_get_seed_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'get-seed']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to get seed data"
                                })
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to get seed data\n', captured_output.getvalue())
        captured_output.close()
