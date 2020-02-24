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


class WlmTest(TestCase):

    def test_wlm_help(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['eventsim', 'wlm']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim wlm [-h]\n                    {create_reservation,modify_reservation,'
                      'delete_reservation,start_job,terminate_job,simulate}\n                    ...\n\npositional '
                      'arguments:\n  {create_reservation,modify_reservation,delete_reservation,start_job,'
                      'terminate_job,simulate}\n                        Subparser for wlm\n    create_reservation  '
                      'generate a log event for a created reservation.\n    modify_reservation  generate a log event '
                      'for a modified reservation.\n    delete_reservation  generate a log event for a deleted '
                      'reservation.\n    start_job           generate a log event for a started job.\n    '
                      'terminate_job       generate a log event for a terminated job.\n    simulate            '
                      'generate random log events for jobs and reservations.\n\noptional arguments:\n  -h, '
                      '--help            show this help message and exit\n', capturedOutput.getvalue())
        capturedOutput.close()

    def test_events_help_1(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'wlm', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim wlm [-h]\n                    {create_reservation,modify_reservation,'
                      'delete_reservation,start_job,terminate_job,simulate}\n                    ...\n\npositional '
                      'arguments:\n  {create_reservation,modify_reservation,delete_reservation,start_job,'
                      'terminate_job,simulate}\n                        Subparser for wlm\n    create_reservation  '
                      'generate a log event for a created reservation.\n    modify_reservation  generate a log event '
                      'for a modified reservation.\n    delete_reservation  generate a log event for a deleted '
                      'reservation.\n    start_job           generate a log event for a started job.\n    '
                      'terminate_job       generate a log event for a terminated job.\n    simulate            '
                      'generate random log events for jobs and reservations.\n\noptional arguments:\n  -h, '
                      '--help            show this help message and exit\n', capturedOutput.getvalue())
        capturedOutput.close()

    def test_events_help_2(self):
        parser = Parser()
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        sys.argv = ['eventsim', 'wlm', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: eventsim wlm [-h]\n                    {create_reservation,modify_reservation,'
                      'delete_reservation,start_job,terminate_job,simulate}\n                    ...\n\npositional '
                      'arguments:\n  {create_reservation,modify_reservation,delete_reservation,start_job,'
                      'terminate_job,simulate}\n                        Subparser for wlm\n    create_reservation  '
                      'generate a log event for a created reservation.\n    modify_reservation  generate a log event '
                      'for a modified reservation.\n    delete_reservation  generate a log event for a deleted '
                      'reservation.\n    start_job           generate a log event for a started job.\n    '
                      'terminate_job       generate a log event for a terminated job.\n    simulate            '
                      'generate random log events for jobs and reservations.\n\noptional arguments:\n  -h, '
                      '--help            show this help message and exit\n', capturedOutput.getvalue())
        capturedOutput.close()
