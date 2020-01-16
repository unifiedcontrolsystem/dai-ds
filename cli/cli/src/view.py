# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
CLI commands for controlling viewing data in database.
"""
import json
import pwd
import os
from .command_result import CommandResult
from .http_client import HttpClient
from .device_regex_resolver import DeviceRegexResolver
from .logger import Logger
from dateutil.parser import parse
from datetime import datetime
from datetime import timedelta
from .json_display import JsonDisplay
from .view_events_summary import ViewEventsSummary
from .json_error_creation import JsonError


class ViewCli(object):
    """CLI for viewing data in database"""

    def __init__(self, root_parser):
        self._root_parser = root_parser
        view_subparsers = root_parser.add_subparsers(help='subparser for quering/viewing data from the database')
        self._root_parser.set_defaults(func=self._view_execute)
        self._add_environment_parser(view_subparsers)
        self._add_events_parser(view_subparsers)
        self._add_job_info_parser(view_subparsers)
        self._add_network_config_parser(view_subparsers)
        self._add_reservation_info_parser(view_subparsers)
        self._add_state_parser(view_subparsers)
        self._add_system_info_parser(view_subparsers)
        self.lgr = Logger()
        self.user = pwd.getpwuid(os.getuid()).pw_name

    def _add_system_info_parser(self, view_parser):
        system_info_parser = view_parser.add_parser('system-info', help='view the system information')
        system_info_parser.add_argument('--all', help='Specify all output fields for more '
                                                      'information than default view', action='store_true')
        system_info_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                        help='Display data either in JSON or table format.'
                                             ' Default will be to display data in tabular format')
        system_info_parser.add_argument('--summary', help='Display a summary of cluster by showing the state count '
                                                          'of all compute and service nodes', action='store_true')
        system_info_parser.set_defaults(func=self._view_system_info_execute)

    def _add_events_parser(self, view_parser):
        events_parser = view_parser.add_parser('event', help='view the events data')
        events_parser.add_argument('--start-time', dest="start_time",
                                   help='provide the start time. The preferred format for the date is'
                                        ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        events_parser.add_argument('--end-time', dest="end_time",
                                   help='provide the end time. The preferred format for the date is'
                                        ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        events_parser.add_argument('--limit', type=int, help='Provide a limit to the number of records of data '
                                                             'being retrieved. The default value is 100')
        events_parser.add_argument('--locations', help='Filter all event data for a given locations. '
                                                       'Provide comma separated location list or location '
                                                       'group. Example: R2-CH0[1-4]-N[1-4]')
        events_parser.add_argument('--jobid', help='Filter all event data for a jobid.')
        events_parser.add_argument('--type', dest="type",
                                   help='Filter all event data for the descriptive name of the event type in the  '
                                        'RASMetadata.json. \n Example: RasGen or Ras. Regex are allowed too')
        events_parser.add_argument('--type-exclude', dest="exclude",
                                   help='excludes all event data for the descriptive name of the event type in the'
                                        'RASMetadata.json. \n Example: RasGen or Ras. Regex are allowed too')
        events_parser.add_argument('--severity', help='Filter all event data for a given severity {INFO, FATAL, ERROR,'
                                                      ' CRITICAL}. This option does not take wildcards or RegEx \n')
        events_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                   help='Display data either in JSON or table '
                                        'format. Default will be to display '
                                        'data in tabular format')
        events_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                            'Uses a default of 900s')
        events_parser.add_argument('--all', help='Specify all output fields for more information than default view',
                                   action='store_true')
        events_parser.add_argument('--summary', help='Summary of RAS Events. This command is not to be used with '
                                                     '--format option', action='store_true')
        events_parser.set_defaults(func=self._view_events_execute)

    def _add_environment_parser(self, view_parser):
        environment_parser = view_parser.add_parser('env', help='view the environmental data')
        environment_parser.add_argument('--start-time', dest="start_time",
                                        help='provide the start time. The preferred format for the date is'
                                        ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        environment_parser.add_argument('--end-time', dest="end_time",
                                        help='provide the end time. The preferred format for the date is'
                                        ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        environment_parser.add_argument('--limit', type=int, help='Provide a limit to the number of records of data '
                                                                  'being retrieved. The default value is 100')
        environment_parser.add_argument('--locations', help='Filter all environmental data for a given locations. '
                                                            'Provide comma separated location list or location group. '
                                                            'Example: R2-CH0[1-4]-N[1-4]')
        environment_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                        help='Display data either in JSON or table format. Default will be to display '
                                             'data in tabular format')
        environment_parser.add_argument('--timeout', default=900, type=int,
                                        help='Timeout value for HTTP request. Uses a default of 900s')
        environment_parser.add_argument('--all',
                                        help='Specify all output fields for more information than default view',
                                        action='store_true')
        environment_parser.set_defaults(func=self._view_environment_execute)

    def _add_state_parser(self, view_parser):
        state_parser = view_parser.add_parser('state', help='view the latest state info data for a specific location')
        state_parser.add_argument('locations',
                                  help='Location that you want to get the state info for. Partial locations'
                                       ' are accepted')
        state_parser.add_argument('--limit', default=100, type=int,
                                  help='Provide a limit to the number of records of data being retrieved. The default '
                                       'value is 100')
        state_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                  help='Display data either in JSON or table format. '
                                       'Default will be to display data in tabular format')
        state_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                           'Uses a default of 900s')
        state_parser.set_defaults(func=self._view_state_execute)

    def _add_network_config_parser(self, view_parser):
        network_config__parser = view_parser.add_parser('network-config', help='view the latest network info data for a'
                                                                               ' specific location')
        network_config__parser.add_argument('locations',
                                            help='Filter all network config data for given locations. '
                                                 'Provide comma separated location list or '
                                                 'location group. Example: R2-CH0[1-4]-N[1-4]')
        network_config__parser.add_argument('--limit', default=100, type=int,
                                            help='Provide a limit to the number of records of data being retrieved. '
                                                 'The default value is 100')
        network_config__parser.add_argument('--format', choices=['json', 'table'], default='table',
                                            help='Display data either in JSON or table format. Default will be to '
                                                 'display data in tabular format')
        network_config__parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                                     'Uses default of 900s')
        network_config__parser.set_defaults(func=self._view_network_config_execute)

    def _add_job_info_parser(self, view_parser):
        job_parser = view_parser.add_parser('job', help='view the job information for the cluster')
        job_parser.add_argument('--start-time', dest="start_time",
                                      help='provide the start time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--end-time', dest="end_time",
                                      help='provide the end time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--jobid', help='Filter all job data for a given jobid. This will also'
                                                ' display accounting information and nodes for given jobid')
        job_parser.add_argument('--user', dest="username", help='Filter all job data for a given username')
        job_parser.add_argument('--limit', default=100, type=int,
                                                  help='Provide a limit to the number of records of data being retrieved. '
                                                       'The default value is 100')
        job_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. Default will be to display '
                                           'data in tabular format')
        job_parser.add_argument('--all', help='Specify all output fields for more '
                                              'information than default view', action='store_true')
        job_parser.add_argument('--active', help='Show only jobs that are currently running', action='store_true')
        job_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        job_parser.set_defaults(func=self._view_job_info_execute)

    def _add_reservation_info_parser(self, view_parser):
        job_parser = view_parser.add_parser('reservation', help='view the reservation information for the cluster')
        job_parser.add_argument('--start-time', dest="start_time",
                                      help='provide the start time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--end-time', dest="end_time",
                                      help='provide the end time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--name', help='Filter all reservation data for a given reservation name.')
        job_parser.add_argument('--user', dest="username", help='Filter all reservation data for a given username')
        job_parser.add_argument('--limit', default=100, type=int,
                                                  help='Provide a limit to the number of records of data being retrieved. '
                                                       'The default value is 100')
        job_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. Default will be to display '
                                           'data in tabular format')
        job_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        job_parser.set_defaults(func=self._view_reservation_info_execute)

    def _view_execute(self, args):
        self._root_parser.print_help()

    def _parse_response_as_per_user_request(self, format, response_code, response):
        json_display = JsonDisplay(response)

        if format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            data_to_display = '\n' + json_display.display_json_in_tabular_format()
        return CommandResult(response_code, data_to_display)

    def _view_system_info_execute(self, args):
        client = HttpClient()
        user = 'user=' + self.user

        if args.summary:
            command = 'cli/system_summary?'
        else:
            command = 'system?'

        url = client.get_base_url() + command + "&".join([x for x in [user] if x != ""])

        self.lgr.debug("_view_system_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)

        data_to_display = ''

        if args.format == 'json':
            json_display = JsonDisplay(response)
            data_to_display = json_display.display_raw_json()
        else:
            json_result = json.loads(response)
            for node_type, nodes in json_result.items():
                json_display = JsonDisplay(json.dumps(nodes))
                if args.summary:
                    data_to_display += '\n' + node_type.upper() + ' NODES'
                    columns_order = ["state","count"]
                else:
                    if args.all:
                        columns_order = ["LCTN", "HOSTNAME", "AGGREGATOR", "STATE", "IPADDR", "MACADDR", "BMCIPADDR",
                                         "BOOTIMAGEID", "TYPE", "SEQUENCENUMBER", "LASTCHGWORKITEMID", "BMCMACADDR",
                                         "LASTCHGTIMESTAMP", "INVENTORYINFO", "DBUPDATEDTIMESTAMP", "OWNER", "SERNUM",
                                         "BMCHOSTNAME", "LASTCHGADAPTERTYPE"]
                    else:
                        columns_order = ["LCTN", "HOSTNAME", "AGGREGATOR", "STATE", "IPADDR", "MACADDR", "BMCIPADDR",
                                         "BOOTIMAGEID", "TYPE"]
                data_to_display += '\n' + json_display.display_json_in_tabular_format(columns_order)

        return CommandResult(response_code, data_to_display)

    @staticmethod
    def get_regex_expressions(filename):
        user_home = os.path.expanduser("~")
        filter_folder = ".ucs"
        filter_path = "{}/{}/{}".format(user_home, filter_folder, filename)
        if not os.path.exists(filter_path):
            return None
        with open(filter_path, 'r') as f:
            content = f.readlines()
        expressions = []
        for expression in content:
            expressions.append(expression.replace('\n', ''))
        return "|".join(expressions)

    def _view_events_execute(self, args):
        client = HttpClient()
        # URL will be GET http://hostaddress:hostport/cli/getraswithfilters?StartTime=YYYY-MM-DD HH:MM:SS.[f]
        # &EndTime=YYYY-MM-DD HH:MM:SS.[f]
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        if args.severity is not None:
            severity = 'Severity=' + args.severity
        else:
            severity = ''

        exclude_expressions = self.get_regex_expressions("exclude")
        if args.exclude is not None:
            exclude = 'Exclude=' + args.exclude
        else:
            exclude = ''
        if exclude and exclude_expressions:
            exclude = exclude + "|" + exclude_expressions
        elif exclude_expressions:
            exclude = 'Exclude=' + exclude_expressions

        include_expressions = self.get_regex_expressions("include")
        if args.type is not None:
            event_type = 'EventType=' + args.type
        else:
            event_type = ''
        if event_type and include_expressions:
            event_type = event_type + "|" + include_expressions
        elif include_expressions:
            event_type = 'EventType=' + include_expressions

        jobid = ''
        if args.jobid is not None:
            if self.is_bad_input(args.jobid):
                response_msg = 'Bad input, please try with a valid jobid'
                return self._parse_response_as_per_user_request(args.format, 1,
                                                                JsonError(response_msg).construct_error_result())
            jobid = 'JobId=' + args.jobid
        user = 'user=' + self.user
        url = client.get_base_url() + 'cli/getraswithfilters?' + "&".join(
            [x for x in [starttime, endtime, limit, jobid, lctn, severity, event_type, user, exclude] if x != ""])
        self.lgr.debug("_view_events_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        if args.summary:
            return self._view_events_summary(response_code, response, display_format)
        else:
            json_display = JsonDisplay(response)
            if display_format == 'json':
                data_to_display = json_display.display_raw_json()
            else:
                if args.all:
                    columns_order = ["time", "lctn", "type", "severity", "controloperation", "detail",
                                     "jobid", "dbupdatedtimestamp"]
                else:
                    columns_order = ["time", "lctn", "type", "severity", "jobid", "controloperation", "detail"]
                data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)

        return CommandResult(response_code, data_to_display)

    def _view_events_summary(self, response_code, response, display_format):
        if display_format:
            response_msg = 'Summary option cannot be used with format option. ' \
                           'Please look at ucs view event help. '
            return self._parse_response_as_per_user_request(display_format, 1,
                                                            JsonError(response_msg).construct_error_result())
        else:
            return CommandResult(response_code, ViewEventsSummary(response).generate_summary())

    def _view_environment_execute(self, args):
        client = HttpClient()
        # URL will be GET http://hostaddress:hostport/cli/getenvwithfilters?StartTime=YYYY-MM-DD HH:MM:SS.[f]
        # &EndTime=YYYY-MM-DD HH:MM:SS.[f]
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        user = 'user=' + self.user
        url = client.get_base_url() + 'cli/getenvwithfilters?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn, user] if x != ""])
        self.lgr.debug("_view_environment_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            if args.all:
                columns_order = ["lctn", "type", "minimumvalue", "maximumvalue", "averagevalue", "timestamp",
                                 "adaptertype", "entrynumber", "workitemid"]
            else:
                columns_order = ["lctn", "type", "minimumvalue", "maximumvalue", "averagevalue", "timestamp"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)

        return CommandResult(response_code, data_to_display)

    def _view_state_execute(self, args):
        client = HttpClient()
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        user = 'user=' + self.user
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join([x for x in [limit, lctn, user] if x != ""])
        self.lgr.debug("_view_inventory_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["lctn", "type", "hostname", "state", "wlmnodestate", "owner", "environment", "bootimageid"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_network_config_execute(self, args):
        client = HttpClient()
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        user = 'user=' + self.user
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join([x for x in [limit, lctn, user] if x != ""])
        self.lgr.debug("_view_inventory_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["lctn", "type", "hostname", "ipaddr", "macaddr", "bmchostname", "bmcipaddr", "bmcmacaddr"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_job_info_execute(self, args):
            args.locations = None
            client = HttpClient()
            starttime, endtime = self._retrieve_time_from_args(args)
            limit, lctn, display_format, time_out = self._retrieve_from_args(args)
            user = 'user=' + self.user
            if args.username is not None:
                username = 'Username=' + args.username
            else:
                username = ''
            if args.jobid is not None:
                jobid = 'Jobid=' + args.jobid
            else:
                jobid = ''
            if args.active:
                state = 'State=S'
            else:
                state = ''
            url = client.get_base_url() + 'cli/getjobinfo?' + "&".join(
                [x for x in [starttime, endtime, limit, user, username, jobid, state] if x != ""])
            self.lgr.debug("_view_job_info_execute: URL for request is {0}".format(url))
            response_code, response = client.send_get_request(url, time_out)

            json_display = JsonDisplay(response)

            if display_format == 'json':
                data_to_display = json_display.display_raw_json()
            else:
                columns_order = ["jobid", "jobname", "state", "numnodes", "username", "starttimestamp", "endtimestamp"]
                if args.jobid is not None or args.all:
                    columns_order = columns_order + ["jobacctinfo", "nodes"]
                data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
            return CommandResult(response_code, data_to_display)

    def _view_reservation_info_execute(self, args):
            args.locations = None
            client = HttpClient()
            starttime, endtime = self._retrieve_time_from_args(args)
            limit, lctn, display_format, time_out = self._retrieve_from_args(args)
            user = 'user=' + self.user
            if args.username is not None:
                username = 'Username=' + args.username
            else:
                username = ''
            if args.name is not None:
                name = 'Name=' + args.name
            else:
                name = ''
            url = client.get_base_url() + 'cli/getreservationinfo?' + "&".join(
                [x for x in [starttime, endtime, limit, user, username, name] if x != ""])
            self.lgr.debug("_view_reservation_info_execute: URL for request is {0}".format(url))
            response_code, response = client.send_get_request(url, time_out)

            json_display = JsonDisplay(response)

            if display_format == 'json':
                data_to_display = json_display.display_raw_json()
            else:
                columns_order = ["reservationname", "users", "nodes", "starttimestamp", "endtimestamp", "deletedtimestamp"]
                data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
            return CommandResult(response_code, data_to_display)

    def _validate_input_timestamp(self, input_time):
        try:
            inpdatetime = parse(input_time)
        except Exception:
            raise RuntimeError("Input timestamp is of invalid type. Try again.")
        return inpdatetime.strftime("%Y-%m-%d %H:%M:%S.%f")

    def _retrieve_from_args(self, args):
        MAX_TIMEOUT = 2147483647
        if args.limit is not None:
            if args.limit < 0:
                raise RuntimeError("Bad input, please try with a valid limit (number larger than 0)")
            limit = 'Limit=' + str(args.limit)
        else:
            if args.start_time is None and args.end_time is None:
                # Default value for queries that have no start or end time specified
                limit = 'Limit=100'
            else:
                limit = ''

        display_format = args.format
        time_out = min([args.timeout, MAX_TIMEOUT])

        if args.locations is not None:
            if self.is_bad_input(args.locations):
                raise RuntimeError("Bad input, please try with a valid location")
            location_input = 'Lctn=' + DeviceRegexResolver.get_devices(args.locations)
        else:
            location_input = ''
        return limit, location_input, display_format, time_out

    def _retrieve_time_from_args(self, args):
        if args.start_time is not None:
            starttime = 'StartTime=' + self._validate_input_timestamp(args.start_time)
        else:
            starttime = ''

        if args.end_time is not None:
            endtime = self._validate_input_timestamp(args.end_time)
            if len(args.end_time) <= 12:
                endtime = 'EndTime=' + str(datetime.strptime(endtime, "%Y-%m-%d %H:%M:%S.%f") + timedelta(days=1))
            else:
                endtime = 'EndTime=' + endtime
        else:
            endtime = ''

        return starttime, endtime

    def _retrieve_data_from_args(self, args):

        limit, location_input, output_file, time_out = self._retrieve_from_args(args)
        if args.start_time is not None:
            start_time = 'StartTime=' + self._validate_input_timestamp(args.start_time)
        else:
            start_time = ''
        if args.end_time is not None:
            end_time = 'EndTime=' + self._validate_input_timestamp(args.end_time)
        else:
            end_time = ''
        return location_input, start_time, end_time, limit, output_file, time_out

    @staticmethod
    def is_bad_input(parameter):
        if "?" in parameter or "*" in parameter or "%" in parameter:
            return True
        return False
