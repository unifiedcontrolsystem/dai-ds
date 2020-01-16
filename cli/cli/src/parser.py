# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
This module creates the command line parser and executes the user commands.
"""
import os
import sys
import argparse
import pkg_resources

from .group import GroupCli
from .view import ViewCli
from .command_result import CommandResult


class Parser(object):
    """Parser Class"""

    CLI_COMMAND = 'ucs'
    restricted_characters = ['#']

    def __init__(self):
        """Init Function for Control Cli Parser"""
        self.CLI_COMMAND = os.path.basename(sys.argv[0])
        self.parser = argparse.ArgumentParser(prog=self.CLI_COMMAND,
                                              description='UCS CLI')
        self.subparser = self.parser.add_subparsers(
            title='Sub Commands',
            description='List of Valid Sub Commands', dest='subparser_name')
        GroupCli(self.subparser.add_parser('group', help='Logical group management commands'))
        ViewCli(self.subparser.add_parser('view', help='View data in database commands'))

    def execute_cli_cmd(self):
        """Function to call appropriate sub-parser"""
        if len(sys.argv) == 1:
            self.parser.print_help()
            sys.exit(1)
        args = self.parser.parse_args()
        try:
            self._check_arguments(args)
            self._validate_arguments_data(args)
            return Parser.handle_command_result(args.func(args))
        except RuntimeError as e:
            print(CommandResult(1, str(e)), file=sys.stderr)
            return 1
        except KeyboardInterrupt:
            print(CommandResult(1, 'User has interrupted the command execution'), file=sys.stderr)
            return 1
        except IOError as e:
            print(CommandResult(1, str(e)), file=sys.stderr)
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
            print(CommandResult(1, 'Invalid result type for command is being used'), file=sys.stderr)
            return 1

    def _check_arguments(self, args):
        if args is not None:
            arguments = list(args.__dict__.keys())
            items = list([x for x in arguments if arguments.count(x) > 1])
            if len(items) != 0:
                msg = 'User cannot enter optional arguments multiple times'
                raise IOError(msg)

    def _validate_arguments_data(self, args):
        if args is not None:
            inputs = list(args.__dict__.values())
            for bad_char in self.restricted_characters:
                for data in inputs:
                    if bad_char in str(data):
                        msg = 'Error: input data contains special characters. Ex:[#]'
                        raise IOError(msg)
