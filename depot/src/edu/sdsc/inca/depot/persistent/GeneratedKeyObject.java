/*
 * GeneratedKeyObject.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
abstract class GeneratedKeyObject extends PersistentObject {

  // nested classes


  /**
   *
   */
  private interface InsertOpBuilder {

    /**
     *
     * @param tableName
     * @param key
     * @param columns
     * @param opQueue
     */
    void pushInsertOps(String tableName, Column<Long> key, List<Column<?>> columns, Deque<DatabaseOperation> opQueue);
  }

  /**
   *
   */
  private static class AutoGenInsertOpBuilder implements InsertOpBuilder {

    /**
     *
     * @param tableName
     * @param key
     * @param columns
     * @param opQueue
     */
    @Override
    public void pushInsertOps(String tableName, Column<Long> key, List<Column<?>> columns, Deque<DatabaseOperation> opQueue)
    {
      opQueue.push(new AutoGenKeyInsertOp(tableName, key, columns));
    }
  }

  /**
   *
   */
  private static class SequenceInsertOpBuilder implements InsertOpBuilder {

    /**
     *
     * @param tableName
     * @param key
     * @param columns
     * @param opQueue
     */
    @Override
    public void pushInsertOps(String tableName, Column<Long> key, List<Column<?>> columns, Deque<DatabaseOperation> opQueue)
    {
      List<Column<?>> allColumns = new ArrayList<Column<?>>();

      allColumns.add(key);
      allColumns.addAll(columns);

      opQueue.push(new InsertOp(tableName, allColumns));
      opQueue.push(new ReadSequenceOp("hibernate_sequence", key));
    }
  }


  // data fields


  protected Column<Long> m_key;
  private static InsertOpBuilder m_opBuilder;


  static {
    if (DatabaseTools.usesGeneratedKeys())
      m_opBuilder = new AutoGenInsertOpBuilder();
    else
      m_opBuilder = new SequenceInsertOpBuilder();
  }


  // constructors


  /**
   *
   * @param keyName
   */
  protected GeneratedKeyObject(String keyName)
  {
    this(null, keyName);
  }

  /**
   *
   * @param tableName
   * @param keyName
   */
  protected GeneratedKeyObject(String tableName, String keyName)
  {
    super(tableName);

    m_key = new LongColumn(keyName, false);
  }


  // public methods


  /**
   *
   * @return
   */
  @Override
  public boolean isNew()
  {
    return m_key.isNull() || m_key.isModified();
  }

  /**
   *
   * @throws SQLException
   * @throws PersistenceException
   */
  @Override
  public void delete() throws SQLException, PersistenceException
  {
    super.delete();

    m_key.assignValue(null);
  }


  // package methods


  /**
   *
   * @return
   */
  @Override
  Criterion getKey()
  {
    return new SimpleKey(m_key);
  }


  // protected methods


  /**
   *
   */
  @Override
  protected void pushInsertOps()
  {
    m_opBuilder.pushInsertOps(m_tableName, m_key, m_columns, m_opQueue);
  }
}
