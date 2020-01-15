# -*- coding: utf-8 -*-
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Display JSON data to screen in raw or tabular format.
"""
import os
import json
from texttable import Texttable
from .logger import Logger
from shutil import get_terminal_size


class JsonDisplay(object):
    """Decode the JSON contents and disply it based on user input to raw JSON format or tabular format"""

    def __init__(self, json_data):
        if json_data is None or len(json_data) == 0:
            raise RuntimeError("No data returned try with different filters.")
        self._json_data = json_data
        self.logger = Logger()

    def display_raw_json(self):
        try:
            # Try to treat the result as a JSON string
            parsed_result = json.loads(self._json_data)
        except ValueError:
            return self._json_data
        try:
            # Try to treat the parsed result as a schemed dictionary
            keys = [item["data"] for item in parsed_result["schema"]]
            data = [dict(zip(keys, row)) for row in parsed_result["data"]]
        except KeyError:
            data = parsed_result

        return json.dumps(data, indent=4)

    def _extract_column_heading_to_display_from_json_result(self, json_result, columns_order):
        column_index_in_data = list()
        header_list = list()
        schema_info = json_result['schema']
        if columns_order is None:
            for column_index in range(json_result['result-data-columns']):
                header_list.append(schema_info[column_index].get("heading").upper())
                column_index_in_data.append(column_index)
        else:
            for key in columns_order:
                for column_index in range(json_result['result-data-columns']):
                    if schema_info[column_index].get("data") == key:
                        header_list.append(schema_info[column_index].get("heading").upper())
                        column_index_in_data.append(column_index)
        return column_index_in_data, header_list

    def _convert_json_data_to_texttable_consumable_list(self, json_result, columns_order, json_field_with_info):
        # json_field_with_info could be 'data' or 'error' based on whether the result-status code is 0 or
        # non-zero respectively
        result = list()
        column_index_in_data, header_list = self._extract_column_heading_to_display_from_json_result(json_result,
                                                                                                     columns_order)
        result.append(header_list)

        # Extracting the data from each line and sorting it based on how user requested for in columns_order list
        for line_index in range(json_result['result-data-lines']):
            line_data = json_result[json_field_with_info][line_index]
            line_result = list()
            for column_index in column_index_in_data:
                line_result.append(line_data[column_index])
            result.append(line_result)
        return result

    def display_json_in_tabular_format(self, columns_order=None):
        json_result = json.loads(self._json_data)
        try:
            if json_result['result-status-code'] != 0:
                return self._print_data_as_table(self._convert_json_data_to_texttable_consumable_list(json_result,
                                                                                                      None,
                                                                                                      'error'))
            if json_result['result-data-columns'] == 0:
                raise RuntimeError("Data returned without schema information")
            if json_result['result-data-lines'] == 0:
                return "No data returned."
            return self._print_data_as_table(self._convert_json_data_to_texttable_consumable_list(json_result,
                                                                                              columns_order, 'data'))
        except (AttributeError, KeyError) as e:
            # self.logger.exception(str(e))
            raise RuntimeError(self._json_data)

    def _print_data_as_table(self, result_list):
        screen_width, _ = get_terminal_size((100, 0))
        t = Texttable(int(screen_width))
        col_type = ["t"] * len(result_list[0])
        t.set_cols_dtype(col_type)
        t.add_rows(result_list)
        return t.draw()
