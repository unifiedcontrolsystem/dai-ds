# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

"""
EventSim CLI commands for Events.
"""
from .command_result import CommandResult
from .http_client import HttpClient


class BootCli(object):
    """EventSim CLI to handle boot functions."""

    def __init__(self, _boot_parser):
        self._boot_parser = _boot_parser
        event_subparsers = _boot_parser.add_subparsers(help='Subparser for boot')
        self._boot_parser.set_defaults(func=self._boot_help_execute)
        self._get_boot_parameters(event_subparsers)

    def _boot_help_execute(self, args):
        self._boot_parser.print_help()

    def _get_boot_parameters(self, event_parser):
        ras_events_parser = event_parser.add_parser('get-bootparam', help='get boot parameters.')
        ras_events_parser.set_defaults(func=self._get_boot_parameters_execute)

    def _get_boot_parameters_execute(self, args):
        client = HttpClient()
        # URL will be GET http://localhost:9998/bootparameters
        url = client.get_base_url() + '/bootparameters'
        response_code, response = client.send_get_request(url, '900')
        return CommandResult(response_code, response)
