/*
 * HqlIterator.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.type.EntityType;
import org.hibernate.type.SingleColumnType;
import org.hibernate.type.Type;


/**
 *
 * @author Paul Hoover
 *
 */
class HqlIterator implements Iterator<Object> {

  // nested classes


  /**
   *
   */
  private interface Resolver {

    Object resolveRow() throws IOException, SQLException, PersistenceException;
  }


  /**
   *
   */
  private class SingleColumnResolver implements Resolver {

    @Override
    public Object resolveRow() throws IOException, SQLException, PersistenceException
    {
      return resolveColumn(0);
    }
  }


  /**
   *
   */
  private class MultiColumnResolver implements Resolver {

    @Override
    public Object resolveRow() throws IOException, SQLException, PersistenceException
    {
      Object[] result = new Object[m_columnNames.length];

      for (int index = 0 ; index < m_columnNames.length ; index += 1) {
        Object column = resolveColumn(index);

        result[index] = column;
      }

      return result;
    }
  }


  // data fields


  private static final Logger m_log = Logger.getLogger(HqlIterator.class);
  private final SessionImplementor m_session;
  private final Connection m_dbConn;
  private final PreparedStatement m_statement;
  private final ResultSet m_rows;
  private final String[][] m_columnNames;
  private final Type[] m_returnTypes;
  private final Resolver m_resolver;
  private boolean m_hasNext;


  // constructors


  /**
   *
   * @param dbConn
   * @param statement
   * @param rows
   * @param translator
   * @param session
   * @throws SQLException
   */
  public HqlIterator(Connection dbConn, PreparedStatement statement, ResultSet rows, QueryTranslator translator, SessionImplementor session) throws SQLException
  {
    m_session = session;
    m_dbConn = dbConn;
    m_statement = statement;
    m_rows = rows;
    m_columnNames = translator.getColumnNames();
    m_returnTypes = translator.getReturnTypes();
    m_hasNext = true;

    if (m_columnNames.length > 1)
      m_resolver = new MultiColumnResolver();
    else
      m_resolver = new SingleColumnResolver();

    advanceCursor();
  }


  // public methods


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasNext()
  {
    return m_hasNext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object next()
  {
    try {
      if (!hasNext())
        return null;

      Object result = m_resolver.resolveRow();

      advanceCursor();

      return result;
    }
    catch (IOException | SQLException | PersistenceException err) {
      m_log.error("", err);

      return null;
    }
  }


  // private methods


  /**
   *
   */
  private void advanceCursor() throws SQLException
  {
    if (!m_rows.next()) {
      m_rows.close();
      m_statement.close();

      m_hasNext = false;
    }
  }

  /**
   *
   */
  private Object resolveColumn(int index) throws IOException, SQLException, PersistenceException
  {
    Object result;

    if (m_returnTypes[index] instanceof EntityType) {
      EntityType entityType = (EntityType)m_returnTypes[index];
      String entityName = entityType.getAssociatedEntityName();
      long id = m_rows.getLong(m_columnNames[index][0]);

      if (entityName.equals("edu.sdsc.inca.depot.persistent.Arg"))
        result = new Arg(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.ArgSignature"))
        result = new ArgSignature(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.ComparisonResult"))
        result = new ComparisonResult(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.KbArticle"))
        result = new KbArticle(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.Report"))
        result = new Report(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.RunInfo"))
        result = new RunInfo(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.Series"))
        result = new Series(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.SeriesConfig"))
        result = new SeriesConfig(m_dbConn, id);
      else if (entityName.equals("edu.sdsc.inca.depot.persistent.Suite"))
        result = new Suite(m_dbConn, id);
      else {
        m_log.warn("Can't instantiate unknown entity type " + entityName);

        result = null;
      }
    }
    else if (m_returnTypes[index] instanceof SingleColumnType)
      result = m_returnTypes[index].nullSafeGet(m_rows, m_columnNames[index], m_session, null);
    else {
      m_log.warn("Unimplemented column type " + m_returnTypes[index].getName());

      result = null;
    }

    return result;
  }
}
