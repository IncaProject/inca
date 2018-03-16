package edu.sdsc.inca.depot.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.log4j.Logger;

/**
 *
 * 1) Writes pass/fail results to summary.properties file for any reports with
 * the "summary.successpct.performance" reporter name.
 *
 * 2) Prefixes error messages in depot reports with "NOT_AT_FAULT: " if the
 * resource the report ran on is not at fault for the all2all error and the
 * report is not already prefixed with "DOWNTIME".  Resources are determined to
 * not be at fault if the summary property that matches their nickname
 * has failed.
 *
 */
public class All2AllFilter extends edu.sdsc.inca.depot.util.ReportFilter {

  private static Logger logger = Logger.getLogger(All2AllFilter.class);
  private static String summaryFilename = null;
  private static final Pattern all2allPattern =
    Pattern.compile("nickname=all2all:(.*_to_.[^\\s]*)");
  private static final Pattern downPattern =
    Pattern.compile(".*<errorMessage>DOWNTIME:.*",Pattern.MULTILINE);
  private static final Pattern successPattern =
    Pattern.compile("<ID>all2all:(.*?)-success</ID>\\s*<value>(.*?)</value>");

  static {
    summaryFilename = System.getProperty("inca.summaryFile");
    if(summaryFilename == null) {
      summaryFilename  = "summary.properties";
    }
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    URL url = cl.getResource(summaryFilename);
    if(url != null) {
      summaryFilename = url.getFile();
    } else if(!new File(summaryFilename).exists()) {
      try {
        new File("etc/" + summaryFilename).createNewFile();
        logger.info("Creating " + summaryFilename);
        summaryFilename = cl.getResource(summaryFilename).getFile();
      } catch(Exception e){
        logger.error("Can't create " + summaryFilename + ": " + e);
      }
    }
  }

  /**
   * Returns property list of summary.successpct.performance report status
   * from file in classpath (summary.properties by default)
   *
   * Example of property list file contents:
   *
   *  gridftp_to_gridftp.bigben.psc.teragrid.org=0.75
   *   (some can reach this resource)
   *
   *  gridftp_to_gridftp.bigben.psc.teragrid.org=0
   *    (num over threshold can't reach)
   *
   */
  synchronized static Properties getSummaries()  {
    Properties summaries = new Properties();
    if(summaryFilename != null) {
      try {
        summaries.load(new FileInputStream(summaryFilename));
      } catch(IOException e) {
        logger.error("Can't load properties file:" + e);
      }
    }
    return summaries;
  }

  /**
   * Write value to property list of summary.successpct.performance report
   * status from file in classpath (summary.properties by default)
   */
  synchronized static void writeSummaries(Properties props)  {
    if(summaryFilename == null) {
      return;
    }
    try {
      props.store(new FileOutputStream(summaryFilename), null);
    } catch (IOException e) {
      logger.error("Can't write to file: " + e);
    }
  }

  /**
   * Writes results to properties file if name is summary.successpct.performance
   *
   * Writes new report with modified error message to depot if not at fault.
   *
   * @return  string with depot report (reporter Stdout)
   */
  public String getStdout() {
    Matcher isA2A = all2allPattern.matcher(super.getContext());
    Matcher isDown = downPattern.matcher(super.getStdout());
    String stdout = super.getStdout();
    if(!isA2A.find() || isDown.find()) {
      return stdout;
    }
    String nickname = isA2A.group(1);
    logger.debug("IS ALL2ALL: " + nickname);
    if(super.getContext().matches(".*summary.successpct.performance.*")) {
      // This is the special summary.successpct.performance report
      Matcher m = successPattern.matcher(super.getStdout());
      Properties summaries = getSummaries();
      while(m.find()) {
        nickname = m.group(1);
        String numSuccesses = m.group(2);
        Matcher failMatch = (Pattern.compile(
            "<ID>all2all:"+nickname+"-fail</ID>\\s*<value>(.*?)</value>"
        )).matcher(super.getStdout());
        String numFailures = null;
        while (failMatch.find()){
          numFailures = failMatch.group(1);
        }
        int total = Integer.valueOf(numFailures) + Integer.valueOf(numSuccesses);
        // Since we consider a resource to not be at fault if there are less
        // than two successes we need at least two results for a summary
        if (total > 1){
          logger.debug("IS SUMMARY ALL2ALL: " + nickname + "="
              + numSuccesses + "/" + total);
          summaries.setProperty(nickname, numSuccesses);
        }
      }
      writeSummaries(summaries);
    } else {
      // This is an all2all reporter to compare to the summary results
      String summaryProp = getSummaries().getProperty(nickname);
      logger.debug("SUMMARY STATUS " + nickname + ": " + summaryProp);
      if(summaryProp != null && Integer.valueOf(summaryProp) < 2) {
        logger.debug( super.getResource() + " NOT AT FAULT " + nickname );
        stdout = stdout.replaceFirst
          ("<errorMessage>", "<errorMessage>NOT_AT_FAULT: ");
      } else {
        logger.debug( super.getResource() + " AT FAULT " + nickname );
      }
    }
    return stdout;
  }

}
