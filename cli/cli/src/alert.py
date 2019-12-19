# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
CLI commands for running alert commands.
"""
from .command_result import CommandResult
from .http_client import HttpClient
from .device_regex_resolver import DeviceRegexResolver
from .logger import Logger
import json
from texttable import Texttable
from datetime import datetime as dt
import pwd
import os


class AlertCli(object):
    """CLI commands for running alert commands."""

    def __init__(self, alert_parser):
        self._alert_parser = alert_parser
        alert_subparsers = alert_parser.add_subparsers(help='Subparser for alert')
        self._alert_parser.set_defaults(func=self._alert_execute)
        self._close_parser(alert_subparsers)
        self._history_parser(alert_subparsers)
        self._list_parser(alert_subparsers)
        self._view_parser(alert_subparsers)
        self.lgr = Logger()
        self.user = pwd.getpwuid(os.getuid()).pw_name

    def _list_parser(self, alert_parser):
        list_parser = alert_parser.add_parser('list', help='List the ras event alerts.')
        list_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                 help='Display data either in JSON or table format.'
                                      'Default will be to display data in tabular format')
        list_parser.set_defaults(func=self._list_execute)

    def _history_parser(self, alert_parser):
        history_parser = alert_parser.add_parser('history', help='History of ras event alerts.')
        history_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                    help='Display data either in JSON or table format.'
                                         'Default will be to display data in tabular format')
        history_parser.set_defaults(func=self._history_execute)

    def _view_parser(self, alert_parser):
        view_parser = alert_parser.add_parser('view', help='View event alerts for a specific id.')
        view_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                 help='Display data either in JSON or table format.'
                                      'Default will be to display data in tabular format')
        view_parser.add_argument('id', help='Provide an id to view its ras event alerts.')
        view_parser.set_defaults(func=self._view_execute)

    def _close_parser(self, alert_parser):
        close_parser = alert_parser.add_parser('close', help='Close event alerts for a specific location, alert type or '
                                                            'id. Location and types can be combined as filters.')
        close_parser.add_argument('--location', help='Provide a location to close its ras event alerts.')
        close_parser.add_argument('--type', help='Provide an alert type to close its ras event alerts.')
        close_parser.add_argument('--id', help='Provide an id to close its ras event alerts.')
        close_parser.set_defaults(func=self._close_execute)

    def _alert_execute(self, args):
        self._alert_parser.print_help()

    def _list_execute(self, args):

        client = HttpClient()
        url = client.get_base_url() + 'alert/list' + '?user=' + self.user
        self.lgr.debug("_list_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)
        if args.format == 'json' or response_code != 0:
            return CommandResult(response_code, response)
        return CommandResult(response_code, "\n" + self.alerts_as_table(json.loads(response)))

    def _history_execute(self, args):

        client = HttpClient()
        url = client.get_base_url() + 'alert/history' + '?user=' + self.user
        self.lgr.debug("_history_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)
        if args.format == 'json' or response_code != 0:
            return CommandResult(response_code, response)
        return CommandResult(response_code, "\n" + self.alerts_as_table(json.loads(response)))

    def _view_execute(self, args):

        client = HttpClient()
        url = client.get_base_url() + 'alert/view'
        parameters = {'id': args.id, 'user': self.user}

        self.lgr.debug("_view_execute: URL for request is {0} Parameters:".format(url, parameters))

        response_code, response = client.send_put_request(url, parameters, 900)
        if args.format == 'json' or response_code != 0:
            return CommandResult(response_code, response)
        alerts = json.loads(response)
        data_to_print = "\n" + self.alerts_as_table(alerts) + "\n\nRAS EVENTS:"
        for alert in alerts:
            data_to_print += "\n" + self.events_as_table(alert['events'])
        return CommandResult(response_code, data_to_print)

    def _close_execute(self, args):

        validate = self.validate_inputs(args)
        if validate is not None :
            return CommandResult(1, validate)

        client = HttpClient()
        url = client.get_base_url() + 'alert/close'
        parameters = {'location': args.location, 'type': args.type, 'id': args.id, 'user': self.user}

        self.lgr.debug("_close_execute: URL for request is {0} Parameters:".format(url, parameters))

        response_code, response = client.send_put_request(url, parameters, 900)
        return CommandResult(response_code, response)

    @staticmethod
    def validate_inputs(args):
        if args.id is not None and (args.location is not None or args.type is not None):
            return "Can only combine type and location inputs, not ids"
        if args.id is None and args.location is None and args.type is None:
            return "Need to specify either a location, type or id"
        return None

    @staticmethod
    def alerts_as_table(alerts):
        tab = Texttable(100)
        tab.header(['ID','TYPE','DESCRIPTION', 'CREATION', 'STATE'])
        for alert in alerts:
            tab.add_row([
                alert['id'],
                alert['type'],
                alert['description'],
                dt.fromtimestamp(alert['creationtime']/1000000.0)
                    .strftime('%Y-%m-%d %H:%M:%S.%f'),
                alert['state']])
        return tab.draw()

    @staticmethod
    def events_as_table(events):
        tab = Texttable(100)
        tab.header(['ID','TYPE','NAME', 'LOCATION', 'SEVERITY', 'DATA', 'TIMESTAMP'])
        for event in events:
            tab.add_row([
                event['id'],
                event['eventtype'],
                event['name'],
                event['location'],
                event['severity'],
                event['instancedata'],
                dt.fromtimestamp(event['timestamp']/1000000.0)
                    .strftime('%Y-%m-%d %H:%M:%S.%f')])
        return tab.draw()
