# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

"""
EventSim CLI commands for Events.
"""
from .command_result import CommandResult
from .http_client import HttpClient
from datetime import datetime

class WlmCli(object):
    """EventSim CLI to handle wlm functions."""

    def __init__(self, _wlm_parser):
        self._wlm_parser = _wlm_parser
        wlm_subparsers = _wlm_parser.add_subparsers(help='Subparser for wlm')
        self._wlm_parser.set_defaults(func=self._wlm_help_execute)
        self._create_reservation_wlm_parser(wlm_subparsers)
        self._modify_reservation_wlm_parser(wlm_subparsers)
        self._delete_reservation_wlm_parser(wlm_subparsers)
        self._start_job_wlm_parser(wlm_subparsers)
        self._terminate_job_wlm_parser(wlm_subparsers)
        self._simulate_wlm_parser(wlm_subparsers)

    def _wlm_help_execute(self, args):
        self._wlm_parser.print_help()

    def _create_reservation_wlm_parser(self, wlm_parser):
        create_reservation_parser = wlm_parser.add_parser('create_reservation', help='generate a log event for a created reservation.')
        create_reservation_parser.add_argument('name', help='Name of the reservation to generate.')
        create_reservation_parser.add_argument('--users', help='Users that own reservation.', default="root")
        create_reservation_parser.add_argument('--nodes', help='Nodes in the reservation.', default="random")
        now = datetime.now()
        create_reservation_parser.add_argument('--start-time', help='Start time of the reservation.', dest="start_time", default=now.strftime("%Y-%m-%d %H:%M:%S"))
        create_reservation_parser.add_argument('--duration', help='Duration of the reservation in microseconds.', default=86400000, type=int)
        create_reservation_parser.set_defaults(func=self._create_reservation_execute)

    def _modify_reservation_wlm_parser(self, wlm_parser):
        modify_reservation_parser = wlm_parser.add_parser('modify_reservation', help='generate a log event for a modified reservation.')
        modify_reservation_parser.add_argument('name', help='Name of the reservation to modify.')
        modify_reservation_parser.add_argument('--users', help='New users that own reservation.', default="false")
        modify_reservation_parser.add_argument('--nodes', help='New nodes in the reservation.', default="false")
        modify_reservation_parser.add_argument('--start-time', help='New start time for the reservation.', dest="start_time", default="false")
        modify_reservation_parser.set_defaults(func=self._modify_reservation_execute)

    def _delete_reservation_wlm_parser(self, wlm_parser):
        delete_reservation_parser = wlm_parser.add_parser('delete_reservation', help='generate a log event for a deleted reservation.')
        delete_reservation_parser.add_argument('name', help='Name of the reservation to delete.')
        delete_reservation_parser.set_defaults(func=self._delete_reservation_execute)

    def _start_job_wlm_parser(self, wlm_parser):
        start_job_parser = wlm_parser.add_parser('start_job', help='generate a log event for a started job.')
        start_job_parser.add_argument('jobid', help='job ID of the job to generate.')
        start_job_parser.add_argument('--name', help='Name of the started job.', default="testjob")
        start_job_parser.add_argument('--users', help='Users that started job.', default="root")
        start_job_parser.add_argument('--nodes', help='Nodes for the job.', default="random")
        now = datetime.now()
        start_job_parser.add_argument('--start-time', help='Start time of the job.', dest="start_time", default=now.strftime("%Y-%m-%d %H:%M:%S"))
        start_job_parser.add_argument('--workdir', help='Work directory of the job.', default="/home")
        start_job_parser.set_defaults(func=self._start_job_execute)

    def _terminate_job_wlm_parser(self, wlm_parser):
        terminate_job_parser = wlm_parser.add_parser('terminate_job', help='generate a log event for a terminated job.')
        terminate_job_parser.add_argument('jobid', help='job ID of the job to terminate.')
        terminate_job_parser.add_argument('--name', help='Name of the started job.', default="testjob")
        terminate_job_parser.add_argument('--users', help='Users that started job.', default="root")
        terminate_job_parser.add_argument('--nodes', help='Nodes for the job.', default="random")
        now = datetime.now()
        terminate_job_parser.add_argument('--start-time', help='Start time of the job.', dest="start_time", default=now.strftime("%Y-%m-%d %H:%M:%S"))
        terminate_job_parser.add_argument('--workdir', help='Work directory of the job.', default="/home")
        terminate_job_parser.add_argument('--exit-status', help='Work directory of the job.', dest="exit_status", default=0, type=int)
        terminate_job_parser.set_defaults(func=self._terminate_job_execute)

    def _simulate_wlm_parser(self, wlm_parser):
        simulate_parser = wlm_parser.add_parser('simulate', help='generate random log events for jobs and reservations.')
        simulate_parser.add_argument('reservations', help='Number of reservations to simulate', default=10, type=int)
        simulate_parser.set_defaults(func=self._simulate_execute)

    def _create_reservation_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/wlm/createRes?
        url = client.get_base_url() + 'wlm/createRes'
        parameters = {'name': args.name, 'users': args.users, 'nodes': args.nodes,
                      'starttime': args.start_time, 'duration': args.duration}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _modify_reservation_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/wlm/modifyRes?
        url = client.get_base_url() + 'wlm/modifyRes'
        parameters = {'name': args.name, 'users': args.users, 'nodes': args.nodes,
                      'starttime': args.start_time}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _delete_reservation_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/wlm/deleteRes?
        url = client.get_base_url() + 'wlm/deleteRes'
        parameters = {'name': args.name }
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _start_job_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/wlm/startJob?
        url = client.get_base_url() + 'wlm/startJob'
        parameters = {'jobid': args.jobid, 'name': args.name, 'users': args.users,
                      'nodes': args.nodes, 'starttime': args.start_time, "workdir": args.workdir}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _terminate_job_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/wlm/terminateJob?
        url = client.get_base_url() + 'wlm/terminateJob'
        parameters = {'jobid': args.jobid, 'name': args.name, 'users': args.users, 'exitstatus': args.exit_status,
                      'nodes': args.nodes, 'starttime': args.start_time, "workdir": args.workdir}
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)

    def _simulate_execute(self, args):
        client = HttpClient()
        # URL will be GET http://127.0.0.1:9998/wlm/simulate?
        url = client.get_base_url() + 'wlm/simulate'
        parameters = {'reservations': args.reservations }
        response_code, response = client.send_post_request(url, parameters, 900)
        return CommandResult(response_code, response)