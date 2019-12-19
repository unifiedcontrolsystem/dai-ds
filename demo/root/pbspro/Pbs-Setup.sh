#! /bin/sh
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0

###
qmgr -c "create hook Provision_Hook"
qmgr -c "import hook Provision_Hook application/x-python default /root/provision_hook.py"
qmgr -c "set hook Provision_Hook event = provision"
qmgr -c "set hook Provision_Hook alarm = 600"
qmgr -c "set server max_concurrent_provision = 24"
###
qmgr -c "set node R00-CH0-CB0-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
#qmgr -c "set node R00-CH0-CB0-PM0-CN0 current_aoe = centos7.2-pbspro"
qmgr -c "set node R00-CH0-CB0-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB0-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB0-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB0-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB1-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB1-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB1-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB2-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB2-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB2-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB3-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB3-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB3-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB4-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB4-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB4-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB5-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB5-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB5-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB6-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB6-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB6-PM3-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM0-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM0-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM0-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM0-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM0-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM0-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM1-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM1-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM1-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM1-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM1-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM1-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM2-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM2-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM2-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM2-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM2-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM2-CN1 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM3-CN0 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM3-CN0 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM3-CN0 state = free"
#
qmgr -c "set node R00-CH0-CB7-PM3-CN1 resources_available.aoe = \"centos7.2-pbspro,centos7.2-vtune,centos7.3-pbspro,centos7.3-vtune\" "
qmgr -c "set node R00-CH0-CB7-PM3-CN1 provision_enable = True"
qmgr -c "set node R00-CH0-CB7-PM3-CN1 state = free"
###
qmgr -c "set hook Provision_Hook enabled = True"
###