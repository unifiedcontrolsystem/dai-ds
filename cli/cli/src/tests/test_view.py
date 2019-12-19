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
        self.assertIn('{env,event,inventory-history,inventory-info,network-config,replacement-history,'
                      'snapshot-getref,snapshot-info,state,system-info}', captured_output.getvalue())
        captured_output.close()

    def test_view_help_1(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', '-h']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{env,event,inventory-history,inventory-info,network-config,replacement-history,'
                      'snapshot-getref,snapshot-info,state,system-info}', captured_output.getvalue())
        captured_output.close()

    def test_view_help_2(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', '--help']
        with self.assertRaises(SystemExit):
            parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('{env,event,inventory-history,inventory-info,network-config,replacement-history,'
                      'snapshot-getref,snapshot-info,state,system-info}', captured_output.getvalue())
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
        captured_output.close()

    def test_events_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'event', '--start_time', '2019-07-09', '--end_time', '2019-07-09']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = json.dumps({"Status":"F",
                                                                  "Result":"{\"schema\":[{\"unit\":\"string\","
                                                                           "\"data\":\"eventtype\","
                                                                           "\"heading\":\"eventtype\"},"
                                                                           "{\"unit\":\"string\","
                                                                           "\"data\":\"lastchgtimestamp\","
                                                                           "\"heading\":\"lastchgtimestamp\"},"
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
                                                                           "{\"unit\":\"string\",\"data\":\"msg\","
                                                                           "\"heading\":\"msg\"},{\"unit\":\"string\","
                                                                           "\"data\":\"instancedata\","
                                                                           "\"heading\":\"instancedata\"}],"
                                                                           "\"result-data-lines\":1,"
                                                                           "\"result-status-code\":0,"
                                                                           "\"data\":[[\"0011000007\","
                                                                           "\"2019-07-09 17:04:10.576635\","
                                                                           "\"2019-07-09 17:04:11.196\",\"ERROR\","
                                                                           "\"SN1-M\",null,\"ErrorOnNode\","
                                                                           "\"ucs-stop-dai-mgr is being run to "
                                                                           "shutdown DaiMgr:\","
                                                                           "\"InvokedBy=ucs-stop-dai-mgr\"]],"
                                                                           "\"result-data-columns\":9}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('CONTROLOPERATION', captured_output.getvalue())
        captured_output.close()

        def test_events_execute_positive_json(self):
            captured_output = io.StringIO()
            sys.stdout = captured_output
            parser = Parser()
            sys.argv = ['ucs', 'view', 'event', '--start_time', '2019-07-09', '--end_time', '2019-07-09',
                        '--format', 'json']
            with patch(
                    'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
                patched_construct.return_value = "http://localhost/4567:"
                with patch('requests.get') as patched_get:
                    type(patched_get.return_value).text = json.dumps({"Status": "F",
                                                                      "Result": "{\"schema\":[{\"unit\":\"string\","
                                                                                "\"data\":\"eventtype\","
                                                                                "\"heading\":\"eventtype\"},"
                                                                                "{\"unit\":\"string\","
                                                                                "\"data\":\"lastchgtimestamp\","
                                                                                "\"heading\":\"lastchgtimestamp\"},"
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
                                                                                "{\"unit\":\"string\",\"data\":\"msg\","
                                                                                "\"heading\":\"msg\"},{\"unit\":\"string\","
                                                                                "\"data\":\"instancedata\","
                                                                                "\"heading\":\"instancedata\"}],"
                                                                                "\"result-data-lines\":1,"
                                                                                "\"result-status-code\":0,"
                                                                                "\"data\":[[\"0011000007\","
                                                                                "\"2019-07-09 17:04:10.576635\","
                                                                                "\"2019-07-09 17:04:11.196\",\"ERROR\","
                                                                                "\"SN1-M\",null,\"ErrorOnNode\","
                                                                                "\"ucs-stop-dai-mgr is being run to "
                                                                                "shutdown DaiMgr:\","
                                                                                "\"InvokedBy=ucs-stop-dai-mgr\"]],"
                                                                                "\"result-data-columns\":9}"}
                                                                     )
                    parser.execute_cli_cmd()
            sys.stdout = sys.__stdout__
            self.assertIn('controloperation', captured_output.getvalue())
            captured_output.close()

    def test_environment_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'env', '--start_time', '2019-07-09', '--end_time', '2019-07-09']
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
        sys.argv = ['ucs', 'view', 'env', '--start_time', '2019-07-09', '--end_time', '2019-07-09', '--format', 'json']
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

    def test_inventory_info_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory-info', 'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                         "\"heading\":\"sequencenumber\"},{\"unit\":\"string\",\"data\":\"state\","
                                         "\"heading\":\"state\"},{\"unit\":\"string\",\"data\":\"hostname\","
                                         "\"heading\":\"hostname\"},{\"unit\":\"string\",\"data\":\"sernum\","
                                         "\"heading\":\"sernum\"},{\"unit\":\"string\",\"data\":\"bootimageid\","
                                         "\"heading\":\"bootimageid\"},{\"unit\":\"string\",\"data\":\"environment\","
                                         "\"heading\":\"environment\"},{\"unit\":\"string\",\"data\":\"ipaddr\","
                                         "\"heading\":\"ipaddr\"},{\"unit\":\"string\",\"data\":\"macaddr\","
                                         "\"heading\":\"macaddr\"},{\"unit\":\"string\",\"data\":\"type\","
                                         "\"heading\":\"type\"},{\"unit\":\"string\",\"data\":\"bmcipaddr\","
                                         "\"heading\":\"bmcipaddr\"},{\"unit\":\"string\",\"data\":\"bmcmacaddr\","
                                         "\"heading\":\"bmcmacaddr\"},{\"unit\":\"string\",\"data\":\"bmchostname\","
                                         "\"heading\":\"bmchostname\"},{\"unit\":\"string\","
                                         "\"data\":\"dbupdatedtimestamp\",\"heading\":\"dbupdatedtimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                         "\"heading\":\"lastchgtimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"lastchgadaptertype\",\"heading\":\"lastchgadaptertype\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                         "\"heading\":\"lastchgworkitemid\"},{\"unit\":\"string\",\"data\":\"owner\","
                                         "\"heading\":\"owner\"},{\"unit\":\"string\",\"data\":\"aggregator\","
                                         "\"heading\":\"aggregator\"},{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                         "\"heading\":\"inventoryinfo\"},{\"unit\":\"string\","
                                         "\"data\":\"wlmnodestate\",\"heading\":\"wlmnodestate\"},{\"unit\":\"string\","
                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[\"R48-CH00-CN0\",0,\"A\",\"c01\",null,\"centos7.3-default\",null,"
                                         "\"192.168.0.85\",\"00:1e:67:38:8f:c1\",\"dense-compute-node\","
                                         "\"192.168.10.105\",\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\","
                                         "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,"
                                         "\"W\",\"SN1-M\",null,\"U\",118]],\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LCTN', captured_output.getvalue())
        captured_output.close()

    def test_inventory_info_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory-info', 'c01', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                         "\"heading\":\"sequencenumber\"},{\"unit\":\"string\",\"data\":\"state\","
                                         "\"heading\":\"state\"},{\"unit\":\"string\",\"data\":\"hostname\","
                                         "\"heading\":\"hostname\"},{\"unit\":\"string\",\"data\":\"sernum\","
                                         "\"heading\":\"sernum\"},{\"unit\":\"string\",\"data\":\"bootimageid\","
                                         "\"heading\":\"bootimageid\"},{\"unit\":\"string\",\"data\":\"environment\","
                                         "\"heading\":\"environment\"},{\"unit\":\"string\",\"data\":\"ipaddr\","
                                         "\"heading\":\"ipaddr\"},{\"unit\":\"string\",\"data\":\"macaddr\","
                                         "\"heading\":\"macaddr\"},{\"unit\":\"string\",\"data\":\"type\","
                                         "\"heading\":\"type\"},{\"unit\":\"string\",\"data\":\"bmcipaddr\","
                                         "\"heading\":\"bmcipaddr\"},{\"unit\":\"string\",\"data\":\"bmcmacaddr\","
                                         "\"heading\":\"bmcmacaddr\"},{\"unit\":\"string\",\"data\":\"bmchostname\","
                                         "\"heading\":\"bmchostname\"},{\"unit\":\"string\","
                                         "\"data\":\"dbupdatedtimestamp\",\"heading\":\"dbupdatedtimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                         "\"heading\":\"lastchgtimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"lastchgadaptertype\",\"heading\":\"lastchgadaptertype\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                         "\"heading\":\"lastchgworkitemid\"},{\"unit\":\"string\",\"data\":\"owner\","
                                         "\"heading\":\"owner\"},{\"unit\":\"string\",\"data\":\"aggregator\","
                                         "\"heading\":\"aggregator\"},{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                         "\"heading\":\"inventoryinfo\"},{\"unit\":\"string\","
                                         "\"data\":\"wlmnodestate\",\"heading\":\"wlmnodestate\"},{\"unit\":\"string\","
                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,"
                                         "\"data\":[[\"R48-CH00-CN0\",0,\"A\",\"c01\",null,\"centos7.3-default\",null,"
                                         "\"192.168.0.85\",\"00:1e:67:38:8f:c1\",\"dense-compute-node\","
                                         "\"192.168.10.105\",\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\","
                                         "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,"
                                         "\"W\",\"SN1-M\",null,\"U\",118]],\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('lctn', captured_output.getvalue())
        captured_output.close()

    def test_snapshot_info_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'snapshot-info', 'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"snapshottimestamp\","
                                         "\"heading\":\"snapshottimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"inventoryinfo\",\"heading\":\"inventoryinfo\"},"
                                         "{\"unit\":\"string\",\"data\":\"id\",\"heading\":\"id\"},"
                                         "{\"unit\":\"string\",\"data\":\"reference\",\"heading\":\"reference\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"R48-CH00-CN0\","
                                         "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\", null, \"12\", "
                                         "\"test_reference\"]],"
                                         "\"result-data-columns\":5}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LCTN', captured_output.getvalue())
        captured_output.close()

    def test_snapshot_info_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'snapshot-info', 'c01', '--format', 'json']
        with patch(
                'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"snapshottimestamp\","
                                          "\"heading\":\"snapshottimestamp\"},{\"unit\":\"string\","
                                          "\"data\":\"inventoryinfo\",\"heading\":\"inventoryinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"id\",\"heading\":\"id\"},"
                                          "{\"unit\":\"string\",\"data\":\"reference\",\"heading\":\"reference\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"R48-CH00-CN0\","
                                          "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\", null, \"12\", "
                                          "\"test_reference\"]],"
                                          "\"result-data-columns\":5}"}
                               )
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('lctn', captured_output.getvalue())
        captured_output.close()

    def test_get_ref_snapshot_info_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'snapshot-getref', 'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"snapshottimestamp\","
                                         "\"heading\":\"snapshottimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"inventoryinfo\",\"heading\":\"inventoryinfo\"},"
                                         "{\"unit\":\"string\",\"data\":\"id\",\"heading\":\"id\"},"
                                         "{\"unit\":\"string\",\"data\":\"reference\",\"heading\":\"reference\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[\"R48-CH00-CN0\","
                                         "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\", null, 1, null],"
                                         "\"result-data-columns\":5}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LCTN', captured_output.getvalue())
        captured_output.close()

    def test_get_ref_snapshot_info_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'snapshot-getref', 'c01', '--format', 'json']
        with patch(
                'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"snapshottimestamp\","
                                          "\"heading\":\"snapshottimestamp\"},{\"unit\":\"string\","
                                          "\"data\":\"inventoryinfo\",\"heading\":\"inventoryinfo\"},"
                                          "{\"unit\":\"string\",\"data\":\"id\",\"heading\":\"id\"},"
                                          "{\"unit\":\"string\",\"data\":\"reference\",\"heading\":\"reference\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[\"R48-CH00-CN0\","
                                          "\"2019-07-15 23:35:53.361\",\"2019-07-15 23:35:53.361\", null, 1, null],"
                                          "\"result-data-columns\":5}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('lctn', captured_output.getvalue())
        captured_output.close()

    def test_inventory_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory-history', '--start_time', '2018-09-14', '--end_time', '2018-09-15',
                    'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                         "\"heading\":\"sequencenumber\"},{\"unit\":\"string\",\"data\":\"state\","
                                         "\"heading\":\"state\"},{\"unit\":\"string\",\"data\":\"hostname\","
                                         "\"heading\":\"hostname\"},{\"unit\":\"string\",\"data\":\"sernum\","
                                         "\"heading\":\"sernum\"},{\"unit\":\"string\",\"data\":\"bootimageid\","
                                         "\"heading\":\"bootimageid\"},{\"unit\":\"string\",\"data\":\"environment\","
                                         "\"heading\":\"environment\"},{\"unit\":\"string\",\"data\":\"ipaddr\","
                                         "\"heading\":\"ipaddr\"},{\"unit\":\"string\",\"data\":\"macaddr\","
                                         "\"heading\":\"macaddr\"},{\"unit\":\"string\",\"data\":\"type\","
                                         "\"heading\":\"type\"},{\"unit\":\"string\",\"data\":\"bmcipaddr\","
                                         "\"heading\":\"bmcipaddr\"},{\"unit\":\"string\",\"data\":\"bmcmacaddr\","
                                         "\"heading\":\"bmcmacaddr\"},{\"unit\":\"string\",\"data\":\"bmchostname\","
                                         "\"heading\":\"bmchostname\"},{\"unit\":\"string\","
                                         "\"data\":\"dbupdatedtimestamp\",\"heading\":\"dbupdatedtimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                         "\"heading\":\"lastchgtimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"lastchgadaptertype\",\"heading\":\"lastchgadaptertype\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                         "\"heading\":\"lastchgworkitemid\"},{\"unit\":\"string\",\"data\":\"owner\","
                                         "\"heading\":\"owner\"},{\"unit\":\"string\",\"data\":\"aggregator\","
                                         "\"heading\":\"aggregator\"},{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                         "\"heading\":\"inventoryinfo\"},{\"unit\":\"string\","
                                         "\"data\":\"wlmnodestate\",\"heading\":\"wlmnodestate\"},{\"unit\":\"string\","
                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"R48-CH00-CN0\","
                                         "0,\"A\",\"c01\",null,\"centos7.3-default\",null,\"192.168.0.85\","
                                         "\"00:1e:67:38:8f:c1\",\"dense-compute-node\",\"192.168.10.105\","
                                         "\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\",\"2019-07-15 23:35:53.361\","
                                         "\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,\"W\",\"SN1-M\",null,\"U\",118]],"
                                         "\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('LCTN', captured_output.getvalue())
        captured_output.close()

    def test_inventory_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'inventory-history', '--start_time', '2018-09-14', '--end_time', '2018-09-15',
                    'c01', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"sequencenumber\","
                                         "\"heading\":\"sequencenumber\"},{\"unit\":\"string\",\"data\":\"state\","
                                         "\"heading\":\"state\"},{\"unit\":\"string\",\"data\":\"hostname\","
                                         "\"heading\":\"hostname\"},{\"unit\":\"string\",\"data\":\"sernum\","
                                         "\"heading\":\"sernum\"},{\"unit\":\"string\",\"data\":\"bootimageid\","
                                         "\"heading\":\"bootimageid\"},{\"unit\":\"string\",\"data\":\"environment\","
                                         "\"heading\":\"environment\"},{\"unit\":\"string\",\"data\":\"ipaddr\","
                                         "\"heading\":\"ipaddr\"},{\"unit\":\"string\",\"data\":\"macaddr\","
                                         "\"heading\":\"macaddr\"},{\"unit\":\"string\",\"data\":\"type\","
                                         "\"heading\":\"type\"},{\"unit\":\"string\",\"data\":\"bmcipaddr\","
                                         "\"heading\":\"bmcipaddr\"},{\"unit\":\"string\",\"data\":\"bmcmacaddr\","
                                         "\"heading\":\"bmcmacaddr\"},{\"unit\":\"string\",\"data\":\"bmchostname\","
                                         "\"heading\":\"bmchostname\"},{\"unit\":\"string\","
                                         "\"data\":\"dbupdatedtimestamp\",\"heading\":\"dbupdatedtimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                         "\"heading\":\"lastchgtimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"lastchgadaptertype\",\"heading\":\"lastchgadaptertype\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgworkitemid\","
                                         "\"heading\":\"lastchgworkitemid\"},{\"unit\":\"string\",\"data\":\"owner\","
                                         "\"heading\":\"owner\"},{\"unit\":\"string\",\"data\":\"aggregator\","
                                         "\"heading\":\"aggregator\"},{\"unit\":\"string\",\"data\":\"inventoryinfo\","
                                         "\"heading\":\"inventoryinfo\"},{\"unit\":\"string\","
                                         "\"data\":\"wlmnodestate\",\"heading\":\"wlmnodestate\"},{\"unit\":\"string\","
                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"R48-CH00-CN0\","
                                         "0,\"A\",\"c01\",null,\"centos7.3-default\",null,\"192.168.0.85\","
                                         "\"00:1e:67:38:8f:c1\",\"dense-compute-node\",\"192.168.10.105\","
                                         "\"53:54:00:47:a4:00\",\"R48-CH00-CN0-BMC\",\"2019-07-15 23:35:53.361\","
                                         "\"2019-07-15 23:35:53.361\",\"DAI_MGR\",2,\"W\",\"SN1-M\",null,\"U\",118]],"
                                         "\"result-data-columns\":22}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('lctn', captured_output.getvalue())
        captured_output.close()

    def test_replacement_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'replacement-history', '--start_time', '2018-09-14', '--end_time', '2018-09-15',
                    'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"frutype\",\"heading\":\"frutype\"},"
                                         "{\"unit\":\"string\",\"data\":\"serviceoperationid\","
                                         "\"heading\":\"serviceoperationid\"},{\"unit\":\"string\","
                                         "\"data\":\"oldsernum\",\"heading\":\"oldsernum\"},{\"unit\":\"string\","
                                         "\"data\":\"newsernum\",\"heading\":\"newsernum\"},{\"unit\":\"string\","
                                         "\"data\":\"oldstate\",\"heading\":\"oldstate\"},{\"unit\":\"string\","
                                         "\"data\":\"newstate\",\"heading\":\"newstate\"},{\"unit\":\"string\","
                                         "\"data\":\"dbupdatedtimestamp\",\"heading\":\"dbupdatedtimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                         "\"heading\":\"lastchgtimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"R48-CH00-CN0\","
                                         "\"compute\", \"0\", null, null, \"A\", \"A\", \"2019-07-15 23:35:53.361\","
                                         "\"2019-07-15 23:35:53.361\", \"21321\"]],"
                                         "\"result-data-columns\":10}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('NEWSERNUM', captured_output.getvalue())
        captured_output.close()

    def test_replacement_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'replacement-history', '--start_time', '2018-09-14', '--end_time', '2018-09-15',
                    'c01', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"frutype\",\"heading\":\"frutype\"},"
                                         "{\"unit\":\"string\",\"data\":\"serviceoperationid\","
                                         "\"heading\":\"serviceoperationid\"},{\"unit\":\"string\","
                                         "\"data\":\"oldsernum\",\"heading\":\"oldsernum\"},{\"unit\":\"string\","
                                         "\"data\":\"newsernum\",\"heading\":\"newsernum\"},{\"unit\":\"string\","
                                         "\"data\":\"oldstate\",\"heading\":\"oldstate\"},{\"unit\":\"string\","
                                         "\"data\":\"newstate\",\"heading\":\"newstate\"},{\"unit\":\"string\","
                                         "\"data\":\"dbupdatedtimestamp\",\"heading\":\"dbupdatedtimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"lastchgtimestamp\","
                                         "\"heading\":\"lastchgtimestamp\"},{\"unit\":\"string\","
                                         "\"data\":\"entrynumber\",\"heading\":\"entrynumber\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"R48-CH00-CN0\","
                                         "\"compute\", \"0\", null, null, \"A\", \"A\", \"2019-07-15 23:35:53.361\","
                                         "\"2019-07-15 23:35:53.361\", \"21321\"]],"
                                         "\"result-data-columns\":10}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('newsernum', captured_output.getvalue())
        captured_output.close()

    def test_jobpower_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'jobpower', '--start_time', '2019-07-15', '--end_time',
                    '2019-07-16', '--location', 'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                         "{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"jobpowertimestamp\","
                                         "\"heading\":\"jobpowertimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"totalruntime\",\"heading\":\"totalruntime\"},"
                                         "{\"unit\":\"string\",\"data\":\"totalpackageenergy\","
                                         "\"heading\":\"totalpackageenergy\"},"
                                         "{\"unit\":\"string\",\"data\":\"totaldramenergy\","
                                         "\"heading\":\"totaldramenergy\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"21\", "
                                         "\"R48-CH00-CN0\", \"2019-07-15 23:35:53.361\", \"221.82\", \"20.18\","
                                         "\"19.8\" ]],"
                                         "\"result-data-columns\":6}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('JOBID', captured_output.getvalue())
        captured_output.close()

    def test_jobpower_execute_positive_json(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'jobpower', '--start_time', '2019-07-15', '--end_time',
                    '2019-07-16', '--location', 'c01', '--format', 'json']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"jobid\",\"heading\":\"jobid\"},"
                                         "{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"jobpowertimestamp\","
                                         "\"heading\":\"jobpowertimestamp\"},"
                                         "{\"unit\":\"string\",\"data\":\"totalruntime\",\"heading\":\"totalruntime\"},"
                                         "{\"unit\":\"string\",\"data\":\"totalpackageenergy\","
                                         "\"heading\":\"totalpackageenergy\"},"
                                         "{\"unit\":\"string\",\"data\":\"totaldramenergy\","
                                         "\"heading\":\"totaldramenergy\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"21\", "
                                         "\"R48-CH00-CN0\", \"2019-07-15 23:35:53.361\", \"221.82\", \"20.18\","
                                         "\"19.8\" ]],"
                                         "\"result-data-columns\":6}"})
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('jobid', captured_output.getvalue())
        captured_output.close()

    def test_diag_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'diag', '--start_time', '2019-07-15', '--end_time', '2019-07-16']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"diagid\",\"heading\":\"diagid\"},"
                                         "{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                         "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                         "{\"unit\":\"string\",\"data\":\"results\",\"heading\":\"results\"},"
                                         "{\"unit\":\"string\",\"data\":\"dbupdatedtimestamp\","
                                         "\"heading\":\"dbupdatedtimestamp\"}],"
                                         "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"1\", "
                                         "\"R48-CH0-N0\", \"P\", \"/var/hit/data/offline_diagnostics/Dudley2/"
                                         "hpcdiag2019-07-15-13-39/R48-CH0-N0-hpcdiag2019-07-15-13-39.log\", "
                                         "\"2019-07-15 23:35:53.361\"]],"
                                         "\"result-data-columns\":5}"}
                               )
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('DIAGID', captured_output.getvalue())
        captured_output.close()

    def test_diag_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'diag', '--start_time', '2019-07-15', '--end_time', '2019-07-16', '--format', 'json']
        with patch(
                'cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status": "F",
                                "Result": "{\"schema\":[{\"unit\":\"string\",\"data\":\"diagid\",\"heading\":\"diagid\"},"
                                          "{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
                                          "{\"unit\":\"string\",\"data\":\"state\",\"heading\":\"state\"},"
                                          "{\"unit\":\"string\",\"data\":\"results\",\"heading\":\"results\"},"
                                          "{\"unit\":\"string\",\"data\":\"dbupdatedtimestamp\","
                                          "\"heading\":\"dbupdatedtimestamp\"}],"
                                          "\"result-data-lines\":1,\"result-status-code\":0,\"data\":[[\"1\", "
                                          "\"R48-CH0-N0\", \"P\", \"/var/hit/data/offline_diagnostics/Dudley2/"
                                          "hpcdiag2019-07-15-13-39/R48-CH0-N0-hpcdiag2019-07-15-13-39.log\", "
                                          "\"2019-07-15 23:35:53.361\"]],"
                                          "\"result-data-columns\":5}"}
                               )
                parser.execute_cli_cmd()
        sys.stdout = sys.__stdout__
        self.assertIn('diagid', captured_output.getvalue())
        captured_output.close()

    def test_state_execute_positive(self):
        captured_output = io.StringIO()
        sys.stdout = captured_output
        parser = Parser()
        sys.argv = ['ucs', 'view', 'state', 'c01']
        with patch('cli.src.http_client.HttpClient._construct_base_url_from_configuration_file') as patched_construct:
            patched_construct.return_value = "http://localhost/4567:"
            with patch('requests.get') as patched_get:
                type(patched_get.return_value).text = \
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
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
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
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
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
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
                    json.dumps({"Status":"F",
                                "Result":"{\"schema\":[{\"unit\":\"string\",\"data\":\"lctn\",\"heading\":\"lctn\"},"
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
        self.assertIn('macaddr', captured_output.getvalue())
        captured_output.close()

    def test_is_bad_input(self):
        self.assertTrue(ViewCli.is_bad_input('123?'))

    def test_is_not_bad_input(self):
        self.assertFalse(ViewCli.is_bad_input('123'))
