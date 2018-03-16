package edu.sdsc.inca.util;

import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A class that represents a cron schedule: an event schedule that allows for
 * execution based on some combination of minute, hour, day of month, month,
 * and day of week.  See crontab(5).
 */
public class CronSchedule {

  String hour;
  String mday;
  String minute;
  String month;
  String wday;

  public CronSchedule() {
    this("*", "*", "*", "*", "*");
  }

  public CronSchedule
    (String min, String hour, String mday, String month, String wday) {
    this.setMinute(min);
    this.setHour(hour);
    this.setMday(mday);
    this.setMonth(month);
    this.setWday(wday);
  }

  /**
   * Returns the hour field of the schedule.
   *
   * @return the hour field of the schedule
   */
  public String getHour() {
    return this.hour;
  }

  /**
   * Returns the day of month field of the schedule.
   *
   * @return the day of month field of the schedule
   */
  public String getMday() {
    return this.mday;
  }

  /**
   * Returns the minute field of the schedule.
   *
   * @return the minute field of the schedule
   */
  public String getMinute() {
    return this.minute;
  }

  /**
   * Returns the month field of the schedule.
   *
   * @return the month field of the schedule
   */
  public String getMonth() {
    return this.month;
  }

  /**
   * Returns the day of week field of the schedule.
   *
   * @return the day of week field of the schedule
   */
  public String getWday() {
    return this.wday;
  }

  /**
   * Sets the hour field of the schedule.
   *
   * @param hour the hour field of the schedule
   */
  public void setHour(String hour) {
    this.hour = hour;
  }

  /**
   * Sets the day of month field of the schedule.
   *
   * @param mday the day of month field of the schedule
   */
  public void setMday(String mday) {
    this.mday = mday;
  }

  /**
   * Sets the minute field of the schedule.
   *
   * @param minute the minute field of the schedule
   */
  public void setMinute(String minute) {
    this.minute = minute;
  }

  /**
   * Sets the month field of the schedule.
   *
   * @param month the month field of the schedule
   */
  public void setMonth(String month) {
    this.month = month;
  }

  /**
   * Sets the day of week field of the schedule.
   *
   * @param wday the day of week field of the schedule
   */
  public void setWday(String wday) {
    this.wday = wday;
  }

  /**
   * Returns the next date/time that this schedule will cause an event.
   * Returns null if no later event will ever occur.
   *
   * @return the next date/time an event will occur
   */
  public Date nextEvent() {
    return this.nextEvent(new Date());
  }

  /**
   * Returns the next date/time after a specified date/time that this schedule
   * will cause an event.  Returns null if no later event will ever occur.
   *
   * @param d a date/time preceding the returned value
   * @return the next date/time an event will occur
   */
  public Date nextEvent(Date d) {

    GregorianCalendar dAsCal = new GregorianCalendar();
    dAsCal.setTime(d);
    int dSecond = dAsCal.get(GregorianCalendar.SECOND);
    int dMinute = dAsCal.get(GregorianCalendar.MINUTE);
    int dHour = dAsCal.get(GregorianCalendar.HOUR_OF_DAY);
    int dMday = dAsCal.get(GregorianCalendar.DAY_OF_MONTH);
    int dMonth = dAsCal.get(GregorianCalendar.MONTH);
    int dWday = dAsCal.get(GregorianCalendar.DAY_OF_WEEK);
    int dYear = dAsCal.get(GregorianCalendar.YEAR);
    int nextSecond = dSecond; // Fixed, since cron doesn't include seconds
    int nextMinute = dMinute + 1; // Advance will be at least 1 minute
    int nextHour = dHour;
    int nextMday = dMday;
    int nextMonth = dMonth;
    int nextWday = dWday;
    int nextYear = dYear;

    // Bump the next-highest field for each field that will wrap
    nextMinute = this.getMinimum(nextMinute, this.minute, 0, 59);
    if(nextMinute < 0) {
      nextHour++;
    }
    nextHour = this.getMinimum(nextHour, this.hour, 0, 23);
    if(nextHour < 0) {
      nextMinute = -1;
      nextMday++;
    }
    nextMday = this.getMinimum(nextMday, this.mday, 1, 31);
    if(nextMday < 0) {
      nextHour = -1;
      nextMinute = -1;
      nextMonth++;
    }
    nextMonth = this.getMinimum(nextMonth, this.month, 0, 11);
    if(nextMonth < 0) {
      nextHour = -1;
      nextMinute = -1;
      nextMday = -1;
      nextYear++;
    }
    // Set fields that wrapped to minimum value
    if(nextMinute < 0) {
       nextMinute = this.getMinimum(0, this.minute, 0, 59);
    }
    if(nextHour < 0) {
       nextHour = this.getMinimum(0, this.hour, 0, 23);
    }
    if(nextMday < 0) {
       nextMday = this.getMinimum(0, this.mday, 1, 31);
    }
    if(nextMonth < 0) {
       nextMonth = this.getMinimum(0, this.month, 0, 11);
    }

    GregorianCalendar result = new GregorianCalendar();
    result.set(GregorianCalendar.SECOND, nextSecond);
    result.set(GregorianCalendar.MINUTE, nextMinute);
    result.set(GregorianCalendar.HOUR_OF_DAY, nextHour);
    result.set(GregorianCalendar.DAY_OF_MONTH, nextMday);
    result.set(GregorianCalendar.MONTH, nextMonth);
    result.set(GregorianCalendar.YEAR, nextYear);

    // Adjust for specs that include a wday setting.  Note that
    // java.util.Calendar wdays are 1-based (Sunday = 1, Monday = 2, etc.)
    dWday = result.get(GregorianCalendar.DAY_OF_WEEK) - 1;
    nextWday = this.getMinimum(dWday, this.wday, 0, 6);
    if(nextWday < 0) {
      nextWday = this.getMinimum(0, this.wday, 0, 6);
    }
    if(dWday < nextWday) {
      result.add(GregorianCalendar.DAY_OF_MONTH, nextWday - dWday);
    } else if(dWday > nextWday) {
      result.add(GregorianCalendar.DAY_OF_MONTH, 7 - (dWday - nextWday));
    }

    return result.getTime();

  }

  /**
   * Given a cron field spec, a target integer, and a range, returns the lowest
   * integer w/in the range, equal or greater to the target, that is included
   * in the list of values specified by the field spec.  Returns -1 if no such
   * value exists.
   *
   * @param target the target integer
   * @param spec cron field spec, format [0-9]+(-[0-9]+)?(,...)*(/[0-9]+)?
   *             (one or more values/ranges with an optional step)
   * @param lowest the range low bound, inclusive
   * @param highest the range high bound, inclusive
   * @return the lowest integer w/in the range equal or greater than the
   *         target; -1 if none
   */
  public int getMinimum(int target, String spec, int lowest, int highest) {

    spec = spec.replaceFirst("\\*", lowest + "-" + highest);

    // Determine step value; defaults to 1
    int step = 1;
    int pos = spec.indexOf("/");
    if(pos >= 0) {
      step = Integer.parseInt(spec.substring(pos + 1));
      spec = spec.substring(0, pos);
    }

    // Look for a sufficiently high value in each range in turn
    String[] ranges = spec.split(",");
    for(int i = 0; i < ranges.length; i++) {
      String range = ranges[i];
      pos = range.indexOf("-");
      int rangeLow = Integer.parseInt(pos<0 ? range : range.substring(0, pos));
      int rangeHigh =
        pos < 0 ? rangeLow : Integer.parseInt(range.substring(pos + 1));
      for(int j = rangeLow; j <= rangeHigh; j += step) {
        if(j >= target) {
          return j;
        }
      }
    }

    return -1;

  }

  /**
   * A factory method that returns the CronSchedule corresponding to a cron
   * spec.
   *
   * @param spec the cron spec: min hour mday month wday
   * @return the corresponding cron spec
   * @throws ParseError on a bad spec
   */
  public static CronSchedule parse(String spec) throws ParseException {
    String[] pieces = spec.split("\\s+");
    return new CronSchedule
      (pieces[0], pieces[1], pieces[2], pieces[3], pieces[4]);
  }

  /**
   * A factory method that returns the CronSchedule corresponding to a cron
   * spec w/an alternate format.
   *
   * @param spec the cron spec: [[wday:]hour:]minute
   *             unspecified fields are equivalent to "*"
   * @return the corresponding cron spec
   * @throws ParseError on a bad spec
   */
  public static CronSchedule parseWHM(String whm) throws ParseException {
    String[] pieces = whm.split(":", 3);
    String minute = pieces[pieces.length - 1];
    String hour = pieces.length >= 2 ? pieces[pieces.length - 2] : "*";
    String wday = pieces.length == 3 ? pieces[0] : "*";
    return parse(minute + " " + hour + " * * " + wday);
  }

}
