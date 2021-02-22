/*
 * UpdateOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
class UpdateOp implements RowOperation {

  // data fields


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
  UpdateOp(String tableName, Criterion key, List<Column<?>> columns)
  {
    m_tableName = tableName;
    m_key = key;
    m_columns = columns;
  }

  /**
   *
   * @param tableName
   * @param key
   * @param column
   */
  UpdateOp(String tableName, Criterion key, Column<?> column)
  {
    m_tableName = tableName;
    m_key = key;
    m_columns = new ArrayList<Column<?>>();

    m_columns.add(column);
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
    if (m_columns.isEmpty())
      return;

    StringBuilder stmtBuilder = new StringBuilder();
    Iterator<Column<?>> columns = m_columns.iterator();

    stmtBuilder.append("UPDATE ");
    stmtBuilder.append(m_tableName);
    stmtBuilder.append(" SET ");
    stmtBuilder.append(columns.next().getName());
    stmtBuilder.append(" = ?");

    while (columns.hasNext()) {
      stmtBuilder.append(", ");
      stmtBuilder.append(columns.next().getName());
      stmtBuilder.append(" = ?");
    }

    stmtBuilder.append(" WHERE ");
    stmtBuilder.append(m_key.getPhrase());

    PreparedStatement updateStmt = dbConn.prepareStatement(stmtBuilder.toString());

    try {
      int index = 1;

      for (columns = m_columns.iterator() ; columns.hasNext() ; index += 1)
        columns.next().setParameter(updateStmt, index);

      m_key.setParameter(updateStmt, index);

      updateStmt.executeUpdate();
    }
    finally {
      updateStmt.close();

      for (Column<?> col : m_columns)
        col.finishUpdate();
    }
  }
}
