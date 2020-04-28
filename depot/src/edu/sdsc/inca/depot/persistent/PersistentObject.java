/*
 * PersistentObject.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;


/**
 * This is an abstract base class that defines common behavior for all objects
 * that the Depot stores in the database.
 *
 * @author Jim Hayes
 * @author Paul Hoover
 */
public abstract class PersistentObject {

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
   *
   * Ideally, we'd like to limit use of this hack to DAO.java, since all DB
   * storage and retrieval goes through the class.  At the moment, though, each
   * persistent object class tests incoming values in each String setter method
   * and overrides null and "" with this value.  Trying to replace string
   * values before and after DB use seems to run afoul of Hibernate's auto
   * update feature.
   */
  public static final String DB_EMPTY_STRING = "\t";

  protected static final Logger m_logger = Logger.getLogger(PersistentObject.class);
  protected final List<Column<?>> m_columns = new ArrayList<Column<?>>();
  protected final Deque<DatabaseOperation> m_opQueue = new LinkedList<DatabaseOperation>();
  protected String m_tableName;


  // constructors


  /**
   *
   */
  protected PersistentObject()
  {
    m_tableName = null;
  }

  /**
   *
   * @param tableName
   */
  protected PersistentObject(String tableName)
  {
    m_tableName = tableName;
  }

  // public methods


  /**
   * Copies information from an Inca schema XmlBean object so that this object
   * contains equivalent information.
   *
   * @param o the XmlBean object to copy
   * @return this, for convenience
   */
  public abstract PersistentObject fromBean(XmlObject o);

  /**
   * Returns a Inca schema XmlBean object that contains information equivalent
   * to this object.
   *
   * @return an XmlBean object that contains equivalent information
   */
  public abstract XmlObject toBean();

  /**
   * Indicates whether or not the object has been persisted.
   *
   * @return <code>true</code> if the object has not been persisted
   */
  public boolean isNew()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns XML that represents the information in this object.
   */
  public String toXml()
  {
    return toBean().xmlText();
  }

  /**
   *
   * @throws SQLException
   * @throws PersistenceException
   */
  public void save() throws SQLException, PersistenceException
  {
    if (isNew())
      pushInsertOps();
    else
      pushUpdateOps();

    executeOps();
  }

  /**
   *
   * @throws SQLException
   * @throws PersistenceException
   */
  public void delete() throws SQLException, PersistenceException
  {
    if (isNew())
      throw new PersistenceException("Not persisted");

    m_opQueue.add(new DeleteOp(m_tableName, getKey()));

    executeOps();

    for (Column<?> col : m_columns)
      col.assignValue(null);
  }


  // package methods


  /**
   * Returns a <code>Criterion</code> object that describes the primary key of the record
   * that the object represents.
   *
   * @return a <code>Criterion</code> object that describes the primary key
   */
  Criterion getKey()
  {
    throw new UnsupportedOperationException();
  }


  // protected methods


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

  /**
   *
   * @param tableName
   * @param columns
   */
  protected void construct(String tableName, Column<?>... columns)
  {
    m_tableName = tableName;

    construct(columns);
  }

  /**
   * A convenience function for implementations that checks the length of a
   * string and truncates with a warning if it's too long.
   *
   * @param s the string to check
   * @param max the maximum length
   * @param label included in the warning message if truncation occurs
   * @return s, truncated if necessary
   */
  protected String truncate(String s, int max, String label)
  {
    if (s != null && s.length() > max) {
      m_logger.warn("Truncating " + label + " '" + s + "' to fit into a DB table column of " + max + " chars");

      s = s.substring(0, max);
    }

    return s;
  }

  /**
   *
   */
  protected void pushInsertOps()
  {
    throw new UnsupportedOperationException();
  }

  /**
   *
   */
  protected void pushUpdateOps()
  {
    assert !m_columns.isEmpty();

    List<Column<?>> modifiedCols = new ArrayList<Column<?>>();

    for (Column<?> col : m_columns) {
      if (col.isModified())
        modifiedCols.add(col);
    }

    m_opQueue.push(new UpdateOp(m_tableName, getKey(), modifiedCols));
  }

  /**
   *
   * @return
   * @throws SQLException
   * @throws PersistenceException
   */
  protected boolean executeOps() throws SQLException, PersistenceException
  {
    Connection dbConn = ConnectionSource.getConnection();

    try {
      dbConn.setAutoCommit(false);

      while (!m_opQueue.isEmpty()) {
        DatabaseOperation op = m_opQueue.remove();

        if (!op.execute(dbConn))
          return false;
      }

      dbConn.commit();

      return true;
    }
    catch (SQLException | PersistenceException err) {
      m_logger.error(err);

      dbConn.rollback();

      throw err;
    }
    finally {
      dbConn.close();
    }
  }

  /**
   *
   * @throws SQLException
   * @throws PersistenceException
   */
  protected void load() throws SQLException, PersistenceException
  {
    assert !m_columns.isEmpty();

    if (isNew())
      throw new PersistenceException("Not persisted");

    Criterion key = getKey();

    m_opQueue.add(new SelectOp(m_tableName, key, m_columns));

    if (!executeOps())
      throw new PersistenceException("No row in " + m_tableName +  " for key " + key.toString());
  }
}
