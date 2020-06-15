# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Module to test HTTP client APIs
"""
import requests
import json
import tempfile, shutil
import os
from unittest import TestCase
from mock import patch, PropertyMock
from ..http_client import HttpClient


class Response(object):

    def __init__(self):
        self.text = json.dumps({"Location": "Test", "Status": "F", "Result": ('{"innerloop": "success"}')})
        self.ok = True


class HttpClientTest(TestCase):

    def setUp(self):
        self.test_dir = tempfile.mkdtemp()
        self.temp_configuration_file_location = os.path.join(self.test_dir, 'test.json')
        with open(self.temp_configuration_file_location, 'w') as f:
            data = dict()
            data['rest_server_address'] = 'localhost'
            data['rest_server_port'] = '4567'
            json.dump(data, f)


    def tearDown(self):
        shutil.rmtree(self.test_dir)

    def test_read_server_information_from_configuration_file_FileNotFoundError(self):
        temp_configuration_file_location = os.path.join(self.test_dir, 'non_existent_file.json')
        with self.assertRaises(RuntimeError) as e:
            HttpClient(temp_configuration_file_location)

    def test_read_server_information_from_configuration_file_ValueError_invalid_json(self):
        temp_configuration_file_location = os.path.join(self.test_dir, 'existing_file.json')
        with open(temp_configuration_file_location, 'w') as f:
            f.write("data")
        with self.assertRaises(RuntimeError) as e:
            HttpClient(temp_configuration_file_location)

    def test_read_server_information_from_configuration_file_KeyError_port_is_missing(self):
        temp_configuration_file_location = os.path.join(self.test_dir, 'existing_file.json')
        with open(temp_configuration_file_location, 'w') as f:
            data = dict()
            data['rest_server_address'] = 'localhost'
            json.dump(data, f)
        with self.assertRaises(RuntimeError) as e:
            HttpClient(temp_configuration_file_location)

    def test_get_base_url(self):
        client = HttpClient(self.temp_configuration_file_location)
        self.assertEqual('http://localhost:4567/', client.get_base_url())

    def test_send_request(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.get') as patched_get:
            patched_get.return_value=Response()
            response_code, response = client.send_get_request('http://localhost:4567/mock', 100)
            self.assertEqual(0, response_code)

    def test_send_request_ConnectionError(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.get') as patched_get:
            with self.assertRaises(RuntimeError) as e:
                patched_get.side_effect = requests.exceptions.ConnectionError
                response_code, response = client.send_get_request('http://localhost:4567/mock', 100)
                self.assertEqual(1, response_code)

    def test_send_request_Timeout(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.get') as patched_get:
            with self.assertRaises(RuntimeError) as e:
                patched_get.side_effect = requests.exceptions.Timeout
                response_code, response = client.send_get_request('http://localhost:4567/mock', 100)
                self.assertEqual(1, response_code)

    def test_put_request(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.put') as patched_put:
            patched_put.return_value=Response()
            response_code, response = client.send_put_request('http://localhost:4567/mock', None, 100)
            self.assertEqual(0, response_code)

    def test_put_request_ConnectionError(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.put') as patched_put:
            with self.assertRaises(RuntimeError) as e:
                patched_put.side_effect = requests.exceptions.ConnectionError
                response_code, response = client.send_put_request('http://localhost:4567/mock', None, 100)
                self.assertEqual(1, response_code)

    def test_put_request_Timeout(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.put') as patched_put:
            with self.assertRaises(RuntimeError) as e:
                patched_put.side_effect = requests.exceptions.Timeout
                response_code, response = client.send_put_request('http://localhost:4567/mock', None, 100)
                self.assertEqual(1, response_code)

    def test_post_request(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.post') as patched_post:
            patched_post.return_value=Response()
            response_code, response = client.send_post_request('http://localhost:4567/mock', None, 100)
            self.assertEqual(0, response_code)

    def test_post_request_ConnectionError(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.post') as patched_post:
            with self.assertRaises(RuntimeError) as e:
                patched_post.side_effect = requests.exceptions.ConnectionError
                response_code, response = client.send_post_request('http://localhost:4567/mock', None, 100)
                self.assertEqual(1, response_code)

    def test_post_request_Timeout(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.post') as patched_post:
            with self.assertRaises(RuntimeError) as e:
                patched_post.side_effect = requests.exceptions.Timeout
                response_code, response = client.send_post_request('http://localhost:4567/mock', None, 100)
                self.assertEqual(1, response_code)

    def test_delete_request(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.delete') as patched_delete:
            patched_delete.return_value=Response()
            response_code, response = client.send_delete_request('http://localhost:4567/mock', 100)
            self.assertEqual(0, response_code)

    def test_delete_request_ConnectionError(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.delete') as patched_delete:
            with self.assertRaises(RuntimeError) as e:
                patched_delete.side_effect = requests.exceptions.ConnectionError
                response_code, response = client.send_delete_request('http://localhost:4567/mock', 100)
                self.assertEqual(1, response_code)

    def test_delete_request_Timeout(self):
        client = HttpClient(self.temp_configuration_file_location)
        with patch('requests.delete') as patched_delete:
            with self.assertRaises(RuntimeError) as e:
                patched_delete.side_effect = requests.exceptions.Timeout
                response_code, response = client.send_delete_request('http://localhost:4567/mock', 100)
                self.assertEqual(1, response_code)

    # def test_send_request_nb_initial_request_error(self):
    #     client = HttpClient(self.temp_configuration_file_location)
    #     with patch('requests.get') as patched_get:
    #         with self.assertRaises(RuntimeError) as e:
    #             patched_get.side_effect = requests.exceptions.ConnectionError
    #             response_code, response = client.send_get_request_nb('http://localhost:4567/mock', 100)
    #
    # def test_send_request_nb_KeyError(self):
    #     client = HttpClient(self.temp_configuration_file_location)
    #     with patch('requests.get') as patched_get:
    #         type(patched_get.return_value).text = \
    #             PropertyMock(return_value='{"Status": "FE", "Result": "Success"}')
    #         response_code, response = client.send_get_request_nb('http://localhost:4567/mock', 100)
    #         self.assertEqual(1, response_code)
