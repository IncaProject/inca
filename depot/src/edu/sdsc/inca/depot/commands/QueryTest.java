package edu.sdsc.inca.depot.commands;


import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.sdsc.inca.depot.persistent.AcceptedOutput;
import edu.sdsc.inca.depot.persistent.PersistentTest;
import edu.sdsc.inca.depot.persistent.Series;
import edu.sdsc.inca.depot.persistent.SeriesConfigDAO;
import edu.sdsc.inca.depot.persistent.Suite;
import edu.sdsc.inca.depot.persistent.SeriesConfig;
import edu.sdsc.inca.depot.persistent.SuiteDAO;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolReader;


public class QueryTest extends PersistentTest {

  private static final String CRLF = "\r\n";
  private static final long MILLIS_IN_A_MINUTE = 1000L * 60L;
  private static final long MILLIS_IN_AN_HOUR = MILLIS_IN_A_MINUTE * 60L;
  private static final long MILLIS_IN_A_DAY = MILLIS_IN_AN_HOUR * 24L;
  private static Calendar TWO_WEEKS_END = Calendar.getInstance();
  private static Calendar TWO_WEEKS_START = Calendar.getInstance();
  static {
    TWO_WEEKS_START.set(2007, 6, 6, 2, 0, 1);
    TWO_WEEKS_END.setTimeInMillis
      (TWO_WEEKS_START.getTimeInMillis() + MILLIS_IN_A_DAY * 14 - 1);
  }

  public void testNoSuiteFound() throws Exception {
    String reply = execHandler(new Query(),
      Protocol.QUERY_LATEST_COMMAND + " suite.guid = 'notThere'" + CRLF
    );
    assertTrue(reply.startsWith("QUERYEND"));
  }

  public void testGetGuids() throws Exception {

    Suite suite1 = Suite.generate("aSuite", 0);
    Suite suite2 = Suite.generate("bSuite", 0);

    String reply = execHandler(new SuiteUpdate(),
      Protocol.SUITE_UPDATE_COMMAND + " " + suite1.toXml() + CRLF
    );
    assertTrue(reply.startsWith("OK"));

    reply = execHandler(new SuiteUpdate(),
      Protocol.SUITE_UPDATE_COMMAND + " " + suite2.toXml() + CRLF
    );
    assertTrue(reply.startsWith("OK"));

    reply = execHandler(new Query(), Protocol.QUERY_GUIDS_COMMAND + CRLF);
    assertEquals
      ("OK " + suite1.getGuid() + "\n" + suite2.getGuid() + CRLF, reply);

  }

  public void testGetLatest() throws Exception {

    int CONFIG_COUNT = 6;
    Suite testSuite = Suite.generate("aSuite", 0);
    String[] reports = new String[CONFIG_COUNT];
    for(int i = 0; i < CONFIG_COUNT; i++) {
      SeriesConfig sc = SeriesConfig.generate("localhost", "@@ " + i, 2);
      sc.setNickname("sc nickname " + i);
      testSuite.addSeriesConfig(sc);
      reports[i] = sc.getSeries().generateReport();
    }

    String reply = execHandler(new SuiteUpdate(),
      Protocol.SUITE_UPDATE_COMMAND + " " + testSuite.toXml() + CRLF
    );
    assertTrue(reply.startsWith("OK"));

    // insert two reports for each series in the suite
    for(int i = 0; i < reports.length; i++) {
      String context = testSuite.getSeriesConfig(i).getSeries().getContext();
      logger.debug("Insert " + context);
      execHandler(new Insert(),
        Protocol.INSERT_COMMAND + " localhost " + context + CRLF +
        "STDOUT " + reports[i] + CRLF +
        "SYSUSAGE cpu_secs=12\nwall_secs=13\nmemory_mb=14\n" + CRLF
      );
      execHandler(new Insert(),
        Protocol.INSERT_COMMAND + " localhost " + context + CRLF +
        "STDOUT " + reports[i] + CRLF +
        "SYSUSAGE cpu_secs=12\nwall_secs=13\nmemory_mb=14\n" + CRLF
      );
    }

    // query for the latest instance from each series
    reply = execHandler(new Query(),
      Protocol.QUERY_LATEST_COMMAND +
      " suite.guid = '" + testSuite.getGuid() + "'" + CRLF
    );
    logger.debug("Suite query reply is '" + reply + "'");
    int replyCount = 0;
    for(int i = reply.indexOf("QUERYRESULT");
        i >= 0;
        i = reply.indexOf("QUERYRESULT", i + 1)) {
      replyCount++;
    }
    assertEquals(reports.length, replyCount);

    // query via series nickname
    String request = Protocol.QUERY_LATEST_COMMAND + " ";
    for(int i = 0; i < 3; i++) {
      SeriesConfig sc = testSuite.getSeriesConfig(i);
      if(i > 0) {
        request += " OR ";
      }
      request += "config.nickname = '" + sc.getNickname() + "'";
    }
    request += CRLF;
    reply = execHandler(new Query(), request);
    logger.debug("Suite query reply is '" + reply + "'");
    replyCount = 0;
    for(int i = reply.indexOf("QUERYRESULT");
        i >= 0;
        i = reply.indexOf("QUERYRESULT", i + 1)) {
      replyCount++;
    }
    assertEquals(3, replyCount);

  }

  public void testHql() throws Exception  {

    int CONFIG_COUNT = 6;
    Suite testSuite = Suite.generate("aSuite", 0);
    String[] reports = new String[CONFIG_COUNT];
    for(int i = 0; i < CONFIG_COUNT; i++) {
      SeriesConfig sc = SeriesConfig.generate("localhost", "@@ " + i, 2);
      testSuite.addSeriesConfig(sc);
      reports[i] = sc.getSeries().generateReport();
    }

    // insert a suite
    String reply = execHandler(new SuiteUpdate(),
      Protocol.SUITE_UPDATE_COMMAND + " " + testSuite.toXml() + CRLF
    );
    assertTrue(reply.startsWith("OK"));

    // insert two reports for each series in the suite
    for(int i = 0; i < reports.length; i++) {
      String context = testSuite.getSeriesConfig(i).getSeries().getContext();
      logger.debug("Insert " + context);
      execHandler(new Insert(),
        Protocol.INSERT_COMMAND + " localhost " + context + CRLF +
        "STDOUT " + reports[i] + CRLF +
        "SYSUSAGE cpu_secs=12\nwall_secs=13\nmemory_mb=14\n" + CRLF
      );
      execHandler(new Insert(),
        Protocol.INSERT_COMMAND + " localhost " + context + CRLF +
        "STDOUT " + reports[i] + CRLF +
        "SYSUSAGE cpu_secs=12\nwall_secs=13\nmemory_mb=14\n" + CRLF
      );
    }

    reply = execHandler(new Query(),
      Protocol.QUERY_HQL_COMMAND + " " + "select r from Report as r" + CRLF
    );
    logger.debug("SQL query reply is '" + reply + "'");
    int pos = reply.indexOf("<body>");
    assertTrue(pos >= 0);

  }


  protected static final int SEQUENTIAL_CONFIG_COUNT = 3;
  protected static final String SEQUENTIAL_ERROR_MESSAGE = "Generic error";
  protected static final String SEQUENTIAL_SUITE_GUID = "aSuite";
  protected static final int SEQUENTIAL_TEST_COUNT = 4;
  /**
   * Inserts into the DB hourly reports for 14 days for each series config,
   * i.e., 336 instances.  The ending time for each run is equal to (15 +
   * config #) minutes into the hour.  The wall clock time starts at 30 seconds
   * for the first run and increases by 1 second per instance, to 365 seconds
   * for the final run.  So, pairs of config instances begin overlapping at run
   * 30, 3 at run 90, 4 at 150, 5 at 210, 6 at 270 and 7 at 330.
   */
  public void testSetupForSequentialTests() throws Exception  {

    Insert inserter = new Insert();

    // Generate a suite with the given # of series and insert it into the DB
    Suite testSuite = Suite.generate(SEQUENTIAL_SUITE_GUID, 0);
    for(int i = 0; i < SEQUENTIAL_CONFIG_COUNT; i++) {
      SeriesConfig sc = SeriesConfig.generate("localhost", "@@ " + i, 2);
      sc.setNickname("sc nickname " + i);
      if(i == 1) {
        sc.setAcceptedOutput(new AcceptedOutput("ExprComparitor", "1 == 1"));
      } else if(i == 2) {
        sc.setAcceptedOutput(new AcceptedOutput("ExprComparitor", "1 != 1"));
      }
      testSuite.addSeriesConfig(sc);
    }
    String reply = execHandler(new SuiteUpdate(),
        Protocol.SUITE_UPDATE_COMMAND + " " + testSuite.toXml() + CRLF
    );
    assertTrue(reply.startsWith("OK"));

    String[] replyParts = reply.split("\\s+");
    long suiteId = Long.parseLong(replyParts[1]);

    testSuite = SuiteDAO.load(suiteId);

    Calendar activation = Calendar.getInstance();

    activation.setTimeInMillis(TWO_WEEKS_START.getTimeInMillis() - MILLIS_IN_A_DAY * 21);

    for (SeriesConfig sc : testSuite.getSeriesConfigs()) {
    	sc.setActivated(activation.getTime());

    	SeriesConfigDAO.update(sc);
    }

    // Insert two weeks' of hourly reports for each config
    for(int i = 0; i < SEQUENTIAL_CONFIG_COUNT; i++) {
      Series s = testSuite.getSeriesConfig(i).getSeries();
      String context = s.getContext();
      int committedOffsetInMinutes = i + 15;
      long wallSecs = 30;
      Calendar c = Calendar.getInstance();
      c.setTimeInMillis(TWO_WEEKS_START.getTimeInMillis());
      c.add(Calendar.MINUTE, committedOffsetInMinutes);
      while(c.getTimeInMillis() <= TWO_WEEKS_END.getTimeInMillis()) {
        String report = s.generateReport(c.getTime());
        if(c.get(Calendar.HOUR_OF_DAY) == 0) {
          report = report.replaceFirst
            ("<completed>true</completed>",
             "<completed>false</completed>" +
             "<errorMessage>" + SEQUENTIAL_ERROR_MESSAGE + "</errorMessage>");
        }
        long cpuSecs = wallSecs * (long)Math.pow(2, i);
        long memory = wallSecs * (i + 2);
        reply = execHandler(inserter,
            Protocol.INSERT_COMMAND + " localhost " + context + CRLF +
            "STDOUT " + report + CRLF +
            "SYSUSAGE cpu_secs=" + cpuSecs + "\n" +
            "wall_secs=" + wallSecs + "\n" +
            "memory_mb=" + memory + "\n" + CRLF
        );
        assertTrue(reply.startsWith("OK"));
        c.setTimeInMillis(c.getTimeInMillis() + MILLIS_IN_AN_HOUR);
        wallSecs += 2;
      }
    }

    PersistentTest.sequentialTestsLeftToRun = SEQUENTIAL_TEST_COUNT + 1;

  }

  public void testGetPeriod() throws Exception  {

    Calendar end = Calendar.getInstance();
    end.setTimeInMillis(TWO_WEEKS_END.getTimeInMillis() - MILLIS_IN_A_DAY * 3L);
    Calendar start = Calendar.getInstance();
    start.setTimeInMillis(end.getTimeInMillis() - MILLIS_IN_A_DAY * 7L);

    String reply = execHandler(new Query(),
      Protocol.QUERY_PERIOD_COMMAND + " " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );

    int replyCount = 0;
    int next = 0;
    for(int i = reply.indexOf("QUERYRESULT"); i >= 0; i = next) {
      next = reply.indexOf("QUERYRESULT", i + 1);
      String result = reply.substring(i + 11, next < 0 ? reply.length() : next);
      if(!result.matches(
          "(?s).*" +
          "<object[^>]*>\\s*" +
          "<resource[^>]*>.*?</resource>\\s*" +
          "<nickname[^>]*>.*?</nickname>\\s*" +
          "<instanceId[^>]*>\\d*?</instanceId>\\s*" +
          "<reportId[^>]*>\\d*?</reportId>\\s*" +
          "<configId[^>]*>\\d*?</configId>\\s*" +
          "<collected[^>]*>.*?</collected>\\s*" +
          "<exit_status[^>]*>.*?</exit_status>\\s*" +
          "<exit_message[^>]*(/>|.*?</exit_message>)\\s*" +
          "<comparisonResult[^>]*(/>|.*?</comparisonResult>)\\s*" +
          "</object>" +
          ".*"
        )) {
        fail("Invalid format of '" + result + "'");
      };
      replyCount++;
    }
    assertEquals(24 * 7 * SEQUENTIAL_CONFIG_COUNT, replyCount);

  }

  public void testSuccessHistory() throws Exception  {

    // Ask for a monthly summary ...
    Calendar end = Calendar.getInstance();
    end.setTimeInMillis(TWO_WEEKS_END.getTimeInMillis());
    Calendar start = Calendar.getInstance();
    start.setTimeInMillis(end.getTimeInMillis() - MILLIS_IN_A_DAY * 28L + 1);

    String reply = execHandler(new Query(),
      Protocol.QUERY_STATUS_COMMAND + " " + "MONTH " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );
    logger.debug("tSH Series query reply is '" + reply + "'");
    Matcher m = Pattern.compile("<success>(\\d+)</success>").matcher(reply);
    int count = 0;
    while(m.find()) {
      assertEquals((14 * 23) + "", m.group(1));
      count++;
    }
    assertEquals(SEQUENTIAL_CONFIG_COUNT, count);

    // ... a weekly one ...
    reply = execHandler(new Query(),
      Protocol.QUERY_STATUS_COMMAND + " " + "WEEK " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );
    logger.debug("Series query reply is '" + reply + "'");
    m = Pattern.compile("<success>(\\d+)</success>").matcher(reply);
    count = 0;
    while(m.find()) {
      if(m.group(1).equals("0")) {
        continue;
      }
      assertEquals((7 * 23) + "", m.group(1));
      count++;
    }
    assertEquals(SEQUENTIAL_CONFIG_COUNT * 2, count);

    // ... a daily one ...
    reply = execHandler(new Query(),
      Protocol.QUERY_STATUS_COMMAND + " " + "DAY " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );
    logger.debug("Series query reply is '" + reply + "'");
    m = Pattern.compile("<success>(\\d+)</success>").matcher(reply);
    count = 0;
    while(m.find()) {
      if(m.group(1).equals("0")) {
        continue;
      }
      assertEquals("23", m.group(1));
      count++;
    }
    assertEquals(SEQUENTIAL_CONFIG_COUNT * 14, count);

    // ... and a period (10 days) that unevenly divides the reports.
    reply = execHandler(new Query(),
      Protocol.QUERY_STATUS_COMMAND + " " + (60 * 24 * 10) + " " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );
    logger.debug("Series query reply is '" + reply + "'");
    m = Pattern.compile("<success>(\\d+)</success>").matcher(reply);
    count = 0;
    while(m.find()) {
      if(m.group(1).equals("0")) {
        continue;
      }
      if(count % 2 == 0)
        assertEquals((23 * 6) + "", m.group(1));
      else
        assertEquals((23 * 8) + "", m.group(1));
      count++;
    }
    assertEquals(SEQUENTIAL_CONFIG_COUNT * 2, count);

  }

  public void testFailureHistory() throws Exception  {

    // Generate a weekly report
    Calendar start = Calendar.getInstance();
    start.setTimeInMillis
      (TWO_WEEKS_START.getTimeInMillis() - 14 * MILLIS_IN_A_DAY);
    Calendar end = Calendar.getInstance();
    end.setTimeInMillis(TWO_WEEKS_END.getTimeInMillis());
    String reply = execHandler(new Query(),
      Protocol.QUERY_STATUS_COMMAND + " " + "WEEK " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );
    logger.debug("tFH Series query reply is '" + reply + "'");
    Matcher m = Pattern.compile("<success>(\\d+)</success>(\\s*<failure>\\s*<message>([^<]+)</message>\\s*<count>(\\d+)</count>)?").matcher(reply);
    boolean comparisonSuccessSeen = false;
    boolean errorMessageSeen = false;
    while(m.find()) {
      if(m.group(1).equals("0")) {
        continue;
      }
      assertEquals(SEQUENTIAL_ERROR_MESSAGE, m.group(3));
      assertEquals("7", m.group(4));
      assertEquals((23 * 7) + "", m.group(1));
      comparisonSuccessSeen = true;
      errorMessageSeen = true;
    }
    assertTrue(comparisonSuccessSeen);
    assertTrue(errorMessageSeen);

  }

  public void testQueryUsage() throws Exception {

    Calendar end = Calendar.getInstance();
    end.setTimeInMillis(TWO_WEEKS_END.getTimeInMillis());
    Calendar start = Calendar.getInstance();
    start.setTimeInMillis(end.getTimeInMillis() - MILLIS_IN_A_DAY * 28);
    String reply = execHandler(new Query(),
      Protocol.QUERY_USAGE_COMMAND + " " +
      start.getTimeInMillis() + " " + end.getTimeInMillis() + " " +
      "suite.guid = '" + SEQUENTIAL_SUITE_GUID + "'" + CRLF
    );
    logger.info("tQU Usage query reply is '" + reply + "'");

    // Sort the returned events by time so that we can easily ask questions
    ArrayList<Event> events = new ArrayList<Event>();
    Event e = new Event();
    Matcher m = Pattern.compile("<(\\w+)>(.*?)</\\w+>").matcher(reply);
    for(int i = 0; m.find(i); i = m.end()) {
      String tag = m.group(1);
      String value = m.group(2);
      if(tag.equals("time")) {
        e.time = Long.parseLong(value);
      } else if(tag.equals("cumulativeCount")) {
        e.count = Long.parseLong(value);
      } else if(tag.equals("cumulativeCpuPct")) {
        e.cpuPct = Double.parseDouble(value);
      } else if(tag.equals("cumulativeMemory")) {
        e.memory = Double.parseDouble(value);
        e.begin = events.size() % 2 == 0;
        int index = 0;
        while(index < events.size()) {
          Event f = events.get(index);
          if(f.time > e.time) {
            break;
          }
          index++;
        }
        events.add(index, e);
        Event nextEvent = new Event();
        nextEvent.execCpu = e.execCpu;
        nextEvent.execWall = e.execWall;
        nextEvent.execCpuPct = e.execCpuPct;
        nextEvent.execMemory = e.execMemory;
        e = nextEvent;
      } else if(tag.equals("cpu")) {
        e.execCpu = Double.parseDouble(value);
      } else if(tag.equals("wall")) {
        e.execWall = Double.parseDouble(value);
      } else if(tag.equals("cpuPct")) {
        e.execCpuPct = Double.parseDouble(value);
      } else if(tag.equals("memory")) {
        e.execMemory = Double.parseDouble(value);
      }
    }

    long cumulativeCount = 0;
    double cumulativeCpuPct = 0.0;
    double cumulativeMemory = 0.0;
    for(int i = 0; i < events.size(); i++) {
      e = events.get(i);
      if(e.begin) {
        cumulativeCount++;
        cumulativeCpuPct += e.execCpuPct;
        cumulativeMemory += e.execMemory;
      } else {
        cumulativeCount--;
        cumulativeCpuPct -= e.execCpuPct;
        cumulativeMemory -= e.execMemory;
      }
      assertEquals("Count #" + i + " at " + e.time, cumulativeCount, e.count);
      assertEquals("Cpu #" + i + " at " + e.time, cumulativeCpuPct, e.cpuPct);
      assertEquals("Mem #" + i + " at " + e.time, cumulativeMemory, e.memory);
    }

  }

  public class Event {
    long time;
    long count;
    double cpuPct;
    double memory;
    double execCpu;
    double execCpuPct;
    double execWall;
    double execMemory;
    boolean begin;
  }

  public String execHandler(MessageHandler handler, String request)
    throws Exception {
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    ProtocolReader reader = new ProtocolReader(new StringReader(request));
    handler.execute(reader, outBytes, null);
    return outBytes.toString();
  }

}
