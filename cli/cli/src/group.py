# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
CLI commands for controlling Logical group management.
"""
from .command_result import CommandResult
from .http_client import HttpClient
from .device_regex_resolver import DeviceRegexResolver
from .logger import Logger
import pwd
import os


class GroupCli(object):
    """CLI for controlling Groups"""

    def __init__(self, group_parser):
        self._group_parser = group_parser
        group_subparsers = group_parser.add_subparsers(help='Subparser for group')
        self._group_parser.set_defaults(func=self._group_execute)
        self._add_add_parser(group_subparsers)
        self._add_get_parser(group_subparsers)
        self._add_list_parser(group_subparsers)
        self._add_remove_parser(group_subparsers)
        self.lgr = Logger()
        self.user = pwd.getpwuid(os.getuid()).pw_name

    def _add_add_parser(self, group_parser):
        add_parser = group_parser.add_parser('add', help='Add location regex to a group. Add locations to one group '
                                                         'at a time. Use: group add node_regex group_name')
        add_parser.add_argument(
            'locations', help='Provide comma separated location list or location regex for the device')
        add_parser.add_argument('group_name', metavar='group-name', help='Group name where the locations have to be '
                                                                         'added to')
        add_parser.set_defaults(func=self._group_add_execute)

    def _add_remove_parser(self, group_parser):
        remove_parser = group_parser.add_parser('remove', help='Remove location regex from a group. Remove locations '
                                                               'from one group at a time. '
                                                               'Use: group remove location_regex group_name')
        remove_parser.add_argument(
            'locations', help='Provide comma separated location list or location regex for the device')
        remove_parser.add_argument('group_name', metavar='group-name', help='Group name where the locations have to be '
                                                                            'added to')
        remove_parser.set_defaults(func=self._group_remove_execute)

    def _add_get_parser(self, group_parser):
        get_parser = group_parser.add_parser('get', help='Get the location regex from a group. Get locations from one '
                                                         'group at a time. Use: group get group_name')
        get_parser.add_argument('group_name', metavar='group-name', help='Group name where the locations have to be '
                                                                         'added to')
        get_parser.set_defaults(func=self._group_get_execute)

    def _add_list_parser(self, group_parser):
        list_parser = group_parser.add_parser('list', help='List the groups created so far. Use: group list')
        list_parser.set_defaults(func=self._group_list_execute)

    def _group_execute(self, args):
        self._group_parser.print_help()

    def _devices_that_dont_exist_in_group(self, args):
        try:
            command_result = self._group_get_execute(args)
        except RuntimeError:
            command_result = CommandResult(0, '')
        devices = DeviceRegexResolver.get_devices(args.locations)
        return set(devices.split(',')) - set(command_result.message.split(','))

    def _group_add_execute(self, args):
        if ',' in args.group_name:
            return CommandResult(2, "group name shouldn't contain comma")
        message_for_existing_devices = ''
        devices_to_add = self._devices_that_dont_exist_in_group(args)
        if len(devices_to_add) == 0:
            return CommandResult(0, 'location(s) may already be part of the group')
        elif len(devices_to_add) != len(DeviceRegexResolver.get_devices(args.locations).split(',')):
            message_for_existing_devices = 'devices(s) may already be part of the group\n'
        client = HttpClient()
        # URL will be PUT http://hostaddress:hostport/groups/group_name?devices=devicelist
        url = client.get_base_url() + 'groups/' + args.group_name
        parameters = {'devices': (','.join(devices_to_add)), 'user': self.user}
        self.lgr.debug("_group_add_execute: URL for request is {0}".format(url))
        response_code, response = client.send_put_request(url, parameters, 900)
        return CommandResult(response_code, message_for_existing_devices + response)

    def _group_remove_execute(self, args):
        if ',' in args.group_name:
            return CommandResult(2, "group name shouldn't contain comma")
        client = HttpClient()
        # URL will be DELETE http://hostaddress:hostport/groups/group_name?devices=devicelist
        url = client.get_base_url() + 'groups/' + args.group_name + '?' + "devices=" \
            + DeviceRegexResolver.get_devices(args.locations) + '&user=' + self.user
        self.lgr.debug("_group_remove_execute: URL for request is {0}".format(url))
        response_code, response = client.send_delete_request(url, 900)
        return CommandResult(response_code, response)

    def _group_get_execute(self, args):
        client = HttpClient()
        # URL will be GET http://hostaddress:hostport/groups/group_name
        url = client.get_base_url() + 'groups/' + args.group_name + '?user=' + self.user
        self.lgr.debug("_group_get_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)
        return CommandResult(response_code, response)

    def _group_list_execute(self, args):
        client = HttpClient()
        # URL will be GET http://hostaddress:hostport/groups/group_name
        url = client.get_base_url() + 'groups' + '?user=' + self.user
        self.lgr.debug("_group_list_execute: URL for request is {0}".format(url))
        response_code, response = client.send_get_request(url, 900)
        return CommandResult(response_code, response)
