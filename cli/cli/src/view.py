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
import re
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
        self._add_inventory_parser(view_subparsers)
        self._add_job_info_parser(view_subparsers)
        self._add_network_config_parser(view_subparsers)
        self._add_replacement_history_parser(view_subparsers)
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
                                                              ' WARN, DEBUG}. This option does not take wildcards or RegEx \n')
        events_parser.add_argument('--format', choices=['json', 'table'],
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

    def _add_inventory_parser(self, view_parser):
        inventory_parser = view_parser.add_parser('inventory', help='view the inventory info data for a '
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
        inventory_parser.add_argument('--sernum', help='Filter all inventory data for a given serial number')
        inventory_parser.add_argument('--history', help='Show all inventory history data', action='store_true')
        inventory_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        inventory_parser.set_defaults(func=self._view_inventory_execute)

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
        replacement_parser = view_parser.add_parser('replacement-history', help='view the replacement history data')
        replacement_parser.add_argument('--start-time', dest="start_time",
                                      help='provide the start time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        replacement_parser.add_argument('--end-time', dest="end_time",
                                      help='provide the end time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        replacement_parser1 = replacement_parser.add_mutually_exclusive_group(required=True)
        replacement_parser1.add_argument('locations', nargs='?',
                                       help='Filter all inventory info and history for a given locations. '
                                            'Provide comma separated location list or location group. '
                                            'Example: R2-CH0[1-4]-N[1-4]')
        replacement_parser1.add_argument('sernum', nargs='?', help='Filter all inventory history data for a given '
                                                                 'serial number')
        replacement_parser.add_argument('--limit', type=int, help='Provide a limit to the number of records of data '
                                                                'being retrieved. The default value is 100')
        replacement_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. '
                                           'Default will be to display data in tabular format')
        replacement_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        replacement_parser.add_argument('--all', help='Specify all output fields for more information than default view',
                                      action='store_true')
        replacement_parser.set_defaults(func=self._view_replacement_history_execute)
    def _add_job_info_parser(self, view_parser):
        job_parser = view_parser.add_parser('job', help='view the job information for the cluster')
        job_parser.add_argument('--start-time', dest="start_time",
                                      help='provide the start time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--end-time', dest="end_time",
                                      help='provide the end time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--at-time', dest="at_time",
                                      help='provide a time to display all jobs running at that time.. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        job_parser.add_argument('--jobid', help='Filter all job data for a given jobid. This will also'
                                                ' display accounting information and nodes for given jobid')
        job_parser.add_argument('--user', dest="username", help='Filter all job data for a given username')
        job_parser.add_argument('--locations', help='Filter all job data for the given locations')
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
        reservation_parser = view_parser.add_parser('reservation', help='view the reservation information for the cluster')
        reservation_parser.add_argument('--start-time', dest="start_time",
                                      help='provide the start time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        reservation_parser.add_argument('--end-time', dest="end_time",
                                      help='provide the end time. The preferred format for the date is'
                                           ' "YYYY-MM-DD HH:MM:SS.[f]" to ensure higher precision')
        reservation_parser.add_argument('--name', help='Filter all reservation data for a given reservation name.')
        reservation_parser.add_argument('--user', dest="username", help='Filter all reservation data for a given username')
        reservation_parser.add_argument('--limit', default=100, type=int,
                                                  help='Provide a limit to the number of records of data being retrieved. '
                                                       'The default value is 100')
        reservation_parser.add_argument('--format', choices=['json', 'table'], default='table',
                                      help='Display data either in JSON or table format. Default will be to display '
                                           'data in tabular format')
        reservation_parser.add_argument('--timeout', default=900, type=int, help='Timeout value for HTTP request. '
                                                                               'Uses a default of 900s')
        reservation_parser.set_defaults(func=self._view_reservation_info_execute)

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
    def assert_regex(regex):
        try:
            re.compile(regex)
            return None
        except re.error as err:
            return "reason: {}".format(str(err))


    @staticmethod
    def get_filter_path(filename):
        user_home = os.path.expanduser("~")
        filter_folder = ".ucs"
        filter_path = "{}/{}/{}".format(user_home, filter_folder, filename)
        if not os.path.exists(filter_path):
            return None
        return filter_path

    def get_regular_expressions(self, filename):
        filter_path = self.get_filter_path(filename)
        if not filter_path:
            return None
        with open(filter_path, 'r') as f:
            content = f.readlines()
        expressions = []
        for expression in content:
            expression = expression.replace('\n', '')
            expressions.append(expression)
        expressions = "|".join(expressions)
        return expressions

    @staticmethod
    def regex_error(expression="", error=None, _file=""):
        msg = "Error: Invalid regular expression"
        if _file:
            return None, "{0} \"{1}\" ({2}) {3}".format(msg, expression, _file, error)
        return None, "{0} \"{1}\" {2}".format(msg, expression, error)


    def get_filters(self, filter_type, arg_name, argument):
        expressions = self.get_regular_expressions(filter_type)
        if expressions:
            error = self.assert_regex(expressions)
            if error:
                file_path = self.get_filter_path(filter_type)
                return self.regex_error(expressions, error, file_path)

        regex_args = ''
        if argument is not None:
            error = self.assert_regex(argument)
            if error:
                return self.regex_error(argument, error)
            regex_args = "{0}={1}".format(arg_name, argument)

        if regex_args and expressions:
            regex_args = regex_args + "|" + expressions
        elif expressions:
            regex_args  = "{0}={1}".format(arg_name, expressions)
        return regex_args, None


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

        exclude, error = self.get_filters("exclude", "Exclude", args.exclude)
        if error:
            return CommandResult(1, error)

        event_type, error = self.get_filters("include", "EventType", args.type)
        if error:
            return CommandResult(1, error)

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

    def _view_replacement_history_execute(self, args):
        client = HttpClient()
        if args.sernum is not None and args.locations is not None:
            response_msg = 'The inventory change takes either a lctn or a serial number as an input filter. ' \
                           'Try again with only one of them'
            return self._parse_response_as_per_user_request(args.format, 1,
                                                            JsonError(response_msg).construct_error_result())
        if args.sernum is None and args.locations is None:
            response_msg = 'The inventory change requires one of the input filters lctn or a serial number. ' \
                           'Try again with one of them'
            return self._parse_response_as_per_user_request(args.format, 1,
                                                            JsonError(response_msg).construct_error_result())
        starttime, endtime = self._retrieve_time_from_args(args)
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        user = 'user=' + self.user
        url = client.get_base_url() + 'cli/getinvchanges?' + "&".join(
            [x for x in [starttime, endtime, limit, lctn, user] if x != ""])
        self.lgr.debug("_view_replacement_history_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["foreigntimestamp", "id", "action", "fruid"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_inventory_execute(self, args):
        def pretty_format_json_dict_str(json_dict):
            return json.dumps(json_dict, sort_keys=True,
                              indent=4, separators=(',', ': '))

        def decode_json_element_str(str):
            try:
                return json.loads(str)
            except ValueError:
                return str
            except TypeError:
                return str

        def fix_up_hw_info_dict(hw_info_dict):
            for key in hw_info_dict.keys():
                hw_info_dict[key]['value'] = decode_json_element_str(hw_info_dict[key]['value'])
            return hw_info_dict

        def pretty_format_inventory_info_dict_list_str(node_location_hist_list, component):
            if not node_location_hist_list:
                return ''

            pretty_formatted_str = '============================================================================\n'
            pretty_formatted_str += 'Details:\n'
            pretty_formatted_str += '============================================================================\n'
            for node_location_hist_entry in node_location_hist_list:
                location = node_location_hist_entry[0]
                serial_number = node_location_hist_entry[4]
                inventory_timestamp = get_inventory_timestamp(node_location_hist_entry)

                inventory_info_dict = json.loads(node_location_hist_entry[3])
                hw_info_dict = inventory_info_dict['HWInfo']
                fixed_up_hw_info_dict = fix_up_hw_info_dict(hw_info_dict)

                if component is not None:
                    fixed_up_hw_info_dict = extract_component_dict(fixed_up_hw_info_dict, component)
                    location = get_component_loc(hw_info_dict, component)
                    serial_number = get_component_fru_id(hw_info_dict, component)
                pretty_formatted_str += location + ' contains ' + serial_number + ' at ' + inventory_timestamp + ':\n'
                pretty_formatted_str += \
                    pretty_format_json_dict_str(fixed_up_hw_info_dict) + '\n'
                pretty_formatted_str += '----------------------------------------------------------------------------\n'
            return pretty_formatted_str

        def pretty_format_response_str(response_str, component=None):
            response_dict = json.loads(response_str)
            node_location_hist_list = response_dict['data']
            return pretty_format_inventory_info_dict_list_str(node_location_hist_list, component)

        def extract_component_dict(hw_info_dict, component):
            return {
                f"fru/{component}/loc": get_component_loc(hw_info_dict, component),
                f"fru/{component}/loc_info": component_loc_info(hw_info_dict, component),
                f"fru/{component}/fru_id": get_component_fru_id(hw_info_dict, component),
                f"fru/{component}/fru_info": component_fru_info(hw_info_dict, component),
            }

        def get_component_fru_id(hw_info_dict, component):
            return hw_info_dict[f"fru/{component}/fru_id"]['value']

        def get_component_loc(hw_info_dict, component):
            return hw_info_dict[f"fru/{component}/loc"]['value']

        def component_loc_info(hw_info_dict, component):
            return hw_info_dict[f"fru/{component}/loc_info"]['value']

        def component_fru_info(hw_info_dict, component):
            return hw_info_dict[f"fru/{component}/fru_info"]['value']

        def get_inventory_timestamp(node_location_hist_entry):
            return node_location_hist_entry[2]

        client = HttpClient()
        if not args.history:
            limit, lctn, display_format, time_out = self._retrieve_from_args(args)
            location_parts = lctn.split('_')  # this may change since it looks really weird

            component_lctn = None
            if len(location_parts) == 2:
                lctn = location_parts[0]
                component_lctn = location_parts[1]

            user = 'user=' + self.user
            sernum = ''
            if args.sernum is not None:
                sernum = 'Sernum=' + str(args.sernum)
            url = client.get_base_url() + 'cli/getnodeinvinfo?' + "&".join([x for x in [limit, lctn, user, sernum] if x != ""])
            self.lgr.debug("_view_inventory_execute: URL for request is {0}".format(url))
            response_code, response = client.send_get_request(url, time_out)

            json_display = JsonDisplay(response)

            if display_format == 'json':
                data_to_display = json_display.display_raw_json()
            else:
                columns_order = ["lctn", "inventorytimestamp", "sernum"]
                data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
                data_to_display += '\n' + pretty_format_response_str(response, component_lctn)
        else:
            limit, lctn, display_format, time_out = self._retrieve_from_args(args)
            user = 'user=' + self.user
            url = client.get_base_url() + 'cli/getinvhislctn?' + "&".join(
            [x for x in [limit, lctn, user] if x != ""])
            self.lgr.debug("_view_inventory_change_execute: URL for request is {0}".format(url))
            response_code, response = client.send_get_request(url, time_out)

            json_display = JsonDisplay(response)

            if display_format == 'json':
                data_to_display = json_display.display_raw_json()
            else:
                columns_order = ["id", "fruid"]
                data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_state_execute(self, args):
        client = HttpClient()
        limit, lctn, display_format, time_out = self._retrieve_from_args(args)
        user = 'user=' + self.user
        url = client.get_base_url() + 'cli/getinvspecificlctn?' + "&".join([x for x in [limit, lctn, user] if x != ""])
        self.lgr.debug("_view_state_execute: URL for request is {0}".format(url))
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
        self.lgr.debug("_view_network_config_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, time_out)

        json_display = JsonDisplay(response)

        if display_format == 'json':
            data_to_display = json_display.display_raw_json()
        else:
            columns_order = ["lctn", "type", "hostname", "ipaddr", "macaddr", "bmchostname", "bmcipaddr", "bmcmacaddr"]
            data_to_display = '\n' + json_display.display_json_in_tabular_format(columns_order)
        return CommandResult(response_code, data_to_display)

    def _view_job_info_execute(self, args):
            client = HttpClient()
            starttime, endtime = self._retrieve_time_from_args(args)
            limit, lctn, display_format, time_out = self._retrieve_from_args(args)
            user = 'user=' + self.user
            if args.username is not None:
                self._validate_input(args.username)
                username = 'Username=' + args.username
            else:
                username = ''
            if args.jobid is not None:
                self._validate_input(args.jobid)
                jobid = 'Jobid=' + args.jobid
            else:
                jobid = ''
            if args.active:
                state = 'State=S'
            else:
                state = ''
            if args.at_time is not None:
                attime = 'AtTime=' + self._validate_input_timestamp(args.at_time)
            else:
                attime = ''
            url = client.get_base_url() + 'cli/getjobinfo?' + "&".join(
                [x for x in [starttime, endtime, limit, user, username, jobid, state, lctn, attime] if x != ""])
            self.lgr.debug("_view_job_info_execute: URL for request is {0}".format(url))
            response_code, response = client.send_get_request(url, time_out)

            json_display = JsonDisplay(response)

            if display_format == 'json':
                data_to_display = json_display.display_raw_json()
            else:
                columns_order = ["jobid", "jobname", "state", "numnodes", "username", "starttimestamp", "endtimestamp"]
                if args.jobid is not None or args.all or args.locations is not None:
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
                self._validate_input(args.username)
                username = 'Username=' + args.username
            else:
                username = ''
            if args.name is not None:
                self._validate_input(args.name)
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

    def _validate_input(self, input):
        if self.is_bad_input(input):
            raise RuntimeError("Bad input, please try with a valid input")

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
        if "?" in parameter or "*" in parameter or "%" in parameter or "$" in parameter:
            return True
        return False
