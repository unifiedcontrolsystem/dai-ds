# -*- coding: utf-8 -*-
# Copyright (C) 2018 Intel Corporation
#
# SPDX-License-Identifier: Apache-2.0
"""
CLI logger
"""

import logging
import sys
import os
import io

VERBOSE = any(arg.startswith("-v") for arg in sys.argv)
XML_REPORTING = any(arg.startswith("-x") for arg in sys.argv)


class StreamHandler(logging.StreamHandler):

    _buffer = []

    def emit(self, record):
        """
        Emit a record.

        If a formatter is specified, it is used to format the record.
        The record is then written to the stream with a trailing newline.  If
        exception information is present, it is formatted using
        traceback.print_exception and appended to the stream.  If the stream
        has an 'encoding' attribute, it is used to determine how to do the
        output to the stream.
        """
        try:
            msg = self.format(record)
            if VERBOSE or not XML_REPORTING or record.levelno > logging.DEBUG:
                self._buffer.append(msg)
            stream = self.stream
            if VERBOSE or record.levelno > logging.DEBUG:
                stream.write(msg)
                stream.write(self.terminator)
                self.flush()
        except Exception:
            self.handleError(record)


class Formatter(logging.Formatter):
    """This class inherits from the logging.Formatter class and is a custom
    formatter used to enable different formats based on log level.
    """
    fmt = "%(asctime)s - %(levelname)s - %(message)s"
    datefmt = "%m-%d-%Y %H:%M:%S"
    if sys.version_info < (3, 0, 0):
        result_fmt = "%(message)s"
    elif sys.version_info < (3, 3, 4):
        result_fmt = logging._STYLES['{']("{message}")
    else:
        result_fmt = logging._STYLES['{'][0]("{message}")
    RESULT = 70

    def __init__(self, fmt=None):
        logging.Formatter.__init__(self, fmt=Formatter.fmt, datefmt=Formatter.datefmt)

    def format(self, record):
        if sys.version_info < (3, 0, 0):
            format_orig = self._fmt
        else:
            format_orig = self._style

        if record.levelno == Formatter.RESULT:
            if sys.version_info < (3, 0, 0):
                self._fmt = Formatter.result_fmt
            else:
                self._style = Formatter.result_fmt

        result = logging.Formatter.format(self, record)
        if sys.version_info < (3, 0, 0):
            self._fmt = format_orig
        else:
            self._style = format_orig

        return result


class Logger(io.TextIOBase):
    """This is a wrapper for Python's logging class that simplifies the setup and teardown of logging.
    This is a singleton design, meaning that only one logger is ever instantiated, even if the constructor is
    used in multiple places.

    The Logger class extends the TextIOBase class and can be used as an output stream
    for redirection. This is used elsewhere to reassign stdout and stderr to the logger
    object. This allows the logger to capture ``print()`` statements.

    """
    def write(self, *args, **kwargs):
        self.__print(*args, **kwargs)
        self.flush()

    def flush(self, *args, **kwargs):
        Logger.sh.flush()

    def read(self, *args, **kwargs):
        return sys.__stdin__.read(*args, **kwargs)

    def readline(self, *args, **kwargs):
        return sys.__stdin__.readline(*args, **kwargs)

    def getvalue(self):
        return "".join(Logger.sh._buffer)

    instances = 0
    logger = None
    filename = "ucs_cli"
    term = "\n"
    encoding = "utf8"

    def __print(self, message):
        Logger.logger.log(70, message)

    def __init__(self, level="NOTSET"):
        """Creates an internal logger object from the logging library.

        **USAGE**

        >>> lgr = Logger()
        >>> lgr = Logger("DEBUG")

        :param level: The logging level which determines what messages get output.
                      The default is "NOTSET" which enables all messages.
        :type level: String
        :returns: Logger object
        :rtype: Logger
        """
        if Logger.instances < 1:

            path = os.getenv("HOME")
            Logger.logger = logging.getLogger('')
            self.setLevel(level)
            Logger.formatter = Formatter()
            Logger.sh = StreamHandler(sys.stdout)
            Logger.sh.terminator = ""
            Logger.fhLog = logging.FileHandler("{0}/{1}.log".format(path, Logger.filename), mode='a')
            Logger.fhLog.terminator = ""
            Logger.sh.setFormatter(Logger.formatter)
            Logger.fhLog.setFormatter(Logger.formatter)
            Logger.logger.addHandler(Logger.sh)
            Logger.logger.addHandler(Logger.fhLog)
            logging.addLevelName(60, "SUCCESS")
            logging.addLevelName(70, "RESULT")
            Logger.instances += 1

    def setLevel(self, level):
        """Sets the logging level
        The level determines what messages get output
        The levels are:

            ============    ======
              **Level**     **# val**
            ------------    ------
            RESULT            70
            SUCCESS           60
            ERROR             40
            WARNING           30
            INFO              20
            DEBUG             10
            NOTSET             0
            ============    ======

        :param level: The logging level minimum to output
        :type level: String
        :returns: Nothing
        :rtype: None
        """
        level = level.upper()
        if level == "DEBUG":
            logLev = logging.DEBUG
        elif level == "INFO":
            logLev = logging.INFO
        elif level == "WARNING":
            logLev = logging.WARNING
        elif level == "ERROR":
            logLev = logging.ERROR
        elif level == "SUCCESS":
            logLev = logging.SUCCESS
        elif level == "RESULT":
            logLev = logging.RESULT
        else:
            logLev = logging.NOTSET

        Logger.logger.setLevel(logLev)

    def tearDown(self):
        """Safely deletes the logger object"""
        Logger.logger.removeHandler(Logger.sh)
        Logger.sh.flush()
        Logger.sh.close()
        Logger.instances -= 1

    def info(self, message):
        """Logs a message at the info level

        **USAGE**

        >>> lgr = Logger()
        >>> lgr.info("Your message here")
        01-15-2016 15:13:22 - INFO - Your message here

        :param message: The message to be logged
        :type message: String
        """
        message = str(message)
        Logger.logger.info(message+Logger.term)

    def debug(self, message):
        """Logs a message at the debug level

        **USAGE**

        >>> lgr = Logger()
        >>> lgr.debug("Your message here")
        01-15-2016 15:14:20 - DEBUG - Your message here

        :param message: The message to be logged
        :type message: String
        """
        message = str(message)
        Logger.logger.debug(message+Logger.term)

    def warning(self, message):
        """Logs a message at the warning level using the non-deprecated warning() method

        Args:
            message (str): The message to be logged
        """
        message = str(message)
        Logger.logger.warning(message+Logger.term)

    def error(self, message):
        """Logs a message at the error level

        **USAGE**

        >>> lgr = Logger()
        >>> lgr.error("Your message here")
        01-15-2016 15:16:24 - ERROR - Your message here

        :param message: The message to be logged
        :type message: String
        """
        message = str(message)

        Logger.logger.error(message+Logger.term)

    def exception(self, message):
        """Logs a message and prints full traceback at the error level.

        Leverages the python logging class' automatic traceback printing functionality when logging.exception is
        called in an except clause.

        Args:
            message (str): User message to be logged before traceback
        """
        message = str(message)
        Logger.logger.exception(message+Logger.term)

    def log(self, level, message):
        """Logs a message at the level passed to it

        **USAGE**

        >>> lgr = Logger()
        >>> lgr.log(9001, "His log level is over 9000!")
        01-15-2016 15:18:10 - Level 9001 - His log level is over 9000!


        :param level: The log level of the message
        :type level: Integer
        :param message: The message to be logged
        :type message: String
        """
        message = str(message)

        Logger.logger.log(level, message+Logger.term)

    def success(self, message):
        """Logs a message at the success level

        **USAGE**

        >>> lgr = Logger()
        >>> lgr.success("Your message here")
        01-15-2016 15:19:16 - SUCCESS - Your message here

        :param message: The message to be logged
        :type message: String
        """
        message = str(message)

        Logger.logger.log(60, message+Logger.term)

    def result(self, message):
        """Logs a message at the result level. The alternate
        name for this function is log_print. They are mapped
        to each other.

        **USAGE**

        >>> lgr = Logger()
        >>> lgr.result("Your message here")
        Your message here
        >>> lgr.log_print("Your message here")
        Your message here

        :param message: The message to be logged
        :type message: String
        """
        message = str(message)

        Logger.logger.log(70, message+Logger.term)

    log_print = result
