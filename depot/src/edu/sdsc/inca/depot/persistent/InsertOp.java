/*
 * InsertOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
class InsertOp implements DatabaseOperation {

  // data fields


  private final String m_tableName;
  private final List<Column<?>> m_columns;


  // constructors


  /**
   *
   * @param tableName
   * @param columns
   */
  InsertOp(String tableName, List<Column<?>> columns)
  {
    m_tableName = tableName;
    m_columns = columns;
  }


  // public methods


  /**
   *
   * @param dbConn
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public boolean execute(Connection dbConn) throws SQLException, PersistenceException
  {
    StringBuilder stmtBuilder = new StringBuilder();
    Iterator<Column<?>> columns = m_columns.iterator();

    stmtBuilder.append("INSERT INTO ");
    stmtBuilder.append(m_tableName);

    if (columns.hasNext()) {
      stmtBuilder.append(" ( ");
      stmtBuilder.append(columns.next().getName());

      while (columns.hasNext()) {
        stmtBuilder.append(", ");
        stmtBuilder.append(columns.next().getName());
      }

      stmtBuilder.append(" ) VALUES ( ?");

      for (int i = 1 ; i < m_columns.size() ; i += 1)
        stmtBuilder.append(", ?");

      stmtBuilder.append(" )");
    }

    PreparedStatement insertStmt = dbConn.prepareStatement(stmtBuilder.toString());

    try {
      int index = 1;

      for (columns = m_columns.iterator() ; columns.hasNext() ; index += 1)
        columns.next().setParameter(insertStmt, index);

      return insertStmt.executeUpdate() > 0;
    }
    finally {
      insertStmt.close();

      for (Column<?> col : m_columns)
        col.finishUpdate();
    }
  }
}
