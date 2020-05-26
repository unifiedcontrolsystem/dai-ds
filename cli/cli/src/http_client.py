# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Module to create HTTP requests.
"""
import requests
import json
import sys

class HttpClient(object):
    """Class to handle HTTP communication"""

    def __init__(self, config_file_location="/opt/ucs/etc/cli_config.json"):
        self._config_file_location = config_file_location
        self._check_non_blocking_status_interval = 1
        self._base_url = self._construct_base_url_from_configuration_file()
        self.proxies = {
          "http": None,
          "https": None
        }

    def _construct_base_url_from_configuration_file(self):
        try:
            with open(self._config_file_location) as f:
                data = json.load(f)
                base_url = "http://" + data['rest_server_address'] + ":" + data['rest_server_port'] + "/"
        except FileNotFoundError as e:
            e = 'Configuration file is missing: {0}'.format(e)
            raise RuntimeError(e)
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
            if(not response.ok):
                sys.stderr.write("Could not connect to server. Retrying by bypassing proxy env variables...\n")
                response = requests.get(request_string, timeout=tval, proxies=self.proxies)
        except requests.exceptions.ConnectionError:
            msg = "Could not connect to server. Is the server running at {0}?".format(self._base_url)
            raise RuntimeError(msg)
        except requests.exceptions.Timeout:
            msg = "Request timed out. Please try again"
            raise RuntimeError(msg)
        except ValueError as e:
            raise RuntimeError(e)
        status, rs_response = self._parse_http_response(response.text)
        if status == 'E':
            raise RuntimeError(rs_response)
        if status == 'FE':
            return 1, rs_response
        return 0, rs_response

    def send_put_request(self, request_string, data, tval):
        try:
            response = requests.put(request_string, data, timeout=tval)
            if(not response.ok):
                sys.stderr.write("Could not connect to server. Retrying by bypassing proxy env variables...\n")
                response = requests.get(request_string, timeout=tval, proxies=self.proxies)
        except requests.exceptions.ConnectionError:
            msg = "Could not connect to server. Is the server running at {0}?".format(self._base_url)
            raise RuntimeError(msg)
        except requests.exceptions.Timeout:
            msg = "Request timed out. Please try again"
            raise RuntimeError(msg)
        except ValueError as e:
            raise RuntimeError(e)
        status, rs_response = self._parse_http_response(response.text)
        if status == 'E':
            raise RuntimeError(rs_response)
        if status == 'FE':
            return 1, rs_response
        return 0, rs_response

    def send_post_request(self, request_string, data, tval):
        try:
            response = requests.post(request_string, data, timeout=tval)
            if(not response.ok):
                sys.stderr.write("Could not connect to server. Retrying by bypassing proxy env variables...\n")
                response = requests.get(request_string, timeout=tval, proxies=self.proxies)
        except requests.exceptions.ConnectionError:
            msg = "Could not connect to server. Is the server running at {0}?".format(self._base_url)
            raise RuntimeError(msg)
        except requests.exceptions.Timeout:
            msg = "Request timed out. Please try again"
            raise RuntimeError(msg)
        except ValueError as e:
            raise RuntimeError(e)
        status, rs_response = self._parse_http_response(response.text)
        if status == 'E':
            raise RuntimeError(rs_response)
        if status == 'FE':
            return 1, rs_response
        return 0, rs_response

    def send_delete_request(self, request_string, tval):
        try:
            response = requests.delete(request_string, timeout=tval)
            if(not response.ok):
                sys.stderr.write("Could not connect to server. Retrying by bypassing proxy env variables...\n")
                response = requests.get(request_string, timeout=tval, proxies=self.proxies)
        except requests.exceptions.ConnectionError:
            msg = "Could not connect to server. Is the server running at {0}?".format(self._base_url)
            raise RuntimeError(msg)
        except requests.exceptions.Timeout:
            msg = "Request timed out. Please try again"
            raise RuntimeError(msg)
        except ValueError as e:
            raise RuntimeError(e)
        status, rs_response = self._parse_http_response(response.text)
        if status == 'E':
            raise RuntimeError(rs_response)
        if status == 'FE':
            return 1, rs_response
        return 0, rs_response

    # def send_get_request_nb(self, request_string, tval):
    #     """
    #     Deprecated code. Will be revisited to check if we need this feature
    #     Non blocking GET request.
    #     :param request_string:
    #     :param tval:
    #     :return:
    #     """
    #     response_code, response = self.send_get_request(request_string, tval)
    #     if response_code > 0:
    #         return response_code, response
    #     try:
    #         track_req = json.loads(response.replace("\\\\", ""))['Location']
    #     except (KeyError, ValueError) as e:
    #         return 1, e
    #     request_done = False
    #     response = None
    #     spinner = Spinner('Command in progress   ')
    #     while request_done is False:
    #         for _ in range(self._check_non_blocking_status_interval * 4):
    #             spinner.next()
    #             time.sleep(1)
    #         response = requests.get(track_req, timeout=tval)
    #         json_response = json.loads(response.text.replace("\\\\", ""))
    #         if json_response['Status'] in 'FE':
    #             request_done = True
    #     status, rs_response = self._parse_http_response(response.text)
    #     if status == 'E':
    #         raise RuntimeError(rs_response)
    #     return 0, rs_response

    @staticmethod
    def append_params(params):
        """
        Return set of parameters with a form  '&key1=value1&key2=value2&key3=value3'
        :param params:
        :return:
        """
        p_str = ''
        for key, val in params.items():
            if val is not None:
                p_str += '&' + key + '=' + val
        return p_str

    def _parse_http_response(self, response):
        try:
            json_response = json.loads(response)
            return json_response['Status'], json_response['Result']
        except Exception:
            # Unable to parse it as JSON
            return "E", response
