/*
 * ReadSequenceOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 *
 * @author Paul Hoover
 *
 */
class ReadSequenceOp implements RowOperation {

  // data fields


  private final String m_sequenceName;
  private final Column<Long> m_column;


  // constructors


  /**
   *
   * @param sequenceName
   * @param column
   */
  ReadSequenceOp(String sequenceName, Column<Long> column)
  {
    m_sequenceName = sequenceName;
    m_column = column;
  }


  // public methods


  /**
   *
   * @param dbConn
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public void execute(Connection dbConn) throws SQLException, PersistenceException
  {
    String query = DatabaseTools.getNextValuePhrase(m_sequenceName);

    try (Statement selectStmt = dbConn.createStatement();
         ResultSet row = selectStmt.executeQuery(query)) {
      if (!row.next())
        throw new PersistenceException("Query failed: " + query);

      m_column.setValue(row.getLong(1));
    }
  }
}
