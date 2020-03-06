# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

"""
EventSim CLI commands for Events.
"""
from .command_result import CommandResult
from .http_client import HttpClient


class EventsCli(object):
    """EventSim CLI to generate events."""

    def __init__(self, _event_parser):
        self._event_parser = _event_parser
        event_subparsers = _event_parser.add_subparsers(help='Subparser for events')
        self._event_parser.set_defaults(func=self._event_help_execute)
        self._add_ras_event_parser(event_subparsers)
        self._add_sensor_event_parser(event_subparsers)
        self._add_boot_event_parser(event_subparsers)

    def _event_help_execute(self, args):
        self._event_parser.print_help()

    def _add_ras_event_parser(self, event_parser):
        ras_events_parser = event_parser.add_parser('ras', help='generate ras events.')
        ras_events_parser.add_argument('--count', type=int, help='Provide number of ras events to be generated. The '
                                                                 'default values are in config file.')
        ras_events_parser.add_argument('--location', help='generate ras events at a given location.')
        ras_events_parser.add_argument('--burst', help='generate events with or without delay.', action='store_false')
        ras_events_parser.add_argument('--label', help='generate ras events of a particular type')
        ras_events_parser.set_defaults(func=self._generate_ras_events_execute)

    def _add_sensor_event_parser(self, event_parser):
        sensor_events_parser = event_parser.add_parser('sensor', help='generate sensor events.')
        sensor_events_parser.add_argument('--count', type=int, help='Provide number of ras events to be generated. '
                                                                    'The default values are in config file.')
        sensor_events_parser.add_argument('--location', help='generate sensor events at a given location.')
        sensor_events_parser.add_argument('--burst', help='generate events with or without delay.', action='store_false')
        sensor_events_parser.add_argument('--label', help='generate sensor events of a particular type')
        sensor_events_parser.set_defaults(func=self._generate_sensor_events_execute)

    def _add_boot_event_parser(self, event_parser):
        boot_events_parser = event_parser.add_parser('boot', help='generate boot events.')
        boot_events_parser.add_argument('--probability', help='generate boot events with probability failure')
        boot_events_parser.add_argument('--burst', help='generate events with or without delay.', action='store_true')
        boot_events_parser.add_argument('--location', help='generate boot events at a given location.')
        boot_events_parser.set_defaults(func=self._generate_boot_events_execute)

    def _generate_ras_events_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/eventsim/ras?count=100000&burst=0&location=R3-20-CH00-CN2
        url = client.get_base_url() + 'api/ras'
        parameters = {'count': args.count, 'burst': args.burst, 'location': args.location,
                      'label': args.label}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _generate_sensor_events_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/eventsim/sensor?count=100000&burst=0&location=R3-20-CH00-CN2
        url = client.get_base_url() + 'api/sensor'
        parameters = {'count': args.count, 'burst': args.burst, 'location': args.location,
                      'label': args.label}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _generate_boot_events_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/eventsim/boot?count=100000&burst=0&location=R3-20-CH00-CN2
        url = client.get_base_url() + 'api/boot'
        parameters = {'burst': args.burst, 'location': args.location, 'probability': args.probability}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)
