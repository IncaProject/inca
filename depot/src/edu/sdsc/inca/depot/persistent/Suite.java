package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;


/**
 * A Suite is a named collection of SeriesConfigs.
 */
public class Suite extends GeneratedKeyRow implements Comparable<Suite> {

  // nested classes


  /**
   *
   */
  private class AddSeriesConfigOp implements RowOperation {

    // data fields


    private final SeriesConfig m_element;


    // constructors


    protected AddSeriesConfigOp(SeriesConfig element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      if (m_element.isNew())
        m_element.save(dbConn);

      Column<Long> seriesConfigId = new LongColumn("incaseriesconfig_id", false, m_element.getId());
      Column<Long> suiteId = new LongColumn("incasuite_id", false, getId());
      List<Column<?>> cols = new ArrayList<Column<?>>();

      cols.add(seriesConfigId);
      cols.add(suiteId);

      (new InsertOp("INCASUITESSERIESCONFIGS", cols)).execute(dbConn);
    }
  }

  /**
   *
   */
  private class RemoveSeriesConfigOp implements RowOperation {

    // data fields


    private final SeriesConfig m_element;


    // constructors


    protected RemoveSeriesConfigOp(SeriesConfig element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      assert !m_element.isNew();

      Column<Long> seriesConfigId = new LongColumn("incaseriesconfig_id", false, m_element.getId());
      Column<Long> suiteId = new LongColumn("incasuite_id", false, getId());
      CompositeKey key = new CompositeKey(seriesConfigId, suiteId);

      (new DeleteOp("INCASUITESSERIESCONFIGS", key)).execute(dbConn);
    }
  }

  /**
   *
   */
  private class SeriesConfigSet extends MonitoredSet<SeriesConfig> {

    // constructors


    protected SeriesConfigSet(Set<SeriesConfig> configs)
    {
      super(configs);
    }


    // protected methods


    @Override
    protected void addSetAddOp(SeriesConfig element)
    {
      m_opQueue.add(new AddSeriesConfigOp(element));
    }

    @Override
    protected void addSetRemoveOp(SeriesConfig element)
    {
      m_opQueue.add(new RemoveSeriesConfigOp(element));
    }

    @Override
    protected void addSetClearOp(List<SeriesConfig> elements)
    {
      for (SeriesConfig element : elements)
        addSetRemoveOp(element);
    }
  }


  // data fields


  private static final String TABLE_NAME = "INCASUITE";
  private static final String KEY_NAME = "incaid";
  private final Column<String> m_name = new StringColumn("incaname", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_guid = new StringColumn("incaguid", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_description = new StringColumn("incadescription", true, MAX_DB_STRING_LENGTH);
  private final Column<Integer> m_version = new IntegerColumn("incaversion", true);
  private final Deque<RowOperation> m_opQueue = new LinkedList<RowOperation>();
  private SeriesConfigSet m_seriesConfigs;


  // constructors


  /**
   *
   */
  public Suite()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_name, m_guid, m_description, m_version);
  }

  /**
   *
   * @param name the name of the Suite
   * @param guid the guid (agent + name) of the Suite
   * @param description the description of the Suite
   */
  public Suite(String name, String guid, String description)
  {
    this();

    setName(name);
    setGuid(guid);
    setDescription(description);
    setVersion(0);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Suite(long id) throws IOException, SQLException, PersistenceException
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
  Suite(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
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
    name = normalize(name, MAX_DB_STRING_LENGTH, "suite name");

    m_name.setValue(name);
  }

  /**
   *
   * @return
   */
  public String getGuid()
  {
    return m_guid.getValue();
  }

  /**
   *
   * @param guid
   */
  public void setGuid(String guid)
  {
    guid = normalize(guid, MAX_DB_STRING_LENGTH, "guid");

    m_guid.setValue(guid);
  }

  /**
   *
   * @return
   */
  public String getDescription()
  {
    return m_description.getValue();
  }

  /**
   *
   * @param description
   */
  public void setDescription(String description)
  {
    description = normalize(description, MAX_DB_STRING_LENGTH, "suite description");

    m_description.setValue(description);
  }

  /**
   *
   * @return
   */
  public int getVersion()
  {
    return m_version.getValue();
  }

  /**
   *
   * @param version
   */
  public void setVersion(Integer version)
  {
    m_version.setValue(version);
  }

  /**
   *
   */
  public void incrementVersion()
  {
    int version = m_version.getValue();

    version += 1;

    m_version.setValue(version);
  }

  /**
   * Gets the collection of SeriesConfig objects associated with this Suite.
   *
   * @return the set of associated SeriesConfigs
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<SeriesConfig> getSeriesConfigs() throws IOException, SQLException, PersistenceException
  {
    if (m_seriesConfigs == null) {
      Set<SeriesConfig> configs = new HashSet<SeriesConfig>();

      if (!isNew()) {
        try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
             PreparedStatement selectStmt = dbConn.prepareStatement(
               "SELECT incaseriesconfig_id " +
               "FROM INCASUITESSERIESCONFIGS " +
               "WHERE incasuite_id = ?"
        )) {
          m_key.setParameter(selectStmt, 1);

          ResultSet rows = selectStmt.executeQuery();

          while (rows.next()) {
            long seriesConfigId = rows.getLong(1);

            configs.add(new SeriesConfig(dbConn, seriesConfigId));
          }
        }
      }

      m_seriesConfigs = new SeriesConfigSet(configs);
    }

    return m_seriesConfigs;
  }

  /**
   * Retrieves a specified SeriesConfig from the associated set.
   *
   * @param i the index into the set of the desired SeriesConfig
   * @return the specified SeriesConfig; null if none
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public SeriesConfig getSeriesConfig(int i) throws IOException, SQLException, PersistenceException
  {
    Set<SeriesConfig> configs = getSeriesConfigs();

    for (SeriesConfig sc : configs) {
      if (i == 0)
        return sc;

      i -= 1;
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) throws IOException, SQLException, PersistenceException
  {
    return fromBean((edu.sdsc.inca.dataModel.suite.Suite) o);
  }

  /**
   * Copies information from an Inca schema XmlBean Suite object so that this
   * object contains equivalent information.
   *
   * @param s the XmlBean Suite object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Suite fromBean(edu.sdsc.inca.dataModel.suite.Suite s) throws IOException, SQLException, PersistenceException
  {
    setName(s.getName());
    setGuid(s.getGuid());
    setDescription(s.getDescription());

    if (s.getVersion() != null)
      setVersion(s.getVersion().intValue());

    edu.sdsc.inca.dataModel.util.SeriesConfig[] scs = s.getSeriesConfigs().getSeriesConfigArray();
    Set<SeriesConfig> configs = getSeriesConfigs();

    for (int i = 0 ; i < scs.length ; i++)
      configs.add(new SeriesConfig().fromBean(scs[i]));

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean() throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.suite.Suite result = edu.sdsc.inca.dataModel.suite.Suite.Factory.newInstance();

    result.setName(getName());
    result.setGuid(getGuid());
    result.setDescription(getDescription());
    result.setVersion(BigInteger.valueOf(getVersion()));

    edu.sdsc.inca.dataModel.suite.Suite.SeriesConfigs resultSeriesConfigs = result.addNewSeriesConfigs();

    Iterator<SeriesConfig> it = getSeriesConfigs().iterator();

    for (int i = 0 ; it.hasNext() ; i++) {
      resultSeriesConfigs.addNewSeriesConfig();
      resultSeriesConfigs.setSeriesConfigArray(i, (edu.sdsc.inca.dataModel.util.SeriesConfig) it.next().toBean());
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toXml() throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.suite.SuiteDocument doc = edu.sdsc.inca.dataModel.suite.SuiteDocument.Factory.newInstance();

    doc.setSuite((edu.sdsc.inca.dataModel.suite.Suite) toBean());

    return doc.xmlText();
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

    if (other instanceof Suite == false)
      return false;

    Suite otherSuite = (Suite) other;

    return getGuid().equals(otherSuite.getGuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * getGuid().hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Suite other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }

  /**
   * Returns a phony suite with a given number of unrelated (sharing neither
   * resource nor context) SeriesConfigs.  Useful for testing.
   *
   * @param guid the suite guid
   * @param seriesConfigs the number of series configs to populate the suite
   * @return a new Suite
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Suite generate(String guid, int seriesConfigs) throws IOException, SQLException, PersistenceException
  {
    Suite result = new Suite("A suite name", guid, "auto-suite");

    for (int i = 0 ; i < seriesConfigs ; i++) {
      SeriesConfig sc = SeriesConfig.generate("host" + i, "reporter" + i, 3);

      sc.setNickname(guid + " series " + i);
      result.getSeriesConfigs().add(sc);
    }

    return result;
  }

  /**
   *
   * @param suite
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Suite find(Suite suite) throws IOException, SQLException, PersistenceException
  {
    return find(suite.getGuid());
  }

  /**
   *
   * @param guid
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Suite find(String guid) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Long id = find(dbConn, guid);

      if (id == null)
        return null;

      return new Suite(dbConn, id);
    }
  }


  // package methods


  /**
   * {@inheritDoc}
   */
  @Override
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.save(dbConn);

    while (!m_opQueue.isEmpty()) {
      RowOperation op = m_opQueue.remove();

      op.execute(dbConn);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.load(dbConn);

    m_opQueue.clear();

    m_seriesConfigs = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    m_opQueue.clear();

    m_seriesConfigs = null;

    deleteDependencies(dbConn, m_key.getValue());

    return super.delete(dbConn);
  }

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
    deleteDependencies(dbConn, id);

    Criterion key = new LongCriterion(KEY_NAME, id);

    return Row.delete(dbConn, TABLE_NAME, key);
  }


  // protected methods


  /**
   *
   * @param configs
   * @throws PersistenceException
   * @throws SQLException
   * @throws IOException
   */
  protected void setSeriesConfigs(Set<SeriesConfig> configs) throws IOException, SQLException, PersistenceException
  {
    getSeriesConfigs().clear();

    if (configs != null)
      getSeriesConfigs().addAll(configs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn) throws SQLException
  {
    return find(dbConn, getGuid());
  }


  // private methods


  /**
   *
   */
  private static Long find(Connection dbConn, String guid) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT incaid " +
      "FROM INCASUITE " +
      "WHERE incaguid = ?"
    )) {
      selectStmt.setString(1, guid);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }

  /**
   *
   */
  private static void deleteDependencies(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
  {
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM INCASUITESSERIESCONFIGS " +
      "WHERE incasuite_id = ?"
    )) {
      deleteStmt.setLong(1, id);
      deleteStmt.executeUpdate();
    }
  }
}
