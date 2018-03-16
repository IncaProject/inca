import re
import unittest

from inca.SimpleUnitReporter import SimpleUnitReporter

class TestSimpleUnit(unittest.TestCase):

  msg = 'I failed'
  unit = 'myUnit'

  def testConstructor(self):
    reporter = SimpleUnitReporter()
    self.assert_(reporter != None)
    self.assertEquals(reporter.getUnitName(), '')

  def testGetSet(self):
    reporter = SimpleUnitReporter()
    reporter.setUnitName(self.unit)
    self.assertEquals(reporter.getUnitName(), self.unit)

  def testConstructorWithParams(self):
    reporter = SimpleUnitReporter(
      unit_name = self.unit
    )
    self.assertEquals(reporter.getUnitName(), self.unit)

  def testUnitFailureSuccess(self):
    reporter = SimpleUnitReporter(
      unit_name = self.unit
    )
    reporter.unitSuccess()
    self.assert_(reporter.getCompleted())
    reporter.unitFailure(self.msg)
    self.assert_(not reporter.getCompleted())
    self.assertEquals(reporter.getFailMessage(), self.msg)
    reporter.unitSuccess()
    body = reporter.reportBody()
    self.assert_(re.search('<ID>' + self.unit + '</ID>', body))

if __name__ == '__main__':
  unittest.main()
