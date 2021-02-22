/*
 * Row.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents a row from a database table.
 *
 * @author Paul Hoover
 *
 */
public abstract class Row extends XmlBeanObject {

  // data fields


  /**
   * Maximum length for string DB fields.  MAX_STRING_LENGTH applies to those
   * fields with no length attribute in the HBM files; MAX_LONG_STRING_LENGTH
   * is the minimum upper bound across underlying DBs for those with a length
   * attribute.  Unfortunately, there seems to be no programmatic way to know
   * one from the other, so implementing classes will simply need to "know"
   * which string fields have been given the larger length bound.
   */
  public static final int MAX_DB_STRING_LENGTH = 255;
  public static final int MAX_DB_LONG_STRING_LENGTH = 4000;

  /**
   * Value used to store null/empty strings in the database.  We avoid storing
   * null because it complicates constructing select statements (must is "is
   * null" rather than "==").  We avoid "" because of Oracle bizarreness--it
   * translates "" into null when storing records in a table.
   */
  public static final String DB_EMPTY_STRING = "\t";

  protected final List<Column<?>> m_columns = new ArrayList<Column<?>>();
  protected final String m_tableName;


  // constructors


  /**
   * Constructs a row representation.
   *
   * @param tableName the name of the backing table
   */
  protected Row(String tableName)
  {
    m_tableName = tableName;
  }


  // public methods


  /**
   * Indicates whether or not the object has been persisted.
   *
   * @return <code>true</code> if the object has not been persisted
   */
  public abstract boolean isNew();

  /**
   * Saves any changes to the columns of the row to the backing table.
   * Only updates columns that have been modified.
   *
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public void save() throws IOException, SQLException, PersistenceException
  {
    Connection dbConn = ConnectionManager.getConnectionSource().getConnection();

    try {
      dbConn.setAutoCommit(false);

      save(dbConn);

      dbConn.commit();
    }
    catch (IOException | SQLException | PersistenceException err) {
      dbConn.rollback();

      throw err;
    }
    finally {
      dbConn.close();
    }
  }

  /**
   * Synchronize the fields of this object with the current values from the database.
   *
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public void load() throws IOException, SQLException, PersistenceException
  {
    Connection dbConn = ConnectionManager.getConnectionSource().getConnection();

    try {
      dbConn.setAutoCommit(false);

      load(dbConn);

      dbConn.commit();
    }
    catch (IOException | SQLException | PersistenceException err) {
      dbConn.rollback();

      throw err;
    }
    finally {
      dbConn.close();
    }
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public boolean delete() throws IOException, SQLException, PersistenceException
  {
    Connection dbConn = ConnectionManager.getConnectionSource().getConnection();

    try {
      dbConn.setAutoCommit(false);

      if (!delete(dbConn)) {
        dbConn.rollback();

        return false;
      }

      dbConn.commit();

      return true;
    }
    catch (IOException | SQLException | PersistenceException err) {
      dbConn.rollback();

      throw err;
    }
    finally {
      dbConn.close();
    }
  }


  // package methods


  /**
   * Returns a <code>Criterion</code> object that describes the primary key of the record
   * that the object represents.
   *
   * @return a <code>Criterion</code> object that describes the primary key
   */
  abstract Criterion getKey();

  /**
   *
   * @return the name of the backing table
   */
  String getTableName()
  {
    return m_tableName;
  }

  /**
   * Saves the current state of the object to the database. If the object has not yet been persisted,
   * new records are inserted in the appropriate tables. If the object has been persisted, then any
   * changes are written to the backing tables. Only those values that have changed are written, and
   * if the state of the object is unchanged, the method does nothing.
   *
   * @param dbConn a <code>Connection</code> object that will be used to communicate with the database
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    if (isNew())
      insert(dbConn);
    else
      update(dbConn);
  }

  /**
   *
   * @param dbConn a <code>Connection</code> object that will be used to communicate with the database
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    assert !m_columns.isEmpty();

    if (isNew())
      throw new PersistenceException("Not persisted");

    (new SelectOp(m_tableName, getKey(), m_columns)).execute(dbConn);
  }

  /**
   *
   * @param dbConn a <code>Connection</code> object that will be used to communicate with the database
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    if (isNew())
      throw new PersistenceException("Not persisted");

    if (!delete(dbConn, m_tableName, getKey()))
      return false;

    for (Column<?> col : m_columns)
      col.reset();

    return true;
  }

  /**
   *
   * @param dbConn a <code>Connection</code> object that will be used to communicate with the database
   * @param tableName
   * @param key
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  static boolean delete(Connection dbConn, String tableName, Criterion key) throws IOException, SQLException, PersistenceException
  {
    try {
      (new DeleteOp(tableName, key)).execute(dbConn);
    }
    catch (SQLException sqlErr) {
      String state = sqlErr.getSQLState();

      if (state.equals("23000") || state.equals("23505"))
        return false;
      else
        throw sqlErr;
    }

    return true;
  }


  // protected methods


  /**
   *
   * @param dbConn
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  protected abstract boolean insert(Connection dbConn) throws IOException, SQLException, PersistenceException;

  /**
   *
   * @param dbConn
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  protected void update(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    assert !m_columns.isEmpty();

    List<Column<?>> modifiedCols = new ArrayList<Column<?>>();

    for (Column<?> col : m_columns) {
      if (col.isModified())
        modifiedCols.add(col);
    }

    (new UpdateOp(m_tableName, getKey(), modifiedCols)).execute(dbConn);
  }

  /**
   *
   * @param columns
   */
  protected void construct(Column<?>... columns)
  {
    assert m_columns.isEmpty();

    for (Column<?> col : columns)
      m_columns.add(col);
  }
}
