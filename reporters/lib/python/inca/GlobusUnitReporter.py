import os
import re
import signal
import socket
import string
import tempfile
import time

from inca.SimpleUnitReporter import SimpleUnitReporter

class GlobusUnitReporter(SimpleUnitReporter):
  """GlobusUnitReporter - Convenience module for creating simple unit reporters
     that submit a test via globus::

       from inca.GlobusUnitReporter import GlobusUnitReporter
       reporter = GlobusUnitReporter(
         name = 'Reporter Name',
         version = 0.1,
         description = 'A really helpful reporter description',
         url = 'http://url.to.more.reporter.info'
         unit_name = 'What this reporter tests'
       )

     This module is a subclass of SimpleUnitReporter that provides convenience
     methods for submitting a unit test via globus.
  """

  def __init__(self, **attributes):
    """Class constructor that returns a new GlobusUnitReporter object.  See
       SimpleUnitReporter for parameters.
    """
    SimpleUnitReporter.__init__(self, **attributes)
    self.addDependency('inca.GlobusUnitReporter')
    self.addDependency('inca.GridProxyReporter')

  def submitCSource(self, **attrs):
    """Submit a small C program to execute via a local GRAM.  In addition to
       the parameters recognized by submitJob, the required attrs['code']
       specifies the source to compile.
    """
    if not attrs.has_key('code') or attrs['code'] == None:
      return (None, 'No code passed to submitCSource')

    clean = 0
    if attrs.has_key('cleanup') and attrs['cleanup'] != None:
      clean = attrs['cleanup']
    dir = tempfile.mkdtemp()
    flavor = ''
    if attrs.has_key('flavor') and attrs['flavor'] != None:
      flavor = '--flavor=' + attrs['flavor']
    os.chdir(dir)

    cc = '$(GLOBUS_CC)'
    ld = '$(GLOBUS_LD)'
    if attrs.has_key('mpi') and attrs['mpi']:
      cc = 'mpicc'
      ld = 'mpicc'
    (status, makeFile) = \
      self.loggedCommandStatusOutput('globus-makefile-header ' + flavor)
    if status != 0:
      return (None, 'globus-makefile-header failed: ' + makeFile + '\n')
    makeFile += '''

all:
	''' + cc + ''' $(GLOBUS_CFLAGS) $(GLOBUS_INCLUDES) -c gh.c
	''' + ld + ''' -o gh gh.o $(GLOBUS_LDFLAGS) $(GLOBUS_PKG_LIBS) $(GLOBUS_LIBS)
'''

    out = open('Makefile', 'w')
    if not out:
      return (None, 'Unable to write Makefile\n')
    out.write(makeFile)
    out.close()
    out = open('gh.c', 'w')
    if not out:
      return (None, 'Unable to write source\n')
    out.write(attrs['code'])
    out.close()

    (status, output) = self.loggedCommandStatusOutput('make')
    if status != 0:
      error = 'make failed: ' + output + '\n'
      output = None
    else:
      if attrs.has_key('env') and attrs['env'] != None and attrs['env'] != '':
        attrs['env'] += ':'
      else:
        attrs['env'] = ''
      attrs['env'] += 'LD_LIBRARY_PATH='+os.environ['GLOBUS_LOCATION']+'/lib'
      attrs['executable'] = dir + '/gh'
      attrs['remote'] = 0
      (output, error) = self.submitJob(**attrs)
    if clean:
      os.chdir(os.environ['HOME'])
      self.loggedCommandOutput('/bin/rm -fr ' + dir)
    return (output, error)

  def submitJob(self, **attrs):
    """Submit a job to execute a command via Globus.
       Recognized parameters::

         arguments
           arguments to pass to executable; default ''

         check
           poll job for completion every this many seconds; default 30

         cleanup
           remove temporary files after run; default true

         count
           number of hosts to use; default 1

         debug
           log the submision command and the result with -dumprsl; default false

         duroc
           add (resourceManagerContact=xx) to rsl; default false

         executable
           the program to run; required

         env
           environment variable to set; default ''

         host
           host where run takes place; default localhost

         mpi
           execute as an MPI program; default false

         queue
           name of batch queue to submit job to; default none

         remote
           executable is already on the jobmanager resource; default true

         service
           the Globus service to invoke; default to Globus default

         timeout
           kill the job and report an error after this many seconds; default
           3600 (1 hr)

    """
    if not attrs.has_key('executable') or attrs['executable'] == None:
      return (None, 'No executable supplied to submitJob\n')

    clean = 1
    if attrs.has_key('cleanup') and attrs['cleanup'] != None:
      clean = attrs['cleanup']
    if attrs.has_key('host') and attrs['host'] != None:
      contact = attrs['host']
    else:
      contact = socket.gethostname()
    if attrs.has_key('service') and attrs['service'] != None:
      contact += '/' + attrs['service']
    count = 1
    if attrs.has_key('count') and attrs['count'] != None:
      count = attrs['count']
    debug = ''
    if attrs.has_key('debug') and attrs['debug']:
      debug = '-dumprsl'
    err = os.environ['HOME'] + '/.inca.tmp.' + str(os.getpid()) + '.err'
    extraRsl = '(host_count=' + str(count) + ')'
    if attrs.has_key('duroc') and attrs['duroc'] != None:
      extraRsl += '(resourceManagerContact=' + contact + ')'
    if attrs.has_key('mpi') and attrs['mpi']:
      extraRsl += '(jobtype=mpi)'
    out = os.environ['HOME'] + '/.inca.tmp.' + str(os.getpid()) + '.out'
    pollTime = 30
    if attrs.has_key('check') and attrs['check'] != None:
      pollTime = attrs['check']
    timeout = 3600
    if attrs.has_key('timeout') and attrs['timeout'] != None:
      timeout = attrs['timeout']

    cmd = \
      'globus-job-submit ' + debug + ' -stderr -s ' + err + ' -stdout -s ' + \
      out + ' ' + contact + ' -count ' + str(count) + ' -maxtime ' + \
      str(int(int(timeout) / 60)) + " -x '" + extraRsl + "'"
    if attrs.has_key('env') and attrs['env'] != None:
      env = attrs['env']
      env = re.sub(r'^|:', ' -env ', env)
      cmd += env
    if attrs.has_key('queue') and attrs['queue'] != None:
      cmd += ' -q ' + attrs['queue']
    if attrs.has_key('executable') and attrs['executable'] != None:
      if attrs.has_key('remote') and attrs['remote']:
        cmd += ' -l ' + attrs['executable']
      else:
        cmd += ' -s ' + attrs['executable']
    if attrs.has_key('arguments') and attrs['arguments'] != None:
      cmd += ' ' + attrs['arguments']

    (status, jobId) = self.loggedCommandStatusOutput(cmd)
    if status != 0:
      return (None, "call to '" + cmd + "' failed: " + jobId + "\n")
    if not re.search('https', jobId):
      return (None, "invalid job id returned: '" + jobId + "'\n")

    oldHandler = signal.signal(signal.SIGALRM, self._timeoutException)
    try:
      jobStatus = 'ACTIVE'
      signal.alarm(int(timeout))
      while re.match('(PENDING|ACTIVE|UNSUBMITTED)$', jobStatus):
        time.sleep(int(pollTime))
        jobStatus = self.loggedCommandOutput('globus-job-status ' + jobId)
      signal.alarm(0)
    except (KeyboardInterrupt, SystemExit):
      self.loggedCommandOutput('globus-job-cancel -f ' + jobId)
      return (None, 'job did not complete within '+str(timeout)+' seconds\n')
    signal.signal(signal.SIGALRM, oldHandler)

    output = None
    file = open(out, 'r')
    if file:
      lines = file.readlines()
      file.close()
      output = string.join(lines, '\n')
      if clean:
        os.unlink(out)
    error = None
    file = open(err, 'r')
    if file:
      lines = file.readlines()
      file.close()
      error = string.join(lines, '\n')
      if clean:
        os.unlink(err)

    return (output, error)
