/*
 * Value.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 *
 * @author Paul Hoover
 *
 * @param <T>
 */
interface Value<T> {

  /**
   *
   * @return
   */
  T getValue();

  /**
   *
   * @param value
   */
  void setValue(T value);

  /**
   * Sets the value of the object using a row from a <code>ResultSet</code> object. The row used
   * is the one indicated by the current position of the <code>ResultSet</code> object's cursor.
   *
   * @param value the <code>ResultSet</code> object that contains the row
   * @param index the offset in the row that indicates the column whose value will be assigned to this object
   * @throws SQLException
   */
  void setValue(ResultSet value, int index) throws SQLException;

  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object using the current value of the object.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @throws SQLException
   * @throws PersistenceException
   */
  void setParamValue(PreparedStatement statement, int index) throws SQLException, PersistenceException;

  /**
   *
   * @return
   */
  boolean isNull();

  /**
   *
   * @param other
   * @return
   */
  boolean isEqual(T other);

  /**
   *
   */
  void reset();
}
