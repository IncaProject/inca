from inca.Reporter import Reporter

class UsageReporter(Reporter):
  """Usage - Module for creating simple usage reports::

       from inca.UsageReporter import UsageReporter
       reporter = new UsageReporter(
         name = 'My Reporter',
         version = 0.1,
         description = 'What my reporter does',
         url = 'http://some.where/'
       )

     This module is a subclass of inca.Reporter that provides a simple schema
     for reporting usage data.  The reporter will return the following body if
     the reporter is successful::

       <usage>
         <entry>
           <type>type1</type>
           <name>foo</name>
           <statistics>
             <statistic>
               <name>count</name>
               <value>9</value>
             </statistic>
           </statistics>
         </entry>
         <entry>

         .
         .
         .

       </usage>
  """

  def __init__(self, **attributes):
    """Class constructor that returns a new Usage object.  The constructor
       accepts the parameters supported by inca.Reporter.
    """
    Reporter.__init__(self, **attributes)
    self.entries = []
    self.addDependency('inca.UsageReporter');

  def addEntry(self, entry):
    """This method is used to add an entry to the usage report.  It takes a
       dict containing the type and name of the entry as well as a dict of
       statistics.  For example::

         addEntry({
           'type' : 'foo',
           'name' : 'bar',
           'stats' : {
             'count' : 1,
             'blort' : 'baz'
           }
         })

       Will create an entry of type 'foo', with a name of 'bar' and two
       statistics, count with a value of 1 and 'blort' with a value of 'baz'.
    """
    self.entries.append(entry)

  def fail(self, error_msg):
    """This method is used to indicate that the reporter failed.  It takes a
       single arguement which is the error message which will be returned.
    """
    self.setResult(0, error_msg)

  def getEntries(self):
    """Returns a list of all entries that have been added to the reporter."""
    return self.entries

  def reportBody(self):
    """Constructs the body and returns it."""
    xml = []
    for entry in self.entries:
      xml.append(self.xmlElement('entry', 0, *self._reportEntry(entry)))
    return self.xmlElement('usage', 0, *xml)

  def success(self):
    """This method is called to indicate that the reporter has succeeded."""
    self.setResult(1)

  def _reportStatistics(self, stats):
    xml = []
    for stat in stats.keys():
      xml.append(self.xmlElement('statistic', 0,
        self.xmlElement('name', 1, stat),
        self.xmlElement('value', 1, stats[stat])
      ))
    return xml

  def _reportEntry(self, entry):
    return [
      self.xmlElement('type', 1, entry['type']),
      self.xmlElement('name', 1, entry['name']),
      self.xmlElement('statistics', 0, *self._reportStatistics(entry['stats']))
    ]
