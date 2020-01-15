# -*- coding: utf-8 -*-
# Copyright (C) 2019 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
Class to generate summary from events received
"""
import json
from .logger import Logger
from .json_display import JsonDisplay
from collections import defaultdict
from collections import OrderedDict
import textwrap


class ViewEventsSummary(object):
    """Generate summary from events"""

    def __init__(self, json_data):
        if json_data is None or len(json_data) == 0:
            raise RuntimeError("No data returned try with different filters.")
        self._json_data = json_data
        self.logger = Logger()
        self.result = ''

    def generate_summary(self):
        raw_json = JsonDisplay(self._json_data).display_raw_json()
        self.result = "\nRAS EVENTS SUMMARY\n"

        # Summary Based on Severity
        self.result += "\n" + self._generate_event_based_summary(raw_json)

        # Summary Based on Lctn
        self.result += "\n" + self._generate_location_based_summary(raw_json)

        # Summary Based on EventType
        self.result += "\n" + self._generate_eventtype_based_summary(raw_json)

        # Event & Location Based Summary
        self.result += "\n" + self._generate_event_location_based_summary(raw_json)

        return self.result

    def _generate_event_based_summary(self, raw_json):
        severity_summary = defaultdict(int)
        severity_dict = json.loads(raw_json)
        for severity in severity_dict:
            severity_summary[severity['severity']] += 1
        result = "EVENTS SUMMARY BASED ON SEVERITY\n"
        result += "{:<10} {:<15}\n".format('SEVERITY', 'COUNT')
        for severity, count in severity_summary.items():
            result += "{:<10} {:<15}\n".format(str(severity), count)
        return result

    def _generate_location_based_summary(self, raw_json):
        location_summary = defaultdict(int)
        location_dict = json.loads(raw_json)
        for location in location_dict:
            location_summary[location['lctn']] += 1
        result = "EVENTS SUMMARY BASED ON LOCATION\n"
        result += "{:<14} {:<15}\n".format('LOCATION', 'COUNT')
        for location, count in location_summary.items():
            result += "{:<14} {:<15}\n".format(str(location), count)
        return result

    def _generate_eventtype_based_summary(self, raw_json):
        event_summary = dict()
        event_dict = json.loads(raw_json)
        wrapper = textwrap.TextWrapper(width = 100)
        for event in event_dict:
            if event['type'] in event_summary:
                event_summary[event['type']][0] = event_summary[event['type']][0] + 1
            else:
                event_summary[event['type']] = list()
                event_summary[event['type']] = [1, event['detail'], event['severity']]
        result = "EVENTS SUMMARY BASED ON EVENT TYPE\n"
        result += "{:<5} {:<70} {:<10} {:<60}\n".format('COUNT', 'EVENT TYPE', 'SEVERITY', 'DETAILS')
        for type, v in event_summary.items():
            count, message, severity = v
            short_message = wrapper.fill(text=(textwrap.shorten(text=message, width=100)))
            result += "{:<5} {:<70} {:<10} {:<60}\n".format(count, str(type),
                                                             str(severity),
                                                             str(short_message))
        return result

    def _generate_event_location_based_summary(self, raw_json):
        location_summary = OrderedDict()
        event_location_summ = json.loads(raw_json)
        for location in event_location_summ:
            if (location['lctn'], location['type']) in location_summary:
                location_summary[(location['lctn'], location['type'])][0] += 1
            else:
                location_summary[(location['lctn'], location['type'])] = list()
                location_summary[(location['lctn'], location['type'])] = [1, location['type'], location['severity'],
                                                                          location['controloperation'], location['time']]
        result = "EVENTS SUMMARY BASED ON THE COMBINATION OF LOCATION & EVENTS\n"
        result += "{:<14} {:<10} {:<70} {:<15} {:<20}\n".format('LOCATION', 'COUNT', 'TYPE', 'SEVERITY',
                                                           'CONTROL OPERATION', 'LATEST EVENT TIME')
        for key, values in location_summary.items():
            count, type, severity, control_operation, timestamp = values
            result += "{:<14} {:<10} {:<70} {:<15} {:<20}\n".format(str(key[0]),
                                                                     count,
                                                                     str(type),
                                                                     str(severity),
                                                                     str(control_operation),
                                                                     str(timestamp))
        return result
