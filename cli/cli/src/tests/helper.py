# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Helper class for tests in cli implementation.
"""
import sys
import io
from mock import patch, PropertyMock
from ..parser import Parser


class Helper(object):

    @staticmethod
    def create_test_with_requests_get(command, expected_result):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "F",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_get_req_with_status(command, expected_result):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "F",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_get_error(command):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_put(command, expected_result):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.put') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "F",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_put_error_code(command, expected_result):
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.put') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "E",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_post(command, expected_result):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "F",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_post_error_code(command, expected_result):
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.post') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "E",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_delete(command, expected_result):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.delete') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "F",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        output = captured_output.getvalue()
        captured_output.close()
        return output

    @staticmethod
    def create_test_with_requests_delete_error_code(command, expected_result):
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = command
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/12345:"
            with patch('requests.delete') as patched_get:
                type(patched_get.return_value).text = \
                    PropertyMock(return_value='{"Location": "Test", "Status": "E",'
                                              ' "Result": "' + expected_result + '"}')
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        output = captured_output.getvalue()
        captured_output.close()
        return output
