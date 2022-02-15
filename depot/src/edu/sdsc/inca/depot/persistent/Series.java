/*
 * Series.java
 */
package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;


/**
 * @author Cathie Olschanowsky
 * @author Jim Hayes
 * @author Paul Hoover
 *
 * This class represents a series of reports.  Each report in a series shares
 * the same resource and context
 */
public class Series extends GeneratedKeyRow implements Comparable<Series> {

  // nested classes


  /**
   *
   */
  private class AddReportOp implements RowOperation {

    // data fields


    private final Report m_element;


    // constructors


    protected AddReportOp(Report element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      m_element.setSeries(Series.this);
      m_element.save(dbConn);
    }
  }

  /**
   *
   */
  private class RemoveReportOp implements RowOperation {

    // data fields


    private final Report m_element;


    // constructors


    protected RemoveReportOp(Report element)
    {
      m_element = element;
    }


    // public methods


    @Override
    public void execute(Connection dbConn) throws IOException, SQLException, PersistenceException
    {
      m_element.delete(dbConn);
    }
  }

  /**
   *
   */
  private class ReportSet extends MonitoredSet<Report> {

    // constructors


    protected ReportSet(Set<Report> reports)
    {
      super(reports);
    }


    // protected methods


    @Override
    protected void addSetAddOp(Report element)
    {
      m_opQueue.add(new AddReportOp(element));
    }

    @Override
    protected void addSetRemoveOp(Report element)
    {
      m_opQueue.add(new RemoveReportOp(element));
    }

    @Override
    protected void addSetClearOp(List<Report> elements)
    {
      for (Report element : elements)
        addSetRemoveOp(element);
    }
  }

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
      m_element.setSeries(Series.this);
      m_element.save(dbConn);
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
      m_element.delete(dbConn);
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


  private static final Logger m_log = Logger.getLogger(Series.class);
  private static final String TABLE_NAME = "INCASERIES";
  private static final String KEY_NAME = "incaid";
  private final Column<String> m_reporter = new StringColumn("incareporter", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_version = new StringColumn("incaversion", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_uri = new StringColumn("incauri", true, MAX_DB_LONG_STRING_LENGTH);
  private final Column<String> m_context = new StringColumn("incacontext", false, MAX_DB_LONG_STRING_LENGTH);
  private final Column<Boolean> m_nice = new BooleanColumn("incanice", true);
  private final Column<String> m_resource = new StringColumn("incaresource", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_targetHostname = new StringColumn("incatargethostname", true, MAX_DB_STRING_LENGTH);
  private final Column<String> m_instanceTableName = new StringColumn("incainstancetablename", true, MAX_DB_STRING_LENGTH);
  private final Column<String> m_linkTableName = new StringColumn("incalinktablename", true, MAX_DB_STRING_LENGTH);
  private final Column<Long> m_argSignatureId = new LongColumn("incaargSignature_id", false);
  private final Deque<RowOperation> m_opQueue = new LinkedList<RowOperation>();
  private ArgSignature m_argSignature;
  private ReportSet m_reports;
  private SeriesConfigSet m_seriesConfigs;


  // constructors


  /**
   *
   */
  public Series()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_reporter, m_version, m_uri, m_context, m_nice, m_resource, m_targetHostname, m_instanceTableName, m_linkTableName, m_argSignatureId);

    m_reporter.setValue(DB_EMPTY_STRING);
    m_version.setValue(DB_EMPTY_STRING);
    m_uri.setValue(DB_EMPTY_STRING);
    m_context.setValue(DB_EMPTY_STRING);
    m_resource.setValue(DB_EMPTY_STRING);
    m_targetHostname.setValue(DB_EMPTY_STRING);

    m_argSignature = new ArgSignature();
  }

  /**
   *
   * @param resource
   * @param context
   * @param reporter
   */
  public Series(String resource, String context, String reporter)
  {
    this();

    setResource(resource);
    setContext(context);
    setReporter(reporter);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Series(long id) throws IOException, SQLException, PersistenceException
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
  Series(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
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
  public String getReporter()
  {
    return m_reporter.getValue();
  }

  /**
   *
   * @param reporter
   */
  public void setReporter(String reporter)
  {
    reporter = normalize(reporter, MAX_DB_STRING_LENGTH, "reporter");

    m_reporter.setValue(reporter);
  }

  /**
   *
   * @return
   */
  public String getVersion()
  {
    return m_version.getValue();
  }

  /**
   *
   * @param version
   */
  public void setVersion(String version)
  {
    version = normalize(version, MAX_DB_STRING_LENGTH, "version");

    m_version.setValue(version);
  }

  /**
   *
   * @return
   */
  public String getUri()
  {
    return m_uri.getValue();
  }

  /**
   *
   * @param uri
   */
  public void setUri(String uri)
  {
    uri = normalize(uri, MAX_DB_LONG_STRING_LENGTH, "uri");

    m_uri.setValue(uri);
  }

  /**
   *
   * @return
   */
  public String getContext()
  {
    return m_context.getValue();
  }

  /**
   *
   * @param context
   */
  public void setContext(String context)
  {
    context = normalize(context, MAX_DB_LONG_STRING_LENGTH, "context");

    m_context.setValue(context);
  }

  /**
   *
   * @return
   */
  public boolean getNice()
  {
    return m_nice.getValue();
  }

  /**
   *
   * @param nice
   */
  public void setNice(Boolean nice)
  {
    m_nice.setValue(nice);
  }

  /**
   *
   * @return
   */
  public String getResource()
  {
    return m_resource.getValue();
  }

  /**
   *
   * @param resource
   */
  public void setResource(String resource)
  {
    resource = normalize(resource, MAX_DB_STRING_LENGTH, "resource");

    m_resource.setValue(resource);
  }

  /**
   *
   * @return
   */
  public String getTargetHostname()
  {
    return m_targetHostname.getValue();
  }

  /**
   *
   * @param target
   */
  public void setTargetHostname(String target)
  {
    target = normalize(target, MAX_DB_STRING_LENGTH, "target hostname");

    m_targetHostname.setValue(target);
  }

  /**
   *
   * @return
   */
  public String getInstanceTableName()
  {
    return m_instanceTableName.getValue();
  }

  /**
   *
   * @param name
   */
  void setInstanceTableName(String name)
  {
    m_instanceTableName.setValue(name);
  }

  /**
   *
   * @return
   */
  public String getLinkTableName()
  {
    return m_linkTableName.getValue();
  }

  /**
   *
   * @param name
   */
  void setLinkTableName(String name)
  {
    m_linkTableName.setValue(name);
  }

  /**
   *
   * @return
   */
  public ArgSignature getArgSignature()
  {
    return m_argSignature;
  }

  /**
   *
   * @param signature
   */
  public void setArgSignature(ArgSignature signature)
  {
    if (!m_argSignature.isNew())
      m_opQueue.add(new DeleteRowOp(m_argSignature));

    m_argSignature = signature;
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Set<Report> getReports() throws IOException, SQLException, PersistenceException
  {
    if (m_reports == null) {
      Set<Report> reports = new HashSet<Report>();

      if (!isNew()) {
        try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection();
             PreparedStatement selectStmt = dbConn.prepareStatement(
               "SELECT incaid " +
               "FROM INCAREPORT " +
               "WHERE incaseries_id = ?"
        )) {
          m_key.setParameter(selectStmt, 1);

          ResultSet rows = selectStmt.executeQuery();

          while (rows.next()) {
            long reportId = rows.getLong(1);

            reports.add(new Report(reportId));
          }
        }
      }

      m_reports = new ReportSet(reports);
    }

    return m_reports;
  }

  /**
   *
   * @return
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
               "SELECT incaid " +
               "FROM INCASERIESCONFIG " +
               "WHERE incaseries_id = ?"
        )) {
          m_key.setParameter(selectStmt, 1);

          ResultSet rows = selectStmt.executeQuery();

          while (rows.next()) {
            long configId = rows.getLong(1);

            configs.add(new SeriesConfig(configId));
          }
        }
      }

      m_seriesConfigs = new SeriesConfigSet(configs);
    }

    return m_seriesConfigs;
  }

  /**
   *
   * @param instanceName
   * @param linkName
   * @throws SQLException
   */
  public static void createInstanceTables(String instanceName, String linkName) throws SQLException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      dbConn.setAutoCommit(false);

      createInstanceTables(dbConn, instanceName, linkName);
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceName
   * @param linkName
   * @throws SQLException
   */
  public static void createInstanceTables(Connection dbConn, String instanceName, String linkName) throws SQLException
  {
    String keyTypeName = DatabaseTools.getKeyTypeName();
    String longTypeName = DatabaseTools.getLongTypeName();
    String dateTypeName = DatabaseTools.getDateTypeName();
    String floatTypeName = DatabaseTools.getTextTypeName();
    String stringTypeName = DatabaseTools.getStringTypeName();
    StringBuilder instanceQueryBuilder = new StringBuilder();

    instanceQueryBuilder.append("CREATE TABLE ");
    instanceQueryBuilder.append(instanceName);
    instanceQueryBuilder.append(" ( incaid ");
    instanceQueryBuilder.append(keyTypeName);
    instanceQueryBuilder.append(" NOT NULL, incacollected ");
    instanceQueryBuilder.append(dateTypeName);
    instanceQueryBuilder.append(" NOT NULL, incacommited ");
    instanceQueryBuilder.append(dateTypeName);
    instanceQueryBuilder.append(" NOT NULL, incamemoryusagemb ");
    instanceQueryBuilder.append(floatTypeName);
    instanceQueryBuilder.append(" NOT NULL, incacpuusagesec ");
    instanceQueryBuilder.append(floatTypeName);
    instanceQueryBuilder.append(" NOT NULL, incawallclocktimesec ");
    instanceQueryBuilder.append(floatTypeName);
    instanceQueryBuilder.append(" NOT NULL, incalog ");
    instanceQueryBuilder.append(stringTypeName);
    instanceQueryBuilder.append("(");
    instanceQueryBuilder.append(MAX_DB_LONG_STRING_LENGTH);
    instanceQueryBuilder.append("), incareportid ");
    instanceQueryBuilder.append(longTypeName);
    instanceQueryBuilder.append(" NOT NULL, PRIMARY KEY (incaid) )");

    StringBuilder linkQueryBuilder = new StringBuilder();

    linkQueryBuilder.append("CREATE TABLE ");
    linkQueryBuilder.append(linkName);
    linkQueryBuilder.append(" ( incainstance_id ");
    linkQueryBuilder.append(longTypeName);
    linkQueryBuilder.append(" NOT NULL, incaseriesconfig_id ");
    linkQueryBuilder.append(longTypeName);
    linkQueryBuilder.append(" NOT NULL, PRIMARY KEY (incaseriesconfig_id, incainstance_id), FOREIGN KEY (incainstance_id) REFERENCES ");
    linkQueryBuilder.append(instanceName);
    linkQueryBuilder.append(" )");

    try (Statement createStmt = dbConn.createStatement()) {
      createStmt.executeUpdate(instanceQueryBuilder.toString());
      createStmt.executeUpdate(linkQueryBuilder.toString());

      if (!DatabaseTools.usesGeneratedKeys()) {
        StringBuilder sequenceQueryBuilder = new StringBuilder();

        sequenceQueryBuilder.append("CREATE SEQUENCE ");
        sequenceQueryBuilder.append(instanceName);
        sequenceQueryBuilder.append("_incaid_seq START WITH 1");

        createStmt.executeUpdate(sequenceQueryBuilder.toString());
      }

      dbConn.commit();
    }
    catch (SQLException sqlErr) {
      dbConn.rollback();

      throw sqlErr;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) throws IOException, SQLException, PersistenceException
  {
    if (o instanceof edu.sdsc.inca.dataModel.util.Report)
      return fromBean((edu.sdsc.inca.dataModel.util.Report)o);
    else
      return fromBean((edu.sdsc.inca.dataModel.util.Series)o);
  }

  /**
   * Copies information from an Inca schema XmlBean Report object so that this
   * object contains equivalent information.
   *
   * @param r the XmlBean Report object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Series fromBean(edu.sdsc.inca.dataModel.util.Report r) throws IOException, SQLException, PersistenceException
  {
    setReporter(r.getName());
    setVersion(r.getVersion());
    setResource(r.getHostname());
    getArgSignature().fromBean(r.getArgs());

    return this;
  }

  /**
   * Copies information from an Inca schema XmlBean Series object so that this
   * object contains equivalent information.
   *
   * @param s the XmlBean Series object to copy
   * @return this, for convenience
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public Series fromBean(edu.sdsc.inca.dataModel.util.Series s) throws IOException, SQLException, PersistenceException
  {
    setReporter(s.getName());

    if (s.getVersion() != null)
      setVersion(s.getVersion());

    setUri(s.getUri());
    setContext(s.getContext());
    setNice(s.getNice());

    if (s.getArgs() != null)
      getArgSignature().fromBean(s.getArgs());

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean() throws IOException, SQLException, PersistenceException
  {
    edu.sdsc.inca.dataModel.util.Series result = edu.sdsc.inca.dataModel.util.Series.Factory.newInstance();

    result.setName(getReporter());

    if (this.getVersion() != null)
      result.setVersion(getVersion());

    if (this.getUri() != null)
      result.setUri(getUri());

    result.setArgs((edu.sdsc.inca.dataModel.util.Args) getArgSignature().toBean());
    result.setContext(getContext());
    result.setNice(getNice());

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return getResource() + "," + getContext() + "," + getVersion();
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

    if (other instanceof Series == false)
      return false;

    Series otherSeries = (Series) other;

    return getResource().equals(otherSeries.getResource()) &&
           getContext().equals(otherSeries.getContext()) &&
           getVersion().equals(otherSeries.getVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (getResource().hashCode() + getContext().hashCode() + getVersion().hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Series other)
  {
    if (other == null)
      throw new NullPointerException("other");

    if (this == other)
      return 0;

    return hashCode() - other.hashCode();
  }

  /**
   * Generates a phony report for this Series.  Useful for testing.
   *
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public String generateReport() throws IOException, SQLException, PersistenceException
  {
    return generateReport(new Date());
  }

  /**
   * Generates a phony report for this Series.  Useful for testing.
   *
   * @param d
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public String generateReport(Date d) throws IOException, SQLException, PersistenceException
  {
    String argsXml = "  <args>\n";
    // Current time in ISO 8601 format
    String gmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(d);

    gmt = gmt.substring(0,gmt.length()-2) + ":" + gmt.substring(gmt.length()-2);

    for (Arg a : m_argSignature.getArgs()) {
      argsXml += "    <arg>\n" +
                 "      <name>" + a.getName() + "</name>\n" +
                 "      <value>" + a.getValue() + "</value>\n" +
                 "    </arg>\n";
    }

    argsXml += "  </args>\n";

    return
      "<rep:report xmlns:rep=\"http://inca.sdsc.edu/dataModel/report_2.1\">\n" +
      "  <gmt>" + gmt + "</gmt>\n" +
      "  <hostname>" + this.getResource() + "</hostname>\n" +
      "  <name>" + this.getReporter() + "</name>\n" +
      "  <version>" + this.getVersion() + "</version>\n" +
      "  <workingDir>/tmp</workingDir>\n" +
      "  <reporterPath>" + this.getReporter() + "</reporterPath>\n" +
      argsXml +
      "  <body><some>xml</some></body>\n" +
      "  <exitStatus>\n" +
      "    <completed>true</completed>\n" +
      "  </exitStatus>\n" +
      "</rep:report>\n";
  }

  /**
   * Returns a phony Series with a given resource and context.  Useful for
   * testing.
   *
   * @param resource the series resource
   * @param context the series context
   * @param args the number of phony arguments to generate
   * @return a new Series
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Series generate(String resource, String context, int args) throws IOException, SQLException, PersistenceException
  {
    Series result = new Series(resource, context, "any.reporter");
    Set<Arg> argSet = result.getArgSignature().getArgs();

    for (int i = 0 ; i < args ; i++)
      argSet.add(new Arg("arg" + i, "" + (i * 3)));

    return result;
  }

  /**
   *
   * @param series
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Series find(Series series) throws IOException, SQLException, PersistenceException
  {
    return find(series.getVersion(), series.getContext(), series.getResource());
  }

  /**
   *
   * @param version
   * @param context
   * @param resource
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static Series find(String version, String context, String resource) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Long id = find(dbConn, version, context, resource);

      if (id == null)
        return null;

      return new Series(dbConn, id);
    }
  }


  // package methods


  /**
   * {@inheritDoc}
   */
  @Override
  void save(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    m_argSignature.save(dbConn);

    m_argSignatureId.setValue(m_argSignature.getId());

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

    m_argSignature = new ArgSignature(m_argSignatureId.getValue());
    m_reports = null;
    m_seriesConfigs = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    m_opQueue.clear();

    m_argSignature = null;
    m_reports = null;
    m_seriesConfigs = null;

    deleteTables(dbConn);

    Criterion key = new LongCriterion("incaseries_id", m_key.getValue());

    Report.delete(dbConn, key);
    SeriesConfig.delete(dbConn, key);

    return super.delete(dbConn);
  }


  // protected methods


  /**
   *
   * @param reports
   * @throws PersistenceException
   * @throws SQLException
   * @throws IOException
   */
  protected void setReports(Collection<Report> reports) throws IOException, SQLException, PersistenceException
  {
    getReports().clear();

    if (reports != null)
      getReports().addAll(reports);
  }

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

  /*
   * {@inheritDoc}
   */
  @Override
  protected boolean insert(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    if (m_uri.isNull())
      m_uri.setValue(DB_EMPTY_STRING);

    if (m_targetHostname.isNull())
      m_targetHostname.setValue(DB_EMPTY_STRING);

    boolean inserted = super.insert(dbConn);

    if (inserted)
      createTables(dbConn);

    return inserted;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long findDuplicate(Connection dbConn) throws SQLException
  {
    return find(dbConn, getVersion(), getContext(), getResource());
  }


  // private methods


  /**
   *
   */
  private void createTables(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    long seriesId = getId();
    String instanceTableName = "incainstanceinfo_" + seriesId;
    String linkTableName = "incaseriesconfigsinstances_" + seriesId;

    setInstanceTableName(instanceTableName);
    setLinkTableName(linkTableName);

    m_log.debug("Creating tables " + instanceTableName + " and " + linkTableName + " for Series " + seriesId);

    createInstanceTables(dbConn, instanceTableName, linkTableName);

    List<Column<?>> modifiedCols = new ArrayList<Column<?>>();

    modifiedCols.add(m_instanceTableName);
    modifiedCols.add(m_linkTableName);

    (new UpdateOp(TABLE_NAME, getKey(), modifiedCols)).execute(dbConn);
  }

  /**
   *
   */
  private void deleteTables(Connection dbConn) throws SQLException
  {
    try (Statement dropStmt = dbConn.createStatement()) {
      dropStmt.executeUpdate("DROP TABLE " + m_linkTableName.getValue() + " CASCADE");
      dropStmt.executeUpdate("DROP TABLE " + m_instanceTableName.getValue() + " CASCADE");

      if (!DatabaseTools.usesGeneratedKeys())
        dropStmt.executeUpdate("DROP SEQUENCE " + m_instanceTableName.getValue() + "_incaid_seq CASCADE");
    }
  }

  /**
   *
   */
  private static Long find(Connection dbConn, String version, String context, String resource) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT incaid " +
      "FROM INCASERIES " +
      "WHERE incaversion = ? " +
        "AND incacontext = ? " +
        "AND incaresource = ?"
    )) {
      selectStmt.setString(1, version);
      selectStmt.setString(2, context);
      selectStmt.setString(3, resource);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }
}
