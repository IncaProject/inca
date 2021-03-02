/*
 * StreamColumn.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;


/**
 * Represents a column from a database table whose value is not retrieved from the database until it's requested.
 *
 * @author Paul Hoover
 *
 * @param <T>
 */
abstract class StreamColumn<T> extends Column<T> {

  // nested classes


  /**
   *
   * @param <V>
   */
  protected interface StreamValue<V> extends Value<V> {

    /**
     *
     * @return
     * @throws IOException
     */
    InputStream getValueAsStream() throws IOException;

    /**
     *
     * @param value
     * @throws IOException
     */
    void setValue(InputStream value) throws IOException;

    /**
     *
     * @return
     */
    long getLength();
  }

  /**
   *
   */
  private class AssignmentColumn extends Column<T> {

    // constructors


    AssignmentColumn()
    {
      super(StreamColumn.this.getName(), StreamColumn.this.isNullable());
    }


    // protected methods


    @Override
    protected int getType()
    {
      return StreamColumn.this.getType();
    }

    @Override
    protected Value<T> createValue()
    {
      return StreamColumn.this.m_value;
    }
  }


  // data fields


  private static final Logger m_log = Logger.getLogger(StreamColumn.class);

  protected final Row m_owner;
  protected boolean m_populated;


  // constructors


  /**
   * Constructs a column representation and assigns a <code>null</code> value to it.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param owner a reference to the <code>Row</code> object that owns this object
   */
  protected StreamColumn(String name, boolean nullable, Row owner)
  {
    super(name, nullable);

    m_owner = owner;
  }

  /**
   * Constructs a column representation and assigns the given value to it. The populated state of
   * the object is set to <code>true</code>.
   *
   * @param name the name of the column
   * @param nullable whether the column lacks or has a <code>NOT NULL</code> constraint
   * @param owner a reference to the <code>Row</code> object that owns this object
   * @param value an initial value to assign to the object
   */
  protected StreamColumn(String name, boolean nullable, Row owner, T value)
  {
    this(name, nullable, owner);

    assignValue(value);
  }


  // public methods


  /**
   * Returns the current value of the object. If the populated state of the object is <code>false</code>,
   * a value is fetched from the database using the primary key and table name of the <code>Row</code>
   * object that owns this object, and the populated state of the object is set to <code>true</code>.
   *
   * @return the current value of the object
   */
  @Override
  public T getValue()
  {
    try {
      if (!m_populated && !m_owner.isNew())
        assignDbValue();

      return m_value.getValue();
    }
    catch (Exception err) {
      m_log.error("", err);

      return null;
    }
  }

  /**
   * Returns an <code>InputStream</code> backed by the column represented by the object. If the populated
   * state of the object is <code>false</code>, a value is fetched from the database using the primary
   * key and table name of the <code>Row</code> object that owns this object, and the populated state
   * of the object is set to <code>true</code>.
   *
   * @return an <code>InputStream</code> backed by the column value
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public InputStream getValueAsStream() throws IOException, SQLException, PersistenceException
  {
    if (!m_populated && !m_owner.isNew())
      assignDbValue();

    return ((StreamValue<T>)m_value).getValueAsStream();
  }

  /**
   * Returns the length of the column value
   *
   * @return the length of the column value
   */
  public long getLength()
  {
    try {
      if (!m_populated && !m_owner.isNew())
        assignDbValue();

      return ((StreamValue<T>)m_value).getLength();
    }
    catch (Exception err) {
      m_log.error("", err);

      return 0;
    }
  }

  /**
   * Assigns a value to the object and sets the modified and populated states to <code>true</code>. The
   * assignment is ignored if the value provided is equal to the current value of the object.
   *
   * @param value the value to assign to the object
   */
  @Override
  public void setValue(T value)
  {
    super.setValue(value);

    m_populated = true;
  }

  /**
   *
   * @param value
   * @throws IOException
   */
  public void setValue(InputStream value) throws IOException
  {
    try {
      ((StreamValue<T>)m_value).setValue(value);

      m_isModified = true;
      m_populated = true;
    }
    finally {
      value.close();
    }
  }

  /**
   * Assigns a value to the object, sets the modified state to <code>false</code>, and
   * sets the populated state to <code>true</code>.
   *
   * @param value the value to assign to the object
   */
  @Override
  public void assignValue(T value)
  {
    super.assignValue(value);

    m_populated = true;
  }

  /**
   * Assigns a value to the object using a row from a <code>ResultSet</code> object. The row used
   * is the one indicated by the current position of the <code>ResultSet</code> object's cursor.
   *
   * @param value the <code>ResultSet</code> object that contains the row
   * @param index the offset in the row that indicates the column whose value will be assigned to this object
   * @throws SQLException
   */
  @Override
  public void assignValue(ResultSet value, int index) throws SQLException
  {
    reset();
  }

  /**
   * Sets the value of the object to <code>null</code> and the modified and populated states to <code>false</code>.
   */
  @Override
  public void reset()
  {
    super.reset();

    m_populated = false;
  }


  // protected methods


  /**
   *
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  protected void assignDbValue() throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      (new SelectOp(m_owner.getTableName(), m_owner.getKey(), new AssignmentColumn())).execute(dbConn);

      m_isModified = false;
      m_populated = true;
    }
  }
}
