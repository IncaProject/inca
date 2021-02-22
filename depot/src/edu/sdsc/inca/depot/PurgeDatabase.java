/*
 * PurgeDatabase.java
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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import edu.sdsc.inca.depot.persistent.ConnectionManager;


/**
 *
 * @author Paul Hoover
 *
 */
public class PurgeDatabase {

  // nested classes


  /**
   *
   */
  private static class SeriesRecord {

    public final long seriesId;
    public final String instanceTableName;
    public final String linkTableName;


    /**
     *
     * @param id
     * @param instance
     * @param link
     */
    private SeriesRecord(long id, String instance, String link)
    {
      seriesId = id;
      instanceTableName = instance;
      linkTableName = link;
    }
  }


  // data fields


  private static final int FETCH_SIZE = 100;
  private static final Logger m_logger = Logger.getLogger(PurgeDatabase.class);


  // public methods


  /**
   *
   * @param cutoff
   * @throws SQLException
   */
  public void purge(Date cutoff) throws SQLException
  {
    m_logger.info("purging database...");

    Connection dbConn = ConnectionManager.getConnectionSource().getConnection();

    try {
      dbConn.setAutoCommit(false);

      List<SeriesRecord> names = getSeriesRecords(dbConn);
      int numSeries = names.size();
      int currentSeries = 1;
      int totalInstancesDeleted = 0;
      int totalReportsDeleted = 0;

      for (SeriesRecord record : names) {
        m_logger.debug("examining Series " + record.seriesId + " (" + currentSeries + " of " + numSeries + ")... ");

        currentSeries += 1;

        if (!tableExists(dbConn, record.instanceTableName)) {
          m_logger.debug("instance table " + record.instanceTableName + " for Series " + record.seriesId + " doesn't exist");

          continue;
        }

        int maxReports = 35000;
        int numReports = getNumReports(dbConn, record.seriesId);
        Date instanceCutoff;

        if (numReports > maxReports) {
          Date reportCutoff = getReportCutoff(dbConn, record.instanceTableName, maxReports);

          if (reportCutoff.after(cutoff))
            instanceCutoff = reportCutoff;
          else
            instanceCutoff = cutoff;
        }
        else
          instanceCutoff = cutoff;

        deleteInstanceLinks(dbConn, record.instanceTableName, record.linkTableName, instanceCutoff);

        int numInstancesDeleted = deleteInstances(dbConn, record.instanceTableName, instanceCutoff);

        resetLatestIds(dbConn, record.seriesId, record.instanceTableName);
        int numReportsDeleted = deleteOrphanedReports(dbConn, record.seriesId, record.instanceTableName);

        totalInstancesDeleted += numInstancesDeleted;
        totalReportsDeleted += numReportsDeleted;

        m_logger.debug("deleted " + numInstancesDeleted + " InstanceInfo records and " + numReportsDeleted +  " Reports from Series " + record.seriesId);
      }

      m_logger.debug("cleaning up ComparisonResult table...");

      deleteOrphanedComparisons(dbConn);

      m_logger.debug("cleaning up RunInfo table...");

      deleteOrphanedRunInfo(dbConn);

      m_logger.info("finished purge, deleted " + totalInstancesDeleted + " InstanceInfo records and " + totalReportsDeleted +  " Reports");
    }
    finally {
      dbConn.close();
    }
  }


  // private methods


  /**
   *
   * @param dbData
   * @param value
   * @return
   * @throws SQLException
   */
  private String convertCase(DatabaseMetaData dbData, String value) throws SQLException
  {
    if (dbData.storesLowerCaseIdentifiers())
      return value.toLowerCase();
    else if (dbData.storesUpperCaseIdentifiers())
      return value.toUpperCase();
    else
      return value;
  }

  /**
   *
   * @param dbConn
   * @param table
   * @return
   * @throws SQLException
   */
  private boolean tableExists(Connection dbConn, String table) throws SQLException
  {
    DatabaseMetaData dbData = dbConn.getMetaData();
    String tableName = convertCase(dbData, table);
    ResultSet row = dbData.getTables(null, null, tableName, null);

    try {
      return row.next();
    }
    finally {
      row.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private List<SeriesRecord> getSeriesRecords(Connection dbConn) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement(
        "SELECT incaid, incainstancetablename, incalinktablename " +
        "FROM incaseries"
    );
    ResultSet rows = null;

    try {
      selectStmt.setFetchSize(FETCH_SIZE);

      rows = selectStmt.executeQuery();

      List<SeriesRecord> result = new ArrayList<SeriesRecord>();

      while (rows.next()) {
        long seriesId = rows.getLong(1);
        String instanceTable = rows.getString(2);
        String linkTable = rows.getString(3);

        result.add(new SeriesRecord(seriesId, instanceTable, linkTable));
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
   * @param seriesId
   * @return
   * @throws SQLException
   */
  private int getNumReports(Connection dbConn, long seriesId) throws SQLException
  {
    PreparedStatement selectStmt = dbConn.prepareStatement("SELECT COUNT(*) FROM incareport WHERE incaseries_id = ?");
    ResultSet row = null;

    try {
      selectStmt.setLong(1, seriesId);

      row = selectStmt.executeQuery();

      if (!row.next())
        return 0;

      return row.getInt(1);
    }
    finally {
      if (row != null)
        row.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param table
   * @param cutoff
   * @return
   * @throws SQLException
   */
  private Date getReportCutoff(Connection dbConn, String table, int cutoff) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("SELECT incareport.incaid, MAX(");
    queryBuilder.append(table);
    queryBuilder.append(".incacollected) AS latest FROM incareport INNER JOIN ");
    queryBuilder.append(table);
    queryBuilder.append(" ON incareport.incaid = ");
    queryBuilder.append(table);
    queryBuilder.append(".incareportid GROUP BY incareport.incaid ORDER BY latest DESC LIMIT 1 OFFSET ");
    queryBuilder.append(cutoff);

    Statement selectStmt = dbConn.createStatement();
    ResultSet row = null;

    try {
      row = selectStmt.executeQuery(queryBuilder.toString());

      if (!row.next())
        return null;

      Timestamp result = row.getTimestamp(2);

      return new Date(result.getTime());
    }
    finally {
      if (row != null)
        row.close();

      selectStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceTable
   * @param linkTable
   * @param cutoff
   * @return
   * @throws SQLException
   */
  private int deleteInstanceLinks(Connection dbConn, String instanceTable, String linkTable, Date cutoff) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("DELETE FROM ");
    queryBuilder.append(linkTable);
    queryBuilder.append(" WHERE incainstance_id IN ( SELECT incaid FROM ");
    queryBuilder.append(instanceTable);
    queryBuilder.append(" WHERE incacollected < ? )");

    PreparedStatement deleteStmt = dbConn.prepareStatement(queryBuilder.toString());

    try {
      deleteStmt.setTimestamp(1, new Timestamp(cutoff.getTime()));

      int numDeleted = deleteStmt.executeUpdate();

      dbConn.commit();

      return numDeleted;
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceTable
   * @param cutoff
   * @return
   * @throws SQLException
   */
  private int deleteInstances(Connection dbConn, String instanceTable, Date cutoff) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append("DELETE FROM ");
    queryBuilder.append(instanceTable);
    queryBuilder.append(" WHERE incacollected < ?");

    PreparedStatement deleteStmt = dbConn.prepareStatement(queryBuilder.toString());

    try {
      deleteStmt.setTimestamp(1, new Timestamp(cutoff.getTime()));

      int numDeleted = deleteStmt.executeUpdate();

      dbConn.commit();

      return numDeleted;
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @param seriesId
   * @param table
   * @throws SQLException
   */
  private void resetLatestIds(Connection dbConn, long seriesId, String table) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append(
        "UPDATE incaseriesconfig " +
        "SET incalatestinstanceid = -1, incalatestcomparisonid = -1 " +
        "WHERE incaseries_id = ? " +
          "AND incalatestinstanceid NOT IN ( SELECT incaid FROM "
    );
    queryBuilder.append(table);
    queryBuilder.append(" )");

    PreparedStatement deleteStmt = dbConn.prepareStatement(queryBuilder.toString());

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
   * @param table
   * @return
   * @throws SQLException
   */
  private int deleteOrphanedReports(Connection dbConn, long seriesId, String table) throws SQLException
  {
    StringBuilder queryBuilder = new StringBuilder();

    queryBuilder.append(
        "DELETE FROM incareport " +
        "WHERE incaseries_id = ? " +
          "AND incaid NOT IN ( SELECT incareportid FROM "
    );
    queryBuilder.append(table);
    queryBuilder.append(" )");

    PreparedStatement deleteStmt = dbConn.prepareStatement(queryBuilder.toString());

    try {
      deleteStmt.setLong(1, seriesId);

      int result = deleteStmt.executeUpdate();

      dbConn.commit();

      return result;
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private int deleteOrphanedComparisons(Connection dbConn) throws SQLException
  {
    Statement deleteStmt = dbConn.createStatement();

    try {
      int result = deleteStmt.executeUpdate(
          "DELETE FROM incacomparisonresult " +
          "WHERE incareportid NOT IN ( " +
            "SELECT incaid " +
            "FROM incareport " +
          ")"
      );

      dbConn.commit();

      return result;
    }
    finally {
      deleteStmt.close();
    }
  }

  /**
   *
   * @param dbConn
   * @return
   * @throws SQLException
   */
  private int deleteOrphanedRunInfo(Connection dbConn) throws SQLException
  {
    Statement deleteStmt = dbConn.createStatement();

    try {
      int result = deleteStmt.executeUpdate(
          "DELETE FROM incaruninfo " +
          "WHERE incaid NOT IN ( " +
            "SELECT incaruninfo_id " +
            "FROM incareport " +
          ")"
      );

      dbConn.commit();

      return result;
    }
    finally {
      deleteStmt.close();
    }
  }
}
