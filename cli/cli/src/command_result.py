# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Module to create HTTP requests.
"""
import json


class CommandResult(object):
    """How the result of commands is stored."""

    def __init__(self, return_code=-1, message="Unknown Error"):
        """Construct a command result object."""
        self.return_code = return_code
        self.message = message

    def __str__(self):
        return "{} - {}".format(self.return_code, self.message)

    def __eq__(self, other):
        if isinstance(other, CommandResult):
            if (self.return_code == other.return_code and
                    self.message == other.message):
                return True
        return False

    @staticmethod
    def parse_command_response(response):
        """
        Parse the http response
        :param response:
        :return: result
        """
        try:
            # Try to treat the result as a JSON string
            parsed_result = json.loads(response)
            result = json.dumps(parsed_result, indent=4)
            return result
        except ValueError:
            return response
        return str(response)
