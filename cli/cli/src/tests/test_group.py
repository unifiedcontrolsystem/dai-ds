# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the group class in cli implementation.
"""
import sys
import io
from unittest import TestCase
from mock import patch
from ..group import GroupCli
from ..parser import Parser
from .helper import Helper
from ..command_result import CommandResult


class GroupTest(TestCase):

    def test_group_help(self):
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['ucs', 'group']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('group [-h] {add,get,list,remove}', capturedOutput.getvalue())
        capturedOutput.close()

    def test_group_help_1(self):
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['ucs', 'group', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('group [-h] {add,get,list,remove}', capturedOutput.getvalue())
        capturedOutput.close()

    def test_group_help_2(self):
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['ucs', 'group', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('group [-h] {add,get,list,remove}', capturedOutput.getvalue())
        capturedOutput.close()

    @patch.object(GroupCli, '_group_get_execute')
    def test_group_add_device_positive(self, group_get):
        group_get.return_value = CommandResult(0, '')
        expected_output = 'Successfully modified the devices in group'
        actual_output = Helper.create_test_with_requests_put(['ucs', 'group', 'add', 'device',
                                                              'group1'], expected_output)
        self.assertIn(expected_output, actual_output)

    @patch.object(GroupCli, '_group_get_execute')
    def test_group_add_device_that_already_exists_positive(self, group_get):
        group_get.return_value = CommandResult(0, 'device')
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['ucs', 'group', 'add', 'device', 'group1']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('location(s) may already be part of the group', capturedOutput.getvalue())
        capturedOutput.close()


    @patch.object(GroupCli, '_group_get_execute')
    def test_group_add_device_that_already_exists_1_positive(self, group_get):
        group_get.return_value = CommandResult(0, 'device1')
        actual_output = Helper.create_test_with_requests_put(['ucs', 'group', 'add', 'device1,device2',
                                                              'group1'], 'Successfully modified the locations in group')
        self.assertIn('devices(s) may already be part of the group', actual_output)


    def test_group_add_device_to_group_with_comma(self):
        expected_output = "group name shouldn't contain comma"
        capturedOutput = io.StringIO()
        sys.stderr = capturedOutput
        parser = Parser()
        sys.argv = ['ucs', 'group', 'add', 'device', 'group1,group2']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stderr__
        self.assertIn(expected_output, capturedOutput.getvalue())
        capturedOutput.close()

    def test_group_add_device_error(self):
        expected_output = 'The requested method PUT is not allowed'
        actual_output = Helper.create_test_with_requests_put(['ucs', 'group', 'add', 'device',
                                                              'group1'], expected_output)
        self.assertIn(expected_output, actual_output)

    def test_group_remove_device_positive(self):
        expected_output = 'Successfully removed the devices in group'
        actual_output = Helper.create_test_with_requests_delete(['ucs', 'group', 'remove', 'device',
                                                                 'group1'], expected_output)
        self.assertIn(expected_output, actual_output)

    def test_group_remove_device_from_group_with_comma(self):
        expected_output = "group name shouldn't contain comma"
        capturedOutput = io.StringIO()
        sys.stderr = capturedOutput
        parser = Parser()
        sys.argv = ['ucs', 'group', 'remove', 'device', 'group1,group2']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stderr__
        self.assertIn(expected_output, capturedOutput.getvalue())
        capturedOutput.close()

    def test_group_remove_device_error(self):
        expected_output = 'The requested method DELETE is not allowed'
        actual_output = Helper.create_test_with_requests_delete(['ucs', 'group', 'remove', 'device',
                                                                 'group1'], expected_output)
        self.assertIn(expected_output, actual_output)

    def test_group_get_positive(self):
        expected_output = 'c01,c02,c03'
        actual_output = Helper.create_test_with_requests_get(['ucs', 'group', 'get', 'group1'], expected_output)
        self.assertIn(expected_output, actual_output)

    def test_group_get_error(self):
        expected_output = 'groups/group1 was not found on this server'
        actual_output = Helper.create_test_with_requests_get_error(['ucs', 'group', 'get', 'group1'])
        self.assertIn(actual_output, expected_output)

    def test_group_list_positive(self):
        expected_output = 'group1'
        actual_output = Helper.create_test_with_requests_get(['ucs', 'group', 'list'], expected_output)
        self.assertIn(expected_output, actual_output)

    def test_group_list_error(self):
        expected_output = 'groups was not found on this server'
        actual_output = Helper.create_test_with_requests_get_error(['ucs', 'group', 'list'])
        self.assertIn(actual_output, expected_output)