# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
This module creates the command line parser and executes the user commands.
"""
import os
import sys
import argparse
import pkg_resources

from .events import EventsCli
from .command_result import CommandResult


class Parser(object):
    """Parser Class"""

    CLI_COMMAND = 'eventsim'

    def __init__(self):
        """Init Function for EventSim Cli Parser"""

        self.parser = argparse.ArgumentParser(prog=self.CLI_COMMAND,
                                              description='EventSim CLI Parser')
        self.subparser = self.parser.add_subparsers(
            title='Sub Commands',
            description='List of Valid Sub Commands', dest='subparser_name')
        self._add_simple_args()
        EventsCli(self.subparser.add_parser('events', help='Generate events of specified type.'))

    def _add_simple_args(self):
        """Add the simple arguments here"""
        version = pkg_resources.require("eventsim_cli")[0].version
        self.parser.add_argument('-V', '--version', action='version', version=version,
                                 help='Provides the version of the tool')

    def execute_cli_cmd(self):
        """Function to call appropriate sub-parser"""
        if len(sys.argv) == 1:
            self.parser.print_help()
            sys.exit(1)
        args = self.parser.parse_args()
        try:
            return Parser.handle_command_result(args.func(args))
        except RuntimeError as e:
            print(e, file=sys.stderr)
            return 1
        except KeyboardInterrupt:
            print(CommandResult(1, 'User has interrupted the command execution'))
            return 1

    @staticmethod
    def handle_command_result(command_result):
        """
        Handle the result coming out of CLI
        :param command_result:
        :return:
        """
        if type(command_result) is CommandResult:
            if command_result.return_code != 0:
                print(command_result, file=sys.stderr)
            else:
                command_result.message = CommandResult.parse_command_response(command_result.message)
                print(command_result)
            return command_result.return_code
        else:
            print("Invalid result type for command is being used", file=sys.stderr)
            return 1
