package edu.sdsc.inca.depot;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.sdsc.inca.depot.persistent.DatabaseTools;


class SequenceKeyInsertStatement extends BatchUpdateStatement {

  private final int m_keyIndex;
  private final String m_nextSeqValQuery;


  // constructors

  /**
   *
   * @param dbConn
   * @param table
   * @param key
   * @param columns
   * @throws SQLException
   */
  public SequenceKeyInsertStatement(Connection dbConn, String table, String key, String columns) throws SQLException
  {
    super(dbConn);

    StringBuilder stmtBuilder = new StringBuilder();

    stmtBuilder.append("INSERT INTO ");
    stmtBuilder.append(table);
    stmtBuilder.append(" ( ");
    stmtBuilder.append(columns);
    stmtBuilder.append(", ");
    stmtBuilder.append(key);
    stmtBuilder.append(" ) VALUES ( ?");

    int numColumns = columns.split("\\s*,\\s*").length;

    assert numColumns > 0;

    for (int i = 1 ; i < numColumns ; i += 1)
      stmtBuilder.append(", ?");

    stmtBuilder.append(", ? )");

    m_updateStmt = dbConn.prepareStatement(stmtBuilder.toString());
    m_keyIndex = numColumns + 1;
    m_nextSeqValQuery = DatabaseTools.getNextValuePhrase("hibernate_sequence");
  }


  // public methods


  /**
   *
   * @return
   * @throws SQLException
   */
  public long update() throws SQLException
  {
    long newId = getNextSequenceValue();

    m_updateStmt.setLong(m_keyIndex, newId);

    super.update();

    return newId;
  }


  // private methods


  /**
   *
   * @return
   * @throws SQLException
   */
  private long getNextSequenceValue() throws SQLException
  {
    Statement selectStmt = m_dbConn.createStatement();
    ResultSet row = null;

    try {
      row = selectStmt.executeQuery(m_nextSeqValQuery);

      if (!row.next())
        return -1;

      return row.getLong(1);
    }
    finally {
      if (row != null)
        row.close();

      selectStmt.close();
    }
  }
}
