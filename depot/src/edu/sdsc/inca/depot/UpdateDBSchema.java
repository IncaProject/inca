/*
 * UpdateDBSchema.java
 */
package edu.sdsc.inca.depot;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.sdsc.inca.depot.persistent.ConnectionSource;
import edu.sdsc.inca.depot.persistent.DatabaseTools;
import edu.sdsc.inca.depot.persistent.SeriesDAO;


/**
 *
 * @author Paul Hoover
 *
 */
public class UpdateDBSchema {

  /**
   *
   */
  private static class ConfigRecord implements Comparable<ConfigRecord> {

    public final long configId;
    public final int activated;
    public final int deactivated;
    public final long latestId;
    public final boolean isActive;
    public final String nickname;
    public Timestamp activatedDate;
    public Timestamp deactivatedDate;


    /**
     *
     * @param id
     * @param act
     * @param deact;
     * @param latest
     * @param nick
     */
    public ConfigRecord(long id, int act, int deact, long latest, String nick)
    {
      configId = id;
      activated = act;
      deactivated = deact;
      latestId = latest;
      isActive = act >= deact;
      nickname = nick;
    }


    /**
     *
     * @param other
     * @return
     */
    @Override
    public boolean equals(Object other)
    {
      if (other == null)
        return false;

      if (this == other)
        return true;

      if (other instanceof ConfigRecord == false)
        return false;

      ConfigRecord otherConfig = (ConfigRecord) other;

      return configId == otherConfig.configId;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode()
    {
      return (new Long(configId)).hashCode();
    }

    /**
     *
     * @param other
     * @return
     */
    public int compareTo(ConfigRecord other)
    {
      if (other == null)
        throw new NullPointerException("other");

      if (this == other)
        return 0;

      return (int) (configId - other.configId);
    }
  }


  /**
   *
   */
  private static class SeriesLinkingRecord {

    public final long seriesId;
    public final String instanceTableName;
    public final String linkTableName;
    public final List<ConfigLinkingRecord> seriesConfigs = new ArrayList<ConfigLinkingRecord>();


    /**
     *
     * @param id
     * @param instance
     * @param link
     */
    public SeriesLinkingRecord(long id, String instance, String link)
    {
      seriesId = id;
      instanceTableName = instance;
      linkTableName = link;
    }
  }


  /**
   *
   */
  private static class ConfigLinkingRecord {

    public final long configId;
    public Timestamp activated;
    public Timestamp deactivated;


    /**
     *
     * @param id
     * @param act
     * @param deact
     */
    public ConfigLinkingRecord(long id, Timestamp act, Timestamp deact)
    {
      configId = id;
      activated = act;
      deactivated = deact;
    }
  }


  /**
   *
   */
  private static class ConfigInstanceLink {

    public final long configId;
    public final long instanceId;

    /**
     *
     * @param config
     * @param instance
     */
    public ConfigInstanceLink(long config, long instance)
    {
      configId = config;
      instanceId = instance;
    }
  }

  /**
   *
   */
  private static class TableNameData {

    public final long seriesId;
    public final String instanceTableName;
    public final String linkTableName;
    public final List<Long> seriesConfigIds = new ArrayList<Long>();

    /**
     *
     * @param id
     * @param instance
     * @param link
     */
    public TableNameData(long id, String instance, String link)
    {
      seriesId = id;
      instanceTableName = instance;
      linkTableName = link;
    }
  }


  private static final int FETCH_SIZE = 100;
  private static final int BATCH_SIZE = 100;
  private static final Logger m_logger = Logger.getLogger(UpdateDBSchema.class);


  // public methods


  /**
   *
   * @return
   * @throws SQLException
   */
  public static boolean update() throws SQLException
  {
    Connection dbConn = ConnectionSource.getConnection();
    boolean updated = false;

    try {
      dbConn.setAutoCommit(false);

      if (!DatabaseTools.tableExists(dbConn, "incakbarticle")) {
        m_logger.info("Creating KbArticle table...");

        createArticleTable(dbConn);

        updated = true;
      }

      if (!DatabaseTools.tableExists(dbConn, "incasuitesseriesconfigs")) {
        m_logger.info("Creating Suite/SeriesConfig linking table...");

        createSuiteConfigLinkingTable(dbConn);

        m_logger.debug("Populating linking table...");

        populateSuiteConfigLinkingTable(dbConn);

        m_logger.debug("Dropping SuiteId column...");

        dropSuiteIdColumn(dbConn);

        updated = true;
      }

      if (!DatabaseTools.columnIsType(dbConn, "incaseriesconfig", "incaactivated", DatabaseTools.getDateTypeName())) {
        m_logger.info("Altering SeriesConfig table activated/deactivated columns...");

        m_logger.debug("Fetching SeriesConfig records...");

        Map<Long, Set<ConfigRecord>> configRecords = getConfigRecords(dbConn);

        m_logger.debug("Creating new activated/deactivated columns...");

        alterActivatedColumns(dbConn);

        m_logger.debug("Populating new activated/deactivated columns...");

        populateActivatedColumns(dbConn, configRecords);

        updated = true;
      }

      if (!DatabaseTools.columnExists(dbConn, "incaseries", "incainstancetablename")) {
        m_logger.info("Separating Instance table...");

        m_logger.debug("Adding table name columns to Series table...");

        addTableNameColumns(dbConn);

        m_logger.debug("Populating new table name columns...");

        populateTableNameColumns(dbConn);

        m_logger.debug("Building a list of table names...");

        if (DatabaseTools.tableExists(dbConn, "incaseriesconfigsinstances")) {
          m_logger.debug("Populating new Instance tables...");

          List<TableNameData> tableNames = getTableNames(dbConn);

          for (TableNameData data : tableNames)
            separateInstances(dbConn, data);

          dropTable(dbConn, "incaseriesconfigsinstances");
        }
        else {
          m_logger.debug("Fetching Series records...");

          List<SeriesLinkingRecord> seriesRecords = getSeriesLinkingRecords();

          m_logger.debug("Populating new Instance tables...");

          populateSeriesInstanceTables(dbConn, seriesRecords);
        }

        dropTable(dbConn, "incainstanceinfo");

        updated = true;
      }

      if (!DatabaseTools.columnExists(dbConn, "incaseries", "incatargethostname")) {
        m_logger.info("Adding target hostname column to Series table...");

        addTargetColumn(dbConn);

        updated = true;
      }
    }
    finally {
      dbConn.close();
    }

    return updated;
  }


  // private methods


  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  private static void createArticleTable(Connection dbConn) throws SQLException
  {
    String keyTypeName = DatabaseTools.getKeyTypeName();
    String dateTypeName = DatabaseTools.getDateTypeName();
    String stringTypeName = DatabaseTools.getStringTypeName();
    String textTypeName = DatabaseTools.getTextTypeName();
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("CREATE TABLE incakbarticle ( incaid ");
    queryBuilder.append(keyTypeName);
    queryBuilder.append(" NOT NULL, incaentered ");
    queryBuilder.append(dateTypeName);
    queryBuilder.append(" NOT NULL, incaerrormsg ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(4000), incaseries ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255) NOT NULL, incareporter ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255) NOT NULL, incaauthorname ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255) NOT NULL, incaauthoremail ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255) NOT NULL, incaarticletitle ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(2000) NOT NULL, incaarticletext ");
    queryBuilder.append(textTypeName);
    queryBuilder.append(" NOT NULL, PRIMARY KEY (incaid) )");

    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(queryBuilder.toString());

      dbConn.commit();
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  private static void createSuiteConfigLinkingTable(Connection dbConn) throws SQLException
  {
    String longTypeName = DatabaseTools.getLongTypeName();
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("CREATE TABLE incasuitesseriesconfigs ( incasuite_id ");
    queryBuilder.append(longTypeName);
    queryBuilder.append(" NOT NULL, incaseriesconfig_id ");
    queryBuilder.append(longTypeName);
    queryBuilder.append(" NOT NULL, PRIMARY KEY (incasuite_id, incaseriesconfig_id), " +
        "FOREIGN KEY (incasuite_id) REFERENCES incasuite (incaid), " +
        "FOREIGN KEY (incaseriesconfig_id) REFERENCES incaseriesconfig (incaid) )");

    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(queryBuilder.toString());

      dbConn.commit();
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  private static void populateSuiteConfigLinkingTable(Connection dbConn) throws SQLException
  {
    Map<Long, List<Long>> suiteConfigIds = getSuiteConfigLinkingIds(dbConn);

    if (m_logger.isDebugEnabled()) {
      int numConfigs = 0;

      for (List<Long> configIds : suiteConfigIds.values())
        numConfigs += configIds.size();

      m_logger.debug("Linking " + suiteConfigIds.size() + " Suites with " + numConfigs + " SeriesConfigs...");
    }

    BatchUpdateStatement insertStmt = new BatchUpdateStatement(dbConn,
        "INSERT INTO incasuitesseriesconfigs ( incasuite_id, incaseriesconfig_id ) " +
        "VALUES ( ?, ? )"
    );

    try {
      for (Map.Entry<Long, List<Long>> entry : suiteConfigIds.entrySet()) {
        Long suiteId = entry.getKey();
        List<Long> configIds = entry.getValue();

        m_logger.debug("Linking Suite " + suiteId + " with " + configIds.size() + " SeriesConfigs...");

        for (Long seriesConfigId : configIds) {
          insertStmt.setLong(1, suiteId);
          insertStmt.setLong(2, seriesConfigId);
          insertStmt.update();
        }
      }
    }
    finally {
      insertStmt.close();
    }

    m_logger.debug("Finished linking Suites with SeriesConfigs");
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private static Map<Long, List<Long>> getSuiteConfigLinkingIds(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incasuite_id " +
        "FROM incaseriesconfig"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      Map<Long, List<Long>> result = new TreeMap<Long, List<Long>>();

      while (rows.next()) {
        long seriesConfigId = rows.getLong(1);
        long suiteId = rows.getLong(2);
        List<Long> configIds = result.get(suiteId);

        if (configIds == null) {
          configIds = new ArrayList<Long>();

          result.put(suiteId, configIds);
        }

        configIds.add(seriesConfigId);
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
   * @param dbData
   * @throws SQLException
   */
  private static void dropSuiteIdColumn(Connection dbConn) throws SQLException
  {
    String keyName = getForeignKeyName(dbConn);
    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(
          "ALTER TABLE incaseriesconfig " +
          "DROP CONSTRAINT " + keyName
      );

      updateStmt.executeUpdate(
          "ALTER TABLE incaseriesconfig " +
          "DROP COLUMN incasuite_id"
      );

      dbConn.commit();
    }
    catch (SQLException sqlErr) {
      dbConn.rollback();

      throw sqlErr;
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbData
   * @return
   * @throws SQLException
   */
  private static String getForeignKeyName(Connection dbConn) throws SQLException
  {
    DatabaseMetaData dbData = dbConn.getMetaData();
    boolean upperCase = dbData.storesUpperCaseIdentifiers();
    final String configTableName = "incaseriesconfig";
    final String suiteIdColumnName = "incasuite_id";
    String tableName = upperCase ? configTableName.toUpperCase() : configTableName;
    String columnName = upperCase ? suiteIdColumnName.toUpperCase() : suiteIdColumnName;
    ResultSet rows = dbData.getImportedKeys(null, null, tableName);

    try {
      while (rows.next()) {
        if (rows.getString(8).equals(columnName))
          return rows.getString(12);
      }

      return null;
    }
    finally {
      rows.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private static Map<Long, Set<ConfigRecord>> getConfigRecords(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaseries_id, incaseriesconfig.incaid, incaactivated, " +
          "incadeactivated, incalatestinstanceid, incanickname " +
        "FROM incaseriesconfig " +
          "INNER JOIN incaseries ON incaseriesconfig.incaseries_id = incaseries.incaid"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      Map<Long, Set<ConfigRecord>> result = new TreeMap<Long, Set<ConfigRecord>>();

      while (rows.next()) {
        long seriesId = rows.getLong(1);
        long configId = rows.getLong(2);
        int activated = rows.getInt(3);
        int deactivated = rows.getInt(4);
        long latestId = rows.getLong(5);
        String nickname = rows.getString(6);
        ConfigRecord newRecord = new ConfigRecord(configId, activated, deactivated, latestId, nickname);
        Set<ConfigRecord> configs = result.get(seriesId);

        if (configs == null) {
          configs = new TreeSet<ConfigRecord>();

          result.put(seriesId, configs);
        }

        configs.add(newRecord);
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
   * @throws SQLException
   */
  private static void alterActivatedColumns(Connection dbConn) throws SQLException
  {
    String dateTypeName = DatabaseTools.getDateTypeName();
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("ALTER TABLE incaseriesconfig ");
    queryBuilder.append("ADD COLUMN incaactivated ");
    queryBuilder.append(dateTypeName);
    queryBuilder.append(", ADD COLUMN incadeactivated ");
    queryBuilder.append(dateTypeName);

    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(
          "ALTER TABLE incaseriesconfig " +
          "DROP COLUMN incaactivated, " +
          "DROP COLUMN incadeactivated"
      );

      updateStmt.executeUpdate(queryBuilder.toString());

      dbConn.commit();
    }
    catch (SQLException sqlErr) {
      dbConn.rollback();

      throw sqlErr;
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param configRecords
   * @throws SQLException
   */
  private static void populateActivatedColumns(Connection dbConn, Map<Long, Set<ConfigRecord>> configRecords) throws SQLException
  {
    if (m_logger.isDebugEnabled()) {
      int numConfigs = 0;

      for (Set<ConfigRecord> configs : configRecords.values())
        numConfigs += configs.size();

      m_logger.debug("Updating columns for " + numConfigs + " SeriesConfigs...");
    }

    int totalUpdates = 0;
    List<ConfigRecord> updates = new ArrayList<ConfigRecord>();

    for (Map.Entry<Long, Set<ConfigRecord>> entry : configRecords.entrySet()) {
      long seriesId = entry.getKey();
      Set<ConfigRecord> configs = entry.getValue();
      Map<Long, Map<Long, Timestamp>> reportDates = getReportDates(dbConn, seriesId);
      Map<String, Map<Integer, Timestamp>> deactivationDates = new TreeMap<String, Map<Integer, Timestamp>>();

      for (ConfigRecord record : configs) {
        if (!record.isActive && record.latestId >= 0) {
          for (Map<Long, Timestamp> instances : reportDates.values()) {
            Timestamp collected = instances.get(record.latestId);

            if (collected != null) {
              Calendar adjusted = Calendar.getInstance();

              adjusted.setTime(collected);
              adjusted.add(Calendar.SECOND, 1);

              record.deactivatedDate = new Timestamp(adjusted.getTimeInMillis());

              Map<Integer, Timestamp> versions = deactivationDates.get(record.nickname);

              if (versions == null) {
                versions = new TreeMap<Integer, Timestamp>();

                deactivationDates.put(record.nickname, versions);
              }

              versions.put(record.deactivated, record.deactivatedDate);

              break;
            }
          }
        }
      }

      for (ConfigRecord record : configs) {
        Map<Integer, Timestamp> versions = deactivationDates.get(record.nickname);
        List<Long> reportIds = null;

        if (versions != null)
          record.activatedDate = versions.get(record.activated);

        if (record.activatedDate == null) {
          reportIds = getReportIds(dbConn, record.configId, seriesId);

          for (Long reportId : reportIds) {
            Map<Long, Timestamp> instances = reportDates.get(reportId);

            if (instances != null) {
              for (Timestamp collected : instances.values()) {
                if (record.activatedDate == null || record.activatedDate.after(collected))
                  record.activatedDate = collected;
              }
            }
          }

          if (record.activatedDate == null)
            record.activatedDate = new Timestamp(1);
          else {
            Calendar adjusted = Calendar.getInstance();

            adjusted.setTime(record.activatedDate);
            adjusted.add(Calendar.SECOND, -1);

            record.activatedDate.setTime(adjusted.getTimeInMillis());
          }
        }

        if (!record.isActive && record.deactivatedDate == null) {
          if (reportIds == null)
            reportIds = getReportIds(dbConn, record.configId, seriesId);

          for (Long reportId : reportIds) {
            Map<Long, Timestamp> instances = reportDates.get(reportId);

            if (instances != null) {
              for (Timestamp collected : instances.values()) {
                if (record.deactivatedDate == null || record.deactivatedDate.before(collected))
                  record.deactivatedDate = collected;
              }
            }
          }

          if (record.deactivatedDate == null)
            record.deactivatedDate = record.activatedDate;

          Calendar cal = Calendar.getInstance();

          cal.setTime(record.deactivatedDate);
          cal.add(Calendar.SECOND, 1);

          record.deactivatedDate = new Timestamp(cal.getTimeInMillis());
        }

        updates.add(record);

        if (updates.size() >= BATCH_SIZE) {
          updateConfigColumns(dbConn, updates);

          updates.clear();

          if (m_logger.isDebugEnabled()) {
            totalUpdates += BATCH_SIZE;

            m_logger.debug("Updated " + totalUpdates + " rows so far...");
          }
        }
      }
    }

    if (updates.size() > 0)
      updateConfigColumns(dbConn, updates);

    m_logger.debug("Finished updating columns for SeriesConfigs");
  }

  /**
   *
   * @param reportId
   * @return
   * @throws SQLException
   */
  private static Map<Long, Timestamp> getInstanceDates(long reportId) throws SQLException
  {
    Connection dbConn = ConnectionSource.getConnection();
    PreparedStatement selectStmt = null;
    ResultSet rows = null;

    try {
      selectStmt = dbConn.prepareStatement(
          "SELECT incaid, incacollected " +
          "FROM incainstanceinfo " +
          "WHERE incareportid = ?"
      );

      selectStmt.setFetchSize(FETCH_SIZE);
      selectStmt.setLong(1, reportId);

      rows = selectStmt.executeQuery();

      Map<Long, Timestamp> result = new TreeMap<Long, Timestamp>();

      while (rows.next()) {
        long instanceId = rows.getLong(1);
        Timestamp collected = rows.getTimestamp(2);

        result.put(instanceId, collected);
      }

      return result;
    }
    finally {
      if (rows != null)
        rows.close();

      if (selectStmt != null)
        selectStmt.close();

      dbConn.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @return
   * @throws SQLException
   */
  private static Map<Long, Map<Long, Timestamp>> getReportDates(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid " +
        "FROM incareport " +
        "WHERE incaseries_id = ?"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);
      selectStmt.setLong(1, seriesId);

      rows = selectStmt.executeQuery();

      Map<Long, Map<Long, Timestamp>> result = new TreeMap<Long, Map<Long, Timestamp>>();

      while (rows.next()) {
        long reportId = rows.getLong(1);
        Map<Long, Timestamp> instances = getInstanceDates(reportId);

        result.put(reportId, instances);
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
   * @param configId
   * @return
   * @throws SQLException
   */
  private static List<Long> getReportIdsFromComparisons(Connection dbConn, long configId) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT DISTINCT incareportid " +
        "FROM incacomparisonresult " +
        "WHERE incaseriesconfigid = ?"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);
      selectStmt.setLong(1, configId);

      rows = selectStmt.executeQuery();

      if (!rows.next())
        return null;

      List<Long> result = new ArrayList<Long>();

      do {
        result.add(rows.getLong(1));
      }
      while (rows.next());

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
   * @param seriesId
   * @return
   * @throws SQLException
   */
  private static List<Long> getReportIdsFromReports(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid " +
        "FROM incareport " +
        "WHERE incaseries_id = ?"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);
      selectStmt.setLong(1, seriesId);

      rows = selectStmt.executeQuery();

      if (!rows.next())
        return null;

      List<Long> result = new ArrayList<Long>();

      do {
        result.add(rows.getLong(1));
      }
      while (rows.next());

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
   * @param configId
   * @param seriesId
   * @return
   * @throws SQLException
   */
  private static List<Long> getReportIds(Connection dbConn, long configId, long seriesId) throws SQLException
  {
    List<Long> reportIds = getReportIdsFromComparisons(dbConn, configId);

    if (reportIds == null)
      reportIds = getReportIdsFromReports(dbConn, seriesId);

    if (reportIds == null)
      return new ArrayList<Long>();

    return reportIds;
  }

  /**
   *
   * @param dbConn
   * @param records
   * @throws SQLException
   */
  private static void updateConfigColumns(Connection dbConn, List<ConfigRecord> records) throws SQLException
  {
    PreparedStatement updateStmt = dbConn.prepareStatement(
        "UPDATE incaseriesconfig " +
        "SET incaactivated = ?, incadeactivated = ? " +
        "WHERE incaid = ?"
    );

    try {
      for (ConfigRecord record : records) {
        updateStmt.setTimestamp(1, record.activatedDate);
        updateStmt.setTimestamp(2, record.deactivatedDate);
        updateStmt.setLong(3, record.configId);
        updateStmt.addBatch();
      }

      updateStmt.executeBatch();

      dbConn.commit();
    }
    catch (SQLException sqlErr) {
      dbConn.rollback();

      throw sqlErr;
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  private static void addTableNameColumns(Connection dbConn) throws SQLException
  {
    String stringTypeName = DatabaseTools.getStringTypeName();
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("ALTER TABLE incaseries ");
    queryBuilder.append("ADD COLUMN incainstancetablename ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255), ADD COLUMN incalinktablename ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255)");

    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(queryBuilder.toString());

      dbConn.commit();
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  private static  void populateTableNameColumns(Connection dbConn) throws SQLException
  {
    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(
          "UPDATE incaseries " +
          "SET incainstancetablename = 'incainstanceinfo_' || incaid, " +
            "incalinktablename = 'incaseriesconfigsinstances_' || incaid"
          );

      dbConn.commit();
    }
    finally {
      updateStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private static List<TableNameData> getTableNames(Connection dbConn) throws SQLException
  {
    Statement selectStmt = dbConn.createStatement();
    ResultSet rows = null;

    try {
      rows = selectStmt.executeQuery(
          "SELECT incaseries.incaid, incaseriesconfig.incaid, incainstancetablename, " +
            "incalinktablename " +
          "FROM incaseriesconfig " +
            "INNER JOIN incaseries ON incaseriesconfig.incaseries_id = incaseries.incaid"
      );

      Map<Long, TableNameData> tableNames = new TreeMap<Long, TableNameData>();

      while (rows.next()) {
        long seriesId = rows.getLong(1);
        long seriesConfigId = rows.getLong(2);
        TableNameData data = tableNames.get(seriesId);

        if (data == null) {
          String instanceTableName = rows.getString(3);
          String linkTableName = rows.getString(4);

          data = new TableNameData(seriesId, instanceTableName, linkTableName);

          tableNames.put(seriesId, data);
        }

        data.seriesConfigIds.add(seriesConfigId);
      }

      return new ArrayList<TableNameData>(tableNames.values());
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
   * @param data
   * @throws SQLException
   */
  private static void separateInstances(Connection dbConn, TableNameData data) throws SQLException
  {
    if (data.seriesConfigIds.isEmpty())
      return;

    StringBuilder listBuilder = new StringBuilder();
    Iterator<Long> ids = data.seriesConfigIds.iterator();

    listBuilder.append(ids.next());

    while (ids.hasNext()) {
      listBuilder.append(", ");
      listBuilder.append(ids.next());
    }

    String idList = listBuilder.toString();

    SeriesDAO.createInstanceTables(dbConn, data.instanceTableName, data.linkTableName);

    int numInstances = copyInstances(dbConn, idList, data.instanceTableName);

    if (numInstances < 1) {
      dropTable(dbConn, data.instanceTableName);
      dropTable(dbConn, data.linkTableName);
      deleteSeries(dbConn, data.seriesId);

      m_logger.debug("Deleted empty series " + data.seriesId);
    }
    else {
      int numLinks = copyLinks(dbConn, idList, data.linkTableName);

      m_logger.debug("Copied " + numInstances + " Instance records, " + numLinks + " SeriesConfig/Instance links for Series " + data.seriesId);
    }
  }

  /**
   *
   * @param dbConn
   * @param idList
   * @param tableName
   * @throws SQLException
   */
  private static int copyInstances(Connection dbConn, String idList, String tableName) throws SQLException
  {
    Statement selectStmt = dbConn.createStatement();

    try {
      int result = selectStmt.executeUpdate(
          "INSERT INTO " + tableName +
            " ( incaid, incacollected, incacommited, incamemoryusagemb, " +
            "incacpuusagesec, incawallclocktimesec, incalog, incareportid ) " +
          "SELECT incaid, incacollected, incacommited, incamemoryusagemb, " +
            "incacpuusagesec, incawallclocktimesec, incalog, incareportid " +
          "FROM incainstanceinfo " +
          "WHERE incaid IN ( " +
            "SELECT DISTINCT incainstance_id " +
            "FROM incaseriesconfigsinstances " +
            "WHERE incaseriesconfig_id IN ( " + idList + " ) " +
          ")"
      );

      dbConn.commit();

      return result;
    }
    finally {
      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param tableName
   * @throws SQLException
   */
  private static void dropTable(Connection dbConn, String tableName) throws SQLException
  {
    Statement dropStmt = dbConn.createStatement();

    try {
      dropStmt.executeUpdate("DROP TABLE " + tableName + " CASCADE");

      dbConn.commit();
    }
    finally {
      dropStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @throws SQLException
   */
  private static void deleteSeries(Connection dbConn, long seriesId) throws SQLException
  {
    deleteSuiteLinks(dbConn, seriesId);
    deleteComparisons(dbConn, seriesId);
    deleteConfigs(dbConn, seriesId);
    deleteReports(dbConn, seriesId);

    PreparedStatement deleteStmt = dbConn.prepareStatement(
        "DELETE FROM incaseries " +
        "WHERE incaid = ? "
    );

    try {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @throws SQLException
   */
  private static void deleteSuiteLinks(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement deleteStmt = dbConn.prepareStatement(
        "DELETE FROM incasuitesseriesconfigs " +
        "WHERE incaseriesconfig_id IN ( " +
          "SELECT incaid " +
          "FROM incaseriesconfig " +
          "WHERE incaseries_id = ? " +
        ")"
    );

    try {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @throws SQLException
   */
  private static void deleteComparisons(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement deleteStmt = dbConn.prepareStatement(
        "DELETE FROM incacomparisonresult " +
        "WHERE incaseriesconfigid IN ( " +
          "SELECT incaid " +
          "FROM incaseriesconfig " +
          "WHERE incaseries_id = ? " +
        ")"
    );

    try {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @throws SQLException
   */
  private static void deleteConfigs(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement deleteStmt = dbConn.prepareStatement(
        "DELETE FROM incaseriesconfig " +
        "WHERE incaseries_id = ? "
    );

    try {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @throws SQLException
   */
  private static void deleteReports(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement deleteStmt = dbConn.prepareStatement(
        "DELETE FROM incareport " +
        "WHERE incaseries_id = ? "
    );

    try {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param idList
   * @param tableName
   * @throws SQLException
   */
  private static int copyLinks(Connection dbConn, String idList, String tableName) throws SQLException
  {
    Statement selectStmt = dbConn.createStatement();

    try {
      int result = selectStmt.executeUpdate(
          "INSERT INTO " + tableName +
            " ( incainstance_id, incaseriesconfig_id ) " +
          "SELECT incainstance_id, incaseriesconfig_id " +
          "FROM incaseriesconfigsinstances, incainstanceinfo " +
          "WHERE incaseriesconfig_id IN ( " + idList + " ) and " + 
          "incaseriesconfigsinstances.incainstance_id = incainstanceinfo.incaid"
      );

      dbConn.commit();

      return result;
    }
    finally {
      selectStmt.close();
    }
  }

  /**
   *
   * @return
   * @throws SQLException
   */
  private static List<SeriesLinkingRecord> getSeriesLinkingRecords() throws SQLException
  {
    Connection dbConn = ConnectionSource.getConnection();
    Statement selectStmt = null;
    ResultSet rows = null;

    try {
      selectStmt = dbConn.createStatement();

      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery(
          "SELECT incaseries.incaid, incaseriesconfig.incaid, incaactivated, incadeactivated, " +
            "incainstancetablename, incalinktablename " +
          "FROM incaseriesconfig " +
            "INNER JOIN incaseries ON incaseriesconfig.incaseries_id = incaseries.incaid"
      );

      Map<Long, SeriesLinkingRecord> records = new TreeMap<Long, SeriesLinkingRecord>();

      while (rows.next()) {
        long seriesId = rows.getLong(1);
        long configId = rows.getLong(2);
        Timestamp activated = rows.getTimestamp(3);
        Timestamp deactivated = rows.getTimestamp(4);
        String instanceTableName = rows.getString(5);
        String linkTableName = rows.getString(6);
        ConfigLinkingRecord configRecord = new ConfigLinkingRecord(configId, activated, deactivated);
        SeriesLinkingRecord seriesRecord = records.get(seriesId);

        if (seriesRecord == null) {
          seriesRecord = new SeriesLinkingRecord(seriesId, instanceTableName, linkTableName);

          records.put(seriesId, seriesRecord);
        }

        seriesRecord.seriesConfigs.add(configRecord);
      }

      return new ArrayList<SeriesLinkingRecord>(records.values());
    }
    finally {
      if (rows != null)
        rows.close();

      if (selectStmt != null)
        selectStmt.close();

      dbConn.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesRecords
   * @throws SQLException
   */
  private static void populateSeriesInstanceTables(Connection dbConn, List<SeriesLinkingRecord> seriesRecords) throws SQLException
  {
    if (m_logger.isDebugEnabled()) {
      int numConfigs = 0;

      for (SeriesLinkingRecord record : seriesRecords)
        numConfigs += record.seriesConfigs.size();

      m_logger.debug("Populating Instance tables for " + seriesRecords.size() + " Series, " + numConfigs + " SeriesConfigs");
    }

    for (SeriesLinkingRecord series : seriesRecords) {
      int numLinks = 0;
      List<ConfigInstanceLink> updates = new ArrayList<ConfigInstanceLink>();
      Map<Long, Map<Long, Timestamp>> reportDates = getReportDates(dbConn, series.seriesId);

      for (ConfigLinkingRecord config : series.seriesConfigs) {
        List<Long> reportIds = getReportIds(dbConn, config.configId, series.seriesId);

        for (Long reportId : reportIds) {
          Map<Long, Timestamp> instances = reportDates.get(reportId);

          if (instances != null) {
            for (Map.Entry<Long, Timestamp> instanceEntry : instances.entrySet()) {
              long instanceId = instanceEntry.getKey();
              Timestamp collected = instanceEntry.getValue();

              if (config.activated.before(collected) && (config.deactivated == null || config.deactivated.after(collected))) {
                if (numLinks == 0) {
                  m_logger.debug("Populating Instance tables for Series " + series.seriesId + "...\nInserting SeriesConfig/Instance links...");

                  SeriesDAO.createInstanceTables(dbConn, series.linkTableName, series.instanceTableName);
                }

                updates.add(new ConfigInstanceLink(config.configId, instanceId));

                if (updates.size() >= BATCH_SIZE) {
                  insertLinkingRows(dbConn, series.linkTableName, updates);

                  numLinks += updates.size();

                  if (m_logger.isDebugEnabled() && numLinks % 1000 == 0)
                    m_logger.debug("Inserted " + numLinks + " rows so far...");

                  updates.clear();
                }
              }
            }
          }
        }
      }

      if (updates.size() > 0) {
        insertLinkingRows(dbConn, series.linkTableName, updates);

        numLinks += updates.size();
      }

      if (numLinks > 0) {
        m_logger.debug("Copying Instance records...");

        int numInstances = copySeriesInstances(dbConn, series.instanceTableName, series.linkTableName);

        m_logger.debug("Copied " + numInstances + " Instance records, " + numLinks + " SeriesConfig/Instance links for Series " + series.seriesId);
      }
      else {
        deleteSeries(dbConn, series.seriesId);

        m_logger.debug("Deleted empty series " + series.seriesId);
      }
    }

    m_logger.debug("Finished populating Instance tables");
  }

  /**
   *
   * @param dbConn
   * @param tableName
   * @param links
   * @throws SQLException
   */
  private static void insertLinkingRows(Connection dbConn, String tableName, List<ConfigInstanceLink> links) throws SQLException
  {
    PreparedStatement insertStmt = dbConn.prepareStatement(
        "INSERT INTO " + tableName + " ( incaseriesconfig_id, incainstance_id ) " +
        "VALUES ( ?, ? )"
    );

    try {
      for (ConfigInstanceLink link : links) {
        insertStmt.setLong(1, link.configId);
        insertStmt.setLong(2, link.instanceId);
        insertStmt.addBatch();
      }

      insertStmt.executeBatch();

      dbConn.commit();
    }
    catch (SQLException sqlErr) {
      dbConn.rollback();

      throw sqlErr;
    }
    finally {
      insertStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceTableName
   * @param linkTableName
   * @return
   * @throws SQLException
   */
  private static int copySeriesInstances(Connection dbConn, String instanceTableName, String linkTableName) throws SQLException
  {
    Statement selectStmt = dbConn.createStatement();

    try {
      int result = selectStmt.executeUpdate(
          "INSERT INTO " + instanceTableName +
            " ( incaid, incacollected, incacommited, incamemoryusagemb, " +
            "incacpuusagesec, incawallclocktimesec, incalog, incareportid ) " +
          "SELECT incaid, incacollected, incacommited, incamemoryusagemb, " +
            "incacpuusagesec, incawallclocktimesec, incalog, incareportid " +
          "FROM incainstanceinfo " +
          "WHERE incaid IN ( " +
            "SELECT DISTINCT incainstance_id " +
            "FROM " + linkTableName +
          " )"
      );

      dbConn.commit();

      return result;
    }
    finally {
      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @throws SQLException
   */
  private static void addTargetColumn(Connection dbConn) throws SQLException
  {
    String stringTypeName = DatabaseTools.getStringTypeName();
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("ALTER TABLE incaseries ");
    queryBuilder.append("ADD COLUMN incatargethostname ");
    queryBuilder.append(stringTypeName);
    queryBuilder.append("(255)");

    Statement updateStmt = dbConn.createStatement();

    try {
      updateStmt.executeUpdate(queryBuilder.toString());

      dbConn.commit();
    }
    finally {
      updateStmt.close();
    }
  }
}
