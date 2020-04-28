/*
 * Column.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Represents a column from a database table.
 *
 * @author Paul Hoover
 *
 * @param <T>
 */
abstract class Column<T> {

  // data fields


  protected final String m_name;
  protected final boolean m_isNullable;
  protected boolean m_isModified;
  protected Value<T> m_value;


  // constructors


  /**
   * Constructs a column representation and assigns a <code>null</code> value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   */
  protected Column(String name, boolean nullable)
  {
    m_name = name;
    m_isNullable = nullable;
    m_value = createValue();
  }

  /**
   * Constructs a column representation and assigns the given value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param value an initial value to assign to the object
   */
  protected Column(String name, boolean nullable, T value)
  {
    this(name, nullable);

    assignValue(value);
  }


  // public methods


  /**
   * Returns the name of the column.
   *
   * @return the name of the column
   */
  public String getName()
  {
    return m_name;
  }

  /**
   * Returns the current value of the object.
   *
   * @return the current value of the object
   */
  public T getValue()
  {
    return m_value.getValue();
  }

  /**
   * Assigns a value to the object and sets the modified state to <code>true</code>. If, however,
   * the value provided is equal to the current value of the object, then the assignment is ignored
   * and the modified state is not set.
   *
   * @param value the value to assign to the object
   */
  public void setValue(T value)
  {
    if (!m_value.isEqual(value)) {
      m_value.setValue(value);

      m_isModified = true;
    }
  }

  /**
   * Assigns a value to the object, and sets the modified state to <code>false</code>.
   *
   * @param value the value to assign to the object
   */
  public void assignValue(T value)
  {
    m_value.setValue(value);

    m_isModified = false;
  }

  /**
   * Assigns a value to the object using a row from a <code>ResultSet</code> object. The row used
   * is the one indicated by the current position of the <code>ResultSet</code> object's cursor.
   *
   * @param value the <code>ResultSet</code> object that contains the row
   * @param index the offset in the row that indicates the column whose value will be assigned to this object
   * @throws SQLException
   */
  public void assignValue(ResultSet value, int index) throws SQLException
  {
    m_value.setValue(value, index);

    m_isModified = false;
  }

  /**
   * Indicates whether or not the current value of the object is <code>null</code>.
   *
   * @return <code>true</code> if the current value is <code>null</code>
   */
  public boolean isNull()
  {
    return m_value.isNull();
  }

  /**
   * Indicates whether the column lacks or has a <code>NOT NULL</code> constraint.
   *
   * @return <code>true</code> if the column lacks a <code>NOT NULL</code> constraint
   */
  public boolean isNullable()
  {
    return m_isNullable;
  }

  /**
   * Indicates whether or not the value of the object has been modified.
   *
   * @return <code>true</code> if the value has been modified
   */
  public boolean isModified()
  {
    return m_isModified;
  }

  /**
   * Sets value of the object to <code>null</code> and the modified state to <code>false</code>.
   */
  public void reset()
  {
    m_value.reset();

    m_isModified = false;
  }

  /**
   * Sets the modified state of the object to <code>false</code>.
   */
  public void finishUpdate()
  {
    m_isModified = false;
  }

  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object using the current value of the object.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @throws SQLException
   * @throws PersistenceException
   */
  public void setParameter(PreparedStatement statement, int index) throws SQLException, PersistenceException
  {
    if (!isNull())
      m_value.setParamValue(statement, index);
    else {
      // if we know that we're going to violate a constraint, don't incur
      // the overhead of establishing a connection to the database server
      if (!m_isNullable)
        throw new PersistenceException(m_name + " is not nullable");

      statement.setNull(index, getType());
    }
  }

  /**
   * Returns a string representation of the object. The representation is a combination
   * of the name of the column and the current value of the object.
   *
   * @return a string representation of the object
   */
  @Override
  public String toString()
  {
    return m_name + " = " + m_value.toString();
  }


  // protected methods


  /**
   * Returns the SQL data type of the column
   *
   * @return the SQL data type of the column
   */
  protected abstract int getType();

  /**
   *
   * @return
   */
  protected abstract Value<T> createValue();
}
