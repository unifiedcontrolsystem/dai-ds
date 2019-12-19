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
from .json_error_creation import JsonError


class ViewCli(object):
    """CLI for viewing data in database"""

    def __init__(self, root_parser):
        self._root_parser = root_parser
        view_subparsers = root_parser.add_subparsers(help='subparser for quering/viewing data from the database')
        self._root_parser.set_defaults(func=self._view_execute)
        self._add_system_info_parser(view_subparsers)
        self._add_events_parser(view_subparsers)
        self._add_environment_parser(view_subparsers)
        # self._add_inventory_history_parser(view_subparsers)
        # self._add_replacement_history_parser(view_subparsers)
        # self._add_inventory_info_parser(view_subparsers)
        self._add_state_parser(view_subparsers)
        self._add_network_config_parser(view_subparsers)
        # self._add_snapshot_info_parser(view_subparsers)
        # self._add_get_ref_snapshot_info_parser(view_subparsers)
        # self._add_jobpower_parser(view_subparsers)
        # self._add_diagsview_parser(view_subparsers)
        self.lgr = Logger()
        self.user = pwd.getpwuid(os.getuid()).pw_name

    def _add_system_info_parser(self, view_parser):
        system_info_parser = view_parser.add_parser('system-info', help='view the system information')
        system_info_parser.add_argument('--all', help='Specify all output fields for more '
                                                      'information than default view', action='store_true')
        system_info_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                        help='Display data either in JSON or table format.'
                                             ' Default will be to display data in tabular format')
        system_info_parser.set_defaults(func=self._view_system_info_execute)

    def _add_events_parser(self, view_parser):
        events_parser = view_parser.add_parser('event', help='view the events data')
        events_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date is'
                                                        ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        events_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is'
                                                      ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        events_parser.add_argument('--limit', type=int, help='Provide a limit to the number of records of data '
                                                             'being retrieved. The default value is 100')
        events_parser.add_argument('--locations', help='Filter all event data for a given locations. '
                                                       'Provide comma separated location list or location '
                                                       'group. Example: R2-CH0[1-4]-N[1-4]')
        events_parser.add_argument('--jobid', help='Filter all event data for a jobid.')
        events_parser.add_argument('--event_type', help='Filter all event data for a given event_type. The event_type'
                                                        ' data depends on the descriptive name of the events in the '
                                                        'RASMetadata.json. \n Example: RasGen or Ras')
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
        events_parser.set_defaults(func=self._view_events_execute)

    def _add_environment_parser(self, view_parser):
        environment_parser = view_parser.add_parser('env', help='view the environmental data')
        environment_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date'
                                                             ' is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        environment_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is'
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

    def _add_inventory_history_parser(self, view_parser):
        inventory_parser = view_parser.add_parser('inventory-history', help='view the history of inventory changes '
                                                                            'for a location')
        inventory_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date '
                                                           'is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        inventory_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is '
                                                         '"YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        inventory_parser.add_argument('locations',
                                      help='Filter all inventory history data for a given locations. '
                                           'Provide comma separated location list or location group. '
                                           'Example: R2-CH0[1-4]-N[1-4]')
        inventory_parser.add_argument('--limit', type=int,
                                      help='Provide a limit to the number of records of data being retrieved. '
                                           'The default value is 100')
        inventory_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. Default will be to display '
                                           'data in tabular format')
        inventory_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        inventory_parser.set_defaults(func=self._view_inventory_change_execute)

    def _add_inventory_info_parser(self, view_parser):
        inventory_parser = view_parser.add_parser('inventory-info', help='view the latest inventory info data for a '
                                                                         'specific location')
        inventory_parser.add_argument('locations',
                                      help='Filter all inventory info data for a given locations. '
                                           'Provide comma separated location list or location group. '
                                           'Example: R2-CH0[1-4]-N[1-4]')
        inventory_parser.add_argument('--limit', default=100, type=int,
                                      help='Provide a limit to the number of records of data being retrieved. '
                                           'The default value is 100')
        inventory_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. '
                                           'Default will be to display data in tabular format')
        inventory_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        inventory_parser.set_defaults(func=self._view_inventory_info_execute)

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

    def _add_replacement_history_parser(self, view_parser):
        inventory_parser = view_parser.add_parser('replacement-history', help='view the replacement history data')
        inventory_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date '
                                                           'is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        inventory_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is '
                                                         '"YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        inventory_parser1 = inventory_parser.add_mutually_exclusive_group(required=True)
        inventory_parser1.add_argument('locations', nargs='?',
                                       help='Filter all inventory info and history for a given locations. '
                                            'Provide comma separated location list or location group. '
                                            'Example: R2-CH0[1-4]-N[1-4]')
        inventory_parser1.add_argument('sernum', nargs='?', help='Filter all inventory history data for a given '
                                                                 'serial number')
        inventory_parser.add_argument('--limit', type=int, help='Provide a limit to the number of records of data '
                                                                'being retrieved. The default value is 100')
        inventory_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. '
                                           'Default will be to display data in tabular format')
        inventory_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        inventory_parser.add_argument('--all', help='Specify all output fields for more information than default view',
                                      action='store_true')
        inventory_parser.set_defaults(func=self._view_replacement_history_execute)

    def _add_snapshot_info_parser(self, view_parser):
        inventory_parser = view_parser.add_parser('snapshot-info', help='view the snapshot data')
        inventory_parser.add_argument('locations',
                                      help='Filter all snapshot info for a given locations. '
                                           'Provide comma separated location list or location group. '
                                           'Example: R2-CH0[1-4]-N[1-4]')
        inventory_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date '
                                                           'is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        inventory_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is '
                                                         '"YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        inventory_parser.add_argument('--limit', type=int,
                                      help='Provide a limit to the number of records of data being retrieved. The '
                                           'default value is 100')
        inventory_parser.add_argument('--sernum', help='Filter all inventory history data for a given serial number')
        inventory_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. '
                                           'Default will be to display data in tabular format')
        inventory_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        inventory_parser.set_defaults(func=self._view_snapshot_info_execute)

    def _add_get_ref_snapshot_info_parser(self, view_parser):
        inventory_parser = view_parser.add_parser('snapshot-getref', help='view the reference snapshot data for '
                                                                          'given locations')
        inventory_parser.add_argument('locations', type=str, help='Filter all snapshot info for a given locations. '
                                                                  'Provide comma separated location list or location '
                                                                  'group. Example: R2-CH0[1-4]-N[1-4]')
        inventory_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. '
                                           'Default will be to display data in tabular format')
        inventory_parser.set_defaults(func=self._view_get_ref_snapshot_info_execute)

    def _add_jobpower_parser(self, view_parser):
        jobpower_parser = view_parser.add_parser('jobpower', help='view the job power data')
        jobpower_parser.add_argument('--jobid', default='', help='job id for which the '
                                                                 'job power information is for.')
        jobpower_parser.add_argument('--locations', default='',
                                     help='Filter all job power information data for a given locations. '
                                          'Provide comma separated location list '
                                          'or location regex. Example: R2-CH0[1-4]-N[1-4]')
        jobpower_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date '
                                                          'is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        jobpower_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is '
                                                        '"YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        jobpower_parser.add_argument('--limit', type=int,
                                     help='Provide a limit to the number of records of data being retrieved. '
                                          'The default value is 100')
        jobpower_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                     help='Display data either in JSON or table format. '
                                          'Default will be to display data in tabular format')
        jobpower_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                              'Uses a default of 900s')
        jobpower_parser.set_defaults(func=self._view_job_power_execute)

    def _add_diagsview_parser(self, view_parser):
        diagsview_parser = view_parser.add_parser('diag', help='view the diagnostics results data')

        diagsview_parser.add_argument('--start_time', help='provide the start time. The preferred format for the date '
                                                           'is "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        diagsview_parser.add_argument('--end_time', help='provide the end time. The preferred format for the date is '
                                                         '"YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        diagsview_parser.add_argument('--locations', help='Filter all diagnostics results for given locations. '
                                                          'Provide comma separated location list or location regex. '
                                                          'Example: R2-CH0[1-4]-N[1-4]')
        diagsview_parser.add_argument('--limit', type=int,
                                      help='Provide a limit to the number of records of data being retrieved. '
                                           'The default value is 100')
        diagsview_parser.add_argument('--diagid', help='Filter all diagnostics data for a given DiagId')
        diagsview_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. '
                                           'Default will be to display data in tabular format')
        diagsview_parser.add_argument('--timeout', default=900, type=int,
                                      help='Timeout value for HTTP request. Uses a default of 900s')
        diagsview_parser.set_defaults(func=self._view_diagsdata_execute)

    def _view_execute(self, args):
        self._root_parser.print_help()

    def _view_system_info_execute(self, args):
        client = HttpClient()
        url = client.get_base_url() + 'system'
        self.lgr.debug("_view_system_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)

        json_result = json.loads(response)

        data_to_display = ''

        if args.format == 'json':
            json_display = JsonDisplay(response)
            data_to_display = json_display.display_raw_json()
        else:
            json_result = json.loads(response)
            for node_type, nodes in json_result.items():
                json_display = JsonDisplay(json.dumps(nodes))
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
        if args.event_type is not None:
            event_type = 'EventType=' + args.event_type
        else:
            event_type = ''
        jobid = ''
        if args.jobid is not None:
            if self.is_bad_input(args.jobid):
                response_msg = 'Bad input, please try with a valid jobid'
                return self._parse_response_as_per_user_request(args.format, 1,
                                                                JsonError(response_msg).construct_error_result())
            jobid = 'JobId=' + args.jobid
        url = client.get_base_url() + 'cli/getraswithfilters?' + "&".join(
            [x for x in [starttime, endtime, limit, jobid, lctn, severity, event_type] if x != ""])
        self.lgr.debug("_view_events_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            if args.all:
                columns_order = ["lastchgtimestamp", "lctn", "eventtype", "severity", "controloperation", "msg",
                                 "jobid", "instancedata", "dbupdatedtimestamp"]
            else:
                columns_order = ["lastchgtimestamp", "lctn", "eventtype", "severity", "controloperation", "msg"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)

        return CommandResult(response_code, data_to_display)

    def _view_environment_execute(self, args):
        client = HttpClient()
        # URL will be GET http://hostaddress:hostport/cli/getenvwithfilters?StartTime=YYYY-MM-DD HH:MM:SS.[f]
        # &EndTime=YYYY-MM-DD HH:MM:SS.[f]
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        url = client.get_base_url() + 'cli/getenvwithfilters?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn] if x != ""])
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

    def _view_replacement_history_execute(self, args):
        client = HttpClient()
        if args.sernum is not None and args.locations is not None:
            raise RuntimeError('The inventory change takes either a lctn or a serial number as an input filter. '
                               'Try again with only one of them')
        if args.sernum is None and args.locations is None:
            raise RuntimeError('The inventory change requires one of the input filters lctn or a serial number. '
                               'Try again with one of them')
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        url = client.get_base_url() + 'cli/getinvchanges?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn] if x != ""])
        self.lgr.debug("_view_replacement_history_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            if args.all:
                columns_order = ["lastchgtimestamp", "lctn", "oldsernum", "newsernum", "serviceoperationid", "oldstate",
                                 "newstate", "frutype", "entrynumber", "dbupdatedtimestamp"]
            else:
                columns_order = ["lastchgtimestamp", "lctn", "oldsernum", "newsernum", "serviceoperationid"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_inventory_info_execute(self, args):
        client = HttpClient()
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join([x for x in [limit, lctn] if x != ""])
        self.lgr.debug("_view_inventory_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["lctn", "type", "hostname", "sequencenumber", "sernum", "entrynumber"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_state_execute(self, args):
        client = HttpClient()
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join([x for x in [limit, lctn] if x != ""])
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
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join([x for x in [limit, lctn] if x != ""])
        self.lgr.debug("_view_inventory_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["lctn", "type", "hostname", "ipaddr", "macaddr", "bmchostname", "bmcipaddr", "bmcmacaddr"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_inventory_change_execute(self, args):
        client = HttpClient()
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn] if x != ""])
        self.lgr.debug("_view_inventory_change_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["lctn", "type", "hostname", "sequencenumber", "sernum", "entrynumber"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_snapshot_info_execute(self, args):
        client = HttpClient()
        lctn, starttime, endtime, limit, display_format, time_out = self._retrieve_data_from_args(args)
        url = client.get_base_url() + 'cli/getsnapshotspecificlctn?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn] if x != ""])
        self.lgr.debug("_view_snapshot_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ['lctn', 'id', 'snapshottimestamp', 'inventoryinfo', 'reference']
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_get_ref_snapshot_info_execute(self, args):
        client = HttpClient()
        if self.is_bad_input(args.locations):
            return CommandResult(1, "Bad input, please try with a valid location")
        lctn = 'Lctn=' + DeviceRegexResolver.get_devices(args.locations)
        url = client.get_base_url() + 'cli/getrefsnapshot?' + lctn
        self.lgr.debug("_view_get_ref_snapshot_info_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)

        json_display = JsonDisplay(response)

        if args.format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ['lctn', 'id', 'snapshottimestamp', 'inventoryinfo', 'reference']
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_job_power_execute(self, args):
        client = HttpClient()
        location, start_time, end_time, limit, display_format, time_out = self._retrieve_data_from_args(args)
        job_id = 'JobId=' + args.jobid
        url = client.get_base_url() + 'cli/getjobdata?' + "&".join(
            [x for x in [start_time, end_time, limit, location, job_id] if x != ""])
        self.lgr.debug("_view_job_power_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ['lctn', 'jobid', 'totalruntime', 'totalpackageenergy', 'totaldramenergy',
                             'jobpowertimestamp']
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_diagsdata_execute(self, args):
        client = HttpClient()
        # URL will be GET http://hostaddress:hostport/cli/getdiagsdata?StartTime=YYYY-MM-DD HH:MM:SS.[f]
        # &EndTime=YYYY-MM-DD HH:MM:SS.[f]
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        if args.diagid is not None:
            diagid = 'DiagId=' + args.diagid
        else:
            diagid = ''
        url = client.get_base_url() + 'cli/getdiagsdata?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn, diagid] if x != ""])
        self.lgr.debug("_view_diagsdata_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ['diagid', 'lctn', 'state', 'results', 'dbupdatedtimestamp']
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
