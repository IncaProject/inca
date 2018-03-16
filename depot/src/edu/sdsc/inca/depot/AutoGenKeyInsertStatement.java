/*
 * AutoGenKeyInsertStatement.java
 */
package edu.sdsc.inca.depot;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 *
 * @author Paul Hoover
 *
 */
class AutoGenKeyInsertStatement extends UpdateStatement {

  // constructors


  /**
   *
   * @param dbConn
   * @param table
   * @param columns
   * @throws SQLException
   */
  public AutoGenKeyInsertStatement(Connection dbConn, String table, String columns) throws SQLException
  {
    StringBuilder stmtBuilder = new StringBuilder();

    stmtBuilder.append("INSERT INTO ");
    stmtBuilder.append(table);
    stmtBuilder.append(" ( ");
    stmtBuilder.append(columns);
    stmtBuilder.append(" ) VALUES ( ?");

    int numColumns = columns.split("\\s*,\\s*").length;

    assert numColumns > 0;

    for (int i = 1 ; i < numColumns ; i += 1)
      stmtBuilder.append(", ?");

    stmtBuilder.append(" )");

    m_updateStmt = dbConn.prepareStatement(stmtBuilder.toString(), Statement.RETURN_GENERATED_KEYS);
  }


  // public methods


  /**
   *
   * @return
   * @throws SQLException
   */
  public long update() throws SQLException
  {
    m_updateStmt.executeUpdate();

    ResultSet newKey = m_updateStmt.getGeneratedKeys();

    try {
      if (!newKey.next())
        return -1;

      return newKey.getLong(1);
    }
    finally {
      newKey.close();
    }
  }
}
