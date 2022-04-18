package edu.sdsc.inca.depot.persistent;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.xmlbeans.XmlObject;


/**
 * Represents host-specific execution information derived from a Report.
 */
public class RunInfo extends GeneratedKeyRow implements Comparable<RunInfo> {

  // data fields


  private static final String TABLE_NAME = "INCARUNINFO";
  private static final String KEY_NAME = "incaid";
  private final Column<String> m_hostname = new StringColumn("incahostname", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_workingDir = new StringColumn("incaworkingDir", false, MAX_DB_STRING_LENGTH);
  private final Column<String> m_reporterPath = new StringColumn("incareporterPath", false, MAX_DB_STRING_LENGTH);
  private final Column<Long> m_argSignatureId = new LongColumn("incaargSignature_id", false);
  private ArgSignature m_argSignature;


  // constructors


  /**
   *
   */
  public RunInfo()
  {
    super(TABLE_NAME, KEY_NAME);

    construct(m_hostname, m_workingDir, m_reporterPath, m_argSignatureId);

    m_hostname.setValue(DB_EMPTY_STRING);
    m_workingDir.setValue(DB_EMPTY_STRING);
    m_reporterPath.setValue(DB_EMPTY_STRING);

    m_argSignature = new ArgSignature();
  }

  /**
   *
   * @param name
   * @param dir
   * @param path
   */
  public RunInfo(String name, String dir, String path)
  {
    this();

    setHostname(name);
    setWorkingDir(dir);
    setReporterPath(path);
  }

  /**
   * @param id
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public RunInfo(long id) throws IOException, SQLException, PersistenceException
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
  RunInfo(Connection dbConn, long id) throws IOException, SQLException, PersistenceException
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
  public String getHostname()
  {
    return m_hostname.getValue();
  }

  /**
   *
   * @param name
   */
  public void setHostname(String name)
  {
    name = normalize(name, MAX_DB_STRING_LENGTH, "hostname");

    m_hostname.setValue(name);
  }

  /**
   *
   * @return
   */
  public String getWorkingDir()
  {
    return m_workingDir.getValue();
  }

  /**
   *
   * @param dir
   */
  public void setWorkingDir(String dir)
  {
    dir = normalize(dir, MAX_DB_STRING_LENGTH, "working dir");

    m_workingDir.setValue(dir);
  }

  /**
   *
   * @return
   */
  public String getReporterPath()
  {
    return m_reporterPath.getValue();
  }

  /**
   *
   * @param path
   */
  public void setReporterPath(String path)
  {
    path = normalize(path, MAX_DB_STRING_LENGTH, "reporter path");

    m_reporterPath.setValue(path);
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
    m_argSignature = signature;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlBeanObject fromBean(XmlObject o) throws IOException, SQLException, PersistenceException
  {
    return this.fromBean((edu.sdsc.inca.dataModel.util.Report) o);
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
  public RunInfo fromBean(edu.sdsc.inca.dataModel.util.Report r) throws IOException, SQLException, PersistenceException
  {
    setHostname(r.getHostname());
    setWorkingDir(r.getWorkingDir());
    setReporterPath(r.getReporterPath());
    setArgSignature(new ArgSignature().fromBean(r.getArgs()));

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XmlObject toBean()
  {
    return null; // No XmlBean equivalent to RunInfo
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toXml()
  {
    // RunInfo has no corresponding XML bean.  This implementation is
    // for debugging purposes.
    return "<runInfo>\n" +
           "  <hostname>" + getHostname() + "</hostname>\n" +
           "  <workingDir>" + getWorkingDir() + "</workingDir>\n" +
           "  <reporterPath>" + getReporterPath() + "</reporterPath>\n" +
           "</runInfo>\n";
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

    if (other instanceof RunInfo == false)
      return false;

    RunInfo otherInfo = (RunInfo) other;

    return getHostname().equals(otherInfo.getHostname()) &&
           getWorkingDir().equals(otherInfo.getWorkingDir()) &&
           getReporterPath().equals(otherInfo.getReporterPath()) &&
           getArgSignature().equals(otherInfo.getArgSignature());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    return 29 * (getHostname().hashCode() + getWorkingDir().hashCode() +
                 getReporterPath().hashCode() + getArgSignature().hashCode());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(RunInfo other)
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
   *
   * @param info
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static RunInfo find(RunInfo info) throws IOException, SQLException, PersistenceException
  {
    return find(info.getHostname(), info.getWorkingDir(), info.getReporterPath(), info.getArgSignature().getSignature());
  }

  /**
   *
   * @param hostname
   * @param workingDir
   * @param reporterPath
   * @param signature
   * @return
   * @throws IOException
   * @throws SQLException
   * @throws PersistenceException
   */
  public static RunInfo find(String hostname, String workingDir, String reporterPath, String signature) throws IOException, SQLException, PersistenceException
  {
    try (Connection dbConn = ConnectionManager.getConnectionSource().getConnection()) {
      Long id = find(dbConn, hostname, workingDir, reporterPath, signature);

      if (id == null)
        return null;

      return new RunInfo(dbConn, id);
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  void load(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    super.load(dbConn);

    m_argSignature = new ArgSignature(m_argSignatureId.getValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean delete(Connection dbConn) throws IOException, SQLException, PersistenceException
  {
    long argSignatureId = m_argSignatureId.getValue();

    boolean result = super.delete(dbConn);

    if (result)
      result = (new ArgSignature(dbConn, argSignatureId)).delete(dbConn);

    return result;
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
    return find(dbConn, m_hostname.getValue(), m_workingDir.getValue(), m_reporterPath.getValue(), m_argSignature.getSignature());
  }


  // private methods


  /**
   *
   * @param dbConn
   * @param hostname
   * @param workingDir
   * @param reporterPath
   * @param signature
   * @return
   * @throws SQLException
   */
  private static Long find(Connection dbConn, String hostname, String workingDir, String reporterPath, String signature) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT INCARUNINFO.incaid " +
      "FROM INCARUNINFO " +
        "INNER JOIN INCAARGSIGNATURE ON INCARUNINFO.incaargSignature_id = INCAARGSIGNATURE.incaid " +
      "WHERE incahostname = ? " +
        "AND incaworkingDir = ? " +
        "AND incareporterPath = ? " +
        "AND incasignature = ?"
    )) {
      selectStmt.setString(1, hostname);
      selectStmt.setString(2, workingDir);
      selectStmt.setString(3, reporterPath);

      if (signature != null)
        selectStmt.setString(4, signature);
      else
        selectStmt.setNull(4, Types.VARCHAR);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return null;

      return row.getLong(1);
    }
  }
}
