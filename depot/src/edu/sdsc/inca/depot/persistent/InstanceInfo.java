/*
 * InstanceInfo.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlObject;


/**
 * Represents information specific to a particular instance of a reporter run.
 *
 * @author Jim Hayes
 * @author Paul Hoover
 */
public class InstanceInfo extends GeneratedKeyObject implements Comparable<InstanceInfo> {

  // nested classes


  /**
   *
   */
  private class AddLinkOp implements DatabaseOperation {

    private final long m_seriesConfigId;


    // constructors


    /**
     *
     * @param seriesConfigId
     */
    public AddLinkOp(long seriesConfigId)
    {
      m_seriesConfigId = seriesConfigId;
    }


    // public methods


    /**
     *
     * @param dbConn
     * @return
     * @throws SQLException
     * @throws PersistenceException
     */
    @Override
    public boolean execute(Connection dbConn) throws SQLException, PersistenceException
    {
      PreparedStatement insertStmt = dbConn.prepareStatement(
          "INSERT INTO " + m_linkTableName + " (incaseriesconfig_id, incainstance_id) " +
          "VALUES (?, ?)"
      );

      try {
        insertStmt.setLong(1, m_seriesConfigId);
        m_key.setParameter(insertStmt, 2);

        return insertStmt.executeUpdate() > 0;
      }
      finally {
        insertStmt.close();
      }
    }
  }

  /**
   *
   */
  private class RemoveLinkOp implements DatabaseOperation {

    private final long m_seriesConfigId;


    // constructors


    /**
     *
     * @param seriesConfigId
     */
    public RemoveLinkOp(long seriesConfigId)
    {
      m_seriesConfigId = seriesConfigId;
    }


    // public methods


    /**
     *
     * @param dbConn
     * @return
     * @throws SQLException
     * @throws PersistenceException
     */
    @Override
    public boolean execute(Connection dbConn) throws SQLException, PersistenceException
    {
      PreparedStatement deleteStmt = dbConn.prepareStatement(
          "DELETE FROM " + m_linkTableName +
          " WHERE incaseriesconfig_id = ? " +
            "AND incainstance_id = ?"
      );

      try {
        deleteStmt.setLong(1, m_seriesConfigId);
        m_key.setParameter(deleteStmt, 2);

        return deleteStmt.executeUpdate() > 0;
      }
      finally {
        deleteStmt.close();
      }
    }
  }

  /**
   *
   */
  private class RemoveAllLinksOp implements DatabaseOperation {

    /**
     *
     * @param dbConn
     * @return
     * @throws SQLException
     * @throws PersistenceException
     */
    @Override
    public boolean execute(Connection dbConn) throws SQLException, PersistenceException
    {
      PreparedStatement deleteStmt = dbConn.prepareStatement(
          "DELETE FROM " + m_linkTableName +
          " WHERE incainstance_id = ?"
      );

      try {
        m_key.setParameter(deleteStmt, 1);

        return deleteStmt.executeUpdate() > 0;
      }
      finally {
        deleteStmt.close();
      }
    }
  }

  /**
   *
   */
  private class LinkSet extends MonitoredSet<SeriesConfig> {

    // constructors


    /**
     *
     * @param seriesConfigs
     */
    public LinkSet(Set<SeriesConfig> seriesConfigs)
    {
      super(seriesConfigs);
    }


    // protected methods


    /**
     *
     * @param element
     */
    @Override
    protected void addSetAddOp(SeriesConfig element)
    {
      Long seriesConfigId = element.getId();

      if (seriesConfigId != null)
        m_opQueue.add(new AddLinkOp(seriesConfigId));
    }

    /**
     *
     * @param element
     */
    @Override
    protected void addSetRemoveOp(SeriesConfig element)
    {
      Long seriesConfigId = element.getId();

      if (seriesConfigId != null)
        m_opQueue.add(new RemoveLinkOp(seriesConfigId));
    }

    /**
     *
     */
    @Override
    protected void addSetClearOp()
    {
      m_opQueue.add(new RemoveAllLinksOp());
    }
  }


  // data fields


  /**
   * Pattern matcher for splitting sysusage string.
   */
  private static final Pattern SYSUSAGE_PATTERN = Pattern.compile("(cpu_secs|wall_secs|memory_mb)\\s*=\\s*(\\d+(\\.\\d+)?)");
  private static final String KEY_NAME = "incaid";

  private final Column<Date> m_collected = new DateColumn("incacollected", false);
  private final Column<Date> m_commited = new DateColumn("incacommited", false);
  private final Column<Float> m_memoryUsageMB = new FloatColumn("incamemoryusagemb", false);
  private final Column<Float> m_cpuUsageSec = new FloatColumn("incacpuusagesec", false);
  private final Column<Float> m_wallClockTimeSec = new FloatColumn("incawallclocktimesec", false);
  private final Column<String> m_log = new StringColumn("incalog", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<Long> m_reportId = new LongColumn("incareportid", false);
  private Set<SeriesConfig> m_seriesConfigs;
  private String m_linkTableName;


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
    super(KEY_NAME);

    construct(instanceTableName, linkTableName);

    setCollected(new Date());
    setCommited(new Date());
    setMemoryUsageMB(new Float(-1));
    setCpuUsageSec(new Float(-1));
    setWallClockTimeSec(new Float(-1));
    setLog("");
    setReportId(new Long(-1));
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
    super(KEY_NAME);

    construct(owner);

    setCollected(collected);
    setCommited(commited);
    setMemoryUsageMB(memoryUsageMB);
    setCpuUsageSec(cpuUsageSec);
    setWallClockTimeSec(wallClockTimeSec);
    setLog("");
    setReportId(new Long(-1));
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
      Float value = new Float(m.group(2));

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
   * @param nickname
   * @param resource
   * @param target
   * @param collected
   * @throws SQLException
   * @throws PersistenceException
   */
  public InstanceInfo(String nickname, String resource, String target, Date collected) throws SQLException, PersistenceException
  {
    super(KEY_NAME);

    Connection dbConn = ConnectionSource.getConnection();
    PreparedStatement selectStmt = null;
    ResultSet rows = null;

    try {
      selectStmt = dbConn.prepareStatement(
          "SELECT incaseriesconfig.incaid, incainstancetablename, incalinktablename " +
          "FROM incaseries " +
            "INNER JOIN incaseriesconfig ON incaseries.incaid = incaseriesconfig.incaseries_id " +
          "WHERE incanickname = ? " +
            "AND incaresource = ? " +
            "AND incatargethostname = ?"
      );

      selectStmt.setString(1, nickname);
      selectStmt.setString(2, resource);

      if (target == null || target.length() < 1)
        selectStmt.setString(3, DB_EMPTY_STRING);
      else
        selectStmt.setString(3, target);

      rows = selectStmt.executeQuery();

      while (rows.next()) {
        long seriesConfigId = rows.getLong(1);
        String instanceTableName = rows.getString(2);
        String linkTableName = rows.getString(3);
        List<Long> instanceIds = findInstanceIds(dbConn, instanceTableName, linkTableName, collected, seriesConfigId);

        if (!instanceIds.isEmpty()) {
          load(instanceTableName, linkTableName, instanceIds.get(0));

          return;
        }
      }

      throw new PersistenceException("No InstanceInfo record found for nickname " + nickname + ", resource " + resource + ", target " + target + ", collected " + collected);
    }
    finally {
      if (rows != null)
        rows.close();

      if (selectStmt != null)
        selectStmt.close();

      dbConn.close();
    }
  }

  /**
   *
   * @param owner
   * @param instanceId
   * @throws SQLException
   * @throws PersistenceException
   */
  public InstanceInfo(Series owner, long instanceId) throws SQLException, PersistenceException
  {
    super(KEY_NAME);

    load(owner, instanceId);
  }

  /**
   *
   * @param seriesId
   * @param instanceId
   * @throws SQLException
   * @throws PersistenceException
   */
  public InstanceInfo(long seriesId, long instanceId) throws SQLException, PersistenceException
  {
    super(KEY_NAME);

    Series owner = SeriesDAO.load(seriesId);

    if (owner == null)
      throw new PersistenceException("No Series record found for primary key " + seriesId);

    load(owner, instanceId);
  }


  // public methods


  /**
   *
   * @return
   */
  public Long getId()
  {
    return m_key.getValue();
  }

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
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<SeriesConfig> getSeriesConfigs() throws SQLException, PersistenceException
  {
    if (m_seriesConfigs == null) {
      Set<SeriesConfig> seriesConfigs = new HashSet<SeriesConfig>();

      if (!isNew()) {
        Connection dbConn = ConnectionSource.getConnection();
        PreparedStatement selectStmt = null;
        ResultSet rows = null;

        try {
          selectStmt = dbConn.prepareStatement(
              "SELECT incaseriesconfig_id " +
              "FROM " + m_linkTableName +
              " WHERE incainstance_id = ?"
          );

          m_key.setParameter(selectStmt, 1);

          rows = selectStmt.executeQuery();

          while (rows.next()) {
            long seriesConfigId = rows.getLong(1);

            seriesConfigs.add(SeriesConfigDAO.load(seriesConfigId));
          }
        }
        finally {
          if (rows != null)
            rows.close();

          if (selectStmt != null)
            selectStmt.close();

          dbConn.close();
        }
      }

      m_seriesConfigs = new LinkSet(seriesConfigs);
    }

    return m_seriesConfigs;
  }

  /**
   * Copies information from an Inca schema XmlBean object so that this object
   * contains equivalent information.
   *
   * @param o the XmlBean object to copy
   * @return this, for convenience
   */
  @Override
  public PersistentObject fromBean(XmlObject o)
  {
    return this; // No XmlBean equivalent to InstanceInfo
  }

  /**
   * Returns a Inca schema XmlBean Limits object that contains information
   * equivalent to this object.
   *
   * @return an XmlBean Limits object that contains equivalent information
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
   * Compares another object to this InstanceInfo for logical equality.
   *
   * @param o the object to compare
   * @return true iff the comparison object represents the same InstanceInfo
   */
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (!(o instanceof InstanceInfo))
      return false;

    InstanceInfo ii = (InstanceInfo)o;

    return
      this.getCollected().equals(ii.getCollected()) &&
      this.getCommited().equals(ii.getCommited()) &&
      this.getCpuUsageSec().equals(ii.getCpuUsageSec()) &&
      this.getMemoryUsageMB().equals(ii.getMemoryUsageMB()) &&
      this.getWallClockTimeSec().equals(ii.getWallClockTimeSec());
  }

  /**
   * Returns XML that represents the information in this object.
   *
   * @return
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
   * Calculate a hash code using the same fields that where used in equals.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode()
  {
    return 29 * getCollected().hashCode() +
                getCommited().hashCode() +
                getCpuUsageSec().hashCode() +
                getMemoryUsageMB().hashCode() +
                getWallClockTimeSec().hashCode();
  }

  /**
   *
   * @param other
   * @return
   */
  @Override
  public int compareTo(InstanceInfo other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    if (isNew())
      return -1;

    if (other.isNew())
      return 1;

    return hashCode() - other.hashCode();
  }


  // private methods


  /**
   *
   * @param owner
   * @param instanceId
   * @throws SQLException
   * @throws PersistenceException
   */
  private void load(Series owner, long instanceId) throws SQLException, PersistenceException
  {
    load(owner.getInstanceTableName(), owner.getLinkTableName(), instanceId);
  }

  /**
   *
   * @param instanceTableName
   * @param linkTableName
   * @param instanceId
   * @throws SQLException
   * @throws PersistenceException
   */
  private void load(String instanceTableName, String linkTableName, long instanceId) throws SQLException, PersistenceException
  {
    construct(instanceTableName, linkTableName);

    m_key.assignValue(instanceId);

    load();
  }

  /**
   *
   * @param owner
   */
  private void construct(Series owner)
  {
    construct(owner.getInstanceTableName(), owner.getLinkTableName());
  }

  /**
   *
   * @param instanceTableName
   * @param linkTableName
   */
  private void construct(String instanceTableName, String linkTableName)
  {
    m_linkTableName = linkTableName;

    construct(instanceTableName, m_collected, m_commited, m_memoryUsageMB, m_cpuUsageSec, m_wallClockTimeSec, m_log, m_reportId);
  }

  /**
   *
   * @param dbConn
   * @param instanceTableName
   * @param linkTableName
   * @param collected
   * @param seriesConfigId
   * @return
   * @throws SQLException
   */
  private List<Long> findInstanceIds(Connection dbConn, String instanceTableName, String linkTableName, Date collected, long seriesConfigId) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT instanceid " +
        "FROM " + linkTableName +
          " INNER JOIN (" +
            "SELECT incaid AS instanceid " +
            "FROM " + instanceTableName +
            " WHERE incacollected = ?" +
          ") AS instances ON " + linkTableName + ".incainstance_id = instances.instanceid " +
        "WHERE " + linkTableName + ".incaseriesconfig_id = ?"
    );
    ResultSet rows = null;

    try {
      selectStmt.setTimestamp(1, new Timestamp(collected.getTime()));
      selectStmt.setLong(2, seriesConfigId);

      rows = selectStmt.executeQuery();

      List<Long> result = new ArrayList<Long>();

      while (rows.next())
        result.add(rows.getLong(1));

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }
}
