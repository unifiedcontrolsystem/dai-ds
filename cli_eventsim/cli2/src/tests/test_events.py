# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the service class in cli implementation.
"""
import sys
import io
from unittest import TestCase
from ..parser import Parser


class ServiceTest(TestCase):

    def test_events_help(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['eventsim', 'events']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events [-h] {ras,sensor,boot} ...\n\npositional arguments:\n  {ras,sensor,'
                      'boot}  Subparser for events\n    ras              generate ras events.\n    sensor           '
                      'generate sensor events.\n    boot             generate boot events.\n\noptional arguments:\n  '
                      '-h, --help         show this help message and exit\n', capturedOutput.getvalue())
        capturedOutput.close()

    def test_events_help_1(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'events', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events [-h] {ras,sensor,boot} ...\n\npositional arguments:\n  {ras,sensor,'
                      'boot}  Subparser for events\n    ras              generate ras events.\n    sensor           '
                      'generate sensor events.\n    boot             generate boot events.\n\noptional arguments:\n  '
                      '-h, --help         show this help message and exit\n', capturedOutput.getvalue())
        capturedOutput.close()

    def test_events_help_2(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'events', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events [-h] {ras,sensor,boot} ...\n\npositional arguments:\n  {ras,sensor,'
                      'boot}  Subparser for events\n    ras              generate ras events.\n    sensor           '
                      'generate sensor events.\n    boot             generate boot events.\n\noptional arguments:\n  '
                      '-h, --help         show this help message and exit\n', capturedOutput.getvalue())
        capturedOutput.close()

    def test_ras_events_help(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'events', 'ras', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events ras [-h] [--count COUNT] [--location LOCATION]\n                        '
                      '   [--burst] [--label LABEL]\n\noptional arguments:\n  -h, --help           show this help '
                      'message and exit\n  --count COUNT        Provide number of ras events to be generated. The\n   '
                      '                    default values are in config file.\n  --location LOCATION  generate ras '
                      'events at a given location.\n  --burst              generate events with or without delay.\n  '
                      '--label LABEL        generate ras events of a particular type\n', capturedOutput.getvalue())
        capturedOutput.close()

    def test_sensor_events_help(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'events', 'sensor', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events sensor [-h] [--count COUNT] [--location LOCATION]\n                     '
                      '         [--burst] [--label LABEL]\n\noptional arguments:\n  -h, --help           show this '
                      'help message and exit\n  --count COUNT        Provide number of ras events to be generated. '
                      'The\n                       default values are in config file.\n  --location LOCATION  '
                      'generate sensor events at a given location.\n  --burst              generate events with or '
                      'without delay.\n  --label LABEL        generate sensor events of a particular type\n',
                      capturedOutput.getvalue())
        capturedOutput.close()

    def test_boot_events_help(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'events', 'boot', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim events boot [-h] [--type {off,on,ready}]\n                            ['
                      '--probability PROBABILITY] [--burst]\n                            [--location '
                      'LOCATION]\n\noptional arguments:\n  -h, --help            show this help message and exit\n  '
                      '--type {off,on,ready}\n                        types of boot events to generate. Default '
                      'will\n                        generate all types of boot events\n  --probability PROBABILITY\n '
                      '                       generate boot events with probability failure\n  --burst               '
                      'generate events with or without delay.\n  --location LOCATION   generate boot events at a '
                      'given location.\n', capturedOutput.getvalue())
        capturedOutput.close()
