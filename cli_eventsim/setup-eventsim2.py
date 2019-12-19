# -*- coding: utf-8 -*-
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

"""
Setup Module
"""
from setuptools import setup, find_packages
import os

description = "simulator for a foreign API"
author = "Intel Corporation"
license = "Apache"
etc = os.environ.get("DAIETC")

setup(name='eventsim_cli',
      version=os.environ.get("DAIVER"),
      description=description,
      author=author,
      license=license,
      packages=find_packages(),
      scripts=['eventsim'],
      data_files=[(etc, ['eventsim_cli_config.json'])],
      install_requires=['requests',
                        'clustershell',
                        'timeout-decorator',
                        'progress'],
      test_suite='tests',
      tests_require=['pytest',
                     'pytest-cov',
                     'pylint',
                     'mock'])
