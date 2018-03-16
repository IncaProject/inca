import commands
import os
import re
import unittest

from inca.VersionReporter import VersionReporter

class TestVersion(unittest.TestCase):

  package = 'myPack'
  pat = r'V([\d\.]+)'
  version = '1.7.3'

  def testConstructor(self):
    reporter = VersionReporter()
    self.assert_(reporter != None)
    self.assertEquals(reporter.getPackageName(), '')
    self.assertEquals(reporter.getPackageVersion(), None)
    self.assertEquals(len(reporter.getSubpackageNames()), 0)

  def testGetSet(self):
    reporter = VersionReporter()
    reporter.setPackageName(self.package)
    reporter.setPackageVersion(self.version)
    self.assertEquals(reporter.getPackageName(), self.package)
    self.assertEquals(reporter.getPackageVersion(), self.version)

  def testConstructorWithParams(self):
    reporter = VersionReporter(
      package_name = self.package,
      package_version = self.version,
      completed = 1
    )
    self.assertEquals(reporter.getPackageName(), self.package)
    self.assertEquals(reporter.getPackageVersion(), self.version)

  def testReportBody(self):
    reporter = VersionReporter(
      package_name = self.package,
      package_version = self.version,
      completed = 1
    )
    body = reporter.reportBody()
    self.assert_(re.search('<ID>' + self.package + '</ID>\s*<version>' +
                           self.version + '</version>', body))

  def testSubpackages(self):
    reporter = VersionReporter(
      package_name = self.package,
      completed = 1
    )
    for i in ('0', '1', '2', '3', '4'):
      reporter.setSubpackageVersion("sub" + i, i)
    self.assertEquals(len(reporter.getSubpackageNames()), 5)
    self.assertEquals(reporter.getSubpackageVersion('sub3'), '3')
    body = reporter.reportBody()
    allThere = True
    for i in ('0', '1', '2', '3', '4'):
      if not re.search('<ID>sub' + i + '</ID>\s*<version>' + i + '</version>',
                       body):
        allThere = False
    self.assert_(allThere)

  def testSetVersionByCompiledProgramOutput(self):
    reporter = VersionReporter(package_name = self.package)
    cCode = '''
#include <stdio.h>
int main(int argc, char **argv) {
  printf("V''' + self.version + '''\\n");
}
'''
    reporter.setVersionByCompiledProgramOutput(code = cCode, pattern = self.pat)
    self.assertEquals(reporter.getPackageVersion(), self.version)

  def testSetVersionByExecutable(self):
    reporter = VersionReporter(package_name = self.package)
    reporter.setVersionByExecutable(
      "/bin/sh -c 'echo V" + self.version + "'", self.pat
    )
    self.assertEquals(reporter.getPackageVersion(), self.version)

  def testSetVersionByFileContents(self):
    reporter = VersionReporter(package_name = self.package)
    path = "/tmp/vrtest" + str(os.getpid())
    try:
      f = open(path, "w")
      for i in range(0, 199):
        f.write(str(i) + "\n")
        if i == 100:
          f.write("  V" + self.version + " XYZ\n")
      f.close()
    except IOError:
      return # Can't write to /tmp?  skip test
    reporter.setVersionByFileContents(path, self.pat)
    os.unlink(path)
    self.assertEquals(reporter.getPackageVersion(), self.version)

  def testSetVersionByGptQuery(self):
    reporter = VersionReporter(package_name = self.package)
    (status, output) = commands.getstatusoutput('gpt-query 2>&1')
    if status != 0 or output.find('version:') < 0:
      return # skip test; gpt not installed
    else:
      packs = output.split('\n')
      i = 0
      while packs[i].find('version:') < 0:
        i += 1
      found = re.match('\s*([^-]+).*version:\s+(.*)$', packs[i])
      (subpackage, version) = (found.groups(1), found.groups(2))
      reporter.setVersionByGptQuery(subpackage)
      self.assertEquals(reporter.getSubpackageVersion(subpackage), version)

  def testSetVersionbyRpmQuery(self):
    reporter = VersionReporter(package_name = self.package)
    (status, output) = \
      commands.getstatusoutput("rpm -qa --qf='%{NAME} version:%{VERSION}\\n'")
    if status != 0 or output.find(' version:\d+\.\d+') < 0:
      return # skip test; rpm not installed
    else:
      packs = output.split('\n')
      i = 0
      while packs[i].find(' version:\d+\.\d+') < 0:
        i += 1
      (subpackage, version) = packs[i].split(' version:', 2)
      reporter.setVersionByRpmQuery(subpackage)
      self.assertEquals(reporter.getSubpackageVersion(subpackage), version)

if __name__ == '__main__':
  unittest.main()
