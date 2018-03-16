/*
 * Column.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;


/**
 * Represents a column from a database table.
 *
 * @author Paul Hoover
 *
 * @param <T>
 */
abstract class Column<T> {

  protected static final Logger m_logger = Logger.getLogger(Column.class);
  protected final String m_name;
  protected final boolean m_isNullable;
  protected boolean m_isModified;
  protected T m_value;


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
    m_name = name;
    m_isNullable = nullable;
    m_value = value;
  }


  // public methods


  /**
   * Indicates whether or not the current value of the object is <code>null</code>.
   *
   * @return <code>true</code> if the current value is <code>null</code>
   */
  public boolean isNull()
  {
    return m_value == null;
  }

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
   *
   * @return
   */
  public abstract T getValue();

  /**
   * Assigns a value to the object and sets the modified state to <code>true</code>. If, however,
   * the value provided is equal to the current value of the object, then the assignment is ignored
   * and the modified state is not set.
   *
   * @param value the value to assign to the object
   */
  public void setValue(T value)
  {
    if (m_value != value) {
      m_value = value;
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
    m_value = value;
    m_isModified = false;
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
    assignValue(null);
  }

  /**
   * Sets the modified state of the object to <code>false</code>.
   */
  public void finishSave()
  {
    m_isModified = false;
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

  /**
   * Assigns a value to the object using a row from a <code>ResultSet</code> object. The row used
   * is the one indicated by the current position of the <code>ResultSet</code> object's cursor.
   *
   * @param value the <code>ResultSet</code> object that contains the row
   * @param index the offset in the row that indicates the column whose value will be assigned to this object
   * @throws SQLException
   */
  public abstract void assignValue(ResultSet value, int index) throws SQLException;

  /**
   * Sets the value of a parameter in a <code>PreparedStatement</code> object using the current value of the object.
   *
   * @param statement the <code>PreparedStatement</code> object for which a parameter will be set
   * @param index the offset that indicates the parameter to set
   * @throws SQLException
   * @throws PersistenceException
   */
  public abstract void setParameter(PreparedStatement statement, int index) throws SQLException, PersistenceException;
}
