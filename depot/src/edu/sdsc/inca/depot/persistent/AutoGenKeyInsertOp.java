/*
 * AutoGenKeyInsertOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
class AutoGenKeyInsertOp implements DatabaseOperation {

  // data fields


  private final String m_tableName;
  private final Column<?> m_key;
  private final List<Column<?>> m_columns;


  // constructors


  /**
   *
   * @param tableName
   * @param key
   * @param columns
   */
  AutoGenKeyInsertOp(String tableName, Column<?> key, List<Column<?>> columns)
  {
    m_tableName = tableName;
    m_key = key;
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

    PreparedStatement insertStmt = dbConn.prepareStatement(stmtBuilder.toString(), Statement.RETURN_GENERATED_KEYS);
    ResultSet newKey = null;

    try {
      int index = 1;

      for (columns = m_columns.iterator() ; columns.hasNext() ; index += 1)
        columns.next().setParameter(insertStmt, index);

      if (insertStmt.executeUpdate() < 1)
        return false;

      newKey = insertStmt.getGeneratedKeys();

      if (newKey.next())
        m_key.assignValue(newKey, 1);

      return true;
    }
    finally {
      if (newKey != null)
        newKey.close();

      insertStmt.close();

      for (Column<?> col : m_columns)
        col.finishUpdate();
    }
  }
}
