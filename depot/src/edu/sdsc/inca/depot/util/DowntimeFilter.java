package edu.sdsc.inca.depot.util;

import org.apache.log4j.Logger;
import edu.sdsc.inca.util.CachedProperties;

/**
 * Prefixes error messages in depot reports with "DOWNTIME: +optionalString+: "
 * if the resource the report ran on is in downtime.   Resources are determined
 * to be in downtime if they are listed in a downtime properties file.  In order
 * to reduce overhead, the downtime properties file is retrieved and cached at
 * a refresh interval in the getDowntimes() method instead of being retrieved
 * for each filter instance.
 *
 * @author Kate Ericson &lt;kericson@sdsc.edu&gt;
 * @author Shava Smallen &lt;ssmallen@sdsc.edu&gt;
 */
public class DowntimeFilter extends edu.sdsc.inca.depot.util.ReportFilter {
  private static Logger logger = Logger.getLogger(DowntimeFilter.class);

  private static CachedProperties cacheDown =
      new CachedProperties("inca.depot.", "downtime", "15");

  /**
   * Writes new report with modified error message to depot if resource is down
   *
   * @return  string with depot report (reporter Stdout)
   */
  public String getStdout() {
    if (cacheDown.getProperties().isEmpty()){
      return super.getStdout();
    } else {
      logger.debug( "Target hostname is " + super.getTargetHostname() );
      logger.debug( "Resource is " + super.getResource() );

      String downtimeResource = super.getTargetHostname() == null ?
        super.getResource() : super.getTargetHostname();
      String resourceProp  =
          cacheDown.getProperties().getProperty(downtimeResource);
      if (resourceProp != null){
        logger.debug( downtimeResource + " is down " + resourceProp );
        return super.getStdout().replaceFirst(
            "<errorMessage>", "<errorMessage>DOWNTIME:"+ resourceProp +": ");
      } else{
        return super.getStdout();
      }
    }
  }

}
