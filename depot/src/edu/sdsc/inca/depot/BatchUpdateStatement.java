/*
 * BatchUpdateStatement.java
 */
package edu.sdsc.inca.depot;


import java.sql.Connection;
import java.sql.SQLException;


/**
 *
 * @author Paul Hoover
 *
 */
class BatchUpdateStatement extends UpdateStatement {

  private static final int MAX_BATCH_SIZE = 100;

  protected final Connection m_dbConn;
  private int m_batchSize = 0;


  // constructors


  /**
   *
   * @param dbConn
   * @param query
   * @throws SQLException
   */
  public BatchUpdateStatement(Connection dbConn, String query) throws SQLException
  {
    this(dbConn);

    m_updateStmt = dbConn.prepareStatement(query);
  }

  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  protected BatchUpdateStatement(Connection dbConn) throws SQLException
  {
    assert !dbConn.getAutoCommit();

    m_dbConn = dbConn;
  }


  // public methods

  /**
   *
   * @throws SQLException
   */
  public void close() throws SQLException
  {
    if (m_batchSize > 0) {
      m_updateStmt.executeBatch();

      m_dbConn.commit();
    }

    super.close();
  }

  /**
   *
   * @return
   * @throws SQLException
   */
  public long update() throws SQLException
  {
    m_updateStmt.addBatch();

    m_batchSize += 1;

    if (m_batchSize < MAX_BATCH_SIZE)
      return 0;

    m_updateStmt.executeBatch();

    m_dbConn.commit();

    m_batchSize = 0;

    return MAX_BATCH_SIZE;
  }
}
