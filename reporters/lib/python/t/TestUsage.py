import re
import unittest

from inca.UsageReporter import UsageReporter

class TestUsage(unittest.TestCase):

  msg = 'I failed'

  def testConstructor(self):
    reporter = UsageReporter()
    self.assert_(reporter != None)
    self.assertEquals(len(reporter.getEntries()), 0)

  def testFailureSuccess(self):
    reporter = UsageReporter()
    reporter.success()
    self.assert_(reporter.getCompleted())
    reporter.fail(self.msg)
    self.assert_(not reporter.getCompleted())
    self.assertEquals(reporter.getFailMessage(), self.msg)

  def testReportBody(self):
    reporter = UsageReporter()
    reporter.success()
    body = reporter.reportBody()
    self.assert_(re.search(r'<usage/>|<usage>\s*</usage>', body))

  def testAddEntry(self):
    reporter = UsageReporter()
    reporter.success()
    reporter.addEntry(
      {'type' : 'anEntry',
       'name' : 'first',
       'stats' : {
         'height' : 11,
         'width' : 22
       }
      }
    )
    self.assertEquals(len(reporter.getEntries()), 1)

  def testBodyWithEntries(self):
    reporter = UsageReporter()
    reporter.success()
    reporter.addEntry(
      {'type' : 'anEntry',
       'name' : 'first',
       'stats' : {
         'height' : 11,
         'width' : 22
       }
      }
    )
    body = reporter.reportBody()
    self.assert_(
      re.search(
        r'<statistic>\s*<name>width</name>\s*<value>22</value>\s*</statistic>',
        body
      )
    )
    reporter.addEntry(
      {'type' : 'anEntry',
       'name' : 'second',
       'stats' : {
         'height' : 33,
         'width' : 44
       }
      }
    )
    reporter.addEntry(
      {'type' : 'anEntry',
       'name' : 'third',
       'stats' : {
         'height' : 55,
         'width' : 66
       }
      }
    )
    self.assertEquals(len(reporter.getEntries()), 3)
    body = reporter.reportBody()
    self.assert_(
      re.search(
        r'<statistic>\s*<name>width</name>\s*<value>44</value>\s*</statistic>',
        body
      ) and
      re.search(
        r'<statistic>\s*<name>width</name>\s*<value>66</value>\s*</statistic>',
        body
      )
    )

if __name__ == '__main__':
  unittest.main()
