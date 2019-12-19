# -*- coding: utf-8 -*-
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Use this class to create a error response in JSON format. The following JSON format is compatible with non control
commands
"""
import json


class JsonError(object):
    """Class to create JSON error object. Expects a error message(string) as input to constructor"""

    def __init__(self, message):
        self._message = message
        self.result = {}

    def _create_schema_array(self):
        schema = []
        schema_data = dict()
        schema_data["data"] = "Message"
        schema_data["unit"] = "string"
        schema_data["heading"] = "Message"
        schema.append(schema_data)
        return schema

    def _construct_error_data(self):
        data = []
        error = [self._message]
        data.append(error)
        return data

    def construct_error_result(self):
        """Returns a dictionary that has error information that
         is parsable by JsonDisplay class in json_display module"""
        self.result["result-data-columns"] = 1
        self.result["result-data-lines"] = 1
        self.result["result-status-code"] = 1
        self.result["schema"] = self._create_schema_array()
        self.result["error"] = self._construct_error_data()
        return json.dumps(self.result)
