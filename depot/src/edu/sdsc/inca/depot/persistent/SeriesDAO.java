package edu.sdsc.inca.depot.persistent;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;


/**
 * This class transfers Series objects between memory and the DB.
 */
public class SeriesDAO extends DAO {

  private static final Logger logger = Logger.getLogger(SeriesDAO.class);


  /**
   * Returns a Series from the DB with the same field values as one specified,
   * null if no such Series appears in the DB.
   *
   * @param s an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static Series load(Series s) throws PersistenceException {
    String query =
      "select s from Series as s " +
              " where s.resource = :p0" +
              " and s.context = :p1" +
              " and s.version = :p2";
    Object[] params =
      new Object[] {s.getResource(), s.getContext(), s.getVersion()};
    return (Series)DAO.selectUnique(query, params);
  }

  /**
   * Returns a Series object from the DB with the id specified, or null if no
   * such object appears in the DB.
   *
   * @param id the id of the object to be retrieved
   * @return an object from the DB marked with the given id
   * @throws PersistenceException on err
   */
  public static Series load(Long id) throws PersistenceException {
    String query = "select s from Series as s where s.id = :p0";
    Object[] params = new Object[] { id };
    return (Series)DAO.selectUnique(query, params);
  }

  /**
   * Returns a Series object from the DB with the same field values as one
   * specified, or the saved version of the specified object if no such object
   * appears in the DB.  Synchronized to avoid race conditions that could
   * result in DB duplicates.
   *
   * @param s an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static synchronized Series loadOrSave(Series s)
    throws PersistenceException {
    Series dbSeries = SeriesDAO.load(s);
    if(dbSeries != null) {
      return dbSeries;
    }
    return save(s);
  }

  /**
   * Saves the specified Series object
   *
   * @param s an object that contains field values used in the retrieval
   * @return an object from the DB that contains the same values
   * @throws PersistenceException on err
   */
  public static Series save(Series s) throws PersistenceException {

    s.setArgSignature(ArgSignatureDAO.loadOrSave(s.getArgSignature()));

    s = (Series)DAO.save(s);

    Long seriesId = s.getId();
    String instanceTableName = "incainstanceinfo_" + seriesId;
    String linkTableName = "incaseriesconfigsinstances_" + seriesId;

    s.setInstanceTableName(instanceTableName);
    s.setLinkTableName(linkTableName);

    logger.debug("Creating tables " + instanceTableName + " and " + linkTableName + " for Series " + seriesId);

    createInstanceTables(instanceTableName, linkTableName);

    return (Series)DAO.update(s);
  }

  /**
   * A wrapper around DAO.update that handles the necessary casting.
   *
   * @param s the Series to update
   * @return s for convenience
   * @throws PersistenceException on error
   */
  public static Series update(Series s) throws PersistenceException {
    s.setArgSignature(ArgSignatureDAO.loadOrSave(s.getArgSignature()));
    return (Series)DAO.update(s);
  }

  /**
   *
   * @param instanceTableName
   * @param linkTableName
   * @throws PersistenceException
   */
  public static void createInstanceTables(String instanceTableName, String linkTableName) throws PersistenceException {

    try {
      Connection dbConn = ConnectionSource.getConnection();

      try {
        dbConn.setAutoCommit(false);

        createInstanceTables(dbConn, instanceTableName, linkTableName);
      }
      finally {
        dbConn.close();
      }
    }
    catch (SQLException sqlErr) {
      throw new PersistenceException(sqlErr);
    }
  }

  /**
   *
   * @param dbConn
   * @param instanceTableName
   * @param linkTableName
   * @throws SQLException
   */
  public static void createInstanceTables(Connection dbConn, String instanceTableName, String linkTableName) throws SQLException {

    String keyTypeName = DatabaseTools.getKeyTypeName();
    String longTypeName = DatabaseTools.getLongTypeName();
    String dateTypeName = DatabaseTools.getDateTypeName();
    String floatTypeName = DatabaseTools.getTextTypeName();
    String stringTypeName = DatabaseTools.getStringTypeName();
    StringBuilder instanceQueryBuilder = new StringBuilder();

    instanceQueryBuilder.append("CREATE TABLE ");
    instanceQueryBuilder.append(instanceTableName);
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
    instanceQueryBuilder.append("(4000), incareportid ");
    instanceQueryBuilder.append(longTypeName);
    instanceQueryBuilder.append(" NOT NULL, PRIMARY KEY (incaid) )");

    StringBuilder linkQueryBuilder = new StringBuilder();

    linkQueryBuilder.append("CREATE TABLE ");
    linkQueryBuilder.append(linkTableName);
    linkQueryBuilder.append(" ( incainstance_id ");
    linkQueryBuilder.append(longTypeName);
    linkQueryBuilder.append(" NOT NULL, incaseriesconfig_id ");
    linkQueryBuilder.append(longTypeName);
    linkQueryBuilder.append(" NOT NULL, PRIMARY KEY (incaseriesconfig_id, incainstance_id), " +
        "FOREIGN KEY (incainstance_id) REFERENCES ");
    linkQueryBuilder.append(instanceTableName);
    linkQueryBuilder.append(" )");

    Statement createStmt = dbConn.createStatement();

    try {
      createStmt.executeUpdate(instanceQueryBuilder.toString());
      createStmt.executeUpdate(linkQueryBuilder.toString());

      dbConn.commit();
    }
    catch (SQLException sqlErr) {
      dbConn.rollback();

      throw sqlErr;
    }
    finally {
      createStmt.close();
    }
  }
}
