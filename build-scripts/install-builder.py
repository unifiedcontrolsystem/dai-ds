#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

"""
Install Builder's application entry point

"""
import base64
import sys
import os
import yaml
import datetime
import subprocess

TEMPLATE = """#!/bin/bash
# Copyright (C) {{YEAR}} Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

PROG=$(basename $0)
VERSION=1.0

REQUIRED_UID=0
DEF_ROOT="/"
ROOT="${DEF_ROOT}"
TMP_PACKAGE_FILE="/tmp/package.tar"
TAR_OPTIONS=vh  # 'v' is optional...
UNINSTALL=0
DRY=
PIP="python3 -m pip"

function error_exit() {
  echo "*** Error: ${1}" >&2
  exit 1
}

function good_exit() {
  exit 0
}

function usage_exit() {
  [[ $1 -eq 0 ]] && echo "'${PROG}' version ${VERSION}"
  echo "Usage: ${PROG} [ -h | --help | -U | --uninstall ] [ --chroot <new_root> ] [ --dry-run ]"
  exit $1
}

function error_exit_with_usage() {
  echo "*** Error: $1" >&2
  usage_exit 1
}

function check_prerequisites() {
  [[ $(id -u) -ne ${REQUIRED_UID} ]] && error_exit "The installer must be run as root user, try using sudo!"
  [[ -z "$(which base64)" ]] && error_exit "The 'base64' tool is missing from your system and is required!"
  [[ -z "$(which tar)" ]] && error_exit "The 'tar' tool is missing from your system and is required!"
}

function parse_for_help() {
  [[ "${1}" == "-h" || "${1}" == "--help" ]] && usage_exit 0
}

function parse_options() {
  while [ "${1}" != "" ]; do
    [[ "${1}" == "--chroot" ]] && shift && ROOT="${1}" && shift && continue
    [[ -z "${ROOT}" ]] && ROOT="${DEF_ROOT}" && continue
    [[ "${1}" == "--dry-run" ]] && shift && DRY="echo" && continue
    [[ "${1}" == "-U" || "${1}" == "--uninstall" ]] && shift && UNINSTALL=1 && continue
    error_exit_with_usage "unknown argument: ${1}"
    usage_exit 1
  done
}

function extract_tarball() {
  ${DRY} base64 -d <<PACKAGE_EOF >${TMP_PACKAGE_FILE}
{{BASE64_TARBALL}}
PACKAGE_EOF
  return $?
}

function delete_tarball() {
  ${DRY} rm -f ${TMP_PACKAGE_FILE}
  return $?
}

function install_tarball() {
  [[ -f "${TMP_PACKAGE_FILE}" ]] || extract_tarball
  [[ $? -eq 0 ]] && ${DRY} tar -C ${ROOT} --no-same-owner -x${TAR_OPTIONS}f ${TMP_PACKAGE_FILE}
  local rv=$?
  delete_tarball
  return $rv
}

function uninstall_tarball() {
  [[ -f "${TMP_PACKAGE_FILE}" ]] || extract_tarball
  for file in $(tar -tf ${TMP_PACKAGE_FILE} | grep -v "/\$"); do
    ${DRY} rm -f "${ROOT%*/}/${file}"
  done
  for folder in $(tar -tf ${TMP_PACKAGE_FILE} | grep "/\$"); do
    ${DRY} rmdir "${ROOT%*/}/${folder}" 2>/dev/null
  done
  delete_tarball
  return 0
}

function install_wheel() {
  ${DRY} ${PIP} install --root ${ROOT} -U ${ROOT%*/}/${1#*/}
  return $?
}

function uninstall_wheel() {
  [[ "${ROOT}" == "${DEF_ROOT}" ]] && ${DRY} ${PIP} uninstall --yes ${1}
  return $?
}

function add_group() {
  ${DRY} groupadd -R ${ROOT} ${1}
  return $?
}

function remove_group() {
  ${DRY} groupdel -R ${ROOT} ${1}
  return $?
}

function add_non_interactive_user() {
  local opts="-R ${ROOT} -M"
  [[ -z "${2}" ]] && opts="${opts} -U"
  [[ -n "${2}" ]] && add_group ${2} && opts="${opts} -g ${2}"
  ${DRY} useradd ${opts} ${1}
  return $?
}

function add_interactive_user() {
  local opts="-R ${ROOT} -m"
  [[ -z "${2}" ]] && opts="${opts} -U"
  [[ -n "${2}" ]] && add_group ${2} && opts="${opts} -g ${2}"
  ${DRY} useradd ${opts} ${1}
  return $?
}

function remove_user() {
  [[ -z "${2}" ]] && remove_group ${1}
  [[ -n "${2}" ]] && remove_group ${2}
  ${DRY} userdel -R ${ROOT} ${1}
  return $?
}

function enable_service() {
  ${DRY} systemctl --root=${ROOT} daemon-reload
  ${DRY} systemctl --root=${ROOT} enable ${1}
  return $?
}

function disable_service() {
  ${DRY} systemctl --root=${ROOT} disable ${1}
  return $?
}

function start_service() {
#  [[ "${ROOT}" == "${DEF_ROOT}" ]] && ${DRY} systemctl start ${1}
#  return $?
  return 0
}

function stop_service() {
  [[ "${ROOT}" == "${DEF_ROOT}" ]] && ${DRY} systemctl stop ${1}
  return $?
}

function install_soft_link() {
  ${DRY} ln -s "${ROOT%*/}/${1#*/}" "${ROOT%*/}/${2#*/}"
  return $?
}

function remove_soft_link() {
  [[ -h "${1}" ]] && ${DRY} rm -f "${ROOT%*/}/${1#*/}"
  return $?
}

function post_install() {
{{POST_INSTALL}}
}

function pre_uninstall() {
{{PRE_UNINSTALL}}
}

parse_for_help $@
check_prerequisites
parse_options $@

if [ ${UNINSTALL} -eq 1 ]; then
  pre_uninstall && uninstall_tarball
else
  install_tarball && post_install
fi

[[ $? -ne 0 ]] && error_exit "Operation failed!"
good_exit
"""


class InstallBuilder(object):
    def __init__(self):
        self.__config = dict()
        self.__script = TEMPLATE

    def build(self, config_filename):
        with open(config_filename) as fd:
            self.__config = yaml.load(fd, Loader=yaml.FullLoader)
        if 'tarball' not in self.__config:
            print('Error: YAML config file is missing required "tarball" entry!', file=sys.stderr)
            return 1
        if 'name' not in self.__config:
            print('Error: YAML config file is missing required "name" entry!', file=sys.stderr)
            return 1
        tarball_filename = self.__config['tarball']
        file_size = os.stat(tarball_filename).st_size
        with open(tarball_filename, 'rb') as fd:
            contents = fd.read(file_size)
        encoded = base64.encodebytes(contents).decode('utf-8').strip('\n')
        self.__script = self.__script.replace('{{YEAR}}', str(datetime.datetime.now().year))
        self.__script = self.__script.replace('{{BASE64_TARBALL}}', encoded)
        self.__build_install()
        return self.__write_install_script()

    def __write_install_script(self):
        try:
            destination = ''
            if 'destination_folder' in self.__config:
                destination = str(self.__config['destination_folder'])
                destination = destination.rstrip('/')
            filename = '{}/install-{}.sh'.format(destination, self.__config['name'])
            with open(filename, 'w') as fd:
                fd.write(self.__script)
            subprocess.call(['chmod', '+x', filename])
        except IOError:
            return 1
        return 0

    def __build_install(self):
        install_lines = ['  # Post Install Tasks']
        uninstall_lines = ['  # Pre Uninstall Tasks']
        self.__add_user_and_group(install_lines, uninstall_lines)
        self.__add_python_installs(install_lines, uninstall_lines)
        self.__add_services(install_lines, uninstall_lines)
        self.__add_soft_links(install_lines, uninstall_lines)
        self.__script = self.__script.replace('{{POST_INSTALL}}', '\n'.join(install_lines))
        self.__script = self.__script.replace('{{PRE_UNINSTALL}}', '\n'.join(uninstall_lines))

    def __add_user_and_group(self, install_lines, uninstall_lines):
        if 'add_user' in self.__config:
            add_name = 'add_non_interactive_user'
            if 'interactive' in self.__config['add_user'] and self.__config['add_user']['interactive']:
                add_name = 'add_interactive_user'
            install_parts = ['  ' + add_name, self.__config['add_user']['user']]
            uninstall_parts = ['  remove_user', self.__config['add_user']['user']]
            if 'group' in self.__config['add_user']:
                group = self.__config['add_user']['group']
                install_parts.append(group)
                uninstall_parts.append(group)
            install_lines.append(' '.join(install_parts))
            if len(uninstall_lines) == 1:
                uninstall_lines.append(' '.join(uninstall_parts))
            else:
                uninstall_lines.insert(1, ' '.join(uninstall_parts))

    def __add_python_installs(self, install_lines, uninstall_lines):
        if 'python_installers' in self.__config:
            for file in self.__config['python_installers']:
                uninstall_name = str(os.path.splitext(os.path.basename(file))[0].split('-')[0])
                install_lines.append('  install_wheel ' + file)
                if len(uninstall_lines) == 1:
                    uninstall_lines.append('  uninstall_wheel ' + uninstall_name)
                else:
                    uninstall_lines.insert(1, '  uninstall_wheel ' + uninstall_name)

    def __add_services(self, install_lines, uninstall_lines):
        if 'services' in self.__config:
            for service in self.__config['services']:
                install_lines.append('  enable_service ' + service)
                install_lines.append('  start_service ' + service)
                if len(uninstall_lines) == 1:
                    uninstall_lines.append('  stop_service ' + service)
                    uninstall_lines.append('  disable_service ' + service)
                else:
                    uninstall_lines.insert(1, '  disable_service ' + service)
                    uninstall_lines.insert(1, '  stop_service ' + service)

    def __add_soft_links(self, install_lines, uninstall_lines):
        if 'soft_links' in self.__config:
            for pair in self.__config['soft_links']:
                install_lines.append('  install_soft_link ' + pair['from'] + ' ' + pair['to'])
                if len(uninstall_lines) == 1:
                    uninstall_lines.append('  remove_soft_link ' + pair['to'])
                else:
                    uninstall_lines.insert(1, '  remove_soft_link ' + pair['to'])


if __name__ == '__main__':
    builder = InstallBuilder()
    argc = len(sys.argv) - 1
    if argc > 1:
        print('Usage: python3 install-builder.py [config_filename]')
        exit(1)
    if argc == 1:
        install_builder_file = sys.argv[1]
    else:
        install_builder_file = "./install-builder.yml"
    result = builder.build(install_builder_file)
    exit(result)
