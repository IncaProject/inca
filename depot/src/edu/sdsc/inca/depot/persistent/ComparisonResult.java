/*
 * ComparisonResult.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.xmlbeans.XmlObject;


/**
 * Represents results of a comparison between accepted values and report output.
 */
public class ComparisonResult extends GeneratedKeyRow implements Comparable<ComparisonResult> {

  // data fields


  private static final String TABLE_NAME = "INCACOMPARISONRESULT";
  private static final String KEY_NAME = "incaid";
  private final Column<String> m_result = new StringColumn("incaresult", false, MAX_DB_STRING_LENGTH);
  private final Column<Long> m_reportId = new LongColumn("incareportId", false);
  private final Column<Long> m_seriesConfigId = new LongColumn("incaseriesConfigId", false);


  // constructors


  /**
   *
   */
  public ComparisonResult()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_result, m_reportId, m_seriesConfigId);
  }

  /**
   *
   * @param result
   * @param reportId
   * @param seriesConfigId
   */
  public ComparisonResult(String result, long reportId, long seriesConfigId)
  {
    this();

    setResult(result);
    setReportId(reportId);
    setSeriesConfigId(seriesConfigId);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public ComparisonResult(long id) throws IOException, SQLException, PersistenceException
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
  ComparisonResult(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
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
  public String getResult()
  {
    return m_result.getValue();
  }

  /**
   *
   * @param result
   */
  public void setResult(String result)
  {
    m_result.setValue(result);
  }

  /**
   *
   * @return
   */
  public Long getReportId()
  {
    if (m_reportId.isNull())
      return null;

    return m_reportId.getValue();
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
  public Long getSeriesConfigId()
  {
    if (m_seriesConfigId.isNull())
      return null;

    return m_seriesConfigId.getValue();
  }

  /**
   *
   * @param seriesConfigId
   */
  public void setSeriesConfigId(Long seriesConfigId)
  {
    m_seriesConfigId.setValue(seriesConfigId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Row fromBean(XmlObject o)
  {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean()
  {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toXml()
  {
    // ComparisonResult has no corresponding XML bean.  This implementation is
    // for debugging purposes.
    return "<comparison>\n" +
      "  <result>" + getResult() + "</result>\n" +
      "  <reportId>" + getReportId() + "</reportId>\n" +
      "  <seriesConfigId>" + getSeriesConfigId() + "</seriesConfigId>\n" +
      "</comparison>\n";
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

    if (other instanceof ComparisonResult == false)
      return false;

    ComparisonResult otherArg = (ComparisonResult) other;

    return getResult().equals(otherArg.getResult()) && getReportId() == otherArg.getReportId() && getSeriesConfigId() == otherArg.getSeriesConfigId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (getResult().hashCode() + Long.valueOf(getReportId()).hashCode() + Long.valueOf(getSeriesConfigId()).hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(ComparisonResult other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }


  // package methods


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
        long comparisonId = rows.getLong(1);

        if (!delete(dbConn, comparisonId))
          result = false;
      }

      return result;
    }
  }


  // protected methods


  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn) throws SQLException
  {
    return find(dbConn, getResult(), getReportId(), getSeriesConfigId());
  }


  // private methods


  /*
   *
   */
  private static Long find(Connection dbConn, String result, long reportId, long seriesConfigId) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT incaid " +
      "FROM INCACOMPARISONRESULT " +
      "WHERE incaresult = ? " +
        "AND incareportId = ? " +
        "AND incaseriesConfigId = ?"
    )) {
      selectStmt.setString(1, result);
      selectStmt.setLong(2, reportId);
      selectStmt.setLong(3, seriesConfigId);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }
}
