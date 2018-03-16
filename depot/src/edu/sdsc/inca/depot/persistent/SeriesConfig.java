package edu.sdsc.inca.depot.persistent;


import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.sdsc.inca.dataModel.util.Tags;
import org.apache.xmlbeans.XmlObject;


/**
 * A SeriesConfig Represents the configuration of a Series as part of a Suite.
 */
public class SeriesConfig extends PersistentObject implements Comparable {

  /** id set iff this object is stored in the DB. */
  private Long id;

  /** Persistent fields. */
  private Date activated;
  private Date deactivated;
  private String nickname;
  private Limits limits;
  private AcceptedOutput acceptedOutput;
  private Schedule schedule;

  /** Relations. */
  private Set<Suite> suites;
  private Set<String> tags;
  private Series series;
  private Long latestInstanceId;
  private Long latestComparisonId;

  /** Non-persistent fields. */
  private String action;

  /**
   * Default constructor.
   */
  public SeriesConfig() {
    this(null, null);
  }

  /**
   * Full constructor.
   */
  public SeriesConfig(Suite suite, Series series) {
    this.setActivated(Calendar.getInstance().getTime());
    this.setDeactivated(null);
    this.setNickname("");
    this.setLimits(null);
    this.setAcceptedOutput(null);
    this.setSchedule(null);
    this.setSuites(new HashSet<Suite>());
    this.setTags(new HashSet<String>());
    this.addSuite(suite);
    this.setSeries(series);
    this.setLatestInstanceId(new Long(-1));
    this.setLatestComparisonId(new Long(-1));
    this.setAction("add");
  }

  /**
   * Overrides default comparison to order SeriesConfigs by their activated
   * timestamp
   *
   * @param o  The SeriesConfig to compare this SeriesConfig to
   * @return -1 if o is younger, 0 if equal, and 1 if older
   */
  public int compareTo( Object o ) {
    SeriesConfig that = (SeriesConfig)o;
    return this.getActivated().compareTo(that.getActivated() );
  }

  /**
   * Copies information from an Inca schema XmlBean SeriesConfig object so that
   * this object contains equivalent information.
   *
   * @param o the XmlBean SeriesConfig object to copy
   * @return this, for convenience
   */
  public PersistentObject fromBean(XmlObject o) {
    return this.fromBean((edu.sdsc.inca.dataModel.util.SeriesConfig)o);
  }

  /**
   * Copies information from an Inca schema XmlBean SeriesConfig object so that
   * this object contains equivalent information.
   *
   * @param sc the XmlBean SeriesConfig object to copy
   * @return this, for convenience
   */
  public SeriesConfig fromBean(edu.sdsc.inca.dataModel.util.SeriesConfig sc) {
    if(sc.getSeries().getLimits() != null) {
      this.setLimits(new Limits().fromBean(sc.getSeries().getLimits()));
    }
    if(sc.getAcceptedOutput() != null) {
      this.setAcceptedOutput
        (new AcceptedOutput().fromBean(sc.getAcceptedOutput()));
    }
    this.setNickname(sc.getNickname());
    this.setSchedule(new Schedule().fromBean(sc.getSchedule()));
    this.setSeries(new Series().fromBean(sc.getSeries()));
    if(sc.isSetResourceHostname()) {
      this.getSeries().setResource(sc.getResourceHostname());
    } else if(sc.isSetResourceSetName()) {
      this.getSeries().setResource(sc.getResourceSetName());
    } else if(sc.isSetResourceXpath()) {
      this.getSeries().setResource(sc.getResourceXpath());
    }
    if(sc.isSetTargetHostname()) {
      this.getSeries().setTargetHostname(sc.getTargetHostname());
    }
    if (sc.getTags() != null) {
      for (String tag : sc.getTags().getTagArray())
        this.getTags().add(tag);
    }
    this.setAction(sc.getAction());
    return this;
  }

  /**
   * Retrieve the id -- null if not yet connected to database.
   *
   * @return The Long representation of the DB ID
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.  Hibernate use only.
   *
   * @param id The DB ID.
   */
  protected void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the version number of the Suite under which this SeriesConfig was
   * most recently activated (i.e., its Series was running); -1 if none.
   *
   * @return the date that the SeriesConfig was activated
   */
  public Date getActivated() {
    return this.activated;
  }

  /**
   * Sets the version number of the Suite under which this SeriesConfig was
   * most recently activated (i.e., its Series was running); -1 if none.
   *
   * @param datetime date that the SeriesConfig was activated
   */
  public void setActivated(Date datetime) {
    this.activated = datetime;
  }

  /**
   * Returns the version number of the Suite under which this SeriesConfig was
   * most recently deactivated (i.e., its Series was suspended); -1 if none.
   *
   * @return the date that the SeriesConfig was deactivated
   */
  public Date getDeactivated() {
    return this.deactivated;
  }

  /**
   * Sets the version number of the Suite under which this SeriesConfig was
   * most recently deactivated (i.e., its Series was suspended); -1 if none.
   *
   * @param datetime date that the SeriesConfig was deactivated
   */
  public void setDeactivated(Date datetime) {
    this.deactivated = datetime;
  }

  /**
   * Get the user-supplied SeriesConfig nickname.
   *
   * @return the SeriesConfig nickname
   */
  public String getNickname() {
    return this.nickname;
  }

  /**
   * Set the user-supplied SeriesConfig nickname.
   *
   * @param nickname the SeriesConfig nickname
   */
  public void setNickname(String nickname) {
    if(nickname == null || nickname.equals("")) {
      nickname = DB_EMPTY_STRING;
    }
    this.nickname = truncate(nickname, MAX_DB_STRING_LENGTH, "nickname");
  }

  /**
   * Returns the resource limits for this SeriesConfig.
   *
   * @return the resource limits
   */
  public Limits getLimits() {
    return this.limits;
  }

  /**
   * Sets the resource limits for this SeriesConfig.
   *
   * @param l the resource limits
   */
  public void setLimits(Limits l) {
    this.limits = l;
  }

  /**
   * Returns comparison and action information for this SeriesConfig.
   *
   * @return the comparison and action information
   */
  public AcceptedOutput getAcceptedOutput() {
    return this.acceptedOutput;
  }

  /**
   * Sets comparison and action information for this SeriesConfig.
   *
   * @param ao the comparison and action information
   */
  public void setAcceptedOutput(AcceptedOutput ao) {
    this.acceptedOutput = ao;
  }

  /**
   * Returns the execution schedule for this SeriesConfig.
   *
   * @return the execution schedule
   */
  public Schedule getSchedule() {
    return this.schedule;
  }

  /**
   * Sets the execution schedule for this SeriesConfig.
   *
   * @param s the execution schedule
   */
  public void setSchedule(Schedule s) {
    this.schedule = s;
  }

  /**
   * Returns a Suite associated with this SeriesConfig.
   *
   * @param i the offset of the desired Suite
   * @return the associated Suite
   */
  public Suite getSuite(int i) {
    if (i >= suites.size())
      return null;

    Iterator<Suite> it = suites.iterator();

    while (i > 0) {
      it.next();

      i -= 1;
    }

    return it.next();
  }

  /**
   * Returns the set of Suites associated with this SeriesConfig
   *
   * @return the set of associated Suites
   */
  public Set<Suite> getSuites() {
    return this.suites;
  }

  /**
   * Sets the set of Suites associated with this SeriesConfig
   *
   * @param s the associated Suite
   */
  public void setSuites(Set<Suite> s) {
    this.suites = s;
  }

  /**
   * Adds a Suite to the set of Suites associated with this SeriesConfig.
   * Used by Suite.addSeriesConfig
   *
   * @param s the Suite to be added
   */
  public void addSuite(Suite s) {
    this.suites.add(s);
  }

  /**
   * Returns a tag associated with this SeriesConfig.
   *
   * @param i the offset of the desired tag
   * @return the associated tag
   */
  public String getTag(int i) {
    if (i >= tags.size())
      return null;

    Iterator<String> it = tags.iterator();

    while (i > 0) {
      it.next();

      i -= 1;
    }

    return it.next();
  }

  /**
   * Returns the set of tags associated with this SeriesConfig
   *
   * @return the set of associated tags
   */
  public Set<String> getTags() {
    return this.tags;
  }

  /**
   * Sets the set of tags associated with this SeriesConfig
   *
   * @param t the associated tags
   */
  public void setTags(Set<String> t) {
    this.tags = t;
  }

  /**
   * Returns the Series associated with this SeriesConfig.
   *
   * @return the associated Series
   */
  public Series getSeries() {
    return this.series;
  }

  /**
   * Sets the Series associated with this SeriesConfig.
   *
   * @param s the associated Series
   */
  public void setSeries(Series s) {
    this.series = s;
  }

  /**
   * Returns the DB id of the latest InstanceInfo associated with this
   * SeriesConfig; -1 if none.
   *
   * @return the latest instance DB id
   */
  public Long getLatestInstanceId() {
    return this.latestInstanceId;
  }

  /**
   * Sets the DB id of the latest InstanceInfo associated with this
   * SeriesConfig; -1 if none.
   *
   * @param id the latest instance DB id
   */
  public void setLatestInstanceId(Long id) {
    this.latestInstanceId = id;
  }

  /**
   * Returns the DB id of the latest ComprisonResult associated with this
   * SeriesConfig; -1 if none.
   *
   * @return the latest comparison DB id
   */
  public Long getLatestComparisonId() {
    return this.latestComparisonId;
  }

  /**
   * Sets the DB id of the latest ComparisonResult associated with this
   * SeriesConfig; -1 if none.
   *
   * @param id the latest comparison DB id
   */
  public void setLatestComparisonId(Long id) {
    this.latestComparisonId = id;
  }

  /**
   * Gets the editing action associated with this SeriesConfig.  This is used
   * by Inca commands in computing suite deltas; it is not stored in the DB.
   *
   * @return the associated editing action
   */
  public String getAction() {
    return this.action;
  }

  /**
   * Sets the editing action associated with this SeriesConfig.  This is used
   * by Inca commands in computing suite deltas; it is not stored in the DB.
   *
   * @param action the associated editing action
   */
  public void setAction(String action) {
    if(action == null) {
      action = "";
    }
    this.action = action;
  }

  /**
   * Returns a Inca schema XmlBean SeriesConfig object that contains
   * information equivalent to this object.
   *
   * @return an XmlBean SeriesConfig object that contains equivalent information
   */
  public XmlObject toBean() {
    edu.sdsc.inca.dataModel.util.SeriesConfig result =
      edu.sdsc.inca.dataModel.util.SeriesConfig.Factory.newInstance();
    if(this.getAcceptedOutput() != null) {
      result.setAcceptedOutput((edu.sdsc.inca.dataModel.util.AcceptedOutput)this.getAcceptedOutput().toBean());
    }
    result.setNickname(this.getNickname());
    result.setSchedule
      ((edu.sdsc.inca.dataModel.util.Schedule)this.getSchedule().toBean());
    result.setSeries
      ((edu.sdsc.inca.dataModel.util.Series)this.getSeries().toBean());
    if(this.getLimits() != null) {
      result.getSeries().setLimits
        ((edu.sdsc.inca.dataModel.util.Limits)this.getLimits().toBean());
    }
    if (!this.getTags().isEmpty()) {
      Tags newTags = result.addNewTags();
      newTags.setTagArray(this.getTags().toArray(new String[this.getTags().size()]));
    }
    result.setResourceHostname(this.getSeries().getResource());
    result.setTargetHostname(this.getSeries().getTargetHostname());
    result.setAction(this.getAction());
    return result;
  }

  /**
   * Returns a string representation of this SeriesConfig.
   *
   * @return a string representation
   */
  public String toString() {
    String result = this.acceptedOutput == null ? "" : this.acceptedOutput.toString();

    result += "," + (this.nickname == null ? "" : this.nickname);
    result += "," + (this.series == null ? "" : this.series.toString());

    return result;
  }

  /**
   * Compares another object to this SeriesConfig for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same SeriesConfig
   */
  public boolean equals(Object o) {
    if(o == this) {
      return true;
    } else if(!(o instanceof SeriesConfig)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  /**
   * Returns a phony SeriesConfig with a given resource and context.  Useful
   * for testing.
   *
   * @param resource the series resource
   * @param context the series context
   * @param args the number of phony arguments to generate
   * @return a new Series
   */
  public static SeriesConfig
    generate(String resource, String context, int args) {
    SeriesConfig result = new SeriesConfig();
    result.setNickname("sc nickname");
    result.setSchedule(new Schedule());
    result.setSeries(Series.generate(resource, context, args));
    return result;
  }

}
