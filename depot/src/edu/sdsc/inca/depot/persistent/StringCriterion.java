/*
 * StringCriterion.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;


/**
 * An object that represents a <code>WHERE</code> clause search criterion that uses a <code>VARCHAR</code> value.
 *
 * @author Paul Hoover
 *
 */
class StringCriterion extends ColumnCriterion<String> {

  // constructors


  /**
   * Constructs a representation of a search criterion for the given column name and value.
   *
   * @param colName the name of the column
   * @param value the value of the column
   */
  StringCriterion(String colName, String value)
  {
    super(colName, value);
  }

  /**
   * Constructs a representation of a search criterion for the given column.
   *
   * @param col
   */
  StringCriterion(Column<String> col)
  {
    this(col.getName(), col.getValue());
  }


  // public methods


  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object using the name and
   * value given in the constructor.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @return the next offset to use when setting parameters
   * @throws SQLException
   */
  @Override
  public int setParameter(PreparedStatement statement, int index) throws SQLException
  {
    if (m_value != null)
      statement.setString(index, m_value);
    else
      statement.setNull(index, Types.VARCHAR);

    index += 1;

    return index;
  }
}
