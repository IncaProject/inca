import commands
import os
import os.path
import re
import signal
import socket
import string
import sys
import time

class Reporter:
  """Reporter - Module for creating Inca reporters::

       from inca.Reporter import Reporter
       reporter = Reporter(
         name = 'hack.version',
         version = 0.1,
         description = 'A really helpful reporter description',
         url = 'http://url.to.more.reporter.info'
       )

     This module creates Inca reporters--objects that produce XML that follows
     the Inca Report schema.  The constructor may be called with a number of
     reporter attributes that can later be set and queried with their
     corresponding get/set functions (described below).  For example::

       reporter = Reporter()
       reporter.setUrl('http://url.to.more.reporter.info')
       reporter.setVersion(0.1)
  """

  def __init__(self, **attributes):
    """Class constructor that returns a new Reporter object.

       The constructor may be called with any of the following named attributes
       as parameters::

         body
           the XML body of the report.  See the Inca Report schema for format.

         completed
           boolean indicating whether or not the reporter has completed
           generating the information it is intended to produce

         description
           a verbose description of the reporter

         fail_message
           a message describing why the reporter failed to complete its task

         name
           the name that identifies this reporter

         url
           URL to get more information about the reporter

         version
           the version of the reporter; defaults to '0'
    """

    self.args = {}
    self.argv = []
    self.body = None
    self.completed = False
    self.cwd = os.getcwd()
    self.description = None
    self.dependencies = []
    self.fail_message = None
    self.log_entries = []
    self.log_pat = '^$'
    self.name = os.path.basename(sys.argv[0])
    self.temp_paths = []
    self.url = None
    self.version = '0'

    self.addArg('help', 'display usage information (no|yes)', 'no', 'no|yes')
    self.addArg(
      'log', 'log message types included in report', '0',
      '[012345]|debug|error|info|system|warn'
    )
    self.addArg('verbose', 'verbosity level (0|1|2)', '1', '[012]')
    self.addArg('version', 'show reporter version (no|yes)', 'no', 'no|yes')
    self.addDependency('inca.Reporter')

    for attr in attributes.keys():
      if hasattr(self, attr):
        setattr(self, attr, attributes[attr])
      else:
        sys.stderr.write("'" + attr + "' is an invalid attribute\n");

  def addArg(self, name, description=None, default=None, pattern=None):
    """Adds a command line argument (invocation syntax -name=value) to the
       reporter.  If supplied, the optional description will be included in the
       reporter help XML and display.  If supplied, default indicates that the
       argument is optional; the argValue method will return default if the
       command line does not include a value for the argument.  The optional
       pattern specifies a pattern for recognizing valid argument values; the
       default is '.*', which means that any text is acceptable for the
       argument value.
    """
    if pattern == None:
      pattern = '.*'
    self.args[name] = {
      'description' : description, 'default' : default, 'pat' : pattern
    }

  def addDependency(self, *dependencies):
    """Add one or more dependencies to the list of modules on which this
       reporter depends.  Dependencies are reported as part of reporter help
       output to assist reporter repository tools in their retrievals.  NOTE:
       dependencies on the standard Inca reporter library modules are added by
       the modules themselves, so a reporter only needs to invoke this method
       to report external dependencies.  The Inca Reporter Instance Manager
       presently only supports dependencies on Inca repository packages.
    """
    self.dependencies.extend(dependencies)

  def argValue(self, name, position=None):
    """Called after the processArgv method, this returns the value of the
       position'th instance (starting with 1) of the name command-line
       argument.  Returns the value of the last instance if position is None.
       Returns None if name is not a recognized argument.  Returns the default
       value for name if it has one and name is included fewer than position
       times on the command line.
    """
    if not self.args.has_key(name):
      sys.stderr.write(
        "'" + name + "' is not a valid command line argument name"
      );
      return None
    argv = self.argv[:]
    if position == None:
      argv.reverse()
      position = 1
    for arg in argv:
      found = re.match(name + '=(.*)', arg)
      if found:
        position -= 1
        if position < 1:
          return found.group(1)
    return self.args[name]['default']

  def argValues(self, name):
    """Called after the processArgv method, this returns an array of all values
       specified for the name command-line argument.  Returns None if name is
       not a recognized argument.  Returns a single-element array containing
       the default value for name if it has one and name does not appear on the
       command line.
    """
    if not self.args.has_key(name):
      sys.stderr.write(
        "'" + name + "' is not a valid command line argument name"
      );
      return None
    default = self.args[name]['default']
    result = []
    for arg in self.argv:
      found = re.match(name + '=(.*)', arg)
      if found:
        result.append(found.group(1))
    if len(result) == 0 and default != None:
      result.append(default)
    return result

  def compiledProgramOutput(self, **params):
    """A convenience; compiles and runs a program, removes the source and exec
       files, and returns the program's combined stderr/out output.  See
       compiledProgramStatusOutput for a list of recognized params.
    """
    (status, output) = self.compiledProgramStatusOutput(**params)
    return output

  def compiledProgramStatusOutput(self, **params):
    """A convenience; compiles and runs a program, removes the source and exec
       files,  and returns a tuple that contains the program's exit code and
       its combined stderr/out output.  Recognized params::

         code
           the code to compile; required

         compiler
           the compiler to invoke; defaults to cc

         language
           source file language--one of 'c', 'c++', 'fortran', or 'java';
           defaults to 'c'.

         out_switch
           the switch to use to specify the compiler output file; default '-o '

         switches
           additional switches to pass to the compiler; defaults to ''

         timeout
           max seconds compilation/execution may take; returns a non-zero exit
           status and any partial program output on time-out
    """
    code = params['code']
    compiler = 'cc'
    if params.has_key('compiler'):
      compiler = params['compiler']
    lang = 'c'
    if params.has_key('language'):
      lang = params['language']
    extension = 'c'
    if lang == 'c++':
      extension = 'C'
    elif lang == 'fortran':
      extension = 'f'
    elif lang == 'java':
      extension = 'java'
    prefix = 'src' + str(os.getpid())
    if lang == 'java':
      code = re.sub(r'class\s+\w+', 'class ' + prefix, code)
      if os.environ['CLASSPATH'] != None:
        os.environ['CLASSPATH'] += ':'
      os.environ['CLASSPATH'] += '.'
    path = prefix + '.' + extension
    output = open(path, 'w')
    if not output:
      return None
    output.write(code + '\n')
    output.close()
    out = '-o '
    if params.has_key('out_switch'):
      out = params['out_switch']
    switches = ''
    if params.has_key('switches'):
      switches = params['switches']
    timeout = None
    if params.has_key('timeout'):
      timeout = params['timeout']
    cmd = None
    if lang == 'java':
      cmd = '('+compiler+' '+path+' '+switches+' && java '+prefix+')'
    else:
      cmd='('+compiler+' '+path+' '+out+prefix+' '+switches+' && ./'+prefix+')'
    oldLd = None
    if os.environ.has_key('LD_LIBRARY_PATH'):
      oldLd = os.environ['LD_LIBRARY_PATH']
    if re.search(r'-L\s*\S+', switches):
      paths = re.findall(r'-L\s*(\S+)', switches)
      os.environ['LD_LIBRARY_PATH'] = string.join(paths, ':')
      if oldLd != None:
        os.environ['LD_LIBRARY_PATH'] += ':' + oldLd
    (status, output) = self.loggedCommandStatusOutput(cmd, timeout)
    if oldLd != None:
      os.environ['LD_LIBRARY_PATH'] = oldLd
    elif os.environ.has_key('LD_LIBRARY_PATH'):
      del os.environ['LD_LIBRARY_PATH']
    self.loggedCommandOutput('/bin/rm -f ' + prefix + '*')
    return (status, output)

  def failPrintAndExit(self, msg):
    """A convenience; calls setResult(0, msg) and printReport() before exiting
       the reporter.
    """
    self.setResult(0, msg)
    self.printReport()
    sys.exit(0)

  def getBody(self):
    """Returns the body of the report."""
    return self.body

  def getCompleted(self):
    """Returns the completion indicator of the reporter."""
    return self.completed

  def getCwd(self):
    """Returns the initial working directory of the reporter."""
    return self.cwd

  def getDescription(self):
    """Returns the initial working directory of the reporter."""
    return self.description

  def getFailMessage(self):
    """Returns the failure message of the reporter."""
    return self.fail_message

  def getName(self):
    """Returns the name that identifies this reporter."""
    return self.name

  def getUrl(self):
    """Returns the url which describes the reporter in more detail."""
    return self.url

  def getVersion(self):
    """Returns the version of the reporter."""
    return self.version

  def log(self, type, *msgs):
    """Appends each element of msgs to the list of type log messages stored
       in the reporter. type must be one of 'debug', 'error', 'info', 'system',
       or 'warn'."""
    if not re.search(self.log_pat, type):
      return
    for msg in msgs:
      if self.argValue('verbose') == '0':
        sys.stderr.write(type + ': ' + msg + '\n')
      self.log_entries.append({
        'type' : type, 'time' : time.time(), 'msg' : msg
      })

  def loggedCommandOutput(self, cmd, timeout=None):
    """A convenience; appends cmd to the 'system'-type log messages stored in
       the reporter, then runs cmd and returns its combined stderr/stdout.
       If timeout is specified and the command doesn't complete within timeout
       seconds, aborts the execution of cmd and returns any partial output.
    """
    (status, output) = self.loggedCommandStatusOutput(cmd, timeout)
    return output

  def loggedCommandStatusOutput(self, cmd, timeout=None):
    """A convenience; appends cmd to the 'system'-type log messages stored in
       the reporter, then runs cmd and returns a tuple that contains its exit
       code and combined stderr/stdout.  If timeout is specified and the
       command doesn't complete within timeout seconds, aborts the execution
       of cmd and returns a non-zero exit code and any partial output.
    """
    self.log('system', cmd)
    if timeout == None:
      (status, output) = commands.getstatusoutput(cmd)
      return (status, output + "\n")
    # fork a child to run the command, sending stderr/out through a pipe.  Set
    # the pgrp of the child so that we can kill it and any processes it spawns.
    (readfd, writefd) = os.pipe();
    childPid = os.fork()
    if childPid == 0:
      os.close(readfd)
      os.dup2(writefd, 1)
      os.dup2(writefd, 2)
      os.setpgrp()
      os.execl('/bin/sh', '/bin/sh', '-c', cmd)
      os.exit(1)
    os.close(writefd)
    readFile = os.fdopen(readfd, 'r')
    timedOut = False
    # Install an alarm handler to interrupt reading the pipe/raise an exception.
    oldHandler = signal.signal(signal.SIGALRM, self._timeoutException)
    output = '';
    signal.alarm(int(timeout))
    try:
      line = readFile.readline()
      while line:
        output += line
        line = readFile.readline()
    except SystemExit:
      timedOut = True
    signal.alarm(0)
    if timedOut:
      os.killpg(childPid, 9)
    (childPid, status) = os.waitpid(childPid, 0)
    status = os.WEXITSTATUS(status)
    if timedOut:
       status = 1
    signal.signal(signal.SIGALRM, oldHandler)
    readFile.close()
    return (status, output)

  def printReport(self, verbose=None):
    """A convenience; prints report(verbose) to stdout."""
    print self.report(verbose) + "\n"

  def processArgv(self, argv):
    """Processes argv which is a list of command-line arguments of the form
       -name=value

       The following options are predefined::

         help
           yes
             Prints help information describing the reporter inputs, then
             forces the reporter to exit.  If the verbose level is 0, the
             output will be text; otherwise, it will be Inca Report XML.
           no (default)
             Normal reporter execution.

         log
           0 (default)
             log no messages
           1
             log error messages
           2
             log error and warning messages
           3
             log error, warning, and system messages
           4
             log error, warning, system, and info messages
           5
             log error, warning, system, info, and debug messages
           debug
             log only debug messages
           error
             log only error messages
           info
             log only info messages
           system
             log only system messages
           warn
             log only warning messages

         verbose
           0
             print will only produce "completed" or "failed".
           1 (default)
             print will produce Inca Report XML.
           2
             print will produce Inca Report XML that includes help information.

        version
          yes
            Prints the reporter version number and exits.
          no (default)
            Normal reporter execution.
    """
    if len(argv) == 1:
      # we have a single argument; check to see if the input is URL-style query
      # string, e.g.,  -file=test.pl&help=no&verbose=1
      argv = argv[0].split('&')
    elif len(argv) == 0 and os.environ.has_key('QUERY_STRING'):
      # maybe we're running as a CGI script
      argv = os.environ['QUERY_STRING'].split('&')

    argValues = []
    badArg = None
    missing = []
    patterns = []

    for arg in argv:
      pieces = arg.split('=')
      name = re.sub('^--?', '', pieces[0])
      if len(pieces) == 1:
        value = 'yes'
      else:
        value = pieces[1]
      if not self.args.has_key(name):
        badArg = "unknown argument '" + name + "'"
      elif not re.search(self.args[name]['pat'], value):
        badArg = "'" + value + "' is not a valid value for -" + name
      argValues.append(name + '=' + value)
    self.argv = argValues
    if badArg != None:
      self.failPrintAndExit(badArg)

    if self.argValue('help') != 'no':
      if self.argValue('verbose') == '0':
        description = self.getDescription()
        version = self.getVersion()
        if version == None:
          version = "No version"
        url = self.getUrl()
        if url == None:
          url = "No URL"
        text = ''
        usage = os.path.basename(sys.argv[0])
        args = self.args.keys()
        args.sort()
        for arg in args:
          argDefault = self.args[arg]['default']
          argDescription = self.args[arg]['description']
          text += ' -' + arg + '\n'
          if argDescription != None:
            text += '\t' + argDescription + '\n'
          usage += ' -' + arg
          if argDefault != None:
            usage += '=' + str(argDefault)
        print "NAME:\n  " + self.getName() + "\n" + \
              "VERSION:\n  " + str(version) + "\n" + \
              "URL:\n  " + url + "\n" + \
              "SYNOPSIS:\n  " + usage + "\n" + text + "\n"
      else:
        print self._reportXml(self._helpXml()) + "\n"
      sys.exit(0)
    if self.argValue('version') != 'no':
      print self.getName() + ' ' + str(self.getVersion()) + "\n"
      sys.exit(0)

    for arg in self.args.keys():
      if self.argValue(arg) == None:
        missing.append(arg)
    if len(missing) == 1:
      self.failPrintAndExit("Missing required argument '" + missing[0] + "'");
    elif len(missing) > 0:
      self.failPrintAndExit \
        ("Missing required arguments '" + string.join(missing, "', '") + "'");

    for arg in self.argValues('log'):
      if re.match('[012345]$', arg):
        allTypes = ['error', 'warn', 'system', 'info', 'debug']
        arg = string.join(allTypes[0:int(arg)], '|')
      patterns.append(arg)
    if len(patterns) > 0:
      self.log_pat = '^(' + string.join(patterns, '|') + ')$'

  def report(self, verbose=None):
    """Returns report text or XML, depending on the value (0, 1, 2) of verbose.
       Uses the value of the -verbose switch if verbose is None.
    """
    completed = self.getCompleted()
    msg = self.getFailMessage()
    if verbose == None:
      verbose = self.argValue('verbose')
    if verbose == '0':
      if completed:
        result = 'completed'
      else:
        result = 'failed'
      if msg != None:
        result += ': ' + msg
    else:
      if completed and self.getBody() == None:
        self.setBody(self.reportBody())
      if msg != None:
        messageXml = self.xmlElement('errorMessage', 1, msg)
      else:
        messageXml = None
      if completed:
        completed = 'true'
      else:
        completed = 'false'
      completedXml = self.xmlElement('completed', 1, completed)
      if verbose == '2':
        helpXml = self._helpXml()
      else:
        helpXml = None
      result = self._reportXml(
        self.xmlElement('body', 0, self.getBody()),
        self.xmlElement('exitStatus', 0, completedXml, messageXml),
        helpXml
      )
    return result
    
  def reportBody(self):
    """Constructs and returns the XML contents of the report body.  Child
       classes should override the default implementation, which returns None.
    """
    return None

  def setBody(self, body):
    """Sets the body of the report to body."""
    self.body = body

  def setCompleted(self, completed):
    """Sets the completion indicator of the reporter to completed."""
    self.completed = completed

  def setCwd(self, cwd):
    """Sets the initial working directory of the reporter to cwd."""
    self.cwd = cwd

  def setDescription(self, description):
    """Sets the description of the reporter to description."""
    self.description = description

  def setFailMessage(self, msg):
    """Sets the failure message of the reporter to msg."""
    self.fail_message = msg

  def setName(self, name):
    """Sets the name that identifies this reporter to name."""
    self.name = name

  def setResult(self, completed, msg=None):
    """A convenience; calls setCompleted(completed) and setFailMessage(msg)."""
    self.setCompleted(completed)
    self.setFailMessage(msg)

  def setUrl(self, url):
    """Sets the url for the reporter to url."""
    self.url = url

  def setVersion(self, version):
    """Sets the version of the reporter to version.  Recognizes and parses CVS
       revision strings.
    """
    if version == None:
      return
    found = re.search('Revision: (.*) ', version)
    if found:
      self.version = re.group(1)
    else:
      self.version = version

  def tempFile(self, *paths):
    """A convenience.  Adds each element of paths to a list of temporary files
       that will be deleted automatically when the reporter is destroyed.
    """
    self.temp_paths.extend(paths)

  def xmlElement(self, name, escape, *contents):
    """Returns the XML element name surrounding contents.  escape should be
       true only for leaf elements; in this case, each special XML character
       (<>&) in contents is replaced by the equivalent XML entity.
    """
    innards = ''
    for content in contents:
      if content == None:
        continue
      content = str(content)
      if escape:
        content = re.sub('&', '&amp;', content)
        content = re.sub('<', '&lt;', content)
        content = re.sub('>', '&gt;', content)
      content = re.sub('^<', '\n<', content, 1)
      innards += content
    innards = re.sub('(?m)^( *)<', r'  \1<', innards)
    innards = re.sub('>$', '>\n', innards, 1)
    return '<' + name + '>' + innards + '</' + name + '>'

  def __del__(self):
    """Class destructor."""
    if len(self.temp_paths) > 0:
      commands.getoutput(
        '/bin/rm -fr ' + string.join(self.temp_paths, ' ')
      )

  def _helpXml(self):
    """Returns help information formatted as the body of an Inca report."""
    argsAndDepsXml = []
    args = self.args.keys()
    args.sort()
    for arg in args:
      info = self.args[arg]
      defaultXml = info['default']
      if defaultXml != None:
        defaultXml = self.xmlElement('default', 1, defaultXml)
      description = info['description']
      if description == None:
        description = ''
      argsAndDepsXml.append(self.xmlElement('argDescription', 0,
        self.xmlElement('ID', 1, arg),
        self.xmlElement('accepted', 1, info['pat']),
        self.xmlElement('description', 1, description),
        defaultXml
      ))
    for dep in self.dependencies:
      argsAndDepsXml.append(
        self.xmlElement('dependency', 0, self.xmlElement('ID', 1, dep))
      )
    return self.xmlElement('help', 0,
      self.xmlElement('ID', 1, 'help'),
      self.xmlElement('name', 1, self.getName()),
      self.xmlElement('version', 1, str(self.getVersion())),
      self.xmlElement('description', 1, self.getDescription()),
      self.xmlElement('url', 1, self.getUrl()), \
      *argsAndDepsXml
    )

  def _iso8601Time(self, when):
    """Returns the UTC time for the time() return value when in ISO 8601
       format: CCMM-MM-DDTHH:MM:SSZ
    """
    t = time.gmtime(when)
    return '%04d-%02d-%02dT%02d:%02d:%02dZ' % \
           (t.tm_year, t.tm_mon, t.tm_mday, t.tm_hour, t.tm_min, t.tm_sec)

  def _reportXml(self, *contents):
    """Returns XML report beginning with the header and input sections plus any
       contents specified in the arguments.
    """
    argXmls = []
    logXmls = []
    hostname = socket.gethostname()
    if hostname.find('.') < 0:
      hostname = socket.getfqdn()

    tags = [
      self.xmlElement('gmt', 1, self._iso8601Time(time.time())),
      self.xmlElement('hostname', 1, hostname),
      self.xmlElement('name', 1, self.getName()),
      self.xmlElement('version', 1, str(self.getVersion())),
      self.xmlElement('workingDir', 1, self.getCwd()),
      self.xmlElement('reporterPath', 1, sys.argv[0])
    ]
    args = self.args.keys()
    args.sort()
    for arg in args:
      for value in self.argValues(arg):
        argXmls.append \
          (self.xmlElement('arg', 0, self.xmlElement('name', 1, arg), \
                                     self.xmlElement('value', 1, value)))
    tags.append(self.xmlElement('args', 0, *argXmls))
    for entry in self.log_entries:
      logXmls.append(self.xmlElement(entry['type'], 0,
          self.xmlElement('gmt', 1, self._iso8601Time(entry['time'])),
          self.xmlElement('message', 1, entry['msg'])
      ))
    if len(logXmls) > 0:
      tags.append(self.xmlElement('log', 0, *logXmls))
    tags.extend(contents)
    result = self.xmlElement('rep:report', 0, *tags)
    result = re.sub('<rep:report', "<rep:report xmlns:rep='http://inca.sdsc.edu/dataModel/report_2.1'", result, 1)
    return "<?xml version='1.0'?>\n" + result

  def _timeoutException(*args):
    """SIGALRM handler that throws an exception."""
    raise SystemExit(1)
