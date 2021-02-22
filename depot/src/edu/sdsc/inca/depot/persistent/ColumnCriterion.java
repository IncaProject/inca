/*
 * ColumnCriterion.java
 */
package edu.sdsc.inca.depot.persistent;


/**
 * An object that represents a <code>WHERE</code> clause search criterion.
 *
 * @author Paul Hoover
 *
 * @param <T>
 */
abstract class ColumnCriterion<T> implements Criterion {

  // data fields


  protected final String m_colName;
  protected final T m_value;


  // constructors


  /**
   * Constructs a representation of a search criterion for the given column name and value.
   *
   * @param colName the name of the column
   * @param value the value of the column
   */
  ColumnCriterion(String colName, T value)
  {
    m_colName = colName;
    m_value = value;
  }


  // public methods


  /**
   * Creates a phrase describing the search clause criterion using the name and value given
   * in the constructor.
   *
   * @return a phrase describing the criterion
   */
  @Override
  public String getPhrase()
  {
    return m_colName + " = ? ";
  }

  @Override
  public String toString()
  {
    if (m_value == null)
      return m_colName + " IS NULL ";
    else
      return m_colName + " = " + m_value;
  }
}
