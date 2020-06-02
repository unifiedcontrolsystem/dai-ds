# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

"""
Module to create HTTP requests.
"""
import sys
import os
import requests
import json


class HttpClient(object):
    """Class to handle HTTP communication"""

    def __init__(self, config_file_location="/opt/ucs/etc/eventsim_cli_config.json"):
        self._config_file_location = config_file_location
        self._check_non_blocking_status_interval = 1
        self._base_url = self._construct_base_url_from_configuration_file()

    def _construct_base_url_from_configuration_file(self):
        if not os.path.exists(self._config_file_location):
            sys.stderr.write('\nError: The configuration file "{}" is missing!\n'.format(self._config_file_location))
            sys.stderr.write('       Please create this file at "{}" with the following format:\n'.
                             format(self._config_file_location))
            sys.stderr.write('{\n  "rest_server_address": "127.0.0.1",\n  "rest_server_port": "5678"\n}\n')
            sys.stderr.write('       Make sure to use the correct address and port for the eventsim service.\n\n')
            sys.stderr.flush()
            e = 'Configuration file is missing: {0}'.format(FileNotFoundError(self._config_file_location))
            raise RuntimeError(e)

        try:
            with open(self._config_file_location) as f:
                data = json.load(f)
                base_url = "http://" + data['rest_server_address'] + ":" + data['rest_server_port'] + "/"
        except KeyError as e:
            e = '{0} is missing in the configuration file'.format(e)
            raise RuntimeError(e)
        except ValueError as e:
            e = 'Invalid JSON style: {0}'.format(e)
            raise RuntimeError(e)
        return base_url

    def get_base_url(self):
        return self._base_url

    def send_get_request(self, request_string, tval):
        try:
            response = requests.get(request_string, timeout=tval)
        except requests.exceptions.ConnectionError:
            msg = "Could not connect to server. Is the server running at {0}?".format(self._base_url.strip('/'))
            raise RuntimeError(msg)
        except requests.exceptions.Timeout:
            msg = "Request timed out. Please try again"
            raise RuntimeError(msg)
        status, rs_response = self._parse_http_response(response.text)
        if status == 'E':
            raise RuntimeError(rs_response)
        return 0, rs_response

    def send_post_request(self, request_string, payload, tval):
        try:
            response = requests.post(request_string, json=payload, timeout=tval)
        except requests.exceptions.ConnectionError:
            msg = "Could not connect to server. Is the server running at {0}?".format(self._base_url.strip('/'))
            raise RuntimeError(msg)
        except requests.exceptions.Timeout:
            msg = "Request timed out. Please try again"
            raise RuntimeError(msg)
        status, rs_response = self._parse_http_response(response.text)
        if status == 'E':
            raise RuntimeError(rs_response)
        return 0, rs_response

    def _parse_http_response(self, response):
        try:
            json_response = json.loads(response)
        except Exception as exp:
            return 'E', str(exp)
        if 'Status' in json_response and 'Result' in json_response:
            return json_response['Status'], json_response['Result']
        else:
            return 'F', json_response
