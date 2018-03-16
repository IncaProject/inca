/*
 * Operation.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.SQLException;


/**
 *
 * @author Paul Hoover
 *
 */
interface DatabaseOperation {

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   * @throws PersistenceException
   */
  boolean execute(Connection dbConn) throws SQLException, PersistenceException;
}
