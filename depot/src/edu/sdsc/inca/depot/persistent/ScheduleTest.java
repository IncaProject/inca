package edu.sdsc.inca.depot.persistent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Basic tests to check for Schedule functionality.
 */

public class ScheduleTest extends PersistentTest {

  protected static final SimpleDateFormat sdf =
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /** Tests the class constructors. */
  public void testConstructors() {
    Schedule s = new Schedule();
    assertNotNull(s);
    assertEquals("cron", s.getType());
    assertEquals("*", s.getMinute());
    assertEquals("*", s.getHour());
    assertEquals("*", s.getMonth());
    assertEquals("*", s.getMday());
    assertEquals("*", s.getWday());
    s = new Schedule("15", "2", "1", "6", "*", "cron", null);
    assertEquals("cron", s.getType());
    assertEquals("15", s.getMinute());
    assertEquals("2", s.getHour());
    assertEquals("1", s.getMday());
    assertEquals("6", s.getMonth());
    assertEquals("*", s.getWday());
  }

  /** Tests the nextEvent method against an hourly cron schedule. */
  public void testHourlyEvent() throws Exception {
    Schedule s = new Schedule("2", "*", "*", "*", "*", "cron", null);
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 10:02:00", sdf.format(next));
    Calendar expires = s.calculateExpires(next);
    assertEquals("2007-11-02 12:02:00", sdf.format(expires.getTime()));
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
    Schedule s = new Schedule("2", "15", "*", "*", "*", "cron", null);
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-02 15:02:00", sdf.format(next));
    Calendar expires = s.calculateExpires(d);
    assertEquals("2007-11-03 15:02:00", sdf.format(expires.getTime()));
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
    Schedule s = new Schedule("2", "15", "*", "*", "0", "cron", null);
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-09 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    System.out.println(next);
    assertEquals("2007-11-11 15:02:00", sdf.format(next));
    Calendar expires = s.calculateExpires(d);
    assertEquals("2007-11-18 15:02:00", sdf.format(expires.getTime()));
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
    Schedule s = new Schedule("2", "15", "4", "*", "*", "cron", null);
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2007-11-04 15:02:00", sdf.format(next));
    Calendar expires = s.calculateExpires(d);
    assertEquals("2007-12-04 15:02:00", sdf.format(expires.getTime()));
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
    Schedule s = new Schedule("2", "15", "4", "8", "*", "cron", null);
    // Date falls between scheduled events
    Date d = sdf.parse("2007-11-02 10:00:00");
    Date next = s.nextEvent(d);
    assertNotNull(next);
    assertEquals("2008-09-04 15:02:00", sdf.format(next));
    Calendar expires = s.calculateExpires(d);
    assertEquals("2009-09-04 15:02:00", sdf.format(expires.getTime()));
    // Date falls on a scheduled event
    next = s.nextEvent(next);
    assertNotNull(next);
    assertEquals("2009-09-04 15:02:00", sdf.format(next));
  }

  /** Tests the nextEvent method against a cron schedule w/ranges. */
  public void testCronRange() throws Exception {
    Schedule s = new Schedule("2", "4-6", "*", "*", "*", "cron", null);
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
    Schedule s = new Schedule("2", "4,7,9,22", "*", "*", "*", "cron", null);
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
    Schedule s = new Schedule("2", "4-9/2", "*", "*", "*", "cron", null);
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
    s = new Schedule("2", "*/2", "*", "*", "*", "cron", null);
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
