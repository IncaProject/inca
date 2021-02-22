/*
 * DeleteRowOp.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


/**
 *
 * @author Paul Hoover
 *
 */
class DeleteRowOp implements RowOperation {

  // data fields


  private final Row m_row;


  // constructors


  /**
   *
   * @param row
   */
  DeleteRowOp(Row row)
  {
    m_row = row;
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
    m_row.delete(dbConn);
  }
}
