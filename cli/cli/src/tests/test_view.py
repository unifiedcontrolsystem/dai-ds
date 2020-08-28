# -*- coding: utf-8 -*-
# !/usr/bin/env python
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Test the viewclass in cli implementation.
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


class ViewTest(TestCase):

    def test_view_help(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view']
        parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{env,event,inventory,job,network-config,replacement-history,'
                      'reservation,state,system-info}', captured_output.getvalue())
        captured_output.close()

    def test_view_help_1(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{env,event,inventory,job,network-config,replacement-history,'
                      'reservation,state,system-info}', captured_output.getvalue())
        captured_output.close()

    def test_view_help_2(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{env,event,inventory,job,network-config,replacement-history,'
                      'reservation,state,system-info}', captured_output.getvalue())
        captured_output.close()

    def test_view_system_info(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'system-info']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"compute\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"SEQUENCENUMBER\","
                                         "\"heading\":\"SEQUENCENUMBER\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGWORKITEMID\","
                                         "\"heading\":\"LASTCHGWORKITEMID\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCMACADDR\",\"heading\":\"BMCMACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"BOOTIMAGEID\",\"heading\":\"BOOTIMAGEID\"},"
                                         "{\"unit\":\"string\",\"data\":\"ENVIRONMENT\",\"heading\":\"ENVIRONMENT\"},"
                                         "{\"unit\":\"string\",\"data\":\"WLMNODESTATE\",\"heading\":\"WLMNODESTATE\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCIPADDR\",\"heading\":\"BMCIPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"STATE\",\"heading\":\"STATE\"},"
                                         "{\"unit\":\"string\",\"data\":\"IPADDR\",\"heading\":\"IPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGTIMESTAMP\","
                                         "\"heading\":\"LASTCHGTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"AGGREGATOR\",\"heading\":\"AGGREGATOR\"},"
                                         "{\"unit\":\"string\",\"data\":\"INVENTORYINFO\","
                                         "\"heading\":\"INVENTORYINFO\"},"
                                         "{\"unit\":\"string\",\"data\":\"LCTN\",\"heading\":\"LCTN\"},"
                                         "{\"unit\":\"string\",\"data\":\"DBUPDATEDTIMESTAMP\","
                                         "\"heading\":\"DBUPDATEDTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"OWNER\",\"heading\":\"OWNER\"},"
                                         "{\"unit\":\"string\",\"data\":\"SERNUM\",\"heading\":\"SERNUM\"},"
                                         "{\"unit\":\"string\",\"data\":\"HOSTNAME\",\"heading\":\"HOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCHOSTNAME\",\"heading\":\"BMCHOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGADAPTERTYPE\","
                                         "\"heading\":\"LASTCHGADAPTERTYPE\"},"
                                         "{\"unit\":\"string\",\"data\":\"MACADDR\",\"heading\":\"MACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"TYPE\",\"heading\":\"TYPE\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[0,4,\"53:54:00:47:a4:00\",\"centos7.3-default\",null,\"U\","
                                         "\"192.168.10.105\",\"A\",\"192.168.0.85\",\"2019-07-16 16:47:16.465\","
                                         "\"SN1-M\",null,\"R48-CH00-CN0\",\"2019-07-16 16:47:16.466\",\"W\",null,"
                                         "\"c01\",\"R48-CH00-CN0-BMC\",\"DAI_MGR\",\"00:1e:67:38:8f:c1\","
                                         "\"dense-compute-node\"]],\"result-data-columns\":21},"
                                         "\"service\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"SEQUENCENUMBER\","
                                         "\"heading\":\"SEQUENCENUMBER\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGWORKITEMID\","
                                         "\"heading\":\"LASTCHGWORKITEMID\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCMACADDR\",\"heading\":\"BMCMACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"BOOTIMAGEID\",\"heading\":\"BOOTIMAGEID\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCIPADDR\",\"heading\":\"BMCIPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"STATE\",\"heading\":\"STATE\"},"
                                         "{\"unit\":\"string\",\"data\":\"IPADDR\",\"heading\":\"IPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGTIMESTAMP\","
                                         "\"heading\":\"LASTCHGTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"AGGREGATOR\",\"heading\":\"AGGREGATOR\"},"
                                         "{\"unit\":\"string\",\"data\":\"INVENTORYINFO\","
                                         "\"heading\":\"INVENTORYINFO\"},"
                                         "{\"unit\":\"string\",\"data\":\"LCTN\",\"heading\":\"LCTN\"},"
                                         "{\"unit\":\"string\",\"data\":\"DBUPDATEDTIMESTAMP\","
                                         "\"heading\":\"DBUPDATEDTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"OWNER\",\"heading\":\"OWNER\"},"
                                         "{\"unit\":\"string\",\"data\":\"SERNUM\",\"heading\":\"SERNUM\"},"
                                         "{\"unit\":\"string\",\"data\":\"HOSTNAME\",\"heading\":\"HOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCHOSTNAME\",\"heading\":\"BMCHOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGADAPTERTYPE\","
                                         "\"heading\":\"LASTCHGADAPTERTYPE\"},"
                                         "{\"unit\":\"string\",\"data\":\"MACADDR\",\"heading\":\"MACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"TYPE\",\"heading\":\"TYPE\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[0,1,\"53:54:00:a1:e2:e3\",\"centos7.3-default\","
                                         "\"192.168.10.100\",\"A\",\"192.168.0.15\",\"2019-07-16 16:47:16.426\","
                                         "\"manual\",null,\"SN1-M\",\"2019-07-16 16:47:16.431\",\"G\",null,\"master3\","
                                         "\"manual\",\"DAI_MGR\",\"00:1e:67:3e:7f:49\",\"dense-service-node\"]],"
                                         "\"result-data-columns\":19}}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LCTN', captured_output.getvalue())
        captured_output.close()

    def test_view_system_info_json_format(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'system-info', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"compute\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"SEQUENCENUMBER\","
                                         "\"heading\":\"SEQUENCENUMBER\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGWORKITEMID\","
                                         "\"heading\":\"LASTCHGWORKITEMID\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCMACADDR\",\"heading\":\"BMCMACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"BOOTIMAGEID\",\"heading\":\"BOOTIMAGEID\"},"
                                         "{\"unit\":\"string\",\"data\":\"ENVIRONMENT\",\"heading\":\"ENVIRONMENT\"},"
                                         "{\"unit\":\"string\",\"data\":\"WLMNODESTATE\",\"heading\":\"WLMNODESTATE\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCIPADDR\",\"heading\":\"BMCIPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"STATE\",\"heading\":\"STATE\"},"
                                         "{\"unit\":\"string\",\"data\":\"IPADDR\",\"heading\":\"IPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGTIMESTAMP\","
                                         "\"heading\":\"LASTCHGTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"AGGREGATOR\",\"heading\":\"AGGREGATOR\"},"
                                         "{\"unit\":\"string\",\"data\":\"INVENTORYINFO\","
                                         "\"heading\":\"INVENTORYINFO\"},"
                                         "{\"unit\":\"string\",\"data\":\"LCTN\",\"heading\":\"LCTN\"},"
                                         "{\"unit\":\"string\",\"data\":\"DBUPDATEDTIMESTAMP\","
                                         "\"heading\":\"DBUPDATEDTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"OWNER\",\"heading\":\"OWNER\"},"
                                         "{\"unit\":\"string\",\"data\":\"SERNUM\",\"heading\":\"SERNUM\"},"
                                         "{\"unit\":\"string\",\"data\":\"HOSTNAME\",\"heading\":\"HOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCHOSTNAME\",\"heading\":\"BMCHOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGADAPTERTYPE\","
                                         "\"heading\":\"LASTCHGADAPTERTYPE\"},"
                                         "{\"unit\":\"string\",\"data\":\"MACADDR\",\"heading\":\"MACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"TYPE\",\"heading\":\"TYPE\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[0,4,\"53:54:00:47:a4:00\",\"centos7.3-default\",null,\"U\","
                                         "\"192.168.10.105\",\"A\",\"192.168.0.85\",\"2019-07-16 16:47:16.465\","
                                         "\"SN1-M\",null,\"R48-CH00-CN0\",\"2019-07-16 16:47:16.466\",\"W\",null,"
                                         "\"c01\",\"R48-CH00-CN0-BMC\",\"DAI_MGR\",\"00:1e:67:38:8f:c1\","
                                         "\"dense-compute-node\"]],\"result-data-columns\":21},"
                                         "\"service\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"SEQUENCENUMBER\","
                                         "\"heading\":\"SEQUENCENUMBER\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGWORKITEMID\","
                                         "\"heading\":\"LASTCHGWORKITEMID\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCMACADDR\",\"heading\":\"BMCMACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"BOOTIMAGEID\",\"heading\":\"BOOTIMAGEID\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCIPADDR\",\"heading\":\"BMCIPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"STATE\",\"heading\":\"STATE\"},"
                                         "{\"unit\":\"string\",\"data\":\"IPADDR\",\"heading\":\"IPADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGTIMESTAMP\","
                                         "\"heading\":\"LASTCHGTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"AGGREGATOR\",\"heading\":\"AGGREGATOR\"},"
                                         "{\"unit\":\"string\",\"data\":\"INVENTORYINFO\","
                                         "\"heading\":\"INVENTORYINFO\"},"
                                         "{\"unit\":\"string\",\"data\":\"LCTN\",\"heading\":\"LCTN\"},"
                                         "{\"unit\":\"string\",\"data\":\"DBUPDATEDTIMESTAMP\","
                                         "\"heading\":\"DBUPDATEDTIMESTAMP\"},"
                                         "{\"unit\":\"string\",\"data\":\"OWNER\",\"heading\":\"OWNER\"},"
                                         "{\"unit\":\"string\",\"data\":\"SERNUM\",\"heading\":\"SERNUM\"},"
                                         "{\"unit\":\"string\",\"data\":\"HOSTNAME\",\"heading\":\"HOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"BMCHOSTNAME\",\"heading\":\"BMCHOSTNAME\"},"
                                         "{\"unit\":\"string\",\"data\":\"LASTCHGADAPTERTYPE\","
                                         "\"heading\":\"LASTCHGADAPTERTYPE\"},"
                                         "{\"unit\":\"string\",\"data\":\"MACADDR\",\"heading\":\"MACADDR\"},"
                                         "{\"unit\":\"string\",\"data\":\"TYPE\",\"heading\":\"TYPE\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[0,1,\"53:54:00:a1:e2:e3\",\"centos7.3-default\","
                                         "\"192.168.10.100\",\"A\",\"192.168.0.15\",\"2019-07-16 16:47:16.426\","
                                         "\"manual\",null,\"SN1-M\",\"2019-07-16 16:47:16.431\",\"G\",null,\"master3\","
                                         "\"manual\",\"DAI_MGR\",\"00:1e:67:3e:7f:49\",\"dense-service-node\"]],"
                                         "\"result-data-columns\":19}}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LASTCHGADAPTERTYPE', captured_output.getvalue())
        self.assertIn('compute', captured_output.getvalue())
        self.assertIn('service', captured_output.getvalue())
        captured_output.close()

    def test_view_system_info_summary(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'system-info', '--summary']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"compute\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                         "{\"unit\":\"string\",\"data\":\"count\",\"heading\":\"count\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[\"ACTIVE\",10000]],\"result-data-columns\":2},"
                                         "\"service\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                         "{\"unit\":\"string\",\"data\":\"count\",\"heading\":\"count\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[\"ACTIVE\",10000]],\"result-data-columns\":2}}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('STATE', captured_output.getvalue())
        captured_output.close()

    def test_view_system_info_summary_json_format(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'system-info', '--summary', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"compute\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                         "{\"unit\":\"string\",\"data\":\"count\",\"heading\":\"count\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[\"ACTIVE\",10000]],\"result-data-columns\":2},"
                                         "\"service\":{\"schema\":"
                                         "[{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                         "{\"unit\":\"string\",\"data\":\"count\",\"heading\":\"count\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[\"ACTIVE\",10000]],\"result-data-columns\":2}}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('state', captured_output.getvalue())
        self.assertIn('compute', captured_output.getvalue())
        self.assertIn('service', captured_output.getvalue())
        captured_output.close()

    def test_events_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--start-time', '2019-07-09', '--end-time', '2019-07-09']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status":"F",
                                                                  "Result":"{\"schema\":[{\"unit\":\"string\","
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
                                                                           "shutdown DaiMgr:\"]],"
                                                                           "\"result-data-columns\":8}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('CONTROLOPERATION', captured_output.getvalue())
        captured_output.close()

    def test_events_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--start-time', '2019-07-09', '--end-time', '2019-07-09',
                    '--format', 'json']
        with patch(
                'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
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
                                                                            "shutdown DaiMgr: InvokedBy="
                                                                            "ucs-stop-dai-mgr\"]],"
                                                                            "\"result-data-columns\":8}"}
                                                                 )
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('controloperation', captured_output.getvalue())
        captured_output.close()

    def test_events_bad_job_input(self):
        captured_output = io.StringIO()
        sys.stderr = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--start-time', '2019-07-09', '--end-time', '2019-07-09', '--jobid', '123?']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"	
            with patch('requests.post') as patched_get:	
                type(patched_get.return_value).text = json.dumps({"Status": "E",	
                                "Result": "Bad input, please try with a valid jobid"	
                                })	
                type(patched_get.return_value).status_code = 200	
                parser.execute_cli_cmd()
        sys.stderr = sys.__stderr__
        self.assertIn('Bad input, please try with a valid jobid', captured_output.getvalue())
        captured_output.close()

    def test_environment_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'env', '--start-time', '2019-07-09', '--end-time', '2019-07-09']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status":"F",
                                                                   "Result":"{\"schema\":[{\"unit\":\"string\","
                                                                            "\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                                                            "{\"unit\":\"string\",\"data\":"
                                                                            "\"timestamp\",\"heading\":\"timestamp\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"type\","
                                                                            "\"heading\":\"type\"},{\"unit\":"
                                                                            "\"string\",\"data\":\"maximumvalue\","
                                                                            "\"heading\":\"maximumvalue\"},"
                                                                            "{\"unit\":\"string\",\"data\":"
                                                                            "\"minimumvalue\",\"heading\":"
                                                                            "\"minimumvalue\"},{\"unit\":\"string\","
                                                                            "\"data\":\"averagevalue\",\"heading\":"
                                                                            "\"averagevalue\"},{\"unit\":\"string\","
                                                                            "\"data\":\"adaptertype\",\"heading\":"
                                                                            "\"adaptertype\"},{\"unit\":\"string\","
                                                                            "\"data\":\"workitemid\",\"heading\":"
                                                                            "\"workitemid\"},{\"unit\":\"string\","
                                                                            "\"data\":\"entrynumber\","
                                                                            "\"heading\":\"entrynumber\"}],"
                                                                            "\"result-data-lines\":1,"
                                                                            "\"result-status-code\":0,"
                                                                            "\"data\":[[\"SN1-M\","
                                                                            "\"2019-07-09 17:04:10.576635 \","
                                                                            "\"Coretemp \", \"41 \", \"20\", \"32\","
                                                                            "\"monitor\", \"3\", \"21345\" ]],"
                                                                            "\"result-data-columns\":9}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LCTN', captured_output.getvalue())
        captured_output.close()

    def test_environment_execute_positive_with_json(self):
        fd, temp_filepath = tempfile.mkstemp()
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'env', '--start-time', '2019-07-09', '--end-time', '2019-07-09', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status":"F",
                                                                   "Result":"{\"schema\":[{\"unit\":\"string\","
                                                                            "\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                                                            "{\"unit\":\"string\",\"data\":"
                                                                            "\"timestamp\",\"heading\":\"timestamp\"},"
                                                                            "{\"unit\":\"string\",\"data\":\"type\","
                                                                            "\"heading\":\"type\"},{\"unit\":"
                                                                            "\"string\",\"data\":\"maximumvalue\","
                                                                            "\"heading\":\"maximumvalue\"},"
                                                                            "{\"unit\":\"string\",\"data\":"
                                                                            "\"minimumvalue\",\"heading\":"
                                                                            "\"minimumvalue\"},{\"unit\":\"string\","
                                                                            "\"data\":\"averagevalue\",\"heading\":"
                                                                            "\"averagevalue\"},{\"unit\":\"string\","
                                                                            "\"data\":\"adaptertype\",\"heading\":"
                                                                            "\"adaptertype\"},{\"unit\":\"string\","
                                                                            "\"data\":\"workitemid\",\"heading\":"
                                                                            "\"workitemid\"},{\"unit\":\"string\","
                                                                            "\"data\":\"entrynumber\","
                                                                            "\"heading\":\"entrynumber\"}],"
                                                                            "\"result-data-lines\":1,"
                                                                            "\"result-status-code\":0,"
                                                                            "\"data\":[[\"SN1-M\","
                                                                            "\"2019-07-09 17:04:10.576635 \","
                                                                            "\"Coretemp \", \"41 \", \"20\", \"32\","
                                                                            "\"monitor\", \"3\", \"21345\" ]],"
                                                                            "\"result-data-columns\":9}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('lctn', captured_output.getvalue())
        captured_output.close()
        os.remove(temp_filepath)

    def test_state_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'state', 'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                          "\"heading\":\"sequencenumber\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"hostname\",\"heading\":\"hostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"sernum\",\"heading\":\"sernum\"},"
                                          "{\"unit\":\"string\",\"data\":\"bootimageid\",\"heading\":\"bootimageid\"},"
                                          "{\"unit\":\"string\",\"data\":\"environment\",\"heading\":\"environment\"},"
                                          "{\"unit\":\"string\",\"data\":\"ipaddr\",\"heading\":\"ipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"macaddr\",\"heading\":\"macaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"type\",\"heading\":\"type\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcipaddr\",\"heading\":\"bmcipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcmacaddr\",\"heading\":\"bmcmacaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmchostname\",\"heading\":\"bmchostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"dbupdatedtimestamp\","
                                          "\"heading\":\"dbupdatedtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                          "\"heading\":\"lastchgtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgadaptertype\","
                                          "\"heading\":\"lastchgadaptertype\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                          "\"heading\":\"lastchgworkitemid\"},"
                                          "{\"unit\":\"string\",\"data\":\"owner\",\"heading\":\"owner\"},"
                                          "{\"unit\":\"string\",\"data\":\"aggregator\",\"heading\":\"aggregator\"},"
                                          "{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                          "\"heading\":\"inventoryinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"wlmnodestate\",\"heading\":\"wlmnodestate\"},"
                                          "{\"unit\":\"string\",\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,"
                                          "\"data\":[[\"R48-CH00-CN0\",0,\"A\",\"c01\",null,\"centos7.3-default\",null,"
                                          "\"192.168.0.85\",\"00:1e:67:38:8f:c1\",\"dense-compute-node\","
                                          "\"192.168.10.105\",\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\","
                                          "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,\"W\","
                                          "\"SN1-M\",null,\"U\",118]],\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('STATE', captured_output.getvalue())
        captured_output.close()

    def test_state_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'state', 'c01', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                          "\"heading\":\"sequencenumber\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"hostname\",\"heading\":\"hostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"sernum\",\"heading\":\"sernum\"},"
                                          "{\"unit\":\"string\",\"data\":\"bootimageid\",\"heading\":\"bootimageid\"},"
                                          "{\"unit\":\"string\",\"data\":\"environment\",\"heading\":\"environment\"},"
                                          "{\"unit\":\"string\",\"data\":\"ipaddr\",\"heading\":\"ipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"macaddr\",\"heading\":\"macaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"type\",\"heading\":\"type\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcipaddr\",\"heading\":\"bmcipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcmacaddr\",\"heading\":\"bmcmacaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmchostname\",\"heading\":\"bmchostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"dbupdatedtimestamp\","
                                          "\"heading\":\"dbupdatedtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                          "\"heading\":\"lastchgtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgadaptertype\","
                                          "\"heading\":\"lastchgadaptertype\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                          "\"heading\":\"lastchgworkitemid\"},"
                                          "{\"unit\":\"string\",\"data\":\"owner\",\"heading\":\"owner\"},"
                                          "{\"unit\":\"string\",\"data\":\"aggregator\",\"heading\":\"aggregator\"},"
                                          "{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                          "\"heading\":\"inventoryinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"wlmnodestate\",\"heading\":\"wlmnodestate\"},"
                                          "{\"unit\":\"string\",\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,"
                                          "\"data\":[[\"R48-CH00-CN0\",0,\"A\",\"c01\",null,\"centos7.3-default\",null,"
                                          "\"192.168.0.85\",\"00:1e:67:38:8f:c1\",\"dense-compute-node\","
                                          "\"192.168.10.105\",\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\","
                                          "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,\"W\","
                                          "\"SN1-M\",null,\"U\",118]],\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('state', captured_output.getvalue())
        captured_output.close()

    def test_network_config_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'network-config', 'c01']
        with patch(
                'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                          "\"heading\":\"sequencenumber\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"hostname\",\"heading\":\"hostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"sernum\",\"heading\":\"sernum\"},"
                                          "{\"unit\":\"string\",\"data\":\"bootimageid\",\"heading\":\"bootimageid\"},"
                                          "{\"unit\":\"string\",\"data\":\"environment\",\"heading\":\"environment\"},"
                                          "{\"unit\":\"string\",\"data\":\"ipaddr\",\"heading\":\"ipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"macaddr\",\"heading\":\"macaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"type\",\"heading\":\"type\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcipaddr\",\"heading\":\"bmcipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcmacaddr\",\"heading\":\"bmcmacaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmchostname\",\"heading\":\"bmchostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"dbupdatedtimestamp\","
                                          "\"heading\":\"dbupdatedtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                          "\"heading\":\"lastchgtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgadaptertype\","
                                          "\"heading\":\"lastchgadaptertype\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                          "\"heading\":\"lastchgworkitemid\"},"
                                          "{\"unit\":\"string\",\"data\":\"owner\",\"heading\":\"owner\"},"
                                          "{\"unit\":\"string\",\"data\":\"aggregator\",\"heading\":\"aggregator\"},"
                                          "{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                          "\"heading\":\"inventoryinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"wlmnodestate\","
                                          "\"heading\":\"wlmnodestate\"}, "
                                          "{\"unit\":\"string\",\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,"
                                          "\"data\":[[\"R48-CH00-CN0\",0,\"A\",\"c01\",null,\"centos7.3-default\",null,"
                                          "\"192.168.0.85\",\"00:1e:67:38:8f:c1\",\"dense-compute-node\","
                                          "\"192.168.10.105\",\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\","
                                          "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,\"W\","
                                          "\"SN1-M\",null,\"U\",118]],\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('MACADDR', captured_output.getvalue())
        captured_output.close()

    def test_network_config_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'network-config', 'c01', '--format', 'json']
        with patch(
                'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                          "\"heading\":\"sequencenumber\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"hostname\",\"heading\":\"hostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"sernum\",\"heading\":\"sernum\"},"
                                          "{\"unit\":\"string\",\"data\":\"bootimageid\",\"heading\":\"bootimageid\"},"
                                          "{\"unit\":\"string\",\"data\":\"environment\",\"heading\":\"environment\"},"
                                          "{\"unit\":\"string\",\"data\":\"ipaddr\",\"heading\":\"ipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"macaddr\",\"heading\":\"macaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"type\",\"heading\":\"type\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcipaddr\",\"heading\":\"bmcipaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmcmacaddr\",\"heading\":\"bmcmacaddr\"},"
                                          "{\"unit\":\"string\",\"data\":\"bmchostname\",\"heading\":\"bmchostname\"},"
                                          "{\"unit\":\"string\",\"data\":\"dbupdatedtimestamp\","
                                          "\"heading\":\"dbupdatedtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                          "\"heading\":\"lastchgtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgadaptertype\","
                                          "\"heading\":\"lastchgadaptertype\"},"
                                          "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                          "\"heading\":\"lastchgworkitemid\"},"
                                          "{\"unit\":\"string\",\"data\":\"owner\",\"heading\":\"owner\"},"
                                          "{\"unit\":\"string\",\"data\":\"aggregator\",\"heading\":\"aggregator\"},"
                                          "{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                          "\"heading\":\"inventoryinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"wlmnodestate\","
                                          "\"heading\":\"wlmnodestate\"}, "
                                          "{\"unit\":\"string\",\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,"
                                          "\"data\":[[\"R48-CH00-CN0\",0,\"A\",\"c01\",null,\"centos7.3-default\",null,"
                                          "\"192.168.0.85\",\"00:1e:67:38:8f:c1\",\"dense-compute-node\","
                                          "\"192.168.10.105\",\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\","
                                          "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,\"W\","
                                          "\"SN1-M\",null,\"U\",118]],\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('macaddr', captured_output.getvalue())
        captured_output.close()

    def test_job_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'job', '--start-time', '2018-09-14', '--end-time', '2018-09-15']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result":
                                    "{\"schema\":[{\"unit\":\"string\",\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                    "{\"unit\":\"string\",\"data\":\"jobname\",\"heading\":\"jobname\"},"
                                    "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                    "{\"unit\":\"string\",\"data\":\"user\",\"heading\":\"username\"},"
                                    "{\"unit\":\"string\",\"data\":\"numnodes\",\"heading\":\"numnodes\"},"
                                    "{\"unit\":\"string\",\"data\":\"starttimestamp\",\"heading\":\"starttimestamp\"},"
                                    "{\"unit\":\"string\",\"data\":\"endtimestamp\",\"heading\":\"endtimestamp\"}],"
                                    "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"42\","
                                    "\"test\",\"A\",\"root\",\"16\","
                                    "\"2019-07-15 22:35:53.361\",\"2019-07-15 23:35:53.361\"]],"
                                    "\"result-data-columns\":7}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('JOBID', captured_output.getvalue())
        captured_output.close()

    def test_job_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'job', '--start-time', '2018-09-14', '--end-time', '2018-09-15',
                    '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result":
                                    "{\"schema\":[{\"unit\":\"string\",\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                    "{\"unit\":\"string\",\"data\":\"jobname\",\"heading\":\"jobname\"},"
                                    "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                    "{\"unit\":\"string\",\"data\":\"user\",\"heading\":\"username\"},"
                                    "{\"unit\":\"string\",\"data\":\"numnodes\",\"heading\":\"numnodes\"},"
                                    "{\"unit\":\"string\",\"data\":\"starttimestamp\",\"heading\":\"starttimestamp\"},"
                                    "{\"unit\":\"string\",\"data\":\"endtimestamp\",\"heading\":\"endtimestamp\"}],"
                                    "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"42\","
                                    "\"test\",\"A\",\"root\",\"16\","
                                    "\"2019-07-15 22:35:53.361\",\"2019-07-15 23:35:53.361\"]],"
                                    "\"result-data-columns\":7}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('jobid', captured_output.getvalue())
        captured_output.close()

    def test_job_execute_jobid(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'job', '--jobid', "42"]
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\": "
                                          "[{\"unit\":\"string\",\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                          "{\"unit\":\"string\",\"data\":\"jobname\",\"heading\":\"jobname\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"user\",\"heading\":\"username\"},"
                                          "{\"unit\":\"string\",\"data\":\"numnodes\",\"heading\":\"numnodes\"},"
                                          "{\"unit\":\"string\",\"data\":\"starttimestamp\","
                                          "\"heading\":\"starttimestamp\"}, "
                                          "{\"unit\":\"string\",\"data\":\"endtimestamp\","
                                          "\"heading\":\"endtimestamp\"}, "
                                          "{\"unit\":\"string\",\"data\":\"jobacctinfo\",\"heading\":\"jobacctinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"nodes\",\"heading\":\"nodes\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"42\","
                                          "\"test\",\"A\",\"root\",\"16\","
                                          "\"2019-07-15 22:35:53.361\",\"2019-07-15 23:35:53.361\",\"info\","
                                          "\"nodes\"]], "
                                          "\"result-data-columns\":9}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('NODES', captured_output.getvalue())
        captured_output.close()

    def test_job_execute_all(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'job', '--all']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\": "
                                          "[{\"unit\":\"string\",\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                          "{\"unit\":\"string\",\"data\":\"jobname\",\"heading\":\"jobname\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"user\",\"heading\":\"username\"},"
                                          "{\"unit\":\"string\",\"data\":\"numnodes\",\"heading\":\"numnodes\"},"
                                          "{\"unit\":\"string\",\"data\":\"starttimestamp\",\"heading\":\"starttimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"endtimestamp\",\"heading\":\"endtimestamp\"},"
                                          "{\"unit\":\"string\",\"data\":\"jobacctinfo\",\"heading\":\"jobacctinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"nodes\",\"heading\":\"nodes\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"42\","
                                          "\"test\",\"A\",\"root\",\"16\","
                                          "\"2019-07-15 22:35:53.361\",\"2019-07-15 23:35:53.361\",\"info\",\"nodes\"]],"
                                          "\"result-data-columns\":9}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('NODES', captured_output.getvalue())
        captured_output.close()

    def test_reservation_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'reservation', '--start-time', '2018-09-14', '--end-time', '2018-09-15']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result":
                                    "{\"schema\":[{\"unit\":\"string\",\"data\":\"reservationname\","
                                    "\"heading\":\"reservationname\"}, "
                                    "{\"unit\":\"string\",\"data\":\"users\",\"heading\":\"users\"},"
                                    "{\"unit\":\"string\",\"data\":\"nodes\",\"heading\":\"nodes\"},"
                                    "{\"unit\":\"string\",\"data\":\"starttimestamp\",\"heading\":\"starttimestamp\"},"
                                    "{\"unit\":\"string\",\"data\":\"endtimestamp\",\"heading\":\"endtimestamp\"},"
                                    "{\"unit\":\"string\",\"data\":\"deletedtimestamp\","
                                    "\"heading\":\"deletedtimestamp\"}, "
                                    "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                    "\"heading\":\"lastchgtimestamp\"}], "
                                    "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"testres\","
                                    "\"username\",\"node0\",\"2019-07-15 22:35:53.361\",\"2019-07-15 23:35:53.361\","
                                    "\"2019-07-15 23:40:53.361\",\"2019-07-15 23:40:53.361\"]],"
                                    "\"result-data-columns\":7}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('RESERVATIONNAME', captured_output.getvalue())
        captured_output.close()

    def test_reservation_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'reservation', '--start-time', '2018-09-14', '--end-time', '2018-09-15',
                    '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result":
                                    "{\"schema\":[{\"unit\":\"string\",\"data\":\"reservationname\","
                                    "\"heading\":\"reservationname\"}, "
                                    "{\"unit\":\"string\",\"data\":\"users\",\"heading\":\"users\"},"
                                    "{\"unit\":\"string\",\"data\":\"nodes\",\"heading\":\"nodes\"},"
                                    "{\"unit\":\"string\",\"data\":\"starttimestamp\",\"heading\":\"starttimestamp\"},"
                                    "{\"unit\":\"string\",\"data\":\"endtimestamp\",\"heading\":\"endtimestamp\"},"
                                    "{\"unit\":\"string\",\"data\":\"deletedtimestamp\","
                                    "\"heading\":\"deletedtimestamp\"}, "
                                    "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                    "\"heading\":\"lastchgtimestamp\"}], "
                                    "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"testres\","
                                    "\"username\",\"node0\",\"2019-07-15 22:35:53.361\",\"2019-07-15 23:35:53.361\","
                                    "\"2019-07-15 23:40:53.361\",\"2019-07-15 23:40:53.361\"]],"
                                    "\"result-data-columns\":7}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('reservationname', captured_output.getvalue())
        captured_output.close()

    def test_view_replacement_history_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'replacement-history', 'R8-21-CH11-CN0']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F", "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"action\","
                                                         "\"heading\":\"action\"},{\"unit\":\"string\",\"data\":\"id\","
                                                         "\"heading\":\"id\"},{\"unit\":\"string\",\"data\":\"fruid\","
                                                         "\"heading\":\"fruid\"},{\"unit\":\"string\","
                                                         "\"data\":\"dbupdatedtimestamp\","
                                                         "\"heading\":\"dbupdatedtimestamp\"},{\"unit\":\"string\","
                                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                                         "\"result-data-lines\":3,\"result-status-code\":0,\"data\":[["
                                                         "\"INSERT\",\"R8-21-CH11-CN0\",\"FRUIDforR8-21-CH11-CN0\","
                                                         "\"2020-02-06 16:12:44.0\",1],[\"DELETE\",\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN0\",\"2020-02-07 16:12:44.0\",2],"
                                                         "[\"INSERT\",\"R8-21-CH11-CN0\",\"FRUIDforR8-21-CH11-CN1\","
                                                         "\"2020-02-08 16:12:44.0\",3]],\"result-data-columns\":5}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('ID', captured_output.getvalue())
        captured_output.close()

    def test_view_replacement_history_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'replacement-history', 'R8-21-CH11-CN0', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F", "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"action\","
                                                         "\"heading\":\"action\"},{\"unit\":\"string\",\"data\":\"id\","
                                                         "\"heading\":\"id\"},{\"unit\":\"string\",\"data\":\"fruid\","
                                                         "\"heading\":\"fruid\"},{\"unit\":\"string\","
                                                         "\"data\":\"dbupdatedtimestamp\","
                                                         "\"heading\":\"dbupdatedtimestamp\"},{\"unit\":\"string\","
                                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                                         "\"result-data-lines\":3,\"result-status-code\":0,\"data\":[["
                                                         "\"INSERT\",\"R8-21-CH11-CN0\",\"FRUIDforR8-21-CH11-CN0\","
                                                         "\"2020-02-06 16:12:44.0\",1],[\"DELETE\",\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN0\",\"2020-02-07 16:12:44.0\",2],"
                                                         "[\"INSERT\",\"R8-21-CH11-CN0\",\"FRUIDforR8-21-CH11-CN1\","
                                                         "\"2020-02-08 16:12:44.0\",3]],\"result-data-columns\":5}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('id', captured_output.getvalue())
        captured_output.close()

    def test_view_inventory_change_execute_positive_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory', 'R8-21-CH11-CN0', '--history']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F", "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"id\","
                                                         "\"heading\":\"id\"},{\"unit\":\"string\",\"data\":\"fruid\","
                                                         "\"heading\":\"fruid\"}],\"result-data-lines\":3,"
                                                         "\"result-status-code\":0,\"data\":[[\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN0\"],[\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN0\"],[\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN1\"]],\"result-data-columns\":2}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('FRUID', captured_output.getvalue())
        captured_output.close()

    def test_view_inventory_change_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory', 'R8-21-CH11-CN0', '--history', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F", "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"id\","
                                                         "\"heading\":\"id\"},{\"unit\":\"string\",\"data\":\"fruid\","
                                                         "\"heading\":\"fruid\"}],\"result-data-lines\":3,"
                                                         "\"result-status-code\":0,\"data\":[[\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN0\"],[\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN0\"],[\"R8-21-CH11-CN0\","
                                                         "\"FRUIDforR8-21-CH11-CN1\"]],\"result-data-columns\":2}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('fruid', captured_output.getvalue())
        captured_output.close()

    def test_view_inventory_info_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory', 'x0c0s24b0n0']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F", "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"id\","
                                                         "\"heading\":\"id\"},{\"unit\":\"string\","
                                                         "\"data\":\"dbupdatedtimestamp\","
                                                         "\"heading\":\"dbupdatedtimestamp\"},{\"unit\":\"string\","
                                                         "\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                                         "{\"unit\":\"string\",\"data\":\"inventorytimestamp\","
                                                         "\"heading\":\"inventorytimestamp\"},{\"unit\":\"string\","
                                                         "\"data\":\"sernum\", "
                                                         "\"heading\":\"sernum\"},{\"unit\":\"string\","
                                                         "\"data\":\"inventoryinfo\",\"heading\":\"inventoryinfo\"},"
                                                         "{\"unit\":\"string\",\"data\":\"frusubtype\","
                                                         "\"heading\":\"frusubtype\"}],\"result-data-lines\":1,"
                                                         "\"result-status-code\":0,\"data\":[[\"x0c0s24b0n0d4\","
                                                         "\"2020-02-06 16:12:44.0\",0,\"FRUIDforx0c0s24b0n0d4\","
                                                         "\"Memory\",\"Memory\",\"Memory\"]],\"result-data-columns\":7}"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('INVENTORYINFO', captured_output.getvalue())
        captured_output.close()

    def test__view_inventory_info_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory', 'x0c0s24b0n0', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F", "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"id\","
                                                         "\"heading\":\"id\"},{\"unit\":\"string\","
                                                         "\"data\":\"dbupdatedtimestamp\","
                                                         "\"heading\":\"dbupdatedtimestamp\"},{\"unit\":\"string\","
                                                         "\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                                         "{\"unit\":\"string\",\"data\":\"inventorytimestamp\","
                                                         "\"heading\":\"inventorytimestamp\"},{\"unit\":\"string\","
                                                         "\"data\":\"sernum\", "
                                                         "\"heading\":\"sernum\"},{\"unit\":\"string\","
                                                         "\"data\":\"inventoryinfo\",\"heading\":\"inventoryinfo\"},"
                                                         "{\"unit\":\"string\",\"data\":\"frusubtype\","
                                                         "\"heading\":\"frusubtype\"}],\"result-data-lines\":1,"
                                                         "\"result-status-code\":0,\"data\":[[\"x0c0s24b0n0d4\","
                                                         "\"2020-02-06 16:12:44.0\",0,\"FRUIDforx0c0s24b0n0d4\","
                                                         "\"Memory\",\"Memory\",\"Memory\"]],\"result-data-columns\":7}"
                                })
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('inventoryinfo', captured_output.getvalue())
        captured_output.close()

    def test_is_bad_input(self):
        self.assertTrue(ViewCli.is_bad_input('123?'))

    def test_is_not_bad_input(self):
        self.assertFalse(ViewCli.is_bad_input('123'))
