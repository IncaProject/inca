class GridProxyReporter:
  """GridProxyReporter - A pseudo-module indicating that a reporter requires a
     proxy credential in order to execute::

       from inca.Reporter import Reporter
       reporter = IncaReporter()
       reporter.addDependency('inca.GridProxyReporter')

     Declaring a dependency on this module is a signal to Inca's ReporterManager
     that a grid proxy credential needs to be initialized before the reporter
     can be run.
  """
