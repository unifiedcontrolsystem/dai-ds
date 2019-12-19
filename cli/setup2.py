# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Setup Module
"""
from setuptools import setup, find_packages
import os

description = "CLI component for UCS"
author = "Intel Corporation"
license = "Apache"
etc = os.environ.get("DAIETC")

setup(name='ucs_cli',
      version=os.environ.get("DAIVER"),
      description=description,
      author=author,
      license=license,
      packages=find_packages(exclude=['cli.src.tests']),
      scripts=['ucs'],
      data_files=[(etc, ['cli_config.json']), ('/etc/bash_completion.d', ['ucs_completion.sh'])],
      install_requires=['requests',
                        'clustershell',
                        'python-dateutil',
                        'progress',
                        'texttable'],
      test_suite='tests',
      tests_require=['pytest',
                     'pytest-cov',
                     'pylint',
                     'mock'])
