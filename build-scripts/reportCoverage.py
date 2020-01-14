#!/usr/bin/env python3
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
import argparse
from xml.etree import ElementTree as Parser
from os import path
from glob import glob


TARGET_METHOD = 90.0
TARGET_BRANCH = 70.0


class ReportJacoco(object):
    """Report Jacoco coverage XML as plain text for a jacoco XML file."""
    VERSION = '2.0'

    def __init__(self):
        self.__parser = argparse.ArgumentParser(description='Report a Summary of Jacoco XML files as HTML.')
        self.__parser.add_argument('--version', action='version',
                                   version='%(prog)s version {}'.format(ReportJacoco.VERSION))
        self.__parser.add_argument('-t', '--title',
                                   help='Specify a title for the result.', default='DAI-DS Coverage Summary',
                                   action='store')
        self.__title = None
        self.__results = []
        self.__html_file = 'build/reports/coverage/index.html'

    def main(self):
        parsed = self.__parser.parse_args()
        self.__title = parsed.title
        for file in glob('build/jacoco/*.xml'):
            self.__results.append(ReportJacoco.__parse_xml_file(file))
        self.__add_totals()
        self.__sort_data()
        self.__output_report(self.__html_file)
        return 0

    @staticmethod
    def __parse_xml_file(file):
        component_name = path.split(file)[-1]
        component_name = component_name.split('-')[-1].replace('.xml', '')
        new_tree = Parser.parse(file)
        report = new_tree.getroot()
        line_covered = 0
        line_total = 0
        line_percent = 0.0
        branch_covered = 0
        branch_total = 0
        branch_percent = 0.0
        for child in list(report):
            if child.tag == 'counter':
                name = child.get('type')
                if name == 'METHOD':
                    line_covered = int(child.get('covered'))
                    line_total = line_covered + int(child.get('missed'))
                    line_percent = float(line_covered) / float(line_total) * 100.0
                elif name == 'BRANCH':
                    branch_covered = int(child.get('covered'))
                    branch_total = int(child.get('covered')) + int(child.get('missed'))
                    branch_percent = float(branch_covered) / float(branch_total) * 100.0
        return {
            'name': component_name,
            'line_covered': line_covered,
            'line_total': line_total,
            'line_percent': line_percent,
            'branch_covered': branch_covered,
            'branch_total': branch_total,
            'branch_percent': branch_percent
        }

    def __add_totals(self):
        total_lines_covered = 0
        total_lines = 0
        total_branches_covered = 0
        total_branches = 0
        for data in self.__results:
            total_lines_covered += data['line_covered']
            total_lines += data['line_total']
            total_branches_covered += data['branch_covered']
            total_branches += data['branch_total']
        self.__results.append({
            'name': "TOTAL",
            'line_covered': total_lines_covered,
            'line_total': total_lines,
            'line_percent': float(total_lines_covered) / float(total_lines) * 100.0,
            'branch_covered': total_branches_covered,
            'branch_total': total_branches,
            'branch_percent': float(total_branches_covered) / float(total_branches) * 100.0
        })

    def __sort_data(self):
        size = len(self.__results) - 1
        for i in range(0, size):
            for j in range(1, size):
                first = self.__results[j-1]['line_percent'] + self.__results[j-1]['branch_percent']
                second = self.__results[j]['line_percent'] + self.__results[j]['branch_percent']
                if first > second:
                    saved = self.__results[j-1]
                    self.__results[j-1] = self.__results[j]
                    self.__results[j] = saved

    def __output_report(self, out_file):
        html = '<html><head><title>' + self.__title + '</title></head><body><h2>' + self.__title + "</h2>"
        html += '<blockquote><table border="1"><tr style="background-color:LightGray;"><th><b>Component</b></th>'
        html += '<th><b>Method Coverage</b></th><th><b>Branch Coverage</b></th></tr>'
        for data in self.__results:
            link = '{}/index.html'.format(data['name'])
            if data['name'] not in ['TOTAL']:
                ucs_back = ''
                html += '<tr><td{}><a href="{}">{}</a></td>'.format(ucs_back, link, data['name'])
                if data['line_percent'] >= TARGET_METHOD:
                    color = 'LightGreen'
                else:
                    color = 'LightCoral'
                html += '<td title="{}/{} covered" align="center" style="background-color:{};">{:3.1f}%</td>'.\
                    format(data['line_covered'], data['line_total'], color, data['line_percent'])
                if data['branch_percent'] >= TARGET_BRANCH:
                    color = 'LightGreen'
                else:
                    color = 'LightCoral'
                html += '<td title="{}/{} covered" align="center" style="background-color:{};">{:3.1f}%</td></tr>'.\
                    format(data['branch_covered'], data['branch_total'], color, data['branch_percent'])
        for data in self.__results:
            if data['name'] in ['TOTAL']:
                html += '<tr style="background-color:yellow;"><td><b>{}</b></td>'.format(data['name'])
                if data['line_percent'] >= TARGET_METHOD:
                    color = 'LightGreen'
                else:
                    color = 'LightCoral'
                html += '<td title="{}/{} covered" align="center" style="background-color:{};"><b>{:3.1f}%</b></td>'.\
                    format(data['line_covered'], data['line_total'], color, data['line_percent'])
                if data['branch_percent'] >= TARGET_BRANCH:
                    color = 'LightGreen'
                else:
                    color = 'LightCoral'
                html += '<td title="{}/{} covered" align="center" ' \
                        'style="background-color:{};"><b>{:3.1f}%</b></td></tr>'.\
                    format(data['branch_covered'], data['branch_total'], color, data['branch_percent'])
                html += '<tr style="background-color: yellow"><td><b>TARGETS</b></td>' \
                        '<td align="center"><b>{}</b></td><td align="center"><b>{}</b></td></tr>'.format(TARGET_METHOD,
                                                                                                         TARGET_BRANCH)
                print('\n###############################################################################')
                print('### Total Coverage = {:3.1f}% / {:3.1f}% (method / branch)'.format(
                    data['line_percent'], data['branch_percent']))
                print('###############################################################################')
        html += '</table>'
        html += '<div><br /><u><b>NOTE:</b></u> Sorted by sum of line and branch percentages.</div>'
        html += '</blockquote></body></html>'
        with open(out_file, 'w') as file:
            file.write(html)


if __name__ == '__main__':
    import sys
    app = ReportJacoco()
    sys.exit(app.main())
