/*
 * ScriptNotifier.java
 */
package edu.sdsc.inca.depot.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Enumeration;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

import edu.sdsc.inca.dataModel.util.Report;
import edu.sdsc.inca.depot.persistent.AcceptedOutput;
import edu.sdsc.inca.depot.persistent.ComparisonResult;
import edu.sdsc.inca.depot.persistent.ComparisonResultDAO;
import edu.sdsc.inca.depot.persistent.DAO;
import edu.sdsc.inca.depot.persistent.InstanceInfo;
import edu.sdsc.inca.depot.persistent.Notification;
import edu.sdsc.inca.depot.persistent.PersistentObject;
import edu.sdsc.inca.depot.persistent.ReportDAO;
import edu.sdsc.inca.depot.persistent.RunInfo;
import edu.sdsc.inca.depot.persistent.Schedule;
import edu.sdsc.inca.depot.persistent.Series;
import edu.sdsc.inca.depot.persistent.SeriesConfig;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.util.XmlWrapper;


/**
 * A class that notifies a target script about a state change in
 * AcceptableOutput.
 *
 * @author Jim Hayes
 * @author Paul Hoover
 */
public class ScriptNotifier implements ReportNotifier {

  private static final Logger logger = Logger.getLogger(ScriptNotifier.class);


  // public methods


  /**
   *
   * @param command
   * @param report
   * @param series
   * @param instance
   * @throws Exception
   */
  public void notify(String command, Report report, Series series, InstanceInfo instance) throws Exception
  {
    for (SeriesConfig dbSc : series.getSeriesConfigs()) {
      if (dbSc.getDeactivated() != null)
        continue;

      if (!command.equals(Protocol.INSERT_COMMAND)) {
        Long latestInstanceId = dbSc.getLatestInstanceId();

        if (latestInstanceId >= 0) {
          InstanceInfo latestInstance = new InstanceInfo(series, latestInstanceId);

          if (latestInstance.getCollected().after(instance.getCollected()))
            continue;
        }
      }

      AcceptedOutput ao = dbSc.getAcceptedOutput();

      if (ao == null)
        continue;

      Notification dbN = ao.getNotification();

      if (dbN == null)
        continue;

      String notifier = dbN.getNotifier();

      if (notifier == null || notifier.length() < 1 || notifier.equals(PersistentObject.DB_EMPTY_STRING))
        continue;

      String comparitor = ao.getComparitor();

      if (comparitor == null || comparitor.length() < 1 || comparitor.equals(PersistentObject.DB_EMPTY_STRING))
        continue;

      edu.sdsc.inca.depot.persistent.Report dbReport = ReportDAO.load(instance.getReportId());

      if (dbReport == null)
        continue;

      String result = (new ExprComparitor()).compare(ao, dbReport);
      ComparisonResult newCr = new ComparisonResult();

      newCr.setResult(result);
      newCr.setReportId(dbReport.getId());
      newCr.setSeriesConfigId(dbSc.getId());

      ComparisonResult dbCr = ComparisonResultDAO.loadOrSave(newCr);
      Long priorComparisonId = dbSc.getLatestComparisonId();

      if (dbCr.getId() == priorComparisonId)
        continue;

      ComparisonResult priorCr = ComparisonResultDAO.load(priorComparisonId);

      if (priorCr == null && dbSc.getNickname() != null) {
        // Test for the latest comparison result from an equivalent series
        String dbResource = dbSc.getSeries().getResource();
        String query =
            "SELECT sc " +
            "FROM SeriesConfig AS sc " +
            "WHERE sc.nickname = :p0 " +
              "AND sc.id != " + dbSc.getId();
        Iterator<?> scs = DAO.selectMultiple(query, new Object[] {dbSc.getNickname()});

        while (scs.hasNext()) {
          SeriesConfig likeSc = (SeriesConfig)scs.next();

          if (!dbResource.equals(likeSc.getSeries().getResource()))
            continue;

          ComparisonResult likeCr = ComparisonResultDAO.load(likeSc.getLatestComparisonId());

          if (likeCr != null)
            priorCr = likeCr;
        }
      }

      if (priorCr != null && result.equals(priorCr.getResult()))
        continue;

      Properties props = new Properties();

      props.setProperty("reportId", dbReport.getId() + "");
      props.setProperty("completed", dbReport.getExit_status() + "");

      String field = dbReport.getExit_message();

      if (field != null && field.length() > 0 && !field.equals(PersistentObject.DB_EMPTY_STRING))
        props.setProperty("errorMessage", field);

      props.setProperty("body", dbReport.getBody());

      field = dbReport.getStderr();

      if (field != null && field.length() > 0 && !field.equals(PersistentObject.DB_EMPTY_STRING))
        props.setProperty("stderr", dbReport.getStderr());

      props.setProperty("instanceId", instance.getId() + "");
      props.setProperty("collected", printXmlDate(instance.getCollected()));
      props.setProperty("commited", printXmlDate(instance.getCommited()));
      props.setProperty("memoryUsage", instance.getMemoryUsageMB() + "");
      props.setProperty("cpuUsage", instance.getCpuUsageSec() + "");
      props.setProperty("wallClockUsage", instance.getWallClockTimeSec() + "");

      field = instance.getLog();

      if (field != null && field.length() > 0 && !field.equals(PersistentObject.DB_EMPTY_STRING)) {
        // Transform log messages from XML format, e.g.,
        // <info><gmt>time</gmt><message>text</message></info>
        // to more readable (info time) text
        field = field.replaceAll("</?xml-fragment[^>]*>", "").replaceAll("(?s)<(\\w+)[^>]*>.*?<gmt[^>]*>([^<]+)</gmt>.*?<message[^>]*>([^<]*)</message>.*?</\\w+>", "($1 $2) $3");

        props.setProperty("log", "\n" + XmlWrapper.unescape(field));
      }

      RunInfo dbRunInfo = dbReport.getRunInfo();

      props.setProperty("runInfoId", dbRunInfo.getId() + "");
      props.setProperty("hostname", dbRunInfo.getHostname());
      props.setProperty("workingDir", dbRunInfo.getWorkingDir());
      props.setProperty("reporterPath", dbRunInfo.getReporterPath());
      props.setProperty("seriesId", series.getId() + "");
      props.setProperty("reporter", series.getReporter());
      props.setProperty("version", series.getVersion());
      props.setProperty("uri", series.getUri());
      props.setProperty("context", series.getContext());
      props.setProperty("nice", series.getNice() + "");
      props.setProperty("resource", series.getResource());
      props.setProperty("targetHostname", series.getTargetHostname());
      props.setProperty("args", series.getArgSignature() + "");
      props.setProperty("configId", dbSc.getId() + "");
      props.setProperty("activated", printXmlDate(dbSc.getActivated()));

      Date deactivated = dbSc.getDeactivated();

      if (deactivated != null)
        props.setProperty("deactivated", printXmlDate(deactivated));

      field = dbSc.getNickname();

      if (field != null && field.length() > 0 && !field.equals(PersistentObject.DB_EMPTY_STRING))
        props.setProperty("nickname", field);

      if (dbSc.getLimits() != null) {
        props.setProperty("memoryLimit", dbSc.getLimits().getMemory() + "");
        props.setProperty("cpuLimit", dbSc.getLimits().getCpuTime() + "");
        props.setProperty("wallClockLimit", dbSc.getLimits().getWallClockTime() + "");
      }

      Schedule sched = dbSc.getSchedule();

      if (sched != null)
        props.setProperty("schedule", sched.toString());

      props.setProperty("comparitor", ao.getComparitor());
      props.setProperty("comparison", ao.getComparison());
      props.setProperty("notifier", dbN.getNotifier());
      props.setProperty("target", dbN.getTarget());
      props.setProperty("comparisonId", dbCr.getId() + "");

      String res = dbCr.getResult();

      props.setProperty("comparisonResult", res);
      props.setProperty("result", res.startsWith(ExprComparitor.FAILURE_RESULT) ? "FAIL" : "PASS");

      // Strip any package prefix from notifier and toss "ScriptNotifier"
      // (holdovers from when notification was handled via Java classes).
      notifier = notifier.replaceFirst("edu\\.sdsc\\.inca\\..*\\.", "");

      String target = !notifier.equals("ScriptNotifier") ? notifier + " " : "";

      target += ao.getNotification().getTarget();

      doNotification(target, props);
    }
  }

  /**
   * Invokes a script, w/info about an AcceptedOutput state change in stdin.
   *
   * @param target the script to notify
   * @param props property values that describe the series and instance
   */
  public void doNotification(String target, Properties props) {
    String[] words = target.split("\\s+");
    String path = words[0];
    // If the script path isn't absolute, search for it in a specified set of
    // directories.
    if(path.indexOf("/") != 0) {
      String searchPath = null; // TODO pick up from property
      if(searchPath == null) {
        searchPath =
          "." + File.pathSeparator + "bin" + File.pathSeparator + "sbin";
      }
      String[] dirs = searchPath.split(File.pathSeparator);
      for(int i = 0; i < dirs.length; i++) {
        File f = new File(dirs[i] + File.separator + path);
        if(f.exists()) {
          path = f.getPath();
          break;
        }
      }
    }
    // Check to see if the script has the working dir as an ancestor; other
    // paths are disallowed for security.  Check cononical path for wd to avoid
    // potential problems with ../ in path.
    try {
      path = new File(path).getCanonicalPath();
      String workingDir =
        new File(System.getProperty("user.dir")).getCanonicalPath();
      if(!path.startsWith(workingDir)) {
        logger.error("Invalid script file path '" + path + "'");
        return;
      }
      words[0] = path;
    } catch(Exception e) {
      logger.error("Error examining script path: " + e);
      return;
    }
    // Store the values of props in environment variables
    String[] environment = new String[props.size()];
    Enumeration<Object> keys = props.keys();
    for(int i = 0; i < environment.length; i++) {
      String key = (String)keys.nextElement();
      environment[i] = "inca" + key + "=" + props.getProperty(key);
    }
    // Now run the script
    try {
      Process p = Runtime.getRuntime().exec(words, environment);
      p.waitFor();
      if(p.exitValue() != 0) {
        BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String stderr = "", line;
        while((line = reader.readLine()) != null) {
          stderr += line;
        }
        logger.error("Error executing script " + target + ": " + stderr);
      }
    } catch(InterruptedException e) {
      // empty
    } catch(IOException e) {
      logger.error("Error executing script " + target + ": " + e);
    }

  }


  // private methods


  /**
   *
   * @param datetime
   * @return
   */
  private String printXmlDate(Date datetime)
  {
    Calendar cal = Calendar.getInstance();

    cal.setTime(datetime);

    return DatatypeConverter.printDateTime(cal);
  }
}
