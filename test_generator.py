import logging
import graypy
import json
import datetime
import random
import time
import sys
import argparse
"""
Sets the json fields from the message as log fields and fixes the information
about the caller. In addition a json field named 'custom' is replaced with the
logger name and some general information is inserted into fields (e.g. timestamp).

This filter could be added to any logger, but some functionalities rely on the
function names (send_log) used in this file to send logs.
"""
class ContextFilter(logging.Filter):
    def __init__(self, logger_name):
        self.logger_name = logger_name
        self.ecs_version = '1.5'

    def filter(self, record):
        """
        Flattens a dict
        """
        def flatten(x, name='', res={}, delim='.'):
            if type(x) is type({}):
                for f, c in x.items():
                    flatten(c, name + f + delim, res, delim)
            else:
                res[name[:-1]] = x
            return res
         
        # keep the unfiltered message as requested by ecs standard
        # log_original = record.msg
        # load the json from the message
        json_dict = json.loads(record.msg)
        # replace the custom field with the programs name
        if "custom" in json_dict:
            json_dict[self.logger_name.lower()] = json_dict.pop('custom')
        # flatten the structure so the fields are set properly
        json_dict = flatten(json_dict)
        # set all fields defined in the json, unless they are already defined
        for field, contents in json_dict.items():
            try:
                getattr(record, field)
            except AttributeError:
                setattr(record, field.replace(" ", "_"), contents)

        # set the message field to the appropriate value
        if "message" in json_dict:
            record.msg = json_dict.pop("message")
        else:
            record.msg = ""

        # set additional common information
        #setattr(record, 'log.original', log_original)
        setattr(record, 'ecs.version', self.ecs_version)
        setattr(record, 'timestamp', datetime.datetime.now(tz=None).isoformat())
        setattr(record, 'log.logger', self.logger_name)
        return True


"""
send log entries to graylog, using defined severity and fields defined by a
json string
"""
def send_log_json(logger, severity, json_string):
    json_string = json.dumps(json_string)
    if severity == 'critical':
        logger.critical(json_string)
    elif severity == 'warning':
        logger.warning(json_string)
    else:
        logger.error(json_string)


def simple(loggers):
    msg = {
        "message":"test_simple",
        "test_case":"test_simple",
        "test_simple_key": "simple",
        "source":"testcase_generator",
    }

    send_log_json(loggers[0], "warning", dict({"test_simple_a": "a"}, **msg))
    send_log_json(loggers[0], "warning", dict({"test_simple_b": "b"}, **msg))

    


def test_key_int(loggers):
    key = random.randint(1, 2**32)
    msg = {
        "message":"test_key_int",
        "test_case":"test_key_int",
        "test_key_int_key": key,
        "source":"testcase_generator",
    }

    send_log_json(loggers[0], "warning", dict({"test_key_int_a": "a"}, **msg))
    send_log_json(loggers[0], "warning", dict({"test_key_int_b": "b"}, **msg))

def test_collect_date(loggers):
    key = random.randint(1, 2**32)
    msg = {
        "message":"test_collect_date",
        "test_case":"test_collect_date",
        "test_collect_date_key": key,
        "source":"testcase_generator",
    }

    send_log_json(loggers[0], "warning", dict({"test_collect_date_a": datetime.datetime.now(tz=None).isoformat()}, **msg))
    send_log_json(loggers[0], "warning", dict({"test_collect_date_b": datetime.datetime.now(tz=None).isoformat()}, **msg))



    
def test_key_multi(loggers):
    key1 = random.randint(1, 2**32)
    key2 = random.randint(1, 2**32)
    msg = {
        "message":"test_key_multi",
        "test_case":"test_key_multi",
        "test_key_multi_key1": key1,
        "test_key_multi_key2": key2,
        "source":"testcase_generator",
        "test_be_collected": "True",
    }

    send_log_json(loggers[0], "warning", dict({"test_key_multi_a": "a"}, **msg))
    send_log_json(loggers[0], "warning", dict({"test_key_multi_b": "b"}, **msg))

    msg = {
        "message":"test_key_multi",
        "test_case":"test_key_multi",
        "test_key_multi_key1": key1,
        "source":"testcase_generator",
        "test_be_collected": "False",
    }

    send_log_json(loggers[0], "warning", dict({"test_key_multi_a": "a"}, **msg))
    send_log_json(loggers[0], "warning", dict({"test_key_multi_b": "b"}, **msg))

    msg = {
        "message":"test_key_multi",
        "test_case":"test_key_multi",
        "test_key_multi_key2": key2,
        "source":"testcase_generator",
        "test_be_collected": "False",
    }

    send_log_json(loggers[0], "warning", dict({"test_key_multi_a": "a"}, **msg))
    send_log_json(loggers[0], "warning", dict({"test_key_multi_b": "b"}, **msg))

parser = argparse.ArgumentParser(prog="test_generator")
parser.add_argument("node1", help="Address for first graylog cluster node")
parser.add_argument("node2", help="Address for second graylog cluster node")
parser.add_argument("node3", help="Address for third graylog cluster node")
parser.add_argument("port", type=int, help="Input port")

if not sys.argv[1:]:
    parser.print_help()
    sys.exit()

args = parser.parse_args(sys.argv[1:])


loggers = []
for no, node_address in enumerate([args.node1, args.node2, args.node3]):
    name = "generator" + str(no)
    logger = logging.getLogger(name)
    logger.addHandler(graypy.GELFTCPHandler(node_address, args.port))
    logger.addFilter(ContextFilter(name))
    loggers.append(logger)

tests = [simple, test_key_int, test_key_multi, test_collect_date]
for test in tests:
    print("Running " + test.__name__)
    test(loggers)
