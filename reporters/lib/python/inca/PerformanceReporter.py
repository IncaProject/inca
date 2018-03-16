from inca.Reporter import Reporter

class PerformanceReporter(Reporter):
  """PerformanceReporter - Convenience module for performance-related
     reporters::

       from inca.PerformanceReporter import PerformanceReporter, PerformanceBenchmark
       performance = PerformanceReporter(
         name = 'My performance reporter',
         version = 1,
         description = 'Measures host performance',
         url = 'http://inca.sdsc.edu',
         measurement_name = 'host performance'
       )
       ...
       benchmark = PerformanceBenchmark()
       benchmark.addParameter('num_cpus', 16)
       benchmark.addStatistic('bandwidth', 10, 'Mb/s')
       performance.addBenchmark('sample', benchmark)
       reporter.printReport()

     Module for writing performance related reporters.  A performance reporter
     has one or more benchmarks.  Each benchmark has one or more statistics
     (i.e., results) and can further be described with one or more parameters.
     For example::

       <performance> 
         <ID>some_id</ID>
         <benchmark>
           <ID>sample</ID>
           <parameters>
             <ID>parameters</ID>
             <parameter>
               <ID>num_cpus</ID>
               <value>16</value>
             </parameter>
           </parameters>
           <statistics>
             <ID>statistics</ID>
             <statistic>
               <ID>bandwidth</ID>
               <value>10</value>
               <units>Mb/s</units>
             </statistic>
           </statistics>
         </benchmark>
       </performance>

     By default, the exit status of the reporter will be set to true (i.e.,
     success).
  """

  def __init__(self, **attrs):
    """Class constructor that returns a new PerformanceReporter object. The
       constructor supports the following parameter in addition to those
       supported by Reporter::
  
         measurement_name
           the name of the performance metric measured by the reporter;
           default ''.
    """
    name = ''
    if attrs.has_key('measurement_name'):
      name = attrs['measurement_name']
      del attrs['measurement_name']
    Reporter.__init__(self, **attrs)
    self.measurement_name = name
    self.benchmark = {}
    self.addDependency('inca.PerformanceReporter')

  def addBenchmark(self, name, benchmark):
    """Add a benchmark to the reporter.  benchmark is an object of type
       PerformanceBenchmark. name identifies the benchmark.
    """
    self.benchmark[name] = benchmark

  def addNewBenchmark(self, name):
    """A convenience that combines the allocation of a benchmark and its
       addition to the reporter.
    """
    benchmark = PerformanceBenchmark()
    self.addBenchmark(name, benchmark)
    return benchmark

  def getMeasurementName(self):
    """Returns the name of the performance metric measured by the reporter."""
    return self.measurement_name

  def reportBody(self):
    """Constructs and returns the body of the reporter."""
    idXml = self.xmlElement('ID', 1, self.getMeasurementName())
    benchmarkXmls = []
    paramsXml = None
    for name in self.benchmark.keys():
      benchmark = self.benchmark[name]
      bidXml = self.xmlElement('ID', 1, name)
      paramXmls = []
      statXmls = []
      params = benchmark.parameterNames()
      params.sort()
      for param in params:
        (value, units) = benchmark.getParameter(param)
        pidXml = self.xmlElement('ID', 1, param)
        valueXml = self.xmlElement('value', 1, value);
        unitsXml = None
        if units != None:
          unitsXml = self.xmlElement('units', 1, units)
        paramXmls.append(
          self.xmlElement('parameter', 0, pidXml, valueXml, unitsXml)
        )
      paramsXml = None
      if len(paramXmls) > 0:
        paramsXml = self.xmlElement('parameters', 0, *paramXmls)
      stats = benchmark.statisticNames()
      stats.sort()
      for stat in stats:
        (value, units) = benchmark.getStatistic(stat);
        sidXml = self.xmlElement('ID', 1, stat)
        valueXml = self.xmlElement('value', 1, value)
        unitsXml = None
        if units != None:
          unitsXml = self.xmlElement('units', 1, units)
        statXmls.append(
          self.xmlElement('statistic', 0, sidXml, valueXml, unitsXml)
        )
      statsXml = None
      if len(statXmls) > 0:
        statsXml = self.xmlElement('statistics', 0, *statXmls)
      benchmarkXmls.append(
        self.xmlElement('benchmark', 0, bidXml, paramsXml, statsXml)
      )
    return self.xmlElement('performance', 0, idXml, *benchmarkXmls);

  def setMeasurementName(self, name):
    """Sets the name of the performance metric measured by the reporter."""
    self.measurement_name = name

class PerformanceBenchmark:
  """PerformanceBenchmark - Convenience class for managing a benchmark entry

     Convenience class for the PerformanceReporter module.  Manages a single
     benchmark entry.
  """

  def __init__(self):
    """Class constructor that returns a new PerformanceBenchmark object."""
    self.parameters = {}
    self.statistics = {}

  def getParameter(self, name):
    if not self.parameters.has_key(name):
      return None
    param = self.parameters[name]
    return (param['value'], param['units'])

  def getStatistic(self, name):
    if not self.statistics.has_key(name):
      return None
    stat = self.statistics[name]
    return (stat['value'], stat['units'])

  def parameterNames(self):
    return self.parameters.keys()

  def setParameter(self, name, value, units=None):
    """Sets a parameter that was used to obtain the benchmark.  name is a
       unique identifier for this parameter and value its value.  The optional
       units describes a scalar value (e.g., Gb/s, secs, etc.).
    """
    self.parameters[name] = {'value' : value, 'units' : units}

  def setStatistic(self, name, value, units=None):
    """Sets a statistic that was obtained for the benchmark.  name is a unique
       identifier for this parameter and value its value.  The optional units
       describes a scalar value (e.g., Gb/s, secs, etc.).
    """
    self.statistics[name] = {'value' : value, 'units' : units}

  def statisticNames(self):
    return self.statistics.keys()
