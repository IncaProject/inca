/*
 * GeneratedKeyRow.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
abstract class GeneratedKeyRow extends Row {

  // nested classes


  /**
   *
   */
  private interface InsertExecutor {

    void insert(Connection dbConn, String tableName, Column<Long> key, List<Column<?>> columns) throws IOException, SQLException, PersistenceException;
  }

  /**
   *
   */
  private static class AutoGenInsertExecutor implements InsertExecutor {

    // public methods


    @Override
    public void insert(Connection dbConn, String tableName, Column<Long> key, List<Column<?>> columns) throws IOException, SQLException, PersistenceException
    {
      (new AutoGenKeyInsertOp(tableName, key, columns)).execute(dbConn);
    }
  }

  /**
   *
   */
  private static class SequenceInsertExecutor implements InsertExecutor {

    // public methods


    @Override
    public void insert(Connection dbConn, String tableName, Column<Long> key, List<Column<?>> columns) throws IOException, SQLException, PersistenceException
    {
      String seqName = tableName + "_" + key.getName() + "_seq";
      List<Column<?>> allColumns = new ArrayList<Column<?>>();

      allColumns.add(key);
      allColumns.addAll(columns);

      (new ReadSequenceOp(seqName, key)).execute(dbConn);
      (new InsertOp(tableName, allColumns)).execute(dbConn);
    }
  }


  // data fields


  protected final Column<Long> m_key;
  private static InsertExecutor m_executor;


  static {
    if (DatabaseTools.usesGeneratedKeys())
      m_executor = new AutoGenInsertExecutor();
    else
      m_executor = new SequenceInsertExecutor();
  }


  // constructors


  /**
   *
   * @param tableName
   * @param keyName
   */
  protected GeneratedKeyRow(String tableName, String keyName)
  {
    super(tableName);

    m_key = new LongColumn(keyName, false);
  }


  // public methods


  /**
   *
   * @return
   */
  public Long getId()
  {
    if (m_key.isNull())
      return null;

    return m_key.getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNew()
  {
    return m_key.isNull() || m_key.isModified();
  }


  // package methods


  /**
   * {@inheritDoc}
   */
  @Override
  Criterion getKey()
  {
    return new SimpleKey(m_key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    if (!super.delete(dbConn))
      return false;

    m_key.reset();

    return true;
  }


  // protected methods


  /**
   *
   * @param dbConn a <code>Connection</code> object that will be used to communicate with the database
   * @return
   * @throws SQLException
   */
  protected abstract Long findDuplicate(Connection dbConn) throws SQLException;

  /**
   *
   * @param dbConn a <code>Connection</code> object that will be used to communicate with the database
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  protected void reconcileDuplicates(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    m_key.assignValue(id);

    load(dbConn);
  }

  /**
   *
   * @param id
   */
  protected void setId(Long id)
  {
    m_key.setValue(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean insert(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    Long id = findDuplicate(dbConn);

    if (id == null) {
      m_executor.insert(dbConn, m_tableName, m_key, m_columns);

      return true;
    }
    else {
      reconcileDuplicates(dbConn, id);

      return false;
    }
  }
}
