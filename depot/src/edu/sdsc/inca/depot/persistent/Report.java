/*
 * Report.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import edu.sdsc.inca.dataModel.util.AnyXmlSequence;


/**
 * This class represents the output from a Single Reporter.
 *
 * @author Cathie Olschanowskyz
 * @author Paul Hoover
 */
public class Report extends GeneratedKeyRow implements Comparable<Report> {

  // data fields


  private static final String TABLE_NAME = "INCAREPORT";
  private static final String KEY_NAME = "incaid";
  private static final Logger m_log = Logger.getLogger(Report.class);
  private final Column<Boolean> m_exitStatus = new BooleanColumn("incaexit_status", true);
  private final Column<String> m_exitMessage = new StringColumn("incaexit_message", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_bodypart1 = new StringColumn("incabodypart1", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_bodypart2 = new StringColumn("incabodypart2", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_bodypart3 = new StringColumn("incabodypart3", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_stderr = new StringColumn("incastderr", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<Long> m_seriesId = new LongColumn("incaseries_id", false);
  private final Column<Long> m_runInfoId = new LongColumn("incarunInfo_id", false);
  private Series m_series;
  private RunInfo m_runInfo;


  // constructors


  /**
   *
   */
  public Report()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_exitStatus, m_exitMessage, m_bodypart1, m_bodypart2, m_bodypart3, m_stderr, m_seriesId, m_runInfoId);

    m_exitMessage.setValue(DB_EMPTY_STRING);
    m_bodypart1.setValue(DB_EMPTY_STRING);
    m_bodypart2.setValue(DB_EMPTY_STRING);
    m_bodypart3.setValue(DB_EMPTY_STRING);
    m_stderr.setValue(DB_EMPTY_STRING);

    m_series = new Series();
    m_runInfo = new RunInfo();
  }

  /**
   *
   * @param status
   * @param message
   * @param body
   * @param series
   */
  public Report(Boolean status, String message, String body, Series series)
  {
    this();

    setExit_status(status);
    setExit_message(message);
    setBody(body);
    setStderr("");
    setSeries(series);
    setRunInfo(new RunInfo());
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Report(long id) throws IOException, SQLException, PersistenceException
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
  Report(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
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
  public boolean getExit_status()
  {
    return m_exitStatus.getValue();
  }

  /**
   *
   * @param status
   */
  public void setExit_status(Boolean status)
  {
    m_exitStatus.setValue(status);
  }

  /**
   *
   * @return
   */
  public String getExit_message()
  {
    return m_exitMessage.getValue();
  }

  /**
   *
   * @param message
   */
  public void setExit_message(String message)
  {
    message = normalize(message, MAX_DB_LONG_STRING_LENGTH, "error message");

    m_exitMessage.setValue(message);
  }

  /**
   *
   * @return
   */
  public String getBody()
  {
    StringBuilder result = new StringBuilder(m_bodypart1.getValue());

    if (!m_bodypart2.isNull() && !m_bodypart2.getValue().equals(DB_EMPTY_STRING))
      result.append(m_bodypart2.getValue());

    if (!m_bodypart3.isNull() && !m_bodypart3.getValue().equals(DB_EMPTY_STRING))
      result.append(m_bodypart3.getValue());

    return result.toString();
  }

  /**
   *
   * @param body
   */
  public void setBody(String body)
  {
    if (body == null || body.isEmpty())
      body = DB_EMPTY_STRING;

    int length = body.length();

    if (length <= MAX_DB_LONG_STRING_LENGTH) {
      m_bodypart1.setValue(body);
      m_bodypart2.setValue(DB_EMPTY_STRING);
      m_bodypart3.setValue(DB_EMPTY_STRING);
    }
    else if (length <= MAX_DB_LONG_STRING_LENGTH * 2) {
      m_bodypart1.setValue(body.substring(0, MAX_DB_LONG_STRING_LENGTH));
      m_bodypart2.setValue(body.substring(MAX_DB_LONG_STRING_LENGTH));
      m_bodypart3.setValue(DB_EMPTY_STRING);
    }
    else if (length <= MAX_DB_LONG_STRING_LENGTH * 3) {
      m_bodypart1.setValue(body.substring(0, MAX_DB_LONG_STRING_LENGTH));
      m_bodypart2.setValue(body.substring(MAX_DB_LONG_STRING_LENGTH, MAX_DB_LONG_STRING_LENGTH * 2));
      m_bodypart3.setValue(body.substring(MAX_DB_LONG_STRING_LENGTH * 2));
    }
    else {
      m_log.error("Rejecting too-long report body '" + body + "'");

      m_bodypart1.setValue(DB_EMPTY_STRING);
      m_bodypart2.setValue(DB_EMPTY_STRING);
      m_bodypart3.setValue(DB_EMPTY_STRING);
    }
  }

  /**
   *
   * @return
   */
  public String getStderr()
  {
    return m_stderr.getValue();
  }

  /**
   *
   * @param stderr
   */
  public void setStderr(String stderr)
  {
    stderr = normalize(stderr, MAX_DB_LONG_STRING_LENGTH, "stderr");

    m_stderr.setValue(stderr);
  }

  /**
   *
   * @return
   */
  public Series getSeries()
  {
    return m_series;
  }

  /**
   *
   * @param series
   */
  public void setSeries(Series series)
  {
    m_series = series;

    m_seriesId.setValue(series.getId());
  }

  /**
   *
   * @return
   */
  public RunInfo getRunInfo()
  {
    return m_runInfo;
  }

  /**
   *
   * @param info
   */
  public void setRunInfo(RunInfo info)
  {
    m_runInfo = info;

    m_runInfoId.setValue(info.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o)
  {
    return fromBean((edu.sdsc.inca.dataModel.util.Report) o);
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param r the XmlBean Report object to copy
   * @return this, for convenience
   */
  public Report fromBean(edu.sdsc.inca.dataModel.util.Report r)
  {
    setBody(r.getBody().xmlText());
    setExit_status(Boolean.valueOf(r.getExitStatus().getCompleted()));
    setExit_message(r.getExitStatus().getErrorMessage());

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean() throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.util.Report result = edu.sdsc.inca.dataModel.util.Report.Factory.newInstance();

    result.setGmt(Calendar.getInstance());
    result.setHostname(getRunInfo().getHostname());
    result.setName(getSeries().getReporter());
    result.setVersion(getSeries().getVersion());
    result.setWorkingDir(getRunInfo().getWorkingDir());
    result.setReporterPath(getRunInfo().getReporterPath());
    result.setArgs((edu.sdsc.inca.dataModel.util.Args) getRunInfo().getArgSignature().toBean());

    try {
      String body = this.getBody();

      if (body.equals(DB_EMPTY_STRING))
        result.setBody(AnyXmlSequence.Factory.newInstance());
      else
        result.setBody(AnyXmlSequence.Factory.parse(body, (new XmlOptions()).setLoadStripWhitespace()));
    }
    catch (XmlException e) {
      m_log.error("Unable to parse body from DB:", e);
    }

    result.addNewExitStatus();
    result.getExitStatus().setCompleted(getExit_status());
    result.getExitStatus().setErrorMessage(getExit_message());

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

    if (other instanceof Report == false)
      return false;

    Report otherReport = (Report) other;

    return getExit_status() == otherReport.getExit_status() &&
           getExit_message().equals(otherReport.getExit_message()) &&
           getBody().equals(otherReport.getBody()) &&
           getStderr().equals(otherReport.getStderr()) &&
           getSeries().equals(otherReport.getSeries()) &&
           getRunInfo().equals(otherReport.getRunInfo());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (Boolean.valueOf(getExit_status()).hashCode() +
                 getExit_message().hashCode() +
                 getBody().hashCode() +
                 getStderr().hashCode() +
                 getSeries().hashCode() +
                 getRunInfo().hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Report other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }

  /**
   *
   * @param report
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Report find(Report report) throws IOException, SQLException, PersistenceException
  {
    return find(report.getExit_status(), report.getExit_message(), report.getBodypart1(), report.getBodypart2(), report.getBodypart3(), report.getStderr(), report.getSeriesId(), report.getRunInfoId());
  }

  /**
   *
   * @param status
   * @param message
   * @param bodypart1
   * @param bodypart2
   * @param bodypart3
   * @param stderr
   * @param seriesId
   * @param runInfoId
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Report find(boolean status, String message, String bodypart1, String bodypart2, String bodypart3, String stderr, long seriesId, long runInfoId) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Long id = find(dbConn, status, message, bodypart1, bodypart2, bodypart3, stderr, seriesId, runInfoId);

      if (id == null)
        return null;

      return new Report(dbConn, id);
    }
  }


  // package methods


  /**
   * Retrieve the first part of the body of this report.
   *
   * @return
   */
  String getBodypart1()
  {
    return m_bodypart1.getValue();
  }

  /**
   * Retrieve the second part of the body of this report.
   *
   * @return
   */
  String getBodypart2()
  {
    return m_bodypart2.getValue();
  }

  /**
   * Retrieve the third part of the body of this report.
   *
   * @return
   */
  String getBodypart3()
  {
    return m_bodypart3.getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    m_series.save(dbConn);
    m_runInfo.save(dbConn);

    m_seriesId.setValue(m_series.getId());
    m_runInfoId.setValue(m_runInfo.getId());

    super.save(dbConn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.load(dbConn);

    m_series = new Series(dbConn, m_seriesId.getValue());
    m_runInfo = new RunInfo(dbConn, m_runInfoId.getValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    deleteDependencies(dbConn, m_key.getValue());

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
        long reportId = rows.getLong(1);

        if (!delete(dbConn, reportId))
          result = false;
      }

      return result;
    }
  }


  // protected methods


  /**
   *
   * @param bodypart
   */
  protected void setBodypart1(String bodypart)
  {
    bodypart = normalize(bodypart, MAX_DB_LONG_STRING_LENGTH, "bodypart1");

    m_bodypart1.setValue(bodypart);
  }

  /**
   *
   * @param bodypart
   */
  protected void setBodypart2(String bodypart)
  {
    bodypart = normalize(bodypart, MAX_DB_LONG_STRING_LENGTH, "bodypart2");

    m_bodypart2.setValue(bodypart);
  }

  /**
   *
   * @param bodypart
   */
  protected void setBodypart3(String bodypart)
  {
    bodypart = normalize(bodypart, MAX_DB_LONG_STRING_LENGTH, "bodypart3");

    m_bodypart3.setValue(bodypart);
  }

  /**
   *
   * @return
   */
  protected long getSeriesId()
  {
    return m_seriesId.getValue();
  }

  /**
   *
   * @return
   */
  protected long getRunInfoId()
  {
    return m_runInfoId.getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn) throws SQLException
  {
    return find(dbConn, getExit_status(), getExit_message(), getBodypart1(), getBodypart2(), getBodypart3(), getStderr(), getSeriesId(), getRunInfoId());
  }


  // private methods


  /**
   *
   */
  private static Long find(Connection dbConn, boolean status, String message, String bodypart1, String bodypart2, String bodypart3, String stderr, long seriesId, long runInfoId) throws SQLException
  {
    StringBuilder stmtBuilder = new StringBuilder();

    stmtBuilder.append(
      "SELECT incaid " +
      "FROM INCAREPORT " +
      "WHERE incaexit_status = ? " +
        "AND incaexit_message = ? " +
        "AND incabodypart1 = ? " +
        "AND incabodypart2 = ? " +
        "AND incabodypart3 = ? " +
        "AND incastderr = ?"
    );

    if (seriesId > 0)
      stmtBuilder.append(" AND incaseries_id = ?");

    if (runInfoId > 0)
      stmtBuilder.append(" AND incarunInfo_id = ?");

    try (PreparedStatement selectStmt = dbConn.prepareStatement(stmtBuilder.toString())) {
      selectStmt.setBoolean(1, status);
      selectStmt.setString(2, message);
      selectStmt.setString(3, bodypart1);
      selectStmt.setString(4, bodypart2);
      selectStmt.setString(5, bodypart3);
      selectStmt.setString(6, stderr);

      if (seriesId > 0)
        selectStmt.setLong(7, seriesId);

      if (runInfoId > 0)
        selectStmt.setLong(8, runInfoId);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }

  /**
   *
   */
  private static void deleteDependencies(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    Criterion key = new LongCriterion("incareportId", id);

    ComparisonResult.delete(dbConn, key);
  }
}
