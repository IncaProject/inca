import os
import re
import socket
import sys
import unittest

from inca.Reporter import Reporter

class TestReporter(unittest.TestCase):

  body = '<stuff><ID>a</ID></stuff>'
  description = 'documentation'
  failMessage = 'Something went wrong'
  name = os.path.basename(sys.argv[0])
  url = 'www.blue.ufo.edu'
  version = '0.5beta'

  def testConstructor(self):
    # constructor with no params
    reporter = Reporter()
    self.assert_(reporter != None, 'new')
    self.assertEquals(reporter.getBody(), None)
    self.assertEquals(reporter.getDescription(), None)
    self.assertEquals(reporter.getFailMessage(), None)
    self.assertEquals(reporter.getCompleted(), False)
    self.assertEquals(reporter.getUrl(), None)
    self.assertEquals(reporter.getVersion(), '0')

  def testGetSet(self):
    reporter = Reporter()
    reporter.setBody(self.body)
    reporter.setDescription(self.description)
    reporter.setFailMessage(self.failMessage)
    reporter.setCompleted(1)
    reporter.setUrl(self.url)
    reporter.setVersion(self.version)
    self.assertEquals(reporter.getBody(), self.body)
    self.assertEquals(reporter.getDescription(), self.description)
    self.assertEquals(reporter.getFailMessage(), self.failMessage)
    self.assert_(reporter.getCompleted())
    self.assertEquals(reporter.getUrl(), self.url)
    self.assertEquals(reporter.getVersion(), self.version)
    # set to undef; name, version should reject
    reporter.setBody(None)
    reporter.setDescription(None)
    reporter.setFailMessage(None)
    reporter.setCompleted(None)
    reporter.setUrl(None)
    reporter.setVersion(None)
    self.assertEquals(reporter.getBody(), None)
    self.assertEquals(reporter.getDescription(), None)
    self.assertEquals(reporter.getFailMessage(), None)
    self.assert_(not reporter.getCompleted())
    self.assertEquals(reporter.getUrl(), None)
    self.assertEquals(reporter.getVersion(), self.version)
    # setResult convenience method
    reporter.setResult(1, self.failMessage)
    self.assertEquals(reporter.getCompleted(), True)
    self.assertEquals(reporter.getFailMessage(), self.failMessage)

  def testConstructorWithParams(self):
    reporter = Reporter(
      body = self.body,
      completed = 1,
      description = self.description,
      fail_message = self.failMessage,
      url = self.url,
      version = self.version
    )
    self.assertEquals(reporter.getBody(), self.body)
    self.assertEquals(reporter.getDescription(), self.description)
    self.assertEquals(reporter.getFailMessage(), self.failMessage)
    self.assert_(reporter.getCompleted())
    self.assertEquals(reporter.getUrl(), self.url)
    self.assertEquals(reporter.getVersion(), self.version)

  def testReportXml(self):
    reporter = Reporter(
      body = self.body,
      completed = 1,
      description = self.description,
      url = self.url,
      version = self.version
    )
    self.assertEquals(reporter.report('0'), 'completed')
    report = reporter.report('1')
    host = socket.gethostname()
    if host.find('.') < 0:
      host = socket.getfqdn()
    self.assert_(re.search('<body>\s*' + self.body + '\s*</body>', report))
    self.assert_(re.search('<completed>true</completed>', report))
    self.assert_(re.search('<hostname>' + host + '</hostname>', report))
    self.assert_(re.search('<name>\s*' + self.name + '\s*</name>', report))
    self.assert_(re.search('<version>' + self.version + '</version>', report))

  def testArgvParsing(self):
    reporter = Reporter(
      body = self.body,
      completed = 1,
      description = self.description,
      url = self.url,
      version = self.version
    )

    argv = []
    reporter.addArg('hasdef', 'has default', 'def')
    reporter.addArg('hasdef2', 'also has default', 'def2')
    reporter.processArgv(argv)
    self.assertEquals(reporter.argValue('hasdef'), 'def')
    self.assertEquals(reporter.argValue('hasdef2'), 'def2')
    self.assertEquals(reporter.argValue('help'), 'no')
    self.assertEquals(reporter.argValue('version'), 'no')
    self.assertEquals(reporter.argValue('verbose'), '1')

    reporter.addArg('nodef', 'no default')
    argv = (
      'nodef=1', 'hasdef=2', 'hasdef=3', 'nodef=4', 'hasdef=5', 'verbose=1'
    )
    reporter.processArgv(argv)
    self.assertEquals(reporter.argValue('hasdef'), '5')
    self.assertEquals(reporter.argValue('help'), 'no')
    self.assertEquals(reporter.argValue('nodef'), '4')
    self.assertEquals(reporter.argValue('version'), 'no')
    self.assertEquals(reporter.argValue('verbose'), '1')
    self.assertEquals(reporter.argValue('hasdef', 1), '2')
    self.assertEquals(reporter.argValue('hasdef', 2), '3')
    self.assertEquals(reporter.argValue('hasdef', 3), '5')
    self.assertEquals(reporter.argValue('hasdef', 4), 'def')
    self.assertEquals(reporter.argValue('hasdef', 5), 'def')
    self.assertEquals(reporter.argValue('hasdef2', 1), 'def2')
    self.assertEquals(reporter.argValue('nodef', 1), '1')
    self.assertEquals(reporter.argValue('nodef', 2), '4')
    self.assertEquals(reporter.argValue('nodef', 3), None)

    hasdefVals = reporter.argValues('hasdef')
    hasdef2Vals = reporter.argValues('hasdef2')
    nodefVals = reporter.argValues('nodef')
    self.assertEquals(len(hasdefVals), 3)
    self.assertEquals(len(hasdef2Vals), 1)
    self.assertEquals(len(nodefVals), 2)
    self.assertEquals(hasdefVals[0], '2')
    self.assertEquals(hasdefVals[1], '3')
    self.assertEquals(hasdefVals[2], '5')
    self.assertEquals(hasdef2Vals[0], 'def2')
    self.assertEquals(nodefVals[0], '1')
    self.assertEquals(nodefVals[1], '4')

  def testCompiledProgramOutput(self):
    reporter = Reporter()
    str = 'Test output'
    cProg = '''
#include <stdio.h>
int main(int argc, char **argv) {
  printf("''' + str + '''\\n");
  return 0;
}
'''
    output = reporter.compiledProgramOutput(code = cProg)
    self.assertEquals(output, str + "\n")
    (status, output) = reporter.compiledProgramStatusOutput(code = cProg)
    self.assertEquals(status, 0)
    self.assertEquals(output, str + "\n")

  def testXmlElement(self):
    reporter = Reporter()
    xml = reporter.xmlElement('tag', 0)
    self.assert_(xml == '<tag/>' or xml == '<tag></tag>')
    xml = reporter.xmlElement('tag', 0, 'ABC<>&<>&ABC')
    self.assertEquals(xml, "<tag>ABC<>&<>&ABC</tag>")
    xml = reporter.xmlElement('tag', 1, 'ABC<>&<>&ABC')
    self.assertEquals(xml, "<tag>ABC&lt;&gt;&amp;&lt;&gt;&amp;ABC</tag>")

  def testLoggedCommandOutput(self):
    reporter = Reporter()
    output = reporter.loggedCommandOutput('sleep 5 && echo hello')
    self.assertEquals(output, "hello\n")
    (status, output) = \
      reporter.loggedCommandStatusOutput('sleep 5 && echo hello', 2)
    self.assert_(output != None)
    self.assert_(status != 0)
    output = reporter.loggedCommandOutput('echo hello && echo hello', 20)
    self.assertEquals("hello\nhello\n", output)
    output = reporter.loggedCommandOutput(
      'echo hello && echo hello && sleep 30 && echo hello', 10
    )
    self.assertEquals("hello\nhello\n", output)

  def testTempFile(self):
    temp = open("tmp1", "w");
    temp = open("tmp2", "w");
    temp = open("tmp3", "w");
    os.mkdir("tmp4")
    temp = open("tmp4/tmp5", "w")
    temp.close()
    reporter = Reporter()
    reporter.tempFile("tmp1")
    reporter.tempFile("tmp2", "tmp3", "tmp4")
    reporter = None
    try:
      temp = open("tmp1", "r");
      self.fail("tmp1 exists");
    except IOError:
      pass
    try:
      temp = open("tmp2", "r");
      self.fail("tmp2 exists");
    except IOError:
      pass
    try:
      temp = open("tmp3", "r");
      self.fail("tmp3 exists");
    except IOError:
      pass
    try:
      temp = open("tmp4/tmp5", "r");
      self.fail("tmp4/tmp5 exists");
    except IOError:
      pass

if __name__ == '__main__':
  unittest.main()
