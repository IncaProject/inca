package edu.sdsc.inca;

import edu.sdsc.inca.dataModel.util.AcceptedOutput;
import edu.sdsc.inca.dataModel.util.AnyXmlSequence;
import edu.sdsc.inca.dataModel.util.Args;
import edu.sdsc.inca.dataModel.util.Args.Arg;
import edu.sdsc.inca.dataModel.util.Cron;
import edu.sdsc.inca.dataModel.util.Limits;
import edu.sdsc.inca.dataModel.util.Notifications.Notification;
import edu.sdsc.inca.dataModel.util.Schedule;
import edu.sdsc.inca.dataModel.util.Series;
import edu.sdsc.inca.dataModel.util.SeriesConfig;
import edu.sdsc.inca.dataModel.util.Tags;
import edu.sdsc.inca.util.XmlWrapper;
import java.awt.Color;
import java.util.Enumeration;
import java.util.Properties;

/**
 * A class that wraps a SeriesConfig and its accompanying Series with some
 * convenience methods.
 */
public class WrapSeries {

  protected Color color;
  protected SeriesConfig config;
  protected Series series;

  /**
   * Constructs a new WrapSeries.
   */
  public WrapSeries() {
    this.config = SeriesConfig.Factory.newInstance();
    this.series = this.config.addNewSeries();
    this.config.addNewSchedule();
    this.setReporter("");
    this.setResource("");
    this.setCron("? ? * * *");
    this.setAction("add");
  }

  /**
   * Constructs a new WrapSeries to wrap an existing SeriesConfig.
   *
   * @param config the SeriesConfig to wrap
   */
  public WrapSeries(SeriesConfig config) {
    this.config = config;
    this.series = config.getSeries();
  }

  /**
   * Copies all information from another series into this one.
   *
   * @param original the WrapSeries to duplicate
   */
  public void copy(WrapSeries original) {
    this.setTags(original.getTags());
    this.setAcceptedComparison(original.getAcceptedComparison());
    this.setAcceptedComparitor(original.getAcceptedComparitor());
    this.setAcceptedTarget
      (original.getAcceptedNotifier(), original.getAcceptedTarget());
    this.setAction(original.getAction());
    this.setArgs(original.getArgs());
    this.setContext(original.getContext());
    this.setCpuLimit(original.getCpuLimit());
    this.setCron(original.getCron());
    this.setMemoryLimit(original.getMemoryLimit());
    this.setNickname(original.getNickname());
    this.setReporter(original.getReporter());
    this.setReporterVersion(original.getReporterVersion());
    this.setResource(original.getResource());
    this.setWallClockLimit(original.getWallClockLimit());
  }

  /**
   * Override of the default equals method.
   *
   * @param o the object to compare to this one
   * @return true iff o specifies the same series
   */
  public boolean equals(Object o) {
    if(!(o instanceof WrapSeries)) {
      return false;
    }
    // NOTE: Although XML comparison seems like it might be fragile due to
    // formatting issues, it appears to work well enough for our purposes.
    String oXml =
      ((WrapSeries)o).toXml().replaceAll(" xmlns[^\"]*\"[^\"]*\"", "");
    String thisXml = this.toXml().replaceAll(" xmlns[^\"]*\"[^\"]*\"", "");
    return oXml.equals(thisXml);
  }

  /**
   * Returns the output comparison associated with series acceptable output
   * testing, or null if there is none.
   *
   * @return the accepted output body, null if none
   */
  public String getAcceptedComparison() {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    return ao == null ? null : ao.getComparison();
  }

  /**
   * Returns the Comparitor class name associated with series acceptable output
   * testing, or null if there is none.
   *
   * @return the accepted output Comparitor class name, null if none
   */
  public String getAcceptedComparitor() {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    return ao == null ? null : ao.getComparitor();
  }

  /**
   * Returns the notifier associated with series acceptable output testing, or
   * null if there is none.
   *
   * @return the accepted notifier, null if none
   */
  public String getAcceptedNotifier() {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    if(ao == null || ao.getNotifications() == null) {
      return null;
    }
    Notification[] notifs = ao.getNotifications().getNotificationArray();
    return notifs.length == 0 ? null : notifs[0].getNotifier();
  }

  /**
   * Returns the notification target associated with series acceptable output
   * testing, or null if there is none.
   *
   * @return the accepted notification target, null if none
   */
  public String getAcceptedTarget() {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    if(ao == null || ao.getNotifications() == null) {
      return null;
    }
    Notification[] notifs = ao.getNotifications().getNotificationArray();
    return notifs.length == 0 ? null : notifs[0].getTarget();
  }

  /**
   * Returns the action ("add", "delete") associated with the series.
   *
   * @return the action associated with the series
   */
  public String getAction() {
    return this.config.getAction();
  }

  /**
   * Returns the reporter arguments associated with the series.
   *
   * @return the arguments as a Properties with the argument names as the keys
   *         and the argument values as the values
   */
  public Properties getArgs() {
    Args args = this.series.getArgs();
    Properties result = new Properties();
    if(args == null) {
      return result;
    }
    Arg[] allArgs = args.getArgArray();
    for(int i = 0; i < allArgs.length; i++) {
      Arg arg = allArgs[i];
      result.setProperty(arg.getName(), arg.getValue());
    }
    return result;
  }

  /**
   * Returns the display color used with this series.
   *
   * @return this series' display color
   */
  public Color getColor() {
    return this.color;
  }

  /**
   * Returns the context string for the series.
   *
   * @return the series context string
   */
  public String getContext() {
    return this.series.getContext();
  }

  /**
   * Returns the maximum CPU seconds an instance of the series is allowed to
   * use; null if unlimited.
   *
   * @return the maximum allowed CPU usage, null if unlimited
   */
  public String getCpuLimit() {
    Limits l = this.series.getLimits();
    return l == null ? null : l.getCpuTime();
  }

  /**
   * Returns the schedule associated with the series via a cron spec.
   *
   * @return a cron spec--a space-delimited list of values for the minutes,
   *         hours, day of month, month, and day of week for the schedule
   * @see crontab(5)
   */
  public String getCron() {
    Schedule s = this.config.getSchedule();
    if(s.isSetCron()) {
      Cron c = this.config.getSchedule().getCron();
      return (c.getMin() + " " + c.getHour() + " " + c.getMday() + " " +
              c.getMonth() + " " + c.getWday());
    } else if(s.isSetCrontime()) {
      return s.getCrontime();
    }
    return null;
  }
 
  /**
   * Returns the maximum Memory MBs an instance of the series is allowed to
   * use; null if unlimited.
   *
   * @return the maximum allowed memory usage, null if unlimited
   */
  public String getMemoryLimit() {
    Limits l = this.series.getLimits();
    return l == null ? null : l.getMemory();
  }

  /**
   * Returns the nickname for the series.
   *
   * @return the series nickname
   */
  public String getNickname() {
    return this.config.getNickname();
  }

  /**
   * Returns the name of the reporter.
   *
   * @return the reporter name
   */
  public String getReporter() {
    return this.series.getName();
  }

  /**
   * Returns the version of the reporter.
   *
   * @return the reporter version, null for latest
   */
  public String getReporterVersion() {
    return this.series.getVersion();
  }

  /**
   * Returns the name of the resource group associated with this series.
   *
   * @return the name of the series resource group
   */
  public String getResource() {
    return this.config.isSetResourceSetName()?this.config.getResourceSetName() :
           this.config.isSetResourceXpath() ? this.config.getResourceXpath() :
           this.config.getResourceHostname();
  }

  /**
   * Returns the SeriesConfig contained in this WrapSeries.
   *
   * @return the wrapped SeriesConfig
   */
  public SeriesConfig getSeries() {
    return this.config;
  }

  /**
   * Returns the series tags or null if there is none.
   *
   * @return the accepted output Comparitor class name, null if none
   */
  public String[] getTags() {
    Tags tags = this.config.getTags();
    return tags == null ? null : tags.getTagArray();
  }

  /**
   * Returns the maximum wall clock seconds an instance of the series is
   * allowed to use; null if unlimited.
   *
   * @return the maximum allowed wall clock usage, null if unlimited
   */
  public String getWallClockLimit() {
    Limits l = this.series.getLimits();
    return l == null ? null : l.getWallClockTime();
  }

  /**
   * Sets the output comparison associated with series acceptable output
   * testing.
   *
   * @param comparison the accepted output comparison, null if none
   */
  public void setAcceptedComparison(String comparison) {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    if(comparison != null) {
      if(ao == null) {
        ao = this.config.addNewAcceptedOutput();
      }
      ao.setComparison(comparison);
    } else if(ao != null) {
      this.config.unsetAcceptedOutput();
    }
  }


  /**
   * Sets the Comparitor class name associated with series acceptable output
   * testing.
   *
   * @param comparitor the accepted output Comparitor class name, null if none
   */
  public void setAcceptedComparitor(String comparitor) {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    if(comparitor != null) {
      if(ao == null) {
        ao = this.config.addNewAcceptedOutput();
      }
      ao.setComparitor(comparitor);
    } else if(ao != null) {
      this.config.unsetAcceptedOutput();
    }
  }

  /**
   * Sets the notifier and notification target associated with series
   * acceptable output testing.
   *
   * @param notifier the accepted notifier, null if none
   * @param target the accepted notification target, null if none
   */
  public void setAcceptedTarget(String notifier, String target) {
    AcceptedOutput ao = this.config.getAcceptedOutput();
    if(ao != null && ao.isSetNotifications()) {
      ao.unsetNotifications();
    }
    if(notifier != null && target != null) {
      if(ao == null) {
        ao = this.config.addNewAcceptedOutput();
      }
      Notification notif = ao.addNewNotifications().addNewNotification();
      notif.setNotifier(notifier);
      notif.setTarget(target);
    }
  }

  /**
   * Sets the action associated with this series to a specified value.
   *
   * @param action the action value ("add", "delete")
   */
  public void setAction(String action) {
    this.config.setAction(action);
  }

  /**
   * Sets the reporter arguments associated with the series.
   *
   * @param args the arguments as a Properties with the argument names as the
   *             keys and the argument values as the values
   */
  public void setArgs(Properties args) {
    Arg[] allArgs = new Arg[args.size()];
    Enumeration e = args.propertyNames();
    for(int i = 0; i < allArgs.length; i++) {
      String name = (String)e.nextElement();
      Arg arg = Arg.Factory.newInstance();
      arg.setName(name);
      arg.setValue(args.getProperty(name));
      allArgs[i] = arg;
    }
    if(this.series.isSetArgs()) {
      this.series.unsetArgs();
    }
    if(allArgs.length > 0) {
      this.series.addNewArgs().setArgArray(allArgs);
    }
  }

  /**
   * Sets the display color used with this series.
   *
   * @param c this series' display color
   */
  public void setColor(Color c) {
    this.color = c;
  }

  /**
   * Sets the context string for the series.
   *
   * @param context the context string of the series
   */
  public void setContext(String context) {
    this.series.setContext(context);
  }

  /**
   * Sets the maximum CPU seconds an instance of the series is allowed to use.
   *
   * @param limit the maximum allowed CPU usage in seconds, null if unlimited
   */
  public void setCpuLimit(String limit) {
    Limits l = this.series.getLimits();
    if(limit != null) {
      if(l == null) {
        l = this.series.addNewLimits();
      }
      l.setCpuTime(limit);
    } else if(l != null && l.isSetCpuTime()) {
      l.unsetCpuTime();
      if(!l.isSetMemory() && !l.isSetWallClockTime()) {
        this.series.unsetLimits();
      }
    }
  }

  /**
   * Sets the schedule associated with the series via a cron spec.
   *
   * @param cron a cron spec--a space-delimited list of values for the minutes,
   *             hours, day of month, month, and day of week for the schedule
   * @see crontab(1)
   */
  public void setCron(String cron) {
    Schedule s = this.config.getSchedule();
    if(s.isSetCron()) {
      s.unsetCron();
    } else if(s.isSetCrontime()) {
      s.unsetCrontime();
    }
    if(cron != null) {
      s.setCrontime(cron);
    }
  }

  /**
   * Sets the maximum memory MBs an instance of the series is allowed to use.
   *
   * @param limit the maximum allowed memory usage in MBs, null if unlimited
   */
  public void setMemoryLimit(String limit) {
    Limits l = this.series.getLimits();
    if(limit != null) {
      if(l == null) {
        l = this.series.addNewLimits();
      }
      l.setMemory(limit);
    } else if(l != null && l.isSetMemory()) {
      l.unsetMemory();
      if(!l.isSetCpuTime() && !l.isSetWallClockTime()) {
        this.series.unsetLimits();
      }
    }
  }

  /**
   * Sets the nickname for the series.
   *
   * @param nickname the nickname of the series
   */
  public void setNickname(String nickname) {
    this.config.setNickname(nickname);
  }

  /**
   * Sets the name of the reporter that this series runs.
   *
   * @param name the name of the reporter
   */
  public void setReporter(String reporter) {
    this.series.setName(reporter);
  }

  /**
   * Sets the version of the reporter that this series runs.
   *
   * @param version the version of the reporter, null for latest
   */
  public void setReporterVersion(String version) {
    if(version != null) {
      this.series.setVersion(version);
    } else if(this.series.isSetVersion()) {
      this.series.unsetVersion();
    }
  }

  /**
   * Sets the name of the resource/group to which this series applies.
   *
   * @param name the name for the resource/group
   */
  public void setResource(String name) {
    this.config.setResourceSetName(name);
  }

  /**
   * Sets the tags
   *
   * @param tagStrings array of tags or null
   */
  public void setTags(String[] tagStrings) {
    Tags tags = this.config.getTags();
    if(tagStrings != null) {
      if(tags == null) {
        tags = this.config.addNewTags();
      }
      tags.setTagArray(tagStrings);
    } else if(tags != null) {
      this.config.unsetTags();
    }
  }

  /**
   * Sets the maximum wall seconds an instance of the series is allowed to use.
   *
   * @param limit the maximum allowed wall usage in secs, null if unlimited
   */
  public void setWallClockLimit(String limit) {
    Limits l = this.series.getLimits();
    if(limit != null) {
      if(l == null) {
        l = this.series.addNewLimits();
      }
      l.setWallClockTime(limit);
    } else if(l != null && l.isSetWallClockTime()) {
      l.unsetWallClockTime();
      if(!l.isSetCpuTime() && !l.isSetMemory()) {
        this.series.unsetLimits();
      }
    }
  }

  /**
   * An override of the default toString function.
   */
  public String toString() {
    String result = this.getNickname();
    return result == null || result.equals("") ? this.getReporter() : result;
  }

  /**
   * Returns XML for the series.
   *
   * @return the suite, as an XML string
   */
  public String toXml() {
    return XmlWrapper.prettyPrint(this.config.toString(), "  ");
  }

}
