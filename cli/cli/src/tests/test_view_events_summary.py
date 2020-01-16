# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the ViewEventsSummary class
"""
import sys
import io
import json
import tempfile
import os
from unittest import TestCase
from mock import patch
from ..parser import Parser
from ..view import ViewCli
from ..view_events_summary import ViewEventsSummary


class TestViewEventsSummary(TestCase):

    def test_generate_summary(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--summary']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status": "F",
                                                                  "Result": "{\"schema\":[{\"unit\":\"string\","
                                                                            "\"data\":\"type\","
                                                                            "\"heading\":\"type\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"time\","
                                                                            "\"heading\":\"time\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"dbupdatedtimestamp\","
                                                                            "\"heading\":\"dbupdatedtimestamp\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"severity\","
                                                                            "\"heading\":\"severity\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"lctn\","
                                                                            "\"heading\":\"lctn\"},{\"unit\":\"string\","
                                                                            "\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"controloperation\","
                                                                            "\"heading\":\"controloperation\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"detail\","
                                                                            "\"heading\":\"detail\"}],"
                                                                            "\"result-data-lines\":3,"
                                                                            "\"result-status-code\":0,"
                                                                            "\"data\":[[\"0011000007\","
                                                                            "\"2019-07-09 17:04:10.576635\","
                                                                            "\"2019-07-09 17:04:11.196\",\"ERROR\","
                                                                            "\"SN1-M\",null,\"ErrorOnNode\","
                                                                            "\"ucs-stop-dai-mgr is being run to "
                                                                            "shutdown DaiMgr:\","
                                                                            "\"InvokedBy=ucs-stop-dai-mgr\"],"
                                                                            "[\"0011000007\","
                                                                            "\"2019-08-09 17:04:10.576635\","
                                                                            "\"2019-08-09 17:04:11.196\",\"ERROR\","
                                                                            "\"SN1-M\",null,\"ErrorOnNode\","
                                                                            "\"ucs-stop-dai-mgr is being run to "
                                                                            "shutdown DaiMgr:\","
                                                                            "\"InvokedBy=ucs-stop-dai-mgr\"],"
                                                                            "[\"RasMntrSensysSyslogEvent\","
                                                                            "\"2019-10-25 07:00:02.574217\","
                                                                            " \"2019-10-25 07:00:11.291\","
                                                                            "null, null, null, \"\","
                                                                            "\"[venus-smw] Metric"
                                                                            " file_monitor:/var/log/messages with "
                                                                            "value 2019-10-25T00:00:02.398442-07:00 "
                                                                            "venus-smw systemd[1]: Reloaded System "
                                                                            "Logging Service.\","
                                                                            "\"Sensys detected a critical "
                                                                            "event from syslog:\"]],"
                                                                            "\"result-data-columns\":8}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('ErrorOnNode', captured_output.getvalue())
        captured_output.close()

    def test_generate_summary_format_json(self):
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--summary', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status": "F",
                                                                  "Result": "{\"schema\":[{\"unit\":\"string\","
                                                                            "\"data\":\"type\","
                                                                            "\"heading\":\"type\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"time\","
                                                                            "\"heading\":\"time\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"dbupdatedtimestamp\","
                                                                            "\"heading\":\"dbupdatedtimestamp\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"severity\","
                                                                            "\"heading\":\"severity\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"lctn\","
                                                                            "\"heading\":\"lctn\"},{\"unit\":\"string\","
                                                                            "\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"controloperation\","
                                                                            "\"heading\":\"controloperation\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"detail\","
                                                                            "\"heading\":\"detail\"}}],"
                                                                            "\"result-data-lines\":1,"
                                                                            "\"result-status-code\":0,"
                                                                            "\"data\":[[\"0011000007\","
                                                                            "\"2019-07-09 17:04:10.576635\","
                                                                            "\"2019-07-09 17:04:11.196\",\"ERROR\","
                                                                            "\"SN1-M\",null,\"ErrorOnNode\","
                                                                            "\"ucs-stop-dai-mgr is being run to "
                                                                            "shutdown DaiMgr:\","
                                                                            "\"InvokedBy=ucs-stop-dai-mgr\"]],"
                                                                            "\"result-data-columns\":8}"})
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Summary option cannot be used with format option.', captured_output.getvalue())
        captured_output.close()

    def test_generate_summary_format_table(self):
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--summary', '--format', 'table']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status": "F",
                                                                  "Result": "{\"schema\":[{\"unit\":\"string\","
                                                                            "\"data\":\"type\","
                                                                            "\"heading\":\"type\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"time\","
                                                                            "\"heading\":\"time\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"dbupdatedtimestamp\","
                                                                            "\"heading\":\"dbupdatedtimestamp\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"severity\","
                                                                            "\"heading\":\"severity\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"lctn\","
                                                                            "\"heading\":\"lctn\"},{\"unit\":\"string\","
                                                                            "\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                                                            "{\"unit\":\"string\","
                                                                            "\"data\":\"controloperation\","
                                                                            "\"heading\":\"controloperation\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"detail\","
                                                                            "\"heading\":\"detail\"}],"
                                                                            "\"result-data-lines\":1,"
                                                                            "\"result-status-code\":0,"
                                                                            "\"data\":[[\"0011000007\","
                                                                            "\"2019-07-09 17:04:10.576635\","
                                                                            "\"2019-07-09 17:04:11.196\",\"ERROR\","
                                                                            "\"SN1-M\",null,\"ErrorOnNode\","
                                                                            "\"ucs-stop-dai-mgr is being run to "
                                                                            "shutdown DaiMgr:\","
                                                                            "\"InvokedBy=ucs-stop-dai-mgr\"]],"
                                                                            "\"result-data-columns\":8}"})
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Summary option cannot be used with format option.', captured_output.getvalue())
        captured_output.close()

    def test_generate_summary_runtime_error(self):
        with self.assertRaises(RuntimeError):
            ViewEventsSummary(None)

