/*
 * LongColumn.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


/**
 * Represents a <code>BIGINT</code> column from a database table.
 *
 * @author Paul Hoover
 *
 */
class LongColumn extends Column<Long> {

  // nested classes


  /**
   *
   */
  private static class LongValue extends MemoryValue<Long> {

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
      Long colValue = value.getLong(index);

      if (value.wasNull())
        colValue = null;

      setValue(colValue);
    }

    /**
     * Returns the current value of the object.
     *
     * @return the current value of the object, or <code>0</code> if the value is <code>null</code>
     */
    @Override
    public Long getValue()
    {
      if (m_memValue == null)
        return 0L;

      return m_memValue;
    }

    /**
     * Sets the value of a parameter in a <code>PreparedStatement</code> object using the current value of the object.
     *
     * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
     * @param index the offset that indicates the parameter to set
     * @throws SQLException
     */
    @Override
    public void setParamValue(PreparedStatement statement, int index) throws SQLException
    {
      statement.setLong(index, m_memValue);
    }
  }


  // constructors


  /**
   * Constructs a column representation and assigns a <code>null</code> value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   */
  LongColumn(String name, boolean nullable)
  {
    super(name, nullable);
  }

  /**
   * Constructs a column representation and assigns the given value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param value an initial value to assign to the object
   */
  LongColumn(String name, boolean nullable, Long value)
  {
    super(name, nullable, value);
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
    return Types.BIGINT;
  }

  /**
   *
   * @return
   */
  @Override
  protected Value<Long> createValue()
  {
    return new LongValue();
  }
}
