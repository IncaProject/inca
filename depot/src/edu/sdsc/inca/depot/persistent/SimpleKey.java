/*
 * SimpleKey.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * An object that represents a <code>WHERE</code> clause search criterion based on a single-column
 * <code>KEY</code> constraint.
 *
 * @author Paul Hoover
 *
 */
class SimpleKey implements Criterion {

  protected final Column<?> m_key;


  // constructors


  /**
   * Constructs a representation of a search criterion.
   *
   * @param key a <code>Column</code> object that describes the <code>KEY</code> constraint column
   */
  SimpleKey(Column<?> key)
  {
    m_key = key;
  }


  // public methods


  /**
   * Creates a phrase describing the search clause criterion using the name and value of the
   * contained <code>Column</code> object.
   *
   * @return a phrase describing the criterion
   */
  public String getPhrase()
  {
    return m_key.getName() + " = ?";
  }

  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object using the value of
   * the contained <code>Column</code> object.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @return the next offset to use when setting parameters
   * @throws SQLException
   * @throws PersistenceException
   */
  public int setParameter(PreparedStatement statement, int index) throws SQLException, PersistenceException
  {
    m_key.setParameter(statement, index);

    return index + 1;
  }

  /**
   * Returns a string representation of the object. Responsibility for producing the representation
   * is given to the contained <code>Column</code> object.
   *
   * @return a string representation of the object
   */
  @Override
  public String toString()
  {
    return m_key.toString();
  }
}
