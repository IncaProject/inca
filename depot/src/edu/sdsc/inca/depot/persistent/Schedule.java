package edu.sdsc.inca.depot.persistent;

import edu.sdsc.inca.util.CronSchedule;
import java.util.Date;
import java.util.Calendar;

import org.apache.xmlbeans.XmlCalendar;
import org.apache.xmlbeans.XmlObject;

/**
 * Schedule holds the information for a cron-like schedule that we use.  In the
 * future there will be other schedule types as well.
 *
 * @author cmills
 */
public class Schedule extends XmlBeanObject {

  private String minute;
  private String hour;
  private String month;
  private String mday;
  private String wday;
  private String type;
  private Integer numOccurs;
  private boolean suspended;

  /**
   * Default constructor.
   */
  public Schedule() {
    this("*", "*", "*", "*", "*", "cron", Integer.valueOf(-1));
  }

  /**
   * Full constructor.
   *
   * @param min
   * @param hour
   * @param mday
   * @param month
   * @param wday
   * @param type
   * @param numOccurs
   */
  public Schedule(String min, String hour, String mday, String month,
                  String wday, String type, Integer numOccurs) {
    this.setMinute(min);
    this.setHour(hour);
    this.setMonth(month);
    this.setMday(mday);
    this.setWday(wday);
    this.setType(type);
    this.setNumOccurs(numOccurs);
    this.setSuspended(false);
  }

  /**
   * Copies information from an Inca schema XmlBean Schedule object so that
   * this object contains equivalent information.
   *
   * @param o the XmlBean Schedule object to copy
   * @return this, for convenience
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Schedule)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Schedule object so that
   * this object contains equivalent information.
   *
   * @param s the XmlBean Schedule object to copy
   * @return this, for convenience
   */
  public Schedule fromBean(edu.sdsc.inca.dataModel.util.Schedule s) {
    if(s.isSetCron()) {
      edu.sdsc.inca.dataModel.util.Cron cron = s.getCron();
      this.setType("cron");
      this.setMday(cron.getMday());
      this.setHour(cron.getHour());
      this.setMinute(cron.getMin());
      this.setMonth(cron.getMonth());
      this.setWday(cron.getWday());
    } else if(s.isSetCrontime()) {
      String[] pieces = s.getCrontime().split("\\s+");
      this.setType("cron");
      this.setMday(pieces.length > 2 ? pieces[2] : "");
      this.setHour(pieces.length > 1 ? pieces[1] : "");
      this.setMinute(pieces[0]);
      this.setMonth(pieces.length > 3 ? pieces[3] : "");
      this.setWday(pieces.length > 4 ? pieces[4] : "");
    } else {
      this.setType("immediate");
    }
    String numOccurs = s.getNumOccurs();
    if(numOccurs != null) {
      this.setNumOccurs(Integer.valueOf(numOccurs));
    }
    this.setSuspended(s.getSuspended());
    return this;
  }

  /**
   * Calculates the time a result will expire based on it's schedule.
   * We are currently saying a result expires two cycles after it was collected
   *
   * @param resultCollectedDate  The time the last result was collected
   *
   * @return The time the result expires
   */
  public Calendar calculateExpires(Date resultCollectedDate) {
    XmlCalendar resultCollected = new XmlCalendar();
    resultCollected.setTime(resultCollectedDate);
    return this.calculateExpires(resultCollected);
  }

  /**
   * Calculates the time a result will expire based on it's schedule.
   * We are currently saying a result expires two cycles after it was collected
   *
   * @param resultCollected  The time the last result was collected
   *
   * @return The time the result expires
   */
    public Calendar calculateExpires(Calendar resultCollected) {
    // Note that the instance expires two cycles after it was collected
    Date expires = this.nextEvent(resultCollected.getTime());
    if(expires != null) {
      expires = this.nextEvent(expires);

    }
    if(expires != null) {
      XmlCalendar gmtExpires = new XmlCalendar();
      gmtExpires.setTime(expires);
      gmtExpires.set(Calendar.MILLISECOND, 0); // needed for testing so always returns same millisecond
      return gmtExpires;
    }
    return null;
  }

  /**
   * Return the minute portion of the cron schedule.
   *
   * @return the minute cron field
   */
  public String getMinute() {
    return this.minute;
  }

  /**
   * Returns the first date/time after a specified date/time that this schedule
   * will cause an event.  Returns null if no later event will ever occur.
   *
   * @param d a date/time preceding the returned value
   * @return the next date/time an event will occur
   */
  public Date nextEvent(Date d) {
    if(this.suspended) {
      return null;
    } else if(this.numOccurs != null && this.numOccurs.intValue() >= 0) {
      // TODO: Debatable assumption that a non-null numOccurs means the final
      // event has already occurred.
      return null;
    }
    String spec = this.minute + " " + this.hour + " " + this.mday + " " +
                  this.month + " " + this.wday;
    spec = spec.replaceAll("\\?=", "");
    try {
      return CronSchedule.parse(spec).nextEvent(d);
    } catch(Exception e) {
      return null;
    }
  }

  /**
   * Set the minute portion of the cron schedule.
   *
   * @param minute this minute portion of the schedule
   */
  public void setMinute(String minute) {
    this.minute = normalize(minute, Row.MAX_DB_STRING_LENGTH, "minute");
  }

  /**
   * Return the hour portion of the cron schedule.
   *
   * @return the hour cron field
   */
  public String getHour() {
    return this.hour;
  }

  /**
   * Set the hour portion of the cron schedule.
   *
   * @param hour this hour portion of the schedule
   */
  public void setHour(String hour) {
    this.hour = normalize(hour, Row.MAX_DB_STRING_LENGTH, "hour");
  }

  /**
   * Return the month portion of the cron schedule.
   *
   * @return the month cron field
   */
  public String getMonth() {
    return this.month;
  }

  /**
   * Set the month portion of the cron schedule.
   *
   * @param month the month portion of the schedule
   */
  public void setMonth(String month) {
    this.month = normalize(month, Row.MAX_DB_STRING_LENGTH, "month");
  }

  /**
   * Return the mday portion of the cron schedule.
   *
   * @return the month day cron field
   */
  public String getMday() {
    return this.mday;
  }

  /**
   * Set the month day portion of the schedule.
   *
   * @param mday the month day portion of the schedule
   */
  public void setMday(String mday) {
    this.mday = normalize(mday, Row.MAX_DB_STRING_LENGTH, "mday");
  }

  /**
   * Return the wday portion of the cron schedule.
   *
   * @return the week day cron field.
   */
  public String getWday() {
    return this.wday;
  }

  /**
   * Set the week day portion of the schedule.
   *
   * @param wday the week day portion of the schedule
   */
  public void setWday(String wday) {
    this.wday = normalize(wday, Row.MAX_DB_STRING_LENGTH, "wday");
  }

  /**
   * Returns the type of schedule.
   *
   * @return the type of schedule
   */
  public String getType() {
    return this.type;
  }

  /**
   * Set the type of schedule.  Current recognized values are "cron" and
   * "immediate".
   *
   * @param type
   */
  public void setType(String type) {
    this.type = normalize(type, Row.MAX_DB_STRING_LENGTH, "schedule type");
  }

  /**
   * Null implies unlimited.
   *
   * @return the number of times the schedule will be executed.
   */
  public Integer getNumOccurs() {
    return numOccurs;
  }

  /**
   * Set the number of times the schedule should be run before being
   * discarded.  Null indicates unlimited.
   * @param numOccurs
   */
  public void setNumOccurs(Integer numOccurs) {
    this.numOccurs = numOccurs;
  }

  /**
   * Returns whether or not further execution has been suspended pending
   * a user request to resume.
   * @return the suspension status
   */
  public boolean getSuspended() {
    return this.suspended;
  }

  /**
   * Indicates whether or not further execution should be suspended pending
   * a user request to resume.
   * @param suspended the suspension status
   */
  public void setSuspended(boolean suspended) {
    this.suspended = suspended;
  }

  /**
   * Returns a Inca schema XmlBean Schedule object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Schedule object that contains equivalent information
   */
  @Override
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.Schedule result =
      edu.sdsc.inca.dataModel.util.Schedule.Factory.newInstance();
    if(this.getType().equals("cron")) {
      edu.sdsc.inca.dataModel.util.Cron cron = result.addNewCron();
      cron.setMday(this.getMday());
      cron.setHour(this.getHour());
      cron.setMin(this.getMinute());
      cron.setMonth(this.getMonth());
      cron.setWday(this.getWday());
    }
    if(this.getNumOccurs() != null) {
      result.setNumOccurs(this.getNumOccurs().toString());
    }
    result.setSuspended(this.getSuspended());
    return result;
  }

  @Override
  public String toString() {
    String result = this.getType();
    if(result.equals("cron")) {
      result += " " + this.getMinute() +
                " " + this.getHour() +
                " " + this.getMonth() +
                " " + this.getMday() +
                " " + this.getWday();
    }
    if(this.getNumOccurs() != null) {
      result += " " + this.getNumOccurs();
    }
    result += " " + this.getSuspended();
    return result;
  }

  /**
   * Override of the default equals method.
   */
  @Override
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }

}
