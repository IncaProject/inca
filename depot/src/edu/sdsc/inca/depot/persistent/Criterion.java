/*
 * Criterion.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * An object that provides a search criterion for a <code>SQL</code> <code>WHERE</code> clause.
 *
 * @author Paul Hoover
 *
 */
interface Criterion {

  /**
   * Creates a phrase describing the <code>WHERE</code> clause criterion.
   *
   * @return a phrase describing the criterion
   */
  String getPhrase();

  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @return the next offset to use when setting parameters
   * @throws SQLException
   * @throws PersistenceException
   */
  int setParameter(PreparedStatement statement, int index) throws SQLException, PersistenceException;
}
