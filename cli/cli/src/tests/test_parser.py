# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the parser class in cli implementation.
"""
import sys
import io
import timeout_decorator
import argparse
from mock import Mock
from mock import patch
from unittest import TestCase
from ..parser import Parser
from ..command_result import CommandResult


class ParserTest(TestCase):

    def test_parser_without_argument(self):
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        parser = Parser()
        sys.argv = ['ucs']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('usage: ucs [-h] [-V]', capturedOutput.getvalue())
        capturedOutput.close()

    def test_parser_help(self):
        parser = Parser()
        sys.argv = ['ucs', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()

    def test_positive_handle_command_result(self):
        capturedOutput = io.StringIO()
        sys.stdout = capturedOutput
        positive_cmd_result = CommandResult(0, "positive_result")
        Parser.handle_command_result(positive_cmd_result)
        sys.stdout = sys.__stdout__
        self.assertEqual('0 - positive_result\n', capturedOutput.getvalue())

    def test_negative_handle_command_result(self):
        capturedOutput = io.StringIO()
        sys.stderr = capturedOutput
        negative_cmd_result = CommandResult(-1, "negative_result")
        Parser.handle_command_result(negative_cmd_result)
        sys.stderr = sys.__stderr__
        self.assertEqual('-1 - negative_result\n', capturedOutput.getvalue())

    @patch('argparse.ArgumentParser.parse_args')
    @patch.object(Parser, 'handle_command_result')
    def test_parser_keyboardInterruptException(self, handle_command_result_mock, mock_parse_args):
        mock_parse_args.return_value = argparse.Namespace(func=Mock())
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'power', 'on', 'tes']
        handle_command_result_mock.side_effect = KeyboardInterrupt
        return_code = parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        self.assertEqual(1, return_code)
        self.assertEqual('1 - User has interrupted the command execution\n', output)

    @patch('argparse.ArgumentParser.parse_args')
    @patch.object(Parser, 'handle_command_result')
    def test_parser_runtimeErrorException(self, handle_command_result_mock, mock_parse_args):
        mock_parse_args.return_value = argparse.Namespace(func=Mock())
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'power', 'on', 'tes']
        handle_command_result_mock.side_effect = RuntimeError('Error in executing command')
        return_code = parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        self.assertEqual(1, return_code)
        self.assertEqual('1 - Error in executing command\n', output)

    @patch('argparse.ArgumentParser.parse_args')
    @patch.object(Parser, 'handle_command_result')
    def test_parser_IOErrorException(self, handle_command_result_mock, mock_parse_args):
        mock_parse_args.return_value = argparse.Namespace(func=Mock())
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'power', 'on', 'tes']
        handle_command_result_mock.side_effect = IOError
        return_code = parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        self.assertEqual(1, return_code)
        self.assertEqual('1 - User cannot enter optional arguments multiple times\n', output)