/*
 * DeleteOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 *
 * @author Paul Hoover
 *
 */
class DeleteOp implements DatabaseOperation {

  private final String m_tableName;
  private final Criterion m_key;


  // constructors


  /**
   *
   * @param tableName
   * @param key
   */
  DeleteOp(String tableName, Criterion key)
  {
    m_tableName = tableName;
    m_key = key;
  }


  // public methods


  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   * @throws PersistenceException
   */
  public boolean execute(Connection dbConn) throws SQLException, PersistenceException
  {
    StringBuilder stmtBuilder = new StringBuilder();

    stmtBuilder.append("DELETE FROM ");
    stmtBuilder.append(m_tableName);
    stmtBuilder.append(" WHERE ");
    stmtBuilder.append(m_key.getPhrase());

    PreparedStatement deleteStmt = dbConn.prepareStatement(stmtBuilder.toString());

    try {
      m_key.setParameter(deleteStmt, 1);

      return deleteStmt.executeUpdate() > 0;
    }
    finally {
      deleteStmt.close();
    }
  }
}
