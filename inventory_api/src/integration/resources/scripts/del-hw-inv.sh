#!/bin/bash

sqlcmd --port=$1 < del-hw-inv.sql
