#!/usr/bin/env python

# This test checks that it can run a /bin/date job through the specified 
# local gatekeeper service.  Because some cases may involve submitting through
# a batch interface, we associate a timeout value.  If the job does not return
# within the specified timeout value (e.g., one hour), we consider the test
# successful.

import re
import sys

from inca.GlobusUnitReporter import GlobusUnitReporter

reporter = GlobusUnitReporter(
  name = 'globus.unit.date.py',
  version = 3,
  description = 'Verifies the submission of a /bin/date job through the ' +
                 'specified local gatekeeper service',
  url = 'http://www.globus.org',
  unit_name = 'globus date'
)
reporter.addArg('count', 'host_count parameter of rsl', '', '\d+')
reporter.addArg('host', 'hostname where gatekeeper is running', '')
reporter.addArg('service', 'the name of the jobmanager', '')
reporter.addArg('timeout', 'kill the job after this many minutes', '60', '\d+')
reporter.processArgv(sys.argv[1:])
count = reporter.argValue('count')
if count == '':
  count = None
host = reporter.argValue('host')
if host == '':
  host = None
service = reporter.argValue('service')
if service == '':
  service = None
timeout = reporter.argValue('timeout')

(date, err) = reporter.submitJob(
  executable = '/bin/date', service = service, host = host,
  count = count, timeout = timeout, remote = 1
)

VALID_DATE = r'\w{3} \w{3} (\s|\d)\d \d{2}:\d{2}:\d{2}.* \w{3} \d{4}'
if date == None or date == '':
  msg = 'test failed'
  if err != None:
    msg += ': ' + err
  reporter.unitFailure(msg)
elif not re.match(VALID_DATE, date):
  reporter.unitFailure('job completed but result is suspect: ' + date)
else:
  reporter.unitSuccess()
reporter.printReport()
