/*
 * DeleteSeriesInstances.java
 */
package edu.sdsc.inca.depot;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;


/**
 *
 * @author Paul Hoover
 *
 */
public class DeleteSeries {

  /**
   *
   */
  private static class SeriesRecord {

    public final long seriesId;
    public final String instanceTableName;
    public final String linkTableName;
    public final List<Long> configIds = new ArrayList<Long>();


    /**
     *
     * @param id
     * @param instance
     * @param link
     */
    public SeriesRecord(long id, String instance, String link)
    {
      seriesId = id;
      instanceTableName = instance;
      linkTableName = link;
    }
  }


  private static final String USAGE = "usage: DeleteSeriesInstances url username nickname resource target [ -s start ] [ -e end ]";


  // public methods


  /**
   *
   * @param args
   */
  public static void main(String[] args)
  {
    try {
      if (args.length != 5 && args.length != 7 && args.length != 9)
        throw new Exception(USAGE);

      String url = "jdbc:postgresql:";

      if (args[0].indexOf('/') >= 0)
        url += "//";

      url += args[0];

      String username = args[1];
      String nickname = args[2];
      String resource = args[3];
      String target = args[4];
      Date start = null;
      Date end = null;

      for (int i = 5 ; i < args.length ; i += 1) {
        if (args[i].equals("-s"))
          start = (new SimpleDateFormat("yyyy-MM-dd")).parse(args[++i]);
        else if (args[i].equals("-e"))
          end = (new SimpleDateFormat("yyyy-MM-dd")).parse(args[++i]);
        else
          throw new Exception(USAGE);
      }

      System.out.print("password>");

      String password = new BufferedReader(new InputStreamReader(System.in)).readLine();
      Properties connProps = new Properties();

      connProps.setProperty("user", username);

      if (password != null && password.length() > 0)
        connProps.setProperty("password", password);

      try (Connection dbConn = DriverManager.getConnection(url, connProps)) {
        dbConn.setAutoCommit(false);

        List<SeriesRecord> series = findSeries(dbConn, nickname, resource, target, start, end);

        for (SeriesRecord record : series) {
          for (long configId : record.configIds) {
            System.out.println("Deleting SeriesConfig " + configId + "...");

            deleteInstanceLinks(dbConn, record.linkTableName, configId);
            deleteSuiteLinks(dbConn, configId);
            deleteComparisons(dbConn, configId);
            deleteConfig(dbConn, configId);
          }

          if (numConfigs(dbConn, record.seriesId) > 0) {
            System.out.println("Deleting affected Instances for Series " + record.seriesId + "...");

            deleteInstances(dbConn, record.instanceTableName, record.linkTableName);
          }
          else {
            System.out.println("Deleting Series " + record.seriesId + "...");

            deleteReports(dbConn, record.seriesId);
            deleteSeries(dbConn, record.seriesId);
            dropInstanceTables(dbConn, record.instanceTableName, record.linkTableName);
          }
        }
      }
    }
    catch (Exception err) {
      err.printStackTrace(System.err);

      System.exit(-1);
    }
  }


  // private methods


  /**
   *
   * @param dbConn
   * @param nickname
   * @param resource
   * @param target
   * @param start
   * @param end
   * @return
   * @throws SQLException
   */
  private static List<SeriesRecord> findSeries(Connection dbConn, String nickname, String resource, String target, Date start, Date end) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append(
      "SELECT incaseries.incaid, incaseriesconfig.incaid, incainstancetablename, incalinktablename " +
      "FROM incaseries " +
        "INNER JOIN incaseriesconfig ON incaseries.incaid = incaseriesconfig.incaseries_id " +
      "WHERE incanickname = ? " +
        "AND incaresource = ? " +
        "AND incatargethostname = ?"
    );

    if (start != null)
      queryBuilder.append(" AND incaactivated >= ?");

    if (end != null)
      queryBuilder.append(" AND incadeactivated <= ?");

    try (PreparedStatement selectStmt = dbConn.prepareStatement(queryBuilder.toString())) {
      selectStmt.setString(1, nickname);
      selectStmt.setString(2, resource);

      if (target == null || target.length() < 1)
        selectStmt.setString(3, "\t");
      else
        selectStmt.setString(3, target);

      if (start != null)
        selectStmt.setTimestamp(4, new Timestamp(start.getTime()));

      if (end != null) {
        int index = start == null ? 4 : 5;

        selectStmt.setTimestamp(index, new Timestamp(end.getTime()));
      }

      ResultSet rows = selectStmt.executeQuery();

      Map<Long, SeriesRecord> result = new TreeMap<Long, SeriesRecord>();

      while (rows.next()) {
        long seriesId = rows.getLong(1);
        long configId = rows.getLong(2);
        SeriesRecord record = result.get(seriesId);

        if (record == null) {
          String instanceTable = rows.getString(3);
          String linkTable = rows.getString(4);

          record = new SeriesRecord(seriesId, instanceTable, linkTable);

          result.put(seriesId, record);
        }

        record.configIds.add(configId);
      }

      return new ArrayList<SeriesRecord>(result.values());
    }
  }

  /**
   *
   * @param dbConn
   * @param linkTable
   * @param configId
   * @throws SQLException
   */
  private static void deleteInstanceLinks(Connection dbConn, String linkTable, long configId) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("DELETE FROM ");
    queryBuilder.append(linkTable);
    queryBuilder.append(" WHERE incaseriesconfig_id = ?");

    try (PreparedStatement deleteStmt = dbConn.prepareStatement(queryBuilder.toString())) {
      deleteStmt.setLong(1, configId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
  }

  /**
   *
   * @param dbConn
   * @param configId
   * @throws SQLException
   */
  private static void deleteSuiteLinks(Connection dbConn, long configId) throws SQLException
  {
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM incasuitesseriesconfigs " +
      "WHERE incaseriesconfig_id = ?"
    )) {
      deleteStmt.setLong(1, configId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
  }

  /**
   *
   * @param dbConn
   * @param configId
   * @throws SQLException
   */
  private static void deleteComparisons(Connection dbConn, long configId) throws SQLException
  {
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM incacomparisonresult " +
      "WHERE incaseriesconfigid = ?"
    )) {
      deleteStmt.setLong(1, configId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
  }

  /**
   *
   * @param dbConn
   * @param configId
   * @throws SQLException
   */
  private static void deleteConfig(Connection dbConn, long configId) throws SQLException
  {
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM incaseriesconfig " +
      "WHERE incaid = ?"
    )) {
      deleteStmt.setLong(1, configId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceTable
   * @param linkTable
   * @throws SQLException
   */
  private static void deleteInstances(Connection dbConn, String instanceTable, String linkTable) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("DELETE FROM ");
    queryBuilder.append(instanceTable);
    queryBuilder.append(" WHERE incaid NOT IN ( SELECT incainstance_id FROM ");
    queryBuilder.append(linkTable);
    queryBuilder.append(" )");

    try (Statement deleteStmt = dbConn.createStatement()) {
      deleteStmt.executeUpdate(queryBuilder.toString());

      dbConn.commit();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @return
   * @throws SQLException
   */
  private static int numConfigs(Connection dbConn, long seriesId) throws SQLException
  {
    try (PreparedStatement selectStmt = dbConn.prepareStatement(
      "SELECT COUNT(*) " +
      "FROM incaseriesconfig " +
      "WHERE incaseries_id = ?"
    )) {
      selectStmt.setLong(1, seriesId);

      ResultSet row = selectStmt.executeQuery();

      if (!row.next())
        return 0;

      return row.getInt(1);
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
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM incareport " +
      "WHERE incaseries_id = ?"
    )) {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
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
    try (PreparedStatement deleteStmt = dbConn.prepareStatement(
      "DELETE FROM incaseries " +
      "WHERE incaid = ?"
    )) {
      deleteStmt.setLong(1, seriesId);
      deleteStmt.executeUpdate();

      dbConn.commit();
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceTable
   * @param linkTable
   * @throws SQLException
   */
  private static void dropInstanceTables(Connection dbConn, String instanceTable, String linkTable) throws SQLException
  {
    try (Statement dropStmt = dbConn.createStatement()) {
      dropStmt.executeUpdate("DROP TABLE " + linkTable + " CASCADE");
      dropStmt.executeUpdate("DROP TABLE " + instanceTable + " CASCADE");

      dbConn.commit();
    }
  }
}
