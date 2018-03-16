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
class ReadSequenceOp implements DatabaseOperation {

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
   * @return
   * @throws SQLException
   */
  public boolean execute(Connection dbConn) throws SQLException
  {
    String query = DatabaseTools.getNextValuePhrase(m_sequenceName);
    Statement selectStmt = dbConn.createStatement();
    ResultSet row = null;

    try {
      row = selectStmt.executeQuery(query);

      if (!row.next())
        return false;

      m_column.setValue(row.getLong(1));

      return true;
    }
    finally {
      if (row != null)
        row.close();

      selectStmt.close();
    }
  }
}
