package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.List;

import edu.sdsc.inca.depot.persistent.AcceptedOutput;
import edu.sdsc.inca.depot.persistent.HqlQuery;
import edu.sdsc.inca.depot.persistent.InstanceInfo;
import edu.sdsc.inca.depot.persistent.Notification;
import edu.sdsc.inca.depot.persistent.PersistenceException;
import edu.sdsc.inca.depot.persistent.PersistentTest;
import edu.sdsc.inca.depot.persistent.Report;
import edu.sdsc.inca.depot.persistent.Row;
import edu.sdsc.inca.depot.persistent.Series;
import edu.sdsc.inca.depot.persistent.SeriesConfig;
import edu.sdsc.inca.depot.persistent.Suite;
import edu.sdsc.inca.depot.util.ReportFilter;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;


/**
 * User: cmills
 * Date: Mar 24, 2005
 * Time: 3:25:08 PM
 * For now this class is just required to be able to have the following conversation
 * REPORT SP bogus.name CRLF
 * STDOUT SP {xml data} CRLF
 * SYSUSAGE SP ignored CRLF
 * <p/>
 * This implies that the depot will be able to injest a report that has no errors.
 */

public class InsertTest extends PersistentTest {

  public static final String CRLF = "\r\n";

  private String report
    (String resource, String context, String stdout, String stderr,
     boolean addSysusage) {
    String result = "REPORT " + resource + " " + context + CRLF;
    if(stderr != null) {
      result += "STDERR " + stderr + CRLF;
    }
    if(stdout != null) {
      result += "STDOUT " + stdout + CRLF;
    }
    if(addSysusage) {
      result += "SYSUSAGE cpu_secs=12\nwall_secs=13\nmemory_mb=14\n" + CRLF;
    }
    return result;
  }

  private String report(String resource, String context, String stdout) {
    return report(resource, context, stdout, null, true);
  }

  private String execHandler(MessageHandler handler, String request)
    throws Exception {
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    ProtocolReader reader = new ProtocolReader(new StringReader(request));
    handler.execute(reader, outBytes, null);
    return outBytes.toString();
  }

  private List<?> selectMultiple(String query, Object[] params) throws SQLException, PersistenceException {

    List<Object> result = (new HqlQuery(query)).select(params);

    return result;
  }

  public void testDuplicateInsert() throws Exception {

    Series s = Series.generate("localhost", "myreporter", 3);
    String report = s.generateReport();

    logger.debug("Insert the first");
    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));

    logger.debug("Insert the second");
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));

    // make sure that the report was put in the db
    Series dbSeries = Series.find(s);
    assertNotNull(dbSeries);
    Object[] reports = dbSeries.getReports().toArray();
    assertEquals(1, reports.length);

    // now try a duplicate w/a different timestamp
    String report2 = report.replaceFirst("<gmt>\\d+", "<gmt>2007");
    logger.debug("Insert the third");
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report2));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));

    logger.debug("Query the second");
    dbSeries = Series.find(s);
    assertNotNull(dbSeries);
    reports = dbSeries.getReports().toArray();
    assertEquals(1, reports.length);

    // we now should have three instances
    assertEquals(3, selectMultiple("select i from InstanceInfo as i where i.reportId = :p0", new Object[] {((Report)reports[0]).getId()}).size());

  }

  /** Tests reports for a SeriesConfig that includes a comparitor. */
  public void testWithComparitor() throws Exception {

    SuiteUpdateTest sut = new SuiteUpdateTest();
    Suite suite = Suite.generate("aSuite", 1);
    SeriesConfig sc = suite.getSeriesConfig(0);
    Series s = sc.getSeries();
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", "body !~ /GOOgi/");
    sc.setAcceptedOutput(ao);
    sut.updateSuite(suite);
    String report = s.generateReport();

    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);

    // See if a ComparisonResult was placed into the DB
    SeriesConfig dbSc =
      (SeriesConfig)(new HqlQuery("select sc from SeriesConfig as sc")).selectUnique();
    assertNotNull(dbSc);
    Long crId = dbSc.getLatestComparisonId();
    assertTrue(crId.longValue() >= 0);

    // An identical report should not trigger a second CR
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    dbSc =
      (SeriesConfig)(new HqlQuery("select sc from SeriesConfig as sc")).selectUnique();
    assertNotNull(dbSc);
    assertTrue(crId.equals(dbSc.getLatestComparisonId()));

    // But a different one should
    String report2 =
      report.replaceAll("<body[^>]*>.*</body>", "<body><x>GOOgi</x></body>");
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report2));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    dbSc =
      (SeriesConfig)(new HqlQuery("select sc from SeriesConfig as sc")).selectUnique();
    assertNotNull(dbSc);
    assertFalse(crId.equals(dbSc.getLatestComparisonId()));

  }

  /** Tests reports for a SeriesConfig that includes a log notifier. */
  public void testWithLogNotifier() throws Exception {

    File logFile = File.createTempFile
      ("logNotifier", "txt", new File(System.getProperty("user.dir")));
    logFile.deleteOnExit();
    String logPath = logFile.getPath();

    SuiteUpdateTest sut = new SuiteUpdateTest();
    Suite suite = Suite.generate("aSuite", 1);
    SeriesConfig sc = suite.getSeriesConfig(0);
    Series s = sc.getSeries();
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", "body !~ /GOOgi/");
    Notification n = new Notification();
    n.setNotifier("LogNotifier");
    n.setTarget(logPath);
    ao.setNotification(n);
    sc.setAcceptedOutput(ao);
    sut.updateSuite(suite);

    FileWriter fw = new FileWriter(logPath);
    fw.close();
    long emptyLength = logFile.length();

    String report = s.generateReport();

    // Successful comparison should not generate a notification
    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(emptyLength == logFile.length());
    long notifyLength = logFile.length();

    // A failed comparison should also
    String report2 =
      report.replaceAll("<body[^>]*>.*</body>", "<body><x>GOOgi</x></body>");
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report2));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(emptyLength == notifyLength);
    notifyLength = logFile.length();

    // An identical failure should not
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report2));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertEquals(notifyLength, logFile.length());

    // A return to success should
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(notifyLength == logFile.length());

    logFile.delete();

  }

  /** Tests backward compatibility for notifiers that specify a Java class. */
  public void testWithClassNotifier() throws Exception {

    File logFile = File.createTempFile
      ("classNotifier", "txt", new File(System.getProperty("user.dir")));
    logFile.deleteOnExit();
    String logPath = logFile.getPath();

    SuiteUpdateTest sut = new SuiteUpdateTest();
    Suite suite = Suite.generate("xSuite", 1);
    SeriesConfig sc = suite.getSeriesConfig(0);
    Series s = sc.getSeries();
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", "body !~ /GOOgi/");
    Notification n = new Notification();
    n.setNotifier("edu.sdsc.inca.depot.util.LogNotifier");
    n.setTarget(logPath);
    ao.setNotification(n);
    sc.setAcceptedOutput(ao);
    sut.updateSuite(suite);

    FileWriter fw = new FileWriter(logPath);
    fw.close();
    long emptyLength = logFile.length();

    String report = s.generateReport();
    report =
      report.replaceAll("<body[^>]*>.*</body>", "<body><x>GOOgi</x></body>");

    // failed comparison should not generate a notification
    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(emptyLength == logFile.length());
    long notifyLength = logFile.length();

    logFile.delete();

  }

  /** Tests that Insert picks up CRs from equivalent series configs. */
  public void testEquivalentConfigs() throws Exception {

    File logFile = File.createTempFile
      ("equivConfigs", "txt", new File(System.getProperty("user.dir")));
    logFile.deleteOnExit();
    String logPath = logFile.getPath();

    SuiteUpdateTest sut = new SuiteUpdateTest();
    Suite suite = Suite.generate("aSuite", 1);

    SeriesConfig sc = suite.getSeriesConfig(0);
    Series s = sc.getSeries();
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", "body !~ /GOOg/");
    Notification n = new Notification();
    n.setNotifier("LogNotifier");
    n.setTarget(logPath);
    ao.setNotification(n);
    sc.setAcceptedOutput(ao);
    sc.setNickname("Series Nickname");
    sut.updateSuite(suite);

    FileWriter fw = new FileWriter(logPath);
    fw.close();
    long emptyLength = logFile.length();

    // Generate a fail CR from the first SC
    String report = s.generateReport().replaceAll
      ("<body[^>]*>.*</body>", "<body><x>GOOg</x></body>");
    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    long notifyLength = logFile.length();
    assertFalse(emptyLength == notifyLength);

    // Deactivate the SC, then add another w/the same nickname and resource
    sc.setAction(SuiteUpdate.DELETE);
    sut.updateSuite(suite);
    ao.setComparison("body =~ /Noon/");
    sc.setAction(SuiteUpdate.ADD);
    sut.updateSuite(suite);

    // An identical CR on the new SC should not notify
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertTrue(logFile.length() == notifyLength);

    logFile.delete();

  }

  /** Tests reports for a SeriesConfig that includes a script notifier. */
  public void testWithScriptNotifier() throws Exception {

    File logFile = File.createTempFile
      ("scriptNotifier", "txt", new File(System.getProperty("user.dir")));
    logFile.deleteOnExit();
    File scriptFile = File.createTempFile
      ("script", "sh", new File(System.getProperty("user.dir")));
    scriptFile.deleteOnExit();
    String logPath = logFile.getPath();
    String scriptPath = scriptFile.getPath();

    SuiteUpdateTest sut = new SuiteUpdateTest();
    Suite suite = Suite.generate("aSuite", 1);
    SeriesConfig sc = suite.getSeriesConfig(0);
    Series s = sc.getSeries();
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", "body !~ /GOOgi/");
    Notification n = new Notification();
    n.setNotifier("ScriptNotifier");
    n.setTarget(scriptPath);
    ao.setNotification(n);
    sc.setAcceptedOutput(ao);
    sut.updateSuite(suite);
    String report = s.generateReport();

    FileWriter fw = new FileWriter(logPath);
    fw.close();
    long emptyLength = logFile.length();
    fw = new FileWriter(scriptPath);
    fw.write(
      "#!/bin/sh\n" +
      "/bin/echo 'Notifier invoked' >> " + logPath + "\n" +
      "set | grep '^inca' >> " + logPath + "\n");
    fw.close();
    try {
      Process p = Runtime.getRuntime().exec("chmod +x " + scriptPath);
      p.waitFor();
    } catch(Exception e) {
      // empty
    }

    // Successful comparison should generate a notification
    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(emptyLength == logFile.length());
    long notifyLength = logFile.length();

    // A failed comparison should
    String report2 =
      report.replaceAll("<body[^>]*>.*</body>", "<body><x>GOOgi</x></body>");
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report2));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(notifyLength == logFile.length());
    notifyLength = logFile.length();

    // An identical failure should not
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report2));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertEquals(notifyLength, logFile.length());

    // A return to success should
    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(notifyLength == logFile.length());

    //logFile.delete();
    scriptFile.delete();

  }

  /** Tests a malformed report. */
  public void testInvalidXml() throws Exception {

    Series s = Series.generate("localhost", "myreporter", 3);
    String report = s.generateReport();
    // Remove hostname--a required tag
    report = report.replaceAll("<hostname>.*</hostname>", "");

    logger.debug("Insert the first");
    // Should throw a protocol exception
    try {
      execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    } catch(ProtocolException e) {
      assertTrue(e.toString().indexOf("Invalid") >= 0);
      return;
    }
    fail("Invalid XML does not raise an exception");

  }

  /** Tests a series w/no arguments. */
  public void testNoArgs() throws Exception {

    Series s = Series.generate("localhost", "myreporter", 0);
    String report = s.generateReport();

    logger.debug("Insert the first");
    logger.info(report);
    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));

    // make sure that the report was put in the db
    Series dbSeries = Series.find(s);
    assertNotNull(dbSeries);
    Object[] reports = dbSeries.getReports().toArray();
    assertEquals(1, reports.length);

  }

  /** Tests an excessive report log section. */
  public void testLongLog() throws Exception {

    Series s = Series.generate("localhost", "myreporter", 3);
    String report = s.generateReport();
    String logContent = "";
    while(logContent.length() <= Row.MAX_DB_LONG_STRING_LENGTH) {
      logContent += "<debug><gmt>0001-01-01T00:00:00</gmt><message>" +
                    logContent.length() + "</message></debug>\n";
    }
    report = report.replaceFirst
      ("</args>", "</args>\n<log>\n" + logContent + "</log>\n");

    logger.debug("Insert the first");
    execHandler(new Insert(), report(s.getResource(), s.getContext(), report));

    logger.debug("Query the first");
    Series dbSeries = Series.find(s);
    assertNotNull(dbSeries);
    Object[] reports = dbSeries.getReports().toArray();
    InstanceInfo ii =
      (InstanceInfo)(new HqlQuery("select i from InstanceInfo as i")).selectUnique();
    assertTrue(ii != null);
    String dbLogContent = ii.getLog();
    assertTrue
      (dbLogContent.length() <= Row.MAX_DB_LONG_STRING_LENGTH);
    while(dbLogContent.indexOf(logContent) < 0) {
      logContent = logContent.replaceFirst("<debug>.*?</debug>\n", "");
    }
    assertTrue(!logContent.equals(""));

  }

  public void testReportFilter() throws Exception {

    Series s = Series.generate("localhost", "myreporter", 3);
    String report = s.generateReport();

    logger.debug("Insert the first");
    execHandler(new Insert(), report(s.getResource(), s.getContext(), report));

    logger.debug("Query the first");
    List<?> instances =
      selectMultiple("select i from InstanceInfo as i", null);
    assertEquals(1, instances.size());

    setDepotProperty("reportFilter", ReportFilter.class.getName());

    logger.debug("Insert the second");
    execHandler(new Insert(), report(s.getResource(), s.getContext(), report));

    logger.debug("Query the second");
    instances = selectMultiple("select i from InstanceInfo as i", null);
    assertEquals(2, instances.size());

    setDepotProperty("reportFilter", SuppressingFilter.class.getName());

    logger.debug("Insert the third");
    execHandler(new Insert(), report(s.getResource(), s.getContext(), report));

    logger.debug("Query the third");
    instances = selectMultiple("select i from InstanceInfo as i", null);
    assertEquals(2, instances.size());

    setDepotProperty("reportFilter", ModifyingFilter.class.getName());
    List<?> reports = selectMultiple("select r from Report as r", null);
    assertEquals(1, reports.size());

    logger.debug("Insert the fourth");
    execHandler(new Insert(), report(s.getResource(), s.getContext(), report));

    logger.debug("Query the fourth");
    reports = selectMultiple("select r from Report as r", null);
    assertEquals(2, reports.size());

    setDepotProperty("reportFilter", TestReportFilter.class.getName());
    logger.debug("Insert the fifth");
    execHandler(new Insert(), report(s.getResource(), s.getContext(), report));
    assertTrue(TestReportFilter.invoked);

  }

  /** Tests what happens when reports arrive out-of-order. */
  public void testOutOfOrder() throws Exception {

    File logFile = File.createTempFile
      ("outOfOrder", "txt", new File(System.getProperty("user.dir")));
    logFile.deleteOnExit();
    String logPath = logFile.getPath();

    SuiteUpdateTest sut = new SuiteUpdateTest();
    Suite suite = Suite.generate("aSuite", 1);
    SeriesConfig sc = suite.getSeriesConfig(0);
    Series s = sc.getSeries();
    AcceptedOutput ao = new AcceptedOutput("ExprComparitor", "body !~ /GOOgi/");
    Notification n = new Notification();
    n.setNotifier("LogNotifier");
    n.setTarget(logPath);
    ao.setNotification(n);
    sc.setAcceptedOutput(ao);
    sut.updateSuite(suite);

    FileWriter fw = new FileWriter(logPath);
    fw.close();
    long emptyLength = logFile.length();

    String report1 = s.generateReport()
      .replaceAll("<body[^>]*>.*</body>", "<body><x>GOOgi</x></body>");
    Thread.sleep(1);
    String report2 = s.generateReport();
    Thread.sleep(1);
    String report3 = s.generateReport()
      .replaceAll("<body[^>]*>.*</body>", "<body><x>GOOgi</x></body>");
    Thread.sleep(1);
    String report4 = s.generateReport();

    String reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report4));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(emptyLength == logFile.length());
    long notifyLength = logFile.length();

    reply = execHandler(new Insert(), report(s.getResource(), s.getContext(), report3));
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertFalse(logFile.length() == notifyLength);
    notifyLength = logFile.length();

    String message = report(s.getResource(), s.getContext(), report2)
      .replaceFirst("REPORT", "RESEND");
    reply = execHandler(new Insert(), message);
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertTrue(logFile.length() == notifyLength);

    message = report(s.getResource(), s.getContext(), report1)
      .replaceFirst("REPORT", "RESEND");
    reply = execHandler(new Insert(), message);
    assertNotNull(reply);
    assertFalse(reply, reply.startsWith("ERROR"));
    Thread.sleep(3);
    assertTrue(logFile.length() == notifyLength);

    logFile.delete();

  }

  public static class ModifyingFilter extends ReportFilter {
    @Override
    public String getStdout() {
      return this.stdout.replaceFirst
        ("(?s)<body>.*</body>", "<body><different>5</different></body>");
    }
  }

  public static class SuppressingFilter extends ReportFilter {
    @Override
    public String getContext() {
      return null;
    }
  }

  public static class TestReportFilter
    extends edu.sdsc.inca.depot.util.ReportFilter {
    public static boolean invoked = false;
    @Override
    public String getStdout() {
      invoked = true;
      return super.getStdout();
    }
  }

}
