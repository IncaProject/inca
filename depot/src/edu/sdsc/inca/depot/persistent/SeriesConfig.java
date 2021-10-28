/*
 * SeriesConfig.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;

import edu.sdsc.inca.dataModel.util.Tags;


/**
 * A SeriesConfig Represents the configuration of a Series as part of a Suite.
 */
public class SeriesConfig extends GeneratedKeyRow implements Comparable<SeriesConfig> {

  // nested classes


  /**
   *
   */
  private class AddSuiteOp implements RowOperation {

    // data fields


    private final Suite m_element;


    // constructors


    protected AddSuiteOp(Suite element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      assert !m_element.isNew();

      Column<Long> suiteId = new LongColumn("incasuite_id", false, m_element.getId());
      Column<Long> seriesConfigId = new LongColumn("incaseriesconfig_id", false, getId());
      List<Column<?>> cols = new ArrayList<Column<?>>();

      cols.add(suiteId);
      cols.add(seriesConfigId);

      (new InsertOp("INCASUITESSERIESCONFIGS", cols)).execute(dbConn);
    }
  }

  /**
   *
   */
  private class RemoveSuiteOp implements RowOperation {

    // data fields


    private final Suite m_element;


    // constructors


    protected RemoveSuiteOp(Suite element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      assert !m_element.isNew();

      Column<Long> suiteId = new LongColumn("incasuite_id", false, m_element.getId());
      Column<Long> seriesConfigId = new LongColumn("incaseriesconfig_id", false, getId());
      CompositeKey key = new CompositeKey(suiteId, seriesConfigId);

      (new DeleteOp("INCASUITESSERIESCONFIGS", key)).execute(dbConn);
    }
  }

  /**
   *
   */
  private class SuiteSet extends MonitoredSet<Suite> {

    // constructors


    protected SuiteSet(Set<Suite> suites)
    {
      super(suites);
    }


    // protected methods


    @Override
    protected void addSetAddOp(Suite element)
    {
      m_opQueue.add(new AddSuiteOp(element));
    }

    @Override
    protected void addSetRemoveOp(Suite element)
    {
      m_opQueue.add(new RemoveSuiteOp(element));
    }

    @Override
    protected void addSetClearOp(List<Suite> elements)
    {
      for (Suite element : elements)
        addSetRemoveOp(element);
    }
  }

  /**
   *
   */
   private class AddTagOp implements RowOperation {

     // data fields


     private final String m_element;


     // constructors


     protected AddTagOp(String element)
     {
       m_element = element;
     }


     // public methods


     @Override
     public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
     {
       Column<Long> configId = new LongColumn("incaseriesconfig_id", false, getId());
       Column<String> tag = new StringColumn("tag", false, MAX_DB_STRING_LENGTH, m_element);
       List<Column<?>> cols = new ArrayList<Column<?>>();

       cols.add(configId);
       cols.add(tag);

       (new InsertOp("INCASERIESCONFIGTAGS", cols)).execute(dbConn);
     }
   }

   /**
    *
    */
   private class RemoveTagOp implements RowOperation {

     // data fields


     private final String m_element;


     // constructors


     protected RemoveTagOp(String element)
     {
       m_element = element;
     }


     // public methods


     @Override
     public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
     {
       Column<Long> configId = new LongColumn("incaseriesconfig_id", false, getId());
       Column<String> tag = new StringColumn("tag", false, MAX_DB_STRING_LENGTH, m_element);
       CompositeKey tagKey = new CompositeKey(configId, tag);

       (new DeleteOp("INCASERIESCONFIGTAGS", tagKey)).execute(dbConn);
     }
   }

  /**
   *
   */
  private class TagSet extends MonitoredSet<String> {

    // constructors


    protected TagSet(Set<String> tags)
    {
      super(tags);
    }


    // protected methods


    @Override
    protected void addSetAddOp(String element)
    {
      m_opQueue.add(new AddTagOp(element));
    }

    @Override
    protected void addSetRemoveOp(String element)
    {
      m_opQueue.add(new RemoveTagOp(element));
    }

    @Override
    protected void addSetClearOp(List<String> elements)
    {
      for (String element : elements)
        addSetRemoveOp(element);
    }
  }


  // data fields


  private static final String TABLE_NAME = "INCASERIESCONFIG";
  private static final String KEY_NAME = "incaid";
  private final Column<Date> m_activated = new DateColumn("incaactivated", true);
  private final Column<Date> m_deactivated = new DateColumn("incadeactivated", true);
  private final Column<String> m_nickname = new StringColumn("incanickname", true, MAX_DB_STRING_LENGTH);
  private final Column<Float> m_wallClockTime = new FloatColumn("incawallClockTime", true);
  private final Column<Float> m_cpuTime = new FloatColumn("incacpuTime", true);
  private final Column<Float> m_memory = new FloatColumn("incamemory", true);
  private final Column<String> m_comparitor = new StringColumn("incacomparitor", true, MAX_DB_STRING_LENGTH);
  private final Column<String> m_comparison = new StringColumn("incacomparison", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_notifier = new StringColumn("incanotifier", true, MAX_DB_STRING_LENGTH);
  private final Column<String> m_target = new StringColumn("incatarget", true, MAX_DB_STRING_LENGTH);
  private final Column<String> m_type = new StringColumn("incatype", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_minute = new StringColumn("incaminute", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_hour = new StringColumn("incahour", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_month = new StringColumn("incamonth", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_mday = new StringColumn("incamday", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_wday = new StringColumn("incawday", false, MAX_DB_STRING_LENGTH);
  private final Column<Integer> m_numOccurs = new IntegerColumn("incanumOccurs", true);
  private final Column<Boolean> m_suspended = new BooleanColumn("incasuspended", true);
  private final Column<Long> m_seriesId = new LongColumn("incaseries_id", true);
  private final Column<Long> m_latestInstanceId = new LongColumn("incalatestInstanceId", false);
  private final Column<Long> m_latestComparisonId = new LongColumn("incalatestComparisonId", false);
  private final Deque<RowOperation> m_opQueue = new LinkedList<RowOperation>();
  private String m_action;
  private Series m_series;
  private Limits m_limits;
  private AcceptedOutput m_acceptedOutput;
  private Schedule m_schedule;
  private SuiteSet m_suites;
  private TagSet m_tags;


  // constructors


  /**
   *
   */
  public SeriesConfig()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(
      m_activated, m_deactivated, m_nickname, m_wallClockTime, m_cpuTime, m_memory, m_comparitor,
      m_comparison, m_notifier, m_target, m_type, m_minute, m_hour, m_month, m_mday, m_wday,
      m_numOccurs, m_suspended, m_seriesId, m_latestInstanceId, m_latestComparisonId
    );

    setActivated(Calendar.getInstance().getTime());
    setNickname("");
    setLimits(null);
    setAcceptedOutput(null);
    setSchedule(null);
    setSeries(null);
    setLatestInstanceId(-1L);
    setLatestComparisonId(-1L);
    setAction("add");
  }

  /**
   *
   * @param suite
   * @param series
   */
  public SeriesConfig(Suite suite, Series series)
  {
    this();

    setSeries(series);

    m_suites = new SuiteSet(new HashSet<Suite>());

    m_suites.add(suite);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public SeriesConfig(long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load();
  }

  /**
   * @param dbConn
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  SeriesConfig(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load(dbConn);
  }


  // public methods


  /**
   *
   * @return
   */
  public Date getActivated()
  {
    return m_activated.getValue();
  }

  /**
   *
   * @param activated
   */
  public void setActivated(Date activated)
  {
    m_activated.setValue(activated);
  }

  /**
   *
   * @return
   */
  public Date getDeactivated()
  {
    return m_deactivated.getValue();
  }

  /**
   *
   * @param deactivated
   */
  public void setDeactivated(Date deactivated)
  {
    m_deactivated.setValue(deactivated);
  }

  /**
   *
   * @return
   */
  public String getNickname()
  {
    return m_nickname.getValue();
  }

  /**
   *
   * @param nickname
   */
  public void setNickname(String nickname)
  {
    nickname = normalize(nickname, MAX_DB_STRING_LENGTH, "nickname");

    m_nickname.setValue(nickname);
  }

  /**
   *
   * @return
   */
  public Long getLatestInstanceId()
  {
    if (m_latestInstanceId.isNull())
      return null;

    return m_latestInstanceId.getValue();
  }

  /**
   *
   * @param id
   */
  public void setLatestInstanceId(Long id)
  {
    m_latestInstanceId.setValue(id);
  }

  /**
   *
   * @return
   */
  public Long getLatestComparisonId()
  {
    if (m_latestComparisonId.isNull())
      return null;

    return m_latestComparisonId.getValue();
  }

  /**
   *
   * @param id
   */
  public void setLatestComparisonId(Long id)
  {
    m_latestComparisonId.setValue(id);
  }

  /**
   * Returns the resource limits for this SeriesConfig.
   *
   * @return the resource limits
   */
  public Limits getLimits()
  {
    return m_limits;
  }

  /**
   * Sets the resource limits for this SeriesConfig.
   *
   * @param l the resource limits
   */
  public void setLimits(Limits l)
  {
    m_limits = l;
  }

  /**
   * Returns comparison and action information for this SeriesConfig.
   *
   * @return the comparison and action information
   */
  public AcceptedOutput getAcceptedOutput()
  {
    return m_acceptedOutput;
  }

  /**
   * Sets comparison and action information for this SeriesConfig.
   *
   * @param ao the comparison and action information
   */
  public void setAcceptedOutput(AcceptedOutput ao)
  {
    m_acceptedOutput = ao;
  }

  /**
   * Returns the execution schedule for this SeriesConfig.
   *
   * @return the execution schedule
   */
  public Schedule getSchedule()
  {
    return m_schedule;
  }

  /**
   * Sets the execution schedule for this SeriesConfig.
   *
   * @param s the execution schedule
   */
  public void setSchedule(Schedule s)
  {
    m_schedule = s;
  }

  /**
   * Returns a Suite associated with this SeriesConfig.
   *
   * @param i the offset of the desired Suite
   * @return the associated Suite
   */
  public Suite getSuite(int i)
  {
    if (i >= m_suites.size())
      return null;

    Iterator<Suite> it = m_suites.iterator();

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
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<Suite> getSuites() throws IOException, SQLException, PersistenceException
  {
    if (m_suites == null) {
      Set<Suite> suites = new HashSet<Suite>();

      if (!isNew()) {
        try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
             PreparedStatement selectStmt = dbConn.prepareStatement(
               "SELECT incasuite_id " +
               "FROM INCASUITESSERIESCONFIGS " +
               "WHERE incaseriesconfig_id = ?"
        )) {
          m_key.setParameter(selectStmt, 1);

          ResultSet rows = selectStmt.executeQuery();

          while (rows.next()) {
            long suiteId = rows.getLong(1);

            suites.add(new Suite(suiteId));
          }
        }
      }

      m_suites = new SuiteSet(suites);
    }

    return m_suites;
  }

  /**
   * Returns a tag associated with this SeriesConfig.
   *
   * @param i the offset of the desired tag
   * @return the associated tag
   */
  public String getTag(int i)
  {
    if (i >= m_tags.size())
      return null;

    Iterator<String> it = m_tags.iterator();

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
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<String> getTags() throws IOException, SQLException, PersistenceException
  {
    if (m_tags == null) {
      Set<String> tags = new HashSet<String>();

      if (!isNew()) {
        try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
             PreparedStatement selectStmt = dbConn.prepareStatement(
               "SELECT tag " +
               "FROM INCASERIESCONFIGTAGS " +
               "WHERE incaseriesconfig_id = ?"
        )) {
          m_key.setParameter(selectStmt, 1);

          ResultSet rows = selectStmt.executeQuery();

          while (rows.next())
            tags.add(rows.getString(1));
        }
      }

      m_tags = new TagSet(tags);
    }

    return m_tags;
  }

  /**
   * Sets the set of tags associated with this SeriesConfig
   *
   * @param t the associated tags
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public void setTags(Set<String> t) throws IOException, SQLException, PersistenceException
  {
    getTags().clear();

    if (t != null)
      getTags().addAll(t);
  }

  /**
   * Returns the Series associated with this SeriesConfig.
   *
   * @return the associated Series
   */
  public Series getSeries()
  {
    return m_series;
  }

  /**
   * Sets the Series associated with this SeriesConfig.
   *
   * @param s the associated Series
   */
  public void setSeries(Series s)
  {
    m_series = s;
  }

  /**
   * Gets the editing action associated with this SeriesConfig.  This is used
   * by Inca commands in computing suite deltas; it is not stored in the DB.
   *
   * @return the associated editing action
   */
  public String getAction()
  {
    return m_action;
  }

  /**
   * Sets the editing action associated with this SeriesConfig.  This is used
   * by Inca commands in computing suite deltas; it is not stored in the DB.
   *
   * @param action the associated editing action
   */
  public void setAction(String action)
  {
    if (action == null)
      action = "";

    m_action = action;
  }

  /**
   * Copies information from an Inca schema XmlBean SeriesConfig object so that
   * this object contains equivalent information.
   *
   * @param o the XmlBean SeriesConfig object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) throws IOException, SQLException, PersistenceException
  {
    return fromBean((edu.sdsc.inca.dataModel.util.SeriesConfig)o);
  }

  /**
   * Copies information from an Inca schema XmlBean SeriesConfig object so that
   * this object contains equivalent information.
   *
   * @param sc the XmlBean SeriesConfig object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public SeriesConfig fromBean(edu.sdsc.inca.dataModel.util.SeriesConfig sc) throws IOException, SQLException, PersistenceException
  {
    if (sc.getSeries().getLimits() != null)
      setLimits(new Limits().fromBean(sc.getSeries().getLimits()));

    if (sc.getAcceptedOutput() != null)
      setAcceptedOutput(new AcceptedOutput().fromBean(sc.getAcceptedOutput()));

    setNickname(sc.getNickname());
    setSchedule(new Schedule().fromBean(sc.getSchedule()));
    setSeries(new Series().fromBean(sc.getSeries()));

    if (sc.isSetResourceHostname())
      getSeries().setResource(sc.getResourceHostname());
    else if (sc.isSetResourceSetName())
      getSeries().setResource(sc.getResourceSetName());
    else if (sc.isSetResourceXpath())
      getSeries().setResource(sc.getResourceXpath());

    if (sc.isSetTargetHostname())
      getSeries().setTargetHostname(sc.getTargetHostname());

    if (sc.getTags() != null) {
      for (String tag : sc.getTags().getTagArray())
        getTags().add(tag);
    }

    setAction(sc.getAction());

    return this;
  }

  /**
   * Returns a Inca schema XmlBean SeriesConfig object that contains
   * information equivalent to this object.
   *
   * @return an XmlBean SeriesConfig object that contains equivalent information
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public XmlObject toBean() throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.util.SeriesConfig result = edu.sdsc.inca.dataModel.util.SeriesConfig.Factory.newInstance();

    if (getAcceptedOutput() != null)
      result.setAcceptedOutput((edu.sdsc.inca.dataModel.util.AcceptedOutput) getAcceptedOutput().toBean());

    result.setNickname(getNickname());
    result.setSchedule((edu.sdsc.inca.dataModel.util.Schedule) getSchedule().toBean());
    result.setSeries((edu.sdsc.inca.dataModel.util.Series) getSeries().toBean());

    if (getLimits() != null)
      result.getSeries().setLimits((edu.sdsc.inca.dataModel.util.Limits) getLimits().toBean());

    if (!getTags().isEmpty()) {
      Tags newTags = result.addNewTags();

      newTags.setTagArray(getTags().toArray(new String[getTags().size()]));
    }

    result.setResourceHostname(getSeries().getResource());
    result.setTargetHostname(getSeries().getTargetHostname());
    result.setAction(getAction());

    return result;
  }

  /**
   * @param other
   * @return
   */
  @Override
  public boolean equals(Object other)
  {
    if (other == null)
      return false;

    if (this == other)
      return true;

    if (other instanceof SeriesConfig == false)
      return false;

    SeriesConfig otherConfig = (SeriesConfig) other;

    return toString().equals(otherConfig.toString());
  }

  /**
   *
   * @return
   */
  @Override
  public int hashCode()
  {
    return 29 * toString().hashCode();
  }

  /**
   *
   * @param other
   * @return
   */
  @Override
  public int compareTo(SeriesConfig other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }

  /**
   * Returns a string representation of this SeriesConfig.
   *
   * @return a string representation
   */
  @Override
  public String toString()
  {
    String result;

    if (!m_comparitor.isNull() && !m_comparison.isNull())
      result = getAcceptedOutput().toString();
    else
      result = "";

    result += "," + (m_nickname.isNull() ? "" : m_nickname.getValue());
    result += "," + (m_series == null ? "" : m_series.toString());

    return result;
  }

  /**
   * Returns a phony SeriesConfig with a given resource and context.  Useful
   * for testing.
   *
   * @param resource the series resource
   * @param context the series context
   * @param args the number of phony arguments to generate
   * @return a new Series
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static SeriesConfig generate(String resource, String context, int args) throws IOException, SQLException, PersistenceException
  {
    SeriesConfig result = new SeriesConfig();

    result.setNickname("sc nickname");
    result.setSchedule(new Schedule());
    result.setSeries(Series.generate(resource, context, args));

    return result;
  }


  // package methods


  /**
   *
   * @param dbConn
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    m_series.save(dbConn);

    m_seriesId.setValue(m_series.getId());

    if (m_limits != null) {
      m_memory.setValue(m_limits.getMemory());
      m_cpuTime.setValue(m_limits.getCpuTime());
      m_wallClockTime.setValue(m_limits.getWallClockTime());
    }
    else {
      m_memory.setValue(null);
      m_cpuTime.setValue(null);
      m_wallClockTime.setValue(null);
    }

    if (m_acceptedOutput != null) {
      m_comparitor.setValue(m_acceptedOutput.getComparitor());
      m_comparison.setValue(m_acceptedOutput.getComparison());

      Notification notification = m_acceptedOutput.getNotification();

      if (notification != null) {
        m_notifier.setValue(notification.getNotifier());
        m_target.setValue(notification.getTarget());
      }
      else {
        m_notifier.setValue(null);
        m_target.setValue(null);
      }
    }
    else {
      m_comparitor.setValue(null);
      m_comparison.setValue(null);
      m_notifier.setValue(null);
      m_target.setValue(null);
    }

    if (m_schedule != null) {
      m_minute.setValue(m_schedule.getMinute());
      m_hour.setValue(m_schedule.getHour());
      m_mday.setValue(m_schedule.getMday());
      m_month.setValue(m_schedule.getMonth());
      m_wday.setValue(m_schedule.getWday());
      m_type.setValue(m_schedule.getType());
      m_numOccurs.setValue(m_schedule.getNumOccurs());
      m_suspended.setValue(m_schedule.getSuspended());
    }
    else {
      m_minute.setValue(null);
      m_hour.setValue(null);
      m_mday.setValue(null);
      m_month.setValue(null);
      m_wday.setValue(null);
      m_type.setValue(null);
      m_numOccurs.setValue(null);
      m_suspended.setValue(null);
    }

    super.save(dbConn);

    while (!m_opQueue.isEmpty()) {
      RowOperation op = m_opQueue.remove();

      op.execute(dbConn);
    }
  }

  /**
   *
   * @param dbConn
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.load(dbConn);

    m_acceptedOutput = new AcceptedOutput(m_comparitor.getValue(), m_comparison.getValue());

    m_acceptedOutput.setNotification(new Notification(m_notifier.getValue(), m_target.getValue()));

    m_schedule = new Schedule(m_minute.getValue(), m_hour.getValue(), m_mday.getValue(), m_month.getValue(), m_wday.getValue(), m_type.getValue(), m_numOccurs.getValue());

    m_schedule.setSuspended(m_suspended.getValue());

    m_limits = new Limits(m_memory.getValue(), m_cpuTime.getValue(), m_wallClockTime.getValue());
    m_series = new Series(dbConn, m_seriesId.getValue());
    m_suites = null;
    m_tags = null;
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    deleteDependencies(dbConn, m_key.getValue());

    m_opQueue.clear();

    m_series = null;
    m_suites = null;
    m_tags = null;

    return super.delete(dbConn);
  }

  /**
   *
   * @param dbConn
   * @param id
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  static boolean delete(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    deleteDependencies(dbConn, id);

    Criterion key = new LongCriterion(KEY_NAME, id);

    return Row.delete(dbConn, TABLE_NAME, key);
  }

  /**
   *
   * @param dbConn
   * @param key
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  static boolean delete(Connection dbConn, Criterion key) throws IOException, SQLException, PersistenceException
  {
    StringBuilder stmtBuilder = new StringBuilder();

    stmtBuilder.append("SELECT ");
    stmtBuilder.append(KEY_NAME);
    stmtBuilder.append(" FROM ");
    stmtBuilder.append(TABLE_NAME);
    stmtBuilder.append(" WHERE ");
    stmtBuilder.append(key.getPhrase());

    try (PreparedStatement selectStmt = dbConn.prepareStatement(stmtBuilder.toString())) {
      key.setParameter(selectStmt, 1);

      ResultSet rows = selectStmt.executeQuery();

      boolean result = true;

      while (rows.next()) {
        long configId = rows.getLong(1);

        if (!delete(dbConn, configId))
          result = false;
      }

      return result;
    }
  }


  // protected methods


  /**
   *
   * @param s
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  protected void setSuites(Set<Suite> s) throws IOException, SQLException, PersistenceException
  {
    getSuites().clear();

    if (s != null)
      getSuites().addAll(s);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn)
  {
    return null;
  }


  // private methods


  /**
   *
   */
  private static void deleteDependencies(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    try (PreparedStatement deleteLinksStmt = dbConn.prepareStatement(
           "DELETE FROM INCASUITESSERIESCONFIGS " +
           "WHERE incaseriesconfig_id = ?"
         );
         PreparedStatement deleteTagsStmt = dbConn.prepareStatement(
           "DELETE FROM INCASERIESCONFIGTAGS " +
           "WHERE incaseriesconfig_id = ?"
    )) {
      deleteLinksStmt.setLong(1, id);
      deleteLinksStmt.executeUpdate();

      deleteTagsStmt.setLong(1, id);
      deleteTagsStmt.executeUpdate();
    }

    Criterion key = new LongCriterion("incaseriesConfigId", id);

    ComparisonResult.delete(dbConn, key);
  }
}
