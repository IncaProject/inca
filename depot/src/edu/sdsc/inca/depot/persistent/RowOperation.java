/*
 * RowOperation.java
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
interface RowOperation {

  /**
   *
   * @param dbConn
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  void execute(Connection dbConn) throws IOException, SQLException, PersistenceException;
}
