/*
 * StringColumn.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


/**
 * Represents a <code>VARCHAR</code> column from a database table.
 *
 * @author Paul Hoover
 *
 */
class StringColumn extends Column<String> {

  // nested classes


  /**
   *
   */
  private class StringValue extends MemoryValue<String> {

    // public methods


    /**
     * Sets the value of the object using a row from a <code>ResultSet</code> object. The row used
     * is the one indicated by the current position of the <code>ResultSet</code> object's cursor.
     *
     * @param value the <code>ResultSet</code> object that contains the row
     * @param index the offset in the row that indicates the column whose value will be assigned to this object
     * @throws SQLException
     */
    @Override
    public void setValue(ResultSet value, int index) throws SQLException
    {
      String colValue = value.getString(index);

      setValue(colValue);
    }

    /**
     * Sets the value of a parameter in a <code>PreparedStatement</code> object using the current value of the object.
     *
     * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
     * @param index the offset that indicates the parameter to set
     * @throws SQLException
     * @throws PersistenceException
     */
    @Override
    public void setParamValue(PreparedStatement statement, int index) throws SQLException, PersistenceException
    {
      // if we know that we're going to violate a constraint, don't incur
      // the overhead of establishing a connection to the database server
      if (m_memValue.length() > m_maxLength)
        throw new PersistenceException(getName() + " exceeds maximum length of " + String.valueOf(m_maxLength));

      statement.setString(index, m_memValue);
    }
  }


  // data fields


  private final int m_maxLength;


  // constructors


  /**
   * Constructs a column representation and assigns a <code>null</code> value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param maxLength the maximum number of characters that can be stored in the column
   */
  StringColumn(String name, boolean nullable, int maxLength)
  {
    super(name, nullable);

    m_maxLength = maxLength;
  }

  /**
   * Constructs a column representation and assigns the given value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param maxLength the maximum number of characters that can be stored in the column
   * @param value an initial value to assign to the object
   */
  StringColumn(String name, boolean nullable, int maxLength, String value)
  {
    super(name, nullable, value);

    m_maxLength = maxLength;
  }


  // protected methods


  /**
   * Returns the SQL data type of the column
   *
   * @return the SQL data type of the column
   */
  @Override
  protected int getType()
  {
    return Types.VARCHAR;
  }

  /**
   *
   * @return
   */
  @Override
  protected Value<String> createValue()
  {
    return new StringValue();
  }
}
