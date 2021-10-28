/*
 * InstanceInfo.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;


/**
 * Represents information specific to a particular instance of a reporter run.
 *
 * @author Jim Hayes
 * @author Paul Hoover
 */
public class InstanceInfo extends GeneratedKeyRow implements Comparable<InstanceInfo> {

  // nested classes


  /**
   *
   */
  private class AddLinkOp implements RowOperation {

    private final SeriesConfig m_seriesConfig;


    // constructors


    public AddLinkOp(SeriesConfig config)
    {
      m_seriesConfig = config;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      try (PreparedStatement insertStmt = dbConn.prepareStatement(
        "INSERT INTO " + m_linkTableName + " (incaseriesconfig_id, incainstance_id) " +
        "VALUES (?, ?)"
      )) {
        insertStmt.setLong(1, m_seriesConfig.getId());
        m_key.setParameter(insertStmt, 2);

        insertStmt.executeUpdate();
      }
    }
  }

  /**
   *
   */
  private class RemoveLinkOp implements RowOperation {

    private final SeriesConfig m_seriesConfig;


    // constructors


    public RemoveLinkOp(SeriesConfig config)
    {
      m_seriesConfig = config;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      try (PreparedStatement deleteStmt = dbConn.prepareStatement(
        "DELETE FROM " + m_linkTableName +
        " WHERE incaseriesconfig_id = ? " +
          "AND incainstance_id = ?"
      )) {
        deleteStmt.setLong(1, m_seriesConfig.getId());
        m_key.setParameter(deleteStmt, 2);

        deleteStmt.executeUpdate();
      }
    }
  }

  /**
   *
   */
  private class LinkSet extends MonitoredSet<SeriesConfig> {

    // constructors


    public LinkSet(Set<SeriesConfig> seriesConfigs)
    {
      super(seriesConfigs);
    }


    // protected methods


    @Override
    protected void addSetAddOp(SeriesConfig element)
    {
      m_opQueue.add(new AddLinkOp(element));
    }

    @Override
    protected void addSetRemoveOp(SeriesConfig element)
    {
      m_opQueue.add(new RemoveLinkOp(element));
    }

    @Override
    protected void addSetClearOp(List<SeriesConfig> elements)
    {
      for (SeriesConfig element : elements)
        m_opQueue.add(new RemoveLinkOp(element));
    }
  }


  // data fields


  private static final Pattern SYSUSAGE_PATTERN = Pattern.compile("(cpu_secs|wall_secs|memory_mb)\\s*=\\s*(\\d+(\\.\\d+)?)");
  private static final String KEY_NAME = "incaid";
  private static final Logger m_logger = Logger.getLogger(DriverConnectionSource.class);
  private final Column<Date> m_collected = new DateColumn("incacollected", false);
  private final Column<Date> m_commited = new DateColumn("incacommited", false);
  private final Column<Float> m_memoryUsageMB = new FloatColumn("incamemoryusagemb", false);
  private final Column<Float> m_cpuUsageSec = new FloatColumn("incacpuusagesec", false);
  private final Column<Float> m_wallClockTimeSec = new FloatColumn("incawallclocktimesec", false);
  private final Column<String> m_log = new StringColumn("incalog", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<Long> m_reportId = new LongColumn("incareportid", false);
  private final Deque<RowOperation> m_opQueue = new LinkedList<RowOperation>();
  private final String m_linkTableName;
  private LinkSet m_seriesConfigs;


  // constructors


  /**
   *
   * @param owner
   */
  public InstanceInfo(Series owner)
  {
    this(owner.getInstanceTableName(), owner.getLinkTableName());
  }

  /**
   *
   * @param instanceTableName
   * @param linkTableName
   */
  public InstanceInfo(String instanceTableName, String linkTableName)
  {
    super(instanceTableName, KEY_NAME);

    m_linkTableName = linkTableName;

    setCollected(new Date());
    setCommited(new Date());
    setMemoryUsageMB(-1F);
    setCpuUsageSec(-1F);
    setWallClockTimeSec(-1F);
    setLog("");
    setReportId(-1L);

    construct(m_collected, m_commited, m_memoryUsageMB, m_cpuUsageSec, m_wallClockTimeSec, m_log, m_reportId);
  }

  /**
   * Full constructor.
   *
   * @param owner
   * @param collected        The time that the data was collected
   * @param commited         The time that the data was saved to the database.
   * @param memoryUsageMB    The amount of memory used by the reporter.
   * @param cpuUsageSec      The cpu used by this run of the reporter
   * @param wallClockTimeSec The time the reporter took to run.
   */
  public InstanceInfo(Series owner, Date collected, Date commited, Float memoryUsageMB, Float cpuUsageSec, Float wallClockTimeSec)
  {
    this(owner);

    setCollected(collected);
    setCommited(commited);
    setMemoryUsageMB(memoryUsageMB);
    setCpuUsageSec(cpuUsageSec);
    setWallClockTimeSec(wallClockTimeSec);
    setLog("");
    setReportId(-1L);
  }

  /**
   * A constructor that parses the Inca protocol SYSUSAGE message.
   *
   * @param owner
   * @param collected The time that the date was collected.
   * @param sysusage  The Inca protocol SYSUSAGE message.
   */
  public InstanceInfo(Series owner, Date collected, String sysusage)
  {
    this(owner);

    setCollected(collected);

    Matcher m = SYSUSAGE_PATTERN.matcher(sysusage);

    for (int start = 0 ; m.find(start) ; start = m.start() + 1) {
      String metric = m.group(1);
      Float value = Float.valueOf(m.group(2));

      if (metric.equals("cpu_secs"))
        setCpuUsageSec(value);
      else if (metric.equals("wall_secs"))
        setWallClockTimeSec(value);
      else
        setMemoryUsageMB(value);
    }
  }

  /**
   *
   * @param owner
   * @param instanceId
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public InstanceInfo(Series owner, long instanceId) throws IOException, SQLException, PersistenceException
  {
    this(owner);

    m_key.assignValue(instanceId);

    load();
  }

  /**
   *
   * @param dbConn
   * @param owner
   * @param instanceId
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  InstanceInfo(Connection dbConn, Series owner, long instanceId) throws IOException, SQLException, PersistenceException
  {
    this(owner);

    m_key.assignValue(instanceId);

    load(dbConn);
  }

  /**
   *
   * @param instanceId
   * @param instanceTableName
   * @param linkTableName
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  private InstanceInfo(long instanceId, String instanceTableName, String linkTableName) throws IOException, SQLException, PersistenceException
  {
    this(instanceTableName, linkTableName);

    m_key.assignValue(instanceId);

    load();
  }


  // public methods


  /**
   *
   * @param collected
   */
  public void setCollected(Date collected)
  {
    m_collected.setValue(collected);
  }

  /**
   *
   * @return
   */
  public Date getCollected()
  {
    return m_collected.getValue();
  }

  /**
   *
   * @param commited
   */
  public void setCommited(Date commited)
  {
    m_commited.setValue(commited);
  }

  /**
   *
   * @return
   */
  public Date getCommited()
  {
    return m_commited.getValue();
  }

  /**
   *
   * @param memoryUsageMB
   */
  public void setMemoryUsageMB(Float memoryUsageMB)
  {
    m_memoryUsageMB.setValue(memoryUsageMB);
  }

  /**
   *
   * @return
   */
  public Float getMemoryUsageMB()
  {
    return m_memoryUsageMB.getValue();
  }

  /**
   *
   * @param cpuUsageSec
   */
  public void setCpuUsageSec(Float cpuUsageSec)
  {
    m_cpuUsageSec.setValue(cpuUsageSec);
  }

  /**
   *
   * @return
   */
  public Float getCpuUsageSec()
  {
    return m_cpuUsageSec.getValue();
  }

  /**
   *
   * @param wallClockTimeSec
   */
  public void setWallClockTimeSec(Float wallClockTimeSec)
  {
    m_wallClockTimeSec.setValue(wallClockTimeSec);
  }

  /**
   *
   * @return
   */
  public Float getWallClockTimeSec()
  {
    return m_wallClockTimeSec.getValue();
  }

  /**
   *
   * @param log
   */
  public void setLog(String log)
  {
    if (log == null || log.length() < 1)
      log = DB_EMPTY_STRING;
    else if (log.length() > MAX_DB_LONG_STRING_LENGTH) {
      m_logger.warn("Discarding messages from over-long log '" + log + "'");

      // Throw away one message at a time until we get under the threshold or
      // the pattern match fails
      do {
        int len = log.length();

        log = log.replaceFirst("(?s)<(debug|error|info|system|warn)>.*?</\\1>\\s*", "");

        if (log.length() == len)
          log = "";
      }
      while (log.length() > MAX_DB_LONG_STRING_LENGTH);
    }

    m_log.setValue(log);
  }

  /**
   *
   * @return
   */
  public String getLog()
  {
    return m_log.getValue();
  }

  /**
   *
   * @param reportId
   */
  public void setReportId(Long reportId)
  {
    m_reportId.setValue(reportId);
  }

  /**
   *
   * @return
   */
  public Long getReportId()
  {
    return m_reportId.getValue();
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<SeriesConfig> getSeriesConfigs() throws IOException, SQLException, PersistenceException
  {
    if (m_seriesConfigs == null) {
      Set<SeriesConfig> seriesConfigs = new HashSet<SeriesConfig>();

      if (!isNew()) {
        try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
             PreparedStatement selectStmt = dbConn.prepareStatement(
               "SELECT incaseriesconfig_id " +
               "FROM " + m_linkTableName +
               " WHERE incainstance_id = ?"
        )) {
          m_key.setParameter(selectStmt, 1);

          ResultSet rows = selectStmt.executeQuery();

          while (rows.next()) {
            long seriesConfigId = rows.getLong(1);

            seriesConfigs.add(new SeriesConfig(seriesConfigId));
          }
        }
      }

      m_seriesConfigs = new LinkSet(seriesConfigs);
    }

    return m_seriesConfigs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o)
  {
    return this; // No XmlBean equivalent to InstanceInfo
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean()
  {
    edu.sdsc.inca.dataModel.util.Limits result = edu.sdsc.inca.dataModel.util.Limits.Factory.newInstance();

    result.setCpuTime(getCpuUsageSec().toString());
    result.setMemory(getMemoryUsageMB().toString());
    result.setWallClockTime(getWallClockTimeSec().toString());

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object other)
  {
    if (other == null)
      return false;

    if (this == other)
      return true;

    if (other instanceof InstanceInfo == false)
      return false;

    InstanceInfo otherInfo = (InstanceInfo) other;

    return getCollected().equals(otherInfo.getCollected()) &&
           getCommited().equals(otherInfo.getCommited()) &&
           getCpuUsageSec().equals(otherInfo.getCpuUsageSec()) &&
           getMemoryUsageMB().equals(otherInfo.getMemoryUsageMB()) &&
           getWallClockTimeSec().equals(otherInfo.getWallClockTimeSec());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toXml()
  {
    // InstanceInfo has no corresponding XML bean.  This implementation is
    // for debugging purposes.
    String result =
      "<instance>\n" +
      "  <collected>" + getCollected() + "</collected>\n" +
      "  <commited>" + getCommited() + "</commited>\n" +
      "  <cpuUsageSec>" + getCpuUsageSec() + "</cpuUsageSec>\n" +
      "  <memoryUsageMB>" + getMemoryUsageMB() + "</memoryUsageMB>\n" +
      "  <wallClockTimeSec>" + getWallClockTimeSec() + "</wallClockTimeSec>\n";
    String log = getLog();

    if (!log.equals(DB_EMPTY_STRING))
      result += "  <log>\n" + log + "  </log>\n";

    result += "</instance>\n";

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (getCollected().hashCode() +
                 getCommited().hashCode() +
                 getCpuUsageSec().hashCode() +
                 getMemoryUsageMB().hashCode() +
                 getWallClockTimeSec().hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(InstanceInfo other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }

  /**
   *
   * @param nickname
   * @param resource
   * @param target
   * @param collected
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static InstanceInfo find(String nickname, String resource, String target, Date collected) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
         PreparedStatement selectStmt = dbConn.prepareStatement(
           "SELECT incaseriesconfig.incaid, incainstancetablename, incalinktablename " +
           "FROM incaseries " +
             "INNER JOIN incaseriesconfig ON incaseries.incaid = incaseriesconfig.incaseries_id " +
           "WHERE incanickname = ? " +
             "AND incaresource = ? " +
             "AND incatargethostname = ?"
    )) {
      selectStmt.setString(1, nickname);
      selectStmt.setString(2, resource);

      if (target == null || target.length() < 1)
        selectStmt.setString(3, DB_EMPTY_STRING);
      else
        selectStmt.setString(3, target);

      ResultSet rows = selectStmt.executeQuery();

      while (rows.next()) {
        long seriesConfigId = rows.getLong(1);
        String instanceTableName = rows.getString(2);
        String linkTableName = rows.getString(3);
        List<Long> instanceIds = findInstanceIds(instanceTableName, linkTableName, collected, seriesConfigId);

        if (!instanceIds.isEmpty())
          return new InstanceInfo(instanceIds.get(0), instanceTableName, linkTableName);
      }

      return null;
    }
  }


  // package methods


  /**
   * {@inheritDoc}
   */
  @Override
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.save(dbConn);

    while (!m_opQueue.isEmpty()) {
      RowOperation op = m_opQueue.remove();

      op.execute(dbConn);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.load(dbConn);

    m_opQueue.clear();

    m_seriesConfigs = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM " + m_linkTableName +
      " WHERE incainstance_id = ?"
    )) {
      m_key.setParameter(deleteStmt, 1);

      deleteStmt.executeUpdate();
    }

    m_opQueue.clear();

    m_seriesConfigs = null;

    return super.delete(dbConn);
  }


  // protected methods


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
   * @param instanceTableName
   * @param linkTableName
   * @param collected
   * @param seriesConfigId
   * @return
   * @throws SQLException
   */
  private static List<Long> findInstanceIds(String instanceTableName, String linkTableName, Date collected, long seriesConfigId) throws SQLException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
         PreparedStatement selectStmt = dbConn.prepareStatement(
           "SELECT instanceid " +
           "FROM " + linkTableName +
             " INNER JOIN (" +
               "SELECT incaid AS instanceid " +
               "FROM " + instanceTableName +
               " WHERE incacollected = ?" +
             ") AS instances ON " + linkTableName + ".incainstance_id = instances.instanceid " +
           "WHERE " + linkTableName + ".incaseriesconfig_id = ?"
    )) {
      selectStmt.setTimestamp(1, new Timestamp(collected.getTime()));
      selectStmt.setLong(2, seriesConfigId);

      ResultSet rows = selectStmt.executeQuery();

      List<Long> result = new ArrayList<Long>();

      while (rows.next())
        result.add(rows.getLong(1));

      return result;
    }
  }
}
