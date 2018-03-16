/*
 * SyncResponse.java
 */
package edu.sdsc.inca.depot.commands;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.log4j.Logger;

import edu.sdsc.inca.Depot;
import edu.sdsc.inca.depot.persistent.ConnectionSource;
import edu.sdsc.inca.depot.persistent.DatabaseTools;
import edu.sdsc.inca.depot.persistent.PersistentObject;
import edu.sdsc.inca.protocol.MessageHandler;
import edu.sdsc.inca.protocol.Protocol;
import edu.sdsc.inca.protocol.ProtocolException;
import edu.sdsc.inca.protocol.ProtocolReader;
import edu.sdsc.inca.protocol.Statement;


/**
 *
 * @author Paul Hoover
 *
 */
public class SyncResponse extends MessageHandler {

  /**
   * An <code>OutputStream</code> wrapper that doesn't propagate a <code>close</code>
   * call to the wrapped stream
   */
  private static class ClonedOutputStream extends OutputStream {

    private final OutputStream m_out;


    // constructors


    /**
     *
     * @param out
     */
    public ClonedOutputStream(OutputStream out)
    {
      m_out = out;
    }


    // public methods


    /**
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException
    {
      m_out.flush();
    }

    /**
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException
    {
      m_out.flush();
    }

    /**
     *
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
      m_out.write(b, off, len);
    }

    /**
     *
     * @param b
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException
    {
      m_out.write(b);
    }
  }

  /**
   *
   */
  private static class SeriesTableNames {

    public final long seriesId;
    public final String instanceTableName;
    public final String linkTableName;


    /**
     *
     * @param id
     * @param instance
     * @param link
     */
    public SeriesTableNames(long id, String instance, String link)
    {
      seriesId = id;
      instanceTableName = instance;
      linkTableName = link;
    }
  }


  private static final byte SP = ' ';
  private static final byte[] CRLF = "\r\n".getBytes();
  private static final int FETCH_SIZE = 100;
  private static final Logger m_logger = Logger.getLogger(SyncResponse.class);


  // public methods


  /**
   *
   * @param reader the reader connected to the client
   * @param output the output stream connected to the client
   * @param dn the DN of the client, null if no authentication
   * @throws Exception
   */
  public void execute(ProtocolReader reader, OutputStream output, String dn) throws Exception
  {
    if (Depot.getRunningDepot().syncInProgress())
      throw new Exception("synchronizing");

    if (!isPermitted(dn, Protocol.SYNC_ACTION))
      throw new ProtocolException(Protocol.SYNC_ACTION + " not allowed by " + dn);

    Statement stmt = reader.readStatement();
    String command = new String(stmt.getCmd());
    boolean isSyncReq = command.equals(Protocol.SYNC_COMMAND);

    if (isSyncReq)
      Depot.getRunningDepot().startSyncResponse();

    try {
      output.write(Protocol.SUCCESS_COMMAND.getBytes());

      try {
        output.write(SP);

        writeResponse(output);
      }
      finally {
        output.write(CRLF);
        output.flush();
      }
    }
    catch (Exception err) {
      ByteArrayOutputStream logMessage = new ByteArrayOutputStream();
      PrintStream stream = new PrintStream(logMessage);

      stream.print("Synchronization response failed: ");
      err.printStackTrace(stream);

      m_logger.error(logMessage.toString());

      errorReply(output, err.getMessage());
    }

    if (isSyncReq)
      Depot.getRunningDepot().endSync();

    reader.close();
  }


  // private methods


  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeBooleanValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    boolean value = row.getBoolean(index);

    if (!row.wasNull())
      output.print(value);
  }

  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeIntegerValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    int value = row.getInt(index);

    if (!row.wasNull())
      output.print(value);
  }

  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeLongValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    long value = row.getLong(index);

    if (!row.wasNull())
      output.print(value);
  }

  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeFloatValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    float value = row.getFloat(index);

    if (!row.wasNull())
      output.print(value);
  }

  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeStringValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    String value = row.getString(index);

    if (!row.wasNull()) {
      if (value.length() > 0)
        output.print(value);
      else
        output.print(PersistentObject.DB_EMPTY_STRING);
    }
  }

  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeTimestampValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    Timestamp value = row.getTimestamp(index);

    if (!row.wasNull())
      output.print(value.toString());
  }

  /**
   *
   * @param row
   * @param index
   * @param output
   * @throws SQLException
   */
  private void writeCdataValue(ResultSet row, int index, PrintStream output) throws SQLException
  {
    String value = row.getString(index);

    output.print("<![CDATA[");

    if (!row.wasNull()) {
      if (value.length() > 0)
        output.print(value.replaceAll("]]>", "]]]]><![CDATA[>"));
      else
        output.print(PersistentObject.DB_EMPTY_STRING);
    }

    output.print("]]>");
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeSuites(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incaname, incaguid, incadescription, incaversion " +
        "FROM incasuite"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<suiteRows>");

      while (rows.next()) {
        output.print("<suite><id>");

        writeLongValue(rows, 1, output);

        output.print("</id><name>");

        writeStringValue(rows, 2, output);

        output.print("</name><guid>");

        writeStringValue(rows, 3, output);

        output.print("</guid><description>");

        writeStringValue(rows, 4, output);

        output.print("</description><version>");

        writeIntegerValue(rows, 5, output);

        output.print("</version></suite>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</suiteRows>");

      m_logger.debug("Wrote " + totalWritten + " Suite records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeArgs(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incaname, incavalue " +
        "FROM incaarg"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<argRows>");

      while (rows.next()) {
        output.print("<arg><id>");

        writeLongValue(rows, 1, output);

        output.print("</id><name>");

        writeStringValue(rows, 2, output);

        output.print("</name><value>");

        writeCdataValue(rows, 3, output);

        output.print("</value></arg>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</argRows>");

      m_logger.debug("Wrote " + totalWritten + " Arg records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private Map<Long, List<Long>> getArgMappings(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaargs_id, incainput_id " +
        "FROM incaargs"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      Map<Long, List<Long>> result = new TreeMap<Long, List<Long>>();

      while (rows.next()) {
        long signatureId = rows.getLong(1);
        long argId = rows.getLong(2);
        List<Long> argIds = result.get(signatureId);

        if (argIds == null) {
          argIds = new ArrayList<Long>();

          result.put(signatureId, argIds);
        }

        argIds.add(argId);
      }

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeArgSignatures(Connection dbConn, PrintStream output) throws SQLException
  {
    Map<Long, List<Long>> mappings = getArgMappings(dbConn);
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid " +
        "FROM incaargsignature"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<argSignatureRows>");

      while (rows.next()) {
        long signatureId = rows.getLong(1);

        output.print("<argSignature><id>");
        output.print(signatureId);
        output.print("</id>");

        List<Long> argIds = mappings.get(signatureId);

        if (argIds != null) {
          output.print("<args>");

          for (Long id : argIds) {
            output.print("<id>");
            output.print(id);
            output.print("</id>");
          }

          output.print("</args>");
        }
        else
          output.print("<args/>");

        output.print("</argSignature>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</argSignatureRows>");

      m_logger.debug("Wrote " + totalWritten + " ArgSignature records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeSeries(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incareporter, incaversion, incauri, " +
          "incacontext, incanice, incaresource, incatargethostname, " +
          "incaargSignature_id " +
        "FROM incaseries"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<seriesRows>");

      while (rows.next()) {
        output.print("<series><id>");

        writeLongValue(rows, 1, output);

        output.print("</id><reporter>");

        writeStringValue(rows, 2, output);

        output.print("</reporter><version>");

        writeStringValue(rows, 3, output);

        output.print("</version><uri>");

        writeStringValue(rows, 4, output);

        output.print("</uri><context>");

        writeCdataValue(rows, 5, output);

        output.print("</context><nice>");

        writeBooleanValue(rows, 6, output);

        output.print("</nice><resource>");

        writeStringValue(rows, 7, output);

        output.print("</resource><targetHostname>");

        writeStringValue(rows, 8, output);

        output.print("</targetHostname><argSignatureId>");

        writeLongValue(rows, 9, output);

        output.print("</argSignatureId></series>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</seriesRows>");

      m_logger.debug("Wrote " + totalWritten + " Series records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private Map<Long, List<Long>> getSuiteConfigMappings(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaseriesconfig_id, incasuite_id " +
        "FROM incasuitesseriesconfigs"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      Map<Long, List<Long>> result = new TreeMap<Long, List<Long>>();

      while (rows.next()) {
        long configId = rows.getLong(1);
        long suiteId = rows.getLong(2);
        List<Long> suiteIds = result.get(configId);

        if (suiteIds == null) {
          suiteIds = new ArrayList<Long>();

          result.put(configId, suiteIds);
        }

        suiteIds.add(suiteId);
      }

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private Map<Long, List<String>> getConfigTags(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaseriesconfig_id, tag " +
        "FROM incaseriesconfigtags"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      Map<Long, List<String>> result = new TreeMap<Long, List<String>>();

      while (rows.next()) {
        long configId = rows.getLong(1);
        String tag = rows.getString(2);
        List<String> tags = result.get(configId);

        if (tags == null) {
          tags = new ArrayList<String>();

          result.put(configId, tags);
        }

        tags.add(tag);
      }

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeSeriesConfigs(Connection dbConn, PrintStream output) throws SQLException
  {
    Map<Long, List<Long>> mappings = getSuiteConfigMappings(dbConn);
    Map<Long, List<String>> configTags = getConfigTags(dbConn);
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incaactivated, incadeactivated, incanickname, " +
          "incawallClockTime, incacpuTime, incamemory, incacomparitor, " +
          "incacomparison, incanotifier, incatarget, incatype, incaminute, " +
          "incahour, incamonth, incamday, incawday, incanumOccurs, " +
          "incasuspended, incaseries_id, incalatestInstanceId, " +
          "incalatestComparisonId " +
        "FROM incaseriesconfig"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<seriesConfigRows>");

      while (rows.next()) {
        long configId = rows.getLong(1);

        output.print("<seriesConfig><id>");
        output.print(configId);
        output.print("</id><activated>");

        writeTimestampValue(rows, 2, output);

        output.print("</activated><deactivated>");

        writeTimestampValue(rows, 3, output);

        output.print("</deactivated><nickname>");

        writeStringValue(rows, 4, output);

        output.print("</nickname><wallClockTime>");

        writeFloatValue(rows, 5, output);

        output.print("</wallClockTime><cpuTime>");

        writeFloatValue(rows, 6, output);

        output.print("</cpuTime><memory>");

        writeFloatValue(rows, 7, output);

        output.print("</memory><comparitor>");

        writeStringValue(rows, 8, output);

        output.print("</comparitor><comparison>");

        writeCdataValue(rows, 9, output);

        output.print("</comparison><notifier>");

        writeStringValue(rows, 10, output);

        output.print("</notifier><target>");

        writeStringValue(rows, 11, output);

        output.print("</target><type>");

        writeStringValue(rows, 12, output);

        output.print("</type><minute>");

        writeStringValue(rows, 13, output);

        output.print("</minute><hour>");

        writeStringValue(rows, 14, output);

        output.print("</hour><month>");

        writeStringValue(rows, 15, output);

        output.print("</month><mday>");

        writeStringValue(rows, 16, output);

        output.print("</mday><wday>");

        writeStringValue(rows, 17, output);

        output.print("</wday><numOccurs>");

        writeIntegerValue(rows, 18, output);

        output.print("</numOccurs><suspended>");

        writeBooleanValue(rows, 19, output);

        output.print("</suspended><seriesId>");

        writeLongValue(rows, 20, output);

        output.print("</seriesId><latestInstanceId>");

        writeLongValue(rows, 21, output);

        output.print("</latestInstanceId><latestComparisonId>");

        writeLongValue(rows, 22, output);

        output.print("</latestComparisonId>");

        List<Long> suiteIds = mappings.get(configId);

        if (suiteIds != null) {
          output.print("<suites>");

          for (Long id : suiteIds) {
            output.print("<id>");
            output.print(id);
            output.print("</id>");
          }

          output.print("</suites>");
        }
        else
          output.print("<suites/>");

        List<String> tags = configTags.get(configId);

        if (tags != null) {
          output.print("<tags>");

          for (String tag : tags) {
            output.print("<tag>");
            output.print(tag);
            output.print("</tag>");
          }

          output.print("</tags>");
        }
        else
          output.print("<tags/>");

        output.print("</seriesConfig>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</seriesConfigRows>");

      m_logger.debug("Wrote " + totalWritten + " SeriesConfig records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeRunInfo(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incahostname, incaworkingDir, incareporterPath, " +
          "incaargSignature_id " +
        "FROM incaruninfo"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<runInfoRows>");

      while (rows.next()) {
        output.print("<runInfo><id>");

        writeLongValue(rows, 1, output);

        output.print("</id><hostname>");

        writeStringValue(rows, 2, output);

        output.print("</hostname><workingDir>");

        writeStringValue(rows, 3, output);

        output.print("</workingDir><reporterPath>");

        writeStringValue(rows, 4, output);

        output.print("</reporterPath><argSignatureId>");

        writeLongValue(rows, 5, output);

        output.print("</argSignatureId></runInfo>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</runInfoRows>");

      m_logger.debug("Wrote " + totalWritten + " RunInfo records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeReports(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incaexit_status, incaexit_message, incabodypart1, " +
          "incabodypart2, incabodypart3, incastderr, incaseries_id, " +
          "incarunInfo_id " +
        "FROM incareport"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<reportRows>");

      while (rows.next()) {
        output.print("<report><id>");

        writeLongValue(rows, 1, output);

        output.print("</id><exitStatus>");

        writeBooleanValue(rows, 2, output);

        output.print("</exitStatus><exitMessage>");

        writeCdataValue(rows, 3, output);

        output.print("</exitMessage><bodypart1>");

        writeCdataValue(rows, 4, output);

        output.print("</bodypart1><bodypart2>");

        writeCdataValue(rows, 5, output);

        output.print("</bodypart2><bodypart3>");

        writeCdataValue(rows, 6, output);

        output.print("</bodypart3><stderr>");

        writeCdataValue(rows, 7, output);

        output.print("</stderr><seriesId>");

        writeLongValue(rows, 8, output);

        output.print("</seriesId><runInfoId>");

        writeLongValue(rows, 9, output);

        output.print("</runInfoId></report>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</reportRows>");

      m_logger.debug("Wrote " + totalWritten + " Report records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeComparisonResults(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incaresult, incareportId, incaseriesConfigId " +
        "FROM incacomparisonresult"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<comparisonResultRows>");

      while (rows.next()) {
        output.print("<comparisonResult><id>");

        writeLongValue(rows, 1, output);

        output.print("</id><result>");

        writeCdataValue(rows, 2, output);

        output.print("</result><reportId>");

        writeLongValue(rows, 3, output);

        output.print("</reportId><seriesConfigId>");

        writeLongValue(rows, 4, output);

        output.print("</seriesConfigId></comparisonResult>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</comparisonResultRows>");

      m_logger.debug("Wrote " + totalWritten + " ComparisonResult records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private List<SeriesTableNames> getTableNames(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incainstancetablename, incalinktablename " +
        "FROM incaseries "
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      List<SeriesTableNames> result = new ArrayList<SeriesTableNames>();

      while (rows.next()) {
        long seriesId = rows.getLong(1);
        String instanceTableName = rows.getString(2);
        String linkTableName = rows.getString(3);

        if (instanceTableName != null && linkTableName != null)
          result.add(new SeriesTableNames(seriesId, instanceTableName, linkTableName));
      }

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param tableName
   * @return
   * @throws SQLException
   */
  private Map<Long, List<Long>> getConfigInstanceMappings(Connection dbConn, String tableName) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incainstance_id, incaseriesconfig_id " +
        "FROM " + tableName
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      Map<Long, List<Long>> result = new TreeMap<Long, List<Long>>();

      while (rows.next()) {
        long instanceId = rows.getLong(1);
        long configId = rows.getLong(2);
        List<Long> configIds = result.get(instanceId);

        if (configIds == null) {
          configIds = new ArrayList<Long>();

          result.put(instanceId, configIds);
        }

        configIds.add(configId);
      }

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @param names
   * @throws SQLException
   */
  private void writeSeriesInstances(Connection dbConn, PrintStream output, SeriesTableNames names) throws SQLException
  {
    Map<Long, List<Long>> mappings = getConfigInstanceMappings(dbConn, names.linkTableName);
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incacollected, incacommited, incamemoryUsageMB, " +
          "incacpuUsageSec, incawallClockTimeSec, incalog, incareportId " +
        "FROM " + names.instanceTableName
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<instanceInfoRows>");

      while (rows.next()) {
        long instanceId = rows.getLong(1);

        output.print("<instanceInfo><id>");
        output.print(instanceId);
        output.print("</id><collected>");

        writeTimestampValue(rows, 2, output);

        output.print("</collected><commited>");

        writeTimestampValue(rows, 3, output);

        output.print("</commited><memoryUsageMB>");

        writeFloatValue(rows, 4, output);

        output.print("</memoryUsageMB><cpuUsageSec>");

        writeFloatValue(rows, 5, output);

        output.print("</cpuUsageSec><wallClockTimeSec>");

        writeFloatValue(rows, 6, output);

        output.print("</wallClockTimeSec><log>");

        writeCdataValue(rows, 7, output);

        output.print("</log><reportId>");

        writeLongValue(rows, 8, output);

        output.print("</reportId>");

        List<Long> configIds = mappings.get(instanceId);

        if (configIds != null) {
          output.print("<seriesConfigs>");

          for (Long id : configIds) {
            output.print("<id>");
            output.print(id);
            output.print("</id>");
          }

          output.print("</seriesConfigs>");
        }
        else
          output.print("<seriesConfigs/>");

        output.print("</instanceInfo>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</instanceInfoRows>");

      m_logger.debug("Wrote " + totalWritten + " InstanceInfo records for Series " + names.seriesId);
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeInstanceInfo(Connection dbConn, PrintStream output) throws SQLException
  {
    List<SeriesTableNames> tableNames = getTableNames(dbConn);

    output.print("<seriesInstanceRows>");

    for (SeriesTableNames names : tableNames) {
      if (!DatabaseTools.tableExists(dbConn, names.instanceTableName)) {
        m_logger.debug("Can't find instance tables for Series " + names.seriesId);

        continue;
      }

      output.print("<seriesInstances><id>");
      output.print(names.seriesId);
      output.print("</id>");

      writeSeriesInstances(dbConn, output, names);

      output.print("</seriesInstances>");
    }

    output.print("</seriesInstanceRows>");

    m_logger.debug("Finished writing InstanceInfo records");
  }

  /**
   *
   * @param dbConn
   * @param output
   * @throws SQLException
   */
  private void writeKbArticles(Connection dbConn, PrintStream output) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaentered, incaerrormsg, incaseries, incareporter, " +
          "incaauthorname, incaauthoremail, incaarticletitle, incaarticletext " +
        "FROM incakbarticle"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      int totalWritten = 0;

      output.print("<kbArticleRows>");

      while (rows.next()) {
        output.print("<kbArticle><entered>");

        writeTimestampValue(rows, 1, output);

        output.print("</entered><errorMsg>");

        writeStringValue(rows, 2, output);

        output.print("</errorMsg><series>");

        writeStringValue(rows, 3, output);

        output.print("</series><reporter>");

        writeStringValue(rows, 4, output);

        output.print("</reporter><authorName>");

        writeStringValue(rows, 5, output);

        output.print("</authorName><authorEmail>");

        writeStringValue(rows, 6, output);

        output.print("</authorEmail><articleTitle>");

        writeStringValue(rows, 7, output);

        output.print("</articleTitle><articleText>");

        writeCdataValue(rows, 8, output);

        output.print("</articleText></kbArticle>");

        if (m_logger.isDebugEnabled())
          totalWritten += 1;
      }

      output.print("</kbArticleRows>");

      m_logger.debug("Wrote " + totalWritten + " KbArticle records");
    }
    finally {
      if (rows != null)
        rows.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param outStream
   * @throws IOException
   * @throws SQLException
   */
  private void writeResponse(OutputStream outStream) throws IOException, SQLException
  {
    PrintStream output = new PrintStream(new BufferedOutputStream(new GZIPOutputStream(new Base64OutputStream(new ClonedOutputStream(outStream), true, 0, CRLF))));
    Connection dbConn = ConnectionSource.getConnection();

    try {
      output.print("<syncResponse>");

      m_logger.debug("Writing Suite records...");

      writeSuites(dbConn, output);

      m_logger.debug("Writing Arg records...");

      writeArgs(dbConn, output);

      m_logger.debug("Writing ArgSignature records...");

      writeArgSignatures(dbConn, output);

      m_logger.debug("Writing Series records...");

      writeSeries(dbConn, output);

      m_logger.debug("Writing SeriesConfig records...");

      writeSeriesConfigs(dbConn, output);

      m_logger.debug("Writing RunInfo records...");

      writeRunInfo(dbConn, output);

      m_logger.debug("Writing Report records...");

      writeReports(dbConn, output);

      m_logger.debug("Writing ComparisonResult records...");

      writeComparisonResults(dbConn, output);

      m_logger.debug("Writing InstanceInfo records...");

      writeInstanceInfo(dbConn, output);

      m_logger.debug("Writing KbArticle records...");

      writeKbArticles(dbConn, output);

      output.print("</syncResponse>");
    }
    finally {
      output.close();
      dbConn.close();
    }
  }
}
