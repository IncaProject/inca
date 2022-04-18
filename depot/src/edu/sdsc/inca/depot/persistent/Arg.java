/*
 * Arg.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.xmlbeans.XmlObject;


/**
 * This class represents a command line input to a reporter.
 *
 * This class is a persistent object.  It has unique constraints on the
 * name value combination.  This means that on save there will be an exception
 * thrown if the combo already exists.
 */
public class Arg extends GeneratedKeyRow implements Comparable<Arg> {

  // data fields


  private static final String TABLE_NAME = "INCAARG";
  private static final String KEY_NAME = "incaid";
  private final Column<String> m_name = new StringColumn("incaname", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_value = new StringColumn("incavalue", false, MAX_DB_STRING_LENGTH);


  // constructors


  /**
   *
   */
  public Arg()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_name, m_value);

    m_name.setValue(DB_EMPTY_STRING);
    m_value.setValue(DB_EMPTY_STRING);
  }

  /**
   *
   * @param name
   * @param value
   */
  public Arg(String name, String value)
  {
    this();

    setName(name);
    setValue(value);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Arg(long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load();
  }

  /**
   * @param dbConn
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  Arg(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    this();

    m_key.assignValue(id);

    load(dbConn);
  }


  // public methods


  /**
   *
   * @return
   */
  public String getName()
  {
    return m_name.getValue();
  }

  /**
   *
   * @param name
   */
  public void setName(String name)
  {
    name = normalize(name, MAX_DB_STRING_LENGTH, "arg name");

    m_name.setValue(name);
  }

  /**
   *
   * @return
   */
  public String getValue()
  {
    return m_value.getValue();
  }

  /**
   *
   * @param value
   */
  public void setValue(String value)
  {
    value = normalize(value, MAX_DB_STRING_LENGTH, "arg value");

    m_value.setValue(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Row fromBean(XmlObject o)
  {
    return fromBean((edu.sdsc.inca.dataModel.util.Args.Arg)o);
  }

  /**
   * {@inheritDoc}
   */
  public Arg fromBean(edu.sdsc.inca.dataModel.util.Args.Arg a)
  {
    setName(a.getName());
    setValue(a.getValue());

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean()
  {
    edu.sdsc.inca.dataModel.util.Args.Arg result = edu.sdsc.inca.dataModel.util.Args.Arg.Factory.newInstance();

    result.setName(getName());
    result.setValue(getValue());

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object other)
  {
    if (other == null)
      return false;

    if (this == other)
      return true;

    if (other instanceof Arg == false)
      return false;

    Arg otherArg = (Arg) other;

    return getName().equals(otherArg.getName()) && getValue().equals(otherArg.getValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (getName().hashCode() + getValue().hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Arg other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    if (isNew()) {
      if (other.isNew())
        return hashCode() - other.hashCode();
      else
        return -1;
    }
    else if (other.isNew())
      return 1;

    return (int)(getId() - other.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return getName() + "=" + getValue();
  }

  /**
   *
   * @param arg
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Arg find(Arg arg) throws IOException, SQLException, PersistenceException
  {
    return find(arg.getName(), arg.getValue());
  }

  /**
   *
   * @param name
   * @param value
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Arg find(String name, String value) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Long id = find(dbConn, name, value);

      if (id == null)
        return null;

      return new Arg(dbConn, id);
    }
  }


  // package methods


  /**
   *
   * @param dbConn
   * @param id
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  static boolean delete(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    Criterion key = new LongCriterion(KEY_NAME, id);

    return Row.delete(dbConn, TABLE_NAME, key);
  }


  // protected methods


  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn) throws SQLException
  {
    return find(dbConn, getName(), getValue());
  }


  // private methods


  /*
   *
   */
  private static Long find(Connection dbConn, String name, String value) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT incaid " +
      "FROM INCAARG " +
      "WHERE incaname = ? " +
        "AND incavalue = ?"
    )) {
      selectStmt.setString(1, name);
      selectStmt.setString(2, value);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }
}
