/*
 * SelectOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
class SelectOp implements DatabaseOperation {

  private final String m_tableName;
  private final Criterion m_key;
  private final List<Column<?>> m_columns;


  // constructors


  /**
   *
   * @param tableName
   * @param key
   * @param columns
   */
  SelectOp(String tableName, Criterion key, List<Column<?>> columns)
  {
    m_tableName = tableName;
    m_key = key;
    m_columns = columns;
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
    if (m_columns.isEmpty())
      return false;

    StringBuilder stmtBuilder = new StringBuilder();
    Iterator<Column<?>> columns = m_columns.iterator();

    stmtBuilder.append("SELECT ");
    stmtBuilder.append(columns.next().getName());

    while (columns.hasNext()) {
      stmtBuilder.append(", ");
      stmtBuilder.append(columns.next().getName());
    }

    stmtBuilder.append(" FROM ");
    stmtBuilder.append(m_tableName);
    stmtBuilder.append(" WHERE ");
    stmtBuilder.append(m_key.getPhrase());

    PreparedStatement selectStmt = dbConn.prepareStatement(stmtBuilder.toString());
    ResultSet row = null;

    try {
      m_key.setParameter(selectStmt, 1);

      row = selectStmt.executeQuery();

      if (!row.next())
        return false;

      int index = 1;

      for (columns = m_columns.iterator() ; columns.hasNext() ; index += 1)
        columns.next().assignValue(row, index);

      return true;
    }
    finally {
      if (row != null)
        row.close();

      selectStmt.close();
    }
  }
}
