/*
 * GeneratedKeyObject.java
 */
package edu.sdsc.inca.depot.persistent;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Paul Hoover
 *
 */
abstract class GeneratedKeyObject extends PersistentObject {

  /**
   *
   */
  private interface InsertOpBuilder {

    /**
     *
     * @param tableName
     * @param key
     * @param columns
     * @return
     */
    List<DatabaseOperation> createInsertOps(String tableName, Column<Long> key, List<Column<?>> columns);
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
     * @return
     */
    public List<DatabaseOperation> createInsertOps(String tableName, Column<Long> key, List<Column<?>> columns)
    {
      List<DatabaseOperation> insertOps = new ArrayList<DatabaseOperation>();

      insertOps.add(new AutoGenKeyInsertOp(tableName, key, columns));

      return insertOps;
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
     * @return
     */
    public List<DatabaseOperation> createInsertOps(String tableName, Column<Long> key, List<Column<?>> columns)
    {
      List<Column<?>> allColumns = new ArrayList<Column<?>>();

      allColumns.add(key);
      allColumns.addAll(columns);

      List<DatabaseOperation> insertOps = new ArrayList<DatabaseOperation>();

      insertOps.add(new ReadSequenceOp("hibernate_sequence", key));
      insertOps.add(new InsertOp(tableName, allColumns));

      return insertOps;
    }
  }


  protected Column<Long> m_key;
  private static InsertOpBuilder m_opBuilder;


  static {
    if (DatabaseTools.usesGeneratedKeys())
      m_opBuilder = new AutoGenInsertOpBuilder();
    else
      m_opBuilder = new SequenceInsertOpBuilder();
  }


  // public methods


  /**
   *
   * @return
   */
  public boolean isNew()
  {
    return m_key.isNull();
  }

  /**
   *
   * @throws SQLException
   * @throws PersistenceException
   */
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
  Criterion getKey()
  {
    return new SimpleKey(m_key);
  }


  // protected methods


  /**
   *
   * @param key
   * @param tableName
   * @param columns
   */
  protected void construct(Column<Long> key, String tableName, Column<?>... columns)
  {
    assert m_key == null;

    construct(tableName, columns);

    m_key = key;
  }

  /**
   *
   * @return
   */
  protected List<DatabaseOperation> createInsertOps()
  {
    return m_opBuilder.createInsertOps(m_tableName, m_key, m_columns);
  }
}
