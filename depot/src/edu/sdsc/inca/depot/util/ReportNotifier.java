/*
 * ReportNotifier.java
 */
package edu.sdsc.inca.depot.util;


import edu.sdsc.inca.dataModel.util.Report;
import edu.sdsc.inca.depot.persistent.InstanceInfo;
import edu.sdsc.inca.depot.persistent.Series;


/**
 *
 * @author Paul Hoover
 *
 */
public interface ReportNotifier {

  /**
   *
   * @param command
   * @param report
   * @param series
   * @param instance
   * @throws Exception
   */
  void notify(String command, Report report, Series series, InstanceInfo instance) throws Exception;
}
