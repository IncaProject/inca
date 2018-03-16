package edu.sdsc.inca.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import junit.framework.TestCase;

/**
 * Basic tests to check for CronSchedule functionality.
 */
public class CronScheduleTest extends TestCase {

  protected static final SimpleDateFormat sdf =
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /** Tests the class constructors. */
  public void testConstructors() {
    CronSchedule s = new CronSchedule();
    assertNotNull(s);
    assertEquals("*", s.getMinute());
    assertEquals("*", s.getHour());
    assertEquals("*", s.getMonth());
    assertEquals("*", s.getMday());
    assertEquals("*", s.getWday());
    s = new CronSchedule("15", "2", "1", "6", "*");
    assertEquals("15", s.getMinute());
    assertEquals("2", s.getHour());
    assertEquals("1", s.getMday());
    assertEquals("6", s.getMonth());
    assertEquals("*", s.getWday());
  }

  /** Tests the static class parsers. */
  public void testParsers() throws Exception {
    CronSchedule s = CronSchedule.parse("15 2 1 6 *");
    assertEquals("15", s.getMinute());
    assertEquals("2", s.getHour());
    assertEquals("1", s.getMday());
    assertEquals("6", s.getMonth());
    assertEquals("*", s.getWday());
    s = CronSchedule.parseWHM("15");
    assertEquals("15", s.getMinute());
    assertEquals("*", s.getHour());
    assertEquals("*", s.getMday());
    assertEquals("*", s.getMonth());
    assertEquals("*", s.getWday());
    s = CronSchedule.parseWHM("22:15");
    assertEquals("15", s.getMinute());
    assertEquals("22", s.getHour());
    assertEquals("*", s.getMday());
    assertEquals("*", s.getMonth());
    assertEquals("*", s.getWday());
    s = CronSchedule.parseWHM("3:22:15");
    assertEquals("15", s.getMinute());
    assertEquals("22", s.getHour());
    assertEquals("*", s.getMday());
    assertEquals("*", s.getMonth());
    assertEquals("3", s.getWday());
  }

  /** Tests the nextEvent method against an hourly cron schedule. */
  public void testHourlyEvent() throws Exception {
    CronSchedule s = new CronSchedule("2", "*", "*", "*", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 10:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-02 11:02:00", sdf.format(next));
    // Day rolls over
    d = sdf.parse("2007-11-02 23:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-03 00:02:00", sdf.format(next));
    // Month rolls over
    d = sdf.parse("2007-11-30 23:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-12-01 00:02:00", sdf.format(next));
    // Year rolls over
    d = sdf.parse("2007-12-31 23:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2008-01-01 00:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a daily cron schedule. */
  public void testDailyEvent() throws Exception {
    CronSchedule s = new CronSchedule("2", "15", "*", "*", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 15:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-03 15:02:00", sdf.format(next));
    // Month rolls over
    d = sdf.parse("2007-11-30 15:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-12-01 15:02:00", sdf.format(next));
    // Year rolls over
    d = sdf.parse("2007-12-31 15:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2008-01-01 15:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a weekly cron schedule. */
  public void testWeeklyEvent() throws Exception {
    CronSchedule s = new CronSchedule("2", "15", "*", "*", "0");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-09 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    System.out.println(next);
    assertEquals("2007-11-11 15:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-18 15:02:00", sdf.format(next));
    // Month rolls over
    d = sdf.parse("2007-11-25 15:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-12-02 15:02:00", sdf.format(next));
    // Year rolls over
    d = sdf.parse("2007-12-30 15:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2008-01-06 15:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a monthly cron schedule. */
  public void testMonthlyEvent() throws Exception {
    CronSchedule s = new CronSchedule("2", "15", "4", "*", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-04 15:02:00", sdf.format(next));
    d = sdf.parse("2007-10-20 10:00:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-04 15:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-12-04 15:02:00", sdf.format(next));
    // Year rolls over
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2008-01-04 15:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a yearly cron schedule. */
  public void testYearlyEvent() throws Exception {
    CronSchedule s = new CronSchedule("2", "15", "4", "8", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2008-09-04 15:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2009-09-04 15:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a cron schedule w/ranges. */
  public void testCronRange() throws Exception {
    CronSchedule s = new CronSchedule("2", "4-6", "*", "*", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 00:02:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 04:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-02 05:02:00", sdf.format(next));
    // Roll over
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-02 06:02:00", sdf.format(next));
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-03 04:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a cron schedule w/lists. */
  public void testCronList() throws Exception {
    CronSchedule s = new CronSchedule("2", "4,7,9,22", "*", "*", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 00:02:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 04:02:00", sdf.format(next));
    d = sdf.parse("2007-11-02 08:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 09:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-02 22:02:00", sdf.format(next));
    // Roll over
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-03 04:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a cron schedule w/a step. */
  public void testCronStep() throws Exception {
    CronSchedule s = new CronSchedule("2", "4-9/2", "*", "*", "*");
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 00:02:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 04:02:00", sdf.format(next));
    d = sdf.parse("2007-11-02 05:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 06:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-02 08:02:00", sdf.format(next));
    // Roll over
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-03 04:02:00", sdf.format(next));
    s = new CronSchedule("2", "*/2", "*", "*", "*");
    // Date falls between scheduled events
    d = sdf.parse("2007-11-02 01:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 02:02:00", sdf.format(next));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2007-11-02 04:02:00", sdf.format(next));
    // Roll over
    d = sdf.parse("2007-11-02 23:02:00");
    next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-03 00:02:00", sdf.format(next));
  }

}
