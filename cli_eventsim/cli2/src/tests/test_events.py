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
        self.assertIn('usage: eventsim events [-h]\n                       {ras,sensor,job,boot,scenario,get-seed,'
                      'echo,list-locations}\n                       ...\n\npositional arguments:\n  {ras,sensor,job,'
                      'boot,scenario,get-seed,echo,list-locations}\n                        subparser for events\n    '
                      'ras                 generate ras events\n    sensor              generate sensor events\n    '
                      'job                 generate job events\n    boot                generate boot events\n    '
                      'scenario            generate events for a given scenario\n    get-seed            fetch prior '
                      'seed to replicate same data.\n    echo                echo json directly to connection\n    '
                      'list-locations      fetch locations data available in system.\n\noptional arguments:\n  -h, '
                      '--help            show this help message and exit\n',
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
        self.assertIn('usage: eventsim events [-h]\n                       {ras,sensor,job,boot,scenario,get-seed,'
                      'echo,list-locations}\n                       ...\n\npositional arguments:\n  {ras,sensor,job,'
                      'boot,scenario,get-seed,echo,list-locations}\n                        subparser for events\n    '
                      'ras                 generate ras events\n    sensor              generate sensor events\n    '
                      'job                 generate job events\n    boot                generate boot events\n    '
                      'scenario            generate events for a given scenario\n    get-seed            fetch prior '
                      'seed to replicate same data.\n    echo                echo json directly to connection\n    '
                      'list-locations      fetch locations data available in system.\n\noptional arguments:\n  -h, '
                      '--help            show this help message and exit\n',
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
                      '       [--locations LOCATIONS] [--output OUTPUT]\n                           [--seed SEED] ['
                      '--template TEMPLATE]\n                           [--timeout TIMEOUT] [--timezone TIMEZONE]\n   '
                      '                        [--type {fabric-crit,old-ras}]\n                           ['
                      '--update-field-jpath UPDATE_FIELD_JPATH]\n                           [--update-field-metadata '
                      'UPDATE_FIELD_METADATA]\n                           [--update-field-metadata-filter '
                      'UPDATE_FIELD_METADATA_FILTER]\n                           [--template-field-jpath '
                      'TEMPLATE_FIELD_JPATH]\n                           [--template-field-filter '
                      'TEMPLATE_FIELD_FILTER]\n\noptional arguments:\n  -h, --help            show this help message '
                      'and exit\n  --burst               generate ras events without delay. Default is constant\n     '
                      '                   mode with delay\n  --count COUNT         given number of ras events are '
                      'generated. The default\n                        values exists in eventsim config file\n  '
                      '--delay DELAY         pause for given value in microseconds to generate ras\n                  '
                      '      events. The default values exists in eventsim config\n                        file\n  '
                      '--locations LOCATIONS\n                        generate ras events at a given location. '
                      'Provide regex\n                        for multiple locations\n  --output OUTPUT       save '
                      'data to a file\n  --seed SEED           seed to duplicate data\n  --template TEMPLATE   sample '
                      'template to generate ras events\n  --timeout TIMEOUT     ras sub-command execution timeout\n  '
                      '--timezone TIMEZONE   generate ras events for a given timezone. The default\n                  '
                      '      values exists in config file\n  --type {fabric-crit,old-ras}\n                        '
                      'provide type of the ras event to generate events\n  --update-field-jpath UPDATE_FIELD_JPATH\n  '
                      '                      Provide multiple json-paths to field only, separated\n                   '
                      '     by comma(,). Ex: level0/level1[*]/item1\n  --update-field-metadata '
                      'UPDATE_FIELD_METADATA\n                        Provide file path with all possible values. '
                      'Ex:\n                        /tmp/source.json\n  --update-field-metadata-filter '
                      'UPDATE_FIELD_METADATA_FILTER\n                        Provide multiple regex value to fill '
                      'data in update-\n                        field-jpath separated by comma(,). Ex:\n              '
                      '          level0/level1[*]/item1\n  --template-field-jpath TEMPLATE_FIELD_JPATH\n              '
                      '          Provide multiple json-paths to template-field only,\n                        '
                      'separated by comma(,). Ex: level0/level1[*]/item1\n  --template-field-filter '
                      'TEMPLATE_FIELD_FILTER\n                        Provide multiple regex value to fill data in '
                      'template-\n                        field-jpath separated by comma(,). Ex:\n                    '
                      '    level0/level1[*]/item1\n', captured_output.getvalue())
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
                      '             [--locations LOCATIONS] [--output OUTPUT]\n                              [--seed '
                      'SEED] [--template TEMPLATE]\n                              [--timeout TIMEOUT] [--timezone '
                      'TIMEZONE]\n                              [--type {energy,fabric-perf,power,temperature,'
                      'voltage}]\n                              [--update-field-jpath UPDATE_FIELD_JPATH]\n           '
                      '                   [--update-field-metadata UPDATE_FIELD_METADATA]\n                           '
                      '   [--update-field-metadata-filter UPDATE_FIELD_METADATA_FILTER]\n                             '
                      ' [--template-field-jpath TEMPLATE_FIELD_JPATH]\n                              ['
                      '--template-field-filter TEMPLATE_FIELD_FILTER]\n\noptional arguments:\n  -h, --help            '
                      'show this help message and exit\n  --burst               generate sensor events without delay. '
                      'Default is\n                        constant mode with delay\n  --count COUNT         given '
                      'number of sensor events are generated. The\n                        default values exists in '
                      'eventsim config file\n  --delay DELAY         pause for given value in microseconds to '
                      'generate\n                        sensor events. The default values exists in eventsim\n       '
                      '                 config file\n  --locations LOCATIONS\n                        generate sensor '
                      'events at a given location. Provide\n                        regex for multiple locations\n  '
                      '--output OUTPUT       save data to a file\n  --seed SEED           seed to duplicate data\n  '
                      '--template TEMPLATE   sample template to generate sensor events\n  --timeout TIMEOUT     '
                      'sensor sub-command execution timeout\n  --timezone TIMEZONE   generate sensor events for given '
                      'timezone. The default\n                        values exists in config file\n  --type {energy,'
                      'fabric-perf,power,temperature,voltage}\n                        provide type of the sensor '
                      'event to generate events\n  --update-field-jpath UPDATE_FIELD_JPATH\n                        '
                      'Provide multiple json-paths to field only, separated\n                        by comma(,'
                      '). Ex: level0/level1[*]/item1\n  --update-field-metadata UPDATE_FIELD_METADATA\n               '
                      '         Provide file path with all possible values. Ex:\n                        '
                      '/tmp/source.json\n  --update-field-metadata-filter UPDATE_FIELD_METADATA_FILTER\n              '
                      '          Provide multiple regex value to fill data in update-\n                        '
                      'field-jpath separated by comma(,). Ex:\n                        level0/level1[*]/item1\n  '
                      '--template-field-jpath TEMPLATE_FIELD_JPATH\n                        Provide multiple '
                      'json-paths to template-field only,\n                        separated by comma(,'
                      '). Ex: level0/level1[*]/item1\n  --template-field-filter TEMPLATE_FIELD_FILTER\n               '
                      '         Provide multiple regex value to fill data in template-\n                        '
                      'field-jpath separated by comma(,). Ex:\n                        level0/level1[*]/item1\n',
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
        self.assertIn('IMEOUT]\n                            [--timezone TIMEZONE] [--type {off,on,ready}]\n           '
                      '                 [--update-field-jpath UPDATE_FIELD_JPATH]\n                            ['
                      '--update-field-metadata UPDATE_FIELD_METADATA]\n                            ['
                      '--update-field-metadata-filter UPDATE_FIELD_METADATA_FILTER]\n                            ['
                      '--template-field-jpath TEMPLATE_FIELD_JPATH]\n                            ['
                      '--template-field-filter TEMPLATE_FIELD_FILTER]\n\noptional arguments:\n  -h, --help            '
                      'show this help message and exit\n  --burst               generate boot events without delay. '
                      'Default is\n                        constant mode with delay.\n  --delay DELAY         pause '
                      'for given value in microseconds to generate boot\n                        events. The default '
                      'values exists in eventsim config\n                        file.\n  --locations LOCATIONS\n     '
                      '                   generate boot events at a given location. Provide\n                        '
                      'regex for multiple locations.\n  --output OUTPUT       store data in a file\n  --probability '
                      'PROBABILITY\n                        generate boot events with probability failure. Default\n  '
                      '                      no failure.\n  --seed SEED           seed to duplicate data\n  '
                      '--template TEMPLATE   sample template to generate boot events\n  --timeout TIMEOUT     boot '
                      'sub-command execution timeout\n  --timezone TIMEZONE   generate boot events for given '
                      'timezone. The default\n                        values exists in config file\n  --type {off,on,'
                      'ready}\n                        generate given type of boot events. Default generates\n        '
                      '                all [on/off/ready] types of boot events.\n  --update-field-jpath '
                      'UPDATE_FIELD_JPATH\n                        Provide multiple json-paths to field only, '
                      'separated\n                        by comma(,). Ex: level0/level1[*]/item1\n  '
                      '--update-field-metadata UPDATE_FIELD_METADATA\n                        Provide file path with '
                      'all possible values. Ex:\n                        /tmp/source.json\n  '
                      '--update-field-metadata-filter UPDATE_FIELD_METADATA_FILTER\n                        Provide '
                      'multiple regex value to fill data in update-\n                        field-jpath separated by '
                      'comma(,). Ex:\n                        level0/level1[*]/item1\n  --template-field-jpath '
                      'TEMPLATE_FIELD_JPATH\n                        Provide multiple json-paths to template-field '
                      'only,\n                        separated by comma(,). Ex: level0/level1[*]/item1\n  '
                      '--template-field-filter TEMPLATE_FIELD_FILTER\n                        Provide multiple regex '
                      'value to fill data in template-\n                        field-jpath separated by comma(,'
                      '). Ex:\n                        level0/level1[*]/item1\n', captured_output.getvalue())
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
                      'LOCATIONS] [--output OUTPUT]\n                                [--probability PROBABILITY] ['
                      '--seed SEED]\n                                [--start-time START_TIME] [--timeout TIMEOUT]\n  '
                      '                              [--timezone TIMEZONE]\n                                [--type {'
                      'burst,group-burst,repeat}]\n                                file\n\npositional arguments:\n  '
                      'file                  scenario configuration file. The file should exist in\n                  '
                      '      installed configuration files folder. Ex:\n                        '
                      '/opt/ucs/etc/\n\noptional arguments:\n  -h, --help            show this help message and '
                      'exit\n  --burst               generate events for a given scenario without delay.\n            '
                      '            Default is constant mode with delay.\n  --counter COUNTER     repeat scenario for '
                      'a given counter times\n  --delay DELAY         pause for given value in microseconds to '
                      'generate\n                        events for a given scenario. The default values exists\n     '
                      '                   in eventsim config file.\n  --duration DURATION   scenario occurs for a '
                      'given duration time. The default\n                        units is minutes only.\n  '
                      '--locations LOCATIONS\n                        generate events for a given scenario at a '
                      'given\n                        location. Provide regex for multiple locations.\n  --output '
                      'OUTPUT       Store data in a file.\n  --probability PROBABILITY\n                        '
                      'generate boot events with probability failure\n  --seed SEED           seed to duplicate '
                      'data\n  --start-time START_TIME\n                        start time/scheduled time to generate '
                      'events for a\n                        given scenario\n  --timeout TIMEOUT     scenario '
                      'sub-command execution timeout\n  --timezone TIMEZONE   generate events for given timezone. The '
                      'default values\n                        exists in config file\n  --type {burst,group-burst,'
                      'repeat}\n                        generate events for a given type of scenario. Default\n       '
                      '                 generates burst type scenario. Scenario data exists in\n                      '
                      '  scenario config file.\n',
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

    def test_list_locations_help(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'list-locations', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events list-locations [-h] [--timeout TIMEOUT]\n\noptional arguments:\n  -h, '
                      '--help         show this help message and exit\n  --timeout TIMEOUT  list-locations '
                      'sub-command execution timeout\n',
                      captured_output.getvalue())
        captured_output.close()

    def test_ras_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'ras']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_ras_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'ras']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create ras events"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create ras events\n', captured_output.getvalue())
        captured_output.close()

    def test_sensor_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'sensor']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_sensor_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'sensor']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create sensor events"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create sensor events\n', captured_output.getvalue())
        captured_output.close()

    def test_jobs_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'job']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_jobs_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'job']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create job events"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create job events\n', captured_output.getvalue())
        captured_output.close()

    def test_boot_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'boot']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_boot_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'boot']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create boot events"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create boot events\n', captured_output.getvalue())
        captured_output.close()

    def test_scenario_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'scenario', '/tmp/scenario.json']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "Success"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - Success', captured_output.getvalue())
        captured_output.close()

    def test_scenario_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'scenario', '/tmp/scenario.json']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to create scenario events"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to create scenario events\n', captured_output.getvalue())
        captured_output.close()

    def test_get_seed_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'get-seed']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "123"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - 123', captured_output.getvalue())
        captured_output.close()

    def test_get_seed_negative(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stderr = captured_output
        sys.argv = ['eventsim', 'events', 'get-seed']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "E",
                                "Result": "Error:unable to get seed data"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Error:unable to get seed data\n', captured_output.getvalue())
        captured_output.close()

    def test_list_locations_positive(self):
        parser = Parser()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        sys.argv = ['eventsim', 'events', 'list-locations']
        with patch('cli2.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "[\"location-0\",\"location-1\"]"
                                })
                type(patched_get.return_value).status_code = 200
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('0 - ["location-0","location-1"]', captured_output.getvalue())
