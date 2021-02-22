/*
 * DeleteOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 *
 * @author Paul Hoover
 *
 */
class DeleteOp implements RowOperation {

  // data fields


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
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    StringBuilder stmtBuilder = new StringBuilder();

    stmtBuilder.append("DELETE FROM ");
    stmtBuilder.append(m_tableName);
    stmtBuilder.append(" WHERE ");
    stmtBuilder.append(m_key.getPhrase());

    PreparedStatement deleteStmt = dbConn.prepareStatement(stmtBuilder.toString());

    try {
      m_key.setParameter(deleteStmt, 1);

      deleteStmt.executeUpdate();
    }
    finally {
      deleteStmt.close();
    }
  }
}
