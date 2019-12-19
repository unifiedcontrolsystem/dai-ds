# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Display to represent data.
"""
import sys
import json
from texttable import Texttable

"""Display to represent data."""


def parse_json_response(rs_response_data):
    json_data = json.loads(rs_response_data)
    return json_data


def convert_to_lists(json_data):
    header_list = list()
    for key in sorted(json_data[0].keys()):
        header_list.append(key.upper())
    return generate_data(json_data, header_list)


def convert_to_lists_ordered(json_data, columns_order):
    header_list = list()
    for key in columns_order:
        header_list.append(key.upper())
    return generate_data(json_data, header_list, columns_order)


def convert_to_list_json_data(json_data, columns_order):
    header_list = list()
    for key in columns_order:
        header_list.append(key.upper())
    return generate_json_data_as_list(json_data, header_list)


def generate_json_data_as_list(json_data, header_list):
    result_table = list()
    result_table.append(header_list)
    for major_prop_key in json_data.keys():
        sub_json_data = json_data[major_prop_key]
        sub_json_data_keys = sub_json_data.keys()
        count = 0
        for key in sub_json_data_keys:
            diff_json_data = sub_json_data[key]
            sub_values_list = list()
            if count == 0:
                sub_values_list.append(major_prop_key)
            else:
                sub_values_list.append("")
            sub_values_list.append(key)
            sub_values_list.append(diff_json_data['actual'])
            sub_values_list.append(diff_json_data['ref'])
            result_table.append(sub_values_list)
            count = count + 1
    return result_table


def generate_data(json_data, header_list, columns_order):
    result_table = list()
    result_table.append(header_list)
    for data in json_data:
        values_list = list()
        for key in columns_order:
                values_list.append(data[key])
        result_table.append(values_list)
    return result_table


def print_data_as_table(result_list):
    t = Texttable(100)
    t.add_rows(result_list)
    return t.draw()


def print_data_as_csv(result_list):
    str_output = '\n'
    for rows in result_list:
        str_output += ', '.join(str(e) for e in rows) + '\n'
    return str_output


def print_response_as_table(response_data, columns_order):
    json_data = parse_json_response(response_data)
    check_view_json_parsed_response(json_data)
    data = convert_to_lists_ordered(json_data, columns_order)
    return print_data_as_table(data)


def print_inventory_diff_as_table(response_data, columns_order):
    json_data = parse_json_response(response_data)
    check_inventory_json_parsed_response(json_data)
    data = convert_to_list_json_data(json_data, columns_order)
    return print_data_as_table(data)


def check_inventory_json_parsed_response(json_data):
    if len(json_data) == 0:
        raise RuntimeError("No Differences Found.")


def check_view_json_parsed_response(json_data):
    if len(json_data) == 0:
        raise RuntimeError("No data returned try with different filters.")
