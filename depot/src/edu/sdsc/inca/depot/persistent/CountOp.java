/*
 * CountOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
class CountOp implements RowOperation {

  // data fields


  private final String m_tableName;
  private final List<Criterion> m_keys;
  private final Column<Integer> m_result;


  // constructors


  /**
   *
   * @param tableName
   * @param key
   * @param result
   */
  CountOp(String tableName, Criterion key, Column<Integer> result)
  {
    m_tableName = tableName;
    m_keys = new ArrayList<Criterion>();

    m_keys.add(key);

    m_result = result;
  }

  CountOp(String tableName, List<Criterion> keys, Column<Integer> result)
  {
    m_tableName = tableName;
    m_keys = keys;
    m_result = result;
  }


  // public methods


  /**
   *
   * @param dbConn
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    StringBuilder stmtBuilder = new StringBuilder();
    Iterator<Criterion> keys = m_keys.iterator();

    stmtBuilder.append("SELECT COUNT(*) FROM ");
    stmtBuilder.append(m_tableName);
    stmtBuilder.append(" WHERE ");
    stmtBuilder.append(keys.next().getPhrase());

    while (keys.hasNext()) {
      stmtBuilder.append(" AND ");
      stmtBuilder.append(keys.next().getPhrase());
    }

    PreparedStatement selectStmt = dbConn.prepareStatement(stmtBuilder.toString());
    ResultSet row = null;

    try {
      int index = 1;

      for (Criterion key : m_keys)
        index = key.setParameter(selectStmt, index);

      row = selectStmt.executeQuery();

      if (row.next())
        m_result.assignValue(row, 1);
      else
        m_result.assignValue(null);
    }
    finally {
      if (row != null)
        row.close();

      selectStmt.close();
    }
  }
}
