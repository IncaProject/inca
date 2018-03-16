import os.path
import re

from inca.Reporter import Reporter

class VersionReporter(Reporter):
  """VersionReporter - Convenience module for creating version reporters::

       from inca.VersionReporter import VersionReporter
       reporter = VersionReporter()
       command = 'somecommand -version'
       pattern = '^version "(.*)"'
       ...
       reporter.setPackageName('packageX')
       reporter.setVersionByExecutable(command, pattern)
       reporter.printReporter()

       or

       reporter.setVersionByGptQuery('packageX')

       or

       reporter.setVersionByRpmQuery('packageX')

       or

       reporter->setPackageVersion('x.x.x')

       or

       for subpackage in subpackages:
         reporter.setSubpackageVersion(subpackage, version)

     This module is a subclass of Reporter that provides convenience methods
     for creating version reporters.  A version reporter reports this version
     information for a package in the following schema (i.e., this is the body
     of the Inca report)::

       <packageVersion>
         <ID>packageX</ID>
         <version>x.x.x</version>
       </packageVersion>

       or

       <packageVersion>
         <ID>packageX</ID>
         <subpackage>
           <ID>subpackageX</ID>
           <version>x.x.x</version>
         </subpackage>
         <subpackage>
           <ID>subpackageY</ID>
           <version>x.x.x</version>
         </subpackage>
       </packageVersion>

     Version information can be set using one of the basic methods
     setPackageVersion (for the first example) or setSubpackageVersion (for the
     second). In this case, the user retrieves a package's version information
     directly and uses one of these two methods to report it.  This module also
     provides convenience methods that retrieve a package version using
     conventional methods of querying version information.
  """

  def __init__(self, **attributes):
    """Class constructor that returns a new VersionReporter object.  The
       constructor supports the following parameters in addition to those
       supported by Reporter::

         package_name
           the name of the package for which a version is being determined;
           default ''.

         package_version
           the version of the package.
    """
    package_name = ''
    if attributes.has_key('package_name'):
      package_name = attributes['package_name']
      del attributes['package_name']
    package_version = None
    if attributes.has_key('package_version'):
      package_version = attributes['package_version']
      del attributes['package_version']
    Reporter.__init__(self, **attributes)
    self.package_name = package_name
    self.package_version = package_version
    self.subpackage_versions = {}
    self.addDependency('inca.VersionReporter')

  def getPackageName(self):
   """Returns the name of the package."""
   return self.package_name

  def getPackageVersion(self):
   """Returns the version of the package."""
   return self.package_version

  def getSubpackageNames(self):
   """Returns a list of all the names of all subpackages with a set version."""
   return self.subpackage_versions.keys()

  def getSubpackageVersion(self, name):
   """Returns the version of subpackage name."""
   result = None
   if self.subpackage_versions.has_key(name):
     result = self.subpackage_versions[name]
   return result

  def reportBody(self):
    """Constructs and returns the body of the reporter."""
    packageXml = [self.xmlElement('ID', 1, self.getPackageName())]
    if self.getCompleted():
      if self.getPackageVersion() != None:
        packageXml.append(
          self.xmlElement('version', 1, self.getPackageVersion())
        )
      subpackages = self.getSubpackageNames()
      subpackages.sort()
      for subpackage in subpackages:
        packageXml.append(self.xmlElement('subpackage', 0,
          self.xmlElement('ID', 1, subpackage),
          self.xmlElement('version', 1, self.getSubpackageVersion(subpackage))
        ))
    return self.xmlElement('package', 0, *packageXml)

  def setPackageName(self, name):
    """Set the name of the package."""
    self.package_name = name

  def setPackageVersion(self, version):
    """Report the version of a package as version."""
    self.package_version = version
    self.setCompleted(1)

  def setSubpackageVersion(self, name, version):
    """Report the version of subpackage name as version."""
    self.subpackage_versions[name] = version
    self.setCompleted(1)

  def setVersionByCompiledProgramOutput(self, **attrs):
    """Retrieve the package version by compiling and running a program and
       matching its output against a pattern.  Returns 1 if successful, else 0.
       The function recognizes the following parameter in addition to those
       supported by the compiledProgramOutput method of Reporter::

        pattern
          pattern to search for in program output; default '(.+)'
    """
    pattern = '(.+)'
    if attrs.has_key('pattern'):
      pattern = attrs['pattern']
      del attrs['pattern']
    output = self.compiledProgramOutput(**attrs)
    if not output:
      self.setCompleted(0)
      self.setFailMessage('program compilation/execution failed')
    else:
      found = re.search(pattern, output)
      if not found:
        self.setCompleted(0)
        self.setFailMessage("'" + pattern + "' not in '" + output + "'")
      else:
        self.setCompleted(1)
        self.setPackageVersion(found.group(1))
    return self.getCompleted()

  def setVersionByExecutable(self, command, pattern=None, timeout=None):
    """Retrieve package version information by executing command and greping
       the output for pattern.  command is the executable and argument string
       to retrieve the version (e.g., command_name -version) and pattern is a
       pattern containing one grouping (i.e., memory parentheses) to retrieve
       the version from the output.  pattern defaults to '([\d\.]+)' if not
       specified.  Fails if timeout is specified and command does not complete
       within timeout seconds.  Returns 1 if successful, else 0.
    """
    if pattern == None:
      pattern = r'([\d\.]+)'
    output = self.loggedCommandOutput(command, timeout)
    if not output or re.search('command not found', output):
      self.setCompleted(0)
      if not output:
        self.setFailMessage('')
      else:
        self.setFailMessage(output)
    else:
      found = re.search(pattern, output)
      if not found:
        self.setCompleted(0)
        self.setFailMessage("'" + pattern + "' not in '" + output + "'")
      else:
        version = None
        for group in found.groups():
          if group != None:
            version = group
        if version != None:
          self.setCompleted(1)
          self.setPackageVersion(version)
        else:
          self.setCompleted(0)
          self.setFailMessage("'" + pattern + "' not in '" + output + "'")
    return self.getCompleted()

  def setVersionByFileContents(self, path, pattern=None):
    """Retrieve the package version by grep'ing the file path for pattern.
       pattern defaults to '([\d\.]+)' if not specified.  Returns 1 if
       successful, else 0.
    """
    if pattern == None:
      pattern = r'([\d\.]+)'
    if not os.path.exists(path):
      self.setCompleted(0)
      self.setFailMessage("file '" + path + "' not present")
    else:
      input = open(path, 'r')
      if not input:
        self.setCompleted(0)
        self.setFailMessage("file '" + path + "' is not readable")
      else:
        line = input.readline()
        while line != None and not re.search(pattern, line):
          line = input.readline()
        input.close()
        if line == None:
          self.setCompleted(0)
          self.setFailMessage("'"+pattern+"' not found in file '"+path+"'")
        else:
          self.setCompleted(1)
          found = re.search(pattern, line)
          self.setPackageVersion(found.group(1))
    return self.getCompleted()

  def setVersionByGptQuery(self, *prefixes):
    """Set subpackage version information by querying GPT for packages prefixed
       with any element of prefixes.  Returns 1 if successful, else 0.
    """
    output = self.loggedCommandOutput('gpt-query')
    if not output:
      self.setResult(0, 'gpt-query failed')
      return 0
    pat = '^\s*(' + string.join(prefixes, '|') + ')'
    lastDefined = None
    for pkg in output.split('\n'):
      if not re.search(pat, pkg):
        continue
      (id, version) = re.search(r'^\s*([^-]+).*version:\s+(.*)$', pkg)
      if not version:
        continue
      self.setSubpackageVersion(id, version)
      lastDefined = id
    if lastDefined == None:
      self.setResult(0, 'package not installed (no GPT packages located)')
    else:
      self.setResult(1)
    return self.getCompleted()

  def setVersionByRpmQuery(self, pattern):
    """Set subpackage version information by querying GPT for packages that
       contain the regular expression pattern.  Returns 1 if successful, else 0.
    """
    rpmCommand = \
      "(rpm -qa --qf='%{NAME} version:%{VERSION}\\n' | " + \
      "grep '^[^ ]*" + pattern + "')"
    rpms = self.loggedCommandOutput(rpmCommand).split('\n');
    rpms.pop() # trim trailing empty element
    if len(rpms) == 0:
      self.setResult(0, 'no rpm packages found for ' + pattern)
      return 0
    for rpm in rpms:
      (subpackage, version) = rpm.split(' version:')
      self.setSubpackageVersion(subpackage, version)
    self.setResult(1)
    return 1;
