from inca.Reporter import Reporter

class SimpleUnitReporter(Reporter):
  """SimpleUnitReporter - Module for creating simple unit reporters::

       from inca.SimpleUnitReporter import SimpleUnitReporter
       reporter = SimpleUnitReporter(
         name = 'Reporter Name',
         version = 0.1,
         description = 'A really helpful reporter description',
         url = 'http://url.to.more.reporter.info'
         unit_name = 'What this reporter tests'
       )

     This module is a subclass of Reporter that provides convenience methods
     for testing the successful operation of a software package.  If the test
     completes, the report body contains::

       <unitTest>
         <ID>unitX</ID>
       </unitTest>

     Otherwise, the exit_status of the report is set to false.
  """

  def __init__(self, **attributes):
    """Class constructor that returns a new SimpleUnitReporter object.  The
       constructor supports the following parameter in addition to those
       supported by Reporter::
  
         unit_name
           the name of the unit being tested; default ''.
    """
    unit_name = ''
    if attributes.has_key('unit_name'):
      unit_name = attributes['unit_name']
      del attributes['unit_name']
    Reporter.__init__(self, **attributes)
    self.unit_name = unit_name
    self.addDependency('inca.SimpleUnitReporter')

  def getUnitName(self):
    """Return the name of the unit being tested."""
    return self.unit_name

  def reportBody(self):
    """Constructs and returns the body of the reporter."""
    idXml = self.xmlElement('ID', 1, self.getUnitName())
    return self.xmlElement('unitTest', 0, idXml)

  def setUnitName(self, name):
    """Set the name of the unit being tested to name."""
    self.unit_name = name

  def unitFailure(self, msg):
    """Sets the result of this unit test to be failed with failure message
       msg.
    """
    self.setResult(0, msg)

  def unitSuccess(self):
    """Sets the result of this unit test to be successful."""
    self.setResult(1)
