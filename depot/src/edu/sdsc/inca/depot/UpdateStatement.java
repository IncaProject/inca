/*
 * UpdateStatement.java
 */
package edu.sdsc.inca.depot;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;


/**
 *
 * @author Paul Hoover
 *
 */
abstract class UpdateStatement {

  protected PreparedStatement m_updateStmt;


  // public methods


  /**
   *
   * @param index
   * @param value
   * @throws SQLException
   */
  public void setBoolean(int index, Boolean value) throws SQLException
  {
    if (value == null)
      m_updateStmt.setNull(index, Types.BOOLEAN);
    else
      m_updateStmt.setBoolean(index, value);
  }

  /**
   *
   * @param index
   * @param value
   * @throws SQLException
   */
  public void setFloat(int index, Float value) throws SQLException
  {
    if (value == null)
      m_updateStmt.setNull(index, Types.FLOAT);
    else
      m_updateStmt.setFloat(index, value);
  }

  /**
   *
   * @param index
   * @param value
   * @throws SQLException
   */
  public void setInt(int index, Integer value) throws SQLException
  {
    if (value == null)
      m_updateStmt.setNull(index, Types.INTEGER);
    else
      m_updateStmt.setInt(index, value);
  }

  /**
   *
   * @param index
   * @param value
   * @throws SQLException
   */
  public void setLong(int index, Long value) throws SQLException
  {
    if (value == null)
      m_updateStmt.setNull(index, Types.BIGINT);
    else
      m_updateStmt.setLong(index, value);
  }

  /**
   *
   * @param index
   * @param value
   * @throws SQLException
   */
  public void setString(int index, String value) throws SQLException
  {
    if (value == null)
      m_updateStmt.setNull(index, Types.VARCHAR);
    else
      m_updateStmt.setString(index, value);
  }

  /**
   *
   * @param index
   * @param value
   * @throws SQLException
   */
  public void setTimestamp(int index, Timestamp value) throws SQLException
  {
    if (value == null)
      m_updateStmt.setNull(index, Types.TIMESTAMP);
    else
      m_updateStmt.setTimestamp(index, value);
  }

  /**
   *
   * @throws SQLException
   */
  public void close() throws SQLException
  {
    m_updateStmt.close();
  }

  /**
   *
   * @return
   * @throws SQLException
   */
  public abstract long update() throws SQLException;
}
