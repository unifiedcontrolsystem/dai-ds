# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
from .command_result import CommandResult
from .http_client import HttpClient

"""
EventSim CLI commands to generate events.
"""


class EventsCli(object):
    default_timeout = 31536000

    """
    This method lists sub-commands for events command.
    """
    def __init__(self, _event_parser):
        self._event_parser = _event_parser
        event_subparsers = _event_parser.add_subparsers(help='subparser for events')
        self._event_parser.set_defaults(func=self._event_help_execute)
        self._add_ras_event_parser(event_subparsers)
        self._add_sensor_event_parser(event_subparsers)
        self._add_job_event_parser(event_subparsers)
        self._add_boot_event_parser(event_subparsers)
        self._add_scenario_event_parser(event_subparsers)
        self._add_get_seed_parser(event_subparsers)

    """
    This method displays help text for each sub-command.
    """
    def _event_help_execute(self, args):
        self._event_parser.print_help()

    """
    This method describes 'ras' sub-command arguments.
    """
    def _add_ras_event_parser(self, event_parser):
        ras_events_parser = event_parser.add_parser('ras', help='generate ras events')
        ras_events_parser.add_argument('--burst',
            help='generate ras events without delay. Default is constant mode with delay.', action='store_true')
        ras_events_parser.add_argument('--count', type=int,
            help='given number of ras events are generated. The default values exists in eventsim config file.')
        ras_events_parser.add_argument('--delay', type=int,
            help='pause for given value in microseconds to generate ras events. The default values exists in eventsim '
                 'config file.')
        ras_events_parser.add_argument('--label', help='generate ras events for a given type/description')
        ras_events_parser.add_argument('--locations',
            help='generate ras events at a given location. Provide regex for multiple locations.')
        ras_events_parser.add_argument('--output', help='store data in a file')
        ras_events_parser.add_argument('--seed', type=int, help='seed to duplicate data')
        ras_events_parser.add_argument('--timeout', type=int, help='ras sub-command execution timeout')
        ras_events_parser.set_defaults(func=self._generate_ras_events_execute)

    """
    This method describes 'sensor' sub-command arguments.
    """
    def _add_sensor_event_parser(self, event_parser):
        sensor_events_parser = event_parser.add_parser('sensor', help='generate sensor events')
        sensor_events_parser.add_argument('--burst',
            help='generate sensor events without delay. Default is constant mode with delay.', action='store_true')
        sensor_events_parser.add_argument('--count', type=int,
            help='given number of sensor events are generated. The default values exists in eventsim config file.')
        sensor_events_parser.add_argument('--delay', type=int,
            help='pause for given value in microseconds to generate sensor events. The default values exists in '
                 'eventsim config file.')
        sensor_events_parser.add_argument('--label', help='generate sensor events for a given type/description')
        sensor_events_parser.add_argument('--locations',
            help='generate sensor events at a given location. Provide regex for multiple locations.')
        sensor_events_parser.add_argument('--output', help='store data in a file')
        sensor_events_parser.add_argument('--seed', type=int, help='seed to duplicate data')
        sensor_events_parser.add_argument('--timeout', type=int, help='sensor sub-command execution timeout')
        sensor_events_parser.set_defaults(func=self._generate_sensor_events_execute)

    """
    This method describes 'job' sub-command arguments.
    """
    def _add_job_event_parser(self, event_parser):
        job_events_parser = event_parser.add_parser('job', help='generate job events')
        job_events_parser.add_argument('--burst',
                    help='generate job events without delay. Default is constant mode with delay.', action='store_true')
        job_events_parser.add_argument('--count', type=int,
                    help='given number of job events are generated. The default values exists in eventsim config file.')
        job_events_parser.add_argument('--delay', type=int,
                    help='pause for given value in microseconds to generate job events. The default values exists in '
                         'eventsim config file.')
        job_events_parser.add_argument('--label', help='generate job events for a given type/description')
        job_events_parser.add_argument('--locations',
                    help='generate job events at a given location. Provide regex for multiple locations.')
        job_events_parser.add_argument('--output', help='store data in a file')
        job_events_parser.add_argument('--seed', type=int, help='seed to duplicate data')
        job_events_parser.add_argument('--timeout', type=int, help='job sub-command execution timeout')
        job_events_parser.set_defaults(func=self._generate_job_events_execute)

    """
    This method describes 'boot' sub-command arguments.
    """
    def _add_boot_event_parser(self, event_parser):
        boot_events_parser = event_parser.add_parser('boot', help='generate boot events')
        boot_events_parser.add_argument('--burst',
            help='generate boot events without delay. Default is constant mode with delay.', action='store_true')
        boot_events_parser.add_argument('--delay', type=int,
            help='pause for given value in microseconds to generate boot events. The default values exists in '
                 'eventsim config file.')
        boot_events_parser.add_argument('--locations',
            help='generate boot events at a given location. Provide regex for multiple locations.')
        boot_events_parser.add_argument('--output', help='store data in a file')
        boot_events_parser.add_argument('--probability', default=0,
            help='generate boot events with probability failure. Default no failure.')
        boot_events_parser.add_argument('--seed', type=int, help='seed to duplicate data')
        boot_events_parser.add_argument('--timeout', type=int, help='boot sub-command execution timeout')
        boot_events_parser.add_argument('--type', choices=['off', 'on', 'ready'], default='all',
            help='generate given type of boot events. Default generates all [on/off/ready] types of boot events.')
        boot_events_parser.set_defaults(func=self._generate_boot_events_execute)

    """
    This method describes 'scenario' sub-command arguments.
    """
    def _add_scenario_event_parser(self, event_parser):
        scenario_events_parser = event_parser.add_parser('scenario', help='generate events for a given scenario')
        scenario_events_parser.add_argument('file', help='scenario configuration file')
        scenario_events_parser.add_argument('--burst',
            help='generate events for a given scenario without delay. Default is constant mode with delay.',
            action='store_true')
        scenario_events_parser.add_argument('--counter', type=int, help='repeat scenario for a given counter')
        scenario_events_parser.add_argument('--delay', type=int,
            help='pause for given value in microseconds to generate events for a given scenario. The default values '
                 'exists in eventsim config file.')
        scenario_events_parser.add_argument('--duration', type=int,
            help='scenario occurs for a given duration. The default units is minutes only.')
        scenario_events_parser.add_argument('--locations',
            help='generate events for a given scenario at a given location. Provide regex for multiple locations.')
        scenario_events_parser.add_argument('--output', help='Store data in a file.')
        scenario_events_parser.add_argument('--probability', type=int,
            help='generate boot events with probability failure')
        scenario_events_parser.add_argument('--ras-label', help='generate ras events of a particular type/description')
        scenario_events_parser.add_argument('--sensor-label',
            help='generate sensor events of a particular type/description')
        scenario_events_parser.add_argument('--seed', type=int, help='seed to duplicate data')
        scenario_events_parser.add_argument('--start-time', help='start time to generate events for a given scenario')
        scenario_events_parser.add_argument('--timeout', type=int, help='scenario sub-command execution timeout')
        scenario_events_parser.add_argument('--mode', choices=['burst', 'group-burst', 'repeat'],
            help='generate events given type of scenario. Default generates burst type scenario. Scenario data exists '
                 'in scenario config file.')
        scenario_events_parser.set_defaults(func=self._generate_scenario_events_execute)

    """
    This method describes 'sensor' sub-command arguments.
    """
    def _add_get_seed_parser(self, event_parser):
        get_seed_parser = event_parser.add_parser('get-seed', help='fetch prior seed to replicate same data.')
        get_seed_parser.add_argument('--seed', type=int, help='seed to duplicate data')
        get_seed_parser.add_argument('--timeout', type=int, help='get-seed sub-command execution timeout')
        get_seed_parser.set_defaults(func=self._fetch_event_seed_execute)

    """
    This method generates url and send api request to generate ras events.
    """
    def _generate_ras_events_execute(self, args):
        client = HttpClient()
        # URL will be POST http://127.0.0.1:9998/apis/events/ras
        url = client.get_base_url() + 'apis/events/ras'
        parameters = {'burst': args.burst, 'count': args.count, 'delay': args.delay, 'label': args.label,
                      'locations': args.locations, 'output': args.output, 'seed': args.seed}
        parameters = {k: v for k, v in parameters.items() if v is not None}

        timeout = args.timeout
        if timeout is None:
            timeout = self.default_timeout
        response_code, response = client.send_post_request(url, parameters, timeout)
        return CommandResult(response_code, response)

    """
    This method generates url and send api request to generate sensor events.
    """
    def _generate_sensor_events_execute(self, args):
        client = HttpClient()
        # URL will be POST http://127.0.0.1:9998/apis/events/sensor
        url = client.get_base_url() + 'apis/events/sensor'
        parameters = {'burst': args.burst, 'count': args.count, 'delay': args.delay, 'label': args.label,
                      'locations': args.locations, 'output': args.output, 'seed': args.seed}
        parameters = {k: v for k, v in parameters.items() if v is not None}

        timeout = args.timeout
        if timeout is None:
            timeout = self.default_timeout
        response_code, response = client.send_post_request(url, parameters, timeout)
        return CommandResult(response_code, response)


    """
    This method generates url and send api request to generate job events.
    """
    def _generate_job_events_execute(self, args):
        client = HttpClient()
        # URL will be POST http://127.0.0.1:9998/apis/events/sensor
        url = client.get_base_url() + 'apis/events/job'
        parameters = {'burst': args.burst, 'count': args.count, 'delay': args.delay, 'label': args.label,
                      'locations': args.locations, 'output': args.output, 'seed': args.seed}
        parameters = {k: v for k, v in parameters.items() if v is not None}

        timeout = args.timeout
        if timeout is None:
            timeout = self.default_timeout
        response_code, response = client.send_post_request(url, parameters, timeout)
        return CommandResult(response_code, response)

    """
    This method generates url and send api request to generate boot events.
    """
    def _generate_boot_events_execute(self, args):
        client = HttpClient()
        # URL will be POST http://127.0.0.1:9998/apis/events/boot
        url = client.get_base_url() + 'apis/events/boot/' + args.type
        parameters = {'burst': args.burst, 'delay': args.delay, 'locations': args.locations, 'output': args.output,
                      'probability': args.probability, 'seed': args.seed}
        parameters = {k: v for k, v in parameters.items() if v is not None}

        timeout = args.timeout
        if timeout is None:
            timeout = self.default_timeout
        response_code, response = client.send_post_request(url, parameters, timeout)
        return CommandResult(response_code, response)

    """
    This method generates url and send api request to generate events for a given scenario.
    """
    def _generate_scenario_events_execute(self, args):
        client = HttpClient()
        # URL will be POST http://127.0.0.1:9998/apis/events/scenario
        url = client.get_base_url() + 'apis/events/scenario'

        parameters = {'file': args.file, 'burst': args.burst, 'delay': args.delay, 'duration': args.duration,
                      'locations': args.locations, 'output': args.output, 'probability': args.probability,
                      'ras-lable': args.ras_label, 'counter': args.counter, 'sensor-label': args.sensor_label,
                      'seed': args.seed, 'start-time': args.start_time, 'type': args.mode}
        parameters = {k: v for k, v in parameters.items() if v is not None}

        timeout = args.timeout
        if timeout is None:
            timeout = self.default_timeout
        response_code, response = client.send_post_request(url, parameters, timeout)
        return CommandResult(response_code, response)

    """
    This method generates url and send api request to fetch prior seed to replicate data.
    """
    def _fetch_event_seed_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/api/events/seed
        url = client.get_base_url() + 'apis/events/seed'

        timeout = args.timeout
        if timeout is None:
            timeout = self.default_timeout
        response_code, response = client.send_get_request(url, timeout)
        return CommandResult(response_code, response)
